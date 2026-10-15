package com.lastbastion.client.fx;

import com.lastbastion.fx.VfxKind;
import com.lastbastion.network.VfxPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side renderer for {@link VfxPacket} shapes: energy beams, branching arcs, slash crescents,
 * shock rings, light pillars, shield domes, vortices, magic circles and ground telegraphs.
 * Everything is procedural geometry drawn in world space with additive or alpha blending.
 */
public final class VfxManager {
    private static final class Effect {
        final VfxPacket p;
        final long seed;
        int age;

        Effect(VfxPacket p, long seed) {
            this.p = p;
            this.seed = seed;
        }
    }

    private static final List<Effect> ACTIVE = new ArrayList<>();
    private static final RandomSource RNG = RandomSource.create();
    private static long seeds = 1;
    private static int clientTicks;

    private VfxManager() {
    }

    public static void add(VfxPacket p) {
        switch (p.kind()) {
            case SHAKE -> CameraShake.add(new Vec3(p.x(), p.y(), p.z()), p.size(), p.extra(), p.life());
            case SCREEN_FLASH -> ScreenFlash.add(new Vec3(p.x(), p.y(), p.z()), p.color(), p.size(), p.extra(), p.life());
            case JET -> Emitters.jet(p);
            case SPRAY -> Emitters.spray(p);
            default -> {
                if (ACTIVE.size() > 900) ACTIVE.remove(0);
                ACTIVE.add(new Effect(p, seeds++ * 0x9E3779B97F4A7C15L));
            }
        }
    }

    public static void tick() {
        clientTicks++;
        CameraShake.tick();
        ScreenFlash.tick();
        ACTIVE.removeIf(e -> ++e.age > e.p.life());
    }

    public static void clear() {
        ACTIVE.clear();
    }

    // ------------------------------------------------------------------ rendering

    public static void render(RenderLevelStageEvent e) {
        boolean dome = coreDomeVisible();
        if (ACTIVE.isEmpty() && !dome) return;
        Minecraft mc = Minecraft.getInstance();
        Camera cam = e.getCamera();
        Vec3 cp = cam.getPosition();
        PoseStack pose = e.getPoseStack();
        float pt = e.getPartialTick();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        Matrix4f m = pose.last().pose();
        // telegraphs first (alpha blended), then additive glow on top
        Draw tele = new Draw(buf.getBuffer(VfxRenderTypes.TRANSLUCENT), m, cp, cam);
        for (Effect fx : ACTIVE) {
            switch (fx.p.kind()) {
                case TELE_CIRCLE, TELE_LINE, TELE_CONE, TELE_DONUT -> telegraph(tele, fx, pt);
                default -> {
                }
            }
        }
        buf.endBatch(VfxRenderTypes.TRANSLUCENT);
        // dark shapes (black fire, void, shadow) are alpha blended so they can darken the world
        boolean anyDark = false;
        for (Effect fx : ACTIVE) {
            if (dark(fx)) {
                anyDark = true;
                break;
            }
        }
        if (anyDark) {
            Draw dk = new Draw(buf.getBuffer(VfxRenderTypes.TRANSLUCENT), m, cp, cam);
            for (Effect fx : ACTIVE) if (dark(fx)) shape(dk, fx, pt);
            buf.endBatch(VfxRenderTypes.TRANSLUCENT);
        }
        Draw d = new Draw(buf.getBuffer(VfxRenderTypes.ADDITIVE), m, cp, cam);
        for (Effect fx : ACTIVE) if (!dark(fx)) shape(d, fx, pt);
        if (dome) coreDome(d, pt);
        buf.endBatch(VfxRenderTypes.ADDITIVE);
    }

    /** Effects flagged with {@code Vfx.DARK} render alpha-blended (dark colours stay dark). */
    static boolean dark(Effect fx) {
        return (fx.p.color() & 0x1000000) != 0;
    }

    /** The hot core of a shape: white for glows, the given core colour for dark shapes. */
    static int hot(Effect fx) {
        return dark(fx) ? fx.p.color2() & 0xFFFFFF : 0xFFFFFF;
    }

    private static void shape(Draw d, Effect fx, float pt) {
        switch (fx.p.kind()) {
            case BEAM -> beam(d, fx, pt);
            case ARC -> arc(d, fx, pt);
            case RING -> ring(d, fx, pt);
            case SLASH -> slash(d, fx, pt);
            case PILLAR -> pillar(d, fx, pt);
            case DOME -> dome(d, fx, pt);
            case SPHERE -> sphere(d, fx, pt);
            case FLASH -> flash(d, fx, pt);
            case VORTEX -> vortex(d, fx, pt);
            case SIGIL -> sigil(d, fx, pt);
            case TETHER -> tether(d, fx, pt);
            case WAVE -> wave(d, fx, pt);
            case AURA -> aura(d, fx, pt);
            case DASH -> dash(d, fx, pt);
            case TELE_CIRCLE, TELE_LINE, TELE_CONE, TELE_DONUT -> teleGlow(d, fx, pt);
            default -> {
            }
        }
    }

    /** Small immediate-mode helper: world positions in, camera-relative coloured quads out. */
    static final class Draw {
        final VertexConsumer vc;
        final Matrix4f m;
        final Vec3 cam;
        final Vector3f left, up;

        Draw(VertexConsumer vc, Matrix4f m, Vec3 cam, Camera c) {
            this.vc = vc;
            this.m = m;
            this.cam = cam;
            this.left = new Vector3f(c.getLeftVector());
            this.up = new Vector3f(c.getUpVector());
        }

