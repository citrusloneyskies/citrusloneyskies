package com.lastbastion.client.render.armor;

import com.lastbastion.LastBastion;
import com.lastbastion.item.armor.ArmorSet;
import com.lastbastion.item.armor.ArmorSets;
import com.lastbastion.network.ArmorJetPacket;
import com.lastbastion.network.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Titan Exo-Frame jump jets: a fresh press of jump while airborne asks the server for a boost. */
@Mod.EventBusSubscriber(modid = LastBastion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArmorInput {
    private static boolean wasDown;
    private static int jets;

    private ArmorInput() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        boolean down = mc.options.keyJump.isDown() && mc.screen == null;
        if (p.onGround()) jets = 0;
        if (down && !wasDown && !p.onGround() && !p.isInWater() && !p.getAbilities().flying && p.getVehicle() == null
                && jets < ArmorSets.MAX_JETS && ArmorSets.wearing(p, ArmorSet.TITAN)) {
            jets++;
            Net.toServer(new ArmorJetPacket(0));
        }
        wasDown = down;
    }
}
