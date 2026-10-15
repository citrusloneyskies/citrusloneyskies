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
import com.lastbastion.entity.hero.MolochDevil;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Moloch, the Furnace King: a bull-headed idol of blackened bronze. A broad bovine skull with a heavy muzzle, a
 * ring through the nose and great forward-sweeping horns under a crown of flame; a barrel torso whose belly is a
 * furnace behind a hinged iron grate; two chimney stacks on his shoulders; burning palms; cloven hooves and a
 * tufted tail. Glowing cracks run through the bronze; at full heat the whole idol turns white-hot.
 */
public final class MolochModel implements HeroModels.Def {
    private static final float K = 1.55f;
    static final int TAIL = 3;
    private static final String[] MOLTEN = {"mol_chest", "mol_pelvis", "mol_head", "mol_r_arm", "mol_l_arm", "mol_r_fore", "mol_l_fore",
            "mol_r_thigh", "mol_l_thigh", "mol_r_shin", "mol_l_shin"};

    @Override
    public float shadow() {
        return 1.25f;
    }

    @Override
    public int density() {
        return 3;
    }

    @Override
    public void build(ModelSpec s) {
        Paint bronze = p(Mat.DARK_METAL, 0x4A3020).gloss(0.7f).pattern(Pattern.CRACKS, 0xFF5A10, 0.7f).accentGlow();
        Paint bronzeHand = p(Mat.DARK_METAL, 0x4A3020).gloss(0.7f).pattern(Pattern.CRACKS, 0xFF8A2A, 1.4f).accentGlow();
        Paint bronzeDark = p(Mat.DARK_METAL, 0x2E2018).gloss(0.6f).pattern(Pattern.PLATES, 0x3E2C1E, 1.2f).trim(0xB06A2A, 0.3f);
        Paint iron = p(Mat.DARK_METAL, 0x1A1412).gloss(0.5f);
        Paint gold = p(Mat.GOLD, 0xD8A040).gloss(0.9f);
        Paint horn = p(Mat.BONE, 0x3A2A22).accent(0x1A120E).pattern(Pattern.BANDS, 0x2A1E18, 1.1f).gloss(0.4f);
        Paint hornTip = Paint.glowing(0xFF7A1A).accent(0xFFE0A0);
        Paint ember = Paint.glowing(0xFF6A1A).accent(0xFFD27A);
        Paint whiteHot = Paint.glowing(0xFFD890).accent(0xFFFFFF);
        Paint furnace = Paint.glowing(0xFF7A1A).accent(0xFFF0B0).pattern(Pattern.VEINS, 0xFFE8A0, 1.4f);
        Paint cloth = p(Mat.CLOTH, 0x3A1A10).accent(0x1A0A06).trim(0x8A5A2A, 0.4f);
        Paint hoof = p(Mat.DARK_METAL, 0x0E0A08).gloss(0.6f);
        Paint fur = p(Mat.FUR, 0x1A1210).accent(0x2E2018);
        Paint molten = p(Mat.METAL, 0xD8742A).gloss(0.9f).pattern(Pattern.CRACKS, 0xFFF4D8, 1.3f).accentGlow();
        Paint face = bronze.copy();

        Body.Spec sp = new Body.Spec(bronze, bronzeDark).scale(K);
        sp.legs(6.4f, 6.4f, 4.6f);
        sp.foot = 1.6f;
        sp.hipW = 8.8f;
        sp.torso(12, 9, 7.4f, 10.4f, 5, 6.6f);
        sp.neck = 0.6f;
        sp.head(7.8f, 6.2f, 7);
        sp.arms(6.4f, 6.4f, 4.8f);
        sp.hand = 3;
        sp.paints(bronze, bronzeDark, bronze, bronze, bronzeHand, fur, bronze, hoof);
        sp.headPaint(face);
        sp.neckP = bronze;
        Body b = Body.build(s.root, sp);

        // ---- the bull's head: muzzle, nose ring, forward-sweeping horns, a crown of flame
        Part head = b.head;
        head.box(-3.1f * K, -3.6f * K, -6.6f * K, 6.2f * K, 3.6f * K, 3.2f * K, bronze);
        head.box(-2.6f * K, -3.2f * K, -6.9f * K, 5.2f * K, 2.8f * K, 0.4f * K, bronzeDark);
        for (int side = -1; side <= 1; side += 2) head.center(side * 1.2f * K, -1.9f * K, -7.05f * K, 0.8f * K, 0.55f * K, 0.3f * K, iron);
        head.box(-4 * K, -5.7f * K, -3.95f * K, 8 * K, 0.9f * K, 1.5f * K, bronzeDark);
        for (int side = -1; side <= 1; side += 2) {
            head.sub(side * 1.9f * K, -4.3f * K, -3.55f * K).rot(0, 0, side * -12).box(-0.9f * K, -0.4f * K, -0.25f * K, 1.8f * K, 0.8f * K, 0.4f * K, whiteHot);
        }
        head.box(-3.9f * K, -6.7f * K, -3.5f * K, 7.8f * K, 1.1f * K, 7 * K, bronzeDark);
        Part ring = head.sub(0, -0.2f * K, -6.4f * K).rot(90, 0, 0);
        ring.octoRing(-0.3f * K, 2.2f * K, 0.4f * K, 0.5f * K, gold);
        for (int side = -1; side <= 1; side += 2) {
            Part h1 = head.sub(side * 3.4f * K, -5.4f * K, -0.6f * K).rot(15, 0, side * 80);
            h1.taper(0, 0, 0, 2.4f * K, 2.4f * K, 2 * K, 2 * K, 3.6f * K, 2, horn);
            Part h2 = h1.sub(0, -3.6f * K, 0).rot(20, 0, side * -45);
            h2.taper(0, 0, 0, 2 * K, 2 * K, 1.5f * K, 1.5f * K, 3.6f * K, 2, horn);
            Part h3 = h2.sub(0, -3.6f * K, 0).rot(30, 0, side * -30);
            h3.taper(0, 0, 0, 1.5f * K, 1.5f * K, 0.7f * K, 0.7f * K, 3.2f * K, 3, horn);
            h3.spike(0, -3.2f * K, 0, 0.7f * K, 1.6f * K, 10, 0, 0, hornTip);
            // ears under the horns
            Gear.ear(head, side, side * 3.6f * K, -3.8f * K, 0.4f * K, 1.2f * K, 2.4f * K, bronze);
        }
        head.sub(0, -6.6f * K, 0).octoRing(-0.8f * K, 6.4f * K, 0.7f * K, 1 * K, gold);
        Part crown = head.child("crown", 0, -6.6f * K, 0);
        for (int i = 0; i < 8; i++) {
            float a = i * 45 * Mth.DEG_TO_RAD;
            crown.spike(Mth.sin(a) * 2.6f * K, 0, Mth.cos(a) * 2.6f * K, 0.9f * K, (i % 2 == 0 ? 2.8f : 1.8f) * K,
                    Mth.cos(a) * 14, 0, -Mth.sin(a) * 14, i % 2 == 0 ? ember : hornTip);
        }
        crown.center(0, -0.6f * K, 0, 2.2f * K, 1.4f * K, 2.2f * K, whiteHot);

        // ---- the furnace belly: glowing core behind a hinged grate in an iron frame
        float cw = 12 * K, cd = 7.4f * K;
        Part fur0 = b.chest.child("furnace", 0, -2.6f * K, -cd / 2);
        fur0.box(-3.2f * K, -2.8f * K, -0.3f * K, 6.4f * K, 5.6f * K, 0.6f * K, furnace);
        fur0.box(-4.2f * K, -3.6f * K, -0.9f * K, 8.4f * K, 0.9f * K, 1.2f * K, iron);
        fur0.box(-4.2f * K, 2.7f * K, -0.9f * K, 8.4f * K, 0.9f * K, 1.2f * K, iron);
        fur0.box(-4.2f * K, -2.8f * K, -0.9f * K, 1 * K, 5.6f * K, 1.2f * K, iron);
        fur0.box(3.2f * K, -2.8f * K, -0.9f * K, 1 * K, 5.6f * K, 1.2f * K, iron);
        Part door = fur0.child("maw_door", -3.2f * K, 0, -0.7f * K);
        for (int i = 0; i < 5; i++) door.box((0.3f + i * 1.3f) * K, -2.8f * K, -0.35f * K, 0.55f * K, 5.6f * K, 0.5f * K, iron);
        door.box(0, -1.2f * K, -0.45f * K, 6.4f * K, 0.5f * K, 0.5f * K, iron);
        door.box(0, 0.8f * K, -0.45f * K, 6.4f * K, 0.5f * K, 0.5f * K, iron);
        door.box(5.2f * K, -0.3f * K, -0.9f * K, 0.8f * K, 0.8f * K, 0.6f * K, gold);
        // chest plate with a bull emblem above the furnace
        b.chest.box(-5 * K, -9.3f * K, -cd / 2 - 0.5f * K, 10 * K, 3.8f * K, 1 * K, bronzeDark.copy()
                .decal(Face.FRONT, DecalType.EMBLEM, 0xFF8A2A, true, 0.5f, 0.5f, 0.8f));

        // ---- chimneys on the shoulders, smoke-stained, glowing at the lip
        for (int side = -1; side <= 1; side += 2) {
            Part ch = b.chest.child(side < 0 ? "chimney_r" : "chimney_l", side * 4.1f * K, -9 * K, 2.3f * K).rot(-12, 0, side * 10);
            ch.octo(0, -7.2f * K, 0, 2.8f * K, 7.4f * K, 2.8f * K, iron);
            ch.octo(0, -7.8f * K, 0, 3.4f * K, 0.9f * K, 3.4f * K, bronzeDark);
            ch.box(-1.1f * K, -7.85f * K, -1.1f * K, 2.2f * K, 0.2f * K, 2.2f * K, ember);
            ch.octo(0, -3.2f * K, 0, 3.1f * K, 0.7f * K, 3.1f * K, bronzeDark);
        }

        // ---- arms: pauldrons, bracers, burning palms
        for (int side = -1; side <= 1; side += 2) {
            Part arm = side < 0 ? b.rArm : b.lArm;
            Part pd = Gear.pauldron(arm, side, 4.8f * K, bronzeDark, gold);
            pd.box(-3 * K, -2.6f * K, -3 * K, 6 * K, 1.2f * K, 6 * K, bronzeDark);
            for (int r = 0; r < 3; r++) pd.center((r - 1) * 1.8f * K, -2.7f * K, -2.8f * K, 0.6f * K, 0.6f * K, 0.6f * K, gold);
            Part fore = side < 0 ? b.rFore : b.lFore;
            Gear.sleeve(fore, 5 * K, 1.4f * K, 4.6f * K, 5 * K, 0.1f, bronzeDark);
            fore.box(-2.6f * K, 5.4f * K, -2.6f * K, 5.2f * K, 0.6f * K, 5.2f * K, gold);
            Part hand = side < 0 ? b.rHand : b.lHand;
            for (int f = 0; f < 4; f++) {
                hand.spike((f - 1.5f) * 0.9f * K, 2.8f * K, -0.6f * K, 0.6f * K, (1.4f + (f % 2) * 0.8f) * K, 180, 0, (f - 1.5f) * 8, whiteHot);
            }
        }
        // belt, chains of offering tokens, loincloth front and back
        Gear.belt(b.pelvis, 10.4f * K, 6.6f * K, -1.8f * K, 1.6f * K, iron, gold.copy().decal(Face.FRONT, DecalType.SKULL, 0x5A2A0A, false));
        Part loin = b.pelvis.child("loin", 0, 0.2f * K, -3.5f * K);
        loin.box(-3.2f * K, 0, -0.3f * K, 6.4f * K, 7.6f * K, 0.6f * K, cloth);
        Part loinB = b.pelvis.child("loin_b", 0, 0.2f * K, 3.5f * K);
        loinB.box(-3.6f * K, 0, -0.3f * K, 7.2f * K, 6.6f * K, 0.6f * K, cloth);
        // legs: bronze greaves, cloven hooves
        for (int side = -1; side <= 1; side += 2) {
            Part shin = side < 0 ? b.rShin : b.lShin;
            Gear.sleeve(shin, 4.8f * K, 0.4f * K, 4.4f * K, 4.8f * K, 0.1f, bronzeDark);
            shin.spike(0, 0.6f * K, -2.4f * K, 0.9f * K, 1.8f * K, -78, 0, 0, gold);
            Part foot = side < 0 ? b.rFoot : b.lFoot;
            foot.box(-0.25f * K, 0, -3.5f * K, 0.5f * K, 1.6f * K, 2.4f * K, p(Mat.PLAIN, 0x050403));
        }
        // a bull's tail with a smouldering tuft
        Part tail = b.pelvis.child("tail0", 0, -1.6f * K, 3.3f * K);
        for (int i = 0; i < TAIL; i++) {
            float len = 3.4f * K;
            tail.box(-0.5f * K, 0, -0.5f * K, 1 * K, len, 1 * K, bronze);
            if (i < TAIL - 1) tail = tail.child("tail" + (i + 1), 0, len, 0);
        }
        tail.box(-1 * K, 3 * K, -1 * K, 2 * K, 2.6f * K, 2 * K, fur);
        tail.box(-0.7f * K, 5.2f * K, -0.7f * K, 1.4f * K, 1 * K, 1.4f * K, ember);

        // ---- white-hot shells, shown only while molten
        moltenShell(b.chest, "mol_chest", -cw / 2, -9 * K, -cd / 2, cw, 9 * K, cd, molten);
        moltenShell(b.pelvis, "mol_pelvis", -5.2f * K, -5 * K, -3.3f * K, 10.4f * K, 6.2f * K, 6.6f * K, molten);
        moltenShell(head, "mol_head", -3.9f * K, -6.2f * K, -3.5f * K, 7.8f * K, 6.2f * K, 7 * K, molten);
        for (int side = -1; side <= 1; side += 2) {
            String sd = side < 0 ? "r" : "l";
            moltenShell(side < 0 ? b.rArm : b.lArm, "mol_" + sd + "_arm", -2.4f * K, -2.2f * K, -2.4f * K, 4.8f * K, 8.6f * K, 4.8f * K, molten);
            moltenShell(side < 0 ? b.rFore : b.lFore, "mol_" + sd + "_fore", -2.3f * K, 0, -2.3f * K, 4.6f * K, 6.4f * K, 4.6f * K, molten);
            moltenShell(side < 0 ? b.rThigh : b.lThigh, "mol_" + sd + "_thigh", -2.3f * K, -0.5f * K, -2.3f * K, 4.6f * K, 6.9f * K, 4.6f * K, molten);
            moltenShell(side < 0 ? b.rShin : b.lShin, "mol_" + sd + "_shin", -2.1f * K, 0, -2.1f * K, 4.2f * K, 6.4f * K, 4.2f * K, molten);
        }
    }

