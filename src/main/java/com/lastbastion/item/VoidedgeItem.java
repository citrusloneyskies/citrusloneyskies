package com.lastbastion.item;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** 'Voidedge' plasma katana: brutal melee damage plus a right-click phase-dash that slices through a line. */
public class VoidedgeItem extends SwordItem {
    public static final Tier PLASMA = new Tier() {
        public int getUses() {
            return 4000;
        }

        public float getSpeed() {
            return 10f;
        }

        public float getAttackDamageBonus() {
            return 9f;
        }

        public int getLevel() {
            return 4;
        }

        public int getEnchantmentValue() {
            return 20;
        }

        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }
    };

    public VoidedgeItem(Properties p) {
        super(PLASMA, 3, -2.0f, p.rarity(net.minecraft.world.item.Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl) {
            Vec3 dir = player.getLookAngle().multiply(1, 0, 1).normalize();
            Vec3 start = player.position();
            Vec3 end = start.add(dir.scale(8));
            for (LivingEntity e : SiegeDamage.enemiesAround(sl, start.add(dir.scale(4)), 5)) {
                Vec3 rel = e.position().subtract(start);
                double along = rel.dot(dir);
                if (along < 0 || along > 8.5 || rel.subtract(dir.scale(along)).lengthSqr() > 3) continue;
                SiegeDamage.deal(e, player, player, 18f, Element.ARCANE, false);
            }
            Fx.line(sl, start.add(0, 1, 0), end.add(0, 1, 0), Fx.dust(0xB04AFF, 1.4f), 0.25);
            sl.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2, SoundSource.PLAYERS, 1f, 1.5f);
        }
        Vec3 d = player.getLookAngle().multiply(1, 0, 1).normalize();
        player.setDeltaMovement(d.x * 1.8, 0.15, d.z * 1.8);
        player.hurtMarked = true;
        player.getCooldowns().addCooldown(this, 50);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.level() instanceof ServerLevel sl) {
            sl.sendParticles(Fx.dust(0xB04AFF, 1.2f), target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.4, 0.3, 0);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Right-click: Phase Dash - slice through every enemy in an 8-block line.").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
