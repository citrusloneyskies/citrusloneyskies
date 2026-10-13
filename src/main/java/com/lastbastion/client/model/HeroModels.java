package com.lastbastion.client.model;

import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.render.ProcRenderer;
import com.lastbastion.entity.hero.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/**
 * Hero models. Each hero is a bespoke build with gear that grows more ornate with hero level
 * (Lv3 adds signature ornaments, Lv5 adds radiant ascension details) and a full moveset of
 * keyframed skill animations.
 */
public final class HeroModels {
    private static final Map<HeroKind, ModelSpec.Baked> CACHE = new EnumMap<>(HeroKind.class);
    static final int GOLD = 0xE0B848, WHITE_PLATE = 0xE8E8F0, SKIN = 0xE0B898, SKIN_DARK = 0xB08060;

    private HeroModels() {
    }

    public static ModelSpec.Baked baked(HeroKind k) {
        return CACHE.computeIfAbsent(k, kind -> {
            ModelSpec s = new ModelSpec("hero_" + kind.id);
            switch (kind) {
                case ASHFANG -> ashfang(s);
                case SYLVA -> sylva(s);
                case BASTION -> bastion(s);
                case ALDRIC -> aldric(s);
                case MORGANA -> morgana(s);
                case KAGE -> kage(s);
                case AEGIS7 -> aegis(s);
                case IGNIS -> ignis(s);
                case VORTHAX -> vorthax(s);
                case ZEUS -> zeus(s);
            }
            return s.bake();
        });
    }

    static Paint face(int skin, int eye, boolean glowEyes) {
        return Paint.of(skin).noise(0.06f).eyes(eye, glowEyes).face(Face.BOTTOM, DecalType.MOUTH, 0x7A4A3A, false);
    }

    // ------------------------------------------------------------------ Ashfang

    private static void ashfang(ModelSpec s) {
        Paint fur = Paint.of(0x8A8F99).pattern(Pattern.FUR, 0x5A5F69).noise(0.18f);
        Paint dark = Paint.of(0x4A4E58).pattern(Pattern.FUR, 0x2A2E38).noise(0.18f);
        Part body = s.root.child("body", 0, 11, 2).box(-4.5f, -4, -9, 9, 8, 17, fur);
        body.child("mane", 0, 0, -6).box(-5.5f, -5, -4, 11, 10, 8, dark);
        body.child("ember_mane", 0, 0, -6).from(3).box(-5.5f, -5.2f, -4, 11, 1, 8, 0.25f, Paint.glowing(0xFF7A2A).pattern(Pattern.STRIPES, 0xFFC040));
        Part head = s.root.child("head", 0, 8, -9).box(-4, -4, -5, 8, 7, 6, fur.copy().eyes(0xFFA020, true));
        head.child("snout", 0, 0, -5).box(-2, -1, -4, 4, 3, 4, Paint.of(0x7A7F89).face(Face.FRONT, DecalType.DOTS, 0x1A1A1A, false));
        head.child("jaw", 0, 2, -5).box(-2, 0, -4, 4, 1.5f, 4, Paint.of(0x6A6F79).face(Face.TOP, DecalType.GRIN, 0xF0F0F0, false));
        head.child("ear_r", -2.5f, -4, -2).rot(0, 0, -15).box(-1, -3, -1, 2, 3, 2, dark);
        head.child("ear_l", 2.5f, -4, -2).rot(0, 0, 15).box(-1, -3, -1, 2, 3, 2, dark);
        String[] legs = {"fl_r", "fl_l", "bl_r", "bl_l"};
        float[][] pos = {{-2.5f, 14, -5}, {2.5f, 14, -5}, {-2.5f, 14, 7}, {2.5f, 14, 7}};
        for (int i = 0; i < 4; i++) {
            Part l = s.root.child(legs[i], pos[i][0], pos[i][1], pos[i][2]).box(-1.5f, 0, -1.5f, 3, 10, 3, fur);
            l.child(legs[i] + "_paw", 0, 9, -0.5f).box(-1.8f, 0, -2, 3.6f, 1, 3.5f, dark);
        }
        Part tail = s.root.child("tail", 0, 9, 9).rot(45, 0, 0).box(-1.5f, 0, -1.5f, 3, 11, 3, fur);
        tail.child("tail_fire", 0, 10, 0).from(5).box(-1.5f, 0, -1.5f, 3, 3, 3, Paint.glowing(0xFF7A2A).pattern(Pattern.GRADIENT, 0xFFE060));
        s.root.child("collar", 0, 7.5f, -5).from(2).box(-5, 0, -1, 10, 2, 3, Paint.of(0x6A3A1A).face(Face.FRONT, DecalType.EMBLEM, GOLD, false));
    }

    // ------------------------------------------------------------------ Sylva

    private static void sylva(ModelSpec s) {
        Paint leather = Paint.of(0x5A6A3A).pattern(Pattern.CLOTH, 0x3A4A2A);
        Rig r = Rig.humanoid(s, 0.95f, 3, Paint.of(SKIN), leather.copy().face(Face.FRONT, DecalType.STRIPE, 0x8A6A3A, false),
                Paint.of(0x3A4A2A).pattern(Pattern.CLOTH, 0x2A3A1A), face(SKIN, 0x40C080, false));
        Rig.shell(r.head, "hood", 8, 0.8f, 1.1f, Paint.of(0x2F6F3A).pattern(Pattern.CLOTH, 0x1F4F2A));
        r.head.child("hair", 0, -1, 3.5f).box(-3.5f, 0, 0, 7, 7, 1.5f, Paint.of(0xE8D8A0).pattern(Pattern.FUR, 0xC8B880));
        r.head.child("ear_r", -3.8f, -4, -1).rot(0, -30, -20).box(-2.5f, -1, -0.5f, 3, 1.5f, 1, Paint.of(SKIN));
        r.head.child("ear_l", 3.8f, -4, -1).rot(0, 30, 20).box(-0.5f, -1, -0.5f, 3, 1.5f, 1, Paint.of(SKIN));
        Part cloak = r.body.child("cloak", 0, 0, 2).rot(6, 0, 0);
        cloak.box(-4.5f, 0, 0, 9, 18, 1, Paint.of(0x2F6F3A).pattern(Pattern.CLOTH, 0x1F4F2A));
        Part quiver = r.body.child("quiver", 2, 1, 3).rot(0, 0, 25).box(-1.5f, 0, 0, 3, 10, 3, Paint.of(0x6A4A2A).pattern(Pattern.BANDS, 0x4A3A1A));
        quiver.child("arrows", 0, -2, 1.5f).box(-1, 0, -1, 2, 2, 2, Paint.of(0xE8E0D0).pattern(Pattern.CHECKER, 0xC03030));
        Part bow = r.larm.child("bow", 0, 9, -1.5f).rot(0, 90, 0);
        Paint wood = Paint.of(0x8A5A2A).pattern(Pattern.WOOD, 0x5A3A1A);
        bow.box(-0.75f, -5, -0.5f, 1.5f, 10, 1, wood);
        bow.child("bow_top", 0, -5, 0).rot(-30, 0, 0).box(-0.5f, -6, -0.5f, 1, 6, 1, wood);
        bow.child("bow_bot", 0, 5, 0).rot(30, 0, 0).box(-0.5f, 0, -0.5f, 1, 6, 1, wood);
        bow.child("bow_leaf", 0, 0, -0.6f).from(3).box(-1.5f, -2, 0, 3, 4, 0.2f, Paint.glowing(0x80FF90).flat());
        r.body.child("mantle", 0, 0, 0).from(5).box(-4.5f, -0.5f, -2.5f, 9, 3, 5, 0.3f, Paint.of(0x2F6F3A).pattern(Pattern.FEATHERS, 0x80FF90));
    }

