package com.lastbastion.client.model;

import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.render.ProcRenderer;
import com.lastbastion.entity.boss.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/** Boss models: hand-built silhouettes with weapons, armour and phase-reactive animation. */
public final class BossModels {
    private static final Map<BossKind, ModelSpec.Baked> CACHE = new EnumMap<>(BossKind.class);
    private static ModelSpec.Baked crystal;

    private BossModels() {
    }

    public static ModelSpec.Baked baked(BossKind k) {
        return CACHE.computeIfAbsent(k, kind -> {
            ModelSpec s = new ModelSpec("boss_" + kind.id);
            switch (kind) {
                case GOREHOUND -> gorehound(s);
                case PLAGUE_MOTHER -> plagueMother(s);
                case MALGRAVE -> malgrave(s);
                case OSSUARY -> ossuary(s);
                case VARKHUL -> varkhul(s);
                case MORVATHRAX -> morvathrax(s);
            }
            return s.bake();
        });
    }

    // ------------------------------------------------------------------ Gorehound the Butcher

    private static void gorehound(ModelSpec s) {
        Paint flesh = Paint.of(0x9A7A6A).pattern(Pattern.CRACKS, 0x5A1A1A).noise(0.14f);
        Paint apron = Paint.of(0xC8C0B0).pattern(Pattern.SPOTS, 0x7A1010).noise(0.12f);
        Paint iron = Paint.of(0x6A6460).pattern(Pattern.PLATES, 0x9A928A);
        Part body = s.root.child("body", 0, -10, 0).rot(12, 0, 0).box(-11, 0, -7, 22, 22, 14, flesh);
        body.child("apron", 0, 6, -7.2f).box(-8, 0, 0, 16, 19, 0.6f, apron);
        body.child("belly", 0, 10, -6).box(-9, 0, -3, 18, 11, 4, flesh.copy().pattern(Pattern.CRACKS, 0x7A2A2A));
        body.child("chainbelt", 0, 18, 0).box(-11.5f, 0, -7.5f, 23, 2, 15, Paint.of(0x5A5A5A).pattern(Pattern.CHECKER, 0x3A3A3A));
        Part pr = body.child("paul_r", -11, 1, 0).box(-5, -3, -6, 8, 6, 12, iron);
        pr.child("spk_r1", -2, -3, -3).rot(0, 0, -20).box(-1, -5, -1, 2, 5, 2, Paint.of(0xB0A898));
        pr.child("spk_r2", -2, -3, 3).rot(0, 0, -20).box(-1, -6, -1, 2, 6, 2, Paint.of(0xB0A898));
        Part pl = body.child("paul_l", 11, 1, 0).box(-3, -3, -6, 8, 6, 12, iron);
        pl.child("spk_l1", 2, -3, 0).rot(0, 0, 20).box(-1, -6, -1, 2, 6, 2, Paint.of(0xB0A898));
        body.child("meathooks", 0, 4, 7).box(-6, 0, 0, 12, 10, 2, Paint.of(0x4A2A2A).pattern(Pattern.STRIPES, 0x8A5A3A));
        Part head = s.root.child("head", 0, -9, -5).box(-5, -9, -5, 10, 10, 10, Paint.of(0x8A6A5A).pattern(Pattern.CRACKS, 0x4A1010)
                .eyes(0xFF2010, true));
        head.child("gmask", 0, -3, -5.3f).box(-5, 0, 0, 10, 5, 1, Paint.of(0x5A5450).pattern(Pattern.PLATES, 0x8A847A)
                .face(Face.FRONT, DecalType.GRIN, 0xC0B8A8, false));
        head.child("stitches", 0, -8, -5.2f).box(-0.5f, 0, 0, 1, 5, 0.3f, Paint.of(0x2A1A10).pattern(Pattern.BANDS, 0xC0A080).flat());
        Part rarm = s.root.child("rarm", -14, -6, 0).box(-4, -2, -4, 8, 24, 8, flesh);
        Part larm = s.root.child("larm", 14, -6, 0).box(-4, -2, -4, 8, 24, 8, flesh);
        Part cleaver = rarm.child("cleaver", 0, 21, 0).rot(-80, 0, 0);
        cleaver.box(-1.5f, -3, -1.5f, 3, 10, 3, Paint.of(0x3A2A1A).pattern(Pattern.WOOD, 0x2A1A10));
        cleaver.box(-1, 6, -14, 2, 16, 18, Paint.of(0x8A8A90).pattern(Pattern.CRACKS, 0x5A1010).face(Face.RIGHT, DecalType.STRIPE, 0xC8C8D0, false));
        cleaver.box(-1.2f, 6, 3, 2.4f, 16, 2, Paint.of(0xC8C8D0));
        Part hook = larm.child("hook", 0, 21, 0);
        hook.box(-0.75f, 0, -0.75f, 1.5f, 8, 1.5f, Paint.of(0x7A7A7A).pattern(Pattern.CHECKER, 0x4A4A4A));
        hook.box(-0.75f, 8, -4, 1.5f, 1.5f, 5, Paint.of(0xA0A0A0));
        hook.box(-0.75f, 5, -4.5f, 1.5f, 4, 1.5f, Paint.of(0xA0A0A0));
        s.root.child("rleg", -6, 12, 0).box(-4.5f, 0, -4.5f, 9, 12, 9, Paint.of(0x4A3A30).pattern(Pattern.CLOTH, 0x3A2A20));
        s.root.child("lleg", 6, 12, 0).box(-4.5f, 0, -4.5f, 9, 12, 9, Paint.of(0x4A3A30).pattern(Pattern.CLOTH, 0x3A2A20));
    }

