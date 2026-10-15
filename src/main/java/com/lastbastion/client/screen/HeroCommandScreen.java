package com.lastbastion.client.screen;

import com.lastbastion.entity.hero.HeroMode;
import com.lastbastion.network.ActionPacket;
import com.lastbastion.network.Net;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Quick orders (default key G): set every hero and devil you own to Guard / Hunt / Follow, or recall them all. */
public class HeroCommandScreen extends Screen {
    public HeroCommandScreen() {
        super(Component.literal("Summon Orders"));
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
        addRenderableWidget(Button.builder(Component.literal("Recall all to me"), b -> {
            Net.toServer(new ActionPacket(ActionPacket.RECALL_HEROES, 0, ""));
            onClose();
        }).bounds(cx - 60, cy - 30 + HeroMode.values().length * 24 + 6, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, "Order all your heroes and devils:", width / 2, height / 2 - 46, 0xFFD966);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
