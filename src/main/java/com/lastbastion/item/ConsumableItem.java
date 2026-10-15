package com.lastbastion.item;

import com.lastbastion.combat.Fx;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class ConsumableItem extends Item {
    public enum Kind {
        BANDAGE("Field Bandage", 16, "Heals 6 HP and stops Bleeding."),
        MEDKIT("Combat Medkit", 32, "Heals 20 HP, cleanses ailments, grants Regeneration II."),
        STIM("Berserker Stim", 10, "30s of Speed II, Strength II and Rally. Hurts a little."),
        CORE_REPAIR("Core Repair Kit", 40, "Restores 25% of the Siege Core's health. Use within 12 blocks of the core.");

        public final String title;
        public final int useTicks;
        public final String desc;

        Kind(String t, int useTicks, String d) {
            title = t;
            this.useTicks = useTicks;
            desc = d;
        }
    }

    public final Kind kind;

    public ConsumableItem(Properties p, Kind kind) {
        super(p.stacksTo(kind == Kind.CORE_REPAIR ? 4 : 16));
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (kind == Kind.CORE_REPAIR && level instanceof ServerLevel sl) {
            BlockPos core = SiegeManager.corePos(sl);
            if (core == null || !core.closerToCenterThan(player.position(), 12)) {
                player.displayClientMessage(Component.literal("Get within 12 blocks of the Siege Core.").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return kind.useTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return kind == Kind.STIM ? UseAnim.DRINK : UseAnim.BOW;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (level instanceof ServerLevel sl) {
            switch (kind) {
                case BANDAGE -> {
                    user.heal(6);
                    user.removeEffect(ModEffects.BLEEDING.get());
                }
                case MEDKIT -> {
                    user.heal(20);
                    user.getActiveEffects().stream().filter(e -> !e.getEffect().isBeneficial()).map(MobEffectInstance::getEffect)
                            .toList().forEach(user::removeEffect);
                    user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                    sl.sendParticles(ParticleTypes.HEART, user.getX(), user.getY() + 1.5, user.getZ(), 6, 0.4, 0.3, 0.4, 0);
                }
                case STIM -> {
                    user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
                    user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
                    user.addEffect(new MobEffectInstance(ModEffects.RALLY.get(), 600, 0));
                    user.hurt(user.damageSources().magic(), 2f);
                }
                case CORE_REPAIR -> {
                    BlockPos core = SiegeManager.corePos(sl);
                    if (core != null) {
                        SiegeManager.healCore(sl, SiegeManager.data(sl).maxHp() * 0.25f);
                        Fx.line(sl, user.position().add(0, 1, 0), net.minecraft.world.phys.Vec3.atCenterOf(core), Fx.dust(0x7CFF9B, 1f), 0.3);
                        Fx.spiral(sl, net.minecraft.world.phys.Vec3.atBottomCenterOf(core), 1.2, 2.5, Fx.dust(0x7CFF9B, 1.2f), 3, 0);
                    }
                }
            }
            sl.playSound(null, user.blockPosition(), kind == Kind.STIM ? SoundEvents.GENERIC_DRINK : SoundEvents.ARMOR_EQUIP_LEATHER,
                    SoundSource.PLAYERS, 1f, 1f);
        }
        if (!(user instanceof Player p) || !p.getAbilities().instabuild) stack.shrink(1);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        try {
        tip.add(Component.literal(kind.desc).withStyle(ChatFormatting.GRAY));
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("ConsumableItem.appendHoverText", lbCrash);
        }
    }
}
