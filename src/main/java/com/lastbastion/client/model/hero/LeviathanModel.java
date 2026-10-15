package com.lastbastion.client.model.hero;

import com.lastbastion.client.model.Anim;
import com.lastbastion.client.model.HeroModels;
import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.model.ProcModel;
import com.lastbastion.entity.hero.HeroEntity;
import com.lastbastion.entity.hero.LeviathanDevil;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Leviathan, Serpent of the Abyss: a sea serpent rearing out of its own coils. A climbing spiral of coils on the
 * ground, a long S-curved neck with a pale banded belly, glowing dorsal spines and rows of lateral lights, a huge
 * armoured head with six glowing eyes, swept horns, fanned frills and a hinged, fanged jaw over a glowing gullet, and
 * a tail that curls out across the ground to a paddle fin. Not a humanoid: every pose here is its own.
 */
public final class LeviathanModel implements HeroModels.Def {
    static final int NECK = 6, TAIL = 5, COILS = 20;
    /** Rest S-curve of the neck, degrees per joint (positive leans toward the face). */
    private static final float[] NECK_REST = {-10, 10, 25, 20, -15, -35};
    private static final float[] NECK_W = {10.4f, 9.8f, 9.2f, 8.8f, 8.4f, 8.2f};
    private static final float SEG = 6f;
    private static final float[] TAIL_CURL = {0, 18, 14, 10, -24};
    private static final float[] TAIL_LEN = {7, 6.5f, 6, 5.5f, 5};
    private static final float[] TAIL_W = {10f, 8.4f, 6.8f, 5.2f, 3.8f};

    @Override
    public float shadow() {
        return 1.5f;
    }

    @Override
    public int density() {
        return 3;
    }

