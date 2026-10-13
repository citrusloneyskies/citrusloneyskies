package com.lastbastion.client.model;

import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.render.ProcRenderer;
import com.lastbastion.entity.zombie.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

import static com.lastbastion.client.model.Anim.D;

/**
 * Models and animations for the twelve zombie archetypes. Every zombie gets rank-gated gear:
 * Armored (rank 3) adds rusted helmets and pauldrons, Corrupted (4) adds glowing void crystals and
 * Abyssal (5) adds horns, burning red eyes and a crown of hellfire.
 */
public final class ZombieModels {
    private static final Map<ZombieKind, ModelSpec.Baked> CACHE = new EnumMap<>(ZombieKind.class);

    static final int SKIN = 0x6E8B5E, SKIN_DARK = 0x4F6644, ROT = 0x3E4F33, BLOOD = 0x7A1010, BONE = 0xD8D0B8,
            SHIRT = 0x4B6584, PANTS = 0x4A3B2A, IRON = 0x7A7268, RUST = 0x8A5530, EYE = 0xFFE066;

    private ZombieModels() {
    }

    static Paint skin() {
        return Paint.of(SKIN).pattern(Pattern.SPOTS, ROT).noise(0.14f);
    }

    static Paint zhead(int eye) {
        return Paint.of(SKIN).pattern(Pattern.SPOTS, ROT).noise(0.12f).eyes(eye, true)
                .face(Face.BOTTOM, DecalType.MOUTH, 0x2A0A0A, false);
    }

    // ------------------------------------------------------------------ specs

    public static ModelSpec.Baked baked(ZombieKind k) {
        return CACHE.computeIfAbsent(k, kind -> spec(kind).bake());
    }

    private static ModelSpec spec(ZombieKind k) {
        ModelSpec s = new ModelSpec("zombie_" + k.id);
        switch (k) {
            case SHAMBLER -> shambler(s);
            case RUNNER -> runner(s);
            case SPITTER -> spitter(s);
            case BRUTE -> brute(s);
            case BLOATER -> bloater(s);
            case LEAPER -> leaper(s);
            case BULWARK -> bulwark(s);
            case BURROWER -> burrower(s);
            case SHRIEKER -> shrieker(s);
            case WRAITH -> wraith(s);
            case WRECKER -> wrecker(s);
            case NECROMANCER -> necromancer(s);
        }
        return s;
    }

    /** Rank gear shared by humanoid zombies. headTop is the Y of the top of the head, relative to the head pivot. */
    static void rankGear(Rig r, float headSize, float scale) {
        float hs = headSize / 2;
        // Rank 3+: rusted iron helmet and pauldrons
        r.head.child("rk_helm", 0, 0, 0).from(3)
                .box(-hs, -headSize, -hs, headSize, headSize * 0.45f, headSize, 0.6f, Paint.of(IRON).pattern(Pattern.PLATES, RUST));
        r.rarm.child("rk_paul_r", 0, 0, 0).from(3)
                .box(-2.5f * scale, -3 * scale, -2.5f * scale, 5 * scale, 4 * scale, 5 * scale, 0.2f, Paint.of(IRON).pattern(Pattern.PLATES, RUST));
        r.larm.child("rk_paul_l", 0, 0, 0).from(3)
                .box(-2.5f * scale, -3 * scale, -2.5f * scale, 5 * scale, 4 * scale, 5 * scale, 0.2f, Paint.of(IRON).pattern(Pattern.PLATES, RUST));
        // Rank 4+: void crystals erupting from the back
        Paint crystal = Paint.glowing(0xB050FF);
        Part growth = r.body.child("rk_growth", 0, 2 * scale, 2 * scale).from(4);
        growth.child("rk_cr1", -2 * scale, 0, 0).rot(-30, 0, -20).box(-1, -5, -1, 2, 5, 2, crystal);
        growth.child("rk_cr2", 2 * scale, 2, 0).rot(-40, 0, 25).box(-1, -6, -1, 2, 6, 2, crystal);
        growth.child("rk_cr3", 0, 5 * scale, 0).rot(-55, 0, 0).box(-1, -4, -1, 2, 4, 2, crystal);
        // Rank 5: abyssal horns, burning eyes and a crown of hellfire
        Paint horn = Paint.of(0x2A1A1A).pattern(Pattern.BANDS, 0x401515);
        r.head.child("rk_horn_r", -hs + 1, -headSize + 1, 0).from(5).rot(0, 0, -35).box(-1, -6, -1, 2, 6, 2, horn);
        r.head.child("rk_horn_l", hs - 1, -headSize + 1, 0).from(5).rot(0, 0, 35).box(-1, -6, -1, 2, 6, 2, horn);
        r.head.child("rk_eyes", 0, 0, 0).from(5)
                .box(-hs + 1, -headSize * 0.62f, -hs - 0.15f, headSize - 2, 1.2f, 0.1f, Paint.glowing(0xFF2A10).flat());
        Paint fire = Paint.glowing(0xFF7A1A).pattern(Pattern.GRADIENT, 0xFFE040);
        Part crown = r.head.child("rk_crown", 0, -headSize - 0.5f, 0).from(5);
        crown.box(-hs, -2, -hs, 1.5f, 2, 1.5f, fire).box(hs - 1.5f, -3, -hs, 1.5f, 3, 1.5f, fire)
                .box(-hs, -3, hs - 1.5f, 1.5f, 3, 1.5f, fire).box(hs - 1.5f, -2, hs - 1.5f, 1.5f, 2, 1.5f, fire);
    }

