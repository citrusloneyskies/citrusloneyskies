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

/**
 * Turret console: stats now vs. next level, perks, targeting mode, upgrade / repair / sell. At level 4
 * the upgrade button splits into the two level-5 specialisations, each described side by side.
 */
public class TurretScreen extends Screen {
    private final TurretEntity turret;
    private Button upgrade, upgradeA, upgradeB, repair, sell, mode;
    private int left, top;
    private static final int W = 300, H = 236;

    public TurretScreen(TurretEntity turret) {
        super(Component.literal(turret.kind.title));
        this.turret = turret;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int by = top + H - 26;
        upgrade = addRenderableWidget(Button.builder(Component.literal("Upgrade"), b -> act(ActionPacket.TURRET_UPGRADE, ""))
                .bounds(left + 6, by, 104, 20).build());
        upgradeA = addRenderableWidget(Button.builder(Component.literal("A"), b -> act(ActionPacket.TURRET_UPGRADE, "A"))
                .bounds(left + 6, top + 150, W / 2 - 9, 20).build());
        upgradeB = addRenderableWidget(Button.builder(Component.literal("B"), b -> act(ActionPacket.TURRET_UPGRADE, "B"))
                .bounds(left + W / 2 + 3, top + 150, W / 2 - 9, 20).build());
        repair = addRenderableWidget(Button.builder(Component.literal("Repair"), b -> act(ActionPacket.TURRET_REPAIR, ""))
                .bounds(left + 114, by, 86, 20).build());
        sell = addRenderableWidget(Button.builder(Component.literal("Sell"), b -> {
            act(ActionPacket.TURRET_SELL, "");
            onClose();
        }).bounds(left + 204, by, W - 210, 20).build());
        mode = addRenderableWidget(Button.builder(Component.literal("Target"), b -> act(ActionPacket.TURRET_MODE, ""))
                .bounds(left + 6, by - 24, W - 12, 20).build());
    }

    private void act(int a, String arg) {
        Net.toServer(new ActionPacket(a, turret.getId(), arg));
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
        g.fill(left, top, left + W, top + H, 0xE80E1216);
        g.fill(left, top, left + W, top + 2, 0xFF000000 | k.color);
        g.fill(left, top + H - 2, left + W, top + H, 0xFF000000 | k.color);
        String title = k.title + (lvl >= 5 ? " - " + turret.specialisation() : "");
        g.drawString(font, title, left + 8, top + 7, k.color, true);
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= TurretKind.MAX_LEVEL; i++) stars.append(i <= lvl ? "★" : "☆");
        g.drawString(font, stars.toString(), left + W - 8 - font.width(stars.toString()), top + 7, 0xFFD966, true);
        int y = top + 22;
        boolean max = lvl >= TurretKind.MAX_LEVEL;
        y = stat(g, y, "Damage", fmt(turret.damage()), max ? null : fmt(k.damageAt(lvl + 1)));
        y = stat(g, y, "Range", fmt(turret.range()), max ? null : fmt(k.rangeAt(lvl + 1)));
        y = stat(g, y, "Attacks/sec", fmt(20f / Math.max(1, turret.fireInterval())), max ? null : fmt(20f / k.cooldownAt(lvl + 1)));
        y = stat(g, y, "Hull", (int) turret.getHealth() + "/" + (int) turret.getMaxHealth(), max ? null : String.valueOf((int) k.hpAt(lvl + 1)));
        g.drawString(font, "Element: " + k.element.name() + "    Kills: " + turret.kills(), left + 8, y, 0xA0A0A0, false);
        y += 13;
        for (FormattedCharSequence s : font.split(Component.literal((lvl >= 3 ? "✔ " : "✖ ") + "Lv3 " + k.perk3), W - 16)) {
            g.drawString(font, s, left + 8, y, lvl >= 3 ? 0xFFB000 : 0x707070, false);
            y += 10;
        }
        y += 3;
        int cost = k.upgradeCost(lvl);
        boolean choosing = lvl == 4;
        upgrade.visible = !choosing;
        upgradeA.visible = upgradeB.visible = choosing;
        if (choosing) {
            g.drawString(font, "Choose a Lv5 specialisation:", left + 8, y, 0xC86BFF, false);
            y += 12;
            int colW = W / 2 - 12;
            int ya = y, yb = y;
            g.drawString(font, "A: " + k.branchA, left + 8, ya, 0xFFD966, false);
            ya += 10;
            for (FormattedCharSequence s : font.split(Component.literal(k.branchADesc), colW)) {
                g.drawString(font, s, left + 8, ya, 0xD0D0D0, false);
                ya += 9;
            }
            g.drawString(font, "B: " + k.branchB, left + W / 2 + 4, yb, 0x7CE0FF, false);
            yb += 10;
            for (FormattedCharSequence s : font.split(Component.literal(k.branchBDesc), colW)) {
                g.drawString(font, s, left + W / 2 + 4, yb, 0xD0D0D0, false);
                yb += 9;
            }
            int by = Math.max(ya, yb) + 3;
            upgradeA.setY(by);
            upgradeB.setY(by);
            boolean afford = ClientSiegeState.credits() >= cost;
            upgradeA.active = upgradeB.active = afford;
            upgradeA.setMessage(Component.literal(k.branchA + " (" + cost + ")"));
            upgradeB.setMessage(Component.literal(k.branchB + " (" + cost + ")"));
        } else {
            String l5 = lvl >= 5 ? "✔ Lv5 " + turret.specialisation() + ": " + (turret.isB() ? k.branchBDesc : k.branchADesc)
                    : "✖ Lv5: choose " + k.branchA + " or " + k.branchB;
            for (FormattedCharSequence s : font.split(Component.literal(l5), W - 16)) {
                g.drawString(font, s, left + 8, y, lvl >= 5 ? 0xC86BFF : 0x707070, false);
                y += 10;
            }
            upgrade.active = !max && ClientSiegeState.credits() >= cost;
            upgrade.setMessage(Component.literal(max ? "MAX LEVEL" : "Upgrade " + cost));
        }
        int rc = turret.repairCost();
        repair.active = rc > 0 && ClientSiegeState.credits() >= rc;
        repair.setMessage(Component.literal(rc > 0 ? "Repair " + rc : "Repaired"));
        sell.setMessage(Component.literal("Sell +" + turret.sellValue()));
        mode.setMessage(Component.literal("Targeting: " + turret.mode().title));
        super.render(g, mx, my, pt);
    }

    private int stat(GuiGraphics g, int y, String name, String now, String next) {
        g.drawString(font, name, left + 8, y, 0xC8C8C8, false);
        g.drawString(font, now, left + 120, y, 0xFFFFFF, false);
        if (next != null) g.drawString(font, "→ " + next, left + 190, y, 0x7CFF9B, false);
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
