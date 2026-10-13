package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.projectile.SiegeProjectile.ProjKind;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
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
 * Anubis, Judge of the Dead: a controller and executioner. Sceptre strikes up close and Sand Bolts at
 * range; Weighing of Hearts executes a wounded foe outright (bosses lose a slice of their health);
 * Sandstorm blinds and slows a crowd; Soul Siphon drains an enemy to heal an ally or the core; Curse of
 * the Pharaoh makes victims burst into sand when they die. Ultimate, Hall of Judgement: the great scales
 * appear and every enemy around him is judged.
 */
public class AnubisHero extends HeroEntity {
    public static final int WEIGH = 6, STORM = 7, SIPHON = 8, CURSE = 9;
    @Nullable
    private LivingEntity siphonFrom;
    @Nullable
    private LivingEntity siphonTo;
    private Vec3 castAt = Vec3.ZERO;

    public AnubisHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.ANUBIS);
    }

    @Override
    public Role role() {
        return Role.SUPPORT;
    }

    @Override
    protected boolean isRanged() {
        return true;
    }

    @Override
    protected double attackRange() {
        return 16;
    }

    @Override
    protected double preferredRange() {
        return 8;
    }

    @Override
    protected int attackWindup() {
        return 8;
    }

    @Override
    protected int attackInterval() {
        return 18;
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
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"The Judge of the Dead has come for what is his.", "These souls are long overdue."};
            case ULT -> new String[]{"STAND BEFORE THE SCALES!", "Your hearts are weighed... and found wanting."};
            case BOSS -> new String[]{"Even you must face the scales.", "A heavy heart. It will sink."};
            case LOW_HP -> new String[]{"I have walked the underworld. This is nothing.", "Sand and silence..."};
            case LEVEL_UP -> new String[]{"The dead whisper my name louder."};
            case RETREAT -> new String[]{"I withdraw to the threshold."};
            case IDLE -> new String[]{"*the scales tip, then settle*", "The dead are restless tonight."};
            default -> new String[0];
        };
    }

    private Vec3 hand() {
        return position().add(0, getBbHeight() * 0.62, 0).add(look().scale(0.7));
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        if (distanceTo(t) < 3.6) {
            arc(level, 3, 120, dmg(1.1f), Element.HOLY);
            Vfx.slash(level, position().add(0, getBbHeight() * 0.55, 0), getYRot(), combo == 0 ? 40 : -40, combo == 0 ? 150 : -150,
                    2.6f, 0xE0B040, 0xFFF4C0, 8, 0.28f);
            playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.8f);
            return;
        }
        SiegeProjectile p = SiegeProjectile.create(level, ProjKind.SAND_ORB, this, hand()).payload(dmg(0.9f), Element.PHYSICAL);
        p.homing(t, 0.15f).aim(t.position().add(0, t.getBbHeight() * 0.5, 0), 1.2f).scale(1.2f);
        p.onEntityHit = (pr, e) -> {
            gainUlt(pr.damage * ultGainPerDamage());
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        };
        p.fire(level);
        playSound(SoundEvents.SAND_BREAK, 1f, 0.7f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (ready(SIPHON)) {
            LivingEntity ally = hurtAlly(level, 14, 0.5f);
            if (ally != null && t != null && dist < 12) {
                siphonTo = ally;
                return SIPHON;
            }
        }
        if (t == null) return 0;
        if (ready(WEIGH)) {
            for (LivingEntity e : enemiesNear(level, 14)) {
                boolean boss = e instanceof SiegeEnemy se && se.isHighPriority();
                if ((!boss && e.getHealth() < e.getMaxHealth() * 0.3f && e.getMaxHealth() > 30) || (boss && random.nextInt(6) == 0)) {
                    target = e;
                    return WEIGH;
                }
            }
        }
        if (ready(STORM)) {
            Vec3 c = bestCluster(level, 18, 4, 4);
            if (c != null) {
                castAt = c;
                return STORM;
            }
        }
        if (ready(CURSE) && enemiesNear(level, 10).size() >= 3) return CURSE;
        return 0;
    }

    @Override
    protected boolean wantsUltimate(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (t == null) return false;
        int n = 0, weak = 0;
        for (LivingEntity e : enemiesNear(level, 12)) {
            n++;
            if (e.getHealth() < e.getMaxHealth() * 0.4f) weak++;
        }
        return n >= 6 || weak >= 3 || (t instanceof SiegeEnemy se && se.isHighPriority() && dist < 12);
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case WEIGH -> {
                if (target != null) faceTowards(target.position());
                if (t == 2 && target != null) {
                    Vfx.pillar(level, target.position(), 0xE0B040, 0.6f + target.getBbWidth() * 0.4f, 6, 24);
                    Vfx.sigil(level, target.position().add(0, 0.04, 0), 0xE0B040, 1.2f + target.getBbWidth(), 24, 6f);
                    playSound(SoundEvents.BELL_RESONATE, 1.5f, 0.6f);
                }
                if (t == 20 && target != null && target.isAlive()) {
                    boolean boss = target instanceof SiegeEnemy se && se.isHighPriority();
                    Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
                    if (!boss && target.getHealth() < target.getMaxHealth() * 0.32f) {
                        hit(target, target.getHealth() + 50, Element.HOLY);
                        Vfx.spray(level, FxStyle.SOUL, at, 0xFFF4C0, 0xE0B040, 0.35f, 10, 0.12f, 1);
                    } else {
                        hit(target, boss ? Math.min(target.getMaxHealth() * 0.06f, dmg(8f)) : dmg(3f), Element.HOLY);
                    }
                    Vfx.flash(level, at, 0xE0B040, 1.6f, 8);
                    playSound(SoundEvents.BELL_BLOCK, 1.5f, 0.5f);
                }
                if (t >= 26) {
                    cooldown(WEIGH, 160);
                    return true;
                }
            }
            case STORM -> {
                faceTowards(castAt);
                if (t == 12) {
                    HazardZone.spawn(level, HazardZone.Kind.SANDSTORM, castAt, 5f, 120, dmg(0.4f), this, false);
                    Vfx.vortex(level, castAt, 0xE8C878, 0xB8985A, 5f, 4.5f, 120, null);
                    playSound(SoundEvents.SAND_BREAK, 2f, 0.4f);
                    playSound(SoundEvents.ELYTRA_FLYING, 1f, 0.5f);
                }
                if (t >= 20) {
                    cooldown(STORM, 300);
                    return true;
                }
            }
            case SIPHON -> {
                if (t == 2) {
                    siphonFrom = target;
                    if (siphonFrom != null && siphonTo != null) {
                        Vfx.tether(level, siphonFrom, this, 0x40E0C0, 0.1f, 42);
                        Vfx.tether(level, this, siphonTo, 0xE0B040, 0.1f, 42);
                    }
                    playSound(SoundEvents.SOUL_ESCAPE, 1.5f, 0.6f);
                }
                if (t > 2 && t < 44 && t % 4 == 0 && siphonFrom != null && siphonFrom.isAlive()) {
                    float d = dmg(0.45f);
                    hit(siphonFrom, d, Element.SHADOW);
                    if (siphonTo != null && siphonTo.isAlive()) siphonTo.heal(d * 1.4f);
                    BlockPos core = SiegeManager.corePos(level);
                    if (core != null && core.closerToCenterThan(position(), 12)) SiegeManager.healCore(level, d);
                    Vfx.spray(level, FxStyle.SOUL, siphonFrom.position().add(0, siphonFrom.getBbHeight() * 0.6, 0), 0x80FFE0, 0x40E0C0, 0.25f, 2, 0.08f, 1);
                }
                if (t >= 46) {
                    cooldown(SIPHON, 240);
                    return true;
                }
            }
            case CURSE -> {
                if (t == 12) {
                    int n = 0;
                    for (LivingEntity e : enemiesNear(level, 10)) {
                        if (n++ >= 6) break;
                        SiegeDamage.apply(e, ModEffects.MARKED.get(), 200, 1);
                        e.getPersistentData().putInt("lb_curse", getId());
                        Vfx.spray(level, FxStyle.SKULL, e.position().add(0, e.getBbHeight() + 0.4, 0), 0xE0B040, 0x8A6A20, 0.35f, 2, 0.05f, 1);
                    }
                    Vfx.ring(level, position().add(0, 0.05, 0), 0xE0B040, 10, 16, 0.6f);
                    playSound(SoundEvents.EVOKER_PREPARE_WOLOLO, 1.5f, 0.5f);
                }
                if (t >= 22) {
                    cooldown(CURSE, 280);
                    return true;
                }
            }
            case ULT -> {
                if (t == 1) {
                    Vfx.sigil(level, position().add(0, 0.04, 0), 0xE0B040, 12, 50, 2f);
                    Vfx.pillar(level, position(), 0xE0B040, 1f, 20, 50);
                    playSound(SoundEvents.BELL_RESONATE, 2.5f, 0.4f);
                }
                if (t == 20) playSound(SoundEvents.BELL_BLOCK, 2.5f, 0.5f);
                if (t == 40) {
                    Vfx.screenFlash(level, position(), 0xFFF0C0, 0.35f, 30, 10);
                    Vfx.shake(level, position(), 0.8f, 24, 10);
                    List<LivingEntity> foes = enemiesNear(level, 12);
                    for (LivingEntity e : foes) {
                        boolean boss = e instanceof SiegeEnemy se && se.isHighPriority();
                        Vec3 at = e.position().add(0, e.getBbHeight() * 0.6, 0);
                        if (!boss && e.getHealth() < e.getMaxHealth() * 0.4f) {
                            hit(e, e.getHealth() + 50, Element.HOLY);
                            Vfx.spray(level, FxStyle.SOUL, at, 0xFFF4C0, 0xE0B040, 0.35f, 6, 0.12f, 1);
                        } else {
                            hit(e, dmg(5f) + (boss ? e.getMaxHealth() * 0.08f : 0), Element.HOLY);
                        }
                        Vfx.pillar(level, e.position(), 0xE0B040, 0.5f, 8, 16);
                    }
                    for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 12)) a.heal(a.getMaxHealth() * 0.4f);
                    playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.5f, 0.6f);
                }
                if (t >= 52) return true;
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    /** Curse of the Pharaoh: called when a cursed enemy dies. */
    public void curseBurst(ServerLevel level, LivingEntity dead) {
        Vec3 at = dead.position().add(0, 0.5, 0);
        Vfx.spray(level, FxStyle.SMOKE, at, 0xE8C878, 0xB8985A, 1.2f, 16, 0.3f, 0.3f);
        Vfx.ring(level, at.add(0, -0.45, 0), 0xE0B040, 3, 10, 0.6f);
        for (LivingEntity e : SiegeDamage.enemiesAround(level, at, 3)) {
            hit(e, dmg(2f), Element.PHYSICAL);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
    }
}
