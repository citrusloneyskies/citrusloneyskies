package com.lastbastion.client.model;

import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
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
 * Turret models. Every family is its own machine with its own foundation, materials and moving
 * parts. Model levels: 1-4 follow the turret level (each adds visible hardware), 5 is the first
 * level-5 specialisation and 6 the second; parts gated {@code levels(1,4)} are replaced at level 5.
 */
public final class TurretModels {
    private static final Map<TurretKind, ModelSpec.Baked> CACHE = new EnumMap<>(TurretKind.class);

    // shared palette
    static final int C_WOOD = 0x7A5230, C_DWOOD = 0x4E3220, C_IRON = 0x7E8088, C_STEEL = 0xB4B8C2, C_GUN = 0x3A3D44,
            C_BRASS = 0xB8903A, C_COPPER = 0xB86E38, C_STONE = 0x8C8A86, C_MARBLE = 0xE6E2EA, C_ROPE = 0xC8B48A;

    private TurretModels() {
    }

    static Paint p(Mat m, int c) {
        return Paint.of(m, c);
    }

    static Paint wood() {
        return p(Mat.WOOD, C_WOOD).accent(0x5A3A22);
    }

    static Paint darkWood() {
        return p(Mat.WOOD, C_DWOOD).accent(0x3A2416);
    }

    static Paint iron() {
        return p(Mat.METAL, C_IRON);
    }

    static Paint steel() {
        return p(Mat.METAL, C_STEEL).gloss(0.6f);
    }

    static Paint gunmetal() {
        return p(Mat.DARK_METAL, C_GUN);
    }

    static Paint brass() {
        return p(Mat.GOLD, C_BRASS).accent(0xF0D890);
    }

    static Paint plate(int c) {
        return p(Mat.PLATE, c);
    }

    static Paint hazard() {
        return p(Mat.PLATE, 0x2A2A2A).pattern(Pattern.HAZARD, 0xE8B820, 1.2f);
    }

    static Paint glow(int c) {
        return Paint.glowing(c);
    }

    public static ModelSpec.Baked baked(TurretKind k) {
        return CACHE.computeIfAbsent(k, kind -> {
            ModelSpec s = new ModelSpec("turret_" + kind.id, 4);
            switch (kind) {
                case BALLISTA -> ballista(s);
                case FLAME -> inferno(s);
                case FROST -> cryo(s);
                case TIDE -> tide(s);
                case TESLA -> tesla(s);
                case VENOM -> venom(s);
                case MORTAR -> mortar(s);
                case PRISM -> prism(s);
                case GALE -> gale(s);
                case SANCTUM -> sanctum(s);
            }
            return s.bake();
        });
    }

    /** Four little rivet heads on a face plane (z fixed) of a box. */
    static void rivets(Part p, float x0, float y0, float x1, float y1, float z, Paint paint) {
        float r = 0.5f;
        p.box(x0, y0, z - r / 2, r, r, r, paint).box(x1 - r, y0, z - r / 2, r, r, r, paint)
                .box(x0, y1 - r, z - r / 2, r, r, r, paint).box(x1 - r, y1 - r, z - r / 2, r, r, r, paint);
    }

    // =====================================================================================
    // BALLISTA: timber siege crossbow -> Siegebreaker harpoon gun / Repeater scorpion
    // =====================================================================================

    private static void ballista(ModelSpec s) {
        Part base = s.root.child("base", 0, 24, 0);
        base.octo(0, -2, 0, 12, 2, 12, p(Mat.STONE, C_STONE).accent(0x5A7A3A).grime(0.4f));
        base.box(-7, -3.2f, -1.5f, 14, 1.4f, 3, wood());
        base.box(-1.5f, -3.2f, -7, 3, 1.4f, 14, wood());
        Paint capP = iron().trim(0x55575E, 0.3f);
        base.sub(0, 0, 0).from(2).box(-7.4f, -3.5f, -1.8f, 1.4f, 2, 3.6f, capP).box(6, -3.5f, -1.8f, 1.4f, 2, 3.6f, capP)
                .box(-1.8f, -3.5f, -7.4f, 3.6f, 2, 1.4f, capP).box(-1.8f, -3.5f, 6, 3.6f, 2, 1.4f, capP)
                .octoRing(-1.6f, 12.6f, 0.6f, 0.9f, iron());
        Part rack = base.sub(0, -3, 5).from(4);
        rack.box(-4.5f, -1.2f, -0.6f, 1, 1.2f, 2.4f, darkWood()).box(3.5f, -1.2f, -0.6f, 1, 1.2f, 2.4f, darkWood());
        for (int i = 0; i < 3; i++) {
            rack.box(-4, -1.8f - i * 0.7f, -0.2f + i * 0.5f, 8, 0.5f, 0.5f, darkWood());
            rack.box(-5, -1.9f - i * 0.7f, -0.3f + i * 0.5f, 1.2f, 0.7f, 0.7f, steel());
        }
        // Siegebreaker: anchor stakes and chain; Repeater: spare drum magazines
        Part stakes = base.sub(0, 0, 0).levels(5, 5);
        stakes.spike(-6.5f, -1, -6.5f, 1.4f, 5, -160, 0, 25, iron());
        stakes.spike(6.5f, -1, -6.5f, 1.4f, 5, -160, 0, -25, iron());
        stakes.box(-6, -1.4f, -6.8f, 12, 0.6f, 0.6f, p(Mat.METAL, 0x5A5C62).pattern(Pattern.CHAINMAIL, 0x9A9CA2, 2f));
        Part mags = base.sub(-5, -3, 4).levels(6, 6);
        mags.octo(0, -2.4f, 0, 2.6f, 2.4f, 2.6f, gunmetal()).octo(2.8f, -2.4f, 0.6f, 2.6f, 2.4f, 2.6f, gunmetal());
        mags.box(-1, -2.6f, -1, 2, 0.3f, 2, brass());

        Part yaw = s.root.child("yaw", 0, 19, 0);
        yaw.octo(0, -1.5f, 0, 9, 1.5f, 9, gunmetal().trim(0x5A5E66, 0.4f));
        yaw.sub(-3.2f, -1.5f, 0).rot(0, 0, -14).box(-1, -6.2f, -1.1f, 2, 6.2f, 2.2f, wood());
        yaw.sub(3.2f, -1.5f, 0).rot(0, 0, 14).box(-1, -6.2f, -1.1f, 2, 6.2f, 2.2f, wood());
        yaw.box(-4, -7.8f, -0.8f, 8, 1.6f, 1.6f, iron());
        yaw.sub(0, 0, 0).from(2).box(-3, -4.2f, -0.6f, 6, 1, 1.2f, iron()).box(-0.6f, -4.2f, -2.5f, 1.2f, 1, 5, iron());
        Part mantlet = yaw.child("mantlet", 0, -1.5f, -5.2f).from(4);
        Paint mantP = plate(0x6E7078).decal(Face.FRONT, DecalType.GRILLE, 0x101010, false, 0.5f, 0.55f, 0.6f);
        mantlet.box(-5, -5, -0.6f, 10, 5, 1.2f, mantP);
        mantlet.spike(-3.5f, -5, 0, 1, 2.5f, 0, 0, 0, steel()).spike(0, -5, 0, 1, 3, 0, 0, 0, steel()).spike(3.5f, -5, 0, 1, 2.5f, 0, 0, 0, steel());

        Part gun = yaw.child("gun", 0, -7, 0);
        gun.box(-1.5f, -1, -8, 3, 2, 15, wood());
        gun.box(-0.6f, -1.25f, -8, 1.2f, 0.3f, 13, gunmetal());
        gun.box(-1.9f, -1.4f, -9.4f, 3.8f, 2.8f, 1.8f, iron());
        Part crank = gun.sub(0, 0, 6.5f);
        crank.octoX(-2.6f, 0, 0, 1.8f, 5.2f, iron());
        crank.box(-3.2f, -0.3f, -0.3f, 0.6f, 2.6f, 0.6f, darkWood()).box(2.6f, -2.3f, -0.3f, 0.6f, 2.6f, 0.6f, darkWood());
        gun.sub(0, 0, 0).from(2).box(-1.6f, -1.1f, -5.5f, 3.2f, 2.2f, 0.8f, 0.15f, iron())
                .box(-1.6f, -1.1f, -0.5f, 3.2f, 2.2f, 0.8f, 0.15f, iron()).box(-1.6f, -1.1f, 3.5f, 3.2f, 2.2f, 0.8f, 0.15f, iron());
        // bow limbs (swept back), string, bolt
        Part bowR = gun.child("bow_r", -1.5f, 0, -7).rot(0, 18, 0);
        bowR.box(-9.6f, -0.7f, -0.8f, 9.6f, 1.4f, 1.6f, wood());
        bowR.sub(-9.6f, 0, 0).rot(0, 35, 0).box(-2.4f, -0.55f, -0.6f, 2.4f, 1.1f, 1.2f, wood());
        bowR.sub(0, 0, 0).from(2).box(-10.4f, -0.8f, -0.9f, 1.4f, 1.6f, 1.8f, iron());
        bowR.sub(0, 0, 0).from(4).box(-8.5f, -0.85f, -0.95f, 6.5f, 1.7f, 1.9f, steel());
        Part bowL = gun.child("bow_l", 1.5f, 0, -7).rot(0, -18, 0);
        bowL.box(0, -0.7f, -0.8f, 9.6f, 1.4f, 1.6f, wood());
        bowL.sub(9.6f, 0, 0).rot(0, -35, 0).box(0, -0.55f, -0.6f, 2.4f, 1.1f, 1.2f, wood());
        bowL.sub(0, 0, 0).from(2).box(9, -0.8f, -0.9f, 1.4f, 1.6f, 1.8f, iron());
        bowL.sub(0, 0, 0).from(4).box(2, -0.85f, -0.95f, 6.5f, 1.7f, 1.9f, steel());
        gun.child("string", 0, -0.1f, -3.9f).box(-9.6f, -0.15f, -0.15f, 19.2f, 0.3f, 0.3f, p(Mat.CLOTH, 0xE8DEC4).flat());
        Part bolt = gun.child("bolt", 0, -1.5f, 0).levels(1, 4);
        bolt.box(-0.35f, -0.35f, -10, 0.7f, 0.7f, 12, darkWood());
        bolt.spike(0, 0, -10, 1.3f, 2.4f, 90, 0, 0, steel());
        bolt.box(-1, -0.1f, 1, 2, 0.2f, 1.2f, p(Mat.CLOTH, 0xA02020)).box(-0.1f, -1, 1, 0.2f, 2, 1.2f, p(Mat.CLOTH, 0xA02020));
        // Lv3 twin bow
        Part twin = gun.child("twin", 0, -3.2f, -1).from(3);
        twin.box(-1, -0.8f, -7, 2, 1.6f, 10, wood());
        twin.box(-0.5f, -0.6f, -3, 1, 2.6f, 1, iron());
        twin.sub(-1, 0, -6).rot(0, 20, 0).box(-6.5f, -0.5f, -0.6f, 6.5f, 1, 1.2f, iron());
        twin.sub(1, 0, -6).rot(0, -20, 0).box(0, -0.5f, -0.6f, 6.5f, 1, 1.2f, iron());
        twin.box(-6.2f, -0.12f, -3.9f, 12.4f, 0.24f, 0.24f, p(Mat.CLOTH, 0xE8DEC4).flat());
        Part bolt2 = twin.child("bolt2", 0, -1.1f, 0);
        bolt2.box(-0.3f, -0.3f, -8, 0.6f, 0.6f, 9, darkWood());
        bolt2.spike(0, 0, -8, 1, 1.8f, 90, 0, 0, steel());
        gun.sub(1.9f, -1.6f, -2).from(4).octoZ(0, 0, -3, 1.2f, 5, brass());

        // Lv5 A: Siegebreaker harpoon + chain winch
        Part harpoon = gun.child("harpoon", 0, -1.8f, 0).levels(5, 5);
        harpoon.box(-0.6f, -0.6f, -13, 1.2f, 1.2f, 15, p(Mat.DARK_METAL, 0x4A4C52));
        harpoon.spike(0, 0, -13, 2.4f, 4, 90, 0, 0, steel());
        harpoon.spike(0, 0, -11.5f, 0.9f, 2.2f, -135, 0, 0, steel());
        harpoon.spike(0, 0, -11.5f, 0.9f, 2.2f, 135, 0, 0, steel());
        harpoon.spike(0, 0, -11.5f, 0.9f, 2.2f, 180, 0, 50, steel());
        harpoon.spike(0, 0, -11.5f, 0.9f, 2.2f, 180, 0, -50, steel());
        Part winch = gun.child("winch", 0, 2.2f, 3).levels(5, 5);
        winch.octoX(-2.8f, 0, 0, 3.4f, 5.6f, p(Mat.METAL, 0x5A5C62).pattern(Pattern.CHAINMAIL, 0x9A9CA2, 2.5f));
        winch.box(-3.2f, -2, -2, 0.6f, 4, 4, gunmetal()).box(2.6f, -2, -2, 0.6f, 4, 4, gunmetal());
        gun.sub(0, 0, 0).levels(5, 5).box(-2.2f, -2, -9, 4.4f, 1, 9, hazard()).box(-1.6f, 1, -9.5f, 3.2f, 1.2f, 16, gunmetal());
        // Lv5 B: Repeater scorpion: revolving drum magazine and cam pulleys
        Part drum = gun.child("drum", 0, -2.6f, 2.5f).levels(6, 6);
        drum.octoZ(0, 0, -2, 4.6f, 4, gunmetal().trim(C_BRASS, 0.4f));
        for (int i = 0; i < 6; i++) {
            float a = i * Mth.TWO_PI / 6;
            drum.box(Mth.cos(a) * 1.5f - 0.35f, Mth.sin(a) * 1.5f - 0.35f, -2.6f, 0.7f, 0.7f, 0.8f, steel());
        }
        gun.child("rbolt", 0, -1.5f, 0).levels(6, 6).box(-0.3f, -0.3f, -8, 0.6f, 0.6f, 9, darkWood()).spike(0, 0, -8, 1, 1.8f, 90, 0, 0, steel());
        Part lever = gun.child("lever", 1.8f, -0.5f, 3).levels(6, 6);
        lever.box(0, -4, -0.3f, 0.6f, 4, 0.6f, iron()).box(-0.2f, -4.6f, -0.5f, 1, 1, 1, darkWood());
        Part cams = gun.sub(0, 0, 0).levels(6, 6);
        cams.sub(-10.6f, 0, -3.9f).octoX(-0.4f, 0, 0, 2, 0.8f, brass());
        cams.sub(10.6f, 0, -3.9f).octoX(-0.4f, 0, 0, 2, 0.8f, brass());
    }

