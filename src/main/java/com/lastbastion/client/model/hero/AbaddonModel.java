package com.lastbastion.client.model.hero;

import com.lastbastion.client.model.Anim;
import com.lastbastion.client.model.Body;
import com.lastbastion.client.model.HeroModels;
import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.model.Poser;
import com.lastbastion.client.model.ProcModel;
import com.lastbastion.entity.hero.AbaddonDevil;
import com.lastbastion.entity.hero.HeroEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Abaddon, the Destroyer: a colossus of blackened iron fused with bone. A horned crown-helm with a single violet
 * visor slit; an open cage of ribs around a spinning vortex of void where his heart should be; chains wound about
 * his arms and hanging from his belt; ragged locust wings; a segmented scorpion tail arching over his shoulder with
 * a glowing stinger; and the Key of the Abyss - a great iron key used as a two-handed mace.
 */
public final class AbaddonModel implements HeroModels.Def {
    private static final float K = 1.6f;
    static final int TAIL = 7;

    @Override
    public float shadow() {
        return 1.3f;
    }

    @Override
    public int density() {
        return 3;
    }

    @Override
    public void build(ModelSpec s) {
        Paint iron = p(Mat.DARK_METAL, 0x1E1C24).gloss(0.6f).pattern(Pattern.PLATES, 0x2C2834, 1.1f).trim(0x5A3A8A, 0.3f);
        Paint ironDark = p(Mat.DARK_METAL, 0x121016).gloss(0.5f);
        Paint bone = p(Mat.BONE, 0xC8BCA4).accent(0x7A6E5A);
        Paint boneDark = p(Mat.BONE, 0x8A7E6A).accent(0x4A4034);
        Paint voidP = Paint.glowing(0xB078FF).accent(0xF0E4FF);
        Paint voidCore = Paint.glowing(0x5A1AB0).accent(0xD0A8FF).pattern(Pattern.VEINS, 0x1A0640, 1.4f);
        Paint chain = p(Mat.METAL, 0x4A4650).pattern(Pattern.CHAINMAIL, 0x7A7680, 2f).gloss(0.5f);
        Paint wing = p(Mat.CHITIN, 0x3A3222).accent(0x6A5A3A).pattern(Pattern.VEINS, 0x1A140A, 1.3f);
        Paint tail = p(Mat.CHITIN, 0x1A1820).gloss(0.8f).pattern(Pattern.PLATES, 0x2A2632, 1.2f).trim(0x5A3A8A, 0.3f);
        Paint stinger = Paint.glowing(0x9A5AFF).accent(0xE8D8FF);
        Paint cloth = p(Mat.CLOTH, 0x1A1022).accent(0x0A0610).trim(0x3A2A4A, 0.4f);
        Paint helm = p(Mat.DARK_METAL, 0x1A1820).gloss(0.6f)
                .decal(Face.FRONT, DecalType.VISOR, 0xA060FF, true, 0.5f, 0.45f, 1.1f);

        Body.Spec sp = new Body.Spec(iron, iron).scale(K);
        sp.legs(6.8f, 6.6f, 4.4f);
        sp.foot = 1.6f;
        sp.hipW = 8.6f;
        sp.torso(11.4f, 8.6f, 6.4f, 8.6f, 5, 5.4f);
        sp.neck = 0.8f;
        sp.head(6.4f, 6.8f, 6.6f);
        sp.arms(6.6f, 6.6f, 4.4f);
        sp.hand = 2.8f;
        sp.paints(iron, ironDark, iron, iron, ironDark, iron, iron, ironDark);
        sp.headPaint(helm);
        sp.neckP = chain;
        Body b = Body.build(s.root, sp);

        // ---- crowned, horned helm
        Part head = b.head;
        head.box(-3.6f * K, -7.4f * K, -3.6f * K, 7.2f * K, 1.2f * K, 7.2f * K, ironDark);
        for (int i = 0; i < 5; i++) {
            float x = (i - 2) * 1.6f * K;
            head.spike(x, -7.4f * K, -3.2f * K, 0.9f * K, (i == 2 ? 3.6f : 2.2f) * K, -8, 0, (i - 2) * -9, bone);
        }
        for (int side = -1; side <= 1; side += 2) {
            Part h = head.sub(side * 3.4f * K, -6 * K, -0.4f * K).rot(-10, 0, side * 58);
            h.taper(0, 0, 0, 2.2f * K, 2.2f * K, 1.4f * K, 1.4f * K, 4 * K, 3, boneDark);
            Part h2 = h.sub(0, -4 * K, 0).rot(10, 0, side * -48);
            h2.taper(0, 0, 0, 1.4f * K, 1.4f * K, 0.3f * K, 0.3f * K, 4.6f * K, 3, bone);
        }
        head.box(-3.7f * K, -4 * K, -3.7f * K, 7.4f * K, 4 * K, 0.5f * K, ironDark.copy().decal(Face.FRONT, DecalType.GRILLE, 0x0A080C, false, 0.5f, 0.6f, 0.9f));

        // ---- the ribcage and the void heart
        Part heart = b.chest.child("heart", 0, -4.6f * K, -1.2f * K);
        heart.sphere(0, 0, 0, 2.6f * K, voidCore);
        Part swirl = heart.child("swirl", 0, 0, -1.6f * K);
        swirl.box(-2.4f * K, -0.3f * K, -0.3f * K, 4.8f * K, 0.6f * K, 0.6f * K, voidP);
        swirl.box(-0.3f * K, -2.4f * K, -0.3f * K, 0.6f * K, 4.8f * K, 0.6f * K, voidP);
        swirl.sub(0, 0, 0).rot(0, 0, 45).box(-1.8f * K, -0.25f * K, -0.25f * K, 3.6f * K, 0.5f * K, 0.5f * K, voidP);
        for (int i = 0; i < 4; i++) {
            float y = (-7.4f + i * 1.7f) * K;
            for (int side = -1; side <= 1; side += 2) {
                Part rib = b.chest.sub(side * 0.6f * K, y, -3.3f * K).rot(0, 0, side * (8 + i * 4));
                rib.box(side < 0 ? -4.6f * K : 0, -0.4f * K, -0.4f * K, 4.6f * K, 0.8f * K, 0.8f * K, bone);
            }
        }
        b.chest.box(-0.6f * K, -8.2f * K, -3.4f * K, 1.2f * K, 7.6f * K, 0.8f * K, bone);
        // massive pauldrons with skulls, chains on the arms
        for (int side = -1; side <= 1; side += 2) {
            Part pd = Gear.pauldron(side < 0 ? b.rArm : b.lArm, side, 5.2f * K, iron, ironDark);
            pd.box(-1.4f * K, -3.6f * K, -1.4f * K, 2.8f * K, 2.6f * K, 2.8f * K, bone.copy().decal(Face.FRONT, DecalType.HOLLOW_EYES, 0x1A1410, false));
            pd.spike(side * 2.2f * K, -2 * K, 1 * K, 1 * K, 3.4f * K, 20, 0, side * -30, boneDark);
            Part arm = side < 0 ? b.rArm : b.lArm;
            for (int i = 0; i < 3; i++) arm.box(-2.4f * K, (1.4f + i * 1.6f) * K, -2.4f * K, 4.8f * K, 0.7f * K, 4.8f * K, chain);
            Part fore = side < 0 ? b.rFore : b.lFore;
            Gear.sleeve(fore, 4.4f * K, 1.4f * K, 4.4f * K, 4.4f * K, 0.15f, ironDark);
            fore.spike(side * 2.3f * K, 3.4f * K, 0, 0.7f * K, 2 * K, 0, 0, side * 85, bone);
        }
        // belt of chains and a tattered loin
        Gear.belt(b.pelvis, 8.6f * K, 5.4f * K, -1.8f * K, 1.6f * K, ironDark, bone.copy().decal(Face.FRONT, DecalType.SKULL, 0x9A5AFF, true));
        Part loin = b.pelvis.child("loin", 0, 0.2f * K, -2.8f * K);
        loin.box(-2.8f * K, 0, -0.3f * K, 5.6f * K, 7.4f * K, 0.6f * K, cloth);
        Part chains = b.pelvis.child("hang_chains", 0, 0, 0);
        for (int i = 0; i < 4; i++) {
            float x = (-3.3f + i * 2.2f) * K;
            chains.box(x - 0.35f * K, 0, i % 2 == 0 ? -2.9f * K : 2.6f * K, 0.7f * K, (4 + i % 3) * K, 0.7f * K, chain);
        }
        for (int side = -1; side <= 1; side += 2) {
            Part shin = side < 0 ? b.rShin : b.lShin;
            Gear.sleeve(shin, 4.6f * K, 0.4f * K, 4 * K, 4.6f * K, 0.1f, ironDark);
            shin.spike(0, 0.6f * K, -2.3f * K, 0.9f * K, 2.2f * K, -80, 0, 0, bone);
        }
        // ---- ragged locust wings
        for (int side = -1; side <= 1; side += 2) {
            Part wg = b.chest.child(side < 0 ? "lwing_r" : "lwing_l", side * 2.2f * K, -7.2f * K, 3.4f * K);
            for (int i = 0; i < 3; i++) {
                Part blade = wg.sub(0, 0, 0.3f * i).rot(0, side * -10, side * (-70 + i * 18));
                float len = (16 - i * 3) * K, w = (4.4f - i * 0.6f) * K;
                blade.box(-w / 2, 0, -0.2f * K, w, len, 0.4f * K, wing);
                blade.box(-w * 0.3f, len, -0.2f * K, w * 0.6f, len * 0.15f, 0.4f * K, wing);
            }
        }
        // ---- the scorpion tail: up from the lower back and over the right shoulder
        Part seg = b.pelvis.child("tail0", 0, -1.4f * K, 2.8f * K);
        float tw = 2.6f * K;
        for (int i = 0; i < TAIL; i++) {
            float len = 3.4f * K;
            seg.box(-tw / 2, -len, -tw / 2, tw, len, tw, tail);
            seg.spike(0, -len * 0.5f, tw / 2, 0.5f * K, 1.2f * K, 70, 0, 0, boneDark);
            if (i < TAIL - 1) {
                seg = seg.child("tail" + (i + 1), 0, -len, 0);
                tw *= 0.9f;
            }
        }
        Part sting = seg.sub(0, -3.4f * K, 0);
        sting.sphere(0, -1 * K, 0, 1.6f * K, tail);
        sting.spike(0, -2 * K, 0, 1.2f * K, 3.6f * K, -40, 0, 0, stinger);

        // ---- the Key of the Abyss: bow (ring) at the grip end, a long shaft, the bit as the striking head
        Part key = b.rHand.child("key", 0, 1.4f * K, 0);
        key.box(-0.6f * K, -0.6f * K, -24 * K, 1.2f * K, 1.2f * K, 26 * K, ironDark);
        Part bow = key.sub(0, 0, 3.4f * K).rot(90, 0, 0);
        bow.octoRing(-0.5f * K, 4.4f * K, 0.9f * K, 1 * K, iron);
        bow.box(-0.8f * K, -0.8f * K, -0.8f * K, 1.6f * K, 1.6f * K, 1.6f * K, voidP);
        Part bit = key.child("key_bit", 0, 0, -22 * K);
        bit.box(-0.7f * K, -0.7f * K, -2 * K, 1.4f * K, 7 * K, 2.6f * K, iron);
        bit.box(-0.7f * K, 4.6f * K, -2 * K, 1.4f * K, 2 * K, 5.4f * K, iron);
        bit.box(-0.7f * K, 1.6f * K, -2 * K, 1.4f * K, 1.6f * K, 4.4f * K, iron);
        bit.box(-0.75f * K, 0.4f * K, -1.4f * K, 1.5f * K, 0.8f * K, 0.8f * K, voidP);
        bit.spike(0, 0, -2 * K, 1.4f * K, 3 * K, 90, 0, 0, ironDark);
        key.box(-0.8f * K, -0.8f * K, -12 * K, 1.6f * K, 1.6f * K, 1.2f * K, voidP);
    }

