package com.lastbastion.client.model.boss;

import com.lastbastion.client.model.Anim;
import com.lastbastion.client.model.Body;
import com.lastbastion.client.model.BossModels;
import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.model.Poser;
import com.lastbastion.client.model.ProcModel;
import com.lastbastion.entity.boss.GorehoundBoss;
import com.lastbastion.entity.boss.SiegeBoss;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Gorehound the Butcher: a hulking, stitched-together brute with a sagging belly, a blood-soaked
 * leather apron, a burlap sack hood with two glowing eye-holes, one swollen cleaver arm, a chain
 * hook in the other hand and meat hooks rattling on his back. In phase 2 his belly splits open.
 */
public final class GorehoundModel implements BossModels.Def {
    private static final float K = 1.5f;

    @Override
    public float shadow() {
        return 1.6f;
    }

    @Override
    public void build(ModelSpec s) {
        Paint flesh = p(Mat.FLESH, 0xB89484).accent(0x7A4A5A).decal(Face.FRONT, DecalType.STITCH, 0, false, 0.3f, 0.5f, 1.2f)
                .decal(Face.FRONT, DecalType.STITCH, 0, false, 0.72f, 0.4f, 1f).decal(Face.BACK, DecalType.SCAR, 0x6A2A2A, false, 0.5f, 0.5f, 1.2f);
        Paint limb = p(Mat.FLESH, 0xAC8878).accent(0x6A3A4A).decal(Face.RIGHT, DecalType.STITCH, 0, false, 0.5f, 0.5f, 1f)
                .decal(Face.LEFT, DecalType.STITCH, 0, false, 0.5f, 0.5f, 1f);
        Paint apron = p(Mat.LEATHER, 0x6A4A34).accent(0x4A2E1E).grime(0.4f)
                .decal(Face.FRONT, DecalType.BLOOD, 0x7A0A0A, false, 0.4f, 0.35f, 0.9f).decal(Face.FRONT, DecalType.BLOOD, 0x6A0808, false, 0.7f, 0.7f, 0.7f);
        Paint sack = p(Mat.CLOTH, 0xA88A5A).accent(0x7A6038).pattern(Pattern.CLOTH, 0x8A6E44, 2f)
                .decal(Face.FRONT, DecalType.HOLLOW_EYES, 0xFF3020, true, 0.5f, 0.45f, 1.2f).decal(Face.FRONT, DecalType.STITCH, 0, false, 0.5f, 0.8f, 0.8f);
        Paint trousers = p(Mat.CLOTH, 0x4A4038).accent(0x2E2620).grime(0.5f);
        Paint boots = p(Mat.LEATHER, 0x2E2018).accent(0x1A120C);
        Paint rust = p(Mat.METAL, 0x8A7A6A).accent(0x5A4030).grime(0.6f).pattern(Pattern.SPOTS, 0x6A3A1A, 1.5f);
        Paint chain = p(Mat.METAL, 0x6A6C72).pattern(Pattern.CHAINMAIL, 0x9A9CA2, 2f);
        Paint gut = p(Mat.FLESH, 0xA02030).accent(0x5A0A14).pattern(Pattern.VEINS, 0xFF4040, 0.8f);

        Body.Spec sp = new Body.Spec(flesh, flesh).scale(K);
        sp.legs(5, 5, 5);
        sp.foot = 1.4f;
        sp.hipW = 10;
        sp.torso(14, 9, 9, 13, 6, 9);
        sp.neck = 1;
        sp.head(6.5f);
        sp.arms(6.5f, 6.5f, 5.5f);
        sp.hand = 3;
        sp.paints(flesh, flesh, limb, limb, limb, trousers, trousers, boots);
        sp.headPaint(sack);
        Body b = Body.build(s.root, sp);
        b.neck.pz -= 1.5f * K;

        // the belly, the apron, and the split gut of phase 2
        Part belly = b.pelvis.child("belly", 0, -4 * K, -4.5f * K);
        belly.box(-6 * K, -4 * K, -2.5f * K, 12 * K, 8 * K, 3.5f * K, flesh);
        Part apronP = b.chest.child("apron", 0, -6 * K, -5 * K);
        apronP.box(-5.2f * K, 0, -0.4f * K, 10.4f * K, 15 * K, 0.6f * K, apron);
        apronP.box(-4.8f * K, -2.6f * K, -0.3f * K, 9.6f * K, 2.6f * K, 0.5f * K, apron.plain());
        b.chest.box(-7.1f * K, -9.2f * K, -0.3f * K, 14.2f * K, 0.6f * K, 0.6f * K, p(Mat.LEATHER, 0x4A2E1E));
        Part gash = b.pelvis.sub(0, -4 * K, -7.1f * K).from(2);
        gash.box(-3.5f * K, -3 * K, -0.2f * K, 7 * K, 6 * K, 0.8f * K, gut);
        for (int i = 0; i < 4; i++) gash.box(-3 * K + i * 2 * K, -3.2f * K, -0.5f * K, 0.7f * K, 6.4f * K, 0.6f * K, p(Mat.BONE, 0xE8DCC8));
        // hood rope, swollen right arm, back meat hooks
        b.head.box(-3.6f * K, -1.2f * K, -3.6f * K, 7.2f * K, 1 * K, 7.2f * K, p(Mat.CLOTH, 0x6A5030));
        b.rArm.box(-3.4f * K, 0, -3.4f * K, 6.8f * K, 5 * K, 6.8f * K, limb);
        b.rFore.box(-3.2f * K, 0.5f * K, -3.2f * K, 6.4f * K, 4.5f * K, 6.4f * K, limb);
        Part hooks = b.chest.sub(0, -7 * K, 4.6f * K);
        for (int i = -1; i <= 1; i++) {
            Part hk = hooks.sub(i * 3.4f * K, 0, 0).rot(0, 0, i * 12);
            hk.box(-0.3f * K, 0, -0.3f * K, 0.6f * K, 3 * K, 0.6f * K, chain);
            hk.box(-0.4f * K, 3 * K, -0.4f * K, 0.8f * K, 2 * K, 0.8f * K, rust);
            hk.spike(0, 5 * K, 0, 0.8f * K, 1.6f * K, -150, 0, 0, rust);
        }
        chainCoil(b);
        // the cleaver: huge rectangular blade along the hand's -Z, notched and bloodied
        Part cleaver = b.rHand.child("cleaver", 0, 1.6f * K, 0);
        cleaver.box(-0.6f * K, -0.6f * K, -2 * K, 1.2f * K, 1.2f * K, 5 * K, p(Mat.WOOD, 0x4A3020).accent(0x2A1A10));
        cleaver.box(-0.35f * K, -1.2f * K, -12 * K, 0.7f * K, 6.4f * K, 10 * K, rust.copy().decal(Face.LEFT, DecalType.BLOOD, 0x6A0808, false, 0.5f, 0.6f, 1f)
                .decal(Face.RIGHT, DecalType.BLOOD, 0x6A0808, false, 0.4f, 0.4f, 1f));
        cleaver.box(-0.4f * K, 4.8f * K, -12 * K, 0.8f * K, 0.6f * K, 10 * K, p(Mat.METAL, 0xC8C4BC).gloss(0.6f));
        cleaver.box(-0.45f * K, -1.4f * K, -9 * K, 0.9f * K, 1.2f * K, 1.6f * K, p(Mat.METAL, 0x5A4A3A));
        // chain hook in the left hand
        Part hook = b.lHand.child("hook", 0, 2 * K, 0);
        for (int i = 0; i < 4; i++) hook.box(-0.4f * K, i * 1.2f * K, -0.4f * K, 0.8f * K, 1 * K, 0.8f * K, chain);
        Part tip = hook.sub(0, 5 * K, 0);
        tip.box(-0.4f * K, 0, -0.4f * K, 0.8f * K, 2.4f * K, 0.8f * K, rust);
        tip.sub(0, 2.4f * K, 0).rot(-120, 0, 0).taper(0, 0, 0, 0.8f * K, 0.8f * K, 0.2f * K, 0.2f * K, 2.4f * K, 3, rust);
    }

