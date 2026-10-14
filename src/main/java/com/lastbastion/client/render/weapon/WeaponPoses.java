package com.lastbastion.client.render.weapon;

import com.lastbastion.item.weapon.SwingStyle;
import com.lastbastion.item.weapon.WeaponKind;
import com.lastbastion.item.weapon.WeaponKind.WeaponClass;

/**
 * Pure transform recipes for the 3D weapons: where the weapon sits in each display context and how
 * it moves through every {@link SwingStyle}. Kept free of rendering classes so the same numbers drive
 * the in-game renderer and the offline model previewer.
 *
 * <p>Model space is vanilla model pixels (Y down, blade toward -Y, flats along Z). {@link #model}
 * converts it to item space (blocks, Y up) with the blade along +Y and the flat faces looking along X.
 */
public final class WeaponPoses {
    /** Minimal transform sink (PoseStack in game, a matrix in the previewer). */
    public interface Xf {
        void translate(float x, float y, float z);

        void rotX(float deg);

        void rotY(float deg);

        void rotZ(float deg);

        void scale(float x, float y, float z);
    }

    private WeaponPoses() {
    }

    /** Overall size of each weapon relative to its model, per class. */
    public static float scale(WeaponKind k) {
        return switch (k.weaponClass) {
            case DAGGER -> 0.95f;
            case LONGSWORD, KATANA -> 0.8f;
            case AXE, HAMMER -> 0.78f;
            case GREATSWORD -> 0.74f;
            case SPEAR, SCYTHE -> 0.72f;
        };
    }

    /**
     * Model pixels -> item space with the blade along +Y. In third person the flats look sideways (edges
     * up and down, axe and scythe heads leading downward); in first person the flats face the camera and
     * hooked heads point inward, toward the centre of the screen.
     */
    public static void model(Xf x, WeaponKind k, float s, boolean firstPerson) {
        boolean hooked = k.weaponClass == WeaponClass.AXE || k.weaponClass == WeaponClass.SCYTHE;
        x.scale(-s, -s, s);
        if (firstPerson) x.rotY(hooked ? 180 : 0);
        else x.rotY(hooked ? -90 : 90);
    }

    /** Where along the model the hand grips (model pixels from the grip origin, + = toward the pommel). */
    public static float gripOffset(WeaponKind k) {
        return switch (k.weaponClass) {
            case SPEAR -> 3.5f;
            case SCYTHE -> 2.0f;
            case HAMMER -> 6.0f;
            case AXE -> 5.0f;
            default -> 0f;
        };
    }

    // ------------------------------------------------------------------ third person

    /**
     * Third person, in the vanilla hand frame (origin at the front of the fist, +X to the body's right,
     * +Y forward, +Z up along the arm).
     */
    public static void thirdPerson(Xf x, WeaponKind k, boolean left) {
        if (left) x.scale(-1, 1, 1);
        float tilt = switch (k.weaponClass) {
            case SPEAR -> 6f;
            case SCYTHE -> 68f;
            case HAMMER, AXE -> 30f;
            case GREATSWORD -> 32f;
            case DAGGER -> 20f;
            default -> 26f;
        };
        x.translate(0, -0.04f, 0.02f);
        x.rotX(tilt);
        float s = scale(k) * 0.9f;
        x.translate(0, gripOffset(k) * s / 16f, 0);
        model(x, k, s, false);
    }

    // ------------------------------------------------------------------ first person

