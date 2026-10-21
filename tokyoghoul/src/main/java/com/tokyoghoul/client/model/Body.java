package com.tokyoghoul.client.model;

import com.tokyoghoul.client.model.ModelSpec.Part;

/**
 * Jointed humanoid skeleton: pelvis -> chest -> neck -> head, chest -> shoulder -> upper arm ->
 * forearm -> hand, pelvis -> thigh -> shin -> foot. Every joint pivots where the real one would, so
 * animation can bend elbows, knees and spine instead of swinging stiff blocks. Proportions are set
 * through the builder; part names are fixed so {@link Poser} can animate any body.
 *
 * <p>Coordinates are vanilla model pixels (Y down, feet at Y=24, facing -Z).
 */
public final class Body {
    public Part pelvis, chest, neck, head;
    public Part rArm, rFore, rHand, lArm, lFore, lHand;
    public Part rThigh, rShin, rFoot, lThigh, lShin, lFoot;
    /** Sizes, in pixels, after scaling. */
    public float legLen, torsoLen, headSize, shoulderW, hipW, armLen, limbW;

    private Body() {
    }

    /** Builder for a humanoid; all sizes in pixels at scale 1 (player-sized defaults). */
    public static final class Spec {
        public float scale = 1f;
        public float thigh = 6, shin = 6, foot = 1.5f;
        public float waist = 5, chestH = 7;
        public float hipW = 7, chestW = 8, chestD = 4.4f, waistW = 7, waistD = 4;
        public float neck = 1, head = 8, headW = -1, headD = -1;
        public float upperArm = 6, foreArm = 6, armW = 3.6f, legW = 3.8f, hand = 2.4f;
        public Paint skin, chestP, waistP, upperArmP, foreArmP, handP, thighP, shinP, footP, headP, neckP;

        public Spec(Paint skin, Paint clothes) {
            this.skin = skin;
            chestP = clothes;
            waistP = clothes;
            upperArmP = clothes;
            foreArmP = skin;
            handP = skin;
            thighP = clothes;
            shinP = clothes;
            footP = clothes;
            headP = skin;
            neckP = skin;
        }

        public Spec scale(float s) {
            scale = s;
            return this;
        }

        public Spec legs(float thigh, float shin, float w) {
            this.thigh = thigh;
            this.shin = shin;
            this.legW = w;
            return this;
        }

        public Spec torso(float chestW, float chestH, float chestD, float waistW, float waist, float waistD) {
            this.chestW = chestW;
            this.chestH = chestH;
            this.chestD = chestD;
            this.waistW = waistW;
            this.waist = waist;
            this.waistD = waistD;
            return this;
        }

        public Spec arms(float upper, float fore, float w) {
            this.upperArm = upper;
            this.foreArm = fore;
            this.armW = w;
            return this;
        }

        public Spec head(float size) {
            this.head = size;
            return this;
        }

        public Spec head(float w, float h, float d) {
            this.head = h;
            this.headW = w;
            this.headD = d;
            return this;
        }

        public Spec paints(Paint chest, Paint waist, Paint upperArm, Paint foreArm, Paint hand, Paint thigh, Paint shin, Paint foot) {
            this.chestP = chest;
            this.waistP = waist;
            this.upperArmP = upperArm;
            this.foreArmP = foreArm;
            this.handP = hand;
            this.thighP = thigh;
            this.shinP = shin;
            this.footP = foot;
            return this;
        }

        public Spec headPaint(Paint p) {
            this.headP = p;
            return this;
        }
    }

