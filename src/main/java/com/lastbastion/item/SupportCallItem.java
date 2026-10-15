package com.lastbastion.item;

import com.lastbastion.combat.Fx;
import com.lastbastion.entity.misc.DropPodEntity;
import com.lastbastion.entity.misc.SentryDroneEntity;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Call-ins from the core: orbital laser, carpet bombing, a sentry drone and the Titan Exosuit drop. */
public class SupportCallItem extends Item {
    public enum Kind {
        ORBITAL("Orbital Strike Remote", "Paint a target: 3 seconds later a satellite lance deals 120 damage in an 7-block radius."),
        AIRSTRIKE("Airstrike Flare", "Bombers carpet-bomb a 20-block line in the direction you face."),
        DRONE("Sentry Drone", "Deploys a hovering gun-drone that follows you and shreds zombies for 3 minutes."),
        EXOSUIT("Titan Exosuit Beacon", "Calls down a drop pod carrying a Titan Exosuit. Climb in for 90 seconds of rotary cannons, missiles and jump-jets.");

        public final String title, desc;

        Kind(String t, String d) {
            title = t;
            desc = d;
        }
    }

    public final Kind kind;

    public SupportCallItem(Properties p, Kind kind) {
        super(p.stacksTo(kind == Kind.EXOSUIT ? 1 : 4).rarity(kind == Kind.EXOSUIT || kind == Kind.ORBITAL ? Rarity.EPIC : Rarity.RARE));
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl)) return InteractionResultHolder.success(stack);
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(96)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 at = hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
        switch (kind) {
            case ORBITAL -> {
                if (at == null) return InteractionResultHolder.fail(stack);
                HazardZone.spawn(sl, HazardZone.Kind.ORBITAL, at, 7f, 1, 1f, player, false).fuse(60);
                sl.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1f, 1.8f);
                player.displayClientMessage(Component.literal("Orbital lance inbound - 3 seconds!").withStyle(ChatFormatting.AQUA), true);
            }
            case AIRSTRIKE -> {
                if (at == null) return InteractionResultHolder.fail(stack);
                HazardZone.spawn(sl, HazardZone.Kind.AIRSTRIKE, at, 3f, 1, 1f, player, false)
                        .heading(player.getLookAngle().multiply(1, 0, 1).normalize()).fuse(50);
                sl.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1f, 0.5f);
                player.displayClientMessage(Component.literal("Bombers on approach!").withStyle(ChatFormatting.GOLD), true);
            }
            case DRONE -> {
                SentryDroneEntity d = ModEntities.DRONE.get().create(sl);
                if (d == null) return InteractionResultHolder.fail(stack);
                d.moveTo(player.getX(), player.getY() + 2, player.getZ());
                d.setOwnerId(player.getUUID());
                sl.addFreshEntity(d);
                Fx.ring(sl, d.position(), 1, Fx.dust(0x7FE8FF, 1f), 12);
                sl.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 2f);
            }
            case EXOSUIT -> {
                Vec3 target = at != null && at.distanceTo(player.position()) < 24 ? at : player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(4));
                DropPodEntity pod = ModEntities.DROP_POD.get().create(sl);
                if (pod == null) return InteractionResultHolder.fail(stack);
                pod.moveTo(target.x, target.y + 80, target.z);
                pod.setOwnerId(player.getUUID());
                sl.addFreshEntity(pod);
                sl.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, SoundSource.PLAYERS, 2f, 0.4f);
                player.displayClientMessage(Component.literal("Titan Exosuit inbound! Stand clear of the landing zone.").withStyle(ChatFormatting.GOLD), true);
            }
        }
        player.getCooldowns().addCooldown(this, 40);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        try {
        tip.add(Component.literal(kind.desc).withStyle(ChatFormatting.GRAY));
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("SupportCallItem.appendHoverText", lbCrash);
        }
    }
}
