package com.lastbastion.entity.ai;

import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Telegraphed melee: wind-up animation, then the strike lands if the target is still in reach. */
public class SiegeMeleeGoal extends Goal {
    private final SiegeZombie mob;
    private int swing = -1;
    private int cooldown;
    private int repath;

    public SiegeMeleeGoal(SiegeZombie mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive() && !mob.isIncapacitated();
    }

    @Override
    public boolean canContinueToUse() {
        return (canUse() || swing >= 0) && !mob.isIncapacitated();
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        swing = -1;
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity t = mob.getTarget();
        if (cooldown > 0) cooldown--;
        if (t == null) {
            swing = -1;
            return;
        }
        mob.getLookControl().setLookAt(t, 30f, 30f);
        double reach = mob.attackReach() + t.getBbWidth() * 0.5;
        double d2 = mob.distanceToSqr(t);
        if (swing >= 0) {
            swing++;
            if (swing == mob.attackWindup()) {
                boolean miss = mob.hasEffect(ModEffects.DISORIENTED.get()) && mob.getRandom().nextBoolean();
                if (!miss && d2 <= (reach + 0.8) * (reach + 0.8)) mob.doHurtTarget(t);
            }
            if (swing >= mob.attackWindup() + 5) swing = -1;
            return;
        }
        if (d2 <= reach * reach && cooldown <= 0 && mob.getSensing().hasLineOfSight(t)) {
            swing = 0;
            cooldown = mob.attackCooldown() + mob.attackWindup();
            mob.getNavigation().stop();
            mob.anim.play(SiegeZombie.ANIM_ATTACK);
            return;
        }
        if (--repath <= 0) {
            repath = 8 + mob.getRandom().nextInt(6);
            mob.getNavigation().moveTo(t, 1.15);
        }
    }
}
