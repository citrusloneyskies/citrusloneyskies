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
 * Bloated-throated ranged zombie. Prefers to snipe turrets with corrosive bile from 8-14 blocks,
 * retreating if defenders close in. Without a ranged target it lobs bile at the core.
 */
public class SpitterZombie extends SiegeZombie {
    private int spitCooldown = 40;

    public SpitterZombie(EntityType<? extends SiegeZombie> type, Level level) {
        super(type, level, ZombieKind.SPITTER);
    }

    @Override
    protected float kindElementMultiplier(Element e) {
        return e == Element.POISON ? 0.5f : 1f;
    }

    @Override
    public double aggroRange() {
        return 4;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (spitCooldown > 0) spitCooldown--;
        if (isIncapacitated()) return;
        LivingEntity aim = null;
        double best = 15 * 15;
        for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 15)) {
            double d = distanceToSqr(e) * (e instanceof TurretEntity ? 0.5 : 1.0);
            if (d < best && getSensing().hasLineOfSight(e)) {
                best = d;
                aim = e;
            }
        }
        Vec3 aimPos = null;
        if (aim != null) aimPos = aim.position().add(0, aim.getBbHeight() * 0.5, 0);
        else {
            BlockPos core = SiegeManager.corePos(level);
            if (core != null && distanceToSqr(Vec3.atCenterOf(core)) < 13 * 13) aimPos = Vec3.atCenterOf(core);
        }
        if (aimPos == null) return;
        double dist = Math.sqrt(distanceToSqr(aimPos));
        if (dist < 7 && aim != null) {
            Vec3 away = position().subtract(aimPos).multiply(1, 0, 1).normalize().scale(4);
            getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.1);
        } else if (dist < 14) {
            getNavigation().stop();
        }
        getLookControl().setLookAt(aimPos.x, aimPos.y, aimPos.z);
        if (spitCooldown == 12) anim.play(ANIM_SPECIAL);
        if (spitCooldown <= 0 && dist < 15) {
            spitCooldown = 60 + random.nextInt(20);
            boolean atCore = aim == null;
            Vec3 mouth = position().add(0, getBbHeight() * 0.85, 0);
            SiegeProjectile p = SiegeProjectile.create(level, SiegeProjectile.ProjKind.BILE, this, mouth);
            p.payload(kind.damage * damageScale(), Element.POISON).area(1.6f).status(ModEffects.CORRODED, 100, 3);
            final Vec3 corePos = aimPos;
            if (atCore) {
                p.onImpact = (proj, at) -> {
                    if (at.distanceToSqr(corePos) < 4) SiegeManager.damageCore(level, coreDamage(), this);
                };
            }
            p.aim(aimPos, 0.9f).scale(1.2f).fire(level);
            Fx.sound(level, position(), SoundEvents.LLAMA_SPIT, 1f, 0.6f);
        }
    }
}
