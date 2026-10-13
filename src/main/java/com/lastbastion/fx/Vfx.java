package com.lastbastion.fx;

import com.lastbastion.network.Net;
import com.lastbastion.network.VfxPacket;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * Server-side API for rich visual effects. Shapes are drawn by the client renderer
 * ({@code client.fx.VfxManager}); tinted particles use {@link FxParticleOptions}.
 */
public final class Vfx {
    /** Players further than this from an effect never receive it. */
    public static final double RANGE = 112;

    private Vfx() {
    }

    // ------------------------------------------------------------------ transport

    private static void send(ServerLevel level, VfxKind kind, Vec3 a, Vec3 b, int color, int color2, float size, float extra, int life, int e1, int e2) {
        VfxPacket pkt = new VfxPacket(kind, a.x, a.y, a.z, b.x, b.y, b.z, color, color2, size, extra, life, e1, e2);
        Net.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(a.x, a.y, a.z, RANGE, level.dimension())), pkt);
    }

    private static void send(ServerLevel level, VfxKind kind, Vec3 a, Vec3 b, int color, int color2, float size, float extra, int life) {
        send(level, kind, a, b, color, color2, size, extra, life, -1, -1);
    }

    // ------------------------------------------------------------------ shapes

    /** Straight energy beam with a white-hot core. {@code jitter} > 0 makes it wobble. */
    public static void beam(ServerLevel l, Vec3 from, Vec3 to, int color, float width, int life, float jitter) {
        send(l, VfxKind.BEAM, from, to, color, 0xFFFFFF, width, jitter, life);
    }

    public static void beam(ServerLevel l, Vec3 from, Vec3 to, int color, float width, int life) {
        beam(l, from, to, color, width, life, 0);
    }

    /** Jagged branching electric arc, re-randomised every couple of frames. */
    public static void arc(ServerLevel l, Vec3 from, Vec3 to, int color, float thickness, int life, float branchiness) {
        send(l, VfxKind.ARC, from, to, color, 0xFFFFFF, thickness, branchiness, life);
    }

    public static void arc(ServerLevel l, Vec3 from, Vec3 to, int color, int life) {
        arc(l, from, to, color, 0.08f, life, 0.5f);
    }

    /** Lightning strike from the sky: arc + flash + shock ring + shake. */
    public static void lightning(ServerLevel l, Vec3 ground, int color, float power) {
        Vec3 top = ground.add((l.random.nextFloat() - 0.5f) * 4, 24 + l.random.nextInt(8), (l.random.nextFloat() - 0.5f) * 4);
        arc(l, top, ground, color, 0.14f * power, 6, 1.0f);
        flash(l, ground.add(0, 0.5, 0), color, 2.5f * power, 5);
        ring(l, ground.add(0, 0.05, 0), color, 2.5f * power, 10, 0.6f);
        shake(l, ground, 0.6f * power, 18, 8);
    }

    /** Shockwave ring expanding along the ground to {@code radius}. */
    public static void ring(ServerLevel l, Vec3 center, int color, float radius, int life, float height) {
        send(l, VfxKind.RING, center, center, color, 0xFFFFFF, radius, height, life);
    }

    /**
     * Weapon slash crescent swept around {@code center}.
     *
     * @param yaw   facing in degrees (entity yRot)
     * @param tilt  roll of the slash plane in degrees (0 = horizontal sweep, 90 = vertical chop)
     * @param sweep arc length in degrees (negative sweeps the other way)
     */
    public static void slash(ServerLevel l, Vec3 center, float yaw, float tilt, float sweep, float radius, int color, int core, int life, float thickness) {
        send(l, VfxKind.SLASH, center, new Vec3(yaw, tilt, sweep), color, core, radius, thickness, life);
    }

    public static void slash(ServerLevel l, Entity e, float tilt, float sweep, float radius, int color, int life) {
        Vec3 c = e.position().add(0, e.getBbHeight() * 0.55, 0);
        slash(l, c, e.getYRot(), tilt, sweep, radius, color, 0xFFFFFF, life, 0.35f);
    }

    /** Column of light/energy rising from {@code ground}. */
    public static void pillar(ServerLevel l, Vec3 ground, int color, float radius, float height, int life) {
        send(l, VfxKind.PILLAR, ground, ground, color, 0xFFFFFF, radius, height, life);
    }

    /** Hemispherical shield dome; follows {@code follow} if non-null. */
    public static void dome(ServerLevel l, Vec3 center, int color, float radius, int life, Entity follow) {
        send(l, VfxKind.DOME, center, center, color, 0xFFFFFF, radius, 0, life, follow == null ? -1 : follow.getId(), -1);
    }

    /** Expanding spherical shell (explosions, novas). */
    public static void sphere(ServerLevel l, Vec3 center, int color, float radius, int life) {
        send(l, VfxKind.SPHERE, center, center, color, 0xFFFFFF, radius, 0, life);
    }

    /** Star-burst flash. */
    public static void flash(ServerLevel l, Vec3 at, int color, float size, int life) {
        send(l, VfxKind.FLASH, at, at, color, 0xFFFFFF, size, 0, life);
    }

    /** Spinning helical vortex (tornado, maelstrom, summoning). */
    public static void vortex(ServerLevel l, Vec3 base, int color, int color2, float radius, float height, int life, Entity follow) {
        send(l, VfxKind.VORTEX, base, base, color, color2, radius, height, life, follow == null ? -1 : follow.getId(), -1);
    }

    /** Rotating magic circle on the ground. */
    public static void sigil(ServerLevel l, Vec3 center, int color, float radius, int life, float spin) {
        send(l, VfxKind.SIGIL, center, center, color, 0xFFFFFF, radius, spin, life);
    }

    /** Beam that stays attached between two entities. */
    public static void tether(ServerLevel l, Entity a, Entity b, int color, float width, int life) {
        send(l, VfxKind.TETHER, a.position(), b.position(), color, 0xFFFFFF, width, 0, life, a.getId(), b.getId());
    }

    /** Wall of energy/water sweeping from {@code from} to {@code to}. */
    public static void wave(ServerLevel l, Vec3 from, Vec3 to, int color, float width, float height, int life) {
        send(l, VfxKind.WAVE, from, to, color, 0xFFFFFF, width, height, life);
    }

    /** Rotating ground aura that follows an entity (buffs, ultimates, charging). */
    public static void aura(ServerLevel l, Entity e, int color, float radius, int life) {
        send(l, VfxKind.AURA, e.position(), e.position(), color, 0xFFFFFF, radius, 0, life, e.getId(), -1);
    }

    /** Tapered after-image streak (dashes, charges, blinks). */
    public static void dash(ServerLevel l, Vec3 from, Vec3 to, int color, float width, int life) {
        send(l, VfxKind.DASH, from, to, color, 0xFFFFFF, width, 0, life);
    }

    /** Camera shake for players within {@code radius}, fading with distance. */
    public static void shake(ServerLevel l, Vec3 at, float intensity, float radius, int life) {
        send(l, VfxKind.SHAKE, at, at, 0, 0, intensity, radius, life);
    }

    /** Full-screen colour flash for players within {@code radius}. */
    public static void screenFlash(ServerLevel l, Vec3 at, int color, float strength, float radius, int life) {
        send(l, VfxKind.SCREEN_FLASH, at, at, color, 0, strength, radius, life);
    }

    // ------------------------------------------------------------------ telegraphs (draw + register danger)

    public static final int DANGER = 0xFF3020;
    public static final int DANGER_DARK = 0xB0109A;
    public static final int DANGER_HOLY = 0xFFD040;

    /** Circular danger zone that fills up until the hit lands. Heroes see it and step out. */
    public static void teleCircle(ServerLevel l, Vec3 center, float radius, int ticks, int color, boolean hostile) {
        send(l, VfxKind.TELE_CIRCLE, center, center, color, 0, radius, 0, ticks);
        DangerZones.circle(l, center, radius, ticks, hostile);
    }

    /** Rectangular lane from {@code from} to {@code to}. */
    public static void teleLine(ServerLevel l, Vec3 from, Vec3 to, float width, int ticks, int color, boolean hostile) {
        send(l, VfxKind.TELE_LINE, from, to, color, 0, width, 0, ticks);
        DangerZones.line(l, from, to, width, ticks, hostile);
    }

    /** Cone from {@code origin} toward {@code toward}. */
    public static void teleCone(ServerLevel l, Vec3 origin, Vec3 toward, float length, float halfAngleDeg, int ticks, int color, boolean hostile) {
        Vec3 dir = toward.subtract(origin);
        dir = new Vec3(dir.x, 0, dir.z);
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);
        Vec3 tip = origin.add(dir.normalize().scale(length));
        send(l, VfxKind.TELE_CONE, origin, tip, color, 0, length, halfAngleDeg, ticks);
        DangerZones.cone(l, origin, dir, length, halfAngleDeg, ticks, hostile);
    }

    /** Ring-shaped zone: safe inside {@code inner}, dangerous out to {@code outer}. */
    public static void teleDonut(ServerLevel l, Vec3 center, float inner, float outer, int ticks, int color, boolean hostile) {
        send(l, VfxKind.TELE_DONUT, center, center, color, 0, outer, inner, ticks);
        DangerZones.donut(l, center, inner, outer, ticks, hostile);
    }

    // ------------------------------------------------------------------ particles

    /** Sends particles to every player within {@link #RANGE} (vanilla only reaches 32 blocks). */
    public static void particles(ServerLevel l, ParticleOptions p, Vec3 at, int count, double dx, double dy, double dz, double speed) {
        for (ServerPlayer sp : l.players()) {
            if (sp.distanceToSqr(at) < RANGE * RANGE) l.sendParticles(sp, p, true, at.x, at.y, at.z, count, dx, dy, dz, speed);
        }
    }

    /** A single particle with an exact velocity. */
    public static void particle(ServerLevel l, ParticleOptions p, Vec3 at, Vec3 vel) {
        particles(l, p, at, 0, vel.x, vel.y, vel.z, 1);
    }

    public static void burst(ServerLevel l, FxStyle style, int color, float size, Vec3 at, int count, double spread, double speed) {
        particles(l, FxParticleOptions.of(style, color, size), at, count, spread, spread, spread, speed);
    }

    /** Radial burst: particles fly outward in all directions at {@code speed}. */
    public static void radial(ServerLevel l, FxStyle style, int color, float size, Vec3 at, int count, double speed, boolean flat) {
        RandomSource r = l.random;
        FxParticleOptions o = FxParticleOptions.of(style, color, size);
        for (int i = 0; i < count; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double up = flat ? 0.05 + r.nextDouble() * 0.15 : r.nextDouble() * 2 - 1;
            double h = Math.sqrt(Math.max(0, 1 - up * up));
            Vec3 v = new Vec3(Math.cos(a) * h, up, Math.sin(a) * h).scale(speed * (0.6 + r.nextDouble() * 0.6));
            particle(l, o, at, v);
        }
    }

    /** Particles streaming along a line (trails, rays). */
    public static void stream(ServerLevel l, FxStyle style, int color, float size, Vec3 from, Vec3 to, double step) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 1e-3) return;
        int n = Math.min(120, (int) Math.ceil(len / step));
        FxParticleOptions o = FxParticleOptions.of(style, color, size);
        for (int i = 0; i <= n; i++) {
            Vec3 at = from.add(d.scale(i / (double) n));
            particles(l, o, at, 1, 0.03, 0.03, 0.03, 0.0);
        }
    }

    /** Ring of particles on the ground moving outward. */
    public static void groundRing(ServerLevel l, FxStyle style, int color, float size, Vec3 c, double radius, int points, double outSpeed) {
        FxParticleOptions o = FxParticleOptions.of(style, color, size);
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
            particle(l, o, c.add(dir.scale(radius)).add(0, 0.1, 0), dir.scale(outSpeed).add(0, 0.02, 0));
        }
    }

    public static int mix(int a, int b, float t) {
        t = Mth.clamp(t, 0, 1);
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return (r << 16) | (g << 8) | bl;
    }
}
