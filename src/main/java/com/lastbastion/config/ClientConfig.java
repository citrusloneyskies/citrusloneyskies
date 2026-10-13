package com.lastbastion.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only visual preferences. */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SCREEN_SHAKE;
    public static final ForgeConfigSpec.DoubleValue TEXTURE_DETAIL;
    public static final ForgeConfigSpec.DoubleValue VFX_DENSITY;
    public static final ForgeConfigSpec.BooleanValue DAMAGE_NUMBERS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        SCREEN_SHAKE = b.comment("Camera shake from explosions, stomps and boss attacks.").define("screenShake", true);
        TEXTURE_DETAIL = b.comment("Resolution multiplier for the procedurally painted entity textures (lower = faster start-up, less VRAM).")
                .defineInRange("textureDetail", 1.0, 0.25, 1.5);
        VFX_DENSITY = b.comment("Particle density multiplier for Last Bastion effects.").defineInRange("vfxDensity", 1.0, 0.1, 2.0);
        DAMAGE_NUMBERS = b.comment("Floating damage numbers when you or your turrets and heroes hit something.").define("damageNumbers", true);
        SPEC = b.build();
    }

    private ClientConfig() {
    }

    private static <T> T safe(ForgeConfigSpec.ConfigValue<T> v, T def) {
        try {
            return SPEC.isLoaded() ? v.get() : def;
        } catch (Exception e) {
            return def;
        }
    }

    public static boolean screenShake() {
        return safe(SCREEN_SHAKE, true);
    }

    public static float textureDetail() {
        return safe(TEXTURE_DETAIL, 1.0).floatValue();
    }

    public static float vfxDensity() {
        return safe(VFX_DENSITY, 1.0).floatValue();
    }

    public static boolean damageNumbers() {
        return safe(DAMAGE_NUMBERS, true);
    }
}
