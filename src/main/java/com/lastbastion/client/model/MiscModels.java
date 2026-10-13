package com.lastbastion.client.model;

import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.render.ProcRenderer;
import com.lastbastion.client.render.SimpleProcRenderer;
import com.lastbastion.entity.misc.DropPodEntity;
import com.lastbastion.entity.misc.SentryDroneEntity;
import com.lastbastion.entity.misc.TitanExoEntity;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.projectile.SiegeProjectile.ProjKind;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Projectiles, the drop pod, the Titan Exosuit, the sentry drone and the Siege Core crystal. */
public final class MiscModels {
    private static ModelSpec.Baked projectiles, pod, exo, drone, core;

    private MiscModels() {
    }

    // ------------------------------------------------------------------ projectiles

    public static ModelSpec.Baked projectiles() {
        if (projectiles != null) return projectiles;
        ModelSpec s = new ModelSpec("projectiles");
        Part r = s.root.child("p", 0, 20, 0);
        for (ProjKind k : ProjKind.values()) {
            Part p = r.child("k_" + k.name().toLowerCase(), 0, 0, 0).levels(k.ordinal() + 1, k.ordinal() + 1);
            switch (k) {
                case BOLT, ARROW -> {
                    p.box(-0.5f, -0.5f, -6, 1, 1, 12, Paint.of(k == ProjKind.ARROW ? 0xC8B080 : 0x6A4A2A));
                    p.box(-1, -1, -8, 2, 2, 2, Paint.of(0xB0B0B8));
                    p.box(-1.5f, -0.25f, 4, 3, 0.5f, 2, Paint.of(0xE8E0D0));
                }
                case FROST_SHARD -> p.child("fs", 0, 0, 0).rot(45, 45, 0).box(-1.5f, -1.5f, -3, 3, 3, 6, Paint.glowing(0xBFF4FF).pattern(Pattern.CRACKS, 0xFFFFFF));
                case WATER_ORB -> p.box(-2, -2, -2, 4, 4, 4, Paint.glowing(0x3A7BFF).pattern(Pattern.CHECKER, 0x8AB8FF));
                case ACID_GLOB, BILE -> p.box(-2, -2, -2, 4, 4, 4, Paint.glowing(0x7BD940).pattern(Pattern.SPOTS, 0xC8FF60));
                case MORTAR_SHELL, GRENADE -> {
                    p.box(-2, -2, -3, 4, 4, 6, Paint.of(k == ProjKind.GRENADE ? 0x4A5A3A : 0x3A3A3A).pattern(Pattern.BANDS, 0x6A6A6A));
                    p.box(-1, -1, -4, 2, 2, 1, Paint.glowing(0xFF5020));
                }
                case HOLY_BOLT -> p.child("hb", 0, 0, 0).rot(45, 0, 45).box(-1.5f, -1.5f, -1.5f, 3, 3, 3, Paint.glowing(0xFFF2B0));
                case ARCANE_BOLT -> p.child("ab", 0, 0, 0).rot(45, 0, 45).box(-1.5f, -1.5f, -1.5f, 3, 3, 3, Paint.glowing(0xC86BFF));
                case FIREBALL, METEOR -> p.box(-2, -2, -2, 4, 4, 4, Paint.glowing(0xFF7A1A).pattern(Pattern.CRACKS, 0xFFE040));
                case MISSILE, ROCKET -> {
                    p.box(-1, -1, -4, 2, 2, 8, Paint.of(0xC8C8C8).pattern(Pattern.BANDS, 0xA02020));
                    p.box(-1.5f, -0.25f, 2, 3, 0.5f, 2, Paint.of(0x5A5A5A));
                    p.box(-0.25f, -1.5f, 2, 0.5f, 3, 2, Paint.of(0x5A5A5A));
                    p.box(-0.8f, -0.8f, 4, 1.6f, 1.6f, 1, Paint.glowing(0xFFA020));
                }
                case BOULDER -> p.box(-3, -3, -3, 6, 6, 6, Paint.of(0xC8C0A8).pattern(Pattern.CRACKS, 0x7A7060));
                case SOUL_BOLT -> p.box(-1.5f, -1.5f, -1.5f, 3, 3, 3, Paint.glowing(0x5AD8FF).face(Face.FRONT, DecalType.SKULL, 0x0A3A5A, false));
                case JAVELIN -> {
                    p.box(-0.5f, -0.5f, -8, 1, 1, 16, Paint.glowing(0xF4F06A));
                    p.child("jz", 0, 0, -3).rot(0, 30, 0).box(-0.5f, -0.5f, -3, 1, 1, 3, Paint.glowing(0xFFFFFF));
                }
                case HOOK -> {
                    p.box(-0.5f, -0.5f, -2, 1, 1, 4, Paint.of(0x8A8A8A));
                    p.box(-2, -0.5f, -3, 4, 1, 1, Paint.of(0xB0B0B0));
                }
                case WIND_BLADE -> p.box(-4, -0.25f, -1, 8, 0.5f, 2, Paint.glowing(0xCFFFF0));
                case SHADOW_ORB -> p.box(-2, -2, -2, 4, 4, 4, Paint.glowing(0x7A3AAA));
            }
        }
        projectiles = s.bake();
        return projectiles;
    }

