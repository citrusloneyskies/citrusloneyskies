package com.lastbastion.client.model.hero;

import com.lastbastion.client.model.Anim;
import com.lastbastion.client.model.Body;
import com.lastbastion.client.model.HeroModels;
import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.model.Poser;
import com.lastbastion.client.model.ProcModel;
import com.lastbastion.entity.hero.HeroEntity;
import com.lastbastion.entity.hero.SylvaHero;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/** Sylva the Huntress: elven ranger in a leaf cloak with a living-wood recurve bow. */
public final class SylvaModel implements HeroModels.Def {
    @Override
    public void build(ModelSpec s) {
        Paint skin = p(Mat.SKIN, 0xF2D3B6);
        Paint tunic = p(Mat.LEATHER, 0x46743E).accent(0x2E5028).pattern(Pattern.CLOTH, 0x34582E, 1.2f);
        Paint trousers = p(Mat.CLOTH, 0x5A4630).accent(0x40301E);
        Paint boots = p(Mat.LEATHER, 0x4A3020).accent(0x2E1C10).trim(0x6A4A2E, 0.5f);
        Paint bracer = p(Mat.LEATHER, 0x6E4E2C).pattern(Pattern.BANDS, 0x4A3018, 1.5f);
        Paint hair = p(Mat.HAIR, 0xB4542C).accent(0x7A3018);
        Paint cloak = p(Mat.CLOTH, 0x2E4E2A).accent(0x1E3418).pattern(Pattern.FEATHERS, 0x3E6A34, 1.4f).trim(0xC8A040, 0.4f);
        Paint wood = p(Mat.WOOD, 0x8A5A30).accent(0x5A3A1C);
        Paint gold = p(Mat.GOLD, 0xD8B048).accent(0xFFF0B0);
        Paint leafGlow = Paint.glowing(0x7CF080);
        Paint head = Gear.face(0xF2D3B6, 0x3AA850, false, 0x7A3418, 0xB8645A);

        Body.Spec sp = new Body.Spec(skin, tunic);
        sp.legs(5.6f, 5.4f, 3f);
        sp.foot = 1.1f;
        sp.hipW = 6.2f;
        sp.torso(7, 6.5f, 3.8f, 6, 4, 3.6f);
        sp.neck = 0.8f;
        sp.head(6.2f, 6.6f, 6.4f);
        sp.arms(5, 5, 2.8f);
        sp.hand = 1.8f;
        sp.paints(tunic, tunic, tunic, bracer, skin, trousers, boots, boots);
        sp.headPaint(head);
        Body b = Body.build(s.root, sp);

        // hair, braid, ears, hood
        Gear.hair(b.head, 6.2f, 6.6f, 6.4f, 0.55f, 0.9f, hair);
        Part braid = b.head.child("braid", 0, -1.6f, 3.4f).rot(18, 0, 0);
        braid.box(-0.8f, 0, -0.8f, 1.6f, 4, 1.6f, hair);
        braid.child("braid2", 0, 4, 0).box(-0.7f, 0, -0.7f, 1.4f, 3.6f, 1.4f, hair).box(-0.85f, 3.4f, -0.85f, 1.7f, 0.6f, 1.7f, gold);
        Gear.ear(b.head, -1, -3.1f, -3.6f, 0.4f, 1.1f, 2.8f, skin);
        Gear.ear(b.head, 1, 3.1f, -3.6f, 0.4f, 1.1f, 2.8f, skin);
        b.chest.box(-3.7f, -7, 1.3f, 7.4f, 2.1f, 2.4f, cloak);
        Gear.cape(b.chest, "cape", 7.6f, 6.5f, 3.8f, 13, cloak);
        // quiver with fletchings
        Part quiver = b.chest.sub(1.4f, -5.2f, 2.7f).rot(0, 0, -22);
        quiver.octo(0, -1, 0, 2.2f, 7.5f, 2.2f, p(Mat.LEATHER, 0x6A4022).trim(0xC8A040, 0.4f));
        for (int i = 0; i < 4; i++) {
            quiver.box(-0.8f + (i % 2) * 0.9f, -2.8f - (i / 2) * 0.4f, -0.6f + (i / 2) * 0.6f, 0.25f, 2, 0.25f, wood);
            quiver.box(-0.95f + (i % 2) * 0.9f, -3.4f - (i / 2) * 0.4f, -0.7f + (i / 2) * 0.6f, 0.55f, 1.1f, 0.12f, p(Mat.FEATHER, i % 2 == 0 ? 0xE8E0D0 : 0xB03028));
        }
        // belt and pouches
        Gear.belt(b.pelvis, 6, 3.6f, -1.2f, 1.2f, p(Mat.LEATHER, 0x3A2414), gold);
        b.pelvis.box(-3.6f, -0.6f, -1.2f, 1.4f, 1.8f, 1.6f, p(Mat.LEATHER, 0x5A3A1E));
        b.pelvis.box(2.3f, -0.4f, 0.4f, 1.3f, 1.6f, 1.4f, p(Mat.LEATHER, 0x5A3A1E));
        // tunic skirt
        b.pelvis.box(-3.2f, 0.2f, -2, 6.4f, 2.2f, 4, tunic);
        // Lv3: leaf pauldrons; Lv4: glowing cloak runes via second cape layer; Lv5: spirit antlers
        Gear.pauldron(b.rArm, -1, 2.8f, p(Mat.LEATHER, 0x6A8A3A).pattern(Pattern.FEATHERS, 0x4A6A2A), gold).levels(3, 9);
        Gear.pauldron(b.lArm, 1, 2.8f, p(Mat.LEATHER, 0x6A8A3A).pattern(Pattern.FEATHERS, 0x4A6A2A), gold).levels(3, 9);
        Part antlers = b.head.sub(0, -6.6f, 0).from(5);
        antlers.box(-3.4f, -0.2f, -3.3f, 6.8f, 0.5f, 0.5f, gold);
        Part al = antlers.sub(-1.8f, 0, -1).rot(-10, 0, -30);
        al.box(-0.3f, -4, -0.3f, 0.6f, 4, 0.6f, leafGlow).sub(0, -2.4f, 0).rot(0, 0, -50).box(-0.25f, -2, -0.25f, 0.5f, 2, 0.5f, leafGlow);
        Part ar = antlers.sub(1.8f, 0, -1).rot(-10, 0, 30);
        ar.box(-0.3f, -4, -0.3f, 0.6f, 4, 0.6f, leafGlow).sub(0, -2.4f, 0).rot(0, 0, 50).box(-0.25f, -2, -0.25f, 0.5f, 2, 0.5f, leafGlow);
        // the bow (limbs along the hand's Z axis; vertical when the arm points at the target)
        Part bow = b.lHand.child("bow", 0, 1.1f, 0);
        bow.box(-0.55f, -0.9f, -0.6f, 1.1f, 1.8f, 1.2f, p(Mat.LEATHER, 0x3A2414));
        Part up = bow.sub(0, 0, -0.6f).rot(-8, 0, 0);
        up.box(-0.4f, -0.45f, -6.4f, 0.8f, 0.9f, 6.4f, wood);
        up.sub(0, 0, -6.4f).rot(-30, 0, 0).box(-0.3f, -0.35f, -2, 0.6f, 0.7f, 2, wood);
        Part lo = bow.sub(0, 0, 0.6f).rot(8, 0, 0);
        lo.box(-0.4f, -0.45f, 0, 0.8f, 0.9f, 6.4f, wood);
        lo.sub(0, 0, 6.4f).rot(30, 0, 0).box(-0.3f, -0.35f, 0, 0.6f, 0.7f, 2, wood);
        bow.child("bowstring", 0, -1.5f, 0).box(-0.1f, -0.1f, -7.6f, 0.2f, 0.2f, 15.2f, p(Mat.CLOTH, 0xF0ECE0).flat());
        Part leaves = bow.sub(0, 0, 0).from(4);
        leaves.box(-0.6f, -0.4f, -8.2f, 1.2f, 1.2f, 1.6f, leafGlow).box(-0.6f, -0.4f, 6.6f, 1.2f, 1.2f, 1.6f, leafGlow);
        Part arrow = bow.child("nock", 0, -1.2f, 0);
        arrow.box(-0.15f, 0, -0.15f, 0.3f, 8, 0.3f, wood);
        arrow.spike(0, 8, 0, 0.8f, 1.4f, 180, 0, 0, p(Mat.METAL, 0xC8CCD4));
        arrow.box(-0.6f, 0.2f, -0.05f, 1.2f, 1.4f, 0.1f, p(Mat.FEATHER, 0xE8E0D0));
    }

