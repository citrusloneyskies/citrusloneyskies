package com.lastbastion.client;

import com.lastbastion.network.DownedPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** Client mirror of who is downed: forces the crawling pose locally and feeds the HUD and world labels. */
public final class ClientDowned {
    public static final class State {
        public int bleed, maxBleed, revive;
        public long at;
    }

    public static final Map<Integer, State> DOWNED = new HashMap<>();

    private ClientDowned() {
    }

    public static void update(DownedPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(p.entity());
        if (!p.downed()) {
            DOWNED.remove(p.entity());
            if (e instanceof Player pl) pl.setForcedPose(null);
            return;
        }
        State s = DOWNED.computeIfAbsent(p.entity(), k -> new State());
        s.bleed = p.bleed();
        s.maxBleed = p.maxBleed();
        s.revive = p.revive();
        s.at = mc.level.getGameTime();
        if (e instanceof Player pl) pl.setForcedPose(Pose.SWIMMING);
    }

    /** The local player's downed state, or null. */
    public static State self() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : DOWNED.get(mc.player.getId());
    }

    public static boolean isDowned(Entity e) {
        return DOWNED.containsKey(e.getId());
    }

    /** Drop stale entries (entity gone, or no update for 2 seconds) and keep poses forced. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            DOWNED.clear();
            return;
        }
        long now = mc.level.getGameTime();
        DOWNED.entrySet().removeIf(en -> {
            Entity e = mc.level.getEntity(en.getKey());
            boolean stale = e == null || !e.isAlive() || now - en.getValue().at > 40;
            if (stale && e instanceof Player pl) pl.setForcedPose(null);
            if (!stale && e instanceof Player pl && pl.getForcedPose() != Pose.SWIMMING) pl.setForcedPose(Pose.SWIMMING);
            return stale;
        });
    }
}
