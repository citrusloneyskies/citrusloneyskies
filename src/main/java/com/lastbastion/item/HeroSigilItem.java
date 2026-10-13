package com.lastbastion.item;

import com.lastbastion.combat.Fx;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
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
        hero.moveTo(at.x, player.getY(), at.z, player.getYRot(), 0);
        hero.setOwnerId(player.getUUID());
        hero.finalizeSpawn(sl, sl.getCurrentDifficultyAt(player.blockPosition()), MobSpawnType.EVENT, null, null);
        var tag = stack.getTag();
        hero.setLevelAndXp(tag != null ? Math.max(1, tag.getInt("lbLevel")) : 1, tag != null ? tag.getInt("lbXp") : 0);
        hero.setHealth(hero.getMaxHealth());
        hero.setMode(HeroMode.GUARD);
        sl.addFreshEntity(hero);
        arrival(sl, hero);
        hero.onSummoned(sl);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.displayClientMessage(Component.literal(kind.title + " answers your call! (Sneak + right-click to change orders)")
                .withStyle(ChatFormatting.GOLD), false);
        return InteractionResultHolder.consume(stack);
    }

    /** Each hero gets a signature arrival. */
    private static void arrival(ServerLevel sl, HeroEntity hero) {
        Vec3 p = hero.position();
        float w = hero.getBbWidth();
        switch (hero.kind) {
            case ASHFANG -> {
                Vfx.spray(sl, FxStyle.SMOKE, p.add(0, 0.5, 0), 0x8A8F99, 0x3A3C40, 1.1f, 24, 0.2f, 0.2f);
                Vfx.spray(sl, FxStyle.EMBER, p.add(0, 0.6, 0), 0xFFB040, 0xFF4010, 0.15f, 18, 0.25f, 0.6f);
                sl.playSound(null, hero.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.NEUTRAL, 2f, 0.7f);
            }
            case SYLVA -> {
                Vfx.vortex(sl, p, 0x6AD07A, 0xE8FFB0, 1.2f, 2.6f, 30, hero);
                Vfx.spray(sl, FxStyle.LEAF, p.add(0, 1.2, 0), 0x6AD07A, 0xD8B040, 0.2f, 30, 0.3f, 0.3f);
                sl.playSound(null, hero.blockPosition(), SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.NEUTRAL, 1.5f, 0.8f);
            }
            case BASTION -> {
                Fx.shockwave(sl, p, 6, 0x8C8577);
                Vfx.spray(sl, FxStyle.EMBER, p.add(0, 1.6, 0), 0xFFC040, 0xFF8020, 0.25f, 16, 0.2f, 0.4f);
                sl.playSound(null, hero.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.NEUTRAL, 2f, 0.5f);
            }
            case ALDRIC -> {
                Vfx.pillar(sl, p, 0xFFE38A, 1.2f, 40, 30);
                Vfx.ring(sl, p.add(0, 0.05, 0), 0xFFE38A, 5, 18, 1);
                sl.playSound(null, hero.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 2f, 1.4f);
            }
            case MORGANA -> {
                Vfx.sigil(sl, p.add(0, 0.03, 0), 0xA070FF, 2.4f, 40, 4f);
                Vfx.arc(sl, p.add(0, 12, 0), p, 0xC8A0FF, 0.12f, 6, 1f);
                sl.playSound(null, hero.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.NEUTRAL, 1.5f, 1f);
            }
            case KAGE -> {
                Vfx.spray(sl, FxStyle.SMOKE, p.add(0, 1, 0), 0x2A2A34, 0x0A0A10, 1.2f, 30, 0.25f, 0);
                Vfx.slash(sl, p.add(0, 1.1, 0), hero.getYRot(), 10, 300, 1.8f, 0xD02030, 0xFFFFFF, 10, 0.3f);
                sl.playSound(null, hero.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 1.5f, 0.6f);
            }
            case AEGIS7 -> {
                Fx.explosion(sl, p, 3);
                Fx.shockwave(sl, p, 6, 0x5B6E7A);
                sl.playSound(null, hero.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 2f, 0.5f);
            }
            case ASTERION -> {
                Fx.shockwave(sl, p, 7, 0x9A5A2E);
                Vfx.spray(sl, FxStyle.SMOKE, p.add(0, 0.3, 0), 0xE8E0D8, 0xB8B0A8, 1.2f, 14, 0.2f, -1);
                sl.playSound(null, hero.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.NEUTRAL, 2.5f, 0.7f);
            }
            case ANUBIS -> {
                Vfx.vortex(sl, p, 0xE8C878, 0xB8985A, 2f, 4f, 40, hero);
                Vfx.sigil(sl, p.add(0, 0.03, 0), 0xE0B040, 3f, 40, -3f);
                Vfx.spray(sl, FxStyle.SMOKE, p.add(0, 0.5, 0), 0xE8C878, 0xB8985A, 1f, 26, 0.3f, 0.2f);
                sl.playSound(null, hero.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.NEUTRAL, 2f, 0.5f);
                sl.playSound(null, hero.blockPosition(), SoundEvents.EVOKER_PREPARE_WOLOLO, SoundSource.NEUTRAL, 1.2f, 0.6f);
            }
            case ZEUS -> {
                for (int i = 0; i < 4; i++) Vfx.lightning(sl, p.add(sl.random.nextGaussian() * 2.5, 0, sl.random.nextGaussian() * 2.5), 0xF4F06A, 1.6f);
                Vfx.lightning(sl, p, 0xFFFFFF, 2.4f);
                Fx.shockwave(sl, p, 8, 0xF4F06A);
                Vfx.screenFlash(sl, p, 0xFFFFE0, 0.35f, 40, 8);
                sl.playSound(null, hero.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 4f, 0.7f);
            }
        }
        Vfx.ring(sl, p.add(0, 0.05, 0), hero.kind.color, 2 + w, 14, 0.6f);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return kind.ordinal() >= 7;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(kind.desc).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal(kind.moves).withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.literal("HP " + (int) kind.hp + "  Damage " + (int) kind.damage + "  Role: " + kind.role.name().toLowerCase()).withStyle(ChatFormatting.GREEN));
        var tag = stack.getTag();
        if (tag != null && tag.getInt("lbLevel") > 1) tip.add(Component.literal("Veteran: Level " + tag.getInt("lbLevel")).withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("Right-click near the core to summon.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
