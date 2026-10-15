package com.lastbastion.entity.boss;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Morvathrax the Rotwing: a colossal undead wyvern of exposed bone and plague-fire, proud and cruel.
 * <p><b>Aerial phase</b> (above 40%): circles the core out of reach and attacks in passes, each one
 * marked on the ground before it lands: the Plague Strafe burns a lane, the Bone Barrage drops shard
 * bombs on circles along its flight path, and the Death Dive craters a marked circle, after which he
 * stays grounded for a few seconds (claw sweep included) and is open to melee.
 * <p><b>Grounded phase</b> (40%): his wings tear and he crashes down to fight on wing-knuckles and
 * talons: Plague Breath sweeps a marked cone, Tail Sweep scythes a ring (safe under his belly), Wing
 * Buffet blasts everyone back, Rending Claws lunge, and the Carrion Pounce leaps onto a marked target.
 * At 15% he enters his <b>Death Throes</b>: a choking rot aura and an endless spill of plague-fire.
 */
public class MorvathraxBoss extends SiegeBoss {
    public static final int STRAFE = 3, DIVE = 4, HAIL = 5, TAIL = 6, BREATH = 7, BUFFET = 8, LAND = 9, CLAW = 10, ROAR = 11, POUNCE = 12;
    public static final int PLAGUE = 0x6BFF8A;
    private double orbit;
    private Vec3 runStart = Vec3.ZERO, runDir = Vec3.ZERO, diveAt = Vec3.ZERO, leapFrom = Vec3.ZERO;
    private int impactTick = -1;
    /** Client-side smoothed flight blend (1 = flying pose, 0 = grounded pose). */
    public float clientAir = 1, clientAirO = 1;

    public MorvathraxBoss(EntityType<? extends SiegeBoss> type, Level level) {
        super(type, level, BossKind.MORVATHRAX);
        setNoGravity(true);
    }

    /** True while in the aerial phase (synced via the boss flag). */
    public boolean airborne() {
        return !flag();
    }

    public float air(float pt) {
        return Mth.lerp(pt, clientAirO, clientAir);
    }

    @Override
    public void tick() {
        try {
        super.tick();
        if (level().isClientSide) {
            clientAirO = clientAir;
            float want = airborne() && !onGround() ? 1 : 0;
            clientAir += (want - clientAir) * 0.12f;
        }
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("MorvathraxBoss.tick", lbCrash);
        }
    }

    @Override
    public int dangerColor() {
        return PLAGUE;
    }

    @Override
    protected String epithet() {
        return "the Sky That Rots";
    }

