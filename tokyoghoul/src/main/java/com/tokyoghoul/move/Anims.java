package com.tokyoghoul.move;

/**
 * One-shot animation ids shared by the server (which plays them) and the client models (which pose
 * them). Body animations move the limbs; the kagune and quinque layers read the same id to strike,
 * spread, spin or guard in sync.
 */
public final class Anims {
    public static final int NONE = 0;
    public static final int JAB = 1;
    public static final int HEAVY = 2;
    public static final int BARRAGE = 3;
    public static final int KICK = 4;
    public static final int UPPERCUT = 5;
    /** Horizontal weapon / kagune slash. */
    public static final int SLASH = 6;
    /** Vertical overhead chop. */
    public static final int OVERHEAD = 7;
    /** Lance or blade thrust. */
    public static final int THRUST = 8;
    /** Arm thrown forward to fire something. */
    public static final int CAST = 9;
    public static final int K_PIERCE = 10;
    public static final int K_SWEEP = 11;
    public static final int K_BARRAGE = 12;
    public static final int K_GRAB = 13;
    public static final int K_VOLLEY = 14;
    public static final int DASH = 15;
    public static final int LEAP = 16;
    public static final int SLAM = 17;
    public static final int GUARD = 18;
    public static final int TRANSFORM = 19;
    public static final int THROW = 20;
    public static final int SPIN = 21;
    public static final int BITE = 22;
    public static final int BREATH = 23;
    public static final int DODGE = 24;
    public static final int STAGGER = 25;
    public static final int K_SHIELD = 26;
    public static final int K_SPIKES = 27;
    public static final int REGEN = 28;
    public static final int CHARGE = 29;
    public static final int HURT = 30;

    /** How long each animation runs before the body returns to its idle pose (ticks). */
    public static int length(int id) {
        return switch (id) {
            case JAB -> 8;
            case HEAVY, KICK, UPPERCUT, SLASH, OVERHEAD, THRUST, CAST, THROW -> 14;
            case BARRAGE, K_BARRAGE, BREATH, CHARGE, REGEN -> 200;
            case K_PIERCE, K_SWEEP, K_VOLLEY, K_SPIKES -> 22;
            case K_GRAB -> 36;
            case DASH, DODGE -> 12;
            case LEAP -> 30;
            case SLAM -> 16;
            case GUARD, K_SHIELD -> 200;
            case TRANSFORM -> 40;
            case SPIN -> 24;
            case BITE -> 20;
            case STAGGER, HURT -> 10;
            default -> 0;
        };
    }

    /** Animations that are held for as long as the move runs (the server clears them when it ends). */
    public static boolean held(int id) {
        return id == BARRAGE || id == K_BARRAGE || id == BREATH || id == GUARD || id == K_SHIELD || id == CHARGE || id == REGEN;
    }

    private Anims() {
    }
}
