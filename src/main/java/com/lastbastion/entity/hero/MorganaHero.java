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

/** Morgana the Stormweaver: battle-mage with chain lightning, frost nova, blink and meteors. */
public class MorganaHero extends HeroEntity {
    public static final int CHAIN = 4, NOVA = 5, BLINK = 6, METEOR = 7;
    private Vec3 meteorAt = Vec3.ZERO;

    public MorganaHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.MORGANA);
    }

    @Override
    protected boolean isRanged() {
        return true;
    }

    @Override
    protected double attackRange() {
        return 18;
    }

    @Override
    protected double preferredRange() {
        return 11;
    }

    @Override
    protected int attackWindup() {
        return 10;
    }

    @Override
    protected int attackInterval() {
        return 20;
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        Vec3 from = position().add(0, getBbHeight() * 0.9, 0);
        SiegeProjectile p = SiegeProjectile.create(level, ProjKind.ARCANE_BOLT, this, from).payload(dmg(1f), Element.ARCANE);
        p.homing(t, 0.2f).aim(t.position().add(0, t.getBbHeight() * 0.5, 0), 1.2f).fire(level);
        playSound(SoundEvents.EVOKER_CAST_SPELL, 0.8f, 1.4f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        int close = SiegeDamage.enemiesAround(level, position(), 5).size();
        if (ready(BLINK) && close > 0 && getHealth() < getMaxHealth() * 0.6f) return BLINK;
        if (ready(NOVA) && close >= 2) return NOVA;
        if (t == null) return 0;
        int cluster = SiegeDamage.enemiesAround(level, t.position(), 5).size();
        if (ready(METEOR) && cluster >= 5 && dist < 22) return METEOR;
        if (ready(CHAIN) && cluster >= 2 && dist < 16) return CHAIN;
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case CHAIN -> {
                if (t == 12 && target != null) {
                    Set<LivingEntity> hit = new HashSet<>();
                    LivingEntity cur = target;
                    Vec3 from = position().add(0, getBbHeight() * 0.9, 0);
                    for (int i = 0; i < 6 && cur != null; i++) {
                        hit.add(cur);
                        Vec3 to = cur.position().add(0, cur.getBbHeight() * 0.5, 0);
                        Fx.bolt(level, from, to, 0xB08AFF, 1f);
                        SiegeDamage.deal(cur, this, this, dmg(2f), Element.LIGHTNING, false);
                        SiegeDamage.apply(cur, ModEffects.SHOCKED.get(), 15, 0);
                        from = to;
                        LivingEntity c = cur;
                        cur = SiegeDamage.enemiesAround(level, to, 6).stream().filter(e -> !hit.contains(e))
                                .min(Comparator.comparingDouble(e -> e.distanceToSqr(c))).orElse(null);
                    }
                    playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1f, 1.5f);
                }
                if (t >= 22) {
                    cooldown(CHAIN, 120);
                    return true;
                }
            }
            case NOVA -> {
                if (t == 12) {
                    for (double r = 1; r <= 5; r += 1) Fx.ring(level, position().add(0, 0.4, 0), r, ParticleTypes.SNOWFLAKE, (int) (r * 10));
                    playSound(SoundEvents.GLASS_BREAK, 1.5f, 0.7f);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 5)) {
                        SiegeDamage.deal(e, this, this, dmg(1.5f), Element.ICE, false);
                        SiegeDamage.apply(e, ModEffects.FROZEN.get(), 60, 0);
                    }
                }
                if (t >= 20) {
                    cooldown(NOVA, 200);
                    return true;
                }
            }
            case BLINK -> {
                if (t == 4) {
                    BlockPos core = SiegeManager.corePos(level);
                    Vec3 dir = core != null ? Vec3.atCenterOf(core).subtract(position()).multiply(1, 0, 1).normalize()
                            : forward(-1).subtract(position());
                    Vec3 dest = position().add(dir.scale(8));
                    level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1, getZ(), 30, 0.3, 0.6, 0.3, 0.1);
                    randomTeleport(dest.x, dest.y + 1, dest.z, false);
                    level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1, getZ(), 30, 0.3, 0.6, 0.3, 0.1);
                    playSound(SoundEvents.ENDERMAN_TELEPORT, 1f, 1.4f);
                }
                if (t >= 8) {
                    cooldown(BLINK, 160);
                    return true;
                }
            }
            case METEOR -> {
                if (t == 1 && target != null) meteorAt = target.position();
                if (t == 20) {
                    SiegeProjectile m = SiegeProjectile.create(level, ProjKind.METEOR, this, meteorAt.add(6, 26, 6));
                    m.payload(dmg(5f), Element.FIRE).area(5f);
                    m.gravity = 0;
                    m.onImpact = (p, at) -> HazardZone.spawn(level, HazardZone.Kind.FIRE, at, 4f, 100, dmg(0.2f), this, false);
                    m.aim(meteorAt, 1.3f).scale(4f).fire(level);
                    playSound(SoundEvents.BLAZE_SHOOT, 2f, 0.4f);
                }
                if (t >= 30) {
                    cooldown(METEOR, 400);
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
