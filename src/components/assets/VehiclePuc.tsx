"use client";

import React, { useEffect, useState } from "react";
import { toast } from "sonner";
import { FileCheck2, Satellite } from "lucide-react";
import { pucState } from "@/lib/assets";

export type PucVehicle = {
  id: string;
  fuel: string;
  gpsDeviceId?: string | null;
  pucNumber?: string | null;
  pucValidUntil?: string | null;
  pucSource?: string | null;
};

type T = (en: string, hi: string) => string;
const day = (d: string) => new Date(d).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });

/** The vehicle's PUC certificate: pending until the family adds it from DigiLocker or types it in. Plus its GPS ID. */
export function VehiclePuc({
  householdId,
  vehicle,
  canAct,
  demoRole,
  onSaved,
  t,
}: {
  householdId: string;
  vehicle: PucVehicle;
  canAct: boolean;
  demoRole: string;
  onSaved: () => void;
  t: T;
}) {
  const state = pucState(vehicle);
  const [typing, setTyping] = useState(false);
  const [form, setForm] = useState({ pucNumber: "", validUntil: "" });
  const [busy, setBusy] = useState(false);

  async function save(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    const res = await fetch(`/api/households/${householdId}/vehicles/${vehicle.id}/puc`, {
      method: "POST",
      headers: { "content-type": "application/json", "x-demo-role": demoRole },
      body: JSON.stringify(form),
    });
    const out = await res.json().catch(() => ({}));
    setBusy(false);
    if (!res.ok) return toast.error(out.error ?? t("Couldn't save the PUC", "PUC सेव नहीं हुआ"));
    toast.success(t("PUC added", "PUC जुड़ गया"));
    setTyping(false);
    onSaved();
  }

  const chip = {
    NOT_NEEDED: ["border-border bg-canvas-subtle text-ink-muted", t("PUC not needed (electric)", "PUC ज़रूरी नहीं (इलेक्ट्रिक)")],
    PENDING: ["border-warning-border bg-warning-subtle text-warning-text", t("PUC pending", "PUC बाकी")],
    VALID: [
      "border-success-border bg-success-subtle text-success-text",
      vehicle.pucValidUntil ? t(`PUC valid till ${day(vehicle.pucValidUntil)}`, `PUC ${day(vehicle.pucValidUntil)} तक मान्य`) : t("PUC added", "PUC जुड़ा है"),
    ],
    EXPIRED: ["border-critical-border bg-critical-subtle text-critical-text", t("PUC expired", "PUC की अवधि ख़त्म")],
  }[state];

  return (
    <div className="mt-2 flex flex-col gap-2 px-4">
      <div className="flex flex-wrap items-center gap-2 text-[12px]">
        <span className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 font-semibold ${chip[0]}`}>
          <FileCheck2 className="h-3.5 w-3.5" /> {chip[1]}
        </span>
        {vehicle.pucNumber && (
          <span className="font-mono text-ink-muted">
            {vehicle.pucNumber}
            {vehicle.pucSource === "DIGILOCKER" ? " · DigiLocker" : ""}
          </span>
        )}
        {vehicle.gpsDeviceId && (
          <span className="inline-flex items-center gap-1 font-mono text-ink-muted">
            <Satellite className="h-3.5 w-3.5" /> GPS {vehicle.gpsDeviceId}
          </span>
        )}
      </div>
      {canAct && (state === "PENDING" || state === "EXPIRED") && !typing && (
        <div className="flex flex-wrap gap-2">
          <a
            href={`/api/households/${householdId}/vehicles/${vehicle.id}/puc/digilocker`}
            className="inline-flex h-9 items-center rounded-md bg-ink px-3 text-[13px] font-medium text-canvas-card hover:opacity-90"
          >
            {t("Add from DigiLocker", "DigiLocker से जोड़ें")}
          </a>
          <button onClick={() => setTyping(true)} className="inline-flex h-9 items-center rounded-md border border-border px-3 text-[13px] hover:border-ink">
            {t("Type it in", "ख़ुद लिखें")}
          </button>
        </div>
      )}
      {typing && (
        <form onSubmit={save} className="grid grid-cols-1 gap-2 sm:grid-cols-[1fr_1fr_auto]">
          <input
            className="h-9 rounded-md border border-border bg-canvas-card px-3 font-mono text-[13px] uppercase focus:border-ink focus:outline-none"
            placeholder={t("PUC certificate no.", "PUC प्रमाणपत्र नंबर")}
            aria-label={t("PUC certificate number", "PUC प्रमाणपत्र नंबर")}
            value={form.pucNumber}
            onChange={(e) => setForm({ ...form, pucNumber: e.target.value })}
            required
            minLength={4}
            maxLength={30}
          />
          <input
            type="date"
            className="h-9 rounded-md border border-border bg-canvas-card px-3 text-[13px] focus:border-ink focus:outline-none"
            aria-label={t("Valid until", "कब तक मान्य")}
            value={form.validUntil}
            onChange={(e) => setForm({ ...form, validUntil: e.target.value })}
            required
          />
          <div className="flex gap-2">
            <button type="submit" disabled={busy} className="h-9 rounded-md bg-ink px-3 text-[13px] font-medium text-canvas-card disabled:opacity-60">
              {busy ? t("Saving…", "सेव हो रहा…") : t("Save", "सेव करें")}
            </button>
            <button type="button" onClick={() => setTyping(false)} className="h-9 rounded-md border border-border px-3 text-[13px]">
              {t("Cancel", "रद्द")}
            </button>
          </div>
        </form>
      )}
    </div>
  );
}

/** Tells the family how the DigiLocker round trip went (the callback sends them back with ?puc=…). */
export function usePucReturnToast(t: T) {
  useEffect(() => {
    const result = new URLSearchParams(window.location.search).get("puc");
    if (!result) return;
    const messages: Record<string, [boolean, string]> = {
      added: [true, t("PUC added from DigiLocker", "DigiLocker से PUC जुड़ गया")],
      notfound: [false, t("No PUC certificate found in your DigiLocker yet. Try again once it's issued, or type it in.", "आपके DigiLocker में अभी PUC नहीं मिला। बनने के बाद फिर कोशिश करें, या ख़ुद लिखें।")],
      unavailable: [false, t("DigiLocker isn't connected yet. Type the PUC in for now.", "DigiLocker अभी जुड़ा नहीं है। अभी PUC ख़ुद लिखें।")],
      error: [false, t("DigiLocker didn't finish. Please try again.", "DigiLocker पूरा नहीं हुआ। फिर कोशिश करें।")],
    };
    const [ok, text] = messages[result] ?? messages.error;
    (ok ? toast.success : toast.error)(text);
    window.history.replaceState(null, "", window.location.pathname);
    // once, on arrival
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
}
