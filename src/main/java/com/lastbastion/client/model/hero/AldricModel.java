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
import com.lastbastion.entity.hero.AldricHero;
import com.lastbastion.entity.hero.HeroEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Sir Aldric, Knight of Dawn: gilded white plate over mail, a blue tabard with a golden sun, a great
 * helm with a glowing visor and crimson plume, a crimson cape, the kite shield of the dawn and
 * Dawnbringer, a sword of living light. Lv5 adds a halo and wings of light.
 */
public final class AldricModel implements HeroModels.Def {
    @Override
    public void build(ModelSpec s) {
        Paint plate = p(Mat.PLATE, 0xE6E8EE).gloss(0.6f).trim(0xD8B048, 0.45f);
        Paint plateDark = p(Mat.PLATE, 0xB8BCC6).gloss(0.5f);
        Paint mail = p(Mat.METAL, 0x9A9CA4).pattern(Pattern.CHAINMAIL, 0x6A6C74, 2.2f);
        Paint gold = p(Mat.GOLD, 0xE0B848).accent(0xFFF4C0);
        Paint tabard = p(Mat.CLOTH, 0x2A4A9A).accent(0x1A3070).trim(0xD8B048, 0.4f);
        Paint crimson = p(Mat.CLOTH, 0x8A1A24).accent(0x5A0E16).trim(0xD8B048, 0.45f);
        Paint light = Paint.glowing(0xFFE38A).accent(0xFFFFFF);
        Paint helm = p(Mat.PLATE, 0xE6E8EE).gloss(0.65f).trim(0xD8B048, 0.35f)
                .decal(Face.FRONT, DecalType.VISOR, 0xFFE38A, true, 0.5f, 0.42f, 1f)
                .decal(Face.FRONT, DecalType.GRILLE, 0x1A1A20, false, 0.5f, 0.72f, 0.6f);

        Body.Spec sp = new Body.Spec(p(Mat.SKIN, 0xE8C0A0), plate);
        sp.legs(5.8f, 5.6f, 3.6f);
        sp.foot = 1.3f;
        sp.hipW = 7.4f;
        sp.torso(8.6f, 7, 5, 7, 4.4f, 4.4f);
        sp.neck = 0.6f;
        sp.head(6.8f, 7, 7);
        sp.arms(5.4f, 5.2f, 3.6f);
        sp.hand = 2;
        sp.paints(plate, mail, mail, plate, plateDark, mail, plate, plateDark);
        sp.headPaint(helm);
        Body b = Body.build(s.root, sp);

        // breastplate details, gorget, tabard
        b.chest.box(-4.5f, -7.3f, -2.8f, 9, 3, 1, plate);
        b.chest.box(-2.8f, -6.4f, -2.75f, 5.6f, 6, 0.4f, tabard.copy().decal(Face.FRONT, DecalType.SUN, 0xFFD966, true, 0.5f, 0.45f, 1.1f));
        b.chest.box(-3, -7.6f, -2.6f, 6, 1.2f, 5.2f, gold);
        Part flap = b.pelvis.child("tabard", 0, 0.4f, -2.45f);
        flap.box(-2.6f, 0, -0.25f, 5.2f, 6.5f, 0.5f, tabard.copy().decal(Face.FRONT, DecalType.CROSS, 0xFFD966, false, 0.5f, 0.35f, 0.9f));
        Part flapB = b.pelvis.child("tabard_b", 0, 0.4f, 2.45f);
        flapB.box(-2.6f, 0, -0.25f, 5.2f, 6, 0.5f, tabard);
        Gear.belt(b.pelvis, 7, 4.4f, -1.6f, 1.4f, p(Mat.LEATHER, 0x4A2A16), gold);
        // helm crest and plume
        b.head.box(-0.5f, -8.2f, -3.2f, 1, 1.4f, 6.4f, gold);
        Part plume = b.head.child("plume", 0, -8.4f, 0);
        for (int i = 0; i < 5; i++) plume.box(-0.7f, -1.4f + i * 0.25f, -2.6f + i * 1.4f, 1.4f, 1.8f, 1.6f, p(Mat.FEATHER, i % 2 == 0 ? 0xC02030 : 0xE8E0D8));
        plume.box(-0.6f, -0.6f, 4.2f, 1.2f, 4, 1.2f, p(Mat.FEATHER, 0xC02030));
        Part halo = b.head.sub(0, -10.2f, 1).from(5).rot(-15, 0, 0);
        halo.octoRing(-0.2f, 7, 0.6f, 0.4f, light);
        // pauldrons (Lv3: second tier), knee cops, cape
        Gear.pauldron(b.rArm, -1, 3.6f, plate, gold);
        Gear.pauldron(b.lArm, 1, 3.6f, plate, gold);
        b.rArm.sub(-1, -2.8f, 0).from(3).rot(0, 0, 15).box(-3.4f, -0.8f, -3, 5, 1.2f, 6, gold);
        b.lArm.sub(1, -2.8f, 0).from(3).rot(0, 0, -15).box(-1.6f, -0.8f, -3, 5, 1.2f, 6, gold);
        b.rShin.box(-2.1f, -0.6f, -2.3f, 4.2f, 1.6f, 1.6f, gold);
        b.lShin.box(-2.1f, -0.6f, -2.3f, 4.2f, 1.6f, 1.6f, gold);
        Gear.cape(b.chest, "cape", 8.4f, 7, 5, 16, crimson);
        Part wings = b.chest.sub(0, -5.5f, 3).from(5);
        Gear.wing(wings.sub(-1.5f, 0, 0).rot(0, 25, 0), -1, 9, light);
        Gear.wing(wings.sub(1.5f, 0, 0).rot(0, -25, 0), 1, 9, light);

        // Dawnbringer: blade along the hand's -Z
        Part sword = b.rHand.child("sword", 0, 1.3f, 0);
        sword.box(-0.45f, -0.45f, -1.2f, 0.9f, 0.9f, 3, p(Mat.LEATHER, 0x3A2414).pattern(Pattern.BANDS, 0x6A4A2A, 2f));
        sword.sphere(0, 0, 2.1f, 0.7f, gold);
        Part guard = sword.sub(0, 0, -1.5f);
        guard.box(-2.4f, -0.5f, -0.4f, 4.8f, 1, 0.8f, gold).box(-2.9f, -0.9f, -0.3f, 0.8f, 1.2f, 0.6f, gold).box(2.1f, -0.9f, -0.3f, 0.8f, 1.2f, 0.6f, gold);
        guard.box(-0.6f, -0.6f, -0.7f, 1.2f, 1.2f, 0.5f, p(Mat.CRYSTAL, 0x60A0FF).glow());
        Part blade = sword.child("blade", 0, 0, -1.9f);
        blade.box(-0.65f, -0.2f, -12, 1.3f, 0.4f, 12, light);
        blade.box(-0.25f, -0.25f, -11.6f, 0.5f, 0.5f, 11.6f, Paint.glowing(0xFFFFFF));
        blade.spike(0, 0, -12, 1.3f, 1.8f, 90, 0, 0, light);
        blade.sub(0, 0, 0).from(4).box(-1.1f, -0.4f, -12.5f, 2.2f, 0.8f, 12.6f, Paint.glowing(0xFFC850).accent(0xFFF8D0));
        // kite shield on the outside of the left forearm (outer face = LEFT face)
        Part shield = b.lFore.child("shield", 2.3f, 2.4f, 0);
        Paint shieldP = p(Mat.PLATE, 0xF0F0F4).gloss(0.5f).trim(0xD8B048, 0.7f)
                .decal(Face.LEFT, DecalType.SUN, 0xFFD040, true, 0.5f, 0.42f, 0.9f);
        shield.box(-0.4f, -4, -3.2f, 0.9f, 6.5f, 6.4f, shieldP);
        shield.box(-0.35f, 2.5f, -2.2f, 0.8f, 1.6f, 4.4f, shieldP.plain());
        shield.box(-0.3f, 4.1f, -1.2f, 0.7f, 1.4f, 2.4f, shieldP.plain());
        shield.sub(0, 0, 0).from(3).box(-0.5f, -4.2f, -3.4f, 1.1f, 0.6f, 6.8f, gold);
    }

