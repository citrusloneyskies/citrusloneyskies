package com.tokyoghoul.network;

import com.tokyoghoul.TokyoGhoul;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** The mod's channel. Client-bound handlers bounce into {@code client.ClientPackets} through DistExecutor. */
public final class TGNet {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(TokyoGhoul.MODID, "main"), () -> VERSION,
            VERSION::equals, VERSION::equals);

    private TGNet() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, Packets.Key.class, Packets.Key::encode, Packets.Key::decode, Packets.Key::handle);
        CHANNEL.registerMessage(id++, Packets.Select.class, Packets.Select::encode, Packets.Select::decode, Packets.Select::handle);
        CHANNEL.registerMessage(id++, Packets.PlayerData.class, Packets.PlayerData::encode, Packets.PlayerData::decode, Packets.PlayerData::handle);
        CHANNEL.registerMessage(id++, Packets.Visual.class, Packets.Visual::encode, Packets.Visual::decode, Packets.Visual::handle);
        CHANNEL.registerMessage(id++, Packets.Anim.class, Packets.Anim::encode, Packets.Anim::decode, Packets.Anim::handle);
    }
}
