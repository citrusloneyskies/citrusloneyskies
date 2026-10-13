package com.lastbastion.client.screen;

import com.lastbastion.client.ClientSiegeState;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.entity.turret.TurretKind;
import com.lastbastion.network.ActionPacket;
import com.lastbastion.network.Net;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Turret console: stats now vs. next level, perks, targeting mode, upgrade / repair / sell. */
public class TurretScreen extends Screen {
    private final TurretEntity turret;
    private Button upgrade, repair, sell, mode;
    private int left, top;
    private static final int W = 260, H = 190;

    public TurretScreen(TurretEntity turret) {
        super(Component.literal(turret.kind.title));
        this.turret = turret;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int by = top + H - 26;
        upgrade = addRenderableWidget(Button.builder(Component.literal("Upgrade"), b -> act(ActionPacket.TURRET_UPGRADE)).bounds(left + 6, by, 80, 20).build());
        repair = addRenderableWidget(Button.builder(Component.literal("Repair"), b -> act(ActionPacket.TURRET_REPAIR)).bounds(left + 90, by, 60, 20).build());
        sell = addRenderableWidget(Button.builder(Component.literal("Sell"), b -> {
            act(ActionPacket.TURRET_SELL);
            onClose();
        }).bounds(left + 154, by, 48, 20).build());
        mode = addRenderableWidget(Button.builder(Component.literal("Target"), b -> act(ActionPacket.TURRET_MODE)).bounds(left + 6, by - 24, W - 12, 20).build());
    }

    private void act(int a) {
        Net.toServer(new ActionPacket(a, turret.getId(), ""));
    }

    @Override
    public void tick() {
        if (!turret.isAlive()) onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        TurretKind k = turret.kind;
        int lvl = turret.turretLevel();
        g.fill(left, top, left + W, top + H, 0xE0101418);
        g.fill(left, top, left + W, top + 2, 0xFF000000 | k.color);
        g.drawString(font, k.title, left + 8, top + 7, k.color, true);
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= TurretKind.MAX_LEVEL; i++) stars.append(i <= lvl ? "★" : "☆");
        g.drawString(font, stars.toString(), left + W - 8 - font.width(stars.toString()), top + 7, 0xFFD966, true);
        int y = top + 22;
        boolean max = lvl >= TurretKind.MAX_LEVEL;
        y = stat(g, y, "Damage", fmt(k.damageAt(lvl)), max ? null : fmt(k.damageAt(lvl + 1)));
        y = stat(g, y, "Range", fmt(k.rangeAt(lvl)), max ? null : fmt(k.rangeAt(lvl + 1)));
        y = stat(g, y, "Attacks/sec", fmt(20f / k.cooldownAt(lvl)), max ? null : fmt(20f / k.cooldownAt(lvl + 1)));
        y = stat(g, y, "Hull", (int) turret.getHealth() + "/" + (int) turret.getMaxHealth(), max ? null : String.valueOf((int) k.hpAt(lvl + 1)));
        g.drawString(font, "Element: " + k.element.name() + "    Kills: " + turret.kills(), left + 8, y, 0xA0A0A0, false);
        y += 12;
        for (FormattedCharSequence s : font.split(Component.literal((lvl >= 3 ? "✔ " : "✖ ") + "Lv3 " + k.perk3), W - 16)) {
            g.drawString(font, s, left + 8, y, lvl >= 3 ? 0xFFB000 : 0x707070, false);
            y += 10;
        }
        for (FormattedCharSequence s : font.split(Component.literal((lvl >= 5 ? "✔ " : "✖ ") + "Lv5 " + k.perk5), W - 16)) {
            g.drawString(font, s, left + 8, y, lvl >= 5 ? 0xC86BFF : 0x707070, false);
            y += 10;
        }
        int cost = k.upgradeCost(lvl);
        upgrade.active = !max && ClientSiegeState.credits() >= cost;
        upgrade.setMessage(Component.literal(max ? "MAX LEVEL" : "Upgrade " + cost));
        int rc = turret.getHealth() < turret.getMaxHealth() ? (int) Math.ceil((turret.getMaxHealth() - turret.getHealth()) * k.cost / (float) k.hp * 0.35f) : 0;
        repair.active = rc > 0 && ClientSiegeState.credits() >= rc;
        repair.setMessage(Component.literal(rc > 0 ? "Repair " + rc : "Repaired"));
        sell.setMessage(Component.literal("Sell +" + (int) (k.totalInvested(lvl) * 0.6f * (turret.getHealth() / turret.getMaxHealth()))));
        mode.setMessage(Component.literal("Targeting: " + turret.mode().title));
        super.render(g, mx, my, pt);
    }

    private int stat(GuiGraphics g, int y, String name, String now, String next) {
        g.drawString(font, name, left + 8, y, 0xC8C8C8, false);
        g.drawString(font, now, left + 110, y, 0xFFFFFF, false);
        if (next != null) g.drawString(font, "→ " + next, left + 170, y, 0x7CFF9B, false);
        return y + 11;
    }

    private static String fmt(float f) {
        return f == (int) f ? String.valueOf((int) f) : String.format("%.1f", f);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
