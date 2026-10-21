package com.tokyoghoul.client.model;

import com.tokyoghoul.client.model.ModelSpec.Part;
import com.tokyoghoul.client.model.Paint.DecalType;
import com.tokyoghoul.client.model.Paint.Face;
import com.tokyoghoul.client.model.Paint.Mat;
import com.tokyoghoul.client.model.Paint.Pattern;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Look;
import com.tokyoghoul.data.Race;
import com.tokyoghoul.move.MoveDef;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Builds every character's 3D model from its {@link Look}: a jointed {@link Body} sized by build and
 * gender, then hair, outfit details, glasses and stitches, normal and kakugan eyes, the mask, and the
 * transformation armour (kakuja plates or Arata). Toggleable groups are named by prefix so the
 * renderer can show them: {@code mask*}, {@code kak*}, {@code eye*}, {@code form*}, {@code face*}.
 */
public final class CharacterModels {
    private static final Map<String, ModelSpec.Baked> CACHE = new HashMap<>();

    private CharacterModels() {
    }

    public static ModelSpec.Baked baked(CharacterDef def) {
        return CACHE.computeIfAbsent(def.id, id -> {
            ModelSpec s = new ModelSpec("char_" + id, 4);
            build(s, def);
            return s.bake();
        });
    }

    /** Torso sizes {chestW, chestH, chestD, waistW, waist, waistD} for a look (kagune anchor on these). */
    public static float[] dims(Look l) {
        float[] d = switch (l.build) {
            case SLIM -> new float[]{7.2f, 7, 4, 6.2f, 5, 3.8f};
            case AVERAGE -> new float[]{8, 7, 4.4f, 7, 5, 4};
            case ATHLETIC -> new float[]{8.8f, 7.4f, 4.8f, 7.2f, 5, 4.2f};
            case BULKY -> new float[]{9.8f, 7.6f, 5.2f, 8.4f, 5, 4.8f};
            case HUGE -> new float[]{11f, 8f, 5.8f, 9.6f, 5.2f, 5.4f};
        };
        if (l.female) {
            d[0] -= 0.6f;
            d[3] -= 0.9f;
        }
        return d;
    }

    static Paint p(Mat m, int c) {
        return Paint.of(m, c);
    }

    // ------------------------------------------------------------------ build

    public static void build(ModelSpec s, CharacterDef def) {
        Look l = def.look;
        Paint skin = p(Mat.SKIN, l.skin);
        Paint head = skin.copy().decal(Face.FRONT, DecalType.NOSE, l.skin, false, 0.5f, 0.66f, 1f)
                .decal(Face.FRONT, DecalType.MOUTH, 0x8A4A44, false, 0.5f, 0.8f, 1f)
                .decal(Face.FRONT, DecalType.BROW, darken(l.hair, 0.8f), false, 0.5f, 0.5f, 1f);
        if (l.has(Look.STITCHES)) head.decal(Face.FRONT, DecalType.STITCH, 0xA02020, false, 0.3f, 0.62f, 0.5f);
        if (l.has(Look.BEARD)) head.decal(Face.FRONT, DecalType.BEARD, l.hair, false);
        Paint top = topPaint(l);
        Paint sleeve = top.plain();
        Paint fore = switch (l.top) {
            case VEST -> p(Mat.CLOTH, l.topAccent);
            case BANDAGES -> top.plain();
            default -> sleeve;
        };
        Paint hand = l.has(Look.GLOVES) ? p(Mat.LEATHER, 0x151515) : skin;
        Paint pants = p(Mat.CLOTH, l.pants);
        Paint shoes = p(Mat.LEATHER, l.shoes).gloss(0.5f);

        Body.Spec spec = new Body.Spec(skin, top);
        float[] dm = dims(l);
        spec.torso(dm[0], dm[1], dm[2], dm[3], dm[4], dm[5]);
        switch (l.build) {
            case SLIM -> spec.arms(6, 6, 3.2f).legs(6, 6, 3.4f);
            case AVERAGE -> spec.arms(6, 6, 3.6f).legs(6, 6, 3.8f);
            case ATHLETIC -> spec.arms(6, 6, 3.9f).legs(6, 6, 4f);
            case BULKY -> spec.arms(6, 6, 4.4f).legs(6, 6, 4.4f);
            case HUGE -> spec.arms(6.2f, 6.2f, 5f).legs(6, 6, 5f);
        }
        if (l.female) spec.hipW += 0.3f;
        spec.paints(top, top.plain(), sleeve, fore, hand, pants, pants, shoes);
        spec.headPaint(head);
        if (l.top == Look.Top.DRESS || l.top == Look.Top.ROBE) {
            spec.thighP = p(Mat.CLOTH, l.pants);
        }
        Body b = Body.build(s.root, spec);
        float hw = 8, hh = 8, hd = 8;

        eyes(b.head, l, def, hw, hh, hd);
        hair(b.head, l, hw, hh, hd);
        outfit(b, l, spec);
        accessories(b, l, hw, hh, hd);
        maskInto(b.head, l, hw, hh, hd);
        form(b, def, spec);
    }