    public static SimpleProcRenderer<SiegeProjectile> projectileRenderer(EntityRendererProvider.Context ctx) {
        return new SimpleProcRenderer<>(ctx, projectiles(), (m, e, ls, la, age, hy, hp, pt) -> {
            m.level = e.kind().ordinal() + 1;
            m.applyGates();
            m.p("p").zRot = e.kind() == ProjKind.BOULDER || e.kind() == ProjKind.GRENADE ? age * 0.4f : 0;
            ModelPart fs = m.opt("fs");
            if (fs != null) fs.zRot = age * 0.5f;
        }, (e, pose, pt) -> {
            Vec3 v = e.getDeltaMovement();
            float yaw = (float) (Mth.atan2(v.x, v.z) * Mth.RAD_TO_DEG);
            float pitch = (float) (Mth.atan2(v.y, v.horizontalDistance()) * Mth.RAD_TO_DEG);
            pose.translate(0, 0.15, 0);
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            pose.mulPose(Axis.XP.rotationDegrees(-pitch));
            float s = e.visualScale() * 0.6f;
            pose.scale(s, s, s);
        }, true);
    }

    // ------------------------------------------------------------------ drop pod

    public static ModelSpec.Baked pod() {
        if (pod != null) return pod;
        ModelSpec s = new ModelSpec("drop_pod");
        Paint hull = Paint.of(0x6A7480).pattern(Pattern.PLATES, 0xA0A8B0);
        Part p = s.root.child("pod", 0, 24, 0);
        p.box(-11, -40, -11, 22, 36, 22, hull.copy().face(Face.FRONT, DecalType.EMBLEM, 0xFFB000, false));
        p.box(-9, -48, -9, 18, 8, 18, Paint.of(0x4A5058).pattern(Pattern.BANDS, 0xE8B020));
        p.box(-5, -52, -5, 10, 4, 10, Paint.of(0x3A4048));
        for (int i = 0; i < 4; i++) {
            p.child("fin" + i, 0, -4, 0).rot(0, 45 + 90 * i, 0).box(10, -16, -1, 6, 16, 2, Paint.of(0xE8B020).pattern(Pattern.STRIPES, 0x2A2A2A));
        }
        p.child("thruster", 0, -4, 0).box(-7, 0, -7, 14, 4, 14, Paint.glowing(0xFF9030).pattern(Pattern.CRACKS, 0xFFF0A0));
        pod = s.bake();
        return pod;
    }

    public static SimpleProcRenderer<DropPodEntity> podRenderer(EntityRendererProvider.Context ctx) {
        return new SimpleProcRenderer<>(ctx, pod(), (m, e, ls, la, age, hy, hp, pt) -> {
            m.p("thruster").visible = !e.landed();
        }, (e, pose, pt) -> pose.scale(1.4f, 1.4f, 1.4f), false);
    }