    @Override
    protected int introLength() {
        return 50;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"I have devoured kingdoms. Your little wall is a morsel.", "Look up, insects. Your doom has wings."};
            case TAUNT -> new String[]{"Run. Burn. It makes no difference.", "Your arrows tickle.", "I smell your fear from the clouds."};
            case PHASE -> new String[]{"MY WINGS! You will pay for every tatter!", "Grounded? Then I will crush you where you stand!"};
            case STAGGER -> new String[]{"Graaah-! Impossible!", "You dare wound a dragon?!"};
            case KILL -> new String[]{"Crunchy.", "Another bone for my hoard.", "Ash and marrow."};
            case DEATH -> new String[]{"The sky... grows... dark...", "I was... a god... of the air..."};
            case CORE -> new String[]{"That glittering heart is MINE."};
        };
    }

    @Override
    protected void onArrive(ServerLevel level) {
        super.onArrive(level);
        setPos(getX(), getY() + 14, getZ());
    }

    @Override
    protected void introTick(ServerLevel level, int t) {
        if (t % 10 == 0) Fx.sound(level, position(), SoundEvents.ENDER_DRAGON_FLAP, 3f, 0.5f);
        if (t == 30) {
            Fx.sound(level, position(), SoundEvents.ENDER_DRAGON_GROWL, 4f, 0.6f);
            Vfx.spray(level, FxStyle.FLAME, position().add(0, 2, 0).add(Vec3.directionFromRotation(0, getYRot()).scale(5)), PLAGUE, 0x1A6A2A, 0.6f, 40, 0.4f, 0.4f);
            Vfx.screenFlash(level, position(), PLAGUE, 0.2f, 80, 12);
        }
    }

    @Override
    public float elementMultiplier(Element e) {
        return e == Element.FIRE ? 0.3f : e == Element.HOLY ? 1.5f : e == Element.LIGHTNING && airborne() ? 1.3f : super.elementMultiplier(e);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // pinned to the ground after a dive he is exposed
        if (airborne() && impactTick > 0 && skill == DIVE) amount *= 1.25f;
        return super.hurt(source, amount);
    }

    @Override
    protected float phaseThreshold(int phase) {
        return phase == 2 ? 0.4f : phase == 3 ? 0.15f : -1f;
    }

    @Override
    protected double meleeReach() {
        return 4.5;
    }

    private Vec3 groundAt(Vec3 p) {
        int y = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(p.x), (int) Math.floor(p.z));
        return new Vec3(p.x, y, p.z);
    }

    private Vec3 mouth() {
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        return position().add(fwd.scale(airborne() ? 6 : 5.5)).add(0, airborne() ? 1.5 : 3, 0);
    }

    @Override
    protected void tickMovementAndMelee(ServerLevel level, @Nullable BlockPos core, double distTarget, double distCore) {
        if (!airborne()) {
            setNoGravity(false);
            super.tickMovementAndMelee(level, core, distTarget, distCore);
            return;
        }
        Vec3 c = core != null ? Vec3.atCenterOf(core) : position();
        orbit += 0.012;
        Vec3 dest = c.add(Math.cos(orbit) * 18, 14, Math.sin(orbit) * 18);
        steer(dest, 0.5);
    }

    private void steer(Vec3 dest, double maxSpeed) {
        Vec3 v = dest.subtract(position());
        Vec3 want = v.normalize().scale(Math.min(maxSpeed, v.length() * 0.1));
        setDeltaMovement(getDeltaMovement().scale(0.85).add(want.scale(0.15)));
        Vec3 m = getDeltaMovement();
        if (m.horizontalDistanceSqr() > 1e-4) face(position().add(m));
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dt, double dc) {
        if (airborne()) {
            if (ready(DIVE) && (t != null || dc < 30)) return DIVE;
            if (ready(STRAFE)) return STRAFE;
            if (ready(HAIL)) return HAIL;
            return 0;
        }
        var near = SiegeDamage.defendersAround(level, position(), 7.5);
        if (near.size() >= 2 && ready(TAIL)) return TAIL;
        if (t != null && dt > 8 && dt < 20 && ready(POUNCE)) return POUNCE;
        if (t != null && dt < 13 && ready(BREATH)) return BREATH;
        if (t != null && dt < 6 && ready(CLAW)) return CLAW;
        if (SiegeDamage.defendersAround(level, position(), 9).size() >= 3 && ready(BUFFET)) return BUFFET;
        if (t != null && random.nextInt(500) == 0) speak(Lines.TAUNT, false);
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        BlockPos core = core();
        Vec3 c = core != null ? Vec3.atCenterOf(core) : position();
        switch (skill) {
            case STRAFE -> {
                if (t == 1) {
                    Vec3 focus = target != null ? target.position() : c;
                    runDir = focus.subtract(position()).multiply(1, 0, 1).normalize();
                    if (runDir.lengthSqr() < 0.01) runDir = new Vec3(1, 0, 0);
                    runStart = focus.subtract(runDir.scale(18)).add(0, 9, 0);
                    Fx.sound(level, position(), SoundEvents.ENDER_DRAGON_GROWL, 3f, 0.6f);
                    diveAt = groundAt(focus);
                }
                if (t == 22) teleLine(level, diveAt.subtract(runDir.scale(12)), diveAt.add(runDir.scale(14)), 4.2f, 58);
                if (t < 40) steer(runStart, 0.9);
                else if (t < 80) {
                    setDeltaMovement(runDir.scale(0.85));
                    face(position().add(runDir));
                    Vec3 m = mouth();
                    Vec3 g = groundAt(position().add(runDir.scale(3)));
                    if (t % 2 == 0) Vfx.jet(level, FxStyle.FLAME, m, g, PLAGUE, 0x1A6A2A, 0.6f, 14, 12);
                    if (t % 4 == 0) {
                        HazardZone.spawn(level, HazardZone.Kind.SOUL_FIRE, g, 2.2f, 80, damageScale, this, true);
                        for (LivingEntity e : SiegeDamage.defendersAround(level, g, 3)) SiegeDamage.deal(e, this, this, dmg(0.5f), Element.FIRE, false);
                        if (core != null && g.distanceTo(c) < 3) SiegeManager.damageCore(level, dmg(0.6f), this);
                    }
                    if (t % 10 == 0) Fx.sound(level, m, SoundEvents.ENDER_DRAGON_SHOOT, 2f, 0.6f);
                } else {
                    cooldown(STRAFE, 170);
                    return true;
                }
            }
            case DIVE -> {
                if (t == 1) {
                    diveAt = groundAt(target != null ? target.position() : c);
                    impactTick = -1;
                    Fx.sound(level, position(), SoundEvents.PHANTOM_SWOOP, 3f, 0.4f);
                    teleCircle(level, diveAt, 7f, 50);
                }
                if (impactTick < 0) {
                    if (t < 25) {
                        steer(diveAt.add(0, 16, 0), 0.8);
                    } else {
                        Vec3 v = diveAt.subtract(position());
                        setDeltaMovement(v.normalize().scale(1.4));
                        face(diveAt);
                        if (t % 2 == 0) Vfx.spray(level, FxStyle.SMOKE, position().add(0, 1, 0), 0x4A4A3A, 0x2A2A20, 1f, 3, 0.05f, 0);
                        if (v.length() < 3 || onGround() || t > 70) {
                            impactTick = t;
                            setDeltaMovement(Vec3.ZERO);
                            Vfx.ring(level, position().add(0, 0.1, 0), PLAGUE, 8, 14, 1.6f);
                            Vfx.spray(level, FxStyle.DEBRIS, position(), 0x5A4A3A, 0x3A2A1A, 0.6f, 40, 0.7f, 0.8f);
                            Vfx.shake(level, position(), 1.3f, 32, 16);
                            Fx.explosion(level, position(), 5);
                            for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 7)) {
                                SiegeDamage.deal(e, this, this, dmg(1.8f), Element.PHYSICAL, false);
                                SiegeDamage.push(e, position(), 1.8, 0.8);
                            }
                            if (core != null && position().distanceTo(c) < 6) SiegeManager.damageCore(level, dmg(2.5f), this);
                            crushBlocks(level);
                        }
                    }
                } else {
                    int s = t - impactTick;
                    setDeltaMovement(getDeltaMovement().multiply(0, 1, 0).add(0, -0.08, 0));
                    if (s == 18) {
                        teleCone(level, position(), forward(4), 6.5f, 70, 14);
                        anim.play(CLAW);
                    }
                    if (s == 34) anim.play(BUFFET);
                    if (s == 32) clawSweep(level, 1.1f);
                    if (s == 50) {
                        Vfx.ring(level, position().add(0, 0.1, 0), 0xC8C0A8, 6, 10, 0.8f);
                        Fx.sound(level, position(), SoundEvents.ENDER_DRAGON_FLAP, 3f, 0.4f);
                        for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 6)) SiegeDamage.push(e, position(), 0.8, 0.3);
                    }
                    if (s > 50) setDeltaMovement(0, 0.7, 0);
                    if (s >= 60) {
                        impactTick = -1;
                        cooldown(DIVE, 280);
                        return true;
                    }
                }
            }
            case HAIL -> {
                tickMovementAndMelee(level, core, 0, 0);
                if (t == 4) {
                    Fx.sound(level, position(), SoundEvents.SKELETON_HORSE_DEATH, 3f, 0.4f);
                    say(random.nextBoolean() ? "Bones of the fallen - RAIN!" : "Take back your dead!");
                }
                if (t >= 10 && t <= 70 && t % 5 == 0) {
                    Vec3 ahead = position().add(getDeltaMovement().scale(14));
                    Vec3 at = groundAt(ahead.add(random.nextGaussian() * 3, 0, random.nextGaussian() * 3));
                    teleCircle(level, at, 2.8f, 16);
                    Vec3 from = position().add(0, -0.5, 0);
                    SiegeProjectile p = SiegeProjectile.create(level, SiegeProjectile.ProjKind.BONE_SHARD, this, from);
                    p.payload(dmg(0.9f), Element.PHYSICAL).area(2.8f);
                    p.gravity = 0;
                    p.onImpact = (pr, hit) -> {
                        Vfx.spray(level, FxStyle.SHARD, hit, 0xE8E0D0, PLAGUE, 0.35f, 12, 0.45f, 0.8f);
                        if (core != null && hit.distanceTo(c) < 3.5) SiegeManager.damageCore(level, dmg(0.5f), this);
                    };
                    p.aim(at, (float) Math.max(0.6, from.distanceTo(at) / 15)).scale(2.4f).fire(level);
                }
                if (t >= 76) {
                    cooldown(HAIL, 300);
                    return true;
                }
            }
            case LAND -> {
                if (t == 1) {
                    diveAt = groundAt(position());
                    teleCircle(level, diveAt, 9f, 40);
                }
                setNoGravity(false);
                setDeltaMovement(getDeltaMovement().multiply(0.5, 1, 0.5).add(0, -0.2, 0));
                if (onGround() || t > 80) {
                    Fx.explosion(level, position(), 6);
                    Vfx.ring(level, position().add(0, 0.1, 0), PLAGUE, 10, 16, 2f);
                    Vfx.spray(level, FxStyle.DEBRIS, position(), 0x5A4A3A, 0x3A2A1A, 0.7f, 50, 0.8f, 0.8f);
                    Vfx.shake(level, position(), 1.5f, 40, 20);
                    Fx.sound(level, position(), SoundEvents.ENDER_DRAGON_GROWL, 4f, 0.3f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 9)) {
                        SiegeDamage.deal(e, this, this, dmg(1.5f), Element.PHYSICAL, false);
                        SiegeDamage.push(e, position(), 2, 0.8);
                    }
                    return true;
                }
            }
            case TAIL -> {
                if (t == 2) teleDonut(level, position(), 2.4f, 8.5f, 18);
                if (t == 20) {
                    Vfx.slash(level, position().add(0, 1, 0), getYRot(), 2, 360, 8f, PLAGUE, 0xFFFFFF, 10, 0.5f);
                    Fx.sound(level, position(), SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 0.3f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 8.5)) {
                        double d = Math.sqrt(e.distanceToSqr(getX(), e.getY(), getZ()));
                        if (d < 2.4) continue;
                        SiegeDamage.deal(e, this, this, dmg(1.3f), Element.PHYSICAL, false);
                        SiegeDamage.push(e, position(), 2.2, 0.7);
                    }
                }
                if (t >= 36) {
                    cooldown(TAIL, 120);
                    return true;
                }
            }
            case BREATH -> {
                if (target != null && t < 16) face(target.position());
                if (t == 2) teleCone(level, position(), forward(6), 13f, 42, 18);
                if (t > 18 && t < 62) {
                    float sweep = (float) Math.sin((t - 18) / 44.0 * Math.PI) * 50f - 25f;
                    Vec3 dir = Vec3.directionFromRotation(18, getYRot() + sweep);
                    Vec3 m = mouth();
                    if (t % 2 == 0) Vfx.jet(level, FxStyle.FLAME, m, m.add(dir.scale(12)), PLAGUE, 0x1A6A2A, 0.7f, 18, 14);
                    if (t % 4 == 0) {
                        for (LivingEntity e : SiegeDamage.defendersAround(level, m.add(dir.scale(6)), 7)) {
                            if (e.position().add(0, 1, 0).subtract(m).normalize().dot(dir) > 0.82) {
                                SiegeDamage.deal(e, this, this, dmg(0.45f), Element.FIRE, false);
                                SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 80, 3);
                            }
                        }
                    }
                    if (t % 12 == 0) HazardZone.spawn(level, HazardZone.Kind.SOUL_FIRE, groundAt(m.add(dir.scale(8))), 2.2f, 90, damageScale * 0.8f, this, true);
                    if (t % 10 == 0) Fx.sound(level, m, SoundEvents.ENDER_DRAGON_SHOOT, 2f, 0.5f);
                }
                if (t >= 70) {
                    cooldown(BREATH, 150);
                    return true;
                }
            }
            case BUFFET -> {
                if (t == 2) teleCircle(level, position(), 10f, 16);
                if (t == 18) {
                    Vfx.ring(level, position().add(0, 0.5, 0), 0xCFFFF0, 10, 12, 1.2f);
                    Vfx.spray(level, FxStyle.SMOKE, position(), 0xC8C0A8, 0x8A8070, 1f, 30, 0.6f, -0.8f);
                    Fx.sound(level, position(), SoundEvents.ENDER_DRAGON_FLAP, 3f, 0.5f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 10)) {
                        SiegeDamage.deal(e, this, this, dmg(0.6f), Element.WIND, false);
                        SiegeDamage.push(e, position(), 2.5, 0.6);
                        SiegeDamage.apply(e, ModEffects.DISORIENTED.get(), 60, 0);
                    }
                }
                if (t >= 32) {
                    cooldown(BUFFET, 180);
                    return true;
                }
            }
            case CLAW -> {
                if (target != null && t < 22) face(target.position());
                if (t == 2 || t == 16) teleCone(level, position(), forward(4), 6.5f, 60, 12);
                if (t == 14) clawSweep(level, 1f);
                if (t == 28) clawSweep(level, 1.2f);
                if (t >= 40) {
                    cooldown(CLAW, 90);
                    return true;
                }
            }
            case POUNCE -> {
                if (t == 1) {
                    leapFrom = position();
                    diveAt = target != null ? groundAt(target.position()) : forward(10);
                    face(diveAt);
                    teleCircle(level, diveAt, 5f, 30);
                    Fx.sound(level, position(), SoundEvents.ENDER_DRAGON_GROWL, 3f, 0.8f);
                }
                if (t >= 10 && t <= 30) {
                    float k = (t - 10) / 20f;
                    Vec3 p = leapFrom.lerp(diveAt, k).add(0, Mth.sin(k * Mth.PI) * 6, 0);
                    setPos(p.x, p.y, p.z);
                    setDeltaMovement(Vec3.ZERO);
                    if (t % 3 == 0) Fx.sound(level, p, SoundEvents.ENDER_DRAGON_FLAP, 1.5f, 0.7f);
                }
                if (t == 30) {
                    Vfx.ring(level, position().add(0, 0.1, 0), PLAGUE, 5.5f, 12, 1.2f);
                    Vfx.spray(level, FxStyle.DEBRIS, position(), 0x5A4A3A, 0x3A2A1A, 0.5f, 30, 0.6f, 0.8f);
                    Vfx.shake(level, position(), 1.0f, 26, 12);
                    Fx.sound(level, position(), SoundEvents.GENERIC_EXPLODE, 2f, 0.5f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 5)) {
                        SiegeDamage.deal(e, this, this, dmg(1.6f), Element.PHYSICAL, false);
                        SiegeDamage.push(e, position(), 1.6, 0.6);
                    }
                    crushBlocks(level);
                }
                if (t >= 44) {
                    cooldown(POUNCE, 240);
                    return true;
                }
            }
            case ROAR -> {
                if (t == 12) {
                    Fx.sound(level, position(), SoundEvents.ENDER_DRAGON_GROWL, 4f, 0.4f);
                    Vfx.ring(level, position().add(0, 0.2, 0), PLAGUE, 12, 16, 1.4f);
                    Vfx.screenFlash(level, position(), PLAGUE, 0.25f, 60, 12);
                    Vfx.shake(level, position(), 1.2f, 40, 20);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 12)) {
                        SiegeDamage.push(e, position(), 1.6, 0.4);
                        SiegeDamage.apply(e, ModEffects.DISORIENTED.get(), 50, 0);
                    }
                }
                return t >= 36;
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    private void clawSweep(ServerLevel level, float mult) {
        Vfx.slash(level, position().add(0, 1.5, 0), getYRot(), 20, 150, 6.5f, PLAGUE, 0xFFFFFF, 9, 0.45f);
        Fx.sound(level, position(), SoundEvents.PLAYER_ATTACK_SWEEP, 2f, 0.4f);
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 6.5)) {
            Vec3 to = e.position().subtract(position()).multiply(1, 0, 1).normalize();
            if (to.dot(fwd) < Math.cos(Math.toRadians(60))) continue;
            SiegeDamage.deal(e, this, this, dmg(mult), Element.PHYSICAL, false);
            SiegeDamage.applyStacking(e, ModEffects.BLEEDING.get(), 100, 3);
            SiegeDamage.push(e, position(), 1.2, 0.4);
        }
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (airborne() && tickCount % 15 == 0) Fx.sound(level, position(), SoundEvents.ENDER_DRAGON_FLAP, 1.5f, 0.6f);
        if (tickCount % 4 == 0) Vfx.spray(level, FxStyle.FLAME, position().add(0, 2, 0), PLAGUE, 0x1A6A2A, 0.3f, 1, 0.05f, 0.5f);
        if (phase() >= 3) {
            if (tickCount % 40 == 0) HazardZone.spawn(level, HazardZone.Kind.POISON_CLOUD, position(), 4f, 80, damageScale * 0.6f, this, true);
            if (tickCount % 6 == 0) Vfx.spray(level, FxStyle.SMOKE, position().add(0, 1.5, 0), 0x5A7A3A, 0x2A3A1A, 1.2f, 3, 0.15f, 0.3f);
        }
    }

    @Override
    protected void onPhase(ServerLevel level, int phase) {
        super.onPhase(level, phase);
        if (!flag()) {
            setFlag(true);
            impactTick = -1;
            startSkill(LAND);
        } else {
            startSkill(ROAR);
        }
    }

    @Override
    protected String phaseLine(int phase) {
        return phase >= 3 ? "enters its DEATH THROES - the air turns to rot!" : "crashes to the earth, wings torn!";
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (phase() >= 2) {
            setFlag(true);
            setNoGravity(false);
        }
    }
}
