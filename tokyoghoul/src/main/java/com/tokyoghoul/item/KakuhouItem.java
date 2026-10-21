package com.tokyoghoul.item;

import com.tokyoghoul.combat.Fighters;
import com.tokyoghoul.combat.Fx;
import com.tokyoghoul.data.KaguneType;
import com.tokyoghoul.network.Sync;
import com.tokyoghoul.player.PlayerCaps;
import com.tokyoghoul.player.TGPlayerData;
import com.tokyoghoul.registry.TGItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A ghoul's kakuhou, the organ that makes Rc cells. Ghouls who eat it grow toward a kakuja (the
 * cannibal path that unlocks ultimates, JJK's mastery advancements); anyone can forge it into a quinque.
 */
public class KakuhouItem extends Item {
    public final KaguneType type;

    public KakuhouItem(KaguneType type) {
        super(new Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(6).saturationMod(0.8f).alwaysEat().meat().build()));
        this.type = type;
    }

    /** Remember whose kakuhou this was (named bosses). */
    public static void set(ItemStack stack, KaguneType type, String owner) {
        if (!owner.isEmpty()) stack.getOrCreateTag().putString("Owner", owner);
    }

    public static ItemStack of(KaguneType type, String owner) {
        ItemStack s = new ItemStack(TGItems.kakuhou(type));
        set(s, type, owner);
        return s;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide && user instanceof Player p) {
            TGPlayerData d = PlayerCaps.get(p);
            if (Fighters.race(p).hasKagune) {
                int gain = stack.hasTag() && stack.getTag().contains("Owner") ? 25 : 10;
                int before = d.kakuja;
                d.kakuja = Math.min(100, d.kakuja + gain);
                d.rc = Math.min(d.maxRc(), d.rc + d.maxRc() * 0.5);
                p.displayClientMessage(Component.translatable("msg.tokyoghoul.kakuja_progress", d.kakuja).withStyle(ChatFormatting.DARK_RED), true);
                if (before < 100 && d.kakuja >= 100) {
                    p.sendSystemMessage(Component.translatable("msg.tokyoghoul.kakuja_ready").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    Fx.sound(level, p.position(), SoundEvents.WITHER_SPAWN, 0.6f, 1.4f);
                }
                if (p instanceof ServerPlayer sp) Sync.playerData(sp);
            } else {
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
                p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            }
        }
        return super.finishUsingItem(stack, level, user);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("kagune.tokyoghoul." + type.id).withStyle(ChatFormatting.DARK_RED));
        if (stack.hasTag() && stack.getTag().contains("Owner")) {
            tip.add(Component.translatable("tooltip.tokyoghoul.kakuhou_owner", stack.getTag().getString("Owner")).withStyle(ChatFormatting.GOLD));
        }
        tip.add(Component.translatable("tooltip.tokyoghoul.kakuhou").withStyle(ChatFormatting.GRAY));
    }
}