    // ------------------------------------------------------------------ Plague Mother

    private static void plagueMother(ModelSpec s) {
        Paint sac = Paint.of(0x7A9A50).pattern(Pattern.VEINS, 0xC8FF60).noise(0.2f);
        Paint chitin = Paint.of(0x4A5A30).pattern(Pattern.SCALES, 0x6A7A40);
        Paint leg = Paint.of(0x3A4A28).pattern(Pattern.BANDS, 0x5A6A38);
        Part body = s.root.child("body", 0, 8, 6);
        body.box(-14, -10, -2, 28, 20, 26, sac);
        body.child("sac_glow1", -8, -10, 8).box(-2, -1, -2, 4, 2, 4, Paint.glowing(0xC8FF60));
        body.child("sac_glow2", 6, -10, 14).box(-2.5f, -1, -2.5f, 5, 2, 5, Paint.glowing(0xC8FF60));
        body.child("sac_glow3", 14.2f, -2, 12).box(-1, -2, -2, 2, 4, 4, Paint.glowing(0xC8FF60));
        body.child("thorax", 0, 0, -4).box(-8, -6, -8, 16, 12, 10, chitin);
        Part torso = s.root.child("head", 0, 2, -8).rot(-20, 0, 0);
        torso.box(-5, -16, -3, 10, 16, 6, Paint.of(0x8AA070).pattern(Pattern.RIBS, 0x5A7040));
        Part face = torso.child("pm_face", 0, -16, 0).box(-4.5f, -9, -4.5f, 9, 9, 9, Paint.of(0x9AB080).pattern(Pattern.SPOTS, 0x5A7040)
                .eyes(0xC8FF60, true));
        face.child("pm_jaw", 0, -1, -2).box(-3.5f, 0, -3, 7, 4, 5, Paint.of(0x6A8050).face(Face.FRONT, DecalType.GRIN, 0xC8FF60, true));
        face.child("pm_crest", 0, -9, 2).rot(-30, 0, 0).box(-3, -6, -1, 6, 6, 2, chitin);
        torso.child("rarm", -6, -14, 0).box(-1.5f, 0, -1.5f, 3, 14, 3, Paint.of(0x8AA070));
        torso.child("larm", 6, -14, 0).box(-1.5f, 0, -1.5f, 3, 14, 3, Paint.of(0x8AA070));
        for (int i = 0; i < 3; i++) {
            for (int side = 0; side < 2; side++) {
                float x = side == 0 ? -10 : 10;
                float z = -6 + i * 9;
                String n = "leg" + i + (side == 0 ? "r" : "l");
                Part upper = s.root.child(n, x, 8, z).rot(0, side == 0 ? 20 - i * 20 : -20 + i * 20, side == 0 ? 50 : -50);
                upper.box(side == 0 ? -16 : 0, -1.5f, -1.5f, 16, 3, 3, leg);
                upper.child(n + "_low", side == 0 ? -15 : 15, 0, 0).rot(0, 0, side == 0 ? -100 : 100)
                        .box(side == 0 ? -18 : 0, -1, -1, 18, 2, 2, leg.copy().pattern(Pattern.GRADIENT, 0x1A2010));
            }
        }
    }

    // ------------------------------------------------------------------ Malgrave the Dread Knight

    private static void malgrave(ModelSpec s) {
        Paint plate = Paint.of(0x2A2830).pattern(Pattern.PLATES, 0x5A5068).noise(0.08f);
        Paint trim = Paint.of(0x6A1A2A).pattern(Pattern.RUNES, 0xB050FF);
        Rig r = Rig.humanoid(s, 1.5f, 4, plate, plate.copy().face(Face.FRONT, DecalType.SKULL, 0xB050FF, true), plate,
                Paint.of(0x1A1820).pattern(Pattern.PLATES, 0x4A4058).face(Face.FRONT, DecalType.VISOR, 0xB050FF, true));
        r.head.child("horn_r", -5, -11, 0).rot(0, 0, -30).box(-1.5f, -9, -1.5f, 3, 9, 3, Paint.of(0x1A1418).pattern(Pattern.BANDS, 0x3A2A30));
        r.head.child("horn_l", 5, -11, 0).rot(0, 0, 30).box(-1.5f, -9, -1.5f, 3, 9, 3, Paint.of(0x1A1418).pattern(Pattern.BANDS, 0x3A2A30));
        r.head.child("crown", 0, -12, 0).box(-6.5f, 0, -6.5f, 13, 2, 13, trim);
        Part pr = r.rarm.child("mpaul_r", 0, -3, 0).box(-5, -2, -5, 9, 6, 10, plate);
        pr.child("mspk_r", -3, -2, 0).rot(0, 0, -25).box(-1, -7, -1, 2, 7, 2, Paint.of(0x4A4058));
        Part pl = r.larm.child("mpaul_l", 0, -3, 0).box(-4, -2, -5, 9, 6, 10, plate);
        pl.child("mspk_l", 3, -2, 0).rot(0, 0, 25).box(-1, -7, -1, 2, 7, 2, Paint.of(0x4A4058));
        r.body.child("tabard", 0, 12, -3.2f).box(-4, 0, 0, 8, 12, 0.5f, trim.copy().face(Face.FRONT, DecalType.EMBLEM, 0xB050FF, true));
        Part cape = r.body.child("cape", 0, 0, 3.2f).rot(8, 0, 0);
        cape.box(-7, 0, 0, 14, 30, 1, Paint.of(0x5A0A14).pattern(Pattern.CLOTH, 0x3A0510).noise(0.15f));
        Part sword = r.rarm.child("greatsword", 0, 16, 0).rot(-90, 0, 0);
        sword.box(-1, -4, -1, 2, 8, 2, Paint.of(0x2A1A1A).pattern(Pattern.BANDS, 0x6A1A2A));
        sword.box(-4, 3, -1.5f, 8, 2, 3, trim);
        sword.child("blade", 0, 5, 0).box(-2.5f, 0, -0.75f, 5, 34, 1.5f, Paint.of(0x3A3A48).pattern(Pattern.VEINS, 0xB050FF).noise(0.06f));
        sword.child("blade_soul", 0, 5, 0).from(2).box(-2.5f, 0, -0.75f, 5, 34, 1.5f, 0.35f, Paint.glowing(0x50E8FF).pattern(Pattern.VEINS, 0xE0FFFF));
    }

