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

/** Bastion the Stone Colossus: the tank. Taunts the horde off the core and shatters the earth. */
public class BastionHero extends HeroEntity {
    public static final int TAUNT = 4, SHATTER = 5, STONESKIN = 6;

    public BastionHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.BASTION);
    }

    @Override
    protected int attackWindup() {
        return 12;
    }

    @Override
    protected int attackInterval() {
        return 26;
    }

    @Override
    protected int comboLength() {
        return 2;
    }

    @Override
    protected double attackRange() {
        return 3.0;
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        cleave(level, 1.8, 2.0, dmg(1f), Element.PHYSICAL);
        SiegeDamage.push(t, position(), 0.9, 0.3);
        level.sendParticles(ParticleTypes.EXPLOSION, t.getX(), t.getY() + 0.5, t.getZ(), 1, 0, 0, 0, 0);
        playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.2f, 0.7f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (ready(STONESKIN) && getHealth() < getMaxHealth() * 0.5f) return STONESKIN;
        if (ready(SHATTER) && SiegeDamage.enemiesAround(level, position(), 6).size() >= 3) return SHATTER;
        if (ready(TAUNT) && SiegeDamage.enemiesAround(level, position(), 10).size() >= 3) return TAUNT;
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case TAUNT -> {
                if (t == 10) {
                    playSound(SoundEvents.IRON_GOLEM_REPAIR, 2f, 0.4f);
                    Fx.shockwave(level, position(), 10, 0xFFB000);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 10)) {
                        if (e instanceof SiegeZombie z) {
                            z.tauntedBy = getUUID();
                            z.tauntTicks = 120;
                            z.setTarget(this);
                        }
                    }
                }
                if (t >= 20) {
                    cooldown(TAUNT, 300);
                    return true;
                }
            }
            case SHATTER -> {
                if (t == 22) {
                    Fx.shockwave(level, position(), 6, 0x8C8577);
                    playSound(SoundEvents.GENERIC_EXPLODE, 1.5f, 0.5f);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 6)) {
                        SiegeDamage.deal(e, this, this, dmg(2f), Element.PHYSICAL, false);
                        e.setDeltaMovement(e.getDeltaMovement().add(0, 0.8, 0));
                        e.hurtMarked = true;
                        SiegeDamage.apply(e, ModEffects.CONCUSSED.get(), 100, 0);
                    }
                }
                if (t >= 36) {
                    cooldown(SHATTER, 180);
                    return true;
                }
            }
            case STONESKIN -> {
                if (t == 8) {
                    addEffect(new MobEffectInstance(ModEffects.AEGIS.get(), 160, 2));
                    level.sendParticles(Fx.dust(0x8C8577, 2f), getX(), getY() + 1.5, getZ(), 40, 1, 1.2, 1, 0);
                    playSound(SoundEvents.STONE_PLACE, 2f, 0.5f);
                }
                if (t >= 16) {
                    cooldown(STONESKIN, 400);
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
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }
}
