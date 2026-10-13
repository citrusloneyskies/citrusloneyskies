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
import com.lastbastion.entity.hero.BastionHero;
import com.lastbastion.entity.hero.HeroEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Bastion, the Stone Colossus: a hunched granite giant with knuckles like boulders, moss on its
 * shoulders and an amber heart glowing through cracks in its chest. Crystals grow on it as it levels;
 * at Lv5 obsidian plates and gold-banded fists.
 */
public final class BastionModel implements HeroModels.Def {
    private static final float K = 1.75f;

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
        Paint granite = p(Mat.STONE, 0x8C8577).accent(0x5A7A3A).grime(0.25f);
        Paint chestP = p(Mat.STONE, 0x8C8577).accent(0x5A7A3A).pattern(Pattern.VEINS, 0xFFB040, 0.55f);
        Paint darkStone = p(Mat.STONE, 0x6E685E).accent(0x4A6A2A);
        Paint moss = p(Mat.FUR, 0x4E7A2E).accent(0x2E4A1A);
        Paint amber = p(Mat.CRYSTAL, 0xFFB040).accent(0xFFF0C0).glow();
        Paint obsidian = p(Mat.CRYSTAL, 0x2A2238).accent(0x7A5AA0);
        Paint gold = p(Mat.GOLD, 0xD8B048).accent(0xFFF0B0);
        Paint headP = p(Mat.STONE, 0x7E786C).accent(0x4A6A2A).decal(Face.FRONT, DecalType.EYES, 0xFFB040, true, 0.5f, 0.52f, 1.1f);

        Body.Spec sp = new Body.Spec(granite, chestP).scale(K);
        sp.legs(4, 4.5f, 5.5f);
        sp.foot = 1.2f;
        sp.hipW = 9;
        sp.torso(14, 9, 8, 10, 3.5f, 6.5f);
        sp.neck = 0;
        sp.head(6.5f, 6, 6.5f);
        sp.arms(7, 7.5f, 5);
        sp.hand = 3.5f;
        sp.paints(chestP, granite, granite, darkStone, darkStone, granite, granite, darkStone);
        sp.headPaint(headP);
        Body b = Body.build(s.root, sp);
        b.neck.py += 3.2f * K;
        b.neck.pz -= 1.5f * K;