    private static Paint topPaint(Look l) {
        return switch (l.top) {
            case HOODIE -> p(Mat.CLOTH, l.topColor).decal(Face.FRONT, DecalType.HSTRIPE, darken(l.topColor, 0.7f), false, 0.5f, 0.75f, 0.5f);
            case TRENCH_COAT -> p(Mat.CLOTH, l.topColor).trim(darken(l.topColor, 0.75f), 0.6f);
            case SUIT, VEST -> p(Mat.CLOTH, l.topColor).trim(darken(l.topColor, 0.8f), 0.5f);
            case BODYSUIT -> p(Mat.LEATHER, l.topColor).pattern(Pattern.MUSCLE, darken(l.topColor, 0.7f)).gloss(0.4f);
            case BANDAGES -> p(Mat.CLOTH, l.topColor).pattern(Pattern.BANDS, l.topAccent, 0.7f);
            case ROBE -> p(Mat.ROBE, l.topColor).trim(l.topAccent, 0.7f);
            case CLOAK -> p(Mat.ROBE, l.topColor).trim(l.topAccent, 0.5f);
            case DRESS -> p(Mat.CLOTH, l.topColor).trim(l.topAccent, 0.5f);
            case QUINX -> p(Mat.LEATHER, l.topColor).pattern(Pattern.STRIPES, l.topAccent, 0.5f).gloss(0.3f);
            case JACKET -> p(Mat.LEATHER, l.topColor).trim(l.topAccent, 0.5f);
            case SHIRT -> p(Mat.CLOTH, l.topColor);
        };
    }

    static int darken(int rgb, float f) {
        return TextureBaker.shadeRgb(rgb, f);
    }

    /** Normal eyes, kakugan (black sclera, glowing red iris) and the one-eyed variant. */
    private static void eyes(Part head, Look l, CharacterDef def, float hw, float hh, float hd) {
        float z = -hd / 2 - 0.06f;
        Paint white = p(Mat.PLAIN, 0xF2EEE8).flat();
        Paint iris = p(Mat.PLAIN, l.eyes).flat();
        Paint sclera = p(Mat.PLAIN, 0x050505).flat();
        Paint red = Paint.glowing(0xFF1010).flat();
        for (int side = -1; side <= 1; side += 2) {
            String sn = side < 0 ? "r" : "l";
            float cx = side * 1.7f;
            Part e = head.child("eye_" + sn, 0, 0, 0);
            e.box(cx - 0.9f, -4.4f, z, 1.8f, 0.9f, 0.1f, white);
            e.box(cx - 0.4f + side * 0.1f, -4.45f, z - 0.05f, 0.8f, 1.0f, 0.1f, iris);
            boolean kak = def.race.hasKagune && (def.race != Race.ONE_EYED && def.race != Race.QUINX || side > 0);
            if (kak) {
                // sits on the face; the animator pushes it out in front of the mask when masked
                float kz = -hd / 2 - 0.12f;
                Part k = head.child("kak_" + sn, 0, 0, 0);
                k.box(cx - 1.0f, -4.5f, kz, 2.0f, 1.1f, 0.1f, sclera);
                k.box(cx - 0.45f, -4.5f, kz - 0.06f, 0.9f, 1.1f, 0.1f, red);
            }
        }
    }