    // ------------------------------------------------------------------ Ossuary Colossus

    private static void ossuary(ModelSpec s) {
        Paint bone = Paint.of(0xD8D0B8).pattern(Pattern.CRACKS, 0x9A9078).noise(0.1f);
        Paint dark = Paint.of(0xA89E86).pattern(Pattern.RIBS, 0x6A6050);
        Part body = s.root.child("body", 0, -22, 0).rot(10, 0, 0);
        body.box(-14, 0, -8, 28, 10, 16, bone);
        Part cage = body.child("ribcage", 0, 10, 0);
        cage.box(-12, 0, -7, 24, 16, 14, dark);
        cage.child("soulcore", 0, 6, 0).box(-4, -4, -4, 8, 8, 8, Paint.glowing(0x6BFF8A).face(Face.FRONT, DecalType.CORE, 0xE0FFE8, true));
        cage.child("rib_door_r", -12, 0, -7.2f).box(0, 0, 0, 12, 16, 1, dark);
        cage.child("rib_door_l", 12, 0, -7.2f).box(-12, 0, 0, 12, 16, 1, dark);
        body.child("spine", 0, 0, 7).box(-2, -4, 0, 4, 32, 4, Paint.of(0xC8C0A8).pattern(Pattern.BANDS, 0x8A8070));
        body.child("pelvis", 0, 26, 0).box(-11, 0, -6, 22, 6, 12, bone);
        Part head = s.root.child("head", 0, -22, -6).box(-7, -12, -7, 14, 12, 14, Paint.of(0xE0D8C0).pattern(Pattern.CRACKS, 0x8A8070)
                .face(Face.FRONT, DecalType.SKULL, 0x6BFF8A, true));
        head.child("ohorn_r", -7, -10, 0).rot(0, 0, -50).box(-2, -12, -2, 4, 12, 4, Paint.of(0x4A4038).pattern(Pattern.BANDS, 0x2A2420));
        head.child("ohorn_l", 7, -10, 0).rot(0, 0, 50).box(-2, -12, -2, 4, 12, 4, Paint.of(0x4A4038).pattern(Pattern.BANDS, 0x2A2420));
        head.child("ojaw", 0, -1, -2).box(-6, 0, -5, 12, 4, 10, Paint.of(0xD0C8B0).face(Face.FRONT, DecalType.GRIN, 0x2A2A20, false));
        Part rarm = s.root.child("rarm", -19, -18, 0).box(-5, -3, -5, 10, 26, 10, bone);
        Part larm = s.root.child("larm", 19, -18, 0).box(-5, -3, -5, 10, 26, 10, bone);
        rarm.child("club_r", 0, 23, 0).box(-7, 0, -7, 14, 14, 14, Paint.of(0xC0B8A0).pattern(Pattern.CRACKS, 0x6A6050));
        larm.child("club_l", 0, 23, 0).box(-7, 0, -7, 14, 14, 14, Paint.of(0xC0B8A0).pattern(Pattern.CRACKS, 0x6A6050));
        larm.child("boulder", 0, 34, 0).box(-6, -6, -6, 12, 12, 12, Paint.of(0x8A8270).pattern(Pattern.CRACKS, 0x5A5448));
        s.root.child("rleg", -8, 8, 0).box(-5, 0, -5, 10, 16, 10, bone);
        s.root.child("lleg", 8, 8, 0).box(-5, 0, -5, 10, 16, 10, bone);
    }

    // ------------------------------------------------------------------ Varkhul the Lich King