    // =====================================================================================
    // INFERNO: industrial flamethrower -> Soulfire dragon maw / Napalm launcher
    // =====================================================================================

    private static void inferno(ModelSpec s) {
        Part base = s.root.child("base", 0, 24, 0);
        base.octo(0, -1, 0, 11, 1, 11, hazard());
        base.octo(0, -6, 0, 5, 5, 5, gunmetal().trim(0x55585F, 0.3f));
        for (int i = 0; i < 3; i++) {
            Part leg = base.sub(0, -4, 0).rot(0, 120 * i + 30, 0);
            leg.sub(0, 0, -2).rot(-35, 0, 0).box(-0.9f, 0, -0.9f, 1.8f, 5, 1.8f, gunmetal());
            leg.box(-1.4f, 3.2f, -6.2f, 2.8f, 0.8f, 2.8f, iron());
        }
        Part fuelLine = base.sub(0, 0, 0).from(2);
        fuelLine.box(2.4f, -5.5f, -0.6f, 3, 1.2f, 1.2f, p(Mat.METAL, C_COPPER)).box(4.8f, -5.5f, -0.6f, 1.2f, 5, 1.2f, p(Mat.METAL, C_COPPER));
        Part reserve = base.sub(-4.5f, -1, 3).from(4);
        reserve.octo(0, -6, 0, 3.4f, 6, 3.4f, p(Mat.METAL, 0xA02818).pattern(Pattern.BANDS, 0x5A1810, 0.6f)
                .decal(Face.FRONT, DecalType.RING, 0xE8E8E8, false, 0.5f, 0.3f, 0.5f));
        reserve.octo(0, -6.8f, 0, 2, 0.8f, 2, brass());

        Part yaw = s.root.child("yaw", 0, 18, 0);
        Paint housing = p(Mat.DARK_METAL, 0x45484F).pattern(Pattern.PLATES, 0x2A2C30, 1.2f)
                .decal(Face.LEFT, DecalType.GRILLE, 0x0A0A0A, false).decal(Face.RIGHT, DecalType.GRILLE, 0x0A0A0A, false);
        yaw.box(-4, -5, -4, 8, 5, 8, housing);
        yaw.box(-4.4f, -0.8f, -4.4f, 8.8f, 0.8f, 8.8f, hazard());
        Part tank = yaw.child("tank", 0, -5, 4.6f);
        Paint tankP = p(Mat.METAL, 0xA82A1A).pattern(Pattern.BANDS, 0x5A1810, 0.5f).gloss(0.55f)
                .decal(Face.BACK, DecalType.FLAME, 0xFFB000, false, 0.5f, 0.45f, 0.8f);
        tank.octo(0, -8, 0, 5.5f, 8, 5.5f, tankP);
        tank.octo(0, -9, 0, 4, 1, 4, p(Mat.METAL, 0x8A2214));
        tank.box(-1.6f, -9.6f, -0.3f, 3.2f, 0.6f, 0.6f, brass()).box(-0.3f, -9.6f, -1.6f, 0.6f, 0.6f, 3.2f, brass());
        tank.sub(0, 0, 0).from(2).octoRing(-6.4f, 6.2f, 0.4f, 0.8f, iron()).octoRing(-2.2f, 6.2f, 0.4f, 0.8f, iron());
        Part soul = tank.child("soultank", 0, 0, 0).levels(5, 5);
        soul.octo(0, -8, 0, 5.8f, 8, 5.8f, glow(0x40D8FF).pattern(Pattern.BANDS, 0x1A6A90, 0.6f));
        yaw.box(-0.6f, -6.2f, 0.6f, 1.2f, 1.2f, 3.8f, p(Mat.METAL, C_COPPER));
        Part tank2 = yaw.child("tank2", 3.4f, -5, 3).from(4);
        tank2.octo(0, -6, 0, 3, 6, 3, p(Mat.METAL, 0xA82A1A).pattern(Pattern.BANDS, 0x5A1810, 0.5f));
        tank2.box(-0.8f, -6.8f, -0.8f, 1.6f, 0.8f, 1.6f, brass());
        yaw.sub(0, 0, 0).from(4).box(-4.6f, -4.5f, -3, 0.6f, 3.5f, 6, plate(0x5A5C62)).box(4, -4.5f, -3, 0.6f, 3.5f, 6, plate(0x5A5C62));

        Part gun = yaw.child("gun", 0, -6.5f, -1.5f);
        gun.box(-2.3f, -2.3f, -1, 4.6f, 4.6f, 3, p(Mat.DARK_METAL, 0x34363C));
        Part barrel = gun.child("barrel", 0, 0, 0).levels(1, 5);
        barrel.octoZ(0, 0, -11, 3.2f, 10, gunmetal());
        for (int i = 0; i < 4; i++) {
            barrel.box(-2.3f, -2.3f, -9.3f + i * 2, 4.6f, 4.6f, 0.5f, p(Mat.DARK_METAL, 0x2A2C30));
        }
        barrel.sub(0, 0, 0).from(4).box(-2.35f, -2.35f, -9.4f, 4.7f, 4.7f, 0.4f, glow(0xFF5A10))
                .box(-2.35f, -2.35f, -7.4f, 4.7f, 4.7f, 0.4f, glow(0xFF5A10)).box(-2.35f, -2.35f, -5.4f, 4.7f, 4.7f, 0.4f, glow(0xFF7A20));
        Part nozzle = barrel.child("nozzle", 0, 0, -11).levels(1, 4);
        nozzle.octoZ(0, 0, -2, 4.6f, 2, p(Mat.DARK_METAL, 0x222226));
        nozzle.box(-1.1f, -1.1f, -2.1f, 2.2f, 2.2f, 0.2f, glow(0xFF7A10));
        gun.child("pilot", 0, 2.4f, -11.6f).levels(1, 4).box(-0.45f, -0.45f, -0.45f, 0.9f, 0.9f, 0.9f, glow(0x6AB8FF));
        gun.sub(0, 0, 0).from(2).box(-2.7f, -3, -9, 5.4f, 0.8f, 8.5f, plate(0x5E6068).trim(0x3A3C42, 0.3f));
        gun.sub(0, 3.3f, 0).from(3).octoZ(0, 0, -8, 2.4f, 7, p(Mat.METAL, 0xD07020).pattern(Pattern.BANDS, 0x3A3A3A, 0.8f));

        // Lv5 A: Soulfire Maw (dragon skull nozzle with hinged jaw)
        Paint bone = p(Mat.BONE, 0xE4DCC8).accent(0x9A8E72);
        Part maw = gun.child("maw", 0, 0, -11).levels(5, 5);
        maw.box(-3, -3.4f, -5, 6, 3.2f, 5.5f, bone.copy().decal(Face.FRONT, DecalType.HOLLOW_EYES, 0x40E0FF, true, 0.5f, 0.35f, 1.2f));
        maw.box(-2, -2.6f, -8, 4, 2.2f, 3.2f, bone);
        maw.spike(-2.4f, -3.2f, 0, 1.2f, 4.5f, -55, 0, 20, bone).spike(2.4f, -3.2f, 0, 1.2f, 4.5f, -55, 0, -20, bone);
        maw.spike(-1.6f, -0.4f, -7.6f, 0.7f, 1.4f, 180, 0, 0, p(Mat.BONE, 0xF4F0E4)).spike(1.6f, -0.4f, -7.6f, 0.7f, 1.4f, 180, 0, 0, p(Mat.BONE, 0xF4F0E4));
        maw.box(-1.4f, -0.8f, -7.9f, 2.8f, 1.2f, 0.4f, glow(0x40E0FF));
        Part jaw = maw.child("jaw", 0, 0, 0);
        jaw.box(-2.6f, 0, -7.5f, 5.2f, 1.4f, 7.5f, bone);
        jaw.spike(-1.6f, 0, -7, 0.6f, 1.2f, 0, 0, 0, p(Mat.BONE, 0xF4F0E4)).spike(1.6f, 0, -7, 0.6f, 1.2f, 0, 0, 0, p(Mat.BONE, 0xF4F0E4));
        gun.sub(0, 0, 0).levels(5, 5).box(-2.4f, -2.4f, -6, 4.8f, 4.8f, 0.5f, glow(0x40D8FF)).box(-2.4f, -2.4f, -3, 4.8f, 4.8f, 0.5f, glow(0x40D8FF));
        // Lv5 B: Napalm launcher: three tubes and a canister drum
        Part launcher = gun.child("launcher", 0, 0, 0).levels(6, 6);
        Paint tube = p(Mat.METAL, 0x5A6A3A).pattern(Pattern.BANDS, 0x3A4428, 0.7f);
        launcher.octoZ(-2.4f, 0.6f, -11, 2.8f, 11, tube).octoZ(2.4f, 0.6f, -11, 2.8f, 11, tube).octoZ(0, -2.6f, -11, 2.8f, 11, tube);
        launcher.box(-4.2f, -4.4f, -11.6f, 8.4f, 6.8f, 0.8f, hazard());
        launcher.box(-1, -3.6f, -11.8f, 2, 2, 0.4f, glow(0xFF8A20)).box(-3.4f, -0.4f, -11.8f, 2, 2, 0.4f, glow(0xFF8A20))
                .box(1.4f, -0.4f, -11.8f, 2, 2, 0.4f, glow(0xFF8A20));
        Part cdrum = launcher.child("cdrum", 0, -0.5f, 1.5f);
        cdrum.octoZ(0, 0, -1.5f, 6.6f, 3.6f, gunmetal().trim(0xD07020, 0.5f));
        for (int i = 0; i < 5; i++) {
            float a = i * Mth.TWO_PI / 5;
            cdrum.box(Mth.cos(a) * 2.2f - 0.6f, Mth.sin(a) * 2.2f - 0.6f, 2.1f, 1.2f, 1.2f, 0.6f, p(Mat.METAL, 0xD07020));
        }
    }

