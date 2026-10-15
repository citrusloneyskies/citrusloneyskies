package com.lastbastion.client.model;

import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import net.minecraft.util.Mth;

/** Geometry for the war-event models (renderers and animation live in {@link RaidModels}). */
public final class RaidShapes {
    public static final int RIFT_SHARDS = 8, RIFT_DEBRIS = 6;

    private RaidShapes() {
    }

    /**
     * Olive-drab airdrop crate: riveted plate body with a hazard band, stencilled emblems, steel corner posts,
     * carry handles and a strobing green locator lamp. While falling it hangs under a striped canopy.
     */
    public static void crate(ModelSpec s) {
        Paint body = Paint.of(Mat.METAL, 0x4E5C3A).pattern(Pattern.PLATES, 0x3A4630, 1.2f).grime(0.25f)
                .decal(Face.FRONT, DecalType.EMBLEM, 0xE8E0C8, false, 0.5f, 0.42f, 0.9f)
                .decal(Face.BACK, DecalType.EMBLEM, 0xE8E0C8, false, 0.5f, 0.42f, 0.9f)
                .decal(Face.LEFT, DecalType.CROSS, 0xE8E0C8, false, 0.5f, 0.42f, 0.8f)
                .decal(Face.RIGHT, DecalType.CROSS, 0xE8E0C8, false, 0.5f, 0.42f, 0.8f);
        Paint band = Paint.of(Mat.METAL, 0xE0B020).pattern(Pattern.HAZARD, 0x1A1A1A, 1.2f);
        Paint steel = Paint.of(Mat.DARK_METAL, 0x34383E).gloss(0.4f);
        Paint lid = Paint.of(Mat.METAL, 0x56643F).pattern(Pattern.PLATES, 0x3A4630, 1.4f).decal(Face.TOP, DecalType.STAR, 0xE8E0C8, false, 0.5f, 0.5f, 0.9f);
        Part c = s.root.child("crate", 0, 24, 0);
        c.box(-7, -12, -7, 14, 12, 14, body);
        c.box(-7.2f, -7.4f, -7.2f, 14.4f, 2.6f, 14.4f, band);
        c.box(-7.4f, -13.2f, -7.4f, 14.8f, 1.6f, 14.8f, lid);
        for (int i = 0; i < 4; i++) {
            float x = i % 2 == 0 ? -7.6f : 5.6f, z = i < 2 ? -7.6f : 5.6f;
            c.box(x, -13.6f, z, 2, 13.6f, 2, steel);
        }
        c.box(-3, -14.4f, -7.8f, 6, 1, 0.8f, steel).box(-3, -14.4f, 7, 6, 1, 0.8f, steel);
        c.box(-7.8f, -6, -3, 0.8f, 1, 6, steel).box(7, -6, -3, 0.8f, 1, 6, steel);
        Part lamp = c.child("lamp", 0, -13.2f, 0);
        lamp.box(-1.2f, -1.8f, -1.2f, 2.4f, 1.8f, 2.4f, Paint.glowing(0x7CFF9B));
        lamp.box(-1.6f, -0.4f, -1.6f, 3.2f, 0.4f, 3.2f, steel);
        // parachute: cords from the corners to a tiered, striped canopy
        Part chute = c.child("chute", 0, -14, 0);
        Paint cord = Paint.of(Mat.CLOTH, 0xE8E0D0);
        for (int i = 0; i < 4; i++) {
            float a = 45 + 90 * i;
            Part cd = chute.sub(0, 0, 0).rot(0, a, 0);
            cd.sub(0, 0, 9).rot(-22, 0, 0).box(-0.2f, -30, -0.2f, 0.4f, 30, 0.4f, cord);
        }
        Paint canopy = Paint.of(Mat.CLOTH, 0xE86A20).pattern(Pattern.STRIPES, 0xF0ECE0, 1.6f);
        Part can = chute.child("canopy", 0, -30, 0);
        can.octo(0, -1, 0, 34, 3, 34, canopy);
        can.octo(0, -4, 0, 28, 3, 28, canopy);
        can.octo(0, -6.5f, 0, 18, 2.5f, 18, canopy);
        can.octo(0, -8, 0, 8, 1.5f, 8, Paint.of(Mat.CLOTH, 0xF0ECE0));
    }

