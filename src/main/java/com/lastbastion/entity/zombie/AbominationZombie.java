package com.lastbastion.entity.zombie;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Elite: a lumbering mass of corpses stitched into one body, with two heads and a third arm. It plucks
 * Crawlers off its own body and hurls them at defenders (the landing spot is marked), slams the ground
 * around itself when crowded, and smashes through walls and turrets alike.
 */
public class AbominationZombie extends SiegeZombie {
    private int hurlCooldown = 80, slamCooldown = 60;
    private int action, actionTick;
    private Vec3 aimAt = Vec3.ZERO;

    public AbominationZombie(EntityType<? extends SiegeZombie> type, Level level) {
        super(type, level, ZombieKind.ABOMINATION);
        setMaxUpStep(1.5f);
    }

    @Override
    protected float kindElementMultiplier(Element e) {
        return e == Element.PHYSICAL ? 0.85f : e == Element.FIRE ? 1.25f : e == Element.POISON ? 0.5f : 1f;
    }

    @Override
    public double aggroRange() {
        return 6;
    }

    @Override
    public boolean targetsTurrets() {
        return true;
    }

    @Override
    public float breachPower() {
        return super.breachPower() * 3f;
    }

    @Override
    public boolean canBreachHard() {
        return true;
    }

    @Override
    public int attackWindup() {
        return 14;
    }

    @Override
    public int attackCooldown() {
        return 34;
    }

    @Override
    public double attackReach() {
        return getBbWidth() * 0.5 + 2;
    }

    @Override
    protected float turretDamageMultiplier() {
        return 1.5f;
    }

    @Override
    protected void onMeleeHit(LivingEntity target, float dmg) {
        SiegeDamage.push(target, position(), 1.3, 0.5);
    }

    @Override
    public boolean overrideMovement() {
        return action != 0;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (hurlCooldown > 0) hurlCooldown--;
        if (slamCooldown > 0) slamCooldown--;
        if (action != 0) {
            actionTick++;
            getNavigation().stop();
            if (action == ANIM_SPECIAL) tickHurl(level);
            else tickSlam(level);
            return;
        }
        if (isIncapacitated() || !onGround()) return;
        if (slamCooldown <= 0 && SiegeDamage.defendersAround(level, position(), 4.5).size() >= 2) {
            start(ANIM_SPECIAL2);
            slamCooldown = 140;
            Vfx.teleCircle(level, position().add(0, 0.05, 0), 4.5f, 18, 0xA0D040, true);
            return;
        }
        LivingEntity t = pickHurlTarget(level);
        if (hurlCooldown <= 0 && t != null) {
            aimAt = t.position();
            start(ANIM_SPECIAL);
            hurlCooldown = 150 + random.nextInt(40);
            Vfx.teleCircle(level, aimAt.add(0, 0.05, 0), 2.2f, 34, 0xA0D040, true);
        }
    }

    @Nullable
    private LivingEntity pickHurlTarget(ServerLevel level) {
        LivingEntity best = null;
        double bestD = 0;
        for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 16)) {
            double d = distanceTo(e);
            if (d < 6 || !getSensing().hasLineOfSight(e)) continue;
            if (best == null || d < bestD) {
                best = e;
                bestD = d;
            }
        }
        return best;
    }

    private void start(int a) {
        action = a;
        actionTick = 0;
        anim.play(a);
        getNavigation().stop();
    }

    private void face(Vec3 at) {
        double dx = at.x - getX(), dz = at.z - getZ();
        float yaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90f;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    private void tickHurl(ServerLevel level) {
        face(aimAt);
        if (actionTick == 6) Fx.sound(level, position(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 1.2f, 0.5f);
        if (actionTick == 20) {
            Vec3 hand = position().add(0, getBbHeight() * 0.9, 0);
            SiegeZombie c = ZombieSpawner.spawn(level, ZombieKind.CRAWLER, rank(), 0, hand, isWaveMob(), false);
            if (c != null) {
                c.setMinion(true);
                Vec3 d = aimAt.subtract(hand);
                double flat = Math.sqrt(d.x * d.x + d.z * d.z);
                double time = Math.max(10, flat / 0.7);
                double g = 0.08;
                c.setDeltaMovement(d.x / time, (d.y + 0.5 * g * time * time) / time, d.z / time);
                c.hurtMarked = true;
            }
            Fx.sound(level, hand, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.5f);
        }
        if (actionTick >= 30) action = 0;
    }

    private void tickSlam(ServerLevel level) {
        if (actionTick == 18) {
            Vfx.ring(level, position().add(0, 0.1, 0), 0xA0D040, 4.5f, 12, 1f);
            Vfx.spray(level, FxStyle.DEBRIS, position(), 0x5A4A3A, 0x3A2A1A, 0.45f, 20, 0.45f, 0.8f);
            Vfx.shake(level, position(), 0.5f, 14, 8);
            Fx.sound(level, position(), SoundEvents.GENERIC_EXPLODE, 1.2f, 0.6f);
            float dmg = (float) getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 4.5)) {
                SiegeDamage.deal(e, this, this, dmg * 1.2f, Element.PHYSICAL, false);
                SiegeDamage.push(e, position(), 1.2, 0.7);
                SiegeDamage.apply(e, ModEffects.CONCUSSED.get(), 60, 0);
            }
            BlockPos core = SiegeManager.corePos(level);
            if (core != null && position().distanceTo(Vec3.atBottomCenterOf(core)) < 5) SiegeManager.damageCore(level, coreDamage() * 0.5f, this);
        }
        if (actionTick >= 32) action = 0;
    }
}
