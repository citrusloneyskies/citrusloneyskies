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

/** Emaciated sprinter. Pounces on defenders and the core from up to 7 blocks away. */
public class RunnerZombie extends SiegeZombie {
    private int leapCooldown = 40;

    public RunnerZombie(EntityType<? extends SiegeZombie> type, Level level) {
        super(type, level, ZombieKind.RUNNER);
    }

    @Override
    public double aggroRange() {
        return 9;
    }

    @Override
    public int attackWindup() {
        return 5;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (leapCooldown > 0) leapCooldown--;
        if (leapCooldown > 0 || isIncapacitated() || !onGround()) return;
        Vec3 goal = null;
        LivingEntity t = getTarget();
        if (t != null) goal = t.position();
        else {
            BlockPos core = SiegeManager.corePos(level);
            if (core != null) goal = Vec3.atBottomCenterOf(core);
        }
        if (goal == null) return;
        double d = goal.distanceTo(position());
        if (d > 3 && d < 7.5) {
            Vec3 dir = goal.subtract(position()).multiply(1, 0, 1).normalize();
            setDeltaMovement(dir.x * 0.85, 0.42, dir.z * 0.85);
            hasImpulse = true;
            anim.play(ANIM_SPECIAL);
            Fx.sound(level, position(), SoundEvents.ZOMBIE_INFECT, 0.8f, 1.6f);
            leapCooldown = 70 + random.nextInt(40);
        }
    }
}