    private static void shambler(ModelSpec s) {
        Rig r = Rig.humanoid(s, 1f, 4, skin(), Paint.of(SHIRT).pattern(Pattern.CLOTH, 0x3A4F66).noise(0.18f),
                Paint.of(PANTS).pattern(Pattern.CLOTH, 0x3A2E20), zhead(EYE));
        // torn shirt flap and exposed ribs
        r.body.child("ribs", 0, 4, -2.1f).box(-3, 0, 0, 3, 4, 0.2f, Paint.of(BONE).pattern(Pattern.RIBS, 0x5A1010).flat());
        r.body.child("flap", 2, 12, -2.05f).rot(10, 0, 0).box(-2, 0, 0, 4, 3, 0.2f, Paint.of(SHIRT).dark(0.85f));
        r.head.child("jaw", 0, -1.5f, -1).rot(15, 0, 0).box(-3, 0, -3, 6, 2, 4, Paint.of(SKIN_DARK).face(Face.FRONT, DecalType.GRIN, 0xD8D0B8, false));
        rankGear(r, 8, 1);
    }

    private static void runner(ModelSpec s) {
        Paint sk = Paint.of(0x7E8F6A).pattern(Pattern.SPOTS, BLOOD).noise(0.15f);
        ModelSpec.Part body = s.root.child("body", 0, 4, 0).rot(25, 0, 0).box(-3, 0, -1.5f, 6, 11, 3, Paint.of(0x5A6B4A).pattern(Pattern.RIBS, 0x3A4A30));
        Part head = s.root.child("head", 0, 4, -1).box(-3.5f, -7, -3.5f, 7, 7, 7, Paint.of(0x7E8F6A).pattern(Pattern.SPOTS, BLOOD)
                .eyes(0xFF3020, true).face(Face.BOTTOM, DecalType.MOUTH, BLOOD, false));
        head.child("r_jaw", 0, -1, -2).rot(20, 0, 0).box(-2.5f, 0, -1.5f, 5, 2, 3, Paint.of(BLOOD).face(Face.FRONT, DecalType.GRIN, BONE, false));
        Part rarm = s.root.child("rarm", -4.5f, 6, 0).box(-1.5f, -1, -1.5f, 3, 15, 3, sk);
        Part larm = s.root.child("larm", 4.5f, 6, 0).box(-1.5f, -1, -1.5f, 3, 15, 3, sk);
        rarm.child("claws_r", 0, 14, 0).box(-1.5f, 0, -1.5f, 3, 2, 3, Paint.of(BONE));
        larm.child("claws_l", 0, 14, 0).box(-1.5f, 0, -1.5f, 3, 2, 3, Paint.of(BONE));
        s.root.child("rleg", -1.6f, 13, 1).box(-1.5f, 0, -1.5f, 3, 11, 3, Paint.of(0x3A3A3A).pattern(Pattern.CLOTH, 0x2A2A2A));
        s.root.child("lleg", 1.6f, 13, 1).box(-1.5f, 0, -1.5f, 3, 11, 3, Paint.of(0x3A3A3A).pattern(Pattern.CLOTH, 0x2A2A2A));
        Rig r = rigOf(s, body, head, rarm, larm);
        rankGear(r, 7, 0.8f);
    }

    private static void spitter(ModelSpec s) {
        Rig r = Rig.humanoid(s, 1f, 4, skin(), Paint.of(0x5E6B3A).pattern(Pattern.CLOTH, 0x4A5530), Paint.of(PANTS), zhead(0x9BFF50));
        Paint sac = Paint.of(0x9BD940).pattern(Pattern.VEINS, 0xD8FF70).noise(0.2f);
        r.head.child("sac", 0, -1, -2).box(-4.5f, 0, -3, 9, 6, 6, sac);
        r.head.child("pustule1", -3, 2, -5).box(-1, -1, -1, 2, 2, 2, Paint.glowing(0xC8FF60));
        r.head.child("pustule2", 2.5f, 3, -4.5f).box(-1, -1, -1, 2, 2, 2, Paint.glowing(0xC8FF60));
        r.head.child("spit_jaw", 0, -1.5f, -3).box(-3, 0, -2, 6, 2, 3, Paint.of(SKIN_DARK).face(Face.FRONT, DecalType.MOUTH, 0x9BFF50, true));
        r.body.child("drip", 0, 0, -2.1f).box(-2, 0, 0, 4, 8, 0.2f, Paint.of(0x9BD940).pattern(Pattern.STRIPES, 0x6B9930).flat());
        rankGear(r, 8, 1);
    }

