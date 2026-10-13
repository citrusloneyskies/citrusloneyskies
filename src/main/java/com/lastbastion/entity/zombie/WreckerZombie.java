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

/** Siege engineer with a stone-headed sledgehammer. Hunts turrets first and deals triple damage to them. */
public class WreckerZombie extends SiegeZombie {
    public WreckerZombie(EntityType<? extends SiegeZombie> type, Level level) {
        super(type, level, ZombieKind.WRECKER);
    }

    @Override
    public boolean targetsTurrets() {
        return true;
    }

    @Override
    public double aggroRange() {
        return 5;
    }

    @Override
    protected float turretDamageMultiplier() {
        return 3f;
    }

    @Override
    public float breachPower() {
        return super.breachPower() * 2f;
    }

    @Override
    public int attackWindup() {
        return 14;
    }

    @Override
    public int attackCooldown() {
        return 26;
    }

    @Override
    protected void onMeleeHit(LivingEntity target, float dmg) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.5, target.getZ(), 1, 0, 0, 0, 0);
            Fx.sound(sl, target.position(), SoundEvents.ANVIL_LAND, 0.6f, 0.6f);
        }
    }
}