    /**
     * Builds the skeleton under {@code parent} (normally the model root) with joints named
     * {@code pelvis, chest, neck, head, r_arm, r_fore, r_hand, l_arm, l_fore, l_hand, r_thigh,
     * r_shin, r_foot, l_thigh, l_shin, l_foot}.
     */
    public static Body build(Part parent, Spec s) {
        Body b = new Body();
        float k = s.scale;
        float thigh = s.thigh * k, shin = s.shin * k, foot = s.foot * k;
        float legW = s.legW * k, hipW = s.hipW * k;
        float waist = s.waist * k, chestH = s.chestH * k;
        float waistW = s.waistW * k, waistD = s.waistD * k, chestW = s.chestW * k, chestD = s.chestD * k;
        float head = s.head * k, headW = (s.headW > 0 ? s.headW : s.head) * k, headD = (s.headD > 0 ? s.headD : s.head) * k;
        float neck = s.neck * k;
        float uArm = s.upperArm * k, fArm = s.foreArm * k, armW = s.armW * k, hand = s.hand * k;
        float hipY = 24 - thigh - shin - foot;
        b.legLen = thigh + shin + foot;
        b.torsoLen = waist + chestH;
        b.headSize = head;
        b.shoulderW = chestW;
        b.hipW = hipW;
        b.armLen = uArm + fArm + hand;
        b.limbW = armW;

        b.pelvis = parent.child("pelvis", 0, hipY, 0);
        b.pelvis.box(-waistW / 2, -waist, -waistD / 2, waistW, waist + 1.2f * k, waistD, s.waistP);
        b.chest = b.pelvis.child("chest", 0, -waist, 0);
        b.chest.box(-chestW / 2, -chestH, -chestD / 2, chestW, chestH, chestD, s.chestP);
        b.neck = b.chest.child("neck", 0, -chestH, 0);
        if (neck > 0.1f) b.neck.box(-headW * 0.22f, -neck, -headD * 0.22f, headW * 0.44f, neck + 0.5f * k, headD * 0.44f, s.neckP);
        b.head = b.neck.child("head", 0, -neck, 0);
        b.head.box(-headW / 2, -head, -headD / 2, headW, head, headD, s.headP);

        float shoulderY = -chestH + armW * 0.45f;
        float sx = chestW / 2 + armW / 2 - 0.2f * k;
        b.rArm = b.chest.child("r_arm", -sx, shoulderY, 0);
        b.rArm.box(-armW / 2, -armW * 0.45f, -armW / 2, armW, uArm + armW * 0.45f, armW, s.upperArmP);
        b.rFore = b.rArm.child("r_fore", 0, uArm, 0);
        b.rFore.box(-armW * 0.47f, 0, -armW * 0.47f, armW * 0.94f, fArm, armW * 0.94f, s.foreArmP);
        b.rHand = b.rFore.child("r_hand", 0, fArm, 0);
        b.rHand.box(-armW * 0.45f, 0, -armW * 0.45f, armW * 0.9f, hand, armW * 0.9f, s.handP);
        b.lArm = b.chest.child("l_arm", sx, shoulderY, 0);
        b.lArm.box(-armW / 2, -armW * 0.45f, -armW / 2, armW, uArm + armW * 0.45f, armW, s.upperArmP);
        b.lFore = b.lArm.child("l_fore", 0, uArm, 0);
        b.lFore.box(-armW * 0.47f, 0, -armW * 0.47f, armW * 0.94f, fArm, armW * 0.94f, s.foreArmP);
        b.lHand = b.lFore.child("l_hand", 0, fArm, 0);
        b.lHand.box(-armW * 0.45f, 0, -armW * 0.45f, armW * 0.9f, hand, armW * 0.9f, s.handP);

        float lx = hipW / 2 - legW / 2;
        b.rThigh = b.pelvis.child("r_thigh", -lx, 0, 0);
        b.rThigh.box(-legW / 2, -0.5f * k, -legW / 2, legW, thigh + 0.5f * k, legW, s.thighP);
        b.rShin = b.rThigh.child("r_shin", 0, thigh, 0);
        b.rShin.box(-legW * 0.46f, 0, -legW * 0.46f, legW * 0.92f, shin, legW * 0.92f, s.shinP);
        b.rFoot = b.rShin.child("r_foot", 0, shin, 0);
        b.rFoot.box(-legW * 0.5f, 0, -legW * 0.75f, legW, foot, legW * 1.25f, s.footP);
        b.lThigh = b.pelvis.child("l_thigh", lx, 0, 0);
        b.lThigh.box(-legW / 2, -0.5f * k, -legW / 2, legW, thigh + 0.5f * k, legW, s.thighP);
        b.lShin = b.lThigh.child("l_shin", 0, thigh, 0);
        b.lShin.box(-legW * 0.46f, 0, -legW * 0.46f, legW * 0.92f, shin, legW * 0.92f, s.shinP);
        b.lFoot = b.lShin.child("l_foot", 0, shin, 0);
        b.lFoot.box(-legW * 0.5f, 0, -legW * 0.75f, legW, foot, legW * 1.25f, s.footP);
        return b;
    }
}
