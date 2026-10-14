package com.lastbastion.item.weapon;

/**
 * Shapes of a melee swing. Each style drives the hit area, the slash VFX and the first/third person
 * animation, so every weapon's combo reads differently on screen.
 */
public enum SwingStyle {
    //            tilt  sweep  arcDeg  lunge
    /** Wide horizontal cut from right to left. */
    SLASH_R(8f, 150f, 120f, 0f),
    /** Backhand cut from left to right. */
    SLASH_L(-8f, -150f, 120f, 0f),
    /** Diagonal cut from top right to bottom left. */
    DIAG_DOWN(-52f, 140f, 100f, 0f),
    /** Rising diagonal cut from bottom left to top right. */
    DIAG_UP(-52f, -140f, 100f, 0f),
    /** Vertical overhead chop. */
    OVERHEAD(88f, -120f, 50f, 0.15f),
    /** Straight thrust: narrow, long. */
    THRUST(0f, 0f, 24f, 0.25f),
    /** Full turn: hits everything around. */
    SPIN(4f, 360f, 360f, 0f),
    /** Rising uppercut that launches targets. */
    UPPERCUT(-82f, -120f, 60f, 0.1f);

    /**
     * Roll of the slash plane in degrees (0 = horizontal, 90 = vertical). With the VFX renderer's
     * convention a positive sweep runs from the attacker's right (or below, for steep tilts) to the left.
     */
    public final float tilt;
    /** Sweep of the slash crescent in degrees (negative sweeps the other way). */
    public final float sweep;
    /** Width of the hit cone in degrees (full angle). */
    public final float arcDeg;
    /** Forward step the swing gives the attacker (blocks/tick impulse). */
    public final float lunge;

    SwingStyle(float tilt, float sweep, float arcDeg, float lunge) {
        this.tilt = tilt;
        this.sweep = sweep;
        this.arcDeg = arcDeg;
        this.lunge = lunge;
    }

    public static SwingStyle byId(int id) {
        SwingStyle[] v = values();
        return v[Math.floorMod(id, v.length)];
    }
}