    // =====================================================================================
    // CRYO SPIRE: crystal growing from frozen rock -> Absolute Zero monolith / Glacial lance
    // =====================================================================================

    private static void cryo(ModelSpec s) {
        Paint rock = p(Mat.STONE, 0x6C7682).accent(0xD8F0FF).grime(0.2f);
        Paint ice = p(Mat.ICE, 0xA8E4FA).accent(0xFFFFFF);
        Paint iceGlow = p(Mat.CRYSTAL, 0x8FE3FF).accent(0xE8FFFF).glow();
        Paint silver = p(Mat.METAL, 0xC8D4E0).gloss(0.7f);
        Part base = s.root.child("base", 0, 24, 0);
        base.box(-6, -3, -5, 12, 3, 10, rock).box(-5, -5, -4, 7, 2.2f, 7, rock).box(1, -4.4f, -1, 5, 1.6f, 6, rock);
        base.box(-6.2f, -3.3f, -4, 4, 0.4f, 6, ice).box(2, -4.6f, -0.5f, 3.5f, 0.3f, 4, ice);
        base.spike(-4.5f, -3, 3, 1.6f, 5, -15, 0, -20, ice).spike(4.5f, -3, -3.5f, 1.4f, 4, 20, 0, 25, ice);
        base.sub(0, 0, 0).from(2).spike(-5, -3, -3, 1.2f, 3.5f, 25, 0, -30, ice).spike(5, -3, 3.5f, 1.6f, 4.5f, -20, 0, 20, ice);
        base.sub(0, 0, 0).from(4).spike(0, -3, -5, 2, 6, 30, 0, 0, iceGlow).spike(-1, -3, 5, 1.8f, 5.5f, -30, 0, 0, iceGlow)
                .spike(6, -2, 0, 1.4f, 4, 0, 0, 40, iceGlow);

        Part yaw = s.root.child("yaw", 0, 19, 0);
        Part column = yaw.child("column", 0, 0, 0);
        column.taper(0, 0, 0, 4.4f, 4.4f, 2.4f, 2.4f, 12, 4, ice);
        yaw.octo(0, -2.4f, 0, 5.6f, 1.2f, 5.6f, silver.copy().pattern(Pattern.RUNES, 0x8FE3FF).accentGlow());
        yaw.sub(0, 0, 0).from(2).octo(0, -8.6f, 0, 4.2f, 1, 4.2f, silver.copy().pattern(Pattern.RUNES, 0x8FE3FF).accentGlow());
        Part cage = yaw.child("cage", 0, -16, 0).levels(4, 5);
        for (int i = 0; i < 4; i++) {
            Part arc = cage.sub(0, 0, 0).rot(0, 45 + 90 * i, 0);
            arc.sub(4.6f, 0, 0).rot(0, 0, 0).box(-0.4f, -4, -0.4f, 0.8f, 8, 0.8f, silver);
            arc.sub(3.2f, -4.2f, 0).rot(0, 0, -45).box(-0.4f, -2.2f, -0.4f, 0.8f, 2.4f, 0.8f, silver);
            arc.sub(3.2f, 4.2f, 0).rot(0, 0, 45).box(-0.4f, -0.2f, -0.4f, 0.8f, 2.4f, 0.8f, silver);
        }
        Part orbit = yaw.child("orbit", 0, -15, 0).levels(1, 5);
        int[] gate = {1, 1, 2, 3, 4, 4};
        for (int i = 0; i < 6; i++) {
            float a = i * Mth.TWO_PI / 6;
            Part sh = orbit.child("shard" + i, Mth.cos(a) * 6, (i % 2) * 2 - 1, Mth.sin(a) * 6).from(gate[i]).rot(0, -i * 60, 18);
            sh.taper(0, 2, 0, 1.6f, 1.6f, 0.3f, 0.3f, 4, 3, iceGlow);
            sh.sub(0, 0, 0).rot(180, 0, 0).taper(0, -2, 0, 1.6f, 1.6f, 0.3f, 0.3f, 2.5f, 2, iceGlow);
        }
        Part gun = yaw.child("gun", 0, -16, 0);
        Part focus = gun.child("focus", 0, 0, 0).levels(1, 6);
        focus.sub(0, 0, 0).rot(45, 0, 35).box(-2.6f, -2.6f, -2.6f, 5.2f, 5.2f, 5.2f, iceGlow);
        focus.sub(0, 0, 0).rot(0, 45, 0).box(-1.2f, -1.2f, -1.2f, 2.4f, 2.4f, 2.4f, glow(0xF0FFFF));

        // Lv5 A: Absolute Zero monolith
        Part mono = yaw.child("mono", 0, 0, 0).levels(5, 5);
        Paint monoP = p(Mat.CRYSTAL, 0x7AD0F4).accent(0xE8FFFF).pattern(Pattern.RUNES, 0xE8FFFF).accentGlow();
        mono.taper(0, 0, 0, 6, 6, 1.5f, 1.5f, 22, 6, monoP);
        mono.sub(-3, 0, 1).rot(0, 0, -18).taper(0, 0, 0, 3.4f, 3.4f, 0.6f, 0.6f, 13, 4, ice);
        mono.sub(3, 0, -1).rot(0, 0, 20).taper(0, 0, 0, 3, 3, 0.5f, 0.5f, 11, 4, ice);
        mono.sub(0, 0, 3).rot(-22, 0, 0).taper(0, 0, 0, 2.6f, 2.6f, 0.5f, 0.5f, 9, 3, ice);
        Part halo = yaw.child("frosthalo", 0, -24, 0).levels(5, 5);
        for (int i = 0; i < 8; i++) {
            float a = i * Mth.TWO_PI / 8;
            halo.sub(Mth.cos(a) * 7, 0, Mth.sin(a) * 7).rot(0, 0, 0).taper(0, 1.5f, 0, 1.2f, 1.2f, 0.2f, 0.2f, 3.5f, 2, iceGlow);
        }
        // Lv5 B: Glacial lance barrel with charge coils
        Part lance = gun.child("lance", 0, 0, -2).levels(6, 6);
        lance.sub(0, 0, 0).rot(90, 0, 0).taper(0, 0, 0, 3.2f, 3.2f, 0.6f, 0.6f, 16, 5, iceGlow);
        for (int i = 0; i < 3; i++) {
            lance.sub(0, 0, -3.5f - i * 4).rot(90, 0, 0).octoRing(-0.5f, 5.2f - i * 0.7f, 0.6f, 1, silver);
        }
        lance.box(-2.2f, -2.2f, 1, 4.4f, 4.4f, 3, silver.copy().pattern(Pattern.RUNES, 0x8FE3FF).accentGlow());
    }

    // =====================================================================================
    // TIDECALLER: conch cannon over a fountain -> Maelstrom rings / Leviathan head
    // =====================================================================================

