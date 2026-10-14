package com.lastbastion.network;

import com.lastbastion.item.weapon.MeleeCombat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: the player swung their melee weapon (left click, or attack held down). */
public record WeaponSwingPacket(int unused) {
    public WeaponSwingPacket(FriendlyByteBuf b) {
        this(b.readByte());
    }

    public void encode(FriendlyByteBuf b) {
        b.writeByte(unused);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        ctx.get().setPacketHandled(true);
        if (p != null) MeleeCombat.swing(p.serverLevel(), p, false);
    }
}
