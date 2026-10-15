package com.lastbastion.client.model;

import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.model.hero.DevilGear;
import com.lastbastion.client.model.hero.Gear;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Geometry for the Devils' lesser demons (kept free of client rendering classes). Animation lives in
 * {@link HellspawnModels}.
 */
public final class HellspawnShapes {
    public static final int IMP_TAIL = 4, HOUND_TAIL = 4;

    private HellspawnShapes() {
    }

    /** A cackling imp: big ears and grin, little horns, bat wings, a spade tail and a hellfire orb in one claw. */
    public static void imp(ModelSpec s) {
        float k = 0.62f;
        Paint skin = p(Mat.SKIN, 0x7A1A14).pattern(Pattern.VEINS, 0xFF6A1A, 0.8f).accentGlow();
        Paint dark = p(Mat.SKIN, 0x4A0E0A).accent(0x2A0606);
        Paint face = p(Mat.SKIN, 0x8A2018)
                .decal(Face.FRONT, DecalType.SLIT_EYES, 0xFFD040, true, 0.5f, 0.42f, 1.2f)
                .decal(Face.FRONT, DecalType.GRIN, 0x1A0404, false, 0.5f, 0.76f, 1f);
        Paint horn = p(Mat.BONE, 0x2A1A14).accent(0x120A08);
        Paint claw = p(Mat.BONE, 0x1A1010);
        Paint membrane = p(Mat.SKIN, 0x5A0A08).accent(0x2A0404).pattern(Pattern.VEINS, 0xC0301A, 1.2f);
        Paint orb = Paint.glowing(0xFF7A1A).accent(0xFFE0A0);
        Body.Spec sp = new Body.Spec(skin, dark).scale(k);
        sp.legs(5, 5, 2.6f);
        sp.foot = 1.6f;
        sp.hipW = 5;
        sp.torso(6, 5, 3.6f, 5.2f, 3.5f, 3.4f);
        sp.neck = 0.6f;
        sp.head(7, 6.4f, 6.4f);
        sp.arms(5, 5, 2.2f);
        sp.hand = 2;
        sp.paints(skin, skin, skin, skin, dark, dark, skin, dark);
        sp.headPaint(face);
        sp.neckP = skin;
        Body b = Body.build(s.root, sp);
        b.pelvis.box(-2.9f * k, -3.4f * k, -2.4f * k, 5.8f * k, 3.6f * k, 1 * k, skin);
        for (int side = -1; side <= 1; side += 2) {
            Gear.ear(b.head, side, side * 3.4f * k, -4 * k, 0.6f * k, 1.6f * k, 4.6f * k, skin);
            b.head.spike(side * 1.8f * k, -6.3f * k, -0.6f * k, 1.1f * k, 2.4f * k, -15, 0, side * 20, horn);
            Part hand = side < 0 ? b.rHand : b.lHand;
            for (int f = 0; f < 3; f++) hand.spike((f - 1) * 0.6f * k, 1.9f * k, -0.6f * k, 0.4f * k, 1.4f * k, 160, 0, 0, claw);
            Part foot = side < 0 ? b.rFoot : b.lFoot;
            foot.spike(0, 1 * k, -1.9f * k, 0.5f * k, 1.2f * k, -100, 0, 0, claw);
            Part wing = b.chest.child(side < 0 ? "wing_r" : "wing_l", side * 1.4f * k, -4.4f * k, 1.9f * k);
            DevilGear.batWing(wing, side, 15 * k, membrane, horn);
        }
        Part orbP = b.rHand.child("orb", 0, 3.2f * k, -1.2f * k);
        orbP.sphere(0, 0, 0, 1.6f * k, orb);
        Part tail = b.pelvis.child("tail0", 0, -0.6f * k, 1.6f * k);
        for (int i = 0; i < IMP_TAIL; i++) {
            float len = 2.6f * k;
            tail.box(-0.4f * k, -0.4f * k, 0, 0.8f * k, 0.8f * k, len, dark);
            if (i < IMP_TAIL - 1) tail = tail.child("tail" + (i + 1), 0, 0, len);
        }
        Part spade = tail.sub(0, 0, 2.6f * k).rot(0, 45, 0);
        spade.box(-1 * k, -0.25f * k, -1 * k, 2 * k, 0.5f * k, 2 * k, horn);
    }

