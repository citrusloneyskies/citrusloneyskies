package com.tokyoghoul.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Animation helper for {@link Body} skeletons: gait cycles with knee and elbow bend, spine
 * counter-rotation, breathing, head look split across neck and head, and arm/leg pose helpers.
 * All methods add to the current pose so they can be layered.
 */
public final class Poser {
    public final ModelPart pelvis, chest, neck, head, rArm, rFore, rHand, lArm, lFore, lHand, rThigh, rShin, rFoot, lThigh, lShin, lFoot;

    private Poser(ProcModel<?> m) {
        pelvis = m.p("pelvis");
        chest = m.p("chest");
        neck = m.p("neck");
        head = m.p("head");
        rArm = m.p("r_arm");
        rFore = m.p("r_fore");
        rHand = m.p("r_hand");
        lArm = m.p("l_arm");
        lFore = m.p("l_fore");
        lHand = m.p("l_hand");
        rThigh = m.p("r_thigh");
        rShin = m.p("r_shin");
        rFoot = m.p("r_foot");
        lThigh = m.p("l_thigh");
        lShin = m.p("l_shin");
        lFoot = m.p("l_foot");
    }

    public static Poser of(ProcModel<?> m) {
        Object c = m.poserCache;
        if (c instanceof Poser p) return p;
        Poser p = new Poser(m);
        m.poserCache = p;
        return p;
    }

    /** Idle breathing and weight shift. */
    public Poser idle(float age, float amp) {
        float b = Mth.sin(age * 0.07f);
        chest.xRot += b * 0.025f * amp;
        chest.y += b * 0.15f * amp;
        rArm.zRot += (0.05f + b * 0.02f) * amp;
        lArm.zRot -= (0.05f + b * 0.02f) * amp;
        rFore.xRot -= 0.08f * amp;
        lFore.xRot -= 0.08f * amp;
        head.xRot += Mth.sin(age * 0.05f + 1) * 0.02f * amp;
        return this;
    }

    /**
     * Gait cycle. {@code stride} scales leg swing (1 = normal walk), {@code armSwing} the arm swing
     * (0 for arms busy holding something), {@code bounce} the vertical bob.
     */
    public Poser walk(float limbSwing, float limbAmount, float stride, float armSwing, float bounce) {
        float amt = Math.min(1f, limbAmount);
        if (amt < 0.01f) return this;
        float ph = limbSwing * 0.6662f;
        float c = Mth.cos(ph), s = Mth.sin(ph);
        float leg = 0.85f * stride * amt;
        rThigh.xRot += c * leg;
        lThigh.xRot -= c * leg;
        rShin.xRot += Math.max(0, s) * 1.1f * stride * amt;
        lShin.xRot += Math.max(0, -s) * 1.1f * stride * amt;
        rFoot.xRot -= Math.max(0, -c) * 0.3f * amt;
        lFoot.xRot -= Math.max(0, c) * 0.3f * amt;
        pelvis.y -= Math.abs(s) * 0.7f * bounce * amt;
        pelvis.yRot += c * 0.08f * amt;
        chest.yRot -= c * 0.16f * amt;
        chest.xRot += 0.05f * stride * amt;
        float arm = 0.7f * armSwing * amt;
        rArm.xRot -= c * arm;
        lArm.xRot += c * arm;
        rFore.xRot -= (0.2f + Math.max(0, -c) * 0.5f) * armSwing * amt;
        lFore.xRot -= (0.2f + Math.max(0, c) * 0.5f) * armSwing * amt;
        return this;
    }

    /** Head look split 40/60 between neck and head. Degrees in, as given by vanilla. */
    public Poser look(float yawDeg, float pitchDeg) {
        float y = Mth.clamp(yawDeg, -70, 70) * Mth.DEG_TO_RAD, p = Mth.clamp(pitchDeg, -45, 45) * Mth.DEG_TO_RAD;
        neck.yRot += y * 0.4f;
        head.yRot += y * 0.6f;
        neck.xRot += p * 0.35f;
        head.xRot += p * 0.65f;
        return this;
    }

