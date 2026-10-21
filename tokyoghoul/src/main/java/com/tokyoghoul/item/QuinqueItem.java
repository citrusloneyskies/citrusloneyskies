package com.tokyoghoul.item;

import com.tokyoghoul.data.Characters;
import com.tokyoghoul.move.MoveDef;
import com.tokyoghoul.move.MoveRunner;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/** A quinque: a melee weapon that also fires its signature technique on right-click. */
public class QuinqueItem extends SwordItem {
    public final QuinqueKind kind;

    private static final Tier QUINQUE_TIER = new Tier() {
        @Override
        public int getUses() {
            return 2400;
        }

        @Override
        public float getSpeed() {
            return 8;
        }

        @Override
        public float getAttackDamageBonus() {
            return 0;
        }

        @Override
        public int getLevel() {
            return 4;
        }

        @Override
        public int getEnchantmentValue() {
            return 14;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(com.tokyoghoul.registry.TGItems.RC_CELLS.get());
        }
    };

    public QuinqueItem(QuinqueKind kind) {
        super(QUINQUE_TIER, (int) kind.damage - 1, kind.speed, new Properties().stacksTo(1).durability(2400));
        this.kind = kind;
    }

    public MoveDef move() {
        MoveDef d = Characters.move(kind.move);
        return d == null ? com.tokyoghoul.data.Moves.KNOCKBACK : d;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            MoveDef d = move();
            MoveRunner.Fail f = MoveRunner.start(player, d, null);
            if (f != MoveRunner.Fail.NONE) {
                Component msg = MoveRunner.failMessage(f, d);
                if (!msg.getString().isEmpty()) player.displayClientMessage(msg, true);
                return InteractionResultHolder.fail(stack);
            }
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("kagune.tokyoghoul." + kind.type.id).withStyle(ChatFormatting.DARK_RED));
        tip.add(Component.translatable("tooltip.tokyoghoul.quinque_move", move().title()).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(com.tokyoghoul.client.render.QuinqueClientExt.INSTANCE);
    }
}
