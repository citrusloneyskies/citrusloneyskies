package com.lastbastion.entity.boss;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.SiegeEnemy;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/** Floating soul crystal that shields Varkhul. Must be destroyed to break his ward. */
public class PhylacteryEntity extends Mob implements SiegeEnemy, Enemy {
    public PhylacteryEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 80).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public void bind(VarkhulBoss boss, float hp) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        setHealth(hp);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        try {
        super.tick();
        setDeltaMovement(0, Math.sin(tickCount * 0.1) * 0.02, 0);
        if (level() instanceof ServerLevel sl && tickCount % 5 == 0) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.6, getZ(), 2, 0.3, 0.4, 0.3, 0.01);
        }
        if (!level().isClientSide && tickCount > 20 * 120) discard();
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("PhylacteryEntity.tick", lbCrash);
        }
    }

    @Override
    public float elementMultiplier(Element e) {
        return e == Element.HOLY ? 2f : e == Element.ICE ? 0.5f : 1f;
    }

    @Override
    public int creditValue() {
        return 40;
    }

    @Override
    public boolean isWaveMob() {
        return false;
    }

    @Override
    public boolean isHighPriority() {
        return true;
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
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GLASS_BREAK;
    }
}
