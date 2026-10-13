package com.lastbastion.client.model;

import com.lastbastion.LastBastion;
import com.lastbastion.client.model.Paint.Decal;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Procedural texture painter. Paints every box face of a {@link ModelSpec} at several texels per
 * model pixel using physically-flavoured material models (brushed metal with specular sweeps,
 * woven cloth with folds, mottled flesh with veins, fur strands, faceted crystal...), ambient
 * occlusion gradients, bevel highlights, grime, trim bands, pattern overlays and vector decals
 * (eyes with pupils and highlights, visors, sigils, runes). Emissive texels go to a second texture
 * rendered full-bright.
 */
public final class TextureBaker {
    public record Result(ResourceLocation texture, @Nullable ResourceLocation glow) {
    }

    private TextureBaker() {
    }

    public static float detailScale() {
        return Mth.clamp(com.lastbastion.config.ClientConfig.textureDetail(), 0.25f, 1.5f);
    }

    /** Working canvas for one model. */
    private static final class Canvas {
        final int w, h, dens;
        final int[] col, glow;
        boolean anyGlow;

        Canvas(int w, int h, int dens) {
            this.w = w;
            this.h = h;
            this.dens = dens;
            col = new int[w * h];
            glow = new int[w * h];
        }

        void put(int x, int y, int rgb, boolean emissive) {
            if (x < 0 || y < 0 || x >= w || y >= h) return;
            col[y * w + x] = 0xFF000000 | (rgb & 0xFFFFFF);
            if (emissive) {
                glow[y * w + x] = 0xFF000000 | (rgb & 0xFFFFFF);
                anyGlow = true;
            } else {
                glow[y * w + x] = 0;
            }
        }

        int get(int x, int y) {
            if (x < 0 || y < 0 || x >= w || y >= h) return 0;
            return col[y * w + x] & 0xFFFFFF;
        }
    }

    public static Result paint(String name, List<ModelSpec.Box> boxes, int uvW, int uvH, int dens) {
        Canvas c = new Canvas(uvW * dens, uvH * dens, dens);
        int seed = name.hashCode();
        for (int bi = 0; bi < boxes.size(); bi++) {
            ModelSpec.Box b = boxes.get(bi);
            float u = b.u, v = b.v, w = b.w, h = b.h, d = b.d;
            int s = seed * 31 + bi * 7919;
            face(c, u + d, v, w, d, Face.TOP, b.paint, s);
            face(c, u + d + w, v, w, d, Face.BOTTOM, b.paint, s + 1);
            face(c, u, v + d, d, h, Face.RIGHT, b.paint, s + 2);
            face(c, u + d, v + d, w, h, Face.FRONT, b.paint, s + 3);
            face(c, u + d + w, v + d, d, h, Face.LEFT, b.paint, s + 4);
            face(c, u + d + w + d, v + d, w, h, Face.BACK, b.paint, s + 5);
        }
        ResourceLocation texLoc = new ResourceLocation(LastBastion.MODID, "dynamic/" + name);
        upload(texLoc, c.col, c.w, c.h);
        ResourceLocation glowLoc = null;
        if (c.anyGlow) {
            glowLoc = new ResourceLocation(LastBastion.MODID, "dynamic/" + name + "_glow");
            upload(glowLoc, c.glow, c.w, c.h);
        }
        return new Result(texLoc, glowLoc);
    }

