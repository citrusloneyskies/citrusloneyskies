package com.lastbastion.entity.zombie;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A husk in a scorched hazmat suit hauling twin fuel tanks and a flamethrower. It stops to hose turrets
 * and defenders with a marked cone of fire; when it dies the tanks rupture in a fireball that also
 * burns the horde around it, so killing it in the middle of a crowd pays off.
 */
public class IncineratorZombie extends SiegeZombie {
    private int sprayCooldown = 60;
    private int spraying;
    private Vec3 sprayDir = Vec3.ZERO;
    private boolean ruptured;

    public IncineratorZombie(EntityType<? extends SiegeZombie> type, Level level) {
        super(type, level, ZombieKind.INCINERATOR);
    }

    @Override
    protected float kindElementMultiplier(Element e) {
        return e == Element.FIRE ? 0f : e == Element.ICE ? 1.25f : e == Element.EXPLOSIVE ? 1.3f : 1f;
    }

    @Override
    public double aggroRange() {
        return 7;
    }

    @Override
    public boolean targetsTurrets() {
        return true;
    }

    @Override
    public boolean overrideMovement() {
        return spraying > 0;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (sprayCooldown > 0) sprayCooldown--;
        if (spraying > 0) {
            spraying--;
            getNavigation().stop();
            float yaw = (float) (Math.atan2(sprayDir.z, sprayDir.x) * 180 / Math.PI) - 90f;
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;
            if (spraying < 30) {
                Vec3 nozzle = position().add(0, 1.1, 0).add(sprayDir.scale(0.9));
                if (spraying % 2 == 0) Vfx.jet(level, FxStyle.FLAME, nozzle, nozzle.add(sprayDir.scale(5.5)), 0xFFD060, 0xFF3A08, 0.45f, 10, 14);
                if (spraying % 5 == 0) {
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position().add(sprayDir.scale(3)), 3.6)) {
                        Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
                        if (to.lengthSqr() < 0.01 || to.normalize().dot(sprayDir) < 0.8) continue;
                        SiegeDamage.deal(e, this, this, 2f * damageScale(), Element.FIRE, false);
                        SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 60, 2);
                        e.setSecondsOnFire(3);
                    }
                }
                if (spraying == 12) HazardZone.spawn(level, HazardZone.Kind.FIRE, position().add(sprayDir.scale(3.5)), 1.6f, 60, damageScale() * 0.5f, this, true);
                if (spraying % 8 == 0) Fx.sound(level, nozzle, SoundEvents.BLAZE_SHOOT, 0.8f, 0.7f);
            }
            return;
        }
        LivingEntity t = getTarget();
        if (sprayCooldown > 0 || t == null || isIncapacitated()) return;
        double d = distanceTo(t);
        if (d < 5.5 && getSensing().hasLineOfSight(t)) {
            sprayDir = t.position().subtract(position()).multiply(1, 0, 1).normalize();
            spraying = 42;
            sprayCooldown = 100 + random.nextInt(40);
            anim.play(ANIM_SPECIAL);
            Vfx.teleCone(level, position().add(0, 0.05, 0), position().add(sprayDir.scale(3)), 5.5f, 36, 12, 0xFF6A10, true);
            Fx.sound(level, position(), SoundEvents.FIRECHARGE_USE, 1f, 0.6f);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!ruptured && level() instanceof ServerLevel sl) {
            ruptured = true;
            Vec3 c = position().add(0, 0.8, 0);
            Vfx.sphere(sl, c, 0xFF7A1A, 3.2f, 10);
            Vfx.flash(sl, c, 0xFFD060, 2.5f, 6);
            Vfx.spray(sl, FxStyle.FLAME, c, 0xFFD060, 0xFF3A08, 0.5f, 30, 0.4f, 0.6f);
            Vfx.shake(sl, c, 0.4f, 12, 6);
            Fx.sound(sl, c, SoundEvents.GENERIC_EXPLODE, 1.2f, 1.1f);
            HazardZone.spawn(sl, HazardZone.Kind.FIRE, position(), 2.6f, 80, damageScale() * 0.6f, this, true);
            for (LivingEntity e : SiegeDamage.defendersAround(sl, position(), 3.2)) {
                SiegeDamage.deal(e, this, this, 6f * damageScale(), Element.FIRE, false);
                e.setSecondsOnFire(4);
            }
            for (LivingEntity e : SiegeDamage.enemiesAround(sl, position(), 3.4)) {
                if (e == this || e instanceof IncineratorZombie) continue;
                e.setSecondsOnFire(5);
                e.hurt(damageSources().inFire(), Math.max(4f, e.getMaxHealth() * 0.15f));
            }
        }
        super.die(source);
    }
}