    private static void hair(Part head, Look l, float hw, float hh, float hd) {
        if (l.hairStyle == Look.Hair.BALD) return;
        Paint hp = p(Mat.HAIR, l.hair);
        Paint under = l.hairAccent >= 0 ? p(Mat.HAIR, l.hairAccent) : hp;
        Part h = head.child("hair", 0, 0, 0);
        float x0 = -hw / 2 - 0.45f, w = hw + 0.9f, z0 = -hd / 2 - 0.45f, d = hd + 0.9f;
        Random rnd = new Random(l.hair * 31L + l.hairStyle.ordinal());
        if (l.hairStyle == Look.Hair.BUZZ) {
            h.box(x0 + 0.2f, -hh - 0.3f, z0 + 0.2f, w - 0.4f, 2.4f, d - 0.4f, hp);
            return;
        }
        // cap + back + sides
        h.box(x0, -hh - 0.7f, z0, w, 2.6f, d, hp);
        h.box(x0, -hh + 1.6f, hd / 2 - 0.5f, w, hh * 0.6f, 0.95f, under);
        h.box(x0, -hh + 1.6f, z0 + 1.2f, 0.9f, hh * 0.35f, d - 1.7f, hp);
        h.box(hw / 2 - 0.45f, -hh + 1.6f, z0 + 1.2f, 0.9f, hh * 0.35f, d - 1.7f, hp);
        switch (l.hairStyle) {
            case SHORT -> h.box(x0, -hh + 1.6f, z0, w, 1.4f, 1.0f, hp);
            case SLICKED -> {
                h.box(x0, -hh - 0.9f, z0 + 0.6f, w, 1.0f, d - 0.4f, hp);
                h.box(x0 + 0.3f, -hh + 1.6f, hd / 2 - 0.2f, w - 0.6f, hh * 0.45f, 0.8f, hp);
            }
            case MESSY, WILD, SPIKY -> {
                int n = l.hairStyle == Look.Hair.SPIKY ? 14 : l.hairStyle == Look.Hair.WILD ? 18 : 10;
                h.box(x0, -hh + 1.6f, z0, w, 2.0f, 1.0f, hp);
                for (int i = 0; i < n; i++) {
                    float px = (rnd.nextFloat() - 0.5f) * hw;
                    float pz = (rnd.nextFloat() - 0.4f) * hd;
                    float len = l.hairStyle == Look.Hair.SPIKY ? 3 + rnd.nextFloat() * 2 : 2 + rnd.nextFloat() * 2.4f;
                    float tilt = l.hairStyle == Look.Hair.SPIKY ? 15 : 55;
                    h.spike(px, -hh + 0.4f, pz, 1.8f, len, (rnd.nextFloat() - 0.2f) * tilt + (pz > 0 ? -25 : 20), 0,
                            (px > 0 ? -1 : 1) * rnd.nextFloat() * tilt, rnd.nextBoolean() ? hp : under);
                }
                // fringe tufts falling over the forehead
                for (int i = 0; i < 4; i++) {
                    float px = -hw / 2 + 1 + i * (hw - 2) / 3f;
                    h.spike(px, -hh + 1.4f, z0 + 0.4f, 1.6f, 2.6f, 160 + rnd.nextFloat() * 20, 0, (rnd.nextFloat() - 0.5f) * 30, hp);
                }
                if (l.hairStyle == Look.Hair.WILD) h.box(x0 - 0.3f, -hh + 1.6f, hd / 2 - 0.6f, w + 0.6f, hh + 2.5f, 1.2f, hp);
            }
            case LONG -> {
                h.box(x0, -hh + 1.6f, z0, w, 1.3f, 1.0f, hp);
                h.box(x0, -hh + 1.6f, hd / 2 - 0.3f, w, hh + 5f, 1.1f, hp);
                h.box(x0 - 0.1f, -hh + 1.6f, z0 + 1.2f, 1.0f, hh + 1.5f, d - 1.5f, hp);
                h.box(hw / 2 - 0.4f, -hh + 1.6f, z0 + 1.2f, 1.0f, hh + 1.5f, d - 1.5f, hp);
            }
            case BOB -> {
                h.box(x0, -hh + 1.6f, z0, w, 1.6f, 1.0f, hp);
                h.box(x0 - 0.2f, -hh + 1.6f, z0 + 0.8f, 1.1f, hh - 1.0f, d - 1.0f, hp);
                h.box(hw / 2 - 0.4f, -hh + 1.6f, z0 + 0.8f, 1.1f, hh - 1.0f, d - 1.0f, hp);
                h.box(x0, -hh + 1.6f, hd / 2 - 0.4f, w, hh - 1.2f, 1.0f, hp);
            }
            case SIDE_SWEPT -> {
                // long fringe swept over the right eye (Touka, Yomo)
                h.box(x0, -hh + 1.6f, z0, w, 1.2f, 1.0f, hp);
                h.box(x0, -hh + 1.6f, z0 - 0.15f, hw * 0.55f, 3.6f, 0.9f, hp);
                h.box(x0, -hh + 1.6f, hd / 2 - 0.4f, w, hh - 0.6f, 1.0f, hp);
            }
            case PONYTAIL -> {
                h.box(x0, -hh + 1.6f, z0, w, 1.3f, 1.0f, hp);
                h.child("ponytail", 0, -hh + 2.5f, hd / 2 + 0.4f).rot(20, 0, 0).box(-1.3f, 0, -0.8f, 2.6f, 9, 1.8f, hp);
            }
            case TWINTAILS -> {
                h.box(x0, -hh + 1.6f, z0, w, 1.6f, 1.0f, hp);
                h.child("tail_l", hw / 2 + 0.5f, -hh + 2.5f, 1).rot(0, 0, -15).box(-1, 0, -1, 2, 8, 2, hp);
                h.child("tail_r", -hw / 2 - 0.5f, -hh + 2.5f, 1).rot(0, 0, 15).box(-1, 0, -1, 2, 8, 2, hp);
            }
            default -> {
            }
        }
        if (l.has(Look.HAIRCLIPS)) {
            Paint clip = p(Mat.PLAIN, 0xD02030);
            h.box(-hw / 2 - 0.7f, -hh + 2.2f, -1.5f, 0.6f, 0.5f, 2.4f, clip);
            h.box(-hw / 2 - 0.7f, -hh + 3.2f, -1.2f, 0.6f, 0.5f, 2.4f, clip);
        }
    }

