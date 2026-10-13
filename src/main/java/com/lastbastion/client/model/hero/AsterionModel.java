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
import com.lastbastion.entity.hero.AsterionHero;
import com.lastbastion.entity.hero.HeroEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Asterion, the Minotaur King: a towering bull-man with sweeping gold-capped horns and a nose ring,
 * dark fur, a war-painted chest crossed by leather straps, bronze pauldrons, belt and greaves, a red
 * loincloth, cloven hooves and a colossal double-bladed labrys. Lv4 hangs trophy skulls from his belt;
 * Lv5 crowns him in gold with molten veins under the fur.
 */
public final class AsterionModel implements HeroModels.Def {
    private static final float K = 1.45f;

    @Override
    public float shadow() {
        return 1.1f;
    }

    @Override
    public int density() {
        return 3;
    }

    @Override
    public void build(ModelSpec s) {
        Paint fur = p(Mat.FUR, 0x5A3420).accent(0x3A2010);
        Paint hide = p(Mat.SKIN, 0x8A5A3A).pattern(Pattern.MUSCLE, 0x6A4028, 1f)
                .decal(Face.FRONT, DecalType.STRIPE, 0xB02020, false, 0.3f, 0.5f, 0.8f)
                .decal(Face.FRONT, DecalType.STRIPE, 0xB02020, false, 0.7f, 0.5f, 0.8f);
        Paint muzzleP = p(Mat.FUR, 0x7A5038).accent(0x5A3420);
        Paint bronze = p(Mat.GOLD, 0xB0702A).accent(0xF0C080).gloss(0.5f);
        Paint leather = p(Mat.LEATHER, 0x4A2A18).accent(0x2A160A);
        Paint ivory = p(Mat.BONE, 0xEDE4CC).accent(0xB0A080);
        Paint gold = p(Mat.GOLD, 0xE0B848).accent(0xFFF4C0);
        Paint hoof = p(Mat.DARK_METAL, 0x2A2420);
        Paint cloth = p(Mat.CLOTH, 0x8A1A1A).accent(0x5A0E0E).trim(0xB0702A, 0.4f);
        Paint steel = p(Mat.METAL, 0xB8BCC4).gloss(0.6f).pattern(Pattern.RUNES, 0x8A9098, 1.4f);
        Paint molten = p(Mat.FUR, 0x5A3420).accent(0x3A2010).pattern(Pattern.VEINS, 0xFF6A10, 0.8f);
        Paint headP = p(Mat.FUR, 0x4A2A18).accent(0x2E1A0E)
                .decal(Face.FRONT, DecalType.SLIT_EYES, 0xFFB040, true, 0.5f, 0.38f, 1.2f).decal(Face.FRONT, DecalType.BROW, 0x2A160A, false, 0.5f, 0.32f, 1.1f);

        Body.Spec sp = new Body.Spec(hide, hide).scale(K);
        sp.legs(6, 5.6f, 4.4f);
        sp.foot = 1.2f;
        sp.hipW = 8;
        sp.torso(11, 8, 6, 8, 4.6f, 5);
        sp.neck = 1.4f;
        sp.head(6.6f, 6.8f, 7);
        sp.arms(6, 6, 4.2f);
        sp.hand = 2.6f;
        sp.paints(hide, fur, fur, fur, hide, fur, fur, hoof);
        sp.headPaint(headP);
        sp.neckP = fur;
        Body b = Body.build(s.root, sp);

        // bull head: long muzzle, nostrils, nose ring, ears, sweeping horns
        Part head = b.head;
        Part muzzle = head.child("muzzle", 0, -1.2f * K, -3.4f * K);
        muzzle.box(-2.4f * K, -2.6f * K, -3.6f * K, 4.8f * K, 3.8f * K, 4 * K, muzzleP.copy()
                .decal(Face.FRONT, DecalType.DOTS, 0x1A0E08, false, 0.5f, 0.35f, 0.5f));
        muzzle.box(-2.2f * K, 1.2f * K, -3.3f * K, 4.4f * K, 1.2f * K, 3.6f * K, muzzleP);
        Part ring = muzzle.sub(0, 0.4f * K, -3.8f * K).rot(90, 0, 0);
        ring.octoRing(-0.2f * K, 2.2f * K, 0.4f * K, 0.4f * K, gold);
        head.box(-3.6f * K, -7.2f * K, -3.6f * K, 7.2f * K, 1.4f * K, 6 * K, fur);
        Gear.ear(head, -1, -3.4f * K, -5 * K, 0.5f * K, 1.4f * K, 2.2f * K, fur);
        Gear.ear(head, 1, 3.4f * K, -5 * K, 0.5f * K, 1.4f * K, 2.2f * K, fur);
        for (int side = -1; side <= 1; side += 2) {
            Part h1 = head.sub(side * 3.2f * K, -5.6f * K, -0.6f * K).rot(0, 0, side * 80);
            h1.taper(0, 0, 0, 2.4f * K, 2.4f * K, 1.8f * K, 1.8f * K, 4 * K, 3, ivory);
            Part h2 = h1.sub(0, -4 * K, 0).rot(-25, 0, side * -55);
            h2.taper(0, 0, 0, 1.8f * K, 1.8f * K, 1 * K, 1 * K, 4.2f * K, 3, ivory);
            Part h3 = h2.sub(0, -4.2f * K, 0).rot(-20, 0, side * -35);
            h3.taper(0, 0, 0, 1 * K, 1 * K, 0.3f * K, 0.3f * K, 3 * K, 3, ivory);
            h3.box(-0.6f * K, -1.4f * K, -0.6f * K, 1.2f * K, 1 * K, 1.2f * K, gold);
        }
        Part crown = head.sub(0, -7.4f * K, 0).from(5);
        for (int i = 0; i < 5; i++) crown.spike((i - 2) * 1.3f * K, 0, -2.6f * K, 0.9f * K, (i == 2 ? 2.6f : 1.6f) * K, 0, 0, 0, gold);
        crown.box(-3.6f * K, -0.4f * K, -3.2f * K, 7.2f * K, 0.8f * K, 6.4f * K, gold);
        // body: fur mantle, straps, bronze pauldrons, belt with skull buckle, loincloth
        b.chest.box(-5.8f * K, -8.4f * K, -3.4f * K, 11.6f * K, 3 * K, 7 * K, fur);
        b.chest.sub(0, -4 * K, -3.1f * K).rot(0, 0, 35).box(-0.6f * K, -6 * K, -0.2f * K, 1.2f * K, 12 * K, 0.5f * K, leather);
        b.chest.sub(0, -4 * K, -3.1f * K).rot(0, 0, -35).box(-0.6f * K, -6 * K, -0.2f * K, 1.2f * K, 12 * K, 0.5f * K, leather);
        b.chest.sub(0, 0, 0).from(5).box(-5.6f * K, -8 * K, -3.2f * K, 11.2f * K, 8 * K, 6.4f * K, molten);
        Gear.pauldron(b.rArm, -1, 4.2f * K, bronze, gold).spike(0, -1.6f * K, 0, 1 * K, 2.4f * K, 0, 0, -30, bronze);
        Gear.pauldron(b.lArm, 1, 4.2f * K, bronze, gold).spike(0, -1.6f * K, 0, 1 * K, 2.4f * K, 0, 0, 30, bronze);
        Gear.belt(b.pelvis, 8 * K, 5 * K, -2 * K, 1.8f * K, leather.copy().trim(0xB0702A, 0.4f), bronze);
        b.pelvis.box(-1.4f * K, -2.4f * K, -2.9f * K, 2.8f * K, 2.6f * K, 0.6f * K, p(Mat.BONE, 0xE8E0C8)
                .decal(Face.FRONT, DecalType.SKULL, 0xE8E0C8, false, 0.5f, 0.5f, 1.6f));
        Part loin = b.pelvis.child("loin", 0, 0.2f * K, -2.6f * K);
        loin.box(-2.2f * K, 0, -0.3f * K, 4.4f * K, 6 * K, 0.6f * K, cloth);
        Part loinB = b.pelvis.child("loin_b", 0, 0.2f * K, 2.6f * K);
        loinB.box(-2.4f * K, 0, -0.3f * K, 4.8f * K, 5.4f * K, 0.6f * K, cloth);
        Part skulls = b.pelvis.sub(0, 0, 0).from(4);
        skulls.box(-4.6f * K, -0.6f * K, -2 * K, 1.6f * K, 1.8f * K, 1.6f * K, p(Mat.BONE, 0xE8E0C8).decal(Face.FRONT, DecalType.HOLLOW_EYES, 0x1A1410, false));
        skulls.box(3 * K, -0.4f * K, -1 * K, 1.6f * K, 1.8f * K, 1.6f * K, p(Mat.BONE, 0xE8E0C8).decal(Face.FRONT, DecalType.HOLLOW_EYES, 0x1A1410, false));
        // greaves, bracers, chain at Lv3
        Gear.sleeve(b.rShin, 4.4f * K, 0.4f * K, 3.6f * K, 4.4f * K, 0.1f, bronze);
        Gear.sleeve(b.lShin, 4.4f * K, 0.4f * K, 3.6f * K, 4.4f * K, 0.1f, bronze);
        Gear.sleeve(b.rFore, 4.2f * K, 2.4f * K, 3 * K, 4.2f * K, 0.1f, bronze);
        Gear.sleeve(b.lFore, 4.2f * K, 2.4f * K, 3 * K, 4.2f * K, 0.1f, bronze);
        b.lArm.sub(0, 0, 0).from(3).box(-2.4f * K, 2 * K, -2.4f * K, 4.8f * K, 1 * K, 4.8f * K, p(Mat.METAL, 0x5A5C62).pattern(Pattern.CHAINMAIL, 0x9A9CA2, 2f));
        // the labrys: haft along the hand's -Z, blades at the far end
        Part axe = b.rHand.child("labrys", 0, 1.6f * K, 0);
        axe.box(-0.55f * K, -0.55f * K, -14 * K, 1.1f * K, 1.1f * K, 18 * K, p(Mat.WOOD, 0x3A2416).accent(0x22140A));
        axe.box(-0.7f * K, -0.7f * K, -6 * K, 1.4f * K, 1.4f * K, 1 * K, bronze).box(-0.7f * K, -0.7f * K, 2 * K, 1.4f * K, 1.4f * K, 1 * K, bronze);
        Part heads = axe.child("axeheads", 0, 0, -12.6f * K);
        for (int side = -1; side <= 1; side += 2) {
            Part bl = heads.sub(side * 0.6f * K, 0, 0);
            bl.box(side < 0 ? -2.4f * K : 0, -0.35f * K, -1.6f * K, 2.4f * K, 0.7f * K, 3.2f * K, steel);
            bl.box(side < 0 ? -4.4f * K : 2.4f * K, -0.3f * K, -3.2f * K, 2 * K, 0.6f * K, 6.4f * K, steel);
            bl.box(side < 0 ? -5.4f * K : 4.4f * K, -0.25f * K, -4.2f * K, 1 * K, 0.5f * K, 8.4f * K, p(Mat.METAL, 0xE0E4EA).gloss(0.9f));
        }
        heads.box(-1 * K, -1 * K, -1 * K, 2 * K, 2 * K, 2 * K, bronze);
        heads.spike(0, 0, -1.2f * K, 0.8f * K, 2.2f * K, 90, 0, 0, bronze);
    }

