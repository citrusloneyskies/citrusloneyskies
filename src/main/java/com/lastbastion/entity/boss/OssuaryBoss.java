package com.lastbastion.entity.boss;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.entity.zombie.ZombieKind;
import com.lastbastion.entity.zombie.ZombieRank;
import com.lastbastion.entity.zombie.ZombieSpawner;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Ossuary Colossus: a cathedral of fused bone eight blocks tall with a soul-furnace burning inside
 * its ribcage. Slow, ancient and single-minded, it marches on the core and only turns on defenders who
 * get underfoot. Every attack is huge and every attack is telegraphed: the Boulder Hurl marks where it
 * lands, the Quake Stomp is followed by an aftershock ring, Bone Spikes erupt in a travelling line,
 * Grave Sweep scythes a wide arc, and the Grasp plucks one defender off the ground to crush and hurl.
 * At half health its ribs crack open, spilling skeletal runners and exposing the furnace (it takes
 * more damage) which then fires a sweeping Soul Furnace beam.
 */
public class OssuaryBoss extends SiegeBoss {
    public static final int BOULDER = 3, STOMP = 4, SPIKES = 5, SMASH = 6, RIBCAGE = 7, SWEEP = 8, GRASP = 9, FURNACE = 10;
    public static final int SOUL = 0x60FFB0;
    private Vec3 throwAt = Vec3.ZERO, dir = Vec3.ZERO, graspAt = Vec3.ZERO;
    @Nullable
    private LivingEntity held;
    private int stepTimer;

    public OssuaryBoss(EntityType<? extends SiegeBoss> type, Level level) {
        super(type, level, BossKind.OSSUARY);
        setMaxUpStep(2.5f);
    }

    @Override
    public float elementMultiplier(Element e) {
        return e == Element.EXPLOSIVE ? 1.3f : e == Element.PHYSICAL ? 0.8f : e == Element.POISON ? 0f : super.elementMultiplier(e);
    }

    @Override
    public int dangerColor() {
        return SOUL;
    }

    @Override
    protected String epithet() {
        return "Cathedral of a Thousand Dead";
    }

