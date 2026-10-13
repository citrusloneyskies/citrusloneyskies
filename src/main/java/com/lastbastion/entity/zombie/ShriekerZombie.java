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
 * Emaciated banshee with a split jaw. Hangs back and every 12 seconds unleashes a scream that
 * enrages zombies within 12 blocks (speed + strength) and jams turrets within 10 blocks.
 * A top-priority target.
 */
public class ShriekerZombie extends SiegeZombie {
    private int screamCooldown = 120;

    public ShriekerZombie(EntityType<? extends SiegeZombie> type, Level level) {
        super(type, level, ZombieKind.SHRIEKER);
    }

    @Override
    public double aggroRange() {
        return 4;
    }

    @Override
    public boolean overrideMovement() {
        return screamCooldown > 220;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (isIncapacitated()) return;
        BlockPos core = SiegeManager.corePos(level);
        if (core != null && getTarget() == null) {
            double d = position().distanceTo(Vec3.atBottomCenterOf(core));
            if (d < 12) {
                Vec3 away = position().subtract(Vec3.atBottomCenterOf(core)).multiply(1, 0, 1).normalize().scale(3);
                getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.0);
            }
        }
        if (--screamCooldown <= 0) {
            screamCooldown = 240;
            anim.play(ANIM_SPECIAL);
            getNavigation().stop();
            Fx.sound(level, position(), SoundEvents.WARDEN_ROAR, 1.5f, 1.8f);
            Fx.sound(level, position(), SoundEvents.GHAST_SCREAM, 1.5f, 0.6f);
            for (double r = 1; r < 12; r += 1.5) Fx.ring(level, position().add(0, 1.5, 0), r, ParticleTypes.SONIC_BOOM, (int) (r * 1.5) + 3);
            for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 12)) {
                if (e == this) continue;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1));
                e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0));
            }
            for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 10)) {
                if (e instanceof TurretEntity) SiegeDamage.apply(e, ModEffects.JAMMED.get(), 60, 0);
                else SiegeDamage.apply(e, ModEffects.DISORIENTED.get(), 40, 0);
            }
        }
    }
}