    /** Coiled chain over the shoulder. */
    private static void chainCoil(Body b) {
        Part coil = b.chest.sub(-4 * K, -8 * K, 0).rot(0, 0, 35);
        coil.box(-0.6f * K, -0.6f * K, -4.8f * K, 1.2f * K, 1.2f * K, 9.6f * K, p(Mat.METAL, 0x6A6C72).pattern(Pattern.CHAINMAIL, 0x9A9CA2, 2f));
    }

    @Override
    public void animate(ProcModel<SiegeBoss> m, SiegeBoss b, BossModels.Ctx c) {
        Poser p = Poser.of(m);
        p.idle(c.age, 1.4f);
        float move = Math.min(1, c.la);
        p.walk(c.ls, c.la, 0.8f, 0.5f, 1.4f);
        p.pelvis.zRot += Mth.sin(c.ls * 0.6662f) * 0.09f * move;
        p.look(c.headYaw, c.headPitch);
        p.flinch(c.hurt);
        p.chest.xRot += 0.2f;
        p.head.xRot -= 0.12f;
        ModelPart apron = m.p("apron"), belly = m.p("belly"), cleaver = m.p("cleaver");
        apron.xRot += -Math.max(0, -Math.min(p.rThigh.xRot, p.lThigh.xRot)) * 0.7f - 0.2f;
        float jiggle = Mth.sin(c.age * 0.2f + c.ls * 0.6662f) * 0.04f * (0.3f + move);
        belly.yScale = 1 + jiggle;
        belly.xScale = 1 - jiggle * 0.5f;
        // butcher's carry: cleaver held high, hook swinging
        p.armR(-50, -10, 10, 50);
        p.armL(-15 + Mth.sin(c.age * 0.1f) * 5, 0, -12, 20);
        if (c.phase >= 2) p.chest.y += Mth.sin(c.age * 0.5f) * 0.4f;
        switch (c.id) {
            case SiegeBoss.ANIM_MELEE, SiegeBoss.ANIM_CORE -> chop(p, c.t, 14, 0);
            case GorehoundBoss.CLEAVE -> {
                chop(p, c.t, 12, 1);
                chop(p, c.t - 14, 12, -1);
                float up = c.t >= 28 && c.t < 44 ? Anim.smooth(Anim.win(c.t, 28, 14)) : 0;
                float down = Anim.hold(c.t, 44, 52, 3);
                p.armR(-120 * up - 40 * down, 10 * up, 0, -20 * up);
                p.armL(-80 * up - 30 * down, -10 * up, 0, 0);
                p.spine(-15 * up + 40 * down, 0, 0);
                p.crouch(down * 0.6f, 5);
            }
            case GorehoundBoss.HOOK -> {
                float wind = Anim.hold(c.t, 0, 16, 2) * (c.t < 18 ? 1 : 0);
                float thr = Anim.pulse(c.t, 17, 10);
                p.armL(-30 * wind - 110 * thr, 40 * wind - 20 * thr, -60 * wind, 30 * wind);
                p.spine(0, -25 * wind + 30 * thr, 0);
            }
            case GorehoundBoss.RAMPAGE -> {
                float k = Anim.hold(c.t, 16, 56, 4);
                float lean = c.t < 18 ? Anim.smooth(c.t / 18f) : k;
                p.spine(35 * lean, 0, 0);
                p.head.xRot -= 0.3f * lean;
                p.armR(-10 * lean, 0, 40 * lean, 30);
                p.armL(-10 * lean, 0, -40 * lean, 30);
                if (c.t > 18 && c.t < 56) p.walk(c.age * 1.6f, 1, 1.2f, 0.6f, 1.6f);
            }
            case GorehoundBoss.GUT_SPILL -> {
                float k = Anim.hold(c.t, 6, 30, 4);
                p.spine(30 * k, 0, 0);
                p.head.xRot += 0.4f * k;
                p.armR(-20 * k, 0, 40 * k, 20);
                p.armL(-20 * k, 0, -40 * k, 20);
                belly.z -= 1.5f * k;
            }
            case GorehoundBoss.FRENZY -> {
                float spin = Anim.win(c.t, 16, 42);
                p.pelvis.yRot += spin * Mth.TWO_PI * 4;
                p.armR(-80, 70, 0, 0);
                p.armL(-80, -70, 0, 0);
                p.crouch(0.3f, 3);
            }
            case GorehoundBoss.ROAR, SiegeBoss.ANIM_INTRO -> {
                float k = c.id == SiegeBoss.ANIM_INTRO ? Anim.hold(c.t, 28, 40, 4) : Anim.hold(c.t, 4, 32, 4);
                if (c.id == SiegeBoss.ANIM_INTRO && c.t < 28) {
                    // sharpening the cleaver on the hook
                    float sh = Mth.sin(c.t * 0.9f);
                    p.armR(-70, -40 + sh * 15, 0, 60);
                    p.armL(-60, 40, 0, 50);
                }
                p.armR(-100 * k, 0, 60 * k, 20);
                p.armL(-100 * k, 0, -60 * k, 20);
                p.spine(-20 * k, 0, 0);
                p.head.xRot -= 0.6f * k;
            }
            default -> {
            }
        }
        if (c.staggered) {
            float k = Anim.hold(c.t, 0, 62, 6);
            p.spine(40 * k, 0, 15 * k);
            p.armR(40 * k, 0, 20 * k, 0);
            p.armL(40 * k, 0, -20 * k, 0);
            p.crouch(0.6f * k, 6);
        }
        if (c.death > 0) {
            float k = Anim.smooth(Math.min(1, c.death / 30f));
            p.crouch(k, 14);
            p.spine(70 * Anim.smooth(Math.min(1, c.death / 45f)), 0, 0);
            p.armR(50 * k, 0, 30 * k, 0);
            p.armL(50 * k, 0, -30 * k, 0);
            cleaver.zRot += k * 0.8f;
        }
    }

    /** A sideways cleaver chop: wind up on {@code side}, hack across. */
    private static void chop(Poser p, float t, float windup, int side) {
        if (t < 0 || t > windup + 14) return;
        float wind = t < windup ? Anim.smooth(t / windup) : 0;
        float cut = Anim.hold(t, windup, windup + 6, 3);
        p.armR(-100 * wind - 70 * cut, side * (50 * wind - 80 * cut), 20 * wind, 40 * wind);
        p.spine(10 * cut, side * (35 * wind - 40 * cut), 0);
    }
}
