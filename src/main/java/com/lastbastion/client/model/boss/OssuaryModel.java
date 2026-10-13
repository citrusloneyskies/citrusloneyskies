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
import com.lastbastion.entity.boss.OssuaryBoss;
import com.lastbastion.entity.boss.SiegeBoss;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * The Ossuary Colossus: a hunched giant of fused bone. A ram-horned skull with a hinged jaw and soul-lit
 * sockets, shoulders heaped with skulls, a ribcage of individual ribs over a burning soul-furnace,
 * vertebral ridges and old spears still lodged in its back, manacled wrists trailing broken chains,
 * massive knuckled fists and a tattered burial shroud. In phase 2 the front ribs swing open like
 * doors and the furnace blazes.
 */
public final class OssuaryModel implements BossModels.Def {
    private static final float K = 1.75f;

    @Override
    public float scale() {
        return 2f;
    }

    @Override
    public float shadow() {
        return 2.6f;
    }

    @Override
    public void build(ModelSpec s) {
        int soul = OssuaryBoss.SOUL;
        Paint bone = p(Mat.BONE, 0xD8CCB0).accent(0x8A7E66).grime(0.35f).pattern(Pattern.CRACKS, 0x7A6E58, 1.4f);
        Paint boneDark = p(Mat.BONE, 0xB8AC90).accent(0x6A5E48).grime(0.5f);
        Paint fused = p(Mat.BONE, 0x5A5048).accent(0x2A241E).pattern(Pattern.CRACKS, soul, 1.1f).accentGlow();
        Paint skull = p(Mat.BONE, 0xE0D6BC).accent(0x8A7E66).grime(0.3f)
                .decal(Face.FRONT, DecalType.HOLLOW_EYES, soul, true, 0.5f, 0.46f, 1.25f)
                .decal(Face.FRONT, DecalType.NOSE, 0x2A2018, false, 0.5f, 0.72f, 1f)
                .decal(Face.TOP, DecalType.SCAR, 0x6A5E48, false, 0.4f, 0.4f, 1f);
        Paint jawP = p(Mat.BONE, 0xD8CCB0).accent(0x8A7E66).decal(Face.FRONT, DecalType.FANGS, 0xF0E8D0, false, 0.5f, 0.3f, 1.2f);
        Paint skulls = p(Mat.BONE, 0xD0C4A8).accent(0x7A6E58).grime(0.4f)
                .decal(Face.FRONT, DecalType.SKULL, 0x3A3028, false, 0.5f, 0.5f, 1.1f)
                .decal(Face.LEFT, DecalType.SKULL, 0x3A3028, false, 0.5f, 0.5f, 1.1f)
                .decal(Face.RIGHT, DecalType.SKULL, 0x3A3028, false, 0.5f, 0.5f, 1.1f)
                .decal(Face.TOP, DecalType.SKULL, 0x3A3028, false, 0.5f, 0.5f, 1.1f);
        Paint iron = p(Mat.DARK_METAL, 0x3A3632).accent(0x1E1A16).grime(0.6f).pattern(Pattern.SPOTS, 0x6A3A1A, 1.5f);
        Paint chain = p(Mat.METAL, 0x5A5A5E).pattern(Pattern.CHAINMAIL, 0x8A8A90, 2f).grime(0.5f);
        Paint shroudP = p(Mat.CLOTH, 0x6A6458).accent(0x3A362E).grime(0.6f).pattern(Pattern.CLOTH, 0x4A4438, 2f);
        Paint earth = p(Mat.STONE, 0x5A4E3E).accent(0x3A3226).grime(0.5f);
        Paint furnace = Paint.glowing(soul).pattern(Pattern.VEINS, 0xFFFFFF, 0.8f);
        Paint flame = Paint.glowing(soul).accent(0xE0FFF0);
        Paint rust = p(Mat.METAL, 0x6A5A4A).accent(0x3A2A1A).grime(0.7f);
        Paint wood = p(Mat.WOOD, 0x4A3A2A).accent(0x2A1E14).grime(0.5f);

        Body.Spec sp = new Body.Spec(bone, bone).scale(K);
        sp.legs(6, 6, 4.6f);
        sp.foot = 1.4f;
        sp.hipW = 9;
        sp.torso(15, 10, 9, 5.5f, 5, 4);
        sp.neck = 1.2f;
        sp.head(6, 5.6f, 6.4f);
        sp.arms(8.5f, 8, 4.2f);
        sp.hand = 3.5f;
        sp.paints(fused, boneDark, bone, bone, bone, bone, bone, bone);
        sp.headPaint(skull);
        sp.neckP = boneDark;
        Body b = Body.build(s.root, sp);
        // hunched: the head hangs low in front of the shoulders
        b.neck.pz -= 3.2f * K;
        b.neck.py += 2.2f * K;

        float cw = 15 * K, ch = 10 * K, cd = 9 * K;
        // pelvic girdle, vertebrae and burial shroud
        b.pelvis.box(-4.6f * K, -1.6f * K, -3 * K, 9.2f * K, 3 * K, 6 * K, bone);
        for (int i = 0; i < 3; i++) b.pelvis.box(-3.4f * K, -4.6f * K + i * 1.6f * K, -2.6f * K, 6.8f * K, 0.9f * K, 5.2f * K, boneDark);
        Part shroud = b.pelvis.child("shroud", 0, 1.2f * K, -3.1f * K);
        float[] sl = {6, 8, 5, 9, 7};
        for (int i = 0; i < sl.length; i++) shroud.box(-4.4f * K + i * 1.76f * K, 0, -0.3f * K, 1.8f * K, sl[i] * K, 0.5f * K, shroudP);
        Part shroudB = b.pelvis.child("shroud_b", 0, 1.2f * K, 3.1f * K);
        for (int i = 0; i < sl.length; i++) shroudB.box(-4.4f * K + i * 1.76f * K, 0, -0.2f * K, 1.8f * K, sl[sl.length - 1 - i] * K, 0.5f * K, shroudP);

        // ribcage: static side ribs, two front "doors" of ribs over the furnace, sternum
        for (int i = 0; i < 5; i++) {
            float y = -8.8f * K + i * 1.8f * K;
            for (int side = -1; side <= 1; side += 2) {
                float x = side < 0 ? -cw / 2 - 0.8f * K : cw / 2 - 0.1f * K;
                b.chest.box(x, y, -cd / 2 - 0.6f * K, 0.9f * K, 1.0f * K, cd + 0.4f * K, bone);
            }
        }
        b.chest.child("furnace", 0, -5 * K, -cd / 2 + 1 * K).sphere(0, 0, 0, 2.1f * K, furnace);
        Part blaze = b.chest.child("furnace2", 0, -5 * K, -cd / 2 + 0.4f * K).from(2);
        blaze.sphere(0, 0, 0, 3 * K, furnace);
        for (int i = 0; i < 5; i++) blaze.spike((i - 2) * 1.1f * K, -2.4f * K, -0.6f * K, 1.0f * K, (2.4f + (i % 2) * 1.6f) * K, 0, 0, (i - 2) * 10, flame);
        for (int side = -1; side <= 1; side += 2) {
            Part door = b.chest.child(side < 0 ? "ribs_r" : "ribs_l", side * (cw / 2), 0, -cd / 2 - 0.6f * K);
            for (int i = 0; i < 5; i++) {
                float y = -8.8f * K + i * 1.8f * K;
                Part rib = door.sub(0, y + 0.5f * K, 0).rot(0, 0, -side * 9);
                float len = cw / 2 - 0.9f * K;
                rib.box(side < 0 ? 0 : -len, -0.5f * K, -0.9f * K, len, 1.0f * K, 0.9f * K, bone);
            }
            if (side < 0) door.box(cw / 2 - 0.9f * K, -9.2f * K, -1.1f * K, 1.8f * K, 8.6f * K, 1.1f * K, boneDark);
        }
        // spine ridge, lodged spears and a broken sword
        for (int i = 0; i < 5; i++) {
            float y = -9 * K + i * 2 * K;
            b.chest.box(-1.4f * K, y, cd / 2 - 0.2f * K, 2.8f * K, 1.4f * K, 1.4f * K, boneDark);
            b.chest.spike(0, y + 0.6f * K, cd / 2 + 1 * K, 1.2f * K, (2.6f - i * 0.3f) * K, -60, 0, 0, bone);
        }
        Part spear1 = b.chest.sub(-4 * K, -7 * K, cd / 2).rot(-50, 20, -15);
        spear1.box(-0.3f * K, -14 * K, -0.3f * K, 0.6f * K, 14 * K, 0.6f * K, wood);
        spear1.spike(0, -14 * K, 0, 0.9f * K, 2.2f * K, 0, 0, 0, rust);
        Part spear2 = b.chest.sub(5 * K, -4 * K, cd / 2).rot(-35, -25, 20);
        spear2.box(-0.3f * K, -11 * K, -0.3f * K, 0.6f * K, 11 * K, 0.6f * K, wood);
        spear2.box(-1.2f * K, -12.5f * K, -0.1f * K, 2.4f * K, 1.6f * K, 0.2f * K, p(Mat.CLOTH, 0x7A2A20).grime(0.5f));
        Part sword = b.chest.sub(2 * K, -9.5f * K, cd / 2 - 1 * K).rot(-25, 0, 35);
        sword.box(-0.15f * K, -7 * K, -0.7f * K, 0.3f * K, 7 * K, 1.4f * K, rust);
        sword.box(-1.6f * K, -7.6f * K, -0.3f * K, 3.2f * K, 0.6f * K, 0.6f * K, iron);

        // shoulders heaped with skulls, big shoulder blades
        for (int side = -1; side <= 1; side += 2) {
            Part arm = side < 0 ? b.rArm : b.lArm;
            Part heap = arm.sub(side * 0.6f * K, -2.4f * K, 0);
            heap.box(-2.6f * K, -1.8f * K, -2.6f * K, 5.2f * K, 2 * K, 5.2f * K, boneDark);
            heap.box(-2.4f * K + side * 0.6f * K, -4 * K, -2.2f * K, 2.2f * K, 2.2f * K, 2.2f * K, skulls);
            heap.box(0.2f * K, -3.8f * K, 0.4f * K, 2.1f * K, 2.1f * K, 2.1f * K, skulls);
            heap.box(-1.2f * K, -3.6f * K, 0.8f * K + 0 * K, 2 * K, 2 * K, 2 * K, skulls);
            heap.box(-1 * K - side * 1 * K, -5.8f * K, -1 * K, 2 * K, 2 * K, 2 * K, skulls);
            heap.spike(side * 2.6f * K, -2 * K, 0, 1.4f * K, 3.4f * K, 0, 0, side * -55, bone);
            Part fore = side < 0 ? b.rFore : b.lFore;
            fore.box(side * 1.6f * K - 0.8f * K, 0, -0.9f * K, 1.6f * K, 8 * K, 1.8f * K, boneDark);
            fore.box(-2.6f * K, 5.6f * K, -2.6f * K, 5.2f * K, 1.6f * K, 5.2f * K, iron);
            Part ch2 = fore.child(side < 0 ? "chain_r" : "chain_l", 0, 6.6f * K, 2.4f * K);
            for (int i = 0; i < 5; i++) ch2.box(-0.4f * K, i * 1.3f * K, -0.4f * K, 0.8f * K, 1.1f * K, 0.8f * K, chain);
            Part hand = side < 0 ? b.rHand : b.lHand;
            hand.box(-2.8f * K, -0.2f * K, -2.8f * K, 5.6f * K, 4.2f * K, 5.6f * K, bone);
            for (int i = 0; i < 4; i++) hand.box(-2.6f * K + i * 1.35f * K, 3.2f * K, -3.2f * K, 1.2f * K, 1.4f * K, 1.2f * K, boneDark);
            hand.spike(side * -2.6f * K, 1.6f * K, -2.4f * K, 1.0f * K, 2 * K, -60, 0, side * 40, bone);
            Part thigh = side < 0 ? b.rThigh : b.lThigh;
            thigh.box(-2.6f * K, 4.4f * K, -2.9f * K, 5.2f * K, 2.2f * K, 2 * K, boneDark);
            Part shin = side < 0 ? b.rShin : b.lShin;
            shin.box(-2.5f * K, 1.6f * K, -2.5f * K, 5 * K, 1.2f * K, 5 * K, iron);
            shin.box(-2.5f * K, 3.6f * K, -2.5f * K, 5 * K, 0.9f * K, 5 * K, chain);
            Part foot = side < 0 ? b.rFoot : b.lFoot;
            foot.box(-2.8f * K, -0.4f * K, -4.2f * K, 5.6f * K, 1.8f * K, 3 * K, earth);
            for (int i = -1; i <= 1; i++) foot.spike(i * 1.6f * K, 0.6f * K, -4.2f * K, 1.1f * K, 2.2f * K, 80, 0, 0, bone);
        }
        // the skull: brow ridge, ram horns, a crown of bone spikes and a hinged jaw
        float hw = 6 * K, hh = 5.6f * K, hd = 6.4f * K;
        b.head.box(-hw / 2 - 0.3f * K, -hh * 0.62f, -hd / 2 - 0.5f * K, hw + 0.6f * K, 1 * K, 1.2f * K, boneDark);
        for (int side = -1; side <= 1; side += 2) {
            Part h1 = b.head.spikePart(side * hw / 2, -hh * 0.7f, 0.5f * K, 2.4f * K, 4.6f * K, -30, 0, side * 85, boneDark);
            Part h2 = h1.spikePart(0, -4.4f * K, 0, 1.6f * K, 4 * K, 70, 0, 0, boneDark);
            h2.spike(0, -3.8f * K, 0, 1 * K, 3 * K, 60, 0, side * 20, bone);
        }
        for (int i = 0; i < 5; i++) b.head.spike((i - 2) * 1.2f * K, -hh, (Math.abs(i - 2) * 0.6f - 0.4f) * K, 0.9f * K, (2.8f - Math.abs(i - 2) * 0.7f) * K, -10, 0, (i - 2) * 12, bone);
        Part jaw = b.head.child("jaw", 0, -0.6f * K, -0.4f * K);
        jaw.box(-hw / 2 + 0.3f * K, 0, -hd / 2 - 0.2f * K, hw - 0.6f * K, 1.8f * K, hd * 0.8f, jawP);
        b.neck.box(-1.6f * K, -1.2f * K, -1.6f * K, 3.2f * K, 3.4f * K, 3.2f * K, boneDark);

        // the boulder it tears from the ground (shown only while throwing)
        Part boulder = b.rHand.child("boulder", 5 * K, 5 * K, -3 * K);
        boulder.sphere(0, 0, 0, 4.2f * K, p(Mat.STONE, 0xB8AC94).accent(0x6A5E48).pattern(Pattern.CRACKS, 0x5A5040, 1.5f)
                .decal(Face.FRONT, DecalType.SKULL, 0x5A5040, false, 0.4f, 0.5f, 1f));
        boulder.spike(0, -3 * K, 0, 1.4f * K, 2.4f * K, 20, 0, 30, bone);
    }