    @Override
    public void animate(ProcModel<HeroEntity> m, HeroEntity h, HeroModels.Ctx c) {
        Poser p = Poser.of(m);
        HeroModels.humanoid(p, h, c, 0.75f, 0.3f);
        boolean challenge = h instanceof AbaddonDevil a && a.challenging();
        // the void heart spins; it flares under Destroyer's Challenge
        ModelPart swirl = m.p("swirl");
        swirl.zRot = c.age * (challenge ? 0.35f : 0.12f);
        ModelPart heart = m.p("heart");
        float pulse = 1f + 0.06f * Mth.sin(c.age * 0.2f) + (challenge ? 0.18f : 0f);
        heart.xScale = heart.yScale = heart.zScale = pulse;
        // tail leans back off the spine, then arches forward over the right shoulder, stinger twitching
        for (int i = 0; i < TAIL; i++) {
            ModelPart t = m.p("tail" + i);
            t.xRot += (i == 0 ? -0.6f : 0.29f) + Mth.sin(c.age * 0.08f - i * 0.5f) * 0.05f;
            t.zRot += (i < 3 ? -0.07f : 0.02f) + Mth.sin(c.age * 0.06f - i * 0.4f) * 0.03f;
        }
        // locust wings flick; hanging chains swing
        float flick = Mth.sin(c.age * (c.combat ? 1.2f : 0.15f)) * (c.combat ? 0.2f : 0.05f);
        m.p("lwing_r").yRot += 0.6f + flick;
        m.p("lwing_l").yRot -= 0.6f + flick;
        m.p("hang_chains").xRot += Mth.sin(c.age * 0.12f) * 0.06f + Math.min(1, c.la) * 0.2f;
        m.p("loin").xRot += Math.min(0, Math.min(p.rThigh.xRot, p.lThigh.xRot)) * 0.7f;
        // the Key: rested on the shoulder, two-handed when fighting
        if (c.combat || c.id != 0) {
            p.armR(-50, -15, 0, 60);
            p.armL(-55, 35, 0, 75);
            p.stance(0.5f);
        } else {
            p.armR(-160, -15, 10, 70);
            m.p("key").xRot += 0.3f;
        }
        switch (c.id) {
            case 1 -> {
                float up = c.t < 12 ? Anim.smooth(Math.min(1, c.t / 11f)) : 0;
                float down = Anim.hold(c.t, 12, 16, 4);
                p.armR(-170 * up - 50 * down, 0, 0, 10);
                p.armL(-165 * up - 45 * down, 0, 0, 10);
                p.spine(-20 * up + 40 * down, 0, 0);
                p.crouch(down * 0.6f, 4);
            }
            case 2 -> {
                float k = Anim.swing(c.t, 12, 3, 8);
                p.armR(-40, 80 * Math.max(0, k) - 100 * Math.max(0, -k), 0, 10);
                p.armL(-50, 70 * Math.max(0, k) - 80 * Math.max(0, -k), 0, 20);
                p.spine(5, 50 * Math.max(0, k) - 70 * Math.max(0, -k), 0);
            }
            case 3 -> {
                float k = Anim.pulse(c.t, 8, 10);
                for (int i = 3; i < TAIL; i++) m.p("tail" + i).xRot += 0.25f * k;
                p.spine(15 * k, 0, 0);
            }
            case AbaddonDevil.PIT, AbaddonDevil.SEAL -> {
                float up = Anim.hold(c.t, 0, 12, 3);
                float slam = Anim.hold(c.t, 13, 24, 4);
                p.armR(-170 * up - 60 * slam, 0, 0, 10);
                p.armL(-165 * up - 55 * slam, 0, 0, 10);
                p.spine(-15 * up + 45 * slam, 0, 0);
                p.crouch(slam * 0.7f, 5);
            }
            case AbaddonDevil.CHAINS -> {
                float out = Anim.hold(c.t, 4, 8, 3);
                float haul = Anim.hold(c.t, 9, 26, 5);
                p.armL(-90 * out + 30 * haul, -40 * out, 0, 90 * haul);
                p.armR(-80 * out, 40 * out, 0, 80 * haul);
                p.spine(-15 * haul, 0, 0);
            }
            case AbaddonDevil.STING -> {
                float k = Anim.swing(c.t, 11, 3, 8);
                for (int i = 2; i < TAIL; i++) m.p("tail" + i).xRot += 0.3f * Math.max(0, -k) - 0.15f * Math.max(0, k);
                p.spine(-10 * Math.max(0, k) + 25 * Math.max(0, -k), 0, 0);
            }
            case AbaddonDevil.LOCUSTS, AbaddonDevil.CHALLENGE -> {
                float k = Anim.hold(c.t, 4, 30, 5);
                p.armR(-90 * k, 0, 50 * k, 20);
                p.armL(-90 * k, 0, -50 * k, 20);
                p.head.xRot -= 0.5f * k;
                p.spine(-20 * k, 0, 0);
                m.p("lwing_r").yRot += 0.6f * k;
                m.p("lwing_l").yRot -= 0.6f * k;
            }
            case HeroEntity.ULT -> {
                float raise = Anim.hold(c.t, 0, 14, 3);
                float turn = Anim.hold(c.t, 16, 150, 6);
                p.armR(-170 * raise - 70 * turn, 0, 0, 10);
                p.armL(-165 * raise - 65 * turn, 0, 0, 10);
                p.crouch(turn * 0.5f, 4);
                m.p("key").zRot += turn * (c.t * 0.05f);
                p.spine(30 * turn, 0, 0);
            }
            default -> {
            }
        }
    }
}
