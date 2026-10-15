package com.lastbastion.entity.misc;

import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.item.ConsumableItem;
import com.lastbastion.item.GrenadeItem;
import com.lastbastion.item.SupportCallItem;
import com.lastbastion.registry.ModItems;
import com.lastbastion.siege.SiegeData;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * War-event supply drop: falls under a parachute, then waits to be claimed (right-click or hit it) for
 * 90 seconds before its scuttle charge goes off. Pays credits and rolls gear that improves with the wave.
 */
public class SupplyCrateEntity extends Entity {
    public static final int LIFETIME = 20 * 90;
    private static final EntityDataAccessor<Boolean> LANDED = SynchedEntityData.defineId(SupplyCrateEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(SupplyCrateEntity.class, EntityDataSerializers.INT);
    private int wave = 1;
    private boolean claimed;

    public SupplyCrateEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void setWave(int wave) {
        this.wave = Math.max(1, wave);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(LANDED, false);
        entityData.define(LIFE, LIFETIME);
    }

    public boolean landed() {
        return entityData.get(LANDED);
    }

    public int life() {
        return entityData.get(LIFE);
    }

    public int secondsLeft() {
        return (life() + 19) / 20;
    }

    @Override
    public void tick() {
        try {
        super.tick();
        if (!landed()) {
            Vec3 v = getDeltaMovement();
            double sway = Mth.sin(tickCount * 0.08f) * 0.02;
            setDeltaMovement(sway, Math.max(-0.26, v.y - 0.03), sway * 0.6);
            move(MoverType.SELF, getDeltaMovement());
            if (level().isClientSide && tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.FIREWORK, getX(), getY() + 0.2, getZ(), 0, -0.05, 0);
            }
            if (onGround() || isInWater()) land();
            if (!level().isClientSide && tickCount > 20 * 60) land();
            return;
        }
        if (!onGround() && !isInWater()) {
            setDeltaMovement(0, Math.max(-0.6, getDeltaMovement().y - 0.06), 0);
            move(MoverType.SELF, getDeltaMovement());
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        if (!(level() instanceof ServerLevel sl)) {
            if (tickCount % 4 == 0) level().addParticle(ParticleTypes.HAPPY_VILLAGER, getX() + (random.nextDouble() - 0.5), getY() + 1.1, getZ() + (random.nextDouble() - 0.5), 0, 0.05, 0);
            return;
        }
        int life = life() - 1;
        entityData.set(LIFE, life);
        if (life % 40 == 0) Vfx.pillar(sl, position(), 0x7CFF9B, 0.18f, 26, 30);
        if (life <= 200 && life % (life <= 60 ? 5 : 20) == 0) {
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.NEUTRAL, 1.2f, life <= 60 ? 2f : 1.4f);
        }
        if (life <= 0) scuttle(sl);
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("SupplyCrateEntity.tick", lbCrash);
        }
    }

    private void land() {
        entityData.set(LANDED, true);
        setDeltaMovement(Vec3.ZERO);
        if (level() instanceof ServerLevel sl) {
            Vfx.spray(sl, FxStyle.SMOKE, position().add(0, 0.3, 0), 0xC8C0B0, 0x8A8270, 0.9f, 14, 0.2f, 0.1f);
            Vfx.ring(sl, position().add(0, 0.05, 0), 0x7CFF9B, 3f, 14, 0.4f);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.8f, 0.7f);
        }
    }

    private void scuttle(ServerLevel sl) {
        Vfx.flash(sl, position().add(0, 0.6, 0), 0xFFB040, 2.2f, 8);
        Vfx.spray(sl, FxStyle.DEBRIS, position().add(0, 0.5, 0), 0x8A6A40, 0x4A3820, 0.25f, 20, 0.5f, 0.8f);
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 1f, 1.3f);
        SiegeData d = SiegeManager.data(sl);
        SiegeManager.broadcast(sl, d, Component.literal("A supply drop was lost.").withStyle(ChatFormatting.GRAY));
        discard();
    }

    /** Hand the loot to {@code p}. */
    public void claim(ServerPlayer p) {
        if (claimed || !isAlive() || !(level() instanceof ServerLevel sl)) return;
        claimed = true;
        com.lastbastion.siege.Milestones.award(p, com.lastbastion.siege.Milestones.SUPPLY);
        SiegeData d = SiegeManager.data(sl);
        int credits = SiegeManager.award(sl, d, p.getUUID(), 60 + 15 * wave, position().add(0, 1.2, 0), false);
        List<ItemStack> loot = roll(sl);
        StringBuilder names = new StringBuilder();
        for (ItemStack s : loot) {
            if (names.length() > 0) names.append(", ");
            names.append(s.getCount() > 1 ? s.getCount() + "x " : "").append(s.getHoverName().getString());
            if (!p.getInventory().add(s)) p.drop(s, false);
        }
        Vfx.spray(sl, FxStyle.STAR, position().add(0, 0.8, 0), 0xFFFFFF, 0x7CFF9B, 0.3f, 24, 0.3f, 0.8f);
        Vfx.ring(sl, position().add(0, 0.1, 0), 0x7CFF9B, 2.5f, 12, 0.6f);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.CHEST_OPEN, SoundSource.NEUTRAL, 1f, 0.8f);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6f, 1.6f);
        SiegeManager.broadcast(sl, d, Component.literal(p.getGameProfile().getName() + " claimed the supply drop: ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal("+" + credits + " credits" + (names.length() > 0 ? ", " + names : "")).withStyle(ChatFormatting.YELLOW)));
        SiegeManager.syncHud(sl, d);
        discard();
    }

    /** Two to three rolls on a loot table that improves with the wave. */
    private List<ItemStack> roll(ServerLevel sl) {
        List<ItemStack> out = new ArrayList<>();
        int rolls = 2 + (wave >= 12 ? 1 : 0);
        for (int i = 0; i < rolls; i++) {
            float r = sl.random.nextFloat();
            ItemStack s;
            if (wave >= 10 && r < 0.08f) {
                s = new ItemStack(ModItems.WARFORGED_TOKEN.get());
            } else if (wave >= 8 && r < 0.16f) {
                SupportCallItem.Kind k = SupportCallItem.Kind.values()[sl.random.nextInt(3)];
                s = new ItemStack(ModItems.SUPPORT.get(k).get());
            } else if (r < 0.45f) {
                GrenadeItem.Kind k = GrenadeItem.Kind.values()[sl.random.nextInt(GrenadeItem.Kind.values().length)];
                s = new ItemStack(ModItems.GRENADES.get(k).get(), k == GrenadeItem.Kind.SINGULARITY ? 1 : 2);
            } else if (r < 0.8f) {
                ConsumableItem.Kind k = ConsumableItem.Kind.values()[sl.random.nextInt(ConsumableItem.Kind.values().length)];
                s = new ItemStack(ModItems.CONSUMABLES.get(k).get(), k == ConsumableItem.Kind.BANDAGE ? 3 : 1);
            } else {
                s = new ItemStack(Items.GOLDEN_APPLE, 2);
            }
            out.add(s);
        }
        return out;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!landed()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) claim(sp);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        if (landed() && source.getEntity() instanceof ServerPlayer sp && source.getDirectEntity() == sp) claim(sp);
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 192 * 192;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        wave = Math.max(1, tag.getInt("wave"));
        entityData.set(LANDED, tag.getBoolean("landed"));
        entityData.set(LIFE, tag.contains("life") ? tag.getInt("life") : LIFETIME);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("wave", wave);
        tag.putBoolean("landed", landed());
        tag.putInt("life", life());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
