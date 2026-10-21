package com.tokyoghoul.entity.ai;

import com.tokyoghoul.combat.FighterState;
import com.tokyoghoul.entity.TGCharacterEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Footwork while fighting (JJK's AIActiveProcedure): sprint in with level-scaled speed and jump boosts
 * when far, circle-strafe at the preferred range with direction switches and hops, back off when a
 * ranged character is crowded. Moves take over movement while they run.
 */
public class CombatMovementGoal extends Goal {
    private final TGCharacterEntity mob;
    private int repath;
    private int strafeTimer;
    private float strafeSide = 1;

    public CombatMovementGoal(TGCharacterEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        mob.setSprinting(false);
        mob.getNavigation().stop();
        mob.getMoveControl().strafe(0, 0);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity t = mob.getTarget();
        if (t == null) return;
        FighterState s = mob.fighterState();
        mob.getLookControl().setLookAt(t, 40, 40);
        if (s.busy() || s.stun > 0) {
            mob.getNavigation().stop();
            mob.setSprinting(false);
            return;
        }
        double dist = mob.distanceTo(t);
        float pref = mob.brain.preferredRange();
        int level = mob.fighterLevel();
        if (dist > pref + 2) {
            if (--repath <= 0) {
                repath = 6 + mob.getRandom().nextInt(6);
                mob.getNavigation().moveTo(t, dist > 10 ? 1.45 : 1.2);
            }
            mob.setSprinting(true);
            if (dist > 6 && !mob.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12, Math.min(2, level / 3), false, false));
            }
            if (dist > 12 && mob.onGround() && mob.getRandom().nextInt(30) == 0) mob.getJumpControl().jump();
        } else {
            mob.getNavigation().stop();
            mob.setSprinting(false);
            if (--strafeTimer <= 0) {
                strafeTimer = 20 + mob.getRandom().nextInt(30);
                strafeSide = mob.getRandom().nextBoolean() ? 1 : -1;
            }
            float fwd = dist < pref - 1.5 ? -0.5f : dist > pref + 0.5 ? 0.4f : 0f;
            mob.getMoveControl().strafe(fwd, strafeSide * 0.45f);
            if (mob.onGround() && mob.getRandom().nextInt(60) == 0) mob.getJumpControl().jump();
        }
    }
}
