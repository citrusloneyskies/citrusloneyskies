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
import com.lastbastion.client.model.hero.Gear;
import com.lastbastion.entity.boss.MalgraveBoss;
import com.lastbastion.entity.boss.SiegeBoss;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Malgrave the Dread Knight: blackened, spiked paladin plate with a defaced sun on the breastplate
 * cracked by violet corruption, a horned great helm with a burning T-visor, a tattered crimson cape
 * and a rune-channelled greatsword. Phase 2 ignites the blade, visor and cape hem in soulfire and
 * grows a crown of cold flame.
 */
public final class MalgraveModel implements BossModels.Def {
    private static final float K = 1.38f;

    @Override
    public float shadow() {
        return 1.1f;
    }

    @Override
    public int density() {
        return 4;
    }

    @Override
    public void build(ModelSpec s) {
        int violet = MalgraveBoss.SHADOW, soul = MalgraveBoss.SOULFIRE;
        Paint plate = p(Mat.DARK_METAL, 0x2C2A34).accent(0x141218).trim(0x7A5A2A, 0.8f).gloss(0.55f);
        Paint plateCracked = plate.copy().pattern(Pattern.CRACKS, violet, 1.2f).accentGlow();
        Paint breast = p(Mat.DARK_METAL, 0x2E2C36).accent(0x141218).trim(0x7A5A2A, 0.8f).gloss(0.55f)
                .decal(Face.FRONT, DecalType.SUN, 0x7A6A3A, false, 0.5f, 0.4f, 1.2f)
                .decal(Face.FRONT, DecalType.SCAR, violet, true, 0.5f, 0.42f, 1.3f)
                .decal(Face.BACK, DecalType.PANEL, 0, false, 0.5f, 0.5f, 1f);
        Paint helm = p(Mat.DARK_METAL, 0x2A2832).accent(0x121016).trim(0x7A5A2A, 0.6f).gloss(0.6f)
                .decal(Face.FRONT, DecalType.VISOR, violet, true, 0.5f, 0.45f, 1f)
                .decal(Face.FRONT, DecalType.GRILLE, 0x0A0A0E, false, 0.5f, 0.78f, 0.6f);
        Paint mail = p(Mat.METAL, 0x46464E).pattern(Pattern.CHAINMAIL, 0x6A6A74, 2f);
        Paint gold = p(Mat.GOLD, 0x8A6A2A).accent(0x5A4418).grime(0.4f);
        Paint leather = p(Mat.LEATHER, 0x2A1E18).accent(0x16100C);
        Paint cape = p(Mat.CLOTH, 0x6A0E18).accent(0x3A0610).grime(0.45f).pattern(Pattern.CLOTH, 0x500A12, 2f);
        Paint steel = p(Mat.METAL, 0x5A5C66).accent(0x2A2C34).gloss(0.8f);
        Paint edge = p(Mat.METAL, 0xB8BCC8).gloss(0.9f);
        Paint runes = Paint.glowing(violet).pattern(Pattern.RUNES, 0xFFFFFF, 1.5f);
        Paint soulGlow = Paint.glowing(soul).pattern(Pattern.RUNES, 0xE0FFFF, 1.5f);
        Paint soulFlame = Paint.glowing(soul).accent(0xE0FFFF);
        Paint spikeP = p(Mat.DARK_METAL, 0x24222A).gloss(0.6f);

        Body.Spec sp = new Body.Spec(plate, plate).scale(K);
        sp.legs(6, 6.2f, 4.2f);
        sp.foot = 1.6f;
        sp.hipW = 8.4f;
        sp.torso(10, 7.5f, 5.4f, 7.4f, 5, 4.6f);
        sp.neck = 1;
        sp.head(6.6f);
        sp.arms(5.6f, 5.6f, 3.9f);
        sp.hand = 2.4f;
        sp.paints(breast, mail, plate, plate, plate, plate, plate, plate);
        sp.headPaint(helm);
        sp.neckP = mail;
        Body b = Body.build(s.root, sp);

        float cw = 10 * K, ch = 7.5f * K, cd = 5.4f * K, hs = 6.6f * K;
        // gorget, plackart and faulds
        b.chest.box(-cw * 0.3f, -ch - 0.6f * K, -cd * 0.5f - 0.3f * K, cw * 0.6f, 1.8f * K, cd + 0.6f * K, plate);
        b.chest.box(-cw * 0.42f, -ch * 0.35f, -cd * 0.5f - 0.5f * K, cw * 0.84f, ch * 0.35f, 0.6f * K, plateCracked);
        Gear.belt(b.pelvis, 7.4f * K, 4.6f * K, -1.6f * K, 1.4f * K, leather, gold);
        Part faulds = b.pelvis.child("faulds", 0, -0.4f * K, -2.7f * K);
        faulds.box(-3.4f * K, 0, -0.4f * K, 6.8f * K, 4.6f * K, 0.8f * K, plate);
        faulds.box(-2.8f * K, 4.4f * K, -0.3f * K, 5.6f * K, 2.2f * K, 0.6f * K, cape);
        for (int side = -1; side <= 1; side += 2) {
            Part thigh = side < 0 ? b.rThigh : b.lThigh;
            thigh.box(side * 0.4f * K - 2.6f * K, 0, -2.6f * K, 5.2f * K, 4 * K, 5.2f * K, plate);
            Part shin = side < 0 ? b.rShin : b.lShin;
            shin.box(-2.3f * K, -0.6f * K, -2.6f * K, 4.6f * K, 2.4f * K, 2.4f * K, plate);
            shin.spike(0, 0.6f * K, -2.6f * K, 1.2f * K, 2.4f * K, 70, 0, 0, spikeP);
            shin.box(-2.2f * K, 2.2f * K, -2.3f * K, 4.4f * K, 3.6f * K, 4.6f * K, plate);
            Part foot = side < 0 ? b.rFoot : b.lFoot;
            foot.box(-2.2f * K, -0.2f * K, -3.8f * K, 4.4f * K, 1.7f * K, 1.4f * K, plate);
            // pauldrons: three stacked lames with a row of spikes
            Part arm = side < 0 ? b.rArm : b.lArm;
            Part pd = Gear.pauldron(arm, side, 3.9f * K, plate, gold);
            pd.box(-3.6f * K, -2.4f * K, -3.2f * K, 7.2f * K, 1.4f * K, 6.4f * K, plate);
            for (int i = -1; i <= 1; i++) pd.spike(side * 1.4f * K, -2.4f * K, i * 2 * K, 1.1f * K, (3.2f - Math.abs(i) * 0.8f) * K, i * -12, 0, side * -28, spikeP);
            Part fore = side < 0 ? b.rFore : b.lFore;
            fore.box(-2.3f * K, 1.4f * K, -2.3f * K, 4.6f * K, 4.2f * K, 4.6f * K, plate);
            fore.box(-2.5f * K, 3.8f * K, -2.5f * K, 5 * K, 1.4f * K, 5 * K, gold);
            fore.spike(side * 2.2f * K, 2.6f * K, 0, 0.9f * K, 1.8f * K, 0, 0, side * -90, spikeP);
            Part hand = side < 0 ? b.rHand : b.lHand;
            hand.box(-2 * K, -0.2f * K, -2 * K, 4 * K, 1.6f * K, 4 * K, plate);
        }
        // great helm: brow ridge, crest of thorns, sweeping horns, soulfire crown and visor (phase 2)
        b.head.box(-hs * 0.55f, -hs * 0.62f, -hs * 0.55f, hs * 1.1f, hs * 0.14f, hs * 0.2f, plate);
        b.head.box(-hs * 0.08f, -hs - 0.6f * K, -hs * 0.6f, hs * 0.16f, hs * 0.8f, hs * 1.2f, gold);
        for (int i = 0; i < 5; i++) b.head.spike(0, -hs - 0.4f * K, -hs * 0.4f + i * hs * 0.2f, 0.9f * K, (1.4f + (i == 2 ? 1.2f : 0.5f * (2 - Math.abs(i - 2)))) * K, -15 + i * 8, 0, 0, spikeP);
        for (int side = -1; side <= 1; side += 2) {
            Part h1 = b.head.spikePart(side * hs * 0.5f, -hs * 0.68f, hs * 0.1f, 1.6f * K, 4.2f * K, 10, 0, side * 72, spikeP);
            h1.spike(0, -4.2f * K, 0, 1 * K, 3.6f * K, 0, 0, side * -60, p(Mat.BONE, 0x3A3438).gloss(0.4f));
        }
        Part visor = b.head.child("visor2", 0, -hs * 0.55f, -hs * 0.5f - 0.15f * K).from(2);
        visor.box(-hs * 0.32f, 0, -0.12f * K, hs * 0.64f, hs * 0.1f, 0.24f * K, soulFlame);
        visor.box(-hs * 0.05f, 0, -0.12f * K, hs * 0.1f, hs * 0.32f, 0.24f * K, soulFlame);
        Part crown = b.head.child("crown", 0, -hs - 0.6f * K, 0).from(2);
        for (int i = 0; i < 7; i++) {
            float a = i * Mth.TWO_PI / 7;
            crown.spike(Mth.cos(a) * hs * 0.42f, 0, Mth.sin(a) * hs * 0.42f, 1 * K, (2.2f + (i % 2) * 1.4f) * K, Mth.sin(a) * -20, 0, Mth.cos(a) * 20, soulFlame);
        }
        // tattered cape: two segments, the lower one torn into uneven strips with a burning hem in phase 2
        Part c1 = b.chest.child("cape", 0, -ch + 0.4f * K, cd / 2 + 0.2f * K);
        c1.box(-cw * 0.46f, 0, 0, cw * 0.92f, 12 * K, 0.6f * K, cape);
        Part c2 = c1.child("cape2", 0, 12 * K, 0);
        float[] lens = {8, 11, 7, 12, 9, 10, 6};
        float sw = cw * 0.96f / lens.length;
        for (int i = 0; i < lens.length; i++) {
            float x = -cw * 0.48f + i * sw;
            c2.box(x, 0, 0, sw + 0.1f, lens[i] * K, 0.6f * K, cape);
        }
        Part hem = c2.child("hem", 0, 0, 0).from(2);
        for (int i = 0; i < lens.length; i++) {
            float x = -cw * 0.48f + i * sw;
            hem.box(x + 0.2f, lens[i] * K - 1.2f * K, -0.1f * K, sw - 0.3f, 1.4f * K, 0.8f * K, soulFlame);
        }
        // greatsword: leather grip, skull pommel, winged crossguard, long blade with a glowing rune fuller
        Part sword = b.rHand.child("sword", 0, 1.4f * K, 0);
        sword.box(-0.55f * K, -0.55f * K, -2.5f * K, 1.1f * K, 1.1f * K, 6.5f * K, leather);
        sword.box(-0.8f * K, -0.8f * K, 4 * K, 1.6f * K, 1.6f * K, 1.6f * K, gold);
        sword.center(0, 0, 4.8f * K, 1.0f * K, 1.0f * K, 1.0f * K, Paint.glowing(violet));
        sword.box(-0.7f * K, -4.2f * K, -3.6f * K, 1.4f * K, 8.4f * K, 1.2f * K, steel);
        for (int side = -1; side <= 1; side += 2) sword.spike(0, side * 4.2f * K, -3 * K, 1 * K, 2 * K, side < 0 ? 40 : 140, 0, 0, spikeP);
        sword.center(0, 0, -3 * K, 1.6f * K, 1.6f * K, 1.6f * K, gold);
        sword.box(-0.32f * K, -1.4f * K, -30 * K, 0.64f * K, 2.8f * K, 26.4f * K, steel);
        sword.box(-0.36f * K, -1.5f * K, -30 * K, 0.72f * K, 0.4f * K, 26.4f * K, edge);
        sword.box(-0.36f * K, 1.1f * K, -30 * K, 0.72f * K, 0.4f * K, 26.4f * K, edge);
        sword.box(-0.4f * K, -0.35f * K, -27 * K, 0.8f * K, 0.7f * K, 22 * K, runes);
        sword.sub(0, 0, -30 * K).rot(90, 0, 0).taper(0, 0, 0, 0.64f * K, 2.8f * K, 0.12f * K, 0.3f * K, 4.4f * K, 4, edge);
        Part fire = sword.child("soulblade", 0, 0, 0).from(2);
        fire.box(-0.45f * K, -0.4f * K, -27.5f * K, 0.9f * K, 0.8f * K, 23 * K, soulGlow);
        for (int i = 0; i < 9; i++) {
            float z = -6 * K - i * 2.6f * K;
            float h = (1.2f + (i % 3) * 0.7f) * K;
            fire.box(-0.25f * K, -1.5f * K - h, z - 0.6f * K, 0.5f * K, h, 1.2f * K, soulFlame);
            fire.box(-0.25f * K, 1.5f * K, z - 1.8f * K, 0.5f * K, h * 0.7f, 1.0f * K, soulFlame);
        }
    }

