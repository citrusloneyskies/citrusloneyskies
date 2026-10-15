package com.lastbastion.client.model.hero;

import com.lastbastion.client.model.Anim;
import com.lastbastion.client.model.Body;
import com.lastbastion.client.model.HeroModels;
import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.model.Poser;
import com.lastbastion.client.model.ProcModel;
import com.lastbastion.entity.hero.HeroEntity;
import com.lastbastion.entity.hero.LuciferDevil;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Lucifer, King of Hell: ash-grey skin split by magma veins, goat legs ending in black hooves, great ram horns
 * under a crown of black-iron thorns, a shattered halo of hellfire hanging behind his head, three pairs of burnt
 * wings with ember-lit edges, a barbed tail, spiked obsidian plate with crimson runes, a tattered crimson tabard
 * and Perdition - a serrated black greatsword with a molten edge and a horned skull for a guard. Lv3 adds more
 * thorns to the crown; Lv5 sets the veins of his whole body burning.
 */
public final class LuciferModel implements HeroModels.Def {
    private static final float K = 1.45f;
    static final int WING_PAIRS = 3, TAIL = 6;

    @Override
    public float shadow() {
        return 1.0f;
    }

    @Override
    public int density() {
        return 3;
    }

    @Override
    public void build(ModelSpec s) {
        Paint skin = p(Mat.SKIN, 0x524646).pattern(Pattern.VEINS, 0xFF4A10, 0.7f).accentGlow();
        Paint skinHot = p(Mat.SKIN, 0x4A3E3E).pattern(Pattern.VEINS, 0xFF7A20, 1.3f).accentGlow();
        Paint fur = p(Mat.FUR, 0x1A1214).accent(0x2E2020);
        Paint hoof = p(Mat.DARK_METAL, 0x0C0A0A).gloss(0.6f);
        Paint obsidian = p(Mat.DARK_METAL, 0x18121A).gloss(0.75f).pattern(Pattern.PLATES, 0x2A2028, 1.1f).trim(0xB0141A, 0.35f);
        Paint plate = p(Mat.PLATE, 0x1E161A).gloss(0.6f).trim(0x8A1418, 0.45f);
        Paint iron = p(Mat.DARK_METAL, 0x1E181A).gloss(0.5f);
        Paint horn = p(Mat.BONE, 0x9A8C7A).accent(0x5A4E42).pattern(Pattern.BANDS, 0x6A5E50, 1.1f);
        Paint hornTip = p(Mat.BONE, 0x7A1A14).accent(0xFF3A1A).glow();
        Paint ember = Paint.glowing(0xFF5A1A).accent(0xFFC060);
        Paint crimsonGlow = Paint.glowing(0xD0141A).accent(0xFF6A3A);
        Paint runeLine = Paint.glowing(0xC0101A).pattern(Pattern.RUNES, 0xFF6A3A, 1.6f);
        Paint cloth = p(Mat.CLOTH, 0x5A0A10).accent(0x2A0408).trim(0x0E0A0A, 0.5f);
        Paint feather = p(Mat.FEATHER, 0x161214).accent(0x3A1A16).pattern(Pattern.FEATHERS, 0x0A0808, 1.2f);
        Paint featherTip = Paint.glowing(0xFF4A10).accent(0x8A1A0A);
        Paint wingBone = p(Mat.BONE, 0x2A2224).accent(0x141010);
        Paint hair = p(Mat.HAIR, 0x0C0808).accent(0x2A1414);
        Paint face = p(Mat.SKIN, 0x5A4C4C).pattern(Pattern.VEINS, 0xFF4A10, 0.5f).accentGlow()
                .decal(Face.FRONT, DecalType.SLIT_EYES, 0xFF3A1A, true, 0.5f, 0.42f, 1.1f)
                .decal(Face.FRONT, DecalType.BROW, 0x0C0808, false, 0.5f, 0.35f, 1.15f)
                .decal(Face.FRONT, DecalType.GRIN, 0x1A0A0A, false, 0.5f, 0.78f, 0.75f);

        Body.Spec sp = new Body.Spec(skin, plate).scale(K);
        sp.legs(6.5f, 6.5f, 3.8f);
        sp.foot = 1.6f;
        sp.hipW = 7.6f;
        sp.torso(10.2f, 8.4f, 5.4f, 7.4f, 5, 4.6f);
        sp.neck = 1.2f;
        sp.head(6.4f, 7, 6.4f);
        sp.arms(6.5f, 6.5f, 3.6f);
        sp.hand = 2.4f;
        sp.paints(obsidian, plate, plate, iron, skin, fur, fur, hoof);
        sp.headPaint(face);
        sp.neckP = skin;
        Body b = Body.build(s.root, sp);

        // ---- head: swept-back hair, pale ram horns, crown of thorns, shattered halo
        b.head.box(-3.4f * K, -7.3f * K, 1.2f * K, 6.8f * K, 4.8f * K, 2.2f * K, hair);
        Part mane = b.head.child("mane", 0, -3.2f * K, 2.6f * K);
        mane.box(-3 * K, 0, -0.6f * K, 6 * K, 7 * K, 1.2f * K, hair);
        for (int side = -1; side <= 1; side += 2) {
            DevilGear.ramHorn(b.head, side, side * 3.1f * K, -5.6f * K, 0.2f * K, 2.9f * K, 3.0f * K, 5, 54, horn, hornTip);
        }
        Part crown = b.head.sub(0, -7.3f * K, 0);
        crown.box(-3.45f * K, -0.6f * K, -3.45f * K, 6.9f * K, 1 * K, 6.9f * K, iron.copy().trim(0xB0141A, 0.3f));
        for (int i = 0; i < 7; i++) {
            float a = (i - 3) * 24;
            crown.spike(Mth.sin(a * Mth.DEG_TO_RAD) * 3.2f * K, -0.5f * K, -Mth.cos(a * Mth.DEG_TO_RAD) * 3.2f * K, 0.75f * K,
                    (i == 3 ? 3.8f : 2.2f + (i % 2) * 0.9f) * K, -12, 0, (i - 3) * -7, iron);
        }
        Part crown3 = crown.sub(0, 0, 0).from(3);
        for (int i = 0; i < 4; i++) {
            float a = 105 + i * 50;
            crown3.spike(Mth.sin(a * Mth.DEG_TO_RAD) * 3.2f * K, -0.5f * K, -Mth.cos(a * Mth.DEG_TO_RAD) * 3.2f * K, 0.6f * K, 2f * K, 12, 0, 0, iron);
        }
        Part halo = b.head.child("halo", 0, -7.5f * K, 4.4f * K);
        int segs = 16;
        for (int i = 0; i < segs; i++) {
            if (i == 2 || i == 3 || i == 10) continue; // the halo is shattered
            float a = i * 360f / segs;
            float r = 7.4f * K;
            Part sg = halo.sub(Mth.cos(a * Mth.DEG_TO_RAD) * r, Mth.sin(a * Mth.DEG_TO_RAD) * r, 0).rot(0, 0, a + 90);
            sg.box(-1.5f * K, -0.5f * K, -0.5f * K, 3f * K, 1f * K, 1f * K, i % 2 == 0 ? crimsonGlow : ember);
            sg.spike(0, -0.5f * K, 0, 0.5f * K, (i % 3 == 0 ? 2.2f : 1.3f) * K, 0, 0, 180, crimsonGlow);
        }

        // ---- armour: spiked pauldrons, breastplate with a glowing skull, belt, tabard, gauntlets
        b.chest.box(-5.3f * K, -8.6f * K, -3 * K, 10.6f * K, 4.6f * K, 6 * K, obsidian.copy()
                .decal(Face.FRONT, DecalType.SKULL, 0xD0141A, true, 0.5f, 0.55f, 0.85f));
        b.chest.box(-0.3f * K, -4 * K, -2.95f * K, 0.6f * K, 3.6f * K, 0.4f * K, runeLine);
        for (int side = -1; side <= 1; side += 2) {
            Part pd = Gear.pauldron(side < 0 ? b.rArm : b.lArm, side, 4.4f * K, plate, p(Mat.DARK_METAL, 0x0C0808));
            pd.box(-3.2f * K, -2.2f * K, -2.8f * K, 6.4f * K, 1.2f * K, 5.6f * K, obsidian);
            pd.spike(side * 1.4f * K, -2 * K, 0, 1.3f * K, 4.2f * K, 0, 0, side * -22, horn);
            pd.spike(side * 0.3f * K, -2 * K, -1.8f * K, 0.9f * K, 2.6f * K, -22, 0, side * -12, iron);
            pd.spike(side * 0.3f * K, -2 * K, 1.8f * K, 0.9f * K, 2.6f * K, 22, 0, side * -12, iron);
        }
        Gear.belt(b.pelvis, 7.4f * K, 4.6f * K, -1.6f * K, 1.5f * K, iron, p(Mat.BONE, 0x9A8C7A).decal(Face.FRONT, DecalType.SKULL, 0xD0141A, true));
        Part tabard = b.pelvis.child("tabard", 0, 0.2f * K, -2.5f * K);
        tabard.box(-2.7f * K, 0, -0.25f * K, 5.4f * K, 8.5f * K, 0.5f * K, cloth.copy().decal(Face.FRONT, DecalType.EYE_SIGIL, 0xD0141A, true, 0.5f, 0.28f, 0.7f));
        Part tabardB = b.pelvis.child("tabard_b", 0, 0.2f * K, 2.5f * K);
        tabardB.box(-3.2f * K, 0, -0.25f * K, 6.4f * K, 9.5f * K, 0.5f * K, cloth);
        Gear.sleeve(b.rFore, 3.6f * K, 2 * K, 4.4f * K, 3.6f * K, 0.15f, obsidian);
        Gear.sleeve(b.lFore, 3.6f * K, 2 * K, 4.4f * K, 3.6f * K, 0.15f, obsidian);
        for (int side = -1; side <= 1; side += 2) {
            Part fore = side < 0 ? b.rFore : b.lFore;
            fore.spike(side * 1.9f * K, 3.2f * K, 0, 0.6f * K, 1.8f * K, 0, 0, side * 80, iron);
            Part hand = side < 0 ? b.rHand : b.lHand;
            for (int f = 0; f < 3; f++) hand.spike((f - 1) * 0.8f * K, 2.2f * K, -0.9f * K, 0.45f * K, 1.6f * K, 180 - 25, 0, 0, hornTip);
        }
        // goat legs: shaggy thighs, iron greaves with knee spikes, cloven hooves
        for (int side = -1; side <= 1; side += 2) {
            Part thigh = side < 0 ? b.rThigh : b.lThigh;
            thigh.box(-2.2f * K, 2 * K, -2.2f * K, 4.4f * K, 3.4f * K, 4.4f * K, fur);
            Part shin = side < 0 ? b.rShin : b.lShin;
            Gear.sleeve(shin, 3.6f * K, 0.4f * K, 3.4f * K, 3.6f * K, 0.1f, iron);
            shin.spike(0, 0.8f * K, -1.8f * K, 0.8f * K, 2 * K, -75, 0, 0, iron);
            Part foot = side < 0 ? b.rFoot : b.lFoot;
            foot.box(-0.2f * K, 0, -2.9f * K, 0.4f * K, 1.6f * K, 2.1f * K, p(Mat.DARK_METAL, 0x050404));
        }
        Part body5 = b.chest.sub(0, 0, 0).from(5);
        body5.box(-4.8f * K, -8.3f * K, -2.5f * K, 9.6f * K, 8.2f * K, 5 * K, skinHot);
        body5.box(-1.2f * K, -9.2f * K, -1 * K, 2.4f * K, 1.4f * K, 2 * K, ember);

        // ---- three pairs of burnt wings and a barbed tail
        String[] wn = {"wing_hi", "wing_mid", "wing_lo"};
        float[] span = {22 * K, 16 * K, 11 * K};
        float[] wy = {-7.6f * K, -5.6f * K, -3.6f * K};
        for (int i = 0; i < WING_PAIRS; i++) {
            Part wr = b.chest.child(wn[i] + "_r", -1.6f * K, wy[i], 2.8f * K);
            DevilGear.burntWing(wr, -1, span[i], feather, wingBone, featherTip);
            Part wl = b.chest.child(wn[i] + "_l", 1.6f * K, wy[i], 2.8f * K);
            DevilGear.burntWing(wl, 1, span[i], feather, wingBone, featherTip);
        }
        Part tail = b.pelvis.child("tail0", 0, -0.6f * K, 2.3f * K);
        Part seg = tail;
        float tw = 1.6f * K;
        for (int i = 0; i < TAIL; i++) {
            float len = 3.2f * K;
            seg.box(-tw / 2, -tw / 2, 0, tw, tw, len, i % 2 == 0 ? skin : p(Mat.SKIN, 0x3A3030));
            if (i < TAIL - 1) {
                seg = seg.child("tail" + (i + 1), 0, 0, len);
                tw *= 0.86f;
            }
        }
        Part barb = seg.sub(0, 0, 3.2f * K);
        barb.box(-1.8f * K, -0.3f * K, 0, 3.6f * K, 0.6f * K, 1.2f * K, crimsonGlow);
        barb.spike(0, 0, 1.2f * K, 1.6f * K, 2.8f * K, 90, 0, 0, crimsonGlow);
        barb.spike(-1.6f * K, 0, 0.4f * K, 0.7f * K, 1.8f * K, 90, -40, 0, crimsonGlow);
        barb.spike(1.6f * K, 0, 0.4f * K, 0.7f * K, 1.8f * K, 90, 40, 0, crimsonGlow);

        // ---- Perdition: grip at the fist, blade along the hand's -Z, edges up and down (Y)
        Part sword = b.rHand.child("perdition", 0, 1.3f * K, 0);
        sword.box(-0.5f * K, -0.5f * K, -0.6f * K, K, K, 5.4f * K, p(Mat.LEATHER, 0x1A0E0E).pattern(Pattern.BANDS, 0x5A0A10, 2f));
        sword.box(-0.8f * K, -0.8f * K, 4.6f * K, 1.6f * K, 1.6f * K, 1.4f * K, p(Mat.BONE, 0x9A8C7A).decal(Face.BACK, DecalType.SKULL, 0xD0141A, true));
        Part guard = sword.sub(0, 0, -0.8f * K);
        guard.box(-1.3f * K, -2.6f * K, -1.1f * K, 2.6f * K, 5.2f * K, 1.4f * K, p(Mat.BONE, 0x9A8C7A).accent(0x5A4E42)
                .decal(Face.FRONT, DecalType.SKULL, 0xD0141A, true, 0.5f, 0.5f, 1.0f));
        guard.spike(0, -2.6f * K, -0.4f * K, 1f * K, 3.4f * K, 30, 0, 0, horn);
        guard.spike(0, 2.6f * K, -0.4f * K, 1f * K, 3.4f * K, 150, 0, 0, horn);
        Part blade = sword.child("blade", 0, 0, -1.9f * K);
        Paint steel = p(Mat.DARK_METAL, 0x141012).gloss(0.95f);
        float bl = 22 * K;
        blade.taper(0, 0, -bl * 0.5f, 0.7f * K, 0.7f * K, 0.7f * K, 0.7f * K, 0, 1, steel);
        blade.box(-0.35f * K, -1.9f * K, -bl, 0.7f * K, 3.8f * K, bl, steel);
        blade.box(-0.4f * K, -2.15f * K, -bl * 0.97f, 0.8f * K, 0.3f * K, bl * 0.95f, ember);
        blade.box(-0.4f * K, 1.85f * K, -bl * 0.97f, 0.8f * K, 0.3f * K, bl * 0.95f, ember);
        blade.box(-0.42f * K, -0.3f * K, -bl * 0.88f, 0.84f * K, 0.6f * K, bl * 0.8f, runeLine);
        for (int i = 0; i < 3; i++) {
            float z = -bl * (0.62f + 0.11f * i);
            blade.spike(0, -1.9f * K, z, 0.9f * K, 1.8f * K, 35, 0, 0, steel);
        }
        blade.spike(0, 0, -bl, 3.8f * K, 4.6f * K, 90, 0, 0, steel);
    }

