import { NextRequest, NextResponse } from "next/server";
import { db } from "@/server/db";
import { requireMembership } from "@/server/auth/guard";
import { exchangeCode, findPuc, issuedDocuments, pucNumberFrom } from "@/server/digilocker";

const COOKIE = "cw_dl";

/** DigiLocker sends the family back here: read their issued documents and keep the vehicle's PUC certificate. */
export async function GET(req: NextRequest) {
  const done = (result: string) => {
    const res = NextResponse.redirect(new URL(`/assets?puc=${result}`, req.url));
    res.cookies.set(COOKIE, "", { path: "/api/digilocker", maxAge: 0 });
    return res;
  };
  try {
    const saved = JSON.parse(req.cookies.get(COOKIE)?.value ?? "null") as
      | { state: string; verifier: string; householdId: string; vehicleId: string }
      | null;
    const code = req.nextUrl.searchParams.get("code");
    if (!saved || !code || req.nextUrl.searchParams.get("state") !== saved.state) return done("error");

    // the person must still be signed in here and allowed to change this home's items
    const auth = await requireMembership(req, saved.householdId, "asset:create");
    const vehicle = await db.vehicle.findFirst({ where: { id: saved.vehicleId, householdId: saved.householdId } });
    if (!vehicle) return done("notfound");

    const token = await exchangeCode(code, saved.verifier);
    const puc = findPuc(await issuedDocuments(token), vehicle);
    if (!puc) return done("notfound");

    const pucNumber = pucNumberFrom(puc);
    await db.vehicle.update({ where: { id: vehicle.id }, data: { pucNumber, pucSource: "DIGILOCKER", pucAddedAt: new Date() } });
    await db.auditLog.create({
      data: { householdId: saved.householdId, userId: auth.userId, action: "vehicle.puc", entity: "Vehicle", details: `PUC ${pucNumber} from DigiLocker` },
    });
    return done("added");
  } catch {
    return done("error");
  }
}
