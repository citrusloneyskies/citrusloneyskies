package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.SiegeAlly;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.AnimTracker;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Lesser demons called up by the Devils for a short while: cackling winged imps that pelt the horde with
 * hellfire, molten-cracked hellhounds that bite and breathe fire, and hulking fiends that crush whole groups.
 * They fight for the devil's summoner (their kills are credited to that player) and burn away when their time
 * is up.
 */
public class HellspawnEntity extends PathfinderMob implements SiegeAlly {
    public enum Variant {
        IMP(60, 9, 0.36, "imp"), HELLHOUND(110, 14, 0.42, "hellhound"), FIEND(260, 26, 0.28, "fiend");
        public final float hp, damage;
        public final double speed;
        public final String id;

        Variant(float hp, float damage, double speed, String id) {
            this.hp = hp;
            this.damage = damage;
            this.speed = speed;
            this.id = id;
        }
    }

    /** Animation ids: 1 claw/bite, 2 ranged (fireball / breath / slam), 3 arrival. */
    public static final int ANIM_ATTACK = 1, ANIM_SPECIAL = 2, ANIM_ARRIVE = 3;
    private static final EntityDataAccessor<Integer> ANIM = SynchedEntityData.defineId(HellspawnEntity.class, EntityDataSerializers.INT);
    /** Remaining life while burning away (synced only over the last {@link #FADE_TICKS} ticks). */
    private static final EntityDataAccessor<Integer> FADE = SynchedEntityData.defineId(HellspawnEntity.class, EntityDataSerializers.INT);
    private static final int FADE_TICKS = 40;

    public final Variant variant;
    public final AnimTracker anim;
    @Nullable
    private UUID owner, master;
    private int life = 500, attackCd, specialCd = 60, repath;
    @Nullable
    private LivingEntity target;

    public HellspawnEntity(EntityType<? extends PathfinderMob> type, Level level, Variant variant) {
        super(type, level);
        this.variant = variant;
        this.anim = new AnimTracker(this, ANIM);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes(Variant v) {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, v.hp).add(Attributes.ATTACK_DAMAGE, v.damage)
                .add(Attributes.MOVEMENT_SPEED, v.speed).add(Attributes.ARMOR, v == Variant.FIEND ? 12 : 4)
                .add(Attributes.KNOCKBACK_RESISTANCE, v == Variant.FIEND ? 0.8 : 0.2).add(Attributes.FOLLOW_RANGE, 32);
    }

    public static EntityType<? extends HellspawnEntity> type(Variant v) {
        return switch (v) {
            case IMP -> ModEntities.IMP.get();
            case HELLHOUND -> ModEntities.HELLHOUND.get();
            case FIEND -> ModEntities.FIEND.get();
        };
    }

