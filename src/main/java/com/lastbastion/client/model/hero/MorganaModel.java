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
import com.lastbastion.entity.hero.HeroEntity;
import com.lastbastion.entity.hero.MorganaHero;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Morgana the Stormweaver: storm-violet robes with silver trim and glowing hem runes, wide bell
 * sleeves, long silver hair under a tall wide-brimmed hat, and a staff whose claw holds a crackling
 * orb. Three rune stones orbit her; at Lv5 a crown of runes floats above the hat.
 */
public final class MorganaModel implements HeroModels.Def {
    @Override
    public void build(ModelSpec s) {
        Paint skin = p(Mat.SKIN, 0xF0D8CC);
        Paint robe = p(Mat.ROBE, 0x5A2E9A).accent(0x3A1A6A).trim(0xC8C8D8, 0.4f);
        Paint robeHem = p(Mat.ROBE, 0x4A2488).accent(0x2A1050).pattern(Pattern.RUNES, 0xC8A0FF, 1.2f).accentGlow().trim(0xC8C8D8, 0.4f);
        Paint inner = p(Mat.CLOTH, 0x2A1A4A).accent(0x1A1030);
        Paint silver = p(Mat.METAL, 0xD0D4E0).gloss(0.7f);
        Paint hair = p(Mat.HAIR, 0xE4E4F0).accent(0xA8A8C0);
        Paint hat = p(Mat.CLOTH, 0x3E1E70).accent(0x24104A).pattern(Pattern.CLOTH, 0x2E1458, 1f);
        Paint wood = p(Mat.WOOD, 0x3A2A22).accent(0x22160E);
        Paint orb = p(Mat.CRYSTAL, 0xB08AFF).accent(0xFFFFFF).glow();
        Paint head = Gear.face(0xF0D8CC, 0xB070FF, true, 0xC8C8E0, 0x8A4A6A);

        Body.Spec sp = new Body.Spec(skin, robe);
        sp.legs(5.4f, 5.4f, 3f);
        sp.foot = 1f;
        sp.hipW = 6.2f;
        sp.torso(6.8f, 6.4f, 3.8f, 5.6f, 4, 3.6f);
        sp.neck = 0.8f;
        sp.head(6.2f, 6.6f, 6.4f);
        sp.arms(5, 5, 2.8f);
        sp.hand = 1.8f;
        sp.paints(robe, inner, robe, robe, skin, inner, inner, p(Mat.LEATHER, 0x2A1A2A));
        sp.headPaint(head);
        Body b = Body.build(s.root, sp);

        // long hair down the back
        Gear.hair(b.head, 6.2f, 6.6f, 6.4f, 0.5f, 1f, hair);
        Part locks = b.head.child("locks", 0, -1, 3.4f);
        locks.box(-3.2f, 0, -0.4f, 6.4f, 7, 1, hair);
        locks.child("locks2", 0, 7, 0).box(-3, 0, -0.4f, 6, 4, 1, hair);
        // hat: wide brim, tall bent cone, silver band with a glowing star
        Part hatP = b.head.child("hat", 0, -6.4f, 0);
        hatP.octo(0, -0.6f, 0, 12, 0.7f, 12, hat);
        hatP.octo(0, -1.8f, 0, 6.8f, 1.4f, 6.8f, silver.copy().decal(Face.FRONT, DecalType.STAR, 0xC8A0FF, true, 0.5f, 0.5f, 1.4f));
        hatP.taper(0, -1.8f, 0, 6.6f, 6.6f, 3.6f, 3.6f, 4.5f, 3, hat);
        Part tip = hatP.child("hattip", 0, -6.3f, 0).rot(-25, 0, 0);
        tip.taper(0, 0, 0, 3.6f, 3.6f, 0.6f, 0.6f, 4.5f, 3, hat);
        Part crown = hatP.sub(0, -12, 0).from(5);
        for (int i = 0; i < 6; i++) {
            float a = i * Mth.TWO_PI / 6;
            crown.sub(Mth.cos(a) * 4.5f, 0, Mth.sin(a) * 4.5f).rot(0, -a * Mth.RAD_TO_DEG, 0).box(-0.8f, -0.8f, -0.3f, 1.6f, 1.6f, 0.6f,
                    Paint.glowing(0xC8A0FF).decal(Face.FRONT, DecalType.RUNE, 0xFFFFFF, true));
        }
        // robe: collar, skirt panels that follow the legs, sash
        b.chest.box(-3.8f, -6.8f, -2.4f, 7.6f, 1.6f, 4.8f, silver);
        b.chest.box(-0.6f, -5.5f, -2.05f, 1.2f, 5.5f, 0.3f, Paint.glowing(0xC8A0FF));
        Part skirtF = b.pelvis.child("skirt_f", 0, 0.6f, -1.9f);
        skirtF.box(-3.4f, 0, -0.5f, 6.8f, 11.5f, 1, robeHem);
        Part skirtB = b.pelvis.child("skirt_b", 0, 0.6f, 1.9f);
        skirtB.box(-3.6f, 0, -0.5f, 7.2f, 12, 1, robeHem);
        b.pelvis.box(-3.5f, 0.2f, -2.3f, 1, 9, 4.6f, robe).box(2.5f, 0.2f, -2.3f, 1, 9, 4.6f, robe);
        Gear.belt(b.pelvis, 5.6f, 3.6f, -1.6f, 1.2f, p(Mat.CLOTH, 0x7A40C0), silver);
        // bell sleeves
        b.rFore.box(-1.9f, 2.6f, -1.9f, 3.8f, 2.8f, 3.8f, robe);
        b.lFore.box(-1.9f, 2.6f, -1.9f, 3.8f, 2.8f, 3.8f, robe);
        Gear.pauldron(b.rArm, -1, 2.8f, robe, silver).levels(3, 9);
        Gear.pauldron(b.lArm, 1, 2.8f, robe, silver).levels(3, 9);
        // staff in the right hand (shaft along the hand's Y so it stands upright when the arm hangs)
        Part staff = b.rHand.child("staff", 0, 1, 0);
        staff.box(-0.45f, -15, -0.45f, 0.9f, 22, 0.9f, wood);
        staff.box(-0.6f, -9, -0.6f, 1.2f, 1, 1.2f, silver).box(-0.6f, 4, -0.6f, 1.2f, 1, 1.2f, silver);
        Part claw = staff.child("claw", 0, -15, 0);
        for (int i = 0; i < 4; i++) claw.sub(0, 0, 0).rot(0, 45 + i * 90, 0).sub(1.2f, 0, 0).rot(0, 0, 25).box(-0.3f, -3.4f, -0.3f, 0.6f, 3.4f, 0.6f, silver);
        claw.child("orb", 0, -2.6f, 0).sphere(0, 0, 0, 1.5f, orb);
        Part ring = claw.child("staffring", 0, -2.6f, 0).from(4);
        ring.octoRing(-0.2f, 5, 0.4f, 0.4f, Paint.glowing(0xC8A0FF));
        // orbiting rune stones
        Part orbit = s.root.child("orbit", 0, 8, 0);
        for (int i = 0; i < 3; i++) {
            float a = i * Mth.TWO_PI / 3;
            orbit.child("rune" + i, Mth.cos(a) * 7, (i - 1) * 1.5f, Mth.sin(a) * 7).rot(45, 0, 45)
                    .box(-0.8f, -0.8f, -0.8f, 1.6f, 1.6f, 1.6f, Paint.glowing(0xB08AFF).decal(Face.FRONT, DecalType.RUNE, 0xFFFFFF, true));
        }
    }

