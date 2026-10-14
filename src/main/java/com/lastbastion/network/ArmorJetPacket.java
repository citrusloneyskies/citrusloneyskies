package com.lastbastion.network;

import com.lastbastion.item.armor.ArmorSets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: jump pressed in mid-air while wearing the Titan Exo-Frame. */
public record ArmorJetPacket(int unused) {
    public ArmorJetPacket(FriendlyByteBuf b) {
        this(b.readByte());
    }

    public void encode(FriendlyByteBuf b) {
        b.writeByte(unused);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        ctx.get().setPacketHandled(true);
        if (p != null) ArmorSets.jumpJet(p);
    }
}
