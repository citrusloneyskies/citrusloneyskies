package com.lastbastion.client.model.boss;

import com.lastbastion.client.model.Anim;
import com.lastbastion.client.model.BossModels;
import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.model.ProcModel;
import com.lastbastion.entity.boss.PlagueMotherBoss;
import com.lastbastion.entity.boss.SiegeBoss;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * The Plague Mother: a gaunt corpse-queen rising from the front of a bloated, vein-lit abdomen
 * studded with pustules and hung with glowing egg sacs, carried on six long spider legs. Long
 * clawed arms, stringy hair, a crown of bone and split mandibles. Phase 2 tears the abdomen open.
 */
public final class PlagueMotherModel implements BossModels.Def {
    @Override
    public float scale() {
        return 1.2f;
    }

    @Override
    public float shadow() {
        return 2.2f;
    }

    @Override
    public void build(ModelSpec s) {
        Paint sac = p(Mat.FLESH, 0x8A9A5A).accent(0x5A6A30).pattern(Pattern.VEINS, 0xB8FF60, 0.6f);
        Paint sacDark = p(Mat.FLESH, 0x6A7A44).accent(0x3E4A24);
        Paint pustule = p(Mat.FLESH, 0xC8E070).accent(0xFFFFA0).glow();
        Paint egg = p(Mat.GLASS, 0xA8F070).accent(0xF0FFD0).glow();
        Paint chitin = p(Mat.CHITIN, 0x3A3428).accent(0x1E1A12).gloss(0.5f);
        Paint corpse = p(Mat.ROT, 0x9AA088).accent(0x5A6448).pattern(Pattern.RIBS, 0x6A705A, 1.2f);
        Paint skinP = p(Mat.ROT, 0xA8AE96).accent(0x5A6448);
        Paint face = p(Mat.ROT, 0xA8AE96).accent(0x5A6448).decal(Face.FRONT, DecalType.EYES, 0x9BFF40, true, 0.5f, 0.42f, 1.1f)
                .decal(Face.FRONT, DecalType.GRIN, 0, false, 0.5f, 0.78f, 1f);
        Paint hair = p(Mat.HAIR, 0x2A2A22).accent(0x1A1A14);
        Paint bone = p(Mat.BONE, 0xE0D8C0).accent(0x9A9078);
        Paint rift = Paint.glowing(0xB8FF60).pattern(Pattern.VEINS, 0xFFFFFF, 1f);

        Part body = s.root.child("body", 0, 6, 6);
        Part abd = body.child("abdomen", 0, 0, 0);
        abd.octo(0, -11, 0, 26, 20, 30, sac);
        abd.octo(0, -14, 2, 18, 3, 22, sacDark);
        abd.sub(0, -1, 15).rot(-70, 0, 0).taper(0, 0, 0, 14, 12, 3, 3, 8, 4, sac);
        int[][] pus = {{-9, -9, -6}, {8, -11, 2}, {-6, -13, 8}, {10, -5, -8}, {-12, -3, 6}, {3, -14, -4}, {12, -8, 10}};
        for (int[] q : pus) abd.sphere(q[0], q[1], q[2], 1.6f, pustule);
        Part eggs = abd.child("eggs", 0, 9, 4);
        eggs.sphere(-5, 1.5f, -2, 2.4f, egg).sphere(4, 2, 2, 2.6f, egg).sphere(0, 2.5f, 7, 2.2f, egg).sphere(-3, 1.8f, 9, 1.8f, egg);
        Part split = abd.child("rift", 0, -15.5f, 2).from(2);
        split.box(-2, -0.5f, -11, 4, 1.4f, 22, rift);
        for (int i = 0; i < 5; i++) split.sphere((i % 2 == 0 ? -1 : 1) * 1.2f, -0.6f, -8 + i * 4, 1.4f, egg);
        // six spider legs
        float[] zs = {-10, -2, 6};
        for (int i = 0; i < 3; i++) {
            for (int side = -1; side <= 1; side += 2) {
                String n = "leg" + i + (side < 0 ? "r" : "l");
                Part up = body.child(n, side * 11, -2, zs[i]).rot(0, side * (i - 1) * -22, side * -40);
                up.box(side < 0 ? -12 : 0, -1.2f, -1.2f, 12, 2.4f, 2.4f, chitin);
                Part lo = up.child(n + "2", side * 12, 0, 0).rot(0, 0, side * 110);
                lo.box(side < 0 ? -27 : 0, -0.9f, -0.9f, 27, 1.8f, 1.8f, chitin);
                lo.spike(side * 27, 0, 0, 1.2f, 2.4f, 0, 0, side * -90, bone);
            }
        }
        // the corpse-queen
        Part waist = body.child("waist", 0, -9, -13).rot(-12, 0, 0);
        waist.box(-4, -6, -3, 8, 7, 6, corpse);
        Part chest = waist.child("chest", 0, -6, 0);
        chest.box(-6, -9, -3.5f, 12, 9, 7, corpse);
        chest.box(-6.5f, -9.5f, -4, 13, 2, 8, p(Mat.CLOTH, 0x3A2A3A).accent(0x24182A));
        Part head = chest.child("head", 0, -9, -0.6f);
        head.box(-3.6f, -7, -3.6f, 7.2f, 7, 7.2f, face);
        for (int i = 0; i < 6; i++) head.box(-3.8f + i * 1.4f, -7.4f, 2.6f, 1, 12, 1.2f, hair);
        head.box(-4, -7.6f, -3.8f, 8, 1.4f, 7.6f, hair);
        head.box(-4.2f, -6, -3, 0.8f, 9, 1, hair).box(3.4f, -6, -3, 0.8f, 9, 1, hair);
        for (int i = 0; i < 5; i++) head.spike(-3 + i * 1.5f, -7.4f, -1, 0.8f, 2.4f + (i == 2 ? 1.4f : 0), -10, 0, (i - 2) * 12, bone);
        for (int side = -1; side <= 1; side += 2) {
            Part mand = head.child(side < 0 ? "mand_r" : "mand_l", side * 2.6f, -1, -3.4f);
            mand.box(side < 0 ? -1.2f : 0.2f, 0, -2.6f, 1, 1, 3, chitin);
            mand.spike(side * 0.7f, 0.5f, -2.6f, 0.9f, 2, 90, 0, side * -30, bone);
        }
        for (int side = -1; side <= 1; side += 2) {
            String n = side < 0 ? "arm_r" : "arm_l";
            Part arm = chest.child(n, side * 7, -7.5f, 0);
            arm.box(-1.3f, -1, -1.3f, 2.6f, 10, 2.6f, skinP);
            Part fore = arm.child(n + "2", 0, 10, 0);
            fore.box(-1.1f, 0, -1.1f, 2.2f, 10, 2.2f, skinP);
            for (int k = 0; k < 3; k++) fore.spike((k - 1) * 0.8f, 10, -0.5f, 0.6f, 3.2f, 160, 0, (k - 1) * 12, bone);
        }
    }

