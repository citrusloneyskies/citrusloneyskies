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
        float hp = com.lastbastion.client.ClientSiegeState.active() ? com.lastbastion.client.ClientSiegeState.coreFraction() : 1f;
        int color = hp > 0.6f ? 0x6FE3FF : hp > 0.3f ? 0xFFB040 : 0xFF4030;
        if (level.random.nextInt(2) == 0) {
            double a = level.random.nextDouble() * Math.PI * 2;
            level.addParticle(com.lastbastion.fx.FxParticleOptions.of(com.lastbastion.fx.FxStyle.GLOW, color, 0.12f, 30),
                    pos.getX() + 0.5 + Math.cos(a) * 0.7, pos.getY() + 0.25, pos.getZ() + 0.5 + Math.sin(a) * 0.7, 0, 0.03, 0);
        }
        if (level.random.nextInt(4) == 0) {
            double a = level.random.nextDouble() * Math.PI * 2;
            level.addParticle(com.lastbastion.fx.FxParticleOptions.of(com.lastbastion.fx.FxStyle.STAR, 0xE0FFFF, 0.08f, 20),
                    pos.getX() + 0.5 + Math.cos(a) * 0.4, pos.getY() + 2.3 + level.random.nextDouble() * 0.4, pos.getZ() + 0.5 + Math.sin(a) * 0.4, 0, 0.01, 0);
        }
        if (hp < 0.3f && level.random.nextInt(3) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 2.2, pos.getZ() + 0.5, 0, 0.04, 0);
            level.addParticle(com.lastbastion.fx.FxParticleOptions.of(com.lastbastion.fx.FxStyle.SPARK, 0xFFB060, 0.08f, 14),
                    pos.getX() + 0.5, pos.getY() + 2.3, pos.getZ() + 0.5, (level.random.nextDouble() - 0.5) * 0.3, 0.2, (level.random.nextDouble() - 0.5) * 0.3);
        }
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(2, 7, 2);
    }
}
