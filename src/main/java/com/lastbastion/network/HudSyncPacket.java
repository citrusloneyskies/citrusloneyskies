package com.lastbastion.network;

import com.lastbastion.client.ClientSiegeState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record HudSyncPacket(int state, int wave, float coreHp, float coreMax, float shield, float shieldMax, int remaining, int total,
                            int modifier, int credits, int toNight, BlockPos corePos, int bestWave, int[] upgrades) {
    public HudSyncPacket(FriendlyByteBuf b) {
        this(b.readVarInt(), b.readVarInt(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readVarInt(), b.readVarInt(),
                b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBlockPos(), b.readVarInt(), b.readVarIntArray());
    }

    public void encode(FriendlyByteBuf b) {
        b.writeVarInt(state);
        b.writeVarInt(wave);
        b.writeFloat(coreHp);
        b.writeFloat(coreMax);
        b.writeFloat(shield);
        b.writeFloat(shieldMax);
        b.writeVarInt(remaining);
        b.writeVarInt(total);
        b.writeVarInt(modifier);
        b.writeVarInt(credits);
        b.writeVarInt(toNight);
        b.writeBlockPos(corePos);
        b.writeVarInt(bestWave);
        b.writeVarIntArray(upgrades);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSiegeState.update(this));
        ctx.get().setPacketHandled(true);
    }
}