    /** A hellhound: charred hide split by molten cracks, a ridge of flame, a fanged jaw and a burning tail. */
    public static void hellhound(ModelSpec s) {
        Paint hide = p(Mat.SKIN, 0x1A1212).pattern(Pattern.CRACKS, 0xFF5A10, 0.9f).accentGlow();
        Paint hideDark = p(Mat.FUR, 0x120C0C).accent(0x2A1A16);
        Paint bone = p(Mat.BONE, 0x2A2020).accent(0x120C0C);
        Paint tooth = p(Mat.BONE, 0xE8DCC8);
        Paint ember = Paint.glowing(0xFF6A1A).accent(0xFFD27A);
        Paint eye = Paint.glowing(0xFFE070).accent(0xFFFFFF);
        Paint mouth = p(Mat.FLESH, 0x3A0A0A).accent(0x1A0404);

        Part body = s.root.child("body", 0, 11.5f, 0);
        body.box(-3.4f, -3.6f, -2, 6.8f, 6.8f, 10, hide);
        body.box(-4, -4.4f, -8.4f, 8, 8.2f, 7, hide);
        body.box(-3.6f, 3, -7.6f, 7.2f, 1, 5, hideDark);
        for (int i = 0; i < 6; i++) {
            body.spike(0, -4.2f + (i > 2 ? 0.6f : 0), -7 + i * 2.6f, 1.4f - i * 0.1f, 3.4f - i * 0.25f, -30, 0, 0, i % 2 == 0 ? ember : bone);
        }
        Part neck = body.child("neck", 0, -2, -7.6f).rot(-35, 0, 0);
        neck.box(-2.6f, -6, -2.6f, 5.2f, 6.6f, 5.2f, hide);
        Part head = neck.child("head", 0, -6, 0).rot(35, 0, 0);
        head.box(-3, -3.6f, -4.6f, 6, 5, 6, hide);
        head.box(-2, -2.4f, -9.6f, 4, 3, 5.2f, hide);
        head.box(-1.6f, -2.5f, -9.8f, 3.2f, 0.8f, 0.4f, p(Mat.PLAIN, 0x050303));
        for (int side = -1; side <= 1; side += 2) {
            head.center(side * 2.9f, -2.2f, -4, 0.5f, 0.9f, 1.4f, eye);
            head.spike(side * 2.2f, -3.4f, -0.4f, 1.4f, 3.2f, -60, 0, side * 25, bone);
            head.spike(side * 1.8f, 0.4f, -8.6f, 0.6f, 1.4f, 180, 0, 0, tooth);
        }
        Part jaw = head.child("jaw", 0, 0.6f, -4);
        jaw.box(-1.8f, 0, -5.6f, 3.6f, 1.6f, 6, hide);
        jaw.box(-1.6f, -0.2f, -5.4f, 3.2f, 0.2f, 5.4f, mouth);
        for (int side = -1; side <= 1; side += 2) jaw.spike(side * 1.4f, 0, -5, 0.5f, 1.2f, 0, 0, 0, tooth);
        head.center(0, 1, -2, 2.6f, 1.6f, 1, ember);
        String[] legs = {"leg_fr", "leg_fl", "leg_br", "leg_bl"};
        float[][] at = {{-2.6f, 2.4f, -5.6f}, {2.6f, 2.4f, -5.6f}, {-2.4f, 2.4f, 5.6f}, {2.4f, 2.4f, 5.6f}};
        for (int i = 0; i < 4; i++) {
            boolean back = i >= 2;
            Part up = body.child(legs[i], at[i][0], at[i][1], at[i][2]).rot(back ? 18 : 0, 0, 0);
            up.box(-1.5f, -1, -1.5f, 3, 6.4f, 3, hide);
            Part lo = up.child(legs[i] + "_lo", 0, 5.2f, 0).rot(back ? -30 : 0, 0, 0);
            lo.box(-1.1f, 0, -1.1f, 2.2f, 5.4f, 2.2f, hideDark);
            lo.box(-1.4f, 4.6f, -2.2f, 2.8f, 1, 3, hide);
            for (int c = -1; c <= 1; c++) lo.spike(c * 0.8f, 5.4f, -2.1f, 0.4f, 1, -100, 0, 0, bone);
        }
        Part tail = body.child("tail0", 0, -2.6f, 7.8f).rot(-50, 0, 0);
        for (int i = 0; i < HOUND_TAIL; i++) {
            float len = 2.6f;
            tail.box(-0.6f, -len, -0.6f, 1.2f, len + 0.2f, 1.2f, i == HOUND_TAIL - 1 ? ember : hide);
            if (i < HOUND_TAIL - 1) tail = tail.child("tail" + (i + 1), 0, -len, 0).rot(12, 0, 0);
        }
        tail.spike(0, -2.6f, 0, 1.4f, 3, 0, 0, 0, ember);
    }

