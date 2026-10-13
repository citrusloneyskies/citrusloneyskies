package com.lastbastion.entity.ai;

import com.lastbastion.combat.SiegeAlly;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.hero.HeroEntity;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Zombie threat model: the core is the default objective. A zombie only diverts when
 * (1) taunted, (2) recently hurt by a nearby defender, or (3) a defender stands within its aggro range.
 * Diversions are leashed so the horde keeps pressure on the core.
 */
public class SiegeTargetGoal extends Goal {
    private final SiegeZombie mob;
    private int engagedTicks;
    private int scan;

    public SiegeTargetGoal(SiegeZombie mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (mob.isIncapacitated() || mob.hasEffect(ModEffects.FEARED.get())) return false;
        if (--scan > 0 && mob.tauntedBy == null) return false;
        scan = 10;
        LivingEntity t = pick();
        if (t != null) {
            mob.setTarget(t);
            return true;
        }
        return false;
    }

    private LivingEntity pick() {
        ServerLevel level = (ServerLevel) mob.level();
        if (mob.tauntedBy != null && level.getEntity(mob.tauntedBy) instanceof LivingEntity taunter && taunter.isAlive()) {
            return taunter;
        }
        LivingEntity hurtBy = mob.getLastHurtByMob();
        if (hurtBy != null && hurtBy.isAlive() && SiegeDamage.isDefender(hurtBy) && valid(hurtBy)
                && mob.tickCount - mob.getLastHurtByMobTimestamp() < 100) {
            double max = hurtBy instanceof TurretEntity ? 10 : 16;
            if (mob.distanceToSqr(hurtBy) < max * max) return hurtBy;
        }
        double range = mob.aggroRange();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        double scanRange = Math.max(range, mob.targetsTurrets() ? 22 : 3);
        for (LivingEntity e : SiegeDamage.defendersAround(level, mob.position(), scanRange)) {
            if (!valid(e)) continue;
            double d = mob.distanceTo(e);
            boolean isTurret = e instanceof TurretEntity;
            if (isTurret && !mob.targetsTurrets() && d > 2.5) continue;
            if (!isTurret && d > range) continue;
            if (!mob.getSensing().hasLineOfSight(e)) continue;
            double score = d;
            if (isTurret && mob.targetsTurrets()) score *= 0.4;
            if (e instanceof HeroEntity) score *= 0.9;
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    private boolean valid(LivingEntity e) {
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        return e.isAlive() && (e instanceof Player || e instanceof SiegeAlly);
    }

    @Override
    public void start() {
        engagedTicks = 0;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = mob.getTarget();
        if (t == null || !valid(t) || mob.isIncapacitated() || mob.hasEffect(ModEffects.FEARED.get())) return false;
        engagedTicks++;
        if (mob.tauntedBy != null) return mob.tauntedBy.equals(t.getUUID());
        double leash = Math.max(mob.aggroRange(), mob.targetsTurrets() ? 24 : 0) + 8;
        if (mob.distanceToSqr(t) > leash * leash) return false;
        boolean recentlyHurt = mob.getLastHurtByMob() == t && mob.tickCount - mob.getLastHurtByMobTimestamp() < 100;
        return engagedTicks < 240 || recentlyHurt;
    }

    @Override
    public void stop() {
        mob.setTarget(null);
        scan = 20;
    }
}
