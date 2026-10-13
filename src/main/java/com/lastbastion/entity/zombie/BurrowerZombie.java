package com.lastbastion.entity.zombie;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Tunnels under your walls. Shortly after rising it dives underground (untargetable, leaving a
 * trail of churned dirt) and erupts within 5 blocks of the core, knocking defenders skyward.
 */
public class BurrowerZombie extends SiegeZombie {
    private int phase; // 0 = waiting, 1 = burrowed, 2 = surfaced
    private int wait = 30;

    public BurrowerZombie(EntityType<? extends SiegeZombie> type, Level level) {
        super(type, level, ZombieKind.BURROWER);
    }

    @Override
    public boolean isUntargetable() {
        return state();
    }

    @Override
    public boolean overrideMovement() {
        return phase == 1;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (state() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        BlockPos core = SiegeManager.corePos(level);
        if (core == null) return;
        if (phase == 0 && !isRising()) {
            if (--wait <= 0 && onGround()) {
                phase = 1;
                setState(true);
                noPhysics = true;
                setNoGravity(true);
                anim.play(ANIM_SPECIAL);
                Fx.sound(level, position(), SoundEvents.ROOTED_DIRT_BREAK, 1.2f, 0.5f);
            }
        } else if (phase == 1) {
            Vec3 to = Vec3.atBottomCenterOf(core).subtract(position()).multiply(1, 0, 1);
            double d = to.length();
            Vec3 step = to.normalize().scale(Math.min(d, 0.22 * (1 + rank().ordinal() * 0.08)));
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(getX() + step.x), (int) Math.floor(getZ() + step.z));
            setPos(getX() + step.x, ground - 1.6, getZ() + step.z);
            setDeltaMovement(Vec3.ZERO);
            setYRot((float) (Math.atan2(to.z, to.x) * 180 / Math.PI) - 90f);
            if (tickCount % 2 == 0) {
                var below = level.getBlockState(BlockPos.containing(getX(), ground - 1, getZ()));
                level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, below),
                        getX(), ground + 0.1, getZ(), 4, 0.3, 0.05, 0.3, 0.05);
            }
            if (tickCount % 20 == 0) Fx.sound(level, position(), SoundEvents.GRAVEL_BREAK, 0.6f, 0.6f);
            if (d < 5) surface(level, ground);
        }
    }

    private void surface(ServerLevel level, int ground) {
        phase = 2;
        setState(false);
        noPhysics = false;
        setNoGravity(false);
        setPos(getX(), ground, getZ());
        anim.play(ANIM_SPECIAL2);
        Fx.shockwave(level, position(), 3, 0x6B4A2A);
        Fx.sound(level, position(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.3f, 0.6f);
        for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 3)) {
            SiegeDamage.deal(e, this, this, kind.damage * damageScale(), Element.PHYSICAL, false);
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.7, 0));
            e.hurtMarked = true;
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("lbPhase", phase == 1 ? 0 : phase);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        phase = tag.getInt("lbPhase");
    }
}
