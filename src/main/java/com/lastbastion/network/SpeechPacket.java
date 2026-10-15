package com.lastbastion.network;

import com.lastbastion.client.ClientSiegeState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A short line spoken by a hero or boss, shown as a floating caption above its head. */
public record SpeechPacket(int entityId, String text, int color) {
    public SpeechPacket(FriendlyByteBuf b) {
        this(b.readVarInt(), b.readUtf(160), b.readInt());
    }

    public void encode(FriendlyByteBuf b) {
        b.writeVarInt(entityId);
        b.writeUtf(text, 160);
        b.writeInt(color);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        try {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSiegeState.speech(entityId, text, color));
        ctx.get().setPacketHandled(true);
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("SpeechPacket.handle", lbCrash);
        }
    }
}
