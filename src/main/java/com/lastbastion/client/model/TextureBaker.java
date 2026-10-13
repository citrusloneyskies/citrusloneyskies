package com.lastbastion.client.model;

import com.lastbastion.LastBastion;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Procedurally paints the UV atlas of a {@link ModelSpec}: per-face shading, value noise, surface
 * patterns (plates, scales, fur, runes...), edge darkening for definition, decals (eyes, visors,
 * mouths, emblems) and a separate emissive layer rendered full-bright for glowing details.
 */
public final class TextureBaker {
    public record Result(ResourceLocation texture, @Nullable ResourceLocation glow) {
    }

    private TextureBaker() {
    }

    public static Result paint(String name, List<ModelSpec.Box> boxes, int w, int h) {
        int[] col = new int[w * h];
        int[] glow = new int[w * h];
        boolean anyGlow = false;
        int seed = name.hashCode();
        for (int bi = 0; bi < boxes.size(); bi++) {
            ModelSpec.Box b = boxes.get(bi);
            int bw = (int) Math.ceil(b.w), bh = (int) Math.ceil(b.h), bd = (int) Math.ceil(b.d);
            int u = b.u, v = b.v;
            int s = seed * 31 + bi * 7919;
            anyGlow |= face(col, glow, w, h, u + bd, v, bw, bd, Paint.Face.TOP, b.paint, s);
            anyGlow |= face(col, glow, w, h, u + bd + bw, v, bw, bd, Paint.Face.BOTTOM, b.paint, s + 1);
            anyGlow |= face(col, glow, w, h, u, v + bd, bd, bh, Paint.Face.RIGHT, b.paint, s + 2);
            anyGlow |= face(col, glow, w, h, u + bd, v + bd, bw, bh, Paint.Face.FRONT, b.paint, s + 3);
            anyGlow |= face(col, glow, w, h, u + bd + bw, v + bd, bd, bh, Paint.Face.LEFT, b.paint, s + 4);
            anyGlow |= face(col, glow, w, h, u + bd + bw + bd, v + bd, bw, bh, Paint.Face.BACK, b.paint, s + 5);
        }
        ResourceLocation texLoc = new ResourceLocation(LastBastion.MODID, "dynamic/" + name);
        register(texLoc, col, w, h);
        ResourceLocation glowLoc = null;
        if (anyGlow) {
            glowLoc = new ResourceLocation(LastBastion.MODID, "dynamic/" + name + "_glow");
            register(glowLoc, glow, w, h);
        }
        return new Result(texLoc, glowLoc);
    }