    @Override
    public void animate(ProcModel<HeroEntity> m, HeroEntity h, HeroModels.Ctx c) {
        Poser p = Poser.of(m);
        boolean shooting = c.id >= 1 && c.id <= 5 || c.id == SylvaHero.PIERCE || c.id == SylvaHero.MARK || c.id == HeroEntity.ULT;
        HeroModels.humanoid(p, h, c, 0.9f, shooting ? 0.2f : 0.85f);
        ModelPart bow = m.p("bow"), nock = m.p("nock"), cape = m.p("cape"), cape2 = m.p("cape2"), string = m.p("bowstring");
        float move = Math.min(1, c.la);
        cape.xRot += 0.06f + move * 0.55f + Mth.sin(c.age * 0.1f) * 0.03f;
        cape2.xRot += move * 0.3f + Mth.sin(c.age * 0.13f + 1) * 0.04f;
        m.p("braid").xRot += move * 0.4f + Mth.sin(c.age * 0.09f) * 0.05f;
        nock.visible = false;
        float draw = 0;
        if (shooting) {
            float wind = c.id == SylvaHero.PIERCE ? 16 : c.id == HeroEntity.ULT ? 5 : 11;
            float tt = c.id == HeroEntity.ULT ? (c.t % 5) : c.t;
            draw = tt < wind ? Anim.smooth(tt / wind) : Math.max(0, 1 - (tt - wind) / 3f);
            float aimUp = c.id == SylvaHero.MARK ? 0 : 0;
            p.lArm.xRot = (-90 + c.headPitch * 0.8f - aimUp) * Anim.D;
            p.lArm.yRot = 12 * Anim.D;
            p.lArm.zRot = 0;
            p.lFore.xRot = 0;
            p.rArm.xRot = (-88 + c.headPitch * 0.8f) * Anim.D;
            p.rArm.yRot = (-20 - draw * 10) * Anim.D;
            p.rArm.zRot = 0;
            p.rFore.xRot = 0;
            p.rFore.zRot = -draw * 135 * Anim.D;
            p.spine(0, 18, 0);
            nock.visible = draw > 0.15f;
            string.z += 0;
            string.y -= draw * 2.2f;
        } else if (c.combat) {
            p.armL(-35, 10, 0, 25);
        }
        if (!shooting) bow.xRot = (c.combat ? 55 : 90) * Anim.D;
        switch (c.id) {
            case SylvaHero.RAIN -> {
                float k = Anim.hold(c.t, 2, 14, 3);
                p.lArm.xRot += -60 * k * Anim.D;
                p.rArm.xRot += -70 * k * Anim.D;
                p.rFore.zRot -= 110 * k * Anim.D;
                p.spine(-18 * k, 15 * k, 0);
                nock.visible = k > 0.3f && c.t < 10;
                bow.xRot = 0;
            }
            case SylvaHero.TRAP -> {
                float k = Anim.swing(c.t, 3, 3, 6);
                p.armR(-60 - 60 * k, 0, 10, 30);
                p.spine(15 * Math.abs(k), 0, 0);
            }
            case SylvaHero.ROLL -> {
                float k = Anim.pulse(c.t, 0, 14);
                p.crouch(k, 5);
                p.spine(-35 * k, 0, 0);
                p.pelvis.xRot -= 0.9f * k;
                p.armR(-80 * k, 0, 0, 60 * k);
            }
            default -> {
            }
        }
    }
}
