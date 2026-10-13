package com.lastbastion.client.model.zombie;

import com.lastbastion.client.model.Anim;
import com.lastbastion.client.model.Body;
import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.model.Poser;
import com.lastbastion.client.model.ProcModel;
import com.lastbastion.client.model.ZombieModels;
import com.lastbastion.entity.zombie.SiegeZombie;
import net.minecraft.util.Mth;

/**
 * Shared zombie costume and motion: rotting skin palettes, faces, wounds, the rank mutation tiers
 * (Feral bone spurs and claws, Armored scrap plate, Corrupted void crystals, Abyssal horns and
 * hellfire) and the common gaits, lunges and rise-from-the-grave pose.
 */
public final class ZGear {
    public static final int BONE = 0xD8D0B8, BLOOD = 0x6A0A0A, RUST = 0x8A5530, IRON = 0x6A645C;

    private ZGear() {
    }

    public static Paint p(Mat m, int c) {
        return Paint.of(m, c);
    }

    /** Grey-green rotting skin with blotches. */
    public static Paint skin(int base) {
        return p(Mat.ROT, base).accent(darker(base, 0.6f)).pattern(Pattern.SPOTS, darker(base, 0.72f), 1.3f).grime(0.35f);
    }

    public static Paint wounded(int base) {
        return skin(base).decal(Face.FRONT, DecalType.BLOOD, BLOOD, false, 0.4f, 0.4f, 0.8f)
                .decal(Face.LEFT, DecalType.SCAR, 0x5A2A2A, false, 0.5f, 0.5f, 1f)
                .decal(Face.BACK, DecalType.BLOOD, BLOOD, false, 0.6f, 0.6f, 0.7f);
    }

    /** Zombie face: sunken glowing eyes, rotten nose, a torn mouth. */
    public static Paint face(int base, int eye) {
        return skin(base).decal(Face.FRONT, DecalType.BROW, darker(base, 0.5f), false, 0.5f, 0.42f, 1f)
                .decal(Face.FRONT, DecalType.EYES, eye, true, 0.5f, 0.47f, 1f)
                .decal(Face.FRONT, DecalType.NOSE, 0, false, 0.5f, 0.64f, 0.9f)
                .decal(Face.FRONT, DecalType.GRIN, 0x2A0A0A, false, 0.5f, 0.82f, 0.85f);
    }

    public static Paint cloth(int c) {
        return p(Mat.CLOTH, c).accent(darker(c, 0.7f)).pattern(Pattern.CLOTH, darker(c, 0.8f), 2f).grime(0.55f)
                .decal(Face.FRONT, DecalType.BLOOD, BLOOD, false, 0.3f, 0.6f, 0.7f);
    }

    public static Paint bone() {
        return p(Mat.BONE, BONE).accent(0x8A8270).grime(0.3f);
    }

    public static Paint rusty() {
        return p(Mat.METAL, IRON).accent(0x3A342C).grime(0.6f).pattern(Pattern.SPOTS, RUST, 1.6f);
    }

    public static int darker(int c, float f) {
        int r = (int) (((c >> 16) & 0xFF) * f), g = (int) (((c >> 8) & 0xFF) * f), b = (int) ((c & 0xFF) * f);
        return (r << 16) | (g << 8) | b;
    }

    /** Hanging shirt strips at the bottom of a chest box (torn clothing). */
    public static void tatters(Part chest, float w, float d, float y, int n, float len, Paint cloth) {
        float sw = w / n;
        for (int i = 0; i < n; i++) {
            float l = len * (0.5f + ((i * 37) % 10) / 18f);
            chest.box(-w / 2 + i * sw, y, -d / 2 - 0.15f, sw + 0.05f, l, 0.4f, cloth);
        }
    }

    /** Exposed ribs through a hole in the flesh. */
    public static void ribs(Part chest, float x, float y, float z, int n, float w) {
        Paint b = bone();
        chest.box(x - w / 2 - 0.2f, y - 0.3f, z - 0.1f, w + 0.4f, n * 1.1f + 0.4f, 0.3f, p(Mat.FLESH, 0x5A1414).accent(0x2A0606));
        for (int i = 0; i < n; i++) chest.box(x - w / 2, y + i * 1.1f, z - 0.35f, w, 0.6f, 0.4f, b);
    }

