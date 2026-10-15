package com.lastbastion.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

/** The Siege Codex: opens the in-game encyclopedia (zombies, bosses, turrets, heroes, devils, arsenal, rules). */
public class CodexItem extends Item {
    public CodexItem(Properties p) {
        super(p.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lastbastion.client.ClientScreens.codex());
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        try {
        tip.add(Component.literal("Everything about the siege, with 3D models of every foe and ally.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Right-click to read.").withStyle(ChatFormatting.DARK_GRAY));
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("CodexItem.appendHoverText", lbCrash);
        }
    }
}
