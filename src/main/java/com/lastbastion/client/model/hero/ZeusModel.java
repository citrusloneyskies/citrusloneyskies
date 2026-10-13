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
import com.lastbastion.entity.hero.ZeusHero;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Zeus, King of Olympus: a towering, broad-chested god with a long white beard and mane, a gold laurel
 * crown and eyes full of lightning; a white toga with a gold key border slung over one shoulder, gold
 * bracers and belt, laced sandals; the Master Bolt crackling in his right hand and the Aegis, bearing the
 * Gorgon's face, on his left arm. Lv4 adds a storm-blue cape; Lv5 a floating crown of storm clouds.
 */
public final class ZeusModel implements HeroModels.Def {
    private static final float K = 1.38f;

    @Override
    public float shadow() {
        return 0.8f;
    }

    @Override
    public void build(ModelSpec s) {
        Paint skin = p(Mat.SKIN, 0xE8B890).accent(0xC89870);
        Paint chest = p(Mat.SKIN, 0xE8B890).pattern(Pattern.MUSCLE, 0xC89870, 1f);
        Paint toga = p(Mat.ROBE, 0xF6F4EE).accent(0xC8C4B8).trim(0xE0B848, 0.4f);
        Paint gold = p(Mat.GOLD, 0xE6C050).accent(0xFFF4C0);
        Paint hair = p(Mat.HAIR, 0xF0F0F4).accent(0xB8B8C8);
        Paint sandal = p(Mat.LEATHER, 0x6A4A2A).pattern(Pattern.BANDS, 0x4A3018, 2.4f);
        Paint bolt = Paint.glowing(0xFFF4A0).accent(0xFFFFFF);
        Paint cloud = p(Mat.CLOTH, 0x5A6070).accent(0x3A3E48).pattern(Pattern.VEINS, 0xC8F0FF, 1f);
        Paint head = Gear.face(0xE8B890, 0xC8F0FF, true, 0xF0F0F4, 0xA0605A).decal(Face.FRONT, DecalType.BEARD, 0xF0F0F4, false, 0.5f, 0.5f, 1f);

        Body.Spec sp = new Body.Spec(skin, chest).scale(K);
        sp.legs(6, 5.6f, 3.8f);
        sp.foot = 1.2f;
        sp.hipW = 7.6f;
        sp.torso(9.6f, 7.6f, 5.4f, 7.4f, 4.6f, 4.6f);
        sp.neck = 1.2f;
        sp.head(6.8f);
        sp.arms(5.8f, 5.6f, 3.8f);
        sp.hand = 2.2f;
        sp.paints(chest, toga, skin, skin, skin, toga, skin, sandal);
        sp.headPaint(head);
        Body b = Body.build(s.root, sp);

        // hair, beard, laurels
        Gear.hair(b.head, 6.8f * K, 6.8f * K, 6.8f * K, 0.7f * K, 1f, hair);
        b.head.child("mane", 0, -2 * K, 3.6f * K).box(-3.8f * K, 0, -0.5f * K, 7.6f * K, 6 * K, 1.2f * K, hair);
        Part beard = b.head.child("beard", 0, -1.2f * K, -3.2f * K);
        beard.box(-3 * K, 0, -0.9f * K, 6 * K, 3.6f * K, 1.6f * K, hair);
        beard.child("beard2", 0, 3.6f * K, 0).box(-2.2f * K, 0, -0.7f * K, 4.4f * K, 2.8f * K, 1.2f * K, hair);
        b.head.box(-1.6f * K, -2.4f * K, -3.75f * K, 3.2f * K, 0.8f * K, 0.5f * K, hair);
        Gear.circlet(b.head, 6.8f * K, 6.8f * K, -5.6f * K, 12, 1.4f * K, gold);
        Part storm = b.head.sub(0, -9.5f * K, 0).from(5);
        for (int i = 0; i < 7; i++) {
            float a = i * Mth.TWO_PI / 7;
            storm.sphere(Mth.cos(a) * 3.8f * K, Mth.sin(a * 3) * 0.5f * K, Mth.sin(a) * 3.8f * K, 1.5f * K, cloud);
        }
        // toga over the left shoulder, gold belt, skirt panels
        Part drape = b.chest.sub(0, -3.6f * K, 0).rot(0, 0, -32);
        drape.box(-2.2f * K, -5.6f * K, -2.95f * K, 4.4f * K, 11.2f * K, 5.9f * K, toga);
        b.lArm.box(-2.2f * K, -2 * K, -2.2f * K, 4.4f * K, 3.4f * K, 4.4f * K, toga);
        Gear.belt(b.pelvis, 7.4f * K, 4.6f * K, -1.8f * K, 1.4f * K, gold, p(Mat.CRYSTAL, 0x60C0FF).glow());
        Part skirtF = b.pelvis.child("skirt_f", 0, 0.4f * K, -2.4f * K);
        skirtF.box(-3.6f * K, 0, -0.35f * K, 7.2f * K, 7.4f * K, 0.7f * K, toga);
        Part skirtB = b.pelvis.child("skirt_b", 0, 0.4f * K, 2.4f * K);
        skirtB.box(-3.8f * K, 0, -0.35f * K, 7.6f * K, 7.8f * K, 0.7f * K, toga);
        Gear.sleeve(b.rFore, 4 * K, 2.6f * K, 3 * K, 4 * K, 0.05f, gold);
        Gear.sleeve(b.lFore, 4 * K, 2.6f * K, 3 * K, 4 * K, 0.05f, gold);
        Gear.sleeve(b.rShin, 3.8f * K, 0.4f * K, 4.6f * K, 3.8f * K, 0.02f, sandal);
        Gear.sleeve(b.lShin, 3.8f * K, 0.4f * K, 4.6f * K, 3.8f * K, 0.02f, sandal);
        Gear.cape(b.chest, "cape", 9.4f * K, 7.6f * K, 5.4f * K, 14 * K, p(Mat.CLOTH, 0x2A4A8A).accent(0x1A2E5A).trim(0xE6C050, 0.4f)).levels(4, 9);
        // the Master Bolt: zigzag of light gripped in the right hand
        Part mb = b.rHand.child("masterbolt", 0, 1.2f * K, 0);
        Part z1 = mb.sub(0, 0, 0).rot(0, 0, 20);
        z1.box(-0.5f * K, -6 * K, -0.5f * K, 1 * K, 6 * K, 1 * K, bolt);
        Part z2 = z1.sub(0, -6 * K, 0).rot(0, 0, -55);
        z2.box(-0.45f * K, -3.4f * K, -0.45f * K, 0.9f * K, 3.4f * K, 0.9f * K, bolt);
        Part z3 = z2.sub(0, -3.4f * K, 0).rot(0, 0, 55);
        z3.taper(0, 0, 0, 1.2f * K, 1.2f * K, 0.2f * K, 0.2f * K, 4 * K, 3, bolt);
        Part z4 = mb.sub(0, 0, 0).rot(0, 0, 200);
        z4.box(-0.45f * K, -4 * K, -0.45f * K, 0.9f * K, 4 * K, 0.9f * K, bolt);
        z4.sub(0, -4 * K, 0).rot(0, 0, 50).taper(0, 0, 0, 1 * K, 1 * K, 0.2f * K, 0.2f * K, 3 * K, 3, bolt);
        mb.box(-0.6f * K, -0.8f * K, -0.6f * K, 1.2f * K, 1.6f * K, 1.2f * K, gold);
        // the Aegis: round shield on the outside of the left forearm, Gorgon face on the outer (LEFT) face
        Part aegis = b.lFore.child("aegis", 2.4f * K, 2.6f * K, 0);
        Paint shieldFace = p(Mat.GOLD, 0xD0A040).accent(0xFFF0B0).decal(Face.LEFT, DecalType.EYE_SIGIL, 0x30B0A0, true, 0.5f, 0.5f, 1.1f)
                .decal(Face.LEFT, DecalType.RING, 0x8A6A20, false, 0.5f, 0.5f, 1.8f);
        aegis.octoX(-0.4f * K, 0, 0, 8.6f * K, 0.8f * K, shieldFace);
        aegis.octoX(0.35f * K, 0, 0, 3.2f * K, 0.5f * K, gold);
        for (int i = 0; i < 8; i++) {
            float a = i * Mth.TWO_PI / 8;
            aegis.sub(0, Mth.sin(a) * 4.3f * K, Mth.cos(a) * 4.3f * K).rot(a * Mth.RAD_TO_DEG, 0, 0).box(-0.35f * K, -0.4f * K, -1 * K, 0.7f * K, 0.8f * K, 2 * K,
                    p(Mat.SCALES, 0x3A7A4A).accent(0x1A4A2A));
        }
    }

