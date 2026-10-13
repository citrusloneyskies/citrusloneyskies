package com.lastbastion.combat;

import com.lastbastion.effect.SiegeEffect;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Server-side particle choreography helpers (beams, rings, bolts, shockwaves). */
public final class Fx {
    private Fx() {
    }

    public static ParticleOptions dust(int rgb, float scale) {
        return SiegeEffect.dust(rgb, scale);
    }

    public static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions p, double step) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 1e-3) return;
        int n = Math.min(160, (int) Math.ceil(len / step));
        for (int i = 0; i <= n; i++) {
            Vec3 at = from.add(d.scale(i / (double) n));
            level.sendParticles(p, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    /** A jagged lightning bolt between two points. */
    public static void bolt(ServerLevel level, Vec3 from, Vec3 to, int rgb, float scale) {
        Vfx.arc(level, from, to, rgb, 0.06f * scale, 5, 0.6f);
        Vfx.spray(level, FxStyle.SPARK, to, rgb, 0xFFFFFF, 0.14f * scale, 6, 0.35f, 0.3f);
        Vfx.flash(level, to, rgb, 0.5f * scale, 4);
    }

    public static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions p, int points) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            level.sendParticles(p, center.x + Math.cos(a) * radius, center.y, center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** Expanding shockwave ring with debris. */
    public static void shockwave(ServerLevel level, Vec3 center, double radius, int rgb) {
        Vfx.ring(level, center.add(0, 0.05, 0), rgb, (float) radius, 14, 1.2f);
        Vfx.ring(level, center.add(0, 0.05, 0), 0xFFFFFF, (float) radius * 0.7f, 10, 0.5f);
        Vfx.spray(level, FxStyle.DEBRIS, center.add(0, 0.2, 0), 0x6A5A48, 0x3A3028, 0.22f, (int) (radius * 4) + 6, 0.5f, 0.75f);
        Vfx.spray(level, FxStyle.SMOKE, center.add(0, 0.3, 0), 0xC8C0B8, 0x8A8480, 0.9f, (int) (radius * 3) + 4, 0.25f, -1f);
        Vfx.shake(level, center, (float) Math.min(1.5, radius * 0.18), (float) radius * 4 + 8, 12);
    }

    public static void burst(ServerLevel level, Vec3 c, ParticleOptions p, int count, double spread, double speed) {
        level.sendParticles(p, c.x, c.y, c.z, count, spread, spread, spread, speed);
    }

    /** Pillar of light/energy descending from the sky. */
    public static void skyBeam(ServerLevel level, Vec3 ground, int rgb, double height, double width) {
        Vfx.pillar(level, ground, rgb, (float) width, (float) height, 22);
        Vfx.ring(level, ground.add(0, 0.05, 0), rgb, (float) width * 3, 14, 0.8f);
        Vfx.spray(level, FxStyle.STAR, ground.add(0, 0.5, 0), rgb, 0xFFFFFF, 0.3f, 14, 0.3f, 0.8f);
    }

    public static void explosion(ServerLevel level, Vec3 c, double radius) {
        float r = (float) radius;
        Vfx.flash(level, c, 0xFFB040, 1.2f + r * 0.5f, 6);
        Vfx.sphere(level, c, 0xFF8A20, r * 1.1f, 10);
        Vfx.ring(level, c.add(0, -0.3, 0), 0xFFC080, r * 1.6f, 12, 0.6f);
        Vfx.spray(level, FxStyle.FLAME, c, 0xFFD060, 0xFF4010, 0.5f + r * 0.08f, (int) (r * 8) + 6, 0.25f + r * 0.05f, 0f);
        Vfx.spray(level, FxStyle.SMOKE, c, 0x5A5450, 0x2A2826, 1.0f + r * 0.15f, (int) (r * 6) + 4, 0.12f + r * 0.02f, 0.5f);
        Vfx.spray(level, FxStyle.DEBRIS, c, 0x5A4A3A, 0x2A2420, 0.2f, (int) (r * 4) + 4, 0.45f, 0.7f);
        Vfx.spray(level, FxStyle.SPARK, c, 0xFFE0A0, 0xFF8020, 0.15f, (int) (r * 5) + 4, 0.7f, 0.4f);
        Vfx.shake(level, c, Math.min(2.2f, 0.35f + r * 0.22f), 10 + r * 6, 10 + (int) r * 2);
        sound(level, c, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, 1.2f, 0.9f + level.random.nextFloat() * 0.2f);
    }

    /** Cone of particles (breath / flamethrower) from origin along dir. */
    public static void cone(ServerLevel level, Vec3 origin, Vec3 dir, double length, double halfAngleDeg, ParticleOptions p, int count) {
        RandomSource r = level.random;
        Vec3 n = dir.normalize();
        double spread = Math.tan(Math.toRadians(halfAngleDeg));
        for (int i = 0; i < count; i++) {
            double t = 0.3 + r.nextDouble() * length;
            Vec3 jitter = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).scale(spread * t * 0.45);
            Vec3 at = origin.add(n.scale(t)).add(jitter);
            Vec3 v = n.scale(0.15 + r.nextDouble() * 0.15);
            level.sendParticles(p, at.x, at.y, at.z, 0, v.x, v.y, v.z, 1.0);
        }
    }

    public static void spiral(ServerLevel level, Vec3 c, double radius, double height, ParticleOptions p, int turns, double phase) {
        int n = turns * 16;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double a = phase + t * Math.PI * 2 * turns;
            level.sendParticles(p, c.x + Math.cos(a) * radius, c.y + t * height, c.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    public static void sound(ServerLevel level, Vec3 at, SoundEvent s, float vol, float pitch) {
        level.playSound(null, at.x, at.y, at.z, s, SoundSource.HOSTILE, vol, pitch);
    }

    public static void soundN(ServerLevel level, Vec3 at, SoundEvent s, float vol, float pitch) {
        level.playSound(null, at.x, at.y, at.z, s, SoundSource.NEUTRAL, vol, pitch);
    }

    public static float rnd(RandomSource r, float min, float max) {
        return Mth.lerp(r.nextFloat(), min, max);
    }
}
