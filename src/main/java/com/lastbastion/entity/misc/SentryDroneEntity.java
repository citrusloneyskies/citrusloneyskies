package com.lastbastion.entity.misc;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeAlly;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.item.Hitscan;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/** Hovering gun drone that orbits its owner's shoulder and fires twin pulse lasers at nearby zombies. */
public class SentryDroneEntity extends Mob implements SiegeAlly {
    public static final int LIFETIME = 20 * 180;
    @Nullable
    private UUID owner;
    @Nullable
    private LivingEntity target;

    public SentryDroneEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.ARMOR, 8);
    }

    public void setOwnerId(@Nullable UUID id) {
        owner = id;
    }

    @Override
    @Nullable
    public UUID getOwnerId() {
        return owner;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        try {
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY() - 0.1, getZ(), 0, -0.05, 0);
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        if (tickCount > LIFETIME) {
            sl.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.02);
            discard();
            return;
        }
        Player p = owner != null ? sl.getPlayerByUUID(owner) : null;
        Vec3 anchor = p != null ? p.position().add(0, p.getBbHeight() + 0.6, 0)
                .add(Vec3.directionFromRotation(0, p.getYRot() + 120 + (float) Math.sin(tickCount * 0.05) * 20).scale(1.3)) : position();
        if (p != null && distanceToSqr(p) > 30 * 30) teleportTo(anchor.x, anchor.y, anchor.z);
        Vec3 v = anchor.subtract(position());
        setDeltaMovement(getDeltaMovement().scale(0.6).add(v.scale(0.12)));
        if (tickCount % 10 == 0) {
            target = null;
            double best = 18 * 18;
            for (LivingEntity e : SiegeDamage.enemiesAround(sl, position(), 18)) {
                if (e instanceof SiegeEnemy se && se.isUntargetable()) continue;
                double d = e.distanceToSqr(this);
                if (d < best && hasLineOfSight(e)) {
                    best = d;
                    target = e;
                }
            }
        }
        if (target != null && target.isAlive()) {
            getLookControl().setLookAt(target, 60, 60);
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
            setYRot((float) (Math.atan2(to.z - getZ(), to.x - getX()) * 180 / Math.PI) - 90f);
            yBodyRot = getYRot();
            if (tickCount % 6 == 0) {
                Hitscan.fire(sl, this, position(), to.subtract(position()).normalize(), 20, 4f, Element.ARCANE, Fx.dust(0x7FE8FF, 0.6f));
                sl.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, net.minecraft.sounds.SoundSource.NEUTRAL, 0.3f, 2f);
            }
        }
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("SentryDroneEntity.tick", lbCrash);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("lbOwner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("lbOwner")) owner = tag.getUUID("lbOwner");
    }
}
