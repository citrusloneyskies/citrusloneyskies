package com.lastbastion.client.screen;

import com.lastbastion.entity.hero.HeroMode;
import com.lastbastion.network.ActionPacket;
import com.lastbastion.network.Net;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Quick order wheel (default key G): set every hero you own to Guard / Hunt / Follow. */
public class HeroCommandScreen extends Screen {
    public HeroCommandScreen() {
        super(Component.literal("Hero Orders"));
    }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;
        for (int i = 0; i < HeroMode.values().length; i++) {
            HeroMode m = HeroMode.values()[i];
            addRenderableWidget(Button.builder(Component.literal(m.title), b -> {
                Net.toServer(new ActionPacket(ActionPacket.ALL_HEROES_MODE, 0, m.name()));
                onClose();
            }).bounds(cx - 60, cy - 30 + i * 24, 120, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, "Order all your heroes:", width / 2, height / 2 - 46, 0xFFD966);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
