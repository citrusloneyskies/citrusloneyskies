package com.lastbastion.item;

import com.lastbastion.combat.Fx;
import com.lastbastion.config.SiegeConfig;
import com.lastbastion.entity.hero.HeroEntity;
import com.lastbastion.entity.hero.HeroKind;
import com.lastbastion.entity.hero.HeroMode;
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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Summons a hero bound to the user. Revived heroes carry their level and XP on the sigil. */
public class HeroSigilItem extends Item {
    public final HeroKind kind;

    public HeroSigilItem(Properties p, HeroKind kind) {
        super(p.rarity(kind.ordinal() >= 7 ? Rarity.EPIC : kind.ordinal() >= 4 ? Rarity.RARE : Rarity.UNCOMMON));
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl)) return InteractionResultHolder.success(stack);
        BlockPos core = SiegeManager.corePos(sl);
        if (core == null || !core.closerToCenterThan(player.position(), SiegeConfig.BUILD_RADIUS.get() + 16)) {
            player.displayClientMessage(Component.literal("Heroes answer only near an active Siege Core.").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        HeroEntity hero = kind.type().get().create(sl);
        if (hero == null) return InteractionResultHolder.fail(stack);
        Vec3 at = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(2.5));
        hero.moveTo(at.x, player.getY() + (kind.flying ? 3 : 0), at.z, player.getYRot(), 0);
        hero.setOwnerId(player.getUUID());
        hero.finalizeSpawn(sl, sl.getCurrentDifficultyAt(player.blockPosition()), MobSpawnType.EVENT, null, null);
        var tag = stack.getTag();
        hero.setLevelAndXp(tag != null ? Math.max(1, tag.getInt("lbLevel")) : 1, tag != null ? tag.getInt("lbXp") : 0);
        hero.setHealth(hero.getMaxHealth());
        hero.setMode(HeroMode.GUARD);
        sl.addFreshEntity(hero);
        arrival(sl, hero);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.displayClientMessage(Component.literal(kind.title + " answers your call! (Sneak + right-click to change orders)")
                .withStyle(ChatFormatting.GOLD), false);
        return InteractionResultHolder.consume(stack);
    }

    /** Each hero gets a signature arrival. */
    private static void arrival(ServerLevel sl, HeroEntity hero) {
        Vec3 p = hero.position();
        switch (hero.kind) {
            case ALDRIC -> {
                Fx.skyBeam(sl, p, 0xFFE38A, 40, 1.4);
                sl.playSound(null, hero.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 2f, 1.4f);
            }
            case ZEUS -> {
                for (int i = 0; i < 4; i++) Fx.bolt(sl, p.add(sl.random.nextGaussian() * 2, 40, sl.random.nextGaussian() * 2), p, 0xFFFFFF, 2f);
                Fx.shockwave(sl, p, 8, 0xF4F06A);
                sl.playSound(null, hero.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 4f, 0.7f);
            }
            case IGNIS -> {
                Fx.spiral(sl, p, 2, 6, ParticleTypes.FLAME, 5, 0);
                sl.playSound(null, hero.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.NEUTRAL, 2f, 0.6f);
            }
            case VORTHAX -> {
                Fx.shockwave(sl, p, 10, 0xB02020);
                sl.playSound(null, hero.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.NEUTRAL, 3f, 0.8f);
            }
            case AEGIS7 -> {
                Fx.explosion(sl, p, 3);
                Fx.shockwave(sl, p, 5, 0x5B6E7A);
            }
            case KAGE -> sl.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y + 1, p.z, 60, 0.5, 1, 0.5, 0.05);
            case MORGANA -> Fx.spiral(sl, p, 1.5, 3, ParticleTypes.WITCH, 4, 0);
            default -> {
                Fx.skyBeam(sl, p, hero.kind.color, 12, 0.8);
                sl.playSound(null, hero.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.NEUTRAL, 1f, 1.2f);
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return kind.ordinal() >= 7;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(kind.desc).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal(kind.moves).withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.literal("HP " + (int) kind.hp + "  Damage " + (int) kind.damage + (kind.flying ? "  Flying" : "")).withStyle(ChatFormatting.GREEN));
        var tag = stack.getTag();
        if (tag != null && tag.getInt("lbLevel") > 1) tip.add(Component.literal("Veteran: Level " + tag.getInt("lbLevel")).withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("Right-click near the core to summon.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
