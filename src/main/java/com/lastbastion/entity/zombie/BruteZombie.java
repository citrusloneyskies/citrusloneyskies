package com.lastbastion.entity.zombie;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.turret.TurretEntity;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Hulking mutated tank. Smashes barricades three times faster, bashes turrets in its path,
 * and periodically charges, bowling defenders aside.
 */
public class BruteZombie extends SiegeZombie {
    private int chargeCooldown = 100;
    private int charging;
    @Nullable
    private Vec3 chargeDir;

    public BruteZombie(EntityType<? extends SiegeZombie> type, Level level) {
        super(type, level, ZombieKind.BRUTE);
    }

    @Override
    protected float kindElementMultiplier(Element e) {
        return e == Element.PHYSICAL ? 0.85f : e == Element.EXPLOSIVE ? 1.2f : 1f;
    }

    @Override
    public double aggroRange() {
        return 6;
    }

    @Override
    public boolean targetsTurrets() {
        return true;
    }

    @Override
    public float breachPower() {
        return super.breachPower() * 3f;
    }

    @Override
    public boolean canBreachHard() {
        return true;
    }

    @Override
    public int attackWindup() {
        return 12;
    }

    @Override
    public int attackCooldown() {
        return 30;
    }

    @Override
    protected float turretDamageMultiplier() {
        return 1.5f;
    }

    @Override
    protected void onMeleeHit(LivingEntity target, float dmg) {
        SiegeDamage.push(target, position(), 1.1, 0.45);
        SiegeDamage.apply(target, ModEffects.CONCUSSED.get(), 60, 0);
    }

    @Override
    public boolean overrideMovement() {
        return charging > 0;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (chargeCooldown > 0) chargeCooldown--;
        if (charging > 0) {
            charging--;
            if (chargeDir != null && charging < 30) {
                setDeltaMovement(chargeDir.x * 0.55, getDeltaMovement().y, chargeDir.z * 0.55);
                level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 2, 0.4, 0.1, 0.4, 0.02);
                for (LivingEntity e : SiegeDamage.defendersAround(level, position().add(chargeDir), 1.6)) {
                    if (e.invulnerableTime > 0) continue;
                    SiegeDamage.deal(e, this, this, (float) getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 1.5f, Element.PHYSICAL, false);
                    SiegeDamage.push(e, position(), 1.6, 0.6);
                }
                if (horizontalCollision) charging = 0;
            }
            return;
        }
        if (chargeCooldown > 0 || isIncapacitated() || !onGround()) return;
        Vec3 goal = getTarget() != null ? getTarget().position() : null;
        if (goal == null) {
            BlockPos core = SiegeManager.corePos(level);
            if (core != null) goal = Vec3.atBottomCenterOf(core);
        }
        if (goal == null) return;
        double d = goal.distanceTo(position());
        if (d > 7 && d < 15) {
            chargeDir = goal.subtract(position()).multiply(1, 0, 1).normalize();
            charging = 42;
            chargeCooldown = 200;
            getNavigation().stop();
            anim.play(ANIM_SPECIAL);
            Fx.sound(level, position(), SoundEvents.RAVAGER_ROAR, 1.2f, 1.2f);
        }
    }
}
