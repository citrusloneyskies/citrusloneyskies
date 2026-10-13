package com.lastbastion.entity.ai;

import com.lastbastion.entity.zombie.SiegeZombie;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Blocks every other behaviour while frozen, shocked, or still clawing out of the ground. */
public class IncapacitatedGoal extends Goal {
    private final SiegeZombie mob;

    public IncapacitatedGoal(SiegeZombie mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return mob.isIncapacitated() || mob.overrideMovement();
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