    @Override
    public void animate(ProcModel<HeroEntity> m, HeroEntity h, HeroModels.Ctx c) {
        Poser p = Poser.of(m);
        HeroModels.humanoid(p, h, c, 0.95f, 0.7f);
        float move = Math.min(1, c.la);
        ModelPart cape = m.p("cape"), cape2 = m.p("cape2"), flap = m.p("tabard"), flapB = m.p("tabard_b"), plume = m.p("plume");
        cape.xRot += 0.06f + move * 0.6f + Mth.sin(c.age * 0.1f) * 0.03f;
        cape2.xRot += move * 0.3f + Mth.sin(c.age * 0.13f + 1) * 0.04f;
        flap.xRot += -Math.max(p.rThigh.xRot, p.lThigh.xRot) * 0.6f;
        flapB.xRot += Math.max(-p.rThigh.xRot, -p.lThigh.xRot) * 0.6f;
        plume.xRot += move * 0.2f;
        // guard stance: shield forward, sword ready
        if (c.combat || c.id != 0) {
            p.armL(-45, -35, 0, 35);
            p.armR(-25, 10, 10, 40);
        }
        switch (c.id) {
            case 1 -> {
                float k = Anim.kf(c.t, 0, 0, 5, 1, 9, 1.05f, 14, 1.1f);
                float back = c.t < 5 ? Anim.smooth(c.t / 5f) : 0;
                p.armR(50 * back - 190 * Anim.smooth(Anim.win(c.t, 5, 4)) + 70 * Anim.win(c.t, 10, 6), 50 * back - 60 * Anim.win(c.t, 5, 4), 0, 30 * back);
                p.spine(0, 30 * back - 40 * Anim.win(c.t, 5, 4) * (1 - Anim.win(c.t, 10, 6)), 0);
                p.lift(0 * k);
            }
            case 2 -> {
                float wind = c.t < 5 ? Anim.smooth(c.t / 5f) : 0;
                float strike = Anim.smooth(Anim.win(c.t, 5, 4)) * (1 - Anim.win(c.t, 11, 6));
                p.armR(-150 * wind + (-150 + 170 * Anim.smooth(Anim.win(c.t, 5, 4))) * (c.t >= 5 ? 1 : 0) * (1 - Anim.win(c.t, 11, 6)),
                        -50 * wind + 80 * strike, 0, 20);
                p.spine(0, -30 * wind + 40 * strike, 0);
            }
            case 3 -> {
                float wind = Anim.hold(c.t, 0, 4, 2) * (c.t < 6 ? 1 : 0);
                float thrust = Anim.hold(c.t, 6, 10, 2);
                p.armR(-60 * wind - 90 * thrust, 20 * wind - 5 * thrust, 0, 100 * wind);
                p.spine(-8 * wind + 18 * thrust, 25 * wind - 10 * thrust, 0);
                p.legL(-30 * thrust, 0, 30 * thrust);
                p.lift(1.2f * thrust);
            }
            case AldricHero.BASH -> {
                float k = Anim.swing(c.t, 5, 2, 7);
                float strike = Math.max(0, -k);
                p.armL(-50 * k - 70 * strike, -60 * strike, 0, -20 * strike);
                p.spine(15 * strike, -30 * strike, 0);
            }
            case AldricHero.DASH -> {
                float run = Anim.hold(c.t, 4, 14, 2);
                p.spine(30 * run, 0, 0);
                p.armR(40 * run, 30 * run, 0, 20);
                float slash = Anim.pulse(c.t, 13, 7);
                p.armR(-140 * slash, -60 * slash, 0, 0);
            }
            case AldricHero.CONSECRATE -> {
                float k = Anim.hold(c.t, 4, 18, 4);
                p.crouch(k, 4);
                p.legR(0, 0, 70 * k);
                p.armR(-70 * k, -20 * k, 0, 50 * k);
                p.armL(-60 * k, 20 * k, 0, 50 * k);
                m.p("sword").xRot += 1.6f * k;
            }
            case AldricHero.AEGIS -> {
                float k = Anim.hold(c.t, 6, 22, 4);
                p.armL(-150 * k, -30 * k, 0, 10);
                p.armR(-160 * k, 15 * k, 0, 10);
                p.spine(-10 * k, 0, 0);
            }
            case HeroEntity.ULT -> {
                float raise = Anim.hold(c.t, 0, 20, 4);
                float spin = Anim.win(c.t, 20, 8);
                p.armR(-175 * raise - 90 * Anim.hold(c.t, 21, 30, 3), 0, 0, 5);
                p.armL(-40 * raise, 0, -30 * raise, 20);
                p.spine(-12 * raise, 0, 0);
                p.pelvis.yRot += spin * Mth.TWO_PI;
                p.crouch(Anim.hold(c.t, 21, 30, 3) * 0.5f, 3);
            }
            default -> {
            }
        }
    }
}
