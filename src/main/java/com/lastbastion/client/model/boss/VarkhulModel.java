package com.lastbastion.client.model.boss;

import com.lastbastion.client.model.Anim;
import com.lastbastion.client.model.Body;
import com.lastbastion.client.model.BossModels;
import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.model.Poser;
import com.lastbastion.client.model.ProcModel;
import com.lastbastion.entity.boss.SiegeBoss;
import com.lastbastion.entity.boss.VarkhulBoss;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Varkhul the Lich King: a skeletal sovereign in layered midnight robes trimmed with silver and frost
 * runes, a towering flared collar, frost-crystal mantles, wide bell sleeves over bare bone hands, a
 * jagged crown of glowing ice and a staff whose bone-finger cage holds a trapped soul. Three phylactery
 * shards orbit him. His robe ends in tattered strips that trail as he floats. Phase 2 adds a halo of
 * ice shards behind him and a blazing crown.
 */
public final class VarkhulModel implements BossModels.Def {
    private static final float K = 1.3f;

    @Override
    public float shadow() {
        return 0.9f;
    }

    @Override
    public int density() {
        return 4;
    }

    @Override
    public void build(ModelSpec s) {
        int frost = VarkhulBoss.FROST;
        Paint robe = p(Mat.ROBE, 0x1A1E3A).accent(0x0C0E1E).trim(0xA8B4C8, 0.7f).pattern(Pattern.RUNES, 0x3A5A8A, 1.4f);
        Paint robeGlow = p(Mat.ROBE, 0x1A1E3A).accent(0x0C0E1E).trim(0xA8B4C8, 0.7f).pattern(Pattern.RUNES, frost, 1.4f).accentGlow();
        Paint inner = p(Mat.CLOTH, 0x3A1A4A).accent(0x200E2A).pattern(Pattern.CLOTH, 0x2A1236, 2f);
        Paint silver = p(Mat.METAL, 0xA8B4C8).accent(0x5A6478).gloss(0.8f);
        Paint bone = p(Mat.BONE, 0xD8D4C8).accent(0x8A8678).grime(0.2f);
        Paint skull = p(Mat.BONE, 0xE0DCD0).accent(0x8A8678)
                .decal(Face.FRONT, DecalType.HOLLOW_EYES, frost, true, 0.5f, 0.44f, 1.2f)
                .decal(Face.FRONT, DecalType.NOSE, 0x2A2830, false, 0.5f, 0.66f, 1f)
                .decal(Face.FRONT, DecalType.CROSS, frost, true, 0.5f, 0.18f, 0.5f);
        Paint jawP = p(Mat.BONE, 0xD8D4C8).accent(0x8A8678).decal(Face.FRONT, DecalType.FANGS, 0xF0ECE0, false, 0.5f, 0.3f, 1f);
        Paint ice = p(Mat.ICE, 0xBFF4FF).accent(0x5AB8E8).glow();
        Paint iceDim = p(Mat.ICE, 0x9AD8F0).accent(0x4A90C0).gloss(0.9f);
        Paint soul = Paint.glowing(0x9AF0FF).pattern(Pattern.VEINS, 0xFFFFFF, 0.8f);
        Paint staffWood = p(Mat.WOOD, 0x2A2430).accent(0x14101A).pattern(Pattern.RUNES, frost, 1.2f).accentGlow();
        Paint crystal = p(Mat.CRYSTAL, 0x5AD8FF).accent(0xE0FFFF).glow();

        Body.Spec sp = new Body.Spec(robe, robe).scale(K);
        sp.legs(6, 6, 3.4f);
        sp.foot = 1.5f;
        sp.hipW = 6.4f;
        sp.torso(8.4f, 7.6f, 4.6f, 6.4f, 5, 4.2f);
        sp.neck = 1.4f;
        sp.head(6.2f, 6.8f, 6.8f);
        sp.arms(6, 6, 2.4f);
        sp.hand = 2.6f;
        sp.paints(robeGlow, robe, robe, bone, bone, robe, robe, robe);
        sp.headPaint(skull);
        sp.neckP = bone;
        Body b = Body.build(s.root, sp);

        float hip = 13.5f * K, cw = 8.4f * K, ch = 7.6f * K, cd = 4.6f * K;
        // robe skirt flaring to the hem, then trailing tattered strips
        Part skirt = b.pelvis.child("robe", 0, 0, 0);
        skirt.taper(0, hip, 0, 13 * K, 10 * K, 7 * K, 5 * K, hip, 6, robe);
        skirt.taper(0, hip - 0.2f * K, -0.2f * K, 13.3f * K, 10.3f * K, 12.4f * K, 9.8f * K, 1.2f * K, 1, silver);
        skirt.box(-1.8f * K, 0.5f * K, -2.8f * K, 3.6f * K, hip - 1.5f * K, 0.6f * K, inner);
        Part trail = skirt.child("robe2", 0, hip, 0);
        float[] tl = {6, 9, 5, 10, 7, 8, 5, 9};
        for (int i = 0; i < tl.length; i++) {
            float a = i * Mth.TWO_PI / tl.length;
            float x = Mth.sin(a) * 5.6f * K, z = Mth.cos(a) * 4.4f * K;
            trail.sub(x, 0, z).rot(Mth.cos(a) * 12, 0, -Mth.sin(a) * 12).box(-1.4f * K, 0, -0.3f * K, 2.8f * K, tl[i] * K, 0.6f * K, robe);
        }
        // belt with a skull clasp, hanging tabard
        b.pelvis.box(-3.6f * K, -1.6f * K, -2.5f * K, 7.2f * K, 1.4f * K, 5 * K, silver);
        b.pelvis.box(-1.1f * K, -2 * K, -2.9f * K, 2.2f * K, 2.2f * K, 0.6f * K, p(Mat.BONE, 0xE0DCD0).decal(Face.FRONT, DecalType.SKULL, 0x2A2830, false, 0.5f, 0.5f, 1.2f));
        Part tabard = b.pelvis.child("tabard", 0, -0.4f * K, -2.6f * K);
        tabard.box(-2.2f * K, 0, -0.3f * K, 4.4f * K, 11 * K, 0.5f * K, p(Mat.ROBE, 0x3A1A4A).trim(0xA8B4C8, 0.6f).decal(Face.FRONT, DecalType.RUNE, frost, true, 0.5f, 0.45f, 1.2f));
        // towering flared collar and frost mantles
        Part collar = b.chest.child("collar", 0, -ch, cd / 2 - 0.4f * K);
        collar.sub(0, 0, 0).rot(-18, 0, 0).box(-5 * K, -9 * K, 0, 10 * K, 9 * K, 0.7f * K, robe);
        collar.sub(-5 * K, 0, 0).rot(-14, -32, -10).box(-4 * K, -7 * K, 0, 4 * K, 7 * K, 0.6f * K, robe);
        collar.sub(5 * K, 0, 0).rot(-14, 32, 10).box(0, -7 * K, 0, 4 * K, 7 * K, 0.6f * K, robe);
        collar.sub(0, 0, 0).rot(-18, 0, 0).box(-5.2f * K, -9.4f * K, -0.1f * K, 10.4f * K, 0.6f * K, 0.9f * K, silver);
        for (int i = 0; i < 5; i++) collar.spike((i - 2) * 2.2f * K, -9 * K + Math.abs(i - 2) * 0.8f * K, 0.8f * K, 0.9f * K, (3 - Math.abs(i - 2) * 0.6f) * K, -25, 0, (i - 2) * 14, iceDim);
        Part cape = b.chest.child("cape", 0, -ch + 0.6f * K, cd / 2 + 0.3f * K);
        cape.box(-cw * 0.48f, 0, 0, cw * 0.96f, 13 * K, 0.6f * K, robe);
        Part cape2 = cape.child("cape2", 0, 13 * K, 0);
        cape2.box(-cw * 0.5f, 0, 0, cw, 9 * K, 0.6f * K, robe);
        for (int side = -1; side <= 1; side += 2) {
            Part arm = side < 0 ? b.rArm : b.lArm;
            Part mantle = arm.sub(side * 0.4f * K, -1.6f * K, 0).rot(0, 0, side * -18);
            mantle.box(-2.6f * K, -1.2f * K, -2.6f * K, 5.2f * K, 2.2f * K, 5.2f * K, robe);
            mantle.box(-2.8f * K, -1.4f * K, -2.8f * K, 5.6f * K, 0.5f * K, 5.6f * K, silver);
            mantle.spike(side * 1 * K, -1.2f * K, -0.8f * K, 1.2f * K, 3.4f * K, -10, 0, side * -30, ice);
            mantle.spike(side * 1.6f * K, -1 * K, 1.2f * K, 0.9f * K, 2.4f * K, 15, 0, side * -45, iceDim);
            arm.box(-1.7f * K, -0.6f * K, -1.7f * K, 3.4f * K, 6.4f * K, 3.4f * K, robe);
            Part fore = side < 0 ? b.rFore : b.lFore;
            Part sleeve = fore.child(side < 0 ? "sleeve_r" : "sleeve_l", 0, 0, 0);
            sleeve.taper(0, 6 * K, 0, 5.4f * K, 5.4f * K, 3.4f * K, 3.4f * K, 6 * K, 3, robe);
            sleeve.box(-2.8f * K, 5.4f * K, -2.8f * K, 5.6f * K, 0.6f * K, 5.6f * K, silver);
            Part hand = side < 0 ? b.rHand : b.lHand;
            for (int i = 0; i < 4; i++) hand.box(-1.1f * K + i * 0.6f * K, 2.4f * K, -0.3f * K, 0.4f * K, 2 * K, 0.5f * K, bone);
            hand.box(-1.2f * K, 0, -1.2f * K, 2.4f * K, 0.8f * K, 2.4f * K, silver);
        }
        // skull: brow, hinged jaw and the crown of ice
        float hw = 6.2f * K, hh = 6.8f * K, hd = 6.8f * K;
        b.head.box(-hw / 2 - 0.2f * K, -hh * 0.62f, -hd / 2 - 0.3f * K, hw + 0.4f * K, 0.8f * K, 1 * K, bone);
        Part jaw = b.head.child("jaw", 0, -0.4f * K, -0.3f * K);
        jaw.box(-hw / 2 + 0.4f * K, 0, -hd / 2 - 0.1f * K, hw - 0.8f * K, 1.8f * K, hd * 0.75f, jawP);
        Part crown = b.head.child("crown", 0, -hh * 0.8f, 0);
        crown.octoRing(0, hw + 1 * K, 0.8f * K, 1.4f * K, silver);
        float[] cs = {5, 3, 4, 3, 7, 3, 4, 3};
        for (int i = 0; i < cs.length; i++) {
            float a = i * Mth.TWO_PI / cs.length;
            crown.spike(Mth.sin(a) * (hw / 2 + 0.4f * K), 0, -Mth.cos(a) * (hd / 2 + 0.4f * K), 1.1f * K, cs[i] * K * 0.9f,
                    -Mth.cos(a) * 14, 0, -Mth.sin(a) * 14, ice);
        }
        Part blaze = b.head.child("crown2", 0, -hh * 0.8f - 1.4f * K, 0).from(2);
        for (int i = 0; i < 6; i++) {
            float a = i * Mth.TWO_PI / 6 + 0.3f;
            blaze.spike(Mth.sin(a) * hw * 0.35f, 0, -Mth.cos(a) * hd * 0.35f, 1.3f * K, (4 + (i % 2) * 3) * K, -Mth.cos(a) * 8, 0, -Mth.sin(a) * 8,
                    Paint.glowing(0x9AF0FF).accent(0xFFFFFF));
        }
        // the staff: rune-cut black wood, bone-finger cage around a trapped soul
        Part staff = b.rHand.child("staff", 0, 1.4f * K, 0);
        staff.box(-0.55f * K, -0.55f * K, -24 * K, 1.1f * K, 1.1f * K, 33 * K, staffWood);
        staff.box(-0.8f * K, -0.8f * K, -24.6f * K, 1.6f * K, 1.6f * K, 1.2f * K, silver);
        staff.box(-0.8f * K, -0.8f * K, 8 * K, 1.6f * K, 1.6f * K, 1.2f * K, silver);
        for (int i = 0; i < 4; i++) {
            float a = i * Mth.HALF_PI + 0.4f;
            Part f = staff.sub(Mth.cos(a) * 0.7f * K, Mth.sin(a) * 0.7f * K, -24.4f * K).rot(90 + 22 * Mth.sin(a), 0, 22 * Mth.cos(a));
            f.taper(0, 0, 0, 0.7f * K, 0.7f * K, 0.25f * K, 0.25f * K, 5.6f * K, 3, bone);
        }
        Part orb = staff.child("staff_soul", 0, 0, -27.5f * K);
        orb.sphere(0, 0, 0, 1.9f * K, soul);
        // phylactery shards circling him
        Part orbit = b.pelvis.child("orbit", 0, -6 * K, 0);
        for (int i = 0; i < 3; i++) {
            float a = i * Mth.TWO_PI / 3;
            Part c = orbit.sub(Mth.cos(a) * 10 * K, 0, Mth.sin(a) * 10 * K);
            c.taper(0, 1.5f * K, 0, 1.6f * K, 1.6f * K, 0.3f * K, 0.3f * K, 3 * K, 3, crystal);
            c.sub(0, 1.5f * K, 0).rot(180, 0, 0).taper(0, 0, 0, 1.6f * K, 1.6f * K, 0.3f * K, 0.3f * K, 2.2f * K, 2, crystal);
        }
        // phase 2: halo of ice shards behind him
        Part halo = b.chest.child("halo", 0, -ch * 0.7f, cd / 2 + 3 * K).from(2);
        for (int i = 0; i < 10; i++) {
            float a = i * 36f;
            halo.spike(0, 0, 0, 1.4f * K, (6 + (i % 2) * 3.5f) * K, 0, 0, a, ice);
        }
    }

