package com.tokyoghoul;

import com.tokyoghoul.network.TGNet;
import com.tokyoghoul.player.PlayerCaps;
import com.tokyoghoul.player.PlayerEvents;
import com.tokyoghoul.registry.TGEffects;
import com.tokyoghoul.registry.TGEntities;
import com.tokyoghoul.registry.TGItems;
import com.tokyoghoul.registry.TGTabs;
import com.tokyoghoul.world.TGGameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Tokyo Ghoul Craft. Pick any major Tokyo Ghoul / :re character from the Ghoul Registry, fight with their
 * canon kagune or quinques, climb the CCG ranks or ghoul ratings, awaken a kakuja, and face NPCs that
 * fight with the same movesets. Structure modelled on Jujutsu Craft (see docs/JJK_STUDY.md).
 */
@Mod(TokyoGhoul.MODID)
public class TokyoGhoul {
    public static final String MODID = "tokyoghoul";
    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    public TokyoGhoul() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        TGItems.ITEMS.register(bus);
        TGEntities.ENTITIES.register(bus);
        TGEffects.EFFECTS.register(bus);
        TGTabs.TABS.register(bus);
        bus.addListener(TGEntities::attributes);
        bus.addListener(PlayerCaps::register);
        bus.addListener(this::setup);
        TGGameRules.register();
        MinecraftForge.EVENT_BUS.addGenericListener(net.minecraft.world.entity.Entity.class, PlayerCaps::attach);
        MinecraftForge.EVENT_BUS.register(PlayerEvents.class);
    }

    private void setup(FMLCommonSetupEvent e) {
        e.enqueueWork(TGNet::register);
    }
}