    private static void varkhul(ModelSpec s) {
        Paint robe = Paint.of(0x1A2A4A).pattern(Pattern.GRADIENT, 0x05080F).noise(0.12f);
        Paint frost = Paint.of(0x9FE8FF).pattern(Pattern.CRACKS, 0xE0FFFF).glow();
        Part body = s.root.child("body", 0, -16, 0).box(-6, 0, -3.5f, 12, 18, 7, robe.copy().face(Face.FRONT, DecalType.RUNE, 0x5AD8FF, true));
        Part skirt = body.child("skirt", 0, 18, 0);
        skirt.box(-7, 0, -4.5f, 14, 10, 9, robe).box(-5.5f, 10, -3.5f, 11, 8, 7, robe).box(-3.5f, 18, -2.5f, 7, 6, 5, robe);
        body.child("vk_paul_r", -6, 0, 0).box(-5, -2, -4, 6, 4, 8, Paint.of(0x5A6A8A).pattern(Pattern.PLATES, 0x9FB8D8));
        body.child("vk_paul_l", 6, 0, 0).box(-1, -2, -4, 6, 4, 8, Paint.of(0x5A6A8A).pattern(Pattern.PLATES, 0x9FB8D8));
        body.child("vk_spk_r", -9, -2, 0).rot(0, 0, -30).box(-1, -6, -1, 2, 6, 2, frost);
        body.child("vk_spk_l", 9, -2, 0).rot(0, 0, 30).box(-1, -6, -1, 2, 6, 2, frost);
        Part head = s.root.child("head", 0, -16, 0).box(-4, -9, -4, 8, 9, 8, Paint.of(0xC8D0D8).pattern(Pattern.CRACKS, 0x8A9098)
                .face(Face.FRONT, DecalType.SKULL, 0x5AD8FF, true));
        Part crown = head.child("icecrown", 0, -9, 0);
        crown.box(-4.5f, -1, -4.5f, 9, 2, 9, frost);
        crown.child("ic1", 0, -1, -4).box(-1, -6, -0.5f, 2, 6, 1, frost);
        crown.child("ic2", -3.5f, -1, -3.5f).rot(0, 45, -15).box(-0.5f, -4, -0.5f, 1, 4, 1, frost);
        crown.child("ic3", 3.5f, -1, -3.5f).rot(0, -45, 15).box(-0.5f, -4, -0.5f, 1, 4, 1, frost);
        crown.child("ic4", 0, -1, 4).box(-0.5f, -3, -0.5f, 1, 3, 1, frost);
        head.child("vk_hood", 0, 0, 1).box(-5, -10, -3, 10, 11, 8, Paint.of(0x10182A).pattern(Pattern.CLOTH, 0x080C14));
        Part rarm = s.root.child("rarm", -8, -14, 0).box(-2, -1, -2, 4, 15, 4, robe);
        Part larm = s.root.child("larm", 8, -14, 0).box(-2, -1, -2, 4, 15, 4, robe);
        rarm.child("vk_hand_r", 0, 14, 0).box(-1.5f, 0, -1.5f, 3, 3, 3, Paint.of(0xC8D0D8));
        larm.child("vk_hand_l", 0, 14, 0).box(-1.5f, 0, -1.5f, 3, 3, 3, Paint.of(0xC8D0D8));
        Part staff = rarm.child("vk_staff", 0, 15, -1).rot(-10, 0, 0);
        staff.box(-1, -26, -1, 2, 36, 2, Paint.of(0x5A6A8A).pattern(Pattern.BANDS, 0x9FB8D8));
        Part orb = staff.child("vk_orb", 0, -30, 0);
        orb.box(-3, -3, -3, 6, 6, 6, Paint.glowing(0x5AD8FF).face(Face.FRONT, DecalType.SKULL, 0x0A3A5A, false));
        orb.child("vk_cage", 0, 0, 0).box(-3.5f, -4, -3.5f, 7, 8, 7, 0.1f, Paint.of(0x5A6A8A).pattern(Pattern.RIBS, 0x9FB8D8));
        s.root.child("rleg", -2, 24, 0);
        s.root.child("lleg", 2, 24, 0);
    }

    // ------------------------------------------------------------------ Morvathrax the Rotwing