    private static void brute(ModelSpec s) {
        Paint sk = Paint.of(0x7A8A62).pattern(Pattern.CRACKS, 0x5A2020).noise(0.15f);
        Part body = s.root.child("body", 0, -8, 0).rot(10, 0, 0).box(-9, 0, -5, 18, 18, 10, sk);
        body.child("hump", 0, 0, 3).box(-6, -3, -2, 12, 6, 6, Paint.of(0x6A7A52).pattern(Pattern.SPOTS, ROT));
        body.child("belt", 0, 15, 0).box(-9, 0, -5, 18, 3, 10, 0.3f, Paint.of(0x4A3020).pattern(Pattern.PLATES, 0xA08040));
        body.child("spike1", -4, 0, 4).rot(-35, 0, 0).box(-1, -6, -1, 2, 6, 2, Paint.of(BONE));
        body.child("spike2", 3, 1, 4).rot(-35, 0, 10).box(-1, -7, -1, 2, 7, 2, Paint.of(BONE));
        Part head = s.root.child("head", 0, -6, -4).box(-4, -7, -4, 8, 8, 8, Paint.of(0x7A8A62).pattern(Pattern.SPOTS, ROT)
                .eyes(0xFF5020, true).face(Face.BOTTOM, DecalType.GRIN, BONE, false));
        head.child("underbite", 0, 0, -3).box(-4, -1, -2, 8, 3, 3, Paint.of(0x6A7A52).face(Face.FRONT, DecalType.GRIN, BONE, false));
        Part rarm = s.root.child("rarm", -12, -4, 0).box(-3.5f, -2, -3.5f, 7, 22, 7, sk);
        Part larm = s.root.child("larm", 12, -4, 0).box(-3.5f, -2, -3.5f, 7, 22, 7, sk);
        rarm.child("fist_r", 0, 20, 0).box(-4.5f, 0, -4.5f, 9, 6, 9, Paint.of(0x6A7A52).pattern(Pattern.CRACKS, BLOOD));
        larm.child("fist_l", 0, 20, 0).box(-4.5f, 0, -4.5f, 9, 6, 9, Paint.of(0x6A7A52).pattern(Pattern.CRACKS, BLOOD));
        larm.child("bone_l", 2, 8, 3).rot(-20, 0, 30).box(-1, -6, -1, 2, 6, 2, Paint.of(BONE));
        s.root.child("rleg", -5, 10, 0).box(-3.5f, 0, -3.5f, 7, 14, 7, Paint.of(PANTS).pattern(Pattern.CLOTH, 0x3A2E20));
        s.root.child("lleg", 5, 10, 0).box(-3.5f, 0, -3.5f, 7, 14, 7, Paint.of(PANTS).pattern(Pattern.CLOTH, 0x3A2E20));
        Rig r = rigOf(s, body, head, rarm, larm);
        rankGear(r, 8, 1.6f);
    }

    private static void bloater(ModelSpec s) {
        Paint belly = Paint.of(0x8FA860).pattern(Pattern.VEINS, 0xB8FF60).noise(0.16f);
        Part body = s.root.child("body", 0, 2, 0).box(-9, 0, -8, 18, 17, 16, belly);
        body.child("p1", -6, 4, -8.5f).box(-1.5f, -1.5f, -1, 3, 3, 2, Paint.glowing(0xC8FF60));
        body.child("p2", 5, 10, -8.5f).box(-2, -2, -1, 4, 4, 2, Paint.glowing(0xC8FF60));
        body.child("p3", 8.5f, 6, 2).box(-1, -2, -2, 2, 4, 4, Paint.glowing(0xC8FF60));
        body.child("p4", -2, 15, -8.3f).box(-1, -1, -1, 2, 2, 2, Paint.glowing(0xC8FF60));
        body.child("stitch", 0, 3, -8.2f).box(-0.5f, 0, 0, 1, 12, 0.3f, Paint.of(0x2A1A10).pattern(Pattern.BANDS, BONE).flat());
        Part head = s.root.child("head", 0, 2, -2).box(-3.5f, -6, -3.5f, 7, 6, 7, zhead(0xC8FF60));
        Part rarm = s.root.child("rarm", -10, 5, 0).box(-2, -1, -2, 4, 10, 4, skin());
        Part larm = s.root.child("larm", 10, 5, 0).box(-2, -1, -2, 4, 10, 4, skin());
        s.root.child("rleg", -4, 19, 0).box(-2.5f, 0, -2.5f, 5, 5, 5, Paint.of(PANTS));
        s.root.child("lleg", 4, 19, 0).box(-2.5f, 0, -2.5f, 5, 5, 5, Paint.of(PANTS));
        Rig r = rigOf(s, body, head, rarm, larm);
        rankGear(r, 6, 1f);
    }

