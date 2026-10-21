package com.tokyoghoul.client.model;

import com.tokyoghoul.client.model.ModelSpec.Part;
import com.tokyoghoul.client.model.Paint.Mat;
import com.tokyoghoul.client.model.Paint.Pattern;
import com.tokyoghoul.data.KaguneType;
import com.tokyoghoul.move.Anims;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

import static com.tokyoghoul.client.model.Anim.hold;
import static com.tokyoghoul.client.model.Anim.pulse;
import static com.tokyoghoul.client.model.Anim.win;

/**
 * Kagune geometry and motion. Models are baked in pale tones and tinted with each character's colour at
 * render time, so one bake per (type, size) serves everyone. Rinkaku and Bikaku grow from the lower
 * back, Ukaku and Koukaku from the shoulder blades. Segments are chained parts, so bending each joint
 * a little curls the whole tentacle or tail; extending a thrust scales the segments.
 */
public final class KaguneModels {
    public static final int SEGMENTS = 8, TAIL_SEGMENTS = 10, FEATHERS = 7;
    private static final Map<String, ModelSpec.Baked> CACHE = new HashMap<>();

    private KaguneModels() {
    }

    public static ModelSpec.Baked baked(KaguneType type, int count) {
        int n = type == KaguneType.KOUKAKU ? 1 : Math.max(1, Math.min(8, count));
        return CACHE.computeIfAbsent(type.id + "_" + n, k -> {
            ModelSpec s = new ModelSpec("kagune_" + k, 4);
            switch (type) {
                case RINKAKU -> rinkaku(s, n);
                case UKAKU -> ukaku(s, n);
                case KOUKAKU -> koukaku(s, n);
                case BIKAKU -> bikaku(s, n);
                default -> {
                }
            }
            return s.bake();
        });
    }

    /** Lower-back kagune anchor (pelvis space) vs. shoulder-blade anchor (chest space). */
    /** Koukaku is worn on the forearms rather than the back. */
    public static boolean onArms(KaguneType t) {
        return t == KaguneType.KOUKAKU;
    }

    public static boolean fromLowerBack(KaguneType t) {
        return t == KaguneType.RINKAKU || t == KaguneType.BIKAKU;
    }

    private static Paint flesh() {
        return Paint.of(Mat.FLESH, 0xE6E6E6).pattern(Pattern.SCALES, 0xFFFFFF, 0.7f).accentGlow().noise(0.15f);
    }

    private static Paint crystal() {
        return Paint.of(Mat.CRYSTAL, 0xF4F4F4).accent(0xFFFFFF).glow();
    }

    private static Paint plate() {
        return Paint.of(Mat.CHITIN, 0xE0E0E0).pattern(Pattern.PLATES, 0xFFFFFF, 0.8f).accentGlow().gloss(0.6f);
    }

    // ------------------------------------------------------------------ build

    private static void rinkaku(ModelSpec s, int n) {
        Part root = s.root.child("rk", 0, 0, 0);
        Paint f = flesh();
        for (int i = 0; i < n; i++) {
            float x = (i - (n - 1) / 2f) * 1.2f;
            Part prev = root.child("t" + i, x, 0, 0);
            for (int j = 0; j < SEGMENTS; j++) {
                float w = Math.max(1.3f, 3.6f - j * 0.3f);
                Part seg = prev.child("t" + i + "_" + j, 0, j == 0 ? 0 : -4.5f, 0);
                seg.box(-w / 2, -4.7f, -w / 2, w, 4.9f, w, f);
                if (j % 2 == 0 && j > 0) seg.spike(0, -2.5f, w / 2, w * 0.45f, 1.6f, 70, 0, 0, f);
                if (j == SEGMENTS - 1) seg.spike(0, -4.5f, 0, w, 4.5f, 0, 0, 0, crystalBlade());
                prev = seg;
            }
        }
    }

    private static Paint crystalBlade() {
        return Paint.of(Mat.CHITIN, 0xF0F0F0).accent(0xFFFFFF).glow();
    }

