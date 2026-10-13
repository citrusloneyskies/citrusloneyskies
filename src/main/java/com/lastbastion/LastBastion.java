package com.lastbastion;

import com.lastbastion.config.SiegeConfig;
import com.lastbastion.network.Net;
import com.lastbastion.registry.*;
import com.lastbastion.siege.SiegeEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Last Bastion: Zombie Siege. Place a Siege Core and hold it against escalating nightly sieges
 * with turrets, heroes, fortifications and an arsenal bought with credits.
 */
@Mod(LastBastion.MODID)
public class LastBastion {
    public static final String MODID = "lastbastion";

    public LastBastion() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModEffects.EFFECTS.register(bus);
        ModTabs.TABS.register(bus);
        bus.addListener(ModEntities::attributes);
        bus.addListener(this::setup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SiegeConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(SiegeEvents.class);
    }

    private void setup(FMLCommonSetupEvent e) {
        e.enqueueWork(Net::register);
    }
}
