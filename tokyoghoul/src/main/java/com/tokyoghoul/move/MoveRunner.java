package com.tokyoghoul.move;

import com.tokyoghoul.combat.Combat;
import com.tokyoghoul.combat.FighterState;
import com.tokyoghoul.combat.Fighters;
import com.tokyoghoul.combat.Fx;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.entity.GroundSpike;
import com.tokyoghoul.entity.RcProjectile;
import com.tokyoghoul.network.Sync;
import com.tokyoghoul.player.PlayerCaps;
import com.tokyoghoul.player.TGPlayerData;
import com.tokyoghoul.registry.TGEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * Runs moves tick by tick for players and NPCs. Starting a move is Jujutsu Craft's
 * StartCursedTechniqueProcedure (cost, cooldown, skill id, effect); ticking it is the per-technique
 * procedure driven by the CURSED_TECHNIQUE effect each tick, generalised over {@link MoveDef.Kind}.
 */
public final class MoveRunner {
    private MoveRunner() {
    }

    /** Why a move could not start (shown to players on the action bar). */
    public enum Fail {NONE, BUSY, COOLDOWN, RANK, KAKUJA, RC, SUPPRESSED, STUNNED}

    public static Fail canStart(LivingEntity user, MoveDef def) {
        FighterState s = Fighters.state(user);
        if (s == null) return Fail.BUSY;
        long now = user.level().getGameTime();
        if (s.stun > 0) return Fail.STUNNED;
        if (s.busy() || now < s.globalReady) return Fail.BUSY;
        if (s.onCooldown(def, now)) return Fail.COOLDOWN;
        if (def.source == MoveDef.Source.KAGUNE && user.hasEffect(TGEffects.SUPPRESSED.get())) return Fail.SUPPRESSED;
        if (user instanceof Player p && !p.getAbilities().instabuild) {
            TGPlayerData d = PlayerCaps.get(p);
            if (d.level < def.minRank) return Fail.RANK;
            if (def.needsKakuja && d.kakuja < 100) return Fail.KAKUJA;
            if (d.rc < def.cost) return Fail.RC;
        }
        return Fail.NONE;
    }

    /** Start {@code def}. NPCs pass their target; players aim with the camera. */
    public static Fail start(LivingEntity user, MoveDef def, @Nullable LivingEntity target) {
        Fail f = canStart(user, def);
        if (f != Fail.NONE) return f;
        FighterState s = Fighters.state(user);
        Fighters.spendRc(user, def.cost);
        long now = user.level().getGameTime();
        s.cooldowns.put(def.id, now + def.cooldown);
        s.globalReady = now + 4;
        s.move = def;
        s.moveTick = 0;
        s.struck.clear();
        s.grabbed = null;
        s.moveTarget = target;
        s.holding = user instanceof Player;
        s.guarding = false;
        s.aim = aimOf(user, target);
        if (def.source == MoveDef.Source.KAGUNE || def.source == MoveDef.Source.FIRE && Fighters.race(user).hasKagune) {
            kaguneOut(user, s, 100);
        }
        Sync.anim(user, def.anim);
        if (user instanceof Player p) Sync.playerData(p);
        return Fail.NONE;
    }

    public static void kaguneOut(LivingEntity user, FighterState s, int ticks) {
        CharacterDef d = Fighters.def(user);
        if (d == null || !d.hasKagune()) return;
        boolean was = s.kaguneTicks > 0 || s.kaguneToggled;
        s.kaguneTicks = Math.max(s.kaguneTicks, ticks);
        if (!was) {
            Fx.kaguneBurst(user.level(), user.position().add(0, 1.1, 0), d.kaguneColor);
            Fx.sound(user.level(), user.position(), SoundEvents.SLIME_SQUISH, 1f, 0.6f);
            Sync.visual(user);
        }
    }

    public static void interrupt(LivingEntity user, FighterState s) {
        if (s.grabbed != null) s.grabbed.setNoGravity(false);
        s.clearMove();
        Sync.anim(user, Anims.NONE);
    }

