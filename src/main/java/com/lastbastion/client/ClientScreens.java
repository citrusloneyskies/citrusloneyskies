package com.lastbastion.client;

import com.lastbastion.client.screen.CoreScreen;
import com.lastbastion.client.screen.HeroScreen;
import com.lastbastion.client.screen.TurretScreen;
import com.lastbastion.entity.hero.HeroEntity;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.network.OpenScreenPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class ClientScreens {
    private ClientScreens() {
    }

    public static void open(OpenScreenPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        switch (p.type()) {
            case OpenScreenPacket.CORE -> mc.setScreen(new CoreScreen(p.pos(), p.fallen()));
            case OpenScreenPacket.TURRET -> {
                Entity e = mc.level.getEntity(p.entityId());
                if (e instanceof TurretEntity t) mc.setScreen(new TurretScreen(t));
            }
            case OpenScreenPacket.HERO -> {
                Entity e = mc.level.getEntity(p.entityId());
                if (e instanceof HeroEntity h) mc.setScreen(new HeroScreen(h));
            }
            default -> {
            }
        }
    }

    public static void codex() {
        Minecraft.getInstance().setScreen(new com.lastbastion.client.screen.CodexScreen());
    }

    public static void rules(com.lastbastion.network.RulesPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof com.lastbastion.client.screen.RulesScreen rs) {
            rs.refresh(p.rules(), p.canEdit());
        } else if (p.open()) {
            mc.setScreen(new com.lastbastion.client.screen.RulesScreen(p.rules(), p.canEdit()));
        }
    }
}
