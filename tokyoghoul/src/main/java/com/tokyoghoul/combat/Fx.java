package com.tokyoghoul.combat;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Server-side particle and sound helpers for move effects (sent to every nearby client). */
public final class Fx {
    private Fx() {
    }

    public static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), size);
    }

    public static void particles(Level level, ParticleOptions p, Vec3 at, int count, double spread, double speed) {
        if (level instanceof ServerLevel sl) sl.sendParticles(p, at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    public static void particles(Level level, ParticleOptions p, Vec3 at, int count, double sx, double sy, double sz, double speed) {
        if (level instanceof ServerLevel sl) sl.sendParticles(p, at.x, at.y, at.z, count, sx, sy, sz, speed);
    }

    public static void sound(Level level, Vec3 at, SoundEvent s, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, volume, pitch);
    }

    /** Impact burst: sparks, a puff of blood and a dust burst in the attack's colour. */
    public static void hit(Level level, Vec3 at, int color, boolean heavy) {
        particles(level, ParticleTypes.CRIT, at, heavy ? 14 : 6, 0.25, 0.4);
        particles(level, dust(color, heavy ? 1.6f : 1.1f), at, heavy ? 12 : 6, 0.3, 0.05);
        particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()), at, heavy ? 10 : 4, 0.2, 0.1);
        if (heavy) particles(level, ParticleTypes.SWEEP_ATTACK, at, 1, 0, 0);
    }

    /** A line of coloured dust (kagune thrusts, lance stabs, dash trails). */
    public static void line(Level level, Vec3 from, Vec3 to, int color, float size, double step) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 1e-3) return;
        Vec3 n = d.scale(1 / len);
        DustParticleOptions dust = dust(color, size);
        for (double t = 0; t <= len; t += step) {
            Vec3 p = from.add(n.scale(t));
            particles(level, dust, p, 1, 0.04, 0);
        }
    }

    /** Expanding ring on the ground (slams, kakuja roars). */
    public static void ring(Level level, Vec3 center, double radius, int color) {
        DustParticleOptions dust = dust(color, 1.8f);
        int n = (int) Math.max(12, radius * 10);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            Vec3 p = center.add(Math.cos(a) * radius, 0.15, Math.sin(a) * radius);
            particles(level, dust, p, 1, 0.05, 0);
            if (i % 3 == 0) particles(level, ParticleTypes.CLOUD, p, 1, 0.05, 0.02);
        }
    }

    /** Ground debris where a slam lands. */
    public static void debris(Level level, Entity at, int count) {
        var state = level.getBlockState(at.blockPosition().below());
        if (state.isAir()) state = Blocks.STONE.defaultBlockState();
        particles(level, new BlockParticleOption(ParticleTypes.BLOCK, state), at.position(), count, 1.2, 0.2, 1.2, 0.3);
    }

    /** Wispy kagune bloom when a kagune is released. */
    public static void kaguneBurst(Level level, Vec3 at, int color) {
        particles(level, dust(color, 2.0f), at, 30, 0.6, 0.1);
        particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()), at, 10, 0.4, 0.1);
    }
}