    private static void tide(ModelSpec s) {
        Paint basin = p(Mat.STONE, 0xBEB6A6).accent(0x6A8A5A).pattern(Pattern.BRICK, 0x9A9282, 1.2f);
        Paint water = p(Mat.ENERGY, 0x2F7BFF).accent(0x8AD0FF).glow();
        Paint coral = p(Mat.FLESH, 0xE8806A).accent(0xC8504A).noise(0.08f);
        Paint shell = p(Mat.BONE, 0xF0D8C8).accent(0xD89A86).pattern(Pattern.RIBS, 0xD8A090, 1.4f);
        Part base = s.root.child("base", 0, 24, 0);
        base.octo(0, -2.5f, 0, 14, 2.5f, 14, basin);
        base.sub(0, 0, 0).octoRing(-4, 14, 1.3f, 1.6f, basin);
        Part water0 = base.child("water", 0, -3.4f, 0);
        water0.octo(0, -0.3f, 0, 11.5f, 0.5f, 11.5f, water);
        base.sub(0, 0, 0).from(2).box(-7.6f, -4.6f, -1.5f, 1.2f, 2, 3, shell).box(6.4f, -4.6f, -1.5f, 1.2f, 2, 3, shell)
                .box(-1.5f, -4.6f, 6.4f, 3, 2, 1.2f, shell).box(-1.5f, -4.6f, -7.6f, 3, 2, 1.2f, shell);
        Part corals = base.sub(0, -4, 0).from(4);
        Paint cglow = p(Mat.FLESH, 0xFF7AB0).accent(0xFFC0E0).glow();
        corals.sub(-5, 0, 4).rot(0, 0, -15).box(-0.6f, -5, -0.6f, 1.2f, 5, 1.2f, cglow).sub(0, -3, 0).rot(0, 0, 50).box(-0.5f, -3, -0.5f, 1, 3, 1, cglow);
        corals.sub(5, 0, 4).rot(0, 0, 12).box(-0.6f, -6, -0.6f, 1.2f, 6, 1.2f, cglow).sub(0, -4, 0).rot(0, 0, -45).box(-0.5f, -3, -0.5f, 1, 3, 1, cglow);
        corals.sphere(-4.5f, -1, -4.5f, 0.9f, glow(0xF8F4FF)).sphere(4.6f, -1, -4.2f, 0.8f, glow(0xF8F4FF));

        Part yaw = s.root.child("yaw", 0, 20, 0);
        yaw.octo(0, -6, 0, 4, 6, 4, coral);
        yaw.sub(1.5f, -3, 0).rot(0, 0, 40).box(-0.5f, -3, -0.5f, 1, 3, 1, coral);
        yaw.sub(-1.5f, -4, 0).rot(0, 0, -40).box(-0.5f, -2.5f, -0.5f, 1, 2.5f, 1, coral);
        yaw.octo(0, -6.8f, 0, 5, 0.8f, 5, brass());
        Part gun = yaw.child("gun", 0, -8.5f, 0);
        Part conch = gun.child("conch", 0, 0, -3).levels(1, 5);
        conch.sub(0, 0, 0).rot(-90, 0, 0).taper(0, 0, 0, 6, 6, 1.4f, 1.4f, 10, 6, shell);
        conch.box(-2.3f, -2.3f, -0.2f, 4.6f, 4.6f, 0.4f, water);
        conch.spike(0, -2.4f, 3, 0.9f, 1.8f, 0, 0, 0, shell).spike(2.2f, -1, 5, 0.8f, 1.5f, 0, 0, -60, shell)
                .spike(-2.2f, -1, 5, 0.8f, 1.5f, 0, 0, 60, shell).spike(0, -1.6f, 7.5f, 0.6f, 1.4f, -30, 0, 0, shell);
        conch.sub(0, 0, 0).from(2).box(-3.2f, -3.2f, 1, 6.4f, 6.4f, 0.7f, brass()).box(-2.4f, -2.4f, 5.5f, 4.8f, 4.8f, 0.6f, brass());
        Part spouts = yaw.sub(0, -6, 0).from(3);
        spouts.sub(-3.6f, 0, 0).rot(0, 0, -25).octo(0, -4, 0, 1.6f, 4, 1.6f, shell).octo(0, -4.4f, 0, 2.2f, 0.6f, 2.2f, brass());
        spouts.sub(3.6f, 0, 0).rot(0, 0, 25).octo(0, -4, 0, 1.6f, 4, 1.6f, shell).octo(0, -4.4f, 0, 2.2f, 0.6f, 2.2f, brass());
        gun.sub(0, 0, 0).from(4).levels(4, 5).sphere(0, -3.8f, 2, 1, glow(0xF8F4FF)).sub(0, 0, 0)
                .box(-4.5f, -0.3f, 1, 2, 0.6f, 4, p(Mat.SCALES, 0x3A9AAA)).box(2.5f, -0.3f, 1, 2, 0.6f, 4, p(Mat.SCALES, 0x3A9AAA));

        // Lv5 A: Maelstrom: spinning water rings and a twisting spout
        Part ringA = yaw.child("ringA", 0, -8, 0).levels(5, 5);
        ringA.octoRing(-0.5f, 15, 1, 1, water);
        Part ringB = yaw.child("ringB", 0, -8, 0).levels(5, 5);
        ringB.octoRing(-0.5f, 11, 0.8f, 0.8f, glow(0x8AD0FF));
        Part spout = yaw.child("spout", 0, -11, 0).levels(5, 5);
        spout.taper(0, 0, 0, 3, 3, 1, 1, 7, 5, water);
        // Lv5 B: Leviathan head with snapping jaw
        Paint scales = p(Mat.SCALES, 0x2E8A9A).accent(0x14505A).gloss(0.4f);
        Part lev = gun.child("leviathan", 0, 0, 0).levels(6, 6);
        lev.box(-3.2f, -3.6f, -9, 6.4f, 4, 8, scales.copy().decal(Face.FRONT, DecalType.SLIT_EYES, 0xFFD040, true, 0.5f, 0.35f, 1.2f));
        lev.box(-2.4f, -2.6f, -12, 4.8f, 2.6f, 3.2f, scales);
        lev.spike(-2.6f, -3.6f, -4, 1, 4, -70, 0, 15, p(Mat.BONE, 0xE8E0C8)).spike(2.6f, -3.6f, -4, 1, 4, -70, 0, -15, p(Mat.BONE, 0xE8E0C8));
        lev.box(-0.3f, -6, -7, 0.6f, 2.6f, 7, p(Mat.CLOTH, 0x3AC8C8).flat());
        lev.box(-4.8f, -2, -6, 1.6f, 0.4f, 4, p(Mat.CLOTH, 0x3AC8C8)).box(3.2f, -2, -6, 1.6f, 0.4f, 4, p(Mat.CLOTH, 0x3AC8C8));
        for (int i = 0; i < 3; i++) lev.octoZ(0, -1, -1 + i * 2.6f, 5 - i * 0.8f, 2.6f, scales);
        Part ljaw = lev.child("ljaw", 0, 0.4f, -3);
        ljaw.box(-2.6f, 0, -8.6f, 5.2f, 1.6f, 8.6f, scales);
        for (int i = 0; i < 3; i++) {
            ljaw.spike(-1.8f, 0, -8 + i * 2.2f, 0.5f, 1.1f, 180, 0, 0, p(Mat.BONE, 0xF4F0E4));
            ljaw.spike(1.8f, 0, -8 + i * 2.2f, 0.5f, 1.1f, 180, 0, 0, p(Mat.BONE, 0xF4F0E4));
        }
        lev.box(-1.6f, -0.6f, -12.2f, 3.2f, 1.2f, 0.4f, glow(0x8AD0FF));
    }

    // =====================================================================================
    // TESLA COIL: insulated copper coil with toroid -> Stormcaller rod / Arc reactor gyroscope
    // =====================================================================================

    private static void tesla(ModelSpec s) {
        Paint concrete = p(Mat.STONE, 0x8E8C88).noise(0.06f);
        Paint ceramic = p(Mat.PLAIN, 0x6A3A2A).gloss(0.5f).noise(0.03f);
        Paint copper = p(Mat.METAL, C_COPPER).pattern(Pattern.BANDS, 0x7A4420, 3f).gloss(0.6f);
        Paint chrome = p(Mat.METAL, 0xD8DCE4).gloss(0.85f);
        Paint arc = glow(0xF8F49A).accent(0xFFFFFF);
        Part base = s.root.child("base", 0, 24, 0);
        base.box(-7, -2, -7, 14, 2, 14, concrete);
        base.box(-7.3f, -2.3f, -7.3f, 14.6f, 0.6f, 1.2f, hazard()).box(-7.3f, -2.3f, 6.1f, 14.6f, 0.6f, 1.2f, hazard())
                .box(-7.3f, -2.3f, -6.1f, 1.2f, 0.6f, 12.2f, hazard()).box(6.1f, -2.3f, -6.1f, 1.2f, 0.6f, 12.2f, hazard());
        Part xf = base.sub(-5, -2, 4).from(2);
        xf.box(-1.5f, -4, -1.5f, 4, 4, 3, p(Mat.METAL, 0x6A7068).decal(Face.FRONT, DecalType.BOLT, 0xE8C020, false, 0.5f, 0.45f, 0.9f));
        xf.box(-1, -5, -1, 0.8f, 1, 0.8f, ceramic).box(1.2f, -5, -1, 0.8f, 1, 0.8f, ceramic);
        xf.box(0, -0.6f, 1.5f, 6, 0.6f, 0.6f, p(Mat.RUBBER, 0x1A1A1A));
        Part caps = base.sub(5, -2, 4).from(4);
        for (int i = 0; i < 3; i++) {
            caps.octo(0, -4, -i * 2.6f + 1.5f, 2, 4, 2, p(Mat.GLASS, 0x9ADCE8).accent(0xFFFFFF));
            caps.octo(0, -3, -i * 2.6f + 1.5f, 1, 2.6f, 1, arc);
            caps.octo(0, -4.4f, -i * 2.6f + 1.5f, 2.2f, 0.5f, 2.2f, copper);
        }

        Part yaw = s.root.child("yaw", 0, 22, 0);
        yaw.octo(0, -3, 0, 6.5f, 3, 6.5f, p(Mat.PLAIN, 0xE8E4D8).pattern(Pattern.BANDS, 0xB8B4A8, 1.5f));
        yaw.octo(0, -16, 0, 4, 13, 4, copper);
        float[] discs = {-5.5f, -9.5f, -13.5f};
        for (int i = 0; i < 3; i++) yaw.sub(0, 0, 0).from(i == 2 ? 2 : 1).octo(0, discs[i], 0, 7 - i * 0.6f, 0.8f, 7 - i * 0.6f, ceramic);
        Part toroid = yaw.child("toroid", 0, -17, 0);
        toroid.octoRing(-1, 10, 2, 2, chrome);
        toroid.octo(0, -0.6f, 0, 6, 1.2f, 6, chrome);
        Part toroid2 = yaw.child("toroid2", 0, -12, 0).from(4);
        toroid2.octoRing(-0.8f, 8.5f, 1.4f, 1.6f, chrome);
        for (int i = 0; i < 4; i++) {
            Part prong = yaw.child("prong" + i, 0, -11, 0).from(3).rot(0, 45 + 90 * i, 0);
            prong.box(2, -0.6f, -0.6f, 4.5f, 1.2f, 1.2f, chrome);
            prong.box(5.8f, -3.5f, -0.5f, 1, 3.5f, 1, copper);
            prong.sphere(6.3f, -4, 0, 0.9f, arc);
        }
        Part gun = yaw.child("gun", 0, -19, 0);
        gun.sphere(0, 0, 0, 2.3f, arc);
        gun.sub(0, 0, 0).rot(45, 45, 0).box(-1.2f, -1.2f, -1.2f, 2.4f, 2.4f, 2.4f, glow(0xFFFFFF));

        // Lv5 A: Stormcaller lightning rod with floating rings
        Part rod = yaw.child("rod", 0, -20, 0).levels(5, 5);
        rod.box(-0.6f, -16, -0.6f, 1.2f, 16, 1.2f, chrome);
        rod.spike(0, -16, 0, 1.4f, 3, 0, 0, 0, arc);
        for (int i = 0; i < 3; i++) {
            Part r = yaw.child("sring" + i, 0, -26 - i * 4, 0).levels(5, 5);
            r.octoRing(-0.4f, 7 - i * 1.6f, 0.6f, 0.8f, i == 1 ? arc : chrome);
        }
        yaw.sub(0, -20, 0).levels(5, 5).spike(-2.5f, 0, 0, 0.8f, 3, 0, 0, -30, p(Mat.DARK_METAL, 0x2A2C34))
                .spike(2.5f, 0, 0, 0.8f, 3, 0, 0, 30, p(Mat.DARK_METAL, 0x2A2C34)).spike(0, 0, 2.5f, 0.8f, 3, -30, 0, 0, p(Mat.DARK_METAL, 0x2A2C34))
                .spike(0, 0, -2.5f, 0.8f, 3, 30, 0, 0, p(Mat.DARK_METAL, 0x2A2C34));
        // Lv5 B: Arc reactor gyroscope around the core
        Part g1 = gun.child("gyro1", 0, 0, 0).levels(6, 6);
        g1.sub(0, 0, 0).rot(90, 0, 0).octoRing(-0.4f, 9, 0.7f, 0.8f, chrome);
        Part g2 = gun.child("gyro2", 0, 0, 0).levels(6, 6);
        g2.sub(0, 0, 0).rot(0, 0, 90).octoRing(-0.4f, 7.4f, 0.7f, 0.8f, copper);
        Part g3 = gun.child("gyro3", 0, 0, 0).levels(6, 6);
        g3.octoRing(-0.4f, 11, 0.8f, 0.8f, arc);
        gun.sub(0, 0, 0).levels(6, 6).sphere(0, 0, 0, 3, p(Mat.ENERGY, 0x8AE0FF).accent(0xFFFFFF).glow());
    }