    private static void bikaku(ModelSpec s, int n) {
        Part root = s.root.child("bk", 0, 0, 0);
        Paint f = flesh();
        for (int i = 0; i < n; i++) {
            Part prev = root.child("tail" + i, (i - (n - 1) / 2f) * 2f, 0, 0);
            for (int j = 0; j < TAIL_SEGMENTS; j++) {
                float w = Math.max(1.1f, 3.4f - j * 0.24f);
                Part seg = prev.child("tail" + i + "_" + j, 0, j == 0 ? 0 : -3.8f, 0);
                seg.box(-w / 2, -4f, -w / 2, w, 4.2f, w, f);
                seg.spike(0, -2, w / 2, w * 0.5f, 1.8f, 75, 0, 0, plate());
                if (j == TAIL_SEGMENTS - 1) {
                    seg.spike(0, -3.8f, 0, w * 1.2f, 6f, 0, 0, 0, crystalBlade());
                    seg.spike(0, -3.0f, 0, w * 0.8f, 3.5f, 0, 0, 40, crystalBlade());
                    seg.spike(0, -3.0f, 0, w * 0.8f, 3.5f, 0, 0, -40, crystalBlade());
                }
                prev = seg;
            }
        }
    }

    private static void ukaku(ModelSpec s, int n) {
        Part root = s.root.child("uk", 0, 0, 0);
        int wings = n >= 2 ? 2 : 1;
        float size = 1.15f + (n - 2) * 0.2f;
        for (int w = 0; w < wings; w++) {
            int side = w == 0 ? 1 : -1;
            Part wing = root.child("w" + w, side * 2.4f, 0, 0.4f);
            for (int k = 0; k < FEATHERS; k++) {
                float len = (11 + k * 2.6f) * size;
                Part fth = wing.child("w" + w + "_f" + k, 0, 0, 0).rot(-30 - k * 3, side * 8, -side * (22 + k * 15));
                fth.taper(0, 0, 0, 2.8f * size, 1.0f, 0.3f, 0.2f, len, 5, crystal());
                if (k % 2 == 0) fth.taper(0, -len * 0.35f, 0.4f, 1.6f * size, 0.6f, 0.2f, 0.2f, len * 0.5f, 3, crystal());
            }
        }
    }

    /**
     * Koukaku is worn on the forearm (Tsukiyama's drill-lance, Urie's blades, Hinami's shield): a plated
     * sheath plus a blade running past the hand. Built in forearm space (+Y runs toward the hand); the
     * layer renders it on one or both arms.
     */
    private static void koukaku(ModelSpec s, int n) {
        Part root = s.root.child("kou0", 0, 0, 0);
        Paint p = plate();
        root.box(-2.6f, -0.5f, -2.6f, 5.2f, 6.5f, 5.2f, p);
        root.spike(2.4f, 1.5f, 0, 1.4f, 3.2f, 0, 0, -105, p);
        root.spike(-2.4f, 1.5f, 0, 1.4f, 3.2f, 0, 0, 105, p);
        root.spike(0, 1.0f, 2.4f, 1.2f, 2.6f, -100, 0, 0, p);
        Part blade = root.child("kou0_blade", 0, 5.5f, -0.6f).rot(180, 0, 0);
        blade.taper(0, 0, 0, 4.4f, 2.2f, 0.3f, 0.3f, 15, 6, p);
        blade.spike(0, -4, 0, 1.6f, 3.5f, 0, 0, 55, crystalBlade());
        blade.spike(0, -4, 0, 1.6f, 3.5f, 0, 0, -55, crystalBlade());
        blade.spike(0, -14.5f, 0, 1.2f, 5, 0, 0, 0, crystalBlade());
    }

    // ------------------------------------------------------------------ animate

    /** Reset and pose a kagune for this frame. */
    public static void animate(ModelSpec.Baked b, KaguneType type, int count, int anim, float t, float age, boolean form) {
        b.root.getAllParts().forEach(ModelPart::resetPose);
        int n = Math.max(1, Math.min(8, count));
        switch (type) {
            case RINKAKU -> animRinkaku(b, n, anim, t, age, form);
            case BIKAKU -> animBikaku(b, n, anim, t, age, form);
            case UKAKU -> animUkaku(b, n, anim, t, age, form);
            case KOUKAKU -> animKoukaku(b, n, anim, t, age, form);
            default -> {
            }
        }
    }

    private static boolean strike(int anim) {
        return anim == Anims.K_PIERCE || anim == Anims.THRUST || anim == Anims.HEAVY || anim == Anims.OVERHEAD;
    }

