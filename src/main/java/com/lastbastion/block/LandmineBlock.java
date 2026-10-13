package com.lastbastion.block;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** Single-use pressure mine: detonates under the first enemy that steps on it. */
public class LandmineBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 2, 12);

    public LandmineBlock(Properties p) {
        super(p);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return Shapes.empty();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel sl) || !(entity instanceof LivingEntity le) || !SiegeDamage.isEnemy(le)) return;
        level.removeBlock(pos, false);
        Vec3 c = Vec3.atBottomCenterOf(pos);
        Fx.explosion(sl, c, 3);
        SiegeDamage.areaDamage(sl, c, 3.5, 30f, Element.EXPLOSIVE, null, null, true);
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, c, 3.5)) SiegeDamage.push(e, c, 0.8, 0.6);
    }

    @Nullable
    @Override
    public BlockPathTypes getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable net.minecraft.world.entity.Mob mob) {
        return BlockPathTypes.OPEN;
    }
}
