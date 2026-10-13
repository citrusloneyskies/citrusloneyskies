package com.lastbastion.client.fx;

import com.lastbastion.config.ClientConfig;
import com.lastbastion.fx.FxParticleOptions;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.network.VfxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Client-side particle emitters driven by a single {@link VfxPacket}. */
final class Emitters {
    private static final RandomSource R = RandomSource.create();

    private Emitters() {
    }

    private static int mix(int a, int b, float t) {
        t = Mth.clamp(t, 0, 1);
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return (r << 16) | (g << 8) | bl;
    }

    private static int count(int n) {
        return Math.max(1, Math.round(n * ClientConfig.vfxDensity()));
    }

    static void jet(VfxPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        FxStyle style = FxStyle.byId(p.ent1());
        Vec3 from = new Vec3(p.x(), p.y(), p.z()), to = new Vec3(p.x2(), p.y2(), p.z2());
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 1e-3) return;
        Vec3 n = d.scale(1 / len);
        Vec3 u = n.cross(new Vec3(0, 1, 0));
        if (u.lengthSqr() < 1e-4) u = new Vec3(1, 0, 0);
        u = u.normalize();
        Vec3 v = n.cross(u).normalize();
        double spread = Math.tan(Math.toRadians(p.extra()));
        int c = count(p.life());
        for (int i = 0; i < c; i++) {
            double speed = len / (10 + R.nextInt(8));
            double a = R.nextDouble() * Math.PI * 2, r = Math.sqrt(R.nextDouble()) * spread;
            Vec3 dir = n.add(u.scale(Math.cos(a) * r)).add(v.scale(Math.sin(a) * r)).normalize();
            Vec3 start = from.add(n.scale(R.nextDouble() * 0.4));
            int col = mix(p.color(), p.color2(), R.nextFloat());
            int life = (int) Math.max(4, len / speed * (0.75 + R.nextDouble() * 0.35));
            mc.particleEngine.createParticle(FxParticleOptions.of(style, col, p.size() * (0.7f + R.nextFloat() * 0.6f), life),
                    start.x, start.y, start.z, dir.x * speed, dir.y * speed, dir.z * speed);
        }
    }

    static void spray(VfxPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        FxStyle style = FxStyle.byId(p.ent1());
        double speed = p.x2(), up = p.y2();
        int c = count(p.life());
        for (int i = 0; i < c; i++) {
            double a = R.nextDouble() * Math.PI * 2;
            double y;
            if (up < 0) y = 0.05 + R.nextDouble() * 0.15;
            else y = Mth.lerp(up, R.nextDouble() * 2 - 1, 0.4 + R.nextDouble() * 0.6);
            double h = Math.sqrt(Math.max(0, 1 - y * y));
            double s = speed * (0.5 + R.nextDouble() * 0.7);
            int col = mix(p.color(), p.color2(), R.nextFloat());
            mc.particleEngine.createParticle(FxParticleOptions.of(style, col, p.size() * (0.7f + R.nextFloat() * 0.6f), 0),
                    p.x(), p.y(), p.z(), Math.cos(a) * h * s, y * s, Math.sin(a) * h * s);
        }
    }
}
