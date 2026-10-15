package com.lastbastion.network;

import com.lastbastion.client.ClientSiegeState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Siege state for the HUD and war room, sent twice a second.
 *
 * @param toWave     ticks until the next wave (-1 when only the war horn starts waves)
 * @param trigger    the world's wave trigger rule ({@link com.lastbastion.siege.SiegeRules#TRIGGERS})
 * @param perfect    the core's hull is still untouched this wave
 * @param boss       the current (or coming) wave is a boss wave under this world's rules
 * @param intel      forecast of the next wave while preparing (see {@code SiegeManager.intel})
 * @param objectives live war events and downed teammates, {@code RaidEvents.OBJ_STRIDE} ints each
 */
public record HudSyncPacket(int state, int wave, float coreHp, float coreMax, float shield, float shieldMax, int remaining, int total,
                            int modifier, int credits, int toWave, BlockPos corePos, int bestWave, int[] upgrades, int[] abilityCd, int aegis,
                            int trigger, int victoryWave, boolean perfect, boolean boss, int[] intel, int[] objectives) {
    public HudSyncPacket(FriendlyByteBuf b) {
        this(b.readVarInt(), b.readVarInt(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readVarInt(), b.readVarInt(),
                b.readVarInt(), b.readVarInt(), b.readInt(), b.readBlockPos(), b.readVarInt(), b.readVarIntArray(), b.readVarIntArray(), b.readVarInt(),
                b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean(), b.readVarIntArray(), b.readVarIntArray());
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
        b.writeInt(toWave);
        b.writeBlockPos(corePos);
        b.writeVarInt(bestWave);
        b.writeVarIntArray(upgrades);
        b.writeVarIntArray(abilityCd);
        b.writeVarInt(aegis);
        b.writeVarInt(trigger);
        b.writeVarInt(victoryWave);
        b.writeBoolean(perfect);
        b.writeBoolean(boss);
        b.writeVarIntArray(intel);
        b.writeVarIntArray(objectives);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSiegeState.update(this));
        ctx.get().setPacketHandled(true);
    }
}
