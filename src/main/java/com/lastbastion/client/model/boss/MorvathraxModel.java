package com.lastbastion.client.model.boss;

import com.lastbastion.client.model.Anim;
import com.lastbastion.client.model.BossModels;
import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.model.ProcModel;
import com.lastbastion.entity.boss.MorvathraxBoss;
import com.lastbastion.entity.boss.SiegeBoss;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Morvathrax the Rotwing: an undead wyvern. A long ribcage of individual ribs over rotting, plague-lit
 * flesh; a four-segment neck with a glowing throat; a horned dragon skull with a hinged, fanged jaw
 * full of plague-fire; a five-segment spiked tail ending in a bone blade; digitigrade taloned hind legs;
 * and vast membrane wings on arm and finger bones that fold into forelegs on the ground. Phase 2 swaps
 * the membranes for torn, tattered ones.
 */
public final class MorvathraxModel implements BossModels.Def {
    private static final String[] SFX = {"_r", "_l"};

    @Override
    public float scale() {
        return 2.4f;
    }

    @Override
    public float shadow() {
        return 3.2f;
    }

    @Override
    public int density() {
        return 4;
    }

    @Override
    public void build(ModelSpec s) {
        int plague = MorvathraxBoss.PLAGUE;
        Paint bone = p(Mat.BONE, 0xD0C8B0).accent(0x7A7058).grime(0.4f);
        Paint boneDark = p(Mat.BONE, 0xA89E86).accent(0x5A5040).grime(0.5f).pattern(Pattern.CRACKS, 0x6A6048, 1.2f);
        Paint rot = p(Mat.ROT, 0x3A3A2A).accent(0x1E1E14).pattern(Pattern.VEINS, plague, 0.8f).accentGlow();
        Paint membrane = p(Mat.LEATHER, 0x4A3A34).accent(0x2A1E1A).pattern(Pattern.VEINS, 0x6A8A4A, 1f).grime(0.5f);
        Paint skull = p(Mat.BONE, 0xD8D0B8).accent(0x7A7058).grime(0.3f);
        Paint glow = Paint.glowing(plague).pattern(Pattern.VEINS, 0xFFFFFF, 0.8f);
        Paint eye = Paint.glowing(plague).accent(0xFFFFFF);
        Paint teeth = p(Mat.BONE, 0xF0E8D0).gloss(0.5f);
        Paint horn = p(Mat.BONE, 0x3A342C).accent(0x1A1610).gloss(0.4f);

        Part body = s.root.child("body", 0, 8, 0);
        body.box(-4.5f, -4, -10, 9, 8, 18, rot);
        body.box(-1.3f, -5.6f, -11, 2.6f, 1.8f, 20, boneDark);
        body.box(-6, -4.5f, -12.5f, 12, 5, 4, bone);
        body.box(-5, -4.6f, 5, 10, 5, 5, bone);
        body.box(-1, 3.6f, -10, 2, 1.4f, 13, bone);
        for (int i = 0; i < 6; i++) {
            float z = -8 + i * 2.4f;
            body.box(-5.4f, -4.4f, z, 0.9f, 8.6f, 1.1f, bone);
            body.box(4.5f, -4.4f, z, 0.9f, 8.6f, 1.1f, bone);
            body.box(-4.6f, 3.4f, z, 9.2f, 0.9f, 1.1f, bone);
        }
        for (int i = 0; i < 8; i++) body.spike(0, -5.4f, -10 + i * 2.5f, 1.4f, 2.4f + (i % 2) * 1.2f, -35, 0, 0, horn);
        for (int i = 0; i < 4; i++) body.box(-3 + i * 1.8f, 4.2f, -6 + i * 3, 1.2f, 2 + (i % 2) * 1.5f, 0.3f, rot);

        // neck: four vertebrae with a glowing plague-fire throat
        Part prev = body;
        for (int i = 1; i <= 4; i++) {
            Part n = i == 1 ? body.child("neck1", 0, -2.5f, -12) .rot(-30, 0, 0) : prev.child("neck" + i, 0, 0, -4).rot(10, 0, 0);
            n.box(-1.8f, -1.8f, -4.3f, 3.6f, 3.6f, 4.6f, boneDark);
            n.box(-1.2f, 1.6f, -4, 2.4f, 0.8f, 4, glow);
            n.spike(0, -1.8f, -2, 0.9f, 2, -40, 0, 0, horn);
            prev = n;
        }
        // skull
        Part head = prev.child("head", 0, 0, -4.2f).rot(10, 0, 0);
        head.box(-3, -3.4f, -5, 6, 5, 6.4f, skull);
        head.box(-2.2f, -2.6f, -12.5f, 4.4f, 3, 7.6f, skull);
        head.box(-2.5f, -3.6f, -6.5f, 5, 1, 2, boneDark);
        head.box(-3.15f, -2.4f, -4.8f, 0.4f, 1.1f, 1.8f, eye);
        head.box(2.75f, -2.4f, -4.8f, 0.4f, 1.1f, 1.8f, eye);
        head.box(-1.5f, -2.8f, -12.6f, 0.8f, 0.6f, 0.6f, eye);
        head.box(0.7f, -2.8f, -12.6f, 0.8f, 0.6f, 0.6f, eye);
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 5; i++) head.spike(side * 1.8f, 0.4f, -12 + i * 1.4f, 0.5f, 1.2f, 180, 0, 0, teeth);
            Part h = head.spikePart(side * 2.3f, -3.2f, 0.5f, 1.8f, 6.5f, -70, 0, side * 20, horn);
            h.spike(0, -6.3f, 0, 1.0f, 4, -40, 0, 0, horn);
            head.spike(side * 2.4f, -1, -6.5f, 0.8f, 2.4f, -80, 0, side * 60, horn);
            head.spike(side * 2.8f, 0.6f, -2, 0.7f, 2, -110, 0, side * 50, horn);
        }
        Part jaw = head.child("jaw", 0, 1.4f, -4.6f);
        jaw.box(-2, 0, -7.6f, 4, 1.4f, 8, skull);
        jaw.box(-1.3f, -0.25f, -7, 2.6f, 0.3f, 6.5f, glow);
        for (int side = -1; side <= 1; side += 2) for (int i = 0; i < 5; i++) jaw.spike(side * 1.6f, 0, -7 + i * 1.4f, 0.45f, 1.0f, 0, 0, 0, teeth);

        // tail with a bone blade
        Part tp = body;
        for (int i = 1; i <= 5; i++) {
            Part t = i == 1 ? body.child("tail1", 0, -2, 9.5f).rot(8, 0, 0) : tp.child("tail" + i, 0, 0, 5.2f).rot(4, 0, 0);
            float w = 3.6f - i * 0.5f;
            t.box(-w / 2, -w / 2, 0, w, w, 5.6f, boneDark);
            t.spike(0, -w / 2, 2.5f, 0.8f, 1.8f - i * 0.2f, 40, 0, 0, horn);
            if (i == 5) {
                t.box(-0.3f, -2.6f, 3.5f, 0.6f, 5.2f, 5, horn);
                t.spike(0, 0, 8.4f, 1.2f, 3, -90, 0, 0, horn);
            }
            tp = t;
        }

        // hind legs
        for (int k = 0; k < 2; k++) {
            int side = k == 0 ? -1 : 1;
            Part leg = body.child("leg" + SFX[k], side * 4.8f, 1.5f, 6).rot(-30, 0, 0);
            leg.box(-2, -1.5f, -2.2f, 4, 8.5f, 4.4f, boneDark);
            Part shin = leg.child("shin" + SFX[k], 0, 7, 0).rot(55, 0, 0);
            shin.box(-1.3f, 0, -1.3f, 2.6f, 7.5f, 2.6f, bone);
            Part foot = shin.child("foot" + SFX[k], 0, 7.5f, 0).rot(-25, 0, 0);
            foot.box(-2, -0.5f, -3, 4, 1.6f, 4.5f, boneDark);
            for (int i = -1; i <= 1; i++) foot.spike(i * 1.3f, 0.5f, -3, 0.8f, 2.2f, 100, 0, 0, horn);
            foot.spike(0, 0.5f, 1.4f, 0.7f, 1.6f, -100, 0, 0, horn);
        }

        // wings: humerus, forearm, three finger bones, membranes (intact in phase 1, torn in phase 2)
        float[] lenA = {9, 11, 8, 10, 7};
        float[] lenB = {12, 14, 11, 13, 10};
        float[] ang = {20, 52, 86};
        float[] fl = {16, 14, 10};
        for (int k = 0; k < 2; k++) {
            int side = k == 0 ? -1 : 1;
            Part w = body.child("wing" + SFX[k], side * 5.6f, -3.6f, -10);
            w.box(side < 0 ? -10 : 0, -0.9f, -0.9f, 10, 1.8f, 1.8f, bone);
            w.spike(side * 1.5f, -0.9f, 0, 1, 2.2f, -20, 0, side * -30, horn);
            Part memA = w.sub(0, 0, 0).levels(1, 1), memB = w.sub(0, 0, 0).from(2);
            for (int j = 0; j < 5; j++) {
                float x = side < 0 ? -2 * (j + 1) : 2 * j;
                memA.box(x, -0.12f, 0.8f, 2.05f, 0.24f, lenA[j], membrane);
                if (j % 2 == 0) memB.box(x, -0.12f, 0.8f, 2.05f, 0.24f, lenA[j] * 0.6f, membrane);
            }
            Part f = w.child("wfore" + SFX[k], side * 10, 0, 0);
            f.box(side < 0 ? -11 : 0, -0.7f, -0.7f, 11, 1.4f, 1.4f, bone);
            f.spike(side * 11, 0, 0, 1.2f, 2.6f, 90, 0, 0, horn);
            Part fmA = f.sub(0, 0, 0).levels(1, 1), fmB = f.sub(0, 0, 0).from(2);
            for (int j = 0; j < 5; j++) {
                float x = side < 0 ? -2.2f * (j + 1) : 2.2f * j;
                fmA.box(x, -0.12f, 0.6f, 2.25f, 0.24f, lenB[j], membrane);
                if (j != 2) fmB.box(x, -0.12f, 0.6f, 2.25f, 0.24f, lenB[j] * (j % 2 == 0 ? 0.5f : 0.75f), membrane);
            }
            for (int i = 0; i < 3; i++) {
                Part fing = f.child("wf" + i + SFX[k], side * 11, 0, 0).rot(0, -side * ang[i], 0);
                fing.box(side < 0 ? -fl[i] : 0, -0.4f, -0.4f, fl[i], 0.8f, 0.8f, bone);
                fing.sub(0, 0, 0).levels(1, 1).box(side < 0 ? -fl[i] * 0.95f : 0, -0.12f, 0.4f, fl[i] * 0.95f, 0.24f, 5.5f, membrane);
                fing.sub(0, 0, 0).from(2).box(side < 0 ? -fl[i] * 0.55f : 0, -0.12f, 0.4f, fl[i] * 0.55f, 0.24f, 3.2f, membrane);
            }
        }
    }

    @Override
    public void animate(ProcModel<SiegeBoss> m, SiegeBoss b, BossModels.Ctx c) {
        float air = b instanceof MorvathraxBoss mb ? mb.air(c.pt) : 0;
        float gnd = 1 - air;
        ModelPart body = m.p("body"), head = m.p("head"), jaw = m.p("jaw");
        ModelPart[] neck = {m.p("neck1"), m.p("neck2"), m.p("neck3"), m.p("neck4")};
        ModelPart[] tail = {m.p("tail1"), m.p("tail2"), m.p("tail3"), m.p("tail4"), m.p("tail5")};
        float move = Math.min(1, c.la);
        float ph = c.ls * 0.6662f;
        // ---------------------------------------------------------- base: blend flight and ground stances
        float flap = Mth.sin(c.age * 0.22f);
        body.y += -flap * 1.2f * air + Mth.sin(c.age * 0.07f) * 0.3f * gnd - Math.abs(Mth.sin(ph)) * 0.8f * move * gnd;
        body.xRot += -0.15f * gnd + 0.05f * air;
        jaw.xRot += 0.08f + Math.max(0, Mth.sin(c.age * 0.05f)) * 0.08f;
        for (int i = 0; i < 4; i++) {
            neck[i].xRot += (0.2f * air) * (i == 0 ? 1 : 0.3f) + Mth.sin(c.age * 0.06f - i * 0.5f) * 0.03f;
            neck[i].yRot += c.headYaw * Mth.DEG_TO_RAD * 0.15f;
        }
        head.yRot += c.headYaw * Mth.DEG_TO_RAD * 0.3f;
        head.xRot += c.headPitch * Mth.DEG_TO_RAD * 0.5f;
        for (int i = 0; i < 5; i++) {
            tail[i].yRot += Mth.sin(c.age * 0.1f - i * 0.6f) * (0.08f + 0.06f * move) + Mth.sin(ph - i * 0.5f) * 0.05f * move;
            tail[i].xRot += -0.06f * air;
        }
        for (int k = 0; k < 2; k++) {
            int side = k == 0 ? -1 : 1;
            ModelPart leg = m.p("leg" + SFX[k]), shin = m.p("shin" + SFX[k]), foot = m.p("foot" + SFX[k]);
            ModelPart wing = m.p("wing" + SFX[k]), fore = m.p("wfore" + SFX[k]);
            // hind legs: digitigrade walk on the ground, tucked back in flight
            float g = Mth.cos(ph + (side < 0 ? 0 : Mth.PI));
            leg.xRot += (g * 0.55f * move) * gnd + 1.1f * air;
            shin.xRot += (Math.max(0, -g) * 0.5f * move) * gnd + 0.7f * air;
            foot.xRot += 0.5f * air;
            // wings: flapping spread in the air, folded into forelegs on the ground
            float up = (flap * 0.65f + 0.1f) * air - 1.0f * gnd;
            wing.zRot += -side * up;
            wing.yRot += side * 0.4f * gnd;
            wing.xRot += Mth.cos(ph + (side < 0 ? Mth.PI : 0)) * 0.4f * move * gnd;
            fore.zRot += -side * (Mth.sin(c.age * 0.22f - 0.7f) * 0.35f * air - 0.6f * gnd);
            for (int i = 0; i < 3; i++) {
                ModelPart fing = m.p("wf" + i + SFX[k]);
                float fold = 2.7f - (new float[]{20, 52, 86}[i]) * Mth.DEG_TO_RAD;
                fing.yRot += -side * fold * gnd;
                fing.zRot += -side * Mth.sin(c.age * 0.22f - 1.1f) * 0.12f * air;
            }
        }
        // ---------------------------------------------------------- moves
        switch (c.id) {
            case SiegeBoss.ANIM_MELEE, SiegeBoss.ANIM_CORE -> {
                float lunge = Anim.pulse(c.t, 6, 14);
                for (ModelPart n : neck) n.xRot += 0.15f * lunge;
                head.xRot += 0.3f * lunge;
                jaw.xRot += 0.7f * Anim.pulse(c.t, 4, 12);
            }
            case MorvathraxBoss.STRAFE, MorvathraxBoss.BREATH -> {
                float k = c.id == MorvathraxBoss.STRAFE ? Anim.hold(c.t, 38, 80, 4) : Anim.hold(c.t, 12, 64, 6);
                for (ModelPart n : neck) n.xRot += 0.12f * k;
                head.xRot += 0.15f * k;
                jaw.xRot += 0.75f * k + Mth.sin(c.age * 1.3f) * 0.05f * k;
                if (c.id == MorvathraxBoss.BREATH && c.t > 18) {
                    float sweep = Mth.sin((c.t - 18) / 44f * Mth.PI) * 50f - 25f;
                    float sw = sweep * Mth.DEG_TO_RAD * k;
                    for (ModelPart n : neck) n.yRot += sw * 0.2f;
                    head.yRot += sw * 0.2f;
                }
                if (c.id == MorvathraxBoss.BREATH) body.xRot -= 0.15f * Anim.hold(c.t, 2, 16, 4) * (c.t < 18 ? 1 : 0);
            }
            case MorvathraxBoss.DIVE -> {
                float k = Anim.hold(c.t, 24, 70, 4) * air;
                body.xRot += 0.6f * k;
                for (int s = 0; s < 2; s++) {
                    int side = s == 0 ? -1 : 1;
                    m.p("wing" + SFX[s]).yRot += side * -0.9f * k;
                    m.p("wing" + SFX[s]).zRot += -side * 0.3f * k;
                    m.p("leg" + SFX[s]).xRot += -1.6f * k;
                }
                jaw.xRot += 0.5f * k;
            }
            case MorvathraxBoss.HAIL -> {
                float k = Anim.hold(c.t, 6, 72, 4);
                body.zRot += Mth.sin(c.t * 1.3f) * 0.07f * k;
                body.xRot += Mth.sin(c.t * 1.1f) * 0.04f * k;
            }
            case MorvathraxBoss.LAND -> {
                float k = Anim.smooth(Anim.win(c.t, 0, 12));
                body.xRot -= 0.5f * k;
                for (int s = 0; s < 2; s++) {
                    int side = s == 0 ? -1 : 1;
                    m.p("wing" + SFX[s]).zRot += -side * (0.9f + Mth.sin(c.t * 0.6f) * 0.4f) * k;
                    m.p("leg" + SFX[s]).xRot -= 1.2f * k;
                }
                jaw.xRot += 0.6f * k;
            }
            case MorvathraxBoss.TAIL -> {
                float spin = Anim.smooth(Anim.win(c.t, 14, 10));
                body.yRot += spin * Mth.TWO_PI;
                float coil = Anim.hold(c.t, 2, 14, 3) * (c.t < 15 ? 1 : 0);
                for (int i = 0; i < 5; i++) tail[i].yRot += 0.25f * coil - 0.1f * Anim.pulse(c.t, 14, 12);
                body.y += 1.5f * coil;
            }
            case MorvathraxBoss.BUFFET, MorvathraxBoss.ROAR, SiegeBoss.ANIM_INTRO -> {
                float rear = c.id == SiegeBoss.ANIM_INTRO ? Anim.hold(c.t, 22, 44, 6) : Anim.hold(c.t, 4, 26, 6);
                float beat = c.id == MorvathraxBoss.BUFFET ? Anim.pulse(c.t, 14, 8) : 0;
                body.xRot -= 0.5f * rear * gnd;
                body.y -= 5 * rear * gnd;
                for (int s = 0; s < 2; s++) {
                    int side = s == 0 ? -1 : 1;
                    m.p("wing" + SFX[s]).zRot += -side * (1.6f * rear * gnd - 1.4f * beat);
                    m.p("wing" + SFX[s]).yRot -= side * 0.4f * rear * gnd;
                    m.p("wfore" + SFX[s]).zRot += -side * 0.6f * rear * gnd;
                    for (int i = 0; i < 3; i++) m.p("wf" + i + SFX[s]).yRot -= -side * (2.7f - new float[]{20, 52, 86}[i] * Mth.DEG_TO_RAD) * rear * gnd;
                }
                if (c.id != MorvathraxBoss.BUFFET) {
                    for (ModelPart n : neck) n.xRot -= 0.18f * rear;
                    head.xRot -= 0.3f * rear;
                    jaw.xRot += 0.9f * rear;
                }
            }
            case MorvathraxBoss.CLAW -> {
                for (int s = 0; s < 2; s++) {
                    int side = s == 0 ? -1 : 1;
                    float sw = Anim.pulse(c.t, s == 0 ? 6 : 20, 12);
                    m.p("wing" + SFX[s]).xRot -= 1.1f * sw;
                    m.p("wing" + SFX[s]).yRot += side * 0.6f * sw;
                    body.yRot += side * -0.15f * sw;
                }
                body.xRot -= 0.2f * Anim.hold(c.t, 2, 34, 4);
                jaw.xRot += 0.4f * Anim.hold(c.t, 4, 32, 4);
            }
            case MorvathraxBoss.POUNCE -> {
                float crouch = Anim.pulse(c.t, 0, 12);
                float fly = Anim.hold(c.t, 10, 30, 3);
                float land = Anim.pulse(c.t, 28, 14);
                body.y += 3 * crouch + 2 * land;
                body.xRot += 0.2f * crouch - 0.3f * fly + 0.3f * land;
                for (int s = 0; s < 2; s++) {
                    int side = s == 0 ? -1 : 1;
                    m.p("wing" + SFX[s]).zRot += -side * (1.4f + Mth.sin(c.t * 0.8f) * 0.5f) * fly;
                    m.p("leg" + SFX[s]).xRot += 0.9f * fly;
                }
                jaw.xRot += 0.6f * (fly + land);
            }
            default -> {
            }
        }
        if (c.staggered) {
            float k = Anim.hold(c.t, 0, 62, 8);
            body.y += 3 * k;
            body.zRot += 0.15f * k;
            for (ModelPart n : neck) n.xRot += 0.2f * k;
            head.zRot += Mth.sin(c.age * 0.3f) * 0.2f * k;
            jaw.xRot += 0.5f * k;
        }
        if (c.death > 0) {
            float k = Anim.smooth(Math.min(1, c.death / 30f));
            body.y += 7 * k;
            body.zRot += 0.35f * k;
            for (ModelPart n : neck) n.xRot += 0.3f * k;
            head.xRot += 0.3f * k;
            jaw.xRot += 0.6f * k;
            for (ModelPart t : tail) t.yRot *= 1 - k;
            for (int s = 0; s < 2; s++) {
                int side = s == 0 ? -1 : 1;
                m.p("wing" + SFX[s]).zRot += -side * 0.9f * k;
            }
        }
    }
}
