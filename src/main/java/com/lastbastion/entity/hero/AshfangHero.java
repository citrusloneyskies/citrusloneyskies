package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.zombie.LeaperZombie;
import com.lastbastion.entity.zombie.RunnerZombie;
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
import java.util.HashSet;
import java.util.Set;

/**
 * Ashfang the Dire Wolf: a fast skirmisher that dives the enemy back line. Bites stack Bleeding;
 * Pounce knocks a target down from range; Hamstring cripples runners and leapers; Rending Frenzy is
 * a flurry of bites; Howl of the Pack rallies allies. Ultimate, Alpha's Hunt: ember-wreathed, he
 * lunges from prey to prey across the field.
 */
public class AshfangHero extends HeroEntity {
    public static final int POUNCE = 6, HAMSTRING = 7, HOWL = 8, FRENZY = 9;
    private Vec3 leapTo = Vec3.ZERO;
    private final Set<Integer> hunted = new HashSet<>();

    public AshfangHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.ASHFANG);
    }

    @Override
    public Role role() {
        return Role.ASSASSIN;
    }

    @Override
    protected int attackWindup() {
        return 5;
    }

    @Override
    protected int attackRecover() {
        return 4;
    }

    @Override
    protected int attackInterval() {
        return 12;
    }

    @Override
    protected int comboLength() {
        return 3;
    }

    @Override
    protected double attackRange() {
        return getBbWidth() * 0.5 + 1.5;
    }

    @Override
    protected float ultGainPerDamage() {
        return 0.11f;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"*a low growl rolls across the field*", "*Ashfang pads out of the smoke, eyes like coals*"};
            case ULT -> new String[]{"*AWOOOOOOOO!*"};
            case BOSS -> new String[]{"*hackles rise, fangs bared*", "*circles the giant, snarling*"};
            case LOW_HP -> new String[]{"*whimpers, but keeps snarling*"};
            case LEVEL_UP -> new String[]{"*a triumphant howl*"};
            case RETREAT -> new String[]{"*limps back toward the core*"};
            case IDLE -> new String[]{"*sniffs the wind*", "*scratches at the dirt*"};
            default -> new String[0];
        };
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        boolean shake = combo == 2;
        float d = dmg(shake ? 1.6f : 1f);
        if (hit(t, d, Element.PHYSICAL)) {
            SiegeDamage.applyStacking(t, ModEffects.BLEEDING.get(), 80, shake ? 3 : 1);
            Vec3 at = t.position().add(0, t.getBbHeight() * 0.6, 0);
            Vfx.spray(level, FxStyle.DROP, at, 0xA01010, 0x5A0808, 0.12f, shake ? 10 : 5, 0.25f, 0.4f);
            Vfx.slash(level, position().add(0, 0.7, 0), getYRot(), shake ? 70 : 15, shake ? 200 : 90, 1.4f, 0xE8E0D8, 0xFFFFFF, 7, 0.25f);
            if (shake && !(t instanceof SiegeEnemy se && se.isHighPriority())) SiegeDamage.push(t, position(), 0.6, 0.2);
        }
        playSound(shake ? SoundEvents.WOLF_GROWL : SoundEvents.FOX_BITE, 1f, shake ? 0.7f : 0.8f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (ready(HOWL) && (crowd(level, position(), 8) >= 4 || hurtAlly(level, 10, 0.4f) != null)) return HOWL;
        if (t == null) return 0;
        if (ready(POUNCE) && dist > 4 && dist < 11) return POUNCE;
        if (ready(HAMSTRING) && dist < 3 && (t instanceof RunnerZombie || t instanceof LeaperZombie || (t instanceof SiegeEnemy se && se.isHighPriority()))) {
            return HAMSTRING;
        }
        if (ready(FRENZY) && dist < 2.8 && t.getHealth() > dmg(3)) return FRENZY;
        return 0;
    }

    @Override
    protected boolean wantsUltimate(ServerLevel level, @Nullable LivingEntity t, double dist) {
        return t != null && (crowd(level, position(), 10) >= 3 || (t instanceof SiegeEnemy se && se.isHighPriority()));
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case POUNCE -> {
                if (t == 1 && target != null) {
                    leapTo = target.position();
                    faceNow(leapTo);
                    playSound(SoundEvents.WOLF_GROWL, 1.2f, 0.6f);
                }
                if (t == 6) {
                    Vec3 d = leapTo.subtract(position());
                    setDeltaMovement(d.x * 0.17, 0.55, d.z * 0.17);
                    hasImpulse = true;
                    Vfx.dash(level, position().add(0, 0.6, 0), leapTo.add(0, 0.6, 0), 0xC8C8D0, 0.6f, 10);
                }
                if (t > 9 && (onGround() || t > 26)) {
                    for (LivingEntity e : enemiesNear(level, 2.4)) {
                        hit(e, dmg(2.2f), Element.PHYSICAL);
                        SiegeDamage.applyStacking(e, ModEffects.BLEEDING.get(), 100, 3);
                        if (!(e instanceof SiegeEnemy se && se.isHighPriority())) SiegeDamage.apply(e, ModEffects.SHOCKED.get(), 20, 0);
                    }
                    Vfx.ring(level, position().add(0, 0.05, 0), 0xB8B0A8, 2.6f, 10, 0.4f);
                    Vfx.spray(level, FxStyle.DEBRIS, position().add(0, 0.2, 0), 0x6A5A48, 0x3A3028, 0.15f, 10, 0.3f, 0.7f);
                    playSound(SoundEvents.RAVAGER_ATTACK, 1f, 1.4f);
                    cooldown(POUNCE, 110);
                    return true;
                }
            }
            case HAMSTRING -> {
                if (t == 4 && target != null) {
                    hit(target, dmg(1.3f), Element.PHYSICAL);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                    SiegeDamage.applyStacking(target, ModEffects.BLEEDING.get(), 100, 3);
                    Vfx.slash(level, position().add(0, 0.4, 0), getYRot(), -10, 120, 1.5f, 0xC01010, 0xFFFFFF, 8, 0.3f);
                    playSound(SoundEvents.FOX_BITE, 1.2f, 0.5f);
                }
                if (t >= 12) {
                    cooldown(HAMSTRING, 150);
                    return true;
                }
            }
            case FRENZY -> {
                if (target != null) faceTowards(target.position());
                if (t % 4 == 0 && t <= 20 && target != null) {
                    hit(target, dmg(0.8f), Element.PHYSICAL);
                    SiegeDamage.applyStacking(target, ModEffects.BLEEDING.get(), 100, 4);
                    Vfx.spray(level, FxStyle.DROP, target.position().add(0, target.getBbHeight() * 0.6, 0), 0xA01010, 0x5A0808, 0.12f, 6, 0.3f, 0.4f);
                    playSound(SoundEvents.FOX_BITE, 1f, 0.7f + t * 0.02f);
                }
                if (t >= 24) {
                    cooldown(FRENZY, 170);
                    return true;
                }
            }
            case HOWL -> {
                if (t == 10) {
                    playSound(SoundEvents.WOLF_HOWL, 2.5f, 0.75f);
                    Vfx.ring(level, position().add(0, 0.05, 0), 0xFFB040, 10, 20, 1.4f);
                    Vfx.spray(level, FxStyle.EMBER, position().add(0, 1, 0), 0xFFC040, 0xFF4010, 0.15f, 24, 0.3f, 0.7f);
                    for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 12)) {
                        a.addEffect(new MobEffectInstance(ModEffects.RALLY.get(), 200, 0));
                    }
                    for (LivingEntity e : enemiesNear(level, 10)) {
                        if (e instanceof RunnerZombie && !(e instanceof SiegeEnemy se && se.isHighPriority())) {
                            e.addEffect(new MobEffectInstance(ModEffects.FEARED.get(), 80, 0));
                        }
                    }
                }
                if (t >= 30) {
                    cooldown(HOWL, 420);
                    return true;
                }
            }
            case ULT -> {
                if (t == 1) {
                    hunted.clear();
                    playSound(SoundEvents.WOLF_HOWL, 3f, 0.6f);
                    Vfx.aura(level, this, 0xFF6A10, 1.4f, 130);
                    addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 130, 1));
                }
                if (t % 12 == 4 && t < 120) {
                    LivingEntity prey = null;
                    double best = 1e9;
                    for (LivingEntity e : enemiesNear(level, 11)) {
                        if (hunted.contains(e.getId()) && enemiesNear(level, 11).size() > hunted.size()) continue;
                        double d2 = distanceToSqr(e) - (e instanceof SiegeEnemy se && se.isHighPriority() ? 40 : 0);
                        if (d2 < best) {
                            best = d2;
                            prey = e;
                        }
                    }
                    if (prey != null) {
                        hunted.add(prey.getId());
                        Vec3 from = position();
                        Vec3 dir = prey.position().subtract(from).multiply(1, 0, 1);
                        Vec3 land = prey.position().subtract(dir.lengthSqr() > 1e-3 ? dir.normalize().scale(1.2) : Vec3.ZERO);
                        teleportTo(land.x, prey.getY(), land.z);
                        faceNow(prey.position());
                        Vfx.dash(level, from.add(0, 0.7, 0), position().add(0, 0.7, 0), 0xFF7A20, 0.8f, 12);
                        Vfx.spray(level, FxStyle.EMBER, from.add(0, 0.6, 0), 0xFFC040, 0xFF3010, 0.15f, 10, 0.2f, 0.5f);
                        hit(prey, dmg(2.4f), Element.FIRE);
                        SiegeDamage.applyStacking(prey, ModEffects.BLEEDING.get(), 120, 4);
                        prey.setSecondsOnFire(3);
                        Vfx.slash(level, position().add(0, 0.7, 0), getYRot(), 30, 160, 1.6f, 0xFF7A20, 0xFFE0A0, 8, 0.35f);
                        playSound(SoundEvents.FOX_BITE, 1.3f, 0.6f);
                    }
                }
                if (t >= 130) {
                    playSound(SoundEvents.WOLF_HOWL, 2f, 0.9f);
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
    protected void passiveTick(ServerLevel level) {
        if (skill == ULT && tickCount % 2 == 0) {
            Vfx.particles(level, com.lastbastion.fx.FxParticleOptions.of(FxStyle.EMBER, 0xFF8A20, 0.14f), position().add(0, 0.7, 0), 2, 0.4, 0.3, 0.6, 0.02);
        }
    }

    @Nullable
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return inCombat() ? SoundEvents.WOLF_GROWL : SoundEvents.WOLF_PANT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s) {
        return SoundEvents.WOLF_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }
}
