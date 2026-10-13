package com.lastbastion.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Animation curve helpers shared by all models. Angles are in radians unless noted. */
public final class Anim {
    public static final float D = Mth.DEG_TO_RAD;

    private Anim() {
    }

    /** 0..1 progress of a window [start, start+len] within an animation timeline. */
    public static float win(float t, float start, float len) {
        return Mth.clamp((t - start) / len, 0f, 1f);
    }

    public static float easeOut(float x) {
        return 1f - (1f - x) * (1f - x);
    }

    public static float easeIn(float x) {
        return x * x;
    }

    public static float smooth(float x) {
        return x * x * (3 - 2 * x);
    }

    /**
     * Classic anticipation-strike-recover curve: rises to 1 during wind-up, snaps to -strike at the
     * impact frame and eases back to 0.
     */
    public static float swing(float t, float windup, float strikeLen, float recover) {
        if (t < 0) return 0;
        if (t < windup) return smooth(t / windup);
        if (t < windup + strikeLen) return Mth.lerp(easeOut((t - windup) / strikeLen), 1f, -1f);
        if (t < windup + strikeLen + recover) return Mth.lerp(smooth((t - windup - strikeLen) / recover), -1f, 0f);
        return 0;
    }

    /** A pulse from 0 to 1 and back over [start, start+len]. */
    public static float pulse(float t, float start, float len) {
        float x = win(t, start, len);
        return Mth.sin(x * Mth.PI);
    }

    public static void walk(ModelPart rightLeg, ModelPart leftLeg, float limbSwing, float limbAmount, float amp) {
        rightLeg.xRot += Mth.cos(limbSwing * 0.6662f) * amp * limbAmount;
        leftLeg.xRot += Mth.cos(limbSwing * 0.6662f + Mth.PI) * amp * limbAmount;
    }

    public static void armsSwing(ModelPart rightArm, ModelPart leftArm, float limbSwing, float limbAmount, float amp) {
        rightArm.xRot += Mth.cos(limbSwing * 0.6662f + Mth.PI) * amp * limbAmount;
        leftArm.xRot += Mth.cos(limbSwing * 0.6662f) * amp * limbAmount;
    }

    public static void look(ModelPart head, float yawDeg, float pitchDeg) {
        head.yRot += yawDeg * D;
        head.xRot += pitchDeg * D;
    }

    public static void breathe(ModelPart body, float age, float amp) {
        body.xRot += Mth.sin(age * 0.08f) * amp;
    }
}