    @Override
    public void animate(ProcModel<SiegeBoss> m, SiegeBoss b, BossModels.Ctx c) {
        Poser p = Poser.of(m);
        ModelPart sword = m.p("sword"), cape = m.p("cape"), cape2 = m.p("cape2"), faulds = m.p("faulds");
        float move = Math.min(1, c.la);
        p.idle(c.age, 1f);
        p.walk(c.ls, c.la, 0.9f, 0.15f, 1.1f);
        p.look(c.headYaw, c.headPitch);
        p.flinch(c.hurt);
        // two-handed high guard: blade angled up and forward over the right shoulder
        p.armR(-38, -12, 4, 62);
        p.armL(-44, 34, -6, 70);
        sword.xRot += 0.35f;
        if (move < 0.3f && c.id == 0) p.stance(0.5f * (1 - move));
        cape.xRot += 0.08f + move * 0.45f + Mth.sin(c.age * 0.09f) * 0.04f;
        cape2.xRot += move * 0.25f + Mth.sin(c.age * 0.11f + 1) * 0.06f;
        faulds.xRot -= Math.max(0, -Math.min(p.rThigh.xRot, p.lThigh.xRot)) * 0.6f;
        if (c.phase >= 2) {
            ModelPart fire = m.p("soulblade"), crown = m.p("crown");
            fire.yScale = 1 + Mth.sin(c.age * 0.9f) * 0.08f;
            crown.yScale = 1 + Mth.sin(c.age * 0.7f) * 0.12f;
            crown.yRot = c.age * 0.04f;
        }
        switch (c.id) {
            case SiegeBoss.ANIM_MELEE, SiegeBoss.ANIM_CORE -> sweep(p, sword, c.t, 12, 1);
            case MalgraveBoss.COMBO -> {
                sweep(p, sword, c.t, 12, 1);
                sweep(p, sword, c.t - 12, 12, -1);
                float up = c.t >= 26 && c.t < 42 ? Anim.smooth(Anim.win(c.t, 26, 14)) : 0;
                float down = Anim.hold(c.t, 42, 50, 3);
                p.armR(-120 * up - 30 * down, 10 * up, 0, -30 * up);
                p.armL(-110 * up - 25 * down, -20 * up, 0, -20 * up);
                sword.xRot += -0.4f * up + 0.6f * down;
                p.spine(-14 * up + 34 * down, 0, 0);
                p.crouch(down * 0.5f, 4);
                p.legR(-30 * down, 0, 30 * down);
            }
            case MalgraveBoss.CHARGE -> {
                float coil = Anim.hold(c.t, 2, 14, 3) * (c.t < 15 ? 1 : 0);
                float run = Anim.hold(c.t, 15, 29, 2);
                float cut = Anim.pulse(c.t, 28, 10);
                p.crouch(coil * 0.6f, 5);
                p.spine(25 * coil + 35 * run, 0, 0);
                p.armR(40 * coil + 50 * run - 90 * cut, 20 * run + 60 * cut, 20 * run, -30 * run);
                p.armL(30 * run, -10 * run, -20 * run, 0);
                sword.xRot += 0.9f * run;
                cape.xRot += 0.9f * run;
                cape2.xRot += 0.4f * run;
                if (run > 0) p.walk(c.age * 2f, 1, 1.3f, 0, 1.2f);
            }
            case MalgraveBoss.WAVE -> {
                float lift = c.t < 19 ? Anim.smooth(Anim.win(c.t, 0, 16)) : 0;
                float plant = Anim.hold(c.t, 19, 40, 5);
                p.armR(-150 * lift - 30 * plant, 0, 10 * lift, -20 * lift);
                p.armL(-150 * lift - 30 * plant, 0, -10 * lift, -20 * lift);
                sword.xRot += 1.5f * plant - 0.5f * lift;
                p.spine(-12 * lift + 40 * plant, 0, 0);
                p.crouch(plant * 0.7f, 6);
                p.legL(-40 * plant, 0, 50 * plant);
            }
            case MalgraveBoss.SUMMON -> {
                float k = Anim.hold(c.t, 6, 32, 6);
                p.armL(-120 * k, -20 * k, -30 * k, 10 * k);
                p.armR(10 * k, 0, 10 * k, -20 * k);
                sword.xRot += 1.2f * k;
                p.head.xRot -= 0.4f * k;
                p.spine(-10 * k, 0, 0);
            }
            case MalgraveBoss.ERUPTION -> {
                float lift = c.t < 16 ? Anim.smooth(Anim.win(c.t, 0, 14)) : 0;
                float plant = Anim.hold(c.t, 16, 44, 5);
                p.armR(-160 * lift + 10 * plant, 0, 0, 0);
                p.armL(-160 * lift + 10 * plant, 0, 0, 0);
                sword.xRot += 1.57f * (lift + plant);
                p.crouch(plant * 0.8f, 7);
                p.spine(30 * plant, 0, 0);
            }
            case MalgraveBoss.GUARD -> {
                float k = Anim.hold(c.t, 3, 46, 4);
                p.armR(-55 * k, 35 * k, 0, 60 * k);
                p.armL(-55 * k, -45 * k, 0, 60 * k);
                sword.xRot += -0.9f * k;
                sword.yRot += 0.5f * k;
                p.stance(0.8f * k);
                p.crouch(0.3f * k, 3);
                p.head.xRot += 0.15f * k;
            }
            case MalgraveBoss.RIPOSTE -> {
                float pull = Anim.pulse(c.t, 0, 6);
                float thrust = Anim.hold(c.t, 4, 9, 3);
                p.armR(-60 * pull - 85 * thrust, 40 * pull, 0, 90 * pull);
                p.armL(-30 * pull, -20 * pull, 0, 40 * pull);
                sword.xRot += 1.57f * thrust;
                p.spine(10 * thrust, -30 * pull + 25 * thrust, 0);
                p.legR(-45 * thrust, 0, 20 * thrust);
                p.legL(30 * thrust, 0, 10 * thrust);
            }
            case MalgraveBoss.EXECUTE -> {
                float coil = Anim.pulse(c.t, 0, 10);
                float air = Anim.hold(c.t, 6, 24, 2);
                float plunge = Anim.hold(c.t, 25, 38, 6);
                p.crouch(coil * 0.8f + plunge * 0.9f, 7);
                p.armR(-170 * air - 20 * plunge, 0, 0, 10 * air);
                p.armL(-170 * air - 20 * plunge, 0, 0, 10 * air);
                sword.xRot += 1.57f * (air + plunge);
                p.legR(-60 * air - 70 * plunge, 0, 80 * air + 90 * plunge);
                p.legL(-20 * air, 0, 100 * air);
                p.spine(-15 * air + 40 * plunge, 0, 0);
                cape.xRot += 0.8f * air;
            }
            case MalgraveBoss.CHALLENGE -> {
                float k = Anim.hold(c.t, 6, 24, 5);
                p.armR(-85 * k, -10 * k, 0, -60 * k);
                p.armL(10 * k, 0, -10 * k, -40 * k);
                sword.xRot += 1.95f * k;
                p.spine(-6 * k, 20 * k, 0);
                p.head.xRot -= 0.1f * k;
            }
            case MalgraveBoss.ASCEND -> ascend(p, sword, c.t);
            case SiegeBoss.ANIM_INTRO -> {
                // kneeling over his planted blade, he rises and swings it to guard
                float kneel = 1 - Anim.smooth(Anim.win(c.t, 18, 14));
                kneel(p, sword, kneel);
                float flourish = Anim.pulse(c.t, 34, 14);
                p.armR(-80 * flourish, 50 * flourish, 0, 20 * flourish);
                sword.xRot += 0.6f * flourish;
            }
            default -> {
            }
        }
        if (c.staggered) {
            float k = Anim.hold(c.t, 0, 62, 6);
            p.spine(-25 * k, 0, 12 * k);
            p.armR(50 * k, 0, 30 * k, -40 * k);
            p.armL(20 * k, 0, -40 * k, 0);
            sword.xRot += 1.2f * k;
            p.legL(-30 * k, 0, 40 * k);
            p.head.xRot -= 0.3f * k;
        }
        if (c.death > 0) {
            float k = Anim.smooth(Math.min(1, c.death / 20f));
            kneel(p, sword, k);
            float fall = Anim.smooth(Math.min(1, Math.max(0, c.death - 28) / 18f));
            p.spine(30 * fall, 0, 0);
            p.head.xRot += 0.6f * k;
            p.armR(30 * fall, 0, 0, 0);
            p.armL(30 * fall, 0, 0, 0);
        }
    }

