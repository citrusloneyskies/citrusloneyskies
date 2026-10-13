package com.lastbastion.entity.ai;

import com.lastbastion.block.BarricadeBlock;
import com.lastbastion.block.SiegeCoreBlock;
import com.lastbastion.config.SiegeConfig;
import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The horde's primary objective: march on the Siege Core and smash it.
 * <ul>
 *   <li>Long marches are split into ~16 block waypoints so pathfinding stays cheap.</li>
 *   <li>Each zombie picks its own approach angle so the horde surrounds the core instead of queueing.</li>
 *   <li>Stuck detection: if a zombie makes no progress it identifies the blocking block toward its next path
 *       node and breaches it (crack overlay, sounds). Breach speed scales with block hardness and the
 *       zombie's breach power; Brutes and bosses can breach very hard blocks.</li>
 *   <li>If hopelessly stuck for 12s, it scrambles forward (prevents permanent hang-ups on odd terrain).</li>
 * </ul>
 */
public class AssaultCoreGoal extends Goal {
    private final SiegeZombie mob;
    private final double approachAngle;
    private int repath;
    private int swing = -1;
    private int cooldown;
    private int stuckTicks;
    private Vec3 lastPos;
    private BlockPos breaching;
    private float breachProgress;
    private int lastStage = -1;

    public AssaultCoreGoal(SiegeZombie mob) {
        this.mob = mob;
        this.approachAngle = mob.getRandom().nextDouble() * Math.PI * 2;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private BlockPos core() {
        return SiegeManager.corePos((ServerLevel) mob.level());
    }

    @Override
    public boolean canUse() {
        return mob.getTarget() == null && !mob.isIncapacitated() && core() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        repath = 0;
        lastPos = mob.position();
        stuckTicks = 0;
    }

    @Override
    public void stop() {
        clearBreach();
        swing = -1;
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        BlockPos core = core();
        if (core == null) return;
        ServerLevel level = (ServerLevel) mob.level();
        Vec3 c = Vec3.atCenterOf(core);
        double hd = Math.sqrt(sq(mob.getX() - c.x) + sq(mob.getZ() - c.z));
        double dy = Math.abs(mob.getY() - core.getY());
        double reach = mob.getBbWidth() * 0.5 + 1.3;
        if (cooldown > 0) cooldown--;

        if (swing >= 0) {
            swing++;
            mob.getLookControl().setLookAt(c.x, c.y, c.z);
            if (swing == mob.attackWindup() && hd <= reach + 0.8) mob.onCoreStrike(level, core);
            if (swing >= mob.attackWindup() + 5) swing = -1;
            return;
        }

        if (hd <= reach && dy < 2.5) {
            mob.getNavigation().stop();
            clearBreach();
            mob.getLookControl().setLookAt(c.x, c.y, c.z);
            if (cooldown <= 0) {
                swing = 0;
                cooldown = mob.attackCooldown() + mob.attackWindup();
                mob.anim.play(SiegeZombie.ANIM_CORE);
            }
            return;
        }

        if (breaching != null) {
            tickBreach(level);
            return;
        }

        if (mob.hasEffect(ModEffects.DISORIENTED.get())) {
            if (mob.getNavigation().isDone() || mob.getRandom().nextInt(20) == 0) {
                Vec3 r = mob.position().add(mob.getRandom().nextGaussian() * 4, 0, mob.getRandom().nextGaussian() * 4);
                mob.getNavigation().moveTo(r.x, r.y, r.z, 0.8);
            }
            return;
        }

        if (--repath <= 0 || mob.getNavigation().isDone()) {
            repath = 20 + mob.getRandom().nextInt(15);
            Vec3 wp = waypoint(level, c, hd);
            mob.getNavigation().moveTo(wp.x, wp.y, wp.z, 1.0);
        }

        if (mob.tickCount % 10 == 0) {
            if (lastPos != null && lastPos.distanceToSqr(mob.position()) < 0.09) {
                stuckTicks += 10;
            } else {
                stuckTicks = 0;
            }
            lastPos = mob.position();
            if (stuckTicks >= 30) findBreach(level, c);
            if (stuckTicks >= 240) scramble(level, c);
        }
    }

    private Vec3 waypoint(ServerLevel level, Vec3 c, double hd) {
        if (hd > 22) {
            Vec3 dir = new Vec3(c.x - mob.getX(), 0, c.z - mob.getZ()).normalize();
            // slight lateral wobble keeps columns from forming
            Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(Math.sin(approachAngle + mob.tickCount * 0.01) * 3);
            Vec3 p = mob.position().add(dir.scale(16)).add(side);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(p.x), (int) Math.floor(p.z));
            return new Vec3(p.x, y, p.z);
        }
        double r = 1.2 + mob.getBbWidth() * 0.5;
        Vec3 dir = new Vec3(mob.getX() - c.x, 0, mob.getZ() - c.z).normalize();
        double a = Math.atan2(dir.z, dir.x) * 0.6 + approachAngle * 0.4;
        return new Vec3(c.x + Math.cos(a) * r, c.y - 0.5, c.z + Math.sin(a) * r);
    }