    private static void moltenShell(Part on, String name, float x, float y, float z, float w, float h, float d, Paint p) {
        // gated out of normal rendering; animate() shows it while molten
        on.child(name, 0, 0, 0).levels(99, 99).box(x, y, z, w, h, d, 0.35f, p);
    }

    @Override
    public void animate(ProcModel<HeroEntity> m, HeroEntity h, HeroModels.Ctx c) {
        Poser p = Poser.of(m);
        HeroModels.humanoid(p, h, c, 0.7f, 0.35f);
        MolochDevil mo = h instanceof MolochDevil md ? md : null;
        boolean molten = mo != null && mo.molten();
        int stoked = mo != null ? mo.stoked() : 0;
        for (String n : MOLTEN) m.p(n).visible = molten;
        float t = c.t;

        // the furnace breathes; the door rattles as the heat builds
        ModelPart door = m.p("maw_door");
        float open = stoked >= 7 ? 0.06f + 0.05f * Mth.sin(c.age * 0.9f) : 0;
        ModelPart fur = m.p("furnace");
        float glow = 1f + 0.03f * Mth.sin(c.age * 0.25f) + 0.012f * stoked;
        fur.xScale = fur.yScale = glow;
        ModelPart crown = m.p("crown");
        crown.yScale = 1f + 0.12f * Mth.sin(c.age * 0.5f) + (molten ? 0.25f : 0);
        // tail sways, the loincloth follows the legs
        for (int i = 0; i < TAIL; i++) {
            ModelPart tl = m.p("tail" + i);
            tl.xRot += (i == 0 ? 0.35f : 0.12f) + Mth.sin(c.age * 0.07f - i * 0.6f) * 0.08f;
            tl.zRot += Mth.sin(c.age * 0.1f - i * 0.7f) * 0.15f;
        }
        m.p("loin").xRot += Math.min(0, Math.min(p.rThigh.xRot, p.lThigh.xRot)) * 0.7f;
        m.p("loin_b").xRot += Math.max(0, Math.max(p.rThigh.xRot, p.lThigh.xRot)) * 0.7f;

        // brawler's guard: burning fists up when fighting
        if (c.combat && c.id == 0) {
            p.armR(-45, 15, 10, 70);
            p.armL(-50, -15, -10, 75);
            p.head.xRot += 0.15f;
        }
        switch (c.id) {
            case HeroEntity.ANIM_ATTACK -> {
                float k = Anim.swing(t, 11, 3, 8);
                float wind = Math.max(0, k), strike = Math.max(0, -k);
                p.armR(-70 * wind - 80 * strike, 70 * wind - 90 * strike, 20 * wind + 40 * strike, 50 * wind + 10 * strike);
                p.spine(5, 35 * wind - 45 * strike, 0);
            }
            case HeroEntity.ANIM_ATTACK2 -> {
                float k = Anim.swing(t, 11, 3, 8);
                float wind = Math.max(0, k), strike = Math.max(0, -k);
                p.armL(-170 * wind - 60 * strike, 0, -10 * wind, 30 * wind);
                p.armR(-30 * wind, 0, 20 * wind, 40);
                p.spine(-15 * wind + 35 * strike, 0, 0);
                p.crouch(strike * 0.5f, 4);
            }
            case HeroEntity.ANIM_ATTACK3 -> {
                float k = Anim.swing(t, 11, 3, 8);
                float wind = Math.max(0, k), strike = Math.max(0, -k);
                p.head.xRot += 0.6f * wind - 0.7f * strike;
                p.spine(30 * wind - 25 * strike, 0, 0);
                p.armR(-20, 0, 40 * strike, 30);
                p.armL(-20, 0, -40 * strike, 30);
                p.crouch(wind * 0.4f, 3);
                open += 0.5f * strike;
            }
            case MolochDevil.CHARGE -> {
                if (t < 10) {
                    float k = Anim.smooth(Anim.win(t, 0, 6));
                    p.crouch(0.5f * k, 4);
                    p.spine(25 * k, 0, 0);
                    p.head.xRot += 0.5f * k;
                    p.legR(-20 * Mth.sin(t * 0.9f) * k, 0, 20 * k);
                    p.armR(20 * k, 0, 20 * k, 40);
                    p.armL(20 * k, 0, -20 * k, 40);
                } else if (t < 28) {
                    p.spine(38, 0, 0);
                    p.head.xRot += 0.55f;
                    p.armR(35, 0, 25, 60);
                    p.armL(35, 0, -25, 60);
                    float g = t * 1.4f;
                    p.legR(-45 * Mth.sin(g), 0, 30 + 30 * Mth.cos(g));
                    p.legL(45 * Mth.sin(g), 0, 30 - 30 * Mth.cos(g));
                } else {
                    float k = Anim.pulse(t, 28, 10);
                    p.head.xRot -= 0.7f * k;
                    p.spine(-15 * k, 0, 0);
                }
            }
            case MolochDevil.MAW -> {
                float k = Anim.hold(t, 6, 42, 5);
                p.armR(-20 * k, 0, 70 * k, 20);
                p.armL(-20 * k, 0, -70 * k, 20);
                p.spine(-18 * k, 0, 0);
                p.head.xRot -= 0.5f * k;
                open += 1.75f * k;
                fur.xScale = fur.yScale = glow + 0.15f * k;
            }
            case MolochDevil.OFFERING -> {
                float reach = Anim.hold(t, 2, 8, 3);
                float lift = Anim.hold(t, 8, 14, 3);
                float feed = Anim.hold(t, 15, 24, 3);
                p.armR(-80 * reach - 150 * lift - 60 * feed, -10 * reach, 10 * lift, 20 * reach + 80 * feed);
                p.armL(-30 * (lift + feed), 0, -30 * (lift + feed), 50);
                p.spine(10 * reach - 15 * lift + 10 * feed, -20 * feed, 0);
                p.head.xRot -= 0.3f * lift;
                open += 1.6f * Anim.hold(t, 18, 30, 4);
            }
            case MolochDevil.MOLTEN_RAIN -> {
                float k = Anim.hold(t, 4, 36, 5);
                p.armR(-150 * k, 0, 35 * k, 30);
                p.armL(-150 * k, 0, -35 * k, 30);
                p.spine(-20 * k, 0, 0);
                p.head.xRot -= 0.6f * k;
                float puff = (t % 3) / 3f;
                m.p("chimney_r").yScale = 1 + 0.08f * puff * k;
                m.p("chimney_l").yScale = 1 + 0.08f * (1 - puff) * k;
            }
            case MolochDevil.PILLARS -> {
                float up = Anim.hold(t, 4, 15, 4);
                float slam = Anim.hold(t, 18, 23, 3);
                p.armR(-165 * up - 50 * slam, 0, 15 * up, 20);
                p.armL(-165 * up - 50 * slam, 0, -15 * up, 20);
                p.spine(-15 * up + 45 * slam, 0, 0);
                p.crouch(slam * 0.8f, 5);
            }
            case MolochDevil.BRAND -> {
                float up = Anim.hold(t, 2, 9, 3);
                float slam = Anim.hold(t, 11, 18, 3);
                p.armR(-160 * up - 40 * slam, 0, 10, 10);
                p.spine(-10 * up + 40 * slam, 0, 10 * slam);
                p.crouch(slam, 6);
                p.legL(-30 * slam, 0, 40 * slam);
            }
            case HeroEntity.ULT -> {
                float raise = Anim.hold(t, 0, 10, 3);
                float slam = Anim.hold(t, 12, 17, 3);
                float preside = Anim.hold(t, 20, 138, 6);
                float erupt = Anim.hold(t, 146, 156, 4);
                p.armR(-165 * raise - 60 * slam - 40 * preside, 0, 20 * raise + 75 * preside, 20);
                p.armL(-165 * raise - 60 * slam - 40 * preside, 0, -20 * raise - 75 * preside, 20);
                p.spine(-20 * raise + 40 * slam - 12 * preside - 25 * erupt, 0, 0);
                p.crouch(slam * 0.7f, 5);
                p.head.xRot -= 0.4f * raise + 0.35f * preside + 0.6f * erupt;
                open += 0.4f * preside + 1.8f * erupt;
                crown.yScale += 0.4f * preside;
            }
            default -> {
            }
        }
        door.yRot += Math.min(1.8f, open);
    }
}
