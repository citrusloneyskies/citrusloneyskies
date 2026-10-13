package com.lastbastion.network;

import com.lastbastion.client.ClientSiegeState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Floating "+12" credit text above a killed enemy. */
public record CreditPopupPacket(Vec3 pos, int amount) {
    public CreditPopupPacket(FriendlyByteBuf b) {
        this(new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), b.readVarInt());
    }

    public void encode(FriendlyByteBuf b) {
        b.writeDouble(pos.x);
        b.writeDouble(pos.y);
        b.writeDouble(pos.z);
        b.writeVarInt(amount);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSiegeState.popup(pos, amount));
        ctx.get().setPacketHandled(true);
    }
}
