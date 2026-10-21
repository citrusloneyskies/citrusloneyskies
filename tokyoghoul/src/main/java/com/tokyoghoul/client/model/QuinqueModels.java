package com.tokyoghoul.client.model;

import com.tokyoghoul.client.model.ModelSpec.Part;
import com.tokyoghoul.client.model.Paint.Mat;
import com.tokyoghoul.client.model.Paint.Pattern;
import com.tokyoghoul.item.QuinqueKind;

/**
 * Quinque models. Built pointing up (-Y) from the grip at the origin; {@link #length} is the overall
 * size used to fit them in hands, on the ground and in the inventory.
 */
public final class QuinqueModels {
    private QuinqueModels() {
    }

    public static float length(QuinqueKind k) {
        return switch (k.style) {
            case LANCE -> 34;
            case SCYTHE -> 32;
            case SPEAR -> 30;
            case GREATSWORD, GREATCLUB -> 26;
            case SWORD, CLUB, WHIP, SHIELD_BLADE -> 22;
            case CASE_BLASTER -> 16;
            case KNIFE -> 10;
        };
    }

    public static void build(QuinqueKind k, ModelSpec s) {
        Paint main = Paint.of(Mat.METAL, k.color).gloss(0.7f);
        Paint kag = Paint.of(Mat.CHITIN, k.color).pattern(Pattern.VEINS, k.accent, 0.8f).accentGlow().gloss(0.5f);
        Paint grip = Paint.of(Mat.LEATHER, 0x1A1A1E).pattern(Pattern.BANDS, 0x2A2A30, 0.6f);
        Paint acc = Paint.of(Mat.METAL, k.accent).gloss(0.6f);
        Paint glow = Paint.glowing(k.accent);
        Part r = s.root.child("q", 0, 0, 0);
        switch (k.style) {
            case LANCE -> {
                r.box(-0.8f, -4, -0.8f, 1.6f, 8, 1.6f, grip);
                r.box(-0.6f, -20, -0.6f, 1.2f, 16, 1.2f, main);
                r.box(-2.2f, -21, -2.2f, 4.4f, 1.4f, 4.4f, acc);
                r.taper(0, -21, 0, 4.2f, 4.2f, 0.4f, 0.4f, 13, 7, kag);
                r.box(-0.25f, -30, -2.3f, 0.5f, 8, 0.3f, glow);
            }
            case SWORD -> {
                r.box(-0.7f, 0, -0.7f, 1.4f, 5, 1.4f, grip);
                r.box(-2.4f, -1, -1, 4.8f, 1, 2, acc);
                r.box(-1.0f, -16, -0.3f, 2.0f, 15, 0.6f, kag);
                r.taper(0, -16, 0, 2.0f, 0.6f, 0.2f, 0.2f, 4, 3, kag);
            }
            case GREATSWORD -> {
                r.box(-0.8f, 0, -0.8f, 1.6f, 6, 1.6f, grip);
                r.box(-3.4f, -1.2f, -1.2f, 6.8f, 1.2f, 2.4f, acc);
                r.box(-1.8f, -19, -0.4f, 3.6f, 18, 0.8f, kag);
                r.taper(0, -19, 0, 3.6f, 0.8f, 0.3f, 0.3f, 5, 3, kag);
            }
            case CLUB -> {
                r.box(-0.8f, 0, -0.8f, 1.6f, 8, 1.6f, grip);
                r.box(-0.7f, -8, -0.7f, 1.4f, 8, 1.4f, main);
                r.octo(0, -18, 0, 5, 10, 5, kag);
                for (int i = 0; i < 4; i++) r.spike(0, -13, 0, 1.4f, 4, 90, i * 90, 0, acc);
            }
            case GREATCLUB -> {
                r.box(-1, 0, -1, 2, 8, 2, grip);
                r.box(-0.9f, -8, -0.9f, 1.8f, 8, 1.8f, main);
                r.octo(0, -22, 0, 8, 14, 8, kag);
                for (int i = 0; i < 6; i++) r.spike(0, -15 - (i % 2) * 3, 0, 1.8f, 5, 90, i * 60, 0, acc);
            }
            case KNIFE -> {
                r.box(-0.5f, 0, -0.5f, 1, 3.5f, 1, grip);
                r.box(-1.2f, -0.6f, -0.6f, 2.4f, 0.6f, 1.2f, acc);
                r.taper(0, -0.6f, 0, 1.6f, 0.4f, 0.2f, 0.2f, 7, 4, kag);
            }
            case SCYTHE -> {
                r.box(-0.7f, -24, -0.7f, 1.4f, 28, 1.4f, main);
                r.box(-0.9f, -2, -0.9f, 1.8f, 5, 1.8f, grip);
                Part blade = r.child("q_blade", 0, -23, 0).rot(0, 0, -100);
                blade.taper(0, 0, 0, 3.4f, 0.6f, 0.3f, 0.2f, 16, 6, kag);
                r.spike(0, -24, 0, 1.4f, 5, 0, 0, 0, acc);
            }
            case WHIP -> {
                r.box(-0.8f, 0, -0.8f, 1.6f, 5, 1.6f, grip);
                Part prev = r;
                for (int i = 0; i < 6; i++) {
                    Part seg = prev.child("q_seg" + i, 0, i == 0 ? -0.5f : -3.2f, 0).rot(0, 0, 8);
                    seg.box(-1.0f + i * 0.08f, -3.4f, -0.6f, 2.0f - i * 0.16f, 3.4f, 1.2f, kag);
                    seg.spike(1, -1.6f, 0, 0.6f, 1.6f, 0, 0, -70, acc);
                    prev = seg;
                }
            }
            case SPEAR -> {
                r.box(-0.6f, -18, -0.6f, 1.2f, 22, 1.2f, main);
                r.box(-0.8f, -2, -0.8f, 1.6f, 6, 1.6f, grip);
                r.taper(0, -18, 0, 3.2f, 0.8f, 0.2f, 0.2f, 9, 5, kag);
                r.spike(0, -19, 0, 1.2f, 3, 0, 0, 55, kag);
                r.spike(0, -19, 0, 1.2f, 3, 0, 0, -55, kag);
            }
            case CASE_BLASTER -> {
                r.box(-0.8f, 0, -0.8f, 1.6f, 4, 1.6f, grip);
                r.box(-3.5f, -9, -2, 7, 9, 4, main.copy().trim(k.accent, 0.5f));
                r.box(-2.6f, -15, -0.6f, 1.2f, 6, 1.2f, acc);
                r.box(1.4f, -15, -0.6f, 1.2f, 6, 1.2f, acc);
                r.box(-0.5f, -14, -0.5f, 1, 5, 1, glow);
            }
            case SHIELD_BLADE -> {
                r.box(-0.7f, 0, -0.7f, 1.4f, 5, 1.4f, grip);
                r.box(-4, -12, -0.8f, 8, 11, 1.6f, kag);
                r.taper(0, -12, 0, 4, 1.2f, 0.3f, 0.3f, 8, 4, kag);
            }
        }
    }
}
