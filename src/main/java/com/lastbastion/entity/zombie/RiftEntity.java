package com.lastbastion.entity.zombie;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.siege.SiegeData;
import com.lastbastion.siege.SiegeManager;
import com.lastbastion.siege.SiegeRules;
import com.lastbastion.siege.WavePlanner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Corruption Rift (war event): a tear in the world, ringed by floating obsidian shards. It spits zombies out
 * every few seconds until destroyed, blasts defenders who stand too close, and counts as a living enemy, so the
 * wave cannot end while it stands. Holy and arcane damage tear it apart faster.
 */
public class RiftEntity extends Monster implements SiegeEnemy {
    private static final EntityDataAccessor<Integer> PULSE = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    private int wave = 1, spawned, budget = 8, interval = 90, timer = 50, shockCd = 140;
    private float hpScale = 1, dmgScale = 1;

    public RiftEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 160).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 6).add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.FOLLOW_RANGE, 0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(PULSE, -100);
        entityData.define(CHARGE, 0);
    }

    /** Scale to the wave and party, using the world's rules. */
    public void setup(int wave, int players, SiegeRules r) {
        this.wave = Math.max(1, wave);
        hpScale = WavePlanner.hpScale(r, this.wave);
        dmgScale = WavePlanner.damageScale(r, this.wave);
        float hp = (140 + 10 * this.wave) * (1 + 0.35f * (players - 1)) * r.f(SiegeRules.ENEMY_HEALTH);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        setHealth(hp);
        budget = Math.round(Math.min(24, 6 + this.wave / 3f) * (1 + 0.25f * (players - 1)));
        interval = Math.max(40, 95 - this.wave);
    }

    /** Client: ticks since the last spawn burst (drives the model's flare). */
    public float sinceBurst(float pt) {
        return tickCount + pt - entityData.get(PULSE);
    }

    /** Client: 0..1 while the defensive shockwave is charging. */
    public float charge() {
        return entityData.get(CHARGE) / 20f;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        if (level().isClientSide) {
            if (random.nextInt(2) == 0) {
                double a = random.nextDouble() * Math.PI * 2, r = 0.6 + random.nextDouble() * 0.8;
                level().addParticle(ParticleTypes.REVERSE_PORTAL, getX() + Math.cos(a) * r, getY() + 0.4 + random.nextDouble() * 2.6,
                        getZ() + Math.sin(a) * r, 0, 0.02, 0);
            }
            if (random.nextInt(4) == 0) level().addParticle(ParticleTypes.SMOKE, getX() + random.nextGaussian() * 0.6, getY() + 0.1, getZ() + random.nextGaussian() * 0.6, 0, 0.03, 0);
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        if (!isAlive()) return;
        if (--timer <= 0) {
            timer = interval;
            SiegeData d = SiegeManager.data(sl);
            if (spawned < budget && d.tracked.size() < d.rules.i(SiegeRules.MAX_ALIVE)) burst(sl, d);
        }
        // anyone loitering at the rift gets blasted back
        if (shockCd > 0) shockCd--;
        int charge = entityData.get(CHARGE);
        if (charge > 0) {
            entityData.set(CHARGE, charge + 1);
            if (charge + 1 >= 20) {
                entityData.set(CHARGE, 0);
                shockwave(sl);
            }
        } else if (shockCd <= 0 && tickCount % 10 == 0 && !SiegeDamage.defendersAround(sl, position(), 4.5).isEmpty()) {
            entityData.set(CHARGE, 1);
            Vfx.teleCircle(sl, position().add(0, 0.05, 0), 5, 20, Vfx.DANGER_DARK, true);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.4f, 0.6f);
        }
        if (tickCount % 30 == 0) Vfx.vortex(sl, position(), 0x8A2AE0, 0x2A0A40, 1.4f, 3.2f, 30, this);
        if (tickCount % 80 == 0) sl.playSound(null, getX(), getY(), getZ(), SoundEvents.PORTAL_AMBIENT, SoundSource.HOSTILE, 0.8f, 0.6f);
    }

    private void burst(ServerLevel sl, SiegeData d) {
        int n = Math.min(budget - spawned, 1 + (wave >= 12 ? 1 : 0) + (wave >= 24 ? 1 : 0));
        List<ZombieKind> pool = new ArrayList<>();
        for (ZombieKind k : ZombieKind.values()) {
            if (k.unlockWave > wave || !d.rules.zombieEnabled(k)) continue;
            if (k == ZombieKind.ABOMINATION || k == ZombieKind.NECROMANCER || k == ZombieKind.SHRIEKER || k == ZombieKind.BURROWER) continue;
            pool.add(k);
        }
        if (pool.isEmpty()) pool.add(ZombieKind.SHAMBLER);
        Vec3 core = d.corePos != null ? Vec3.atCenterOf(d.corePos) : position().add(getLookAngle());
        Vec3 toCore = core.subtract(position()).multiply(1, 0, 1);
        Vec3 out = toCore.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : toCore.normalize();
        for (int i = 0; i < n; i++) {
            ZombieKind k = pool.get(random.nextInt(pool.size()));
            int rank = Mth.clamp((int) Math.round((wave - 1) / 7.0 + random.nextGaussian() * 0.6), 0, 4);
            Vec3 side = new Vec3(-out.z, 0, out.x).scale((i - (n - 1) / 2.0) * 1.2);
            Vec3 at = position().add(out.scale(1.3)).add(side);
            SiegeZombie z = ZombieSpawner.spawn(sl, k, ZombieRank.byId(rank), 0, at, true, false, hpScale, dmgScale);
            if (z != null) {
                z.setDeltaMovement(out.x * 0.4, 0.25, out.z * 0.4);
                spawned++;
            }
        }
        entityData.set(PULSE, tickCount);
        Vfx.spray(sl, FxStyle.SOUL, position().add(0, 1.6, 0), 0xD08AFF, 0x5A1A8A, 0.35f, 18, 0.35f, 0.2f);
        Vfx.flash(sl, position().add(0, 1.6, 0), 0xB050FF, 2f, 6);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.2f, 0.6f);
    }

    private void shockwave(ServerLevel sl) {
        shockCd = 160;
        Vfx.ring(sl, position().add(0, 0.1, 0), 0xB050FF, 5, 14, 0.8f);
        Vfx.sphere(sl, position().add(0, 1.4, 0), 0x8A2AE0, 3.5f, 10);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1f, 1.4f);
        for (LivingEntity e : SiegeDamage.defendersAround(sl, position(), 5)) {
            SiegeDamage.deal(e, this, this, 6f * dmgScale, Element.SHADOW, false);
            SiegeDamage.push(e, position(), 1.3, 0.5);
            e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 60, 0, false, false));
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            Vec3 c = position().add(0, 1.5, 0);
            Vfx.sphere(sl, c, 0xB050FF, 4f, 14);
            Vfx.flash(sl, c, 0xFFFFFF, 4f, 10);
            Vfx.ring(sl, position().add(0, 0.1, 0), 0xB050FF, 7, 20, 1);
            Vfx.spray(sl, FxStyle.SHARD, c, 0x2A1A3A, 0x0A0510, 0.3f, 30, 0.6f, 0.4f);
            Vfx.shake(sl, c, 0.5f, 24, 12);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.HOSTILE, 2f, 0.5f);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.4f, 0.7f);
            Entity killer = source.getEntity();
            SiegeManager.broadcast(sl, SiegeManager.data(sl), Component.literal("✦ The Corruption Rift collapses"
                    + (killer != null ? " (" + killer.getDisplayName().getString() + ")" : "") + ".").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    @Override
    protected void tickDeath() {
        // collapse instantly into the implosion instead of the vanilla tip-over
        if (!level().isClientSide && !isRemoved()) remove(RemovalReason.KILLED);
    }

    // ------------------------------------------------------------------ SiegeEnemy

    @Override
    public float elementMultiplier(Element e) {
        return switch (e) {
            case HOLY -> 1.6f;
            case ARCANE -> 1.3f;
            case POISON -> 0f;
            case SHADOW -> 0.25f;
            case ICE -> 0.6f;
            default -> 1f;
        };
    }

    @Override
    public int creditValue() {
        return 40 + 6 * wave;
    }

    @Override
    public boolean isWaveMob() {
        return true;
    }

    @Override
    public boolean immuneToStatus() {
        return true;
    }

    @Override
    public boolean isHighPriority() {
        return true;
    }

    // ------------------------------------------------------------------ physics & misc

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity e) {
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
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
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GLASS_BREAK;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("lbWave", wave);
        tag.putInt("lbSpawned", spawned);
        tag.putInt("lbBudget", budget);
        tag.putInt("lbInterval", interval);
        tag.putFloat("lbHp", hpScale);
        tag.putFloat("lbDmg", dmgScale);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        wave = Math.max(1, tag.getInt("lbWave"));
        spawned = tag.getInt("lbSpawned");
        budget = tag.contains("lbBudget") ? tag.getInt("lbBudget") : 8;
        interval = tag.contains("lbInterval") ? tag.getInt("lbInterval") : 90;
        hpScale = tag.contains("lbHp") ? tag.getFloat("lbHp") : 1f;
        dmgScale = tag.contains("lbDmg") ? tag.getFloat("lbDmg") : 1f;
    }
}
