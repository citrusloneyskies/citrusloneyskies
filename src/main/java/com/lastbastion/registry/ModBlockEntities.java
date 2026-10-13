package com.lastbastion.registry;

import com.lastbastion.LastBastion;
import com.lastbastion.block.SiegeCoreBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, LastBastion.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<SiegeCoreBlockEntity>> SIEGE_CORE = BLOCK_ENTITIES.register("siege_core",
            () -> BlockEntityType.Builder.of(SiegeCoreBlockEntity::new, ModBlocks.SIEGE_CORE.get()).build(null));

    private ModBlockEntities() {
    }
}
