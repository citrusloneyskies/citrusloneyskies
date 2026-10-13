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
 * Ethereal revenant. Drifts straight through walls toward the core. Physical and explosive damage
 * is halved; holy and arcane damage are devastating. Rendered translucent with spectral flames.
 */
public class WraithZombie extends SiegeZombie {
    public WraithZombie(EntityType<? extends SiegeZombie> type, Level level) {
        super(type, level, ZombieKind.WRAITH);
        setNoGravity(true);
    }

    @Override
    protected float kindElementMultiplier(Element e) {
        return switch (e) {
            case PHYSICAL, EXPLOSIVE -> 0.5f;
            case HOLY -> 1.2f;
            case ARCANE -> 1.5f;
            case SHADOW -> 0f;
            default -> 1f;
        };
    }

    @Override
    public double aggroRange() {
        return 5;
    }

    @Override
    public boolean overrideMovement() {
        return getTarget() == null && !isRising();
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    private int swing = -1;
    private int coreCd;

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        noPhysics = getTarget() == null;
        if (coreCd > 0) coreCd--;
        if (!overrideMovement() || isIncapacitated()) {
            if (!noPhysics) setNoGravity(false);
            return;
        }
        setNoGravity(true);
        BlockPos core = SiegeManager.corePos(level);
        if (core == null) return;
        Vec3 c = Vec3.atBottomCenterOf(core);
        Vec3 to = c.subtract(position());
        double hd = to.horizontalDistance();
        double reach = 1.6;
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(getX()), (int) Math.floor(getZ()));
        double targetY = hd < 6 ? core.getY() : Math.max(ground, core.getY()) + 0.2;
        setYRot((float) (Math.atan2(to.z, to.x) * 180 / Math.PI) - 90f);
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        if (swing >= 0) {
            swing++;
            if (swing == attackWindup()) onCoreStrike(level, core);
            if (swing > attackWindup() + 5) swing = -1;
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (hd > reach) {
            double speed = getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 0.8;
            Vec3 step = new Vec3(to.x, 0, to.z).normalize().scale(speed);
            double dy = (targetY - getY()) * 0.15;
            setDeltaMovement(step.x, dy, step.z);
        } else if (coreCd <= 0) {
            swing = 0;
            coreCd = attackCooldown() + attackWindup();
            anim.play(ANIM_CORE);
        }
        if (tickCount % 3 == 0) level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1, getZ(), 1, 0.3, 0.5, 0.3, 0.01);
    }
}