    private void findBreach(ServerLevel level, Vec3 core) {
        Vec3 aim = core;
        Path path = mob.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            Node n = path.getNextNode();
            aim = new Vec3(n.x + 0.5, n.y, n.z + 0.5);
            if (aim.distanceToSqr(mob.position()) < 0.5) aim = core;
        }
        Vec3 dir = new Vec3(aim.x - mob.getX(), 0, aim.z - mob.getZ());
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(core.x - mob.getX(), 0, core.z - mob.getZ());
        dir = dir.normalize();
        double ahead = mob.getBbWidth() * 0.5 + 0.6;
        int height = (int) Math.ceil(mob.getBbHeight());
        for (double side : new double[]{0, 0.45, -0.45}) {
            Vec3 lat = new Vec3(-dir.z, 0, dir.x).scale(side * mob.getBbWidth());
            BlockPos front = BlockPos.containing(mob.getX() + dir.x * ahead + lat.x, mob.getY() + 0.1, mob.getZ() + dir.z * ahead + lat.z);
            for (int up = Math.min(height, 3); up >= 0; up--) {
                BlockPos p = front.above(up);
                if (canBreach(level, p)) {
                    breaching = p;
                    breachProgress = 0;
                    lastStage = -1;
                    mob.getNavigation().stop();
                    return;
                }
            }
        }
        // stuck under a ledge? try the block above our head
        BlockPos head = BlockPos.containing(mob.getX(), mob.getY() + mob.getBbHeight() + 0.2, mob.getZ());
        if (mob.getY() < core.y - 1.5 && canBreach(level, head)) {
            breaching = head;
            breachProgress = 0;
        }
    }

    private boolean canBreach(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (s.isAir() || s.getCollisionShape(level, p).isEmpty()) return false;
        if (s.getBlock() instanceof SiegeCoreBlock) return false;
        float hardness = s.getDestroySpeed(level, p);
        if (hardness < 0) return false;
        if (s.getBlock() instanceof BarricadeBlock) return true;
        if (!SiegeConfig.ZOMBIES_BREAK_BLOCKS.get()) return false;
        return hardness <= SiegeConfig.MAX_BREAKABLE_HARDNESS.get() || mob.canBreachHard();
    }

    private void tickBreach(ServerLevel level) {
        BlockPos p = breaching;
        BlockState s = level.getBlockState(p);
        if (s.isAir() || mob.position().distanceToSqr(Vec3.atCenterOf(p)) > 9 + mob.getBbWidth() * mob.getBbWidth()) {
            clearBreach();
            stuckTicks = 0;
            return;
        }
        mob.getLookControl().setLookAt(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
        float hardness = Math.max(0.2f, s.getDestroySpeed(level, p));
        if (s.getBlock() instanceof BarricadeBlock b) hardness = b.breachHardness();
        breachProgress += mob.breachPower() / (hardness * 30f + 10f);
        if (mob.tickCount % 15 == 0) {
            mob.anim.play(SiegeZombie.ANIM_ATTACK);
            if (s.getBlock() instanceof BarricadeBlock b) b.onBreachTick(level, p, mob);
            boolean metal = hardness > 4;
            level.playSound(null, p, metal ? SoundEvents.ZOMBIE_ATTACK_IRON_DOOR : SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
                    SoundSource.HOSTILE, 0.6f, 0.8f + mob.getRandom().nextFloat() * 0.4f);
        }
        int stage = (int) (breachProgress * 10);
        if (stage != lastStage) {
            level.destroyBlockProgress(mob.getId(), p, Math.min(9, stage));
            lastStage = stage;
        }
        if (breachProgress >= 1f) {
            if (s.getBlock() instanceof BarricadeBlock b) {
                b.onBreached(level, p, s, mob);
            } else {
                level.destroyBlock(p, false, mob);
            }
            clearBreach();
            stuckTicks = 0;
            repath = 0;
        }
    }

    private void scramble(ServerLevel level, Vec3 core) {
        Vec3 dir = new Vec3(core.x - mob.getX(), 0, core.z - mob.getZ()).normalize().scale(1.5);
        double nx = mob.getX() + dir.x, nz = mob.getZ() + dir.z;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(nx), (int) Math.floor(nz));
        if (Math.abs(y - mob.getY()) < 4) mob.teleportTo(nx, y, nz);
        stuckTicks = 0;
    }

    private void clearBreach() {
        if (breaching != null) {
            mob.level().destroyBlockProgress(mob.getId(), breaching, -1);
            breaching = null;
        }
    }

    private static double sq(double d) {
        return d * d;
    }
}