    @Override
    public void animate(ProcModel<HeroEntity> m, HeroEntity h, HeroModels.Ctx c) {
        Poser p = Poser.of(m);
        HeroModels.humanoid(p, h, c, 0.8f, 0.5f);
        float move = Math.min(1, c.la);
        ModelPart sf = m.p("skirt_f"), sb = m.p("skirt_b"), locks = m.p("locks"), orbit = m.p("orbit"), orb = m.p("orb"), staff = m.p("staff");
        sf.xRot += Math.min(0, Math.min(p.rThigh.xRot, p.lThigh.xRot)) * 0.8f;
        sb.xRot += Math.max(0, Math.max(p.rThigh.xRot, p.lThigh.xRot)) * 0.8f + move * 0.15f;
        locks.xRot += move * 0.3f + Mth.sin(c.age * 0.08f) * 0.04f;
        m.p("hattip").xRot += Mth.sin(c.age * 0.07f) * 0.05f;
        orbit.yRot = c.age * 0.05f;
        orbit.y += Mth.sin(c.age * 0.08f) * 0.8f;
        for (int i = 0; i < 3; i++) m.p("rune" + i).yRot = c.age * 0.1f;
        float pulse = 1 + Mth.sin(c.age * 0.2f) * 0.08f;
        orb.xScale = orb.yScale = orb.zScale = pulse;
        ModelPart ring = m.opt("staffring");
        if (ring != null) ring.yRot = c.age * 0.12f;
        // staff planted / held
        p.armR(-12, 0, 6, 25);
        staff.xRot -= p.rArm.xRot * 0.5f;
        if (c.combat) p.armL(-30, 20, -10, 50);
        switch (c.id) {
            case 1, 2, 3 -> {
                float k = Anim.swing(c.t, 9, 3, 8);
                p.armR(-60 * Math.max(0, -k) - 30 * Math.max(0, k), 0, 0, 20);
                p.armL(-80 * Math.max(0, -k), 15, 0, 10);
                p.spine(5 * Math.max(0, -k), 10 * k, 0);
            }
            case MorganaHero.CHAIN -> {
                float k = Anim.hold(c.t, 6, 18, 3);
                p.armL(-95 * k, 25 * k, 0, 0);
                p.armR(-40 * k, 0, 20 * k, 20);
                p.spine(-5 * k, 20 * k, 0);
            }
            case MorganaHero.NOVA -> {
                float up = Anim.hold(c.t, 0, 8, 2) * (c.t < 10 ? 1 : 0);
                float slam = Anim.hold(c.t, 10, 16, 2);
                p.armR(-120 * up - 40 * slam, 0, 0, 10);
                p.armL(-60 * up + 20 * slam, 0, -40 * up - 60 * slam, 10);
                p.crouch(slam, 4);
            }
            case MorganaHero.BLINK -> {
                float k = Anim.pulse(c.t, 0, 8);
                p.armL(-40 * k, 0, -60 * k, 0);
                p.armR(-40 * k, 0, 60 * k, 0);
                p.spine(-10 * k, 0, 0);
            }
            case MorganaHero.METEOR, MorganaHero.WELL -> {
                float k = Anim.hold(c.t, 2, 16, 4);
                p.armR(-170 * k, 0, 15 * k, 10);
                p.armL(-160 * k, 0, -25 * k, 10);
                p.spine(-15 * k, 0, 0);
                p.head.xRot -= 0.5f * k;
            }
            case HeroEntity.ULT -> {
                float k = Math.min(1, c.t / 12f) * (1 - Anim.win(c.t, 86, 6));
                p.armR(-140 * k, 0, 40 * k, 10);
                p.armL(-140 * k, 0, -40 * k, 10);
                p.spine(-14 * k, 0, 0);
                p.head.xRot -= 0.4f * k;
                sf.xRot -= 0.3f * k + Mth.sin(c.age * 0.5f) * 0.1f * k;
                sb.xRot += 0.4f * k + Mth.sin(c.age * 0.45f) * 0.1f * k;
                p.legR(-10 * k, 0, 25 * k);
                p.legL(5 * k, 0, 15 * k);
                orbit.yRot = c.age * 0.25f;
            }
            default -> {
            }
        }
    }
}
