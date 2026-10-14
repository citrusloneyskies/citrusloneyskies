package com.lastbastion.client.screen;

import com.lastbastion.client.ClientSiegeState;
import com.lastbastion.network.ActionPacket;
import com.lastbastion.network.Net;
import com.lastbastion.siege.CoreAbility;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Core Protocols: the core's active abilities, usable within 48 blocks of it (default key V). */
public class ProtocolScreen extends Screen {
    private final List<Button> buttons = new ArrayList<>();
    private int left, top, w, h;
    private static final int ROW = 34;

    public ProtocolScreen() {
        super(Component.literal("Core Protocols"));
    }

    @Override
    protected void init() {
        w = Math.min(330, width - 20);
        h = 30 + CoreAbility.values().length * ROW + 8;
        left = (width - w) / 2;
        top = (height - h) / 2;
        buttons.clear();
        for (CoreAbility a : CoreAbility.values()) {
            int y = top + 26 + a.ordinal() * ROW;
            Button b = Button.builder(Component.literal("Activate"), btn -> {
                Net.toServer(new ActionPacket(ActionPacket.CORE_ABILITY, 0, a.name()));
                onClose();
            }).bounds(left + w - 72, y + 6, 64, 20).build();
            buttons.add(addRenderableWidget(b));
        }
        updateButtons();
    }

    private void updateButtons() {
        int[] up = ClientSiegeState.upgrades();
        for (CoreAbility a : CoreAbility.values()) {
            Button b = buttons.get(a.ordinal());
            boolean unlocked = a.unlocked(up);
            int cd = ClientSiegeState.abilityCooldown(a.ordinal());
            b.active = ClientSiegeState.active() && unlocked && cd <= 0;
            b.setMessage(Component.literal(!unlocked ? "Locked" : cd > 0 ? cd + "s" : "Activate"));
        }
    }

    @Override
    public void tick() {
        updateButtons();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(left, top, left + w, top + h, 0xE0101418);
        g.fill(left, top, left + w, top + 1, 0xFF6FE3FF);
        g.fill(left, top + h - 1, left + w, top + h, 0xFF6FE3FF);
        g.drawString(font, "✦ CORE PROTOCOLS", left + 8, top + 8, 0x6FE3FF, true);
        String core = "Core " + Math.round(ClientSiegeState.coreFraction() * 100) + "%";
        g.drawString(font, core, left + w - 8 - font.width(core), top + 8, 0xA0A0A0, false);
        int[] up = ClientSiegeState.upgrades();
        for (CoreAbility a : CoreAbility.values()) {
            int y = top + 26 + a.ordinal() * ROW;
            boolean unlocked = a.unlocked(up);
            int cd = ClientSiegeState.abilityCooldown(a.ordinal());
            g.fill(left + 6, y, left + w - 6, y + ROW - 2, 0xFF161B21);
            int col = 0xFF000000 | a.color;
            g.fill(left + 6, y, left + 9, y + ROW - 2, unlocked ? col : 0xFF404040);
            if (unlocked && cd > 0) {
                float frac = 1f - cd / (float) a.cooldownSeconds;
                g.fill(left + 9, y + ROW - 4, left + 9 + (int) ((w - 90) * frac), y + ROW - 2, col);
            }
            g.drawString(font, a.title, left + 14, y + 4, unlocked ? a.color : 0x707070, true);
            String sub = unlocked ? a.desc : "Requires " + (a.requires == null ? "" : a.requires.title + " " + a.requiredLevel) + " (core upgrade)";
            List<FormattedCharSequence> lines = font.split(Component.literal(sub), w - 96);
            for (int i = 0; i < Math.min(2, lines.size()); i++) g.drawString(font, lines.get(i), left + 14, y + 14 + i * 9, 0xB0B0B0, false);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
