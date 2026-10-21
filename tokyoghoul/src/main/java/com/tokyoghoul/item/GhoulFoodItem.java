package com.tokyoghoul.item;

import com.tokyoghoul.combat.Fighters;
import com.tokyoghoul.player.PlayerCaps;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Food that matters to the ghoul diet: human flesh (ghouls only; humans get sick), Anteiku coffee
 * (the one drink a ghoul's tongue tolerates) and Rc cell packs (restore Rc).
 */
public class GhoulFoodItem extends Item {
    public enum Kind {FLESH, COFFEE, RC_CELLS}

    public final Kind kind;

    public GhoulFoodItem(Kind kind) {
        super(new Properties().stacksTo(kind == Kind.COFFEE ? 16 : 64).food(switch (kind) {
            case FLESH -> new FoodProperties.Builder().nutrition(8).saturationMod(0.9f).meat().build();
            case COFFEE -> new FoodProperties.Builder().nutrition(2).saturationMod(0.4f).alwaysEat().build();
            case RC_CELLS -> new FoodProperties.Builder().nutrition(1).saturationMod(0.2f).alwaysEat().fast().build();
        }));
        this.kind = kind;
    }

    /** Ghouls may eat these without retching. */
    public static boolean ghoulSafe(ItemStack s) {
        return s.getItem() instanceof GhoulFoodItem || s.getItem() instanceof KakuhouItem;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return kind == Kind.COFFEE ? UseAnim.DRINK : UseAnim.EAT;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide && user instanceof Player p) {
            boolean ghoul = Fighters.ghoulBody(p);
            switch (kind) {
                case FLESH -> {
                    if (ghoul) {
                        p.heal(4);
                        Fighters.addRc(p, PlayerCaps.get(p).maxRc() * 0.2);
                    } else {
                        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 0));
                        p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 1));
                    }
                }
                case COFFEE -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
                case RC_CELLS -> Fighters.addRc(p, 30);
            }
        }
        return super.finishUsingItem(stack, level, user);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.tokyoghoul." + kind.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
    }
}
