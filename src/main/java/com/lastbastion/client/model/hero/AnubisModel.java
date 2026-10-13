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
import com.lastbastion.entity.hero.AnubisHero;
import com.lastbastion.entity.hero.HeroEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Anubis, Judge of the Dead: a tall black-jackal-headed god with gold-banded ears and glowing amber
 * eyes, a striped gold-and-lapis headdress, a broad usekh collar, gold armlets, a pleated white kilt,
 * the was-sceptre in his right hand and an ankh in his left. From Lv4 the golden scales of judgement
 * float at his shoulder.
 */
public final class AnubisModel implements HeroModels.Def {
    private static final float K = 1.35f;

    @Override
    public float shadow() {
        return 0.8f;
    }

    @Override
    public void build(ModelSpec s) {
        Paint skin = p(Mat.SKIN, 0x3A2A24).accent(0x2A1C18);
        Paint jackal = p(Mat.FUR, 0x1A1A1E).accent(0x0E0E10);
        Paint gold = p(Mat.GOLD, 0xE0B040).accent(0xFFF0B0);
        Paint lapis = p(Mat.PLAIN, 0x1E3A9A).gloss(0.6f);
        Paint stripes = p(Mat.GOLD, 0xE0B040).pattern(Pattern.BANDS, 0x1E3A9A, 1.6f);
        Paint linen = p(Mat.CLOTH, 0xF2EEE2).accent(0xC8C2B0).pattern(Pattern.STRIPES, 0xD8D2C0, 1.6f);
        Paint headP = p(Mat.FUR, 0x1A1A1E).accent(0x0E0E10).decal(Face.FRONT, DecalType.SLIT_EYES, 0xFFB030, true, 0.5f, 0.42f, 1.2f);
        Paint sand = p(Mat.STONE, 0xE8C878).accent(0xB8985A).glow();

        Body.Spec sp = new Body.Spec(skin, skin).scale(K);
        sp.legs(6, 6, 3.2f);
        sp.foot = 1.2f;
        sp.hipW = 6.6f;
        sp.torso(8, 7, 4.4f, 6.2f, 4.2f, 4);
        sp.neck = 1.6f;
        sp.head(5.6f, 6, 6.6f);
        sp.arms(6, 5.6f, 3);
        sp.hand = 2;
        sp.paints(skin, linen, skin, skin, skin, linen, skin, gold);
        sp.headPaint(headP);
        sp.neckP = jackal;
        Body b = Body.build(s.root, sp);

        // jackal snout and ears
        Part snout = b.head.child("snout", 0, -1.4f * K, -3.3f * K);
        snout.box(-1.6f * K, -1.4f * K, -1.2f * K, 3.2f * K, 2.8f * K, 1.6f * K, jackal);
        snout.sub(0, 0, 0).rot(90, 0, 0).taper(0, 0, 0, 3 * K, 2.6f * K, 1.2f * K, 1.4f * K, 4.6f * K, 4, jackal);
        snout.box(-0.6f * K, -0.9f * K, -4.9f * K, 1.2f * K, 0.9f * K, 0.8f * K, p(Mat.RUBBER, 0x0A0A0A).gloss(0.7f));
        for (int side = -1; side <= 1; side += 2) {
            Part ear = b.head.sub(side * 1.8f * K, -5.6f * K, 0.6f * K).rot(-8, 0, side * 10);
            ear.taper(0, 0, 0, 1.8f * K, 1.2f * K, 0.3f * K, 0.4f * K, 6 * K, 4, jackal);
            ear.box(-1 * K, -2.2f * K, -0.7f * K, 2 * K, 0.6f * K, 1.4f * K, gold);
            ear.box(-0.8f * K, -3.8f * K, -0.6f * K, 1.6f * K, 0.5f * K, 1.2f * K, gold);
        }
        // nemes-style headdress lappets and usekh collar
        b.head.box(-3 * K, -6.2f * K, -2.6f * K, 6 * K, 1 * K, 5.6f * K, stripes);
        for (int side = -1; side <= 1; side += 2) {
            Part lap = b.head.sub(side * 2.6f * K, -4.6f * K, -0.8f * K).rot(0, 0, side * 8);
            lap.box(-0.9f * K, 0, -1.2f * K, 1.8f * K, 9 * K, 2.4f * K, stripes);
        }
        b.head.box(-2.9f * K, -5.4f * K, 2 * K, 5.8f * K, 6.8f * K, 1.4f * K, stripes);
        Part collar = b.chest.sub(0, -7 * K, 0);
        collar.box(-4.6f * K, 0, -2.6f * K, 9.2f * K, 1.4f * K, 5.2f * K, gold);
        collar.box(-4.2f * K, 1.4f * K, -2.65f * K, 8.4f * K, 1 * K, 0.3f * K, lapis);
        collar.box(-3.8f * K, 2.4f * K, -2.6f * K, 7.6f * K, 1 * K, 0.3f * K, gold);
        collar.box(-3.2f * K, 3.4f * K, -2.55f * K, 6.4f * K, 0.8f * K, 0.3f * K, p(Mat.PLAIN, 0x30B0A0).gloss(0.5f));
        // gold armlets/bracers, kilt and belt
        Gear.sleeve(b.rArm, 3.2f * K, 2 * K, 1 * K, 3.2f * K, 0.05f, gold);
        Gear.sleeve(b.lArm, 3.2f * K, 2 * K, 1 * K, 3.2f * K, 0.05f, gold);
        Gear.sleeve(b.rFore, 3 * K, 3 * K, 2.6f * K, 3 * K, 0.05f, stripes);
        Gear.sleeve(b.lFore, 3 * K, 3 * K, 2.6f * K, 3 * K, 0.05f, stripes);
        Gear.belt(b.pelvis, 6.2f * K, 4 * K, -1.6f * K, 1.4f * K, gold, lapis);
        Part kiltF = b.pelvis.child("kilt", 0, 0.2f * K, -2.1f * K);
        kiltF.box(-3.4f * K, 0, -0.3f * K, 6.8f * K, 5.2f * K, 0.6f * K, linen);
        kiltF.box(-0.8f * K, 0, -0.45f * K, 1.6f * K, 5.6f * K, 0.4f * K, stripes);
        b.pelvis.box(-3.5f * K, 0.2f * K, -2.1f * K, 7 * K, 4.4f * K, 4.2f * K, linen);
        Gear.sleeve(b.rShin, 3 * K, 4.6f * K, 1 * K, 3 * K, 0.05f, gold);
        Gear.sleeve(b.lShin, 3 * K, 4.6f * K, 1 * K, 3 * K, 0.05f, gold);
        // was-sceptre in the right hand (shaft along the hand's Y)
        Part sceptre = b.rHand.child("sceptre", 0, 1 * K, 0);
        sceptre.box(-0.4f * K, -15 * K, -0.4f * K, 0.8f * K, 22 * K, 0.8f * K, p(Mat.DARK_METAL, 0x1E1E24).trim(0xE0B040, 0.25f));
        Part top = sceptre.sub(0, -15 * K, 0).rot(-55, 0, 0);
        top.box(-0.5f * K, -2.6f * K, -0.5f * K, 1 * K, 2.6f * K, 1 * K, gold);
        top.box(-0.6f * K, -3.4f * K, -1.6f * K, 1.2f * K, 1.2f * K, 2.2f * K, gold.copy().decal(Face.FRONT, DecalType.EYES, 0xFFB030, true));
        sceptre.box(-1.2f * K, 6.6f * K, -0.3f * K, 0.6f * K, 1.6f * K, 0.6f * K, gold).box(0.6f * K, 6.6f * K, -0.3f * K, 0.6f * K, 1.6f * K, 0.6f * K, gold);
        // ankh in the left hand
        Part ankh = b.lHand.child("ankh", 0, 2.4f * K, 0);
        Paint ankhP = gold.copy();
        ankh.box(-0.35f * K, 0, -0.35f * K, 0.7f * K, 3.6f * K, 0.7f * K, ankhP);
        ankh.box(-1.4f * K, 0.8f * K, -0.35f * K, 2.8f * K, 0.7f * K, 0.7f * K, ankhP);
        ankh.sub(0, 0, 0).rot(90, 0, 0).octoRing(-0.35f * K, 2.2f * K, 0.5f * K, 0.7f * K, ankhP);
        ankh.sub(0, 0, 0).from(3).box(-0.5f * K, 1.4f * K, -0.5f * K, 1 * K, 1 * K, 1 * K, Paint.glowing(0x40E0C0));
        // floating scales of judgement (Lv4+)
        Part scales = s.root.child("scales", 0, -10, 6).from(4);
        scales.box(-0.3f, -2, -0.3f, 0.6f, 4, 0.6f, gold);
        Part beam = scales.child("beam", 0, -2, 0);
        beam.box(-5, -0.3f, -0.3f, 10, 0.6f, 0.6f, gold);
        for (int side = -1; side <= 1; side += 2) {
            Part pan = beam.child(side < 0 ? "pan_r" : "pan_l", side * 4.6f, 0, 0);
            pan.box(-0.1f, 0, -0.1f, 0.2f, 3, 0.2f, gold);
            pan.octo(0, 3, 0, 3, 0.5f, 3, gold);
        }
        scales.box(-1, 1.6f, -1, 2, 1, 2, sand);
    }

