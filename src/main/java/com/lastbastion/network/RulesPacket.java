package com.lastbastion.network;

import com.lastbastion.client.ClientScreens;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: the world's siege rules, whether this player may edit them, and whether to open the screen. */
public record RulesPacket(CompoundTag rules, boolean canEdit, boolean open) {
    public RulesPacket(FriendlyByteBuf b) {
        this(b.readNbt(), b.readBoolean(), b.readBoolean());
    }

    public void encode(FriendlyByteBuf b) {
        b.writeNbt(rules);
        b.writeBoolean(canEdit);
        b.writeBoolean(open);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientScreens.rules(this));
        ctx.get().setPacketHandled(true);
    }
}