    private static void leaper(ModelSpec s) {
        Paint sk = Paint.of(0x8A9A72).pattern(Pattern.SPOTS, 0x4A5A3A).noise(0.15f);
        Part body = s.root.child("body", 0, 6, 0).rot(35, 0, 0).box(-3.5f, 0, -2, 7, 10, 4, Paint.of(0x6A7A52).pattern(Pattern.RIBS, 0x4A5A3A));
        Part head = s.root.child("head", 0, 6, -2).box(-3.5f, -7, -4, 7, 7, 7, Paint.of(0x8A9A72).pattern(Pattern.SPOTS, BLOOD)
                .face(Face.FRONT, DecalType.SLIT_EYES, 0xFFB020, true));
        Part rarm = s.root.child("rarm", -5, 8, -1).box(-1.5f, -1, -1.5f, 3, 14, 3, sk);
        Part larm = s.root.child("larm", 5, 8, -1).box(-1.5f, -1, -1.5f, 3, 14, 3, sk);
        rarm.child("claw_r", 0, 13, 0).box(-2, 0, -2, 4, 4, 1, Paint.of(BONE));
        larm.child("claw_l", 0, 13, 0).box(-2, 0, -2, 4, 4, 1, Paint.of(BONE));
        Part rthigh = s.root.child("rleg", -2.5f, 14, 0).rot(-40, 0, 0).box(-1.5f, 0, -1.5f, 3, 7, 3, sk);
        Part lthigh = s.root.child("lleg", 2.5f, 14, 0).rot(-40, 0, 0).box(-1.5f, 0, -1.5f, 3, 7, 3, sk);
        rthigh.child("rshin", 0, 7, 0).rot(80, 0, 0).box(-1.5f, 0, -1.5f, 3, 7, 3, sk);
        lthigh.child("lshin", 0, 7, 0).rot(80, 0, 0).box(-1.5f, 0, -1.5f, 3, 7, 3, sk);
        Rig r = rigOf(s, body, head, rarm, larm);
        rankGear(r, 7, 0.8f);
    }

    private static void bulwark(ModelSpec s) {
        Paint plate = Paint.of(0x6A6A70).pattern(Pattern.PLATES, 0x9A9AA0);
        Rig r = Rig.humanoid(s, 1.05f, 4, skin(), plate, Paint.of(0x4A4A50).pattern(Pattern.PLATES, 0x7A7A80),
                Paint.of(0x5A5A60).pattern(Pattern.PLATES, 0x8A8A90).face(Face.FRONT, DecalType.VISOR, 0xFFB040, true));
        Paint shield = Paint.of(0x5A3A20).pattern(Pattern.WOOD, 0x3A2410).face(Face.FRONT, DecalType.SKULL, 0xD8D0B8, false);
        Part sh = r.larm.child("shield", 1, 6, -4).rot(0, 10, 0);
        sh.box(-6, -10, -1, 12, 22, 2, shield);
        sh.box(-6.5f, -10.5f, -1.2f, 13, 2, 2.4f, Paint.of(IRON).pattern(Pattern.PLATES, RUST));
        sh.box(-6.5f, 10.5f, -1.2f, 13, 2, 2.4f, Paint.of(IRON).pattern(Pattern.PLATES, RUST));
        Part mace = r.rarm.child("mace", 0, 10, 0).rot(-80, 0, 0);
        mace.box(-0.75f, -2, -0.75f, 1.5f, 12, 1.5f, Paint.of(0x4A3020));
        mace.box(-2.5f, 9, -2.5f, 5, 5, 5, Paint.of(IRON).pattern(Pattern.PLATES, 0xB0B0B0));
        r.head.child("plume", 0, -8.5f, 0).box(-0.5f, -3, -3, 1, 3, 6, Paint.of(0x8A1010).pattern(Pattern.FUR, 0x5A0A0A));
        rankGear(r, 8.4f, 1.05f);
    }