    // ------------------------------------------------------------------ Titan exosuit

    public static ModelSpec.Baked exo() {
        if (exo != null) return exo;
        ModelSpec s = new ModelSpec("titan_exo");
        Paint hull = Paint.of(0xC8A030).pattern(Pattern.PLATES, 0x8A7020).noise(0.06f);
        Paint dark = Paint.of(0x3A3A40).pattern(Pattern.PLATES, 0x5A5A60);
        Part body = s.root.child("body", 0, -6, 0);
        body.box(-9, -12, -2, 18, 14, 10, hull.copy().face(Face.BACK, DecalType.EMBLEM, 0x2A2A2A, false));
        body.box(-7, 2, -5, 14, 4, 10, dark);
        body.child("seat", 0, -2, -1).box(-5, -2, -3, 10, 2, 6, Paint.of(0x2A2A2A).pattern(Pattern.CLOTH, 0x1A1A1A));
        body.child("cage", 0, -12, -2).box(-8, -2, -4, 16, 2, 2, dark).box(-8, -12, -4, 2, 12, 2, dark).box(6, -12, -4, 2, 12, 2, dark);
        body.child("jets", 0, -6, 8).box(-7, -4, 0, 4, 10, 4, dark).box(3, -4, 0, 4, 10, 4, dark)
                .box(-6.5f, 6, 0.5f, 3, 1, 3, Paint.glowing(0x50C8FF)).box(3.5f, 6, 0.5f, 3, 1, 3, Paint.glowing(0x50C8FF));
        body.child("missile_rack", 0, -12, 4).box(-6, -4, -2, 12, 4, 5, hull.copy().face(Face.TOP, DecalType.DOTS, 0xFF4020, true));
        for (int side = 0; side < 2; side++) {
            float sx = side == 0 ? -1 : 1;
            String n = side == 0 ? "r" : "l";
            Part arm = s.root.child(side == 0 ? "rarm" : "larm", sx * 12, -14, 2).box(-3, -3, -3, 6, 6, 6, hull);
            Part gun = arm.child("egun_" + n, 0, 6, 0).box(-2.5f, 0, -2.5f, 5, 10, 5, dark);
            Part brl = gun.child("ebarrels_" + n, 0, 8, -2.5f).rot(-90, 0, 0);
            for (int i = 0; i < 3; i++) {
                double a = Mth.TWO_PI * i / 3;
                brl.box((float) Math.cos(a) * 1.2f - 0.6f, -10, (float) Math.sin(a) * 1.2f - 0.6f, 1.2f, 10, 1.2f, Paint.of(0x2A2A2A).pattern(Pattern.BANDS, 0x5A5A5A));
            }
            Part leg = s.root.child(side == 0 ? "rleg" : "lleg", sx * 5, 0, 2).box(-3, 0, -3, 6, 13, 6, hull);
            leg.child("efoot_" + n, 0, 13, 0).box(-4, 0, -6, 8, 11, 10, dark);
        }
        exo = s.bake();
        return exo;
    }

    public static ProcRenderer<TitanExoEntity> exoRenderer(EntityRendererProvider.Context ctx) {
        return new ProcRenderer<TitanExoEntity>(ctx, exo(), (m, e, ls, la, age, hy, hp, pt) -> {
            Anim.walk(m.p("rleg"), m.p("lleg"), ls * 0.8f, la, 0.7f);
            float spin = e.firing() ? age * 1.6f : 0;
            m.p("ebarrels_r").yRot = spin;
            m.p("ebarrels_l").yRot = -spin;
            float aim = e.isVehicle() ? -1.3f + hp * Anim.D : -0.2f;
            m.p("rarm").xRot += aim;
            m.p("larm").xRot += aim;
            if (e.firing()) {
                m.p("rarm").z += Mth.sin(age * 3) * 0.4f;
                m.p("larm").z += Mth.cos(age * 3) * 0.4f;
            }
            m.p("body").y += Math.abs(Mth.sin(ls * 0.66f)) * la;
        }, 1.2f).scale(1.15f);
    }

