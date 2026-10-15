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
import com.lastbastion.entity.hero.BeelzebubDevil;
import com.lastbastion.entity.hero.HeroEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Beelzebub, Lord of the Flies: a hunched, bloated insect-king. A great fly's head with two enormous faceted red
 * eyes, twitching antennae, hooked mandibles and a dripping proboscis under a crown of bone and teeth; glossy
 * black-green chitin over a swollen abdomen of glowing rot-sacs; a pair of hooked forelimbs below his clawed arms;
 * jointed insect legs; two pairs of veined fly wings; a moth-eaten purple robe; and a sceptre crowned with a
 * pulsing maggot-orb. His belly swells with every meal (Gluttony).
 */
public final class BeelzebubModel implements HeroModels.Def {
    private static final float K = 1.35f;

    @Override
    public float shadow() {
        return 1.0f;
    }

    @Override
    public int density() {
        return 3;
    }

    @Override
    public void build(ModelSpec s) {
        Paint chitin = p(Mat.CHITIN, 0x1A2214).accent(0x3A4A22).gloss(0.8f).pattern(Pattern.PLATES, 0x10160C, 1.1f);
        Paint chitinLight = p(Mat.CHITIN, 0x2E3A1E).accent(0x4A5A2A).gloss(0.7f);
        Paint belly = p(Mat.FLESH, 0x8A9A5A).pattern(Pattern.VEINS, 0x3A4A1A, 1.2f).grime(0.3f);
        Paint sacs = Paint.glowing(0x9BE040).accent(0xE0FF90).pattern(Pattern.VEINS, 0x5A8A20, 1f);
        Paint eye = p(Mat.CRYSTAL, 0x7A0A06).pattern(Pattern.HEX, 0xFF3A1A, 1.8f).accentGlow().gloss(0.9f);
        Paint bone = p(Mat.BONE, 0xD8CCB0).accent(0x8A7A60);
        Paint robe = p(Mat.CLOTH, 0x3A1446).accent(0x220A2A).pattern(Pattern.SPOTS, 0x14061A, 1.4f).trim(0x8A7A3A, 0.4f);
        Paint wing = p(Mat.GLASS, 0x9AA8A8).accent(0xC8D0C8).pattern(Pattern.VEINS, 0x3A4038, 1.5f).gloss(0.9f);
        Paint hair = p(Mat.FUR, 0x141810).accent(0x2A3020);
        Paint drip = Paint.glowing(0xA8E048).accent(0xE8FF90);
        Paint face = p(Mat.CHITIN, 0x1E2618).gloss(0.8f).pattern(Pattern.PLATES, 0x10160C, 1.4f);

        Body.Spec sp = new Body.Spec(chitin, chitin).scale(K);
        sp.legs(6.5f, 6.5f, 3.4f);
        sp.foot = 1.4f;
        sp.hipW = 8.4f;
        sp.torso(10, 8, 6.4f, 10.4f, 6, 8);
        sp.neck = 1f;
        sp.head(8.2f, 7, 7.6f);
        sp.arms(6.4f, 6.8f, 3.2f);
        sp.hand = 2.6f;
        sp.paints(chitin, belly, chitin, chitinLight, chitinLight, chitin, chitinLight, chitin);
        sp.headPaint(face);
        sp.neckP = hair;
        Body b = Body.build(s.root, sp);

        // ---- the fly's head: compound eyes, antennae, mandibles, proboscis, crown of bone
        Part head = b.head;
        for (int side = -1; side <= 1; side += 2) {
            Part e = head.sub(side * 3.2f * K, -4.2f * K, -1.2f * K);
            e.sphere(0, 0, 0, 2.7f * K, eye);
            e.sphere(side * 0.3f * K, -0.2f * K, -0.6f * K, 2.2f * K, eye);
            Part ant = head.child(side < 0 ? "antenna_r" : "antenna_l", side * 1.4f * K, -7 * K, -2.4f * K).rot(-35, 0, side * 25);
            ant.box(-0.25f * K, -6 * K, -0.25f * K, 0.5f * K, 6 * K, 0.5f * K, chitinLight);
            for (int i = 0; i < 4; i++) ant.sub(0, -2.2f * K - i * 1.1f * K, 0).rot(0, 0, side * 50).box(-0.15f * K, -1.4f * K, -0.15f * K, 0.3f * K, 1.4f * K, 0.3f * K, hair);
            Part mand = head.child(side < 0 ? "mandible_r" : "mandible_l", side * 1.8f * K, -1 * K, -3.6f * K);
            mand.spike(0, 0, 0, 1.2f * K, 3.6f * K, 160, side * 25, side * -15, bone);
        }
        head.box(-2 * K, -2.6f * K, -4.2f * K, 4 * K, 2.4f * K, 1.4f * K, chitinLight.copy().decal(Face.FRONT, DecalType.FANGS, 0x1A0A06, false));
        Part prob = head.child("proboscis", 0, -0.8f * K, -4 * K);
        prob.taper(0, 0, 0, 1.6f * K, 1.6f * K, 0.8f * K, 0.8f * K, -6 * K, 3, chitinLight);
        prob.box(-0.4f * K, 6 * K, -0.4f * K, 0.8f * K, 1.2f * K, 0.8f * K, drip);
        Part crown = head.sub(0, -7 * K, 0);
        crown.box(-3.6f * K, -0.6f * K, -3.4f * K, 7.2f * K, 1 * K, 6.8f * K, bone);
        for (int i = 0; i < 7; i++) {
            float a = (i - 3) * 26;
            crown.spike(Mth.sin(a * Mth.DEG_TO_RAD) * 3.4f * K, -0.5f * K, -Mth.cos(a * Mth.DEG_TO_RAD) * 3.2f * K, 0.8f * K,
                    (1.6f + (i % 2) * 1.4f) * K, -10, 0, (i - 3) * -6, bone);
        }
        crown.sub(0, 0, 0).from(4).box(-0.7f * K, -2.4f * K, -3.6f * K, 1.4f * K, 1.4f * K, 0.8f * K, Paint.glowing(0xC81A10));

        // ---- the bloated body: swollen abdomen with rot-sacs, chitin plates, the robe
        Part gut = b.pelvis.child("gut", 0, -3 * K, -2.8f * K);
        gut.sphere(0, 0, 0, 4.2f * K, belly);
        gut.sphere(-2.4f * K, 1 * K, -2.6f * K, 1.4f * K, sacs);
        gut.sphere(2.2f * K, -0.6f * K, -2.8f * K, 1.2f * K, sacs);
        gut.sphere(0.4f * K, 2.2f * K, -3 * K, 1.0f * K, sacs);
        gut.sub(0, 0, 0).from(5).sphere(-0.8f * K, -2 * K, -3.4f * K, 1.1f * K, sacs);
        b.chest.box(-5.2f * K, -8.4f * K, -3.4f * K, 10.4f * K, 3.2f * K, 6.8f * K, chitin);
        for (int i = 0; i < 3; i++) b.chest.box(-4 * K, (-5 + i * 1.6f) * K, -3.5f * K, 8 * K, 0.6f * K, 0.4f * K, chitinLight);
        Part robePart = Gear.cape(b.chest, "robe", 11 * K, 8 * K, 6.4f * K, 18 * K, robe);
        robePart.sub(0, 0, 0).box(-6 * K, -1 * K, -0.4f * K, 12 * K, 2.2f * K, 1.4f * K, p(Mat.FUR, 0x2A2010).accent(0x4A3A20));
        // hooked forelimbs under the arms
        for (int side = -1; side <= 1; side += 2) {
            Part fl = b.chest.child(side < 0 ? "fore_r" : "fore_l", side * 3.6f * K, -3 * K, -2.6f * K).rot(-40, 0, side * 10);
            fl.box(-0.5f * K, 0, -0.5f * K, K, 4 * K, K, chitinLight);
            Part fl2 = fl.child(side < 0 ? "fore2_r" : "fore2_l", 0, 4 * K, 0).rot(-70, 0, 0);
            fl2.box(-0.4f * K, 0, -0.4f * K, 0.8f * K, 3.4f * K, 0.8f * K, chitin);
            fl2.spike(0, 3.4f * K, 0, 0.6f * K, 1.8f * K, 160, 0, 0, bone);
        }
        // clawed hands, spiked insect legs
        for (int side = -1; side <= 1; side += 2) {
            Part hand = side < 0 ? b.rHand : b.lHand;
            for (int f = 0; f < 2; f++) hand.spike((f - 0.5f) * 1.2f * K, 2.6f * K, -0.6f * K, 0.5f * K, 2.2f * K, 180 - 20, 0, 0, bone);
            Part shin = side < 0 ? b.rShin : b.lShin;
            shin.spike(0, 1.5f * K, 1.4f * K, 0.6f * K, 2.2f * K, 70, 0, 0, chitinLight);
            shin.spike(0, 4 * K, 1.4f * K, 0.5f * K, 1.6f * K, 70, 0, 0, chitinLight);
            Part thigh = side < 0 ? b.rThigh : b.lThigh;
            thigh.box(-2 * K, 0, -2 * K, 4 * K, 3 * K, 4 * K, hair);
        }
        // two pairs of veined fly wings
        String[] wn = {"fwing_hi", "fwing_lo"};
        for (int i = 0; i < 2; i++) {
            float len = (i == 0 ? 19 : 13) * K, w = (i == 0 ? 5.4f : 4f) * K;
            for (int side = -1; side <= 1; side += 2) {
                Part wg = b.chest.child(wn[i] + (side < 0 ? "_r" : "_l"), side * 1.8f * K, (-7 + i * 2.6f) * K, 3.4f * K);
                Part blade = wg.sub(0, 0, 0).rot(0, side * -10, side * -60);
                blade.box(-w / 2, 0, -0.15f * K, w, len, 0.3f * K, wing);
                blade.box(-w * 0.35f, len, -0.15f * K, w * 0.7f, len * 0.12f, 0.3f * K, wing);
                blade.box(-0.25f * K, 0, -0.25f * K, 0.5f * K, len, 0.5f * K, chitinLight);
            }
        }
        // the sceptre of rot: a femur haft crowned with a maggot-orb
        Part sceptre = b.rHand.child("sceptre", 0, 1.3f * K, 0);
        sceptre.box(-0.5f * K, -0.5f * K, -14 * K, K, K, 18 * K, bone);
        sceptre.sphere(0, 0, 4.8f * K, 1 * K, bone);
        Part orb = sceptre.child("orb", 0, 0, -15.5f * K);
        orb.sphere(0, 0, 0, 2.2f * K, sacs);
        for (int i = 0; i < 4; i++) orb.spike(0, 0, 0, 0.6f * K, 3.4f * K, 90 + i * 35, i * 90, 0, bone);
        orb.box(-0.4f * K, 2 * K, -0.4f * K, 0.8f * K, 1.4f * K, 0.8f * K, drip);
    }

