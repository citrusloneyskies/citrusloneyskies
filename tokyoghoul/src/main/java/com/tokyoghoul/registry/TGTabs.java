package com.tokyoghoul.registry;

import com.tokyoghoul.TokyoGhoul;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class TGTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TokyoGhoul.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tokyoghoul"))
            .icon(() -> new ItemStack(TGItems.GHOUL_REGISTRY.get()))
            .displayItems((params, out) -> {
                for (RegistryObject<Item> r : TGItems.ITEMS.getEntries()) {
                    if (!r.getId().getPath().endsWith("_spawn_egg")) out.accept(r.get());
                }
            }).build());

    public static final RegistryObject<CreativeModeTab> CHARACTERS = TABS.register("characters", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tokyoghoul.characters"))
            .icon(() -> new ItemStack(TGItems.EGGS.get("kaneki").get()))
            .withTabsBefore(MAIN.getKey())
            .displayItems((params, out) -> {
                for (RegistryObject<Item> r : TGItems.EGGS.values()) out.accept(r.get());
            }).build());

    private TGTabs() {
    }
}