    @Override
    public void build(ModelSpec s) {
        Paint scales = p(Mat.SCALES, 0x14424C).accent(0x0C2E36).gloss(0.7f);
        Paint band = p(Mat.SCALES, 0x0C2C34).accent(0x071E24).gloss(0.7f);
        Paint headP = p(Mat.SCALES, 0x123A42).accent(0x061A1E).pattern(Pattern.PLATES, 0x0A262C, 1.4f).gloss(0.8f);
        Paint belly = p(Mat.SCALES, 0x7A9A8C).accent(0x4A6A5E).pattern(Pattern.BANDS, 0x4E6E62, 1.3f);
        Paint lateral = p(Mat.SCALES, 0x0A2226).pattern(Pattern.SPOTS, 0x4AF0FF, 1.4f).accentGlow();
        Paint spine = p(Mat.BONE, 0x1E3C40).accent(0x0C1C1E);
        Paint glow = Paint.glowing(0x4AE8F4).accent(0xE0FFFF);
        Paint eye = Paint.glowing(0x9AFFFF).accent(0xFFFFFF);
        Paint fin = p(Mat.SKIN, 0x12545A).accent(0x0A3036).pattern(Pattern.VEINS, 0x5AF0F8, 1.2f).accentGlow();
        Paint tooth = p(Mat.BONE, 0xE8E2CC).accent(0xA89E84);
        Paint mouth = p(Mat.FLESH, 0x3A0C18).accent(0x1A0408);
        Paint nostril = p(Mat.PLAIN, 0x020808);
        Paint throat = Paint.glowing(0x2FB8C8).accent(0xBFF8FF);
        Paint horn = p(Mat.BONE, 0x1A2A2C).accent(0x4A6A6A).gloss(0.6f);
        Paint tongue = p(Mat.FLESH, 0x1E4A50).accent(0x0E2A2E);

        Part body = s.root.child("body", 0, 24, 0);
        Part coil = body.child("coil", 0, 0, 0);

        // ---- the coils: a spiral climbing 1.75 turns inward, ending at the back where the neck rises
        float step = 630f / (COILS - 1);
        for (int i = 0; i < COILS; i++) {
            float f = i / (COILS - 1f);
            float r = Mth.lerp(f, 16, 7), dia = Mth.lerp(f, 11, 9), yc = Mth.lerp(f, -5.5f, -17);
            float a = 270 - (COILS - 1 - i) * step;
            float arc = r * step * Mth.DEG_TO_RAD, len = arc * 1.3f + 1.5f;
            Part sg = coil.sub(0, 0, 0).rot(0, a, 0);
            sg.octoZ(r, yc, -len / 2, dia, len, i % 4 == 1 ? band : scales);
            sg.box(r + dia / 2 - 0.25f, yc - 0.8f, -len * 0.4f, 0.5f, 1.6f, len * 0.8f, lateral);
            sg.box(r - 0.15f, yc - dia / 2 - 2.2f, -arc / 2, 0.3f, 2.6f, arc, fin);
            if (i % 3 == 2) {
                Part sp = sg.spikePart(r, yc - dia / 2 + 0.4f, 0, 1.5f, 3.6f, -40, 0, 0, spine);
                sp.center(0, -3, 0, 0.5f, 1.2f, 0.5f, glow);
            }
        }

        // ---- the tail: out of the spiral's outer end, curling across the ground to a paddle fin
        Part t = coil.child("tail0", 16, 0, 0).rot(0, 180, 0);
        for (int i = 0; i < TAIL; i++) {
            float len = TAIL_LEN[i], d = TAIL_W[i];
            if (i > 0) t = t.child("tail" + i, 0, 0, -TAIL_LEN[i - 1]).rot(0, TAIL_CURL[i], 0);
            t.octoZ(0, -d / 2, -len - 0.6f, d, len + 1.2f, i % 2 == 1 ? band : scales);
            t.box(-0.15f, -d - 1.6f, -len, 0.3f, 2, len, fin);
        }
        // the paddle: a tall fin of glowing-veined membrane on spined rays
        Part fluke = t.sub(0, 0, -TAIL_LEN[TAIL - 1] + 0.5f);
        float[] fh = {5, 7.5f, 9, 9.5f, 8.5f, 6};
        for (int j = 0; j < fh.length; j++) fluke.box(-0.2f, -fh[j] - 0.5f, -1.2f * (j + 1), 0.4f, fh[j] + 1, 1.25f, fin);
        for (int j = 0; j < 4; j++) {
            fluke.sub(0, -0.5f, -0.5f).rot(-20 - j * 18, 0, 0).box(-0.35f, -9.5f + j * 0.6f, -0.35f, 0.7f, 9.5f - j * 0.6f, 0.7f, spine);
        }
        fluke.center(0, -10, -4.2f, 0.6f, 1, 5, glow);

        // ---- the neck: an S rising from the back of the coils, a fin running down its spine
        Part prev = body, neck0 = null;
        for (int i = 0; i < NECK; i++) {
            float d = NECK_W[i];
            Part seg = i == 0 ? prev.child("neck0", 0, -18.5f, 7.5f) : prev.child("neck" + i, 0, -SEG, 0);
            seg.rot(NECK_REST[i], 0, 0);
            seg.octo(0, -SEG - 1, 0, d, SEG + 2, d, i % 3 == 1 ? band : scales);
            seg.box(-d * 0.32f, -SEG - 0.6f, -d / 2 - 0.35f, d * 0.64f, SEG + 1.2f, 0.7f, belly);
            for (int side = -1; side <= 1; side += 2) {
                seg.box(side * d / 2 - 0.25f, -SEG + 0.8f, -0.9f, 0.5f, SEG - 1.6f, 1.8f, lateral);
            }
            seg.box(-0.15f, -SEG - 0.2f, d / 2 - 0.5f, 0.3f, SEG + 0.4f, 3.4f - i * 0.2f, fin);
            float sl = 5f - i * 0.4f;
            Part sp = seg.spikePart(0, -SEG * 0.8f, d / 2 - 0.4f, 1.7f, sl, -118, 0, 0, spine);
            sp.center(0, -sl * 0.85f, 0, 0.55f, 1.2f, 0.55f, glow);
            if (i == 0) neck0 = seg;
            prev = seg;
        }
        // pectoral fins at the base of the neck
        for (int side = -1; side <= 1; side += 2) {
            Part f = neck0.child(side < 0 ? "fin_r" : "fin_l", side * 4.6f, -3.4f, 0.6f).rot(0, side * 70, 0);
            fan(f, side < 0 ? "fin_r" : "fin_l", new float[]{24, 0, -24}, new float[]{9, 10.5f, 8}, spine, fin, glow);
        }

        // ---- the head: a long armoured wedge, six eyes, swept horns, a crest and fanned frills
        Part head = prev.child("head", 0, -SEG, 0).rot(20, 0, 0);
        wedge(head, 1.5f, 11, 12, 11.2f, 8, 7.4f, 2, false, headP);
        head.box(-5, -6.8f, 1.4f, 10, 5.8f, 1.4f, headP);
        wedge(head, -9.5f, 11.5f, 9.6f, 5.8f, 6.6f, 3.8f, 4, false, headP);
        head.box(-2.4f, -3.4f, -22, 4.8f, 3, 1.1f, headP);
        head.sub(0, -6.6f, -9.5f).rot(12.5f, 0, 0).box(-0.9f, -0.9f, -11.6f, 1.8f, 0.9f, 11.6f, headP);
        wedge(head, -1, 19.8f, 8, 4.6f, 0.3f, 0.3f, 3, true, mouth);
        head.center(0, 0.6f, -1.8f, 5.4f, 2.4f, 1.2f, throat);
        for (int side = -1; side <= 1; side += 2) {
            float x = side;
            // brow ridge over the main eyes, sloping down to a scowl
            Part brow = head.sub(x * 5.3f, -7.6f, -3).rot(8, 0, 0);
            brow.box(-0.95f, -0.9f, -8.4f, 1.9f, 1.9f, 8.4f, headP);
            brow.spike(0, -0.8f, -1.5f, 1.1f, 2.8f, -60, 0, side * 20, spine);
            // three pairs of eyes, the largest first
            head.center(x * 5.85f, -4.9f, -7.6f, 0.8f, 1.9f, 2.8f, eye);
            head.center(x * 5.9f, -6.2f, -4.2f, 0.8f, 1.3f, 2, eye);
            head.center(x * 4.4f, -4.3f, -12.6f, 0.8f, 1.1f, 1.6f, eye);
            head.center(x * 1.5f, -4.1f, -21.2f, 1, 0.5f, 1.2f, nostril);
            // swept horns, curling up at the tips
            Part hn = head.sub(x * 3.8f, -7.2f, 0).rot(-62, 0, side * 18);
            hn.taper(0, 0, 0, 2.8f, 2.8f, 1.6f, 1.6f, 9, 3, horn);
            hn.sub(0, -9, 0).rot(34, 0, 0).taper(0, 0, 0, 1.6f, 1.6f, 0.3f, 0.3f, 6, 3, horn);
            // cheek spines
            head.spike(x * 5.6f, -1.6f, 0.4f, 1.2f, 3.4f, -80, 0, side * 30, spine);
            // upper teeth along the snout edge, a great fang at the front
            for (int j = 0; j < 4; j++) {
                float z = -10.6f - j * 2.2f;
                head.spike(x * (snoutHalf(z) - 0.7f), 0, z, 0.8f, 1.6f, 180, 0, 0, tooth);
            }
            head.spike(x * (snoutHalf(-19.4f) + 0.05f), 0, -19.4f, 1.3f, 4.2f, 180, 0, 0, tooth);
        }
        for (int j = 0; j < 3; j++) {
            Part sp = head.spikePart(0, -7.6f, -5 + j * 3.2f, 1.5f, 3.6f - j * 0.5f, -65, 0, 0, spine);
            sp.center(0, -(3.6f - j * 0.5f) * 0.8f, 0, 0.5f, 1.1f, 0.5f, glow);
        }
        // frills behind the jaw
        for (int side = -1; side <= 1; side += 2) {
            String n = side < 0 ? "frill_r" : "frill_l";
            Part fr = head.child(n, side * 5.8f, -4.4f, 0).rot(0, side * 25, 0);
            fan(fr, n, new float[]{40, 15, -10, -35}, new float[]{12, 13, 11, 8.5f}, spine, fin, glow);
        }

        // ---- the lower jaw: hinged at the back, fangs up, a forked tongue inside
        Part jaw = head.child("jaw", 0, 0, -1);
        wedge(jaw, 0.8f, 19.6f, 8.8f, 5.2f, 3.4f, 2.2f, 4, true, headP);
        jaw.box(-2.2f, 0.2f, -20, 4.4f, 2, 1.3f, headP);
        wedge(jaw, 0, 18.6f, 7.2f, 4.2f, 0.3f, 0.3f, 3, false, mouth);
        for (int side = -1; side <= 1; side += 2) {
            for (int j = 0; j < 4; j++) {
                float z = -9.4f - j * 2.2f;
                jaw.spike(side * (jawHalf(z) - 0.8f), 0, z, 0.7f, 1.4f, 0, 0, 0, tooth);
            }
            jaw.spike(side * 2.1f, 0, -17.4f, 1.1f, 3.2f, 0, 0, 0, tooth);
        }
        Part tg = jaw.child("tongue", 0, -0.5f, -3);
        tg.box(-0.5f, -0.25f, -14, 1, 0.5f, 14, tongue);
        for (int side = -1; side <= 1; side += 2) tg.sub(0, 0, -14).rot(0, side * 22, 0).box(-0.3f, -0.2f, -3, 0.6f, 0.4f, 3, tongue);
    }

