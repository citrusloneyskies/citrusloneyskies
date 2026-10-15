package com.lastbastion.network;

import com.lastbastion.client.ClientScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Opens the core war-room, a turret console or a hero command screen on the client. */
public record OpenScreenPacket(int type, int entityId, BlockPos pos, int[] fallen) {
    public static final int CORE = 0, TURRET = 1, HERO = 2;

    public static OpenScreenPacket core(BlockPos pos) {
        return new OpenScreenPacket(CORE, -1, pos, new int[0]);
    }

    public static OpenScreenPacket core(BlockPos pos, int[] fallen) {
        return new OpenScreenPacket(CORE, -1, pos, fallen);
    }

    public static OpenScreenPacket turret(int id) {
        return new OpenScreenPacket(TURRET, id, BlockPos.ZERO, new int[0]);
    }

    public static OpenScreenPacket hero(int id) {
        return new OpenScreenPacket(HERO, id, BlockPos.ZERO, new int[0]);
    }

    public OpenScreenPacket(FriendlyByteBuf b) {
        this(b.readVarInt(), b.readVarInt(), b.readBlockPos(), b.readVarIntArray());
    }

    public void encode(FriendlyByteBuf b) {
        b.writeVarInt(type);
        b.writeVarInt(entityId);
        b.writeBlockPos(pos);
        b.writeVarIntArray(fallen);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        try {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientScreens.open(this));
        ctx.get().setPacketHandled(true);
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("OpenScreenPacket.handle", lbCrash);
        }
    }
}