    // ------------------------------------------------------------------ Bastion

    private static void bastion(ModelSpec s) {
        Paint stone = Paint.of(0x8C8577).pattern(Pattern.CRACKS, 0x5A554A).noise(0.15f);
        Paint moss = Paint.of(0x8C8577).pattern(Pattern.SPOTS, 0x4A7A3A).noise(0.15f);
        Part body = s.root.child("body", 0, -8, 0).rot(8, 0, 0).box(-11, 0, -7, 22, 18, 14, moss.copy().face(Face.FRONT, DecalType.CORE, 0xFFB030, true));
        body.child("shoulder_r", -11, 0, 0).box(-4, -4, -6, 8, 6, 12, stone);
        body.child("shoulder_l", 11, 0, 0).box(-4, -4, -6, 8, 6, 12, stone);
        body.child("crystals", 0, 0, 0).from(3)
                .box(-14, -9, -2, 3, 6, 3, Paint.glowing(0xFFB030)).box(11, -10, -1, 3, 7, 3, Paint.glowing(0xFFB030))
                .box(-3, -3, 6, 2, 5, 2, Paint.glowing(0xFFB030));
        body.child("runes", 0, 0, 0).from(5).box(-11, 0, -7, 22, 18, 14, 0.15f, Paint.of(0x8C8577).pattern(Pattern.VEINS, 0xFFD060).flat());
        Part head = s.root.child("head", 0, -8, -4).box(-4.5f, -7, -5, 9, 8, 9, stone.copy().eyes(0xFFB030, true));
        head.child("brow", 0, -6, -5).box(-5, 0, -1, 10, 2, 2, Paint.of(0x6A6458));
        Part rarm = s.root.child("rarm", -15, -5, 0).box(-4, -2, -4, 8, 22, 8, stone);
        Part larm = s.root.child("larm", 15, -5, 0).box(-4, -2, -4, 8, 22, 8, stone);
        rarm.child("fist_r", 0, 20, 0).box(-5, 0, -5, 10, 8, 10, moss);
        larm.child("fist_l", 0, 20, 0).box(-5, 0, -5, 10, 8, 10, moss);
        s.root.child("rleg", -6, 10, 0).box(-4.5f, 0, -4.5f, 9, 14, 9, stone);
        s.root.child("lleg", 6, 10, 0).box(-4.5f, 0, -4.5f, 9, 14, 9, stone);
    }

    // ------------------------------------------------------------------ Sir Aldric

    private static void aldric(ModelSpec s) {
        Paint plate = Paint.of(WHITE_PLATE).pattern(Pattern.PLATES, 0xB8B8C8).noise(0.06f);
        Paint gold = Paint.of(GOLD).pattern(Pattern.PLATES, 0xA08030);
        Rig r = Rig.humanoid(s, 1.05f, 4, plate, plate.copy().face(Face.FRONT, DecalType.EMBLEM, GOLD, false), plate,
                Paint.of(0xD8D8E0).pattern(Pattern.PLATES, 0xA8A8B8).face(Face.FRONT, DecalType.VISOR, 0xFFE38A, true));
        r.head.child("crest", 0, -8.4f, 0).box(-0.75f, -3, -4.5f, 1.5f, 3, 9, gold);
        r.head.child("plume", 0, -11, 2).rot(-20, 0, 0).box(-0.75f, -2, 0, 1.5f, 3, 6, Paint.of(0x2A5ADA).pattern(Pattern.FUR, 0x1A3A9A));
        r.body.child("tabard", 0, 6, -2.4f).box(-3.5f, 0, 0, 7, 12, 0.4f, Paint.of(0x2A5ADA).face(Face.FRONT, DecalType.EMBLEM, GOLD, false));
        r.rarm.child("apaul_r", 0, -1, 0).box(-3, -3, -3, 6, 4, 6, 0.1f, gold);
        r.larm.child("apaul_l", 0, -1, 0).box(-3, -3, -3, 6, 4, 6, 0.1f, gold);
        Part cape = r.body.child("cape", 0, 0, 2.3f).rot(6, 0, 0);
        cape.box(-5, 0, 0, 10, 22, 0.8f, Paint.of(0x2A5ADA).pattern(Pattern.CLOTH, 0x1A3A9A));
        r.body.child("backshield", 0, 3, 3).box(-4, 0, 0, 8, 9, 1, Paint.of(WHITE_PLATE).face(Face.BACK, DecalType.EMBLEM, GOLD, false));
        Part sword = r.rarm.child("sword", 0, 10, 0).rot(-90, 0, 0);
        sword.box(-0.75f, -3, -0.75f, 1.5f, 5, 1.5f, Paint.of(0x5A3A2A).pattern(Pattern.BANDS, GOLD));
        sword.box(-3.5f, 1.5f, -1, 7, 1.5f, 2, gold);
        sword.child("dawnblade", 0, 3, 0).box(-1.25f, 0, -0.5f, 2.5f, 20, 1, Paint.glowing(0xFFF2B0).pattern(Pattern.GRADIENT, 0xFFD050));
        Part wings = r.body.child("lightwings", 0, 2, 2.5f).from(3);
        wings.child("lw_r", -1, 0, 0).rot(0, 25, -20).box(-14, -2, 0, 14, 12, 0.5f, Paint.glowing(0xFFE38A).pattern(Pattern.FEATHERS, 0xFFF8D0));
        wings.child("lw_l", 1, 0, 0).rot(0, -25, 20).box(0, -2, 0, 14, 12, 0.5f, Paint.glowing(0xFFE38A).pattern(Pattern.FEATHERS, 0xFFF8D0));
        Part halo = r.head.child("halo", 0, -13, 2).from(5).rot(-15, 0, 0);
        Paint hp = Paint.glowing(0xFFE38A);
        halo.box(-4, 0, -4, 8, 0.6f, 1, hp).box(-4, 0, 3, 8, 0.6f, 1, hp).box(-4, 0, -3, 1, 0.6f, 6, hp).box(3, 0, -3, 1, 0.6f, 6, hp);
    }

    // ------------------------------------------------------------------ Morgana