    /** First-person rest pose (relative to vanilla's arm transform at the bottom corner of the view). */
    public static void firstPersonRest(Xf x, WeaponKind k, boolean left) {
        if (left) x.scale(-1, 1, 1);
        switch (k.weaponClass) {
            case SPEAR -> {
                x.translate(-0.02f, -0.16f, 0.06f);
                x.rotY(-8);
                x.rotX(-34);
                x.rotZ(30);
            }
            case SCYTHE -> {
                x.translate(0.02f, -0.22f, 0.04f);
                x.rotY(-20);
                x.rotX(-16);
                x.rotZ(24);
            }
            case HAMMER, AXE -> {
                x.translate(0.0f, -0.18f, 0.04f);
                x.rotY(-30);
                x.rotX(-22);
                x.rotZ(34);
            }
            case GREATSWORD -> {
                x.translate(-0.02f, -0.12f, 0.02f);
                x.rotY(-14);
                x.rotX(-22);
                x.rotZ(30);
            }
            case DAGGER -> {
                x.translate(-0.02f, 0.02f, 0.0f);
                x.rotY(-12);
                x.rotX(-40);
                x.rotZ(20);
            }
            default -> {
                x.translate(-0.03f, -0.04f, 0.0f);
                x.rotY(-14);
                x.rotX(-30);
                x.rotZ(28);
            }
        }
    }

    /** First person model transform applied after the rest pose and any animation. */
    public static void firstPersonModel(Xf x, WeaponKind k) {
        float s = scale(k);
        x.translate(0, gripOffset(k) * s / 16f, 0);
        model(x, k, s, true);
    }

    /**
     * First-person swing: called after vanilla's arm placement and before {@link #firstPersonRest}.
     * {@code t} runs 0..1 through the swing; heavier weapons get a longer wind-up and a bigger arc.
     */
    public static void firstPersonSwing(Xf x, SwingStyle style, float t, WeaponKind k, boolean left) {
        if (t <= 0 || t >= 1) return;
        float side = left ? -1 : 1;
        float heft = k.weaponClass.heft;
        // phases: wind-up (0..a), strike (a..b), recover (b..1)
        float a = 0.22f * heft, b = a + 0.28f;
        float wind = t < a ? ease(t / a) : t < b ? 1f - easeOut((t - a) / (b - a)) : 0f;
        float strike = t < a ? 0f : t < b ? easeOut((t - a) / (b - a)) : 1f - smooth((t - b) / (1f - b));
        float follow = t < b ? 0f : 1f - smooth((t - b) / (1f - b));
        switch (style) {
            case SLASH_R -> {
                x.translate(side * (0.2f * wind - 0.45f * strike), 0.12f * wind - 0.05f * strike, -0.15f * strike);
                x.rotY(side * (55f * wind - 70f * strike));
                x.rotZ(side * (-30f * wind + 72f * strike));
                x.rotX(-25f * strike);
            }
            case SLASH_L -> {
                x.translate(side * (-0.3f * wind + 0.25f * strike), 0.08f * wind - 0.05f * strike, -0.15f * strike);
                x.rotY(side * (-45f * wind + 70f * strike));
                x.rotZ(side * (40f * wind - 85f * strike));
                x.rotX(-28f * strike);
            }
            case DIAG_DOWN -> {
                x.translate(side * (0.12f * wind - 0.35f * strike), 0.3f * wind - 0.35f * strike, -0.1f * strike);
                x.rotZ(side * (-25f * wind + 55f * strike));
                x.rotX(30f * wind - 70f * strike);
                x.rotY(side * (20f * wind - 30f * strike));
            }
            case DIAG_UP -> {
                x.translate(side * (-0.25f * wind + 0.2f * strike), -0.3f * wind + 0.32f * strike, -0.1f * strike);
                x.rotZ(side * (50f * wind - 30f * strike));
                x.rotX(-50f * wind + 35f * strike);
                x.rotY(side * (-30f * wind + 25f * strike));
            }
            case OVERHEAD -> {
                x.translate(0, 0.32f * wind - 0.32f * strike, 0.12f * wind - 0.28f * strike);
                x.rotX(45f * wind - 95f * strike);
                x.rotZ(side * (-10f * wind + 6f * strike));
            }
            case THRUST -> {
                x.translate(side * -0.08f * strike, 0.04f * wind, 0.22f * wind - 0.55f * strike);
                x.rotX(-55f * strike - 10f * wind);
                x.rotY(side * 10f * strike);
            }
            case SPIN -> {
                float spin = t < a ? -25f * wind : t < b ? -25f + 385f * easeOut((t - a) / (b - a)) : 360f * (1f - 0f * follow);
                x.translate(0, -0.05f * strike, -0.12f * strike);
                x.rotY(side * -spin * 0.35f);
                x.rotZ(side * (30f * strike));
                x.rotX(-40f * strike);
            }
            case UPPERCUT -> {
                x.translate(side * -0.1f * strike, -0.35f * wind + 0.4f * strike, -0.12f * strike);
                x.rotX(-40f * wind + 60f * strike);
                x.rotZ(side * (12f * wind - 20f * strike));
            }
        }
    }