    /** Lower the hips by bending the knees ({@code amt} 0..1). */
    public Poser crouch(float amt, float depth) {
        if (amt <= 0) return this;
        pelvis.y += depth * amt;
        rThigh.xRot -= 0.9f * amt;
        lThigh.xRot -= 0.9f * amt;
        rShin.xRot += 1.6f * amt;
        lShin.xRot += 1.6f * amt;
        rFoot.xRot -= 0.7f * amt;
        lFoot.xRot -= 0.7f * amt;
        chest.xRot += 0.25f * amt;
        return this;
    }

    /** Wide fighting stance: feet apart, slight crouch. */
    public Poser stance(float amt) {
        rThigh.zRot += 0.18f * amt;
        lThigh.zRot -= 0.18f * amt;
        rThigh.xRot -= 0.25f * amt;
        lThigh.xRot += 0.15f * amt;
        rShin.xRot += 0.35f * amt;
        lShin.xRot += 0.1f * amt;
        pelvis.yRot += 0.25f * amt;
        chest.yRot -= 0.2f * amt;
        return this;
    }

    /** Right arm pose (degrees): shoulder pitch/yaw/roll and elbow bend (positive = flex). */
    public Poser armR(float pitch, float yaw, float roll, float elbow) {
        rArm.xRot += pitch * Mth.DEG_TO_RAD;
        rArm.yRot += yaw * Mth.DEG_TO_RAD;
        rArm.zRot += roll * Mth.DEG_TO_RAD;
        rFore.xRot -= elbow * Mth.DEG_TO_RAD;
        return this;
    }

    public Poser armL(float pitch, float yaw, float roll, float elbow) {
        lArm.xRot += pitch * Mth.DEG_TO_RAD;
        lArm.yRot += yaw * Mth.DEG_TO_RAD;
        lArm.zRot += roll * Mth.DEG_TO_RAD;
        lFore.xRot -= elbow * Mth.DEG_TO_RAD;
        return this;
    }

    public Poser legR(float pitch, float roll, float knee) {
        rThigh.xRot += pitch * Mth.DEG_TO_RAD;
        rThigh.zRot += roll * Mth.DEG_TO_RAD;
        rShin.xRot += knee * Mth.DEG_TO_RAD;
        return this;
    }

    public Poser legL(float pitch, float roll, float knee) {
        lThigh.xRot += pitch * Mth.DEG_TO_RAD;
        lThigh.zRot += roll * Mth.DEG_TO_RAD;
        lShin.xRot += knee * Mth.DEG_TO_RAD;
        return this;
    }

    /** Spine: bend (pitch), twist (yaw) and side lean (roll), degrees, split across pelvis/chest. */
    public Poser spine(float pitch, float yaw, float roll) {
        chest.xRot += pitch * 0.7f * Mth.DEG_TO_RAD;
        pelvis.xRot += pitch * 0.3f * Mth.DEG_TO_RAD;
        chest.yRot += yaw * 0.65f * Mth.DEG_TO_RAD;
        pelvis.yRot += yaw * 0.35f * Mth.DEG_TO_RAD;
        chest.zRot += roll * Mth.DEG_TO_RAD;
        return this;
    }

    /** Raise / lower the whole body (pixels, negative = up). */
    public Poser lift(float y) {
        pelvis.y += y;
        return this;
    }

    /** Hurt flinch: brief recoil when damaged. */
    public Poser flinch(float hurt) {
        if (hurt <= 0) return this;
        chest.xRot -= hurt * 0.25f;
        head.xRot -= hurt * 0.2f;
        rArm.zRot += hurt * 0.2f;
        lArm.zRot -= hurt * 0.2f;
        return this;
    }
}