    private static void burrower(ModelSpec s) {
        Paint dirt = Paint.of(0x6B5A3A).pattern(Pattern.SPOTS, 0x4A3A20).noise(0.2f);
        Part body = s.root.child("body", 0, 6, 0).rot(40, 0, 0).box(-4.5f, 0, -3, 9, 11, 6, dirt);
        body.child("back_plate", 0, 1, 3).box(-4, 0, 0, 8, 9, 2, Paint.of(0x4A3A28).pattern(Pattern.SCALES, 0x6A5A40));
        Part head = s.root.child("head", 0, 7, -3).box(-3.5f, -6, -5, 7, 6, 7, Paint.of(0x7A6A4A).pattern(Pattern.SPOTS, ROT)
                .face(Face.FRONT, DecalType.SLIT_EYES, 0xFFD040, true));
        head.child("snout", 0, -2, -5).box(-2, -2, -3, 4, 3, 3, Paint.of(0x5A4A30).face(Face.FRONT, DecalType.DOTS, 0x2A1A10, false));
        Part rarm = s.root.child("rarm", -6, 9, -1).box(-2, -1, -2, 4, 11, 4, dirt);
        Part larm = s.root.child("larm", 6, 9, -1).box(-2, -1, -2, 4, 11, 4, dirt);
        rarm.child("digclaw_r", 0, 10, -1).box(-3, 0, -2, 6, 5, 2, Paint.of(0x9A9080).pattern(Pattern.STRIPES, 0x5A5040));
        larm.child("digclaw_l", 0, 10, -1).box(-3, 0, -2, 6, 5, 2, Paint.of(0x9A9080).pattern(Pattern.STRIPES, 0x5A5040));
        s.root.child("rleg", -2.5f, 15, 1).box(-2, 0, -2, 4, 9, 4, Paint.of(PANTS));
        s.root.child("lleg", 2.5f, 15, 1).box(-2, 0, -2, 4, 9, 4, Paint.of(PANTS));
        Rig r = rigOf(s, body, head, rarm, larm);
        rankGear(r, 6, 1f);
    }

    private static void shrieker(ModelSpec s) {
        Paint pale = Paint.of(0xB8C0A8).pattern(Pattern.VEINS, 0x7A5A7A).noise(0.1f);
        Part body = s.root.child("body", 0, 1, 0).box(-3, 0, -1.5f, 6, 12, 3, Paint.of(0x3A3040).pattern(Pattern.CLOTH, 0x2A2030));
        body.child("rags", 0, 12, 0).box(-3.5f, 0, -2, 7, 4, 4, Paint.of(0x2A2030).pattern(Pattern.STRIPES, 0x1A1020));
        Part head = s.root.child("head", 0, 1, 0).box(-3.5f, -9, -3.5f, 7, 7, 7, pale.copy().eyes(0xF0F8FF, true));
        Part jaw = head.child("shriek_jaw", 0, -2, -1).box(-3, 0, -2.5f, 6, 6, 5, Paint.of(0x9AA090).face(Face.FRONT, DecalType.GRIN, 0x1A0A0A, false));
        jaw.child("tongue", 0, 3, -2).box(-1, 0, -1, 2, 4, 2, Paint.of(BLOOD));
        head.child("hair", 0, -9, 1).box(-4, 0, 0, 8, 12, 3, Paint.of(0x1A1A20).pattern(Pattern.FUR, 0x3A3A40));
        Part rarm = s.root.child("rarm", -4.5f, 3, 0).box(-1, -1, -1, 2, 14, 2, pale);
        Part larm = s.root.child("larm", 4.5f, 3, 0).box(-1, -1, -1, 2, 14, 2, pale);
        s.root.child("rleg", -1.5f, 13, 0).box(-1, 0, -1, 2, 11, 2, pale);
        s.root.child("lleg", 1.5f, 13, 0).box(-1, 0, -1, 2, 11, 2, pale);
        Rig r = rigOf(s, body, head, rarm, larm);
        rankGear(r, 7, 0.7f);
    }

    private static void wraith(ModelSpec s) {
        Paint robe = Paint.of(0x40506A).pattern(Pattern.GRADIENT, 0x101820).noise(0.2f);
        Part body = s.root.child("body", 0, 2, 0).box(-4, 0, -2.5f, 8, 11, 5, robe);
        Part tail = body.child("tail", 0, 11, 0);
        tail.box(-3.5f, 0, -2, 7, 5, 4, robe).box(-2.5f, 5, -1.5f, 5, 4, 3, robe).box(-1.5f, 9, -1, 3, 3, 2, robe);
        Part head = s.root.child("head", 0, 2, 0).box(-3.5f, -8, -3.5f, 7, 8, 7, Paint.of(0x1A2030).eyes(0x7FFFF0, true));
        Rig.shell(head, "hood", 8, 0.8f, 1.2f, Paint.of(0x30405A).pattern(Pattern.CLOTH, 0x202838));
        Paint claw = Paint.of(0x9FE8E0).pattern(Pattern.GRADIENT, 0x40A0A0).glow();
        Part rarm = s.root.child("rarm", -5, 4, 0).box(-1.5f, -1, -1.5f, 3, 12, 3, robe);
        Part larm = s.root.child("larm", 5, 4, 0).box(-1.5f, -1, -1.5f, 3, 12, 3, robe);
        rarm.child("wclaw_r", 0, 11, 0).box(-1.5f, 0, -1.5f, 3, 4, 3, claw);
        larm.child("wclaw_l", 0, 11, 0).box(-1.5f, 0, -1.5f, 3, 4, 3, claw);
        body.child("core", 0, 4, -2.6f).box(-1.5f, 0, 0, 3, 3, 0.2f, Paint.glowing(0x7FFFF0).flat());
        Rig r = rigOf(s, body, head, rarm, larm);
        rankGear(r, 8, 1f);
    }

