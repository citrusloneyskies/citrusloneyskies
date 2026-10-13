package com.lastbastion.client;

import com.lastbastion.entity.misc.TitanExoEntity;
import com.lastbastion.siege.NightModifier;
import com.lastbastion.siege.SiegeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Top-left siege panel (wave, core health/barrier, enemies or nightfall timer, credits) and the exosuit HUD. */
public final class SiegeHud implements IGuiOverlay {
    public static final SiegeHud INSTANCE = new SiegeHud();

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        if (mc.player.getVehicle() instanceof TitanExoEntity exo) exoHud(g, mc.font, exo, w, h);
        var hud = ClientSiegeState.hud;
        if (hud == null) return;
        Font f = mc.font;
        SiegeState state = ClientSiegeState.state();
        int x = 6, y = 6, pw = 170;
        if (state == SiegeState.INACTIVE) {
            if (hud.bestWave() > 0) {
                g.drawString(f, "Credits: " + hud.credits(), x, y, 0xFFD966, true);
            }
            return;
        }
        int ph = state == SiegeState.WAVE ? 54 : 46;
        g.fill(x - 3, y - 3, x + pw, y + ph, 0x90000000);
        g.fill(x - 3, y - 3, x + pw, y - 2, 0xFF6FE3FF);
        NightModifier mod = ClientSiegeState.modifier();
        String title = "WAVE " + hud.wave() + (state == SiegeState.WAVE ? "" : "  (preparing)");
        g.drawString(f, title, x, y, state == SiegeState.WAVE ? 0xFF5050 : 0xFFFFFF, true);
        if (hud.wave() % 5 == 0) g.drawString(f, "BOSS", x + pw - 30, y, 0xFF2020, true);
        if (mod != NightModifier.NONE) g.drawString(f, mod.title, x + f.width(title) + 6, y, mod.color, true);
        y += 12;
        // core health bar
        float frac = Mth.clamp(ClientSiegeState.displayedCoreHp / Math.max(1, hud.coreMax()), 0, 1);
        int bw = pw - 6;
        g.fill(x, y, x + bw, y + 8, 0xFF2A0A0A);
        int col = frac > 0.5f ? 0xFF3FD050 : frac > 0.25f ? 0xFFE0C030 : 0xFFE03030;
        if (frac <= 0.25f && (mc.level.getGameTime() / 5) % 2 == 0) col = 0xFFFF6060;
        g.fill(x, y, x + (int) (bw * frac), y + 8, col);
        float sf = ClientSiegeState.shieldFraction();
        if (sf > 0) g.fill(x, y, x + (int) (bw * sf), y + 3, 0xFF6FE3FF);
        String hp = "Core " + (int) hud.coreHp() + " / " + (int) hud.coreMax() + (hud.shield() > 0 ? "  +" + (int) hud.shield() : "");
        g.drawString(f, hp, x + bw / 2 - f.width(hp) / 2, y, 0xFFFFFF, true);
        y += 12;
        if (state == SiegeState.WAVE) {
            int killed = Math.max(0, hud.total() - hud.remaining());
            float wf = hud.total() <= 0 ? 0 : killed / (float) hud.total();
            g.fill(x, y, x + bw, y + 6, 0xFF1A1A1A);
            g.fill(x, y, x + (int) (bw * wf), y + 6, 0xFFB050FF);
            g.drawString(f, "Undead remaining: " + hud.remaining(), x, y + 8, 0xD0D0D0, true);
            y += 20;
        } else {
            int secs = hud.toNight() / 20;
            String s = "Nightfall in " + secs / 60 + ":" + String.format("%02d", secs % 60);
            g.drawString(f, s, x, y, 0xA0C8FF, true);
            y += 10;
        }
        int c = ClientSiegeState.creditFlash > 0 ? 0xFFFFA0 : 0xFFD966;
        g.drawString(f, "⛁ " + String.format("%,d", hud.credits()) + " credits", x, y, c, true);
    }

    private void exoHud(GuiGraphics g, Font f, TitanExoEntity exo, int w, int h) {
        int cx = w / 2, by = h - 70;
        float time = exo.timeLeft() / (float) TitanExoEntity.DURATION;
        float fuel = exo.fuel() / (float) TitanExoEntity.MAX_FUEL;
        float hp = exo.getHealth() / exo.getMaxHealth();
        g.fill(cx - 91, by - 2, cx + 91, by + 30, 0x80000000);
        bar(g, f, cx - 88, by, 176, "POWER " + exo.timeLeft() / 20 + "s", time, 0xFFE8B020);
        bar(g, f, cx - 88, by + 10, 176, "HULL", hp, 0xFF50D060);
        bar(g, f, cx - 88, by + 20, 176, "JETS", fuel, 0xFF50C8FF);
        int ch = h / 2, cw = w / 2;
        g.fill(cw - 8, ch, cw - 3, ch + 1, 0xFFFF6040);
        g.fill(cw + 3, ch, cw + 8, ch + 1, 0xFFFF6040);
        g.fill(cw, ch - 8, cw + 1, ch - 3, 0xFFFF6040);
        g.fill(cw, ch + 3, cw + 1, ch + 8, 0xFFFF6040);
    }

    private void bar(GuiGraphics g, Font f, int x, int y, int w, String label, float frac, int col) {
        g.fill(x, y, x + w, y + 8, 0xFF202020);
        g.fill(x, y, x + (int) (w * Mth.clamp(frac, 0, 1)), y + 8, col);
        g.drawString(f, label, x + 2, y, 0xFFFFFF, true);
    }
}