    /** First person while charging a special: the weapon rises overhead, trembling with power when full. */
    public static void firstPersonCharge(Xf x, float t, boolean left) {
        float side = left ? -1 : 1;
        float e = ease(t);
        float tremble = t >= 1f ? (float) Math.sin(System.nanoTime() / 2.5e7) * 0.006f : 0f;
        x.translate(side * (-0.18f * e) + tremble, 0.22f * e, 0.08f * e);
        x.rotZ(side * -28f * e);
        x.rotX(32f * e);
    }

    // ------------------------------------------------------------------ world & GUI

    /** Item frames and display stands: the weapon lies diagonally across the frame. */
    public static void fixed(Xf x, WeaponKind k) {
        x.rotZ(-45);
        float len = length(k);
        float s = Math.min(1.25f, 22f / len) * 0.9f;
        x.translate(0, -centerOffset(k) * s / 16f, 0);
        x.scale(-s, -s, s);
    }

    /** Dropped on the ground: small and upright, spun by the item entity renderer. */
    public static void ground(Xf x, WeaponKind k) {
        float len = length(k);
        float s = Math.min(1f, 16f / len) * 0.72f;
        x.translate(0, -0.12f - centerOffset(k) * s / 16f, 0);
        x.scale(-s, -s, s);
        x.rotY(90);
    }

    /** GUI (used when no icon sprite is available, and to bake icons). */
    public static void gui(Xf x, WeaponKind k) {
        x.rotZ(-45);
        float len = length(k);
        float s = 21.5f / len;
        x.translate(0, -centerOffset(k) * s / 16f, 0);
        x.scale(-s, -s, s);
        x.rotY(20);
    }

    /** Approximate total length in model pixels. */
    public static float length(WeaponKind k) {
        return switch (k) {
            case MILITIA_LONGSWORD -> 33f;
            case FROSTFANG -> 18f;
            case BONECLEAVER -> 32f;
            case VOIDEDGE -> 40f;
            case STORMPIERCER -> 47f;
            case EARTHSHAKER -> 37f;
            case DAWNBREAKER -> 47f;
            case SOULREAPER -> 46f;
        };
    }

    /** Distance from the grip origin to the visual centre (model pixels, + = toward the tip). */
    public static float centerOffset(WeaponKind k) {
        return switch (k) {
            case MILITIA_LONGSWORD -> 9f;
            case FROSTFANG -> 4.5f;
            case BONECLEAVER -> 3f;
            case VOIDEDGE -> 11f;
            case STORMPIERCER -> 7f;
            case EARTHSHAKER -> 3f;
            case DAWNBREAKER -> 12f;
            case SOULREAPER -> 4f;
        };
    }

    // ------------------------------------------------------------------ easing

    static float ease(float t) {
        t = clamp01(t);
        return t * t * (3 - 2 * t);
    }

    static float easeOut(float t) {
        t = clamp01(t);
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    static float smooth(float t) {
        t = clamp01(t);
        return t * t * (3 - 2 * t);
    }

    static float clamp01(float t) {
        return t < 0 ? 0 : Math.min(1, t);
    }
}