    private static void morvathrax(ModelSpec s) {
        Paint bone = Paint.of(0xC8C0A0).pattern(Pattern.CRACKS, 0x6A6048).noise(0.1f);
        Paint rot = Paint.of(0x4A5A3A).pattern(Pattern.VEINS, 0x6BFF8A).noise(0.18f);
        Paint membrane = Paint.of(0x3A4A30).pattern(Pattern.SPOTS, 0x1A2010).noise(0.25f);
        Part body = s.root.child("body", 0, 0, 0);
        body.box(-9, -6, -14, 18, 14, 30, rot);
        body.child("ribs", 0, -6, -10).box(-9.5f, 0, 0, 19, 14, 20, 0.3f, Paint.of(0xC8C0A0).pattern(Pattern.RIBS, 0x3A4A30));
        body.child("plaguecore", 0, 2, -4).box(-3, -3, -3, 6, 6, 6, Paint.glowing(0x6BFF8A));
        Part neck1 = s.root.child("neck1", 0, -2, -14).rot(-25, 0, 0).box(-4, -4, -10, 8, 8, 10, bone);
        Part neck2 = neck1.child("neck2", 0, 0, -10).rot(15, 0, 0).box(-3.5f, -3.5f, -10, 7, 7, 10, bone);
        Part head = neck2.child("head", 0, 0, -10).rot(15, 0, 0);
        head.box(-6, -5, -16, 12, 9, 16, Paint.of(0xD8D0B0).pattern(Pattern.CRACKS, 0x6A6048).face(Face.FRONT, DecalType.SKULL, 0x6BFF8A, true));
        head.child("mhorn_r", -5, -4, -2).rot(30, -20, 0).box(-1.5f, -1.5f, 0, 3, 3, 14, Paint.of(0x3A3020).pattern(Pattern.BANDS, 0x1A1810));
        head.child("mhorn_l", 5, -4, -2).rot(30, 20, 0).box(-1.5f, -1.5f, 0, 3, 3, 14, Paint.of(0x3A3020).pattern(Pattern.BANDS, 0x1A1810));
        head.child("mjaw", 0, 4, -2).box(-5, 0, -14, 10, 3, 14, Paint.of(0xC8C0A0).face(Face.TOP, DecalType.DOTS, 0x6BFF8A, true));
        head.child("meyes", 0, -3, -16.2f).box(-5, 0, 0, 10, 1.5f, 0.2f, Paint.glowing(0x6BFF8A).flat());
        for (int side = 0; side < 2; side++) {
            float sx = side == 0 ? -1 : 1;
            String n = side == 0 ? "r" : "l";
            Part wing = s.root.child("wing_" + n, sx * 9, -4, -4);
            wing.box(side == 0 ? -26 : 0, -2, -3, 26, 3, 5, bone);
            wing.box(side == 0 ? -26 : 0, -1, 2, 26, 1, 22, membrane);
            Part tip = wing.child("wingtip_" + n, sx * 26, 0, 0);
            tip.box(side == 0 ? -24 : 0, -1.5f, -2, 24, 2, 4, bone);
            tip.box(side == 0 ? -24 : 0, -0.5f, 2, 24, 1, 18, membrane);
        }
        Part tail = s.root.child("tail1", 0, 0, 16).box(-4, -4, 0, 8, 8, 12, bone);
        Part t2 = tail.child("tail2", 0, 0, 12).box(-3, -3, 0, 6, 6, 12, bone);
        Part t3 = t2.child("tail3", 0, 0, 12).box(-2, -2, 0, 4, 4, 12, bone);
        t3.child("tailspike", 0, 0, 12).box(-3, -0.5f, 0, 6, 1, 8, Paint.of(0x3A3020));
        for (int i = 0; i < 4; i++) {
            float x = i % 2 == 0 ? -8 : 8;
            float z = i < 2 ? -10 : 10;
            String n = "mleg" + i;
            Part l = s.root.child(n, x, 6, z).box(-2.5f, 0, -2.5f, 5, 12, 5, bone);
            l.child(n + "_claw", 0, 12, -1).box(-3, 0, -4, 6, 4, 7, Paint.of(0x3A3020).pattern(Pattern.STRIPES, 0x5A5040));
        }
        s.root.child("rarm", 0, 0, 0);
        s.root.child("larm", 0, 0, 0);
        s.root.child("rleg", 0, 0, 0);
        s.root.child("lleg", 0, 0, 0);
    }

    // ------------------------------------------------------------------ animation

    public static void animate(ProcModel<SiegeBoss> m, SiegeBoss b, float ls, float la, float age, float hy, float hp, float pt) {
        int id = b.anim.id();
        float t = b.anim.time(pt);
        switch (b.kind) {
            case GOREHOUND -> animGorehound(m, b, ls, la, age, hy, hp, id, t);
            case PLAGUE_MOTHER -> animPlague(m, b, ls, la, age, hy, hp, id, t);
            case MALGRAVE -> animMalgrave(m, b, ls, la, age, hy, hp, id, t);
            case OSSUARY -> animOssuary(m, b, ls, la, age, hy, hp, id, t);
            case VARKHUL -> animVarkhul(m, b, ls, la, age, hy, hp, id, t);
            case MORVATHRAX -> animMorvathrax(m, b, ls, la, age, hy, hp, id, t);
        }
    }

    private static void biped(ProcModel<SiegeBoss> m, float ls, float la, float amp) {
        Anim.walk(m.p("rleg"), m.p("lleg"), ls, la, amp);
        Anim.armsSwing(m.p("rarm"), m.p("larm"), ls, la, amp * 0.5f);
    }

    private static void animGorehound(ProcModel<SiegeBoss> m, SiegeBoss b, float ls, float la, float age, float hy, float hp, int id, float t) {
        ModelPart head = m.p("head"), body = m.p("body"), rarm = m.p("rarm"), larm = m.p("larm");
        biped(m, ls, la, 0.9f);
        Anim.look(head, hy * 0.6f, hp);
        Anim.breathe(body, age, 0.03f);
        rarm.xRot += -0.4f;
        larm.xRot += -0.2f;
        larm.zRot -= 0.15f;
        m.p("hook").xRot = Mth.sin(age * 0.1f) * 0.2f;
        boolean rage = b.phase() >= 2;
        if (rage) {
            head.x += Mth.sin(age * 1.7f) * 0.2f;
            body.xRot += 0.08f;
        }
        switch (id) {
            case SiegeBoss.ANIM_MELEE, SiegeBoss.ANIM_CORE -> {
                float s = Anim.swing(t, 14, 4, 10);
                rarm.xRot += -1.6f * s;
                body.yRot += 0.3f * s;
            }
            case GorehoundBoss.CLEAVE -> {
                float s1 = Anim.swing(t, 12, 3, 4), s2 = Anim.swing(t - 14, 12, 3, 4), s3 = Anim.swing(t - 28, 16, 4, 8);
                rarm.xRot += -1.7f * s1 - 1.7f * s3;
                rarm.yRot += 0.6f * s2;
                rarm.zRot += 1.2f * s2;
                body.yRot += 0.35f * s1 - 0.4f * s2;
                larm.xRot += -2.4f * s3;
                body.xRot += 0.2f * Math.max(0, -s3);
            }
            case GorehoundBoss.HOOK -> {
                float s = Anim.swing(t, 14, 3, 10);
                larm.xRot += -2.2f * s;
                larm.zRot -= 0.4f * s;
                body.yRot -= 0.3f * s;
            }
            case GorehoundBoss.RAMPAGE -> {
                float c = Anim.win(t, 0, 16);
                body.xRot += 0.5f * c;
                head.xRot -= 0.4f * c;
                rarm.xRot = -0.6f - 0.6f * c;
                larm.xRot = -0.6f - 0.6f * c;
            }
            case GorehoundBoss.ROAR -> {
                float r = Anim.pulse(t, 0, 40);
                head.xRot -= 0.8f * r;
                rarm.zRot += 1.2f * r;
                larm.zRot -= 1.2f * r;
                body.xRot -= 0.25f * r;
            }
            default -> {
            }
        }
    }

