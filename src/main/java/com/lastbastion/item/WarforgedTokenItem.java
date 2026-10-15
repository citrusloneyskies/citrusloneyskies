package com.lastbastion.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Warforged Token: hammered from the arms of fallen warlords. Spent at the Siege Core it raises the weapon in
 * your hand by one Mk for free. Dropped by Warlords and bosses, granted at wave milestones and found in rare
 * supply drops.
 */
public class WarforgedTokenItem extends Item {
    public WarforgedTokenItem(Properties p) {
        super(p.stacksTo(16).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Hammered from the arms of fallen warlords.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tip.add(Component.literal("At the Siege Core (Arsenal tab): upgrade the weapon in your hand by one Mk, free.").withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("Dropped by Warlords and bosses; awarded every 10 waves.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
