package com.lastbastion.client.render.weapon;

/**
 * Pure transform recipes for the firearms (see {@link WeaponPoses} for the conventions). Gun models
 * point their muzzle along -Z with the grip at the origin; {@link #model} maps them to item space with
 * the muzzle forward (-Z) and the top of the gun up (+Y).
 */
public final class GunPoses {
    private GunPoses() {
    }

    public static float scale(String kind) {
        return switch (kind) {
            case "RAVAGER", "STARPIERCER" -> 0.62f;
            case "HELLFIRE" -> 0.6f;
            case "STORMFIST" -> 0.78f;
            case "SUNLANCE" -> 0.64f;
            default -> 0.66f;
        };
    }

    /** Length of the model along Z, used to fit GUI and frame views. */
    public static float length(String kind) {
        return switch (kind) {
            case "RAVAGER" -> 32f;
            case "THUNDERBOOM" -> 28f;
            case "HELLFIRE" -> 26f;
            case "STARPIERCER" -> 34f;
            case "DRAGONBREATH" -> 27f;
            case "STORMFIST" -> 14f;
            case "GLACIER" -> 27f;
            case "SUNLANCE" -> 21f;
            default -> 26f;
        };
    }

    /** Distance from the grip to the visual centre along the barrel (model pixels toward the muzzle). */
    public static float centerOffset(String kind) {
        return switch (kind) {
            case "RAVAGER" -> 6f;
            case "STARPIERCER" -> 7f;
            case "STORMFIST" -> 0.5f;
            case "SUNLANCE" -> 2f;
            default -> 4f;
        };
    }

    public static void model(WeaponPoses.Xf x, float s) {
        x.scale(-s, -s, s);
    }

    /** Third person, in the vanilla hand frame; the aiming arm pose points the arm (and so -Z) forward. */
    public static void thirdPerson(WeaponPoses.Xf x, String kind, boolean left) {
        if (left) x.scale(-1, 1, 1);
        x.translate(0, 0.07f, -0.02f);
        model(x, scale(kind) * 0.85f);
    }

    /**
     * First person, after vanilla's arm placement: the gun sits low on the right, angled slightly toward
     * the crosshair. {@code recoil} (0..1) kicks it back and up; {@code reload} (0..1) tips it down to swap
     * the magazine; {@code charge} (0..1) pulls it in tight while a shot charges.
     */
    public static void firstPerson(WeaponPoses.Xf x, String kind, boolean left, float recoil, float reload, float charge) {
        if (left) x.scale(-1, 1, 1);
        boolean gauntlet = kind.equals("STORMFIST");
        if (gauntlet) {
            x.translate(-0.02f, 0.06f, -0.04f);
            x.rotY(-6);
        } else {
            x.translate(-0.16f, 0.17f, 0.06f);
            x.rotY(-5);
            x.rotX(2);
        }
        if (charge > 0) {
            x.translate(-0.06f * charge, 0.03f * charge, 0.04f * charge);
            x.rotZ(-6 * charge);
        }
        if (reload > 0) {
            float r = (float) Math.sin(Math.min(1f, reload) * Math.PI);
            x.translate(0.04f * r, -0.18f * r, 0.05f * r);
            x.rotX(-28 * r);
            x.rotZ(32 * r);
        }
        if (recoil > 0) {
            x.translate(0, 0.02f * recoil, 0.11f * recoil);
            x.rotX(9 * recoil);
        }
        model(x, scale(kind));
    }

    /** Side profile for inventories and item frames: muzzle to the upper right. */
    public static void profile(WeaponPoses.Xf x, String kind, float fill) {
        x.rotZ(30);
        x.rotY(-90);
        x.rotX(12);
        float s = fill / length(kind);
        x.translate(0, 0, centerOffset(kind) * s / 16f);
        model(x, s);
    }

    public static void ground(WeaponPoses.Xf x, String kind) {
        x.translate(0, -0.1f, 0);
        x.rotY(-90);
        float s = 14f / length(kind);
        x.translate(0, 0, centerOffset(kind) * s / 16f);
        model(x, s);
    }
}
