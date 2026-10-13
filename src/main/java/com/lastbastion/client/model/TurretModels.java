package com.lastbastion.client.model;

import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.render.ProcRenderer;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.entity.turret.TurretKind;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/**
 * Turret models. Each family has a distinct silhouette; every level visibly upgrades it:
 * Lv2 reinforced plinth trim, Lv3 a second weapon/extra components (matching the Lv3 perk),
 * Lv4 corner pylons with glowing gems, Lv5 a floating crown ring and a radiant core.
 */
public final class TurretModels {
    private static final Map<TurretKind, ModelSpec.Baked> CACHE = new EnumMap<>(TurretKind.class);
    static final int STONE = 0x8A8A8A, DARKSTONE = 0x5A5A60, WOOD = 0x7A5230, IRON = 0x8A8A90, GOLD = 0xD8B040;

    private TurretModels() {
    }

    public static ModelSpec.Baked baked(TurretKind k) {
        return CACHE.computeIfAbsent(k, kind -> {
            ModelSpec s = new ModelSpec("turret_" + kind.id);
            Part yaw = base(s, kind);
            switch (kind) {
                case BALLISTA -> ballista(yaw);
                case FLAME -> flame(yaw);
                case FROST -> frost(yaw);
                case TIDE -> tide(yaw);
                case TESLA -> tesla(yaw);
                case VENOM -> venom(yaw);
                case MORTAR -> mortar(yaw);
                case PRISM -> prism(yaw);
                case GALE -> gale(yaw);
                case SANCTUM -> sanctum(yaw);
            }
            return s.bake();
        });
    }

    private static Part base(ModelSpec s, TurretKind k) {
        Part base = s.root.child("base", 0, 24, 0);
        base.box(-7, -3, -7, 14, 3, 14, Paint.of(STONE).pattern(Pattern.BRICK, 0x6A6A6A));
        base.child("trim", 0, 0, 0).from(2).box(-7.5f, -3.5f, -7.5f, 15, 1.5f, 15, Paint.of(IRON).pattern(Pattern.PLATES, 0xB0B0B8));
        Paint gem = Paint.glowing(k.color);
        int[][] corners = {{-6, -6}, {6, -6}, {-6, 6}, {6, 6}};
        for (int i = 0; i < 4; i++) {
            Part post = base.child("post" + i, corners[i][0], -3, corners[i][1]).from(3)
                    .box(-1, -4, -1, 2, 4, 2, Paint.of(DARKSTONE).pattern(Pattern.BANDS, IRON));
            post.child("gem" + i, 0, -4, 0).from(4).box(-1, -2, -1, 2, 2, 2, gem);
        }
        Part crown = s.root.child("crown", 0, 2, 0).from(5);
        Paint gold = Paint.of(GOLD).pattern(Pattern.RUNES, k.color);
        crown.box(-9, 0, -9, 18, 1, 1, gold).box(-9, 0, 8, 18, 1, 1, gold).box(-9, 0, -8, 1, 1, 16, gold).box(8, 0, -8, 1, 1, 16, gold);
        return s.root.child("yaw", 0, 21, 0);
    }

    private static void ballista(Part yaw) {
        Paint wood = Paint.of(WOOD).pattern(Pattern.WOOD, 0x5A3A20);
        Paint band = Paint.of(IRON).pattern(Pattern.PLATES, 0x5A5A60);
        yaw.box(-2, -6, -2, 4, 6, 4, wood);
        Part gun = yaw.child("gun", 0, -7, 0);
        gun.box(-1.5f, -1.5f, -9, 3, 3, 15, wood);
        gun.child("bands", 0, 0, 0).from(2).box(-1.5f, -1.5f, -6, 3, 3, 1, 0.3f, band).box(-1.5f, -1.5f, 2, 3, 3, 1, 0.3f, band);
        gun.child("bow_r", -1, -0.5f, -7).rot(0, -25, 0).box(-11, -0.5f, -0.75f, 11, 1, 1.5f, wood);
        gun.child("bow_l", 1, -0.5f, -7).rot(0, 25, 0).box(0, -0.5f, -0.75f, 11, 1, 1.5f, wood);
        gun.child("string", 0, -0.5f, 0).box(-9, -0.2f, -0.2f, 18, 0.4f, 0.4f, Paint.of(0xE8E0C8).flat());
        gun.child("bolt", 0, -2, -4).box(-0.5f, -0.5f, -8, 1, 1, 12, Paint.of(0x5A4020)).box(-1, -1, -10, 2, 2, 2, Paint.of(0xB0B0B8));
        Part twin = gun.child("twin", 0, -3.5f, 0).from(3);
        twin.box(-1.2f, -1, -8, 2.4f, 2, 13, wood);
        twin.child("bow2_r", -1, 0, -6).rot(0, -25, 0).box(-9, -0.5f, -0.5f, 9, 1, 1, Paint.of(IRON));
        twin.child("bow2_l", 1, 0, -6).rot(0, 25, 0).box(0, -0.5f, -0.5f, 9, 1, 1, Paint.of(IRON));
        gun.child("steeltips", 0, 0, 0).from(4).box(-12, -1, -9, 2, 2, 2, Paint.of(0xC8C8D0)).box(10, -1, -9, 2, 2, 2, Paint.of(0xC8C8D0));
        gun.child("runebolt", 0, -2, -12).from(5).box(-1, -1, -1, 2, 2, 2, Paint.glowing(0xFFD050));
    }

