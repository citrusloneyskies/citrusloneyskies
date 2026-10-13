package com.lastbastion.entity.boss;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Morvathrax the Rotwing (wave 30). A colossal undead dragon: exposed ribs, tattered membrane wings,
 * a skull crowned with horns and a throat glowing with plague-fire.
 * <p><b>Aerial phase</b>: circles the core at altitude.
 * <ul>
 *   <li><b>Plague Strafe</b> - flies a straight run spraying green fire downward.</li>
 *   <li><b>Death Dive</b> - plummets onto its target or the core with a crater-making impact.</li>
 *   <li><b>Bone Hail</b> - rains flaming bone shards over the core.</li>
 * </ul>
 * <p><b>Grounded phase (40%)</b>: crashes down and fights on foot.
 * <ul>
 *   <li><b>Tail Sweep</b> - 360 degree sweep that hurls defenders away.</li>
 *   <li><b>Plague Breath</b> - sweeping cone of plague-fire.</li>
 *   <li><b>Wing Buffet</b> - gale that blasts defenders back and disorients them.</li>
 * </ul>
 */
public class MorvathraxBoss extends SiegeBoss {
    public static final int STRAFE = 3, DIVE = 4, HAIL = 5, TAIL = 6, BREATH = 7, BUFFET = 8, LAND = 9;
    private double orbit;
    private Vec3 runStart = Vec3.ZERO, runDir = Vec3.ZERO, diveAt = Vec3.ZERO;

    public MorvathraxBoss(EntityType<? extends SiegeBoss> type, Level level) {
        super(type, level, BossKind.MORVATHRAX);
        setNoGravity(true);
    }

    /** True while airborne (synced via the boss flag). */
    public boolean airborne() {
        return !flag();
    }

    @Override
    protected void onArrive(ServerLevel level) {
        super.onArrive(level);
        setPos(getX(), getY() + 14, getZ());
    }

    @Override
    public float elementMultiplier(Element e) {
        return e == Element.FIRE ? 0.3f : e == Element.HOLY ? 1.5f : e == Element.LIGHTNING && airborne() ? 1.3f : super.elementMultiplier(e);
    }

    @Override
    protected float phaseThreshold(int phase) {
        return phase == 2 ? 0.4f : -1f;
    }

