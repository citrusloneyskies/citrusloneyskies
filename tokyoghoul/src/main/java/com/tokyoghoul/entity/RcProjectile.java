package com.tokyoghoul.entity;

import com.tokyoghoul.combat.Combat;
import com.tokyoghoul.combat.Fx;
import com.tokyoghoul.data.Characters;
import com.tokyoghoul.move.MoveDef;
import com.tokyoghoul.registry.TGEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * Ukaku shards, thrown knives, quinque spears, Narukami lightning and fire bolts. One entity, styled by
 * {@link MoveDef.Shot} and tinted with the user's colour. Lightning chains once to a second enemy.
 */
public class RcProjectile extends Projectile {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(RcProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHOT = SynchedEntityData.defineId(RcProjectile.class, EntityDataSerializers.INT);
    private float damage = 4;
    private String moveId = "";
    private boolean chained;
    private int life = 60;

    public RcProjectile(EntityType<? extends RcProjectile> type, Level level) {
        super(type, level);
    }

    public static void fire(LivingEntity owner, MoveDef move, Vec3 at, Vec3 dir, float speed, float damage, int color) {
        RcProjectile p = new RcProjectile(TGEntities.RC_PROJECTILE.get(), owner.level());
        p.setOwner(owner);
        p.damage = damage;
        p.moveId = move.id;
        p.entityData.set(COLOR, color);
        p.entityData.set(SHOT, move.shot.ordinal());
        p.setPos(at.x, at.y, at.z);
        p.setDeltaMovement(dir.scale(speed));
        p.faceVelocity();
        owner.level().addFreshEntity(p);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(COLOR, 0xB01020);
        entityData.define(SHOT, 0);
    }

    public int color() {
        return entityData.get(COLOR);
    }

    public MoveDef.Shot shot() {
        int i = entityData.get(SHOT);
        MoveDef.Shot[] v = MoveDef.Shot.values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }

    private void faceVelocity() {
        Vec3 v = getDeltaMovement();
        double h = v.horizontalDistance();
        setYRot((float) (Math.atan2(v.x, v.z) * (180 / Math.PI)));
        setXRot((float) (Math.atan2(v.y, h) * (180 / Math.PI)));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && --life <= 0) {
            discard();
            return;
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hit)) onHit(hit);
        Vec3 v = getDeltaMovement();
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        if (shot() == MoveDef.Shot.SPEAR || shot() == MoveDef.Shot.KNIFE) setDeltaMovement(v.add(0, -0.012, 0));
        faceVelocity();
        if (level().isClientSide) {
            switch (shot()) {
                case LIGHTNING -> level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0, 0, 0);
                case FLAME -> level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0, 0, 0);
                default -> {
                }
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!super.canHitEntity(e) || !(e instanceof LivingEntity le)) return false;
        return getOwner() instanceof LivingEntity o && Combat.canHurt(o, le);
    }

    @Override
    protected void onHitEntity(EntityHitResult r) {
        if (level().isClientSide || !(r.getEntity() instanceof LivingEntity target) || !(getOwner() instanceof LivingEntity owner)) return;
        MoveDef move = Characters.move(moveId);
        if (move == null) move = com.tokyoghoul.data.Moves.ATTACK;
        Vec3 kb = getDeltaMovement().normalize().scale(0.25 + move.knockback * 0.3).add(0, 0.08, 0);
        Combat.hit(owner, target, move, damage, kb, shot() == MoveDef.Shot.LIGHTNING ? 10 : 5);
        if (shot() == MoveDef.Shot.LIGHTNING && !chained) chain(owner, target, move);
        if (shot() == MoveDef.Shot.FLAME) target.setSecondsOnFire(3);
        discard();
    }

    private void chain(LivingEntity owner, LivingEntity from, MoveDef move) {
        LivingEntity next = null;
        double best = 36;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(from.blockPosition()).inflate(6),
                e -> e != from && Combat.canHurt(owner, e))) {
            double d = e.distanceToSqr(from);
            if (d < best) {
                best = d;
                next = e;
            }
        }
        if (next == null) return;
        Vec3 a = from.getBoundingBox().getCenter(), b = next.getBoundingBox().getCenter();
        Fx.line(level(), a, b, color(), 0.8f, 0.3);
        Fx.particles(level(), ParticleTypes.ELECTRIC_SPARK, b, 10, 0.3, 0.2);
        Combat.hit(owner, next, move, damage * 0.6f, b.subtract(a).normalize().scale(0.3), 8);
        chained = true;
    }

    @Override
    protected void onHitBlock(BlockHitResult r) {
        super.onHitBlock(r);
        if (!level().isClientSide) {
            Fx.particles(level(), Fx.dust(color(), 1.2f), r.getLocation(), 6, 0.15, 0.02);
            if (shot() == MoveDef.Shot.SHARD) Fx.sound(level(), r.getLocation(), SoundEvents.AMETHYST_BLOCK_HIT, 0.5f, 1.4f);
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 128 * 128;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putFloat("damage", damage);
        t.putString("move", moveId);
        t.putInt("color", color());
        t.putInt("shot", entityData.get(SHOT));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        damage = t.getFloat("damage");
        moveId = t.getString("move");
        entityData.set(COLOR, t.getInt("color"));
        entityData.set(SHOT, t.getInt("shot"));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Nullable
    public LivingEntity ownerLiving() {
        return getOwner() instanceof LivingEntity l ? l : null;
    }
}