    private static void animRinkaku(ModelSpec.Baked b, int n, int anim, float t, float age, boolean form) {
        float wr = form ? 2.2f : 1f;
        for (int i = 0; i < n; i++) {
            float spread = n == 1 ? 0 : (i / (n - 1f) - 0.5f) * 1.6f;
            ModelPart base = b.get("t" + i);
            float bx = -0.65f, by = spread, bz = spread * 0.4f, seg = 0.13f, ext = 1f;
            switch (anim) {
                case Anims.K_PIERCE, Anims.THRUST -> {
                    float k = hold(t, 2 + i * 2, 12 + i * 2, 4);
                    bx = Mth.lerp(k, bx, 0.35f);
                    by = Mth.lerp(k, by, spread * 0.12f);
                    seg = Mth.lerp(k, seg, 0.17f);
                    ext = 1 + 0.9f * k;
                }
                case Anims.K_BARRAGE -> {
                    float ph = (t * 0.45f + i * 0.27f) % 1f;
                    float k = Mth.sin(ph * Mth.PI);
                    bx = Mth.lerp(k, -0.3f, 0.12f);
                    by = Mth.lerp(k, spread, spread * 0.2f);
                    seg = Mth.lerp(k, 0.16f, 0.2f);
                    ext = 1 + 0.8f * k;
                }
                case Anims.K_SWEEP, Anims.SPIN -> {
                    float k = hold(t, 2, 18, 3);
                    bx = Mth.lerp(k, bx, -1.45f);
                    by = spread + k * win(t, 3, 14) * Mth.TWO_PI;
                    seg = Mth.lerp(k, seg, 0.03f);
                    ext = 1 + 0.5f * k;
                }
                case Anims.K_GRAB -> {
                    float reach = hold(t, 4, 30, 4);
                    float lift = win(t, 10, 18);
                    if (i < 2) {
                        bx = Mth.lerp(reach, bx, 0.15f - lift * 0.4f);
                        by = Mth.lerp(reach, by, spread * 0.15f);
                        seg = Mth.lerp(reach, seg, 0.19f - lift * 0.15f);
                        ext = 1 + 0.6f * reach;
                    }
                }
                case Anims.K_SPIKES -> {
                    float k = hold(t, 2, 18, 4);
                    bx = Mth.lerp(k, bx, 0.6f);
                    seg = Mth.lerp(k, seg, 0.27f);
                    ext = 1 + 0.4f * k;
                }
                case Anims.GUARD, Anims.K_SHIELD -> {
                    bx = 0.15f;
                    by = spread * 0.6f;
                    seg = 0.32f;
                }
                case Anims.TRANSFORM, Anims.REGEN -> wr = 4f;
                default -> {
                    if (strike(anim)) {
                        float k = pulse(t, 2, 10);
                        bx = Mth.lerp(k, bx, 0.05f);
                        seg = Mth.lerp(k, seg, 0.2f);
                        ext = 1 + 0.6f * k;
                    }
                }
            }
            base.xRot = bx;
            base.yRot = by;
            base.zRot = bz;
            for (int j = 0; j < SEGMENTS; j++) {
                ModelPart sp = b.get("t" + i + "_" + j);
                float wave = Mth.sin(age * 0.11f * wr + i * 1.3f + j * 0.55f) * 0.09f;
                sp.xRot = seg + wave;
                sp.zRot = Mth.cos(age * 0.07f * wr + i * 2.1f + j * 0.4f) * 0.06f;
                sp.yScale = ext;
                if (form) {
                    sp.xScale = 1.3f;
                    sp.zScale = 1.3f;
                }
            }
        }
    }