    @Override
    protected int introLength() {
        return 60;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"...WE... ARE... THE... FORGOTTEN...", "...SO MANY... BONES... ROOM... FOR... YOURS..."};
            case TAUNT -> new String[]{"...KNEEL... BENEATH...", "...ALL... WALLS... FALL...", "...DUST... TO... DUST..."};
            case PHASE -> new String[]{"...OPEN... THE... OSSUARY...", "...THE... DEAD... SPILL... FORTH..."};
            case STAGGER -> new String[]{"...CRACKED... NOT... BROKEN...", "...THE... FOUNDATIONS... SHAKE..."};
            case KILL -> new String[]{"...ANOTHER... STONE... FOR... THE... WALL...", "...JOIN... US..."};
            case DEATH -> new String[]{"...AT... LAST... SILENCE...", "...WE... RETURN... TO... THE... EARTH..."};
            case CORE -> new String[]{"...THE... LIGHT... MUST... END..."};
        };
    }

    @Override
    protected void introTick(ServerLevel level, int t) {
        if (t % 6 == 0 && t < 44) {
            Vfx.spray(level, FxStyle.DEBRIS, position().add(0, 0.2, 0), 0x6A5A48, 0xE8E0D0, 0.5f, 14, 0.4f, 0.8f);
            Vfx.shake(level, position(), 0.4f, 30, 8);
            Fx.sound(level, position(), SoundEvents.ROOTED_DIRT_BREAK, 2f, 0.4f);
        }
        if (t == 48) {
            Fx.sound(level, position(), SoundEvents.WARDEN_ROAR, 3f, 0.5f);
            Vfx.spray(level, FxStyle.SOUL, position().add(0, getBbHeight() * 0.6, 0), SOUL, 0xFFFFFF, 0.5f, 30, 0.3f, 0.5f);
        }
    }

    @Override
    protected double engageRange() {
        return 6;
    }

    @Override
    protected double meleeReach() {
        return getBbWidth() * 0.5 + 3;
    }

    @Override
    protected int meleeWindup() {
        return 20;
    }

    @Override
    protected float coreDamage() {
        return dmg(2.5f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // the exposed furnace is a weak point
        return super.hurt(source, phase() >= 2 ? amount * 1.15f : amount);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dt, double dc) {
        if (dc < meleeReach() + 0.5 && ready(SMASH)) return SMASH;
        var close = SiegeDamage.defendersAround(level, position(), 8);
        if (close.size() >= 2 && ready(SWEEP)) return SWEEP;
        if (!close.isEmpty() && ready(STOMP)) return STOMP;
        if (ready(GRASP)) {
            for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 7)) {
                if (e instanceof TurretEntity || (e instanceof Player p && (p.isCreative() || p.isSpectator()))) continue;
                held = e;
                return GRASP;
            }
        }
        if (phase() >= 2 && ready(FURNACE) && !SiegeDamage.defendersAround(level, position(), 16).isEmpty()) return FURNACE;
        if (ready(BOULDER)) {
            LivingEntity turret = null;
            for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 28)) {
                if (e instanceof TurretEntity && distanceTo(e) > 8) {
                    turret = e;
                    break;
                }
            }
            if (turret != null) {
                throwAt = turret.position();
                return BOULDER;
            }
            if (dc > 8 && dc < 28) {
                BlockPos c = core();
                if (c != null) {
                    throwAt = Vec3.atBottomCenterOf(c);
                    return BOULDER;
                }
            }
        }
        LivingEntity far = t;
        if (far == null) {
            for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 18)) {
                if (!(e instanceof TurretEntity)) {
                    far = e;
                    break;
                }
            }
        }
        if (far != null && distanceTo(far) > 5 && ready(SPIKES)) {
            dir = far.position().subtract(position()).multiply(1, 0, 1).normalize();
            return SPIKES;
        }
        if (random.nextInt(700) == 0) speak(Lines.TAUNT, false);
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case BOULDER -> {
                face(throwAt);
                if (t == 1) {
                    double flight = position().distanceTo(throwAt) / 1.1;
                    teleCircle(level, throwAt, 4f, 26 + (int) flight + 4);
                }
                if (t == 12) {
                    Vfx.spray(level, FxStyle.DEBRIS, forward(3), 0x6A5A48, 0xE8E0D0, 0.5f, 20, 0.4f, 1f);
                    Fx.sound(level, forward(3), SoundEvents.STONE_BREAK, 2f, 0.4f);
                }
                if (t == 26) {
                    Vec3 hand = position().add(0, getBbHeight() * 0.95, 0);
                    SiegeProjectile b = SiegeProjectile.create(level, SiegeProjectile.ProjKind.BOULDER, this, hand);
                    b.payload(dmg(1.6f), Element.PHYSICAL).area(4f);
                    b.knockback = 1.2f;
                    BlockPos core = core();
                    b.onImpact = (p, at) -> {
                        Vfx.shake(level, at, 0.6f, 20, 10);
                        Vfx.spray(level, FxStyle.SHARD, at, 0xE8E0D0, 0xB8A890, 0.35f, 20, 0.5f, 0.8f);
                        if (core != null && at.distanceTo(Vec3.atCenterOf(core)) < 4) SiegeManager.damageCore(level, dmg(1.5f), this);
                    };
                    b.aim(throwAt, 1.1f).scale(3f).fire(level);
                    Fx.sound(level, hand, SoundEvents.IRON_GOLEM_ATTACK, 2f, 0.4f);
                }
                if (t >= 40) {
                    cooldown(BOULDER, 150);
                    return true;
                }
            }
            case STOMP -> {
                if (t == 1) teleCircle(level, position(), 8f, 22);
                if (t == 22) {
                    Vfx.ring(level, position().add(0, 0.1, 0), SOUL, 8, 14, 1.4f);
                    Vfx.spray(level, FxStyle.DEBRIS, position(), 0x6A5A48, 0x4A3A2A, 0.5f, 30, 0.6f, 0.7f);
                    Vfx.shake(level, position(), 1.1f, 28, 14);
                    Fx.sound(level, position(), SoundEvents.GENERIC_EXPLODE, 2.5f, 0.4f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 8)) {
                        SiegeDamage.deal(e, this, this, dmg(1.2f), Element.PHYSICAL, false);
                        SiegeDamage.push(e, position(), 1.4, 0.9);
                        SiegeDamage.apply(e, ModEffects.CONCUSSED.get(), 80, 0);
                    }
                    teleDonut(level, position(), 8, 12.5f, 14);
                }
                if (t == 36) {
                    Vfx.ring(level, position().add(0, 0.1, 0), SOUL, 12.5f, 12, 0.9f);
                    Fx.sound(level, position(), SoundEvents.GENERIC_EXPLODE, 1.6f, 0.3f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 12.5)) {
                        double d = Math.sqrt(e.distanceToSqr(getX(), e.getY(), getZ()));
                        if (d < 8) continue;
                        SiegeDamage.deal(e, this, this, dmg(0.7f), Element.PHYSICAL, false);
                        e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
                        e.hurtMarked = true;
                    }
                }
                if (t >= 48) {
                    cooldown(STOMP, 170);
                    return true;
                }
            }
            case SPIKES -> {
                face(position().add(dir));
                if (t == 1) teleLine(level, position(), position().add(dir.scale(20)), 2.6f, 16);
                if (t >= 16 && t <= 36 && t % 2 == 0) {
                    double dist = 2 + (t - 16) * 0.9;
                    Vec3 at = position().add(dir.scale(dist));
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(at.x), (int) Math.floor(at.z));
                    at = new Vec3(at.x, Math.min(y, getY() + 3), at.z);
                    HazardZone.spawn(level, HazardZone.Kind.BONE_SPIKES, at, 1.5f, 12, damageScale * 0.4f, this, true);
                    Vfx.spray(level, FxStyle.SHARD, at, 0xE8E0D0, 0xB8A890, 0.4f, 10, 0.45f, 1f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, at, 1.7)) {
                        if (e.hurtTime > 0) continue;
                        SiegeDamage.deal(e, this, this, dmg(1f), Element.PHYSICAL, false);
                        e.setDeltaMovement(e.getDeltaMovement().add(0, 0.8, 0));
                        e.hurtMarked = true;
                    }
                    if (t % 4 == 0) Fx.sound(level, at, SoundEvents.BONE_BLOCK_BREAK, 2f, 0.5f);
                }
                if (t >= 42) {
                    cooldown(SPIKES, 120);
                    return true;
                }
            }
            case SMASH -> {
                BlockPos c = core();
                if (c != null) face(Vec3.atCenterOf(c));
                if (t == 1 && c != null) teleCircle(level, Vec3.atBottomCenterOf(c), 4f, 30);
                if (t == 30 && c != null) {
                    SiegeManager.damageCore(level, dmg(3f) * 1.5f, this);
                    Fx.shockwave(level, Vec3.atBottomCenterOf(c), 6, 0xE8E0D0);
                    Vfx.shake(level, position(), 1.2f, 30, 16);
                    Fx.sound(level, Vec3.atCenterOf(c), SoundEvents.ANVIL_LAND, 2f, 0.3f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, Vec3.atBottomCenterOf(c), 4)) {
                        SiegeDamage.deal(e, this, this, dmg(1.4f), Element.PHYSICAL, false);
                        SiegeDamage.push(e, Vec3.atBottomCenterOf(c), 1.2, 0.6);
                    }
                    speak(Lines.CORE, false);
                }
                if (t >= 50) {
                    cooldown(SMASH, 140);
                    return true;
                }
            }
            case SWEEP -> {
                if (target != null && t < 10) face(target.position());
                if (t == 2) teleCone(level, position(), forward(5), 9.5f, 80, 18);
                if (t == 20) {
                    Vfx.slash(level, position().add(0, 1.2, 0), getYRot(), 4, 170, 9f, SOUL, 0xFFFFFF, 10, 0.5f);
                    Vfx.spray(level, FxStyle.DEBRIS, forward(5), 0x6A5A48, 0x4A3A2A, 0.45f, 24, 0.5f, 0.5f);
                    Fx.sound(level, position(), SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 0.3f);
                    Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
                    Vec3 side = new Vec3(fwd.z, 0, -fwd.x);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 9.5)) {
                        Vec3 to = e.position().subtract(position()).multiply(1, 0, 1).normalize();
                        if (to.dot(fwd) < Math.cos(Math.toRadians(80))) continue;
                        SiegeDamage.deal(e, this, this, dmg(1.3f), Element.PHYSICAL, false);
                        e.setDeltaMovement(side.x * 1.6 + to.x * 0.6, 0.6, side.z * 1.6 + to.z * 0.6);
                        e.hurtMarked = true;
                    }
                }
                if (t >= 36) {
                    cooldown(SWEEP, 150);
                    return true;
                }
            }
            case GRASP -> {
                LivingEntity v = held;
                if (t == 1 && v != null) {
                    graspAt = v.position();
                    face(graspAt);
                    teleCircle(level, graspAt, 2f, 14);
                }
                if (t == 14) {
                    Vfx.spray(level, FxStyle.DEBRIS, graspAt, 0x6A5A48, 0x4A3A2A, 0.4f, 16, 0.4f, 0.8f);
                    Fx.sound(level, graspAt, SoundEvents.IRON_GOLEM_ATTACK, 2f, 0.5f);
                    if (v == null || !v.isAlive() || v.position().distanceToSqr(graspAt) > 2.5 * 2.5) {
                        held = null;
                        Vfx.ring(level, graspAt.add(0, 0.1, 0), SOUL, 2.4f, 8, 0.5f);
                    } else {
                        say(v instanceof Player ? "...SMALL... THING..." : "...MINE...");
                    }
                }
                if (t > 14 && t < 44 && held != null) {
                    LivingEntity h = held;
                    if (!h.isAlive()) {
                        held = null;
                    } else {
                        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
                        Vec3 hand = position().add(fwd.scale(2.6)).add(0, getBbHeight() * (0.55 + 0.25 * Math.min(1, (t - 14) / 10.0)), 0);
                        h.teleportTo(hand.x, hand.y - h.getBbHeight() * 0.5, hand.z);
                        h.setDeltaMovement(Vec3.ZERO);
                        h.fallDistance = 0;
                        if (t % 6 == 0) {
                            SiegeDamage.deal(h, this, this, dmg(0.35f), Element.PHYSICAL, false);
                            Vfx.spray(level, FxStyle.SHARD, hand, 0xE8E0D0, SOUL, 0.25f, 6, 0.2f, 0);
                            Fx.sound(level, hand, SoundEvents.BONE_BLOCK_BREAK, 1.5f, 0.6f);
                        }
                    }
                }
                if (t == 44 && held != null) {
                    LivingEntity h = held;
                    BlockPos c = core();
                    Vec3 away = c != null ? h.position().subtract(Vec3.atCenterOf(c)).multiply(1, 0, 1) : Vec3.directionFromRotation(0, getYRot());
                    if (away.lengthSqr() < 0.01) away = Vec3.directionFromRotation(0, getYRot());
                    away = away.normalize();
                    SiegeDamage.deal(h, this, this, dmg(0.9f), Element.PHYSICAL, false);
                    h.setDeltaMovement(away.x * 1.8, 0.7, away.z * 1.8);
                    h.hurtMarked = true;
                    h.fallDistance = 0;
                    Vfx.dash(level, h.position(), h.position().add(away.scale(6)).add(0, 2, 0), SOUL, 0.8f, 10);
                    Fx.sound(level, h.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2f, 0.4f);
                    held = null;
                }
                if (t >= 54) {
                    held = null;
                    cooldown(GRASP, 260);
                    return true;
                }
            }
            case FURNACE -> {
                if (t == 1) {
                    LivingEntity v = target != null ? target : SiegeDamage.defendersAround(level, position(), 16).stream().findFirst().orElse(null);
                    if (v != null) face(v.position());
                    teleCone(level, position(), forward(8), 16f, 38, 22);
                    Vfx.flash(level, position().add(0, getBbHeight() * 0.6, 0).add(Vec3.directionFromRotation(0, getYRot()).scale(1.2)), SOUL, 3f, 22);
                    Fx.sound(level, position(), SoundEvents.BEACON_ACTIVATE, 2.5f, 0.4f);
                }
                if (t >= 22 && t <= 54) {
                    float sweep = -38 + (t - 22) / 32f * 76;
                    Vec3 d = Vec3.directionFromRotation(0, getYRot() + sweep);
                    Vec3 chest = position().add(0, getBbHeight() * 0.6, 0).add(Vec3.directionFromRotation(0, getYRot()).scale(1.2));
                    Vec3 ground = position().add(d.scale(15));
                    if (t % 2 == 0) Vfx.beam(level, chest, ground, SOUL, 0.7f, 3, 0.1f);
                    if (t % 3 == 0) Vfx.spray(level, FxStyle.SOUL, ground, SOUL, 0xFFFFFF, 0.4f, 6, 0.2f, 1f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position().add(d.scale(8)), 8.5)) {
                        Vec3 rel = e.position().subtract(position()).multiply(1, 0, 1);
                        double along = rel.dot(d);
                        if (along < 1 || along > 16 || rel.subtract(d.scale(along)).lengthSqr() > 1.6 * 1.6 || e.hurtTime > 0) continue;
                        SiegeDamage.deal(e, this, this, dmg(0.8f), Element.SHADOW, false);
                        SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 60, 2);
                    }
                    if (t % 8 == 0) Fx.sound(level, chest, SoundEvents.BEACON_AMBIENT, 2f, 0.5f);
                }
                if (t >= 62) {
                    cooldown(FURNACE, 240);
                    return true;
                }
            }
            case RIBCAGE -> {
                if (t == 20) {
                    for (int i = 0; i < 6; i++) {
                        Vec3 p = position().add(0, 3, 0);
                        SiegeZombie z = ZombieSpawner.spawn(level, ZombieKind.RUNNER, ZombieRank.ARMORED, 0, p, true, false);
                        if (z != null) {
                            z.setMinion(true);
                            z.setDeltaMovement(random.nextGaussian() * 0.5, 0.6, random.nextGaussian() * 0.5);
                        }
                    }
                    Vfx.spray(level, FxStyle.SHARD, position().add(0, getBbHeight() * 0.6, 0), 0xE8E0D0, 0xB8A890, 0.5f, 30, 0.6f, 0.3f);
                    Vfx.flash(level, position().add(0, getBbHeight() * 0.6, 0), SOUL, 4f, 14);
                    Vfx.shake(level, position(), 0.9f, 30, 14);
                    Fx.sound(level, position(), SoundEvents.SKELETON_HORSE_DEATH, 2f, 0.4f);
                }
                return t >= 44;
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (getDeltaMovement().horizontalDistanceSqr() > 0.003 && ++stepTimer >= 18) {
            stepTimer = 0;
            Vfx.shake(level, position(), 0.18f, 22, 6);
            Vfx.spray(level, FxStyle.SMOKE, position(), 0x8A7A68, 0x6A5A48, 0.8f, 3, 0.08f, -1f);
            Fx.sound(level, position(), SoundEvents.IRON_GOLEM_STEP, 2f, 0.4f);
        }
        if (phase() >= 2 && tickCount % 6 == 0) {
            Vfx.spray(level, FxStyle.SOUL, position().add(0, getBbHeight() * 0.6, 0), SOUL, 0xFFFFFF, 0.3f, 2, 0.06f, 0.6f);
        }
    }

    @Override
    protected void onPhase(ServerLevel level, int phase) {
        super.onPhase(level, phase);
        held = null;
        startSkill(RIBCAGE);
    }

    @Override
    protected String phaseLine(int phase) {
        return "tears open its ribcage - its soul-furnace is exposed!";
    }
}