    @Override
    public void animate(ProcModel<HeroEntity> m, HeroEntity h, HeroModels.Ctx c) {
        Poser p = Poser.of(m);
        HeroModels.humanoid(p, h, c, 0.9f, 0.5f);
        float move = Math.min(1, c.la);
        ModelPart sf = m.p("skirt_f"), sb = m.p("skirt_b"), beard = m.p("beard"), mane = m.p("mane"), mb = m.p("masterbolt");
        sf.xRot += Math.min(0, Math.min(p.rThigh.xRot, p.lThigh.xRot)) * 0.8f;
        sb.xRot += Math.max(0, Math.max(p.rThigh.xRot, p.lThigh.xRot)) * 0.8f;
        beard.xRot += Mth.sin(c.age * 0.07f) * 0.03f + move * 0.1f;
        mane.xRot += move * 0.3f + Mth.sin(c.age * 0.06f) * 0.04f;
        ModelPart cape = m.opt("cape");
        if (cape != null && cape.visible) {
            cape.xRot += 0.06f + move * 0.5f + Mth.sin(c.age * 0.1f) * 0.04f;
            m.p("cape2").xRot += move * 0.25f;
        }
        float crackle = 1 + Mth.sin(c.age * 1.7f) * Mth.sin(c.age * 0.9f) * 0.08f;
        mb.xScale = mb.zScale = crackle;
        p.chest.xRot -= 0.05f;
        p.head.xRot -= 0.05f;
        // regal stance: bolt raised loosely, Aegis forward
        p.armR(-30, 0, 15, 70);
        p.armL(c.combat ? -50 : -15, c.combat ? -30 : 0, -8, 40);
        switch (c.id) {
            case 1, 2, 3 -> {
                float wind = c.t < 12 ? Anim.smooth(c.t / 12f) : 0;
                float thr = Anim.hold(c.t, 12, 15, 2);
                p.armR(-140 * wind - 60 * thr + 30, 30 * wind, 20 * wind, -40 * wind);
                p.spine(-8 * wind + 15 * thr, 30 * wind - 25 * thr, 0);
                p.legL(-25 * thr, 0, 20 * thr);
                mb.visible = c.t < 12 || c.t > 20;
            }
            case ZeusHero.CLAP -> {
                float up = Anim.hold(c.t, 0, 9, 2) * (c.t < 11 ? 1 : 0);
                float slam = Anim.hold(c.t, 11, 18, 2);
                p.armL(-120 * up - 50 * slam, -40 * up, 0, 20);
                p.crouch(slam, 5);
                p.spine(-10 * up + 30 * slam, 0, 0);
            }
            case ZeusHero.CHAIN -> {
                float k = Anim.hold(c.t, 8, 20, 3);
                p.armR(-95 * k, -20 * k, 0, 0);
                p.armL(-60 * k, 60 * k, -50 * k, 0);
                p.spine(0, 25 * k, 0);
            }
            case ZeusHero.SMITE -> {
                float up = Anim.hold(c.t, 2, 18, 3) * (c.t < 21 ? 1 : 0);
                float down = Anim.hold(c.t, 21, 28, 2);
                p.armR(-175 * up - 100 * down, 0, 10, 0);
                p.spine(-15 * up + 20 * down, 0, 0);
                p.head.xRot -= 0.5f * up;
            }
            case ZeusHero.AEGIS -> {
                float k = Anim.hold(c.t, 6, 20, 4);
                p.armL(-165 * k, 0, -10 * k, 10);
                p.armR(-110 * k, 0, 40 * k, 20);
                p.spine(-12 * k, 0, 0);
            }
            case HeroEntity.ULT -> {
                float rise = Math.min(1, c.t / 14f);
                float land = Anim.hold(c.t, 93, 140, 3);
                float air = (c.t < 92 ? rise : 1 - Anim.win(c.t, 92, 4));
                p.armR(-165 * air, 0, 30 * air, 10);
                p.armL(-60 * air, 0, -70 * air, 10);
                p.legR(15 * air, 5 * air, 40 * air);
                p.legL(-10 * air, -5 * air, 20 * air);
                p.spine(-10 * air, 0, 0);
                sf.xRot -= 0.3f * air;
                sb.xRot += 0.5f * air + Mth.sin(c.age * 0.4f) * 0.1f * air;
                p.crouch(land, 6);
                p.armR(-60 * land, 0, 0, 0);
            }
            default -> {
            }
        }
    }
}
