package com.lastbastion.entity.projectile;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Lingering ground effects: fire patches, acid pools, poison clouds, frost fields, whirlpools,
 * tornados, holy ground, storm zones and orbital strike markers. Particles are drawn client-side.
 */
public class HazardZone extends Entity {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(HazardZone.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(HazardZone.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(HazardZone.class, EntityDataSerializers.INT);

    public enum Kind {
        FIRE, ACID, POISON_CLOUD, FROST, WHIRLPOOL, TORNADO, HOLY, STORM, ORBITAL, BONE_SPIKES, PLAGUE_RAIN, SOUL_FIRE, METEOR_RAIN, AIRSTRIKE,
        SANDSTORM, GRAVITY_WELL, CONSECRATION, BLOOD_POOL, SHADOW_POOL
    }

    public int duration = 100;
    public float power = 1f;
    /** If true, harms defenders (enemy hazard); otherwise harms enemies. */
    public boolean hostile;
    @Nullable
    private UUID ownerId;
    @Nullable
    private Entity cachedOwner;
    private int age;
    private Vec3 heading = Vec3.ZERO;

    public HazardZone(EntityType<? extends HazardZone> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static HazardZone spawn(ServerLevel level, Kind kind, Vec3 at, float radius, int duration, float power,
                                   @Nullable Entity owner, boolean hostile) {
        HazardZone z = new HazardZone(ModEntities.HAZARD.get(), level);
        z.setPos(at.x, at.y, at.z);
        z.entityData.set(KIND, kind.ordinal());
        z.entityData.set(RADIUS, radius);
        z.duration = duration;
        z.power = power;
        z.hostile = hostile;
        if (owner != null) {
            z.ownerId = owner.getUUID();
            z.cachedOwner = owner;
        }
        level.addFreshEntity(z);
        return z;
    }

    /** Delayed strike (orbital/airstrike/meteor): fuse ticks before the payload lands. */
    public HazardZone fuse(int ticks) {
        entityData.set(FUSE, ticks);
        return this;
    }

    public HazardZone heading(Vec3 dir) {
        this.heading = dir;
        return this;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(KIND, 0);
        entityData.define(RADIUS, 2f);
        entityData.define(FUSE, 0);
    }

    public Kind kind() {
        int i = entityData.get(KIND);
        return Kind.values()[Math.max(0, Math.min(Kind.values().length - 1, i))];
    }

    public float radius() {
        return entityData.get(RADIUS);
    }

    @Nullable
    private Entity owner() {
        if (cachedOwner != null && cachedOwner.isAlive()) return cachedOwner;
        if (ownerId != null && level() instanceof ServerLevel sl) cachedOwner = sl.getEntity(ownerId);
        return cachedOwner;
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (level().isClientSide) {
            clientFx();
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        int fuse = entityData.get(FUSE);
        if (fuse > 0) {
            entityData.set(FUSE, fuse - 1);
            if (fuse == 1) detonate(sl);
            return;
        }
        if (age > duration) {
            discard();
            return;
        }
        if (heading.lengthSqr() > 0) setPos(position().add(heading));
        if (age % 10 != 0) return;
        float r = radius();
        Vec3 c = position();
        var targets = hostile ? SiegeDamage.defendersAround(sl, c, r) : SiegeDamage.enemiesAround(sl, c, r);
        Entity owner = owner();
        switch (kind()) {
            case FIRE -> targets.forEach(e -> {
                SiegeDamage.deal(e, this, owner, 2f * power, Element.FIRE, false);
                e.setSecondsOnFire(3);
            });
            case SOUL_FIRE -> targets.forEach(e -> {
                SiegeDamage.deal(e, this, owner, 3f * power, Element.FIRE, false);
                SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 60, 2);
            });
            case ACID -> targets.forEach(e -> {
                SiegeDamage.deal(e, this, owner, 1.5f * power, Element.POISON, false);
                SiegeDamage.applyStacking(e, ModEffects.CORRODED.get(), 80, 4);
            });
            case POISON_CLOUD, PLAGUE_RAIN -> targets.forEach(e -> {
                SiegeDamage.deal(e, this, owner, 2f * power, Element.POISON, false);
                e.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0));
            });
            case FROST -> targets.forEach(e -> {
                SiegeDamage.deal(e, this, owner, 1f * power, Element.ICE, false);
                SiegeDamage.applyStacking(e, ModEffects.CHILLED.get(), 40, 4);
            });
            case WHIRLPOOL -> targets.forEach(e -> {
                Vec3 pull = c.subtract(e.position()).normalize().scale(0.35);
                e.setDeltaMovement(e.getDeltaMovement().scale(0.3).add(pull.x, 0.02, pull.z));
                e.hurtMarked = true;
                SiegeDamage.deal(e, this, owner, 1.5f * power, Element.WATER, false);
                SiegeDamage.apply(e, ModEffects.SOAKED.get(), 120, 0);
            });
            case TORNADO -> targets.forEach(e -> {
                Vec3 tangent = new Vec3(-(e.getZ() - c.z), 0, e.getX() - c.x).normalize().scale(0.3);
                e.setDeltaMovement(tangent.x, 0.45, tangent.z);
                e.hurtMarked = true;
                SiegeDamage.deal(e, this, owner, 2f * power, Element.WIND, false);
                SiegeDamage.apply(e, ModEffects.DISORIENTED.get(), 60, 0);
            });
            case HOLY -> {
                targets.forEach(e -> {
                    SiegeDamage.deal(e, this, owner, 2f * power, Element.HOLY, false);
                    SiegeDamage.apply(e, ModEffects.PURIFIED.get(), 60, 0);
                });
                for (LivingEntity d : SiegeDamage.defendersAround(sl, c, r)) d.heal(1f * power);
            }
            case STORM -> {
                if (!targets.isEmpty() && age % 20 == 0) {
                    LivingEntity t = targets.get(sl.random.nextInt(targets.size()));
                    Fx.bolt(sl, t.position().add(0, 18, 0), t.position(), 0xF4F06A, 1.3f);
                    SiegeDamage.deal(t, this, owner, 10f * power, Element.LIGHTNING, false);
                    SiegeDamage.apply(t, ModEffects.SHOCKED.get(), 15, 0);
                    Fx.sound(sl, t.position(), net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT, 1f, 1.1f);
                }
            }
            case BONE_SPIKES -> targets.forEach(e -> {
                SiegeDamage.deal(e, this, owner, 4f * power, Element.PHYSICAL, false);
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.5, 0));
                e.hurtMarked = true;
            });
            case SANDSTORM -> targets.forEach(e -> {
                SiegeDamage.deal(e, this, owner, 1.5f * power, Element.PHYSICAL, false);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                SiegeDamage.apply(e, ModEffects.DISORIENTED.get(), 30, 0);
            });
            case GRAVITY_WELL -> targets.forEach(e -> {
                Vec3 pull = c.subtract(e.position()).multiply(1, 0, 1);
                if (pull.lengthSqr() > 0.2) {
                    pull = pull.normalize().scale(0.45);
                    e.setDeltaMovement(e.getDeltaMovement().scale(0.2).add(pull.x, 0.04, pull.z));
                    e.hurtMarked = true;
                }
                SiegeDamage.deal(e, this, owner, 1f * power, Element.ARCANE, false);
            });
            case CONSECRATION -> {
                targets.forEach(e -> {
                    SiegeDamage.deal(e, this, owner, 1.5f * power, Element.HOLY, false);
                    SiegeDamage.apply(e, ModEffects.PURIFIED.get(), 40, 0);
                });
                for (LivingEntity d : hostile ? SiegeDamage.enemiesAround(sl, c, r) : SiegeDamage.defendersAround(sl, c, r)) d.heal(1.5f * power);
            }
            case BLOOD_POOL -> targets.forEach(e -> {
                SiegeDamage.deal(e, this, owner, 1.5f * power, Element.PHYSICAL, false);
                SiegeDamage.applyStacking(e, ModEffects.BLEEDING.get(), 60, 2);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
            });
            case SHADOW_POOL -> targets.forEach(e -> {
                SiegeDamage.deal(e, this, owner, 2f * power, Element.SHADOW, false);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            });
            case METEOR_RAIN -> {
                if (age % 10 == 0) {
                    RandomSource rr = sl.random;
                    Vec3 at = c.add((rr.nextDouble() - 0.5) * r * 2, 0, (rr.nextDouble() - 0.5) * r * 2);
                    SiegeProjectile m = SiegeProjectile.create(sl, SiegeProjectile.ProjKind.METEOR, owner, at.add(rr.nextGaussian() * 3, 22, rr.nextGaussian() * 3));
                    m.payload(10f * power, Element.FIRE).area(3f);
                    m.gravity = 0;
                    m.aim(at, 1.4f).scale(2f).fire(sl);
                }
            }
            default -> {
            }
        }
    }

    private void detonate(ServerLevel sl) {
        Vec3 c = position();
        float r = radius();
        Entity owner = owner();
        switch (kind()) {
            case ORBITAL -> {
                Fx.skyBeam(sl, c, 0x7FE8FF, 60, r * 0.6);
                Fx.shockwave(sl, c, r, 0x7FE8FF);
                Fx.explosion(sl, c, r);
                Fx.sound(sl, c, net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM, 3f, 0.6f);
                SiegeDamage.areaDamage(sl, c, r, 120f * power, Element.ARCANE, this, owner, true);
            }
            case AIRSTRIKE -> {
                Vec3 dir = heading.lengthSqr() > 0 ? heading.normalize() : new Vec3(1, 0, 0);
                for (int i = -4; i <= 4; i++) {
                    Vec3 at = c.add(dir.scale(i * 2.5));
                    SiegeProjectile b = SiegeProjectile.create(sl, SiegeProjectile.ProjKind.MORTAR_SHELL, owner, at.add(0, 25 + Math.abs(i), 0));
                    b.payload(35f * power, Element.EXPLOSIVE).area(3.5f);
                    b.setDeltaMovement(dir.x * 0.2, -1.2, dir.z * 0.2);
                    b.scale(1.4f).fire(sl);
                }
                Fx.sound(sl, c, net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LAUNCH, 3f, 0.4f);
            }
            default -> {
            }
        }
        discard();
    }

    private void clientFx() {
        Level l = level();
        RandomSource r = random;
        float rad = radius();
        int fuse = entityData.get(FUSE);
        if (fuse > 0) {
            // targeting reticle
            for (int i = 0; i < 6; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                l.addParticle(kind() == Kind.ORBITAL ? ParticleTypes.END_ROD : ParticleTypes.FLAME,
                        getX() + Math.cos(a) * rad, getY() + 0.1, getZ() + Math.sin(a) * rad, 0, 0.02, 0);
            }
            return;
        }
        int n = (int) (rad * 2) + 1;
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * rad;
            double x = getX() + Math.cos(a) * d, z = getZ() + Math.sin(a) * d, y = getY() + 0.1;
            switch (kind()) {
                case FIRE -> l.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.03, 0);
                case SOUL_FIRE -> l.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.03, 0);
                case ACID -> l.addParticle(Fx.dust(0x7BD940, 1.3f), x, y, z, 0, 0, 0);
                case POISON_CLOUD -> l.addParticle(Fx.dust(0x5E9E2A, 2.2f), x, y + r.nextDouble() * 2, z, 0, 0.01, 0);
                case PLAGUE_RAIN -> l.addParticle(Fx.dust(0x9BD940, 1.0f), x, y + 6 + r.nextDouble() * 4, z, 0, -0.5, 0);
                case FROST -> l.addParticle(ParticleTypes.SNOWFLAKE, x, y + r.nextDouble(), z, 0, 0, 0);
                case SANDSTORM -> {
                    double sp = age * 0.25 + d * 0.8;
                    l.addParticle(com.lastbastion.fx.FxParticleOptions.of(com.lastbastion.fx.FxStyle.SMOKE, 0xE8C878, 0.9f, 24),
                            getX() + Math.cos(sp) * d, y + r.nextDouble() * 3, getZ() + Math.sin(sp) * d, -Math.sin(sp) * 0.2, 0.02, Math.cos(sp) * 0.2);
                }
                case GRAVITY_WELL -> {
                    Vec3 to = position().subtract(x, y, z).scale(0.12);
                    l.addParticle(com.lastbastion.fx.FxParticleOptions.of(com.lastbastion.fx.FxStyle.GLOW, 0xA070FF, 0.25f, 10), x, y + 0.5, z, to.x, 0.02, to.z);
                }
                case CONSECRATION -> l.addParticle(com.lastbastion.fx.FxParticleOptions.of(com.lastbastion.fx.FxStyle.STAR, 0xFFE8A0, 0.18f, 20), x, y, z, 0, 0.05, 0);
                case BLOOD_POOL -> l.addParticle(com.lastbastion.fx.FxParticleOptions.of(com.lastbastion.fx.FxStyle.DROP, 0x8A0A14, 0.14f, 14), x, y + 0.2, z, 0, 0.1, 0);
                case SHADOW_POOL -> l.addParticle(com.lastbastion.fx.FxParticleOptions.of(com.lastbastion.fx.FxStyle.SOUL, 0x5A2A7A, 0.3f, 20), x, y, z, 0, 0.04, 0);
                case WHIRLPOOL -> {
                    double sp = age * 0.3 + d;
                    l.addParticle(ParticleTypes.BUBBLE_POP, getX() + Math.cos(sp) * d, y + 0.2, getZ() + Math.sin(sp) * d, 0, 0.05, 0);
                    l.addParticle(ParticleTypes.SPLASH, x, y + 0.3, z, 0, 0.1, 0);
                }
                case TORNADO -> {
                    double h = r.nextDouble() * 6, rr = 0.4 + h * 0.35, sp = age * 0.5 + h * 2;
                    l.addParticle(ParticleTypes.CLOUD, getX() + Math.cos(sp) * rr, y + h, getZ() + Math.sin(sp) * rr, 0, 0.05, 0);
                }
                case HOLY -> l.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.05, 0);
                case STORM -> l.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y + 5 + r.nextDouble() * 3, z, 0, 0, 0);
                case BONE_SPIKES -> l.addParticle(Fx.dust(0xE8E0D0, 1.5f), x, y + r.nextDouble() * 1.5, z, 0, 0.1, 0);
                case METEOR_RAIN -> l.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
                default -> {
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
