package com.lastbastion.combat;

import com.lastbastion.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Single entry point for all mod damage. Resolves elemental resistances, status-effect
 * amplification, friendly-fire rules, invulnerability frames and kill-credit bookkeeping.
 */
public final class SiegeDamage {
    public static final String TAG_CREDIT = "lb_credit";
    public static final String TAG_KILLER = "lb_killer";
    public static final String TAG_ELEMENT = "lb_element";

    private SiegeDamage() {
    }

    /** Is {@code target} hostile to the defending side? */
    public static boolean isEnemy(Entity target) {
        return target instanceof SiegeEnemy || target instanceof Enemy;
    }

    /** Is {@code target} on the defending side (players, turrets, heroes)? */
    public static boolean isDefender(Entity target) {
        return target instanceof Player || target instanceof SiegeAlly;
    }

    public static boolean canHit(@Nullable Entity attacker, Entity target) {
        if (!(target instanceof LivingEntity living) || !living.isAlive()) return false;
        if (attacker == null) return true;
        if (attacker == target) return false;
        boolean attackerEnemy = isEnemy(attacker);
        if (attackerEnemy) return isDefender(target) || !isEnemy(target);
        if (target instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        // defenders never hurt each other
        return !isDefender(target);
    }

    @Nullable
    public static UUID creditOwner(@Nullable Entity e) {
        if (e instanceof Player p) return p.getUUID();
        if (e instanceof SiegeAlly a) return a.getOwnerId();
        return null;
    }

    public static float multiplier(LivingEntity target, Element element) {
        float m = 1f;
        if (target instanceof SiegeEnemy se) m *= se.elementMultiplier(element);
        MobEffectInstance soaked = target.getEffect(ModEffects.SOAKED.get());
        if (soaked != null) {
            if (element == Element.LIGHTNING) m *= 1.5f;
            if (element == Element.FIRE) m *= 0.5f;
        }
        MobEffectInstance marked = target.getEffect(ModEffects.MARKED.get());
        if (marked != null) m *= 1f + 0.15f * Math.min(3, marked.getAmplifier() + 1);
        MobEffectInstance corroded = target.getEffect(ModEffects.CORRODED.get());
        if (corroded != null) m *= 1f + 0.05f * Math.min(5, corroded.getAmplifier() + 1);
        if (element == Element.HOLY && target.hasEffect(ModEffects.PURIFIED.get())) m *= 1.25f;
        MobEffectInstance aegis = target.getEffect(ModEffects.AEGIS.get());
        if (aegis != null) m *= Math.max(0.4f, 1f - 0.2f * (aegis.getAmplifier() + 1));
        return m;
    }

    /**
     * Deal elemental damage.
     *
     * @param direct    the entity physically delivering the hit (projectile, turret, hero)
     * @param owner     the entity that should be credited (player, turret, hero). May equal direct.
     * @param knockback whether vanilla knockback should apply
     */
    public static boolean deal(LivingEntity target, @Nullable Entity direct, @Nullable Entity owner, float amount,
                               Element element, boolean knockback) {
        if (target.level().isClientSide || amount <= 0) return false;
        Entity responsible = owner != null ? owner : direct;
        if (!canHit(responsible, target)) return false;
        float m = multiplier(target, element);
        if (m <= 0f) {
            immuneFx(target);
            return false;
        }
        float dmg = amount * m;
        if ((element == Element.PHYSICAL || element == Element.EXPLOSIVE) && target.hasEffect(ModEffects.FROZEN.get())) {
            dmg *= 1.5f;
            target.removeEffect(ModEffects.FROZEN.get());
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ITEM_SNOWBALL, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 20, 0.4, 0.5, 0.4, 0.15);
                sl.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.8f, 1.4f);
            }
        }
        if (isEnemy(target)) {
            UUID credit = creditOwner(responsible);
            if (credit != null) target.getPersistentData().putUUID(TAG_CREDIT, credit);
            if (responsible != null) target.getPersistentData().putUUID(TAG_KILLER, responsible.getUUID());
        }
        target.getPersistentData().putString(TAG_ELEMENT, element.name());
        DamageSource source = source(target, direct, responsible, element);
        Vec3 motion = target.getDeltaMovement();
        target.invulnerableTime = 0;
        boolean hit = target.hurt(source, dmg);
        if (!knockback) {
            target.setDeltaMovement(motion);
        }
        return hit;
    }

    public static boolean deal(LivingEntity target, @Nullable Entity attacker, float amount, Element element) {
        return deal(target, attacker, attacker, amount, element, false);
    }

    /** Damage over time (bleed, burn, poison pools). Keeps previous kill-credit tags. */
    public static void dot(LivingEntity target, float amount, Element element) {
        if (target.level().isClientSide || !target.isAlive()) return;
        float dmg = amount * multiplier(target, element);
        if (dmg <= 0) return;
        Vec3 motion = target.getDeltaMovement();
        target.invulnerableTime = 0;
        target.hurt(target.damageSources().magic(), dmg);
        target.setDeltaMovement(motion);
    }

    private static DamageSource source(LivingEntity target, @Nullable Entity direct, @Nullable Entity responsible, Element element) {
        var sources = target.damageSources();
        if (element == Element.EXPLOSIVE) return sources.explosion(direct, responsible);
        if (element == Element.PHYSICAL) {
            if (responsible instanceof Player p) return sources.playerAttack(p);
            if (responsible instanceof LivingEntity le) return sources.mobAttack(le);
            return sources.generic();
        }
        if (direct != null || responsible != null) return sources.indirectMagic(direct != null ? direct : responsible, responsible);
        return sources.magic();
    }

    private static void immuneFx(LivingEntity target) {
        if (target.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + target.getBbHeight() + 0.2, target.getZ(), 3, 0.2, 0.1, 0.2, 0.0);
        }
    }

    /** Apply/stack a status effect. Stacks increase amplifier up to {@code maxAmp}. */
    public static void applyStacking(LivingEntity target, MobEffect effect, int duration, int maxAmp) {
        if (target instanceof SiegeEnemy se && se.immuneToStatus()) return;
        MobEffectInstance cur = target.getEffect(effect);
        int amp = cur == null ? 0 : Math.min(maxAmp, cur.getAmplifier() + 1);
        target.addEffect(new MobEffectInstance(effect, Math.max(duration, cur == null ? 0 : cur.getDuration()), amp, false, true));
    }

    public static void apply(LivingEntity target, MobEffect effect, int duration, int amp) {
        MobEffectInstance cur = target.getEffect(effect);
        if (cur != null && cur.getAmplifier() > amp && cur.getDuration() > duration) return;
        target.addEffect(new MobEffectInstance(effect, duration, Math.max(amp, cur == null ? 0 : cur.getAmplifier()), false, true));
    }

    public static List<LivingEntity> enemiesAround(ServerLevel level, Vec3 center, double radius) {
        return around(level, center, radius, SiegeDamage::isEnemy);
    }

    public static List<LivingEntity> defendersAround(ServerLevel level, Vec3 center, double radius) {
        return around(level, center, radius, e -> isDefender(e) && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
    }

    public static List<LivingEntity> around(ServerLevel level, Vec3 center, double radius, Predicate<LivingEntity> filter) {
        AABB box = new AABB(center, center).inflate(radius);
        double r2 = radius * radius;
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && filter.test(e))) {
            if (e.position().add(0, e.getBbHeight() * 0.5, 0).distanceToSqr(center) <= r2 + e.getBbWidth() * e.getBbWidth()) {
                out.add(e);
            }
        }
        return out;
    }

    /** Area damage with linear falloff to 50% at the edge. */
    public static int areaDamage(ServerLevel level, Vec3 center, double radius, float amount, Element element,
                                 @Nullable Entity direct, @Nullable Entity owner, boolean hitEnemies) {
        int hits = 0;
        List<LivingEntity> targets = hitEnemies ? enemiesAround(level, center, radius) : defendersAround(level, center, radius);
        for (LivingEntity e : targets) {
            double d = Math.sqrt(e.position().distanceToSqr(center));
            float falloff = (float) (1.0 - 0.5 * Math.min(1.0, d / Math.max(0.1, radius)));
            if (deal(e, direct, owner, amount * falloff, element, false)) hits++;
        }
        return hits;
    }

    public static void push(Entity e, Vec3 from, double strength, double lift) {
        Vec3 dir = e.position().subtract(from).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(e.getRandom().nextDouble() - 0.5, 0, e.getRandom().nextDouble() - 0.5);
        dir = dir.normalize().scale(strength);
        double resist = e instanceof LivingEntity le ? le.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) : 0;
        double f = Math.max(0.1, 1.0 - resist);
        e.setDeltaMovement(e.getDeltaMovement().add(dir.x * f, lift * f, dir.z * f));
        e.hurtMarked = true;
    }
}
