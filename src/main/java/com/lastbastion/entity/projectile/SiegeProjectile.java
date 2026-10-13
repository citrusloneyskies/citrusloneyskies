package com.lastbastion.entity.projectile;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.registry.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Generic configurable projectile used by turrets, heroes, zombies, bosses and player gear.
 * Appearance and trail come from {@link ProjKind}; gameplay payload (damage, element, AoE, status,
 * pierce, homing, custom impact callback) is configured by the shooter.
 */
public class SiegeProjectile extends Projectile {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(SiegeProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(SiegeProjectile.class, EntityDataSerializers.FLOAT);

    public float damage = 4f;
    public Element element = Element.PHYSICAL;
    public float aoe = 0f;
    public float gravity = 0f;
    public int pierce = 0;
    public int life = 100;
    public float knockback = 0f;
    @Nullable
    public Supplier<MobEffect> status;
    public int statusDuration;
    public int statusMaxStack;
    @Nullable
    public LivingEntity homingTarget;
    public float homingStrength = 0.15f;
    /** Server-side impact hook: (projectile, impact position). Called once at the end of flight. */
    @Nullable
    public BiConsumer<SiegeProjectile, Vec3> onImpact;
    /** Per-entity hook for every entity hit (in addition to damage). */
    @Nullable
    public BiConsumer<SiegeProjectile, LivingEntity> onEntityHit;
    private final Set<Integer> hitIds = new HashSet<>();

    public SiegeProjectile(EntityType<? extends SiegeProjectile> type, Level level) {
        super(type, level);
    }

    public static SiegeProjectile create(Level level, ProjKind kind, @Nullable Entity owner, Vec3 from) {
        SiegeProjectile p = new SiegeProjectile(ModEntities.PROJECTILE.get(), level);
        p.setOwner(owner);
        p.setKind(kind);
        p.setPos(from.x, from.y, from.z);
        p.gravity = kind.gravity;
        return p;
    }

    /** Aim at a point with the given speed; for arcing shots computes the needed launch angle. */
    public SiegeProjectile aim(Vec3 target, float speed) {
        Vec3 d = target.subtract(position());
        if (gravity > 0) {
            double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
            double t = Math.max(4, horiz / speed);
            double vy = (d.y + 0.5 * gravity * t * t) / t;
            Vec3 h = new Vec3(d.x, 0, d.z).normalize().scale(horiz / t);
            setDeltaMovement(h.x, vy, h.z);
        } else {
            setDeltaMovement(d.normalize().scale(speed));
        }
        updateRotation();
        return this;
    }

    public SiegeProjectile payload(float damage, Element element) {
        this.damage = damage;
        this.element = element;
        return this;
    }

    public SiegeProjectile status(Supplier<MobEffect> effect, int duration, int maxStack) {
        this.status = effect;
        this.statusDuration = duration;
        this.statusMaxStack = maxStack;
        return this;
    }

    public SiegeProjectile area(float radius) {
        this.aoe = radius;
        return this;
    }

    public SiegeProjectile homing(@Nullable LivingEntity target, float strength) {
        this.homingTarget = target;
        this.homingStrength = strength;
        return this;
    }

    public SiegeProjectile scale(float s) {
        entityData.set(SCALE, s);
        return this;
    }

    public void fire(Level level) {
        level.addFreshEntity(this);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(KIND, 0);
        entityData.define(SCALE, 1f);
    }

    public ProjKind kind() {
        return ProjKind.byId(entityData.get(KIND));
    }

    public void setKind(ProjKind k) {
        entityData.set(KIND, k.ordinal());
    }

    public float visualScale() {
        return entityData.get(SCALE);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        if (!level().isClientSide) {
            if (--life <= 0) {
                impact(position());
                return;
            }
            if (homingTarget != null && homingTarget.isAlive()) {
                Vec3 to = homingTarget.position().add(0, homingTarget.getBbHeight() * 0.5, 0).subtract(position()).normalize();
                double speed = motion.length();
                motion = motion.normalize().lerp(to, homingStrength).normalize().scale(speed);
            }
        }
        motion = motion.add(0, -gravity, 0);
        setDeltaMovement(motion);

        Vec3 from = position();
        Vec3 to = from.add(motion);
        HitResult blockHit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : to;
        EntityHitResult eh = ProjectileUtil.getEntityHitResult(level(), this, from, end,
                getBoundingBox().expandTowards(motion).inflate(0.4 + 0.2 * visualScale()), this::canHitEntity);

        if (!level().isClientSide) {
            if (eh != null && eh.getEntity() instanceof LivingEntity le) {
                hitEntity(le);
                if (pierce-- <= 0) {
                    impact(eh.getLocation());
                    return;
                }
            } else if (blockHit.getType() == HitResult.Type.BLOCK) {
                impact(blockHit.getLocation());
                return;
            }
        }
        setPos(end.x, end.y, end.z);
        updateRotation();
        if (level().isClientSide) trail(from, end);
    }

    private void trail(Vec3 from, Vec3 to) {
        ProjKind k = kind();
        ParticleOptions p = k.trail();
        if (p == null) return;
        int n = Math.max(1, k.trailDensity);
        for (int i = 0; i < n; i++) {
            Vec3 at = from.lerp(to, i / (double) n);
            level().addParticle(p, at.x, at.y, at.z, 0, 0, 0);
        }
        if (k.secondary() != null && random.nextInt(2) == 0) {
            level().addParticle(k.secondary(), to.x, to.y, to.z, (random.nextDouble() - 0.5) * 0.05, 0.02, (random.nextDouble() - 0.5) * 0.05);
        }
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!(e instanceof LivingEntity) || !e.isAlive() || e.isSpectator()) return false;
        if (hitIds.contains(e.getId())) return false;
        Entity owner = getOwner();
        if (e == owner) return false;
        return SiegeDamage.canHit(owner, e);
    }

