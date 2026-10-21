package com.tokyoghoul.entity;

import com.tokyoghoul.combat.Combat;
import com.tokyoghoul.combat.Fx;
import com.tokyoghoul.data.Characters;
import com.tokyoghoul.move.MoveDef;
import com.tokyoghoul.registry.TGEntities;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A kagune or IXA spike that erupts from the ground, launches whatever stands on it, then sinks back.
 * Spawned in lines and spirals by {@link MoveDef.Kind#SPIKES} moves.
 */
public class GroundSpike extends Entity {
    public static final int LIFE = 26;
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(GroundSpike.class, EntityDataSerializers.INT);
    private float damage = 6;
    private String moveId = "";
    @Nullable
    private UUID owner;
    private int age;

    public GroundSpike(EntityType<? extends GroundSpike> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static void spawn(LivingEntity owner, MoveDef move, Vec3 at, float damage, int color) {
        GroundSpike s = new GroundSpike(TGEntities.GROUND_SPIKE.get(), owner.level());
        s.owner = owner.getUUID();
        s.damage = damage;
        s.moveId = move.id;
        s.entityData.set(COLOR, color);
        s.setPos(at.x, at.y, at.z);
        s.setYRot(owner.getRandom().nextFloat() * 360);
        owner.level().addFreshEntity(s);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(COLOR, 0xB01020);
    }

    public int color() {
        return entityData.get(COLOR);
    }

    /** 0..1 how far the spike has risen (client render uses age + partial ticks). */
    public static float rise(float age) {
        if (age < 4) return age / 4f;
        if (age < LIFE - 8) return 1f;
        return Math.max(0, (LIFE - age) / 8f);
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (level().isClientSide) return;
        if (age == 2) {
            Entity o = owner == null ? null : ((net.minecraft.server.level.ServerLevel) level()).getEntity(owner);
            if (o instanceof LivingEntity ol) {
                MoveDef move = Characters.move(moveId);
                if (move == null) move = com.tokyoghoul.data.Moves.KNOCKBACK;
                for (LivingEntity e : Combat.targetsIn(ol, position().add(0, 0.8, 0), 1.3)) {
                    Combat.hit(ol, e, move, damage, new Vec3(0, 0.85, 0).add(e.position().subtract(position()).normalize().scale(0.2)), 14);
                }
            }
            Fx.debris(level(), this, 10);
            Fx.sound(level(), position(), SoundEvents.POINTED_DRIPSTONE_LAND, 1f, 0.6f + random.nextFloat() * 0.3f);
        }
        if (age >= LIFE) discard();
    }

    public int age() {
        return age;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        damage = t.getFloat("damage");
        moveId = t.getString("move");
        entityData.set(COLOR, t.getInt("color"));
        age = t.getInt("age");
        if (t.hasUUID("owner")) owner = t.getUUID("owner");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putFloat("damage", damage);
        t.putString("move", moveId);
        t.putInt("color", color());
        t.putInt("age", age);
        if (owner != null) t.putUUID("owner", owner);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
