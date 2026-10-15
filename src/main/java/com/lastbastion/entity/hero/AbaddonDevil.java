package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Abaddon, the Destroyer, angel of the bottomless pit: a towering king of blackened iron and bone with a vortex of
 * violet void where his heart should be, a scorpion's tail arched over his shoulder, ragged locust wings and the
 * Key of the Abyss for a mace. The horde breaks on him.
 * <ul>
 *   <li><b>Key of the Abyss</b> - crushing overhead blows, a sweeping strike and a tail jab.</li>
 *   <li><b>Abyss Gate</b> - he opens the pit under a crowd; it drags them in, then erupts in chains and smoke.</li>
 *   <li><b>Chains of the Pit</b> - chains lash out of his chest and haul up to six enemies to his feet.</li>
 *   <li><b>Scorpion's Torment</b> - a stinger strike whose venom wracks even bosses.</li>
 *   <li><b>Locust Plague</b> - a cone of locusts that strips and blinds.</li>
 *   <li><b>Destroyer's Challenge</b> - every enemy around turns on him while he shrugs off most damage.</li>
 *   <li><b>Shattering the Seal</b> - the Key splits the ground in a line of erupting fissures.</li>
 *   <li><b>ULT The Bottomless Pit</b> - he turns the Key in the earth; the ground opens around him, smoke blots out
 *   the sky, locusts rise, and chains drag the horde down into the dark.</li>
 * </ul>
 * Passive, Sovereign of the Abyss: he takes 30% less damage and slows everything that comes near.
 */
public class AbaddonDevil extends DevilEntity {
    public static final int PIT = 6, CHAINS = 7, STING = 8, LOCUSTS = 9, CHALLENGE = 10, SEAL = 11;
    public static final int VOID = 0x8A4CFF, VOID_DARK = 0x14081E, IRON = 0x1A1820, SMOKE = 0x1A161C, LOCUST = 0x5A4A22;
    private Vec3 pitAt = Vec3.ZERO;
    private int challenge;
    private final List<LivingEntity> chained = new ArrayList<>();

