package com.lastbastion.fx;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Telegraphed areas that are about to be hit. Every boss / elite AoE registers one when it starts its
 * wind-up, so hero AI (and smart players watching the ground) can get out in time.
 */
public final class DangerZones {
    public enum Shape {CIRCLE, LINE, CONE, DONUT}

    public record Zone(Shape shape, Vec3 a, Vec3 b, double r1, double r2, long expires, boolean hostile) {
        /** True if {@code p} (feet position) is inside the zone, with an extra margin. */
        public boolean contains(Vec3 p, double margin) {
            double dx, dz;
            switch (shape) {
                case CIRCLE -> {
                    dx = p.x - a.x;
                    dz = p.z - a.z;
                    return dx * dx + dz * dz < (r1 + margin) * (r1 + margin) && Math.abs(p.y - a.y) < 6;
                }
                case DONUT -> {
                    dx = p.x - a.x;
                    dz = p.z - a.z;
                    double d2 = dx * dx + dz * dz;
                    return d2 < (r1 + margin) * (r1 + margin) && d2 > Math.max(0, r2 - margin) * Math.max(0, r2 - margin) && Math.abs(p.y - a.y) < 6;
                }
                case LINE -> {
                    double lx = b.x - a.x, lz = b.z - a.z;
                    double len2 = lx * lx + lz * lz;
                    double t = len2 < 1e-6 ? 0 : Mth.clamp(((p.x - a.x) * lx + (p.z - a.z) * lz) / len2, 0, 1);
                    dx = p.x - (a.x + lx * t);
                    dz = p.z - (a.z + lz * t);
                    return dx * dx + dz * dz < (r1 / 2 + margin) * (r1 / 2 + margin) && Math.abs(p.y - a.y) < 6;
                }
                case CONE -> {
                    dx = p.x - a.x;
                    dz = p.z - a.z;
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > r1 + margin) return false;
                    if (d < 1.0 + margin) return true;
                    double dot = (dx * b.x + dz * b.z) / d;
                    return dot > Math.cos(Math.toRadians(r2)) - margin * 0.05 && Math.abs(p.y - a.y) < 6;
                }
            }
            return false;
        }

        /** A horizontal direction that leaves the zone fastest from {@code p}. */
        public Vec3 escape(Vec3 p) {
            Vec3 out;
            switch (shape) {
                case LINE -> {
                    Vec3 dir = b.subtract(a);
                    out = new Vec3(-dir.z, 0, dir.x);
                    Vec3 rel = p.subtract(a);
                    if (out.x * rel.x + out.z * rel.z < 0) out = out.scale(-1);
                }
                case DONUT -> {
                    // closer to the safe centre or the outside?
                    double d = Math.sqrt((p.x - a.x) * (p.x - a.x) + (p.z - a.z) * (p.z - a.z));
                    out = new Vec3(p.x - a.x, 0, p.z - a.z);
                    if (d - r2 < r1 - d) out = out.scale(-1);
                }
                case CONE -> {
                    Vec3 rel = new Vec3(p.x - a.x, 0, p.z - a.z);
                    out = new Vec3(-b.z, 0, b.x);
                    if (out.x * rel.x + out.z * rel.z < 0) out = out.scale(-1);
                }
                default -> out = new Vec3(p.x - a.x, 0, p.z - a.z);
            }
            if (out.lengthSqr() < 1e-4) out = new Vec3(1, 0, 0);
            return out.normalize();
        }
    }

    private static final Map<ResourceKey<Level>, List<Zone>> ZONES = new HashMap<>();

    private DangerZones() {
    }

    private static void add(ServerLevel l, Zone z) {
        List<Zone> list = ZONES.computeIfAbsent(l.dimension(), k -> new ArrayList<>());
        long now = l.getGameTime();
        list.removeIf(o -> o.expires < now);
        if (list.size() < 256) list.add(z);
    }

    public static void circle(ServerLevel l, Vec3 c, double radius, int ticks, boolean hostile) {
        add(l, new Zone(Shape.CIRCLE, c, c, radius, 0, l.getGameTime() + ticks + 2, hostile));
    }

    public static void line(ServerLevel l, Vec3 from, Vec3 to, double width, int ticks, boolean hostile) {
        add(l, new Zone(Shape.LINE, from, to, width, 0, l.getGameTime() + ticks + 2, hostile));
    }

    public static void cone(ServerLevel l, Vec3 origin, Vec3 dir, double length, double halfAngle, int ticks, boolean hostile) {
        Vec3 n = new Vec3(dir.x, 0, dir.z).normalize();
        add(l, new Zone(Shape.CONE, origin, n, length, halfAngle, l.getGameTime() + ticks + 2, hostile));
    }

    public static void donut(ServerLevel l, Vec3 c, double inner, double outer, int ticks, boolean hostile) {
        add(l, new Zone(Shape.DONUT, c, c, outer, inner, l.getGameTime() + ticks + 2, hostile));
    }

    /** The first active zone (of the given allegiance) containing {@code p}, or null. */
    public static Zone threatAt(ServerLevel l, Vec3 p, boolean hostileOnly, double margin) {
        List<Zone> list = ZONES.get(l.dimension());
        if (list == null || list.isEmpty()) return null;
        long now = l.getGameTime();
        Iterator<Zone> it = list.iterator();
        while (it.hasNext()) {
            Zone z = it.next();
            if (z.expires < now) {
                it.remove();
                continue;
            }
            if (z.hostile == hostileOnly && z.contains(p, margin)) return z;
        }
        return null;
    }

    public static void clear() {
        ZONES.clear();
    }
}