    private void hitEntity(LivingEntity le) {
        hitIds.add(le.getId());
        Entity owner = getOwner();
        if (aoe <= 0) {
            SiegeDamage.deal(le, this, owner, damage, element, knockback > 0);
            if (knockback > 0) SiegeDamage.push(le, position(), knockback, 0.15);
            if (status != null) SiegeDamage.applyStacking(le, status.get(), statusDuration, statusMaxStack);
        }
        if (onEntityHit != null) onEntityHit.accept(this, le);
    }

    private void impact(Vec3 at) {
        if (!(level() instanceof ServerLevel sl)) return;
        Entity owner = getOwner();
        if (aoe > 0) {
            boolean ownerIsEnemy = owner != null && SiegeDamage.isEnemy(owner);
            for (LivingEntity e : ownerIsEnemy ? SiegeDamage.defendersAround(sl, at, aoe) : SiegeDamage.enemiesAround(sl, at, aoe)) {
                double d = Math.sqrt(e.position().distanceToSqr(at));
                float f = (float) (1.0 - 0.5 * Math.min(1, d / aoe));
                SiegeDamage.deal(e, this, owner, damage * f, element, false);
                if (knockback > 0) SiegeDamage.push(e, at, knockback * f, 0.3 * f);
                if (status != null) SiegeDamage.applyStacking(e, status.get(), statusDuration, statusMaxStack);
                if (onEntityHit != null && !hitIds.contains(e.getId())) onEntityHit.accept(this, e);
            }
        }
        kind().impactFx(sl, at, aoe);
        if (onImpact != null) onImpact.accept(this, at);
        discard();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 128 * 128;
    }

    /** Visual identity for each projectile family. */
    public enum ProjKind {
        BOLT(0.0f, 0x8A6A3A, 2),
        FROST_SHARD(0.0f, 0x9FEAFF, 2),
        WATER_ORB(0.01f, 0x2F6BFF, 3),
        ACID_GLOB(0.04f, 0x7BD940, 2),
        MORTAR_SHELL(0.06f, 0x3A3A3A, 2),
        HOLY_BOLT(0.0f, 0xFFF2B0, 3),
        ARCANE_BOLT(0.0f, 0xC86BFF, 3),
        ARROW(0.02f, 0xC8B080, 1),
        FIREBALL(0.0f, 0xFF7A1A, 3),
        MISSILE(0.0f, 0xD0D0D0, 2),
        ROCKET(0.0f, 0x606060, 3),
        GRENADE(0.05f, 0x4A5A3A, 1),
        BILE(0.04f, 0x9BD940, 2),
        BOULDER(0.05f, 0xD8D0C0, 1),
        SOUL_BOLT(0.0f, 0x5AD8FF, 3),
        METEOR(0.03f, 0xFF4A00, 4),
        JAVELIN(0.0f, 0xF4F06A, 4),
        HOOK(0.0f, 0x6A6A6A, 1),
        WIND_BLADE(0.0f, 0xCFFFF0, 2),
        SHADOW_ORB(0.0f, 0x5A2A7A, 3);