    public AbaddonDevil(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.ABADDON);
    }

    @Override
    protected int fireColor() {
        return VOID;
    }

    @Override
    protected int fireCore() {
        return 0xD0A0FF;
    }

    @Override
    public Role role() {
        return Role.TANK;
    }

    /** True while Destroyer's Challenge is active (the model's chest vortex flares). */
    public boolean challenging() {
        return flag();
    }

    @Override
    protected double attackRange() {
        return getBbWidth() * 0.5 + 3.4;
    }

    @Override
    protected int attackWindup() {
        return 12;
    }

    @Override
    protected int attackRecover() {
        return 8;
    }

    @Override
    protected int attackInterval() {
        return 22;
    }

    @Override
    protected int comboLength() {
        return 3;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"The pit is open. I am its key.", "ABADDON. APOLLYON. DESTROYER. Choose a name to scream."};
            case ULT -> new String[]{"THE PIT OPENS!", "COME FORTH FROM THE ABYSS!"};
            case BOSS -> new String[]{"Face me, carrion king.", "Your bones will hold my gates shut."};
            case KILL -> new String[]{"Torment. Then nothing.", "Into the pit."};
            case LOW_HP -> new String[]{"The abyss has no bottom. Neither does my patience.", "You only wake me further."};
            case LEVEL_UP -> new String[]{"The pit widens."};
            case IDLE -> new String[]{"*chains rattle in the dark of his chest*", "Five months of torment. A gentle start."};
            default -> new String[0];
        };
    }

    private void crush(LivingEntity e, float amount) {
        if (hitKills(e, amount, Element.INFERNAL)) feed(0.015f);
    }

    // ------------------------------------------------------------------ basic attack

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        Vec3 c = position().add(0, getBbHeight() * 0.5, 0);
        switch (combo) {
            case 0 -> {
                // overhead: the Key comes down like a gate slamming
                Vec3 at = forward(2.6);
                Vfx.darkRing(level, at.add(0, 0.1, 0), VOID_DARK, 3.4f, 12, 0.6f);
                Vfx.ring(level, at.add(0, 0.1, 0), VOID, 3.2f, 10, 0.4f);
                Vfx.spray(level, FxStyle.DEBRIS, at, 0x2A2420, 0x100C0A, 0.3f, 14, 0.45f, 0.9f);
                Vfx.shake(level, at, 0.5f, 14, 8);
                for (LivingEntity e : SiegeDamage.enemiesAround(level, at, 3.4)) {
                    crush(e, dmg(1.4f));
                    if (!big(e)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                }
                playSound(SoundEvents.ANVIL_LAND, 1.4f, 0.5f);
            }
            case 1 -> {
                Vfx.darkSlash(level, c, getYRot(), 6, 220, 3.8f, VOID_DARK, VOID, 10, 0.5f);
                arc(level, 4, 220, dmg(1.1f), Element.INFERNAL);
                for (LivingEntity e : enemiesNear(level, 4.5)) if (!big(e)) SiegeDamage.push(e, position(), 1.1, 0.3);
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.6f, 0.4f);
            }
            default -> {
                // the stinger darts over his shoulder
                Vfx.darkBeam(level, position().add(0, getBbHeight() * 1.05, 0), t.position().add(0, t.getBbHeight() * 0.6, 0), VOID_DARK, VOID, 0.25f, 6, 0);
                crush(t, dmg(1.5f));
                SiegeDamage.applyStacking(t, ModEffects.BLEEDING.get(), 120, 3);
                playSound(SoundEvents.TRIDENT_HIT, 1.4f, 0.5f);
            }
        }
    }

    // ------------------------------------------------------------------ skills

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        int near = crowd(level, position(), 10);
        if (ready(CHALLENGE) && (near >= 5 || hurtAlly(level, 12, 0.5f) != null)) return CHALLENGE;
        if (ready(CHAINS) && crowd(level, position(), 14) >= 3 && near < 6) return CHAINS;
        if (t == null) return 0;
        if (ready(STING) && big(t) && dist < 6) return STING;
        if (ready(PIT)) {
            Vec3 c = bestCluster(level, 18, 4.5, 4);
            if (c != null) {
                pitAt = c;
                return PIT;
            }
        }
        if (ready(LOCUSTS) && dist < 9 && crowd(level, t.position(), 4) >= 3) return LOCUSTS;
        if (ready(SEAL) && dist < 10 && crowd(level, t.position(), 4) >= 2) return SEAL;
        if (ready(STING) && dist < 6) return STING;
        return 0;
    }

    @Override
    protected boolean wantsUltimate(ServerLevel level, @Nullable LivingEntity t, double dist) {
        return t != null && (crowd(level, position(), 14) >= 9 || (big(t) && dist < 10 && getHealth() < getMaxHealth() * 0.7f));
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case PIT -> {
                faceTowards(pitAt);
                if (t == 6) {
                    Vfx.darkSigil(level, pitAt.add(0, 0.04, 0), VOID_DARK, VOID, 5, 70, -5f);
                    HazardZone.spawn(level, HazardZone.Kind.GRAVITY_WELL, pitAt, 5, 60, dmg(0.15f), this, false);
                    playSound(SoundEvents.END_PORTAL_SPAWN, 1.5f, 0.5f);
                }
                if (t > 6 && t < 64 && t % 4 == 0) {
                    Vfx.darkVortex(level, pitAt, VOID_DARK, VOID, 4.6f, 1.6f, 6, null);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, pitAt, 6)) {
                        if (!big(e)) {
                            Vec3 pull = pitAt.subtract(e.position()).multiply(1, 0, 1);
                            if (pull.lengthSqr() > 0.3) {
                                pull = pull.normalize().scale(0.28);
                                e.setDeltaMovement(e.getDeltaMovement().multiply(0.4, 1, 0.4).add(pull));
                                e.hurtMarked = true;
                            }
                        }
                        if (t % 12 == 0) crush(e, dmg(0.5f));
                    }
                }
                if (t == 64) {
                    Vfx.darkSphere(level, pitAt.add(0, 0.6, 0), VOID_DARK, VOID, 5.5f, 16);
                    Vfx.darkPillar(level, pitAt, SMOKE, VOID, 4f, 9, 40);
                    Vfx.spray(level, FxStyle.SMOKE, pitAt.add(0, 0.5, 0), SMOKE, 0x050406, 1.4f, 40, 0.4f, 0.8f);
                    Vfx.shake(level, pitAt, 0.9f, 28, 14);
                    for (int i = 0; i < 8; i++) {
                        double a = i * Math.PI / 4;
                        Vec3 to = pitAt.add(Math.cos(a) * 4, 3.5, Math.sin(a) * 4);
                        Vfx.darkBeam(level, pitAt.add(0, 0.2, 0), to, 0x1A1418, VOID, 0.18f, 14, 0.2f);
                    }
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, pitAt, 5.5)) {
                        crush(e, dmg(4f));
                        if (!big(e)) {
                            e.setDeltaMovement(0, 1.1, 0);
                            e.hurtMarked = true;
                        }
                        e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
                    }
                    playSound(SoundEvents.GENERIC_EXPLODE, 2f, 0.5f);
                    playSound(SoundEvents.CHAIN_BREAK, 2f, 0.4f);
                }
                if (t >= 72) {
                    cooldown(PIT, 300);
                    return true;
                }
            }
            case CHAINS -> {
                if (t == 8) {
                    chained.clear();
                    for (LivingEntity e : enemiesAt(level, position(), 14)) {
                        if (chained.size() >= 6) break;
                        chained.add(e);
                        Vfx.darkTether(level, this, getBbHeight() * 0.6f, e, 0x2A2428, VOID, 0.2f, 34);
                        crush(e, dmg(1.2f));
                        if (big(e)) SiegeDamage.apply(e, ModEffects.CONCUSSED.get(), 120, 0);
                        else SiegeDamage.apply(e, ModEffects.SHOCKED.get(), 40, 0);
                    }
                    playSound(SoundEvents.CHAIN_PLACE, 2f, 0.4f);
                    playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.5f, 0.5f);
                }
                if (t > 8 && t <= 24) {
                    for (LivingEntity e : chained) {
                        if (!e.isAlive() || big(e)) continue;
                        Vec3 to = position().add(look().scale(2.4));
                        Vec3 pull = to.subtract(e.position());
                        if (pull.lengthSqr() > 1.5) {
                            e.setDeltaMovement(pull.normalize().scale(0.7).add(0, 0.12, 0));
                            e.hurtMarked = true;
                        }
                    }
                }
                if (t >= 32) {
                    chained.clear();
                    cooldown(CHAINS, 240);
                    return true;
                }
            }
            case STING -> {
                if (target != null) faceTowards(target.position());
                if (t == 12 && target != null && target.isAlive()) {
                    Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
                    Vfx.darkBeam(level, position().add(0, getBbHeight() * 1.1, 0), at, VOID_DARK, VOID, 0.45f, 8, 0);
                    Vfx.darkFlash(level, at, VOID_DARK, VOID, 2.4f, 8);
                    Vfx.spray(level, FxStyle.DROP, at, 0x9A6AE0, 0x3A1A5A, 0.2f, 16, 0.35f, 0.3f);
                    crush(target, dmg(6f) * (big(target) ? 1.4f : 1f));
                    SiegeDamage.applyStacking(target, ModEffects.BLEEDING.get(), 200, 4);
                    SiegeDamage.apply(target, ModEffects.CONCUSSED.get(), 200, 0);
                    SiegeDamage.dot(target, dmg(1.5f), Element.INFERNAL);
                    playSound(SoundEvents.TRIDENT_HIT, 2f, 0.4f);
                    playSound(SoundEvents.SPIDER_HURT, 1.5f, 0.3f);
                }
                if (t >= 24) {
                    cooldown(STING, 200);
                    return true;
                }
            }
            case LOCUSTS -> {
                if (target != null) faceTowards(target.position());
                if (t >= 8 && t <= 36 && t % 3 == 0) {
                    Vec3 from = position().add(0, getBbHeight() * 0.6, 0);
                    Vec3 dir = look();
                    Vfx.jet(level, FxStyle.DEBRIS, from, from.add(dir.scale(10)), LOCUST, 0x2A2010, 0.15f, 30, 35);
                    Vfx.jet(level, FxStyle.SMOKE, from, from.add(dir.scale(8)), SMOKE, 0x0A080A, 0.5f, 6, 30);
                    double cos = Math.cos(Math.toRadians(35));
                    for (LivingEntity e : enemiesNear(level, 10)) {
                        Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
                        if (to.lengthSqr() > 0.5 && to.normalize().dot(dir) < cos) continue;
                        crush(e, dmg(0.45f));
                        SiegeDamage.applyStacking(e, ModEffects.CORRODED.get(), 120, 4);
                        if (!big(e)) SiegeDamage.apply(e, ModEffects.DISORIENTED.get(), 60, 0);
                    }
                    if (t % 9 == 0) playSound(SoundEvents.BEE_LOOP_AGGRESSIVE, 2f, 0.6f);
                }
                if (t >= 40) {
                    cooldown(LOCUSTS, 220);
                    return true;
                }
            }
            case CHALLENGE -> {
                if (t == 8) {
                    setFlag(true);
                    challenge = 140;
                    Vfx.darkRing(level, position().add(0, 0.1, 0), VOID_DARK, 14, 20, 1.6f);
                    Vfx.darkAura(level, this, VOID_DARK, VOID, 2.2f, 140);
                    Vfx.shake(level, position(), 0.6f, 20, 10);
                    playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.5f, 0.4f);
                    for (LivingEntity e : enemiesNear(level, 14)) {
                        if (e instanceof Mob mob) mob.setTarget(this);
                        if (e instanceof com.lastbastion.entity.zombie.SiegeZombie z) z.taunt(this, 140);
                    }
                }
                if (t >= 18) {
                    cooldown(CHALLENGE, 420);
                    return true;
                }
            }
            case SEAL -> {
                if (target != null) faceTowards(target.position());
                if (t == 14) {
                    Vec3 dir = look();
                    playSound(SoundEvents.GENERIC_EXPLODE, 1.5f, 0.5f);
                    for (int i = 0; i < 7; i++) {
                        Vec3 at = position().add(dir.scale(2 + i * 1.6));
                        Vfx.darkPillar(level, at, SMOKE, VOID, 1.1f, 3.5f, 18 + i * 2);
                        Vfx.spray(level, FxStyle.DEBRIS, at.add(0, 0.2, 0), 0x2A2420, 0x100C0A, 0.3f, 8, 0.4f, 1f);
                    }
                    Vfx.shake(level, position(), 0.7f, 20, 10);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, position().add(dir.scale(6.5)), 7)) {
                        Vec3 rel = e.position().subtract(position()).multiply(1, 0, 1);
                        double along = rel.dot(dir);
                        if (along < 0 || along > 13 || rel.subtract(dir.scale(along)).lengthSqr() > 2.5 * 2.5) continue;
                        crush(e, dmg(3f));
                        if (!big(e)) {
                            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.9, 0));
                            e.hurtMarked = true;
                        }
                    }
                }
                if (t >= 26) {
                    cooldown(SEAL, 180);
                    return true;
                }
            }
            case ULT -> {
                if (t == 1) {
                    Vfx.screenFlash(level, position(), 0x100418, 0.5f, 48, 30);
                    playSound(SoundEvents.WITHER_SPAWN, 2.5f, 0.4f);
                }
                if (t == 16) {
                    Vfx.darkSigil(level, position().add(0, 0.04, 0), VOID_DARK, VOID, 14, 160, 1.5f);
                    Vfx.shake(level, position(), 1.2f, 40, 30);
                    playSound(SoundEvents.END_PORTAL_SPAWN, 2.5f, 0.4f);
                    for (LivingEntity e : enemiesNear(level, 14)) e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
                }
                if (t > 16 && t < 150) {
                    if (t % 6 == 0) {
                        double a = random.nextDouble() * Math.PI * 2, r = 3 + random.nextDouble() * 10;
                        Vec3 crack = position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                        Vfx.darkPillar(level, crack, SMOKE, VOID, 1.2f, 6, 26);
                        Vfx.spray(level, FxStyle.SMOKE, crack.add(0, 0.5, 0), SMOKE, 0x050406, 1.2f, 10, 0.3f, 1f);
                        Vfx.jet(level, FxStyle.DEBRIS, crack, crack.add(0, 6, 0), LOCUST, 0x2A2010, 0.14f, 14, 40);
                    }
                    if (t % 10 == 0) {
                        for (LivingEntity e : enemiesNear(level, 14)) {
                            crush(e, dmg(0.9f));
                            if (!big(e) && random.nextInt(3) == 0) {
                                Vec3 pull = position().subtract(e.position()).normalize().scale(0.6);
                                e.setDeltaMovement(pull.x, -0.2, pull.z);
                                e.hurtMarked = true;
                                Vfx.darkTether(level, this, 0.2f, e, 0x2A2428, VOID, 0.12f, 10);
                            }
                        }
                    }
                    if (t % 30 == 0) playSound(SoundEvents.BEE_LOOP_AGGRESSIVE, 2.5f, 0.5f);
                }
                if (t == 150) {
                    Vfx.darkSphere(level, position().add(0, 1, 0), VOID_DARK, VOID, 14, 24);
                    Vfx.ring(level, position().add(0, 0.1, 0), VOID, 14, 24, 1.6f);
                    Vfx.shake(level, position(), 1.6f, 48, 30);
                    playSound(SoundEvents.GENERIC_EXPLODE, 3f, 0.4f);
                    for (LivingEntity e : enemiesNear(level, 14)) {
                        if (!big(e) && e.getHealth() < e.getMaxHealth() * 0.3f) {
                            SiegeDamage.deal(e, this, this, e.getMaxHealth() * 4f + 50f, Element.INFERNAL, false, true);
                            Vfx.darkPillar(level, e.position(), SMOKE, VOID, e.getBbWidth() + 0.4f, 3, 18);
                            continue;
                        }
                        crush(e, dmg(5f));
                    }
                    return true;
                }
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ passive

    @Override
    protected void aura(ServerLevel level) {
        if (challenge > 0 && --challenge == 0) setFlag(false);
        if (tickCount % 20 == 0) {
            for (LivingEntity e : enemiesNear(level, 4.5)) {
                if (!big(e)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false));
                crush(e, dmg(0.15f));
            }
            if (inCombat()) Vfx.darkRing(level, position().add(0, 0.05, 0), VOID_DARK, 4.5f, 16, 0.25f);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount * (challenging() ? 0.35f : 0.7f));
    }

    @Override
    protected void arrival(ServerLevel level) {
        Vfx.darkVortex(level, position(), VOID_DARK, VOID, 3.6f, 5, 50, this);
        Vfx.spray(level, FxStyle.SMOKE, position().add(0, 1, 0), SMOKE, 0x050406, 1.4f, 40, 0.3f, 0.6f);
        playSound(SoundEvents.CHAIN_BREAK, 2.5f, 0.4f);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return inCombat() ? SoundEvents.CHAIN_STEP : null;
    }
}