    // =====================================================================================
    // VENOM LOBBER: swamp catapult with bubbling cauldron -> Plague cauldron / Blightspore hive
    // =====================================================================================

    private static void venom(ModelSpec s) {
        Paint moss = p(Mat.STONE, 0x5E6650).accent(0x4A7A2A).grime(0.4f);
        Paint brew = p(Mat.ENERGY, 0x7BD940).accent(0xD8FF80).glow();
        Paint cast = p(Mat.DARK_METAL, 0x34362E).grime(0.3f);
        Part base = s.root.child("base", 0, 24, 0);
        base.box(-6.5f, -2, -6, 13, 2, 12, moss);
        base.box(-6.5f, -4, -5.5f, 2, 2.2f, 11, wood()).box(4.5f, -4, -5.5f, 2, 2.2f, 11, wood());
        Part caul = base.child("cauldron", 0, -2, 4.5f).levels(1, 4);
        caul.octo(0, -3.6f, 0, 5.6f, 3.6f, 5.6f, cast);
        caul.octo(0, -3.8f, 0, 4.6f, 0.4f, 4.6f, brew);
        caul.box(-3.4f, -3.2f, -0.4f, 0.8f, 0.8f, 0.8f, iron()).box(2.6f, -3.2f, -0.4f, 0.8f, 0.8f, 0.8f, iron());
        base.sub(0, 0, 0).from(2).box(2.6f, -5.6f, 3.6f, 1, 1, 1, p(Mat.METAL, C_COPPER)).box(3, -9, 3.6f, 1, 3.6f, 1, p(Mat.METAL, C_COPPER));
        base.sub(0, 0, 0).from(4).levels(4, 4).sphere(-4.5f, -5, 4.5f, 1.1f, p(Mat.BONE, 0xE8E0C8).decal(Face.FRONT, DecalType.HOLLOW_EYES, 0x7BD940, true));

        Part yaw = s.root.child("yaw", 0, 20, 0);
        yaw.octo(0, -1, 0, 8, 1, 8, wood());
        yaw.box(-4, -7.2f, -1, 1.6f, 7, 2, wood()).box(2.4f, -7.2f, -1, 1.6f, 7, 2, wood());
        yaw.box(-4.2f, -7.4f, -0.6f, 8.4f, 1.2f, 1.2f, iron());
        yaw.sub(0, 0, 0).from(2).box(-4.3f, -3.5f, -1.2f, 2, 0.8f, 2.4f, iron()).box(2.3f, -3.5f, -1.2f, 2, 0.8f, 2.4f, iron());
        Part gun = yaw.child("gun", 0, -6.8f, 0);
        gun.box(-0.8f, -0.8f, -10, 1.6f, 1.6f, 13, wood());
        gun.sub(0, 0, 0).from(2).box(-0.95f, -0.95f, -7, 1.9f, 1.9f, 0.7f, iron()).box(-0.95f, -0.95f, -2, 1.9f, 1.9f, 0.7f, iron());
        Part weight = gun.child("weight", 0, 0.5f, 2.6f);
        weight.box(-2.2f, 0, -1.2f, 4.4f, 3.4f, 2.6f, cast);
        Part bucket = gun.child("bucket", 0, -0.6f, -10).levels(1, 5);
        bucket.box(-2.6f, -3, -2.6f, 5.2f, 3, 5.2f, darkWood().trim(C_IRON, 0.5f));
        bucket.box(-2.1f, -3.3f, -2.1f, 4.2f, 0.8f, 4.2f, brew);
        bucket.sub(0, 0, 0).from(3).box(-3, -3.4f, -3, 6, 0.6f, 6, iron());
        gun.child("glob", 0, -4.5f, -10).levels(1, 5).sphere(0, 0, 0, 1.4f, brew);

        // Lv5 A: Plague cauldron with skull face and smoke stacks
        Part plague = base.child("plague", 0, -2, 4).levels(5, 5);
        plague.octo(0, -5, 0, 7.5f, 5, 7.5f, cast.copy().decal(Face.FRONT, DecalType.SKULL, 0x9AD860, true, 0.5f, 0.5f, 0.9f));
        plague.octo(0, -5.3f, 0, 6.2f, 0.4f, 6.2f, brew);
        plague.sub(-3, -5, 2.5f).octo(0, -6, 0, 1.6f, 6, 1.6f, cast).octo(0, -6.4f, 0, 2.2f, 0.6f, 2.2f, brew);
        plague.sub(3, -5, 2.5f).octo(0, -8, 0, 1.6f, 8, 1.6f, cast).octo(0, -8.4f, 0, 2.2f, 0.6f, 2.2f, brew);
        plague.box(-4, -2, -4.2f, 8, 0.6f, 0.6f, p(Mat.METAL, 0x5A5C52).pattern(Pattern.CHAINMAIL, 0x8A8C80, 2f));
        // Lv5 B: Blightspore hive: pulsing pods, chitin plates, spore maw
        Paint flesh = p(Mat.FLESH, 0x6A8A3A).accent(0x3A5A1A).pattern(Pattern.VEINS, 0xC8FF60).accentGlow();
        Paint chitin = p(Mat.CHITIN, 0x4A2A4A).accent(0x2A142A).gloss(0.5f);
        Part pods = yaw.child("pods", 0, -1, 0).levels(6, 6);
        pods.sphere(-4, -3, 3, 2.2f, flesh).sphere(4, -2.6f, 3.4f, 2, flesh).sphere(0, -4.2f, 4.6f, 2.4f, flesh);
        pods.spike(-4, -5.5f, 3, 1.2f, 2.5f, 0, 0, -10, chitin).spike(4, -5, 3.4f, 1.1f, 2.4f, 0, 0, 10, chitin);
        Part maw = gun.child("sporemaw", 0, -0.6f, -10).levels(6, 6);
        maw.sphere(0, -2, 0, 2.6f, flesh);
        maw.box(-1.4f, -4.8f, -1.4f, 2.8f, 0.6f, 2.8f, glow(0xC8FF60));
        for (int i = 0; i < 5; i++) maw.spike(Mth.cos(i * 1.256f) * 2, -3.8f, Mth.sin(i * 1.256f) * 2, 0.8f, 2, 0, 0, 0, chitin);
    }

    // =====================================================================================
    // SIEGE MORTAR: sandbagged mortar pit -> Bombardment battery / Grand cannon
    // =====================================================================================

    private static void mortar(ModelSpec s) {
        Paint sand = p(Mat.CLOTH, 0xB8A070).accent(0x8A7850).grime(0.3f).decal(Face.FRONT, DecalType.STITCH, 0x5A4A30, false, 0.5f, 0.5f, 1f);
        Paint tube = p(Mat.DARK_METAL, 0x3E4046).pattern(Pattern.BANDS, 0x2A2C30, 0.8f);
        Part base = s.root.child("base", 0, 24, 0);
        for (int i = 0; i < 8; i++) {
            Part bag = base.sub(0, 0, 0).rot(0, i * 45, 0);
            bag.box(-3, -2.4f, -7.4f, 6, 2.4f, 3, sand);
            if (i % 2 == 0) bag.sub(0, 0, 0).from(2).box(-2.6f, -4.6f, -7.2f, 5.2f, 2.2f, 2.8f, sand);
        }
        Part crates = base.sub(4.5f, 0, 4.5f).from(2);
        crates.box(-1.5f, -3, -1.5f, 3, 3, 3, wood().decal(Face.FRONT, DecalType.EMBLEM, 0xC82020, false, 0.5f, 0.5f, 0.8f));
        crates.box(-1.2f, -5.2f, -1.2f, 2.4f, 2.2f, 2.4f, wood());
        Part shells = base.sub(-4.5f, 0, 4.5f).from(3);
        for (int i = 0; i < 3; i++) {
            shells.octo(i * 1.4f - 1.4f, -3, 0, 1.2f, 3, 1.2f, brass());
            shells.octo(i * 1.4f - 1.4f, -3.8f, 0, 0.8f, 0.8f, 0.8f, p(Mat.DARK_METAL, 0x3A3A3A));
        }

        Part yaw = s.root.child("yaw", 0, 21, 0);
        yaw.octo(0, -1.5f, 0, 9, 1.5f, 9, gunmetal().trim(0x55585F, 0.4f));
        yaw.box(-4.2f, -6, -1.6f, 1, 4.5f, 3.2f, gunmetal()).box(3.2f, -6, -1.6f, 1, 4.5f, 3.2f, gunmetal());
        yaw.sub(0, 0, 0).from(4).box(-5.2f, -6.5f, -3.5f, 1, 5, 7, plate(0x5A5E52)).box(4.2f, -6.5f, -3.5f, 1, 5, 7, plate(0x5A5E52));
        Part sight = yaw.child("sight", 3.6f, -6, 2).from(2);
        sight.box(-0.4f, -3, -0.4f, 0.8f, 3, 0.8f, iron()).octoZ(0, -3.4f, -1.5f, 1.2f, 3, brass());
        Part gun = yaw.child("gun", 0, -4.5f, 0);
        Part tube1 = gun.child("tube", 0, 0, 0).levels(1, 4);
        tube1.octoZ(0, 0, -9, 5, 12, tube);
        tube1.octoZ(0, 0, -10, 6, 1.4f, p(Mat.DARK_METAL, 0x2C2E32));
        tube1.box(-3.2f, -0.8f, -1, 6.4f, 1.6f, 1.6f, iron());
        tube1.sub(0, 0, 0).from(3).octoZ(0, 0, -6, 5.6f, 1, brass()).octoZ(0, 0, -2, 5.6f, 1, brass());
        tube1.box(-1.8f, -1.8f, -9.2f, 3.6f, 3.6f, 0.4f, p(Mat.PLAIN, 0x0C0C0C).flat());
        // Lv5 A: four-tube battery
        Part bat = gun.child("battery", 0, 0, 0).levels(5, 5);
        for (int i = 0; i < 4; i++) {
            float x = (i % 2) * 5 - 2.5f, y = (i / 2) * 5 - 2.5f;
            bat.octoZ(x, y, -9, 4.2f, 11, tube);
            bat.octoZ(x, y, -9.8f, 4.8f, 1, p(Mat.DARK_METAL, 0x2C2E32));
            bat.box(x - 1.5f, y - 1.5f, -9.9f, 3, 3, 0.3f, p(Mat.PLAIN, 0x0C0C0C).flat());
        }
        bat.box(-5.4f, -5.4f, -1, 10.8f, 10.8f, 1.4f, hazard());
        // Lv5 B: grand cannon on a wheeled recoil carriage
        Part grand = gun.child("grand", 0, 0, 0).levels(6, 6);
        grand.octoZ(0, 0, -18, 6, 20, p(Mat.DARK_METAL, 0x30333A).pattern(Pattern.BANDS, 0x1E2024, 0.5f).gloss(0.4f));
        grand.octoZ(0, 0, -20, 7.6f, 3, gunmetal());
        grand.box(-4, -1, -19.5f, 8, 2, 1.6f, p(Mat.PLAIN, 0x0C0C0C).flat());
        grand.octoZ(0, 0, 1, 7, 4, brass());
        grand.box(-2.4f, -2.4f, -20.1f, 4.8f, 4.8f, 0.3f, p(Mat.PLAIN, 0x0A0A0A).flat());
        Part wheels = yaw.child("wheels", 0, -2, 0).levels(6, 6);
        Paint wheel = darkWood().trim(C_IRON, 0.6f);
        wheels.sub(-5.4f, 0, 0).octoX(-0.8f, 0, 0, 6, 1.6f, wheel);
        wheels.sub(5.4f, 0, 0).octoX(-0.8f, 0, 0, 6, 1.6f, wheel);
    }