    private static void flame(Part yaw) {
        Paint tank = Paint.of(0xA02A1A).pattern(Pattern.BANDS, 0x6A1A10);
        yaw.box(-3, -4, -3, 6, 4, 6, Paint.of(DARKSTONE).pattern(Pattern.PLATES, IRON));
        yaw.child("tank", 0, -4, 3).box(-3, -9, -1, 6, 9, 5, tank.copy().face(Face.BACK, DecalType.EMBLEM, 0xFFB000, false));
        yaw.child("soultank", 0, -4, 3).from(5).box(-3, -9, -1, 6, 9, 5, 0.2f, Paint.glowing(0x50D8FF).pattern(Pattern.BANDS, 0x2080A0));
        Part gun = yaw.child("gun", 0, -7, 0);
        gun.box(-1.5f, -1.5f, -9, 3, 3, 9, Paint.of(0x3A3A40).pattern(Pattern.BANDS, 0x5A5A60));
        gun.child("nozzle", 0, 0, -9).box(-2.2f, -2.2f, -2, 4.4f, 4.4f, 2, Paint.of(0x2A2A2A));
        gun.child("pilot", 0, 2.5f, -10).box(-0.5f, -0.5f, -0.5f, 1, 1, 1, Paint.glowing(0xFFA020));
        gun.child("barrel2", 3, 0, 0).from(3).box(-1.2f, -1.2f, -8, 2.4f, 2.4f, 8, Paint.of(0x3A3A40).pattern(Pattern.BANDS, 0x5A5A60));
        gun.child("barrel3", -3, 0, 0).from(4).box(-1.2f, -1.2f, -8, 2.4f, 2.4f, 8, Paint.of(0x3A3A40).pattern(Pattern.BANDS, 0x5A5A60));
    }