        // brow ridge and jaw slab
        b.head.box(-3.6f * K, -4.4f * K, -3.6f * K, 7.2f * K, 1.2f * K, 2 * K, darkStone);
        b.head.box(-2.6f * K, -1.2f * K, -3.5f * K, 5.2f * K, 1.4f * K, 1.4f * K, darkStone);
        // amber heart set in the chest, with a stone frame
        Part heart = b.chest.child("heart", 0, -5 * K, -4 * K);
        heart.box(-1.8f * K, -1.8f * K, -0.9f * K, 3.6f * K, 3.6f * K, 1.4f * K, amber.copy().decal(Face.FRONT, DecalType.CORE, 0xFFF4C0, true));
        heart.box(-2.4f * K, -2.4f * K, -0.3f * K, 4.8f * K, 0.6f * K, 0.8f * K, darkStone).box(-2.4f * K, 1.8f * K, -0.3f * K, 4.8f * K, 0.6f * K, 0.8f * K, darkStone);
        // shoulder boulders with moss caps
        for (int side = -1; side <= 1; side += 2) {
            Part arm = side < 0 ? b.rArm : b.lArm;
            Part sh = arm.sub(side * 0.6f * K, -2.6f * K, 0);
            sh.sphere(0, 0, 0, 3.6f * K, darkStone);
            sh.box(-3 * K, -3.9f * K, -3 * K, 6 * K, 1.4f * K, 6 * K, moss);
            Part cr = sh.sub(0, -3 * K, 0).from(3);
            cr.spike(side * 1.4f * K, 0, -0.8f * K, 1.3f * K, 3.2f * K, 10, 0, side * 22, amber).spike(side * 0.2f * K, 0, 1 * K, 1 * K, 2.4f * K, -15, 0, side * 8, amber);
            Part plate = sh.sub(0, 0, 0).from(5);
            plate.box(-3.9f * K, -2.4f * K, -3.9f * K, 7.8f * K, 2 * K, 7.8f * K, obsidian.copy().trim(0xD8B048, 0.5f));
            // fists
            Part hand = side < 0 ? b.rHand : b.lHand;
            hand.box(-2.4f * K, 0.6f * K, -2.3f * K, 4.8f * K, 3.6f * K, 4.6f * K, darkStone);
            for (int k = 0; k < 4; k++) hand.box(-2.1f * K + k * 1.1f * K, 3.8f * K, -2.6f * K, 0.9f * K, 0.8f * K, 1.4f * K, granite);
            Part fore = side < 0 ? b.rFore : b.lFore;
            fore.sub(0, 0, 0).from(5).box(-2.7f * K, 5 * K, -2.7f * K, 5.4f * K, 1.2f * K, 5.4f * K, gold);
            fore.sub(0, 0, 0).from(2).box(-2.6f * K, 1 * K, -2.6f * K, 5.2f * K, 3 * K, 5.2f * K, darkStone.copy().pattern(Pattern.CRACKS, 0x3A3630));
        }
        // back crystals and moss
        b.chest.box(-6 * K, -9.4f * K, -2 * K, 12 * K, 1.4f * K, 6 * K, moss);
        Part backCr = b.chest.sub(0, -6 * K, 4 * K).from(4);
        backCr.spike(-3 * K, 0, 0, 1.6f * K, 4 * K, -50, 0, -20, amber).spike(2.4f * K, -1.5f * K, 0, 1.3f * K, 3.4f * K, -55, 0, 15, amber)
                .spike(0, 2 * K, 0, 1.1f * K, 2.6f * K, -40, 0, 0, amber);
        b.chest.sub(0, 0, 0).from(5).box(-5.4f * K, -8.4f * K, -4.6f * K, 10.8f * K, 2 * K, 1 * K, obsidian.copy().pattern(Pattern.RUNES, 0xFFB040).accentGlow());
        b.pelvis.box(-5.4f * K, -1.6f * K, -3.6f * K, 10.8f * K, 2.4f * K, 7.2f * K, darkStone);
    }

    @Override
    public void animate(ProcModel<HeroEntity> m, HeroEntity h, HeroModels.Ctx c) {
        Poser p = Poser.of(m);
        HeroModels.humanoid(p, h, c, 0.65f, 0.55f);
        float move = Math.min(1, c.la);
        p.pelvis.zRot += Mth.sin(c.ls * 0.6662f) * 0.06f * move;
        p.chest.xRot += 0.18f;
        p.head.xRot -= 0.15f;
        p.armR(0, 0, 8, 12);
        p.armL(0, 0, -8, 12);
        ModelPart heart = m.p("heart");
        float glow = 1 + Mth.sin(c.age * 0.1f) * 0.05f;
        heart.xScale = heart.yScale = heart.zScale = glow;
        switch (c.id) {
            case 1, 2 -> {
                boolean right = c.id == 1;
                float k = Anim.swing(c.t, 9, 3, 8);
                float wind = Math.max(0, k), strike = Math.max(0, -k);
                if (right) p.armR(40 * wind - 110 * strike, -25 * strike, 20 * wind, 60 * wind - 10 * strike);
                else p.armL(40 * wind - 110 * strike, 25 * strike, -20 * wind, 60 * wind - 10 * strike);
                p.spine(8 * strike, (right ? 1 : -1) * (-25 * wind + 30 * strike), 0);
            }
            case 3 -> {
                float k = Anim.swing(c.t, 9, 3, 9);
                float up = Math.max(0, k), down = Math.max(0, -k);
                p.armR(-170 * up - 60 * down, 0, 15 * up, 20 * up);
                p.armL(-170 * up - 60 * down, 0, -15 * up, 20 * up);
                p.spine(-15 * up + 35 * down, 0, 0);
                p.crouch(down, 4);
            }
            case BastionHero.TAUNT -> {
                float beat = Anim.hold(c.t, 0, 14, 2);
                float roar = Anim.hold(c.t, 8, 22, 3);
                p.armR(-60 * beat, -40 * beat, 0, 100 * beat * (0.5f + 0.5f * Mth.sin(c.t * 1.4f)));
                p.armL(-60 * beat, 40 * beat, 0, 100 * beat * (0.5f + 0.5f * Mth.cos(c.t * 1.4f)));
                p.spine(-20 * roar, 0, 0);
                p.head.xRot -= 0.5f * roar;
            }
            case BastionHero.SHATTER -> {
                float k = Anim.swing(c.t, 12, 2, 18);
                float up = Math.max(0, k), down = Math.max(0, -k);
                p.armR(-160 * up - 50 * down, 0, 0, 10);
                p.armL(-160 * up - 50 * down, 0, 0, 10);
                p.spine(-10 * up + 45 * down, 0, 0);
                p.crouch(down, 6);
            }
            case BastionHero.BOULDER -> {
                float grab = Anim.hold(c.t, 0, 8, 3);
                float lift = Anim.hold(c.t, 9, 16, 2);
                float thr = Anim.pulse(c.t, 16, 8);
                p.crouch(grab, 7);
                p.armR(-40 * grab - 170 * lift - 100 * thr, 0, 0, 30 * lift);
                p.armL(-40 * grab - 170 * lift - 100 * thr, 0, 0, 30 * lift);
                p.spine(30 * grab - 15 * lift + 30 * thr, 0, 0);
            }
            case BastionHero.STONESKIN -> {
                float k = Anim.hold(c.t, 0, 16, 4);
                p.crouch(k * 0.6f, 4);
                p.armR(-80 * k, -50 * k, 0, 110 * k);
                p.armL(-80 * k, 50 * k, 0, 110 * k);
            }
            case HeroEntity.ULT -> {
                float crouch = Anim.hold(c.t, 0, 11, 2);
                float air = Anim.hold(c.t, 12, 40, 3) * (c.t < 60 ? 1 : 0);
                float slam = Anim.hold(c.t, 58, 90, 4);
                p.crouch(crouch, 8);
                p.armR(-170 * air - 40 * slam, 0, 20 * air, 10);
                p.armL(-170 * air - 40 * slam, 0, -20 * air, 10);
                p.spine(-20 * air + 40 * slam, 0, 0);
                p.crouch(slam * 0.7f, 6);
            }
            default -> {
            }
        }
        if (h.flag()) {
            float k = 0.97f;
            p.chest.xScale = p.chest.zScale = 1.03f * k + 0.03f;
        }
    }
}
