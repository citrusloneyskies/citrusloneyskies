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

/** Sylva the Huntress: elven archer. Kites, marks priority targets and blankets groups with arrow rain. */
public class SylvaHero extends HeroEntity {
    public static final int PIERCE = 4, RAIN = 5, ROLL = 6, MARK = 7;
    private Vec3 rainAt = Vec3.ZERO;

    public SylvaHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.SYLVA);
    }

    @Override
    protected boolean isRanged() {
        return true;
    }

    @Override
    protected double attackRange() {
        return 22;
    }

    @Override
    protected double preferredRange() {
        return 13;
    }

    @Override
    protected int attackWindup() {
        return 10;
    }

    @Override
    protected int attackInterval() {
        return 18;
    }

    private void arrow(ServerLevel level, LivingEntity t, float mult, int pierce, float scale) {
        Vec3 from = position().add(0, getBbHeight() * 0.8, 0);
        SiegeProjectile p = SiegeProjectile.create(level, ProjKind.ARROW, this, from).payload(dmg(mult), Element.PHYSICAL);
        p.pierce = pierce;
        p.aim(t.position().add(0, t.getBbHeight() * 0.6, 0), 2.2f).scale(scale).fire(level);
        playSound(SoundEvents.ARROW_SHOOT, 1f, 1.1f);
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        arrow(level, t, 1f, 0, 1f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (ready(ROLL) && !SiegeDamage.enemiesAround(level, position(), 3).isEmpty()) return ROLL;
        if (t == null) return 0;
        if (ready(MARK) && t instanceof SiegeEnemy se && se.isHighPriority()) return MARK;
        if (ready(RAIN) && SiegeDamage.enemiesAround(level, t.position(), 4).size() >= 4 && dist < 22) return RAIN;
        if (ready(PIERCE) && dist > 6 && dist < 22) return PIERCE;
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case PIERCE -> {
                if (target != null) getLookControl().setLookAt(target, 30, 30);
                if (t == 18 && target != null) {
                    arrow(level, target, 2.5f, 5, 1.6f);
                    level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.4, getZ(), 10, 0.2, 0.2, 0.2, 0.3);
                }
                if (t >= 24) {
                    cooldown(PIERCE, 110);
                    return true;
                }
            }
            case RAIN -> {
                if (t == 1 && target != null) rainAt = target.position();
                if (t == 10) playSound(SoundEvents.CROSSBOW_LOADING_END, 1f, 0.8f);
                if (t >= 14 && t <= 44 && t % 2 == 0) {
                    Vec3 at = rainAt.add(random.nextGaussian() * 2.5, 0, random.nextGaussian() * 2.5);
                    SiegeProjectile p = SiegeProjectile.create(level, ProjKind.ARROW, this, at.add(random.nextGaussian(), 16, random.nextGaussian()));
                    p.payload(dmg(0.8f), Element.PHYSICAL).area(1.2f);
                    p.aim(at, 1.8f).fire(level);
                }
                if (t >= 46) {
                    cooldown(RAIN, 240);
                    return true;
                }
            }
            case ROLL -> {
                if (t == 2) {
                    LivingEntity near = SiegeDamage.enemiesAround(level, position(), 4).stream().findFirst().orElse(null);
                    Vec3 away = near != null ? position().subtract(near.position()).multiply(1, 0, 1).normalize() : forward(-1).subtract(position());
                    setDeltaMovement(away.x * 1.1, 0.3, away.z * 1.1);
                    hasImpulse = true;
                    level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 8, 0.3, 0.1, 0.3, 0.02);
                }
                if (t >= 12) {
                    cooldown(ROLL, 120);
                    return true;
                }
            }
            case MARK -> {
                if (t == 8 && target != null) {
                    target.addEffect(new MobEffectInstance(ModEffects.MARKED.get(), 200, 2));
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                    Fx.line(level, position().add(0, 1.5, 0), target.position().add(0, target.getBbHeight(), 0), Fx.dust(0xE04AE0, 0.8f), 0.5);
                }
                if (t >= 14) {
                    cooldown(MARK, 300);
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
