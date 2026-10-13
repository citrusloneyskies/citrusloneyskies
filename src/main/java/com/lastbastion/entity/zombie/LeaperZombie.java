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

/** Long-limbed acrobat. Vaults over walls up to six blocks high and pounces from rooftops. */
public class LeaperZombie extends SiegeZombie {
    private int jumpCooldown;

    public LeaperZombie(EntityType<? extends SiegeZombie> type, Level level) {
        super(type, level, ZombieKind.LEAPER);
    }

    @Override
    public double aggroRange() {
        return 8;
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return false;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (jumpCooldown > 0) jumpCooldown--;
        if (jumpCooldown > 0 || isIncapacitated() || !onGround()) return;
        BlockPos core = SiegeManager.corePos(level);
        Vec3 goal = getTarget() != null ? getTarget().position() : core != null ? Vec3.atBottomCenterOf(core) : null;
        if (goal == null) return;
        Vec3 dir = goal.subtract(position()).multiply(1, 0, 1);
        double d = dir.length();
        if (d < 0.5) return;
        dir = dir.normalize();
        boolean wall = horizontalCollision || getNavigation().isStuck();
        if (wall || (d > 4 && d < 8 && getTarget() != null)) {
            double up = wall ? 1.15 : 0.55;
            setDeltaMovement(dir.x * 0.6, up, dir.z * 0.6);
            hasImpulse = true;
            anim.play(ANIM_SPECIAL);
            level.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 8, 0.3, 0.05, 0.3, 0.02);
            Fx.sound(level, position(), SoundEvents.GOAT_LONG_JUMP, 1f, 0.7f);
            jumpCooldown = wall ? 50 : 90;
        }
    }
}