    @Override
    public void animate(ProcModel<SiegeBoss> m, SiegeBoss b, BossModels.Ctx c) {
        Poser p = Poser.of(m);
        ModelPart jaw = m.p("jaw"), furnace = m.p("furnace"), ribsR = m.p("ribs_r"), ribsL = m.p("ribs_l");
        ModelPart chainR = m.p("chain_r"), chainL = m.p("chain_l"), shroud = m.p("shroud"), shroudB = m.p("shroud_b");
        ModelPart boulder = m.p("boulder");
        boulder.visible = c.id == OssuaryBoss.BOULDER && c.t >= 12 && c.t < 26;
        float move = Math.min(1, c.la);
        p.idle(c.age, 1.6f);
        p.walk(c.ls, c.la, 0.65f, 0, 2f);
        // knuckle-dragging gait: arms swing opposite the legs from a forward hang
        float ph = c.ls * 0.6662f;
        p.armR(-12 + Mth.cos(ph) * 22 * move, 0, 8, 25);
        p.armL(-12 - Mth.cos(ph) * 22 * move, 0, -8, 25);
        p.pelvis.zRot += Mth.sin(ph) * 0.06f * move;
        p.spine(16, 0, 0);
        p.look(c.headYaw, c.headPitch);
        p.head.xRot -= 0.2f;
        p.flinch(c.hurt * 0.5f);
        float breathe = Mth.sin(c.age * 0.08f);
        furnace.xScale = furnace.yScale = furnace.zScale = 1 + breathe * 0.07f;
        jaw.xRot += 0.06f + Math.max(0, breathe) * 0.05f;
        chainR.xRot += -p.rArm.xRot * 0.7f + Mth.sin(c.age * 0.1f) * 0.1f;
        chainL.xRot += -p.lArm.xRot * 0.7f + Mth.sin(c.age * 0.1f + 2) * 0.1f;
        shroud.xRot -= Math.max(0, -Math.min(p.rThigh.xRot, p.lThigh.xRot)) * 0.8f;
        shroudB.xRot += Math.max(0, Math.max(p.rThigh.xRot, p.lThigh.xRot)) * 0.5f;
        if (c.phase >= 2) {
            float flick = 0.45f + Mth.sin(c.age * 0.3f) * 0.05f;
            ribsR.yRot -= flick;
            ribsL.yRot += flick;
            ModelPart blaze = m.p("furnace2");
            blaze.xScale = blaze.zScale = 1 + Mth.sin(c.age * 0.5f) * 0.06f;
            blaze.yScale = 1 + Mth.sin(c.age * 0.7f) * 0.1f;
        }
        switch (c.id) {
            case SiegeBoss.ANIM_MELEE -> backhand(p, c.t);
            case SiegeBoss.ANIM_CORE, OssuaryBoss.SMASH -> {
                float len = c.id == OssuaryBoss.SMASH ? 28 : 18;
                float up = c.t < len ? Anim.smooth(c.t / len) : 0;
                float slam = Anim.hold(c.t, len, len + 14, 5);
                p.armR(-170 * up - 60 * slam, 25 * up, 10 * up, 20 * up);
                p.armL(-170 * up - 60 * slam, -25 * up, -10 * up, 20 * up);
                p.spine(-20 * up + 45 * slam, 0, 0);
                p.crouch(slam * 0.6f, 8);
                jaw.xRot += 0.5f * slam;
            }
            case OssuaryBoss.BOULDER -> {
                float reach = Anim.hold(c.t, 2, 12, 4) * (c.t < 14 ? 1 : 0);
                float lift = c.t >= 12 && c.t < 26 ? Anim.smooth(Anim.win(c.t, 12, 8)) : 0;
                float thr = Anim.pulse(c.t, 24, 12);
                p.crouch(reach * 0.8f, 10);
                p.spine(45 * reach - 20 * lift + 30 * thr, 0, 0);
                p.armR(-60 * reach - 170 * lift - 40 * thr, -20 * lift, 0, 20 * reach);
                p.armL(-60 * reach - 170 * lift - 40 * thr, 20 * lift, 0, 20 * reach);
            }
            case OssuaryBoss.STOMP -> {
                float raise = c.t < 22 ? Anim.smooth(Anim.win(c.t, 2, 16)) : 0;
                float slam = Anim.hold(c.t, 22, 32, 6);
                p.legR(-75 * raise - 10 * slam, 0, 70 * raise);
                p.legL(5 * raise, 0, 15 * raise);
                p.spine(-12 * raise + 24 * slam, 0, 8 * raise);
                p.armR(-50 * raise, 0, 50 * raise, 30);
                p.armL(-30 * raise, 0, -60 * raise, 30);
                p.crouch(slam * 0.45f, 6);
                jaw.xRot += 0.4f * slam;
            }
            case OssuaryBoss.SPIKES -> {
                float raise = c.t < 16 ? Anim.smooth(Anim.win(c.t, 0, 14)) : 0;
                float down = Anim.hold(c.t, 16, 36, 5);
                p.armR(-160 * raise - 70 * down, 0, 15 * raise, 10);
                p.armL(-160 * raise - 70 * down, 0, -15 * raise, 10);
                p.spine(-15 * raise + 55 * down, 0, 0);
                p.crouch(down * 0.7f, 9);
            }
            case OssuaryBoss.SWEEP -> {
                float wind = c.t < 18 ? Anim.smooth(Anim.win(c.t, 0, 16)) : 0;
                float cut = Anim.hold(c.t, 18, 24, 6);
                p.armR(-70 * wind - 60 * cut, 70 * wind - 110 * cut, 30 * wind, 10);
                p.spine(30 * wind + 20 * cut, 40 * wind - 55 * cut, 0);
                p.crouch((wind + cut) * 0.4f, 6);
                p.armL(-20 * cut, 0, -40 * cut, 30);
            }
            case OssuaryBoss.GRASP -> {
                float reach = c.t < 16 ? Anim.smooth(Anim.win(c.t, 0, 12)) : 0;
                float hold = Anim.hold(c.t, 16, 42, 4);
                float hurl = Anim.pulse(c.t, 42, 10);
                p.armR(-65 * reach - 100 * hold - 40 * hurl, 10 * reach, 50 * hurl, 10 * reach + 50 * hold);
                p.spine(40 * reach - 10 * hold + 10 * hurl, -30 * hurl, 0);
                p.crouch(reach * 0.6f, 8);
                p.head.xRot -= 0.3f * hold;
                jaw.xRot += 0.35f * hold;
            }
            case OssuaryBoss.FURNACE -> {
                float open = Anim.hold(c.t, 4, 56, 8);
                float sweep = c.t < 22 ? -38 : c.t > 54 ? 38 : -38 + (c.t - 22) / 32f * 76;
                p.spine(-28 * open, sweep * open, 0);
                p.armR(-30 * open, 0, 70 * open, 20);
                p.armL(-30 * open, 0, -70 * open, 20);
                p.head.xRot -= 0.3f * open;
                jaw.xRot += 0.6f * open;
                ribsR.yRot -= 0.5f * open;
                ribsL.yRot += 0.5f * open;
            }
            case OssuaryBoss.RIBCAGE -> {
                float k = Anim.hold(c.t, 6, 36, 6);
                float burst = Anim.win(c.t, 16, 6);
                p.spine(-30 * k, 0, 0);
                p.armR(-60 * k, 0, 90 * k, 40);
                p.armL(-60 * k, 0, -90 * k, 40);
                p.head.xRot -= 0.7f * k;
                jaw.xRot += 0.9f * k;
                ribsR.yRot -= 0.7f * burst * k;
                ribsL.yRot += 0.7f * burst * k;
            }
            case SiegeBoss.ANIM_INTRO -> {
                // claws its way out of the earth, then roars
                float rise = Anim.smooth(Anim.win(c.t, 0, 44));
                p.lift((1 - rise) * 30 * K);
                float claw = (1 - rise) * Mth.sin(c.t * 0.35f);
                p.armR(-150 + 40 * claw + 150 * rise, 0, 20, 30);
                p.armL(-150 - 40 * claw + 150 * rise, 0, -20, 30);
                p.spine(40 * (1 - rise), 0, 0);
                float roar = Anim.hold(c.t, 46, 58, 4);
                p.spine(-25 * roar, 0, 0);
                p.armR(-40 * roar, 0, 70 * roar, 0);
                p.armL(-40 * roar, 0, -70 * roar, 0);
                p.head.xRot -= 0.6f * roar;
                jaw.xRot += 1f * roar;
            }
            default -> {
            }
        }
        if (c.staggered) {
            float k = Anim.hold(c.t, 0, 62, 8);
            p.crouch(0.8f * k, 10);
            p.legL(-60 * k, 0, 80 * k);
            p.armR(-50 * k, 0, 10 * k, 0);
            p.spine(35 * k, 0, -10 * k);
            p.head.zRot += Mth.sin(c.age * 0.3f) * 0.15f * k;
            jaw.xRot += 0.5f * k;
        }
        if (c.death > 0) {
            float k = Anim.smooth(Math.min(1, c.death / 26f));
            float k2 = Anim.smooth(Math.min(1, Math.max(0, c.death - 14) / 22f));
            p.crouch(k, 18);
            p.spine(50 * k2, 0, 10 * k2);
            p.armR(40 * k, 0, 50 * k2, 0);
            p.armL(40 * k, 0, -60 * k2, 0);
            ribsR.yRot -= 1.1f * k2;
            ribsL.yRot += 1.1f * k2;
            // the skull tumbles from its shoulders
            p.head.y += 16 * K * k2;
            p.head.z -= 6 * K * k2;
            p.head.zRot += 1.2f * k2;
            p.head.xRot += 0.9f * k2;
            jaw.xRot += 0.8f * k;
            furnace.xScale = furnace.yScale = furnace.zScale = Math.max(0.05f, 1 - k2);
        }
    }

    /** Heavy backhand swipe for ordinary melee. */
    private static void backhand(Poser p, float t) {
        float wind = t < 14 ? Anim.smooth(t / 14f) : 0;
        float hit = Anim.hold(t, 14, 20, 5);
        p.armR(-60 * wind - 50 * hit, 60 * wind - 90 * hit, 20 * wind, 20);
        p.spine(10 * wind + 15 * hit, 30 * wind - 40 * hit, 0);
    }
}