    private static void outfit(Body b, Look l, Body.Spec spec) {
        Paint top = topPaint(l).plain();
        float cw = spec.chestW, ch = spec.chestH, cd = spec.chestD;
        switch (l.top) {
            case SUIT, VEST -> {
                Paint shirt = p(Mat.CLOTH, l.topAccent);
                b.chest.box(-1.4f, -ch + 0.2f, -cd / 2 - 0.12f, 2.8f, ch - 0.4f, 0.2f, shirt);
                lapels(b, l, cw, ch, cd);
            }
            case TRENCH_COAT -> {
                b.chest.box(-cw / 2 - 0.3f, -ch - 0.6f, -cd / 2 - 0.3f, cw + 0.6f, 1.6f, cd + 0.6f, top);
                Paint inner = p(Mat.CLOTH, l.topAccent);
                b.chest.box(-1.3f, -ch + 0.6f, -cd / 2 - 0.12f, 2.6f, ch - 0.6f, 0.2f, inner);
                lapels(b, l, cw, ch, cd);
                float tail = spec.thigh + spec.shin * 0.55f;
                b.rThigh.child("coat_r", 0, -0.5f, 0).box(-spec.legW / 2 - 0.6f, 0, -spec.legW / 2 - 0.6f, spec.legW + 1.2f, tail, spec.legW + 1.2f, top);
                b.lThigh.child("coat_l", 0, -0.5f, 0).box(-spec.legW / 2 - 0.6f, 0, -spec.legW / 2 - 0.6f, spec.legW + 1.2f, tail, spec.legW + 1.2f, top);
                b.pelvis.child("coat_back", 0, 0, spec.waistD / 2).box(-spec.hipW / 2 - 0.6f, -1, 0.1f, spec.hipW + 1.2f, tail, 0.8f, top);
            }
            case HOODIE -> b.chest.box(-cw / 2 + 0.6f, -ch - 1.6f, cd / 2 - 0.5f, cw - 1.2f, 2.6f, 1.6f, top);
            case CLOAK -> {
                Part cape = b.chest.child("cape", 0, -ch + 0.4f, cd / 2 + 0.1f);
                cape.box(-cw / 2 - 0.6f, 0, 0, cw + 1.2f, ch + spec.waist + spec.thigh + spec.shin * 0.5f, 0.7f, top);
                b.chest.box(-cw / 2 - 0.4f, -ch - 0.6f, -cd / 2 - 0.4f, cw + 0.8f, 1.8f, cd + 0.8f, top);
            }
            case DRESS, ROBE -> {
                float len = l.top == Look.Top.ROBE ? spec.thigh + spec.shin * 0.8f : spec.thigh * 0.95f;
                Part sk = b.pelvis.child("skirt", 0, 0, 0);
                sk.taper(0, len + 0.6f, 0, spec.hipW + 2.6f, spec.waistD + 2.4f, spec.hipW + 0.6f, spec.waistD + 0.6f, len, 4, top);
            }
            case QUINX -> {
                Paint strap = p(Mat.LEATHER, l.topAccent);
                b.chest.box(-cw / 2 - 0.15f, -ch * 0.62f, -cd / 2 - 0.15f, cw + 0.3f, 0.7f, cd + 0.3f, strap);
                b.pelvis.box(-spec.waistW / 2 - 0.15f, -1.2f, -spec.waistD / 2 - 0.15f, spec.waistW + 0.3f, 0.8f, spec.waistD + 0.3f, strap);
            }
            case BANDAGES -> b.head.child("face_wrap", 0, 0, 0).box(-4.3f, -2.6f, -4.3f, 8.6f, 2.6f, 8.6f, topPaint(l).plain());
            default -> {
            }
        }
    }

