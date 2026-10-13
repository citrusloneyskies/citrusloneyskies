package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.projectile.SiegeProjectile.ProjKind;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
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
import java.util.List;

/**
 * AEGIS-7 War Mech: a walking fortress. Twin rotary cannons spin up and hose the target; Missile
 * Swarm salvoes homing rockets; Seismic Stomp clears its feet; Repulsor Shield throws a dome over
 * nearby allies; Overdrive vents the reactor for double fire rate. Ultimate, Orbital Lance: it paints a
 * target with a designator laser and calls down a beam from orbit.
 */
public class Aegis7Hero extends HeroEntity {
    public static final int MISSILES = 6, STOMP = 7, SHIELD = 8, OVERDRIVE = 9;
    private int overdrive;
    private Vec3 strikeAt = Vec3.ZERO;

    public Aegis7Hero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.AEGIS7);
    }

    public boolean overdriven() {
        return flag();
    }

    @Override
    public Role role() {
        return Role.TANK;
    }

    @Override
    protected boolean isRanged() {
        return true;
    }

    @Override
    protected double attackRange() {
        return 21;
    }

    @Override
    protected double preferredRange() {
        return 11;
    }

    @Override
    protected int attackWindup() {
        return 1;
    }

    @Override
    protected int attackRecover() {
        return 1;
    }

    @Override
    protected int attackInterval() {
        return overdriven() ? 1 : 3;
    }

    @Override
    protected int comboLength() {
        return 2;
    }

    @Override
    protected float ultGainPerDamage() {
        return 0.05f;
    }

    @Override
    protected boolean canDodge() {
        return false;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"AEGIS-7 ONLINE. DEFENSIVE PROTOCOLS ENGAGED.", "UNIT 07 DEPLOYED. AWAITING TARGETS."};
            case ULT -> new String[]{"ORBITAL UPLINK ESTABLISHED. FIRING.", "TARGET PAINTED. STAND CLEAR."};
            case BOSS -> new String[]{"HIGH-VALUE TARGET ACQUIRED.", "THREAT LEVEL: EXTREME. ENGAGING."};
            case LOW_HP -> new String[]{"WARNING: HULL INTEGRITY CRITICAL.", "DAMAGE REPORT: SEVERE."};
            case LEVEL_UP -> new String[]{"FIRMWARE UPDATED. COMBAT EFFICIENCY +12%."};
            case IDLE -> new String[]{"*servos whir; sensors sweep the horizon*", "STANDING BY."};
            default -> new String[0];
        };
    }

    private Vec3 muzzle(int side) {
        Vec3 s = Vec3.directionFromRotation(0, getYRot() + (side == 0 ? 90 : -90)).scale(1.45);
        return position().add(0, getBbHeight() * 0.56, 0).add(s).add(look().scale(1.9));
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        Vec3 muzzle = muzzle(combo);
        Vec3 aim = t.position().add(0, t.getBbHeight() * 0.5, 0).add(random.nextGaussian() * 0.25, random.nextGaussian() * 0.25, random.nextGaussian() * 0.25);
        Vfx.beam(level, muzzle, aim, 0xFFD070, 0.05f, 2);
        if (combo == 0) Vfx.flash(level, muzzle, 0xFFC060, 0.45f, 2);
        if (tickCount % 3 == 0) Vfx.spray(level, FxStyle.SPARK, aim, 0xFFE0A0, 0xFF9030, 0.07f, 3, 0.3f, 0.3f);
        hit(t, dmg(1f), Element.PHYSICAL);
        if (tickCount % 4 == 0) playSound(SoundEvents.CROSSBOW_SHOOT, 0.6f, 2f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (ready(STOMP) && crowd(level, position(), 4.5) >= 2) return STOMP;
        if (ready(SHIELD)) {
            int hurt = 0;
            for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 6)) {
                if (a != this && !(a instanceof com.lastbastion.entity.turret.TurretEntity) && a.getHealth() < a.getMaxHealth() * 0.6f) hurt++;
            }
            if (hurt >= 1 && crowd(level, position(), 10) >= 3) return SHIELD;
        }
        if (ready(OVERDRIVE) && (getHealth() < getMaxHealth() * 0.5f || (t instanceof SiegeEnemy se && se.isHighPriority()) || crowd(level, position(), 14) >= 8)) return OVERDRIVE;
        if (ready(MISSILES) && enemiesNear(level, 20).size() >= 3) return MISSILES;
        return 0;
    }

    @Override
    protected boolean wantsUltimate(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (t == null) return false;
        if (t instanceof SiegeEnemy se && se.isHighPriority()) {
            strikeAt = t.position();
            return true;
        }
        Vec3 c = bestCluster(level, 22, 4, 6);
        if (c != null) {
            strikeAt = c;
            return true;
        }
        return false;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case MISSILES -> {
                if (t == 8) playSound(SoundEvents.IRON_TRAPDOOR_OPEN, 1.5f, 0.6f);
                if (t >= 12 && t < 28 && t % 2 == 0) {
                    List<LivingEntity> targets = enemiesNear(level, 22);
                    targets.sort(Comparator.comparingDouble(e -> -e.getHealth()));
                    if (!targets.isEmpty()) {
                        LivingEntity e = targets.get(((t - 12) / 2) % targets.size());
                        Vec3 pod = position().add(0, getBbHeight() * 0.92, 0).add(Vec3.directionFromRotation(0, getYRot() + 90).scale(1.2));
                        SiegeProjectile m = SiegeProjectile.create(level, ProjKind.MISSILE, this, pod).payload(dmg(3f), Element.EXPLOSIVE).area(2.5f);
                        m.setDeltaMovement(random.nextGaussian() * 0.2, 0.75, random.nextGaussian() * 0.2);
                        m.homing(e, 0.13f);
                        m.life = 120;
                        m.onEntityHit = (p, hitE) -> gainUlt(p.damage * ultGainPerDamage());
                        m.fire(level);
                        Vfx.spray(level, FxStyle.SMOKE, pod, 0xB8B0A8, 0x6A6460, 0.6f, 3, 0.08f, 1);
                        playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.7f);
                    }
                }
                if (t >= 34) {
                    cooldown(MISSILES, 220);
                    return true;
                }
            }
            case STOMP -> {
                if (t == 16) {
                    Fx.shockwave(level, position(), 5, 0xFFB040);
                    playSound(SoundEvents.GENERIC_EXPLODE, 1.5f, 0.7f);
                    for (LivingEntity e : enemiesNear(level, 5)) {
                        hit(e, dmg(6f), Element.PHYSICAL);
                        SiegeDamage.apply(e, ModEffects.CONCUSSED.get(), 80, 0);
                        if (!(e instanceof SiegeEnemy se && se.isHighPriority())) SiegeDamage.push(e, position(), 1.3, 0.5);
                    }
                }
                if (t >= 26) {
                    cooldown(STOMP, 160);
                    return true;
                }
            }
            case SHIELD -> {
                if (t == 10) {
                    Vfx.dome(level, position(), 0x6FE3FF, 5.5f, 120, this);
                    playSound(SoundEvents.BEACON_ACTIVATE, 1.5f, 1.8f);
                    for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 6)) {
                        a.addEffect(new MobEffectInstance(ModEffects.AEGIS.get(), 120, 1));
                    }
                }
                if (t >= 16) {
                    cooldown(SHIELD, 480);
                    return true;
                }
            }
            case OVERDRIVE -> {
                if (t == 10) {
                    setFlag(true);
                    overdrive = 160;
                    Vfx.spray(level, FxStyle.SMOKE, position().add(0, getBbHeight() * 0.8, 0), 0xE8E8E8, 0xB8B8B8, 1.2f, 20, 0.3f, 0.8f);
                    playSound(SoundEvents.BLAZE_AMBIENT, 1.5f, 0.5f);
                    playSound(SoundEvents.BEACON_POWER_SELECT, 1f, 0.6f);
                }
                if (t >= 16) {
                    cooldown(OVERDRIVE, 400);
                    return true;
                }
            }
            case ULT -> {
                faceTowards(strikeAt);
                if (t == 2) {
                    playSound(SoundEvents.BEACON_POWER_SELECT, 1.5f, 1.8f);
                    Vfx.teleCircle(level, strikeAt.add(0, 0.05, 0), 4.5f, 36, 0x7FE8FF, false);
                }
                if (t >= 2 && t < 36 && t % 3 == 0) {
                    Vec3 eye = position().add(0, getBbHeight() * 0.85, 0).add(look().scale(0.8));
                    Vfx.beam(level, eye, strikeAt.add(0, 0.2, 0), 0xFF3030, 0.04f, 4);
                }
                if (t == 38) {
                    Vfx.pillar(level, strikeAt, 0x7FE8FF, 2.6f, 60, 30);
                    Vfx.sphere(level, strikeAt.add(0, 1, 0), 0xB8F4FF, 5f, 12);
                    Fx.shockwave(level, strikeAt, 6, 0x7FE8FF);
                    Vfx.screenFlash(level, strikeAt, 0xD8F8FF, 0.35f, 40, 10);
                    Vfx.shake(level, strikeAt, 1.6f, 40, 14);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, strikeAt, 4.8)) {
                        hit(e, dmg(10f) * (e instanceof SiegeEnemy se && se.isHighPriority() ? 1.5f : 1f), Element.ARCANE);
                        e.setSecondsOnFire(4);
                    }
                    HazardZone.spawn(level, HazardZone.Kind.FIRE, strikeAt, 3.5f, 80, dmg(0.3f), this, false);
                    playSound(SoundEvents.WARDEN_SONIC_BOOM, 3f, 0.6f);
                }
                if (t >= 50) return true;
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void passiveTick(ServerLevel level) {
        if (overdrive > 0) {
            overdrive--;
            if (tickCount % 3 == 0) Vfx.particles(level, com.lastbastion.fx.FxParticleOptions.of(FxStyle.SMOKE, 0xD8D8D8, 0.6f),
                    position().add(0, getBbHeight() * 0.7, 0).add(look().scale(-1)), 1, 0.3, 0.2, 0.3, 0.02);
            if (overdrive == 0) setFlag(false);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) amount *= 0.6f;
        return super.hurt(source, amount);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_EXPLODE;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.2f, 0.5f);
    }
}