    private static void animPlague(ProcModel<SiegeBoss> m, SiegeBoss b, float ls, float la, float age, float hy, float hp, int id, float t) {
        ModelPart head = m.p("head"), body = m.p("body");
        float walk = ls * 0.9f;
        for (int i = 0; i < 3; i++) {
            for (int side = 0; side < 2; side++) {
                ModelPart leg = m.p("leg" + i + (side == 0 ? "r" : "l"));
                float phase = (i + side) % 2 == 0 ? 0 : Mth.PI;
                leg.yRot += Mth.cos(walk + phase) * 0.35f * la;
                leg.zRot += Math.abs(Mth.sin(walk + phase)) * 0.3f * la * (side == 0 ? -1 : 1);
            }
        }
        float pulse = 1f + Mth.sin(age * 0.08f) * 0.03f;
        if (id == PlagueMotherBoss.BROOD) pulse += Anim.pulse(t, 0, 24) * 0.2f;
        body.xScale = body.yScale = body.zScale = pulse;
        Anim.look(m.p("pm_face"), hy * 0.5f, hp * 0.5f);
        m.p("pm_jaw").xRot += 0.1f + Mth.sin(age * 0.2f) * 0.05f;
        m.p("rarm").xRot += -0.6f + Mth.sin(age * 0.1f) * 0.2f;
        m.p("larm").xRot += -0.6f - Mth.sin(age * 0.1f) * 0.2f;
        switch (id) {
            case PlagueMotherBoss.SPEW -> {
                float o = Anim.win(t, 6, 8) * (1 - Anim.win(t, 52, 8));
                head.xRot += 0.4f * o;
                m.p("pm_jaw").xRot += 0.9f * o;
                head.yRot += Mth.sin((t - 12) / 40f * Mth.TWO_PI) * 0.4f * o;
            }
            case PlagueMotherBoss.RAIN -> {
                float r = Anim.pulse(t, 0, 30);
                m.p("rarm").xRot += -2.4f * r;
                m.p("larm").xRot += -2.4f * r;
                head.xRot -= 0.5f * r;
            }
            case PlagueMotherBoss.LEAP -> {
                float c = Anim.win(t, 0, 10);
                for (int i = 0; i < 3; i++) {
                    m.p("leg" + i + "r").zRot += 0.5f * c;
                    m.p("leg" + i + "l").zRot -= 0.5f * c;
                }
            }
            default -> {
            }
        }
    }

    private static void animMalgrave(ProcModel<SiegeBoss> m, SiegeBoss b, float ls, float la, float age, float hy, float hp, int id, float t) {
        ModelPart head = m.p("head"), body = m.p("body"), rarm = m.p("rarm"), larm = m.p("larm");
        biped(m, ls, la, 1.0f);
        Anim.look(head, hy, hp);
        rarm.xRot += -0.5f;
        rarm.yRot += -0.2f;
        larm.xRot += -0.4f;
        larm.yRot += 0.6f;
        larm.zRot -= 0.2f;
        m.p("cape").xRot += 0.1f + la * 0.6f + Mth.sin(age * 0.12f) * 0.05f;
        switch (id) {
            case SiegeBoss.ANIM_MELEE, SiegeBoss.ANIM_CORE -> {
                float s = Anim.swing(t, 14, 4, 10);
                rarm.xRot += -1.5f * s;
                larm.xRot += -1.5f * s;
            }
            case MalgraveBoss.COMBO -> {
                float a = Anim.swing(t, 10, 3, 3), c = Anim.swing(t - 13, 9, 3, 3), o = Anim.swing(t - 26, 14, 4, 10);
                rarm.yRot += 1.1f * a - 1.1f * c;
                rarm.zRot += 0.9f * a;
                larm.yRot += 1.1f * a - 1.1f * c;
                body.yRot += 0.45f * a - 0.45f * c;
                rarm.xRot += -2.0f * o;
                larm.xRot += -2.0f * o;
                body.xRot += 0.25f * Math.max(0, -o);
            }
            case MalgraveBoss.CHARGE -> {
                float c = Anim.win(t, 0, 12);
                body.xRot += 0.5f * c;
                rarm.xRot = -1.4f;
                larm.xRot = -1.4f;
                head.xRot -= 0.3f * c;
            }
            case MalgraveBoss.WAVE -> {
                float s = Anim.swing(t, 18, 3, 20);
                rarm.xRot += -2.4f * s;
                larm.xRot += -2.4f * s;
                body.xRot += 0.35f * Math.max(0, -s);
            }
            case MalgraveBoss.SUMMON, MalgraveBoss.ERUPTION -> {
                float r = Anim.pulse(t, 0, 36);
                rarm.xRot = -2.9f * r - 0.5f * (1 - r);
                larm.xRot = -2.9f * r - 0.4f * (1 - r);
                head.xRot -= 0.4f * r;
            }
            default -> {
            }
        }
    }

