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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lucifer, King of Hell: the first rebel, crowned in thorns of black iron under a shattered halo of hellfire,
 * six burnt wings at his back and Perdition, a serrated greatsword forged in the pit, in his hands.
 * <ul>
 *   <li><b>Perdition combo</b> - four strokes of black hellfire; the last rends the earth in a burning fissure.</li>
 *   <li><b>The Fall</b> - he rises on his wings and falls on the horde like a burning star, leaving a crater of fire.</li>
 *   <li><b>Damnation</b> - brands every foe around him, then tears their souls out; the weak are dragged into the pit.</li>
 *   <li><b>Chains of Hell</b> - burning chains erupt from the ground, binding and dragging up to six victims.</li>
 *   <li><b>Wings of Perdition</b> - a storm of black feathers and hellfire hurls the crowd away.</li>
 *   <li><b>Dread Sovereign</b> - a roar that breaks the horde's nerve and drives his allies into a frenzy.</li>
 *   <li><b>Legions of Hell</b> - a hellgate spits out imps and hellhounds to fight at his side.</li>
 *   <li><b>ULT Hell on Earth</b> - he ascends, the sky burns, hellfire meteors rain on the battlefield, and he falls
 *   back to earth in a black nova that executes the broken.</li>
 * </ul>
 * Passive, Sovereign of Hell: foes near him smoulder and lose armour; every kill feeds him.
 */
public class LuciferDevil extends DevilEntity {
    public static final int FALL = 6, DAMNATION = 7, CHAINS = 8, WINGS = 9, DREAD = 10, LEGION = 11;
    public static final int CRIMSON = 0xD0141A, EMBER = 0xFF5A1A, BLACK = 0x0A0506, ASH = 0x2A1C1C;
    private Vec3 fallTo = Vec3.ZERO;
    private final List<LivingEntity> branded = new ArrayList<>();

