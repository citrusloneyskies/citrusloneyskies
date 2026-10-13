package com.lastbastion.client.fx;

import com.lastbastion.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Trauma-style camera shake: overlapping impulses, distance falloff, smooth noise. */
public final class CameraShake {
    private record Impulse(float intensity, int life, int[] age) {
    }

    private static final List<Impulse> ACTIVE = new ArrayList<>();
    private static float time;

    private CameraShake() {
    }

    public static void add(Vec3 at, float intensity, float radius, int life) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ClientConfig.screenShake()) return;
        double d = mc.player.position().distanceTo(at);
        float fall = radius <= 0 ? 1f : (float) Mth.clamp(1 - d / radius, 0, 1);
        if (fall <= 0) return;
        if (ACTIVE.size() > 24) ACTIVE.remove(0);
        ACTIVE.add(new Impulse(intensity * fall, Math.max(2, life), new int[]{0}));
    }

    public static void tick() {
        time += 1;
        ACTIVE.removeIf(i -> ++i.age[0] >= i.life);
    }

    /** Current shake amplitude in degrees. */
    public static float amplitude(float pt) {
        float a = 0;
        for (Impulse i : ACTIVE) {
            float t = (i.age[0] + pt) / i.life;
            float k = 1 - t;
            a += i.intensity * k * k;
        }
        return Math.min(a, 4f) * 2.2f;
    }

    public static float[] offsets(float pt) {
        float amp = amplitude(pt);
        if (amp <= 0.001f) return null;
        float t = (time + pt) * 1.7f;
        float yaw = (Mth.sin(t * 1.9f) + Mth.sin(t * 3.7f + 1.3f) * 0.5f) * amp * 0.6f;
        float pitch = (Mth.sin(t * 2.3f + 0.7f) + Mth.sin(t * 4.1f + 2.1f) * 0.5f) * amp * 0.5f;
        float roll = Mth.sin(t * 1.3f + 2.9f) * amp * 0.45f;
        return new float[]{yaw, pitch, roll};
    }
}