    private static void animBikaku(ModelSpec.Baked b, int n, int anim, float t, float age, boolean form) {
        for (int i = 0; i < n; i++) {
            ModelPart base = b.get("tail" + i);
            float bx = -1.9f, by = Mth.sin(age * 0.05f + i) * 0.25f, seg = -0.1f, ext = 1f;
            switch (anim) {
                case Anims.K_PIERCE, Anims.THRUST -> {
                    float k = hold(t, 2, 12, 4);
                    bx = Mth.lerp(k, bx, -0.25f);
                    by = Mth.lerp(k, by, 0);
                    seg = Mth.lerp(k, seg, 0.17f);
                    ext = 1 + 0.6f * k;
                }
                case Anims.K_SWEEP, Anims.SPIN, Anims.SLASH -> {
                    float k = hold(t, 2, 16, 3);
                    bx = Mth.lerp(k, bx, -1.55f);
                    by = k * win(t, 3, 14) * Mth.TWO_PI;
                    seg = Mth.lerp(k, seg, 0.0f);
                    ext = 1 + 0.4f * k;
                }
                case Anims.K_SPIKES, Anims.OVERHEAD -> {
                    float k = hold(t, 2, 14, 4);
                    bx = Mth.lerp(k, bx, 0.2f);
                    seg = Mth.lerp(k, seg, 0.24f);
                    ext = 1 + 0.3f * k;
                }
                case Anims.GUARD, Anims.K_SHIELD -> {
                    bx = -1.4f;
                    by = 1.2f;
                    seg = 0.12f;
                }
                default -> {
                }
            }
            base.xRot = bx;
            base.yRot = by;
            for (int j = 0; j < TAIL_SEGMENTS; j++) {
                ModelPart sp = b.get("tail" + i + "_" + j);
                sp.xRot = seg + Mth.sin(age * 0.09f + j * 0.5f) * 0.05f;
                sp.zRot = Mth.sin(age * 0.06f + j * 0.45f + i) * 0.08f;
                sp.yScale = ext;
                if (form) {
                    sp.xScale = 1.3f;
                    sp.zScale = 1.3f;
                }
            }
        }
    }

    private static void animUkaku(ModelSpec.Baked b, int n, int anim, float t, float age, boolean form) {
        int wings = n >= 2 ? 2 : 1;
        for (int w = 0; w < wings; w++) {
            int side = w == 0 ? 1 : -1;
            ModelPart wing = b.get("w" + w);
            float flare = 0, back = 0, scale = form ? 1.35f : 1f;
            switch (anim) {
                case Anims.K_VOLLEY, Anims.CAST -> {
                    flare = hold(t, 1, 18, 3);
                    scale *= 1 + 0.25f * flare;
                    wing.zRot += Mth.sin(t * 2.2f) * 0.05f * flare;
                }
                case Anims.DASH -> back = 1;
                case Anims.GUARD, Anims.K_SHIELD -> {
                    wing.yRot = side * -1.2f;
                    wing.z = -1f;
                }
                case Anims.K_SWEEP, Anims.SLASH -> {
                    float k = pulse(t, 2, 14);
                    wing.yRot = side * -1.4f * k;
                    scale *= 1 + 0.3f * k;
                }
                case Anims.TRANSFORM -> flare = 1;
                default -> {
                }
            }
            wing.zRot += side * (-0.15f * flare + Mth.sin(age * 0.08f + w) * 0.05f);
            wing.xRot = -0.25f * flare + 0.7f * back;
            wing.xScale = wing.yScale = wing.zScale = scale;
        }
    }

    private static void animKoukaku(ModelSpec.Baked b, int n, int anim, float t, float age, boolean form) {
        ModelPart sheath = b.get("kou0"), blade = b.get("kou0_blade");
        float ext = 1f, wide = 1f;
        switch (anim) {
            case Anims.K_SHIELD, Anims.GUARD -> {
                wide = 2.4f;
                ext = 0.55f;
            }
            case Anims.K_PIERCE, Anims.THRUST, Anims.DASH -> ext = 1 + 0.7f * (anim == Anims.DASH ? 1 : hold(t, 2, 12, 4));
            case Anims.K_SWEEP, Anims.SLASH, Anims.SPIN -> ext = 1 + 0.4f * pulse(t, 2, 14);
            case Anims.K_SPIKES -> ext = 1 + 0.5f * hold(t, 2, 14, 4);
            default -> {
            }
        }
        // Tsukiyama's drill spins while thrusting
        if (anim == Anims.K_PIERCE || anim == Anims.THRUST || anim == Anims.DASH) blade.yRot = age * 0.9f;
        float f = form ? 1.3f : 1f;
        sheath.xScale = wide * f;
        sheath.zScale = (wide > 1 ? 1.2f : 1f) * f;
        blade.yScale = ext;
        blade.xScale = blade.zScale = (wide > 1 ? 1.8f : 1f) * f;
    }
}
