import { describe, it, expect, beforeAll, afterAll } from "vitest";
import { NextRequest } from "next/server";
import { db } from "@/server/db";
import { MerchantVehicleSchema, VehiclePucSchema, pucState } from "@/lib/assets";
import { findPuc, pucNumberFrom } from "@/server/digilocker";
import { POST as addPuc } from "@/app/api/households/[id]/vehicles/[vehicleId]/puc/route";
import { GET as startDigilocker } from "@/app/api/households/[id]/vehicles/[vehicleId]/puc/digilocker/route";

const stamp = Date.now();
const req = (url: string, init: { method?: string; body?: unknown; headers?: Record<string, string> } = {}) =>
  new NextRequest(`http://localhost:3000${url}`, {
    method: init.method ?? "GET",
    headers: { "content-type": "application/json", ...(init.headers ?? {}) },
    body: init.body === undefined ? undefined : JSON.stringify(init.body),
  });
const p = <T extends Record<string, string>>(v: T) => ({ params: Promise.resolve(v) });
const CAR = { type: "Car", fuel: "Petrol", make: "Demo", model: "Hatch", year: 2026, vin: "MA1TEST0000054321" };

describe("Phase 33: vehicles at the counter, GPS ID and PUC from DigiLocker", () => {
  it("takes an optional GPS / tracker ID with a vehicle, and drops a blank one", () => {
    expect(MerchantVehicleSchema.parse({ ...CAR, gpsDeviceId: "356938035643809" }).gpsDeviceId).toBe("356938035643809");
    expect(MerchantVehicleSchema.parse({ ...CAR, gpsDeviceId: "tcu-ab12-99" }).gpsDeviceId).toBe("TCU-AB12-99");
    expect(MerchantVehicleSchema.parse({ ...CAR, gpsDeviceId: "" }).gpsDeviceId).toBeNull();
    expect(MerchantVehicleSchema.parse(CAR).gpsDeviceId).toBeUndefined();
    expect(MerchantVehicleSchema.safeParse({ ...CAR, gpsDeviceId: "12 34" }).success).toBe(false);
  });

  it("says PUC is pending until added, valid or expired after, and not needed for electric", () => {
    const now = new Date("2026-10-27T00:00:00Z");
    expect(pucState({ fuel: "Electric" }, now)).toBe("NOT_NEEDED");
    expect(pucState({ fuel: "Petrol" }, now)).toBe("PENDING");
    expect(pucState({ fuel: "Diesel", pucNumber: "UP16-123", pucValidUntil: "2027-04-27" }, now)).toBe("VALID");
    expect(pucState({ fuel: "CNG", pucNumber: "UP16-123", pucValidUntil: "2026-01-01" }, now)).toBe("EXPIRED");
    expect(VehiclePucSchema.safeParse({ pucNumber: "ab", validUntil: "2027-01-01" }).success).toBe(false);
  });

  it("finds the PUC among DigiLocker documents, preferring the one for this vehicle", () => {
    const docs = [
      { name: "Driving Licence", uri: "in.gov.transport-DRVLC-UP1620190001", doctype: "DRVLC", date: "01-01-2024" },
      { name: "PUC Certificate", uri: "in.gov.transport-PUCER-OTHER111", doctype: "PUCER", date: "10-10-2026", description: "DL3CAB1234" },
      { name: "PUC Certificate", uri: "in.gov.transport-PUCER-UP16AB0001", doctype: "PUCER", date: "01-09-2026", description: "UP16AB0001" },
    ];
    const hit = findPuc(docs, { registrationNo: "UP 16 AB 0001", vin: null });
    expect(hit?.uri).toBe("in.gov.transport-PUCER-UP16AB0001");
    expect(pucNumberFrom(hit!)).toBe("UP16AB0001");
    // no match on the vehicle: the most recently issued PUC
    expect(findPuc(docs, { registrationNo: null, vin: null })?.uri).toBe("in.gov.transport-PUCER-OTHER111");
    expect(findPuc([docs[0]], { registrationNo: null, vin: null })).toBeNull();
  });

  describe("adding a PUC to a home's vehicle", () => {
    let HH = "";
    let ownerId = "";
    let viewerId = "";
    let petrol = "";
    let electric = "";

    beforeAll(async () => {
      const owner = await db.user.create({ data: { name: "PUC Owner", email: `puc.owner.${stamp}@example.com` } });
      const viewer = await db.user.create({ data: { name: "PUC Viewer", email: `puc.viewer.${stamp}@example.com` } });
      const hh = await db.household.create({
        data: {
          name: `PUC Home ${stamp}`,
          city: "Noida",
          state: "Uttar Pradesh",
          size: 2,
          homeType: "Apartment",
          floorAreaM2: 70,
          joinCode: `PUC${String(stamp).slice(-6)}`,
          memberships: { create: [{ userId: owner.id, role: "OWNER" }, { userId: viewer.id, role: "VIEWER" }] },
          vehicles: {
            create: [
              { name: "Petrol hatch", type: "Car", fuel: "Petrol", kmPerUnit: 18, monthlyKm: 500, vin: "MA1TEST0000077777", gpsDeviceId: "356938035643809" },
              { name: "E-scooter", type: "TwoWheeler", fuel: "Electric", kmPerUnit: 30, monthlyKm: 300 },
            ],
          },
        },
        include: { vehicles: true },
      });
      HH = hh.id;
      ownerId = owner.id;
      viewerId = viewer.id;
      petrol = hh.vehicles.find((v) => v.fuel === "Petrol")!.id;
      electric = hh.vehicles.find((v) => v.fuel === "Electric")!.id;
    });

    afterAll(async () => {
      await db.household.deleteMany({ where: { id: HH } });
      await db.user.deleteMany({ where: { id: { in: [ownerId, viewerId] } } });
    });

    it("lets the owner type the PUC in, and records it", async () => {
      const res = await addPuc(
        req("/x", { method: "POST", headers: { "x-user-id": ownerId }, body: { pucNumber: "up16-ab-778899", validUntil: "2027-04-30" } }),
        p({ id: HH, vehicleId: petrol }),
      );
      expect(res.status).toBe(200);
      const v = await db.vehicle.findUniqueOrThrow({ where: { id: petrol } });
      expect(v).toMatchObject({ pucNumber: "UP16-AB-778899", pucSource: "MANUAL", gpsDeviceId: "356938035643809" });
      expect(pucState(v)).toBe("VALID");
      expect(await db.auditLog.count({ where: { householdId: HH, action: "vehicle.puc" } })).toBe(1);
    });

    it("refuses a PUC for an electric vehicle, and a viewer adding one", async () => {
      const ev = await addPuc(req("/x", { method: "POST", headers: { "x-user-id": ownerId }, body: { pucNumber: "X1234", validUntil: "2027-01-01" } }), p({ id: HH, vehicleId: electric }));
      expect(ev.status).toBe(400);
      const viewer = await addPuc(req("/x", { method: "POST", headers: { "x-user-id": viewerId }, body: { pucNumber: "X1234", validUntil: "2027-01-01" } }), p({ id: HH, vehicleId: petrol }));
      expect(viewer.status).toBe(403);
    });

    it("says DigiLocker isn't connected when no partner client is set", async () => {
      const res = await startDigilocker(req("/x", { headers: { "x-user-id": ownerId } }), p({ id: HH, vehicleId: petrol }));
      expect(res.status).toBe(307);
      expect(res.headers.get("location")).toContain("/assets?puc=unavailable");
    });
  });
});