    /** Kneeling with both hands on the pommel of the planted greatsword ({@code k} 0..1). */
    private static void kneel(Poser p, ModelPart sword, float k) {
        if (k <= 0) return;
        p.pelvis.y += 9 * K * 0.45f * k;
        p.legR(-90 * k, 0, 90 * k);
        p.legL(10 * k, 0, 100 * k);
        p.lFoot.xRot -= 0.9f * k;
        p.armR(-20 * k, 20 * k, 0, 40 * k);
        p.armL(-20 * k, -20 * k, 0, 40 * k);
        sword.xRot += 1.57f * k - 0.35f * k;
        p.head.xRot += 0.35f * k;
        p.spine(12 * k, 0, 0);
    }

    /** Soulfire Ascension: kneel, then rise lifting the burning blade skyward. */
    private static void ascend(Poser p, ModelPart sword, float t) {
        float kneel = Anim.hold(t, 0, 24, 4);
        kneel(p, sword, kneel);
        float raise = Anim.hold(t, 28, 44, 4);
        p.armR(-170 * raise, 0, 6 * raise, 0);
        p.armL(-170 * raise, 0, -6 * raise, 0);
        sword.xRot += 1.57f * raise;
        p.spine(-15 * raise, 0, 0);
        p.head.xRot -= 0.5f * raise;
    }

    /** A sideways greatsword sweep: wind up on {@code side}, cut across. */
    private static void sweep(Poser p, ModelPart sword, float t, float windup, int side) {
        if (t < 0 || t > windup + 12) return;
        float wind = t < windup ? Anim.smooth(t / windup) : 0;
        float cut = Anim.hold(t, windup, windup + 5, 3);
        p.armR(-80 * wind - 70 * cut, side * (60 * wind - 80 * cut), 10 * wind, 30 * wind);
        p.armL(-70 * wind - 60 * cut, side * (50 * wind - 70 * cut), 0, 30 * wind);
        sword.xRot += 0.5f * cut;
        p.spine(6 * cut, side * (40 * wind - 45 * cut), 0);
    }
}