    private static void wrecker(ModelSpec s) {
        Rig r = Rig.humanoid(s, 1.15f, 5, skin(), Paint.of(0x8A6A30).pattern(Pattern.CLOTH, 0x6A5020),
                Paint.of(0x3A3A40).pattern(Pattern.CLOTH, 0x2A2A30), zhead(0xFF9020));
        r.head.child("hardhat", 0, -9.2f, 0).box(-5, 0, -5, 10, 3, 10, Paint.of(0xC89020).pattern(Pattern.BANDS, 0x8A6010))
                .box(-5.5f, 2.5f, -5.5f, 11, 1, 11, Paint.of(0xA07018));
        r.body.child("toolbelt", 0, 11, 0).box(-5, 0, -2.8f, 10, 2.5f, 5.6f, Paint.of(0x4A3020).pattern(Pattern.PLATES, 0xA08040));
        Part hammer = r.rarm.child("hammer", 0, 12, 0).rot(-75, 0, 0);
        hammer.box(-1, -12, -1, 2, 26, 2, Paint.of(0x5A3A20).pattern(Pattern.WOOD, 0x3A2410));
        hammer.box(-4, 12, -5, 8, 7, 10, Paint.of(0x7A7A7A).pattern(Pattern.CRACKS, 0x3A3A3A));
        hammer.box(-4.3f, 14.5f, -5.3f, 8.6f, 2, 10.6f, Paint.of(IRON).pattern(Pattern.PLATES, RUST));
        rankGear(r, 9.2f, 1.15f);
    }

    private static void necromancer(ModelSpec s) {
        Paint robe = Paint.of(0x2A1A3A).pattern(Pattern.CLOTH, 0x1A1028).face(Face.FRONT, DecalType.RUNE, 0x6BFF8A, true);
        Part body = s.root.child("body", 0, 2, 0).box(-4, 0, -2.5f, 8, 22, 5, robe);
        body.child("robe_hem", 0, 18, 0).box(-5, 0, -3.5f, 10, 4, 7, Paint.of(0x1A1028).pattern(Pattern.STRIPES, 0x3A2A4A));
        Part head = s.root.child("head", 0, 2, 0).box(-3.5f, -8, -3.5f, 7, 8, 7, Paint.of(0xC8C0B0).face(Face.FRONT, DecalType.SKULL, 0x6BFF8A, true));
        Rig.shell(head, "n_hood", 8, 0.8f, 1.2f, Paint.of(0x2A1A3A).pattern(Pattern.CLOTH, 0x1A1028));
        Part rarm = s.root.child("rarm", -5.5f, 4, 0).box(-1.5f, -1, -2, 3, 12, 4, Paint.of(0x2A1A3A));
        Part larm = s.root.child("larm", 5.5f, 4, 0).box(-1.5f, -1, -2, 3, 12, 4, Paint.of(0x2A1A3A));
        Part staff = rarm.child("staff", 0, 11, -1).rot(-10, 0, 0);
        staff.box(-0.75f, -18, -0.75f, 1.5f, 28, 1.5f, Paint.of(0x3A2A1A).pattern(Pattern.WOOD, 0x1A1008));
        staff.child("orb", 0, -20, 0).box(-2, -2, -2, 4, 4, 4, Paint.glowing(0x6BFF8A).face(Face.FRONT, DecalType.SKULL, 0x1A4A2A, false));
        larm.child("book", 0, 11, -2).box(-2.5f, 0, -1, 5, 6, 2, Paint.of(0x4A1A1A).pattern(Pattern.PLATES, 0xA08040));
        s.root.child("rleg", -2, 24, 0);
        s.root.child("lleg", 2, 24, 0);
        Rig r = rigOf(s, body, head, rarm, larm);
        rankGear(r, 8, 1f);
    }

    private static Rig rigOf(ModelSpec s, Part body, Part head, Part rarm, Part larm) {
        return Rig.of(body, head, rarm, larm);
    }

    // ------------------------------------------------------------------ animation