    @Override
    public void animate(ProcModel<HeroEntity> m, HeroEntity h, HeroModels.Ctx c) {
        Poser p = Poser.of(m);
        HeroModels.humanoid(p, h, c, 0.85f, 0.4f);
        float move = Math.min(1, c.la);
        ModelPart loin = m.p("loin"), loinB = m.p("loin_b"), axe = m.p("labrys");
        loin.xRot += Math.min(0, Math.min(p.rThigh.xRot, p.lThigh.xRot)) * 0.8f;
        loinB.xRot += Math.max(0, Math.max(p.rThigh.xRot, p.lThigh.xRot)) * 0.8f;
        p.chest.xRot += 0.12f;
        p.head.xRot -= 0.08f;
        boolean rage = h instanceof AsterionHero a && a.raging();
        if (rage) {
            p.chest.y += Mth.sin(c.age * 0.6f) * 0.3f;
            p.head.xRot += 0.15f;
        }
        // carry the labrys on the shoulder; two-handed when fighting
        if (c.combat || c.id != 0) {
            p.armR(-40, -20, 0, 50);
            p.armL(-50, 35, 0, 70);
            p.stance(0.4f);
        } else {
            p.armR(-150, -20, 10, 60);
            axe.xRot += 0.4f;
        }
        switch (c.id) {
            case 1 -> {
                float wind = c.t < 9 ? Anim.smooth(c.t / 9f) : 0;
                float cut = Anim.smooth(Anim.win(c.t, 9, 3)) * (1 - Anim.win(c.t, 13, 6));
                p.armR(-120 * wind + 70 * cut, -40 * wind + 40 * cut, 0, -30 * wind);
                p.armL(-110 * wind + 60 * cut, 30 * wind, 0, -20 * wind);
                p.spine(-15 * wind + 30 * cut, 25 * wind - 30 * cut, 0);
            }
            case 2 -> {
                float wind = c.t < 9 ? Anim.smooth(c.t / 9f) : 0;
                float cut = Anim.smooth(Anim.win(c.t, 9, 3)) * (1 - Anim.win(c.t, 13, 6));
                p.armR(-60 * wind - 60 * cut, 70 * wind - 90 * cut, 0, 10);
                p.armL(-60 * wind - 60 * cut, 60 * wind - 70 * cut, 0, 20);
                p.spine(5 * cut, 50 * wind - 70 * cut, 0);
            }
            case 3 -> {
                float k = Anim.swing(c.t, 9, 2, 7);
                float gore = Math.max(0, -k);
                p.spine(-15 * Math.max(0, k) + 45 * gore, 0, 0);
                p.neck.xRot += 0.6f * gore;
                p.crouch(gore * 0.5f, 3);
            }
            case AsterionHero.RUSH -> {
                float k = Anim.hold(c.t, 8, 26, 3);
                p.spine(40 * k, 0, 0);
                p.neck.xRot += 0.5f * k;
                p.armR(40 * k, 0, 20 * k, 40);
                p.armL(40 * k, 0, -20 * k, 40);
            }
            case AsterionHero.STOMP -> {
                float lift = Anim.hold(c.t, 2, 11, 2) * (c.t < 13 ? 1 : 0);
                float stomp = Anim.pulse(c.t, 13, 8);
                p.legR(-80 * lift, 0, 70 * lift);
                p.spine(-10 * lift + 15 * stomp, 0, 0);
                p.armL(-30 * lift, 0, -40 * lift, 30);
            }
            case AsterionHero.SPLIT -> {
                float up = c.t < 16 ? Anim.smooth(Math.min(1, c.t / 14f)) : 0;
                float down = Anim.hold(c.t, 17, 30, 4);
                p.armR(-175 * up - 70 * down, 0, 0, 10);
                p.armL(-170 * up - 65 * down, 0, 0, 10);
                p.spine(-20 * up + 45 * down, 0, 0);
                p.crouch(down * 0.7f, 5);
            }
            case AsterionHero.BLOODRAGE -> {
                float k = Anim.pulse(c.t, 0, 16);
                p.armR(-120 * k, 0, 50 * k, 70);
                p.armL(-120 * k, 0, -50 * k, 70);
                p.spine(-20 * k, 0, 0);
                p.head.xRot -= 0.6f * k;
            }
            case HeroEntity.ULT -> {
                float roar = Anim.hold(c.t, 0, 12, 3) * (c.t < 15 ? 1 : 0);
                p.armR(-120 * roar, 0, 50 * roar, 60);
                p.armL(-120 * roar, 0, -50 * roar, 60);
                p.head.xRot -= 0.7f * roar;
                if (c.t > 14 && c.t < 170) {
                    p.pelvis.yRot += (c.age * 0.9f) % Mth.TWO_PI;
                    p.armR(-50, 80, 0, 0);
                    p.armL(-60, 70, 0, 10);
                    p.crouch(0.3f, 2);
                }
            }
            default -> {
            }
        }
        if (move > 0.6f && c.id == 0) p.spine(8, 0, 0);
    }
}