    /**
     * Rank mutations (gated by renderer level = rank + 1): 2 Feral bone spurs and claws, 3 Armored scrap
     * helm/plate/pauldron, 4 Corrupted void crystals and violet eyes, 5 Abyssal horns, burning eyes,
     * magma-cracked chest and a crown of hellfire.
     */
    public static void rankGear(Body b, float hw, float hh, float hd, float cw, float ch, float cd) {
        Paint bone = bone();
        // 2: Feral
        Part spurs = b.chest.child("rk_spurs", 0, 0, cd / 2).from(2);
        spurs.spike(-cw * 0.25f, -ch * 0.8f, 0, 1.1f, 2.6f, -55, 0, -15, bone);
        spurs.spike(cw * 0.2f, -ch * 0.55f, 0, 1.0f, 2.2f, -60, 0, 20, bone);
        spurs.spike(0, -ch * 0.3f, 0, 0.9f, 1.8f, -65, 0, 0, bone);
        for (int side = -1; side <= 1; side += 2) {
            Part hand = side < 0 ? b.rHand : b.lHand;
            Part claws = hand.child(side < 0 ? "rk_claw_r" : "rk_claw_l", 0, 1.4f, 0).from(2);
            for (int i = -1; i <= 1; i++) claws.spike(i * 0.6f, 0.6f, -0.4f, 0.45f, 1.6f, 160, 0, i * 8, bone);
        }
        // 3: Armored
        Paint iron = rusty();
        Part helm = b.head.child("rk_helm", 0, 0, 0).from(3);
        helm.box(-hw / 2 - 0.5f, -hh - 0.6f, -hd / 2 - 0.5f, hw + 1, hh * 0.5f, hd + 1, iron);
        helm.box(-hw / 2 - 0.7f, -hh * 0.55f, -hd / 2 - 0.7f, hw + 1.4f, 0.6f, hd + 1.4f, iron);
        helm.box(-0.4f, -hh - 1.2f, -hd / 2 - 0.4f, 0.8f, 0.8f, hd + 0.8f, p(Mat.METAL, 0x8A8070).grime(0.5f));
        Part plate = b.chest.child("rk_plate", 0, -ch, -cd / 2 - 0.4f).from(3);
        plate.box(-cw * 0.42f, 0.6f, -0.3f, cw * 0.84f, ch * 0.7f, 0.7f, iron.copy().decal(Face.FRONT, DecalType.SCAR, 0x3A342C, false, 0.5f, 0.5f, 1f));
        plate.box(-cw * 0.5f, 1.2f, 0.1f, cw, 0.5f, cd + 0.4f, p(Mat.LEATHER, 0x3A2A1A));
        Part pd = b.lArm.child("rk_paul", 0.4f, -1.4f, 0).from(3).rot(0, 0, -12);
        pd.box(-2.4f, -1.2f, -2.4f, 4.8f, 2.2f, 4.8f, iron);
        pd.spike(0.8f, -1.2f, 0, 0.9f, 1.8f, 0, 0, -25, p(Mat.METAL, 0x8A8070));
        // 4: Corrupted
        Paint crystal = p(Mat.CRYSTAL, 0xB050FF).accent(0xF0D0FF).glow();
        Part cry = b.chest.child("rk_crystals", 0, -ch * 0.6f, cd / 2).from(4);
        cry.spike(-cw * 0.3f, 0, 0, 1.4f, 4, -40, 0, -25, crystal);
        cry.spike(cw * 0.25f, ch * 0.2f, 0, 1.2f, 3.4f, -50, 0, 30, crystal);
        cry.spike(0, -ch * 0.3f, 0, 1.0f, 2.6f, -30, 0, 0, crystal);
        b.rArm.child("rk_cry_arm", -0.6f, -0.6f, 0).from(4).spike(0, 0, 0, 1.0f, 2.6f, 0, 0, 30, crystal);
        Part violet = b.head.child("rk_veyes", 0, -hh * 0.53f, -hd / 2 - 0.12f).levels(4, 4);
        violet.box(-hw * 0.32f, 0, 0, hw * 0.24f, 0.9f, 0.1f, Paint.glowing(0xD080FF));
        violet.box(hw * 0.08f, 0, 0, hw * 0.24f, 0.9f, 0.1f, Paint.glowing(0xD080FF));
        // 5: Abyssal
        Paint horn = p(Mat.BONE, 0x2A1A1A).accent(0x100808).gloss(0.4f);
        for (int side = -1; side <= 1; side += 2) {
            Part h = b.head.child(side < 0 ? "rk_horn_r" : "rk_horn_l", side * hw * 0.35f, -hh, 0).from(5);
            Part h1 = h.spikePart(0, 0, 0, 1.3f, 3.4f, -15, 0, side * 40, horn);
            h1.spike(0, -3.2f, 0, 0.7f, 2.2f, -35, 0, side * -20, horn);
        }
        Part eyes = b.head.child("rk_reyes", 0, -hh * 0.53f, -hd / 2 - 0.12f).from(5);
        eyes.box(-hw * 0.34f, 0, 0, hw * 0.26f, 1f, 0.1f, Paint.glowing(0xFF3010));
        eyes.box(hw * 0.08f, 0, 0, hw * 0.26f, 1f, 0.1f, Paint.glowing(0xFF3010));
        b.chest.child("rk_magma", 0, -ch * 0.5f, -cd / 2 - 0.25f).from(5)
                .box(-cw * 0.3f, -ch * 0.3f, -0.1f, cw * 0.6f, ch * 0.6f, 0.3f, p(Mat.STONE, 0x2A1A14).pattern(Pattern.CRACKS, 0xFF6A10, 1.2f).accentGlow());
        Paint fire = Paint.glowing(0xFF7A1A).accent(0xFFE040);
        Part crown = b.head.child("rk_crown", 0, -hh - 0.4f, 0).from(5);
        for (int i = 0; i < 5; i++) {
            float a = i * Mth.TWO_PI / 5;
            crown.spike(Mth.cos(a) * hw * 0.32f, 0, Mth.sin(a) * hd * 0.32f, 0.9f, 2 + (i % 2) * 1.2f, Mth.sin(a) * -15, 0, Mth.cos(a) * 15, fire);
        }
    }

