package com.lastbastion.block;

import com.lastbastion.network.Net;
import com.lastbastion.network.OpenScreenPacket;
import com.lastbastion.registry.ModBlockEntities;
import com.lastbastion.registry.ModItems;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** The Siege Core. Placing it starts the siege; right-click opens the war room (shop, upgrades, heroes). */
public class SiegeCoreBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public SiegeCoreBlock(Properties p) {
        super(p);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SiegeCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, ModBlockEntities.SIEGE_CORE.get(), SiegeCoreBlockEntity::clientTick) : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel sl) {
            Component err = SiegeManager.tryPlaceCore(sl, pos, placer instanceof ServerPlayer sp ? sp : null);
            if (err == null && placer instanceof ServerPlayer owner) com.lastbastion.siege.Milestones.award(owner, com.lastbastion.siege.Milestones.ROOT);
            if (err == null && placer instanceof ServerPlayer sp && !sp.getInventory().contains(new ItemStack(ModItems.CODEX.get()))) {
                // a first-time commander gets the manual
                if (!sp.getInventory().add(new ItemStack(ModItems.CODEX.get()))) sp.drop(new ItemStack(ModItems.CODEX.get()), false);
                sp.displayClientMessage(Component.literal("The Siege Codex explains every foe, turret, hero and rule. Right-click it to read.")
                        .withStyle(ChatFormatting.GOLD), false);
            }
            if (err != null) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                if (placer instanceof Player p) {
                    p.displayClientMessage(err.copy().withStyle(ChatFormatting.RED), false);
                    if (!p.getAbilities().instabuild) p.getInventory().add(new ItemStack(ModItems.SIEGE_CORE.get()));
                }
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            SiegeManager.syncHud((ServerLevel) level, SiegeManager.data((ServerLevel) level));
            Net.sendTo(sp, OpenScreenPacket.core(pos, com.lastbastion.network.ActionPacket.fallenFor(sp)));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter getter, BlockPos pos) {
        if (player.level() instanceof ServerLevel sl && !SiegeManager.canBreakCore(sl)) return 0f;
        return super.getDestroyProgress(state, player, getter, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel sl) SiegeManager.onCoreRemoved(sl, pos);
        super.onRemove(state, level, pos, newState, moved);
    }
}