    @Override
    public void animate(ProcModel<SiegeBoss> m, SiegeBoss b, BossModels.Ctx c) {
        ModelPart body = m.p("body"), abd = m.p("abdomen"), waist = m.p("waist"), chest = m.p("chest"), head = m.p("head");
        ModelPart armR = m.p("arm_r"), armL = m.p("arm_l"), armR2 = m.p("arm_r2"), armL2 = m.p("arm_l2");
        ModelPart mandR = m.p("mand_r"), mandL = m.p("mand_l");
        float amt = Math.min(1, c.la);
        // alternating tripod gait
        for (int i = 0; i < 3; i++) {
            for (int side = -1; side <= 1; side += 2) {
                String n = "leg" + i + (side < 0 ? "r" : "l");
                ModelPart up = m.p(n), lo = m.p(n + "2");
                boolean group = (i + (side < 0 ? 0 : 1)) % 2 == 0;
                float ph = c.ls * 0.9f + (group ? 0 : Mth.PI);
                up.yRot += Mth.cos(ph) * 0.35f * amt * side;
                up.zRot += Math.max(0, Mth.sin(ph)) * -0.35f * amt * side;
                lo.zRot += Math.sin(c.age * 0.05f + i) * 0.03f;
            }
        }
        body.y += Mth.sin(c.ls * 1.8f) * 0.6f * amt;
        float breathe = Mth.sin(c.age * 0.07f);
        abd.xScale = abd.zScale = 1 + breathe * 0.03f;
        abd.yScale = 1 + breathe * 0.04f;
        waist.xRot += Mth.sin(c.age * 0.05f) * 0.06f;
        waist.zRot += Mth.sin(c.age * 0.037f) * 0.05f;
        head.yRot += Mth.clamp(c.headYaw, -60, 60) * Anim.D;
        head.xRot += Mth.clamp(c.headPitch, -30, 30) * Anim.D;
        mandR.yRot += 0.15f + Mth.sin(c.age * 0.3f) * 0.08f;
        mandL.yRot -= 0.15f + Mth.sin(c.age * 0.3f) * 0.08f;
        armR.xRot += -0.5f + Mth.sin(c.age * 0.06f) * 0.1f;
        armL.xRot += -0.5f + Mth.cos(c.age * 0.06f) * 0.1f;
        armR.zRot += 0.25f;
        armL.zRot -= 0.25f;
        armR2.xRot -= 0.8f;
        armL2.xRot -= 0.8f;
        if (c.hurt > 0) {
            chest.xRot -= c.hurt * 0.3f;
            abd.yScale += c.hurt * 0.06f;
        }
        switch (c.id) {
            case SiegeBoss.ANIM_MELEE, SiegeBoss.ANIM_CORE -> {
                float k = Anim.swing(c.t, 14, 3, 8);
                armR.xRot += -1.6f * Math.max(0, k) + 0.4f * Math.max(0, -k);
                armL.xRot += -1.6f * Math.max(0, k) + 0.4f * Math.max(0, -k);
                waist.xRot += 0.5f * Math.max(0, -k);
            }
            case PlagueMotherBoss.SPEW -> {
                float k = Anim.hold(c.t, 8, 54, 5);
                waist.xRot += 0.45f * k;
                head.xRot += 0.2f * k;
                mandR.yRot += 0.7f * k;
                mandL.yRot -= 0.7f * k;
                float sweep = c.t > 12 && c.t < 52 ? Mth.sin((c.t - 12) / 40f * Mth.TWO_PI) * 0.45f : 0;
                waist.yRot += sweep * k;
            }
            case PlagueMotherBoss.BROOD -> {
                float k = Anim.pulse(c.t, 0, 30);
                abd.xScale += 0.12f * k;
                abd.yScale -= 0.1f * k;
                abd.zScale += 0.1f * k;
                waist.xRot -= 0.4f * k;
                armR.zRot += 0.8f * k;
                armL.zRot -= 0.8f * k;
            }
            case PlagueMotherBoss.RAIN -> {
                float k = Anim.hold(c.t, 2, 24, 4);
                armR.xRot -= 1.8f * k;
                armL.xRot -= 1.8f * k;
                waist.xRot -= 0.35f * k;
                head.xRot -= 0.5f * k;
            }
            case PlagueMotherBoss.LEAP -> {
                float crouch = Anim.hold(c.t, 0, 11, 2) * (c.t < 13 ? 1 : 0);
                float air = Anim.hold(c.t, 13, 40, 3);
                body.y += crouch * 5 - air * 2;
                for (int i = 0; i < 3; i++) {
                    m.p("leg" + i + "r").zRot += (crouch * 0.5f - air * 0.4f);
                    m.p("leg" + i + "l").zRot -= (crouch * 0.5f - air * 0.4f);
                }
                armR.xRot -= 1.2f * air;
                armL.xRot -= 1.2f * air;
            }
            case PlagueMotherBoss.BURST -> {
                float swell = Anim.hold(c.t, 0, 18, 2) * (c.t < 20 ? 1 : 0);
                float pop = Anim.pulse(c.t, 20, 10);
                abd.xScale += 0.2f * swell - 0.1f * pop;
                abd.yScale += 0.25f * swell - 0.12f * pop;
                abd.zScale += 0.2f * swell - 0.1f * pop;
                waist.xRot -= 0.3f * swell;
            }
            case SiegeBoss.ANIM_INTRO -> {
                float rise = Anim.easeOut(Anim.win(c.t, 0, 30));
                body.y += (1 - rise) * 8;
                waist.xRot += (1 - rise) * 0.9f;
                float shriek = Anim.hold(c.t, 26, 40, 3);
                head.xRot -= 0.6f * shriek;
                mandR.yRot += 0.8f * shriek;
                mandL.yRot -= 0.8f * shriek;
                armR.zRot += 0.9f * shriek;
                armL.zRot -= 0.9f * shriek;
            }
            default -> {
            }
        }
        if (c.staggered) {
            float k = Anim.hold(c.t, 0, 62, 6);
            body.y += 4 * k;
            waist.xRot += 0.8f * k;
            head.xRot += 0.4f * k;
        }
        if (c.death > 0) {
            float k = Anim.smooth(Math.min(1, c.death / 35f));
            body.y += 9 * k;
            waist.xRot += 1.1f * k;
            for (int i = 0; i < 3; i++) {
                m.p("leg" + i + "r").zRot += 0.7f * k;
                m.p("leg" + i + "l").zRot -= 0.7f * k;
            }
            abd.yScale -= 0.2f * k;
        }
    }
}
