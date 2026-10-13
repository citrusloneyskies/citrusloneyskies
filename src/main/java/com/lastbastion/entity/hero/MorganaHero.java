package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.SiegeDamage;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Morgana the Stormweaver: a glass-cannon archmage who stays behind the front line. Homing Arcane
 * Bolts; Chain Lightning; Frost Nova when cornered; Blink toward safety; Gravity Well to clump a
 * crowd; Meteor to erase it. Ultimate, Tempest: she rises on a column of wind and calls a storm that
 * hammers the battlefield with lightning.
 */
public class MorganaHero extends HeroEntity {
    public static final int CHAIN = 6, NOVA = 7, BLINK = 8, METEOR = 9, WELL = 10;
    private Vec3 castAt = Vec3.ZERO;

    public MorganaHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.MORGANA);
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
        return 20;
    }

    @Override
    protected double preferredRange() {
        return 13;
    }

    @Override
    protected int attackWindup() {
        return 10;
    }

    @Override
    protected int attackInterval() {
        return 20;
    }

    @Override
    protected float retreatThreshold() {
        return 0.35f;
    }

    @Override
    protected float ultGainPerDamage() {
        return 0.11f;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"You rang? I do so love a storm.", "Morgana Stormweaver. Try not to stand in the lightning."};
            case ULT -> new String[]{"KNEEL BEFORE THE TEMPEST!", "Let the heavens break!"};
            case BOSS -> new String[]{"Oh, a big one. How delightful.", "I'll need a bigger spell."};
            case LOW_HP -> new String[]{"Someone get these things off me!", "Too close, far too close!"};
            case LEVEL_UP -> new String[]{"I can feel the storm in my bones.", "More power. Lovely."};
            case RETREAT -> new String[]{"Tactical retreat, darlings!", "I'm a mage, not a wall!"};
            case IDLE -> new String[]{"*hums while runes orbit lazily*", "The air smells of ozone... good."};
            default -> new String[0];
        };
    }

    private Vec3 staffTip() {
        return position().add(0, getBbHeight() * 1.05, 0).add(look().scale(0.4)).add(Vec3.directionFromRotation(0, getYRot() - 90).scale(0.45));
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        Vec3 from = staffTip();
        SiegeProjectile p = SiegeProjectile.create(level, ProjKind.ARCANE_BOLT, this, from).payload(dmg(1f), Element.ARCANE)
                .status(ModEffects.MARKED, 60, 1);
        p.homing(t, 0.18f).aim(t.position().add(0, t.getBbHeight() * 0.6, 0), 1.25f).scale(1.2f);
        p.onEntityHit = (pr, e) -> gainUlt(pr.damage * ultGainPerDamage());
        p.fire(level);
        Vfx.flash(level, from, 0xB08AFF, 0.6f, 4);
        playSound(SoundEvents.EVOKER_CAST_SPELL, 0.8f, 1.6f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        int close = crowd(level, position(), 4);
        if (ready(BLINK) && close >= 1 && (getHealth() < getMaxHealth() * 0.7f || close >= 2)) return BLINK;
        if (ready(NOVA) && close >= 2) return NOVA;
        if (t == null) return 0;
        if (ready(METEOR)) {
            Vec3 c = bestCluster(level, 22, 4, 5);
            if (c != null) {
                castAt = c;
                return METEOR;
            }
        }
        if (ready(WELL)) {
            Vec3 c = bestCluster(level, 18, 4, 3);
            if (c != null) {
                castAt = c;
                return WELL;
            }
        }
        if (ready(CHAIN) && crowd(level, t.position(), 6) >= 2 && dist < 16) return CHAIN;
        return 0;
    }

    @Override
    protected boolean wantsUltimate(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (t == null) return false;
        Vec3 c = bestCluster(level, 20, 6, 5);
        if (c != null) {
            castAt = c;
            return true;
        }
        return false;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case CHAIN -> {
                if (target != null) faceTowards(target.position());
                if (t == 12 && target != null) {
                    Set<LivingEntity> hits = new HashSet<>();
                    LivingEntity cur = target;
                    Vec3 from = staffTip();
                    float d = dmg(2f);
                    for (int i = 0; i < 6 && cur != null; i++) {
                        hits.add(cur);
                        Vec3 to = cur.position().add(0, cur.getBbHeight() * 0.5, 0);
                        Vfx.arc(level, from, to, 0xB08AFF, 0.07f, 6, 0.7f);
                        Vfx.spray(level, FxStyle.SPARK, to, 0xFFFFFF, 0xB08AFF, 0.1f, 5, 0.35f, 0.3f);
                        hit(cur, d, Element.LIGHTNING);
                        SiegeDamage.apply(cur, ModEffects.SHOCKED.get(), 15, 0);
                        d *= 0.85f;
                        from = to;
                        LivingEntity c = cur;
                        cur = SiegeDamage.enemiesAround(level, to, 6).stream().filter(e -> !hits.contains(e))
                                .min(Comparator.comparingDouble(e -> e.distanceToSqr(c))).orElse(null);
                    }
                    playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1f, 1.5f);
                }
                if (t >= 22) {
                    cooldown(CHAIN, 120);
                    return true;
                }
            }
            case NOVA -> {
                if (t == 10) {
                    Vfx.sphere(level, position().add(0, 0.8, 0), 0x9FE8FF, 5.5f, 12);
                    Vfx.ring(level, position().add(0, 0.05, 0), 0xD8F8FF, 5.5f, 14, 1);
                    Vfx.spray(level, FxStyle.SHARD, position().add(0, 0.8, 0), 0xE8FFFF, 0x8FE3FF, 0.2f, 22, 0.45f, 0.3f);
                    playSound(SoundEvents.GLASS_BREAK, 1.5f, 0.7f);
                    for (LivingEntity e : enemiesNear(level, 5.5)) {
                        hit(e, dmg(1.5f), Element.ICE);
                        SiegeDamage.apply(e, ModEffects.FROZEN.get(), 60, 0);
                    }
                }
                if (t >= 18) {
                    cooldown(NOVA, 200);
                    return true;
                }
            }
            case BLINK -> {
                if (t == 3) {
                    BlockPos core = SiegeManager.corePos(level);
                    Vec3 away = Vec3.ZERO;
                    for (LivingEntity e : enemiesNear(level, 6)) away = away.add(position().subtract(e.position()));
                    if (away.lengthSqr() < 1e-3 && core != null) away = Vec3.atCenterOf(core).subtract(position());
                    if (away.lengthSqr() < 1e-3) away = look().scale(-1);
                    away = away.multiply(1, 0, 1).normalize();
                    Vec3 from = position();
                    Vec3 dest = position().add(away.scale(8));
                    Vfx.spray(level, FxStyle.GLOW, from.add(0, 1, 0), 0xC8A0FF, 0x6A3AE0, 0.3f, 16, 0.2f, 0);
                    if (randomTeleport(dest.x, dest.y + 1, dest.z, false)) {
                        Vfx.dash(level, from.add(0, 1, 0), position().add(0, 1, 0), 0xB08AFF, 0.6f, 10);
                        Vfx.spray(level, FxStyle.GLOW, position().add(0, 1, 0), 0xC8A0FF, 0x6A3AE0, 0.3f, 16, 0.2f, 0);
                    }
                    playSound(SoundEvents.ENDERMAN_TELEPORT, 1f, 1.4f);
                }
                if (t >= 8) {
                    cooldown(BLINK, 160);
                    return true;
                }
            }
            case METEOR -> {
                faceTowards(castAt);
                if (t == 2) {
                    Vfx.teleCircle(level, castAt.add(0, 0.05, 0), 5f, 34, 0xFF7A20, false);
                    Vfx.sigil(level, position().add(0, 0.05, 0), 0xFF7A20, 2.2f, 34, 6f);
                    playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.4f, 0.7f);
                }
                if (t == 14) {
                    SiegeProjectile m = SiegeProjectile.create(level, ProjKind.METEOR, this, castAt.add(7, 26, 7));
                    m.payload(dmg(5f), Element.FIRE).area(5f);
                    m.gravity = 0;
                    m.onImpact = (p, at) -> {
                        HazardZone.spawn(level, HazardZone.Kind.FIRE, at, 4f, 100, dmg(0.25f), this, false);
                        Vfx.screenFlash(level, at, 0xFFC080, 0.2f, 24, 6);
                    };
                    m.onEntityHit = (p, e) -> gainUlt(p.damage * ultGainPerDamage());
                    m.aim(castAt, 1.3f).scale(4f).fire(level);
                    playSound(SoundEvents.BLAZE_SHOOT, 2f, 0.4f);
                }
                if (t >= 30) {
                    cooldown(METEOR, 400);
                    return true;
                }
            }
            case WELL -> {
                faceTowards(castAt);
                if (t == 10) {
                    HazardZone.spawn(level, HazardZone.Kind.GRAVITY_WELL, castAt, 4.5f, 50, dmg(0.3f), this, false);
                    Vfx.vortex(level, castAt, 0x6A3AE0, 0xC8A0FF, 4.5f, 2.2f, 50, null);
                    Vfx.sigil(level, castAt.add(0, 0.05, 0), 0xA070FF, 4.5f, 50, -8f);
                    playSound(SoundEvents.PORTAL_TRIGGER, 0.8f, 1.6f);
                }
                if (t == 58) {
                    Vfx.sphere(level, castAt.add(0, 0.5, 0), 0xC8A0FF, 4.5f, 10);
                    Vfx.flash(level, castAt.add(0, 0.8, 0), 0xC8A0FF, 2f, 6);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, castAt, 4.5)) hit(e, dmg(2.5f), Element.ARCANE);
                    playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1.5f, 0.6f);
                }
                if (t >= 62) {
                    cooldown(WELL, 300);
                    return true;
                }
            }
            case ULT -> {
                if (t == 1) {
                    setNoGravity(true);
                    Vfx.vortex(level, position(), 0x9A7AFF, 0xE0D8FF, 1.2f, 3f, 92, this);
                    Vfx.teleCircle(level, castAt.add(0, 0.05, 0), 7.5f, 20, 0xB08AFF, false);
                    playSound(SoundEvents.EVOKER_PREPARE_WOLOLO, 1.5f, 0.6f);
                }
                if (t < 12) setDeltaMovement(0, 0.08, 0);
                else setDeltaMovement(getDeltaMovement().multiply(0.5, 0, 0.5));
                faceTowards(castAt);
                if (t == 20) {
                    HazardZone.spawn(level, HazardZone.Kind.STORM, castAt, 7.5f, 110, dmg(0.5f), this, false);
                    Vfx.sigil(level, castAt.add(0, 0.05, 0), 0xB08AFF, 7.5f, 100, 2f);
                    Vfx.screenFlash(level, castAt, 0xD8C8FF, 0.25f, 30, 8);
                    playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 2.5f, 0.8f);
                }
                if (t > 20 && t < 90 && t % 7 == 0) {
                    List<LivingEntity> foes = SiegeDamage.enemiesAround(level, castAt, 8);
                    if (!foes.isEmpty()) {
                        LivingEntity e = foes.get(random.nextInt(foes.size()));
                        Vfx.lightning(level, e.position(), 0xC8A0FF, 1.2f);
                        hit(e, dmg(2.5f), Element.LIGHTNING);
                        SiegeDamage.apply(e, ModEffects.SHOCKED.get(), 20, 0);
                        playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2f, 1.2f);
                    }
                }
                if (t >= 92) {
                    setNoGravity(false);
                    return true;
                }
            }
            default -> {
                return true;
            }
        }
        return false;
    }
}
