package com.lastbastion.item;

import com.lastbastion.combat.Fx;
import com.lastbastion.config.SiegeConfig;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.entity.turret.TurretKind;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Deploys a level 1 turret on the clicked block. Must be within build range of the Siege Core. */
public class TurretKitItem extends Item {
    public final TurretKind kind;

    public TurretKitItem(Properties p, TurretKind kind) {
        super(p);
        this.kind = kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        BlockPos core = SiegeManager.corePos(sl);
        if (core == null) {
            fail(player, "Place a Siege Core first.");
            return InteractionResult.FAIL;
        }
        int r = SiegeConfig.BUILD_RADIUS.get();
        if (!core.closerThan(pos, r)) {
            fail(player, "Turrets must be within " + r + " blocks of the core.");
            return InteractionResult.FAIL;
        }
        Vec3 at = Vec3.atBottomCenterOf(pos);
        if (!level.getEntitiesOfClass(TurretEntity.class, new AABB(at, at).inflate(1.2, 1, 1.2)).isEmpty()) {
            fail(player, "Too close to another turret.");
            return InteractionResult.FAIL;
        }
        TurretEntity t = kind.type().get().create(sl);
        if (t == null) return InteractionResult.FAIL;
        t.moveTo(at.x, at.y, at.z, 0, 0);
        if (!level.noCollision(t)) {
            fail(player, "Not enough space here.");
            return InteractionResult.FAIL;
        }
        t.setOwnerId(player != null ? player.getUUID() : null);
        t.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        t.setTurretLevel(1);
        t.setHealth(t.getMaxHealth());
        level.addFreshEntity(t);
        Fx.skyBeam(sl, at, kind.color, 8, 0.6);
        Fx.ring(sl, at.add(0, 0.1, 0), 1.2, Fx.dust(kind.color, 1.2f), 16);
        level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.7f, 1.3f);
        if (player == null || !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    private static void fail(@Nullable Player p, String msg) {
        if (p != null) p.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.RED), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        try {
        tip.add(Component.literal(kind.desc).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Damage " + kind.damage + "  Range " + kind.range + "  Element " + kind.element.name()).withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.literal("Lv3: " + kind.perk3).withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("Lv5: " + kind.perk5).withStyle(ChatFormatting.LIGHT_PURPLE));
        tip.add(Component.literal("Right-click a block near the core to deploy.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("TurretKitItem.appendHoverText", lbCrash);
        }
    }
}
