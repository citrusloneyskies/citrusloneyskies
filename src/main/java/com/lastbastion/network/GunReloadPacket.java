package com.lastbastion.network;

import com.lastbastion.item.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: reload the gun in the main hand (Reload key). */
public record GunReloadPacket(int unused) {
    public GunReloadPacket(FriendlyByteBuf b) {
        this(b.readByte());
    }

    public void encode(FriendlyByteBuf b) {
        b.writeByte(unused);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        ctx.get().setPacketHandled(true);
        if (p != null && p.getMainHandItem().getItem() instanceof GunItem g) g.startReload(p.level(), p, p.getMainHandItem());
    }
}