    /** Aim from the user toward the target, or the player's look. */
    public static Vec3 aimOf(LivingEntity user, @Nullable LivingEntity target) {
        if (target != null && target.isAlive()) {
            Vec3 d = target.getBoundingBox().getCenter().subtract(user.getEyePosition());
            if (d.lengthSqr() > 1e-4) return d.normalize();
        }
        return user.getLookAngle();
    }

    static Vec3 flat(Vec3 v) {
        Vec3 f = new Vec3(v.x, 0, v.z);
        return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    static void face(LivingEntity user, Vec3 dir) {
        if (user instanceof Player) return;
        float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90f;
        user.setYRot(yaw);
        user.setYHeadRot(yaw);
        user.yBodyRot = yaw;
    }

    // ------------------------------------------------------------------ per tick

    /** Called every server tick for every fighter. */
    public static void tick(LivingEntity user) {
        FighterState s = Fighters.state(user);
        if (s == null) return;
        tickTimers(user, s);
        if (s.move == null) return;
        if (!user.isAlive()) {
            interrupt(user, s);
            return;
        }
        MoveDef d = s.move;
        if (user instanceof Player) s.aim = user.getLookAngle();
        else if (s.moveTarget != null && s.moveTarget.isAlive() && d.kind != MoveDef.Kind.DASH) s.aim = aimOf(user, s.moveTarget);
        if (!(user instanceof Player)) face(user, s.aim);
        user.fallDistance = 0;
        boolean done;
        try {
            done = run(user, s, d, s.moveTick);
        } catch (RuntimeException e) {
            com.tokyoghoul.util.Safe.report("MoveRunner." + d.id, e);
            done = true;
        }
        s.moveTick++;
        if (done || s.moveTick > d.busyTicks() + (d.hold ? d.duration * 2 : 0) + 40) {
            if (s.grabbed != null) s.grabbed.setNoGravity(false);
            s.clearMove();
            if (Anims.held(d.anim)) Sync.anim(user, Anims.NONE);
        }
    }

    private static void tickTimers(LivingEntity user, FighterState s) {
        if (s.stun > 0) {
            s.stun--;
            user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2, 2, false, false));
        }
        if (s.guardTicks > 0) s.guardTicks--;
        if (s.counterTicks > 0 && --s.counterTicks == 0) s.counter = null;
        if (s.dodgeTicks > 0) s.dodgeTicks--;
        if (s.buffTicks > 0 && --s.buffTicks == 0) s.buff = null;
        if (s.formTicks > 0) {
            if (user.tickCount % 10 == 0) user.heal(1f);
            if (--s.formTicks == 0) {
                s.form = null;
                Sync.visual(user);
            }
        }
        if (s.kaguneTicks > 0 && --s.kaguneTicks == 0 && !s.kaguneToggled) Sync.visual(user);
    }

    /** Run one tick of a move. Returns true when finished. */
    private static boolean run(LivingEntity u, FighterState s, MoveDef d, int t) {
        Level level = u.level();
        int color = d.color >= 0 ? d.color : (d.source == MoveDef.Source.QUINQUE ? 0xD8E8FF : d.source == MoveDef.Source.FIRE ? 0xFF6A1A
                : d.source == MoveDef.Source.LIGHTNING ? 0xA8D8FF : d.source == MoveDef.Source.BODY ? 0xE8E8E8 : Fighters.kaguneColor(u));
        Vec3 aim = s.aim;
        Vec3 eye = u.getEyePosition();
        Vec3 chest = u.position().add(0, u.getBbHeight() * 0.6, 0);
        switch (d.kind) {
            case STRIKE -> {
                if (t < d.windup) {
                    slow(u);
                    return false;
                }
                if (t == d.windup) {
                    if (d.lunge) push(u, flat(aim).scale(0.7));
                    Vec3 c = chest.add(aim.scale(d.range * 0.6));
                    for (LivingEntity e : Combat.targetsIn(u, c, d.radius + 0.4)) {
                        Combat.hit(u, e, d, d.damage, flat(aim).scale(d.knockback).add(0, d.lift, 0), stunFor(d));
                    }
                    swing(level, c, color, d.knockback > 1.2f);
                }
                return t >= d.windup + 8;
            }
            case COMBO -> {
                if (t < d.windup) return false;
                int k = t - d.windup;
                if (k % d.interval == 0 && k / d.interval < d.count) {
                    int i = k / d.interval;
                    boolean last = i == d.count - 1;
                    if (i > 0) Sync.anim(u, d.anim == Anims.JAB ? (last ? Anims.HEAVY : Anims.JAB) : d.anim);
                    push(u, flat(aim).scale(0.25));
                    Vec3 c = chest.add(aim.scale(d.range * 0.6));
                    for (LivingEntity e : Combat.targetsIn(u, c, d.radius + 0.4)) {
                        Vec3 kb = last ? flat(aim).scale(d.knockback * 3).add(0, 0.3, 0) : flat(aim).scale(d.knockback * 0.4);
                        Combat.hit(u, e, d, d.damage * (last ? 1.5f : 1f), kb, last ? 12 : 8);
                    }
                    swing(level, c, color, last);
                }
                return k >= d.count * d.interval + 2;
            }
            case BARRAGE -> {
                if (t < d.windup) return false;
                int k = t - d.windup;
                boolean extend = s.holding && u instanceof Player && k < d.duration * 2 && Fighters.spendRc(u, 0.4);
                if (k >= d.duration && !extend) {
                    // final blow
                    Vec3 c = chest.add(aim.scale(d.range * 0.6));
                    for (LivingEntity e : Combat.targetsIn(u, c, d.radius + 0.5)) {
                        Combat.hit(u, e, d, d.damage * 4, flat(aim).scale(1.6).add(0, 0.35, 0), 14);
                    }
                    swing(level, c, color, true);
                    Sync.anim(u, Anims.HEAVY);
                    return true;
                }
                slow(u);
                if (k % d.interval == 0) {
                    Vec3 c = chest.add(aim.scale(d.range * 0.6));
                    for (LivingEntity e : Combat.targetsIn(u, c, d.radius + 0.4)) {
                        Combat.hit(u, e, d, d.damage, flat(aim).scale(0.12), 6);
                    }
                    Fx.particles(level, ParticleTypes.CRIT, c, 3, 0.4, 0.2);
                    if (d.source == MoveDef.Source.KAGUNE) Fx.line(level, chest, c.add(aim.scale(1.5)), color, 0.9f, 0.5);
                    if (k % 4 == 0) Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_WEAK, 0.7f, 1.2f + level.random.nextFloat() * 0.4f);
                }
                return false;
            }
            case PIERCE -> {
                if (t < d.windup) {
                    slow(u);
                    return false;
                }
                int k = t - d.windup;
                if (k % d.interval == 0 && k / d.interval < d.count) {
                    if (k > 0) Sync.anim(u, d.anim);
                    Vec3 from = chest;
                    Vec3 to = clip(u, from, from.add(aim.scale(d.range)));
                    for (LivingEntity e : Combat.targetsAlong(u, from, to, d.radius)) {
                        Combat.hit(u, e, d, d.damage, aim.scale(d.knockback).add(0, d.lift, 0), stunFor(d));
                    }
                    Fx.line(level, from, to, color, 1.4f, 0.35);
                    Fx.sound(level, to, SoundEvents.TRIDENT_THROW, 0.9f, 0.8f + level.random.nextFloat() * 0.3f);
                }
                return k >= d.count * d.interval + 4;
            }
            case SWEEP -> {
                if (t < d.windup) {
                    slow(u);
                    return false;
                }
                int k = t - d.windup;
                if (k % d.interval == 0 && k / d.interval < d.count) {
                    if (k > 0) Sync.anim(u, d.anim);
                    for (LivingEntity e : Combat.targetsIn(u, u.position().add(0, 0.8, 0), d.range)) {
                        Combat.hit(u, e, d, d.damage, Combat.away(u.position(), e, d.knockback, d.lift + 0.2f), stunFor(d));
                    }
                    Fx.ring(level, u.position(), d.range * 0.8, color);
                    Fx.particles(level, ParticleTypes.SWEEP_ATTACK, chest, 6, d.range * 0.4, 0.1, d.range * 0.4, 0);
                    Fx.sound(level, chest, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.7f);
                }
                return k >= d.count * d.interval + 4;
            }
            case VOLLEY -> {
                if (t < d.windup) return false;
                int k = t - d.windup;
                if (k % d.interval == 0 && k / d.interval < d.count) {
                    int i = k / d.interval;
                    Vec3 side = flat(aim).yRot((float) Math.PI / 2).scale((i % 2 == 0 ? 0.6 : -0.6));
                    Vec3 origin = d.shot == MoveDef.Shot.SHARD ? chest.add(side).add(0, 0.4, 0) : eye.add(aim.scale(0.6));
                    Vec3 dir = aim.add(rand(u, 0.07)).normalize();
                    RcProjectile.fire(u, d, origin, dir, d.speed, d.damage, color);
                    if (i % 2 == 0) Fx.sound(level, origin, d.shot == MoveDef.Shot.KNIFE ? SoundEvents.TRIDENT_THROW : SoundEvents.AMETHYST_BLOCK_CHIME,
                            0.8f, 1.3f + level.random.nextFloat() * 0.4f);
                }
                return k >= d.count * d.interval + 4;
            }
            case STORM -> {
                if (t < d.windup) {
                    slow(u);
                    if (t % 3 == 0) Fx.particles(level, Fx.dust(color, 1.5f), chest, 8, 1.0, 0.05);
                    return false;
                }
                if (t == d.windup) {
                    for (int i = 0; i < d.count; i++) {
                        double a = i * Math.PI * 2 / d.count + level.random.nextDouble() * 0.2;
                        double pitch = (level.random.nextDouble() - 0.35) * 0.6;
                        Vec3 dir = new Vec3(Math.cos(a), pitch, Math.sin(a)).normalize();
                        RcProjectile.fire(u, d, chest.add(dir.scale(0.8)), dir, d.speed, d.damage, color);
                    }
                    Fx.ring(level, u.position(), 2.5, color);
                    Fx.sound(level, chest, SoundEvents.AMETHYST_BLOCK_BREAK, 1.4f, 0.7f);
                    Fx.sound(level, chest, SoundEvents.FIREWORK_ROCKET_BLAST, 1f, 0.6f);
                }
                return t >= d.windup + 10;
            }
            case DASH -> {
                if (t < d.windup) {
                    slow(u);
                    return false;
                }
                int k = t - d.windup;
                if (k < d.duration) {
                    Vec3 v = flat(aim).scale(d.speed);
                    u.setDeltaMovement(v.x, Math.min(u.getDeltaMovement().y, 0.1), v.z);
                    u.hurtMarked = true;
                    for (LivingEntity e : Combat.targetsIn(u, u.position().add(0, 0.9, 0).add(flat(aim).scale(0.8)), d.radius + 0.6)) {
                        if (s.struck.add(e.getId())) {
                            Vec3 kb = flat(aim).scale(d.knockback).add(flat(aim).yRot((float) Math.PI / 2).scale(0.3)).add(0, d.lift + 0.2, 0);
                            Combat.hit(u, e, d, d.damage, kb, stunFor(d));
                        }
                    }
                    Fx.particles(level, Fx.dust(color, 1.3f), chest, 4, 0.3, 0.02);
                    if (k % 3 == 0) Fx.particles(level, ParticleTypes.CLOUD, u.position(), 2, 0.2, 0.01);
                    return false;
                }
                u.setDeltaMovement(u.getDeltaMovement().multiply(0.3, 1, 0.3));
                u.hurtMarked = true;
                return k >= d.duration + 3;
            }
            case LEAP_SLAM -> {
                if (t < d.windup) {
                    slow(u);
                    return false;
                }
                if (t == d.windup) {
                    Vec3 dest = s.moveTarget != null && s.moveTarget.isAlive() ? s.moveTarget.position()
                            : u.position().add(flat(aim).scale(Math.max(4, d.range * 2)));
                    Vec3 dlt = dest.subtract(u.position());
                    double dist = Math.min(18, Math.sqrt(dlt.x * dlt.x + dlt.z * dlt.z));
                    Vec3 h = flat(dlt).scale(dist / 16.0);
                    u.setDeltaMovement(h.x, 0.95, h.z);
                    u.hurtMarked = true;
                    Fx.debris(level, u, 12);
                    Fx.sound(level, u.position(), SoundEvents.RAVAGER_STEP, 1f, 0.8f);
                    Sync.anim(u, Anims.LEAP);
                    return false;
                }
                int k = t - d.windup;
                if (k > 4 && (u.onGround() || k >= d.duration - d.windup)) {
                    Sync.anim(u, Anims.SLAM);
                    for (LivingEntity e : Combat.targetsIn(u, u.position().add(0, 0.5, 0), d.radius)) {
                        Combat.hit(u, e, d, d.damage, Combat.away(u.position(), e, d.knockback, 0.55f), 16);
                    }
                    Fx.ring(level, u.position(), d.radius, color);
                    Fx.debris(level, u, 40);
                    Fx.particles(level, ParticleTypes.EXPLOSION, u.position(), 2, 0.6, 0);
                    Fx.sound(level, u.position(), SoundEvents.GENERIC_EXPLODE, 1f, 1.1f);
                    return true;
                }
                if (k > 2 && s.moveTarget != null && s.moveTarget.isAlive()) {
                    // steer slightly toward the target while airborne
                    Vec3 to = flat(s.moveTarget.position().subtract(u.position())).scale(0.05);
                    u.setDeltaMovement(u.getDeltaMovement().add(to.x, 0, to.z));
                }
                return false;
            }
            case GRAB -> {
                if (t < d.windup) {
                    slow(u);
                    return false;
                }
                if (t == d.windup) {
                    Vec3 to = clip(u, chest, chest.add(aim.scale(d.range)));
                    Fx.line(level, chest, to, color, 1.4f, 0.4);
                    List<LivingEntity> hits = Combat.targetsAlong(u, chest, to, 1.2);
                    hits.sort(Comparator.comparingDouble(e -> e.distanceToSqr(u)));
                    if (hits.isEmpty()) {
                        Sync.anim(u, Anims.NONE);
                        return false;
                    }
                    s.grabbed = hits.get(0);
                    FighterState gs = Fighters.state(s.grabbed);
                    if (gs != null && gs.dodgeTicks > 0) {
                        s.grabbed = null;
                        return false;
                    }
                    Combat.applyStun(s.grabbed, gs, d.duration + 10);
                    Fx.sound(level, s.grabbed.position(), SoundEvents.SLIME_ATTACK, 1f, 0.6f);
                    return false;
                }
                int k = t - d.windup;
                if (s.grabbed == null) return k >= 6;
                LivingEntity g = s.grabbed;
                if (!g.isAlive()) return true;
                if (k < d.duration) {
                    float lift = Math.min(1f, k / 12f);
                    Vec3 hold = u.position().add(flat(aim).scale(2.2 + g.getBbWidth())).add(0, 1.2 + lift * 2.2, 0);
                    g.setDeltaMovement(Vec3.ZERO);
                    g.setNoGravity(true);
                    g.teleportTo(hold.x, hold.y, hold.z);
                    g.fallDistance = 0;
                    if (k % 6 == 0) Fx.particles(level, Fx.dust(color, 1.2f), g.getBoundingBox().getCenter(), 6, 0.4, 0.02);
                    return false;
                }
                g.setNoGravity(false);
                Vec3 slam = u.position().add(flat(aim).scale(2.0));
                g.teleportTo(slam.x, u.getY(), slam.z);
                Sync.anim(u, Anims.SLAM);
                Combat.hit(u, g, d, d.damage, flat(aim).scale(0.6).add(0, -0.4, 0), 20);
                Fx.debris(level, g, 30);
                Fx.particles(level, ParticleTypes.EXPLOSION, g.position(), 1, 0.2, 0);
                Fx.sound(level, g.position(), SoundEvents.GENERIC_EXPLODE, 0.8f, 1.3f);
                s.grabbed = null;
                return true;
            }
            case SPIKES -> {
                if (t < d.windup) {
                    slow(u);
                    return false;
                }
                int k = t - d.windup;
                if (k % d.interval == 0 && k / d.interval < d.count) {
                    int i = k / d.interval;
                    Vec3 dir = flat(aim);
                    Vec3 p;
                    if (d.range < 1.45f) {
                        // ring pattern (ultimate blooms): spikes spiral outward around the user
                        double a = i * 2.39996;
                        double r = 2 + i * 0.35;
                        p = u.position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                    } else {
                        p = u.position().add(dir.scale(1.8 + i * d.range));
                        if (i % 2 == 1) p = p.add(dir.yRot((float) Math.PI / 2).scale(level.random.nextGaussian() * 0.6));
                    }
                    BlockPos ground = groundAt(level, p);
                    if (ground != null) GroundSpike.spawn(u, d, Vec3.atBottomCenterOf(ground), d.damage, color);
                }
                return k >= d.count * d.interval + 4;
            }
            case LIGHTNING -> {
                if (t < d.windup) {
                    if (t % 2 == 0) Fx.particles(level, ParticleTypes.ELECTRIC_SPARK, chest, 6, 0.6, 0.1);
                    return false;
                }
                int k = t - d.windup;
                if (k % d.interval == 0 && k / d.interval < d.count) {
                    Vec3 origin = eye.add(aim.scale(0.8)).add(rand(u, 0.4));
                    RcProjectile.fire(u, d, origin, aim.add(rand(u, 0.05)).normalize(), d.speed, d.damage, color);
                    Fx.sound(level, origin, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5f, 1.6f + level.random.nextFloat() * 0.3f);
                }
                return k >= d.count * d.interval + 4;
            }
            case FIRE -> {
                if (t < d.windup) return false;
                int k = t - d.windup;
                boolean extend = s.holding && u instanceof Player && k < d.duration * 2 && Fighters.spendRc(u, 0.5);
                if (k >= d.duration && !extend) return true;
                slow(u);
                Vec3 mouth = eye.add(aim.scale(0.5)).add(0, -0.2, 0);
                if (level instanceof ServerLevel sl) {
                    for (int i = 0; i < 6; i++) {
                        Vec3 v = aim.add(rand(u, 0.18)).normalize().scale(0.6 + level.random.nextDouble() * 0.5);
                        sl.sendParticles(ParticleTypes.FLAME, mouth.x, mouth.y, mouth.z, 0, v.x, v.y, v.z, 1);
                    }
                    if (k % 4 == 0) sl.sendParticles(ParticleTypes.LARGE_SMOKE, mouth.x, mouth.y, mouth.z, 0, aim.x * 0.3, aim.y * 0.3, aim.z * 0.3, 1);
                }
                if (k % 3 == 0) {
                    for (LivingEntity e : Combat.targetsIn(u, mouth.add(aim.scale(d.range * 0.5)), d.range * 0.6)) {
                        Vec3 to = e.getBoundingBox().getCenter().subtract(mouth).normalize();
                        if (to.dot(aim) > 0.75) Combat.hit(u, e, d, d.damage, aim.scale(0.15), 0);
                    }
                }
                if (k % 8 == 0) Fx.sound(level, mouth, SoundEvents.BLAZE_SHOOT, 0.7f, 0.6f);
                return false;
            }
            case GUARD_COUNTER -> {
                if (t == 0) {
                    s.counter = d;
                    s.counterTicks = d.duration;
                    Fx.sound(level, chest, SoundEvents.ARMOR_EQUIP_NETHERITE, 1f, 0.8f);
                    if (d.source == MoveDef.Source.KAGUNE) Fx.particles(level, Fx.dust(color, 1.5f), chest.add(aim), 14, 0.5, 0.02);
                }
                slow(u);
                return t >= d.duration || s.counterTicks <= 0;
            }
            case FORM -> {
                if (t < d.windup) {
                    s.dodgeTicks = Math.max(s.dodgeTicks, 2);
                    if (t % 2 == 0) {
                        Fx.particles(level, Fx.dust(color, 2.2f), chest, 12, 0.8, 0.1);
                        Fx.particles(level, ParticleTypes.SOUL_FIRE_FLAME, u.position(), 4, 0.6, 0.05);
                    }
                    if (t == 0) Fx.sound(level, chest, SoundEvents.WARDEN_EMERGE, 1f, 1.2f);
                    return false;
                }
                s.form = d;
                s.formTicks = d.duration;
                if (Fighters.race(u).hasKagune) kaguneOut(u, s, d.duration);
                for (LivingEntity e : Combat.targetsIn(u, chest, 5)) {
                    Combat.hit(u, e, d, 4, Combat.away(u.position(), e, 1.6f, 0.5f), 10);
                }
                Fx.ring(level, u.position(), 4.5, color);
                Fx.particles(level, ParticleTypes.EXPLOSION_EMITTER, chest, 1, 0, 0);
                Fx.sound(level, chest, SoundEvents.RAVAGER_ROAR, 1.5f, 0.7f);
                if (u instanceof Player p) p.displayClientMessage(d.title().withStyle(net.minecraft.ChatFormatting.DARK_RED), true);
                Sync.visual(u);
                return true;
            }
            case BUFF -> {
                if (t < d.windup) {
                    if (t % 3 == 0) Fx.particles(level, Fx.dust(color, 1.6f), chest, 8, 0.6, 0.1);
                    return false;
                }
                s.buff = d;
                s.buffTicks = d.duration;
                u.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, d.duration, 1, false, false));
                u.addEffect(new MobEffectInstance(MobEffects.JUMP, d.duration, 1, false, false));
                Fx.ring(level, u.position(), 2.5, color);
                Fx.sound(level, chest, SoundEvents.EVOKER_PREPARE_ATTACK, 1f, 0.9f);
                return true;
            }
            case DEVOUR -> {
                if (t < d.windup) return false;
                if (t == d.windup) {
                    Vec3 c = chest.add(aim.scale(d.range * 0.6));
                    for (LivingEntity e : Combat.targetsIn(u, c, 1.4)) {
                        if (Combat.hit(u, e, d, d.damage, aim.scale(0.3), 14)) {
                            u.heal(d.damage * 0.8f);
                            Fighters.addRc(u, 20);
                            if (u instanceof Player p && Fighters.ghoulBody(e)) {
                                TGPlayerData pd = PlayerCaps.get(p);
                                pd.kakuja = Math.min(100, pd.kakuja + 3);
                            }
                            Fx.particles(level, Fx.dust(0x8A0A0A, 1.8f), e.getBoundingBox().getCenter(), 20, 0.4, 0.05);
                            Fx.sound(level, c, SoundEvents.GENERIC_EAT, 1.2f, 0.7f);
                        }
                        break;
                    }
                }
                return t >= d.windup + 8;
            }
            case BLINK -> {
                if (t < d.windup) return false;
                if (t == d.windup) {
                    LivingEntity tgt = s.moveTarget;
                    if (tgt == null || !tgt.isAlive() || tgt.distanceTo(u) > d.range) {
                        List<LivingEntity> hits = Combat.targetsAlong(u, eye, eye.add(aim.scale(d.range)), 1.5);
                        hits.sort(Comparator.comparingDouble(e -> e.distanceToSqr(u)));
                        tgt = hits.isEmpty() ? null : hits.get(0);
                    }
                    Fx.particles(level, ParticleTypes.CLOUD, chest, 10, 0.3, 0.05);
                    if (tgt != null) {
                        Vec3 behind = tgt.position().subtract(Vec3.directionFromRotation(0, tgt.getYRot()).scale(1.4));
                        if (level.noCollision(u, u.getBoundingBox().move(behind.subtract(u.position())))) u.teleportTo(behind.x, behind.y, behind.z);
                        s.moveTarget = tgt;
                        s.aim = aimOf(u, tgt);
                        face(u, s.aim);
                    } else {
                        Vec3 to = clip(u, u.position().add(0, 0.5, 0), u.position().add(0, 0.5, 0).add(flat(aim).scale(d.range * 0.5)));
                        Vec3 dest = to.subtract(flat(aim).scale(0.6)).subtract(0, 0.5, 0);
                        if (level.noCollision(u, u.getBoundingBox().move(dest.subtract(u.position())))) u.teleportTo(dest.x, dest.y, dest.z);
                    }
                    Fx.sound(level, u.position(), SoundEvents.ENDERMAN_TELEPORT, 0.6f, 1.6f);
                    Sync.anim(u, d.anim);
                    return false;
                }
                if (t == d.windup + 2) {
                    Vec3 c = u.position().add(0, u.getBbHeight() * 0.6, 0).add(s.aim.scale(1.6));
                    for (LivingEntity e : Combat.targetsIn(u, c, 1.8)) {
                        Combat.hit(u, e, d, d.damage, flat(s.aim).scale(1.4).add(0, 0.3, 0), 14);
                    }
                    swing(level, c, color, true);
                }
                return t >= d.windup + 10;
            }
        }
        return true;
    }

    /** A GUARD_COUNTER stance was struck: retaliate immediately. */
    public static void counter(LivingEntity defender, LivingEntity attacker, MoveDef d) {
        FighterState s = Fighters.state(defender);
        if (s != null) s.clearMove();
        Vec3 dir = attacker.position().subtract(defender.position());
        face(defender, dir.normalize());
        Sync.anim(defender, d.source == MoveDef.Source.QUINQUE ? Anims.THRUST : Anims.HEAVY);
        Combat.hit(defender, attacker, d, d.damage, flat(dir).scale(1.8).add(0, 0.35, 0), 18);
        Fx.particles(defender.level(), ParticleTypes.FLASH, attacker.getBoundingBox().getCenter(), 1, 0, 0);
    }

    // ------------------------------------------------------------------ helpers

    private static int stunFor(MoveDef d) {
        return (int) Mth.clamp(6 + d.knockback * 6 + d.damage * 0.3f, 6, 24);
    }

    private static void slow(LivingEntity u) {
        u.setDeltaMovement(u.getDeltaMovement().multiply(0.5, 1, 0.5));
    }

    private static void push(LivingEntity u, Vec3 v) {
        u.setDeltaMovement(u.getDeltaMovement().add(v.x, 0.05, v.z));
        u.hurtMarked = true;
    }

    private static Vec3 rand(LivingEntity u, double spread) {
        var r = u.getRandom();
        return new Vec3(r.nextGaussian() * spread, r.nextGaussian() * spread, r.nextGaussian() * spread);
    }

    private static void swing(Level level, Vec3 at, int color, boolean heavy) {
        Fx.particles(level, ParticleTypes.SWEEP_ATTACK, at, 1, 0, 0);
        Fx.particles(level, Fx.dust(color, 1.0f), at, heavy ? 8 : 3, 0.4, 0.02);
        Fx.sound(level, at, heavy ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.6f, heavy ? 0.8f : 1.3f);
    }

    /** Stop a line at the first solid block. */
    private static Vec3 clip(LivingEntity u, Vec3 from, Vec3 to) {
        var hit = u.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, u));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : to;
    }

    @Nullable
    private static BlockPos groundAt(Level level, Vec3 p) {
        BlockPos pos = BlockPos.containing(p.x, p.y + 2, p.z);
        for (int i = 0; i < 7; i++) {
            BlockPos below = pos.below();
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty() && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return pos;
            }
            pos = below;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(p.x), (int) Math.floor(p.z));
        return Math.abs(y - p.y) < 6 ? BlockPos.containing(p.x, y, p.z) : null;
    }

    /** Players' action-bar message for a failed start. */
    public static Component failMessage(Fail f, MoveDef d) {
        return switch (f) {
            case COOLDOWN -> Component.translatable("msg.tokyoghoul.cooldown", d.title());
            case RANK -> Component.translatable("msg.tokyoghoul.rank_locked", d.minRank);
            case KAKUJA -> Component.translatable("msg.tokyoghoul.kakuja_locked");
            case RC -> Component.translatable("msg.tokyoghoul.no_rc");
            case SUPPRESSED -> Component.translatable("msg.tokyoghoul.suppressed");
            case STUNNED -> Component.translatable("msg.tokyoghoul.stunned");
            default -> Component.empty();
        };
    }
}
