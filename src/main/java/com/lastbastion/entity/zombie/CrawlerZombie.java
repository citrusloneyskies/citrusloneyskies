package com.lastbastion.entity.zombie;

import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A legless torso dragging itself along by its arms. Low to the ground and easy to miss, it lunges at
 * the ankles of anyone close and clings on, slowing them to a crawl. Abominations throw them.
 */
public class CrawlerZombie extends SiegeZombie {
    private int lungeCooldown = 30;

    public CrawlerZombie(EntityType<? extends SiegeZombie> type, Level level) {
        super(type, level, ZombieKind.CRAWLER);
    }

    @Override
    public double aggroRange() {
        return 5;
    }

    @Override
    public int attackWindup() {
        return 6;
    }

    @Override
    public double attackReach() {
        return 1.3;
    }

    @Override
    protected void onMeleeHit(LivingEntity target, float dmg) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (lungeCooldown > 0) lungeCooldown--;
        LivingEntity t = getTarget();
        if (lungeCooldown > 0 || t == null || isIncapacitated() || !onGround()) return;
        double d = distanceTo(t);
        if (d > 1.8 && d < 4) {
            Vec3 dir = t.position().subtract(position()).multiply(1, 0, 1).normalize();
            setDeltaMovement(dir.x * 0.6, 0.22, dir.z * 0.6);
            hasImpulse = true;
            anim.play(ANIM_SPECIAL);
            Fx.sound(level, position(), SoundEvents.ZOMBIE_INFECT, 0.7f, 1.4f);
            lungeCooldown = 60 + random.nextInt(30);
            for (LivingEntity e : SiegeDamage.defendersAround(level, position().add(dir.scale(1.5)), 1.4)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }
        }
    }
}
