package com.lastbastion.client.model.hero;

import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import net.minecraft.util.Mth;

/** Shared anatomy for the Devils and their hellspawn: burnt-feather wings, bat wings, curling horns, spiked tails. */
public final class DevilGear {
    private DevilGear() {
    }

    /**
     * Fallen-angel wing: a bone arm and forearm carrying broad, ragged feathers that darken toward glowing
     * ember tips. {@code side} -1 spreads toward -X (the body's right), +1 toward +X. The wing lies in the XY
     * plane behind the body (its flat faces -Z/+Z), fanning downward from the bone.
     */
    public static void burntWing(Part root, int side, float span, Paint feather, Paint bone, Paint tip) {
        float armLen = span * 0.48f, foreLen = span * 0.42f;
        Part arm = root.sub(0, 0, 0).rot(0, 0, side * -18);
        arm.box(side < 0 ? -armLen : 0, -0.8f, -0.6f, armLen, 1.6f, 1.2f, bone);
        Part fore = arm.sub(side * armLen, 0, 0).rot(0, 0, side * -32);
        fore.box(side < 0 ? -foreLen : 0, -0.6f, -0.5f, foreLen, 1.2f, 1.0f, bone);
        fore.spike(side * foreLen, 0, 0, 0.9f, 2.4f, 0, 0, side * 70, tip);
        // coverts: a dark shoulder of small feathers along the bones, so the wing reads as one surface
        arm.box(side < 0 ? -armLen : 0, 0.4f, -0.35f, armLen, span * 0.16f, 0.7f, feather);
        fore.box(side < 0 ? -foreLen * 0.9f : 0, 0.3f, -0.3f, foreLen * 0.9f, span * 0.12f, 0.6f, feather);
        // primaries fan from the forearm, overlapping, ragged in length
        int prim = 8;
        for (int i = 0; i < prim; i++) {
            float t = i / (float) (prim - 1);
            float x = side * foreLen * (0.12f + 0.88f * t);
            float len = span * (0.58f - 0.24f * t) * (i % 3 == 1 ? 0.88f : 1f);
            // feather direction (-sin a, cos a): 12 deg hangs nearly straight down, 80 deg points outward
            float ang = Mth.lerp(t, 12, 80);
            Part f = fore.sub(x, 0.3f, 0.12f + 0.05f * i).rot(0, 0, side < 0 ? ang : -ang);
            f.box(-1.9f, 0, -0.25f, 3.8f, len, 0.5f, feather);
            f.box(-1.4f, len, -0.2f, 2.8f, 1.6f, 0.4f, tip);
        }
        // secondaries hang from the arm
        int sec = 6;
        for (int i = 0; i < sec; i++) {
            float t = i / (float) (sec - 1);
            float x = side * armLen * (0.12f + 0.88f * t);
            float len = span * (0.3f + 0.1f * t) * (i % 2 == 0 ? 1f : 0.9f);
            Part f = arm.sub(x, 0.5f, 0.18f + 0.05f * i).rot(0, 0, side * -(6 - 4 * t));
            f.box(-1.8f, 0, -0.25f, 3.6f, len, 0.5f, feather);
            f.box(-1.3f, len, -0.2f, 2.6f, 1.2f, 0.4f, tip);
        }
    }

    /**
     * Leathery bat wing for imps and fiends: three finger bones with a membrane between them. Same orientation
     * conventions as {@link #burntWing}.
     */
    public static void batWing(Part root, int side, float span, Paint membrane, Paint bone) {
        Part arm = root.sub(0, 0, 0).rot(0, 0, side * -25);
        float armLen = span * 0.45f;
        arm.box(side < 0 ? -armLen : 0, -0.5f, -0.4f, armLen, 1f, 0.8f, bone);
        Part hand = arm.sub(side * armLen, 0, 0);
        // finger directions (-sin a, cos a): outward-up, outward, outward-down
        float[] fingers = {135, 90, 40};
        for (int i = 0; i < fingers.length; i++) {
            float len = span * (0.55f - 0.08f * i);
            Part f = hand.sub(0, 0, 0).rot(0, 0, side < 0 ? fingers[i] : -fingers[i]);
            f.box(-0.3f, 0, -0.3f, 0.6f, len, 0.6f, bone);
        }
        // membrane panels between the fingers, stepped toward the body
        for (int i = 0; i < 4; i++) {
            float t = i / 3f;
            float w = span * (0.55f - 0.12f * i);
            float h = span * (0.42f - 0.06f * i);
            Part m = root.sub(side * span * (0.18f + 0.22f * t), 0.5f, 0.05f).rot(0, 0, side * (10 + 12 * t));
            m.box(side < 0 ? -w * 0.6f : -w * 0.4f, 0, -0.15f, w, h, 0.3f, membrane);
        }
    }

    /**
     * Curling ram horn from a point on the head: {@code segs} tapering segments, each curled back by
     * {@code curl} degrees, flaring outward first.
     */
    public static Part ramHorn(Part head, int side, float x, float y, float z, float base, float segLen, int segs, float curl, Paint horn, Paint tip) {
        Part h = head.sub(x, y, z).rot(-25, side * -15, side * 72);
        float w = base;
        for (int i = 0; i < segs; i++) {
            float w1 = w * 0.8f;
            h.taper(0, 0, 0, w, w, w1, w1, segLen, 2, i == segs - 1 ? tip : horn);
            if (i < segs - 1) h = h.sub(0, -segLen, 0).rot(-curl, 0, side * -8);
            w = w1;
        }
        return h;
    }
}
