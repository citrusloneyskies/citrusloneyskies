package com.lastbastion.fx;

/** Shapes the client-side VFX renderer knows how to draw. See {@link Vfx} for the parameter meaning of each. */
public enum VfxKind {
    BEAM, ARC, RING, SLASH, PILLAR, DOME, SPHERE, FLASH,
    TELE_CIRCLE, TELE_LINE, TELE_CONE, TELE_DONUT,
    SHAKE, VORTEX, SIGIL, TETHER, WAVE, AURA, DASH, SCREEN_FLASH,
    /** Client-side particle emitters: one packet, many particles. */
    JET, SPRAY;

    private static final VfxKind[] VALUES = values();

    public static VfxKind byId(int id) {
        return VALUES[Math.floorMod(id, VALUES.length)];
    }
}