    /**
     * A vertical tear in the world: a lens of violet light around a starless void, ringed by orbiting obsidian
     * shards, with debris circling above a disc of corrupted, glowing-cracked ground.
     */
    public static void rift(ModelSpec s) {
        Paint ground = Paint.of(Mat.STONE, 0x241A2C).pattern(Pattern.CRACKS, 0xB050FF, 1.2f).accentGlow();
        Paint rim = Paint.glowing(0xC060FF).pattern(Pattern.VEINS, 0xFFD8FF, 0.8f);
        Paint halo = Paint.glowing(0x7A2AC0).pattern(Pattern.GRADIENT, 0xC070FF, 1f);
        Paint voidP = Paint.of(Mat.ENERGY, 0x07020C).pattern(Pattern.GRADIENT, 0x1C0830, 1f).flat();
        Paint star = Paint.glowing(0xE8C8FF);
        Paint shard = Paint.of(Mat.CRYSTAL, 0x2E1440).accent(0xD090FF).gloss(0.9f);
        Paint shardGlow = Paint.of(Mat.CRYSTAL, 0x3A1A50).pattern(Pattern.VEINS, 0xC070FF, 1.2f).accentGlow().gloss(0.9f);
        Paint debris = Paint.of(Mat.STONE, 0x3A2A44).pattern(Pattern.CRACKS, 0xC070FF, 1f).accentGlow();
        Part base = s.root.child("rift_ground", 0, 24, 0);
        base.octo(0, -0.6f, 0, 34, 0.8f, 34, ground);
        base.octo(0, -1.2f, 0, 22, 0.8f, 22, Paint.of(Mat.STONE, 0x1A1220).pattern(Pattern.CRACKS, 0xD080FF, 1.4f).accentGlow());
        for (int i = 0; i < 7; i++) {
            float a = i * 360f / 7 + 12;
            Part sp = base.sub(0, 0, 0).rot(0, a, 0).sub(12 + (i % 3) * 1.5f, -0.8f, 0).rot(0, 0, -(20 + (i % 3) * 8));
            sp.taper(0, 0, 0, 3.2f, 3.2f, 0.5f, 0.5f, 5 + (i % 4) * 2.2f, 4, i % 2 == 0 ? shardGlow : shard);
        }
        // the tear: an ellipse of glowing slices (with a dimmer halo) around a starless void, 52 px tall
        Part tear = s.root.child("tear", 0, -6, 0);
        float hgt = 52, wid = 18;
        int n = 18;
        for (int i = 0; i < n; i++) {
            float t = (i + 0.5f) / n;
            float half = (float) Math.sqrt(Math.max(0, 1 - (2 * t - 1) * (2 * t - 1)));
            float y = -hgt / 2 + t * hgt;
            float wOuter = Math.max(1.2f, wid * half);
            tear.box(-wOuter / 2, y - hgt / n / 2, -0.8f, wOuter, hgt / n + 0.05f, 1.6f, rim);
            tear.box(-wOuter / 2 - 1.4f, y - hgt / n / 2, -0.4f, wOuter + 2.8f, hgt / n + 0.05f, 0.8f, halo);
        }
        Part core = tear.child("tear_void", 0, 0, 0);
        for (int i = 1; i < n - 1; i++) {
            float t = (i + 0.5f) / n;
            float half = (float) Math.sqrt(Math.max(0, 1 - (2 * t - 1) * (2 * t - 1)));
            float y = -hgt / 2 + t * hgt;
            float wInner = Math.max(0.6f, wid * half - 3.6f);
            core.box(-wInner / 2, y - hgt / n / 2, -1.1f, wInner, hgt / n + 0.05f, 2.2f, voidP);
        }
        for (int i = 0; i < 9; i++) {
            float yy = -hgt * 0.35f + (i * 37 % 100) / 100f * hgt * 0.7f;
            float t = (yy + hgt / 2) / hgt;
            float half = (float) Math.sqrt(Math.max(0, 1 - (2 * t - 1) * (2 * t - 1)));
            float xx = ((i * 53 % 100) / 100f - 0.5f) * (wid * half - 6);
            float sz = 0.5f + (i % 3) * 0.3f;
            core.box(xx - sz / 2, yy - sz / 2, -1.3f, sz, sz, 2.6f, star);
        }
        // tendrils licking down to the ground
        Part tendrils = s.root.child("tendrils", 0, 20, 0);
        for (int i = 0; i < 4; i++) {
            float a = 45 + i * 90;
            tendrils.sub(0, 0, 0).rot(0, a, 0).sub(0, 0, 3).rot(28, 0, 0).box(-0.4f, -14, -0.4f, 0.8f, 14, 0.8f, Paint.glowing(0xB050FF));
        }
        // orbiting obsidian crystals, tilted outward like a crown of thorns, and floating debris
        Part ring = s.root.child("shard_ring", 0, -6, 0);
        for (int i = 0; i < RIFT_SHARDS; i++) {
            float a = i * 360f / RIFT_SHARDS;
            Part arm = ring.child("shard_arm" + i, 0, 0, 0).rot(0, a, 0);
            Part sh = arm.child("shard" + i, 0, (i % 2 == 0 ? -12 : 8) + (i % 3) * 2, 16);
            Part up = sh.sub(0, 0, 0).rot(30 + (i % 3) * 8, 0, 0);
            float len = 10 + (i % 3) * 3;
            up.taper(0, len * 0.35f, 0, 3.8f, 3.8f, 0.6f, 0.6f, len, 4, i % 2 == 0 ? shard : shardGlow);
            Part down = sh.sub(0, 0, 0).rot(180 + 30 + (i % 3) * 8, 0, 0);
            down.taper(0, len * 0.35f, 0, 3.8f, 3.8f, 1.2f, 1.2f, len * 0.45f, 3, shard);
        }
        Part orbit = s.root.child("debris_orbit", 0, -18, 0);
        for (int i = 0; i < RIFT_DEBRIS; i++) {
            float a = i * 360f / RIFT_DEBRIS + 30;
            float r = 22 + (i % 2) * 4;
            Part d = orbit.child("debris" + i, Mth.cos(a * Mth.DEG_TO_RAD) * r, (i % 3) * 6 - 6, Mth.sin(a * Mth.DEG_TO_RAD) * r).rot(i * 23, i * 41, i * 17);
            float sz = 2 + (i % 3);
            d.box(-sz / 2, -sz / 2, -sz / 2, sz, sz, sz, debris);
        }
    }
}