    public LuciferDevil(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.LUCIFER);
    }

    @Override
    protected int fireColor() {
        return CRIMSON;
    }

    @Override
    protected int fireCore() {
        return EMBER;
    }

    @Override
    public Role role() {
        return Role.BRUISER;
    }

    /** True while airborne (the model spreads all six wings). */
    public boolean soaring() {
        return flag();
    }

    @Override
    protected double attackRange() {
        return getBbWidth() * 0.5 + 3.2;
    }

    @Override
    protected int attackWindup() {
        return 8;
    }

    @Override
    protected int attackRecover() {
        return 5;
    }

    @Override
    protected int attackInterval() {
        return 14;
    }

    @Override
    protected int comboLength() {
        return 4;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"Kneel. Your king has risen from the pit.", "You opened the gate, mortal. Pray you can close it."};
            case ULT -> new String[]{"LET HELL REIGN ON EARTH!", "BURN! ALL OF YOU, BURN!"};
            case BOSS -> new String[]{"A crowned corpse? I collect those.", "Come, carrion king. Bow or break."};
            case KILL -> new String[]{"Another soul for the pit.", "Pride. Fall. Repeat."};
            case LOW_HP -> new String[]{"I have fallen farther than this.", "You dare draw the blood of a king?"};
            case LEVEL_UP -> new String[]{"The pit grows deeper. So do I."};
            case IDLE -> new String[]{"Better to reign in Hell...", "*black flames crawl along his wings*"};
            default -> new String[0];
        };
    }

    // ------------------------------------------------------------------ helpers

    private Vec3 swordTip(double reach) {
        return position().add(0, getBbHeight() * 0.55, 0).add(look().scale(reach));
    }

    /** Infernal hit that feeds Lucifer on a kill. */
    private void burn(LivingEntity e, float amount) {
        if (hitKills(e, amount, Element.INFERNAL)) {
            feed(0.025f);
            Vfx.spray((ServerLevel) level(), FxStyle.SOUL, e.position().add(0, e.getBbHeight() * 0.6, 0), 0xFF8080, CRIMSON, 0.25f, 6, 0.12f, 1f);
        }
    }

    private void hellSlash(ServerLevel level, float tilt, float sweep, float radius) {
        Vec3 c = position().add(0, getBbHeight() * 0.55, 0);
        Vfx.darkSlash(level, c, getYRot(), tilt, sweep, radius, BLACK, CRIMSON, 10, 0.45f);
        Vfx.slash(level, c, getYRot(), tilt, sweep, radius * 1.04f, CRIMSON, EMBER, 8, 0.18f);
    }

    /** Execute a broken, non-boss enemy: its soul is dragged screaming into the pit. */
    private void dragToPit(ServerLevel level, LivingEntity e) {
        Vec3 p = e.position();
        Vfx.darkPillar(level, p, BLACK, CRIMSON, e.getBbWidth() + 0.4f, 3, 18);
        Vfx.spray(level, FxStyle.SKULL, p.add(0, e.getBbHeight() * 0.6, 0), 0xFFB0A0, CRIMSON, 0.3f, 3, 0.1f, 1f);
        SiegeDamage.deal(e, this, this, e.getMaxHealth() * 4f + 50f, Element.INFERNAL, false, true);
        feed(0.02f);
    }

    // ------------------------------------------------------------------ basic attack

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        switch (combo) {
            case 0 -> {
                hellSlash(level, -55, 150, 3.6f);
                for (LivingEntity e : arcTargets(level, 3.8, 160)) burn(e, dmg(1.1f));
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.55f);
            }
            case 1 -> {
                hellSlash(level, -55, -150, 3.6f);
                for (LivingEntity e : arcTargets(level, 3.8, 160)) burn(e, dmg(1.0f));
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);
            }
            case 2 -> {
                hellSlash(level, 4, 320, 3.8f);
                for (LivingEntity e : arcTargets(level, 3.9, 330)) {
                    burn(e, dmg(0.9f));
                    e.setSecondsOnFire(4);
                }
                Vfx.spray(level, FxStyle.EMBER, position().add(0, 1.4, 0), EMBER, CRIMSON, 0.2f, 18, 0.35f, 0.2f);
                playSound(SoundEvents.BLAZE_SHOOT, 1.2f, 0.5f);
            }
            default -> {
                // Rend: the blade splits the ground in a line of hellfire
                Vec3 dir = look();
                Vec3 from = position().add(dir.scale(1.2));
                Vfx.darkWave(level, from, from.add(dir.scale(9)), BLACK, CRIMSON, 1.6f, 1.4f, 14);
                for (int i = 0; i < 6; i++) {
                    Vec3 p = from.add(dir.scale(1.5 * i));
                    Vfx.jet(level, FxStyle.FLAME, p, p.add(0, 2.4, 0), EMBER, CRIMSON, 0.35f, 6, 18);
                }
                line(level, from, dir, 9, 1.3, dmg(1.8f), Element.INFERNAL);
                for (LivingEntity e : SiegeDamage.enemiesAround(level, from.add(dir.scale(4.5)), 5)) e.setSecondsOnFire(5);
                Vfx.shake(level, position(), 0.5f, 14, 8);
                playSound(SoundEvents.GENERIC_EXPLODE, 1.2f, 0.6f);
            }
        }
    }

    private List<LivingEntity> arcTargets(ServerLevel level, double reach, double arcDeg) {
        Vec3 look = look();
        double cos = Math.cos(Math.toRadians(arcDeg / 2));
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : enemiesNear(level, reach + 1.5)) {
            Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
            double d = to.length();
            if (d > reach + e.getBbWidth() * 0.5) continue;
            if (d > 0.8 && to.normalize().dot(look) < cos) continue;
            out.add(e);
        }
        return out;
    }

    // ------------------------------------------------------------------ skills

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        int close = crowd(level, position(), 4.5);
        if (ready(WINGS) && (close >= 3 || (close >= 2 && getHealth() < getMaxHealth() * 0.5f))) return WINGS;
        if (ready(DREAD) && (crowd(level, position(), 14) >= 7 || (t != null && big(t) && dist < 14 && getHealth() < getMaxHealth() * 0.6f))) return DREAD;
        if (ready(LEGION) && (crowd(level, position(), 16) >= 5 || (t != null && big(t)))) return LEGION;
        if (ready(DAMNATION) && (crowd(level, position(), 12) >= 4 || (t != null && big(t) && dist < 12))) return DAMNATION;
        if (t == null) return 0;
        if (ready(CHAINS) && crowd(level, position(), 14) >= 3 && dist > 4) return CHAINS;
        if (ready(FALL) && dist > 5 && dist < 22) {
            Vec3 c = bestCluster(level, 22, 5, 3);
            fallTo = c != null ? c : t.position();
            return FALL;
        }
        return 0;
    }

    @Override
    protected boolean wantsUltimate(ServerLevel level, @Nullable LivingEntity t, double dist) {
        return t != null && (crowd(level, position(), 16) >= 8 || (big(t) && dist < 16));
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case FALL -> {
                if (t == 1) {
                    setFlag(true);
                    setNoGravity(true);
                    Vfx.teleCircle(level, fallTo.add(0, 0.05, 0), 6, 34, CRIMSON, false);
                    Vfx.spray(level, FxStyle.FEATHER, position().add(0, 2, 0), 0x1A1416, BLACK, 0.3f, 16, 0.3f, 0.4f);
                    Vfx.ring(level, position().add(0, 0.05, 0), CRIMSON, 4, 12, 0.6f);
                    playSound(SoundEvents.ENDER_DRAGON_FLAP, 2f, 0.6f);
                }
                if (t <= 10) {
                    setDeltaMovement(0, 1.1, 0);
                    hasImpulse = true;
                } else if (t <= 22) {
                    Vec3 above = fallTo.add(0, 9, 0);
                    Vec3 step = above.subtract(position()).scale(0.25);
                    setDeltaMovement(step.x, step.y, step.z);
                    faceNow(fallTo);
                    if (t % 4 == 0) playSound(SoundEvents.ENDER_DRAGON_FLAP, 1.5f, 0.7f);
                } else if (t == 23) {
                    Vec3 dive = fallTo.subtract(position()).normalize().scale(2.6);
                    setDeltaMovement(dive);
                    Vfx.darkDash(level, position().add(0, 1.5, 0), fallTo.add(0, 1, 0), BLACK, CRIMSON, 1.6f, 12);
                    playSound(SoundEvents.WITHER_SHOOT, 2f, 0.5f);
                }
                if (t > 23 && (onGround() || t > 40)) {
                    setNoGravity(false);
                    setFlag(false);
                    impact(level, position(), 6f, dmg(5f));
                    HazardZone.spawn(level, HazardZone.Kind.FIRE, position(), 5, 100, dmg(0.12f), this, false);
                    cooldown(FALL, 260);
                    return true;
                }
            }
            case DAMNATION -> {
                if (t == 1) {
                    branded.clear();
                    branded.addAll(enemiesNear(level, 12));
                    if (branded.size() > 14) branded.subList(14, branded.size()).clear();
                    for (LivingEntity e : branded) {
                        Vfx.darkSigil(level, e.position().add(0, 0.05, 0), BLACK, CRIMSON, 1.1f + e.getBbWidth() * 0.5f, 24, 6f);
                        e.addEffect(new MobEffectInstance(ModEffects.MARKED.get(), 120, 1));
                    }
                    Vfx.darkSigil(level, position().add(0, 0.05, 0), BLACK, CRIMSON, 4, 30, -3f);
                    playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 2f, 0.5f);
                }
                if (t == 22) {
                    for (LivingEntity e : branded) {
                        if (!e.isAlive()) continue;
                        Vec3 at = e.position().add(0, e.getBbHeight() * 0.6, 0);
                        Vfx.darkBeam(level, at, position().add(0, getBbHeight() * 0.7, 0), 0x3A0A10, CRIMSON, 0.22f, 10, 0.3f);
                        Vfx.spray(level, FxStyle.SOUL, at, 0xFFB0A0, CRIMSON, 0.3f, 6, 0.2f, 0.6f);
                        if (!big(e) && e.getHealth() < e.getMaxHealth() * 0.35f) dragToPit(level, e);
                        else burn(e, dmg(3f));
                        feed(0.008f);
                    }
                    Vfx.screenFlash(level, position(), 0x400008, 0.35f, 24, 10);
                    playSound(SoundEvents.WITHER_SHOOT, 2f, 0.4f);
                    playSound(SoundEvents.SOUL_ESCAPE, 2f, 0.6f);
                }
                if (t >= 32) {
                    branded.clear();
                    cooldown(DAMNATION, 320);
                    return true;
                }
            }
            case CHAINS -> {
                if (t == 8) {
                    List<LivingEntity> foes = enemiesAt(level, position(), 14);
                    int n = 0;
                    for (LivingEntity e : foes) {
                        if (n++ >= 6) break;
                        Vfx.darkTether(level, this, getBbHeight() * 0.5f, e, 0x1A0A0A, CRIMSON, 0.16f, 30);
                        Vfx.spray(level, FxStyle.DEBRIS, e.position().add(0, 0.1, 0), 0x3A2A2A, 0x1A1010, 0.25f, 8, 0.25f, 1f);
                        burn(e, dmg(1.5f));
                        SiegeDamage.applyStacking(e, ModEffects.BLEEDING.get(), 100, 2);
                        if (big(e)) {
                            SiegeDamage.apply(e, ModEffects.CONCUSSED.get(), 100, 0);
                        } else {
                            SiegeDamage.apply(e, ModEffects.SHOCKED.get(), 50, 0);
                            Vec3 pull = position().subtract(e.position()).normalize().scale(Math.min(1.6, e.distanceTo(this) * 0.16));
                            e.setDeltaMovement(pull.x, 0.35, pull.z);
                            e.hurtMarked = true;
                        }
                    }
                    playSound(SoundEvents.CHAIN_BREAK, 2f, 0.5f);
                    playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.5f, 0.6f);
                }
                if (t >= 30) {
                    cooldown(CHAINS, 220);
                    return true;
                }
            }
            case WINGS -> {
                if (t == 9) {
                    Vec3 c = position().add(0, 1.4, 0);
                    Vfx.darkSphere(level, c, BLACK, CRIMSON, 5f, 12);
                    Vfx.ring(level, position().add(0, 0.1, 0), CRIMSON, 5.5f, 14, 0.8f);
                    Vfx.spray(level, FxStyle.FEATHER, c, 0x1A1416, BLACK, 0.4f, 40, 0.7f, 0.1f);
                    Vfx.spray(level, FxStyle.EMBER, c, EMBER, CRIMSON, 0.2f, 30, 0.6f, 0.2f);
                    for (LivingEntity e : enemiesNear(level, 5.5)) {
                        burn(e, dmg(2.4f));
                        if (!big(e)) SiegeDamage.push(e, position(), 2.0, 0.7);
                    }
                    playSound(SoundEvents.ENDER_DRAGON_FLAP, 2.5f, 0.5f);
                }
                if (t >= 20) {
                    cooldown(WINGS, 180);
                    return true;
                }
            }
            case DREAD -> {
                if (t == 10) {
                    Vfx.darkRing(level, position().add(0, 0.1, 0), 0x200406, 14, 22, 2.2f);
                    Vfx.ring(level, position().add(0, 0.1, 0), CRIMSON, 14, 20, 1f);
                    Vfx.screenFlash(level, position(), 0x500008, 0.4f, 30, 14);
                    Vfx.shake(level, position(), 0.8f, 30, 18);
                    playSound(SoundEvents.ENDER_DRAGON_GROWL, 3f, 0.55f);
                    for (LivingEntity e : enemiesNear(level, 14)) {
                        if (big(e)) {
                            SiegeDamage.apply(e, ModEffects.CONCUSSED.get(), 200, 0);
                            e.addEffect(new MobEffectInstance(ModEffects.MARKED.get(), 200, 2));
                        } else {
                            SiegeDamage.apply(e, ModEffects.FEARED.get(), 100, 0);
                            SiegeDamage.apply(e, ModEffects.CONCUSSED.get(), 200, 0);
                        }
                    }
                    // Infernal Command: allies fight like the damned
                    for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 14)) {
                        if (a instanceof com.lastbastion.entity.turret.TurretEntity) continue;
                        a.addEffect(new MobEffectInstance(ModEffects.RALLY.get(), 240, 0));
                        a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 240, 1));
                        Vfx.spray(level, FxStyle.EMBER, a.position().add(0, 1, 0), EMBER, CRIMSON, 0.2f, 8, 0.15f, 1f);
                    }
                }
                if (t >= 26) {
                    cooldown(DREAD, 500);
                    return true;
                }
            }
            case LEGION -> {
                if (t == 6) {
                    Vec3 gate = forward(3);
                    Vfx.darkSigil(level, gate.add(0, 0.05, 0), BLACK, CRIMSON, 2.8f, 40, 4f);
                    Vfx.darkPillar(level, gate, BLACK, CRIMSON, 1.4f, 4, 30);
                    playSound(SoundEvents.END_PORTAL_SPAWN, 1.2f, 0.6f);
                }
                if (t == 20) {
                    Vec3 gate = forward(3);
                    HellspawnEntity.summon(level, this, HellspawnEntity.Variant.IMP, gate.add(-1, 0, 0), 500);
                    HellspawnEntity.summon(level, this, HellspawnEntity.Variant.IMP, gate.add(1, 0, 0), 500);
                    HellspawnEntity.summon(level, this, HellspawnEntity.Variant.HELLHOUND, gate, 500);
                    if (heroLevel() >= 4) HellspawnEntity.summon(level, this, HellspawnEntity.Variant.FIEND, gate.add(0, 0, 1), 500);
                    Vfx.spray(level, FxStyle.FLAME, gate.add(0, 1, 0), EMBER, CRIMSON, 0.35f, 30, 0.4f, 0.8f);
                    playSound(SoundEvents.BLAZE_SHOOT, 2f, 0.4f);
                }
                if (t >= 28) {
                    cooldown(LEGION, 640);
                    return true;
                }
            }
            case ULT -> {
                if (t == 1) {
                    setFlag(true);
                    setNoGravity(true);
                    Vfx.screenFlash(level, position(), 0x600008, 0.5f, 48, 30);
                    Vfx.darkSigil(level, position().add(0, 0.05, 0), BLACK, CRIMSON, 8, 150, 2f);
                    playSound(SoundEvents.WITHER_SPAWN, 3f, 0.5f);
                    playSound(SoundEvents.ENDER_DRAGON_GROWL, 3f, 0.5f);
                }
                if (t <= 12) {
                    setDeltaMovement(0, 0.5, 0);
                    hasImpulse = true;
                } else if (t < 110) {
                    setDeltaMovement(getDeltaMovement().multiply(0.3, 0, 0.3));
                    if (target != null) faceTowards(target.position());
                    if (t % 20 == 0) {
                        Vfx.darkSphere(level, position().add(0, getBbHeight() + 3, 0), BLACK, CRIMSON, 2.6f, 22);
                        Vfx.flash(level, position().add(0, getBbHeight() + 3, 0), CRIMSON, 4f, 8);
                    }
                    if (t % 5 == 0) {
                        List<LivingEntity> foes = enemiesNear(level, 26);
                        Vec3 at = !foes.isEmpty() ? foes.get(random.nextInt(foes.size())).position()
                                : position().add(random.nextGaussian() * 10, -6, random.nextGaussian() * 10);
                        meteor(level, at);
                    }
                } else if (t == 110) {
                    setDeltaMovement(0, -2.2, 0);
                    hasImpulse = true;
                }
                if (t > 110 && (onGround() || t > 140)) {
                    setNoGravity(false);
                    setFlag(false);
                    Vec3 p = position();
                    Vfx.darkSphere(level, p.add(0, 1, 0), BLACK, CRIMSON, 10f, 20);
                    Vfx.sphere(level, p.add(0, 1, 0), CRIMSON, 10.5f, 16);
                    Vfx.ring(level, p.add(0, 0.1, 0), EMBER, 11, 20, 1.4f);
                    Vfx.screenFlash(level, p, 0x800010, 0.5f, 48, 16);
                    Vfx.shake(level, p, 1.6f, 48, 30);
                    playSound(SoundEvents.GENERIC_EXPLODE, 3f, 0.5f);
                    playSound(SoundEvents.WITHER_DEATH, 1.5f, 1.4f);
                    for (LivingEntity e : enemiesNear(level, 10)) {
                        if (!big(e) && e.getHealth() < e.getMaxHealth() * 0.4f) {
                            dragToPit(level, e);
                            continue;
                        }
                        burn(e, dmg(6f));
                        e.setSecondsOnFire(8);
                        if (!big(e)) SiegeDamage.push(e, p, 2.4, 0.8);
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

    /** One hellfire meteor of the ultimate. */
    private void meteor(ServerLevel level, Vec3 at) {
        Vec3 sky = at.add(random.nextGaussian() * 3, 22, random.nextGaussian() * 3);
        Vfx.darkBeam(level, sky, at.add(0, 0.5, 0), 0x1A0204, CRIMSON, 0.7f, 8, 0.2f);
        Vfx.beam(level, sky, at.add(0, 0.5, 0), CRIMSON, EMBER, 0.25f, 6, 0.1f);
        impact(level, at, 3.2f, dmg(2.6f));
        if (random.nextInt(3) == 0) HazardZone.spawn(level, HazardZone.Kind.FIRE, at, 2.5f, 60, dmg(0.1f), this, false);
    }

    /** A burning-star impact: black blast, crimson shell, crater ring, damage with knock-up. */
    private void impact(ServerLevel level, Vec3 at, float radius, float damage) {
        Vfx.darkSphere(level, at.add(0, 0.5, 0), BLACK, CRIMSON, radius, 14);
        Vfx.sphere(level, at.add(0, 0.5, 0), CRIMSON, radius * 1.05f, 10);
        Vfx.ring(level, at.add(0, 0.05, 0), EMBER, radius + 1, 14, 0.7f);
        Vfx.spray(level, FxStyle.DEBRIS, at.add(0, 0.3, 0), 0x2A1E1A, 0x0E0A08, 0.3f, 16, 0.6f, 0.9f);
        Vfx.spray(level, FxStyle.EMBER, at.add(0, 0.5, 0), EMBER, CRIMSON, 0.25f, 18, 0.5f, 0.6f);
        Vfx.shake(level, at, 0.7f, 24, 10);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.6f, 0.7f);
        for (LivingEntity e : SiegeDamage.enemiesAround(level, at, radius)) {
            float falloff = (float) (1 - 0.4 * Math.min(1, e.position().distanceTo(at) / radius));
            burn(e, damage * falloff);
            e.setSecondsOnFire(5);
            if (!big(e)) {
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
                e.hurtMarked = true;
            }
        }
    }

    // ------------------------------------------------------------------ passive

    @Override
    protected void aura(ServerLevel level) {
        if (tickCount % 20 != 0) return;
        for (LivingEntity e : enemiesNear(level, 6)) {
            burn(e, dmg(0.2f));
            SiegeDamage.applyStacking(e, ModEffects.CORRODED.get(), 60, 2);
        }
        if (inCombat()) Vfx.darkRing(level, position().add(0, 0.05, 0), 0x1A0204, 6, 18, 0.35f);
    }

    @Override
    protected void arrival(ServerLevel level) {
        Vfx.spray(level, FxStyle.FEATHER, position().add(0, 2.5, 0), 0x1A1416, BLACK, 0.4f, 40, 0.4f, 0.2f);
        Vfx.darkPillar(level, position(), BLACK, CRIMSON, 2.4f, 30, 50);
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 3f, 0.5f);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return inCombat() ? SoundEvents.BLAZE_AMBIENT : null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p && !p.isCreative()) return false;
        return super.hurt(source, amount);
    }

    @Override
    protected void onSkillInterrupted(int skill) {
        super.onSkillInterrupted(skill);
        setFlag(false);
        branded.clear();
    }
}