    // =====================================================================================
    // ARCANE PRISM: floating crystal held by marble claws -> Annihilation lance / Kaleidoscope
    // =====================================================================================

    private static void prism(ModelSpec s) {
        Paint marble = p(Mat.STONE, C_MARBLE).noise(0.05f).pattern(Pattern.CRACKS, 0xB8B0C8);
        Paint silver = p(Mat.METAL, 0xD0D4E0).gloss(0.75f);
        Paint crystal = p(Mat.CRYSTAL, 0xC86BFF).accent(0xF4DCFF).glow();
        Part base = s.root.child("base", 0, 24, 0);
        base.octo(0, -3, 0, 12, 3, 12, marble);
        base.octo(0, -4.5f, 0, 9, 1.5f, 9, marble.copy().pattern(Pattern.RUNES, 0xC86BFF).accentGlow());
        base.sub(0, 0, 0).octoRing(-2.2f, 12.4f, 0.5f, 0.6f, silver);
        Part clusters = base.sub(0, -3, 0).from(4);
        for (int i = 0; i < 4; i++) {
            float a = i * Mth.HALF_PI + Mth.PI / 4;
            clusters.spike(Mth.cos(a) * 5.4f, 0, Mth.sin(a) * 5.4f, 1.4f, 3.2f, Mth.sin(a) * 20, 0, -Mth.cos(a) * 20, crystal);
        }
        Part yaw = s.root.child("yaw", 0, 19.5f, 0);
        for (int k = 0; k < 3; k++) {
            Part arm = yaw.sub(0, 0, 0).rot(0, 120 * k, 0);
            Part up = arm.sub(0, -1, 3).rot(-14, 0, 0);
            up.box(-0.9f, -9, -0.9f, 1.8f, 9, 1.8f, marble);
            up.sub(0, -9, 0).rot(40, 0, 0).box(-0.7f, -4, -0.7f, 1.4f, 4, 1.4f, silver);
            up.sub(0, 0, 0).from(2).box(-1.1f, -6, -1.1f, 2.2f, 1, 2.2f, silver).box(-1.1f, -2, -1.1f, 2.2f, 1, 2.2f, silver);
        }
        Part rings = yaw.child("rings", 0, -13, 0).from(2).levels(2, 4);
        rings.octoRing(-0.3f, 12, 0.5f, 0.6f, silver);
        Part rings2 = yaw.child("rings2", 0, -13, 0).from(4).levels(4, 4);
        rings2.sub(0, 0, 0).rot(90, 0, 0).octoRing(-0.3f, 10, 0.5f, 0.6f, glow(0xE0A0FF));
        Part gun = yaw.child("gun", 0, -13, 0);
        Part gem = gun.child("gem", 0, 0, 0).levels(1, 6);
        gem.taper(0, 0, 0, 4.2f, 4.2f, 0.4f, 0.4f, 5.5f, 4, crystal);
        gem.sub(0, 0, 0).rot(180, 0, 0).taper(0, 0, 0, 4.2f, 4.2f, 0.4f, 0.4f, 4.5f, 4, crystal);
        gem.box(-1, -1, -1, 2, 2, 2, glow(0xFFFFFF));
        Part refr = yaw.child("refr", 0, -13, 0).levels(3, 5);
        Part rgem = refr.child("rgem", 7, 0, 0);
        rgem.taper(0, 0, 0, 1.8f, 1.8f, 0.3f, 0.3f, 2.6f, 2, crystal).sub(0, 0, 0).rot(180, 0, 0).taper(0, 0, 0, 1.8f, 1.8f, 0.3f, 0.3f, 2.2f, 2, crystal);

        // Lv5 A: Annihilation lance with focusing rings
        Part lance = gun.child("lance", 0, 0, 0).levels(5, 5);
        lance.sub(0, 0, -2).rot(90, 0, 0).taper(0, 0, 0, 2.6f, 2.6f, 0.4f, 0.4f, 13, 5, crystal);
        for (int i = 0; i < 3; i++) {
            lance.sub(0, 0, -5 - i * 4).rot(90, 0, 0).octoRing(-0.3f, 6.5f - i * 1.2f, 0.5f, 0.6f, i == 1 ? glow(0xF0C8FF) : silver);
        }
        lance.box(-2.4f, -2.4f, 2.2f, 4.8f, 4.8f, 1.6f, silver.copy().pattern(Pattern.RUNES, 0xC86BFF).accentGlow());
        // Lv5 B: Kaleidoscope of orbiting prisms and mirrors
        Part kal = yaw.child("kal", 0, -13, 0).levels(6, 6);
        for (int i = 0; i < 4; i++) {
            float a = i * Mth.HALF_PI;
            Part k = kal.child("k" + i, Mth.cos(a) * 7.5f, (i % 2) * 2 - 1, Mth.sin(a) * 7.5f);
            k.taper(0, 0, 0, 2.2f, 2.2f, 0.3f, 0.3f, 3.2f, 3, crystal);
            k.sub(0, 0, 0).rot(180, 0, 0).taper(0, 0, 0, 2.2f, 2.2f, 0.3f, 0.3f, 2.6f, 2, crystal);
            k.sub(0, 0, 0).rot(0, -i * 90, 0).box(-1.6f, -2.5f, 1.8f, 3.2f, 5, 0.3f, p(Mat.GLASS, 0xE8E0FF).accent(0xFFFFFF));
        }
        kal.octoRing(-0.3f, 15, 0.4f, 0.4f, glow(0xE0A0FF));
    }

    // =====================================================================================
    // GALE TOWER: stone windmill -> Cyclone pylon / Tempest Eye turbine
    // =====================================================================================

    private static void gale(ModelSpec s) {
        Paint stone = p(Mat.STONE, 0xD4D0C4).accent(0x7A9A6A).pattern(Pattern.BRICK, 0xA8A498, 1.2f);
        Paint roof = p(Mat.SCALES, 0x3E7A8A).accent(0x24505A);
        Paint sail = p(Mat.CLOTH, 0xF0ECE0).pattern(Pattern.STRIPES, 0xC8D8D0, 0.8f);
        Paint teal = glow(0x9FFFE8);
        Part base = s.root.child("base", 0, 24, 0);
        base.octo(0, -6, 0, 10, 6, 10, stone.copy().decal(Face.FRONT, DecalType.PANEL, 0x5A4A3A, false));
        base.box(-1.5f, -4, -5.1f, 3, 4, 0.4f, darkWood());
        base.sub(0, -6, 0).from(2).octoRing(-0.8f, 12, 1.2f, 0.8f, wood());
        Part poles = base.sub(0, -6, 0).from(4);
        for (int i = 0; i < 2; i++) {
            float x = i == 0 ? -5.5f : 5.5f;
            poles.box(x - 0.3f, -7, -0.3f, 0.6f, 7, 0.6f, darkWood());
            poles.box(x + (i == 0 ? -3 : 0.3f), -7, -0.1f, 2.7f, 3.6f, 0.2f, p(Mat.CLOTH, 0x3E9AAA).decal(Face.FRONT, DecalType.CHEVRON, 0xF0F0E0, false));
        }
        Part yaw = s.root.child("yaw", 0, 18, 0);
        yaw.octo(0, -6, 0, 8, 6, 8, stone);
        yaw.box(-1.5f, -4.5f, -4.2f, 3, 3, 0.4f, p(Mat.GLASS, 0x6AA8C8).accent(0xFFFFFF));
        Part roofP = yaw.child("roof", 0, -6, 0).levels(1, 4);
        roofP.taper(0, 0, 0, 10, 10, 1, 1, 6, 4, roof);
        roofP.spike(0, -6, 0, 0.6f, 2.5f, 0, 0, 0, iron());
        Part vane = yaw.child("fan2", 0, -12.5f, 0).from(3).levels(3, 4);
        for (int i = 0; i < 3; i++) vane.sub(0, 0, 0).rot(0, 120 * i, 0).box(0, -0.25f, -0.8f, 6, 0.5f, 1.6f, teal);
        Part gun = yaw.child("gun", 0, -3.5f, -4.5f);
        gun.box(-1.1f, -1.1f, -1.6f, 2.2f, 2.2f, 2.2f, iron());
        Part fan = gun.child("fan", 0, 0, -1.8f).levels(1, 5);
        for (int i = 0; i < 4; i++) {
            Part blade = fan.sub(0, 0, 0).rot(0, 0, 90 * i);
            blade.box(-0.3f, -10.5f, -0.3f, 0.6f, 10, 0.6f, wood());
            blade.box(-1.6f, -10, -0.15f, 3.2f, 8, 0.3f, sail);
            blade.sub(0, 0, 0).from(4).box(-1.7f, -10.6f, -0.2f, 3.4f, 0.8f, 0.4f, teal);
        }
        fan.box(-0.8f, -0.8f, -0.8f, 1.6f, 1.6f, 1.6f, brass());

        // Lv5 A: Cyclone pylon with orbiting sails and a spire
        Part cyc = yaw.child("cyc", 0, -6, 0).levels(5, 5);
        cyc.taper(0, 0, 0, 6, 6, 1, 1, 12, 5, stone);
        cyc.spike(0, -12, 0, 1, 3, 0, 0, 0, teal);
        Part sails = yaw.child("sails", 0, -6, 0).levels(5, 5);
        for (int i = 0; i < 3; i++) {
            Part sl = sails.sub(0, 0, 0).rot(0, 120 * i, 0);
            sl.box(6.2f, -14, -0.2f, 0.6f, 15, 0.6f, darkWood());
            sl.box(6.4f, -13, -2.4f, 0.3f, 12, 2.6f, p(Mat.CLOTH, 0x3E9AAA).pattern(Pattern.STRIPES, 0x9FFFE8, 0.6f).accentGlow());
            sl.box(0, -14.2f, -0.2f, 6.4f, 0.4f, 0.4f, iron());
        }
        // Lv5 B: Tempest Eye: ducted turbine with a glowing eye
        Part turbine = gun.child("turbine", 0, 0, -1.5f).levels(6, 6);
        turbine.sub(0, 0, 0).rot(90, 0, 0).octoRing(-3, 14, 1.6f, 4, p(Mat.METAL, 0xB8C4CC).gloss(0.6f).trim(0x3E9AAA, 0.5f));
        turbine.box(-1.8f, -1.8f, -1, 3.6f, 3.6f, 2, glow(0xC8FFF0).decal(Face.FRONT, DecalType.EYE_SIGIL, 0x1A6A6A, false));
        Part blades = turbine.child("tblades", 0, 0, 0);
        for (int i = 0; i < 8; i++) blades.sub(0, 0, 0).rot(0, 0, 45 * i).rot(15, 0, 45 * i).box(-0.8f, -6.2f, -0.15f, 1.6f, 5, 0.3f, p(Mat.METAL, 0xD8E0E4));
    }