    // ------------------------------------------------------------------ motion

    /** Rank-gear flicker (hellfire crown) and the rise-from-the-grave pose. */
    public static void common(ProcModel<?> v, Poser p, SiegeZombie z, ZombieModels.Ctx c) {
        if (c.rank >= 4) {
            var crown = v.opt("rk_crown");
            if (crown != null) crown.yScale = 1 + Mth.sin(c.age * 0.8f + c.variant) * 0.15f;
        }
        if (c.rise < 1) {
            float k = 1 - c.rise;
            float claw = Mth.sin(c.age * 0.9f) * 25 * k;
            p.armR(-160 * k + claw, 0, 15 * k, 20 * k);
            p.armL(-160 * k - claw, 0, -15 * k, 20 * k);
            p.head.xRot -= 0.5f * k;
        }
    }

    /** Classic shamble: lurching, uneven steps with a dragged leg, arms reaching forward. */
    public static void shamble(Poser p, ZombieModels.Ctx c, float reach, boolean dragRight) {
        float move = Math.min(1, c.la);
        p.idle(c.age, 0.7f);
        p.walk(c.ls, c.la, 0.75f, 0.2f, 1.2f);
        float ph = c.ls * 0.6662f;
        if (dragRight) {
            p.rShin.xRot *= 0.3f;
            p.rFoot.xRot += 0.4f * move;
            p.rThigh.zRot += 0.12f * move;
        }
        p.spine(10 + 6 * move, 0, Mth.sin(ph) * 7 * move);
        p.head.zRot += Mth.sin(c.age * 0.05f + c.variant) * 0.12f;
        p.head.xRot += 0.1f;
        float sway = Mth.sin(c.age * 0.07f + c.variant) * 6;
        p.armR(-reach + sway + Mth.cos(ph) * 10 * move, 5, 6, 15);
        p.armL(-reach * 0.92f - sway - Mth.cos(ph) * 10 * move, -5, -6, 18);
    }

    /** A wind-up and strike on the right arm (both for core swings and heavy hitters). */
    public static void claw(Poser p, ZombieModels.Ctx c, boolean both) {
        if (c.id != SiegeZombie.ANIM_ATTACK && c.id != SiegeZombie.ANIM_CORE) return;
        float sw = Anim.swing(c.t, c.windup, 3, 7);
        boolean two = both || c.id == SiegeZombie.ANIM_CORE;
        float up = Math.max(0, sw), hit = Math.max(0, -sw);
        p.armR(-60 * up - 30 * hit, 20 * up - 20 * hit, 0, 50 * up);
        if (two) p.armL(-60 * up - 30 * hit, -20 * up + 20 * hit, 0, 50 * up);
        else p.armL(-15 * hit, 0, 0, 0);
        p.spine(-8 * up + 18 * hit, 0, 0);
    }
}