    public static void animate(ProcModel<SiegeZombie> m, SiegeZombie z, float ls, float la, float age, float hy, float hp, float pt) {
        ModelPart head = m.p("head"), body = m.p("body"), rarm = m.p("rarm"), larm = m.p("larm"), rleg = m.p("rleg"), lleg = m.p("lleg");
        ZombieKind k = z.kind;
        Anim.look(head, hy, hp);
        float legAmp = k == ZombieKind.RUNNER ? 1.6f : k == ZombieKind.BRUTE ? 0.8f : 1.2f;
        if (k != ZombieKind.WRAITH && k != ZombieKind.NECROMANCER) Anim.walk(rleg, lleg, ls, la, legAmp);
        float sway = Mth.sin(age * 0.067f) * 0.05f;
        body.zRot += Mth.sin(age * 0.05f) * 0.03f;

        // default "zombie reach" arms
        float reach = switch (k) {
            case RUNNER -> 0.6f;
            case SPITTER, WRECKER, BULWARK, NECROMANCER -> 0f;
            case BRUTE, BLOATER -> 0.25f;
            case LEAPER, BURROWER -> 0.9f;
            default -> 1.45f;
        };
        rarm.xRot += -reach + sway + Mth.cos(ls * 0.6662f) * 0.3f * la;
        larm.xRot += -reach * 0.95f - sway + Mth.cos(ls * 0.6662f + Mth.PI) * 0.3f * la;
        rarm.zRot += Mth.cos(age * 0.09f) * 0.05f + 0.05f;
        larm.zRot -= Mth.cos(age * 0.09f) * 0.05f + 0.05f;

        int id = z.anim.id();
        float t = z.anim.time(pt);
        if (id == SiegeZombie.ANIM_ATTACK || id == SiegeZombie.ANIM_CORE) {
            float sw = Anim.swing(t, z.attackWindup(), 3, 7);
            boolean both = id == SiegeZombie.ANIM_CORE || k == ZombieKind.BRUTE || k == ZombieKind.WRECKER;
            rarm.xRot += -1.3f * sw;
            if (both) larm.xRot += -1.3f * sw;
            else larm.xRot += -0.4f * sw;
            body.xRot += 0.12f * Math.max(0, -sw);
        }
        if (k == ZombieKind.RUNNER) {
            rarm.xRot += 0.9f * la;
            larm.xRot += 0.9f * la;
            if (id == SiegeZombie.ANIM_SPECIAL && t < 14) {
                rarm.xRot = -2.6f;
                larm.xRot = -2.6f;
                rleg.xRot = 0.8f;
                lleg.xRot = -0.6f;
            }
            m.p("r_jaw").xRot += Mth.sin(age * 0.4f) * 0.15f;
        }
        if (k == ZombieKind.SPITTER) {
            ModelPart sac = m.p("sac");
            float pump = 1f + Mth.sin(age * 0.15f) * 0.05f;
            if (id == SiegeZombie.ANIM_SPECIAL) pump += Anim.pulse(t, 0, 14) * 0.35f;
            sac.xScale = sac.yScale = sac.zScale = pump;
            m.p("spit_jaw").xRot += id == SiegeZombie.ANIM_SPECIAL ? Anim.pulse(t, 6, 10) * 0.9f : 0.05f;
            head.xRot += id == SiegeZombie.ANIM_SPECIAL ? -Anim.pulse(t, 0, 12) * 0.4f : 0;
        }
        if (k == ZombieKind.BRUTE && id == SiegeZombie.ANIM_SPECIAL) {
            float c = Anim.win(t, 0, 8);
            body.xRot += 0.4f * c;
            head.xRot -= 0.3f * c;
            rarm.xRot -= 0.6f * c;
            larm.xRot -= 0.6f * c;
            rleg.xRot += Mth.cos(age * 1.2f) * 0.9f * c;
            lleg.xRot -= Mth.cos(age * 1.2f) * 0.9f * c;
        }
        if (k == ZombieKind.BLOATER && z instanceof BloaterZombie b) {
            float sw = b.swell(pt);
            ModelPart bd = m.p("body");
            float pulse = 1f + Mth.sin(age * (0.1f + sw * 1.2f)) * (0.03f + sw * 0.08f) + sw * 0.18f;
            bd.xScale = bd.yScale = bd.zScale = pulse;
            rarm.zRot += 0.6f + sw * 0.5f;
            larm.zRot -= 0.6f + sw * 0.5f;
        }
        if (k == ZombieKind.LEAPER) {
            float crouch = id == SiegeZombie.ANIM_SPECIAL ? Anim.pulse(t, 0, 16) : 0;
            rleg.xRot += -0.4f * crouch;
            lleg.xRot += -0.4f * crouch;
            rarm.xRot += -1.5f * crouch;
            larm.xRot += -1.5f * crouch;
        }
        if (k == ZombieKind.BULWARK) {
            larm.xRot += -0.9f;
            larm.yRot += -0.3f;
            if (id == SiegeZombie.ANIM_BLOCK) {
                larm.xRot += -0.4f * Anim.pulse(t, 0, 8);
                body.xRot += 0.15f * Anim.pulse(t, 0, 8);
            }
            rarm.xRot += -0.4f;
        }
        if (k == ZombieKind.BURROWER) {
            float dig = Mth.sin(age * 0.6f) * 0.5f;
            if (id == SiegeZombie.ANIM_SPECIAL2) {
                rarm.xRot = -2.8f * (1 - Anim.win(t, 0, 15));
                larm.xRot = -2.8f * (1 - Anim.win(t, 0, 15));
            } else if (la < 0.1f) {
                rarm.xRot += dig * 0.2f;
                larm.xRot -= dig * 0.2f;
            }
        }
        if (k == ZombieKind.SHRIEKER) {
            ModelPart jaw = m.p("shriek_jaw");
            jaw.xRot += 0.15f + Mth.sin(age * 0.2f) * 0.05f;
            if (id == SiegeZombie.ANIM_SPECIAL) {
                float sc = Anim.pulse(t, 0, 30);
                jaw.xRot += 1.1f * sc;
                head.xRot -= 0.6f * sc;
                rarm.xRot = -0.3f - 0.5f * sc;
                larm.xRot = -0.3f - 0.5f * sc;
                rarm.zRot += 1.1f * sc;
                larm.zRot -= 1.1f * sc;
                body.xRot -= 0.25f * sc;
                head.x += (Mth.sin(age * 3f) * 0.3f) * sc;
            }
            m.p("hair").xRot += 0.15f + Mth.sin(age * 0.1f) * 0.08f;
        }
        if (k == ZombieKind.WRAITH) {
            float bob = Mth.sin(age * 0.12f) * 1.2f;
            for (ModelPart p : new ModelPart[]{head, body, rarm, larm}) p.y += bob - 2;
            m.p("tail").xRot += 0.35f + Mth.sin(age * 0.2f) * 0.15f;
            m.p("tail").zRot += Mth.sin(age * 0.13f) * 0.12f;
            rarm.xRot += -0.4f;
            larm.xRot += -0.4f;
        }
        if (k == ZombieKind.WRECKER) {
            rarm.xRot += -0.3f;
            if (id == SiegeZombie.ANIM_ATTACK || id == SiegeZombie.ANIM_CORE) {
                float sw = Anim.swing(t, z.attackWindup(), 3, 7);
                larm.xRot = rarm.xRot;
                body.xRot += 0.25f * Math.max(0, -sw) - 0.15f * Math.max(0, sw);
            }
        }
        if (k == ZombieKind.NECROMANCER) {
            float hover = Mth.sin(age * 0.1f) * 0.5f;
            body.y += hover;
            head.y += hover;
            rarm.y += hover;
            larm.y += hover;
            rarm.xRot += -0.5f;
            larm.xRot += -0.7f;
            larm.yRot += 0.3f;
            m.p("orb").yRot = age * 0.08f;
            if (id == SiegeZombie.ANIM_SPECIAL) {
                float r = Anim.pulse(t, 0, 30);
                rarm.xRot += -2.0f * r;
                larm.xRot += -1.5f * r;
                larm.zRot -= 0.8f * r;
                head.xRot -= 0.5f * r;
            }
            if (la > 0.05f) body.xRot += 0.08f;
        }
    }