    // =====================================================================================
    // SANCTUM OBELISK: rune-carved obelisk with halo -> Judgement angel / Sanctuary shrine
    // =====================================================================================

    private static void sanctum(ModelSpec s) {
        Paint marble = p(Mat.STONE, 0xF2EFE8).noise(0.04f);
        Paint runed = p(Mat.STONE, 0xF2EFE8).noise(0.04f).pattern(Pattern.RUNES, 0xFFD86A).accentGlow();
        Paint gold = p(Mat.GOLD, 0xE0B848).accent(0xFFF4C0);
        Paint holy = glow(0xFFF2B0);
        Paint feather = p(Mat.FEATHER, 0xFFFFFF).accent(0xD8D4C8);
        Part base = s.root.child("base", 0, 24, 0);
        base.octo(0, -2, 0, 14, 2, 14, marble);
        base.octo(0, -3.5f, 0, 10.5f, 1.5f, 10.5f, marble.copy().trim(0xE0B848, 0.4f));
        Part candles = base.sub(0, -3.5f, 0).from(2);
        for (int i = 0; i < 4; i++) {
            float a = i * Mth.HALF_PI + Mth.PI / 4;
            float x = Mth.cos(a) * 6.2f, z = Mth.sin(a) * 6.2f;
            candles.octo(x, 1.5f - 3, z, 1, 1.5f + (i % 2), 1, p(Mat.PLAIN, 0xF4ECD8));
            candles.box(x - 0.25f, -2.4f - (i % 2), z - 0.25f, 0.5f, 0.9f, 0.5f, glow(0xFFC040));
        }
        Part braziers = base.sub(0, -2, 0).from(4);
        braziers.sub(-6, 0, -6).octo(0, -3, 0, 2, 3, 2, gold).octo(0, -3.6f, 0, 2.8f, 0.6f, 2.8f, gold).octo(0, -4.6f, 0, 1.6f, 1, 1.6f, holy);
        braziers.sub(6, 0, -6).octo(0, -3, 0, 2, 3, 2, gold).octo(0, -3.6f, 0, 2.8f, 0.6f, 2.8f, gold).octo(0, -4.6f, 0, 1.6f, 1, 1.6f, holy);

        Part yaw = s.root.child("yaw", 0, 20.5f, 0);
        yaw.taper(0, 0, 0, 5, 5, 3, 3, 16, 4, runed);
        yaw.taper(0, -16, 0, 3, 3, 0.4f, 0.4f, 3, 3, gold);
        yaw.octo(0, -1, 0, 6, 1, 6, gold);
        Part wings = yaw.sub(0, -10, 1.8f).from(3);
        wings.sub(-1.4f, 0, 0).rot(0, 15, -20).box(-9, -1, -0.4f, 9, 7, 0.8f, feather).sub(-9, 0, 0).rot(0, 0, 20).box(-4, -0.5f, -0.3f, 4, 4, 0.6f, feather);
        wings.sub(1.4f, 0, 0).rot(0, -15, 20).box(0, -1, -0.4f, 9, 7, 0.8f, feather).sub(9, 0, 0).rot(0, 0, -20).box(0, -0.5f, -0.3f, 4, 4, 0.6f, feather);
        Part gun = yaw.child("gun", 0, -23, 0);
        Part halo = gun.child("halo", 0, 0, 0).levels(1, 4);
        halo.octoRing(-0.3f, 9, 0.7f, 0.6f, holy);
        halo.sphere(0, 0, 0, 1.3f, glow(0xFFFFFF));
        Part halo2 = yaw.child("halo2", 0, -23, 0).levels(4, 4);
        halo2.sub(0, 0, 0).rot(70, 0, 0).octoRing(-0.2f, 12, 0.5f, 0.4f, glow(0xFFE8A0));

        // Lv5 A: Judgement: a marble angel raising a blazing sword
        Part angel = gun.child("angel", 0, 3, 0).levels(5, 5);
        Paint statue = p(Mat.STONE, 0xEEEAE2).noise(0.035f);
        angel.box(-1.6f, -7, -1, 3.2f, 7, 2, p(Mat.ROBE, 0xEEEAE2).accent(0xC8C4BA));
        angel.box(-1.8f, -1, -1.2f, 3.6f, 3, 2.4f, p(Mat.ROBE, 0xEEEAE2).accent(0xC8C4BA));
        angel.box(-1.2f, -9.6f, -1.2f, 2.4f, 2.6f, 2.4f, statue.copy().decal(Face.FRONT, DecalType.EYES, 0xFFF2B0, true, 0.5f, 0.45f, 1f));
        angel.sub(0, -10.6f, 0).octoRing(-0.2f, 3.6f, 0.4f, 0.3f, holy);
        Part arm = angel.child("sword_arm", 2, -6.5f, 0).rot(0, 0, -150);
        arm.box(-0.5f, 0, -0.5f, 1, 4, 1, statue);
        Part sword = arm.child("sword", 0, 4, 0);
        sword.box(-0.4f, 0, -1.2f, 0.8f, 0.6f, 2.4f, gold);
        sword.box(-0.25f, 0.6f, -0.5f, 0.5f, 9, 1, holy);
        angel.sub(-2, -6.5f, 0).rot(-30, 0, 20).box(-0.5f, 0, -0.5f, 1, 4, 1, statue);
        angel.sub(-1, -6, 1).rot(0, 30, -30).box(-7, -2, -0.3f, 7, 6, 0.6f, feather);
        angel.sub(1, -6, 1).rot(0, -30, 30).box(0, -2, -0.3f, 7, 6, 0.6f, feather);
        // Lv5 B: Sanctuary: columned canopy with dome, bell and a radiant heart
        Part sanct = yaw.child("sanct", 0, 0, 0).levels(6, 6);
        for (int i = 0; i < 4; i++) {
            float a = i * Mth.HALF_PI + Mth.PI / 4;
            sanct.octo(Mth.cos(a) * 6.5f, -18, Mth.sin(a) * 6.5f, 1.6f, 18, 1.6f, marble.copy().trim(0xE0B848, 0.3f));
        }
        sanct.octo(0, -19.5f, 0, 16, 1.5f, 16, marble.copy().trim(0xE0B848, 0.5f));
        sanct.taper(0, -19.5f, 0, 13, 13, 3, 3, 5, 4, gold);
        sanct.spike(0, -24.5f, 0, 1.4f, 3, 0, 0, 0, holy);
        Part bell = sanct.child("bell", 0, -19.5f, 0);
        bell.box(-0.2f, 0, -0.2f, 0.4f, 2, 0.4f, iron());
        bell.taper(0, 5, 0, 3.2f, 3.2f, 1.6f, 1.6f, 3, 3, gold);
        Part heart = gun.child("heart", 0, 0, 0).levels(6, 6);
        heart.sub(0, 0, 0).rot(45, 0, 45).box(-1.8f, -1.8f, -1.8f, 3.6f, 3.6f, 3.6f, p(Mat.CRYSTAL, 0xFFE8A0).accent(0xFFFFFF).glow());
        heart.sphere(0, 0, 0, 1.2f, glow(0xFFFFFF));
    }

    // =====================================================================================
    // animation
    // =====================================================================================

