package com.lastbastion.client.model;

import com.lastbastion.client.model.ModelSpec.Part;

/**
 * Humanoid skeleton builder (vanilla proportions by default). Returns named parts so callers can
 * hang armour, weapons and anatomy on them.
 */
public final class Rig {
    public Part body, head, rarm, larm, rleg, lleg;

    private Rig() {
    }

    /**
     * An open-faced shell around a head-sized box (hoods, hair, helmets with an open visor):
     * top, back and side slabs only, so the face underneath stays visible.
     */
    public static Part shell(Part parent, String name, float size, float t, float drop, Paint p) {
        float h = size / 2;
        Part sh = parent.child(name, 0, 0, 0);
        sh.box(-h - t, -size - t, -h - t, size + 2 * t, t, size + 2 * t, p);
        sh.box(-h - t, -size, h, size + 2 * t, size * drop, t, p);
        sh.box(-h - t, -size, -h - t, t, size * drop, size + t, p);
        sh.box(h, -size, -h - t, t, size * drop, size + t, p);
        return sh;
    }

    /** Wrap custom-built parts so shared helpers (rank gear, armour) can decorate them. */
    public static Rig of(Part body, Part head, Part rarm, Part larm) {
        Rig r = new Rig();
        r.body = body;
        r.head = head;
        r.rarm = rarm;
        r.larm = larm;
        return r;
    }

    /**
     * @param s     spec
     * @param scale overall size multiplier (1 = player sized, 32px)
     * @param armW  arm thickness (3 = slim, 4 = normal)
     */
    public static Rig humanoid(ModelSpec s, float scale, int armW, Paint skin, Paint torso, Paint legs, Paint headPaint) {
        Rig r = new Rig();
        float legH = 12 * scale, bodyH = 12 * scale, headS = 8 * scale;
        float bodyW = 8 * scale, bodyD = 4 * scale, limb = 4 * scale, arm = armW * scale;
        float hipY = 24 - legH;
        float neckY = hipY - bodyH;
        r.body = s.root.child("body", 0, neckY, 0).box(-bodyW / 2, 0, -bodyD / 2, bodyW, bodyH, bodyD, torso);
        r.head = s.root.child("head", 0, neckY, 0).box(-headS / 2, -headS, -headS / 2, headS, headS, headS, headPaint);
        r.rarm = s.root.child("rarm", -bodyW / 2 - arm / 2, neckY + 2 * scale, 0).box(-arm / 2, -2 * scale, -limb / 2, arm, bodyH, limb, skin);
        r.larm = s.root.child("larm", bodyW / 2 + arm / 2, neckY + 2 * scale, 0).box(-arm / 2, -2 * scale, -limb / 2, arm, bodyH, limb, skin);
        r.rleg = s.root.child("rleg", -bodyW / 4, hipY, 0).box(-limb / 2, 0, -limb / 2, limb, legH, limb, legs);
        r.lleg = s.root.child("lleg", bodyW / 4, hipY, 0).box(-limb / 2, 0, -limb / 2, limb, legH, limb, legs);
        return r;
    }
}