    private static void lapels(Body b, Look l, float cw, float ch, float cd) {
        Paint lap = p(Mat.CLOTH, darken(l.topColor, 0.82f));
        b.chest.sub(-1.5f, -ch, -cd / 2 - 0.2f).rot(0, 0, 12).box(-1.2f, 0, -0.1f, 1.4f, ch * 0.6f, 0.3f, lap);
        b.chest.sub(1.5f, -ch, -cd / 2 - 0.2f).rot(0, 0, -12).box(-0.2f, 0, -0.1f, 1.4f, ch * 0.6f, 0.3f, lap);
        if (l.has(Look.TIE)) b.chest.box(-0.45f, -ch + 0.4f, -cd / 2 - 0.3f, 0.9f, ch * 0.7f, 0.25f, p(Mat.CLOTH, 0x1A1A1A));
    }

    private static void accessories(Body b, Look l, float hw, float hh, float hd) {
        if (l.has(Look.GLASSES)) {
            Paint frame = p(Mat.METAL, 0x2A2A2A);
            Part g = b.head.child("face_glasses", 0, 0, 0);
            float z = -hd / 2 - 0.35f;
            g.box(-3.2f, -5.0f, z, 2.6f, 0.3f, 0.2f, frame);
            g.box(0.6f, -5.0f, z, 2.6f, 0.3f, 0.2f, frame);
            g.box(-3.2f, -3.5f, z, 2.6f, 0.3f, 0.2f, frame);
            g.box(0.6f, -3.5f, z, 2.6f, 0.3f, 0.2f, frame);
            g.box(-0.6f, -4.6f, z, 1.2f, 0.3f, 0.2f, frame);
            g.box(-hw / 2 - 0.2f, -4.8f, z, 0.3f, 0.3f, hd * 0.6f, frame);
            g.box(hw / 2 - 0.1f, -4.8f, z, 0.3f, 0.3f, hd * 0.6f, frame);
        }
        if (l.has(Look.EYEPATCH)) {
            // medical patch over the left (kakugan) eye
            b.head.child("face_patch", 0, 0, 0).box(0.6f, -5.4f, -hd / 2 - 0.25f, 2.4f, 2.2f, 0.2f, p(Mat.CLOTH, 0xF4F2EE).flat());
        }
        if (l.has(Look.HOOD)) {
            Paint hood = p(Mat.ROBE, l.topColor);
            Part h = b.head.child("hood", 0, 0, 0);
            h.box(-hw / 2 - 0.9f, -hh - 1.0f, -hd / 2 - 0.4f, hw + 1.8f, 1.2f, hd + 1.4f, hood);
            h.box(-hw / 2 - 0.9f, -hh, -hd / 2 - 0.4f, 0.9f, hh + 0.6f, hd + 1.4f, hood);
            h.box(hw / 2, -hh, -hd / 2 - 0.4f, 0.9f, hh + 0.6f, hd + 1.4f, hood);
            h.box(-hw / 2 - 0.9f, -hh, hd / 2 + 0.1f, hw + 1.8f, hh + 0.6f, 0.9f, hood);
        }
        if (l.has(Look.SUSPENDERS)) {
            Paint strap = p(Mat.LEATHER, 0x1A1A1A);
            b.chest.box(-2.6f, -b.torsoLen + 5, -2.4f, 0.6f, 7.2f, 0.2f, strap);
            b.chest.box(2.0f, -b.torsoLen + 5, -2.4f, 0.6f, 7.2f, 0.2f, strap);
        }
        if (l.has(Look.SCARF)) b.chest.box(-3, -7.6f, -2.8f, 6, 1.6f, 5.6f, p(Mat.CLOTH, l.topAccent));
    }