    public static void animate(ProcModel<TurretEntity> m, TurretEntity t, float ls, float la, float age, float hy, float hp, float pt) {
        ModelPart yaw = m.p("yaw"), gun = m.p("gun");
        int ml = m.level;
        float yawDeg = Mth.rotLerp(pt, t.clientYawO, t.clientYaw);
        float pitchDeg = Mth.lerp(pt, t.clientPitchO, t.clientPitch);
        yaw.yRot = Mth.wrapDegrees(yawDeg - Mth.rotLerp(pt, t.yBodyRotO, t.yBodyRot)) * Anim.D;
        int id = t.anim.id();
        float at = t.anim.time(pt);
        float spin = Mth.lerp(pt, t.spinO, t.spin);
        float recoil = (id == 1 || id == 2) ? Math.max(0, 1 - at / 6f) : 0;
        float kick = recoil * recoil;
        float charge = id == 3 ? Mth.clamp(at / 20f, 0, 1) : 0;
        float release = id == 4 ? Math.max(0, 1 - at / 10f) : 0;

        // deploy: the turret assembles itself, rising out of its foundation
        if (id == 8) {
            float d = Anim.easeOut(Anim.win(at, 0, 26));
            yaw.y += (1 - d) * 10;
            yaw.yRot += (1 - d) * Mth.PI;
            gun.xRot += (1 - d) * 0.8f;
        }
        // upgrade: lift, spin and settle
        if (id == 9) {
            float up = Anim.pulse(at, 0, 20);
            yaw.y -= up * 3;
            yaw.yRot += Anim.easeOut(Anim.win(at, 0, 20)) * Mth.TWO_PI;
        }

        switch (t.kind) {
            case BALLISTA -> {
                gun.xRot += pitchDeg * Anim.D;
                gun.z += kick * 1.2f;
                ModelPart str = m.p("string");
                str.z += -kick * 1.6f + Math.max(0, 0.5f - recoil) * 0;
                float flex = kick * 0.18f;
                m.p("bow_r").yRot -= flex;
                m.p("bow_l").yRot += flex;
                float reload = (id == 1 || id == 2) ? Anim.win(at, 8, 10) : 1;
                ModelPart bolt = m.opt("bolt");
                if (bolt != null) {
                    bolt.visible = bolt.visible && reload > 0;
                    bolt.z += (1 - Anim.easeOut(reload)) * 7;
                }
                ModelPart b2 = m.opt("bolt2");
                if (b2 != null) b2.visible = reload > 0.5f || id == 0;
                ModelPart har = m.opt("harpoon");
                if (har != null && har.visible) {
                    har.visible = reload > 0;
                    har.z += (1 - Anim.easeOut(reload)) * 9;
                    m.p("winch").xRot = -spin * 0.6f;
                }
                ModelPart drum = m.opt("drum");
                if (drum != null && drum.visible) {
                    drum.zRot = t.clientShots * (Mth.TWO_PI / 6) + (id == 1 ? -(1 - Anim.easeOut(Anim.win(at, 0, 3))) * (Mth.TWO_PI / 6) : 0);
                    m.p("lever").xRot = -Anim.pulse(at % 4, 0, 4) * 0.9f * (id == 1 && at < 12 ? 1 : 0);
                    ModelPart rb = m.p("rbolt");
                    rb.visible = !(id == 1 && at % 4 < 1.5f && at < 12);
                }
                ModelPart mant = m.opt("mantlet");
                if (mant != null) mant.xRot = -pitchDeg * Anim.D * 0.2f;
            }
            case FLAME -> {
                gun.xRot += pitchDeg * Anim.D;
                boolean firing = t.isActive();
                float shake = firing ? Mth.sin(age * 2.7f) * 0.25f : 0;
                gun.y += shake;
                ModelPart jaw = m.opt("jaw");
                if (jaw != null) jaw.xRot = firing ? 0.55f + Mth.sin(age * 0.9f) * 0.08f : 0.08f;
                ModelPart cd = m.opt("cdrum");
                if (cd != null) {
                    cd.zRot = t.clientShots * (Mth.TWO_PI / 5) - (id == 1 ? (1 - Anim.easeOut(Anim.win(at, 0, 6))) * (Mth.TWO_PI / 5) : 0);
                    gun.z += kick * 1.5f;
                }
                ModelPart pilot = m.opt("pilot");
                if (pilot != null) pilot.xScale = pilot.yScale = pilot.zScale = 0.8f + 0.25f * Mth.sin(age * 1.3f);
            }
            case FROST -> {
                ModelPart orbit = m.opt("orbit");
                if (orbit != null) {
                    orbit.yRot = spin * 0.5f;
                    orbit.y += Mth.sin(age * 0.05f) * 0.5f;
                }
                ModelPart focus = m.p("focus");
                focus.yRot = age * 0.03f + spin * 0.3f;
                focus.y += Mth.sin(age * 0.1f) * 0.7f - kick;
                float f = 1 + kick * 0.15f + charge * 0.3f;
                focus.xScale = focus.yScale = focus.zScale = f;
                ModelPart cage = m.opt("cage");
                if (cage != null) cage.yRot = -age * 0.02f;
                ModelPart halo = m.opt("frosthalo");
                if (halo != null && halo.visible) {
                    halo.yRot = age * 0.015f + spin * 0.2f;
                    halo.y += Mth.sin(age * 0.06f) * 0.8f;
                    float pulse = 1 + release * 0.35f;
                    halo.xScale = halo.zScale = pulse;
                }
                ModelPart lance = m.opt("lance");
                if (lance != null && lance.visible) {
                    gun.xRot += pitchDeg * Anim.D;
                    focus.y = 0;
                    gun.z += release * 2.5f;
                    lance.zRot = spin * 0.8f;
                }
            }
            case TIDE -> {
                gun.xRot += pitchDeg * Anim.D;
                gun.z += kick * 1.4f;
                m.p("water").y += Mth.sin(age * 0.12f) * 0.25f;
                ModelPart conch = m.opt("conch");
                if (conch != null) conch.zRot = Mth.sin(age * 0.05f) * 0.05f;
                ModelPart ra = m.opt("ringA"), rb = m.opt("ringB");
                if (ra != null && ra.visible) {
                    ra.yRot = spin;
                    ra.xRot = Mth.sin(age * 0.04f) * 0.25f;
                    rb.yRot = -spin * 1.6f;
                    rb.zRot = Mth.cos(age * 0.05f) * 0.3f;
                    ModelPart sp = m.p("spout");
                    sp.yRot = spin * 2;
                    sp.yScale = 1 + Mth.sin(age * 0.2f) * 0.12f;
                }
                ModelPart lj = m.opt("ljaw");
                if (lj != null) lj.xRot = 0.1f + kick * 0.7f + release * 0.9f + Mth.sin(age * 0.07f) * 0.05f;
            }
            case TESLA -> {
                gun.yRot = spin;
                float f = 1 + kick * 0.3f + Mth.sin(age * 0.8f) * 0.04f + charge * 0.4f;
                gun.xScale = gun.yScale = gun.zScale = f;
                m.p("toroid").yRot = -spin * 0.3f;
                ModelPart t2 = m.opt("toroid2");
                if (t2 != null) t2.yRot = spin * 0.4f;
                for (int i = 0; i < 3; i++) {
                    ModelPart r = m.opt("sring" + i);
                    if (r != null && r.visible) {
                        r.yRot = spin * (i % 2 == 0 ? 0.6f : -0.9f);
                        r.y += Mth.sin(age * 0.08f + i) * 0.7f - charge * 2 * i;
                    }
                }
                ModelPart g1 = m.opt("gyro1");
                if (g1 != null && g1.visible) {
                    g1.xRot = spin * 1.3f;
                    m.p("gyro2").zRot = spin * 1.7f;
                    m.p("gyro3").yRot = -spin * 0.9f;
                }
            }
            case VENOM -> {
                float sling = (id == 1 || id == 2) ? Anim.swing(at, 4, 3, 14) : 0;
                gun.xRot += 0.45f - 1.5f * Math.max(0, -sling) + 0.35f * Math.max(0, sling);
                ModelPart glob = m.opt("glob");
                if (glob != null) glob.visible = !(id == 1 && at > 6 && at < 18);
                ModelPart w = m.p("weight");
                w.xRot = -gun.xRot;
                ModelPart pods = m.opt("pods");
                if (pods != null && pods.visible) {
                    float b = 1 + Mth.sin(age * 0.15f) * 0.05f + kick * 0.1f;
                    pods.xScale = pods.zScale = b;
                    pods.yScale = 2 - b;
                }
            }
            case MORTAR -> {
                float elev = -55 + Mth.clamp(-pitchDeg, -20, 20) * 0.4f;
                gun.xRot += elev * Anim.D;
                gun.z += kick * 1.6f;
                gun.y += kick * 0.8f;
                ModelPart bat = m.opt("battery");
                if (bat != null && bat.visible) bat.zRot = (id == 3 ? charge : 0) * 0.2f * Mth.sin(age * 2);
                ModelPart wh = m.opt("wheels");
                if (wh != null && wh.visible) {
                    gun.xRot = (-30 + Mth.clamp(-pitchDeg, -15, 25)) * Anim.D;
                    gun.z += kick * 2.5f;
                    wh.xRot = -kick * 0.6f;
                }
            }
            case PRISM -> {
                ModelPart gem = m.p("gem");
                gem.yRot = spin;
                gem.y += Mth.sin(age * 0.08f) * 0.8f;
                float f = 1 + (t.isActive() ? 0.12f + Mth.sin(age * 0.9f) * 0.05f : 0);
                gem.xScale = gem.yScale = gem.zScale = f;
                ModelPart rings = m.opt("rings");
                if (rings != null) {
                    rings.yRot = -spin * 0.4f;
                    rings.xRot = Mth.sin(age * 0.04f) * 0.4f;
                }
                ModelPart r2 = m.opt("rings2");
                if (r2 != null) r2.yRot = spin * 0.3f;
                ModelPart refr = m.opt("refr");
                if (refr != null) refr.yRot = spin * 0.5f;
                ModelPart lance = m.opt("lance");
                if (lance != null && lance.visible) {
                    gun.xRot += pitchDeg * Anim.D;
                    gem.yRot = 0;
                    lance.zRot = spin * 0.5f;
                }
                ModelPart kal = m.opt("kal");
                if (kal != null && kal.visible) {
                    kal.yRot = spin * 0.35f;
                    for (int i = 0; i < 4; i++) m.p("k" + i).yRot = -spin;
                }
            }
            case GALE -> {
                ModelPart fan = m.opt("fan");
                if (fan != null) fan.zRot = spin;
                ModelPart f2 = m.opt("fan2");
                if (f2 != null) f2.yRot = spin * 0.6f;
                ModelPart sails = m.opt("sails");
                if (sails != null) sails.yRot = spin * 0.5f;
                ModelPart tb = m.opt("tblades");
                if (tb != null) tb.zRot = spin * 2.2f;
                gun.xRot += pitchDeg * Anim.D * 0.5f;
            }
            case SANCTUM -> {
                gun.y += Mth.sin(age * 0.06f) * 0.8f;
                ModelPart halo = m.opt("halo");
                if (halo != null) halo.yRot = spin * 0.4f;
                ModelPart h2 = m.opt("halo2");
                if (h2 != null) h2.yRot = -age * 0.03f;
                ModelPart heart = m.opt("heart");
                if (heart != null && heart.visible) {
                    heart.yRot = spin * 0.5f;
                    float f = 1 + Mth.sin(age * 0.12f) * 0.08f + kick * 0.2f;
                    heart.xScale = heart.yScale = heart.zScale = f;
                    m.p("bell").zRot = Mth.sin(age * 0.09f) * 0.12f + kick * Mth.sin(age * 1.2f) * 0.3f;
                }
                ModelPart arm = m.opt("sword_arm");
                if (arm != null && arm.visible) {
                    gun.yRot = 0;
                    arm.zRot += -charge * 0.6f + release * 1.6f;
                }
            }
        }
    }

    public static ProcRenderer<TurretEntity> renderer(EntityRendererProvider.Context ctx, TurretKind kind) {
        return new ProcRenderer<>(ctx, baked(kind), TurretModels::animate, 0.6f)
                .level(TurretEntity::modelLevel)
                .scaleBy(t -> TurretKind.SCALE[Mth.clamp(t.turretLevel(), 1, 5) - 1])
                .tint((t, c) -> {
                    if (t.hasEffect(com.lastbastion.registry.ModEffects.JAMMED.get())) {
                        c[0] = c[1] = c[2] = 0.55f;
                    }
                })
                .glowAlpha(t -> t.hasEffect(com.lastbastion.registry.ModEffects.JAMMED.get()) ? 0.2f
                        : 0.85f + 0.15f * Mth.sin((t.tickCount) * 0.15f));
    }
}
