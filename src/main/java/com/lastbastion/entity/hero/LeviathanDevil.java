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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Leviathan, Serpent of the Abyss: the coiled titan of the black sea - envy given scales. He rears out of his own
 * coils, all teeth and glowing fins, and drowns the horde in a sea that isn't there.
 * <ul>
 *   <li><b>Abyssal Bite / Tail Lash</b> - a lunging bite that soaks, then a sweep of the coils.</li>
 *   <li><b>Black Tide</b> - a wall of black water rolls the horde back.</li>
 *   <li><b>Maelstrom</b> - a whirlpool drags a crowd under.</li>
 *   <li><b>Crushing Coils</b> - he wraps the biggest enemy (bosses too), crushes it, then hurls it away.</li>
 *   <li><b>Abyssal Breath</b> - a beam of crushing deep-sea pressure.</li>
 *   <li><b>The Deep Boils</b> - the ground around him erupts in scalding steam.</li>
 *   <li><b>Abyssal Dive</b> - he sinks into the earth and bursts up under the horde.</li>
 *   <li><b>ULT The Deep Rises</b> - the abyss floods the battlefield: everything is lifted, drowned, and slammed
 *   back down.</li>
 * </ul>
 * Passive, Envy of the Deep: soaked enemies take 40% more damage from him.
 */
public class LeviathanDevil extends DevilEntity {
    public static final int TIDE = 6, MAELSTROM = 7, COILS = 8, BREATH = 9, BOIL = 10, DIVE = 11;
    public static final int ABYSS = 0x2FB8C8, DEEP = 0x041418, FOAM = 0xBFF8FF, STEAM = 0xE8F4F4;
    @Nullable
    private LivingEntity coiled;
    private Vec3 diveTo = Vec3.ZERO;

