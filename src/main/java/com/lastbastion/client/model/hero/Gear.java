package com.lastbastion.client.model.hero;

import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import net.minecraft.util.Mth;

/** Reusable costume pieces for hero (and boss) models: faces, hair, capes, belts, armour plates. */
public final class Gear {
    private Gear() {
    }

    public static Paint p(Mat m, int c) {
        return Paint.of(m, c);
    }

    /** Skin paint with a painted face: eyes (with pupils), brows, nose shading and mouth. */
    public static Paint face(int skin, int eye, boolean glowEyes, int brow, int lips) {
        Paint f = Paint.of(Mat.SKIN, skin);
        f.decal(Face.FRONT, DecalType.BROW, brow, false, 0.5f, 0.47f, 1f);
        f.decal(Face.FRONT, DecalType.EYES, eye, glowEyes, 0.5f, 0.5f, 1f);
        f.decal(Face.FRONT, DecalType.NOSE, 0, false, 0.5f, 0.66f, 1f);
        f.decal(Face.FRONT, DecalType.MOUTH, lips, false, 0.5f, 0.8f, 0.9f);
        return f;
    }

    /**
     * Hair cap over a head box (w x h x d, pivot at the bottom centre): crown, back and sides down to
     * {@code drop} (fraction of the head height), leaving the face clear.
     */
    public static Part hair(Part head, float w, float h, float d, float t, float drop, Paint hair) {
        Part s = head.sub(0, 0, 0);
        float hw = w / 2, hd = d / 2;
        s.box(-hw - t, -h - t, -hd - t, w + 2 * t, t + 0.6f, d + 2 * t, hair);
        s.box(-hw - t, -h, hd - 0.2f, w + 2 * t, h * drop, t + 0.2f, hair);
        s.box(-hw - t, -h, -hd - t, t, h * drop * 0.75f, d + t, hair);
        s.box(hw, -h, -hd - t, t, h * drop * 0.75f, d + t, hair);
        s.box(-hw - t * 0.5f, -h - t * 0.5f, -hd - t, w + t, h * 0.18f, t, hair);
        return s;
    }

    /**
     * Two-segment cape hanging from the back of the chest. Parts are named {@code name} and
     * {@code name + "2"} so the animator can make it flow.
     */
    public static Part cape(Part chest, String name, float w, float chestH, float chestD, float len, Paint cloth) {
        Part c1 = chest.child(name, 0, -chestH + 0.4f, chestD / 2 + 0.15f);
        float l1 = len * 0.55f, l2 = len * 0.45f;
        c1.box(-w / 2, 0, 0, w, l1, 0.6f, cloth);
        Part c2 = c1.child(name + "2", 0, l1, 0);
        c2.box(-w / 2 - 0.2f, 0, 0, w + 0.4f, l2, 0.6f, cloth);
        return c1;
    }

    /** A belt around the pelvis with a buckle on the front. */
    public static void belt(Part pelvis, float waistW, float waistD, float y, float h, Paint belt, Paint buckle) {
        pelvis.box(-waistW / 2 - 0.25f, y, -waistD / 2 - 0.25f, waistW + 0.5f, h, waistD + 0.5f, belt);
        pelvis.box(-0.9f, y - 0.15f, -waistD / 2 - 0.45f, 1.8f, h + 0.3f, 0.4f, buckle);
    }

    /** Layered shoulder guard on an upper arm ({@code side} -1 right, +1 left). */
    public static Part pauldron(Part arm, int side, float armW, Paint plate, Paint trim) {
        Part pd = arm.sub(side * 0.4f, -armW * 0.45f, 0).rot(0, 0, side * -12);
        float w = armW + 2.2f, d = armW + 1.6f;
        pd.box(-w / 2, -1.4f, -d / 2, w, 2.2f, d, plate);
        pd.box(-w / 2 - 0.2f, 0.6f, -d / 2 - 0.2f, w + 0.4f, 1.2f, d + 0.4f, plate);
        pd.box(-w / 2 - 0.25f, -1.5f, -d / 2 - 0.25f, w + 0.5f, 0.4f, d + 0.5f, trim);
        return pd;
    }

    /** Thin armour shell around a limb segment of size w x len x d starting at y0. */
    public static void sleeve(Part limb, float w, float y0, float len, float d, float inflate, Paint p) {
        limb.box(-w / 2, y0, -d / 2, w, len, d, inflate, p);
    }

    /** A pointed ear (or horn stub) on the side of a head. */
    public static void ear(Part head, int side, float x, float y, float z, float base, float len, Paint p) {
        head.spike(x, y, z, base, len, -18, 0, side * 68, p);
    }

    /**
     * A spread wing: an arm bone with a fan of primary feathers longest in the middle.
     * {@code side} -1 extends toward -X (right), +1 toward +X (left). Feathers fan upward.
     */
    public static void wing(Part root, int side, float span, Paint feather) {
        Paint bone = feather.copy().dark(0.92f);
        root.box(side < 0 ? -span * 0.55f : 0, -0.6f, -0.4f, span * 0.55f, 1.2f, 0.8f, bone);
        int n = 6;
        for (int i = 0; i < n; i++) {
            float t = i / (float) (n - 1);
            float len = span * (0.55f + 0.5f * Mth.sin(t * Mth.PI * 0.85f + 0.3f));
            float ang = Mth.lerp(t, -12, 62);
            float x = side * span * 0.55f * t;
            Part f = root.sub(x, 0, 0.05f * i).rot(0, 0, side < 0 ? ang : -ang);
            f.box(side < 0 ? -len : 0, -0.55f, -0.18f, len, 1.1f, 0.36f, feather);
        }
    }

    /** Ring of small leaves/beads around a head (laurels, circlets). */
    public static void circlet(Part head, float w, float d, float y, int n, float size, Paint p) {
        for (int i = 0; i < n; i++) {
            float a = i * Mth.TWO_PI / n;
            float x = Mth.cos(a) * (w / 2 + 0.3f), z = Mth.sin(a) * (d / 2 + 0.3f);
            head.sub(x, y, z).rot(0, -a * Mth.RAD_TO_DEG, 25).box(-size / 2, -size * 0.35f, -size * 0.25f, size, size * 0.7f, size * 0.5f, p);
        }
    }
}
