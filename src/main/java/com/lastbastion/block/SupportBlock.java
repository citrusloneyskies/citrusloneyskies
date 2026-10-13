package com.lastbastion.block;

import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.turret.TurretEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Support structures:
 * <ul>
 *   <li><b>Medic Shrine</b> - heals players and heroes within 7 blocks and cleanses harmful effects.</li>
 *   <li><b>Soul Harvester</b> - +25% credits for kills within 16 blocks.</li>
 *   <li><b>Repair Pylon</b> - slowly repairs turrets within 8 blocks.</li>
 * </ul>
 */
public class SupportBlock extends Block {
    public enum Kind {MEDIC, HARVESTER, REPAIR}

    /** Positions of active Soul Harvesters (server-side, refreshed by scheduled ticks). */
    public static final Set<BlockPos> HARVESTERS = Collections.synchronizedSet(new HashSet<>());
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);
    public final Kind kind;

    public SupportBlock(Properties p, Kind kind) {
        super(p);
        this.kind = kind;
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!level.isClientSide) level.scheduleTick(pos, this, 40);
        if (kind == Kind.HARVESTER && !level.isClientSide) HARVESTERS.add(pos.immutable());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (kind == Kind.HARVESTER) HARVESTERS.remove(pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, 40);
        Vec3 c = Vec3.atCenterOf(pos);
        switch (kind) {
            case MEDIC -> {
                Fx.ring(level, c.add(0, -0.4, 0), 7, ParticleTypes.HAPPY_VILLAGER, 24);
                for (LivingEntity e : SiegeDamage.defendersAround(level, c, 7)) {
                    if (e instanceof TurretEntity) continue;
                    e.heal(3f);
                    e.getActiveEffects().stream().filter(i -> !i.getEffect().isBeneficial()).map(MobEffectInstance::getEffect)
                            .toList().forEach(e::removeEffect);
                }
            }
            case HARVESTER -> {
                HARVESTERS.add(pos.immutable());
                level.sendParticles(ParticleTypes.SOUL, c.x, c.y + 0.8, c.z, 3, 0.3, 0.3, 0.3, 0.01);
            }
            case REPAIR -> {
                for (LivingEntity e : SiegeDamage.defendersAround(level, c, 8)) {
                    if (e instanceof TurretEntity t && t.getHealth() < t.getMaxHealth()) {
                        t.heal(4f);
                        Fx.line(level, c.add(0, 0.6, 0), t.position().add(0, 1, 0), Fx.dust(0xFFC040, 0.6f), 0.5);
                    }
                }
            }
        }
    }

    public static boolean harvesterNear(Vec3 at) {
        synchronized (HARVESTERS) {
            for (BlockPos p : HARVESTERS) if (p.closerToCenterThan(at, 16)) return true;
        }
        return false;
    }
}