    /** The mask group (shown in combat or always, per character). */
    public static void maskInto(Part head, Look l, float hw, float hh, float hd) {
        if (l.mask == Look.Mask.NONE) return;
        Paint m = p(Mat.LEATHER, l.maskColor).gloss(0.45f);
        Paint a = p(Mat.PLAIN, l.maskAccent);
        Part k = head.child("mask", 0, 0, 0);
        float z = -hd / 2 - 0.6f, x0 = -hw / 2 - 0.25f, w = hw + 0.5f;
        switch (l.mask) {
            case KANEKI -> {
                k.box(x0, -hh + 0.4f, z, w, hh - 0.5f, 0.6f, m.copy().decal(Face.FRONT, DecalType.GRIN, l.maskAccent, false, 0.5f, 0.8f, 1.1f));
                k.box(x0 - 0.1f, -5.6f, z + 0.5f, w + 0.2f, 0.7f, hd + 0.4f, m);
                k.box(-3.3f, -5.6f, z - 0.15f, 2.8f, 2.8f, 0.2f, p(Mat.LEATHER, 0x0A0A0A));
            }
            case RABBIT, BLACK_RABBIT -> {
                k.box(x0, -hh + 1.6f, z, w, hh - 3.2f, 0.6f, m.copy().decal(Face.FRONT, DecalType.HOLLOW_EYES, 0x0A0A0A, false, 0.5f, 0.35f, 1f));
                k.spike(-2, -hh - 0.2f, 0.5f, 1.8f, 6.5f, -12, 0, 10, m);
                k.spike(2, -hh - 0.2f, 0.5f, 1.8f, 6.5f, -12, 0, -10, m);
                if (l.mask == Look.Mask.BLACK_RABBIT) k.box(x0 - 0.3f, -hh - 0.6f, -hd / 2, w + 0.6f, hh * 0.6f, hd + 0.6f, m);
            }
            case GOURMET -> {
                k.box(-0.2f, -hh + 1.0f, z, hw / 2 + 0.45f, hh - 2.6f, 0.6f, m.copy().decal(Face.FRONT, DecalType.MOON, l.maskAccent, false));
                k.box(x0, -5.4f, z + 0.5f, w, 0.5f, hd + 0.3f, a);
            }
            case NISHIKI -> {
                k.box(x0, -3.4f, z, w, 3.2f, 0.6f, m.copy().decal(Face.FRONT, DecalType.GRILLE, 0x0A0A0A, false));
                k.box(0.4f, -hh + 1.6f, z, hw / 2 - 0.15f, 3.0f, 0.6f, m);
            }
            case CROW -> {
                k.box(x0, -hh + 1.2f, z, w, hh - 1.6f, 0.6f, m.copy().decal(Face.FRONT, DecalType.HOLLOW_EYES, 0x0A0A0A, false, 0.5f, 0.38f, 1f));
                k.child("mask_beak", 0, -3.6f, z).rot(-90, 0, 0).taper(0, 0, 0, 2.6f, 1.8f, 0.3f, 0.3f, 4.5f, 4, m);
            }
            case OWL -> {
                k.box(x0, -hh + 0.6f, z, w, hh - 1.0f, 0.6f, m.copy().decal(Face.FRONT, DecalType.HOLLOW_EYES, 0x0A0A0A, false, 0.5f, 0.38f, 1.3f)
                        .pattern(Pattern.FEATHERS, l.maskAccent, 0.8f));
                k.child("mask_beak", 0, -3.6f, z).rot(-100, 0, 0).taper(0, 0, 0, 1.6f, 1.2f, 0.2f, 0.2f, 2.2f, 3, a);
                k.spike(-3, -hh, 0, 1.8f, 3.5f, -15, 0, 25, m);
                k.spike(3, -hh, 0, 1.8f, 3.5f, -15, 0, -25, m);
            }
            case JASON -> k.box(x0, -hh + 0.2f, z, w, hh - 0.2f, 0.7f, p(Mat.PLATE, l.maskColor)
                    .decal(Face.FRONT, DecalType.HOLLOW_EYES, 0x0A0A0A, false, 0.5f, 0.36f, 1f)
                    .decal(Face.FRONT, DecalType.DOTS, 0x3A3A3A, false)
                    .decal(Face.FRONT, DecalType.CHEVRON, l.maskAccent, false, 0.5f, 0.2f, 0.8f));
            case TATARA -> k.box(x0, -3.6f, z, w, 3.6f, 0.6f, m.copy().decal(Face.FRONT, DecalType.FANGS, l.maskAccent, false, 0.5f, 0.45f, 1f));
            case NORO -> k.box(x0, -hh + 0.2f, z, w, hh - 0.2f, 0.6f, m.copy().decal(Face.FRONT, DecalType.STITCH, l.maskAccent, false, 0.5f, 0.75f, 1f));
            case CLOWN -> k.box(x0, -hh + 0.4f, z, w, hh - 0.6f, 0.6f, p(Mat.PLAIN, l.maskColor)
                    .decal(Face.FRONT, DecalType.GRIN, l.maskAccent, false, 0.5f, 0.78f, 1.2f)
                    .decal(Face.FRONT, DecalType.HOLLOW_EYES, 0x0A0A0A, false, 0.5f, 0.38f, 1f)
                    .decal(Face.FRONT, DecalType.STAR, l.maskAccent, false, 0.25f, 0.55f, 0.4f));
            case BANDAGE -> {
                Paint wrap = p(Mat.CLOTH, l.maskColor).pattern(Pattern.BANDS, darken(l.maskColor, 0.8f), 0.6f);
                k.box(-hw / 2 - 0.45f, -hh - 0.4f, -hd / 2 - 0.45f, hw + 0.9f, hh + 0.4f, hd + 0.9f, wrap
                        .decal(Face.FRONT, DecalType.HOLLOW_EYES, 0x0A0A0A, false, 0.5f, 0.45f, 1f));
            }
            case KURONA -> k.box(x0, -hh + 0.6f, z, w, hh - 1.0f, 0.6f, p(Mat.PLAIN, l.maskColor)
                    .decal(Face.FRONT, DecalType.STITCH, l.maskAccent, false, 0.5f, 0.6f, 1.2f)
                    .decal(Face.FRONT, DecalType.HOLLOW_EYES, 0x0A0A0A, false, 0.5f, 0.38f, 1f));
            case HANNYA -> {
                k.box(x0, -hh + 0.2f, z, w, hh - 0.3f, 0.6f, m.copy().decal(Face.FRONT, DecalType.FANGS, l.maskAccent, false, 0.5f, 0.78f, 1.2f)
                        .decal(Face.FRONT, DecalType.HOLLOW_EYES, 0x0A0A0A, false, 0.5f, 0.38f, 1f));
                k.spike(-2.6f, -hh + 0.4f, -1, 1.6f, 4, -25, 0, 20, a);
                k.spike(2.6f, -hh + 0.4f, -1, 1.6f, 4, -25, 0, -20, a);
            }
            case PLAIN -> k.box(x0, -hh + 0.8f, z, w, hh - 1.4f, 0.6f, p(Mat.PLAIN, l.maskColor)
                    .decal(Face.FRONT, DecalType.HOLLOW_EYES, 0x0A0A0A, false, 0.5f, 0.4f, 1f)
                    .decal(Face.FRONT, DecalType.STRIPE, l.maskAccent, false, 0.5f, 0.5f, 0.5f));
            default -> {
            }
        }
    }