    // ------------------------------------------------------------------ renderer

    public static ProcRenderer<SiegeZombie> renderer(EntityRendererProvider.Context ctx, ZombieKind kind) {
        ModelSpec.Baked b = baked(kind);
        float shadow = switch (kind) {
            case BRUTE, BLOATER -> 1.0f;
            case WRECKER -> 0.7f;
            default -> 0.5f;
        };
        ProcRenderer<SiegeZombie> r = new ProcRenderer<>(ctx, b, ZombieModels::animate, shadow, kind == ZombieKind.WRAITH);
        r.level(z -> z.rank().ordinal() + 1)
                .scaleBy(z -> z.renderScale() * modelHeightFix(kind))
                .tint((z, c) -> {
                    switch (z.rank()) {
                        case FERAL -> {
                            c[0] = 0.92f;
                            c[1] = 1.05f;
                            c[2] = 0.9f;
                        }
                        case CORRUPTED -> {
                            c[0] = 0.95f;
                            c[1] = 0.82f;
                            c[2] = 1.08f;
                        }
                        case ABYSSAL -> {
                            c[0] = 1.05f;
                            c[1] = 0.72f;
                            c[2] = 0.72f;
                        }
                        default -> {
                        }
                    }
                    if (z.isChampion()) {
                        c[0] = Math.min(1.2f, c[0] * 1.1f);
                        c[1] *= 1.0f;
                        c[2] *= 0.8f;
                    }
                    if (kind == ZombieKind.WRAITH) c[3] = 0.55f + Mth.sin(z.tickCount * 0.1f) * 0.1f;
                })
                .pre((z, pose, pt) -> {
                    float rise = z.riseProgress(pt);
                    if (rise < 1f) pose.translate(0, -(1f - Anim.easeOut(rise)) * z.getBbHeight() * 1.05f, 0);
                    if (kind == ZombieKind.BURROWER && z.state()) pose.scale(0.001f, 0.001f, 0.001f);
                });
        return r;
    }

    /** Model geometry vs hitbox ratio corrections (models are authored at their natural size). */
    private static float modelHeightFix(ZombieKind k) {
        return switch (k) {
            case BRUTE, BLOATER -> 1.25f;
            case WRECKER -> 0.95f;
            default -> 1f;
        };
    }
}
