package com.lastbastion.block;

import com.lastbastion.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds client-side animation state for the floating core crystal. */
public class SiegeCoreBlockEntity extends BlockEntity {
    public int ticks;

    public SiegeCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIEGE_CORE.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SiegeCoreBlockEntity be) {
        be.ticks++;
        if (level.random.nextInt(3) == 0) {
            double a = level.random.nextDouble() * Math.PI * 2;
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + Math.cos(a) * 0.9, pos.getY() + 1.2 + level.random.nextDouble(),
                    pos.getZ() + 0.5 + Math.sin(a) * 0.9, 0, 0.02, 0);
        }
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(2, 4, 2);
    }
}