    @Override
    public void animate(ProcModel<HeroEntity> m, HeroEntity h, HeroModels.Ctx c) {
        Poser p = Poser.of(m);
        HeroModels.humanoid(p, h, c, 0.85f, 0.45f);
        boolean soaring = h instanceof LuciferDevil l && l.soaring();
        float move = Math.min(1, c.la);
        // digitigrade goat legs
        p.rThigh.xRot -= 0.32f;
        p.lThigh.xRot -= 0.32f;
        p.rShin.xRot += 0.68f;
        p.lShin.xRot += 0.68f;
        p.rFoot.xRot -= 0.36f;
        p.lFoot.xRot -= 0.36f;
        p.pelvis.y += 1.3f;
        p.chest.xRot += 0.05f;
        // tabard flaps with the legs
        ModelPart tab = m.p("tabard"), tabB = m.p("tabard_b");
        tab.xRot += Math.min(0, Math.min(p.rThigh.xRot, p.lThigh.xRot)) * 0.7f;
        tabB.xRot += Math.max(0, Math.max(p.rThigh.xRot, p.lThigh.xRot)) * 0.7f + 0.08f;
        // mane sways; halo turns slowly and hovers; tail sways
        m.p("mane").xRot += 0.1f + Math.min(1, c.la) * 0.3f + Mth.sin(c.age * 0.08f) * 0.04f;
        ModelPart halo = m.p("halo");
        halo.zRot = c.age * 0.02f;
        halo.y += Mth.sin(c.age * 0.06f) * 0.8f;
        for (int i = 0; i < TAIL; i++) {
            ModelPart t = m.p("tail" + i);
            t.xRot += (i == 0 ? 0.55f : 0.12f) + Mth.sin(c.age * 0.09f - i * 0.6f) * 0.08f;
            t.yRot += Mth.sin(c.age * 0.07f - i * 0.5f) * (0.18f + 0.04f * i);
        }
        // wings: folded half-open at rest, spread while fighting, fully spread and beating aloft
        float spread = soaring ? 1f : c.combat ? 0.45f : 0.15f;
        float beat = soaring ? Mth.sin(c.age * 0.45f) : Mth.sin(c.age * 0.06f) * 0.25f;
        String[] wn = {"wing_hi", "wing_mid", "wing_lo"};
        for (int i = 0; i < WING_PAIRS; i++) {
            float fold = (1 - spread) * (1.0f - i * 0.15f);
            float lift = (0.35f - i * 0.25f) + beat * (0.5f - i * 0.12f);
            ModelPart wr = m.p(wn[i] + "_r"), wl = m.p(wn[i] + "_l");
            wr.yRot += 0.5f + fold * 1.0f;
            wl.yRot -= 0.5f + fold * 1.0f;
            wr.zRot += -lift + fold * 0.3f;
            wl.zRot -= -lift + fold * 0.3f;
            wr.xRot += 0.15f * i;
            wl.xRot += 0.15f * i;
        }
        // Perdition: carried low and to the side; two-handed guard in combat
        if (c.combat || c.id != 0) {
            p.armR(-45, -20, 0, 50);
            p.armL(-55, 40, 0, 70);
            p.stance(0.4f);
        } else {
            p.armR(-15, 0, 10, 20);
            m.p("perdition").xRot += 0.9f;
        }
        switch (c.id) {
            case 1 -> {
                float k = Anim.swing(c.t, 8, 3, 6);
                p.armR(-110 * Math.max(0, k) + 70 * Math.max(0, -k), 30 * Math.max(0, k), 0, -20 * Math.max(0, k));
                p.armL(-100 * Math.max(0, k) + 60 * Math.max(0, -k), 20 * Math.max(0, k), 0, -10);
                p.spine(-15 * Math.max(0, k) + 30 * Math.max(0, -k), 25 * Math.max(0, k) - 30 * Math.max(0, -k), 0);
            }
            case 2 -> {
                float k = Anim.swing(c.t, 8, 3, 6);
                p.armR(-30 + 20 * k, 70 * Math.max(0, k) - 90 * Math.max(0, -k), 0, 10);
                p.armL(-40 + 20 * k, 60 * Math.max(0, k) - 70 * Math.max(0, -k), 0, 20);
                p.spine(5, 45 * Math.max(0, k) - 60 * Math.max(0, -k), 0);
            }
            case 3 -> {
                float spin = Anim.win(c.t, 8, 6);
                p.pelvis.yRot += spin * Mth.TWO_PI;
                p.armR(-80, 70, 0, 0);
                p.armL(-80, 60, 0, 10);
                p.crouch(0.3f * Anim.pulse(c.t, 6, 10), 3);
            }
            case 4 -> {
                float up = c.t < 9 ? Anim.smooth(Math.min(1, c.t / 8f)) : 0;
                float down = Anim.hold(c.t, 9, 14, 4);
                p.armR(-175 * up - 60 * down, 0, 0, 10);
                p.armL(-170 * up - 55 * down, 0, 0, 10);
                p.spine(-20 * up + 45 * down, 0, 0);
                p.crouch(down * 0.6f, 4);
            }
            case LuciferDevil.FALL -> {
                float crouch = Anim.pulse(c.t, 0, 8);
                p.crouch(crouch, 5);
                if (c.t > 8) {
                    p.armR(-160, 0, 20, 20);
                    p.armL(-160, 0, -20, 20);
                    p.legR(-30, 0, 60);
                    p.legL(-50, 0, 80);
                    p.spine(c.t > 22 ? 40 : -10, 0, 0);
                }
            }
            case LuciferDevil.DAMNATION -> {
                float raise = Anim.hold(c.t, 2, 20, 4);
                float slam = Anim.pulse(c.t, 20, 10);
                p.armL(-160 * raise + 40 * slam, 0, -20 * raise, 30);
                p.head.xRot -= 0.4f * raise;
                p.spine(-15 * raise + 30 * slam, 0, 0);
                p.crouch(slam * 0.4f, 3);
            }
            case LuciferDevil.CHAINS -> {
                float reach = Anim.hold(c.t, 0, 7, 3);
                float yank = Anim.hold(c.t, 8, 20, 6);
                p.armL(-90 * reach + 20 * yank, -30 * reach, 0, 80 * yank);
                p.armR(-60 * reach, 20 * reach, 0, 60 * yank);
                p.spine(15 * reach - 25 * yank, 0, 0);
            }
            case LuciferDevil.WINGS -> {
                float k = Anim.pulse(c.t, 0, 18);
                p.armR(-60 * k, 0, 70 * k, 10);
                p.armL(-60 * k, 0, -70 * k, 10);
                p.spine(-20 * k, 0, 0);
            }
            case LuciferDevil.DREAD -> {
                float k = Anim.hold(c.t, 4, 20, 5);
                p.armR(-100 * k, 0, 60 * k, 40 * k);
                p.armL(-100 * k, 0, -60 * k, 40 * k);
                p.head.xRot -= 0.7f * k;
                p.spine(-25 * k, 0, 0);
            }
            case LuciferDevil.LEGION -> {
                float k = Anim.hold(c.t, 2, 22, 4);
                p.armR(-60 * k, 0, 0, 0);
                p.armL(-60 * k, 0, 0, 0);
                p.crouch(0.5f * k, 4);
                p.spine(25 * k, 0, 0);
            }
            case HeroEntity.ULT -> {
                float rise = Anim.hold(c.t, 0, 108, 6);
                p.armR(-150 * rise, 0, 40 * rise, 20);
                p.armL(-150 * rise, 0, -40 * rise, 20);
                p.legR(-20 * rise, 0, 40 * rise);
                p.legL(-35 * rise, 0, 60 * rise);
                p.head.xRot -= 0.3f * rise;
                if (c.t > 108) {
                    p.armR(-170, 0, 0, 0);
                    p.armL(-170, 0, 0, 0);
                    p.spine(35, 0, 0);
                }
            }
            default -> {
            }
        }
        if (move > 0.6f && c.id == 0) p.spine(10, 0, 0);
    }
}
