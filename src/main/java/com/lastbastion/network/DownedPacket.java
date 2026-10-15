package com.lastbastion.network;

import com.lastbastion.client.ClientDowned;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Downed-state updates for a player: pose, bleed-out timer and revive progress (HUD and world labels). */
public record DownedPacket(int entity, boolean downed, int bleed, int maxBleed, int revive) {
    public DownedPacket(FriendlyByteBuf b) {
        this(b.readVarInt(), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt());
    }

    public void encode(FriendlyByteBuf b) {
        b.writeVarInt(entity);
        b.writeBoolean(downed);
        b.writeVarInt(Math.max(0, bleed));
        b.writeVarInt(Math.max(1, maxBleed));
        b.writeVarInt(Math.max(0, revive));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientDowned.update(this));
        ctx.get().setPacketHandled(true);
    }
}