    /** A hulking fiend: a hunched wall of charred muscle with obsidian plates, ram horns and fists like anvils. */
    public static void fiend(ModelSpec s) {
        float k = 1.25f;
        Paint skin = p(Mat.SKIN, 0x2A1A18).pattern(Pattern.CRACKS, 0xFF5A10, 0.8f).accentGlow();
        Paint plate = p(Mat.DARK_METAL, 0x161214).gloss(0.75f).pattern(Pattern.PLATES, 0x241C20, 1.1f).trim(0xB0301A, 0.3f);
        Paint horn = p(Mat.BONE, 0x3A2E26).accent(0x1A1410).pattern(Pattern.BANDS, 0x2A201A, 1.1f);
        Paint hornTip = Paint.glowing(0xFF5A1A).accent(0xFFC060);
        Paint core = Paint.glowing(0xFF6A1A).accent(0xFFE0A0).pattern(Pattern.VEINS, 0xFFF0C0, 1.2f);
        Paint face = p(Mat.SKIN, 0x2A1A18).decal(Face.FRONT, DecalType.SLIT_EYES, 0xFF7A1A, true, 0.5f, 0.45f, 1.2f)
                .decal(Face.FRONT, DecalType.FANGS, 0xE8DCC8, false, 0.5f, 0.8f, 0.9f);
        Paint loin = p(Mat.LEATHER, 0x2A1810).accent(0x140A06);
        Body.Spec sp = new Body.Spec(skin, plate).scale(k);
        sp.legs(5.5f, 5.5f, 4.6f);
        sp.foot = 1.6f;
        sp.hipW = 8;
        sp.torso(12.5f, 9, 6.8f, 8.4f, 4.5f, 5.6f);
        sp.neck = 0.2f;
        sp.head(5.4f, 5.2f, 5.4f);
        sp.arms(7, 7, 4.8f);
        sp.hand = 3.8f;
        sp.paints(skin, skin, skin, skin, skin, skin, skin, plate);
        sp.headPaint(face);
        sp.neckP = skin;
        Body b = Body.build(s.root, sp);
        b.chest.rot(18, 0, 0);
        b.neck.rot(-14, 0, 0);
        b.chest.box(-1.4f * k, -7 * k, -3.5f * k, 2.8f * k, 5 * k, 0.4f * k, core);
        for (int side = -1; side <= 1; side += 2) {
            DevilGear.ramHorn(b.head, side, side * 2.4f * k, -4.4f * k, 0.2f * k, 2.2f * k, 2.4f * k, 4, 50, horn, hornTip);
            Part pd = Gear.pauldron(side < 0 ? b.rArm : b.lArm, side, 4.8f * k, plate, p(Mat.DARK_METAL, 0x0A0808));
            pd.spike(side * 1.6f * k, -1.8f * k, 0, 1.2f * k, 3.4f * k, 0, 0, side * -25, horn);
            pd.spike(side * 0.4f * k, -1.8f * k, 1.8f * k, 0.9f * k, 2.4f * k, 25, 0, side * -10, horn);
            Part fore = side < 0 ? b.rFore : b.lFore;
            Gear.sleeve(fore, 5 * k, 2 * k, 4.6f * k, 5 * k, 0.1f, plate);
            Part hand = side < 0 ? b.rHand : b.lHand;
            for (int f = 0; f < 3; f++) hand.spike((f - 1) * 1.2f * k, 1.4f * k, -2.2f * k, 0.7f * k, 1.4f * k, -90, 0, 0, horn);
            Part shin = side < 0 ? b.rShin : b.lShin;
            Gear.sleeve(shin, 4.6f * k, 0.4f * k, 4 * k, 4.6f * k, 0.1f, plate);
        }
        b.pelvis.child("loin", 0, 0.2f * k, -2.9f * k).box(-2.6f * k, 0, -0.3f * k, 5.2f * k, 5.4f * k, 0.6f * k, loin);
        Gear.belt(b.pelvis, 8.4f * k, 5.6f * k, -1.4f * k, 1.4f * k, plate, p(Mat.BONE, 0xC8BCA4).decal(Face.FRONT, DecalType.SKULL, 0xFF5A1A, true));
        Part tail = b.pelvis.child("tail0", 0, -0.8f * k, 2.8f * k).rot(60, 0, 0);
        tail.taper(0, 0, 0, 2 * k, 2 * k, 0.8f * k, 0.8f * k, 6 * k, 3, skin);
        tail.spike(0, -6 * k, 0, 0.8f * k, 1.6f * k, 0, 0, 0, hornTip);
    }
}
