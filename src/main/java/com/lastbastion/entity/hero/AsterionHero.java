package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.zombie.RunnerZombie;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Asterion, the Minotaur King: a berserker who wants to be in the middle of the horde. Labrys
 * cleaves (diagonal, sweeping, then a horn gore); Bull Rush tramples a path; War Stomp stuns; Earthsplitter
 * cracks the ground in a line; Bloodrage turns his wounds into strength. Every hit heals him a little.
 * Ultimate, King of the Labyrinth: he roars, grows to full size and becomes a whirlwind of bronze.
 */
public class AsterionHero extends HeroEntity {
    public static final int RUSH = 6, STOMP = 7, SPLIT = 8, BLOODRAGE = 9;
    private static final EntityDataAccessor<Boolean> GIANT = SynchedEntityData.defineId(AsterionHero.class, EntityDataSerializers.BOOLEAN);
    private Vec3 rushDir = Vec3.ZERO;
    private final Set<Integer> trampled = new HashSet<>();
    private int rage;
    private boolean big;

    public AsterionHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.ASTERION);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(GIANT, false);
    }

    /** True during the ultimate (the model grows). */
    public boolean giant() {
        return entityData.get(GIANT);
    }

    public boolean raging() {
        return flag();
    }

    @Override
    public Role role() {
        return Role.BRUISER;
    }

    @Override
    protected int attackWindup() {
        return 9;
    }

    @Override
    protected int attackRecover() {
        return 7;
    }

    @Override
    protected int attackInterval() {
        return raging() ? 13 : 18;
    }

    @Override
    protected int comboLength() {
        return 3;
    }

    @Override
    protected double attackRange() {
        return getBbWidth() * 0.5 + 2.7;
    }

    @Override
    protected float retreatThreshold() {
        return 0.12f;
    }

    @Override
    protected float ultGainPerDamage() {
        return 0.045f;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"BOW BEFORE THE KING OF THE LABYRINTH!", "Who dares summon Asterion? ...Good. I was bored."};
            case ULT -> new String[]{"THE LABYRINTH HUNGERS!", "I AM THE MAZE AND THE BEAST WITHIN IT!"};
            case BOSS -> new String[]{"Finally, something worth goring!", "Ha! Come, big one!"};
            case LOW_HP -> new String[]{"BLOOD! MORE BLOOD!", "Pain only makes me STRONGER!"};
            case KILL -> new String[]{"Next!", "Your bones will pave my halls."};
            case LEVEL_UP -> new String[]{"HA! Stronger still!"};
            case IDLE -> new String[]{"*snorts steam and paws the ground*", "When do the dead come? I hunger."};
            default -> new String[0];
        };
    }

    private void lifesteal(float dealt) {
        heal(dealt * (raging() ? 0.25f : 0.08f));
    }

    @Override
    protected boolean hit(LivingEntity e, float amount, Element element) {
        float a = amount * (raging() ? 1.2f : 1f);
        boolean ok = super.hit(e, a, element);
        if (ok) lifesteal(a);
        return ok;
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        Vec3 c = position().add(0, getBbHeight() * 0.5, 0);
        switch (combo) {
            case 0 -> {
                arc(level, 3.4, 150, dmg(1.1f), Element.PHYSICAL);
                Vfx.slash(level, c, getYRot(), 60, 170, 3.2f, 0xD0A060, 0xFFFFFF, 9, 0.3f);
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.6f);
            }
            case 1 -> {
                arc(level, 3.6, 220, dmg(1f), Element.PHYSICAL);
                Vfx.slash(level, c.add(0, -0.3, 0), getYRot(), 5, -230, 3.4f, 0xD0A060, 0xFFFFFF, 9, 0.3f);
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.5f);
            }
            default -> {
                if (hit(t, dmg(1.4f), Element.PHYSICAL)) {
                    if (!(t instanceof SiegeEnemy se && se.isHighPriority())) SiegeDamage.push(t, position(), 1.5, 0.5);
                    SiegeDamage.applyStacking(t, ModEffects.BLEEDING.get(), 80, 2);
                    Vfx.spray(level, FxStyle.DROP, t.position().add(0, t.getBbHeight() * 0.6, 0), 0xA01010, 0x5A0808, 0.14f, 10, 0.35f, 0.5f);
                }
                Vfx.spray(level, FxStyle.SMOKE, position().add(0, getBbHeight() * 0.85, 0).add(look().scale(1)), 0xE8E0D8, 0xB8B0A8, 0.4f, 4, 0.12f, 0);
                playSound(SoundEvents.RAVAGER_ATTACK, 1.4f, 0.8f);
            }
        }
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (ready(BLOODRAGE) && getHealth() < getMaxHealth() * 0.5f && target != null) return BLOODRAGE;
        if (ready(STOMP) && crowd(level, position(), 4) >= 3) return STOMP;
        if (t == null) return 0;
        if (ready(RUSH) && dist > 5 && dist < 14) return RUSH;
        if (ready(SPLIT) && dist < 9) {
            Vec3 n = t.position().subtract(position()).multiply(1, 0, 1);
            if (n.lengthSqr() > 0.01) {
                n = n.normalize();
                int inLane = 0;
                for (LivingEntity e : enemiesNear(level, 11)) {
                    Vec3 rel = e.position().subtract(position()).multiply(1, 0, 1);
                    double along = rel.dot(n);
                    if (along > 0 && along < 11 && rel.subtract(n.scale(along)).lengthSqr() < 3) inLane++;
                }
                if (inLane >= 3) {
                    rushDir = n;
                    return SPLIT;
                }
            }
        }
        return 0;
    }

    @Override
    protected boolean wantsUltimate(ServerLevel level, @Nullable LivingEntity t, double dist) {
        return t != null && (crowd(level, position(), 7) >= 4 || (t instanceof SiegeEnemy se && se.isHighPriority() && dist < 8));
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case RUSH -> {
                if (t == 1 && target != null) {
                    rushDir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
                    faceNow(target.position());
                    trampled.clear();
                    playSound(SoundEvents.RAVAGER_ROAR, 1.6f, 0.9f);
                    Vfx.spray(level, FxStyle.SMOKE, position().add(0, 0.3, 0), 0xC8B8A0, 0x8A7A60, 0.9f, 8, 0.15f, -1);
                }
                if (t >= 10 && t < 26) {
                    setDeltaMovement(rushDir.x * 0.95, getDeltaMovement().y, rushDir.z * 0.95);
                    if (t % 2 == 0) Vfx.spray(level, FxStyle.DEBRIS, position().add(0, 0.2, 0), 0x6A5A48, 0x3A3028, 0.18f, 3, 0.2f, 0.8f);
                    for (LivingEntity e : enemiesNear(level, 2.3)) {
                        if (!trampled.add(e.getId())) continue;
                        hit(e, dmg(1.8f), Element.PHYSICAL);
                        if (!(e instanceof SiegeEnemy se && se.isHighPriority())) {
                            Vec3 side = new Vec3(-rushDir.z, 0, rushDir.x);
                            if (side.dot(e.position().subtract(position())) < 0) side = side.scale(-1);
                            e.setDeltaMovement(side.x * 1.1 + rushDir.x * 0.4, 0.55, side.z * 1.1 + rushDir.z * 0.4);
                            e.hurtMarked = true;
                        }
                        playSound(SoundEvents.RAVAGER_ATTACK, 1.2f, 0.9f);
                    }
                    if (horizontalCollision && t > 12) skillTick = 26;
                }
                if (t == 26) Vfx.shake(level, position(), 0.4f, 12, 6);
                if (t >= 30) {
                    cooldown(RUSH, 170);
                    return true;
                }
            }
            case STOMP -> {
                if (t == 14) {
                    Fx.shockwave(level, position(), 4.5, 0xC8A070);
                    playSound(SoundEvents.ANVIL_LAND, 1.5f, 0.5f);
                    for (LivingEntity e : enemiesNear(level, 4.5)) {
                        hit(e, dmg(1.5f), Element.PHYSICAL);
                        SiegeDamage.apply(e, ModEffects.SHOCKED.get(), 30, 0);
                        if (e instanceof RunnerZombie) e.addEffect(new MobEffectInstance(ModEffects.FEARED.get(), 60, 0));
                    }
                }
                if (t >= 24) {
                    cooldown(STOMP, 200);
                    return true;
                }
            }
            case SPLIT -> {
                faceTowards(position().add(rushDir));
                if (t == 4) Vfx.teleLine(level, position(), position().add(rushDir.scale(11)), 2.4f, 14, 0xD07030, false);
                if (t == 18) {
                    Vfx.shake(level, position(), 0.9f, 18, 10);
                    playSound(SoundEvents.GENERIC_EXPLODE, 1.4f, 0.6f);
                    playSound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 0.5f);
                }
                if (t >= 18 && t < 34) {
                    double d = (t - 18) * 0.7 + 1.5;
                    Vec3 at = position().add(rushDir.scale(d));
                    Vfx.spray(level, FxStyle.DEBRIS, at.add(0, 0.2, 0), 0x6A5A48, 0x3A3028, 0.28f, 6, 0.35f, 1f);
                    Vfx.spray(level, FxStyle.EMBER, at.add(0, 0.2, 0), 0xFF8A20, 0xC03010, 0.14f, 3, 0.2f, 1f);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, at, 1.5)) {
                        if (!trampled.add(e.getId() * 31 + 7)) continue;
                        hit(e, dmg(3f), Element.PHYSICAL);
                        if (!(e instanceof SiegeEnemy se && se.isHighPriority())) e.setDeltaMovement(e.getDeltaMovement().add(0, 0.7, 0));
                        e.hurtMarked = true;
                    }
                }
                if (t == 1) trampled.clear();
                if (t >= 38) {
                    cooldown(SPLIT, 240);
                    return true;
                }
            }
            case BLOODRAGE -> {
                if (t == 8) {
                    setFlag(true);
                    rage = 200;
                    Vfx.aura(level, this, 0xC01010, 1.8f, 200);
                    Vfx.spray(level, FxStyle.DROP, position().add(0, getBbHeight() * 0.7, 0), 0xC01010, 0x5A0808, 0.15f, 20, 0.3f, 0.6f);
                    playSound(SoundEvents.RAVAGER_ROAR, 2f, 0.6f);
                }
                if (t >= 16) {
                    cooldown(BLOODRAGE, 600);
                    return true;
                }
            }
            case ULT -> {
                if (t == 1) {
                    playSound(SoundEvents.RAVAGER_ROAR, 3f, 0.5f);
                    Vfx.ring(level, position().add(0, 0.05, 0), 0xD07030, 10, 18, 1.5f);
                    Vfx.shake(level, position(), 0.8f, 24, 12);
                    for (LivingEntity e : enemiesNear(level, 10)) {
                        if (!(e instanceof SiegeEnemy se && se.isHighPriority())) e.addEffect(new MobEffectInstance(ModEffects.FEARED.get(), 40, 0));
                    }
                }
                if (t == 14) {
                    entityData.set(GIANT, true);
                    big = true;
                    refreshDimensions();
                    addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0));
                }
                if (t > 14 && t < 170) {
                    LivingEntity near = null;
                    double best = 1e9;
                    for (LivingEntity e : enemiesNear(level, 12)) {
                        double d = distanceToSqr(e);
                        if (d < best) {
                            best = d;
                            near = e;
                        }
                    }
                    if (near != null && best > 4) getNavigation().moveTo(near, 1.1);
                    if (t % 6 == 0) {
                        Vfx.slash(level, position().add(0, getBbHeight() * 0.5, 0), getYRot() + t * 40, 3, 360, 3.6f, 0xD0A060, 0xFFFFFF, 6, 0.25f);
                        for (LivingEntity e : enemiesNear(level, 3.8)) {
                            hit(e, dmg(1.4f), Element.PHYSICAL);
                            if (!(e instanceof SiegeEnemy se && se.isHighPriority())) SiegeDamage.push(e, position(), 0.5, 0.2);
                        }
                        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.5f + random.nextFloat() * 0.2f);
                    }
                }
                if (t >= 170) {
                    entityData.set(GIANT, false);
                    big = false;
                    refreshDimensions();
                    getNavigation().stop();
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
        if (rage > 0 && --rage == 0) setFlag(false);
        if (giant() && skill != ULT) {
            entityData.set(GIANT, false);
            big = false;
            refreshDimensions();
        }
        if (tickCount % 40 == 0 && inCombat()) {
            Vfx.particles(level, com.lastbastion.fx.FxParticleOptions.of(FxStyle.SMOKE, 0xF0ECE8, 0.25f, 14),
                    position().add(0, getBbHeight() * 0.86, 0).add(look().scale(0.9)), 2, 0.05, 0.02, 0.05, 0.02);
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        EntityDimensions d = super.getDimensions(pose);
        return big ? d.scale(1.2f) : d;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (GIANT.equals(key)) {
            big = giant();
            refreshDimensions();
        }
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Nullable
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return inCombat() ? SoundEvents.RAVAGER_AMBIENT : null;
    }
}