    private static void animOssuary(ProcModel<SiegeBoss> m, SiegeBoss b, float ls, float la, float age, float hy, float hp, int id, float t) {
        ModelPart head = m.p("head"), body = m.p("body"), rarm = m.p("rarm"), larm = m.p("larm");
        biped(m, ls * 0.6f, la, 0.6f);
        Anim.look(head, hy * 0.5f, hp * 0.5f);
        m.p("ojaw").xRot += 0.1f + Mth.sin(age * 0.07f) * 0.08f;
        body.zRot += Mth.sin(ls * 0.4f) * 0.05f * la;
        m.p("boulder").visible = id == OssuaryBoss.BOULDER && t < 26;
        float core = 1f + Mth.sin(age * 0.2f) * 0.1f;
        m.p("soulcore").xScale = m.p("soulcore").yScale = m.p("soulcore").zScale = core;
        boolean open = b.phase() >= 2;
        m.p("rib_door_r").yRot = open ? -1.2f : 0;
        m.p("rib_door_l").yRot = open ? 1.2f : 0;
        switch (id) {
            case SiegeBoss.ANIM_MELEE, SiegeBoss.ANIM_CORE, OssuaryBoss.SMASH -> {
                float len = id == OssuaryBoss.SMASH ? 30 : 20;
                float s = Anim.swing(t, len, 4, 14);
                rarm.xRot += -2.2f * s;
                larm.xRot += -2.2f * s;
                body.xRot += 0.3f * Math.max(0, -s);
            }
            case OssuaryBoss.BOULDER -> {
                float s = Anim.swing(t, 24, 3, 12);
                larm.xRot += -2.8f * Math.max(0, s) + 0.6f * Math.min(0, s);
                body.yRot += 0.4f * s;
            }
            case OssuaryBoss.STOMP -> {
                float lift = Anim.pulse(t, 0, 24);
                m.p("rleg").xRot -= 1.2f * lift;
                body.xRot -= 0.15f * lift;
            }
            case OssuaryBoss.SPIKES -> {
                float s = Anim.swing(t, 14, 3, 10);
                rarm.xRot += -2.2f * s;
            }
            case OssuaryBoss.RIBCAGE -> {
                float r = Anim.pulse(t, 0, 40);
                m.p("rib_door_r").yRot = -1.4f * Math.max(r, open ? 1 : 0);
                m.p("rib_door_l").yRot = 1.4f * Math.max(r, open ? 1 : 0);
                rarm.zRot += 0.8f * r;
                larm.zRot -= 0.8f * r;
                head.xRot -= 0.5f * r;
            }
            default -> {
            }
        }
    }

    private static void animVarkhul(ProcModel<SiegeBoss> m, SiegeBoss b, float ls, float la, float age, float hy, float hp, int id, float t) {
        ModelPart head = m.p("head"), body = m.p("body"), rarm = m.p("rarm"), larm = m.p("larm");
        Anim.look(head, hy, hp);
        float bob = Mth.sin(age * 0.08f) * 1.5f;
        for (ModelPart p : new ModelPart[]{head, body, rarm, larm}) p.y += bob;
        m.p("skirt").xRot += 0.1f + la * 0.5f + Mth.sin(age * 0.15f) * 0.06f;
        m.p("vk_orb").yRot = age * 0.1f;
        rarm.xRot += -0.4f;
        larm.xRot += -0.5f + Mth.sin(age * 0.1f) * 0.1f;
        larm.zRot -= 0.3f;
        boolean warded = b instanceof VarkhulBoss v && v.warded();
        if (warded) {
            rarm.xRot = -2.6f;
            larm.xRot = -2.6f;
            head.xRot -= 0.3f;
        }
        switch (id) {
            case VarkhulBoss.VOLLEY -> {
                float r = Anim.pulse(t, 4, 24);
                rarm.xRot += -1.2f * r;
                larm.xRot += -1.0f * r;
            }
            case VarkhulBoss.NOVA -> {
                float r = Anim.pulse(t, 0, 28);
                rarm.zRot += 1.4f * r;
                larm.zRot -= 1.4f * r;
                body.xRot -= 0.2f * r;
            }
            case VarkhulBoss.COIL -> {
                float r = Anim.win(t, 0, 8) * (1 - Anim.win(t, 38, 6));
                larm.xRot = -1.6f * r - 0.5f * (1 - r);
            }
            case VarkhulBoss.WARD, VarkhulBoss.ARMY -> {
                float r = Anim.pulse(t, 0, 40);
                rarm.xRot = -3f * r - 0.4f * (1 - r);
                larm.xRot = -3f * r - 0.4f * (1 - r);
                head.xRot -= 0.5f * r;
            }
            default -> {
            }
        }
    }