        public final float gravity;
        public final int color;
        public final int trailDensity;

        ProjKind(float gravity, int color, int trailDensity) {
            this.gravity = gravity;
            this.color = color;
            this.trailDensity = trailDensity;
        }

        public static ProjKind byId(int i) {
            return i >= 0 && i < values().length ? values()[i] : BOLT;
        }

        @Nullable
        public ParticleOptions trail() {
            return switch (this) {
                case FROST_SHARD -> ParticleTypes.SNOWFLAKE;
                case WATER_ORB -> ParticleTypes.FALLING_WATER;
                case ACID_GLOB, BILE -> Fx.dust(color, 1.0f);
                case MORTAR_SHELL, ROCKET, MISSILE -> ParticleTypes.SMOKE;
                case HOLY_BOLT -> ParticleTypes.END_ROD;
                case ARCANE_BOLT -> ParticleTypes.WITCH;
                case FIREBALL, METEOR -> ParticleTypes.FLAME;
                case SOUL_BOLT -> ParticleTypes.SOUL_FIRE_FLAME;
                case JAVELIN -> ParticleTypes.ELECTRIC_SPARK;
                case WIND_BLADE -> ParticleTypes.CLOUD;
                case SHADOW_ORB -> ParticleTypes.SQUID_INK;
                case BOULDER -> ParticleTypes.ASH;
                case ARROW, BOLT -> ParticleTypes.CRIT;
                default -> null;
            };
        }

        @Nullable
        public ParticleOptions secondary() {
            return switch (this) {
                case FIREBALL, METEOR -> ParticleTypes.LARGE_SMOKE;
                case ROCKET, MISSILE -> ParticleTypes.FLAME;
                case HOLY_BOLT, JAVELIN -> Fx.dust(color, 1.2f);
                case ARCANE_BOLT -> Fx.dust(color, 1.0f);
                default -> null;
            };
        }

        public void impactFx(ServerLevel level, Vec3 at, float aoe) {
            switch (this) {
                case MORTAR_SHELL, ROCKET, MISSILE, METEOR, GRENADE -> Fx.explosion(level, at, Math.max(1.5, aoe));
                case FROST_SHARD -> {
                    level.sendParticles(ParticleTypes.ITEM_SNOWBALL, at.x, at.y, at.z, 10, 0.2, 0.2, 0.2, 0.1);
                    Fx.sound(level, at, SoundEvents.GLASS_BREAK, 0.4f, 1.6f);
                }
                case WATER_ORB -> {
                    level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 25, 0.4, 0.2, 0.4, 0.2);
                    Fx.sound(level, at, SoundEvents.GENERIC_SPLASH, 0.5f, 1.2f);
                }
                case ACID_GLOB, BILE -> {
                    level.sendParticles(Fx.dust(color, 1.5f), at.x, at.y, at.z, 20, Math.max(0.3, aoe * 0.4), 0.2, Math.max(0.3, aoe * 0.4), 0);
                    Fx.sound(level, at, SoundEvents.SLIME_SQUISH, 0.6f, 0.8f);
                }
                case HOLY_BOLT -> level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.1);
                case ARCANE_BOLT, SHADOW_ORB -> level.sendParticles(ParticleTypes.WITCH, at.x, at.y, at.z, 15, 0.3, 0.3, 0.3, 0.1);
                case FIREBALL -> {
                    level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 20, 0.4, 0.4, 0.4, 0.05);
                    Fx.sound(level, at, SoundEvents.BLAZE_SHOOT, 0.5f, 0.7f);
                }
                case SOUL_BOLT -> level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 10, 0.3, 0.3, 0.3, 0.05);
                case JAVELIN -> {
                    Fx.bolt(level, at.add(0, 12, 0), at, 0xF4F06A, 1.3f);
                    Fx.sound(level, at, SoundEvents.LIGHTNING_BOLT_IMPACT, 1f, 1.2f);
                }
                case BOULDER -> {
                    level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 20, 1, 0.3, 1, 0.05);
                    Fx.sound(level, at, SoundEvents.STONE_BREAK, 1.5f, 0.5f);
                }
                case WIND_BLADE -> level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 10, 0.3, 0.3, 0.3, 0.1);
                default -> level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.1, 0.1, 0.1, 0.2);
            }
        }
    }
}
