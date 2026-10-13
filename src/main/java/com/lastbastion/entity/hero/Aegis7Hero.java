package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.projectile.SiegeProjectile.ProjKind;
import com.lastbastion.entity.zombie.RunnerZombie;
import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** AEGIS-7 War Mech: rotary cannons, a homing missile swarm, seismic stomps and an overdrive mode. */
public class Aegis7Hero extends HeroEntity {
    public static final int MISSILES = 4, STOMP = 5, OVERDRIVE = 6;
    private int overdrive;

    public Aegis7Hero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.AEGIS7);
    }

    public boolean overdriven() {
        return flag();
    }

    @Override
    protected boolean isRanged() {
        return true;
    }

    @Override
    protected double attackRange() {
        return 20;
    }

    @Override
    protected double preferredRange() {
        return 12;
    }

    @Override
    protected int attackWindup() {
        return 1;
    }

    @Override
    protected int attackInterval() {
        return overdriven() ? 1 : 3;
    }

    @Override
    protected int comboLength() {
        return 2;
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        Vec3 side = Vec3.directionFromRotation(0, getYRot() + (combo == 0 ? 90 : -90)).scale(1.3);
        Vec3 muzzle = position().add(0, getBbHeight() * 0.6, 0).add(side).add(Vec3.directionFromRotation(0, getYRot()).scale(1.6));
        Vec3 aim = t.position().add(0, t.getBbHeight() * 0.5, 0).add(random.nextGaussian() * 0.2, random.nextGaussian() * 0.2, random.nextGaussian() * 0.2);
        Fx.line(level, muzzle, aim, Fx.dust(0xFFD070, 0.5f), 0.7);
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 1, 0.05, 0.05, 0.05, 0.01);
        SiegeDamage.deal(t, this, this, dmg(1f), Element.PHYSICAL, false);
        if (tickCount % 4 == 0) playSound(SoundEvents.CROSSBOW_SHOOT, 0.5f, 2f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (ready(STOMP) && SiegeDamage.enemiesAround(level, position(), 4.5).size() >= 2) return STOMP;
        if (ready(OVERDRIVE) && (getHealth() < getMaxHealth() * 0.5f || (t instanceof SiegeEnemy se && se.isHighPriority()))) return OVERDRIVE;
        if (ready(MISSILES) && SiegeDamage.enemiesAround(level, position(), 20).size() >= 3) return MISSILES;
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case MISSILES -> {
                if (t == 10) {
                    List<LivingEntity> targets = SiegeDamage.enemiesAround(level, position(), 22);
                    targets.sort(Comparator.comparingDouble(e -> -e.getHealth()));
                    for (int i = 0; i < 8 && !targets.isEmpty(); i++) {
                        LivingEntity e = targets.get(i % targets.size());
                        Vec3 pod = position().add(0, getBbHeight() + 0.3, 0).add(Vec3.directionFromRotation(0, getYRot() + 180).scale(0.8));
                        SiegeProjectile m = SiegeProjectile.create(level, ProjKind.MISSILE, this, pod).payload(dmg(3f), Element.EXPLOSIVE).area(2.5f);
                        m.setDeltaMovement(random.nextGaussian() * 0.25, 0.8 + i * 0.03, random.nextGaussian() * 0.25);
                        m.homing(e, 0.12f);
                        m.life = 120;
                        m.fire(level);
                    }
                    playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 2f, 0.6f);
                }
                if (t >= 24) {
                    cooldown(MISSILES, 220);
                    return true;
                }
            }
            case STOMP -> {
                if (t == 16) {
                    Fx.shockwave(level, position(), 5, 0x5B6E7A);
                    playSound(SoundEvents.ANVIL_LAND, 1.5f, 0.4f);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 5)) {
                        SiegeDamage.deal(e, this, this, dmg(3f), Element.PHYSICAL, false);
                        SiegeDamage.push(e, position(), 1.4, 0.6);
                    }
                }
                if (t >= 28) {
                    cooldown(STOMP, 140);
                    return true;
                }
            }
            case OVERDRIVE -> {
                if (t == 10) {
                    overdrive = 200;
                    setFlag(true);
                    playSound(SoundEvents.BEACON_POWER_SELECT, 2f, 0.5f);
                    level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 2, getZ(), 40, 1, 1.5, 1, 0.05);
                }
                if (t >= 16) {
                    cooldown(OVERDRIVE, 600);
                    return true;
                }
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void passiveTick(ServerLevel level) {
        if (overdrive > 0 && --overdrive == 0) setFlag(false);
        if (overdriven() && tickCount % 3 == 0) level.sendParticles(ParticleTypes.FLAME, getX(), getY() + getBbHeight() * 0.7, getZ(), 2, 0.6, 0.4, 0.6, 0.02);
        if (getDeltaMovement().horizontalDistanceSqr() > 0.002 && tickCount % 10 == 0) playSound(SoundEvents.IRON_GOLEM_STEP, 1f, 0.6f);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.ANVIL_HIT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_EXPLODE;
    }
}
