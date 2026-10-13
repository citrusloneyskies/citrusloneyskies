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

/**
 * Sir Aldric, Knight of Dawn. A paladin whose blade Dawnbringer channels light.
 * Combo: rising slash, reverse slash, radiant thrust. Radiant Dash cuts through lines of zombies;
 * Judgement is a leaping plunge that calls down a ring of light pillars; Aegis of Dawn shields and
 * heals allies and the core.
 */
public class AldricHero extends HeroEntity {
    public static final int DASH = 4, JUDGEMENT = 5, AEGIS = 6;
    private Vec3 dashDir = Vec3.ZERO;
    private Vec3 leapAt = Vec3.ZERO;

    public AldricHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.ALDRIC);
    }

    @Override
    protected int attackWindup() {
        return 6;
    }

    @Override
    protected int attackInterval() {
        return 13;
    }

    @Override
    protected int comboLength() {
        return 3;
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        float mult = combo == 2 ? 1.6f : 1f;
        cleave(level, 1.6, combo == 2 ? 1.6 : 2.2, dmg(mult), Element.HOLY);
        Vec3 c = forward(1.6).add(0, 1.1, 0);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(Fx.dust(0xFFE38A, 1.2f), c.x, c.y, c.z, 12, 0.6, 0.3, 0.6, 0);
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 4, 0.4, 0.2, 0.4, 0.05);
        playSound(combo == 2 ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.1f);
        if (combo == 2) SiegeDamage.apply(t, ModEffects.PURIFIED.get(), 60, 0);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (ready(AEGIS) && needsAegis(level)) return AEGIS;
        if (t == null) return 0;
        if (ready(JUDGEMENT) && SiegeDamage.enemiesAround(level, t.position(), 5).size() >= 3 && dist < 10) return JUDGEMENT;
        if (ready(DASH) && dist > 4 && dist < 12) return DASH;
        return 0;
    }

    private boolean needsAegis(ServerLevel level) {
        var d = SiegeManager.data(level);
        if (d.hasCore() && d.coreHp < d.maxHp() * 0.6f && SiegeManager.corePos(level) != null
                && SiegeManager.corePos(level).closerToCenterThan(position(), 14)) return true;
        for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 10)) {
            if (!(a instanceof com.lastbastion.entity.turret.TurretEntity) && a.getHealth() < a.getMaxHealth() * 0.5f) return true;
        }
        return false;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case DASH -> {
                if (t == 1 && target != null) {
                    dashDir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
                    faceTowards(target.position());
                    playSound(SoundEvents.TRIDENT_RIPTIDE_1, 1f, 1.3f);
                }
                if (t >= 6 && t < 16) {
                    setDeltaMovement(dashDir.x * 1.2, getDeltaMovement().y, dashDir.z * 1.2);
                    level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 4, 0.3, 0.5, 0.3, 0.02);
                    level.sendParticles(Fx.dust(0xFFE38A, 1.5f), getX(), getY() + 1, getZ(), 6, 0.4, 0.6, 0.4, 0);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 2)) {
                        if (e.hurtTime > 0) continue;
                        SiegeDamage.deal(e, this, this, dmg(2f), Element.HOLY, false);
                        SiegeDamage.apply(e, ModEffects.PURIFIED.get(), 80, 0);
                    }
                }
                if (t >= 20) {
                    cooldown(DASH, 120);
                    return true;
                }
            }
            case JUDGEMENT -> {
                if (t == 1 && target != null) leapAt = target.position();
                if (t == 8) {
                    Vec3 d = leapAt.subtract(position());
                    setDeltaMovement(d.x * 0.12, 0.95, d.z * 0.12);
                    hasImpulse = true;
                    playSound(SoundEvents.BEACON_POWER_SELECT, 1.5f, 1.4f);
                }
                if (t > 8 && t < 40) level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 3, 0.2, 0.4, 0.2, 0.02);
                if (t > 14 && (onGround() || t >= 40)) {
                    Fx.shockwave(level, position(), 5, 0xFFE38A);
                    for (int i = 0; i < 6; i++) {
                        double a = Math.PI * 2 * i / 6;
                        Fx.skyBeam(level, position().add(Math.cos(a) * 4, 0, Math.sin(a) * 4), 0xFFE38A, 14, 0.6);
                    }
                    playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.5f, 1.5f);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 5)) {
                        SiegeDamage.deal(e, this, this, dmg(3f), Element.HOLY, false);
                        SiegeDamage.apply(e, ModEffects.PURIFIED.get(), 120, 0);
                        SiegeDamage.push(e, position(), 0.8, 0.5);
                    }
                    cooldown(JUDGEMENT, 260);
                    return true;
                }
            }
            case AEGIS -> {
                if (t == 14) {
                    Fx.spiral(level, position(), 3, 3, Fx.dust(0xFFE38A, 1.4f), 3, 0);
                    Fx.ring(level, position().add(0, 0.2, 0), 10, ParticleTypes.END_ROD, 40);
                    playSound(SoundEvents.BEACON_ACTIVATE, 1.5f, 1.6f);
                    for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 10)) {
                        a.addEffect(new MobEffectInstance(ModEffects.AEGIS.get(), 200, 1));
                        a.heal(a.getMaxHealth() * 0.2f);
                    }
                    BlockPos core = SiegeManager.corePos(level);
                    if (core != null && core.closerToCenterThan(position(), 14)) SiegeManager.healCore(level, SiegeManager.data(level).maxHp() * 0.05f);
                }
                if (t >= 26) {
                    cooldown(AEGIS, 500);
                    return true;
                }
            }
            default -> {
                return true;
            }
        }
        return false;
    }
}
