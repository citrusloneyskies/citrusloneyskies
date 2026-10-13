package com.lastbastion.client;

import com.lastbastion.network.HudSyncPacket;
import net.minecraft.world.phys.Vec3;

public final class ClientSiegeState {
    public static HudSyncPacket hud;

    public static void update(HudSyncPacket p) {
        hud = p;
    }

    public static void popup(Vec3 pos, int amount) {
    }

    private ClientSiegeState() {
    }
}
