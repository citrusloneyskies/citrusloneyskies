package com.tokyoghoul.combat;

import com.tokyoghoul.TokyoGhoul;
import com.tokyoghoul.data.Faction;
import com.tokyoghoul.move.Anims;
import com.tokyoghoul.move.MoveDef;
import com.tokyoghoul.move.MoveRunner;
import com.tokyoghoul.network.Sync;
import com.tokyoghoul.world.TGGameRules;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The hit core, Jujutsu Craft's RangeAttackProcedure rebuilt: faction checks (LogicAttack), guard
 * reduction and guard break, dodge frames, counter stances, hit-stun that interrupts the victim's move,
 * velocity knockback, and impact effects. Every move and projectile funnels its damage through here.
 */
public final class Combat {
    public static final ResourceKey<DamageType> KAGUNE = key("kagune");
    public static final ResourceKey<DamageType> QUINQUE = key("quinque");
    public static final ResourceKey<DamageType> STRIKE = key("strike");

    private Combat() {
    }

    private static ResourceKey<DamageType> key(String id) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(TokyoGhoul.MODID, id));
    }

    public static DamageSource source(LivingEntity attacker, MoveDef.Source src) {
        ResourceKey<DamageType> k = switch (src) {
            case KAGUNE, FIRE -> KAGUNE;
            case QUINQUE -> QUINQUE;
            case LIGHTNING -> Fighters.race(attacker).hasKagune ? KAGUNE : QUINQUE;
            case BODY -> STRIKE;
        };
        return new DamageSource(attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(k), attacker);
    }

    /** Is {@code a} engaged with {@code b} (targeting it or recently hurt by it)? */
    public static boolean engaged(LivingEntity a, LivingEntity b) {
        if (a instanceof Mob m && m.getTarget() == b) return true;
        if (a.getLastHurtByMob() == b && a.tickCount - a.getLastHurtByMobTimestamp() < 200) return true;
        return a.getLastHurtMob() == b && a.tickCount - a.getLastHurtMobTimestamp() < 200;
    }

    /** JJK's LogicAttackProcedure: may {@code attacker}'s techniques hurt {@code target}? */
    public static boolean canHurt(LivingEntity attacker, LivingEntity target) {
        if (attacker == target || !target.isAlive() || target.isInvulnerable() || target instanceof ArmorStand) return false;
        if (target instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        if (attacker instanceof Player && target instanceof Player) {
            return attacker.level().getGameRules().getBoolean(TGGameRules.PVP) && (attacker.getTeam() == null || !attacker.isAlliedTo(target));
        }
        if (target instanceof TamableAnimal t && t.getOwner() == attacker) return false;
        if (attacker instanceof TamableAnimal t && t.getOwner() == target) return false;
        Faction fa = Fighters.faction(attacker), ft = Fighters.faction(target);
        if (fa != null && ft != null) {
            if (fa == ft) return false;
            return fa.canHurt(ft) || engaged(attacker, target) || engaged(target, attacker);
        }
        if (target instanceof Enemy) return true;
        return engaged(attacker, target) || engaged(target, attacker);
    }

    /** All living entities in a sphere that the attacker may hit. */
    public static List<LivingEntity> targetsIn(LivingEntity attacker, Vec3 center, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        AABB box = new AABB(center, center).inflate(radius + 1.5);
        for (LivingEntity e : attacker.level().getEntitiesOfClass(LivingEntity.class, box, e -> canHurt(attacker, e))) {
            Vec3 c = e.getBoundingBox().getCenter();
            double r = radius + e.getBbWidth() * 0.5;
            if (c.distanceToSqr(center) <= r * r || e.getBoundingBox().inflate(radius * 0.5).contains(center)) out.add(e);
        }
        return out;
    }

    /** All hittable entities within {@code width} of the segment from..to. */
    public static List<LivingEntity> targetsAlong(LivingEntity attacker, Vec3 from, Vec3 to, double width) {
        List<LivingEntity> out = new ArrayList<>();
        AABB box = new AABB(from, to).inflate(width + 1.5);
        Vec3 d = to.subtract(from);
        double len2 = Math.max(1e-6, d.lengthSqr());
        for (LivingEntity e : attacker.level().getEntitiesOfClass(LivingEntity.class, box, e -> canHurt(attacker, e))) {
            Vec3 c = e.getBoundingBox().getCenter();
            double t = Math.max(0, Math.min(1, c.subtract(from).dot(d) / len2));
            Vec3 closest = from.add(d.scale(t));
            double r = width + e.getBbWidth() * 0.6;
            if (closest.distanceToSqr(c) <= r * r + e.getBbHeight() * 0.25) out.add(e);
        }
        return out;
    }

    /**
     * Apply one hit. Returns true if it landed (not blocked by dodge frames). Damage is scaled by the
     * attacker's level and form; guard, counters, ghoul toughness and transformations are applied here.
     */
    public static boolean hit(LivingEntity attacker, LivingEntity target, MoveDef move, float baseDamage, Vec3 knock, int stun) {
        Level level = attacker.level();
        if (level.isClientSide) return false;
        FighterState ts = Fighters.state(target);
        Vec3 at = target.getBoundingBox().getCenter();
        int color = move.color >= 0 ? move.color : (move.source == MoveDef.Source.KAGUNE ? Fighters.kaguneColor(attacker) : 0xE0E0E0);

        if (ts != null && ts.dodgeTicks > 0) {
            Fx.particles(level, net.minecraft.core.particles.ParticleTypes.CLOUD, at, 4, 0.3, 0.02);
            return false;
        }
        float dmg = baseDamage * Fighters.damageMult(attacker) * Fighters.armorMult(target);
        boolean guarded = false;
        if (ts != null && ts.guardingNow()) {
            Vec3 toAttacker = attacker.position().subtract(target.position()).normalize();
            Vec3 facing = Vec3.directionFromRotation(0, target.getYRot());
            if (facing.dot(toAttacker) > -0.1) {
                guarded = true;
                if (ts.counter != null && ts.counterTicks > 0) {
                    MoveRunner.counter(target, attacker, ts.counter);
                    ts.counterTicks = 0;
                    Fx.sound(level, at, SoundEvents.ANVIL_LAND, 0.7f, 1.6f);
                    return false;
                }
                dmg *= 0.2f;
                knock = knock.scale(0.25);
                stun = 0;
                Fx.sound(level, at, SoundEvents.SHIELD_BLOCK, 1f, 0.9f + level.random.nextFloat() * 0.2f);
                Fx.particles(level, net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT, at, 8, 0.3, 0.2);
                if (target instanceof Player p && !Fighters.spendRc(p, baseDamage * 2)) {
                    // guard break: out of Rc
                    ts.guarding = false;
                    ts.guardTicks = 0;
                    ts.stun = 30;
                    Sync.anim(target, Anims.STAGGER);
                    Fx.sound(level, at, SoundEvents.SHIELD_BREAK, 1f, 1f);
                }
            }
        }

        target.invulnerableTime = 0;
        float before = target.getHealth();
        boolean landed = target.hurt(source(attacker, move.source), Math.max(0.5f, dmg));
        target.invulnerableTime = 0;
        if (!landed && target.getHealth() >= before) return false;

        if (move.source == MoveDef.Source.FIRE) target.setSecondsOnFire(4);
        double resist = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        if (knock.lengthSqr() > 1e-4) {
            Vec3 k = knock.scale(1 - Math.min(0.9, resist));
            if (ts != null && ts.transformed()) k = k.scale(0.4);
            target.setDeltaMovement(target.getDeltaMovement().scale(0.2).add(k));
            target.hurtMarked = true;
        }
        if (!guarded && stun > 0) applyStun(target, ts, stun);
        Fx.hit(level, at, color, dmg >= 8 || knock.lengthSqr() > 1.2);
        Fx.sound(level, at, dmg >= 8 ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8f,
                0.8f + level.random.nextFloat() * 0.4f);
        // feed Rc to the attacker on hits (players regain a little energy by fighting)
        Fighters.addRc(attacker, Math.min(4, dmg * 0.2));
        return true;
    }

    /** Hit-stun: interrupts the victim's move unless transformed, like JJK's attack trading. */
    public static void applyStun(LivingEntity target, FighterState ts, int stun) {
        if (ts != null) {
            if (ts.transformed() && stun < 20) return;
            ts.stun = Math.max(ts.stun, stun);
            if (ts.move != null && !ts.move.ultimate) {
                MoveRunner.interrupt(target, ts);
            }
            ts.guarding = false;
            ts.guardTicks = 0;
            Sync.anim(target, Anims.HURT);
        } else {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stun, 3, false, false));
        }
    }

    /** Knockback vector away from a point, with some lift. */
    public static Vec3 away(Vec3 from, LivingEntity target, float strength, float lift) {
        Vec3 d = target.position().subtract(from);
        d = new Vec3(d.x, 0, d.z);
        if (d.lengthSqr() < 1e-4) d = new Vec3(0, 0, 1);
        return d.normalize().scale(strength).add(0, lift, 0);
    }
}