    /** Transformation armour: kakuja plates in kagune colour, or Shinohara's Arata. */
    private static void form(Body b, CharacterDef def, Body.Spec spec) {
        MoveDef ult = def.ultimate();
        if (ult == null || ult.kind != MoveDef.Kind.FORM) return;
        boolean arata = ult.source == MoveDef.Source.QUINQUE && def.id.equals("shinohara");
        if (ult.source == MoveDef.Source.QUINQUE && !arata) return;
        int base = arata ? 0x1E1E24 : darken(def.kaguneColor, 0.55f);
        int glow = arata ? 0xD02020 : def.kaguneColor;
        Paint plate = Paint.of(arata ? Mat.DARK_METAL : Mat.CHITIN, base).pattern(arata ? Pattern.PLATES : Pattern.RIBS, glow, 0.8f).accentGlow();
        Paint spikeP = Paint.of(Mat.CHITIN, darken(def.kaguneColor, 0.75f));
        float cw = spec.chestW, ch = spec.chestH, cd = spec.chestD;
        Part c = b.chest.child("form_chest", 0, 0, 0);
        c.box(-cw / 2 - 0.6f, -ch - 0.4f, -cd / 2 - 0.6f, cw + 1.2f, ch + 0.4f, cd + 1.2f, plate);
        for (int i = 0; i < 5; i++) {
            // centipede-style spine segments down the back
            c.box(-1.8f + i * 0.1f, -ch + i * 1.5f, cd / 2 + 0.4f, 3.6f - i * 0.2f, 1.2f, 1.4f, plate);
            if (!arata) c.spike(0, -ch + i * 1.5f + 0.5f, cd / 2 + 1.5f, 1.2f, 2.4f, 120, 0, 0, spikeP);
        }
        b.rArm.child("form_rarm", 0, 0, 0).box(-spec.armW / 2 - 0.6f, -spec.armW * 0.5f - 0.6f, -spec.armW / 2 - 0.6f, spec.armW + 1.2f, spec.armW + 1.4f,
                spec.armW + 1.2f, plate);
        b.lArm.child("form_larm", 0, 0, 0).box(-spec.armW / 2 - 0.6f, -spec.armW * 0.5f - 0.6f, -spec.armW / 2 - 0.6f, spec.armW + 1.2f, spec.armW + 1.4f,
                spec.armW + 1.2f, plate);
        b.rFore.child("form_rfore", 0, 0, 0).box(-spec.armW / 2 - 0.4f, 0, -spec.armW / 2 - 0.4f, spec.armW + 0.8f, spec.foreArm, spec.armW + 0.8f, plate);
        b.lFore.child("form_lfore", 0, 0, 0).box(-spec.armW / 2 - 0.4f, 0, -spec.armW / 2 - 0.4f, spec.armW + 0.8f, spec.foreArm, spec.armW + 0.8f, plate);
        if (!arata) {
            b.rFore.child("form_rblade", 0, spec.foreArm * 0.5f, spec.armW / 2).spike(0, 0, 0, 1.4f, 5, 140, 0, 0, spikeP);
            b.lFore.child("form_lblade", 0, spec.foreArm * 0.5f, spec.armW / 2).spike(0, 0, 0, 1.4f, 5, 140, 0, 0, spikeP);
        }
        b.rThigh.child("form_rthigh", 0, 0, 0).box(-spec.legW / 2 - 0.5f, -0.5f, -spec.legW / 2 - 0.5f, spec.legW + 1.0f, spec.thigh + 0.5f, spec.legW + 1.0f, plate);
        b.lThigh.child("form_lthigh", 0, 0, 0).box(-spec.legW / 2 - 0.5f, -0.5f, -spec.legW / 2 - 0.5f, spec.legW + 1.0f, spec.thigh + 0.5f, spec.legW + 1.0f, plate);
        b.rShin.child("form_rshin", 0, 0, 0).box(-spec.legW / 2 - 0.4f, 0, -spec.legW / 2 - 0.4f, spec.legW + 0.8f, spec.shin, spec.legW + 0.8f, plate);
        b.lShin.child("form_lshin", 0, 0, 0).box(-spec.legW / 2 - 0.4f, 0, -spec.legW / 2 - 0.4f, spec.legW + 0.8f, spec.shin, spec.legW + 0.8f, plate);
        Part h = b.head.child("form_head", 0, 0, 0);
        Paint helm = plate.copy();
        if (arata) helm.decal(Face.FRONT, DecalType.VISOR, 0xFF2020, true);
        else helm.decal(Face.FRONT, DecalType.GRIN, 0xE8E0D0, false, 0.5f, 0.78f, 1.1f);
        h.box(-4.7f, -8.7f, -4.7f, 9.4f, 8.8f, 9.4f, helm);
        if (!arata) {
            h.spike(-2.5f, -8.4f, 1, 1.6f, 4.5f, -20, 0, 25, spikeP);
            h.spike(2.5f, -8.4f, 1, 1.6f, 4.5f, -20, 0, -25, spikeP);
        }
    }
}
