package com.lastbastion.network;

import com.lastbastion.entity.misc.TitanExoEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ExoInputPacket(boolean fire, boolean missiles, boolean boost) {
    public ExoInputPacket(FriendlyByteBuf b) {
        this(b.readBoolean(), b.readBoolean(), b.readBoolean());
    }

    public void encode(FriendlyByteBuf b) {
        b.writeBoolean(fire);
        b.writeBoolean(missiles);
        b.writeBoolean(boost);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        ctx.get().setPacketHandled(true);
        if (p != null && p.getVehicle() instanceof TitanExoEntity exo) {
            exo.fireInput = fire;
            exo.missileInput = missiles;
            exo.boostInput = boost;
        }
    }
}