    private static void morgana(ModelSpec s) {
        Paint robe = Paint.of(0x5A2AA0).pattern(Pattern.GRADIENT, 0x1A0A3A).noise(0.1f);
        Rig r = Rig.humanoid(s, 0.98f, 3, Paint.of(0x4A2A8A), robe.copy().face(Face.FRONT, DecalType.RUNE, 0x80E0FF, true),
                Paint.of(0x2A1A5A), face(0xE8C8B0, 0x80E0FF, true));
        r.body.child("skirt", 0, 11, 0).box(-4.5f, 0, -2.5f, 9, 12, 5, robe);
        Rig.shell(r.head, "mhair", 8, 0.6f, 1.25f, Paint.of(0x1A1A2A).pattern(Pattern.FUR, 0x3A2A5A));
        r.head.child("circlet", 0, -6.2f, 0).box(-4.5f, 0, -4.5f, 9, 1, 9, Paint.of(GOLD).face(Face.FRONT, DecalType.EMBLEM, 0x80E0FF, true));
        r.head.child("hat", 0, -8, 0).from(3).box(-6, -1, -6, 12, 1, 12, Paint.of(0x3A1A7A)).box(-3, -6, -3, 6, 5, 6, Paint.of(0x3A1A7A))
                .box(-1.5f, -9, -1.5f, 3, 3, 3, Paint.of(0x3A1A7A).pattern(Pattern.RUNES, 0x80E0FF));
        Part staff = r.rarm.child("mstaff", 0, 10, 0).rot(-15, 0, 0);
        staff.box(-0.6f, -20, -0.6f, 1.2f, 30, 1.2f, Paint.of(0x3A2A1A).pattern(Pattern.WOOD, 0x1A1008));
        Part orb = staff.child("storm_orb", 0, -22, 0);
        orb.box(-2, -2, -2, 4, 4, 4, Paint.glowing(0x80E0FF).pattern(Pattern.CRACKS, 0xFFFFFF));
        orb.child("prongs", 0, 2, 0).box(-2.5f, -3, -0.4f, 0.8f, 4, 0.8f, Paint.of(GOLD)).box(1.7f, -3, -0.4f, 0.8f, 4, 0.8f, Paint.of(GOLD));
        Part runes = s.root.child("runes", 0, 8, 0);
        for (int i = 0; i < 3; i++) {
            double a = Mth.TWO_PI * i / 3;
            runes.child("rune" + i, (float) Math.cos(a) * 9, 0, (float) Math.sin(a) * 9).levels(i == 2 ? 3 : 1, 99)
                    .box(-1, -1, -1, 2, 2, 2, Paint.glowing(i == 1 ? 0xB080FF : 0x80E0FF));
        }
        r.body.child("mantle", 0, 0, 0).from(5).box(-5, -1, -3, 10, 4, 6, Paint.of(0x7A4AD0).pattern(Pattern.RUNES, 0x80E0FF));
    }

    // ------------------------------------------------------------------ Kage

    private static void kage(ModelSpec s) {
        Paint kimono = Paint.of(0x2A2A38).pattern(Pattern.CLOTH, 0x1A1A24);
        Rig r = Rig.humanoid(s, 1f, 4, kimono, kimono.copy().face(Face.FRONT, DecalType.STRIPE, 0x8A1020, false),
                Paint.of(0x1A1A24).pattern(Pattern.CLOTH, 0x2A2A38), face(SKIN, 0xFF2030, false));
        r.head.child("oni", 0, -4, -4.2f).box(-4, -4, 0, 8, 7, 0.6f, Paint.of(0xA01020).face(Face.FRONT, DecalType.GRIN, 0xF0E0D0, false)
                .face(Face.FRONT, DecalType.SLIT_EYES, 0xFF4040, true));
        Part hat = r.head.child("kasa", 0, -7.5f, 0);
        hat.box(-8, 0, -8, 16, 1, 16, Paint.of(0xC8A860).pattern(Pattern.WOOD, 0xA08840));
        hat.box(-4, -2, -4, 8, 2, 8, Paint.of(0xC8A860).pattern(Pattern.WOOD, 0xA08840));
        hat.box(-1.5f, -3, -1.5f, 3, 1, 3, Paint.of(0xB89850));
        r.body.child("sash", 0, 8, 0).box(-4.3f, 0, -2.3f, 8.6f, 2, 4.6f, Paint.of(0x8A1020));
        r.body.child("scabbard", 3.5f, 9, -1).rot(-60, 0, 10).box(-0.75f, 0, -0.75f, 1.5f, 16, 1.5f, Paint.of(0x1A1010).pattern(Pattern.BANDS, 0x8A1020));
        Part katana = r.rarm.child("katana", 0, 10, 0).rot(-90, 0, 0);
        katana.box(-0.6f, -4, -0.6f, 1.2f, 5, 1.2f, Paint.of(0x1A1A1A).pattern(Pattern.CHECKER, 0x8A1020));
        katana.box(-1.5f, 1, -1.5f, 3, 0.6f, 3, Paint.of(GOLD));
        katana.child("kblade", 0, 1.5f, 0).rot(4, 0, 0).box(-0.3f, 0, -0.9f, 0.6f, 18, 1.8f, Paint.of(0xD8D8E0).pattern(Pattern.GRADIENT, 0xFFFFFF));
        katana.child("kedge", 0, 1.5f, 0).from(3).rot(4, 0, 0).box(-0.35f, 0, 0.6f, 0.7f, 18, 0.4f, Paint.glowing(0xFF2040));
        Part scarf = r.head.child("scarf", 0, -1, 3).from(3).rot(20, 0, 0);
        scarf.box(-2, 0, 0, 4, 12, 0.5f, Paint.of(0xA01020).pattern(Pattern.CLOTH, 0x701018));
        r.body.child("shadow_aura", 0, 0, 0).from(5).box(-4, 0, -2, 8, 12, 4, 0.6f, Paint.glowing(0x6A1030).pattern(Pattern.VEINS, 0xFF2040));
    }

    // ------------------------------------------------------------------ AEGIS-7

