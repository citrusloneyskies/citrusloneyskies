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

/** Ashfang the Dire Wolf: fast skirmisher that pounces on stragglers and rallies allies with a howl. */
public class AshfangHero extends HeroEntity {
    public static final int POUNCE = 4, HOWL = 5;
    private Vec3 pounceAt = Vec3.ZERO;

    public AshfangHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.ASHFANG);
    }

    @Override
    protected int attackWindup() {
        return 5;
    }

    @Override
    protected int attackInterval() {
        return 14;
    }

    @Override
    protected int comboLength() {
        return 2;
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        SiegeDamage.deal(t, this, this, dmg(combo == 0 ? 1f : 0.8f), Element.PHYSICAL, false);
        SiegeDamage.applyStacking(t, ModEffects.BLEEDING.get(), 80, 2);
        playSound(SoundEvents.WOLF_GROWL, 0.6f, 1.2f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (ready(HOWL) && SiegeDamage.enemiesAround(level, position(), 10).size() >= 3) return HOWL;
        if (t != null && dist > 4 && dist < 10 && ready(POUNCE) && onGround()) return POUNCE;
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        if (skill == POUNCE) {
            if (t == 4 && target != null) {
                pounceAt = target.position();
                Vec3 d = pounceAt.subtract(position());
                setDeltaMovement(d.x * 0.18, 0.55, d.z * 0.18);
                hasImpulse = true;
                playSound(SoundEvents.WOLF_GROWL, 1f, 0.8f);
            }
            if (t > 8 && (onGround() || t > 30)) {
                for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 2.2)) {
                    SiegeDamage.deal(e, this, this, dmg(1.6f), Element.PHYSICAL, false);
                    SiegeDamage.applyStacking(e, ModEffects.BLEEDING.get(), 100, 3);
                    SiegeDamage.push(e, position(), 0.6, 0.3);
                }
                level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.5, getZ(), 15, 0.6, 0.3, 0.6, 0.2);
                cooldown(POUNCE, 90);
                return true;
            }
            return false;
        }
        if (skill == HOWL) {
            if (t == 12) {
                playSound(SoundEvents.WOLF_HOWL, 2f, 0.7f);
                for (double r = 1; r < 12; r += 2) Fx.ring(level, position().add(0, 1, 0), r, Fx.dust(0xFF8A3D, 1.2f), (int) (r * 6));
                for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 12)) {
                    a.addEffect(new MobEffectInstance(ModEffects.RALLY.get(), 200, 0));
                }
                for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 10)) {
                    if (e instanceof RunnerZombie z) {
                        z.fearFrom = position();
                        z.addEffect(new MobEffectInstance(ModEffects.FEARED.get(), 60, 0));
                    }
                }
            }
            if (t >= 30) {
                cooldown(HOWL, 400);
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.WOLF_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.WOLF_PANT;
    }
}
