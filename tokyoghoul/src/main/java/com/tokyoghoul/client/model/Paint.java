package com.tokyoghoul.client.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Surface description for one model box: a physical material (drives shading, specular, micro
 * detail), base / accent colours, an optional pattern overlay, trim bands, grime, emissive
 * treatment and any number of per-face decals.
 */
public final class Paint {
    /** Physical materials. Each has its own shading model in {@link TextureBaker}. */
    public enum Mat {
        PLAIN, SKIN, FLESH, ROT, BONE, CLOTH, LEATHER, METAL, DARK_METAL, GOLD, STONE, WOOD, FUR, SCALES, CRYSTAL,
        ENERGY, ICE, CHITIN, FEATHER, ROBE, HAIR, PLATE, RUBBER, GLASS
    }

    public enum Pattern {
        NONE, STRIPES, BANDS, SPOTS, PLATES, SCALES, GRADIENT, CHECKER, RUNES, CRACKS, FUR, WOOD, RIBS, CLOTH, VEINS, FEATHERS,
        BRICK, HEX, HAZARD, CIRCUIT, CHAINMAIL, MUSCLE
    }

    public enum Face {TOP, BOTTOM, RIGHT, FRONT, LEFT, BACK}

    public enum DecalType {
        EYES, SLIT_EYES, HOLLOW_EYES, VISOR, MOUTH, GRIN, FANGS, CROSS, RUNE, EMBLEM, STRIPE, HSTRIPE, CORE, DOTS, SKULL, MASK,
        GRILLE, PANEL, STITCH, SCAR, BLOOD, GEM, SUN, STAR, BOLT, EYE_SIGIL, MOON, FLAME, CHEVRON, RING, NOSE, BEARD, BROW
    }

    /** A decal drawn in normalised face space. x/y/size are fractions of the face (0..1). */
    public record Decal(DecalType type, int color, boolean glow, float x, float y, float size) {
    }

    public Mat mat;
    public int base;
    public int second = -1;
    public int trim = -1;
    public float trimWidth = 1f;
    public float noise = -1f;
    public float grime = 0f;
    public Pattern pattern = Pattern.NONE;
    public float patternScale = 1f;
    public boolean glow;
    /** Partial emissive: pattern/accents glow, base does not. */
    public boolean accentGlow;
    public boolean outline = true;
    public float shade = 1f;
    public float gloss = -1f;
    public final Map<Face, List<Decal>> decals = new EnumMap<>(Face.class);

    private Paint(Mat mat, int base) {
        this.mat = mat;
        this.base = base;
    }

    public static Paint of(int rgb) {
        return new Paint(Mat.PLAIN, rgb);
    }

    public static Paint of(Mat mat, int rgb) {
        return new Paint(mat, rgb);
    }

    public static Paint glowing(int rgb) {
        Paint p = new Paint(Mat.ENERGY, rgb);
        p.glow = true;
        return p;
    }

    public Paint mat(Mat m) {
        this.mat = m;
        return this;
    }

    public Paint noise(float n) {
        this.noise = n;
        return this;
    }

    public Paint pattern(Pattern p, int second) {
        this.pattern = p;
        this.second = second;
        return this;
    }

    public Paint pattern(Pattern p, int second, float scale) {
        this.pattern = p;
        this.second = second;
        this.patternScale = scale;
        return this;
    }

    /** Secondary colour used by materials (moss on stone, rot on flesh, inner glow on crystal...). */
    public Paint accent(int c) {
        this.second = c;
        return this;
    }

    public Paint trim(int color) {
        this.trim = color;
        return this;
    }

    public Paint trim(int color, float width) {
        this.trim = color;
        this.trimWidth = width;
        return this;
    }

    public Paint grime(float g) {
        this.grime = g;
        return this;
    }

    public Paint glow() {
        this.glow = true;
        return this;
    }

    public Paint accentGlow() {
        this.accentGlow = true;
        return this;
    }

    public Paint flat() {
        this.outline = false;
        return this;
    }

    public Paint dark(float f) {
        this.shade = f;
        return this;
    }

    public Paint gloss(float g) {
        this.gloss = g;
        return this;
    }

    public Paint decal(Face f, DecalType t, int color, boolean glow) {
        return decal(f, t, color, glow, 0.5f, 0.5f, 1f);
    }

    public Paint decal(Face f, DecalType t, int color, boolean glow, float x, float y, float size) {
        decals.computeIfAbsent(f, k -> new ArrayList<>()).add(new Decal(t, color, glow, x, y, size));
        return this;
    }

    /** Kept for older model code: single decal on a face. */
    public Paint face(Face f, DecalType t, int color, boolean glow) {
        return decal(f, t, color, glow);
    }

    public Paint eyes(int color, boolean glow) {
        return decal(Face.FRONT, DecalType.EYES, color, glow, 0.5f, 0.42f, 1f);
    }

    public Paint copy() {
        Paint p = new Paint(mat, base);
        p.second = second;
        p.trim = trim;
        p.trimWidth = trimWidth;
        p.noise = noise;
        p.grime = grime;
        p.pattern = pattern;
        p.patternScale = patternScale;
        p.glow = glow;
        p.accentGlow = accentGlow;
        p.outline = outline;
        p.shade = shade;
        p.gloss = gloss;
        for (Map.Entry<Face, List<Decal>> e : decals.entrySet()) p.decals.put(e.getKey(), new ArrayList<>(e.getValue()));
        return p;
    }

    /** Copy without decals (for limbs sharing a torso material). */
    public Paint plain() {
        Paint p = copy();
        p.decals.clear();
        return p;
    }

    public Paint color(int rgb) {
        Paint p = copy();
        p.base = rgb;
        return p;
    }
}
