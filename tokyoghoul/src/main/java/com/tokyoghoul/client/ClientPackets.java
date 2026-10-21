package com.tokyoghoul.client;

import com.tokyoghoul.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Client-side packet handlers (only loaded on the client). */
public final class ClientPackets {
    private ClientPackets() {
    }

    public static void playerData(Packets.PlayerData m) {
        ClientState.self = m;
    }

    public static void visual(Packets.Visual m) {
        ClientState.visual(m.entity(), m.character(), m.flags());
        var level = Minecraft.getInstance().level;
        if (level != null) {
            Entity e = level.getEntity(m.entity());
            if (e != null) e.refreshDimensions();
        }
    }

    public static void anim(Packets.Anim m) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity e = level.getEntity(m.entity());
        if (e != null) ClientState.anim(e, m.anim());
    }
}
