package com.lastbastion.item;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Instant-hit ballistics with tracers for player weapons, drones and the exosuit. */
public final class Hitscan {
    private Hitscan() {
    }

    public static Vec3 spreadDir(Entity shooter, Vec3 dir, double spread) {
        var r = shooter.level().random;
        return dir.add(r.nextGaussian() * spread, r.nextGaussian() * spread, r.nextGaussian() * spread).normalize();
    }

    @Nullable
    public static LivingEntity fire(ServerLevel level, Entity shooter, Vec3 from, Vec3 dir, double range, float damage, Element element,
                                    ParticleOptions tracer) {
        Vec3 to = from.add(dir.scale(range));
        HitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        Vec3 end = block.getType() != HitResult.Type.MISS ? block.getLocation() : to;
        EntityHitResult eh = ProjectileUtil.getEntityHitResult(level, shooter, from, end,
                shooter.getBoundingBox().expandTowards(dir.scale(range)).inflate(1), e -> e instanceof LivingEntity && e != shooter
                        && e.isAlive() && SiegeDamage.canHit(shooter, e) && e != shooter.getVehicle());
        LivingEntity hit = null;
        if (eh != null && eh.getEntity() instanceof LivingEntity le) {
            end = eh.getLocation();
            hit = le;
            SiegeDamage.deal(le, shooter, shooter, damage, element, false);
            level.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 3, 0.1, 0.1, 0.1, 0.1);
        } else if (block.getType() != HitResult.Type.MISS) {
            level.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 2, 0.05, 0.05, 0.05, 0.01);
        }
        Fx.line(level, from.add(dir.scale(0.8)), end, tracer, 0.9);
        return hit;
    }
}
