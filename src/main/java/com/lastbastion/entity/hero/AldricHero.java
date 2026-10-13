package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Sir Aldric, Knight of Dawn: a paladin who fights at the front and keeps his allies standing.
 * Combo: rising slash, reverse slash, radiant thrust. Shield Bash stuns; Radiant Dash cuts through a
 * line; Consecration hallows the ground (heals allies, burns the dead); Aegis of Dawn shields and heals
 * everyone nearby and the core. Ultimate, Dawnbreak: he raises Dawnbringer into a pillar of sunlight
 * and sweeps a blade of light around him.
 */
public class AldricHero extends HeroEntity {
    public static final int BASH = 6, DASH = 7, CONSECRATE = 8, AEGIS = 9;
    private Vec3 dashDir = Vec3.ZERO;

    public AldricHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.ALDRIC);
    }

    @Override
    public Role role() {
        return Role.BRUISER;
    }

    @Override
    protected int attackWindup() {
        return 6;
    }

    @Override
    protected int attackInterval() {
        return 13;
    }

    @Override
    protected int comboLength() {
        return 3;
    }

    @Override
    protected double attackRange() {
        return getBbWidth() * 0.5 + 2.0;
    }

    @Override
    protected float ultGainPerDamage() {
        return 0.06f;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"Dawn has come. Stand with me!", "Sir Aldric, at your command."};
            case ULT -> new String[]{"BY THE LIGHT OF DAWN!", "Let the sun rise!"};
            case BOSS -> new String[]{"Face me, abomination!", "Your night ends here."};
            case LOW_HP -> new String[]{"I am not yet finished!", "The light... still burns."};
            case LEVEL_UP -> new String[]{"My oath grows stronger.", "The dawn smiles on us."};
            case RETREAT -> new String[]{"Falling back - hold the line for me!"};
            case IDLE -> new String[]{"*polishes his shield*", "Rest while you can, friends."};
            default -> new String[0];
        };
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        Vec3 c = position().add(0, 1.1, 0);
        switch (combo) {
            case 0 -> {
                arc(level, 2.4, 130, dmg(1f), Element.HOLY);
                Vfx.slash(level, c, getYRot(), 55, 150, 2.1f, 0xFFE38A, 0xFFFFFF, 8, 0.32f);
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.1f);
            }
            case 1 -> {
                arc(level, 2.4, 140, dmg(1f), Element.HOLY);
                Vfx.slash(level, c, getYRot(), -40, -160, 2.1f, 0xFFE38A, 0xFFFFFF, 8, 0.32f);
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.25f);
            }
            default -> {
                line(level, position(), look(), 3.4, 0.9, dmg(1.6f), Element.HOLY);
                SiegeDamage.apply(t, ModEffects.PURIFIED.get(), 80, 0);
                Vfx.dash(level, c.add(look().scale(0.6)), c.add(look().scale(3.6)), 0xFFE38A, 0.35f, 7);
                Vfx.flash(level, c.add(look().scale(3)), 0xFFE38A, 0.8f, 5);
                playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1f);
            }
        }
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (ready(AEGIS) && needsAegis(level)) return AEGIS;
        if (ready(CONSECRATE) && (crowd(level, position(), 5) >= 3 || hurtAlly(level, 6, 0.6f) != null) && t != null && dist < 6) return CONSECRATE;
        if (t == null) return 0;
        if (ready(BASH) && dist < 2.8 && (t instanceof SiegeEnemy se && se.isHighPriority() || (t instanceof Mob m && m.getTarget() != null
                && m.getTarget() != this && SiegeDamage.isDefender(m.getTarget())))) return BASH;
        if (ready(DASH) && dist > 4 && dist < 12) return DASH;
        return 0;
    }

    private boolean needsAegis(ServerLevel level) {
        var d = SiegeManager.data(level);
        BlockPos core = SiegeManager.corePos(level);
        if (d.hasCore() && d.coreHp < d.maxHp() * 0.6f && core != null && core.closerToCenterThan(position(), 14)) return true;
        int hurt = 0;
        for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 10)) {
            if (!(a instanceof com.lastbastion.entity.turret.TurretEntity) && a.getHealth() < a.getMaxHealth() * 0.5f) hurt++;
        }
        return hurt >= 1 && (hurt >= 2 || getHealth() < getMaxHealth() * 0.4f || hurtAlly(level, 10, 0.35f) != null);
    }

    @Override
    protected boolean wantsUltimate(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (t == null) return false;
        return crowd(level, position(), 7) >= 4 || hurtAlly(level, 8, 0.35f) != null || (t instanceof SiegeEnemy se && se.isHighPriority() && dist < 5);
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case BASH -> {
                if (target != null) faceTowards(target.position());
                if (t == 6 && target != null) {
                    hit(target, dmg(1.2f), Element.PHYSICAL);
                    SiegeDamage.apply(target, ModEffects.SHOCKED.get(), 28, 0);
                    if (!(target instanceof SiegeEnemy se && se.isHighPriority())) SiegeDamage.push(target, position(), 1.1, 0.3);
                    Vfx.flash(level, forward(1.2).add(0, 1.1, 0), 0xFFE38A, 1.2f, 6);
                    Vfx.ring(level, forward(1.2).add(0, 1.1, 0), 0xFFE38A, 1.5f, 8, 0.2f);
                    playSound(SoundEvents.SHIELD_BLOCK, 1.5f, 0.7f);
                    playSound(SoundEvents.ANVIL_LAND, 0.6f, 1.6f);
                }
                if (t >= 14) {
                    cooldown(BASH, 140);
                    return true;
                }
            }
            case DASH -> {
                if (t == 1 && target != null) {
                    dashDir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
                    faceNow(target.position());
                    playSound(SoundEvents.TRIDENT_RIPTIDE_1, 1f, 1.3f);
                }
                if (t >= 5 && t < 14) {
                    Vec3 before = position();
                    setDeltaMovement(dashDir.x * 1.25, getDeltaMovement().y, dashDir.z * 1.25);
                    Vfx.dash(level, before.add(0, 1, 0), before.add(dashDir.scale(1.3)).add(0, 1, 0), 0xFFE38A, 0.7f, 10);
                    for (LivingEntity e : enemiesNear(level, 2)) {
                        if (e.hurtTime > 0) continue;
                        hit(e, dmg(2f), Element.HOLY);
                        SiegeDamage.apply(e, ModEffects.PURIFIED.get(), 80, 0);
                    }
                }
                if (t == 14) Vfx.slash(level, position().add(0, 1.1, 0), getYRot(), 5, 200, 2.4f, 0xFFE38A, 0xFFFFFF, 9, 0.35f);
                if (t >= 20) {
                    cooldown(DASH, 120);
                    return true;
                }
            }
            case CONSECRATE -> {
                if (t == 12) {
                    HazardZone.spawn(level, HazardZone.Kind.CONSECRATION, position(), 4.5f, 120, dmg(0.6f), this, false);
                    Vfx.sigil(level, position().add(0, 0.04, 0), 0xFFE38A, 4.5f, 120, 1.5f);
                    Vfx.pillar(level, position(), 0xFFE38A, 0.5f, 6, 14);
                    Vfx.ring(level, position().add(0, 0.05, 0), 0xFFFFFF, 5, 12, 0.8f);
                    playSound(SoundEvents.BEACON_ACTIVATE, 1.5f, 1.3f);
                }
                if (t >= 22) {
                    cooldown(CONSECRATE, 320);
                    return true;
                }
            }
            case AEGIS -> {
                if (t == 14) {
                    Vfx.dome(level, position(), 0xFFE38A, 3.5f, 40, this);
                    Vfx.ring(level, position().add(0, 0.05, 0), 0xFFE38A, 10, 20, 1);
                    playSound(SoundEvents.BEACON_ACTIVATE, 1.5f, 1.6f);
                    for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 10)) {
                        a.addEffect(new MobEffectInstance(ModEffects.AEGIS.get(), 200, 1));
                        a.heal(a.getMaxHealth() * 0.2f);
                        Vfx.spray(level, FxStyle.STAR, a.position().add(0, a.getBbHeight() * 0.6, 0), 0xFFFFFF, 0xFFE38A, 0.2f, 6, 0.12f, 1);
                    }
                    BlockPos core = SiegeManager.corePos(level);
                    if (core != null && core.closerToCenterThan(position(), 14)) SiegeManager.healCore(level, SiegeManager.data(level).maxHp() * 0.05f);
                }
                if (t >= 26) {
                    cooldown(AEGIS, 500);
                    return true;
                }
            }
            case ULT -> {
                if (t == 1) {
                    Vfx.pillar(level, position(), 0xFFE38A, 1.2f, 30, 34);
                    Vfx.sigil(level, position().add(0, 0.05, 0), 0xFFE38A, 7, 40, 6f);
                    playSound(SoundEvents.BEACON_POWER_SELECT, 2f, 1.2f);
                }
                if (t == 22) {
                    Vec3 c = position().add(0, 1.1, 0);
                    Vfx.slash(level, c, getYRot(), 0, 360, 7f, 0xFFE38A, 0xFFFFFF, 14, 0.25f);
                    Vfx.slash(level, c.add(0, 0.3, 0), getYRot() + 40, 8, 360, 6.2f, 0xFFF4C0, 0xFFFFFF, 12, 0.2f);
                    Vfx.ring(level, position().add(0, 0.05, 0), 0xFFE38A, 8, 16, 1.6f);
                    Vfx.screenFlash(level, position(), 0xFFF4D0, 0.3f, 24, 8);
                    Vfx.shake(level, position(), 0.9f, 20, 10);
                    for (LivingEntity e : enemiesNear(level, 7.5)) {
                        hit(e, dmg(5f) * (e instanceof com.lastbastion.entity.zombie.WraithZombie ? 1.5f : 1f), Element.HOLY);
                        SiegeDamage.apply(e, ModEffects.PURIFIED.get(), 160, 0);
                        if (!(e instanceof SiegeEnemy se && se.isHighPriority())) SiegeDamage.push(e, position(), 1.2, 0.4);
                    }
                    for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 9)) {
                        a.heal(a.getMaxHealth() * 0.3f);
                        a.addEffect(new MobEffectInstance(ModEffects.AEGIS.get(), 160, 0));
                    }
                    playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.6f, 1.5f);
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2f, 0.6f);
                }
                if (t >= 36) return true;
            }
            default -> {
                return true;
            }
        }
        return false;
    }
}