    @Override
    public void animate(ProcModel<HeroEntity> m, HeroEntity h, HeroModels.Ctx c) {
        Poser p = Poser.of(m);
        HeroModels.humanoid(p, h, c, 0.9f, 0.45f);
        ModelPart kilt = m.p("kilt"), sceptre = m.p("sceptre");
        kilt.xRot += Math.min(0, Math.min(p.rThigh.xRot, p.lThigh.xRot)) * 0.8f;
        // sceptre held upright like a staff of office
        p.armR(-14, 0, 6, 30);
        sceptre.xRot -= p.rArm.xRot * 0.5f;
        p.armL(-20, 0, -6, 40);
        ModelPart scales = m.opt("scales");
        if (scales != null && scales.visible) {
            scales.y += Mth.sin(c.age * 0.07f) * 1.2f;
            scales.yRot = Mth.sin(c.age * 0.03f) * 0.4f;
            ModelPart beam = m.p("beam");
            float tilt = Mth.sin(c.age * 0.05f) * 0.25f;
            beam.zRot = tilt;
            m.p("pan_r").zRot = -tilt;
            m.p("pan_l").zRot = -tilt;
        }
        switch (c.id) {
            case 1, 2 -> {
                float k = Anim.swing(c.t, 8, 3, 8);
                p.armR(-80 * Math.max(0, k) - 40 * Math.max(0, -k), (c.id == 1 ? 30 : -30) * k, 0, 20);
                p.armL(-90 * Math.max(0, -k), 0, 0, 10);
                p.spine(5 * Math.max(0, -k), (c.id == 1 ? 20 : -20) * k, 0);
            }
            case AnubisHero.WEIGH -> {
                float k = Anim.hold(c.t, 2, 22, 4);
                p.armL(-150 * k, 0, -20 * k, 15);
                p.armR(-40 * k, 0, 30 * k, 20);
                p.head.xRot -= 0.25f * k;
                if (scales != null) scales.y -= 4 * k;
            }
            case AnubisHero.STORM -> {
                float k = Anim.hold(c.t, 2, 16, 3);
                p.armR(-100 * k, 0, 60 * k, 10);
                p.armL(-100 * k, 0, -60 * k, 10);
                p.spine(0, Mth.sin(c.t * 0.6f) * 30 * k, 0);
            }
            case AnubisHero.SIPHON -> {
                float k = Anim.hold(c.t, 2, 42, 4);
                p.armL(-90 * k, 20 * k, 0, 0);
                p.armR(-60 * k, -30 * k, 0, 30);
                p.spine(-5 * k, 10 * k, 0);
            }
            case AnubisHero.CURSE -> {
                float k = Anim.hold(c.t, 4, 18, 3);
                p.armL(-95 * k, -15 * k, 0, 0);
                p.head.xRot += 0.2f * k;
            }
            case HeroEntity.ULT -> {
                float k = Anim.hold(c.t, 0, 48, 4);
                p.armR(-170 * k, 0, 20 * k, 5);
                p.armL(-170 * k, 0, -20 * k, 5);
                p.spine(-15 * k, 0, 0);
                p.head.xRot -= 0.5f * k;
                if (scales != null) {
                    scales.y -= 6 * k;
                    scales.xScale = scales.yScale = scales.zScale = 1 + k * 0.6f;
                }
            }
            default -> {
            }
        }
    }
}
