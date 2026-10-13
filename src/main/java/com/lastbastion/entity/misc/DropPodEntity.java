package com.lastbastion.entity.misc;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/** Orbital drop pod. Screams down trailing fire, cratering on impact, then unfolds into a Titan Exosuit. */
public class DropPodEntity extends Entity {
    @Nullable
    private UUID owner;
    private boolean landed;
    private int landedTicks;

    public DropPodEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void setOwnerId(UUID id) {
        owner = id;
    }

    public boolean landed() {
        return landed || onGround();
    }

    public int landedTicks() {
        return landedTicks;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!onGround()) {
            setDeltaMovement(0, Math.max(-2.2, getDeltaMovement().y - 0.12), 0);
            move(MoverType.SELF, getDeltaMovement());
            if (level().isClientSide) {
                for (int i = 0; i < 4; i++) {
                    level().addParticle(ParticleTypes.FLAME, getX() + (random.nextDouble() - 0.5), getY() + 2 + random.nextDouble() * 2,
                            getZ() + (random.nextDouble() - 0.5), 0, 0.3, 0);
                    level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + 3, getZ(), 0, 0.2, 0);
                }
            }
            return;
        }
        if (!landed) {
            landed = true;
            if (level() instanceof ServerLevel sl) {
                Fx.explosion(sl, position(), 4);
                Fx.shockwave(sl, position(), 6, 0xFF9030);
                Fx.sound(sl, position(), SoundEvents.ANVIL_LAND, 2f, 0.4f);
                for (LivingEntity e : SiegeDamage.enemiesAround(sl, position(), 5)) {
                    SiegeDamage.deal(e, this, null, 40f, Element.EXPLOSIVE, false);
                    SiegeDamage.push(e, position(), 1.5, 0.6);
                }
            }
        }
        landedTicks++;
        if (landedTicks == 30 && level() instanceof ServerLevel sl) {
            TitanExoEntity exo = ModEntities.TITAN_EXO.get().create(sl);
            if (exo != null) {
                exo.moveTo(getX(), getY(), getZ(), getYRot(), 0);
                exo.setOwnerId(owner);
                sl.addFreshEntity(exo);
            }
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 40, 1, 1, 1, 0.1);
            Fx.sound(sl, position(), SoundEvents.PISTON_EXTEND, 2f, 0.5f);
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) owner = tag.getUUID("owner");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) tag.putUUID("owner", owner);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 256 * 256;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
