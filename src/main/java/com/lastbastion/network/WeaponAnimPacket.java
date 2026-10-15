package com.lastbastion.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: an entity performed a weapon swing (style, combo step, duration in ticks). */
public record WeaponAnimPacket(int entity, int weapon, int style, int step, int duration) {
    public WeaponAnimPacket(FriendlyByteBuf b) {
        this(b.readVarInt(), b.readByte(), b.readByte(), b.readByte(), b.readByte());
    }

    public void encode(FriendlyByteBuf b) {
        b.writeVarInt(entity);
        b.writeByte(weapon);
        b.writeByte(style);
        b.writeByte(step);
        b.writeByte(duration);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        try {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lastbastion.client.render.weapon.WeaponAnimations.onPacket(this));
        ctx.get().setPacketHandled(true);
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("WeaponAnimPacket.handle", lbCrash);
        }
    }
}