    private static void aegis(ModelSpec s) {
        Paint hull = Paint.of(0x5B6E7A).pattern(Pattern.PLATES, 0x8AA0B0).noise(0.06f);
        Paint dark = Paint.of(0x2A3238).pattern(Pattern.PLATES, 0x4A5A64);
        Paint hazard = Paint.of(0xE8B020).pattern(Pattern.STRIPES, 0x2A2A2A);
        s.root.child("hips", 0, 0, 0).box(-7, -3, -5, 14, 5, 10, dark);
        Part body = s.root.child("body", 0, -2, 0).box(-10, -16, -7, 20, 16, 14, hull.copy().face(Face.FRONT, DecalType.VISOR, 0x40E0FF, true));
        body.child("reactor", 0, -8, -7.2f).from(5).box(-2.5f, -2.5f, 0, 5, 5, 0.5f, Paint.glowing(0x40E0FF).flat());
        body.child("vents", 0, -4, 7).box(-6, -8, 0, 12, 8, 1, Paint.of(0x2A2A2A).pattern(Pattern.RIBS, 0x5A5A5A));
        body.child("trim", 0, -16, 0).box(-10, -1, -7, 20, 1, 14, hazard);
        body.child("armor_plates", 0, 0, 0).from(3).box(-11, -15, -8, 22, 6, 2, hull).box(-11, -7, -8, 22, 4, 2, hull);
        Part head = s.root.child("head", 0, -18, -2).box(-4, -4, -4, 8, 4, 7, dark.copy().face(Face.FRONT, DecalType.VISOR, 0xFF3030, true));
        head.child("antenna", 3, -4, 2).box(-0.4f, -7, -0.4f, 0.8f, 7, 0.8f, Paint.of(0x8A8A8A)).box(-0.8f, -8, -0.8f, 1.6f, 1.6f, 1.6f, Paint.glowing(0xFF3030));
        Part pod = s.root.child("pod", 6, -18, 4);
        pod.box(-4, -5, -4, 8, 5, 8, hull.copy().face(Face.TOP, DecalType.DOTS, 0xFF5030, true));
        pod.child("pod_lid", 0, -5, -4).box(-4, -1, 0, 8, 1, 8, dark);
        for (int side = 0; side < 2; side++) {
            String n = side == 0 ? "r" : "l";
            float sx = side == 0 ? -1 : 1;
            Part arm = s.root.child(side == 0 ? "rarm" : "larm", sx * 13, -14, 0).box(-3.5f, -3.5f, -4.5f, 7, 7, 9, hull);
            arm.child("shield_" + n, sx * 2, -3, 0).from(5).box(-3, -2, -6, 6, 3, 12, hazard);
            Part gun = arm.child("gun_" + n, 0, 4, -2);
            gun.box(-3, -3, -6, 6, 6, 10, dark);
            Part barrels = gun.child("barrels_" + n, 0, 0, -6);
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 2 * i;
                barrels.box((float) Math.cos(a) * 1.5f - 0.7f, (float) Math.sin(a) * 1.5f - 0.7f, -12, 1.4f, 1.4f, 12, Paint.of(0x3A3A3A).pattern(Pattern.BANDS, 0x6A6A6A));
            }
            barrels.child("muzzle_" + n, 0, 0, -12).box(-2.4f, -2.4f, -1, 4.8f, 4.8f, 1, Paint.of(0x1A1A1A));
            Part thigh = s.root.child(side == 0 ? "rleg" : "lleg", sx * 5, 1, 0).rot(-25, 0, 0).box(-3, 0, -3, 6, 13, 6, hull);
            Part shin = thigh.child("shin_" + n, 0, 13, 0).rot(50, 0, 0).box(-2.5f, 0, -2.5f, 5, 12, 5, dark);
            shin.child("foot_" + n, 0, 12, 0).rot(-25, 0, 0).box(-3.5f, 0, -6, 7, 3, 9, hull);
        }
    }

    // ------------------------------------------------------------------ Ignis

    private static void ignis(ModelSpec s) {
        Paint feathers = Paint.glowing(0xFF7A1A).pattern(Pattern.FEATHERS, 0xFFC040);
        Paint body = Paint.of(0xE05A10).pattern(Pattern.FEATHERS, 0xFF9A30);
        Part b = s.root.child("body", 0, 12, 0).rot(-10, 0, 0).box(-3.5f, -3.5f, -7, 7, 7, 13, body);
        Part head = s.root.child("head", 0, 8, -7).box(-2.5f, -4, -4, 5, 5, 5, Paint.of(0xF08020).pattern(Pattern.FEATHERS, 0xFFC040)
                .eyes(0xFFFFA0, true));
        head.child("beak", 0, -1.5f, -4).box(-1, -0.5f, -3, 2, 2, 3, Paint.of(0xFFD040));
        Part crest = head.child("crest", 0, -4, 0);
        for (int i = 0; i < 3; i++) crest.child("cr" + i, 0, 0, i * 1.5f - 1).rot(-30 - i * 15, 0, 0).box(-0.5f, -5 + i, -0.5f, 1, 5 - i, 1, feathers);
        for (int side = 0; side < 2; side++) {
            String n = side == 0 ? "r" : "l";
            float sx = side == 0 ? -1 : 1;
            Part w = s.root.child("wing_" + n, sx * 3.5f, 10, -2);
            w.box(side == 0 ? -14 : 0, -0.5f, -4, 14, 1, 10, feathers);
            Part tip = w.child("wingtip_" + n, sx * 14, 0, 0);
            tip.box(side == 0 ? -12 : 0, -0.5f, -3, 12, 1, 9, Paint.glowing(0xFFB030).pattern(Pattern.FEATHERS, 0xFFF080));
        }
        Part tail = s.root.child("tail", 0, 12, 6).rot(-15, 0, 0);
        for (int i = 0; i < 3; i++) tail.child("tf" + i, (i - 1) * 1.8f, 0, 0).rot(0, (i - 1) * 15, 0).box(-0.75f, -0.5f, 0, 1.5f, 1, 16 - Math.abs(i - 1) * 4, feathers);
        s.root.child("legs", 0, 15, 0).box(-2, 0, -1, 1, 4, 1, Paint.of(0x8A4A10)).box(1, 0, -1, 1, 4, 1, Paint.of(0x8A4A10));
        s.root.child("egg", 0, 18, 0).box(-5, -10, -5, 10, 14, 10, Paint.glowing(0xFF9A30).pattern(Pattern.CRACKS, 0xFFF0A0));
        s.root.child("plumes", 0, 12, 0).from(5).box(-4, -4.5f, -6, 8, 1, 12, Paint.glowing(0x60C0FF).pattern(Pattern.FEATHERS, 0xC0F0FF));
    }

    // ------------------------------------------------------------------ Vorthax

    private static void vorthax(ModelSpec s) {
        Paint scales = Paint.of(0xA01818).pattern(Pattern.SCALES, 0x6A0A0A).noise(0.1f);
        Paint belly = Paint.of(0xD8A040).pattern(Pattern.RIBS, 0xA87020);
        Paint horn = Paint.of(0x1A1418).pattern(Pattern.BANDS, 0x3A2A30);
        Paint membrane = Paint.of(0x7A1010).pattern(Pattern.VEINS, 0xC03020).noise(0.15f);
        Part body = s.root.child("body", 0, 2, 0).box(-8, -6, -13, 16, 13, 28, scales);
        body.child("belly", 0, 6.5f, 0).box(-6, 0, -12, 12, 1, 26, belly);
        for (int i = 0; i < 5; i++) body.child("spine" + i, 0, -6, -10 + i * 6).box(-0.75f, -4, -1, 1.5f, 4, 2, horn);
        Part n1 = s.root.child("neck1", 0, 0, -13).rot(-30, 0, 0).box(-3.5f, -3.5f, -9, 7, 7, 9, scales);
        Part n2 = n1.child("neck2", 0, 0, -9).rot(20, 0, 0).box(-3, -3, -9, 6, 6, 9, scales);
        Part head = n2.child("head", 0, 0, -9).rot(10, 0, 0);
        head.box(-5, -4, -13, 10, 8, 13, scales.copy().face(Face.FRONT, DecalType.SLIT_EYES, 0xFFC020, true));
        head.child("vjaw", 0, 4, -1).box(-4, 0, -12, 8, 2.5f, 12, Paint.of(0xA01818).face(Face.TOP, DecalType.GRIN, 0xF0F0F0, false));
        head.child("veyes", 0, -2, -13.1f).box(-4, 0, 0, 8, 1.2f, 0.1f, Paint.glowing(0xFFC020).flat());
        head.child("vhorn_r", -4, -4, -1).rot(35, -15, 0).box(-1, -1, 0, 2, 2, 12, horn);
        head.child("vhorn_l", 4, -4, -1).rot(35, 15, 0).box(-1, -1, 0, 2, 2, 12, horn);
        head.child("throat", 0, 1, -8).from(3).box(-2, 0, -4, 4, 3, 8, Paint.glowing(0xFF7A1A));
        for (int side = 0; side < 2; side++) {
            String n = side == 0 ? "r" : "l";
            float sx = side == 0 ? -1 : 1;
            Part wing = s.root.child("wing_" + n, sx * 8, -4, -6);
            wing.box(side == 0 ? -22 : 0, -1.5f, -2, 22, 3, 4, scales);
            wing.box(side == 0 ? -22 : 0, -0.5f, 2, 22, 1, 20, membrane);
            Part tip = wing.child("wingtip_" + n, sx * 22, 0, 0);
            tip.box(side == 0 ? -20 : 0, -1, -1.5f, 20, 2, 3, scales);
            tip.box(side == 0 ? -20 : 0, -0.5f, 1.5f, 20, 1, 16, membrane);
        }
        Part t1 = s.root.child("tail1", 0, 0, 15).box(-3.5f, -3.5f, 0, 7, 7, 12, scales);
        Part t2 = t1.child("tail2", 0, 0, 12).box(-2.5f, -2.5f, 0, 5, 5, 12, scales);
        Part t3 = t2.child("tail3", 0, 0, 12).box(-1.5f, -1.5f, 0, 3, 3, 10, scales);
        t3.child("tailblade", 0, 0, 10).box(-3, -0.5f, 0, 6, 1, 6, horn);
        for (int i = 0; i < 4; i++) {
            float x = i % 2 == 0 ? -7 : 7, z = i < 2 ? -8 : 9;
            Part l = s.root.child("vleg" + i, x, 7, z).box(-2.5f, 0, -2.5f, 5, 11, 5, scales);
            l.child("vleg" + i + "_claw", 0, 11, -1).box(-3, 0, -4, 6, 3, 7, horn);
        }
        s.root.child("crownfire", 0, -4, -25).from(5);
    }

    // ------------------------------------------------------------------ Zeus

    private static void zeus(ModelSpec s) {
        Paint skin = Paint.of(0xE8C8A0).noise(0.05f);
        Paint toga = Paint.of(0xF4F2EA).pattern(Pattern.CLOTH, 0xD8D4C8);
        Rig r = Rig.humanoid(s, 1.3f, 4, skin, toga.copy().face(Face.FRONT, DecalType.STRIPE, GOLD, false), toga,
                Paint.of(0xE8C8A0).eyes(0x9FE8FF, true));
        r.body.child("belt", 0, 9, 0).box(-5.5f, 0, -3, 11, 2, 6, Paint.of(GOLD).face(Face.FRONT, DecalType.EMBLEM, 0x9FE8FF, true));
        r.body.child("drape", 0, 0, -2.8f).rot(0, 0, 30).box(-2, -1, 0, 4, 18, 0.6f, toga);
        r.head.child("beard", 0, -3, -4.6f).box(-4.5f, 0, 0, 9, 7, 2, Paint.of(0xF0F0F0).pattern(Pattern.FUR, 0xC8C8C8));
        Rig.shell(r.head, "zhair", 10.4f, 0.7f, 0.9f, Paint.of(0xF0F0F0).pattern(Pattern.FUR, 0xC8C8C8));
        Part laurel = r.head.child("laurel", 0, -9, 0);
        laurel.box(-5.6f, 0, -5.6f, 11.2f, 1.2f, 11.2f, Paint.of(GOLD).pattern(Pattern.FEATHERS, 0x6A9A30));
        r.rarm.child("bracer_r", 0, 7, 0).box(-2.7f, 0, -2.7f, 5.4f, 3, 5.4f, Paint.of(GOLD));
        r.larm.child("bracer_l", 0, 7, 0).box(-2.7f, 0, -2.7f, 5.4f, 3, 5.4f, Paint.of(GOLD));
        Part bolt = r.rarm.child("bolt", 0, 13, 0).rot(-90, 0, 0);
        Paint light = Paint.glowing(0xF4F06A).pattern(Pattern.GRADIENT, 0xFFFFFF);
        bolt.box(-0.75f, -6, -0.75f, 1.5f, 7, 1.5f, light);
        bolt.child("bolt2", 0, 1, 0).rot(0, 0, 30).box(-0.75f, 0, -0.75f, 1.5f, 7, 1.5f, light);
        bolt.child("bolt3", 3.5f, 7, 0).rot(0, 0, -20).box(-0.75f, 0, -0.75f, 1.5f, 8, 1.5f, light);
        Part aegis = r.larm.child("aegis", 3, 6, 0).rot(0, 90, 0);
        aegis.box(-6, -6, -0.8f, 12, 12, 1.6f, Paint.of(GOLD).pattern(Pattern.PLATES, 0xB89030).face(Face.FRONT, DecalType.SKULL, 0x5A8A3A, false));
        aegis.box(-6.5f, -6.5f, -0.4f, 13, 13, 0.4f, Paint.of(0xC8A040));
        Part cape = r.body.child("zcape", 0, 0, 2.8f).rot(5, 0, 0);
        cape.box(-6, 0, 0, 12, 26, 0.8f, Paint.of(0x6A2A9A).pattern(Pattern.CLOTH, 0x4A1A7A));
        Part cloud = s.root.child("cloud", 0, 24, 0).from(3);
        cloud.box(-8, -2, -6, 16, 3, 12, Paint.of(0xB8C0C8).pattern(Pattern.SPOTS, 0x8A9098)).box(-5, -4, -4, 10, 2, 8, Paint.of(0xC8D0D8));
        Part halo = r.head.child("zhalo", 0, -8, 6).from(5);
        Paint hp = Paint.glowing(0xF4F06A);
        halo.box(-7, -7, 0, 14, 1, 0.5f, hp).box(-7, 6, 0, 14, 1, 0.5f, hp).box(-7, -6, 0, 1, 12, 0.5f, hp).box(6, -6, 0, 1, 12, 0.5f, hp);
    }

    // ------------------------------------------------------------------ animation

    public static void animate(ProcModel<HeroEntity> m, HeroEntity h, float ls, float la, float age, float hy, float hp, float pt) {
        int id = h.anim.id();
        float t = h.anim.time(pt);
        switch (h.kind) {
            case ASHFANG -> animWolf(m, h, ls, la, age, hy, hp, id, t);
            case SYLVA -> animSylva(m, h, ls, la, age, hy, hp, id, t);
            case BASTION -> animBastion(m, h, ls, la, age, hy, hp, id, t);
            case ALDRIC -> animAldric(m, h, ls, la, age, hy, hp, id, t);
            case MORGANA -> animMorgana(m, h, ls, la, age, hy, hp, id, t);
            case KAGE -> animKage(m, h, ls, la, age, hy, hp, id, t);
            case AEGIS7 -> animAegis(m, h, ls, la, age, hy, hp, id, t);
            case IGNIS -> animIgnis(m, h, ls, la, age, hy, hp, id, t);
            case VORTHAX -> animVorthax(m, h, ls, la, age, hy, hp, id, t);
            case ZEUS -> animZeus(m, h, ls, la, age, hy, hp, id, t);
        }
    }

    private static void humanoidBase(ProcModel<HeroEntity> m, float ls, float la, float age, float hy, float hp) {
        Anim.look(m.p("head"), hy, hp);
        Anim.walk(m.p("rleg"), m.p("lleg"), ls, la, 1.1f);
        Anim.armsSwing(m.p("rarm"), m.p("larm"), ls, la, 0.8f);
        m.p("rarm").zRot += Mth.cos(age * 0.09f) * 0.04f + 0.04f;
        m.p("larm").zRot -= Mth.cos(age * 0.09f) * 0.04f + 0.04f;
    }

    private static void animWolf(ProcModel<HeroEntity> m, HeroEntity h, float ls, float la, float age, float hy, float hp, int id, float t) {
        ModelPart head = m.p("head");
        Anim.look(head, hy, hp);
        float g = ls * 0.6662f;
        m.p("fl_r").xRot += Mth.cos(g) * 1.2f * la;
        m.p("fl_l").xRot += Mth.cos(g + Mth.PI) * 1.2f * la;
        m.p("bl_r").xRot += Mth.cos(g + Mth.PI) * 1.2f * la;
        m.p("bl_l").xRot += Mth.cos(g) * 1.2f * la;
        m.p("tail").yRot += Mth.sin(age * 0.3f) * 0.3f;
        m.p("tail").xRot += la * 0.5f;
        m.p("jaw").xRot += 0.05f + Mth.sin(age * 0.25f) * 0.04f;
        if (id >= 1 && id <= 3) {
            float b = Anim.pulse(t, 0, 10);
            head.z -= 2 * b;
            head.xRot += 0.3f * b;
            m.p("jaw").xRot += 0.6f * b;
        } else if (id == AshfangHero.POUNCE) {
            float c = Anim.win(t, 0, 6), air = Anim.win(t, 6, 10);
            m.p("body").xRot -= 0.3f * c * (1 - air);
            m.p("fl_r").xRot -= 1.2f * air;
            m.p("fl_l").xRot -= 1.2f * air;
            m.p("bl_r").xRot += 1.0f * air;
            m.p("bl_l").xRot += 1.0f * air;
            m.p("jaw").xRot += 0.7f * air;
        } else if (id == AshfangHero.HOWL) {
            float r = Anim.pulse(t, 4, 26);
            head.xRot -= 1.0f * r;
            m.p("jaw").xRot += 0.6f * r;
            m.p("body").xRot -= 0.25f * r;
        }
    }

    private static void animSylva(ProcModel<HeroEntity> m, HeroEntity h, float ls, float la, float age, float hy, float hp, int id, float t) {
        humanoidBase(m, ls, la, age, hy, hp);
        ModelPart rarm = m.p("rarm"), larm = m.p("larm");
        m.p("cloak").xRot += 0.08f + la * 0.5f + Mth.sin(age * 0.1f) * 0.04f;
        larm.xRot += -0.6f;
        larm.yRot += 0.2f;
        if (id >= 1 && id <= 3 || id == SylvaHero.PIERCE) {
            float draw = id == SylvaHero.PIERCE ? Anim.win(t, 0, 16) * (1 - Anim.win(t, 18, 3)) : Anim.win(t, 0, 8) * (1 - Anim.win(t, 10, 3));
            larm.xRot = -1.55f;
            larm.yRot = 0.1f;
            rarm.xRot = -1.55f;
            rarm.yRot = -0.1f - 0.6f * draw;
            m.p("head").yRot += 0.15f;
        } else if (id == SylvaHero.RAIN) {
            float up = Anim.win(t, 0, 10);
            larm.xRot = -2.5f * up;
            rarm.xRot = -2.5f * up;
            rarm.yRot = -0.4f * Anim.win(t, 6, 6);
            m.p("head").xRot -= 0.6f * up;
        } else if (id == SylvaHero.ROLL) {
            m.p("root").xRot = Anim.win(t, 0, 12) * Mth.TWO_PI;
        } else if (id == SylvaHero.MARK) {
            rarm.xRot = -1.6f * Anim.pulse(t, 0, 14);
        }
    }

    private static void animBastion(ProcModel<HeroEntity> m, HeroEntity h, float ls, float la, float age, float hy, float hp, int id, float t) {
        Anim.look(m.p("head"), hy * 0.6f, hp * 0.6f);
        Anim.walk(m.p("rleg"), m.p("lleg"), ls * 0.7f, la, 0.8f);
        ModelPart rarm = m.p("rarm"), larm = m.p("larm"), body = m.p("body");
        Anim.armsSwing(rarm, larm, ls * 0.7f, la, 0.4f);
        Anim.breathe(body, age, 0.02f);
        if (id >= 1 && id <= 3) {
            float s = Anim.swing(t, 12, 3, 8);
            ModelPart arm = id == 1 ? rarm : larm;
            arm.xRot += -1.6f * s;
            body.yRot += (id == 1 ? 0.3f : -0.3f) * s;
        } else if (id == BastionHero.TAUNT) {
            float beat = Mth.sin(t * 0.8f) * Anim.pulse(t, 0, 20);
            rarm.xRot -= 1.2f;
            larm.xRot -= 1.2f;
            rarm.zRot -= 0.9f + beat * 0.3f;
            larm.zRot += 0.9f + beat * 0.3f;
            m.p("head").xRot -= 0.4f * Anim.pulse(t, 0, 20);
        } else if (id == BastionHero.SHATTER) {
            float s = Anim.swing(t, 20, 3, 12);
            rarm.xRot += -2.6f * s;
            larm.xRot += -2.6f * s;
            body.xRot += 0.3f * Math.max(0, -s);
        } else if (id == BastionHero.STONESKIN) {
            float c = Anim.pulse(t, 0, 16);
            body.xRot += 0.3f * c;
            rarm.xRot -= 0.8f * c;
            larm.xRot -= 0.8f * c;
        }
    }

    private static void animAldric(ProcModel<HeroEntity> m, HeroEntity h, float ls, float la, float age, float hy, float hp, int id, float t) {
        humanoidBase(m, ls, la, age, hy, hp);
        ModelPart rarm = m.p("rarm"), larm = m.p("larm"), body = m.p("body");
        m.p("cape").xRot += 0.08f + la * 0.6f + Mth.sin(age * 0.11f) * 0.04f;
        rarm.xRot += -0.5f;
        ModelPart wings = m.opt("lightwings");
        if (wings != null) {
            m.p("lw_r").yRot += Mth.sin(age * 0.08f) * 0.15f;
            m.p("lw_l").yRot -= Mth.sin(age * 0.08f) * 0.15f;
        }
        ModelPart halo = m.opt("halo");
        if (halo != null) halo.y += Mth.sin(age * 0.1f) * 0.3f;
        switch (id) {
            case 1 -> {
                float s = Anim.swing(t, 6, 3, 5);
                rarm.xRot += -1.8f * s;
                rarm.zRot += 0.5f * s;
                body.yRot += 0.3f * s;
            }
            case 2 -> {
                float s = Anim.swing(t, 6, 3, 5);
                rarm.xRot += -1.0f;
                rarm.yRot += 1.4f * s;
                body.yRot -= 0.4f * s;
            }
            case 3 -> {
                float wind = Anim.win(t, 0, 6), thrust = Anim.win(t, 6, 3) * (1 - Anim.win(t, 10, 5));
                rarm.xRot = -0.8f - 0.8f * wind;
                rarm.yRot = 0.3f * wind;
                rarm.z -= 4 * thrust;
                body.xRot += 0.2f * thrust;
                m.p("rleg").xRot -= 0.5f * thrust;
            }
            case AldricHero.DASH -> {
                float c = Anim.win(t, 0, 6) * (1 - Anim.win(t, 16, 4));
                body.xRot += 0.45f * c;
                rarm.xRot = 0.6f * c - 0.5f * (1 - c);
                rarm.yRot = -0.6f * c;
                larm.xRot = 0.5f * c;
            }
            case AldricHero.JUDGEMENT -> {
                float up = Anim.win(t, 0, 10), down = Anim.win(t, 14, 6);
                rarm.xRot = -3.0f * up * (1 - down) - 1.0f * down;
                larm.xRot = -3.0f * up * (1 - down) - 1.0f * down;
                body.xRot += 0.3f * down;
            }
            case AldricHero.AEGIS -> {
                float r = Anim.pulse(t, 0, 26);
                rarm.xRot = -3.1f * r - 0.5f * (1 - r);
                larm.zRot -= 1.0f * r;
                m.p("head").xRot -= 0.4f * r;
            }
            default -> {
            }
        }
    }

    private static void animMorgana(ProcModel<HeroEntity> m, HeroEntity h, float ls, float la, float age, float hy, float hp, int id, float t) {
        humanoidBase(m, ls, la, age, hy, hp);
        ModelPart rarm = m.p("rarm"), larm = m.p("larm");
        m.p("runes").yRot = age * 0.06f;
        m.p("runes").y += Mth.sin(age * 0.08f);
        m.p("storm_orb").yRot = age * 0.1f;
        m.p("skirt").xRot += la * 0.3f;
        rarm.xRot += -0.4f;
        switch (id) {
            case 1, 2, 3 -> {
                float c = Anim.pulse(t, 0, 14);
                rarm.xRot -= 1.0f * c;
                larm.xRot -= 1.3f * c;
            }
            case MorganaHero.CHAIN -> {
                float c = Anim.pulse(t, 0, 22);
                rarm.xRot = -1.6f * c - 0.4f;
                larm.xRot = -1.6f * c;
                larm.yRot = 0.4f * c;
                rarm.yRot = -0.4f * c;
            }
            case MorganaHero.NOVA -> {
                float c = Anim.pulse(t, 0, 20);
                rarm.zRot += 1.4f * c;
                larm.zRot -= 1.4f * c;
                m.p("body").xRot -= 0.15f * c;
            }
            case MorganaHero.BLINK -> m.p("body").xRot += 0.4f * Anim.pulse(t, 0, 8);
            case MorganaHero.METEOR -> {
                float c = Anim.pulse(t, 0, 30);
                rarm.xRot = -3.0f * c - 0.4f * (1 - c);
                larm.xRot = -2.6f * c;
                m.p("head").xRot -= 0.6f * c;
            }
            default -> {
            }
        }
    }

    private static void animKage(ProcModel<HeroEntity> m, HeroEntity h, float ls, float la, float age, float hy, float hp, int id, float t) {
        humanoidBase(m, ls, la, age, hy, hp);
        ModelPart rarm = m.p("rarm"), larm = m.p("larm"), body = m.p("body");
        rarm.xRot += -0.6f;
        rarm.yRot += 0.2f;
        ModelPart scarf = m.opt("scarf");
        if (scarf != null) scarf.xRot += 0.3f + la * 0.8f + Mth.sin(age * 0.2f) * 0.1f;
        switch (id) {
            case 1 -> {
                float s = Anim.swing(t, 4, 2, 4);
                rarm.yRot += 1.6f * s;
                rarm.xRot -= 0.6f;
                body.yRot -= 0.4f * s;
            }
            case 2 -> {
                float s = Anim.swing(t, 4, 2, 4);
                rarm.yRot -= 1.6f * s;
                rarm.xRot -= 0.6f;
                body.yRot += 0.4f * s;
            }
            case 3 -> {
                float s = Anim.swing(t, 4, 2, 5);
                rarm.xRot += -2.2f * s;
            }
            case KageHero.IAIDO -> {
                float crouch = Anim.win(t, 0, 10) * (1 - Anim.win(t, 12, 2));
                float after = Anim.win(t, 12, 2) * (1 - Anim.win(t, 18, 4));
                body.xRot += 0.4f * crouch;
                rarm.xRot = 0.3f * crouch - 1.6f * after;
                rarm.yRot = -0.8f * crouch + 1.2f * after;
                m.p("rleg").xRot -= 0.6f * crouch;
            }
            case KageHero.CLONES -> {
                float c = Anim.pulse(t, 0, 16);
                rarm.xRot = -1.4f * c;
                larm.xRot = -1.4f * c;
                rarm.yRot = 0.5f * c;
                larm.yRot = -0.5f * c;
            }
            default -> {
            }
        }
    }

    private static void animAegis(ProcModel<HeroEntity> m, HeroEntity h, float ls, float la, float age, float hy, float hp, int id, float t) {
        Anim.look(m.p("head"), hy * 0.5f, hp * 0.3f);
        Anim.walk(m.p("rleg"), m.p("lleg"), ls * 0.8f, la, 0.6f);
        m.p("body").yRot += hy * 0.4f * Anim.D;
        m.p("body").y += Math.abs(Mth.sin(ls * 0.66f)) * la * 1.2f;
        ModelPart rarm = m.p("rarm"), larm = m.p("larm");
        rarm.xRot += hp * Anim.D * 0.6f;
        larm.xRot += hp * Anim.D * 0.6f;
        boolean firing = (id == 1 || id == 2) && t < 12 || h instanceof Aegis7Hero a && a.overdriven() && h.getTarget() != null;
        float spin = firing ? age * 1.4f : age * 0.05f;
        m.p("barrels_r").zRot = spin;
        m.p("barrels_l").zRot = -spin;
        if (firing) {
            rarm.z += Mth.sin(age * 3f) * 0.3f;
            larm.z += Mth.cos(age * 3f) * 0.3f;
        }
        if (id == Aegis7Hero.MISSILES) m.p("pod_lid").xRot = -1.5f * Anim.pulse(t, 0, 24);
        if (id == Aegis7Hero.STOMP) {
            float lift = Anim.pulse(t, 0, 18);
            m.p("rleg").xRot -= 0.8f * lift;
            m.p("body").xRot -= 0.1f * lift;
        }
        if (id == Aegis7Hero.OVERDRIVE) {
            m.p("body").y -= Anim.pulse(t, 0, 16) * 2;
            rarm.zRot += 0.6f * Anim.pulse(t, 0, 16);
            larm.zRot -= 0.6f * Anim.pulse(t, 0, 16);
        }
    }

    private static void animIgnis(ProcModel<HeroEntity> m, HeroEntity h, float ls, float la, float age, float hy, float hp, int id, float t) {
        boolean egg = h instanceof IgnisHero ig && ig.reborn();
        for (String n : new String[]{"body", "head", "wing_r", "wing_l", "tail", "legs"}) m.p(n).visible = !egg;
        m.p("egg").visible = egg;
        ModelPart plumes = m.opt("plumes");
        if (plumes != null && egg) plumes.visible = false;
        if (egg) {
            m.p("egg").y += Mth.sin(age * 0.3f) * 0.5f;
            m.p("egg").zRot = Mth.sin(age * 0.5f) * 0.08f;
            return;
        }
        Anim.look(m.p("head"), hy, hp);
        float flap = Mth.sin(age * 0.45f);
        ModelPart wr = m.p("wing_r"), wl = m.p("wing_l");
        wr.zRot += flap * 0.7f;
        wl.zRot -= flap * 0.7f;
        m.p("wingtip_r").zRot += flap * 0.4f;
        m.p("wingtip_l").zRot -= flap * 0.4f;
        m.p("tail").yRot += Mth.sin(age * 0.15f) * 0.2f;
        m.p("tail").xRot += Mth.sin(age * 0.2f) * 0.1f;
        m.p("crest").xRot += Mth.sin(age * 0.3f) * 0.1f;
        if (id == IgnisHero.DIVE && t > 14) {
            wr.zRot = 0.2f;
            wl.zRot = -0.2f;
            wr.yRot = 0.9f;
            wl.yRot = -0.9f;
            m.p("body").xRot += 0.6f;
        }
        if (id == IgnisHero.EMBER) {
            float c = Anim.pulse(t, 0, 24);
            wr.zRot = -0.3f * c + wr.zRot * (1 - c);
            wl.zRot = 0.3f * c + wl.zRot * (1 - c);
            m.p("head").xRot -= 0.5f * c;
        }
        if (id >= 1 && id <= 3) m.p("head").z -= Anim.pulse(t, 0, 8) * 1.5f;
    }

    private static void animVorthax(ProcModel<HeroEntity> m, HeroEntity h, float ls, float la, float age, float hy, float hp, int id, float t) {
        float flap = Mth.sin(age * 0.22f);
        ModelPart wr = m.p("wing_r"), wl = m.p("wing_l");
        wr.zRot += flap * 0.75f;
        wl.zRot -= flap * 0.75f;
        m.p("wingtip_r").zRot += flap * 0.45f;
        m.p("wingtip_l").zRot -= flap * 0.45f;
        for (int i = 1; i <= 3; i++) m.p("tail" + i).yRot += Mth.sin(age * 0.12f - i * 0.7f) * 0.18f;
        for (int i = 0; i < 4; i++) m.p("vleg" + i).xRot += 0.7f + Mth.sin(age * 0.1f + i) * 0.05f;
        m.p("neck1").yRot += hy * 0.4f * Anim.D;
        m.p("head").xRot += hp * 0.5f * Anim.D;
        ModelPart jaw = m.p("vjaw");
        jaw.xRot += 0.06f + Mth.sin(age * 0.1f) * 0.04f;
        switch (id) {
            case 1, 2, 3 -> {
                float o = Anim.win(t, 4, 6) * (1 - Anim.win(t, 22, 6));
                jaw.xRot += 0.8f * o;
                m.p("neck1").xRot += 0.3f * o;
            }
            case VorthaxHero.TAIL -> m.p("tail1").yRot += Mth.sin(Anim.win(t, 6, 16) * Mth.TWO_PI) * 1.4f;
            case VorthaxHero.GUST -> {
                float c = Anim.pulse(t, 0, 20);
                wr.yRot += -0.9f * c;
                wl.yRot += 0.9f * c;
            }
            case VorthaxHero.ROAR, VorthaxHero.METEORS -> {
                float c = Anim.pulse(t, 0, 30);
                m.p("neck1").xRot -= 0.6f * c;
                jaw.xRot += 0.9f * c;
            }
            default -> {
            }
        }
    }

    private static void animZeus(ProcModel<HeroEntity> m, HeroEntity h, float ls, float la, float age, float hy, float hp, int id, float t) {
        Anim.look(m.p("head"), hy, hp);
        ModelPart rarm = m.p("rarm"), larm = m.p("larm"), rleg = m.p("rleg"), lleg = m.p("lleg");
        rleg.xRot += 0.15f + Mth.sin(age * 0.08f) * 0.05f;
        lleg.xRot += -0.1f - Mth.sin(age * 0.08f) * 0.05f;
        m.p("zcape").xRot += 0.15f + Mth.sin(age * 0.1f) * 0.06f + la * 0.4f;
        larm.xRot += -0.7f;
        larm.yRot += 0.4f;
        rarm.xRot += -0.3f;
        ModelPart cloud = m.opt("cloud");
        if (cloud != null) cloud.yRot = age * 0.02f;
        switch (id) {
            case 1, 2, 3 -> {
                float s = Anim.swing(t, 10, 3, 8);
                rarm.xRot += -2.4f * Math.max(0, s) + 0.8f * Math.min(0, s);
                m.p("body").yRot += 0.3f * s;
            }
            case ZeusHero.CHAIN -> {
                float c = Anim.pulse(t, 0, 24);
                rarm.xRot = -1.6f * c - 0.3f;
                larm.xRot = -1.6f * c - 0.7f;
            }
            case ZeusHero.SMITE -> {
                float up = Anim.win(t, 0, 20), down = Anim.win(t, 30, 4);
                rarm.xRot = -3.1f * up * (1 - down) - 1.2f * down;
                m.p("head").xRot -= 0.5f * up * (1 - down);
            }
            case ZeusHero.AEGIS -> {
                float c = Anim.pulse(t, 0, 26);
                larm.xRot = -1.6f * c - 0.7f * (1 - c);
                larm.yRot = 0.4f * (1 - c);
            }
            case ZeusHero.WRATH -> {
                float c = Anim.pulse(t, 0, 36);
                rarm.xRot = -3.0f * c;
                larm.xRot = -3.0f * c;
                rarm.zRot += 0.3f * c;
                larm.zRot -= 0.3f * c;
                m.p("head").xRot -= 0.7f * c;
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ renderer

    public static ProcRenderer<HeroEntity> renderer(EntityRendererProvider.Context ctx, HeroKind kind) {
        float scale = switch (kind) {
            case AEGIS7 -> 1.3f;
            case VORTHAX -> 1.25f;
            case BASTION -> 1.05f;
            default -> 1f;
        };
        float shadow = switch (kind) {
            case AEGIS7, VORTHAX, BASTION -> 1.4f;
            case IGNIS -> 0.6f;
            default -> 0.5f;
        };
        ProcRenderer<HeroEntity> r = new ProcRenderer<>(ctx, baked(kind), HeroModels::animate, shadow);
        r.scale(scale).level(HeroEntity::heroLevel);
        if (kind == HeroKind.AEGIS7) {
            r.tint((h, c) -> {
                if (h instanceof Aegis7Hero a && a.overdriven()) {
                    c[0] = 1.25f;
                    c[1] = 0.8f;
                    c[2] = 0.75f;
                }
            });
        }
        if (kind == HeroKind.ZEUS || kind == HeroKind.IGNIS) {
            r.pre((h, pose, pt) -> pose.translate(0, Mth.sin((h.tickCount + pt) * 0.08f) * 0.08f, 0));
        }
        return r;
    }

    /** Kage's shadow clones: the same model rendered as translucent living shadow. */
    public static ProcRenderer<HeroEntity> cloneRenderer(EntityRendererProvider.Context ctx) {
        ProcRenderer<HeroEntity> r = new ProcRenderer<>(ctx, baked(HeroKind.KAGE), HeroModels::animate, 0.3f, true);
        r.level(h -> 1).tint((h, c) -> {
            c[0] = 0.25f;
            c[1] = 0.15f;
            c[2] = 0.35f;
            c[3] = 0.6f;
        });
        return r;
    }
}