    /** Call up a lesser demon next to a devil; it serves the devil's summoner for {@code ticks}. */
    @Nullable
    public static HellspawnEntity summon(ServerLevel level, HeroEntity devil, Variant v, Vec3 at, int ticks) {
        HellspawnEntity h = type(v).create(level);
        if (h == null) return null;
        Vec3 p = Companions.safeSpotNear(level, h, at);
        h.moveTo(p.x, p.y, p.z, devil.getYRot(), 0);
        h.owner = devil.getOwnerId();
        h.master = devil.getUUID();
        h.life = ticks;
        float scale = 1f + 0.12f * (devil.heroLevel() - 1);
        h.getAttribute(Attributes.MAX_HEALTH).setBaseValue(v.hp * scale);
        h.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(v.damage * scale);
        h.setHealth(h.getMaxHealth());
        h.finalizeSpawn(level, level.getCurrentDifficultyAt(h.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        level.addFreshEntity(h);
        h.anim.play(ANIM_ARRIVE);
        Vfx.spray(level, FxStyle.FLAME, p.add(0, 0.5, 0), 0xFF7A1A, 0xD0141A, 0.3f, 16, 0.25f, 0.8f);
        Vfx.spray(level, FxStyle.SMOKE, p.add(0, 0.3, 0), 0x2A1A1A, 0x0A0606, 0.6f, 10, 0.15f, 0.4f);
        return h;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ANIM, 0);
        entityData.define(FADE, FADE_TICKS);
    }

    /** 1 while healthy, falling to 0 as the summoning runs out (client tint). */
    public float fade() {
        return Math.max(0, Math.min(1, entityData.get(FADE) / (float) FADE_TICKS));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (anim != null) anim.onSync(key);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    @Nullable
    public UUID getOwnerId() {
        return owner;
    }

    @Nullable
    private Entity masterEntity(ServerLevel level) {
        return master == null ? null : level.getEntity(master);
    }

    // ------------------------------------------------------------------ brain

    @Override
    public void aiStep() {
        LivingEntity lbPrev = com.lastbastion.combat.SiegeDamage.pushHostile(getTarget());
        try {
        super.aiStep();
        if (!(level() instanceof ServerLevel sl) || !isAlive()) return;
        if (--life <= 0) {
            vanish(sl);
            return;
        }
        if (life < FADE_TICKS) entityData.set(FADE, life);
        if (isNoAi()) return;
        if (attackCd > 0) attackCd--;
        if (specialCd > 0) specialCd--;
        Entity m = masterEntity(sl);
        Vec3 anchor = m != null ? m.position() : position();
        if (tickCount % 10 == 0 || target == null || !target.isAlive()) {
            target = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity e : SiegeDamage.enemiesAround(sl, anchor, 18)) {
                if (e instanceof Player || (e instanceof SiegeEnemy se && se.isUntargetable())) continue;
                if (e instanceof net.minecraft.world.entity.NeutralMob && !(e instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() != null)) continue;
                double d = distanceToSqr(e);
                if (d < best) {
                    best = d;
                    target = e;
                }
            }
            setTarget(target);
        }
        if (target == null) {
            if (m != null && distanceToSqr(m) > 6 * 6 && --repath <= 0) {
                repath = 15;
                getNavigation().moveTo(m, 1.1);
            }
            return;
        }
        double dist = distanceTo(target);
        getLookControl().setLookAt(target, 40, 40);
        if (specialCd <= 0 && special(sl, target, dist)) return;
        double reach = getBbWidth() * 0.5 + target.getBbWidth() * 0.5 + (variant == Variant.FIEND ? 1.8 : 1.1);
        if (dist <= reach) {
            getNavigation().stop();
            if (attackCd <= 0) {
                attackCd = variant == Variant.FIEND ? 26 : variant == Variant.HELLHOUND ? 14 : 18;
                anim.play(ANIM_ATTACK);
                float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
                if (variant == Variant.FIEND) {
                    Vec3 c = position().add(getLookAngle().multiply(1, 0, 1).normalize().scale(1.6));
                    Vfx.ring(sl, c.add(0, 0.1, 0), 0xD0141A, 2.6f, 10, 0.4f);
                    Vfx.spray(sl, FxStyle.DEBRIS, c, 0x3A2A22, 0x1A1210, 0.25f, 10, 0.4f, 0.8f);
                    for (LivingEntity e : SiegeDamage.enemiesAround(sl, c, 2.6)) SiegeDamage.deal(e, this, owner(sl), dmg, Element.INFERNAL, false);
                    playSound(SoundEvents.RAVAGER_ATTACK, 1.2f, 0.6f);
                } else {
                    SiegeDamage.deal(target, this, owner(sl), dmg, Element.INFERNAL, false);
                    target.setSecondsOnFire(3);
                    playSound(variant == Variant.HELLHOUND ? SoundEvents.WOLF_GROWL : SoundEvents.VEX_CHARGE, 1f, variant == Variant.IMP ? 1.4f : 0.6f);
                }
            }
        } else if (--repath <= 0) {
            repath = 8;
            getNavigation().moveTo(target, variant == Variant.HELLHOUND ? 1.4 : 1.15);
        }
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("HellspawnEntity.aiStep", lbCrash);
        } finally {
            com.lastbastion.combat.SiegeDamage.popHostile(lbPrev);
        }
    }

    /** Variant special: imp fireballs, hellhound fire breath, fiend ground slam. Returns true if used. */
    private boolean special(ServerLevel level, LivingEntity t, double dist) {
        float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        switch (variant) {
            case IMP -> {
                if (dist < 3 || dist > 14) return false;
                specialCd = 40;
                anim.play(ANIM_SPECIAL);
                Vec3 hand = position().add(0, getBbHeight() * 0.8, 0);
                SiegeProjectile p = SiegeProjectile.create(level, SiegeProjectile.ProjKind.FIREBALL, this, hand).payload(dmg * 1.4f, Element.INFERNAL);
                p.area(1.6f);
                p.aim(t.position().add(0, t.getBbHeight() * 0.5, 0), 1.4f).scale(0.8f).fire(level);
                playSound(SoundEvents.BLAZE_SHOOT, 1f, 1.5f);
                return true;
            }
            case HELLHOUND -> {
                if (dist > 5) return false;
                specialCd = 100;
                anim.play(ANIM_SPECIAL);
                Vec3 mouth = position().add(0, getBbHeight() * 0.6, 0);
                Vec3 dir = t.position().subtract(position()).normalize();
                Vfx.jet(level, FxStyle.FLAME, mouth, mouth.add(dir.scale(5)), 0xFFB040, 0xD0141A, 0.4f, 30, 28);
                for (LivingEntity e : SiegeDamage.enemiesAround(level, position().add(dir.scale(2.5)), 3)) {
                    SiegeDamage.deal(e, this, owner(level), dmg * 1.2f, Element.INFERNAL, false);
                    e.setSecondsOnFire(5);
                }
                playSound(SoundEvents.BLAZE_SHOOT, 1.2f, 0.5f);
                return true;
            }
            case FIEND -> {
                if (SiegeDamage.enemiesAround(level, position(), 4).size() < 3) return false;
                specialCd = 120;
                anim.play(ANIM_SPECIAL);
                Vfx.darkRing(level, position().add(0, 0.1, 0), 0x1A0204, 5, 16, 0.6f);
                Vfx.ring(level, position().add(0, 0.1, 0), 0xD0141A, 5, 14, 0.5f);
                Vfx.shake(level, position(), 0.4f, 12, 8);
                for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 5)) {
                    SiegeDamage.deal(e, this, owner(level), dmg * 1.6f, Element.INFERNAL, false);
                    if (!(e instanceof SiegeEnemy se && se.isHighPriority())) SiegeDamage.push(e, position(), 1.2, 0.5);
                }
                playSound(SoundEvents.GENERIC_EXPLODE, 1f, 0.7f);
                return true;
            }
        }
        return false;
    }

    @Nullable
    private Entity owner(ServerLevel level) {
        Entity m = masterEntity(level);
        return m != null ? m : this;
    }

    private void vanish(ServerLevel level) {
        Vfx.spray(level, FxStyle.EMBER, position().add(0, getBbHeight() * 0.5, 0), 0xFF7A1A, 0xD0141A, 0.2f, 14, 0.2f, 0.8f);
        Vfx.spray(level, FxStyle.SMOKE, position().add(0, getBbHeight() * 0.5, 0), 0x2A1A1A, 0x0A0606, 0.5f, 8, 0.1f, 0.6f);
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl) vanish(sl);
    }

    // ------------------------------------------------------------------ constitution

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p && !p.isCreative()) return false;
        if (source.getEntity() instanceof SiegeAlly) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect().getCategory() != MobEffectCategory.HARMFUL && super.canBeAffected(effect);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof Player || other instanceof SiegeAlly || super.isAlliedTo(other);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return switch (variant) {
            case IMP -> SoundEvents.VEX_AMBIENT;
            case HELLHOUND -> SoundEvents.WOLF_GROWL;
            case FIEND -> SoundEvents.RAVAGER_AMBIENT;
        };
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return variant == Variant.IMP ? SoundEvents.VEX_HURT : variant == Variant.HELLHOUND ? SoundEvents.WOLF_HURT : SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLAZE_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("lbOwner", owner);
        if (master != null) tag.putUUID("lbMaster", master);
        tag.putInt("lbLife", life);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("lbOwner")) owner = tag.getUUID("lbOwner");
        if (tag.hasUUID("lbMaster")) master = tag.getUUID("lbMaster");
        life = tag.contains("lbLife") ? tag.getInt("lbLife") : 200;
    }
}
