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
 * Armoured zombie behind a riveted tower shield. Projectiles and turret fire from the front
 * 120 degree arc are reduced by 80%. Flank it, use AoE, or melee it.
 */
public class BulwarkZombie extends SiegeZombie {
    public BulwarkZombie(EntityType<? extends SiegeZombie> type, Level level) {
        super(type, level, ZombieKind.BULWARK);
    }

    @Override
    public double aggroRange() {
        return 5;
    }

    @Override
    public int attackWindup() {
        return 10;
    }

    @Override
    protected void onMeleeHit(LivingEntity target, float dmg) {
        SiegeDamage.push(target, position(), 0.9, 0.3);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        var direct = source.getDirectEntity();
        if (direct != null && !level().isClientSide && distanceToSqr(direct) > 9 && !source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)
                && !isIncapacitated()) {
            Vec3 look = getViewVector(1f).multiply(1, 0, 1).normalize();
            Vec3 to = direct.position().subtract(position()).multiply(1, 0, 1).normalize();
            if (look.dot(to) > 0.5) {
                amount *= 0.2f;
                anim.play(ANIM_BLOCK);
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CRIT, getX() + look.x, getY() + 1.2, getZ() + look.z, 6, 0.2, 0.3, 0.2, 0.2);
                    Fx.sound(sl, position(), SoundEvents.SHIELD_BLOCK, 0.8f, 0.8f);
                }
            }
        }
        return super.hurt(source, amount);
    }
}
