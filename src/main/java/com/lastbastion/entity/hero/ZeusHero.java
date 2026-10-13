package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.projectile.SiegeProjectile.ProjKind;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Zeus, King of Olympus. He fights on foot like any hero (and can be hit like any hero): thunderbolt
 * javelins at range, Thunderclap with the Aegis when mobbed, Chain Storm through crowds, Olympian
 * Smite against bosses and Aegis of Olympus to shield the army. Only his ultimate, Wrath of the Sky,
 * takes him into the air - for about four seconds - before he comes down like a thunderbolt.
 */
public class ZeusHero extends HeroEntity {
    public static final int CLAP = 6, CHAIN = 7, SMITE = 8, AEGIS = 9;
    private int throwsMade;
    private Vec3 ultHome = Vec3.ZERO;

    public ZeusHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.ZEUS);
    }

    @Override
    public Role role() {
        return Role.RANGED;
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
        return 11;
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
    protected float retreatThreshold() {
        return 0.2f;
    }

    @Override
    protected float ultPassive() {
        return 0.12f;
    }

    @Override
    protected float ultGainPerDamage() {
        return 0.02f;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"KNEEL! The King of Olympus walks among you.", "Who calls upon Zeus? Speak quickly, mortal."};
            case ULT -> new String[]{"WRATH OF THE SKY!", "BEHOLD THE FURY OF OLYMPUS!"};
            case BOSS -> new String[]{"You dare stand against a god?", "Titans fell before me. You are nothing."};
            case LOW_HP -> new String[]{"By the Styx... these vermin bite.", "Ichor spilled? Unthinkable!"};
            case LEVEL_UP -> new String[]{"Olympus grows mightier!"};
            case RETREAT -> new String[]{"Even a god must regather his storm."};
            case IDLE -> new String[]{"*thunder rumbles faintly overhead*", "Mortals. Always waiting."};
            default -> new String[0];
        };
    }

    private Vec3 boltHand() {
        return position().add(0, getBbHeight() * 0.95, 0).add(Vec3.directionFromRotation(0, getYRot() - 70).scale(0.7));
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        throwsMade++;
        javelin(level, t, 1f);
        if (throwsMade % 4 == 0) {
            List<LivingEntity> o = enemiesNear(level, 22);
            o.remove(t);
            if (!o.isEmpty()) javelin(level, o.get(random.nextInt(o.size())), 0.8f);
        }
        playSound(SoundEvents.TRIDENT_THROW, 1.2f, 0.7f);
    }

    private void javelin(ServerLevel level, LivingEntity t, float mult) {
        Vec3 hand = boltHand();
        SiegeProjectile p = SiegeProjectile.create(level, ProjKind.JAVELIN, this, hand).payload(dmg(mult), Element.LIGHTNING)
                .status(ModEffects.SHOCKED, 12, 0);
        p.area(1.8f);
        p.onEntityHit = (pr, e) -> gainUlt(pr.damage * ultGainPerDamage());
        p.aim(t.position().add(0, t.getBbHeight() * 0.5, 0), 2.2f).scale(1.2f).fire(level);
        Vfx.flash(level, hand, 0xF4F06A, 0.7f, 4);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (ready(AEGIS) && needsAegis(level)) return AEGIS;
        if (ready(CLAP) && crowd(level, position(), 4) >= 2) return CLAP;
        if (t == null) return 0;
        if (ready(SMITE) && t instanceof SiegeEnemy se && se.isHighPriority() && dist < 24) return SMITE;
        if (ready(CHAIN) && crowd(level, t.position(), 7) >= 3 && dist < 18) return CHAIN;
        return 0;
    }

    private boolean needsAegis(ServerLevel level) {
        int hurt = 0;
        for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 14)) {
            if (!(a instanceof com.lastbastion.entity.turret.TurretEntity) && a.getHealth() < a.getMaxHealth() * 0.55f) hurt++;
        }
        var d = SiegeManager.data(level);
        return hurt >= 2 || (d.hasCore() && d.coreHp < d.maxHp() * 0.4f);
    }

    @Override
    protected boolean wantsUltimate(ServerLevel level, @Nullable LivingEntity t, double dist) {
        return t != null && (enemiesNear(level, 16).size() >= 7 || (t instanceof SiegeEnemy se && se.isHighPriority() && dist < 16));
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case CLAP -> {
                if (t == 12) {
                    Fx.shockwave(level, position(), 4.5, 0xF4F06A);
                    for (int i = 0; i < 6; i++) {
                        double a = Math.PI * 2 * i / 6;
                        Vfx.arc(level, position().add(0, 0.4, 0), position().add(Math.cos(a) * 4.5, 0.2, Math.sin(a) * 4.5), 0xF4F06A, 0.06f, 6, 0.4f);
                    }
                    playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.5f, 0.8f);
                    playSound(SoundEvents.SHIELD_BLOCK, 1.5f, 0.6f);
                    for (LivingEntity e : enemiesNear(level, 4.5)) {
                        hit(e, dmg(1.6f), Element.LIGHTNING);
                        SiegeDamage.apply(e, ModEffects.SHOCKED.get(), 25, 0);
                        if (!(e instanceof SiegeEnemy se && se.isHighPriority())) SiegeDamage.push(e, position(), 1.4, 0.4);
                    }
                }
                if (t >= 20) {
                    cooldown(CLAP, 160);
                    return true;
                }
            }
            case CHAIN -> {
                if (target != null) faceTowards(target.position());
                if (t == 14 && target != null) {
                    Set<LivingEntity> hits = new HashSet<>();
                    LivingEntity cur = target;
                    Vec3 from = boltHand();
                    for (int i = 0; i < 9 && cur != null; i++) {
                        hits.add(cur);
                        Vec3 to = cur.position().add(0, cur.getBbHeight() * 0.5, 0);
                        Vfx.arc(level, from, to, 0xF4F06A, 0.09f, 7, 0.8f);
                        Vfx.spray(level, FxStyle.SPARK, to, 0xFFFFFF, 0xF4F06A, 0.12f, 6, 0.4f, 0.3f);
                        hit(cur, dmg(2f), Element.LIGHTNING);
                        SiegeDamage.apply(cur, ModEffects.SHOCKED.get(), 18, 0);
                        from = to;
                        LivingEntity c = cur;
                        cur = SiegeDamage.enemiesAround(level, to, 7).stream().filter(e -> !hits.contains(e))
                                .min(Comparator.comparingDouble(e -> e.distanceToSqr(c))).orElse(null);
                    }
                    playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2f, 1.4f);
                }
                if (t >= 24) {
                    cooldown(CHAIN, 160);
                    return true;
                }
            }
            case SMITE -> {
                if (target != null) faceTowards(target.position());
                if (t == 4 && target != null) {
                    Vfx.teleCircle(level, target.position().add(0, 0.05, 0), 2.2f + target.getBbWidth() * 0.5f, 18, 0xF4F06A, false);
                    playSound(SoundEvents.BEACON_POWER_SELECT, 1.5f, 1.6f);
                }
                if (t == 22 && target != null && target.isAlive()) {
                    Vec3 at = target.position();
                    for (int i = 0; i < 3; i++) Vfx.lightning(level, at.add(random.nextGaussian() * 0.6, 0, random.nextGaussian() * 0.6), 0xFFFFFF, 2f);
                    Vfx.screenFlash(level, at, 0xFFFFE8, 0.3f, 30, 6);
                    hit(target, dmg(6f) * (target instanceof SiegeEnemy se && se.isHighPriority() ? 1.5f : 1f), Element.LIGHTNING);
                    SiegeDamage.apply(target, ModEffects.SHOCKED.get(), 40, 0);
                    playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 3f, 0.8f);
                }
                if (t >= 30) {
                    cooldown(SMITE, 300);
                    return true;
                }
            }
            case AEGIS -> {
                if (t == 12) {
                    Vfx.dome(level, position(), 0xF4F06A, 4f, 50, this);
                    Vfx.ring(level, position().add(0, 0.05, 0), 0xF4F06A, 14, 22, 1);
                    playSound(SoundEvents.BEACON_ACTIVATE, 2f, 1.2f);
                    for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 14)) {
                        a.addEffect(new MobEffectInstance(ModEffects.AEGIS.get(), 200, 1));
                        a.heal(a.getMaxHealth() * 0.15f);
                    }
                    BlockPos core = SiegeManager.corePos(level);
                    if (core != null && core.closerToCenterThan(position(), 16)) {
                        var d = SiegeManager.data(level);
                        d.shield = Math.max(d.shield, Math.min(d.maxHp() * 0.15f, d.shield + d.maxHp() * 0.1f));
                        d.setDirty();
                        Vfx.dome(level, Vec3.atBottomCenterOf(core), 0xF4F06A, 3.2f, 60, null);
                    }
                }
                if (t >= 22) {
                    cooldown(AEGIS, 600);
                    return true;
                }
            }
            case ULT -> {
                if (t == 1) {
                    ultHome = position();
                    setNoGravity(true);
                    Vfx.ring(level, position().add(0, 0.05, 0), 0xF4F06A, 6, 14, 1.2f);
                    Vfx.spray(level, FxStyle.SMOKE, position().add(0, 0.2, 0), 0xE0E0E8, 0xA0A0B0, 1.1f, 14, 0.25f, -1);
                    playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 2.5f, 0.6f);
                }
                if (t <= 14) {
                    setDeltaMovement(0, 0.42, 0);
                    hasImpulse = true;
                } else if (t < 92) {
                    setDeltaMovement(getDeltaMovement().multiply(0.4, 0, 0.4));
                    if (target != null) faceTowards(target.position());
                    if (t % 6 == 0) {
                        List<LivingEntity> foes = enemiesNear(level, 18);
                        if (!foes.isEmpty()) {
                            LivingEntity e = foes.get(random.nextInt(foes.size()));
                            Vfx.arc(level, boltHand(), e.position().add(0, e.getBbHeight() * 0.5, 0), 0xF4F06A, 0.1f, 6, 0.9f);
                            Vfx.lightning(level, e.position(), 0xF4F06A, 1.3f);
                            hit(e, dmg(3f), Element.LIGHTNING);
                            SiegeDamage.apply(e, ModEffects.SHOCKED.get(), 20, 0);
                            playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.4f, 1f);
                        }
                    }
                    if (t % 20 == 0) Vfx.sigil(level, ultHome.add(0, 0.05, 0), 0xF4F06A, 5, 22, 6f);
                } else if (t == 92) {
                    setNoGravity(false);
                    setDeltaMovement(0, -1.6, 0);
                    hasImpulse = true;
                }
                if (t > 92 && (onGround() || t > 120)) {
                    Fx.shockwave(level, position(), 6, 0xF4F06A);
                    Vfx.lightning(level, position(), 0xFFFFFF, 2.2f);
                    Vfx.screenFlash(level, position(), 0xFFFFE8, 0.3f, 30, 8);
                    for (LivingEntity e : enemiesNear(level, 6)) {
                        hit(e, dmg(4f), Element.LIGHTNING);
                        SiegeDamage.apply(e, ModEffects.SHOCKED.get(), 30, 0);
                        if (!(e instanceof SiegeEnemy se && se.isHighPriority())) SiegeDamage.push(e, position(), 1.5, 0.6);
                    }
                    playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 3f, 0.5f);
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
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }
}
