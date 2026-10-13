package com.lastbastion.client.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Describes how one model box is textured: base colour, noise, a surface pattern, optional
 * emissive (glow) treatment and per-face decals such as eyes, visors or runes.
 */
public final class Paint {
    public enum Pattern {
        NONE, STRIPES, BANDS, SPOTS, PLATES, SCALES, GRADIENT, CHECKER, RUNES, CRACKS, FUR, WOOD, RIBS, CLOTH, VEINS, FEATHERS, BRICK
    }

    public enum Face {TOP, BOTTOM, RIGHT, FRONT, LEFT, BACK}

    public enum DecalType {EYES, SLIT_EYES, VISOR, MOUTH, GRIN, CROSS, RUNE, EMBLEM, STRIPE, CORE, DOTS, SKULL, MASK}

    public record Decal(DecalType type, int color, boolean glow) {
    }

    public int base;
    public int second = -1;
    public float noise = 0.10f;
    public Pattern pattern = Pattern.NONE;
    public boolean glow;
    public boolean outline = true;
    public float shade = 1f;
    public final Map<Face, Decal> decals = new EnumMap<>(Face.class);

    private Paint(int base) {
        this.base = base;
    }

    public static Paint of(int rgb) {
        return new Paint(rgb);
    }

    public static Paint glowing(int rgb) {
        Paint p = new Paint(rgb);
        p.glow = true;
        p.noise = 0.05f;
        return p;
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

    public Paint glow() {
        this.glow = true;
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

    public Paint face(Face f, DecalType t, int color, boolean glow) {
        decals.put(f, new Decal(t, color, glow));
        return this;
    }

    /** Front-face eyes, the most common decal. */
    public Paint eyes(int color, boolean glow) {
        return face(Face.FRONT, DecalType.EYES, color, glow);
    }

    public Paint copy() {
        Paint p = new Paint(base);
        p.second = second;
        p.noise = noise;
        p.pattern = pattern;
        p.glow = glow;
        p.outline = outline;
        p.shade = shade;
        p.decals.putAll(decals);
        return p;
    }
}
