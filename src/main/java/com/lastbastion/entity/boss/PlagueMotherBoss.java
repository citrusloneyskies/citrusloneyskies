package com.lastbastion.entity.boss;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.entity.zombie.ZombieKind;
import com.lastbastion.entity.zombie.ZombieRank;
import com.lastbastion.entity.zombie.ZombieSpawner;
import com.lastbastion.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Plague Mother (wave 10). A swollen broodmother on six spindly legs.
 * <ul>
 *   <li><b>Toxic Spew</b> - sweeping cone of bile (Corroded, poison).</li>
 *   <li><b>Brood Birth</b> - her sac splits and four Runners spill out.</li>
 *   <li><b>Acid Rain</b> - a sickly cloud rains acid over the target's area.</li>
 *   <li><b>Phase 2 (50%)</b> - pustules burst into poison clouds; gains <b>Crushing Leap</b>.</li>
 * </ul>
 */
public class PlagueMotherBoss extends SiegeBoss {
    public static final int SPEW = 3, BROOD = 4, RAIN = 5, LEAP = 6;
    private Vec3 leapTarget = Vec3.ZERO;

    public PlagueMotherBoss(EntityType<? extends SiegeBoss> type, Level level) {
        super(type, level, BossKind.PLAGUE_MOTHER);
    }

    @Override
    public float elementMultiplier(Element e) {
        return e == Element.POISON ? 0f : e == Element.FIRE ? 1.25f : super.elementMultiplier(e);
    }

    @Override
    protected double engageRange() {
        return 12;
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dt, double dc) {
        if (ready(BROOD) && tickCount > 60) return BROOD;
        if (t != null && dt < 9 && ready(SPEW)) return SPEW;
        if (phase() >= 2 && t != null && dt > 5 && dt < 16 && ready(LEAP)) return LEAP;
        if ((t != null && dt < 22 || dc < 22) && ready(RAIN)) return RAIN;
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case SPEW -> {
                Vec3 aim = aimPoint();
                face(aim);
                if (t > 12 && t < 52 && t % 4 == 0) {
                    Vec3 mouth = position().add(0, getBbHeight() * 0.55, 0).add(Vec3.directionFromRotation(0, getYRot()).scale(1.5));
                    float sweep = (float) Math.sin((t - 12) / 40.0 * Math.PI * 2) * 25f;
                    Vec3 dir = Vec3.directionFromRotation(15, getYRot() + sweep);
                    Fx.cone(level, mouth, dir, 9, 18, Fx.dust(0x9BD940, 1.6f), 25);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, mouth.add(dir.scale(4.5)), 4.5)) {
                        Vec3 to = e.position().subtract(mouth).normalize();
                        if (to.dot(dir) > 0.75) {
                            SiegeDamage.deal(e, this, this, dmg(0.35f), Element.POISON, false);
                            SiegeDamage.applyStacking(e, ModEffects.CORRODED.get(), 100, 4);
                        }
                    }
                    if (t % 8 == 0) Fx.sound(level, mouth, SoundEvents.SLIME_SQUISH, 1.5f, 0.5f);
                }
                if (t >= 60) {
                    cooldown(SPEW, 140);
                    return true;
                }
            }
            case BROOD -> {
                if (t == 20) {
                    Fx.sound(level, position(), SoundEvents.TURTLE_EGG_HATCH, 2f, 0.5f);
                    level.sendParticles(Fx.dust(0x7BD940, 2f), getX(), getY() + 1, getZ(), 40, 1.2, 0.6, 1.2, 0);
                    for (int i = 0; i < 4; i++) {
                        Vec3 p = position().add(random.nextGaussian() * 1.5, 0.5, random.nextGaussian() * 1.5);
                        SiegeZombie z = ZombieSpawner.spawn(level, ZombieKind.RUNNER, ZombieRank.byId(1 + phase()), 0, p, true, false);
                        if (z != null) {
                            z.setMinion(true);
                            z.setDeltaMovement(random.nextGaussian() * 0.3, 0.5, random.nextGaussian() * 0.3);
                        }
                    }
                }
                if (t >= 36) {
                    cooldown(BROOD, 320);
                    return true;
                }
            }
            case RAIN -> {
                if (t == 18) {
                    Vec3 at = target != null ? target.position() : aimPoint();
                    HazardZone.spawn(level, HazardZone.Kind.PLAGUE_RAIN, at, 5f, 140, damageScale, this, true);
                    HazardZone.spawn(level, HazardZone.Kind.ACID, at, 4f, 140, damageScale, this, true);
                    Fx.sound(level, at, SoundEvents.WEATHER_RAIN_ABOVE, 2f, 0.6f);
                }
                if (t >= 30) {
                    cooldown(RAIN, 260);
                    return true;
                }
            }
            case LEAP -> {
                if (t == 10) {
                    leapTarget = target != null ? target.position() : aimPoint();
                    Vec3 d = leapTarget.subtract(position());
                    setDeltaMovement(d.x * 0.09, 1.0, d.z * 0.09);
                    hasImpulse = true;
                }
                if (t > 14 && onGround()) {
                    Fx.shockwave(level, position(), 6, 0x7BD940);
                    Fx.sound(level, position(), SoundEvents.GENERIC_EXPLODE, 2f, 0.6f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 6)) {
                        SiegeDamage.deal(e, this, this, dmg(1.4f), Element.PHYSICAL, false);
                        SiegeDamage.push(e, position(), 1.4, 0.6);
                    }
                    HazardZone.spawn(level, HazardZone.Kind.POISON_CLOUD, position(), 4f, 80, damageScale, this, true);
                    cooldown(LEAP, 180);
                    return true;
                }
                if (t > 60) return true;
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
        if (tickCount % 6 == 0) level.sendParticles(Fx.dust(0x9BD940, 1.2f), getX(), getY() + 0.3, getZ(), 2, 1.2, 0.1, 1.2, 0);
    }

    @Override
    protected void onPhase(ServerLevel level, int phase) {
        super.onPhase(level, phase);
        for (int i = 0; i < 5; i++) {
            Vec3 p = position().add(random.nextGaussian() * 4, 0, random.nextGaussian() * 4);
            HazardZone.spawn(level, HazardZone.Kind.POISON_CLOUD, p, 3f, 200, damageScale, this, true);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 2, getZ(), 6, 1.5, 1, 1.5, 0);
    }

    @Override
    protected String phaseLine(int phase) {
        return "bursts with pestilence!";
    }
}