    public LeviathanDevil(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.LEVIATHAN);
    }

    @Override
    protected int fireColor() {
        return ABYSS;
    }

    @Override
    protected int fireCore() {
        return FOAM;
    }

    @Override
    public Role role() {
        return Role.BRUISER;
    }

    /** True while burrowed during Abyssal Dive. */
    public boolean submerged() {
        return flag();
    }

    @Override
    protected double attackRange() {
        return getBbWidth() * 0.5 + 3.0;
    }

    @Override
    protected int attackWindup() {
        return 10;
    }

    @Override
    protected int attackRecover() {
        return 6;
    }

    @Override
    protected int attackInterval() {
        return 16;
    }

    @Override
    protected int comboLength() {
        return 2;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"WHO STIRS THE DEEP?", "Little drowning things. You called the sea."};
            case ULT -> new String[]{"THE DEEP RISES!", "DROWN IN THE DARK!"};
            case BOSS -> new String[]{"A whale among minnows. Good.", "I will coil around its heart."};
            case KILL -> new String[]{"Swallowed.", "The tide takes."};
            case LOW_HP -> new String[]{"My scales were old before your world.", "No hook can hold me!"};
            case LEVEL_UP -> new String[]{"The sea grows hungrier."};
            case IDLE -> new String[]{"*a long, low rumble from deep in his coils*", "Your sky is so... dry."};
            default -> new String[0];
        };
    }

    @Override
    protected boolean hit(LivingEntity e, float amount, Element element) {
        if (e.hasEffect(ModEffects.SOAKED.get())) amount *= 1.4f;
        return super.hit(e, amount, element);
    }

    private void drown(LivingEntity e, float amount) {
        if (hitKills(e, amount, Element.INFERNAL)) feed(0.015f);
        SiegeDamage.apply(e, ModEffects.SOAKED.get(), 100, 0);
    }

    private Vec3 mouth() {
        return position().add(0, getBbHeight() * 0.85, 0).add(look().scale(1.6));
    }

    // ------------------------------------------------------------------ basic attack

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        if (combo == 0) {
            Vec3 at = t.position().add(0, t.getBbHeight() * 0.6, 0);
            Vfx.darkFlash(level, at, DEEP, ABYSS, 1.6f, 6);
            Vfx.spray(level, FxStyle.BUBBLE, at, FOAM, ABYSS, 0.2f, 12, 0.3f, 0.3f);
            drown(t, dmg(1.6f));
            if (!big(t)) SiegeDamage.push(t, position(), 0.6, 0.2);
            playSound(SoundEvents.EVOKER_FANGS_ATTACK, 1.6f, 0.5f);
        } else {
            Vfx.darkSlash(level, position().add(0, 0.8, 0), getYRot(), 2, 300, 4.6f, DEEP, ABYSS, 10, 0.5f);
            Vfx.spray(level, FxStyle.BUBBLE, position().add(0, 0.5, 0), FOAM, ABYSS, 0.25f, 16, 0.4f, 0.1f);
            for (LivingEntity e : enemiesNear(level, 5)) {
                drown(e, dmg(1.1f));
                if (!big(e)) SiegeDamage.push(e, position(), 1.3, 0.4);
            }
            playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.6f, 0.4f);
        }
    }

    // ------------------------------------------------------------------ skills

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (ready(BOIL) && crowd(level, position(), 6) >= 4) return BOIL;
        if (t == null) return 0;
        if (ready(COILS)) {
            LivingEntity best = null;
            for (LivingEntity e : enemiesNear(level, 6)) {
                if (best == null || (big(e) && !big(best)) || e.getMaxHealth() > best.getMaxHealth()) best = e;
            }
            if (best != null && (big(best) || best.getMaxHealth() > 120)) {
                coiled = best;
                return COILS;
            }
        }
        if (ready(BREATH) && dist > 4 && dist < 15) return BREATH;
        if (ready(TIDE) && dist < 10 && crowd(level, t.position(), 4) >= 3) return TIDE;
        if (ready(MAELSTROM) && bestCluster(level, 16, 4.5, 4) != null) return MAELSTROM;
        if (ready(DIVE) && dist > 8 && dist < 20) {
            Vec3 c = bestCluster(level, 20, 4, 3);
            diveTo = c != null ? c : t.position();
            return DIVE;
        }
        return 0;
    }

    @Override
    protected boolean wantsUltimate(ServerLevel level, @Nullable LivingEntity t, double dist) {
        return t != null && (crowd(level, position(), 14) >= 8 || (big(t) && dist < 12));
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case TIDE -> {
                if (target != null) faceTowards(target.position());
                if (t == 12) {
                    Vec3 dir = look();
                    Vec3 from = position().add(dir.scale(1.5));
                    Vfx.darkWave(level, from, from.add(dir.scale(13)), DEEP, ABYSS, 4.5f, 3.2f, 20);
                    Vfx.wave(level, from, from.add(dir.scale(13)), ABYSS, 4f, 2.6f, 16);
                    Vfx.jet(level, FxStyle.BUBBLE, from, from.add(dir.scale(12)), FOAM, ABYSS, 0.3f, 40, 30);
                    playSound(SoundEvents.GENERIC_SPLASH, 2.5f, 0.5f);
                    playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.2f, 0.6f);
                    for (LivingEntity e : enemiesNear(level, 14)) {
                        Vec3 rel = e.position().subtract(position()).multiply(1, 0, 1);
                        double along = rel.dot(dir);
                        if (along < 0 || along > 14 || rel.subtract(dir.scale(along)).lengthSqr() > 3 * 3) continue;
                        drown(e, dmg(2.6f));
                        if (!big(e)) {
                            e.setDeltaMovement(dir.x * 1.6, 0.4, dir.z * 1.6);
                            e.hurtMarked = true;
                        }
                    }
                }
                if (t >= 22) {
                    cooldown(TIDE, 200);
                    return true;
                }
            }
            case MAELSTROM -> {
                Vec3 c = bestCluster(level, 16, 4.5, 1);
                if (t == 10 && c != null) {
                    diveTo = c;
                    HazardZone.spawn(level, HazardZone.Kind.WHIRLPOOL, c, 5.5f, 100, dmg(0.2f), this, false);
                    Vfx.darkVortex(level, c, DEEP, ABYSS, 5.5f, 2.4f, 100, null);
                    playSound(SoundEvents.CONDUIT_ACTIVATE, 2f, 0.5f);
                }
                if (t > 10 && t < 100 && t % 10 == 0) {
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, diveTo, 6)) {
                        drown(e, dmg(0.6f));
                        if (!big(e)) {
                            Vec3 in = diveTo.subtract(e.position()).multiply(1, 0, 1);
                            Vec3 swirl = new Vec3(-in.z, 0, in.x).normalize().scale(0.25);
                            if (in.lengthSqr() > 0.3) e.setDeltaMovement(in.normalize().scale(0.3).add(swirl));
                            e.hurtMarked = true;
                        }
                    }
                }
                if (t >= 20 && t < 100) return false;
                if (t >= 100) {
                    cooldown(MAELSTROM, 280);
                    return true;
                }
            }
            case COILS -> {
                LivingEntity e = coiled;
                if (e == null || !e.isAlive() || e.distanceToSqr(this) > 12 * 12) {
                    coiled = null;
                    cooldown(COILS, 120);
                    return true;
                }
                faceNow(e.position());
                if (t >= 6 && t < 66) {
                    Vec3 hold = position().add(look().scale(2.3)).add(0, 0.8, 0);
                    e.teleportTo(hold.x, hold.y, hold.z);
                    e.setDeltaMovement(Vec3.ZERO);
                    e.hurtMarked = true;
                    SiegeDamage.apply(e, ModEffects.SHOCKED.get(), 10, 0);
                    if (t % 10 == 0) {
                        drown(e, dmg(1.3f) * (big(e) ? 1.3f : 1f));
                        Vfx.darkRing(level, e.position().add(0, e.getBbHeight() * 0.5, 0), DEEP, 1.4f + e.getBbWidth(), 8, 0.8f);
                        playSound(SoundEvents.IRON_GOLEM_DAMAGE, 1.4f, 0.5f);
                    }
                }
                if (t == 66) {
                    drown(e, dmg(2.2f));
                    if (!big(e)) {
                        Vec3 fling = look().scale(2.4);
                        e.setDeltaMovement(fling.x, 0.9, fling.z);
                        e.hurtMarked = true;
                    }
                    playSound(SoundEvents.ENDER_DRAGON_FLAP, 2f, 0.4f);
                }
                if (t >= 74) {
                    coiled = null;
                    cooldown(COILS, 300);
                    return true;
                }
            }
            case BREATH -> {
                if (target != null) faceTowards(target.position());
                if (t >= 10 && t <= 40 && t % 3 == 0) {
                    Vec3 from = mouth();
                    Vec3 dir = look();
                    Vec3 to = from.add(dir.scale(16)).add(0, -getBbHeight() * 0.5, 0);
                    Vfx.darkBeam(level, from, to, DEEP, ABYSS, 1.3f, 5, 0.15f);
                    Vfx.beam(level, from, to, ABYSS, FOAM, 0.45f, 4, 0.1f);
                    Vfx.spray(level, FxStyle.BUBBLE, to, FOAM, ABYSS, 0.25f, 8, 0.35f, 0.4f);
                    line(level, position(), dir, 16, 1.6, dmg(0.55f), Element.INFERNAL);
                    for (LivingEntity e : enemiesNear(level, 16)) {
                        Vec3 rel = e.position().subtract(position()).multiply(1, 0, 1);
                        double along = rel.dot(dir);
                        if (along > 0 && along < 16 && rel.subtract(dir.scale(along)).lengthSqr() < 2.6) {
                            SiegeDamage.apply(e, ModEffects.SOAKED.get(), 100, 0);
                            if (!big(e)) {
                                e.setDeltaMovement(e.getDeltaMovement().add(dir.scale(0.3)));
                                e.hurtMarked = true;
                            }
                        }
                    }
                    if (t % 9 == 0) playSound(SoundEvents.GUARDIAN_ATTACK, 2f, 0.4f);
                }
                if (t >= 44) {
                    cooldown(BREATH, 220);
                    return true;
                }
            }
            case BOIL -> {
                if (t == 10 || t == 18 || t == 26) {
                    float r = 3 + (t - 10) / 8f * 1.6f;
                    Vfx.ring(level, position().add(0, 0.1, 0), ABYSS, r + 1, 12, 0.5f);
                    for (int i = 0; i < 8; i++) {
                        double a = i * Math.PI / 4 + t;
                        Vec3 at = position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                        Vfx.jet(level, FxStyle.SMOKE, at, at.add(0, 4, 0), STEAM, 0xA0B0B0, 0.9f, 10, 20);
                        Vfx.spray(level, FxStyle.BUBBLE, at.add(0, 0.3, 0), FOAM, ABYSS, 0.2f, 6, 0.3f, 1f);
                    }
                    for (LivingEntity e : enemiesNear(level, r + 1.5)) {
                        drown(e, dmg(1.6f));
                        SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 100, 2);
                    }
                    playSound(SoundEvents.FIRE_EXTINGUISH, 2f, 0.4f);
                    playSound(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 2f, 0.6f);
                }
                if (t >= 34) {
                    cooldown(BOIL, 260);
                    return true;
                }
            }
            case DIVE -> {
                if (t <= 10 && t % 2 == 0) Vfx.spray(level, FxStyle.DEBRIS, position().add(0, 0.2, 0), 0x3A3028, 0x1A140E, 0.3f, 10, 0.3f, 1f);
                if (t == 10) {
                    setFlag(true);
                    setInvisible(true);
                    Vfx.darkVortex(level, position(), DEEP, ABYSS, 2.4f, 1.6f, 14, null);
                    playSound(SoundEvents.GENERIC_SPLASH, 2f, 0.4f);
                }
                if (t > 10 && t < 24) {
                    Vec3 step = diveTo.subtract(position()).multiply(1, 0, 1);
                    if (step.lengthSqr() > 0.5) {
                        Vec3 to = position().add(step.normalize().scale(Math.min(1.4, step.length())));
                        teleportTo(to.x, position().y, to.z);
                    }
                    Vfx.spray(level, FxStyle.DEBRIS, position().add(0, 0.1, 0), 0x3A3028, 0x1A140E, 0.25f, 6, 0.2f, 1f);
                }
                if (t == 24) {
                    setFlag(false);
                    setInvisible(false);
                    Vfx.darkPillar(level, position(), DEEP, ABYSS, 2.6f, 6, 24);
                    Vfx.spray(level, FxStyle.BUBBLE, position().add(0, 1, 0), FOAM, ABYSS, 0.35f, 40, 0.6f, 1f);
                    Vfx.spray(level, FxStyle.DEBRIS, position().add(0, 0.3, 0), 0x3A3028, 0x1A140E, 0.35f, 24, 0.6f, 1f);
                    Vfx.shake(level, position(), 0.8f, 24, 12);
                    playSound(SoundEvents.ELDER_GUARDIAN_AMBIENT, 2.5f, 0.6f);
                    for (LivingEntity e : enemiesNear(level, 4.8)) {
                        drown(e, dmg(3.2f));
                        if (!big(e)) {
                            e.setDeltaMovement(e.getDeltaMovement().add(0, 1.0, 0));
                            e.hurtMarked = true;
                        }
                    }
                }
                if (t >= 34) {
                    cooldown(DIVE, 240);
                    return true;
                }
            }
            case ULT -> {
                if (t == 1) {
                    Vfx.screenFlash(level, position(), 0x02303A, 0.45f, 48, 30);
                    playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 2.5f, 0.4f);
                    playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.5f, 0.6f);
                }
                if (t >= 10 && t < 150) {
                    if (t % 40 == 10) Vfx.darkDome(level, position(), DEEP, ABYSS, 14, 44, this);
                    if (t % 5 == 0) {
                        Vec3 at = position().add(random.nextGaussian() * 7, random.nextDouble() * 4, random.nextGaussian() * 7);
                        Vfx.spray(level, FxStyle.BUBBLE, at, FOAM, ABYSS, 0.3f, 10, 0.2f, 1f);
                    }
                    if (t % 10 == 0) {
                        for (LivingEntity e : enemiesNear(level, 14)) {
                            drown(e, dmg(0.85f));
                            if (!big(e)) e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 14, 1, false, false));
                        }
                        playSound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 2f, 0.5f);
                    }
                }
                if (t == 150) {
                    Vfx.darkSphere(level, position().add(0, 1, 0), DEEP, ABYSS, 14, 22);
                    Vfx.ring(level, position().add(0, 0.1, 0), FOAM, 14, 22, 1.4f);
                    Vfx.shake(level, position(), 1.6f, 48, 30);
                    playSound(SoundEvents.GENERIC_SPLASH, 3f, 0.3f);
                    playSound(SoundEvents.GENERIC_EXPLODE, 2f, 0.5f);
                    for (LivingEntity e : enemiesNear(level, 14)) {
                        e.removeEffect(MobEffects.LEVITATION);
                        drown(e, dmg(5f));
                        if (!big(e)) {
                            e.setDeltaMovement(0, -1.6, 0);
                            e.hurtMarked = true;
                        }
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
        if (tickCount % 30 == 0 && inCombat()) {
            Vfx.spray(level, FxStyle.BUBBLE, position().add(0, 0.6, 0), FOAM, ABYSS, 0.2f, 6, 0.15f, 1f);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (submerged()) return false;
        return super.hurt(source, amount);
    }

    @Override
    protected void arrival(ServerLevel level) {
        Vfx.darkPillar(level, position(), DEEP, ABYSS, 3f, 8, 40);
        Vfx.spray(level, FxStyle.BUBBLE, position().add(0, 1, 0), FOAM, ABYSS, 0.4f, 60, 0.5f, 1f);
        playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 2f, 0.5f);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.ELDER_GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ELDER_GUARDIAN_DEATH;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ELDER_GUARDIAN_AMBIENT;
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setInvisible(false);
    }
}
