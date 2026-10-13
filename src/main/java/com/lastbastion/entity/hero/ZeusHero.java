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

/** Zeus, King of Olympus: hurls thunderbolts, chains storms, smites bosses and shields the whole army. */
public class ZeusHero extends HeroEntity {
    public static final int CHAIN = 4, SMITE = 5, AEGIS = 6, WRATH = 7;

    public ZeusHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.ZEUS);
    }

    @Override
    protected boolean isRanged() {
        return true;
    }

    @Override
    protected double attackRange() {
        return 24;
    }

    @Override
    protected double preferredRange() {
        return 14;
    }

    @Override
    protected int attackWindup() {
        return 10;
    }

    @Override
    protected int attackInterval() {
        return 24;
    }

    @Override
    protected double preferredAltitude() {
        return 3;
    }

    @Override
    protected double flySpeed() {
        return 0.45;
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        Vec3 hand = position().add(0, getBbHeight() * 0.95, 0).add(Vec3.directionFromRotation(0, getYRot() + 60).scale(0.6));
        SiegeProjectile p = SiegeProjectile.create(level, ProjKind.JAVELIN, this, hand).payload(dmg(1f), Element.LIGHTNING)
                .area(2.5f).status(ModEffects.SHOCKED, 15, 0);
        p.aim(t.position().add(0, t.getBbHeight() * 0.5, 0), 2.4f).scale(1.6f).fire(level);
        playSound(SoundEvents.TRIDENT_THUNDER, 1f, 1.4f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        BlockPos core = SiegeManager.corePos(level);
        if (ready(AEGIS) && danger(level)) return AEGIS;
        if (ready(WRATH) && core != null && SiegeDamage.enemiesAround(level, Vec3.atCenterOf(core), 20).size() >= 10) return WRATH;
        LivingEntity boss = SiegeDamage.enemiesAround(level, position(), 30).stream()
                .filter(e -> e instanceof SiegeEnemy se && se.isHighPriority()).findFirst().orElse(null);
        if (ready(SMITE) && boss != null) {
            target = boss;
            return SMITE;
        }
        if (t != null && ready(CHAIN) && SiegeDamage.enemiesAround(level, t.position(), 6).size() >= 3) return CHAIN;
        return 0;
    }

    private boolean danger(ServerLevel level) {
        var d = SiegeManager.data(level);
        if (d.hasCore() && d.coreHp < d.maxHp() * 0.6f) return true;
        for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 20)) {
            if (a instanceof Player || a instanceof HeroEntity) {
                if (a.getHealth() < a.getMaxHealth() * 0.5f) return true;
            }
        }
        return false;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        setDeltaMovement(getDeltaMovement().scale(0.6));
        switch (skill) {
            case CHAIN -> {
                if (t == 14 && target != null) {
                    Set<LivingEntity> hit = new HashSet<>();
                    LivingEntity cur = target;
                    Vec3 from = position().add(0, getBbHeight(), 0);
                    for (int i = 0; i < 8 && cur != null; i++) {
                        hit.add(cur);
                        Vec3 to = cur.position().add(0, cur.getBbHeight() * 0.5, 0);
                        Fx.bolt(level, from, to, 0xF4F06A, 1.3f);
                        SiegeDamage.deal(cur, this, this, dmg(1.2f), Element.LIGHTNING, false);
                        SiegeDamage.apply(cur, ModEffects.SHOCKED.get(), 20, 0);
                        from = to;
                        LivingEntity c = cur;
                        cur = SiegeDamage.enemiesAround(level, to, 7).stream().filter(e -> !hit.contains(e))
                                .min(Comparator.comparingDouble(e -> e.distanceToSqr(c))).orElse(null);
                    }
                    playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5f, 1.2f);
                }
                if (t >= 24) {
                    cooldown(CHAIN, 140);
                    return true;
                }
            }
            case SMITE -> {
                if (target != null) faceTowards(target.position());
                if (t < 30 && t % 3 == 0) level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + getBbHeight() + 0.5, getZ(), 10, 0.3, 0.3, 0.3, 0.2);
                if (t == 30 && target != null) {
                    Vec3 at = target.position();
                    for (int i = 0; i < 3; i++) Fx.bolt(level, at.add(random.nextGaussian(), 40, random.nextGaussian()), at, 0xFFFFFF, 2.2f);
                    Fx.shockwave(level, at, 6, 0xF4F06A);
                    level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 2, 0, 0, 0, 0);
                    level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.WEATHER, 5f, 0.6f);
                    SiegeDamage.deal(target, this, this, dmg(8f), Element.LIGHTNING, false);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, at, 6)) {
                        if (e != target) SiegeDamage.deal(e, this, this, dmg(2f), Element.LIGHTNING, false);
                    }
                }
                if (t >= 40) {
                    cooldown(SMITE, 300);
                    return true;
                }
            }
            case AEGIS -> {
                if (t == 16) {
                    Fx.spiral(level, position(), 3, 4, Fx.dust(0xFFE38A, 1.6f), 4, 0);
                    playSound(SoundEvents.BEACON_ACTIVATE, 2f, 0.8f);
                    for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 20)) {
                        a.addEffect(new MobEffectInstance(ModEffects.AEGIS.get(), 200, 2));
                        Fx.line(level, position().add(0, getBbHeight(), 0), a.position().add(0, a.getBbHeight(), 0), Fx.dust(0xFFE38A, 0.7f), 0.8);
                    }
                    var d = SiegeManager.data(level);
                    if (d.hasCore()) SiegeManager.healCore(level, d.maxHp() * 0.08f);
                }
                if (t >= 26) {
                    cooldown(AEGIS, 600);
                    return true;
                }
            }
            case WRATH -> {
                if (t == 24) {
                    BlockPos core = SiegeManager.corePos(level);
                    if (core != null) HazardZone.spawn(level, HazardZone.Kind.STORM, Vec3.atBottomCenterOf(core), 14f, 200, dmg(0.5f), this, false);
                    level.setWeatherParameters(0, 400, true, true);
                    playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 4f, 0.5f);
                }
                if (t >= 36) {
                    cooldown(WRATH, 900);
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
        if (tickCount % 5 == 0) level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + getBbHeight() * 0.6, getZ(), 2, 0.5, 0.8, 0.5, 0.05);
    }
}
