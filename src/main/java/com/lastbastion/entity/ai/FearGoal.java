package com.lastbastion.entity.ai;

import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Feared zombies (Dragon roar, Howl) flee from the source of terror. */
public class FearGoal extends Goal {
    private final SiegeZombie mob;
    private int repath;

    public FearGoal(SiegeZombie mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.hasEffect(ModEffects.FEARED.get()) && !mob.isIncapacitated();
    }

    @Override
    public void start() {
        repath = 0;
        mob.setTarget(null);
    }

    @Override
    public void tick() {
        if (--repath > 0) return;
        repath = 15;
        Vec3 from = mob.fearFrom;
        if (from == null) {
            BlockPos core = SiegeManager.corePos((ServerLevel) mob.level());
            from = core != null ? Vec3.atCenterOf(core) : mob.position();
        }
        Vec3 pos = DefaultRandomPos.getPosAway(mob, 12, 5, from);
        if (pos != null) mob.getNavigation().moveTo(pos.x, pos.y, pos.z, 1.3);
    }
}
