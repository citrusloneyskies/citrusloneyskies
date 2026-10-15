package com.lastbastion.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Brief full-screen colour flashes (Zeus' thunder, nukes, the core falling). */
public final class ScreenFlash implements IGuiOverlay {
    public static final ScreenFlash INSTANCE = new ScreenFlash();
    private static int color;
    private static float strength;
    private static int life, age;

    public static void add(Vec3 at, int rgb, float s, float radius, int l) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double d = mc.player.position().distanceTo(at);
        float fall = radius <= 0 ? 1f : (float) Mth.clamp(1 - d / radius, 0, 1);
        if (fall * s < strength * (1f - (float) age / Math.max(1, life))) return;
        color = rgb;
        strength = Math.min(0.85f, s * fall);
        life = Math.max(2, l);
        age = 0;
    }

    public static void tick() {
        if (age < life) age++;
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        int lbCrashDepth = com.lastbastion.util.Safe.depth(g.pose());
        try {
        if (age >= life || strength <= 0) return;
        float t = (age + pt) / life;
        float a = strength * (1 - t) * (1 - t);
        if (a <= 0.01f) return;
        g.fill(0, 0, w, h, ((int) (a * 255) << 24) | (color & 0xFFFFFF));
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("ScreenFlash.render", lbCrash);
            com.lastbastion.util.Safe.restore(g.pose(), lbCrashDepth);
        }
    }
}