        Vector3f rel(Vec3 p) {
            return new Vector3f((float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z));
        }

        void v(Vector3f p, int rgb, float a) {
            a = Mth.clamp(a, 0, 1);
            vc.vertex(m, p.x, p.y, p.z).color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, (int) (a * 255)).endVertex();
        }

        void quad(Vector3f a, Vector3f b, Vector3f c, Vector3f d, int rgb, float aa, float ab, float ac, float ad) {
            v(a, rgb, aa);
            v(b, rgb, ab);
            v(c, rgb, ac);
            v(d, rgb, ad);
        }

        void quad(Vector3f a, Vector3f b, Vector3f c, Vector3f d, int rgb, float alpha) {
            quad(a, b, c, d, rgb, alpha, alpha, alpha, alpha);
        }

        /** Camera-facing ribbon between two camera-relative points. */
        void ribbon(Vector3f a, Vector3f b, float wa, float wb, int rgb, float alphaA, float alphaB) {
            Vector3f dir = new Vector3f(b).sub(a);
            Vector3f mid = new Vector3f(a).add(b).mul(0.5f);
            Vector3f side = dir.cross(mid.negate(), new Vector3f());
            if (side.lengthSquared() < 1e-8f) return;
            side.normalize();
            Vector3f sa = new Vector3f(side).mul(wa * 0.5f), sb = new Vector3f(side).mul(wb * 0.5f);
            quad(new Vector3f(a).sub(sa), new Vector3f(a).add(sa), new Vector3f(b).add(sb), new Vector3f(b).sub(sb), rgb, alphaA, alphaA, alphaB, alphaB);
        }

        /** Billboard quad facing the camera, rotated by {@code roll}. */
        void billboard(Vector3f c, float w, float h, float roll, int rgb, float alpha) {
            float cs = Mth.cos(roll), sn = Mth.sin(roll);
            Vector3f r = new Vector3f(left).mul(cs).add(new Vector3f(up).mul(sn));
            Vector3f u = new Vector3f(up).mul(cs).sub(new Vector3f(left).mul(sn));
            r.mul(w * 0.5f);
            u.mul(h * 0.5f);
            quad(new Vector3f(c).sub(r).sub(u), new Vector3f(c).add(r).sub(u), new Vector3f(c).add(r).add(u), new Vector3f(c).sub(r).add(u), rgb, alpha);
        }

        /** Soft round glow made of a centre-bright fan (alpha falls to zero at the rim). */
        void glowDisc(Vector3f c, float radius, int rgb, float alpha) {
            int seg = 12;
            for (int i = 0; i < seg; i++) {
                float a0 = Mth.TWO_PI * i / seg, a1 = Mth.TWO_PI * (i + 1) / seg;
                Vector3f p0 = new Vector3f(c).add(new Vector3f(left).mul(Mth.cos(a0) * radius)).add(new Vector3f(up).mul(Mth.sin(a0) * radius));
                Vector3f p1 = new Vector3f(c).add(new Vector3f(left).mul(Mth.cos(a1) * radius)).add(new Vector3f(up).mul(Mth.sin(a1) * radius));
                quad(c, c, p1, p0, rgb, alpha, alpha, 0, 0);
            }
        }

        /** Flat annulus in the XZ plane (world y), with separate inner/outer alpha. */
        void annulus(Vec3 center, float rIn, float rOut, float y, int rgb, float aIn, float aOut, float a0, float a1, int seg) {
            Vector3f c = rel(center);
            for (int i = 0; i < seg; i++) {
                float t0 = a0 + (a1 - a0) * i / seg, t1 = a0 + (a1 - a0) * (i + 1) / seg;
                float c0 = Mth.cos(t0), s0 = Mth.sin(t0), c1 = Mth.cos(t1), s1 = Mth.sin(t1);
                quad(new Vector3f(c.x + c0 * rIn, c.y + y, c.z + s0 * rIn), new Vector3f(c.x + c0 * rOut, c.y + y, c.z + s0 * rOut),
                        new Vector3f(c.x + c1 * rOut, c.y + y, c.z + s1 * rOut), new Vector3f(c.x + c1 * rIn, c.y + y, c.z + s1 * rIn),
                        rgb, aIn, aOut, aOut, aIn);
            }
        }