    @Override
    public void animate(ProcModel<SiegeBoss> m, SiegeBoss b, BossModels.Ctx c) {
        Poser p = Poser.of(m);
        ModelPart jaw = m.p("jaw"), staff = m.p("staff"), orb = m.p("staff_soul"), orbit = m.p("orbit");
        ModelPart robe2 = m.p("robe2"), cape = m.p("cape"), cape2 = m.p("cape2"), tabard = m.p("tabard");
        ModelPart sleeveR = m.p("sleeve_r"), sleeveL = m.p("sleeve_l");
        float move = Math.min(1, c.la);
        float bob = Mth.sin(c.age * 0.08f);
        p.idle(c.age, 1f);
        p.lift(bob * 1.4f);
        p.look(c.headYaw, c.headPitch);
        p.flinch(c.hurt);
        // a sovereign's bearing: staff planted at his side, free hand curled before him
        p.armR(-22, 8, 6, 72);
        p.armL(-30 + bob * 4, -20, -6, 60);
        p.spine(6 + move * 12, 0, 0);
        // legs drift under the robe
        p.rThigh.xRot -= 0.2f;
        p.lThigh.xRot -= 0.1f;
        p.rShin.xRot += 0.4f;
        p.lShin.xRot += 0.3f;
        robe2.xRot += 0.15f + move * 0.6f + bob * 0.05f;
        robe2.zRot += Mth.sin(c.age * 0.06f) * 0.05f;
        cape.xRot += 0.12f + move * 0.4f + Mth.sin(c.age * 0.07f) * 0.04f;
        cape2.xRot += 0.1f + move * 0.3f + Mth.sin(c.age * 0.09f + 1) * 0.06f;
        tabard.xRot -= 0.05f + move * 0.25f;
        sleeveR.xRot += Mth.sin(c.age * 0.1f) * 0.05f;
        sleeveL.xRot += Mth.sin(c.age * 0.1f + 1.5f) * 0.05f;
        orb.yRot = c.age * 0.12f;
        orb.xRot = c.age * 0.07f;
        float pulse = 1 + Mth.sin(c.age * 0.25f) * 0.1f;
        orb.xScale = orb.yScale = orb.zScale = pulse;
        orbit.yRot = c.age * 0.05f;
        orbit.y += Mth.sin(c.age * 0.1f) * 1.5f;
        jaw.xRot += 0.05f;
        if (c.phase >= 2) {
            ModelPart halo = m.p("halo"), blaze = m.p("crown2");
            halo.zRot = c.age * 0.03f;
            blaze.yScale = 1 + Mth.sin(c.age * 0.6f) * 0.15f;
        }
        switch (c.id) {
            case SiegeBoss.ANIM_MELEE, SiegeBoss.ANIM_CORE -> {
                float k = Anim.pulse(c.t, 0, 16);
                p.armL(-90 * k, 30 * k, 0, -30 * k);
            }
            case VarkhulBoss.VOLLEY -> {
                float raise = Anim.hold(c.t, 2, 30, 4);
                p.armR(-60 * raise, 20 * raise, 0, -30 * raise);
                staff.xRot -= 0.4f * raise;
                for (int i = 0; i < 5; i++) {
                    float k = Anim.pulse(c.t, 8 + i * 4, 5);
                    p.armL(-70 * k, 15 * k, 0, -50 * k);
                }
                p.armL(-40 * raise, 0, 0, 0);
            }
            case VarkhulBoss.NOVA -> {
                float up = c.t < 18 ? Anim.smooth(Anim.win(c.t, 0, 16)) : 0;
                float burst = Anim.hold(c.t, 18, 26, 4);
                p.armR(-60 * up + 20 * burst, 0, 70 * burst, 40 * up);
                p.armL(-150 * up + 20 * burst, 0, -100 * burst, 40 * up);
                p.spine(-15 * up + 10 * burst, 0, 0);
                p.head.xRot -= 0.4f * up;
                jaw.xRot += 0.6f * burst;
            }
            case VarkhulBoss.COIL -> {
                float k = Anim.hold(c.t, 6, 42, 5);
                p.armL(-90 * k, -10 * k, 0, -10 * k);
                p.spine(-8 * k, -15 * k, 0);
                p.head.xRot += 0.15f * k;
                float clutch = Mth.sin(c.t * 0.6f) * 0.15f * k;
                p.lHand.xRot += clutch;
            }
            case VarkhulBoss.WARD -> {
                float cross = Anim.hold(c.t, 0, 18, 4);
                float spread = Anim.hold(c.t, 22, 36, 4);
                p.armL(-70 * cross - 80 * spread, -50 * cross, -60 * spread, 100 * cross);
                p.armR(-40 * cross - 60 * spread, 40 * cross, 40 * spread, 60 * cross);
                p.spine(20 * cross - 20 * spread, 0, 0);
                p.head.xRot += 0.4f * cross - 0.5f * spread;
            }
            case VarkhulBoss.BLINK -> {
                float k = Anim.pulse(c.t, 0, 10);
                p.spine(35 * k, 0, 0);
                p.armL(-70 * k, -40 * k, 0, 100 * k);
                cape.xRot -= 0.5f * k;
            }
            case VarkhulBoss.ARMY, VarkhulBoss.BLIZZARD -> {
                float raise = Anim.hold(c.t, 4, 34, 6);
                float slam = c.id == VarkhulBoss.ARMY ? Anim.pulse(c.t, 28, 10) : 0;
                p.armR(-150 * raise + 80 * slam, 0, 10 * raise, -40 * raise);
                p.armL(-150 * raise, 0, -30 * raise, 0);
                staff.xRot -= 1.2f * raise;
                p.spine(-20 * raise + 20 * slam, 0, 0);
                p.head.xRot -= 0.5f * raise;
                jaw.xRot += 0.5f * raise;
                p.lift(-4 * raise);
            }
            case VarkhulBoss.TOMB -> {
                float aim = Anim.hold(c.t, 2, 20, 4);
                float clench = Anim.hold(c.t, 20, 28, 3);
                p.armL(-95 * aim - 60 * clench, 0, 0, 30 * clench);
                p.armR(-50 * aim, 20 * aim, 0, -20 * aim);
                staff.xRot -= 0.9f * aim;
                p.spine(0, -20 * aim, 0);
                p.lHand.xRot += 0.5f * clench;
            }
            case VarkhulBoss.HARVEST -> {
                float k = Anim.hold(c.t, 8, 66, 8);
                p.armR(-60 * k, 0, 80 * k, 0);
                p.armL(-60 * k, 0, -80 * k, 0);
                p.spine(-25 * k, 0, 0);
                p.head.xRot -= 0.6f * k;
                jaw.xRot += 0.7f * k + Mth.sin(c.t * 0.5f) * 0.1f * k;
                p.lift(-6 * k);
                orbit.yRot += c.t * 0.15f * k;
            }
            case SiegeBoss.ANIM_INTRO -> {
                // a frozen statue that cracks free, spreads its arms and rises
                float statue = 1 - Anim.smooth(Anim.win(c.t, 16, 10));
                p.spine(40 * statue, 0, 0);
                p.armR(-60 * statue, 40 * statue, 0, 110 * statue);
                p.armL(-60 * statue, -40 * statue, 0, 110 * statue);
                p.head.xRot += 0.6f * statue;
                float spread = Anim.hold(c.t, 26, 44, 6);
                p.armR(-40 * spread, 0, 60 * spread, 0);
                p.armL(-140 * spread, 0, -40 * spread, 0);
                staff.xRot -= 0.6f * spread;
                p.head.xRot -= 0.5f * spread;
                jaw.xRot += 0.8f * Anim.pulse(c.t, 30, 18);
                p.lift(-6 * Anim.smooth(Anim.win(c.t, 30, 26)) + 6);
            }
            default -> {
            }
        }
        if (c.staggered) {
            float k = Anim.hold(c.t, 0, 62, 6);
            p.spine(30 * k, 0, 18 * k);
            p.armR(30 * k, 0, 20 * k, -40 * k);
            p.armL(30 * k, 0, -30 * k, 0);
            staff.zRot += 0.5f * k;
            p.lift(6 * k);
            p.head.zRot += Mth.sin(c.age * 0.4f) * 0.12f * k;
        }
        if (c.death > 0) {
            float k = Anim.smooth(Math.min(1, c.death / 30f));
            float k2 = Anim.smooth(Math.min(1, Math.max(0, c.death - 20) / 26f));
            p.spine(-30 * k + 70 * k2, 0, 0);
            p.armR(-100 * k + 120 * k2, 0, 60 * k, 0);
            p.armL(-100 * k + 120 * k2, 0, -60 * k, 0);
            p.head.xRot -= 0.8f * k - 1.2f * k2;
            jaw.xRot += 0.9f * k;
            p.lift(14 * K * k2);
            ModelPart robe = m.p("robe");
            robe.yScale = 1 - 0.7f * k2;
            orbit.xScale = orbit.yScale = orbit.zScale = Math.max(0.01f, 1 - k);
        }
    }
}
