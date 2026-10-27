import { randomBytes } from "crypto";
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/server/db";
import { requireMembership, handleAuthError } from "@/server/auth/guard";
import { authorizeUrl, digilockerConfigured, pkce } from "@/server/digilocker";

type Ctx = { params: Promise<{ id: string; vehicleId: string }> };

// read back by /api/digilocker/callback
const DIGILOCKER_COOKIE = "cw_dl";

/** Opened from the vehicle's "Add PUC from DigiLocker" link: sends the family to DigiLocker to allow access. */
export async function GET(req: NextRequest, { params }: Ctx) {
  try {
    const { id: householdId, vehicleId } = await params;
    await requireMembership(req, householdId, "asset:create");
    const vehicle = await db.vehicle.findFirst({ where: { id: vehicleId, householdId }, select: { id: true } });
    if (!vehicle) return NextResponse.redirect(new URL("/assets?puc=notfound", req.url));
    if (!digilockerConfigured()) return NextResponse.redirect(new URL("/assets?puc=unavailable", req.url));

    const state = randomBytes(18).toString("base64url");
    const { verifier, challenge } = pkce();
    const res = NextResponse.redirect(authorizeUrl(state, challenge));
    // what the callback needs, kept only in this browser for ten minutes
    res.cookies.set(DIGILOCKER_COOKIE, JSON.stringify({ state, verifier, householdId, vehicleId }), {
      httpOnly: true,
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production" && process.env.COOKIE_INSECURE !== "1",
      path: "/api/digilocker",
      maxAge: 600,
    });
    return res;
  } catch (err) {
    return handleAuthError(err);
  }
}
