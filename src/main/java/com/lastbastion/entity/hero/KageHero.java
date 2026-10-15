package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.zombie.BruteZombie;
import com.lastbastion.entity.zombie.WreckerZombie;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Kage the Ronin: a blade-dancer who hunts priority targets. Four-cut combo (slash, reverse,
 * spinning cut, thrust); Iaido Flash dashes through a line and the cuts open a heartbeat later;
 * Parry & Riposte turns a heavy blow back on its owner; Smoke Step reappears behind the target for a
 * critical; Shadow Clones split him in three. Ultimate, Thousand Cuts: he vanishes, slashes through up
 * to ten enemies, then sheathes his sword - and they all fall apart at once.
 */
public class KageHero extends HeroEntity {
    public static final int IAIDO = 6, PARRY = 7, SMOKE = 8, CLONES = 9;
    private Vec3 dashFrom = Vec3.ZERO, dashDir = Vec3.ZERO;
    private final List<LivingEntity> marked = new ArrayList<>();
    private boolean parrying;
    private Vec3 ultHome = Vec3.ZERO;

    public KageHero(EntityType<? extends HeroEntity> type, Level level) {
        this(type, level, HeroKind.KAGE);
    }

    protected KageHero(EntityType<? extends HeroEntity> type, Level level, HeroKind kind) {
        super(type, level, kind);
    }

    @Override
    public Role role() {
        return Role.ASSASSIN;
    }

    @Override
    protected int attackWindup() {
        return 4;
    }

    @Override
    protected int attackRecover() {
        return 3;
    }

    @Override
    protected int attackInterval() {
        return 10;
    }

    @Override
    protected int comboLength() {
        return 4;
    }

    @Override
    protected double attackRange() {
        return getBbWidth() * 0.5 + 2.0;
    }