    @Override
    protected double meleeReach() {
        return 4.5;
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
        if (!SiegeDamage.defendersAround(level, position(), 6).isEmpty() && ready(TAIL)) return TAIL;
        if (t != null && dt < 12 && ready(BREATH)) return BREATH;
        if (!SiegeDamage.defendersAround(level, position(), 9).isEmpty() && ready(BUFFET)) return BUFFET;
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
                    runStart = focus.subtract(runDir.scale(16)).add(0, 9, 0);
                    Fx.sound(level, position(), SoundEvents.ENDER_DRAGON_GROWL, 3f, 0.6f);
                }
                if (t < 40) steer(runStart, 0.9);
                else if (t < 80) {
                    setDeltaMovement(runDir.scale(0.85));
                    face(position().add(runDir));
                    Vec3 mouth = position().add(runDir.scale(3)).add(0, 0.5, 0);
                    Vec3 down = runDir.scale(0.6).add(0, -1, 0);
                    Fx.cone(level, mouth, down, 10, 14, ParticleTypes.SOUL_FIRE_FLAME, 18);
                    if (t % 4 == 0) {
                        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(getX()), (int) Math.floor(getZ()));
                        Vec3 g = new Vec3(getX(), y, getZ());
                        HazardZone.spawn(level, HazardZone.Kind.SOUL_FIRE, g, 2.2f, 80, damageScale, this, true);
                        for (LivingEntity e : SiegeDamage.defendersAround(level, g, 3)) SiegeDamage.deal(e, this, this, dmg(0.5f), Element.FIRE, false);
                        if (core != null && g.distanceTo(c) < 3) SiegeManager.damageCore(level, dmg(0.6f), this);
                    }
                    if (t % 10 == 0) Fx.sound(level, mouth, SoundEvents.ENDER_DRAGON_SHOOT, 2f, 0.6f);
                } else {
                    cooldown(STRAFE, 160);
                    return true;
                }
            }
            case DIVE -> {
                if (t == 1) {
                    diveAt = target != null ? target.position() : c;
                    Fx.sound(level, position(), SoundEvents.PHANTOM_SWOOP, 3f, 0.4f);
                }
                if (t < 25) {
                    steer(diveAt.add(0, 16, 0), 0.8);
                } else {
                    Vec3 v = diveAt.subtract(position());
                    setDeltaMovement(v.normalize().scale(1.4));
                    face(diveAt);
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 4, 1, 1, 1, 0.02);
                    if (v.length() < 3 || onGround() || t > 70) {
                        Fx.explosion(level, position(), 5);
                        Fx.shockwave(level, position(), 9, 0x6BFF8A);
                        for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 7)) {
                            SiegeDamage.deal(e, this, this, dmg(1.8f), Element.PHYSICAL, false);
                            SiegeDamage.push(e, position(), 1.8, 0.8);
                        }
                        if (core != null && position().distanceTo(c) < 6) SiegeManager.damageCore(level, dmg(2.5f), this);
                        crushBlocks(level);
                        setDeltaMovement(0, 0.8, 0);
                        cooldown(DIVE, 260);
                        return true;
                    }
                }
            }
            case HAIL -> {
                tickMovementAndMelee(level, core, 0, 0);
                if (t == 15) {
                    HazardZone.spawn(level, HazardZone.Kind.METEOR_RAIN, c, 9f, 100, damageScale * 0.8f, this, true);
                    Fx.sound(level, position(), SoundEvents.ENDER_DRAGON_GROWL, 3f, 0.4f);
                }
                if (t >= 30) {
                    cooldown(HAIL, 300);
                    return true;
                }
            }
            case LAND -> {
                setNoGravity(false);
                setDeltaMovement(getDeltaMovement().add(0, -0.2, 0));
                if (onGround() || t > 80) {
                    Fx.explosion(level, position(), 6);
                    Fx.shockwave(level, position(), 10, 0x6BFF8A);
                    Fx.sound(level, position(), SoundEvents.ENDER_DRAGON_GROWL, 4f, 0.3f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 9)) {
                        SiegeDamage.deal(e, this, this, dmg(1.5f), Element.PHYSICAL, false);
                        SiegeDamage.push(e, position(), 2, 0.8);
                    }
                    return true;
                }
            }
            case TAIL -> {
                if (t == 20) {
                    Fx.ring(level, position().add(0, 1, 0), 5, ParticleTypes.SWEEP_ATTACK, 12);
                    Fx.sound(level, position(), SoundEvents.PLAYER_ATTACK_SWEEP, 2f, 0.3f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 7)) {
                        SiegeDamage.deal(e, this, this, dmg(1.3f), Element.PHYSICAL, false);
                        SiegeDamage.push(e, position(), 2.2, 0.7);
                    }
                }
                if (t >= 36) {
                    cooldown(TAIL, 110);
                    return true;
                }
            }
            case BREATH -> {
                if (target != null && t < 20) face(target.position());
                if (t > 18 && t < 62) {
                    float sweep = (float) Math.sin((t - 18) / 44.0 * Math.PI) * 40f - 20f;
                    Vec3 dir = Vec3.directionFromRotation(20, getYRot() + sweep);
                    Vec3 mouth = position().add(0, 3, 0).add(Vec3.directionFromRotation(0, getYRot()).scale(4));
                    Fx.cone(level, mouth, dir, 12, 16, ParticleTypes.SOUL_FIRE_FLAME, 25);
                    if (t % 4 == 0) {
                        for (LivingEntity e : SiegeDamage.defendersAround(level, mouth.add(dir.scale(6)), 6)) {
                            if (e.position().subtract(mouth).normalize().dot(dir) > 0.8) {
                                SiegeDamage.deal(e, this, this, dmg(0.45f), Element.FIRE, false);
                                SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 80, 3);
                            }
                        }
                    }
                    if (t % 10 == 0) Fx.sound(level, mouth, SoundEvents.ENDER_DRAGON_SHOOT, 2f, 0.5f);
                }
                if (t >= 70) {
                    cooldown(BREATH, 150);
                    return true;
                }
            }
            case BUFFET -> {
                if (t == 16) {
                    Fx.shockwave(level, position(), 9, 0xCFFFF0);
                    Fx.sound(level, position(), SoundEvents.ENDER_DRAGON_FLAP, 3f, 0.5f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 10)) {
                        SiegeDamage.deal(e, this, this, dmg(0.6f), Element.WIND, false);
                        SiegeDamage.push(e, position(), 2.5, 0.6);
                        SiegeDamage.apply(e, ModEffects.DISORIENTED.get(), 60, 0);
                    }
                }
                if (t >= 30) {
                    cooldown(BUFFET, 180);
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
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (airborne() && tickCount % 15 == 0) Fx.sound(level, position(), SoundEvents.ENDER_DRAGON_FLAP, 1.5f, 0.6f);
        if (tickCount % 3 == 0) level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 2, getZ(), 1, 1, 0.5, 1, 0.01);
    }

    @Override
    protected void onPhase(ServerLevel level, int phase) {
        super.onPhase(level, phase);
        setFlag(true);
        startSkill(LAND);
    }

    @Override
    protected String phaseLine(int phase) {
        return "crashes to the earth, wings torn!";
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