        /** Vertical cylindrical band (wall) with alpha fading from bottom to top. */
        void band(Vec3 center, float radius, float y0, float y1, int rgb, float aBottom, float aTop, int seg) {
            Vector3f c = rel(center);
            for (int i = 0; i < seg; i++) {
                float t0 = Mth.TWO_PI * i / seg, t1 = Mth.TWO_PI * (i + 1) / seg;
                float c0 = Mth.cos(t0) * radius, s0 = Mth.sin(t0) * radius, c1 = Mth.cos(t1) * radius, s1 = Mth.sin(t1) * radius;
                quad(new Vector3f(c.x + c0, c.y + y0, c.z + s0), new Vector3f(c.x + c1, c.y + y0, c.z + s1),
                        new Vector3f(c.x + c1, c.y + y1, c.z + s1), new Vector3f(c.x + c0, c.y + y1, c.z + s0), rgb, aBottom, aBottom, aTop, aTop);
            }
        }
    }

    private static float t(Effect fx, float pt) {
        return Mth.clamp((fx.age + pt) / Math.max(1, fx.p.life()), 0, 1);
    }

    private static float easeOut(float t) {
        float k = 1 - t;
        return 1 - k * k * k;
    }

    private static Vec3 a(Effect fx) {
        return new Vec3(fx.p.x(), fx.p.y(), fx.p.z());
    }

    private static Vec3 b(Effect fx) {
        return new Vec3(fx.p.x2(), fx.p.y2(), fx.p.z2());
    }

    private static Vec3 entityPos(int id, float pt, Vec3 fallback, boolean center) {
        if (id < 0 || Minecraft.getInstance().level == null) return fallback;
        Entity e = Minecraft.getInstance().level.getEntity(id);
        if (e == null || !e.isAlive()) return fallback;
        Vec3 p = e.getPosition(pt);
        return center ? p.add(0, e.getBbHeight() * 0.55, 0) : p;
    }

    private static int mix(int a, int b, float t) {
        t = Mth.clamp(t, 0, 1);
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return (r << 16) | (g << 8) | bl;
    }

    // ------------------------------------------------------------------ shapes

    private static void beam(Draw d, Effect fx, float pt) {
        float t = t(fx, pt);
        float fade = 1 - t * t;
        float time = clientTicks + pt;
        float w = fx.p.size() * (1 + 0.12f * Mth.sin(time * 1.3f)) * (t < 0.1f ? 0.4f + t * 6 : 1);
        Vector3f from = d.rel(a(fx)), to = d.rel(b(fx));
        int segs = fx.p.extra() > 0 ? 10 : 1;
        Vector3f prev = from;
        Vector3f axis = new Vector3f(to).sub(from);
        Vector3f perp = axis.cross(new Vector3f(0, 1, 0), new Vector3f());
        if (perp.lengthSquared() < 1e-6f) perp.set(1, 0, 0);
        perp.normalize();
        for (int i = 1; i <= segs; i++) {
            float s = i / (float) segs;
            Vector3f next = new Vector3f(from).lerp(to, s);
            if (i < segs && fx.p.extra() > 0) {
                float wob = Mth.sin(s * 9 + time * 0.8f) * fx.p.extra() * Mth.sin(s * Mth.PI);
                next.add(new Vector3f(perp).mul(wob)).add(0, Mth.cos(s * 7 + time * 0.6f) * fx.p.extra() * 0.5f * Mth.sin(s * Mth.PI), 0);
            }
            d.ribbon(prev, next, w * 2.4f, w * 2.4f, fx.p.color(), 0.22f * fade, 0.22f * fade);
            d.ribbon(prev, next, w, w, fx.p.color(), 0.75f * fade, 0.75f * fade);
            d.ribbon(prev, next, w * 0.35f, w * 0.35f, fx.p.color2(), fade, fade);
            prev = next;
        }
        d.glowDisc(from, w * 2.2f, fx.p.color(), 0.8f * fade);
        d.glowDisc(to, w * 3f, fx.p.color(), 0.9f * fade);
        d.glowDisc(to, w * 1.2f, hot(fx), fade);
    }

    private static void arc(Draw d, Effect fx, float pt) {
        float t = t(fx, pt);
        float fade = (1 - t) * (((fx.age + clientTicks) & 1) == 0 ? 1f : 0.65f);
        RNG.setSeed(fx.seed + (fx.age / 2) * 31L);
        Vec3 from = a(fx), to = b(fx);
        float thick = fx.p.size();
        drawBolt(d, from, to, thick, fx.p.color(), hot(fx), fade, RNG, fx.p.extra(), 0);
    }

    private static void drawBolt(Draw d, Vec3 from, Vec3 to, float thick, int color, int core, float fade, RandomSource r, float branch, int depth) {
        Vec3 dv = to.subtract(from);
        double len = dv.length();
        if (len < 0.05) return;
        int n = Math.max(3, Math.min(24, (int) (len / 0.7)));
        Vec3 dir = dv.normalize();
        Vec3 p1 = dir.cross(new Vec3(0, 1, 0));
        if (p1.lengthSqr() < 1e-4) p1 = new Vec3(1, 0, 0);
        p1 = p1.normalize();
        Vec3 p2 = dir.cross(p1).normalize();
        double amp = Math.min(1.6, len * 0.09);
        Vec3 prev = from;
        for (int i = 1; i <= n; i++) {
            double s = i / (double) n;
            Vec3 next = from.add(dv.scale(s));
            if (i < n) next = next.add(p1.scale((r.nextDouble() - 0.5) * 2 * amp)).add(p2.scale((r.nextDouble() - 0.5) * 2 * amp));
            Vector3f a = d.rel(prev), b = d.rel(next);
            d.ribbon(a, b, thick * 5, thick * 5, color, 0.18f * fade, 0.18f * fade);
            d.ribbon(a, b, thick * 2, thick * 2, color, 0.7f * fade, 0.7f * fade);
            d.ribbon(a, b, thick * 0.8f, thick * 0.8f, core, fade, fade);
            if (depth < 2 && i < n - 1 && r.nextFloat() < branch * 0.22f) {
                Vec3 bdir = dir.add(p1.scale((r.nextDouble() - 0.5) * 2.2)).add(p2.scale((r.nextDouble() - 0.5) * 2.2)).normalize();
                drawBolt(d, next, next.add(bdir.scale(len * (0.15 + r.nextDouble() * 0.2))), thick * 0.6f, color, core, fade * 0.8f, r, branch * 0.5f, depth + 1);
            }
            prev = next;
        }
    }

    private static void ring(Draw d, Effect fx, float pt) {
        float t = t(fx, pt);
        float r = Math.max(0.05f, fx.p.size() * easeOut(t));
        float a = (1 - t);
        a *= a;
        int seg = Mth.clamp((int) (r * 8), 16, 72);
        float h = Math.max(0.05f, fx.p.extra()) * (1 - t * 0.6f);
        Vec3 c = a(fx);
        d.band(c, r, 0.02f, h, fx.p.color(), 0.85f * a, 0f, seg);
        d.band(c, r * 0.97f, 0.02f, h * 0.45f, fx.p.color2(), 0.6f * a, 0f, seg);
        d.annulus(c, r * 0.7f, r, 0.04f, fx.p.color(), 0f, 0.55f * a, 0, Mth.TWO_PI, seg);
    }

    private static void slash(Draw d, Effect fx, float pt) {
        float t = t(fx, pt);
        float yaw = (float) fx.p.x2() * Mth.DEG_TO_RAD, tilt = (float) fx.p.y2() * Mth.DEG_TO_RAD, sweep = (float) fx.p.z2() * Mth.DEG_TO_RAD;
        float radius = fx.p.size();
        float thick = Mth.clamp(fx.p.extra(), 0.05f, 0.9f);
        Vector3f f = new Vector3f(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vector3f s0 = new Vector3f(Mth.cos(yaw), 0, Mth.sin(yaw));
        Vector3f s = new Vector3f(s0).mul(Mth.cos(tilt)).add(0, Mth.sin(tilt), 0);
        float swing = Math.min(1f, t / 0.4f);
        swing = 1 - (1 - swing) * (1 - swing);
        float fade = t < 0.4f ? 1f : 1f - (t - 0.4f) / 0.6f;
        float start = -sweep / 2f;
        float head = start + sweep * swing;
        float tail = start + (head - start) * Mth.clamp((t - 0.25f) * 1.1f, 0, 1);
        Vector3f c = d.rel(a(fx));
        int seg = 22;
        for (int i = 0; i < seg; i++) {
            float u0 = i / (float) seg, u1 = (i + 1) / (float) seg;
            float a0 = Mth.lerp(u0, tail, head), a1 = Mth.lerp(u1, tail, head);
            float w0 = thick * Mth.sin(u0 * Mth.PI * 0.5f + 0.0001f), w1 = thick * Mth.sin(u1 * Mth.PI * 0.5f);
            Vector3f o0 = point(c, f, s, a0, radius), o1 = point(c, f, s, a1, radius);
            Vector3f i0 = point(c, f, s, a0, radius * (1 - w0)), i1 = point(c, f, s, a1, radius * (1 - w1));
            float al0 = fade * (0.15f + 0.85f * u0), al1 = fade * (0.15f + 0.85f * u1);
            d.quad(i0, o0, o1, i1, fx.p.color(), 0, al0 * 0.9f, al1 * 0.9f, 0);
            Vector3f e0 = point(c, f, s, a0, radius * (1 - w0 * 0.25f)), e1 = point(c, f, s, a1, radius * (1 - w1 * 0.25f));
            d.quad(e0, o0, o1, e1, fx.p.color2(), al0 * 0.4f, al0, al1, al1 * 0.4f);
            Vector3f g0 = point(c, f, s, a0, radius * 1.08f), g1 = point(c, f, s, a1, radius * 1.08f);
            d.quad(o0, g0, g1, o1, fx.p.color(), al0 * 0.5f, 0, 0, al1 * 0.5f);
        }
    }

    private static Vector3f point(Vector3f c, Vector3f f, Vector3f s, float ang, float r) {
        return new Vector3f(c).add(new Vector3f(f).mul(Mth.cos(ang) * r)).add(new Vector3f(s).mul(Mth.sin(ang) * r));
    }

    private static void pillar(Draw d, Effect fx, float pt) {
        float t = t(fx, pt);
        float grow = t < 0.12f ? t / 0.12f : 1f;
        float fade = t < 0.5f ? 1f : 1f - (t - 0.5f) * 2f;
        float r = fx.p.size() * (t < 0.12f ? 0.3f + grow * 0.9f : 1.2f - (t - 0.12f) * 0.7f);
        float h = fx.p.extra() * grow;
        Vec3 c = a(fx);
        float time = clientTicks + pt;
        d.band(c, r * 1.35f, 0, h * 0.6f, fx.p.color(), 0.25f * fade, 0, 20);
        d.band(c, r, 0, h, fx.p.color(), 0.7f * fade, 0.05f * fade, 20);
        d.band(c, r * 0.4f, 0, h, fx.p.color2(), fade, 0.2f * fade, 12);
        // spiralling motes
        for (int k = 0; k < 6; k++) {
            float ang = time * 0.25f + k * Mth.TWO_PI / 6;
            float y = ((time * 0.35f + k * 1.7f) % Math.max(1f, h));
            Vector3f p = d.rel(c.add(Mth.cos(ang) * r * 1.1f, y, Mth.sin(ang) * r * 1.1f));
            d.billboard(p, 0.25f, 0.25f, ang, fx.p.color2(), fade);
        }
        d.annulus(c, 0, r * 2.4f, 0.05f, fx.p.color(), 0.6f * fade, 0, 0, Mth.TWO_PI, 24);
    }

    private static void dome(Draw d, Effect fx, float pt) {
        float t = t(fx, pt);
        float fade = Math.min(1, Math.min(t * 8, (1 - t) * 5));
        Vec3 c = entityPos(fx.p.ent1(), pt, a(fx), false);
        float R = fx.p.size() * (t < 0.1f ? easeOut(t * 10) : 1f);
        float time = clientTicks + pt;
        int lat = 9, lon = 28;
        float ripple = (time * 0.04f) % 1f;
        for (int i = 0; i < lat; i++) {
            float p0 = (Mth.HALF_PI) * i / lat, p1 = (Mth.HALF_PI) * (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                float q0 = Mth.TWO_PI * j / lon, q1 = Mth.TWO_PI * (j + 1) / lon;
                Vector3f v00 = sph(d, c, R, p0, q0), v01 = sph(d, c, R, p0, q1), v11 = sph(d, c, R, p1, q1), v10 = sph(d, c, R, p1, q0);
                float a00 = domeAlpha(v00, c, d, p0, ripple), a01 = domeAlpha(v01, c, d, p0, ripple);
                float a11 = domeAlpha(v11, c, d, p1, ripple), a10 = domeAlpha(v10, c, d, p1, ripple);
                d.quad(v00, v01, v11, v10, fx.p.color(), a00 * fade, a01 * fade, a11 * fade, a10 * fade);
            }
        }
        d.annulus(c, R * 0.96f, R * 1.03f, 0.06f, fx.p.color2(), 0.5f * fade, 0.5f * fade, 0, Mth.TWO_PI, 48);
    }

    // ------------------------------------------------------------------ the core's barrier

    private static boolean coreDomeVisible() {
        if (!com.lastbastion.client.ClientSiegeState.active()) return false;
        if (com.lastbastion.client.ClientSiegeState.shieldFraction() <= 0.01f && !com.lastbastion.client.ClientSiegeState.aegis()) return false;
        net.minecraft.core.BlockPos cp = com.lastbastion.client.ClientSiegeState.corePos();
        if (cp.equals(net.minecraft.core.BlockPos.ZERO) || Minecraft.getInstance().player == null) return false;
        return Minecraft.getInstance().player.position().distanceToSqr(Vec3.atCenterOf(cp)) < 128 * 128;
    }

    /** Persistent hex-shimmer barrier over the core while it has shield (bright white-blue during Aegis). */
    private static void coreDome(Draw d, float pt) {
        Vec3 c = Vec3.atBottomCenterOf(com.lastbastion.client.ClientSiegeState.corePos());
        boolean aegis = com.lastbastion.client.ClientSiegeState.aegis();
        float sh = com.lastbastion.client.ClientSiegeState.shieldFraction();
        float strength = aegis ? 1.1f : 0.25f + 0.55f * sh;
        int color = aegis ? 0x9FE0FF : 0x3A9ACF;
        float R = 3.2f;
        float time = clientTicks + pt;
        int lat = 9, lon = 32;
        float ripple = (time * 0.025f) % 1f;
        for (int i = 0; i < lat; i++) {
            float p0 = Mth.HALF_PI * i / lat, p1 = Mth.HALF_PI * (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                float q0 = Mth.TWO_PI * j / lon + time * 0.004f, q1 = Mth.TWO_PI * (j + 1) / lon + time * 0.004f;
                Vector3f v00 = sph(d, c, R, p0, q0), v01 = sph(d, c, R, p0, q1), v11 = sph(d, c, R, p1, q1), v10 = sph(d, c, R, p1, q0);
                float hex = ((i + j) % 2 == 0) ? 1f : 0.7f;
                float k = strength * hex * 0.55f;
                d.quad(v00, v01, v11, v10, color, domeAlpha(v00, c, d, p0, ripple) * k, domeAlpha(v01, c, d, p0, ripple) * k,
                        domeAlpha(v11, c, d, p1, ripple) * k, domeAlpha(v10, c, d, p1, ripple) * k);
            }
        }
        d.annulus(c, R * 0.97f, R * 1.04f, 0.06f, 0x6FE3FF, 0.45f * strength, 0.45f * strength, 0, Mth.TWO_PI, 48);
    }

    private static Vector3f sph(Draw d, Vec3 c, float r, float phi, float theta) {
        float y = Mth.sin(phi) * r, h = Mth.cos(phi) * r;
        return d.rel(c.add(Mth.cos(theta) * h, y, Mth.sin(theta) * h));
    }

    private static float domeAlpha(Vector3f v, Vec3 c, Draw d, float phi, float ripple) {
        Vector3f center = d.rel(c);
        Vector3f n = new Vector3f(v).sub(center).normalize();
        Vector3f view = new Vector3f(v).normalize();
        float fres = 1 - Math.abs(n.dot(view));
        float a = 0.05f + fres * fres * 0.55f;
        float latT = 1 - phi / Mth.HALF_PI;
        float rd = Math.abs(latT - ripple);
        if (rd < 0.08f) a += (0.08f - rd) * 4f;
        return a;
    }

    private static void sphere(Draw d, Effect fx, float pt) {
        float t = t(fx, pt);
        float R = Math.max(0.05f, fx.p.size() * easeOut(t));
        float fade = (1 - t) * (1 - t);
        Vec3 c = a(fx);
        int lat = 10, lon = 20;
        Vector3f center = d.rel(c);
        for (int i = 0; i < lat; i++) {
            float p0 = -Mth.HALF_PI + Mth.PI * i / lat, p1 = -Mth.HALF_PI + Mth.PI * (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                float q0 = Mth.TWO_PI * j / lon, q1 = Mth.TWO_PI * (j + 1) / lon;
                Vector3f v00 = sph(d, c, R, p0, q0), v01 = sph(d, c, R, p0, q1), v11 = sph(d, c, R, p1, q1), v10 = sph(d, c, R, p1, q0);
                d.quad(v00, v01, v11, v10, fx.p.color(), shellA(v00, center) * fade, shellA(v01, center) * fade, shellA(v11, center) * fade,
                        shellA(v10, center) * fade);
            }
        }
        d.glowDisc(center, R * 0.6f, fx.p.color2(), fade * 0.8f);
    }

    private static float shellA(Vector3f v, Vector3f center) {
        Vector3f n = new Vector3f(v).sub(center).normalize();
        Vector3f view = new Vector3f(v).normalize();
        float f = 1 - Math.abs(n.dot(view));
        return 0.08f + f * f * 0.8f;
    }

    private static void flash(Draw d, Effect fx, float pt) {
        float t = t(fx, pt);
        float s = fx.p.size() * (t < 0.2f ? 0.5f + t * 2.5f : 1f + (t - 0.2f) * 0.3f);
        float fade = t < 0.2f ? 1f : 1 - (t - 0.2f) / 0.8f;
        Vector3f c = d.rel(a(fx));
        d.glowDisc(c, s, fx.p.color(), 0.9f * fade);
        d.glowDisc(c, s * 0.45f, hot(fx), fade);
        RNG.setSeed(fx.seed);
        int rays = 8;
        for (int i = 0; i < rays; i++) {
            float roll = i * Mth.PI / rays + RNG.nextFloat() * 0.3f + t * 0.4f;
            float len = s * (1.6f + RNG.nextFloat() * 1.4f);
            d.billboard(c, len * 2, s * 0.08f, roll, fx.p.color(), 0.8f * fade);
            d.billboard(c, len * 1.2f, s * 0.03f, roll, hot(fx), fade);
        }
    }

    private static void vortex(Draw d, Effect fx, float pt) {
        float t = t(fx, pt);
        float fade = Math.min(1, Math.min(t * 6, (1 - t) * 4));
        Vec3 c = entityPos(fx.p.ent1(), pt, a(fx), false);
        float R = fx.p.size(), H = Math.max(0.5f, fx.p.extra());
        float time = clientTicks + pt;
        int strands = 4, steps = 26;
        for (int k = 0; k < strands; k++) {
            Vector3f prev = null;
            for (int i = 0; i <= steps; i++) {
                float u = i / (float) steps;
                float ang = time * 0.32f + k * Mth.TWO_PI / strands + u * 7f;
                float r = R * (0.25f + 0.75f * u);
                Vector3f p = d.rel(c.add(Mth.cos(ang) * r, u * H, Mth.sin(ang) * r));
                if (prev != null) {
                    int col = k % 2 == 0 ? fx.p.color() : fx.p.color2();
                    float al = fade * (1 - u * 0.7f);
                    d.ribbon(prev, p, 0.28f + u * 0.3f, 0.28f + u * 0.3f, col, al * 0.8f, al * 0.8f);
                }
                prev = p;
            }
        }
        for (int i = 0; i < 4; i++) {
            float u = i / 4f;
            d.band(c, R * (0.3f + 0.7f * u), u * H, (u + 0.25f) * H, fx.p.color(), 0.12f * fade, 0.04f * fade, 20);
        }
        d.annulus(c, 0, R * 1.2f, 0.05f, fx.p.color(), 0.35f * fade, 0, 0, Mth.TWO_PI, 24);
    }

    private static void sigil(Draw d, Effect fx, float pt) {
        float t = t(fx, pt);
        float fade = Math.min(1, Math.min(t * 8, (1 - t) * 5));
        float time = clientTicks + pt;
        float R = fx.p.size() * (t < 0.12f ? 0.6f + t * 3.3f : 1f);
        Vec3 c = a(fx);
        float rot = time * fx.p.extra() * Mth.DEG_TO_RAD;
        int col = fx.p.color();
        d.annulus(c, R * 0.93f, R, 0.06f, col, 0.95f * fade, 0.95f * fade, 0, Mth.TWO_PI, 56);
        d.annulus(c, R * 0.86f, R * 0.88f, 0.06f, col, 0.7f * fade, 0.7f * fade, 0, Mth.TWO_PI, 56);
        d.annulus(c, R * 0.5f, R * 0.54f, 0.06f, col, 0.8f * fade, 0.8f * fade, 0, Mth.TWO_PI, 40);
        d.annulus(c, 0, R * 1.1f, 0.03f, col, 0.18f * fade, 0, 0, Mth.TWO_PI, 32);
        // rune ticks between the outer rings, counter-rotating
        for (int i = 0; i < 24; i++) {
            float a0 = -rot * 0.7f + i * Mth.TWO_PI / 24;
            float a1 = a0 + Mth.TWO_PI / 24 * (i % 3 == 0 ? 0.6f : 0.25f);
            d.annulus(c, R * 0.72f, R * 0.84f, 0.065f, col, 0.85f * fade, 0.85f * fade, a0, a1, 2);
        }
        // hexagram
        Vector3f cc = d.rel(c);
        for (int tri = 0; tri < 2; tri++) {
            for (int i = 0; i < 3; i++) {
                float a0 = rot + tri * Mth.PI / 3 + i * Mth.TWO_PI / 3, a1 = a0 + Mth.TWO_PI / 3;
                Vector3f p0 = new Vector3f(cc.x + Mth.cos(a0) * R * 0.86f, cc.y + 0.07f, cc.z + Mth.sin(a0) * R * 0.86f);
                Vector3f p1 = new Vector3f(cc.x + Mth.cos(a1) * R * 0.86f, cc.y + 0.07f, cc.z + Mth.sin(a1) * R * 0.86f);
                flatLine(d, p0, p1, 0.07f * Math.max(1, R / 3), col, 0.9f * fade);
            }
        }
        d.glowDisc(new Vector3f(cc.x, cc.y + 0.4f, cc.z), R * 0.35f, fx.p.color2(), 0.35f * fade);
    }

    /** Horizontal line segment drawn as a flat quad on the XZ plane. */
    private static void flatLine(Draw d, Vector3f a, Vector3f b, float w, int col, float alpha) {
        Vector3f dir = new Vector3f(b).sub(a);
        Vector3f side = new Vector3f(-dir.z, 0, dir.x);
        if (side.lengthSquared() < 1e-8f) return;
        side.normalize().mul(w * 0.5f);
        d.quad(new Vector3f(a).sub(side), new Vector3f(a).add(side), new Vector3f(b).add(side), new Vector3f(b).sub(side), col, alpha);
    }

    private static void tether(Draw d, Effect fx, float pt) {
        float t = t(fx, pt);
        float fade = Math.min(1, (1 - t) * 4);
        Vec3 from = fx.p.extra() > 0 ? entityPos(fx.p.ent1(), pt, a(fx), false).add(0, fx.p.extra(), 0) : entityPos(fx.p.ent1(), pt, a(fx), true);
        Vec3 to = entityPos(fx.p.ent2(), pt, b(fx), true);
        float time = clientTicks + pt;
        float w = fx.p.size();
        Vector3f prev = d.rel(from);
        int segs = 12;
        for (int i = 1; i <= segs; i++) {
            float s = i / (float) segs;
            Vec3 p = from.lerp(to, s).add(0, Mth.sin(s * Mth.PI) * 0.4f * Mth.sin(time * 0.3f) + Mth.sin(s * 12 + time * 0.9f) * 0.06f, 0);
            Vector3f next = d.rel(p);
            d.ribbon(prev, next, w * 2.5f, w * 2.5f, fx.p.color(), 0.2f * fade, 0.2f * fade);
            d.ribbon(prev, next, w, w, fx.p.color(), 0.8f * fade, 0.8f * fade);
            d.ribbon(prev, next, w * 0.3f, w * 0.3f, hot(fx), fade, fade);
            prev = next;
        }
        d.glowDisc(d.rel(to), w * 3, fx.p.color(), 0.7f * fade);
    }

    private static void wave(Draw d, Effect fx, float pt) {
        float t = t(fx, pt);
        Vec3 from = a(fx), to = b(fx);
        Vec3 dir = to.subtract(from);
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        if (flat.lengthSqr() < 1e-4) return;
        Vec3 n = flat.normalize();
        Vec3 side = new Vec3(-n.z, 0, n.x);
        float fade = Math.min(1, Math.min(t * 6, (1 - t) * 3));
        float W = fx.p.size(), H = fx.p.extra() * (0.6f + 0.4f * Mth.sin(t * Mth.PI));
        float time = clientTicks + pt;
        for (int layer = 0; layer < 3; layer++) {
            Vec3 centre = from.add(dir.scale(Math.max(0, easeOut(t) - layer * 0.05f)));
            int segs = 16;
            for (int i = 0; i < segs; i++) {
                float u0 = i / (float) segs - 0.5f, u1 = (i + 1) / (float) segs - 0.5f;
                float h0 = H * (1 - 4 * u0 * u0) * (1 + 0.15f * Mth.sin(u0 * 14 + time * 0.7f));
                float h1 = H * (1 - 4 * u1 * u1) * (1 + 0.15f * Mth.sin(u1 * 14 + time * 0.7f));
                Vec3 b0 = centre.add(side.scale(u0 * W)), b1 = centre.add(side.scale(u1 * W));
                Vec3 lean = n.scale(H * 0.3f);
                Vector3f p0 = d.rel(b0), p1 = d.rel(b1), q0 = d.rel(b0.add(0, h0, 0).add(lean)), q1 = d.rel(b1.add(0, h1, 0).add(lean));
                int col = layer == 0 ? fx.p.color2() : fx.p.color();
                float al = fade * (layer == 0 ? 0.55f : 0.35f);
                d.quad(p0, p1, q1, q0, col, al * 0.5f, al * 0.5f, al, al);
            }
        }
    }

    private static void aura(Draw d, Effect fx, float pt) {
        float t = t(fx, pt);
        float fade = Math.min(1, Math.min(t * 6, (1 - t) * 4));
        Vec3 c = entityPos(fx.p.ent1(), pt, a(fx), false);
        float R = fx.p.size();
        float time = clientTicks + pt;
        float rot = time * 0.08f;
        for (int i = 0; i < 6; i++) {
            float a0 = rot + i * Mth.TWO_PI / 6;
            d.annulus(c, R * 0.86f, R, 0.05f, fx.p.color(), 0.85f * fade, 0.85f * fade, a0, a0 + Mth.TWO_PI / 6 * 0.7f, 6);
        }
        d.annulus(c, R * 0.5f, R * 0.56f, 0.05f, fx.p.color(), 0.5f * fade, 0.5f * fade, 0, Mth.TWO_PI, 32);
        d.band(c, R * 0.95f, 0, 1.4f, fx.p.color(), 0.3f * fade, 0, 32);
        for (int k = 0; k < 5; k++) {
            float ang = -rot * 2 + k * Mth.TWO_PI / 5;
            float y = (time * 0.05f + k * 0.37f) % 1f * 2.2f;
            Vector3f p = d.rel(c.add(Mth.cos(ang) * R * 0.8f, y, Mth.sin(ang) * R * 0.8f));
            d.billboard(p, 0.18f, 0.18f, ang, fx.p.color2(), fade * (1 - y / 2.2f));
        }
    }

    private static void dash(Draw d, Effect fx, float pt) {
        float t = t(fx, pt);
        float fade = (1 - t) * (1 - t);
        Vector3f from = d.rel(a(fx)), to = d.rel(b(fx));
        float w = fx.p.size();
        d.ribbon(from, to, 0, w * 2.2f, fx.p.color(), 0, 0.3f * fade);
        d.ribbon(from, to, 0, w, fx.p.color(), 0, 0.8f * fade);
        d.ribbon(from, to, 0, w * 0.3f, hot(fx), 0, fade);
    }

    // ------------------------------------------------------------------ telegraphs

    private static void telegraph(Draw d, Effect fx, float pt) {
        float t = t(fx, pt);
        float time = clientTicks + pt;
        float pulse = 0.75f + 0.25f * Mth.sin(time * (0.4f + t * 0.8f));
        int col = fx.p.color();
        float y = 0.07f;
        switch (fx.p.kind()) {
            case TELE_CIRCLE -> {
                Vec3 c = a(fx);
                float R = fx.p.size();
                d.annulus(c, R * 0.95f, R, y, col, 0.9f * pulse, 0.9f * pulse, 0, Mth.TWO_PI, 48);
                d.annulus(c, 0, R * 0.95f, y, col, 0.12f, 0.2f, 0, Mth.TWO_PI, 40);
                float fr = R * easeOut(t);
                d.annulus(c, 0, fr, y + 0.005f, col, 0.18f, 0.42f, 0, Mth.TWO_PI, 40);
                d.annulus(c, Math.max(0, fr - 0.12f), fr, y + 0.01f, col, 0.8f, 0.8f, 0, Mth.TWO_PI, 40);
            }
            case TELE_DONUT -> {
                Vec3 c = a(fx);
                float R = fx.p.size(), r = fx.p.extra();
                d.annulus(c, R * 0.96f, R, y, col, 0.9f * pulse, 0.9f * pulse, 0, Mth.TWO_PI, 48);
                d.annulus(c, r, r + 0.12f, y, col, 0.9f * pulse, 0.9f * pulse, 0, Mth.TWO_PI, 40);
                d.annulus(c, r, R, y, col, 0.12f, 0.2f, 0, Mth.TWO_PI, 40);
                float fr = Mth.lerp(easeOut(t), R, r);
                d.annulus(c, fr, R, y + 0.005f, col, 0.45f, 0.3f, 0, Mth.TWO_PI, 40);
            }
            case TELE_LINE -> {
                Vec3 from = a(fx), to = b(fx);
                Vec3 dir = to.subtract(from);
                Vec3 flat = new Vec3(dir.x, 0, dir.z);
                if (flat.lengthSqr() < 1e-4) return;
                Vec3 side = new Vec3(-flat.z, 0, flat.x).normalize().scale(fx.p.size() / 2);
                Vec3 fill = from.add(dir.scale(easeOut(t)));
                Vector3f a0 = d.rel(from.subtract(side).add(0, y, 0)), a1 = d.rel(from.add(side).add(0, y, 0));
                Vector3f b0 = d.rel(to.subtract(side).add(0, y, 0)), b1 = d.rel(to.add(side).add(0, y, 0));
                Vector3f f0 = d.rel(fill.subtract(side).add(0, y + 0.005f, 0)), f1 = d.rel(fill.add(side).add(0, y + 0.005f, 0));
                d.quad(a0, a1, b1, b0, col, 0.16f);
                d.quad(a0, a1, f1, f0, col, 0.4f);
                flatLine(d, a0, b0, 0.12f, col, 0.9f * pulse);
                flatLine(d, a1, b1, 0.12f, col, 0.9f * pulse);
                flatLine(d, b0, b1, 0.12f, col, 0.9f * pulse);
                flatLine(d, f0, f1, 0.1f, col, 0.85f);
            }
            case TELE_CONE -> {
                Vec3 o = a(fx), tip = b(fx);
                Vec3 dir = new Vec3(tip.x - o.x, 0, tip.z - o.z);
                if (dir.lengthSqr() < 1e-4) return;
                float L = fx.p.size(), half = fx.p.extra() * Mth.DEG_TO_RAD;
                float base = (float) Math.atan2(dir.z, dir.x);
                d.annulus(o, 0, L, y, col, 0.12f, 0.2f, base - half, base + half, 24);
                d.annulus(o, 0, L * easeOut(t), y + 0.005f, col, 0.25f, 0.45f, base - half, base + half, 24);
                d.annulus(o, L - 0.14f, L, y + 0.01f, col, 0.9f * pulse, 0.9f * pulse, base - half, base + half, 24);
                Vector3f oc = d.rel(o.add(0, y, 0));
                for (int sgn = -1; sgn <= 1; sgn += 2) {
                    float ang = base + sgn * half;
                    Vector3f e = d.rel(o.add(Mth.cos(ang) * L, y, Mth.sin(ang) * L));
                    flatLine(d, oc, e, 0.12f, col, 0.9f * pulse);
                }
            }
            default -> {
            }
        }
    }

    /** Subtle additive glow on top of telegraphs during the last moments before impact. */
    private static void teleGlow(Draw d, Effect fx, float pt) {
        float t = t(fx, pt);
        if (t < 0.7f) return;
        float k = (t - 0.7f) / 0.3f;
        if (fx.p.kind() == VfxKind.TELE_CIRCLE) {
            d.annulus(a(fx), 0, fx.p.size(), 0.09f, fx.p.color(), 0.25f * k, 0.5f * k, 0, Mth.TWO_PI, 40);
        } else if (fx.p.kind() == VfxKind.TELE_DONUT) {
            d.annulus(a(fx), fx.p.extra(), fx.p.size(), 0.09f, fx.p.color(), 0.4f * k, 0.4f * k, 0, Mth.TWO_PI, 40);
        }
    }
}
