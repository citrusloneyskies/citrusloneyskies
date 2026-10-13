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

/** Vorthax the Elder Dragon: aerial artillery with breath, tail sweeps, gusts, a terrifying roar and meteor storms. */
public class VorthaxHero extends HeroEntity {
    public static final int TAIL = 4, GUST = 5, ROAR = 6, METEORS = 7;

    public VorthaxHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.VORTHAX);
    }

    @Override
    protected boolean isRanged() {
        return true;
    }

    @Override
    protected double attackRange() {
        return 14;
    }

    @Override
    protected double preferredRange() {
        return 8;
    }

    @Override
    protected int attackWindup() {
        return 10;
    }

    @Override
    protected int attackInterval() {
        return 30;
    }

    @Override
    protected double preferredAltitude() {
        return 6;
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        Vec3 mouth = position().add(0, getBbHeight() * 0.6, 0).add(Vec3.directionFromRotation(0, getYRot()).scale(3));
        Vec3 dir = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(mouth).normalize();
        Fx.cone(level, mouth, dir, 14, 12, ParticleTypes.FLAME, 60);
        playSound(SoundEvents.ENDER_DRAGON_SHOOT, 1.5f, 0.8f);
        for (LivingEntity e : SiegeDamage.enemiesAround(level, mouth.add(dir.scale(7)), 7)) {
            if (e.position().add(0, 0.5, 0).subtract(mouth).normalize().dot(dir) < 0.8) continue;
            SiegeDamage.deal(e, this, this, dmg(1f), Element.FIRE, false);
            SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 80, 2);
            e.setSecondsOnFire(4);
        }
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        BlockPos core = SiegeManager.corePos(level);
        if (ready(METEORS) && core != null && SiegeDamage.enemiesAround(level, Vec3.atCenterOf(core), 18).size() >= 8) return METEORS;
        if (ready(ROAR) && SiegeDamage.enemiesAround(level, position(), 14).size() >= 6) return ROAR;
        if (ready(TAIL) && SiegeDamage.enemiesAround(level, position(), 7).size() >= 3) return TAIL;
        if (ready(GUST) && SiegeDamage.enemiesAround(level, position(), 10).size() >= 4) return GUST;
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        setDeltaMovement(getDeltaMovement().scale(0.7));
        switch (skill) {
            case TAIL -> {
                if (t == 14) {
                    Fx.ring(level, position().add(0, 1, 0), 6, ParticleTypes.SWEEP_ATTACK, 14);
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2f, 0.4f);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 7)) {
                        SiegeDamage.deal(e, this, this, dmg(2.5f), Element.PHYSICAL, false);
                        SiegeDamage.push(e, position(), 1.8, 0.6);
                    }
                }
                if (t >= 26) {
                    cooldown(TAIL, 160);
                    return true;
                }
            }
            case GUST -> {
                if (t == 12) {
                    Fx.shockwave(level, position().subtract(0, preferredAltitude() * 0.5, 0), 10, 0xCFFFF0);
                    playSound(SoundEvents.ENDER_DRAGON_FLAP, 3f, 0.6f);
                    BlockPos core = SiegeManager.corePos(level);
                    Vec3 from = core != null ? Vec3.atCenterOf(core) : position();
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 11)) {
                        SiegeDamage.deal(e, this, this, dmg(1f), Element.WIND, false);
                        SiegeDamage.push(e, from, 1.8, 0.5);
                        SiegeDamage.apply(e, ModEffects.DISORIENTED.get(), 80, 0);
                    }
                }
                if (t >= 22) {
                    cooldown(GUST, 200);
                    return true;
                }
            }
            case ROAR -> {
                if (t == 15) {
                    playSound(SoundEvents.ENDER_DRAGON_GROWL, 4f, 0.7f);
                    for (double r = 2; r < 14; r += 2) Fx.ring(level, position(), r, ParticleTypes.SONIC_BOOM, (int) r * 2);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 14)) {
                        if (e instanceof SiegeZombie z) {
                            z.fearFrom = position();
                            z.addEffect(new MobEffectInstance(ModEffects.FEARED.get(), 80, 0));
                        }
                    }
                }
                if (t >= 30) {
                    cooldown(ROAR, 500);
                    return true;
                }
            }
            case METEORS -> {
                if (t == 20) {
                    BlockPos core = SiegeManager.corePos(level);
                    if (core != null) {
                        List<LivingEntity> es = SiegeDamage.enemiesAround(level, Vec3.atCenterOf(core), 18);
                        Vec3 sum = Vec3.ZERO;
                        for (LivingEntity e : es) sum = sum.add(e.position());
                        Vec3 c = es.isEmpty() ? Vec3.atCenterOf(core) : sum.scale(1.0 / es.size());
                        HazardZone.spawn(level, HazardZone.Kind.METEOR_RAIN, c, 8f, 120, dmg(0.12f), this, false);
                    }
                    playSound(SoundEvents.ENDER_DRAGON_GROWL, 3f, 0.5f);
                }
                if (t >= 34) {
                    cooldown(METEORS, 700);
                    return true;
                }
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void passiveTick(ServerLevel level) {
        if (tickCount % 20 == 0) playSound(SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 0.9f);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_DEATH;
    }
}
