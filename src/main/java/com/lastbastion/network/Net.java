package com.lastbastion.network;

import com.lastbastion.LastBastion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Net {
    private static final String PROTOCOL = "3";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(LastBastion.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private Net() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(HudSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HudSyncPacket::encode).decoder(HudSyncPacket::new).consumerMainThread(HudSyncPacket::handle).add();
        CHANNEL.messageBuilder(CreditPopupPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CreditPopupPacket::encode).decoder(CreditPopupPacket::new).consumerMainThread(CreditPopupPacket::handle).add();
        CHANNEL.messageBuilder(OpenScreenPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenScreenPacket::encode).decoder(OpenScreenPacket::new).consumerMainThread(OpenScreenPacket::handle).add();
        CHANNEL.messageBuilder(ActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActionPacket::encode).decoder(ActionPacket::new).consumerMainThread(ActionPacket::handle).add();
        CHANNEL.messageBuilder(ExoInputPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ExoInputPacket::encode).decoder(ExoInputPacket::new).consumerMainThread(ExoInputPacket::handle).add();
        CHANNEL.messageBuilder(VfxPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(VfxPacket::encode).decoder(VfxPacket::new).consumerMainThread(VfxPacket::handle).add();
        CHANNEL.messageBuilder(SpeechPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SpeechPacket::encode).decoder(SpeechPacket::new).consumerMainThread(SpeechPacket::handle).add();
    }

    public static void sendTo(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void toServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }
}
