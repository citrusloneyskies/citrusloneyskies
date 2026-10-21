package com.tokyoghoul.item;

import com.tokyoghoul.combat.Fx;
import com.tokyoghoul.network.Sync;
import com.tokyoghoul.player.PlayerCaps;
import com.tokyoghoul.player.TGPlayerData;
import com.tokyoghoul.registry.TGEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
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

/** The select book, the satiety thermos and the Rc suppressant. */
public final class UtilityItems {
    private UtilityItems() {
    }

    /**
     * The Ghoul Registry: the CCG's file on every known ghoul and investigator. Opening it lets you become
     * any of them (Jujutsu Craft's Cursed Technique Changer book).
     */
    public static class GhoulRegistry extends Item {
        public GhoulRegistry() {
            super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.tokyoghoul.client.ClientHooks::openSelectScreen);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.translatable("tooltip.tokyoghoul.registry").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Anteiku's thermos of blend: right-click to toggle infinite satiety (Jujutsu Craft's Insect). */
    public static class Thermos extends Item {
        public Thermos() {
            super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide) {
                TGPlayerData d = PlayerCaps.get(player);
                d.satiated = !d.satiated;
                player.displayClientMessage(Component.translatable(d.satiated ? "msg.tokyoghoul.satiated_on" : "msg.tokyoghoul.satiated_off")
                        .withStyle(ChatFormatting.GOLD), true);
                Fx.sound(level, player.position(), SoundEvents.GENERIC_DRINK, 1f, 1f);
                if (player instanceof ServerPlayer sp) Sync.playerData(sp);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return true;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.translatable("tooltip.tokyoghoul.thermos").withStyle(ChatFormatting.GRAY));
        }
    }

    /** CCG RC suppressant syringe: stab a ghoul to shut its kagune down for a while. */
    public static class Suppressant extends Item {
        public Suppressant() {
            super(new Properties().stacksTo(16));
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
            if (!player.level().isClientSide) {
                target.addEffect(new MobEffectInstance(TGEffects.SUPPRESSED.get(), 300, 0));
                Fx.sound(player.level(), target.position(), SoundEvents.BOTTLE_EMPTY, 1f, 1.5f);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.translatable("tooltip.tokyoghoul.suppressant").withStyle(ChatFormatting.GRAY));
        }
    }
}
