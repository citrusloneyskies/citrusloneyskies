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
 * Hooded caster that hangs back ~16 blocks from the core, raising fresh shamblers from the earth
 * every 10 seconds and mending nearby undead. Flees from heroes and players. Kill it first.
 */
public class NecromancerZombie extends SiegeZombie {
    private int raiseCooldown = 100;
    private int healCooldown = 60;

    public NecromancerZombie(EntityType<? extends SiegeZombie> type, Level level) {
        super(type, level, ZombieKind.NECROMANCER);
    }

    @Override
    protected float kindElementMultiplier(Element e) {
        return e == Element.HOLY ? 1.3f : e == Element.SHADOW ? 0f : 1f;
    }

    @Override
    public double aggroRange() {
        return 3;
    }

    @Override
    public boolean overrideMovement() {
        BlockPos core = level() instanceof ServerLevel sl ? SiegeManager.corePos(sl) : null;
        return core != null && position().distanceTo(Vec3.atBottomCenterOf(core)) < 18 && getTarget() == null;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (isIncapacitated()) return;
        BlockPos core = SiegeManager.corePos(level);
        // keep distance from threats and from the core
        LivingEntity threat = null;
        for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 7)) {
            if (!(e instanceof TurretEntity)) {
                threat = e;
                break;
            }
        }
        if (threat != null && tickCount % 10 == 0) {
            Vec3 away = position().subtract(threat.position()).multiply(1, 0, 1).normalize().scale(6);
            getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.25);
        } else if (overrideMovement() && core != null && tickCount % 20 == 0) {
            double d = position().distanceTo(Vec3.atBottomCenterOf(core));
            if (d < 14) {
                Vec3 away = position().subtract(Vec3.atBottomCenterOf(core)).multiply(1, 0, 1).normalize().scale(4);
                getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.0);
            } else getNavigation().stop();
        }
        if (--raiseCooldown <= 0) {
            raiseCooldown = 200;
            anim.play(ANIM_SPECIAL);
            Fx.sound(level, position(), SoundEvents.EVOKER_PREPARE_SUMMON, 1.2f, 0.8f);
            int n = 2 + (rank().ordinal() >= 3 ? 1 : 0);
            for (int i = 0; i < n; i++) {
                Vec3 p = position().add(random.nextGaussian() * 2.5, 0, random.nextGaussian() * 2.5);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(p.x), (int) Math.floor(p.z));
                SiegeZombie z = ZombieSpawner.spawn(level, ZombieKind.SHAMBLER, ZombieRank.byId(rank().ordinal() - 1), 0,
                        new Vec3(p.x, y, p.z), true, true);
                if (z != null) {
                    z.setMinion(true);
                    level.sendParticles(ParticleTypes.SOUL, p.x, y + 0.5, p.z, 12, 0.3, 0.4, 0.3, 0.03);
                }
            }
        }
        if (--healCooldown <= 0) {
            healCooldown = 100;
            for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 8)) {
                if (e == this || e.getHealth() >= e.getMaxHealth()) continue;
                e.heal(e.getMaxHealth() * 0.1f);
                Fx.line(level, position().add(0, 1.6, 0), e.position().add(0, e.getBbHeight() * 0.6, 0), Fx.dust(0x6BFF8A, 0.8f), 0.4);
            }
        }
    }
}