    private static float snoutHalf(float z) {
        return Mth.lerp(Mth.clamp((-9.5f - z) / 11.5f, 0, 1), 9.6f, 5.8f) / 2;
    }

    private static float jawHalf(float z) {
        return Mth.lerp(Mth.clamp((0.8f - z) / 19.6f, 0, 1), 8.8f, 5.2f) / 2;
    }

    /**
     * Boxes stepping forward (-Z) from {@code z0} for {@code len}, the width easing w0 -> w1 and the height h0 -> h1,
     * standing on y = 0 ({@code down} false) or hanging from it.
     */
    private static void wedge(Part p, float z0, float len, float w0, float w1, float h0, float h1, int steps, boolean down, Paint paint) {
        float seg = len / steps;
        for (int i = 0; i < steps; i++) {
            float f = (i + 0.5f) / steps;
            float w = Mth.lerp(f, w0, w1), h = Mth.lerp(f, h0, h1);
            p.box(-w / 2, down ? 0 : -h, z0 - (i + 1) * seg, w, h, seg + 0.01f, paint);
        }
    }

    /**
     * A fan of spined membrane rays pointing back (+Z) from the pivot; {@code angles} spread them vertically
     * (degrees, positive = upward). Rays are named {@code name + index} so they can spread when the fan flares.
     */
    private static void fan(Part root, String name, float[] angles, float[] lens, Paint spine, Paint fin, Paint glow) {
        for (int j = 0; j < angles.length; j++) {
            float len = lens[j];
            Part ray = root.child(name + j, 0, 0, 0).rot(angles[j], 0, 0);
            ray.box(-0.35f, -0.35f, 0, 0.7f, 0.7f, len, spine);
            ray.center(0, 0, len, 0.9f, 0.9f, 1.2f, glow);
            if (j < angles.length - 1) ray.box(-0.15f, 0, 0.5f, 0.3f, len * 0.42f, len * 0.85f, fin);
        }
    }

