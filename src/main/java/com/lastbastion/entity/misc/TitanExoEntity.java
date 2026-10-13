package com.lastbastion.entity.misc;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeAlly;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.item.Hitscan;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The Titan Exosuit: a 3.5-block power armour frame the player climbs into.
 * <ul>
 *   <li>WASD to stomp around, JUMP for jump-jets (fuel regenerates on the ground).</li>
 *   <li>Hold ATTACK for twin rotary cannons aimed where you look.</li>
 *   <li>USE to launch a 6-missile homing salvo (4s cooldown).</li>
 *   <li>Absorbs all damage aimed at the pilot. Lasts 90 seconds, then self-destructs safely.</li>
 * </ul>
 */
public class TitanExoEntity extends Mob implements SiegeAlly {
    private static final EntityDataAccessor<Integer> TIME = SynchedEntityData.defineId(TitanExoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FIRING = SynchedEntityData.defineId(TitanExoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(TitanExoEntity.class, EntityDataSerializers.INT);
    public static final int DURATION = 20 * 90;
    public static final int MAX_FUEL = 60;

    @Nullable
    private UUID owner;
    private boolean activated;
    public boolean fireInput, missileInput, boostInput;
    private int missileCooldown;
    private int gunTick;

    public TitanExoEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 500).add(Attributes.ARMOR, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(TIME, DURATION);
        entityData.define(FIRING, false);
        entityData.define(FUEL, MAX_FUEL);
    }

    @Override
    protected void registerGoals() {
    }

    public void setOwnerId(@Nullable UUID id) {
        owner = id;
    }

    @Override
    @Nullable
    public UUID getOwnerId() {
        return owner;
    }

    public int timeLeft() {
        return entityData.get(TIME);
    }

    public int fuel() {
        return entityData.get(FUEL);
    }

    public boolean firing() {
        return entityData.get(FIRING);
    }

    // ------------------------------------------------------------------ riding

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isVehicle()) return InteractionResult.PASS;
        if (owner != null && !owner.equals(player.getUUID()) && !player.hasPermissions(2)) {
            if (!level().isClientSide) player.displayClientMessage(Component.literal("Biometric lock: this suit belongs to another pilot."), true);
            return InteractionResult.FAIL;
        }
        if (!level().isClientSide) {
            player.startRiding(this);
            activated = true;
            player.displayClientMessage(Component.literal("TITAN ONLINE - Attack: cannons | Use: missiles | Jump: jets | Sneak: eject")
                    .withStyle(ChatFormatting.GOLD), true);
            playSound(SoundEvents.BEACON_ACTIVATE, 1.5f, 1.4f);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        setYRot(player.getYRot());
        yRotO = getYRot();
        setXRot(player.getXRot() * 0.5f);
        setRot(getYRot(), getXRot());
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        if (boostInput && fuel() > 0) {
            Vec3 m = getDeltaMovement();
            setDeltaMovement(m.x, Math.min(0.55, m.y + 0.16), m.z);
            if (level().isClientSide) {
                for (int i = 0; i < 3; i++) {
                    level().addParticle(ParticleTypes.FLAME, getX() + (random.nextDouble() - 0.5) * 1.2, getY() + 0.8, getZ() + (random.nextDouble() - 0.5) * 1.2, 0, -0.3, 0);
                }
            }
        }
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        return new Vec3(player.xxa * 0.6, 0, player.zza > 0 ? player.zza : player.zza * 0.5);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    public double getPassengersRidingOffset() {
        return getBbHeight() * 0.62;
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        if (d > 3 && level() instanceof ServerLevel sl) {
            Fx.shockwave(sl, position(), Math.min(6, d * 0.6), 0xFF9030);
            for (LivingEntity e : SiegeDamage.enemiesAround(sl, position(), Math.min(6, d * 0.6))) {
                SiegeDamage.deal(e, this, getControllingPassenger(), d * 3, Element.PHYSICAL, false);
                SiegeDamage.push(e, position(), 1, 0.5);
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        ServerLevel sl = (ServerLevel) level();
        if (onGround() && fuel() < MAX_FUEL && tickCount % 2 == 0) entityData.set(FUEL, fuel() + 1);
        if (boostInput && fuel() > 0 && isVehicle()) entityData.set(FUEL, fuel() - 1);
        if (activated) {
            int t = timeLeft() - 1;
            entityData.set(TIME, t);
            if (t == 200 || t == 100) {
                if (getControllingPassenger() instanceof Player p) p.displayClientMessage(Component.literal("Exosuit power: " + t / 20 + "s").withStyle(ChatFormatting.RED), true);
            }
            if (t <= 0) {
                shutdown(sl);
                return;
            }
        } else if (tickCount > 20 * 120) {
            shutdown(sl);
            return;
        }
        Player pilot = getControllingPassenger() instanceof Player p ? p : null;
        if (pilot == null) {
            fireInput = missileInput = boostInput = false;
        }
        entityData.set(FIRING, fireInput);
        if (missileCooldown > 0) missileCooldown--;
        if (pilot != null && fireInput && ++gunTick % 2 == 0) {
            Vec3 look = pilot.getLookAngle();
            boolean left = gunTick % 4 == 0;
            Vec3 side = Vec3.directionFromRotation(0, getYRot() + (left ? -90 : 90)).scale(1.25);
            Vec3 muzzle = position().add(0, getBbHeight() * 0.62, 0).add(side).add(Vec3.directionFromRotation(0, getYRot()).scale(1.2));
            Vec3 aimPoint = pilot.getEyePosition().add(look.scale(40));
            Vec3 dir = Hitscan.spreadDir(this, aimPoint.subtract(muzzle).normalize(), 0.025);
            Hitscan.fire(sl, this, muzzle, dir, 48, 7f, Element.PHYSICAL, Fx.dust(0xFFD070, 0.6f));
            sl.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 1, 0.05, 0.05, 0.05, 0.01);
            if (gunTick % 4 == 0) sl.playSound(null, blockPosition(), SoundEvents.CROSSBOW_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.5f);
        }
        if (pilot != null && missileInput && missileCooldown <= 0) {
            missileCooldown = 80;
            Vec3 look = pilot.getLookAngle();
            Vec3 focus = pilot.getEyePosition().add(look.scale(20));
            List<LivingEntity> targets = SiegeDamage.enemiesAround(sl, focus, 14);
            targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(focus)));
            for (int i = 0; i < 6; i++) {
                Vec3 pod = position().add(0, getBbHeight(), 0).add(Vec3.directionFromRotation(0, getYRot() + 180).scale(0.6));
                SiegeProjectile m = SiegeProjectile.create(sl, SiegeProjectile.ProjKind.MISSILE, this, pod).payload(40f, Element.EXPLOSIVE).area(3f);
                m.setDeltaMovement(look.scale(0.6).add(random.nextGaussian() * 0.2, 0.6, random.nextGaussian() * 0.2));
                if (!targets.isEmpty()) m.homing(targets.get(i % targets.size()), 0.14f);
                m.life = 100;
                m.fire(sl);
            }
            sl.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, net.minecraft.sounds.SoundSource.PLAYERS, 2f, 0.6f);
        }
        if (getDeltaMovement().horizontalDistanceSqr() > 0.003 && onGround() && tickCount % 9 == 0) {
            sl.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_STEP, net.minecraft.sounds.SoundSource.NEUTRAL, 1.2f, 0.6f);
        }
    }

    private void shutdown(ServerLevel sl) {
        ejectPassengers();
        Fx.explosion(sl, position().add(0, 1, 0), 2);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 40, 0.8, 1, 0.8, 0.05);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl) shutdown(sl);
        super.die(source);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity e) {
        return getPassengers().isEmpty() && e instanceof Player;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("lbOwner", owner);
        tag.putInt("lbTime", timeLeft());
        tag.putBoolean("lbActive", activated);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("lbOwner")) owner = tag.getUUID("lbOwner");
        entityData.set(TIME, tag.contains("lbTime") ? tag.getInt("lbTime") : DURATION);
        activated = tag.getBoolean("lbActive");
    }
}