    private static void upload(ResourceLocation loc, int[] argb, int w, int h) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, w, h, true);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = argb[y * w + x];
                int a = (c >>> 24) & 0xFF, r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, bl = c & 0xFF;
                img.setPixelRGBA(x, y, (a << 24) | (bl << 16) | (g << 8) | r);
            }
        }
        Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(img));
    }

    // ------------------------------------------------------------------ face painting

    private static void face(Canvas c, float fu0, float fv0, float fw, float fh, Face face, Paint p, int seed) {
        if (fw <= 0.001f || fh <= 0.001f) return;
        int dens = c.dens;
        int x0 = (int) Math.floor(fu0 * dens), y0 = (int) Math.floor(fv0 * dens);
        int x1 = (int) Math.ceil((fu0 + fw) * dens), y1 = (int) Math.ceil((fv0 + fh) * dens);
        int tw = Math.max(1, x1 - x0), th = Math.max(1, y1 - y0);
        boolean side = face != Face.TOP && face != Face.BOTTOM;
        float faceLight = switch (face) {
            case TOP -> 1.12f;
            case BOTTOM -> 0.64f;
            case FRONT -> 1.0f;
            case BACK -> 0.88f;
            default -> 0.9f;
        };
        float noiseAmt = p.noise >= 0 ? p.noise : defaultNoise(p.mat);
        int accent = p.second >= 0 ? p.second : defaultAccent(p);
        float gloss = p.gloss >= 0 ? p.gloss : defaultGloss(p.mat);
        boolean[] emit = new boolean[1];
        for (int ty = y0; ty < y1; ty++) {
            for (int tx = x0; tx < x1; tx++) {
                float fu = (tx + 0.5f - x0) / tw, fv = (ty + 0.5f - y0) / th;
                float mx = (tx - x0 + 0.5f) / dens, my = (ty - y0 + 0.5f) / dens;
                emit[0] = p.glow;
                int rgb = material(p, accent, gloss, face, fu, fv, mx, my, fw, fh, tx, ty, seed, noiseAmt, emit);
                rgb = overlay(p, accent, rgb, mx, my, fu, fv, fw, fh, face, seed, emit);
                // lighting: face orientation, vertical ambient occlusion gradient, bevel
                float light = faceLight * p.shade;
                if (side) light *= Mth.lerp(fv, 1.07f, 0.80f);
                else if (face == Face.TOP) light *= 1f - 0.08f * (Math.abs(fu - 0.5f) + Math.abs(fv - 0.5f));
                int ex = Math.min(tx - x0, x1 - 1 - tx), ey = Math.min(ty - y0, y1 - 1 - ty);
                boolean bigFace = tw > dens * 2 && th > dens * 2;
                if (p.outline && bigFace) {
                    if (Math.min(ex, ey) == 0) light *= 0.74f;
                    else if (side && ty - y0 <= Math.max(1, dens / 2)) light *= 1.12f;
                    else if (Math.min(ex, ey) == 1 && dens >= 3) light *= 0.92f;
                }
                if (p.trim >= 0) {
                    float tdist = Math.min(Math.min(mx, fw - mx), Math.min(my, fh - my));
                    if (tdist < p.trimWidth && (side || face == Face.TOP)) {
                        rgb = metalShade(p.trim, fu, fv, mx, my, seed + 99, 0.9f);
                        if (tdist < 0.5f / dens + 0.01f) light *= 0.85f;
                    }
                }
                if (p.grime > 0 && side) {
                    float g = p.grime * smooth(0.45f, 1f, fv) * (0.6f + 0.6f * fbm(seed + 7, mx * 0.4f, my * 0.4f));
                    rgb = lerpRgb(rgb, shadeRgb(0x3A2E20, 0.9f), Mth.clamp(g, 0, 0.75f));
                }
                if (emit[0]) light = Math.max(light, 0.95f) * (side ? Mth.lerp(fv, 1.05f, 0.92f) : 1f);
                c.put(tx, ty, shadeRgb(rgb, light), emit[0]);
            }
        }
        List<Decal> ds = p.decals.get(face);
        if (ds != null) for (Decal d : ds) decal(c, x0, y0, tw, th, d, seed);
    }

    private static float defaultNoise(Mat m) {
        return switch (m) {
            case METAL, GOLD, PLATE, DARK_METAL, GLASS, ENERGY, CRYSTAL, ICE -> 0.04f;
            case SKIN, RUBBER -> 0.035f;
            case CLOTH, ROBE, LEATHER -> 0.07f;
            case FLESH, ROT, STONE, BONE -> 0.12f;
            default -> 0.09f;
        };
    }

    private static int defaultAccent(Paint p) {
        return switch (p.mat) {
            case ROT -> 0x3A4A2A;
            case FLESH -> 0x6A1A1A;
            case BONE -> 0x8A7E66;
            case STONE -> 0x4A4A4A;
            case CRYSTAL, ICE -> 0xFFFFFF;
            case GOLD -> 0xFFF4C0;
            default -> shadeRgb(p.base, 0.62f);
        };
    }

    private static float defaultGloss(Mat m) {
        return switch (m) {
            case METAL, PLATE -> 0.45f;
            case GOLD -> 0.6f;
            case DARK_METAL -> 0.3f;
            case CHITIN, GLASS -> 0.5f;
            case CRYSTAL, ICE -> 0.4f;
            case SCALES, HAIR, LEATHER -> 0.15f;
            default -> 0f;
        };
    }

    // ------------------------------------------------------------------ materials

    private static int material(Paint p, int accent, float gloss, Face face, float fu, float fv, float mx, float my, float fw, float fh,
                                int tx, int ty, int seed, float na, boolean[] emit) {
        int base = p.base;
        float n1 = fbm(seed, mx * 0.35f, my * 0.35f);
        float n2 = hash(seed + 13, tx, ty);
        switch (p.mat) {
            case PLAIN -> {
                return shadeRgb(base, 1f + (n2 - 0.5f) * 2 * na + (n1 - 0.5f) * na);
            }
            case SKIN -> {
                int c = lerpRgb(base, shadeRgb(warm(base), 1.02f), n1 * 0.35f);
                return shadeRgb(c, 1f + (n2 - 0.5f) * na);
            }
            case FLESH, ROT -> {
                int c = base;
                float blot = fbm(seed + 3, mx * 0.22f, my * 0.22f);
                if (blot > 0.58f) c = lerpRgb(c, accent, Mth.clamp((blot - 0.58f) * 4f, 0, 0.85f));
                float vein = ridge(seed + 5, mx * 0.5f, my * 0.5f);
                if (vein > 0.93f) c = lerpRgb(c, p.mat == Mat.ROT ? 0x5A1A2A : shadeRgb(accent, 0.6f), 0.6f);
                if (hash(seed + 17, tx, ty) > 0.985f) c = p.mat == Mat.ROT ? 0x7A1010 : shadeRgb(c, 1.35f);
                return shadeRgb(c, 1f + (n2 - 0.5f) * 2 * na);
            }
            case BONE -> {
                int c = lerpRgb(base, shadeRgb(base, 0.85f), fv * 0.3f);
                if (hash(seed + 19, tx / 2, ty / 2) > 0.92f) c = shadeRgb(c, 0.72f);
                if (ridge(seed + 21, mx * 0.4f, my * 0.4f) > 0.95f) c = shadeRgb(accent, 0.8f);
                return shadeRgb(c, 1f + (n2 - 0.5f) * na + (n1 - 0.5f) * 0.1f);
            }
            case CLOTH, ROBE -> {
                float weave = ((tx + ty) & 1) == 0 ? 1.03f : 0.97f;
                float folds = 1f + Mth.sin(mx * (p.mat == Mat.ROBE ? 1.1f : 0.8f) + n1 * 3f) * (p.mat == Mat.ROBE ? 0.09f : 0.05f);
                int c = base;
                float stain = fbm(seed + 9, mx * 0.18f, my * 0.18f);
                if (stain > 0.66f) c = lerpRgb(c, shadeRgb(base, 0.75f), 0.4f);
                return shadeRgb(c, weave * folds * (1f + (n2 - 0.5f) * na));
            }
            case LEATHER -> {
                int c = lerpRgb(base, shadeRgb(base, 0.8f), n1 * 0.6f);
                float edge = Math.min(Math.min(mx, fw - mx), Math.min(my, fh - my));
                if (edge > 0.55f && edge < 0.85f && ((tx + ty) / 2) % 3 == 0) c = shadeRgb(accent == base ? base : accent, 1.25f);
                return shadeRgb(c, 1f + (n2 - 0.5f) * na + specular(fu, fv, gloss * 0.6f));
            }
            case METAL, DARK_METAL -> {
                return metalShade(base, fu, fv, mx, my, seed, gloss);
            }
            case GOLD -> {
                int c = metalShade(base, fu, fv, mx, my, seed, gloss);
                float spec = specular(fu, fv, gloss);
                return lerpRgb(c, accent, Mth.clamp(spec * 0.6f, 0, 0.7f));
            }
            case PLATE -> {
                int c = metalShade(base, fu, fv, mx, my, seed, gloss);
                float plateH = 3.5f;
                float py = my % plateH;
                if (py < 0.35f) c = shadeRgb(c, 0.62f);
                else if (py < 0.7f) c = shadeRgb(c, 1.15f);
                float pxm = mx % 4f;
                if (fw > 5 && pxm > 3.6f && py > 1f) c = shadeRgb(c, 0.8f);
                if (rivet(mx, my, fw, fh)) c = lerpRgb(c, 0xF0F0F0, 0.55f);
                return c;
            }
            case STONE -> {
                int c = base;
                if (n2 > 0.86f) c = shadeRgb(c, 1.12f);
                if (n2 < 0.12f) c = shadeRgb(c, 0.86f);
                if (ridge(seed + 23, mx * 0.3f, my * 0.3f) > 0.955f) c = shadeRgb(c, 0.55f);
                if (p.second >= 0) {
                    float moss = fbm(seed + 29, mx * 0.25f, my * 0.25f) + (face == Face.TOP ? 0.25f : 0) - fv * 0.25f;
                    if (moss > 0.62f) c = lerpRgb(c, p.second, Mth.clamp((moss - 0.62f) * 5, 0, 0.9f));
                }
                return shadeRgb(c, 1f + (n1 - 0.5f) * 0.18f);
            }
            case WOOD -> {
                float grain = Mth.sin((my + fbm(seed, mx * 0.3f, my * 0.1f) * 3f) * 2.2f);
                int c = lerpRgb(base, shadeRgb(base, 0.72f), Mth.clamp(grain * 0.5f + 0.2f, 0, 1) * 0.6f);
                if (hash(seed + 31, (int) (mx / 3), (int) (my / 3)) > 0.97f) c = shadeRgb(c, 0.7f);
                return shadeRgb(c, 1f + (n2 - 0.5f) * na);
            }
            case FUR, HAIR -> {
                float strand = hash(seed + 37, tx, 0);
                float along = fbm(seed + 41, tx * 0.6f, my * 0.6f);
                int c = lerpRgb(base, accent, Mth.clamp(strand * 0.55f + along * 0.2f - 0.1f, 0, 1));
                float tip = side(face) ? 1f - fv * 0.25f : 1f;
                float shine = p.mat == Mat.HAIR ? 1f + Math.max(0, Mth.sin(mx * 0.7f + strand * 6f)) * 0.12f : 1f;
                return shadeRgb(c, tip * shine * (0.88f + 0.24f * along));
            }
            case SCALES, CHITIN -> {
                float cell = p.mat == Mat.CHITIN ? 3f : 1.6f;
                float row = (float) Math.floor(my / cell);
                float lx = (mx + (row % 2) * cell * 0.5f) % cell / cell, ly = (my % cell) / cell;
                float dist = (lx - 0.5f) * (lx - 0.5f) + (ly - 0.15f) * (ly - 0.15f) * 0.6f;
                int c = base;
                if (dist > 0.22f) c = lerpRgb(c, accent, 0.65f);
                else c = shadeRgb(c, 1.1f - ly * 0.2f);
                return shadeRgb(c, 1f + specular(fu, fv, gloss) + (n2 - 0.5f) * na);
            }
            case CRYSTAL, ICE, GLASS -> {
                int facet = (int) Math.floor((fu + fv * 0.7f) * 3f) + (int) Math.floor((fu - fv) * 2f);
                float f = 0.85f + 0.3f * hash(seed + 43, facet, 0);
                int c = shadeRgb(base, f);
                float edge = Math.abs(((fu + fv * 0.7f) * 3f) % 1f - 0.5f);
                if (edge > 0.45f) {
                    c = lerpRgb(c, accent, 0.6f);
                    if (p.accentGlow) emit[0] = true;
                }
                if (p.mat == Mat.ICE && ridge(seed + 47, mx * 0.6f, my * 0.6f) > 0.93f) c = lerpRgb(c, 0xFFFFFF, 0.7f);
                return lerpRgb(c, 0xFFFFFF, Mth.clamp(specular(fu, fv, gloss), 0, 0.6f));
            }
            case ENERGY -> {
                float dx = Math.abs(fu - 0.5f) * 2, dy = Math.abs(fv - 0.5f) * 2;
                float core = 1f - Math.max(dx, dy);
                int c = lerpRgb(base, accent == shadeRgb(base, 0.62f) ? brighten(base) : accent, Mth.clamp(core * 0.9f, 0, 1));
                float band = Mth.sin(my * 1.6f + n1 * 4f) * 0.06f;
                return shadeRgb(c, 1f + band + (n2 - 0.5f) * na);
            }
            case FEATHER -> {
                float rowH = 2.2f;
                float row = (float) Math.floor(my / rowH);
                float lx = ((mx + row * 1.1f) % 2.4f) / 2.4f, ly = (my % rowH) / rowH;
                int c = lerpRgb(base, accent, Mth.clamp(ly * 0.8f, 0, 1));
                if (Math.abs(lx - 0.5f) < 0.08f) c = shadeRgb(c, 1.2f);
                if (Math.abs(lx - 0.5f) > 0.45f) c = shadeRgb(c, 0.75f);
                return shadeRgb(c, 1f + (n2 - 0.5f) * na);
            }
            case RUBBER -> {
                return shadeRgb(base, 1f + (n2 - 0.5f) * na);
            }
        }
        return base;
    }

    private static boolean side(Face f) {
        return f != Face.TOP && f != Face.BOTTOM;
    }

    static int metalShade(int base, float fu, float fv, float mx, float my, int seed, float gloss) {
        float brushed = hash(seed + 51, 0, (int) (my * 4)) * 0.08f - 0.04f;
        float scratch = ridge(seed + 53, mx * 0.7f, my * 0.2f) > 0.96f ? 0.18f : 0f;
        float spec = specular(fu, fv, gloss);
        int c = shadeRgb(base, 1f + brushed + scratch);
        return lerpRgb(c, brighten(base), Mth.clamp(spec, 0, 0.8f));
    }

    private static float specular(float fu, float fv, float gloss) {
        if (gloss <= 0) return 0;
        float band = 1f - Math.abs((fu + (1f - fv)) * 0.5f - 0.38f) * 5.5f;
        float band2 = 1f - Math.abs((fu + (1f - fv)) * 0.5f - 0.62f) * 12f;
        return Math.max(0, band) * gloss * 0.7f + Math.max(0, band2) * gloss * 0.35f;
    }

    private static boolean rivet(float mx, float my, float fw, float fh) {
        if (fw < 4 || fh < 4) return false;
        float[][] pts = {{0.9f, 0.9f}, {fw - 0.9f, 0.9f}, {0.9f, fh - 0.9f}, {fw - 0.9f, fh - 0.9f}};
        for (float[] q : pts) {
            float dx = mx - q[0], dy = my - q[1];
            if (dx * dx + dy * dy < 0.12f) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ pattern overlays

    private static int overlay(Paint p, int accent, int rgb, float mx, float my, float fu, float fv, float fw, float fh, Face face,
                               int seed, boolean[] emit) {
        float sx = mx * p.patternScale, sy = my * p.patternScale;
        int sec = accent;
        switch (p.pattern) {
            case STRIPES -> {
                if (((int) (sx / 1.5f)) % 2 == 0) rgb = lerpRgb(rgb, sec, 0.85f);
            }
            case BANDS -> {
                if (((int) (sy / 2f)) % 2 == 1) rgb = lerpRgb(rgb, sec, 0.85f);
            }
            case SPOTS -> {
                if (fbm(seed * 3, sx * 0.4f, sy * 0.4f) > 0.64f) rgb = lerpRgb(rgb, sec, 0.8f);
            }
            case PLATES -> {
                if (sx % 4f < 0.4f || sy % 5f < 0.4f) rgb = shadeRgb(rgb, 0.65f);
                else if (sx % 4f < 0.8f && sy % 5f < 0.8f) rgb = lerpRgb(rgb, sec, 0.8f);
            }
            case SCALES -> {
                float row = (float) Math.floor(sy / 1.5f);
                float lx = ((sx + row * 0.75f) % 1.5f) / 1.5f;
                if ((sy % 1.5f) / 1.5f > 0.7f && (lx < 0.15f || lx > 0.85f)) rgb = lerpRgb(rgb, sec, 0.7f);
            }
            case GRADIENT -> {
                float t = face == Face.TOP ? 0f : face == Face.BOTTOM ? 1f : fv;
                rgb = lerpRgb(rgb, sec, t);
            }
            case CHECKER -> {
                if ((((int) sx) + ((int) sy)) % 2 == 0) rgb = lerpRgb(rgb, sec, 0.85f);
            }
            case RUNES -> {
                int gx = (int) (sx / 2.5f), gy = (int) (sy / 2.5f);
                if (hash(seed * 5, gx, gy) > 0.55f) {
                    float lx = (sx % 2.5f) / 2.5f, ly = (sy % 2.5f) / 2.5f;
                    if (glyph((int) (hash(seed * 7, gx, gy) * 8), lx, ly)) {
                        rgb = sec;
                        emit[0] = true;
                    }
                }
            }
            case CRACKS -> {
                if (ridge(seed + 61, sx * 0.35f, sy * 0.35f) > 0.94f) rgb = lerpRgb(rgb, sec, 0.85f);
            }
            case VEINS -> {
                if (ridge(seed + 67, sx * 0.3f, sy * 0.3f) > 0.935f) {
                    rgb = sec;
                    emit[0] = true;
                }
            }
            case FUR -> rgb = shadeRgb(rgb, 0.85f + 0.3f * hash(seed + 71, (int) (sx * 3), (int) (sy / 2)));
            case WOOD -> {
                if (Mth.sin((sy + fbm(seed, sx * 0.3f, 0) * 3) * 2f) > 0.8f) rgb = lerpRgb(rgb, sec, 0.6f);
            }
            case RIBS -> {
                if (sy % 2.4f < 0.8f && fu > 0.08f && fu < 0.92f) rgb = lerpRgb(rgb, sec, 0.85f);
            }
            case CLOTH -> {
                if (sy % 4f < 0.5f) rgb = lerpRgb(rgb, sec, 0.6f);
            }
            case FEATHERS -> {
                if (((int) (sx + sy)) % 3 == 0 || ((int) (sx - sy + 300)) % 3 == 0) rgb = lerpRgb(rgb, sec, 0.5f);
            }
            case BRICK -> {
                int row = (int) (sy / 2f);
                float off = row % 2 == 0 ? 0 : 2f;
                if (sy % 2f < 0.35f || (sx + off) % 4f < 0.35f) rgb = lerpRgb(rgb, sec, 0.8f);
            }
            case HEX -> {
                float hx = sx / 2.2f, hy = sy / 1.9f;
                float row = (float) Math.floor(hy);
                float lx = (hx + (row % 2) * 0.5f) % 1f, ly = hy % 1f;
                if (lx < 0.1f || ly < 0.12f) {
                    rgb = lerpRgb(rgb, sec, 0.85f);
                    if (p.accentGlow) emit[0] = true;
                }
            }
            case HAZARD -> {
                if (((int) ((sx + sy) / 1.6f)) % 2 == 0) rgb = lerpRgb(rgb, sec, 0.92f);
            }
            case CIRCUIT -> {
                int gx = (int) (sx / 1.5f), gy = (int) (sy / 1.5f);
                float hv = hash(seed + 73, gx, gy);
                float lx = (sx % 1.5f) / 1.5f, ly = (sy % 1.5f) / 1.5f;
                boolean trace = (hv > 0.5f && Math.abs(ly - 0.5f) < 0.13f) || (hv < 0.3f && Math.abs(lx - 0.5f) < 0.13f);
                if (trace) {
                    rgb = sec;
                    emit[0] = true;
                }
            }
            case CHAINMAIL -> {
                float lx = (sx % 1f), ly = ((sy + ((int) sx % 2) * 0.5f) % 1f);
                float dd = (lx - 0.5f) * (lx - 0.5f) + (ly - 0.5f) * (ly - 0.5f);
                rgb = dd > 0.12f && dd < 0.24f ? shadeRgb(rgb, 1.2f) : shadeRgb(rgb, 0.78f);
            }
            case MUSCLE -> {
                float m = Mth.sin(sx * 0.9f) * Mth.sin(sy * 0.6f + 1f);
                rgb = shadeRgb(rgb, 1f + m * 0.08f);
            }
            default -> {
            }
        }
        return rgb;
    }

    /** Eight tiny rune glyphs on a 0..1 cell. */
    private static boolean glyph(int g, float x, float y) {
        float t = 0.13f;
        return switch (g) {
            case 0 -> Math.abs(x - 0.5f) < t || (Math.abs(y - 0.3f) < t && x > 0.25f && x < 0.75f);
            case 1 -> Math.abs(x - y) < t || Math.abs(x - 0.25f) < t && y > 0.2f;
            case 2 -> (Math.abs(y - 0.2f) < t || Math.abs(y - 0.8f) < t) && x > 0.2f && x < 0.8f || Math.abs(x - 0.5f) < t;
            case 3 -> Math.abs(x + y - 1f) < t || Math.abs(x - y) < t && y < 0.5f;
            case 4 -> Math.abs(x - 0.3f) < t || Math.abs(x - 0.7f) < t && y < 0.6f || Math.abs(y - 0.5f) < t;
            case 5 -> Math.abs((x - 0.5f) * (x - 0.5f) + (y - 0.5f) * (y - 0.5f) - 0.09f) < 0.05f;
            case 6 -> Math.abs(x - 0.5f) < t || Math.abs(y - x * 0.5f - 0.2f) < t;
            default -> Math.abs(y - 0.5f) < t || Math.abs(x - 0.2f) < t && y > 0.5f || Math.abs(x - 0.8f) < t && y < 0.5f;
        };
    }

    // ------------------------------------------------------------------ decals

    private static void decal(Canvas c, int x0, int y0, int tw, int th, Decal d, int seed) {
        float s = Math.min(tw, th) * d.size();
        float cx = x0 + d.x() * tw, cy = y0 + d.y() * th;
        int col = d.color();
        boolean g = d.glow();
        int dark = 0x120808;
        int minX = x0, maxX = x0 + tw, minY = y0, maxY = y0 + th;
        for (int ty = minY; ty < maxY; ty++) {
            for (int tx = minX; tx < maxX; tx++) {
                float px = (tx + 0.5f - cx) / s, py = (ty + 0.5f - cy) / s;
                float fx = (tx + 0.5f - x0) / tw, fy = (ty + 0.5f - y0) / th;
                int cur = c.get(tx, ty);
                int out = -1;
                boolean og = false;
                switch (d.type()) {
                    case EYES, SLIT_EYES, HOLLOW_EYES -> {
                        for (int e = -1; e <= 1; e += 2) {
                            float ex = px - e * 0.22f, ey = py;
                            float rx = 0.12f, ry = d.type() == DecalType.SLIT_EYES ? 0.045f : 0.085f;
                            float q = (ex * ex) / (rx * rx) + (ey * ey) / (ry * ry);
                            float sock = (ex * ex) / (rx * rx * 2.6f) + ((ey + 0.01f) * (ey + 0.01f)) / (ry * ry * 3.2f);
                            if (sock < 1f && q >= 1f) out = lerpRgb(cur, dark, d.type() == DecalType.HOLLOW_EYES ? 0.85f : 0.45f);
                            if (q < 1f) {
                                if (d.type() == DecalType.HOLLOW_EYES) {
                                    out = q < 0.25f ? col : dark;
                                    og = q < 0.25f && g;
                                } else if (g) {
                                    out = lerpRgb(col, 0xFFFFFF, Mth.clamp(1f - q * 1.6f, 0, 0.75f));
                                    og = true;
                                } else {
                                    out = q < 0.35f ? col : 0xF0EEE8;
                                    if (ex < -0.03f && ey < -0.02f && q < 0.2f) out = 0xFFFFFF;
                                }
                            }
                            if (d.type() == DecalType.SLIT_EYES && ey < -ry && ey > -ry - 0.06f && Math.abs(ex) < rx * 1.1f) out = dark;
                        }
                    }
                    case BROW -> {
                        for (int e = -1; e <= 1; e += 2) {
                            float ex = px - e * 0.22f;
                            if (Math.abs(py + 0.13f + ex * e * 0.15f) < 0.035f && Math.abs(ex) < 0.15f) out = shadeRgb(col, 1f);
                        }
                    }
                    case VISOR -> {
                        if (Math.abs(py) < 0.07f && Math.abs(fx - 0.5f) < 0.42f) {
                            out = lerpRgb(col, 0xFFFFFF, Mth.clamp(0.6f - Math.abs(py) * 8f, 0, 0.6f));
                            og = g;
                        } else if (Math.abs(py) < 0.12f && Math.abs(fx - 0.5f) < 0.45f) out = dark;
                    }
                    case MOUTH -> {
                        if (Math.abs(py) < 0.035f && Math.abs(px) < 0.17f) out = shadeRgb(col, 1f);
                    }
                    case GRIN, FANGS -> {
                        if (Math.abs(py) < 0.07f && Math.abs(px) < 0.24f) {
                            boolean tooth = d.type() == DecalType.GRIN ? ((int) Math.floor((px + 1) * 22)) % 2 == 0 && Math.abs(py) > 0.01f
                                    : (Math.abs(Math.abs(px) - 0.15f) < 0.035f && py < 0.05f);
                            out = tooth ? 0xE8E2D0 : dark;
                        }
                    }
                    case CROSS -> {
                        if ((Math.abs(px) < 0.07f && Math.abs(py) < 0.3f) || (Math.abs(py + 0.06f) < 0.07f && Math.abs(px) < 0.2f)) {
                            out = col;
                            og = g;
                        }
                    }
                    case RUNE -> {
                        if (Math.abs(px) < 0.35f && Math.abs(py) < 0.35f && glyph((seed & 7), px / 0.7f + 0.5f, py / 0.7f + 0.5f)) {
                            out = col;
                            og = g;
                        }
                    }
                    case EMBLEM -> {
                        float ax = Math.abs(px);
                        boolean shield = py > -0.3f && py < 0.3f && ax < 0.26f - Math.max(0, py) * 0.6f;
                        boolean inner = py > -0.22f && py < 0.22f && ax < 0.18f - Math.max(0, py) * 0.6f;
                        if (shield) {
                            out = inner ? lerpRgb(cur, col, 0.25f) : col;
                            og = g && !inner;
                        }
                        if (inner && Math.abs(px) < 0.04f) out = col;
                    }
                    case SUN -> {
                        float r = (float) Math.sqrt(px * px + py * py);
                        float a = (float) Math.atan2(py, px);
                        if (r < 0.14f || (r < 0.32f && Math.abs(Mth.sin(a * 6)) > 0.8f)) {
                            out = lerpRgb(col, 0xFFFFFF, Mth.clamp(0.4f - r, 0, 0.4f));
                            og = g;
                        }
                    }
                    case STAR -> {
                        float r = (float) Math.sqrt(px * px + py * py);
                        float a = (float) Math.atan2(py, px);
                        float lim = 0.12f + 0.18f * Math.abs(Mth.cos(a * 2.5f));
                        if (r < lim) {
                            out = col;
                            og = g;
                        }
                    }
                    case BOLT -> {
                        boolean seg1 = Math.abs(px - (-py * 0.4f + 0.04f)) < 0.06f && py > -0.32f && py < 0.02f;
                        boolean seg2 = Math.abs(px - (-py * 0.4f - 0.06f)) < 0.06f && py > -0.02f && py < 0.32f;
                        boolean bar = Math.abs(py) < 0.04f && px > -0.12f && px < 0.14f;
                        if (seg1 || seg2 || bar) {
                            out = lerpRgb(col, 0xFFFFFF, 0.25f);
                            og = g;
                        }
                    }
                    case EYE_SIGIL -> {
                        float q = px * px / 0.09f + py * py / 0.02f;
                        float r = px * px + py * py;
                        if (q < 1f) out = r < 0.006f ? dark : r < 0.016f ? col : lerpRgb(cur, col, 0.3f);
                        if (Math.abs(q - 1f) < 0.25f) out = col;
                        og = g && out != -1;
                    }
                    case MOON -> {
                        float r1 = px * px + py * py, r2 = (px - 0.1f) * (px - 0.1f) + py * py;
                        if (r1 < 0.08f && r2 > 0.06f) {
                            out = col;
                            og = g;
                        }
                    }
                    case FLAME -> {
                        float w = 0.2f * (1f - (py + 0.3f) / 0.6f) + Mth.sin(py * 18f) * 0.03f;
                        if (py > -0.3f && py < 0.3f && Math.abs(px) < Math.max(0, 0.22f - Math.abs(py - 0.1f) * 0.6f)) {
                            out = lerpRgb(col, 0xFFF080, Mth.clamp(0.3f - Math.abs(px) * 2f + w, 0, 0.7f));
                            og = g;
                        }
                    }
                    case CHEVRON -> {
                        if (Math.abs(py - Math.abs(px) * 0.8f) < 0.07f && Math.abs(px) < 0.3f) {
                            out = col;
                            og = g;
                        }
                    }
                    case RING -> {
                        float r = (float) Math.sqrt(px * px + py * py);
                        if (Math.abs(r - 0.25f) < 0.05f) {
                            out = col;
                            og = g;
                        }
                    }
                    case GEM -> {
                        if (Math.abs(px) + Math.abs(py) < 0.18f) {
                            out = lerpRgb(col, 0xFFFFFF, Mth.clamp(0.15f - px - py, 0, 0.5f));
                            og = g;
                        } else if (Math.abs(px) + Math.abs(py) < 0.24f) out = 0xD8B040;
                    }
                    case STRIPE -> {
                        if (Math.abs(fx - d.x()) < 0.09f * d.size()) {
                            out = col;
                            og = g;
                        }
                    }
                    case HSTRIPE -> {
                        if (Math.abs(fy - d.y()) < 0.09f * d.size()) {
                            out = col;
                            og = g;
                        }
                    }
                    case CORE -> {
                        float r = (float) Math.sqrt(px * px + py * py);
                        if (r < 0.32f) {
                            out = lerpRgb(col, 0xFFFFFF, Mth.clamp(1f - r / 0.2f, 0, 0.85f));
                            og = g;
                        } else if (r < 0.38f) out = shadeRgb(col, 0.5f);
                    }
                    case DOTS -> {
                        float gx = fx * 6 % 1f, gy = fy * 4 % 1f;
                        if ((gx - 0.5f) * (gx - 0.5f) + (gy - 0.5f) * (gy - 0.5f) < 0.05f) {
                            out = col;
                            og = g;
                        }
                    }
                    case GRILLE -> {
                        if (Math.abs(px) < 0.3f && Math.abs(py) < 0.25f && ((int) Math.floor((py + 1) * 14)) % 2 == 0) out = dark;
                    }
                    case PANEL -> {
                        float ix = Math.abs(fx - 0.5f), iy = Math.abs(fy - 0.5f);
                        if ((Math.abs(ix - 0.38f) < 0.025f && iy < 0.4f) || (Math.abs(iy - 0.38f) < 0.025f && ix < 0.4f)) out = shadeRgb(cur, 0.6f);
                    }
                    case STITCH -> {
                        if (Math.abs(px) < 0.03f && Math.abs(py) < 0.4f) out = 0x2A1A10;
                        if (Math.abs(py * 10 % 1f) < 0.25f && Math.abs(px) < 0.08f && Math.abs(py) < 0.4f) out = 0xC8B898;
                    }
                    case SCAR -> {
                        if (Math.abs(px - py * 0.7f) < 0.03f && Math.abs(py) < 0.3f) out = shadeRgb(col, 0.8f);
                        if (Math.abs(px - py * 0.7f) < 0.07f && Math.abs((py * 12) % 1f) < 0.2f && Math.abs(py) < 0.3f) out = 0x2A1010;
                    }
                    case BLOOD -> {
                        float r = (float) Math.sqrt(px * px + py * py);
                        float wob = hash(seed, (int) (Math.atan2(py, px) * 6), 0) * 0.15f;
                        if (r < 0.15f + wob || (hash(seed + 3, tx / 2, ty / 2) > 0.8f && r < 0.4f)) out = lerpRgb(col, dark, r);
                        if (py > 0 && Math.abs(px - 0.05f) < 0.025f && py < 0.45f) out = col;
                    }
                    case SKULL -> {
                        float hx = Math.abs(px);
                        boolean head = px * px / 0.09f + (py + 0.05f) * (py + 0.05f) / 0.07f < 1f;
                        boolean jaw = hx < 0.17f && py > 0.12f && py < 0.3f;
                        boolean eye = (hx - 0.11f) * (hx - 0.11f) + (py + 0.02f) * (py + 0.02f) < 0.005f;
                        boolean nose = hx < 0.03f && py > 0.06f && py < 0.12f;
                        boolean teeth = jaw && py > 0.2f && ((int) Math.floor((px + 1) * 25)) % 2 == 0;
                        if (head || jaw) out = col;
                        if (eye || nose || teeth) out = dark;
                        og = g && (head || jaw) && !(eye || nose || teeth);
                    }
                    case MASK -> {
                        if (fy > 0.52f) out = lerpRgb(cur, col, 0.92f);
                    }
                    case NOSE -> {
                        if (Math.abs(px) < 0.06f && py > -0.05f && py < 0.08f) out = shadeRgb(cur, px > 0 ? 0.82f : 0.92f);
                    }
                    case BEARD -> {
                        if (fy > 0.58f && Math.abs(fx - 0.5f) < 0.48f - (fy - 0.58f) * 0.4f && !(Math.abs(py) < 0.05f && Math.abs(px) < 0.12f)) {
                            out = shadeRgb(col, 0.85f + 0.3f * hash(seed, tx, ty / 2));
                        }
                    }
                }
                if (out != -1) c.put(tx, ty, out, og);
            }
        }
    }

    // ------------------------------------------------------------------ noise & colour maths

    static float hash(int seed, int x, int y) {
        int h = seed * 374761393 + x * 668265263 + y * 2147483647;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535f;
    }

    static float valueNoise(int seed, float x, float y) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        float fx = x - ix, fy = y - iy;
        float sx = fx * fx * (3 - 2 * fx), sy = fy * fy * (3 - 2 * fy);
        float a = hash(seed, ix, iy), b = hash(seed, ix + 1, iy), c = hash(seed, ix, iy + 1), d = hash(seed, ix + 1, iy + 1);
        return Mth.lerp(sy, Mth.lerp(sx, a, b), Mth.lerp(sx, c, d));
    }

    static float fbm(int seed, float x, float y) {
        return valueNoise(seed, x, y) * 0.62f + valueNoise(seed + 101, x * 2.1f, y * 2.1f) * 0.28f + valueNoise(seed + 202, x * 4.3f, y * 4.3f) * 0.1f;
    }

    /** Ridged noise: values near 1 form thin vein / crack lines. */
    static float ridge(int seed, float x, float y) {
        return 1f - Math.abs(fbm(seed, x, y) * 2f - 1f);
    }

    static float smooth(float a, float b, float x) {
        float t = Mth.clamp((x - a) / (b - a), 0, 1);
        return t * t * (3 - 2 * t);
    }

    static int warm(int rgb) {
        int r = Math.min(255, ((rgb >> 16) & 0xFF) + 14), g = (rgb >> 8) & 0xFF, b = Math.max(0, (rgb & 0xFF) - 6);
        return (r << 16) | (g << 8) | b;
    }

    static int brighten(int rgb) {
        return lerpRgb(rgb, 0xFFFFFF, 0.55f);
    }

    public static int shadeRgb(int rgb, float f) {
        int r = Mth.clamp((int) (((rgb >> 16) & 0xFF) * f), 0, 255);
        int g = Mth.clamp((int) (((rgb >> 8) & 0xFF) * f), 0, 255);
        int b = Mth.clamp((int) ((rgb & 0xFF) * f), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    public static int lerpRgb(int a, int b, float t) {
        t = Mth.clamp(t, 0, 1);
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return (r << 16) | (g << 8) | bl;
    }
}
