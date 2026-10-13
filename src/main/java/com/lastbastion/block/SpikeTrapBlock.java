package com.lastbastion.block;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** Floor of iron spikes: damages, bleeds and slows enemies walking over it. Defenders are unharmed. */
public class SpikeTrapBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    public SpikeTrapBlock(Properties p) {
        super(p);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity le) || !SiegeDamage.isEnemy(le)) return;
        le.makeStuckInBlock(state, new Vec3(0.6, 0.9, 0.6));
        if (le.tickCount % 10 == 0) {
            SiegeDamage.deal(le, null, null, 3f, Element.PHYSICAL, false);
            SiegeDamage.applyStacking(le, ModEffects.BLEEDING.get(), 60, 2);
        }
    }

    @Nullable
    @Override
    public BlockPathTypes getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable net.minecraft.world.entity.Mob mob) {
        return BlockPathTypes.OPEN;
    }
}