    private static void frost(Part yaw) {
        Paint ice = Paint.of(0x9FD8F0).pattern(Pattern.PLATES, 0xD0F4FF);
        yaw.box(-2.5f, -12, -2.5f, 5, 12, 5, ice);
        yaw.child("collar", 0, -12, 0).box(-3.5f, -1, -3.5f, 7, 1, 7, Paint.of(IRON).pattern(Pattern.PLATES, 0xC0E8FF));
        Part gun = yaw.child("gun", 0, -17, 0);
        gun.box(-2.5f, -4, -2.5f, 5, 8, 5, Paint.glowing(0x8FE3FF).pattern(Pattern.CRACKS, 0xE0FFFF));
        gun.child("tip", 0, -4, 0).box(-1.5f, -3, -1.5f, 3, 3, 3, Paint.glowing(0xE0FFFF));
        Part orbit = yaw.child("orbit", 0, -15, 0);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2 * i;
            orbit.child("shard" + i, (float) Math.cos(a) * 6, 0, (float) Math.sin(a) * 6).from(i + 2).rot(0, 0, 20)
                    .box(-1, -3, -1, 2, 6, 2, Paint.glowing(0xBFF4FF));
        }
    }

    private static void tide(Part yaw) {
        Paint shell = Paint.of(0x3A8A9A).pattern(Pattern.SCALES, 0x5AB8C8);
        yaw.box(-5, -4, -5, 10, 4, 10, shell);
        yaw.child("water", 0, -4, 0).box(-4, -0.5f, -4, 8, 1, 8, Paint.glowing(0x3A7BFF).pattern(Pattern.CHECKER, 0x6AA8FF));
        Part gun = yaw.child("gun", 0, -7, 0);
        gun.box(-2, -2, -7, 4, 4, 9, Paint.of(0xE8806A).pattern(Pattern.BANDS, 0xC8604A));
        gun.child("orb", 0, 0, -8).box(-1.5f, -1.5f, -1.5f, 3, 3, 3, Paint.glowing(0x6AA8FF));
        yaw.child("spout_r", -5, -4, 0).from(3).rot(0, 0, -20).box(-1, -5, -1, 2, 5, 2, Paint.of(0xE8806A));
        yaw.child("spout_l", 5, -4, 0).from(3).rot(0, 0, 20).box(-1, -5, -1, 2, 5, 2, Paint.of(0xE8806A));
        Part coral = yaw.child("coral", 0, -4, 4).from(5);
        coral.box(-4, -6, -1, 2, 6, 2, Paint.glowing(0xFF7AB0)).box(2, -8, -1, 2, 8, 2, Paint.glowing(0xFF7AB0)).box(-1, -5, 1, 2, 5, 2, Paint.glowing(0xFFA0D0));
    }

    private static void tesla(Part yaw) {
        Paint copper = Paint.of(0xB87333).pattern(Pattern.BANDS, 0x7A4A20);
        yaw.box(-3, -14, -3, 6, 14, 6, copper);
        yaw.child("insulator", 0, -14, 0).box(-4, -1, -4, 8, 1, 8, Paint.of(0x2A2A2A)).box(-4, -6, -4, 8, 1, 8, Paint.of(0x2A2A2A));
        Part gun = yaw.child("gun", 0, -18, 0);
        gun.box(-3, -3, -3, 6, 6, 6, Paint.glowing(0xF4F06A).pattern(Pattern.CRACKS, 0xFFFFFF));
        for (int i = 0; i < 4; i++) {
            Part prong = yaw.child("prong" + i, 0, -10, 0).from(3).rot(0, 45 + 90 * i, 0);
            prong.box(3, -1, -0.5f, 4, 1, 1, Paint.of(0x5A5A60)).box(6.5f, -3, -0.5f, 1, 3, 1, Paint.glowing(0xF4F06A));
        }
        Part ring = yaw.child("tring", 0, -21, 0).from(5);
        ring.box(-6, 0, -6, 12, 1, 1, Paint.glowing(0xF4F06A)).box(-6, 0, 5, 12, 1, 1, Paint.glowing(0xF4F06A))
                .box(-6, 0, -5, 1, 1, 10, Paint.glowing(0xF4F06A)).box(5, 0, -5, 1, 1, 10, Paint.glowing(0xF4F06A));
    }

    private static void venom(Part yaw) {
        yaw.box(-5, -6, -5, 10, 6, 10, Paint.of(0x3A3A2A).pattern(Pattern.PLATES, 0x5A5A3A));
        yaw.child("brew", 0, -6, 0).box(-4, -0.5f, -4, 8, 1, 8, Paint.glowing(0x7BD940).pattern(Pattern.SPOTS, 0xC8FF60));
        Part gun = yaw.child("gun", 0, -8, 2);
        gun.box(-1, -1, -10, 2, 2, 12, Paint.of(WOOD).pattern(Pattern.WOOD, 0x5A3A20));
        gun.child("bucket", 0, -1, -11).box(-2.5f, -3, -2.5f, 5, 3, 5, Paint.of(0x4A4A3A)).box(-2, -3.2f, -2, 4, 0.5f, 4, Paint.glowing(0x7BD940));
        yaw.child("pipes", 0, -6, 5).from(3).box(-4, -6, 0, 2, 6, 2, Paint.of(0x6A6A4A)).box(2, -8, 0, 2, 8, 2, Paint.of(0x6A6A4A));
        yaw.child("vials", 0, -3, -5.3f).from(5).box(-4, -2, 0, 2, 3, 1, Paint.glowing(0xC8FF60)).box(2, -2, 0, 2, 3, 1, Paint.glowing(0xC8FF60));
    }

    private static void mortar(Part yaw) {
        Paint sand = Paint.of(0xB8A070).pattern(Pattern.CLOTH, 0x8A7850);
        yaw.box(-6, -3, -6, 12, 3, 12, sand);
        yaw.box(-4, -5, -4, 8, 2, 8, Paint.of(DARKSTONE));
        Part gun = yaw.child("gun", 0, -6, 0).rot(-40, 0, 0);
        gun.box(-3, -3, -11, 6, 6, 13, Paint.of(0x4A4A50).pattern(Pattern.BANDS, 0x6A6A70));
        gun.child("muzzle", 0, 0, -11).box(-3.5f, -3.5f, -1, 7, 7, 1, Paint.of(0x2A2A2A));
        gun.child("barrel_b", 4.5f, 0, 0).from(3).box(-2, -2, -9, 4, 4, 10, Paint.of(0x4A4A50).pattern(Pattern.BANDS, 0x6A6A70));
        gun.child("barrel_c", -4.5f, 0, 0).from(5).box(-2, -2, -9, 4, 4, 10, Paint.of(0x4A4A50).pattern(Pattern.BANDS, 0x6A6A70));
        yaw.child("shells", 5, -3, 4).from(2).box(0, -3, 0, 2, 3, 2, Paint.of(0xC8A040)).box(-2.5f, -3, 0, 2, 3, 2, Paint.of(0xC8A040));
    }

    private static void prism(Part yaw) {
        Paint marble = Paint.of(0xE8E4F0).pattern(Pattern.CRACKS, 0xB8B0D0);
        yaw.box(-2.5f, -9, -2.5f, 5, 9, 5, marble.copy().face(Face.FRONT, DecalType.RUNE, 0xC86BFF, true));
        yaw.child("cap", 0, -9, 0).box(-3.5f, -1, -3.5f, 7, 1, 7, Paint.of(GOLD));
        Part gun = yaw.child("gun", 0, -15, 0);
        gun.child("gem", 0, 0, 0).rot(45, 0, 45).box(-3, -3, -3, 6, 6, 6, Paint.glowing(0xC86BFF).pattern(Pattern.CRACKS, 0xF0D0FF));
        Part rings = yaw.child("rings", 0, -15, 0);
        rings.child("ring1", 0, 0, 0).from(2).box(-6, -0.5f, -6, 12, 1, 1, Paint.of(GOLD)).box(-6, -0.5f, 5, 12, 1, 1, Paint.of(GOLD));
        rings.child("ring2", 0, 0, 0).from(4).box(-6, -6, -0.5f, 1, 12, 1, Paint.glowing(0xE0A0FF)).box(5, -6, -0.5f, 1, 12, 1, Paint.glowing(0xE0A0FF));
    }

    private static void gale(Part yaw) {
        Paint pale = Paint.of(0xD8E8E0).pattern(Pattern.BRICK, 0xA8B8B0);
        yaw.box(-3, -12, -3, 6, 12, 6, pale);
        yaw.child("roof", 0, -12, 0).box(-4, -2, -4, 8, 2, 8, Paint.of(0x4A7A8A).pattern(Pattern.SCALES, 0x2A5A6A));
        Part gun = yaw.child("gun", 0, -8, -3.5f);
        gun.box(-1, -1, -1, 2, 2, 2, Paint.of(IRON));
        Part fan = gun.child("fan", 0, 0, -1);
        for (int i = 0; i < 4; i++) fan.child("blade" + i, 0, 0, 0).rot(0, 0, 90 * i).box(-1, -9, -0.25f, 2, 8, 0.5f, Paint.of(0xF0F0E8).pattern(Pattern.STRIPES, 0xC8D8D0));
        Part fan2 = yaw.child("fan2", 0, -16, 0).from(3);
        for (int i = 0; i < 3; i++) fan2.child("vane" + i, 0, 0, 0).rot(0, 120 * i, 0).box(0, -0.25f, -0.75f, 7, 0.5f, 1.5f, Paint.glowing(0xCFFFF0));
    }

    private static void sanctum(Part yaw) {
        Paint marble = Paint.of(0xF0EEE8).pattern(Pattern.RUNES, 0xFFD966).noise(0.05f);
        yaw.box(-3, -18, -3, 6, 18, 6, marble);
        yaw.child("tip", 0, -18, 0).box(-2, -3, -2, 4, 3, 4, Paint.of(GOLD)).box(-1, -5, -1, 2, 2, 2, Paint.glowing(0xFFF2B0));
        Part gun = yaw.child("gun", 0, -24, 0);
        Paint halo = Paint.glowing(0xFFE38A);
        gun.box(-5, 0, -5, 10, 1, 1, halo).box(-5, 0, 4, 10, 1, 1, halo).box(-5, 0, -4, 1, 1, 8, halo).box(4, 0, -4, 1, 1, 8, halo);
        yaw.child("wing_r", -3, -14, 0).from(3).rot(0, 0, -25).box(-8, -1, -0.5f, 8, 6, 1, Paint.of(0xFFFFFF).pattern(Pattern.FEATHERS, 0xE8E0C8));
        yaw.child("wing_l", 3, -14, 0).from(3).rot(0, 0, 25).box(0, -1, -0.5f, 8, 6, 1, Paint.of(0xFFFFFF).pattern(Pattern.FEATHERS, 0xE8E0C8));
        Part halo2 = yaw.child("halo2", 0, -24, 0).from(5);
        halo2.box(-8, 0, -8, 16, 0.5f, 1, halo).box(-8, 0, 7, 16, 0.5f, 1, halo).box(-8, 0, -7, 1, 0.5f, 14, halo).box(7, 0, -7, 1, 0.5f, 14, halo);
    }

    // ------------------------------------------------------------------ animation

    public static void animate(ProcModel<TurretEntity> m, TurretEntity t, float ls, float la, float age, float hy, float hp, float pt) {
        ModelPart yaw = m.p("yaw"), gun = m.p("gun");
        float yawDeg = Mth.rotLerp(pt, t.clientYawO, t.clientYaw);
        float pitchDeg = Mth.lerp(pt, t.clientPitchO, t.clientPitch);
        yaw.yRot = Mth.wrapDegrees(yawDeg - Mth.rotLerp(pt, t.yBodyRotO, t.yBodyRot)) * Anim.D;
        int id = t.anim.id();
        float at = t.anim.time(pt);
        float recoil = id == 1 ? Math.max(0, 1 - at / 5f) : 0;
        switch (t.kind) {
            case FROST, TESLA, PRISM, SANCTUM -> {
            }
            case MORTAR -> gun.xRot += -pitchDeg * 0.3f * Anim.D;
            default -> gun.xRot += pitchDeg * Anim.D;
        }
        gun.z += recoil * 1.5f;
        if (id == 9) {
            float up = Anim.pulse(at, 0, 20);
            yaw.y -= up * 3;
            yaw.yRot += Anim.easeOut(Anim.win(at, 0, 20)) * Mth.TWO_PI;
        }
        ModelPart crown = m.opt("crown");
        if (crown != null) {
            crown.yRot = age * 0.03f;
            crown.y += Mth.sin(age * 0.07f) * 0.6f;
        }
        switch (t.kind) {
            case FROST -> {
                m.p("orbit").yRot = age * 0.06f;
                gun.yRot = age * 0.02f;
                gun.y += Mth.sin(age * 0.1f) * 0.6f - recoil;
            }
            case TESLA -> {
                gun.yRot = age * 0.05f;
                float f = 1f + recoil * 0.25f + Mth.sin(age * 0.6f) * 0.03f;
                gun.xScale = gun.yScale = gun.zScale = f;
                ModelPart ring = m.opt("tring");
                if (ring != null) {
                    ring.yRot = -age * 0.08f;
                    ring.y += Mth.sin(age * 0.1f) * 0.8f;
                }
            }
            case PRISM -> {
                gun.yRot = age * 0.08f;
                gun.y += Mth.sin(age * 0.08f) * 0.8f;
                m.p("rings").yRot = -age * 0.05f;
                m.p("rings").xRot = Mth.sin(age * 0.04f) * 0.4f;
            }
            case GALE -> {
                m.p("fan").zRot = age * (0.3f + recoil * 0.5f);
                ModelPart f2 = m.opt("fan2");
                if (f2 != null) f2.yRot = age * 0.25f;
            }
            case SANCTUM -> {
                gun.yRot = age * 0.04f;
                gun.y += Mth.sin(age * 0.06f) * 0.8f;
                ModelPart h2 = m.opt("halo2");
                if (h2 != null) h2.yRot = -age * 0.03f;
            }
            case VENOM -> {
                float sling = id == 1 ? Anim.swing(at, 1, 3, 10) : 0;
                gun.xRot += -0.8f * sling;
            }
            case MORTAR -> gun.z += recoil * 1.5f;
            case BALLISTA -> m.p("bolt").visible = recoil < 0.2f;
            default -> {
            }
        }
    }

    public static ProcRenderer<TurretEntity> renderer(EntityRendererProvider.Context ctx, TurretKind kind) {
        return new ProcRenderer<>(ctx, baked(kind), TurretModels::animate, 0.6f)
                .level(TurretEntity::turretLevel)
                .scaleBy(t -> TurretKind.SCALE[Mth.clamp(t.turretLevel(), 1, 5) - 1])
                .tint((t, c) -> {
                    if (t.hasEffect(com.lastbastion.registry.ModEffects.JAMMED.get())) {
                        c[0] = c[1] = c[2] = 0.55f;
                    }
                })
                .glowAlpha(t -> t.hasEffect(com.lastbastion.registry.ModEffects.JAMMED.get()) ? 0.2f : 1f);
    }
}