    @Override
    public void animate(ProcModel<HeroEntity> m, HeroEntity h, HeroModels.Ctx c) {
        Poser p = Poser.of(m);
        HeroModels.humanoid(p, h, c, 0.7f, 0.35f);
        BeelzebubDevil bz = h instanceof BeelzebubDevil d ? d : null;
        int gorged = bz != null ? bz.gorged() : 0;
        // hunched, heavy
        p.chest.xRot += 0.25f;
        p.head.xRot -= 0.15f;
        p.rThigh.xRot -= 0.2f;
        p.lThigh.xRot -= 0.2f;
        p.rShin.xRot += 0.45f;
        p.lShin.xRot += 0.45f;
        p.pelvis.y += 1.0f;
        ModelPart gut = m.p("gut");
        float swell = 1f + 0.07f * gorged + 0.02f * Mth.sin(c.age * 0.1f);
        gut.xScale = gut.yScale = gut.zScale = swell;
        // twitching antennae, chattering mandibles, swaying proboscis
        m.p("antenna_r").zRot += Mth.sin(c.age * 0.35f) * 0.12f;
        m.p("antenna_l").zRot -= Mth.sin(c.age * 0.31f + 1) * 0.12f;
        float chatter = c.combat ? Mth.sin(c.age * 1.6f) * 0.15f : Mth.sin(c.age * 0.2f) * 0.05f;
        m.p("mandible_r").yRot += chatter;
        m.p("mandible_l").yRot -= chatter;
        m.p("proboscis").xRot += Mth.sin(c.age * 0.12f) * 0.1f;
        // wings buzz
        float buzz = Mth.sin(c.age * 2.6f) * (c.combat ? 0.35f : 0.12f);
        m.p("fwing_hi_r").yRot += 0.3f + buzz;
        m.p("fwing_hi_l").yRot -= 0.3f + buzz;
        m.p("fwing_lo_r").yRot += 0.5f - buzz;
        m.p("fwing_lo_l").yRot -= 0.5f - buzz;
        // forelimbs twitch
        m.p("fore_r").xRot += Mth.sin(c.age * 0.25f) * 0.12f;
        m.p("fore_l").xRot += Mth.sin(c.age * 0.25f + 2) * 0.12f;
        m.p("robe").xRot += 0.15f + Math.min(1, c.la) * 0.35f + Mth.sin(c.age * 0.08f) * 0.03f;
        m.p("robe2").xRot += 0.08f;
        m.p("orb").yRot = c.age * 0.08f;
        // sceptre held aloft in combat
        if (c.combat || c.id != 0) p.armR(-70, -10, 0, 40);
        else p.armR(-20, 0, 8, 30);
        switch (c.id) {
            case 1, 2, 3, 4, 5 -> {
                float k = Anim.swing(c.t, 9, 3, 6);
                p.armR(-60 * Math.max(0, k) + 40 * Math.max(0, -k), 0, 0, 0);
                p.armL(-90 * Math.max(0, -k), 20, 0, 30);
            }
            case BeelzebubDevil.SWARM -> {
                float k = Anim.pulse(c.t, 0, 18);
                p.armR(-140 * k, 0, 20 * k, 10);
                p.armL(-140 * k, 0, -20 * k, 10);
                p.head.xRot -= 0.5f * k;
            }
            case BeelzebubDevil.GLUTTONY -> {
                float lunge = Anim.hold(c.t, 0, 8, 3);
                float bite = Anim.pulse(c.t, 8, 6);
                p.spine(40 * lunge, 0, 0);
                p.armR(-90 * lunge, 30, 0, 50);
                p.armL(-90 * lunge, -30, 0, 50);
                m.p("mandible_r").yRot -= 0.6f * bite;
                m.p("mandible_l").yRot += 0.6f * bite;
                p.head.xRot += 0.5f * bite;
            }
            case BeelzebubDevil.PESTILENCE -> {
                float k = Anim.hold(c.t, 2, 14, 4);
                p.armR(-170 * k, 0, 0, 0);
                p.armL(-160 * k, 0, -30 * k, 10);
            }
            case BeelzebubDevil.BROOD, BeelzebubDevil.DECAY -> {
                float k = Anim.pulse(c.t, 0, 20);
                p.armR(-60 * k, 0, 70 * k, 20);
                p.armL(-60 * k, 0, -70 * k, 20);
                p.spine(-15 * k, 0, 0);
                m.p("fwing_hi_r").yRot += 0.5f * k;
                m.p("fwing_hi_l").yRot -= 0.5f * k;
            }
            case BeelzebubDevil.BILE -> {
                float k = Anim.hold(c.t, 6, 22, 4);
                p.head.xRot += 0.4f * k;
                p.spine(20 * k, 0, 0);
                m.p("mandible_r").yRot -= 0.7f * k;
                m.p("mandible_l").yRot += 0.7f * k;
            }
            default -> {
            }
        }
    }
}