    @Override
    protected float ultGainPerDamage() {
        return 0.035f;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"...", "The blade is drawn."};
            case ULT -> new String[]{"Thousand Cuts.", "You are already dead."};
            case BOSS -> new String[]{"One cut. One kill.", "A worthy edge."};
            case LOW_HP -> new String[]{"...a scratch.", "Hn."};
            case LEVEL_UP -> new String[]{"The way of the sword deepens."};
            case RETREAT -> new String[]{"I withdraw. For now."};
            case IDLE -> new String[]{"*wipes the blade, sheathes it*", "Wind... carries rot."};
            default -> new String[0];
        };
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        Vec3 c = position().add(0, 1.05, 0);
        int red = 0xD02030;
        switch (combo) {
            case 0 -> {
                arc(level, 2.3, 120, dmg(1f), Element.PHYSICAL);
                Vfx.slash(level, c, getYRot(), 30, 140, 2f, red, 0xFFFFFF, 6, 0.22f);
            }
            case 1 -> {
                arc(level, 2.3, 120, dmg(1f), Element.PHYSICAL);
                Vfx.slash(level, c, getYRot(), -150, 140, 2f, red, 0xFFFFFF, 6, 0.22f);
            }
            case 2 -> {
                int n = 0;
                for (LivingEntity e : enemiesNear(level, 2.6)) if (hit(e, dmg(0.9f), Element.PHYSICAL)) n++;
                Vfx.slash(level, c.add(0, -0.2, 0), getYRot(), 4, 360, 2.4f, red, 0xFFFFFF, 7, 0.2f);
            }
            default -> {
                line(level, position(), look(), 3.2, 0.7, dmg(1.7f), Element.PHYSICAL);
                SiegeDamage.applyStacking(t, ModEffects.BLEEDING.get(), 80, 3);
                Vfx.dash(level, c.add(look().scale(0.5)), c.add(look().scale(3.4)), 0xFFFFFF, 0.25f, 6);
            }
        }
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.4f + combo * 0.08f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        int near = crowd(level, position(), 7);
        if (ready(CLONES) && near >= 4) return CLONES;
        if (t == null) return 0;
        if (ready(PARRY) && dist < 3.5 && (t instanceof BruteZombie || t instanceof WreckerZombie || t instanceof SiegeEnemy se && se.isHighPriority())
                && t instanceof Mob m && m.getTarget() == this) return PARRY;
        if (ready(IAIDO) && dist > 2.5 && dist < 8) return IAIDO;
        if (ready(SMOKE) && dist > 4 && dist < 14 && (t instanceof SiegeEnemy se2 && se2.isHighPriority()
                || t instanceof com.lastbastion.entity.zombie.NecromancerZombie || t instanceof com.lastbastion.entity.zombie.SpitterZombie)) return SMOKE;
        return 0;
    }

    @Override
    protected boolean wantsUltimate(ServerLevel level, @Nullable LivingEntity t, double dist) {
        return t != null && (enemiesNear(level, 12).size() >= 4 || (t instanceof SiegeEnemy se && se.isHighPriority() && dist < 10));
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case IAIDO -> {
                if (t == 1 && target != null) {
                    faceNow(target.position());
                    dashDir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
                    playSound(SoundEvents.ARMOR_EQUIP_CHAIN, 1f, 0.6f);
                }
                if (t == 10) {
                    dashFrom = position();
                    Vec3 to = position().add(dashDir.scale(7.5));
                    marked.clear();
                    for (LivingEntity e : enemiesNear(level, 9)) {
                        Vec3 rel = e.position().subtract(dashFrom).multiply(1, 0, 1);
                        double along = rel.dot(dashDir);
                        if (along > -0.5 && along < 8 && rel.subtract(dashDir.scale(along)).lengthSqr() < 2.2) marked.add(e);
                    }
                    randomTeleport(to.x, to.y + 0.5, to.z, false);
                    Vfx.dash(level, dashFrom.add(0, 1, 0), position().add(0, 1, 0), 0xFFFFFF, 0.5f, 12);
                    Vfx.dash(level, dashFrom.add(0, 0.9, 0), position().add(0, 0.9, 0), 0xD02030, 0.9f, 14);
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.6f);
                }
                if (t == 18) {
                    for (LivingEntity e : marked) {
                        if (!e.isAlive()) continue;
                        hit(e, dmg(3f), Element.PHYSICAL);
                        SiegeDamage.applyStacking(e, ModEffects.BLEEDING.get(), 100, 4);
                        Vfx.slash(level, e.position().add(0, e.getBbHeight() * 0.55, 0), getYRot() + 90, 35, 120, 1.4f, 0xD02030, 0xFFFFFF, 8, 0.2f);
                        Vfx.spray(level, FxStyle.DROP, e.position().add(0, e.getBbHeight() * 0.6, 0), 0xA01010, 0x5A0808, 0.12f, 8, 0.3f, 0.4f);
                    }
                    playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 0.8f);
                }
                if (t >= 24) {
                    cooldown(IAIDO, 130);
                    return true;
                }
            }
            case PARRY -> {
                parrying = t < 36;
                if (target != null) faceTowards(target.position());
                if (t >= 40) {
                    parrying = false;
                    cooldown(PARRY, 200);
                    return true;
                }
            }
            case SMOKE -> {
                if (t == 4 && target != null) {
                    Vec3 behind = target.position().add(target.position().subtract(position()).multiply(1, 0, 1).normalize().scale(1.6));
                    Vfx.spray(level, FxStyle.SMOKE, position().add(0, 1, 0), 0x2A2A34, 0x0A0A10, 1.1f, 18, 0.2f, 0);
                    randomTeleport(behind.x, behind.y + 0.5, behind.z, false);
                    faceNow(target.position());
                    Vfx.spray(level, FxStyle.SMOKE, position().add(0, 1, 0), 0x2A2A34, 0x0A0A10, 1.1f, 18, 0.2f, 0);
                    playSound(SoundEvents.FIRECHARGE_USE, 0.8f, 1.6f);
                }
                if (t == 9 && target != null) {
                    hit(target, dmg(2.6f), Element.PHYSICAL);
                    SiegeDamage.applyStacking(target, ModEffects.BLEEDING.get(), 100, 4);
                    Vfx.slash(level, position().add(0, 1.1, 0), getYRot(), 60, 170, 2.2f, 0xD02030, 0xFFFFFF, 8, 0.3f);
                    playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 1.2f);
                }
                if (t >= 16) {
                    cooldown(SMOKE, 170);
                    return true;
                }
            }
            case CLONES -> {
                if (t == 10) {
                    for (int i = 0; i < 2; i++) {
                        KageCloneEntity c = com.lastbastion.registry.ModEntities.KAGE_CLONE.get().create(level);
                        if (c == null) continue;
                        Vec3 p = position().add(Vec3.directionFromRotation(0, getYRot() + (i == 0 ? 90 : -90)).scale(2));
                        c.moveTo(p.x, p.y, p.z, getYRot(), 0);
                        c.setOwnerId(getOwnerId());
                        c.setMode(HeroMode.HUNT);
                        c.setLevelAndXp(heroLevel(), 0);
                        level.addFreshEntity(c);
                        Vfx.spray(level, FxStyle.SMOKE, p.add(0, 1, 0), 0x2A2A34, 0x0A0A10, 1f, 16, 0.18f, 0);
                    }
                    playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.5f, 1f);
                }
                if (t >= 18) {
                    cooldown(CLONES, 520);
                    return true;
                }
            }
            case ULT -> {
                if (t == 1) {
                    ultHome = position();
                    marked.clear();
                    List<LivingEntity> foes = enemiesNear(level, 12);
                    foes.sort((a, b) -> Boolean.compare(b instanceof SiegeEnemy sb && sb.isHighPriority(), a instanceof SiegeEnemy sa && sa.isHighPriority()));
                    for (int i = 0; i < Math.min(10, foes.size()); i++) marked.add(foes.get(i));
                    setInvisible(true);
                    Vfx.spray(level, FxStyle.SMOKE, position().add(0, 1, 0), 0x2A2A34, 0x0A0A10, 1.2f, 24, 0.2f, 0);
                    playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.5f, 0.6f);
                }
                int idx = (t - 4) / 3;
                if (t >= 4 && (t - 4) % 3 == 0 && idx < marked.size()) {
                    LivingEntity e = marked.get(idx);
                    if (e.isAlive()) {
                        Vec3 at = e.position().add(0, e.getBbHeight() * 0.55, 0);
                        float yaw = random.nextFloat() * 360;
                        Vfx.slash(level, at, yaw, random.nextFloat() * 160 - 80, 200, 1.4f + e.getBbWidth() * 0.5f, 0xD02030, 0xFFFFFF, 10, 0.2f);
                        Vfx.dash(level, at.add(Vec3.directionFromRotation(0, yaw).scale(-2)), at.add(Vec3.directionFromRotation(0, yaw).scale(2)), 0xFFFFFF, 0.3f, 8);
                        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.6f);
                    }
                }
                int end = 6 + marked.size() * 3;
                if (t == end) {
                    setInvisible(false);
                    teleportTo(ultHome.x, ultHome.y, ultHome.z);
                    Vfx.spray(level, FxStyle.SMOKE, position().add(0, 1, 0), 0x2A2A34, 0x0A0A10, 1.2f, 20, 0.2f, 0);
                    playSound(SoundEvents.ARMOR_EQUIP_CHAIN, 1.5f, 0.5f);
                }
                if (t == end + 10) {
                    for (LivingEntity e : marked) {
                        if (!e.isAlive()) continue;
                        hit(e, dmg(4.5f) * (e instanceof SiegeEnemy se && se.isHighPriority() ? 1.4f : 1f), Element.PHYSICAL);
                        SiegeDamage.applyStacking(e, ModEffects.BLEEDING.get(), 120, 4);
                        Vfx.spray(level, FxStyle.DROP, e.position().add(0, e.getBbHeight() * 0.6, 0), 0xA01010, 0x5A0808, 0.14f, 10, 0.35f, 0.4f);
                        Vfx.flash(level, e.position().add(0, e.getBbHeight() * 0.6, 0), 0xD02030, 1f, 5);
                    }
                    Vfx.shake(level, position(), 0.6f, 16, 8);
                    playSound(SoundEvents.PLAYER_ATTACK_CRIT, 2f, 0.6f);
                }
                if (t >= end + 16) return true;
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (skill == ULT && isInvisible()) return false;
        if (parrying && source.getEntity() instanceof LivingEntity att && att != this && !source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            if (level() instanceof ServerLevel sl) {
                faceNow(att.position());
                hit(att, dmg(2.5f) + amount * 0.5f, Element.PHYSICAL);
                if (!(att instanceof SiegeEnemy se && se.isHighPriority())) SiegeDamage.apply(att, ModEffects.SHOCKED.get(), 20, 0);
                Vfx.flash(sl, position().add(0, 1.2, 0).add(look().scale(0.6)), 0xFFFFFF, 1.2f, 4);
                Vfx.spray(sl, FxStyle.SPARK, position().add(0, 1.2, 0).add(look().scale(0.6)), 0xFFFFFF, 0xFFD080, 0.12f, 12, 0.4f, 0.2f);
                Vfx.slash(sl, position().add(0, 1.1, 0), getYRot(), 20, 180, 2.2f, 0xD02030, 0xFFFFFF, 8, 0.3f);
                playSound(SoundEvents.ANVIL_PLACE, 0.8f, 1.8f);
                anim.play(ANIM_ATTACK3);
            }
            parrying = false;
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        setInvisible(false);
        super.die(source);
    }

    /** Clones and the real Kage never hurt each other; clones don't count as allies for priority. */
    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof KageHero || super.isAlliedTo(other);
    }

    @Override
    protected void onSkillInterrupted(int skill) {
        super.onSkillInterrupted(skill);
        setInvisible(false);
    }
}
