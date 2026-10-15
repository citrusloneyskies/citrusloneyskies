package com.lastbastion.client.screen;

import com.lastbastion.entity.hero.HeroEntity;
import com.lastbastion.entity.hero.HeroMode;
import com.lastbastion.network.ActionPacket;
import com.lastbastion.network.Net;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Orders for a single hero: behaviour mode, stats, moveset, dismissal. */
public class HeroScreen extends Screen {
    private final HeroEntity hero;
    private int left, top;
    private static final int W = 260, H = 180;

    public HeroScreen(HeroEntity hero) {
        super(Component.literal(hero.kind.title));
        this.hero = hero;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int bw = (W - 16) / 3;
        for (int i = 0; i < HeroMode.values().length; i++) {
            HeroMode m = HeroMode.values()[i];
            addRenderableWidget(Button.builder(Component.literal(m.title), b -> Net.toServer(new ActionPacket(ActionPacket.HERO_MODE, hero.getId(), m.name())))
                    .bounds(left + 6 + i * (bw + 2), top + H - 50, bw, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal(hero.kind.devil ? "Release (returns to the pact)" : "Dismiss (returns to the sigil)"), b -> {
            Net.toServer(new ActionPacket(ActionPacket.HERO_DISMISS, hero.getId(), ""));
            onClose();
        }).bounds(left + 6, top + H - 26, W - 12, 20).build());
    }

    @Override
    public void tick() {
        try {
        if (!hero.isAlive()) onClose();
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("HeroScreen.tick", lbCrash);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int lbCrashDepth = com.lastbastion.util.Safe.depth(g.pose());
        try {
        renderBackground(g);
        g.fill(left, top, left + W, top + H, 0xE0101418);
        g.fill(left, top, left + W, top + 2, 0xFF000000 | hero.kind.color);
        g.drawString(font, hero.kind.title, left + 8, top + 7, hero.kind.color, true);
        String lvl = "Level " + hero.heroLevel();
        g.drawString(font, lvl, left + W - 8 - font.width(lvl), top + 7, 0xFFD966, true);
        int y = top + 22;
        int lv = hero.heroLevel();
        int cur = HeroEntity.XP_LEVELS[lv - 1];
        int next = lv < 5 ? HeroEntity.XP_LEVELS[lv] : cur;
        float xpFrac = lv >= 5 ? 1f : (hero.xp() - cur) / (float) Math.max(1, next - cur);
        g.fill(left + 8, y, left + W - 8, y + 5, 0xFF202020);
        g.fill(left + 8, y, left + 8 + (int) ((W - 16) * Math.max(0, Math.min(1, xpFrac))), y + 5, 0xFFFFD966);
        y += 9;
        float hp = hero.getHealth() / hero.getMaxHealth();
        g.fill(left + 8, y, left + W - 8, y + 5, 0xFF202020);
        g.fill(left + 8, y, left + 8 + (int) ((W - 16) * hp), y + 5, 0xFF50D060);
        y += 7;
        float ult = hero.ultEnergy() / 100f;
        g.fill(left + 8, y, left + W - 8, y + 4, 0xFF202020);
        int ultCol = ult >= 1 ? (hero.tickCount / 4 % 2 == 0 ? 0xFFFFFFFF : 0xFF000000 | hero.kind.color) : 0xFF000000 | hero.kind.color;
        g.fill(left + 8, y, left + 8 + (int) ((W - 16) * ult), y + 4, ultCol);
        y += 8;
        g.drawString(font, "HP " + (int) hero.getHealth() + "/" + (int) hero.getMaxHealth() + "   XP " + hero.xp() + (lv < 5 ? "/" + next : " (max)")
                        + "   Ultimate " + (int) hero.ultEnergy() + "%   " + hero.kind.role.name().charAt(0) + hero.kind.role.name().substring(1).toLowerCase(),
                left + 8, y, 0xC8C8C8, false);
        y += 12;
        for (FormattedCharSequence s : font.split(Component.literal(hero.kind.moves), W - 16)) {
            g.drawString(font, s, left + 8, y, 0x9FD8FF, false);
            y += 10;
        }
        y += 2;
        g.drawString(font, "Orders: " + hero.mode().title, left + 8, y, 0x7CFF9B, false);
        y += 10;
        for (FormattedCharSequence s : font.split(Component.literal(hero.mode().desc), W - 16)) {
            g.drawString(font, s, left + 8, y, 0x909090, false);
            y += 10;
        }
        super.render(g, mx, my, pt);
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("HeroScreen.render", lbCrash);
            com.lastbastion.util.Safe.restore(g.pose(), lbCrashDepth);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
