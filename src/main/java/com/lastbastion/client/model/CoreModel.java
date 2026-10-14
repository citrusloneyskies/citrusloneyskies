package com.lastbastion.client.model;

import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import net.minecraft.util.Mth;

/**
 * The Siege Core monument. Origin at the bottom centre of the block, -Y up (the renderer flips it).
 * A stepped octagonal plinth with a rune band, a conduit-lit pillar, a four-clawed cradle holding the
 * great crystal, three gyroscopic rune rings and a halo of orbiting shards. Every core upgrade bolts
 * on visible hardware: buttresses and battlements (Fortify), emitter spires (Barrier), capacitor
 * coils (Pulse), gold inlay and coin hoards (Treasury), spinning cogs (Overclock) and a crown with a
 * light shaft (Beacon). Cracks spread through the crystal as the core is damaged.
 */
public final class CoreModel {
    private static ModelSpec.Baked baked;

    private CoreModel() {
    }

    public static ModelSpec.Baked baked() {
        if (baked != null) return baked;
        ModelSpec s = new ModelSpec("siege_core_monument", 4);
        Paint stone = Paint.of(Mat.STONE, 0x3A4048).accent(0x1E2228).pattern(Pattern.BRICK, 0x2A3036, 1.4f).grime(0.3f);
        Paint stoneTrim = Paint.of(Mat.STONE, 0x4A525C).accent(0x262A30).trim(0xC8A040, 0.6f);
        Paint runes = Paint.of(Mat.STONE, 0x2A3038).pattern(Pattern.RUNES, 0x6FE3FF, 1.2f).accentGlow();
        Paint metal = Paint.of(Mat.DARK_METAL, 0x2E3440).accent(0x161A20).gloss(0.6f).pattern(Pattern.PLATES, 0x3A4250, 1.2f);
        Paint gold = Paint.of(Mat.GOLD, 0xC8A040).accent(0x7A5A18).gloss(0.8f);
        Paint conduit = Paint.glowing(0x6FE3FF).pattern(Pattern.VEINS, 0xE0FFFF, 1f);
        Paint crystal = Paint.of(Mat.CRYSTAL, 0x6FE3FF).accent(0xE0FFFF).glow().pattern(Pattern.CRACKS, 0xB8F8FF, 1f);
        Paint coreLight = Paint.glowing(0xFFFFFF).accent(0xBFF8FF);
        Paint ringP = Paint.of(Mat.GOLD, 0xD8B040).accent(0x7A5A18).pattern(Pattern.RUNES, 0x6FE3FF, 1.4f).accentGlow();
        Paint crack = Paint.of(Mat.PLAIN, 0x0E1418).flat();

        Part root = s.root;
        // ---------------------------------------------------------- plinth and pillar
        Part base = root.child("base", 0, 0, 0);
        base.octo(0, -4, 0, 22, 4, 22, stone);
        base.octo(0, -7, 0, 17, 3, 17, stoneTrim);
        base.octoRing(-3.2f, 22.4f, 0.5f, 1.4f, runes);
        for (int i = 0; i < 4; i++) {
            float a = i * Mth.HALF_PI + Mth.PI / 4;
            base.sub(Mth.cos(a) * 9.4f, -4, Mth.sin(a) * 9.4f).rot(0, -a * Mth.RAD_TO_DEG, 0).box(-1.2f, -1.4f, -1.2f, 2.4f, 1.4f, 2.4f, gold);
        }
        Part pillar = root.child("pillar", 0, -7, 0);
        pillar.taper(0, 0, 0, 9, 9, 6, 6, 14, 4, metal);
        for (int i = 0; i < 4; i++) {
            Part cd = pillar.sub(0, 0, 0).rot(0, i * 90, 0);
            cd.box(-0.6f, -13, -4.1f, 1.2f, 12, 0.6f, conduit);
        }
        pillar.octo(0, -17, 0, 10, 3, 10, gold);
        // cradle claws
        for (int i = 0; i < 4; i++) {
            Part claw = pillar.child("claw" + i, 0, -17, 0).rot(0, i * 90 + 45, 0);
            Part c1 = claw.sub(0, 0, -4).rot(-28, 0, 0);
            c1.box(-0.9f, -10, -0.9f, 1.8f, 10, 1.8f, metal);
            Part c2 = c1.sub(0, -10, 0).rot(48, 0, 0);
            c2.box(-0.7f, -7, -0.7f, 1.4f, 7, 1.4f, gold);
            c2.spike(0, -7, 0, 1.0f, 2.4f, 20, 0, 0, gold);
        }
        // ---------------------------------------------------------- the crystal
        Part cr = root.child("crystal", 0, -37, 0);
        cr.taper(0, 0, 0, 9, 9, 1.2f, 1.2f, 13, 6, crystal);
        cr.sub(0, 0, 0).rot(180, 0, 0).taper(0, 0, 0, 9, 9, 1.2f, 1.2f, 9, 5, crystal);
        cr.sub(0, 0, 0).rot(0, 45, 0).taper(0, 0, 0, 7, 7, 1, 1, 11, 4, crystal);
        cr.child("core_light", 0, -1, 0).sphere(0, 0, 0, 2.4f, coreLight);
        Part c1 = cr.child("crack1", 0, 0, 0);
        c1.box(-3, -6, -4.6f, 0.5f, 7, 0.3f, crack).box(-2.5f, -1.5f, -4.4f, 3, 0.5f, 0.3f, crack).box(4.3f, -4, -1, 0.3f, 6, 0.5f, crack);
        Part c2 = cr.child("crack2", 0, 0, 0);
        c2.box(1, -9, -3.1f, 0.5f, 10, 0.3f, crack).box(-4.4f, -3, 0, 0.3f, 7, 0.5f, crack).box(-1, 2, 3.4f, 4, 0.5f, 0.3f, crack)
                .box(2.4f, -1, -4.2f, 0.4f, 5, 0.3f, crack);
        // rings and shards
        root.child("ring1", 0, -37, 0).octoRing(-0.5f, 27, 0.8f, 1.1f, ringP);
        Part r2 = root.child("ring2", 0, -37, 0);
        r2.sub(0, 0, 0).rot(90, 0, 0).octoRing(-0.5f, 22, 0.8f, 1.1f, ringP);
        Part r3 = root.child("ring3", 0, -37, 0);
        r3.sub(0, 0, 0).rot(0, 0, 90).octoRing(-0.4f, 17, 0.6f, 0.8f, ringP);
        Part shards = root.child("shards", 0, -37, 0);
        for (int i = 0; i < 6; i++) {
            float a = i * Mth.TWO_PI / 6;
            Part sh = shards.child("shard" + i, Mth.cos(a) * 16, (i % 2 == 0 ? -3 : 3), Mth.sin(a) * 16);
            sh.taper(0, 1.4f, 0, 1.8f, 1.8f, 0.3f, 0.3f, 3.4f, 2, crystal);
            sh.sub(0, 1.4f, 0).rot(180, 0, 0).taper(0, 0, 0, 1.8f, 1.8f, 0.3f, 0.3f, 2.2f, 2, crystal);
        }
        // ---------------------------------------------------------- upgrade hardware
        Part fort1 = root.child("fort1", 0, 0, 0);
        for (int i = 0; i < 4; i++) {
            Part b = fort1.sub(0, 0, 0).rot(0, i * 90 + 45, 0);
            b.sub(0, -4, -11).rot(-35, 0, 0).box(-2.4f, -8, -1, 4.8f, 8, 2, stoneTrim);
        }
        Part fort3 = root.child("fort3", 0, -7, 0);
        fort3.octoRing(-4, 9.6f, 0.6f, 1.2f, gold).octoRing(-11, 8.4f, 0.6f, 1.2f, gold);
        Part fort5 = root.child("fort5", 0, -4, 0);
        for (int i = 0; i < 8; i++) {
            float a = i * Mth.TWO_PI / 8;
            fort5.sub(Mth.cos(a) * 10.2f, 0, Mth.sin(a) * 10.2f).rot(0, -a * Mth.RAD_TO_DEG, 0).box(-1.2f, -2.4f, -1.2f, 2.4f, 2.4f, 2.4f, stoneTrim);
        }
        Part barrier = root.child("barrier1", 0, 0, 0);
        for (int i = 0; i < 4; i++) {
            float a = i * Mth.HALF_PI + Mth.PI / 4;
            Part sp = barrier.sub(Mth.cos(a) * 12.5f, 0, Mth.sin(a) * 12.5f);
            sp.taper(0, 0, 0, 2.6f, 2.6f, 1.2f, 1.2f, 14, 4, metal);
            sp.sub(0, -14, 0).rot(180, 0, 0).taper(0, 0, 0, 1.8f, 1.8f, 0.3f, 0.3f, 1.6f, 2, Paint.glowing(0x8FD8FF));
            sp.taper(0, -14, 0, 1.8f, 1.8f, 0.3f, 0.3f, 3.6f, 3, Paint.glowing(0x8FD8FF).accent(0xFFFFFF));
            sp.octoRing(-9, 3.4f, 0.4f, 0.6f, gold);
        }
        Paint copper = Paint.of(Mat.METAL, 0xB8703A).accent(0x6A3A1A).gloss(0.7f);
        Part pulse = root.child("pulse1", 0, -7, 0);
        pulse.octoRing(-6, 11.4f, 0.8f, 1.2f, copper).octoRing(-6.3f, 11.8f, 0.3f, 0.6f, Paint.glowing(0xC86BFF));
        pulse.octoRing(-9, 10.6f, 0.8f, 1.2f, copper).octoRing(-9.3f, 11f, 0.3f, 0.6f, Paint.glowing(0xC86BFF));
        Part pulse3 = root.child("pulse3", 0, -7, 0);
        for (int i = 0; i < 4; i++) {
            Part pr = pulse3.sub(0, -12, 0).rot(0, i * 90, 0);
            pr.sub(0, 0, -5).rot(-60, 0, 0).box(-0.4f, -5, -0.4f, 0.8f, 5, 0.8f, copper);
            pr.sub(0, 0, -5).rot(-60, 0, 0).sphere(0, -5.4f, 0, 0.8f, Paint.glowing(0xC86BFF));
        }
        Part treasury = root.child("treasury1", 0, 0, 0);
        treasury.octoRing(-6.6f, 17.4f, 0.4f, 0.8f, gold);
        for (int i = 0; i < 3; i++) {
            float a = Mth.PI * 0.75f + i * 0.5f;
            Part st = treasury.sub(Mth.cos(a) * 9.4f, -7, Mth.sin(a) * 9.4f);
            for (int k = 0; k < 2 + i % 2; k++) st.box(-1.2f, -0.6f - k * 0.7f, -1.2f, 2.4f, 0.6f, 2.4f, gold);
        }
        Part treasury3 = root.child("treasury3", 0, -7, -8.4f);
        treasury3.octo(0, -2.4f, 0, 3.4f, 2.4f, 3.4f, gold);
        treasury3.spike(0, -2.4f, 0, 2.2f, 3, 0, 0, 0, Paint.glowing(0xFFB040).accent(0xFFF0A0));
        for (int side = -1; side <= 1; side += 2) {
            Part g = root.child(side < 0 ? "gear_r" : "gear_l", side * 5.4f, -14, 0);
            g.sub(0, 0, 0).rot(0, 0, 0).octoX(-0.6f, 0, 0, 7, 1.2f, metal);
            for (int t = 0; t < 8; t++) g.sub(0, 0, 0).rot(t * 45, 0, 0).box(-0.5f, -4.6f, -0.7f, 1, 1.4f, 1.4f, gold);
            g.octoX(-0.8f, 0, 0, 2.4f, 1.6f, gold);
        }
        Part halo = root.child("halo", 0, -54, 0);
        halo.octoRing(-0.5f, 14, 0.8f, 1, gold);
        for (int i = 0; i < 8; i++) {
            float a = i * Mth.TWO_PI / 8;
            halo.spike(Mth.cos(a) * 6.6f, -0.5f, Mth.sin(a) * 6.6f, 1, i % 2 == 0 ? 3.6f : 2.2f, 0, 0, 0, gold);
        }
        Part shaft = root.child("beacon2", 0, -56, 0);
        shaft.box(-0.8f, -30, -0.8f, 1.6f, 30, 1.6f, Paint.glowing(0xFFF4C0).accent(0xFFFFFF));
        // the crest on the plinth front
        base.box(-2, -6.6f, -8.6f, 4, 4, 0.4f, Paint.of(Mat.GOLD, 0xC8A040).decal(Face.FRONT, DecalType.SUN, 0x6FE3FF, true, 0.5f, 0.5f, 1.2f));
        baked = s.bake();
        return baked;
    }
}