    // ------------------------------------------------------------------ animation

    @Override
    public void animate(ProcModel<HeroEntity> m, HeroEntity h, HeroModels.Ctx c) {
        ModelPart body = m.p("body"), coil = m.p("coil"), head = m.p("head"), jaw = m.p("jaw"), tongue = m.p("tongue");
        ModelPart[] neck = new ModelPart[NECK];
        for (int i = 0; i < NECK; i++) neck[i] = m.p("neck" + i);
        ModelPart[] tail = new ModelPart[TAIL];
        for (int i = 0; i < TAIL; i++) tail[i] = m.p("tail" + i);
        float move = Math.min(1, c.la), age = c.age, t = c.t;
        coil.yScale = 1;

        // ---- idle life: a slow serpentine sway through the neck, the tail never quite still
        for (int i = 0; i < NECK; i++) {
            neck[i].xRot += Mth.sin(age * 0.07f - i * 0.6f) * 0.03f;
            neck[i].zRot += Mth.sin(age * 0.045f - i * 0.7f) * (0.05f + 0.03f * move);
        }
        coil.yRot += Mth.sin(c.ls * 0.5f) * 0.12f * move;
        for (int i = 0; i < TAIL; i++) {
            tail[i].yRot += Mth.sin(c.ls * 0.6f - i * 0.9f) * 0.3f * move + Mth.sin(age * 0.05f - i) * 0.08f;
        }
        float yaw = c.headYaw * Mth.DEG_TO_RAD, pitch = c.headPitch * Mth.DEG_TO_RAD;
        neck[3].yRot += yaw * 0.2f;
        neck[4].yRot += yaw * 0.25f;
        neck[5].yRot += yaw * 0.2f;
        head.yRot += yaw * 0.3f;
        head.xRot += pitch * 0.6f;
        head.xRot -= c.hurt * 0.35f;
        neck[5].xRot -= c.hurt * 0.2f;

        float jawOpen = 0.04f + 0.03f * (Mth.sin(age * 0.06f) * 0.5f + 0.5f);
        float flare = c.combat ? 0.45f : 0.1f;
        float cyc = age % 110f;
        if (c.id == 0 && cyc < 14) {
            float k = Anim.pulse(cyc, 0, 14);
            tongue.z -= 7.5f * k;
            jawOpen += 0.12f * k;
        }
        float sink = 0;

        switch (c.id) {
            case HeroEntity.ANIM_ATTACK -> {
                // lunging bite: rear, jaws wide, snap
                float k = Anim.swing(t, 10, 3, 8);
                float back = Math.max(0, k), strike = Math.max(0, -k);
                rear(neck, head, back * 0.45f);
                lunge(neck, head, strike);
                jawOpen += back * 0.85f - strike * 0.05f;
                flare += back * 0.4f;
            }
            case HeroEntity.ANIM_ATTACK2 -> {
                // tail lash: the coils wind up and whip round
                float k = Anim.swing(t, 10, 3, 8);
                float back = Math.max(0, k), strike = Math.max(0, -k);
                coil.yRot += -0.6f * back + 1.4f * strike;
                for (int i = 1; i < TAIL; i++) tail[i].yRot += 0.35f * back - 0.55f * strike;
                for (int i = 2; i < NECK; i++) neck[i].zRot += 0.08f * back - 0.12f * strike;
                jawOpen += 0.3f * strike;
            }
            case LeviathanDevil.TIDE -> {
                float k = Anim.swing(t, 11, 4, 8);
                float back = Math.max(0, k), strike = Math.max(0, -k);
                rear(neck, head, back * 0.7f);
                lunge(neck, head, strike * 0.8f);
                jawOpen += back * 0.6f + strike * 1.0f;
                flare += 0.6f * (back + strike);
            }
            case LeviathanDevil.MAELSTROM -> {
                float k = Anim.hold(t, 3, 18, 4);
                float a = t * 0.45f;
                rear(neck, head, 0.35f * k);
                neck[2].zRot += Mth.sin(a) * 0.25f * k;
                neck[3].xRot += Mth.cos(a) * 0.18f * k;
                neck[4].zRot += Mth.sin(a + 1.2f) * 0.25f * k;
                head.xRot -= 0.3f * k;
                coil.yRot += Mth.sin(a) * 0.2f * k;
                jawOpen += 0.75f * k;
                flare += 0.6f * k;
            }
            case LeviathanDevil.COILS -> {
                float grab = Anim.hold(t, 3, 64, 3);
                float squeeze = t > 6 && t < 66 ? Anim.pulse(t % 10, 0, 10) : 0;
                float fling = Anim.pulse(t, 63, 12);
                lunge(neck, head, 0.65f * grab);
                rear(neck, head, fling * 1.1f);
                for (int i = 2; i < NECK; i++) neck[i].zRot += 0.1f * squeeze * (i % 2 == 0 ? 1 : -1);
                for (int i = 0; i < TAIL; i++) tail[i].yRot += 0.32f * grab + 0.06f * squeeze;
                coil.yScale = 1 - 0.05f * squeeze;
                jawOpen += t < 6 ? 0.9f * Anim.win(t, 0, 4) : 0.08f + 0.9f * fling;
                flare += 0.5f * squeeze + 0.5f * fling;
            }
            case LeviathanDevil.BREATH -> {
                float charge = Anim.hold(t, 0, 9, 4) * (t < 10 ? 1 : 0);
                float beam = Anim.hold(t, 10, 40, 3);
                rear(neck, head, charge * 0.6f);
                lunge(neck, head, beam * 0.5f);
                head.xRot -= beam * 0.15f;
                for (int i = 3; i < NECK; i++) neck[i].xRot += Mth.sin(t * 2.1f + i) * 0.025f * beam;
                jawOpen += charge * 0.7f + beam * 1.05f;
                flare += 0.8f * (charge + beam);
            }
            case LeviathanDevil.BOIL -> {
                float k = Anim.hold(t, 4, 30, 5);
                float pulse = Anim.pulse(t, 9, 5) + Anim.pulse(t, 17, 5) + Anim.pulse(t, 25, 5);
                rear(neck, head, k);
                head.xRot -= 0.35f * k;
                jawOpen += 0.9f * k;
                flare += 0.9f * k;
                coil.yScale = 1 - 0.07f * pulse;
                sink += pulse * 1.2f;
            }
            case LeviathanDevil.DIVE -> {
                if (t < 12) {
                    float k = Anim.smooth(Anim.win(t, 0, 10));
                    lunge(neck, head, 1.3f * k);
                    head.xRot += 0.5f * k;
                    sink += 36 * Anim.easeIn(Anim.win(t, 3, 8));
                } else {
                    float k = Anim.win(t, 24, 9);
                    sink += 40 * (1 - Anim.easeOut(k));
                    rear(neck, head, Anim.pulse(t, 24, 14) * 1.1f);
                    jawOpen += Anim.pulse(t, 24, 14) * 1.0f;
                    flare += Anim.pulse(t, 24, 14);
                }
            }
            case HeroEntity.ULT -> {
                float up = Anim.hold(t, 6, 146, 8);
                float crash = Anim.hold(t, 148, 154, 3);
                float sway = Mth.sin(t * 0.12f);
                rear(neck, head, 1.15f * up);
                head.xRot -= 0.25f * up;
                for (int i = 1; i < NECK; i++) neck[i].zRot += sway * 0.06f * up;
                jawOpen += up * (0.55f + 0.35f * Math.max(0, Mth.sin(t * 0.21f))) + 0.6f * crash;
                flare += 1.1f * up;
                lunge(neck, head, 1.3f * crash);
                coil.yScale = 1 - 0.1f * crash;
                for (int i = 0; i < TAIL; i++) tail[i].yRot += Mth.sin(t * 0.25f - i) * 0.15f * up;
            }
            case HeroEntity.ANIM_ARRIVE -> {
                float k = Anim.win(t, 0, 26);
                sink += 40 * (1 - Anim.easeOut(k));
                float roar = Anim.pulse(t, 14, 22);
                rear(neck, head, roar);
                jawOpen += roar;
                flare += roar;
            }
            case HeroEntity.ANIM_LEVELUP -> {
                float k = Anim.pulse(t, 0, 30);
                rear(neck, head, k);
                jawOpen += 0.8f * k;
                flare += k;
            }
            case HeroEntity.ANIM_DODGE -> {
                float k = Anim.pulse(t, 0, 12);
                for (int i = 1; i < NECK; i++) neck[i].zRot += 0.18f * k;
                coil.yScale = 1 - 0.12f * k;
                sink += 2 * k;
            }
            default -> {
            }
        }

        jaw.xRot += Math.min(0.9f, jawOpen);
        body.y += sink;
        flare(m, "frill_r", -1, 4, flare);
        flare(m, "frill_l", 1, 4, flare);
        float paddle = Mth.sin(age * 0.09f) * 0.12f + (c.combat ? 0.1f : 0);
        flare(m, "fin_r", -1, 3, paddle + 0.15f * move);
        flare(m, "fin_l", 1, 3, paddle + 0.15f * move);
    }