    private static void register(ResourceLocation loc, int[] argb, int w, int h) {
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

    // ------------------------------------------------------------------ painting

    private static boolean face(int[] col, int[] glow, int tw, int th, int fx, int fy, int fw, int fh, Paint.Face face, Paint p, int seed) {
        if (fw <= 0 || fh <= 0) return false;
        boolean glowed = false;
        float faceShade = switch (face) {
            case TOP -> 1.10f;
            case BOTTOM -> 0.72f;
            case FRONT -> 1.0f;
            case BACK -> 0.9f;
            default -> 0.86f;
        };
        int second = p.second >= 0 ? p.second : shadeRgb(p.base, 0.7f);
        for (int j = 0; j < fh; j++) {
            for (int i = 0; i < fw; i++) {
                int x = fx + i, y = fy + j;
                if (x < 0 || y < 0 || x >= tw || y >= th) continue;
                float n = hash(seed, i, j);
                int c = p.base;
                boolean patGlow = false;
                switch (p.pattern) {
                    case STRIPES -> {
                        if ((i / 2) % 2 == 0) c = second;
                    }
                    case BANDS -> {
                        if ((j / 3) % 2 == 1) c = second;
                    }
                    case SPOTS -> {
                        if (hash(seed * 3, i / 2, j / 2) > 0.72f) c = second;
                    }
                    case PLATES -> {
                        if (i % 4 == 3 || j % 5 == 4) c = shadeRgb(p.base, 0.62f);
                        if (i % 4 == 0 && j % 5 == 0) c = second;
                    }
                    case SCALES -> {
                        int sx = (i + ((j / 2) % 2) * 2) % 4;
                        if (j % 2 == 1 && (sx == 0 || sx == 3)) c = second;
                        else if (j % 2 == 0 && sx == 1) c = shadeRgb(p.base, 1.15f);
                    }
                    case GRADIENT -> {
                        float t = face == Paint.Face.TOP ? 0f : face == Paint.Face.BOTTOM ? 1f : j / (float) Math.max(1, fh - 1);
                        c = lerpRgb(p.base, second, t);
                    }
                    case CHECKER -> {
                        if (((i / 2) + (j / 2)) % 2 == 0) c = second;
                    }
                    case RUNES -> {
                        if (hash(seed * 5, i / 3, j / 3) > 0.6f && ((i + j) % 3 == 0 || i % 3 == 1 && j % 3 != 2)) {
                            c = second;
                            patGlow = true;
                        }
                    }
                    case CRACKS -> {
                        if (crack(seed, i, j, fw, fh)) c = second;
                    }
                    case VEINS -> {
                        if (crack(seed * 7, i, j, fw, fh)) {
                            c = second;
                            patGlow = true;
                        }
                    }
                    case FUR -> {
                        float col2 = hash(seed * 11, i, 0);
                        c = shadeRgb(lerpRgb(p.base, second, col2 * 0.6f), 0.85f + 0.3f * hash(seed, i, j / 2));
                    }
                    case WOOD -> {
                        float grain = (float) Math.sin((j + hash(seed, i / 3, 0) * 3) * 1.7);
                        if (grain > 0.75f) c = second;
                    }
                    case RIBS -> {
                        if (j % 3 == 1 && i > 0 && i < fw - 1) c = second;
                    }
                    case CLOTH -> {
                        if ((i + j) % 2 == 0) c = shadeRgb(p.base, 0.93f);
                        if (j % 4 == 3) c = second;
                    }
                    case FEATHERS -> {
                        if ((i + j) % 4 == 0 || (i - j + 400) % 4 == 0) c = second;
                    }
                    case BRICK -> {
                        int off = (j / 3) % 2 == 0 ? 0 : 2;
                        if (j % 3 == 2 || (i + off) % 4 == 3) c = second;
                    }
                    default -> {
                    }
                }
                float shade = faceShade * p.shade * (1f + (n - 0.5f) * 2f * p.noise);
                if (p.outline && (i == 0 || j == 0 || i == fw - 1 || j == fh - 1) && fw > 2 && fh > 2) shade *= 0.8f;
                if (p.glow || patGlow) shade = Math.max(shade, 0.95f);
                c = shadeRgb(c, shade);
                col[y * tw + x] = 0xFF000000 | c;
                if (p.glow || patGlow) {
                    glow[y * tw + x] = 0xFF000000 | c;
                    glowed = true;
                }
            }
        }
        Paint.Decal d = p.decals.get(face);
        if (d != null) glowed |= decal(col, glow, tw, th, fx, fy, fw, fh, d);
        return glowed;
    }

    private static boolean decal(int[] col, int[] glow, int tw, int th, int fx, int fy, int fw, int fh, Paint.Decal d) {
        int c = d.color();
        int dark = 0x150A0A;
        java.util.List<int[]> px = new java.util.ArrayList<>();
        java.util.List<int[]> darkPx = new java.util.ArrayList<>();
        int cx = fw / 2;
        switch (d.type()) {
            case EYES, SLIT_EYES -> {
                int ey = Math.max(1, fh * 3 / 8);
                int ew = fw >= 8 ? 2 : 1;
                int gap = Math.max(1, fw / 4);
                for (int e = 0; e < 2; e++) {
                    int ex = e == 0 ? cx - gap - ew + (fw % 2 == 0 ? 0 : 0) : cx + gap - (fw % 2 == 0 ? 0 : -1);
                    if (e == 0) ex = cx - gap - ew + 1;
                    else ex = cx + gap - 1;
                    for (int k = 0; k < ew; k++) {
                        px.add(new int[]{ex + k, ey});
                        if (d.type() == Paint.DecalType.EYES && fh >= 8) px.add(new int[]{ex + k, ey + 1});
                        if (d.type() == Paint.DecalType.SLIT_EYES) darkPx.add(new int[]{ex + k, ey - 1});
                    }
                }
            }
            case VISOR -> {
                int vy = Math.max(1, fh * 3 / 8);
                for (int i = 1; i < fw - 1; i++) {
                    px.add(new int[]{i, vy});
                    if (fh >= 8) darkPx.add(new int[]{i, vy + 1});
                }
            }
            case MOUTH -> {
                int my = fh * 3 / 4;
                for (int i = cx - fw / 4; i < cx + fw / 4; i++) px.add(new int[]{i, my});
            }
            case GRIN -> {
                int my = fh * 3 / 4 - 1;
                for (int i = cx - fw / 3; i < cx + fw / 3; i++) {
                    darkPx.add(new int[]{i, my});
                    px.add(new int[]{i, my + ((i % 2 == 0) ? 1 : 0)});
                }
            }
            case CROSS -> {
                int cy = fh / 2;
                for (int k = -2; k <= 2; k++) {
                    px.add(new int[]{cx + k, cy});
                    px.add(new int[]{cx, cy + k});
                }
            }
            case RUNE -> {
                int cy = fh / 2;
                int[][] g = {{0, -2}, {0, -1}, {0, 0}, {0, 1}, {0, 2}, {-1, -1}, {1, 1}, {-1, 1}, {1, -1}, {-2, 0}, {2, 0}};
                for (int[] o : g) px.add(new int[]{cx + o[0], cy + o[1]});
            }
            case EMBLEM -> {
                int cy = fh / 2;
                for (int k = -2; k <= 2; k++) {
                    int span = 2 - Math.abs(k);
                    for (int s = -span; s <= span; s++) px.add(new int[]{cx + s, cy + k});
                }
            }
            case STRIPE -> {
                for (int j = 0; j < fh; j++) {
                    px.add(new int[]{cx - 1, j});
                    px.add(new int[]{cx, j});
                }
            }
            case CORE -> {
                int cy = fh / 2;
                int r = Math.max(1, Math.min(fw, fh) / 3);
                for (int j = -r; j <= r; j++)
                    for (int i = -r; i <= r; i++)
                        if (i * i + j * j <= r * r) px.add(new int[]{cx + i, cy + j});
            }
            case DOTS -> {
                for (int j = 1; j < fh - 1; j += 2)
                    for (int i = 1 + (j / 2) % 2; i < fw - 1; i += 3) px.add(new int[]{i, j});
            }
            case SKULL -> {
                int ey = Math.max(1, fh * 3 / 8);
                int gap = Math.max(1, fw / 4);
                for (int k = 0; k < 2; k++) {
                    darkPx.add(new int[]{cx - gap - k, ey});
                    darkPx.add(new int[]{cx + gap - 1 + k, ey});
                    darkPx.add(new int[]{cx - gap - k, ey + 1});
                    darkPx.add(new int[]{cx + gap - 1 + k, ey + 1});
                }
                px.add(new int[]{cx - gap, ey});
                px.add(new int[]{cx + gap - 1, ey});
                darkPx.add(new int[]{cx, ey + 2});
                darkPx.add(new int[]{cx - 1, ey + 2});
                int my = Math.min(fh - 1, ey + 4);
                for (int i = cx - gap; i <= cx + gap; i++) if (i % 2 == 0) darkPx.add(new int[]{i, my});
            }
            case MASK -> {
                for (int j = fh / 2; j < fh; j++)
                    for (int i = 0; i < fw; i++) px.add(new int[]{i, j});
            }
        }
        for (int[] q : darkPx) set(col, null, tw, th, fx, fy, fw, fh, q, dark, false);
        boolean g = false;
        for (int[] q : px) g |= set(col, glow, tw, th, fx, fy, fw, fh, q, c, d.glow());
        return g;
    }

    private static boolean set(int[] col, int[] glow, int tw, int th, int fx, int fy, int fw, int fh, int[] q, int c, boolean g) {
        if (q[0] < 0 || q[1] < 0 || q[0] >= fw || q[1] >= fh) return false;
        int x = fx + q[0], y = fy + q[1];
        if (x >= tw || y >= th) return false;
        col[y * tw + x] = 0xFF000000 | c;
        if (g && glow != null) {
            glow[y * tw + x] = 0xFF000000 | c;
            return true;
        }
        return false;
    }

    private static boolean crack(int seed, int i, int j, int fw, int fh) {
        // a few jagged diagonal lines
        for (int k = 0; k < 2; k++) {
            float start = hash(seed + k * 13, 0, 0) * fw;
            float drift = (hash(seed + k * 17, j / 2, 1) - 0.5f) * 2f;
            int lineX = (int) (start + j * 0.6f * (k == 0 ? 1 : -1) + drift);
            if (Math.floorMod(lineX, Math.max(1, fw)) == i && hash(seed + k, i, j) > 0.25f) return true;
        }
        return false;
    }

    private static float hash(int seed, int x, int y) {
        int h = seed * 374761393 + x * 668265263 + y * 2147483647;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535f;
    }

    public static int shadeRgb(int rgb, float f) {
        int r = Mth.clamp((int) (((rgb >> 16) & 0xFF) * f), 0, 255);
        int g = Mth.clamp((int) (((rgb >> 8) & 0xFF) * f), 0, 255);
        int b = Mth.clamp((int) ((rgb & 0xFF) * f), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    public static int lerpRgb(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return (r << 16) | (g << 8) | bl;
    }
}
