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
 * Walking plague-bomb. When it reaches the core or a defender it swells and detonates in a toxic
 * blast that devastates the core. Killing it at range still pops it, but far from your walls.
 */
public class BloaterZombie extends SiegeZombie {
    private int fuse = -1;
    private boolean exploded;

    public BloaterZombie(EntityType<? extends SiegeZombie> type, Level level) {
        super(type, level, ZombieKind.BLOATER);
    }

    @Override
    protected float kindElementMultiplier(Element e) {
        return e == Element.FIRE ? 1.3f : e == Element.POISON ? 0f : 1f;
    }

    @Override
    public double aggroRange() {
        return 4;
    }

    public float swell(float pt) {
        return fuse < 0 ? 0f : Math.min(1f, (fuse + pt) / 30f);
    }

    @Override
    public boolean overrideMovement() {
        return fuse >= 0;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (exploded) return;
        if (fuse >= 0) {
            fuse++;
            if (fuse % 5 == 0) Fx.sound(level, position(), SoundEvents.CREEPER_PRIMED, 1f, 0.5f + fuse / 60f);
            level.sendParticles(Fx.dust(0x7BD940, 1.6f), getX(), getY() + 1, getZ(), 3, 0.6, 0.6, 0.6, 0);
            if (fuse >= 30) explode(level, true);
            return;
        }
        if (isIncapacitated()) return;
        boolean near = false;
        BlockPos core = SiegeManager.corePos(level);
        if (core != null && position().distanceTo(Vec3.atBottomCenterOf(core)) < 2.8 + getBbWidth() * 0.5) near = true;
        if (!near && !SiegeDamage.defendersAround(level, position(), 2.6).isEmpty()) near = true;
        if (near) {
            fuse = 0;
            anim.play(ANIM_SPECIAL);
            getNavigation().stop();
        }
    }

    private void explode(ServerLevel level, boolean full) {
        if (exploded) return;
        exploded = true;
        Vec3 c = position().add(0, 0.8, 0);
        float dmg = kind.damage * damageScale() * (full ? 1f : 0.6f);
        Fx.explosion(level, c, 3.5);
        level.sendParticles(Fx.dust(0x5E9E2A, 3f), c.x, c.y, c.z, 60, 1.6, 1.0, 1.6, 0);
        SiegeDamage.areaDamage(level, c, 4.5, dmg, Element.POISON, this, this, false);
        BlockPos core = SiegeManager.corePos(level);
        if (core != null) {
            double d = c.distanceTo(Vec3.atCenterOf(core));
            if (d < 5) SiegeManager.damageCore(level, coreDamage() * (float) (1.0 - d / 8.0), this);
        }
        HazardZone.spawn(level, HazardZone.Kind.POISON_CLOUD, position(), 3.5f, 100, damageScale(), this, true);
        if (full) {
            discard();
            SiegeManager.untrack(level, this);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl && !exploded) explode(sl, false);
        super.die(source);
    }
}
