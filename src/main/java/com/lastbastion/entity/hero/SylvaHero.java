package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.projectile.SiegeProjectile.ProjKind;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Sylva the Huntress: an elven archer who kites at long range. Aimed Shot (every third a double shot),
 * Piercing Arrow down a line, Arrow Rain on crowds, a vine Snare Trap for chargers, an Evasive Roll that
 * fires mid-air, and Hunter's Mark on the biggest threat. Ultimate, Wild Hunt: a spirit stag rises
 * behind her while she looses a storm of homing star-arrows.
 */
public class SylvaHero extends HeroEntity {
    public static final int PIERCE = 6, RAIN = 7, TRAP = 8, ROLL = 9, MARK = 10;
    private Vec3 rainAt = Vec3.ZERO;
    private int shots;

    public SylvaHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.SYLVA);
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
        return 22;
    }

    @Override
    protected double preferredRange() {
        return 14;
    }

    @Override
    protected int attackWindup() {
        return 11;
    }

    @Override
    protected int attackInterval() {
        return 18;
    }

    @Override
    protected float ultGainPerDamage() {
        return 0.15f;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"The forest sent me. Point me at the rot.", "Swift and silent. Where are they?"};
            case ULT -> new String[]{"Ride with me, old spirit!", "The Wild Hunt is loosed!"};
            case BOSS -> new String[]{"That one's mine. Mark it!", "Big prey. Bigger arrows."};
            case LOW_HP -> new String[]{"I'm hit... I'm hit!", "Need cover!"};
            case LEVEL_UP -> new String[]{"My aim grows truer.", "The woods remember this hunt."};
            case RETREAT -> new String[]{"Falling back to the core!", "Regrouping. Keep them busy."};
            case IDLE -> new String[]{"*restrings her bow*", "Quiet. Too quiet."};
            default -> new String[0];
        };
    }

    private Vec3 bowPos() {
        return position().add(0, getBbHeight() * 0.75, 0).add(look().scale(0.6));
    }

    private void loose(ServerLevel level, LivingEntity t, float mult, float spreadDeg) {
        Vec3 from = bowPos();
        Vec3 aim = t.position().add(0, t.getBbHeight() * 0.55, 0).add(t.getDeltaMovement().scale(6));
        SiegeProjectile p = SiegeProjectile.create(level, ProjKind.ARROW, this, from).payload(dmg(mult), Element.PHYSICAL);
        p.onEntityHit = (pr, e) -> gainUlt(pr.damage * ultGainPerDamage());
        if (spreadDeg != 0) {
            Vec3 d = aim.subtract(from);
            double a = Math.toRadians(spreadDeg);
            aim = from.add(new Vec3(d.x * Math.cos(a) - d.z * Math.sin(a), d.y, d.x * Math.sin(a) + d.z * Math.cos(a)));
        }
        p.aim(aim, 2.4f).fire(level);
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        shots++;
        loose(level, t, 1f, 0);
        if (shots % 3 == 0) loose(level, t, 0.8f, 4);
        playSound(SoundEvents.ARROW_SHOOT, 1f, 1.1f + random.nextFloat() * 0.2f);
        Vfx.spray(level, FxStyle.LEAF, bowPos(), 0x6AD07A, 0xD8B040, 0.12f, 2, 0.1f, 0);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (t == null) return 0;
        LivingEntity close = null;
        for (LivingEntity e : enemiesNear(level, 3.2)) {
            close = e;
            break;
        }
        if (close != null && ready(ROLL)) return ROLL;
        if (close == null && ready(TRAP)) {
            for (LivingEntity e : enemiesNear(level, 6)) {
                if (e.getDeltaMovement().horizontalDistanceSqr() > 0.01) return TRAP;
            }
        }
        if (ready(MARK) && t instanceof SiegeEnemy se && se.isHighPriority()) return MARK;
        if (ready(RAIN)) {
            Vec3 c = bestCluster(level, 22, 3.5, 4);
            if (c != null) {
                rainAt = c;
                return RAIN;
            }
        }
        if (ready(PIERCE) && dist > 5 && crowd(level, t.position(), 4) >= 2) return PIERCE;
        return 0;
    }

    @Override
    protected boolean wantsUltimate(ServerLevel level, @Nullable LivingEntity t, double dist) {
        return t != null && (enemiesNear(level, 22).size() >= 5 || (t instanceof SiegeEnemy se && se.isHighPriority()));
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case PIERCE -> {
                if (target != null) faceTowards(target.position());
                if (t == 1) playSound(SoundEvents.CROSSBOW_LOADING_MIDDLE, 1f, 1.3f);
                if (t == 16 && target != null) {
                    Vec3 from = bowPos();
                    SiegeProjectile p = SiegeProjectile.create(level, ProjKind.STAR_ARROW, this, from).payload(dmg(2.2f), Element.PHYSICAL)
                            .status(ModEffects.BLEEDING, 80, 2);
                    p.pierce = 6;
                    p.onEntityHit = (pr, e) -> gainUlt(pr.damage * ultGainPerDamage());
                    p.aim(target.position().add(0, target.getBbHeight() * 0.5, 0), 3.2f).scale(1.3f).fire(level);
                    Vfx.flash(level, from, 0xE8FFB0, 0.8f, 5);
                    playSound(SoundEvents.ARROW_SHOOT, 1.4f, 0.7f);
                }
                if (t >= 22) {
                    cooldown(PIERCE, 120);
                    return true;
                }
            }
            case RAIN -> {
                faceTowards(rainAt);
                if (t == 2) {
                    Vfx.teleCircle(level, rainAt.add(0, 0.05, 0), 4f, 26, 0x6AD07A, false);
                    playSound(SoundEvents.CROSSBOW_QUICK_CHARGE_3, 1f, 1.2f);
                }
                if (t == 10) {
                    Vec3 up = bowPos();
                    for (int i = 0; i < 5; i++) {
                        SiegeProjectile p = SiegeProjectile.create(level, ProjKind.ARROW, this, up);
                        p.setDeltaMovement((random.nextDouble() - 0.5) * 0.3, 1.6, (random.nextDouble() - 0.5) * 0.3);
                        p.life = 10;
                        p.fire(level);
                    }
                    playSound(SoundEvents.ARROW_SHOOT, 1.4f, 0.8f);
                }
                if (t >= 22 && t <= 46 && t % 4 == 2) {
                    for (int i = 0; i < 6; i++) {
                        Vec3 at = rainAt.add((random.nextDouble() - 0.5) * 7, 0, (random.nextDouble() - 0.5) * 7);
                        SiegeProjectile p = SiegeProjectile.create(level, ProjKind.ARROW, this, at.add(random.nextGaussian(), 16, random.nextGaussian()))
                                .payload(dmg(0.9f), Element.PHYSICAL);
                        p.gravity = 0;
                        p.aim(at, 2.2f).fire(level);
                    }
                }
                if (t >= 48) {
                    cooldown(RAIN, 300);
                    return true;
                }
            }
            case TRAP -> {
                if (t == 6) {
                    LivingEntity rush = null;
                    double best = 1e9;
                    for (LivingEntity e : enemiesNear(level, 7)) {
                        double d = distanceToSqr(e);
                        if (d < best) {
                            best = d;
                            rush = e;
                        }
                    }
                    if (rush != null) {
                        Vec3 at = rush.position();
                        Vfx.spray(level, FxStyle.LEAF, at.add(0, 0.3, 0), 0x4A9A3A, 0x2A5A1A, 0.2f, 16, 0.25f, 0.6f);
                        Vfx.ring(level, at.add(0, 0.05, 0), 0x4A9A3A, 2.4f, 12, 0.5f);
                        for (LivingEntity e : SiegeDamage.enemiesAround(level, at, 2.4)) {
                            hit(e, dmg(1.2f), Element.PHYSICAL);
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 5));
                        }
                        playSound(SoundEvents.SWEET_BERRY_BUSH_BREAK, 1.5f, 0.6f);
                    }
                }
                if (t >= 12) {
                    cooldown(TRAP, 220);
                    return true;
                }
            }
            case ROLL -> {
                if (t == 1) {
                    LivingEntity threat = null;
                    double best = 1e9;
                    for (LivingEntity e : enemiesNear(level, 4)) {
                        double d = distanceToSqr(e);
                        if (d < best) {
                            best = d;
                            threat = e;
                        }
                    }
                    Vec3 away = threat != null ? position().subtract(threat.position()).multiply(1, 0, 1) : look().scale(-1);
                    if (away.lengthSqr() < 1e-3) away = look().scale(-1);
                    away = away.normalize();
                    setDeltaMovement(away.x * 1.1, 0.42, away.z * 1.1);
                    hasImpulse = true;
                    if (threat != null) faceNow(threat.position());
                    Vfx.dash(level, position().add(0, 0.8, 0), position().add(away.scale(3)).add(0, 0.8, 0), 0x6AD07A, 0.5f, 8);
                }
                if (t == 7 && target != null) {
                    loose(level, target, 1.2f, 0);
                    playSound(SoundEvents.ARROW_SHOOT, 1f, 1.4f);
                }
                if (t >= 14) {
                    cooldown(ROLL, 90);
                    return true;
                }
            }
            case MARK -> {
                if (target != null) faceTowards(target.position());
                if (t == 8 && target != null) {
                    SiegeDamage.apply(target, ModEffects.MARKED.get(), 240, 2);
                    Vfx.sigil(level, target.position().add(0, 0.05, 0), 0x6AD07A, 1.4f + target.getBbWidth(), 60, 3f);
                    Vfx.flash(level, target.position().add(0, target.getBbHeight() + 0.5, 0), 0x6AD07A, 0.8f, 10);
                    playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.2f, 1.4f);
                }
                if (t >= 14) {
                    cooldown(MARK, 360);
                    return true;
                }
            }
            case ULT -> {
                if (t == 1) {
                    Vfx.vortex(level, position(), 0x6AD07A, 0xE8FFB0, 1.6f, 4f, 70, this);
                    Vfx.sigil(level, position().add(0, 0.05, 0), 0x6AD07A, 2.6f, 70, 3f);
                    playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5f, 1.4f);
                }
                if (t >= 8 && t <= 68 && t % 5 == 3) {
                    List<LivingEntity> foes = enemiesNear(level, 24);
                    if (!foes.isEmpty()) {
                        LivingEntity e = foes.get(random.nextInt(foes.size()));
                        faceTowards(e.position());
                        SiegeProjectile p = SiegeProjectile.create(level, ProjKind.STAR_ARROW, this, bowPos()).payload(dmg(1.8f), Element.HOLY);
                        p.homing(e, 0.25f).aim(e.position().add(0, e.getBbHeight() * 0.5 + 2, 0), 1.8f).fire(level);
                        playSound(SoundEvents.ARROW_SHOOT, 1f, 1.5f);
                    }
                }
                if (t >= 74) return true;
            }
            default -> {
                return true;
            }
        }
        return false;
    }
}