    private static void animMorvathrax(ProcModel<SiegeBoss> m, SiegeBoss b, float ls, float la, float age, float hy, float hp, int id, float t) {
        boolean air = b instanceof MorvathraxBoss mb && mb.airborne();
        float flap = air ? Mth.sin(age * 0.25f) : Mth.sin(age * 0.05f) * 0.2f;
        ModelPart wr = m.p("wing_r"), wl = m.p("wing_l");
        if (air) {
            wr.zRot += flap * 0.7f;
            wl.zRot -= flap * 0.7f;
            m.p("wingtip_r").zRot += flap * 0.5f;
            m.p("wingtip_l").zRot -= flap * 0.5f;
        } else {
            wr.zRot += -0.9f + flap;
            wl.zRot -= -0.9f + flap;
            wr.yRot += 0.6f;
            wl.yRot -= 0.6f;
            m.p("wingtip_r").zRot += 1.6f;
            m.p("wingtip_l").zRot -= 1.6f;
        }
        for (int i = 1; i <= 3; i++) m.p("tail" + i).yRot += Mth.sin(age * 0.1f - i * 0.6f) * 0.2f;
        for (int i = 0; i < 4; i++) {
            ModelPart l = m.p("mleg" + i);
            if (air) l.xRot += 0.9f;
            else l.xRot += Mth.cos(ls * 0.6f + (i % 2 == 0 ? 0 : Mth.PI) + (i < 2 ? 0 : Mth.PI)) * 0.6f * la;
        }
        ModelPart head = m.p("head"), jaw = m.p("mjaw");
        m.p("neck1").yRot += hy * 0.4f * Anim.D;
        head.xRot += hp * 0.5f * Anim.D;
        jaw.xRot += 0.08f + Mth.sin(age * 0.1f) * 0.05f;
        switch (id) {
            case MorvathraxBoss.STRAFE -> jaw.xRot += t > 40 && t < 80 ? 0.7f : 0;
            case MorvathraxBoss.BREATH -> jaw.xRot += Anim.win(t, 12, 6) * (1 - Anim.win(t, 62, 6)) * 0.8f;
            case MorvathraxBoss.DIVE -> {
                float d = Anim.win(t, 20, 10);
                m.p("body").xRot += 0.7f * d;
                wr.zRot = -0.3f * d + wr.zRot * (1 - d);
                wl.zRot = 0.3f * d + wl.zRot * (1 - d);
            }
            case MorvathraxBoss.TAIL -> m.p("tail1").yRot += Mth.sin(Anim.win(t, 8, 18) * Mth.TWO_PI) * 1.4f;
            case MorvathraxBoss.BUFFET -> {
                float r = Anim.pulse(t, 0, 24);
                wr.zRot += -1.2f * r;
                wl.zRot += 1.2f * r;
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ renderer

    public static ProcRenderer<SiegeBoss> renderer(EntityRendererProvider.Context ctx, BossKind kind) {
        float scale = switch (kind) {
            case GOREHOUND -> 1.25f;
            case PLAGUE_MOTHER -> 1.2f;
            case MALGRAVE -> 1.15f;
            case OSSUARY -> 1.65f;
            case VARKHUL -> 1.1f;
            case MORVATHRAX -> 1.6f;
        };
        ProcRenderer<SiegeBoss> r = new ProcRenderer<>(ctx, baked(kind), BossModels::animate, kind == BossKind.MORVATHRAX ? 3f : 1.5f);
        r.scale(scale).level(b -> b.phase());
        if (kind == BossKind.MALGRAVE) {
            r.tint((b, c) -> {
                if (b.phase() >= 2) {
                    c[0] = 0.85f;
                    c[1] = 0.95f;
                    c[2] = 1.1f;
                }
            });
        }
        if (kind == BossKind.GOREHOUND) {
            r.tint((b, c) -> {
                if (b.phase() >= 2) {
                    c[0] = 1.15f;
                    c[1] = 0.8f;
                    c[2] = 0.8f;
                }
            });
        }
        if (kind == BossKind.VARKHUL) {
            r.tint((b, c) -> {
                if (b instanceof VarkhulBoss v && v.warded()) c[2] = 1.25f;
            });
        }
        return r;
    }

    // ------------------------------------------------------------------ phylactery crystal

    public static ModelSpec.Baked crystal() {
        if (crystal == null) {
            ModelSpec s = new ModelSpec("phylactery");
            Part c = s.root.child("crystal", 0, 12, 0);
            Paint glow = Paint.glowing(0x5AD8FF).pattern(Pattern.CRACKS, 0xE0FFFF);
            c.box(-3, -8, -3, 6, 16, 6, glow);
            c.child("shard1", -4, 4, 0).rot(0, 0, 25).box(-1, -6, -1, 2, 6, 2, glow);
            c.child("shard2", 4, 4, 0).rot(0, 0, -25).box(-1, -6, -1, 2, 6, 2, glow);
            Part ring = s.root.child("ring", 0, 14, 0);
            ring.box(-6, 0, -6, 12, 1, 1, Paint.of(0x5A6A8A)).box(-6, 0, 5, 12, 1, 1, Paint.of(0x5A6A8A))
                    .box(-6, 0, -5, 1, 1, 10, Paint.of(0x5A6A8A)).box(5, 0, -5, 1, 1, 10, Paint.of(0x5A6A8A));
            s.root.child("rarm", 0, 0, 0);
            crystal = s.bake();
        }
        return crystal;
    }

    public static ProcRenderer<PhylacteryEntity> crystalRenderer(EntityRendererProvider.Context ctx) {
        return new ProcRenderer<PhylacteryEntity>(ctx, crystal(), (m, e, ls, la, age, hy, hp, pt) -> {
            m.p("crystal").yRot = age * 0.05f;
            m.p("crystal").y += Mth.sin(age * 0.1f) * 1.5f;
            m.p("ring").yRot = -age * 0.08f;
            m.p("ring").zRot = Mth.sin(age * 0.05f) * 0.3f;
        }, 0.3f);
    }
}
