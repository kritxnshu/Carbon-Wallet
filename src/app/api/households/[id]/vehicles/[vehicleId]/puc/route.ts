import { NextRequest, NextResponse } from "next/server";
import { db } from "@/server/db";
import { requireMembership, handleAuthError } from "@/server/auth/guard";
import { VehiclePucSchema } from "@/lib/assets";

type Ctx = { params: Promise<{ id: string; vehicleId: string }> };

/** The family adds the vehicle's PUC certificate by hand, once it has been issued (DigiLocker is the other way in). */
export async function POST(req: NextRequest, { params }: Ctx) {
  try {
    const { id: householdId, vehicleId } = await params;
    const auth = await requireMembership(req, householdId, "asset:create");
    const vehicle = await db.vehicle.findFirst({ where: { id: vehicleId, householdId } });
    if (!vehicle) return NextResponse.json({ error: "Not found" }, { status: 404 });
    if (vehicle.fuel === "Electric") return NextResponse.json({ error: "Electric vehicles don't need a PUC certificate." }, { status: 400 });

    const parsed = VehiclePucSchema.safeParse(await req.json().catch(() => null));
    if (!parsed.success) return NextResponse.json({ error: parsed.error.issues[0]?.message ?? "Check the PUC details." }, { status: 400 });

    const updated = await db.vehicle.update({
      where: { id: vehicle.id },
      data: { pucNumber: parsed.data.pucNumber, pucValidUntil: parsed.data.validUntil, pucSource: "MANUAL", pucAddedAt: new Date() },
    });
    await db.auditLog.create({
      data: { householdId, userId: auth.userId, action: "vehicle.puc", entity: "Vehicle", details: `PUC ${parsed.data.pucNumber} added by hand` },
    });
    return NextResponse.json({ vehicle: updated });
  } catch (err) {
    return handleAuthError(err);
  }
}
