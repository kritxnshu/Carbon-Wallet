import { createHash, randomBytes } from "crypto";

/*
 * DigiLocker (MeitY) for a vehicle's PUC certificate: the family signs in to DigiLocker, allows access, and we read
 * the list of documents issued to them and keep the PUC that matches the vehicle. OAuth 2.0 with PKCE, per the
 * Authorized Partner API. Needs a partner client from API Setu: DIGILOCKER_CLIENT_ID, DIGILOCKER_CLIENT_SECRET and
 * DIGILOCKER_REDIRECT_URI (…/api/digilocker/callback). DIGILOCKER_BASE_URL overrides the API host.
 */

const BASE = () => (process.env.DIGILOCKER_BASE_URL || "https://api.digitallocker.gov.in/public").replace(/\/$/, "");
const TIMEOUT_MS = 15_000;

export function digilockerConfigured() {
  return !!(process.env.DIGILOCKER_CLIENT_ID && process.env.DIGILOCKER_CLIENT_SECRET && process.env.DIGILOCKER_REDIRECT_URI);
}

export function pkce() {
  const verifier = randomBytes(32).toString("base64url");
  const challenge = createHash("sha256").update(verifier).digest("base64url");
  return { verifier, challenge };
}

export function authorizeUrl(state: string, challenge: string) {
  const q = new URLSearchParams({
    response_type: "code",
    client_id: process.env.DIGILOCKER_CLIENT_ID!,
    redirect_uri: process.env.DIGILOCKER_REDIRECT_URI!,
    state,
    code_challenge: challenge,
    code_challenge_method: "S256",
  });
  return `${BASE()}/oauth2/1/authorize?${q}`;
}

export async function exchangeCode(code: string, verifier: string): Promise<string> {
  const res = await fetch(`${BASE()}/oauth2/1/token`, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "authorization_code",
      code,
      client_id: process.env.DIGILOCKER_CLIENT_ID!,
      client_secret: process.env.DIGILOCKER_CLIENT_SECRET!,
      redirect_uri: process.env.DIGILOCKER_REDIRECT_URI!,
      code_verifier: verifier,
    }),
    signal: AbortSignal.timeout(TIMEOUT_MS),
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok || !body.access_token) throw new Error(`DigiLocker sign-in failed (${res.status})`);
  return body.access_token as string;
}

export type IssuedDoc = { name: string; uri: string; doctype?: string; date?: string; issuer?: string; description?: string };

export async function issuedDocuments(token: string): Promise<IssuedDoc[]> {
  const res = await fetch(`${BASE()}/oauth2/1/files/issued`, {
    headers: { authorization: `Bearer ${token}` },
    signal: AbortSignal.timeout(TIMEOUT_MS),
  });
  if (!res.ok) throw new Error(`DigiLocker documents failed (${res.status})`);
  const body = await res.json().catch(() => ({}));
  return Array.isArray(body.items) ? body.items : [];
}

/** DigiLocker dates are "DD-MM-YYYY"; anything else is left alone. */
function issuedOn(d: IssuedDoc) {
  const m = /^(\d{2})-(\d{2})-(\d{4})$/.exec(d.date ?? "");
  return m ? new Date(`${m[3]}-${m[2]}-${m[1]}T00:00:00Z`).getTime() : 0;
}

/**
 * The PUC certificate among the issued documents: one whose type or name says PUC / pollution, preferring one that
 * mentions this vehicle's registration or chassis number, then the most recently issued.
 */
export function findPuc(docs: IssuedDoc[], vehicle: { registrationNo?: string | null; vin?: string | null }) {
  const pucs = docs.filter((d) => /\bpuc|pollution/i.test(`${d.doctype ?? ""} ${d.name ?? ""} ${d.description ?? ""}`));
  const ids = [vehicle.registrationNo, vehicle.vin].filter((x): x is string => !!x).map((x) => x.replace(/\s/g, "").toUpperCase());
  const mentions = (d: IssuedDoc) => ids.some((id) => `${d.name} ${d.description} ${d.uri}`.replace(/\s/g, "").toUpperCase().includes(id));
  return [...pucs].sort((a, b) => Number(mentions(b)) - Number(mentions(a)) || issuedOn(b) - issuedOn(a))[0] ?? null;
}

/**
 * A PUC is valid for a fixed period from its test date; we keep the issue date DigiLocker gives and leave the expiry
 * for the family to confirm, since the period depends on the vehicle (and is printed on the certificate).
 */
export function pucNumberFrom(doc: IssuedDoc) {
  return (doc.uri.split("-").pop() || doc.name).slice(0, 30).toUpperCase();
}
