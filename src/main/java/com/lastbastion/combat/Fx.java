package com.lastbastion.combat;

import com.lastbastion.effect.SiegeEffect;
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
        RandomSource r = level.random;
        Vec3 d = to.subtract(from);
        double len = d.length();
        int segs = Math.max(3, (int) (len / 1.2));
        Vec3 prev = from;
        ParticleOptions core = dust(rgb, scale);
        for (int i = 1; i <= segs; i++) {
            Vec3 next = from.add(d.scale(i / (double) segs));
            if (i < segs) {
                double j = Math.min(0.9, len * 0.06);
                next = next.add((r.nextDouble() - 0.5) * j * 2, (r.nextDouble() - 0.5) * j * 2, (r.nextDouble() - 0.5) * j * 2);
            }
            line(level, prev, next, core, 0.18);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, next.x, next.y, next.z, 2, 0.05, 0.05, 0.05, 0.1);
            prev = next;
        }
    }

    public static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions p, int points) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            level.sendParticles(p, center.x + Math.cos(a) * radius, center.y, center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** Expanding shockwave ring with debris. */
    public static void shockwave(ServerLevel level, Vec3 center, double radius, int rgb) {
        for (double r = 0.5; r <= radius; r += 0.8) {
            ring(level, center.add(0, 0.15, 0), r, dust(rgb, 1.6f), (int) (r * 7) + 6);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.3, center.z, 2, radius * 0.3, 0.1, radius * 0.3, 0);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.2, center.z, (int) (radius * 6), radius * 0.5, 0.1, radius * 0.5, 0.05);
    }

    public static void burst(ServerLevel level, Vec3 c, ParticleOptions p, int count, double spread, double speed) {
        level.sendParticles(p, c.x, c.y, c.z, count, spread, spread, spread, speed);
    }

    /** Pillar of light/energy descending from the sky. */
    public static void skyBeam(ServerLevel level, Vec3 ground, int rgb, double height, double width) {
        ParticleOptions p = dust(rgb, 2.2f);
        for (double y = 0; y < height; y += 0.5) {
            level.sendParticles(p, ground.x, ground.y + y, ground.z, 2, width * 0.3, 0.1, width * 0.3, 0);
        }
        ring(level, ground.add(0, 0.1, 0), width, ParticleTypes.END_ROD, 24);
        level.sendParticles(ParticleTypes.FLASH, ground.x, ground.y + 1, ground.z, 1, 0, 0, 0, 0);
    }

    public static void explosion(ServerLevel level, Vec3 c, double radius) {
        level.sendParticles(radius > 3 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, (int) (radius * 8), radius * 0.4, radius * 0.3, radius * 0.4, 0.03);
        level.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, (int) (radius * 10), radius * 0.4, radius * 0.3, radius * 0.4, 0.08);
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
