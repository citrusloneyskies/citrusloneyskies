package com.lastbastion.entity.boss;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.entity.zombie.ZombieKind;
import com.lastbastion.entity.zombie.ZombieRank;
import com.lastbastion.entity.zombie.ZombieSpawner;
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
import java.util.ArrayList;
import java.util.List;

/**
 * The Plague Mother: a corpse-queen fused to a bloated, egg-laden abdomen on six spindly legs. She
 * sweeps a cone of bile, hatches broods of runners, marks the ground for acid rain and bursts her
 * pustules when crowded. At half health her abdomen splits and she starts leaping onto her prey.
 */
public class PlagueMotherBoss extends SiegeBoss {
    public static final int SPEW = 3, BROOD = 4, RAIN = 5, LEAP = 6, BURST = 7;
    private Vec3 leapTarget = Vec3.ZERO;
    private final List<Vec3> rainSpots = new ArrayList<>();

    public PlagueMotherBoss(EntityType<? extends SiegeBoss> type, Level level) {
        super(type, level, BossKind.PLAGUE_MOTHER);
    }

    @Override
    public int dangerColor() {
        return 0x8BE040;
    }

    @Override
    protected String epithet() {
        return "Queen of the Rotting Brood";
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"Come to Mother, little ones...", "My children are HUNGRY."};
            case TAUNT -> new String[]{"Feed them... feed them all!", "Shhh... let the brood kiss you.", "Such soft flesh. Such good nurseries."};
            case PHASE -> new String[]{"You hurt my babies! I will birth a THOUSAND more!", "The brood WAKES!"};
            case STAGGER -> new String[]{"Nnngh... the eggs... the eggs!"};
            case DEATH -> new String[]{"My children... will remember you...", "Mother... is... tired..."};
            case CORE -> new String[]{"A warm stone to lay my eggs upon..."};
            default -> new String[0];
        };
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
    protected void introTick(ServerLevel level, int t) {
        if (t % 8 == 0) Vfx.spray(level, FxStyle.BUBBLE, position().add(0, 1.5, 0), 0xA8F050, 0x5A9A20, 0.2f, 8, 0.2f, 1);
        if (t == 20) Fx.sound(level, position(), SoundEvents.TURTLE_EGG_CRACK, 2f, 0.5f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dt, double dc) {
        if (ready(BURST) && SiegeDamage.defendersAround(level, position(), 4.5).size() >= 2) return BURST;
        if (ready(BROOD) && tickCount > 80) return BROOD;
        if (t != null && dt < 9 && ready(SPEW)) return SPEW;
        if (phase() >= 2 && t != null && dt > 5 && dt < 16 && ready(LEAP)) return LEAP;
        if ((t != null && dt < 22 || dc < 22) && ready(RAIN)) return RAIN;
        if (t != null && random.nextInt(400) == 0) speak(Lines.TAUNT, false);
        return 0;
    }

    private Vec3 mouth() {
        return position().add(0, getBbHeight() * 0.85, 0).add(Vec3.directionFromRotation(0, getYRot()).scale(1.8));
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case SPEW -> {
                Vec3 aim = aimPoint();
                if (t < 10) face(aim);
                if (t == 2) teleCone(level, position(), aim, 9.5f, 40, 12);
                if (t > 12 && t < 52 && t % 4 == 0) {
                    Vec3 mouth = mouth();
                    float sweep = (float) Math.sin((t - 12) / 40.0 * Math.PI * 2) * 30f;
                    Vec3 dir = Vec3.directionFromRotation(18, getYRot() + sweep);
                    Vfx.jet(level, FxStyle.DROP, mouth, mouth.add(dir.scale(9)), 0xC8FF60, 0x5A9A20, 0.22f, 22, 14);
                    Vfx.jet(level, FxStyle.SMOKE, mouth, mouth.add(dir.scale(8)), 0x9BD940, 0x4A6A20, 0.6f, 5, 18);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, mouth.add(dir.scale(4.5)), 4.8)) {
                        Vec3 to = e.position().subtract(mouth).normalize();
                        if (to.dot(dir) > 0.72) {
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
                if (t == 4) Fx.sound(level, position(), SoundEvents.TURTLE_EGG_CRACK, 2f, 0.6f);
                if (t == 22) {
                    Fx.sound(level, position(), SoundEvents.TURTLE_EGG_HATCH, 2f, 0.5f);
                    Vfx.spray(level, FxStyle.DROP, position().add(0, 1.2, 0), 0xC8FF60, 0x5A9A20, 0.25f, 30, 0.4f, 0.7f);
                    int n = 3 + phase();
                    for (int i = 0; i < n; i++) {
                        Vec3 p = position().add(random.nextGaussian() * 1.5, 0.5, random.nextGaussian() * 1.5);
                        SiegeZombie z = ZombieSpawner.spawn(level, ZombieKind.RUNNER, ZombieRank.byId(1 + phase()), 0, p, true, false);
                        if (z != null) {
                            z.setMinion(true);
                            z.setDeltaMovement(random.nextGaussian() * 0.3, 0.5, random.nextGaussian() * 0.3);
                        }
                    }
                    speak(Lines.TAUNT, false);
                }
                if (t >= 38) {
                    cooldown(BROOD, 320);
                    return true;
                }
            }
            case RAIN -> {
                if (t == 2) {
                    rainSpots.clear();
                    List<LivingEntity> defs = SiegeDamage.defendersAround(level, position(), 22);
                    for (int i = 0; i < 5; i++) {
                        Vec3 at;
                        if (!defs.isEmpty()) {
                            LivingEntity d = defs.get(random.nextInt(defs.size()));
                            at = d.position().add(random.nextGaussian() * 1.5, 0, random.nextGaussian() * 1.5);
                        } else {
                            at = aimPoint().add(random.nextGaussian() * 4, 0, random.nextGaussian() * 4);
                        }
                        rainSpots.add(at);
                        teleCircle(level, at, 3f, 28 + i * 3);
                    }
                    Fx.sound(level, position(), SoundEvents.WITCH_CELEBRATE, 1.5f, 0.5f);
                }
                if (t >= 16 && t < 16 + rainSpots.size() * 3 && (t - 16) % 3 == 0) {
                    Vec3 at = rainSpots.get((t - 16) / 3);
                    SiegeProjectile glob = SiegeProjectile.create(level, SiegeProjectile.ProjKind.ACID_GLOB, this, at.add(random.nextGaussian(), 16, random.nextGaussian()));
                    glob.payload(dmg(0.9f), Element.POISON).area(3f).status(ModEffects.CORRODED, 120, 4);
                    glob.gravity = 0;
                    glob.onImpact = (p, hit) -> HazardZone.spawn(level, HazardZone.Kind.ACID, hit, 2.6f, 120, damageScale, this, true);
                    glob.aim(at, 1.2f).scale(2f).fire(level);
                }
                if (t >= 44) {
                    cooldown(RAIN, 260);
                    return true;
                }
            }
            case LEAP -> {
                if (t == 2) {
                    leapTarget = target != null ? target.position() : aimPoint();
                    face(leapTarget);
                    teleCircle(level, leapTarget, 5.5f, 22);
                }
                if (t == 12) {
                    Vec3 d = leapTarget.subtract(position());
                    setDeltaMovement(d.x * 0.09, 1.0, d.z * 0.09);
                    hasImpulse = true;
                    Fx.sound(level, position(), SoundEvents.SPIDER_AMBIENT, 2f, 0.4f);
                }
                if (t > 16 && onGround()) {
                    Fx.shockwave(level, position(), 6, 0x7BD940);
                    Vfx.spray(level, FxStyle.DROP, position().add(0, 0.6, 0), 0xC8FF60, 0x5A9A20, 0.3f, 40, 0.5f, 0.6f);
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
            case BURST -> {
                if (t == 2) teleCircle(level, position(), 5.5f, 18);
                if (t == 20) {
                    Vfx.sphere(level, position().add(0, 1.2, 0), 0x8BE040, 5.5f, 12);
                    Vfx.spray(level, FxStyle.SMOKE, position().add(0, 1, 0), 0x9BD940, 0x4A6A20, 1.4f, 30, 0.35f, 0);
                    HazardZone.spawn(level, HazardZone.Kind.POISON_CLOUD, position(), 5f, 100, damageScale, this, true);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 5.5)) {
                        SiegeDamage.deal(e, this, this, dmg(1.1f), Element.POISON, false);
                        SiegeDamage.push(e, position(), 1.3, 0.4);
                        e.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                    }
                    Fx.sound(level, position(), SoundEvents.SLIME_DEATH, 2.5f, 0.4f);
                }
                if (t >= 34) {
                    cooldown(BURST, 200);
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
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (tickCount % 8 == 0) Vfx.spray(level, FxStyle.DROP, position().add(0, 0.6, 0), 0xC8FF60, 0x5A9A20, 0.14f, 2, 0.05f, 0);
    }

    @Override
    protected void onPhase(ServerLevel level, int phase) {
        super.onPhase(level, phase);
        for (int i = 0; i < 5; i++) {
            Vec3 p = position().add(random.nextGaussian() * 4, 0, random.nextGaussian() * 4);
            HazardZone.spawn(level, HazardZone.Kind.POISON_CLOUD, p, 3f, 200, damageScale, this, true);
        }
        Vfx.spray(level, FxStyle.DROP, position().add(0, 1.5, 0), 0xC8FF60, 0x5A9A20, 0.3f, 50, 0.5f, 0.7f);
        startSkill(BROOD);
    }

    @Override
    protected String phaseLine(int phase) {
        return "bursts with pestilence!";
    }
}
