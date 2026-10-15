package com.lastbastion.network;

import com.lastbastion.siege.SiegeManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: apply edited siege rules ({@code reset} restores the server defaults instead). */
public record RulesApplyPacket(CompoundTag rules, boolean reset) {
    public RulesApplyPacket(FriendlyByteBuf b) {
        this(b.readNbt(), b.readBoolean());
    }

    public void encode(FriendlyByteBuf b) {
        b.writeNbt(rules);
        b.writeBoolean(reset);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        ctx.get().setPacketHandled(true);
        if (p != null) SiegeManager.applyRules(p, rules == null ? new CompoundTag() : rules, reset);
    }
}
