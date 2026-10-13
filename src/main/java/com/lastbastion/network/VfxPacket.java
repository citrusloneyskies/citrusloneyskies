package com.lastbastion.network;

import com.lastbastion.client.fx.VfxManager;
import com.lastbastion.fx.VfxKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** One client-side visual effect (beam, slash, telegraph, shake...). Built by {@link com.lastbastion.fx.Vfx}. */
public record VfxPacket(VfxKind kind, double x, double y, double z, double x2, double y2, double z2, int color, int color2, float size,
                        float extra, int life, int ent1, int ent2) {
    public VfxPacket(FriendlyByteBuf b) {
        this(VfxKind.byId(b.readByte()), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readInt(), b.readInt(), b.readFloat(), b.readFloat(), b.readVarInt(), b.readVarInt() - 1, b.readVarInt() - 1);
    }

    public void encode(FriendlyByteBuf b) {
        b.writeByte(kind.ordinal());
        b.writeDouble(x);
        b.writeDouble(y);
        b.writeDouble(z);
        b.writeDouble(x2);
        b.writeDouble(y2);
        b.writeDouble(z2);
        b.writeInt(color);
        b.writeInt(color2);
        b.writeFloat(size);
        b.writeFloat(extra);
        b.writeVarInt(Math.max(1, life));
        b.writeVarInt(ent1 + 1);
        b.writeVarInt(ent2 + 1);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> VfxManager.add(this));
        ctx.get().setPacketHandled(true);
    }
}