    // ------------------------------------------------------------------ sentry drone

    public static ModelSpec.Baked drone() {
        if (drone != null) return drone;
        ModelSpec s = new ModelSpec("sentry_drone");
        Part b = s.root.child("body", 0, 18, 0);
        b.box(-3, -2, -3, 6, 3, 6, Paint.of(0xE0E4E8).pattern(Pattern.PLATES, 0x9AA0A8).face(Face.FRONT, DecalType.VISOR, 0x7FE8FF, true));
        b.child("gunpod", 0, 1, -2).box(-1, 0, -3, 2, 1.5f, 3, Paint.of(0x3A3A40));
        for (int i = 0; i < 4; i++) {
            Part arm = b.child("arm" + i, 0, -1, 0).rot(0, 45 + 90 * i, 0);
            arm.box(0, -0.5f, -0.5f, 5, 1, 1, Paint.of(0x5A5A60));
            arm.child("rotor" + i, 5, -1, 0).box(-2.5f, -0.25f, -0.5f, 5, 0.5f, 1, Paint.of(0x2A2A2A));
        }
        b.child("light", 0, 1, 0).box(-0.75f, 0, -0.75f, 1.5f, 1, 1.5f, Paint.glowing(0x7FE8FF));
        drone = s.bake();
        return drone;
    }

    public static ProcRenderer<SentryDroneEntity> droneRenderer(EntityRendererProvider.Context ctx) {
        return new ProcRenderer<SentryDroneEntity>(ctx, drone(), (m, e, ls, la, age, hy, hp, pt) -> {
            for (int i = 0; i < 4; i++) m.p("rotor" + i).yRot = age * 2.2f;
            m.p("body").y += Mth.sin(age * 0.15f) * 0.6f;
            m.p("body").zRot = Mth.sin(age * 0.07f) * 0.08f;
        }, 0.2f);
    }

    // ------------------------------------------------------------------ Siege Core crystal (block entity)

    public static ModelSpec.Baked core() {
        if (core != null) return core;
        ModelSpec s = new ModelSpec("siege_core");
        Part c = s.root.child("crystal", 0, 0, 0);
        c.child("gem", 0, 0, 0).rot(45, 0, 45).box(-4, -4, -4, 8, 8, 8, Paint.glowing(0x6FE3FF).pattern(Pattern.CRACKS, 0xE0FFFF));
        c.child("inner", 0, 0, 0).rot(0, 45, 0).box(-2, -6, -2, 4, 12, 4, Paint.glowing(0xFFFFFF).pattern(Pattern.GRADIENT, 0x6FE3FF));
        Paint ring = Paint.of(0xD8B040).pattern(Pattern.RUNES, 0x6FE3FF);
        Part r1 = s.root.child("ring1", 0, 0, 0);
        r1.box(-9, -0.5f, -9, 18, 1, 1, ring).box(-9, -0.5f, 8, 18, 1, 1, ring).box(-9, -0.5f, -8, 1, 1, 16, ring).box(8, -0.5f, -8, 1, 1, 16, ring);
        Part r2 = s.root.child("ring2", 0, 0, 0);
        r2.box(-7, -7, -0.5f, 14, 1, 1, ring).box(-7, 6, -0.5f, 14, 1, 1, ring).box(-7, -6, -0.5f, 1, 12, 1, ring).box(6, -6, -0.5f, 1, 12, 1, ring);
        Part shield = s.root.child("barrier", 0, 0, 0);
        Paint glass = Paint.glowing(0x3A9ACF).pattern(Pattern.SCALES, 0x6FE3FF);
        shield.box(-12, -12, -12, 24, 24, 24, glass);
        core = s.bake();
        return core;
    }
}