    /** Straighten the S of the neck toward vertical and lift the head: rearing up to strike or roar. */
    private static void rear(ModelPart[] neck, ModelPart head, float k) {
        if (k == 0) return;
        for (int i = 0; i < NECK; i++) neck[i].xRot -= NECK_REST[i] * Mth.DEG_TO_RAD * 0.75f * k;
        neck[0].xRot -= 0.12f * k;
        head.xRot -= 0.3f * k;
    }

    /** Throw the upper neck forward and down, keeping the head aimed ahead: a strike. */
    private static void lunge(ModelPart[] neck, ModelPart head, float k) {
        if (k == 0) return;
        neck[1].xRot += 0.12f * k;
        neck[2].xRot += 0.3f * k;
        neck[3].xRot += 0.28f * k;
        neck[4].xRot += 0.3f * k;
        neck[5].xRot += 0.1f * k;
        head.xRot -= 0.35f * k;
    }

    /** Spread a fan of rays: the fan swings out and its rays splay apart. */
    private static void flare(ProcModel<HeroEntity> m, String name, int side, int rays, float k) {
        m.p(name).yRot += side * 0.5f * k;
        for (int j = 0; j < rays; j++) m.p(name + j).xRot += (rays - 1 - 2 * j) * 0.12f * k;
    }
}
