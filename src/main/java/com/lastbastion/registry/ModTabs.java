package com.lastbastion.registry;

import com.lastbastion.LastBastion;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LastBastion.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.lastbastion"))
            .icon(() -> new ItemStack(ModItems.SIEGE_CORE.get()))
            .displayItems((params, out) -> ModItems.ITEMS.getEntries().forEach(i -> out.accept(i.get())))
            .build());

    private ModTabs() {
    }
}
