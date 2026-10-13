package com.lastbastion.registry;

import com.lastbastion.LastBastion;
import com.lastbastion.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LastBastion.MODID);

    public static final RegistryObject<Block> SIEGE_CORE = BLOCKS.register("siege_core", () -> new SiegeCoreBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).strength(50f, 3_600_000f).lightLevel(s -> 15)
                    .noOcclusion().sound(SoundType.AMETHYST)));

    public static final RegistryObject<Block> BARRICADE_WOOD = BLOCKS.register("barricade_wood", () -> new BarricadeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2f, 3f).sound(SoundType.WOOD), BarricadeBlock.Tier.WOOD, null));
    public static final RegistryObject<Block> BARRICADE_STONE = BLOCKS.register("barricade_stone", () -> new BarricadeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3f, 9f).sound(SoundType.STONE), BarricadeBlock.Tier.STONE, ModBlocks.BARRICADE_WOOD));
    public static final RegistryObject<Block> BARRICADE_IRON = BLOCKS.register("barricade_iron", () -> new BarricadeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5f, 12f).sound(SoundType.METAL), BarricadeBlock.Tier.IRON, ModBlocks.BARRICADE_STONE));
    public static final RegistryObject<Block> BARRICADE_RUNIC = BLOCKS.register("barricade_runic", () -> new BarricadeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(8f, 1200f).lightLevel(s -> 7).sound(SoundType.DEEPSLATE),
            BarricadeBlock.Tier.RUNIC, ModBlocks.BARRICADE_IRON));

    public static final RegistryObject<Block> SPIKE_TRAP = BLOCKS.register("spike_trap", () -> new SpikeTrapBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f).noOcclusion().sound(SoundType.METAL)));
    public static final RegistryObject<Block> LANDMINE = BLOCKS.register("landmine", () -> new LandmineBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5f).noOcclusion().sound(SoundType.METAL)));
    public static final RegistryObject<Block> MEDIC_SHRINE = BLOCKS.register("medic_shrine", () -> new SupportBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(3f).lightLevel(s -> 10).noOcclusion(), SupportBlock.Kind.MEDIC));
    public static final RegistryObject<Block> SOUL_HARVESTER = BLOCKS.register("soul_harvester", () -> new SupportBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3f).lightLevel(s -> 9).noOcclusion().sound(SoundType.SOUL_SOIL),
            SupportBlock.Kind.HARVESTER));
    public static final RegistryObject<Block> REPAIR_PYLON = BLOCKS.register("repair_pylon", () -> new SupportBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3f).lightLevel(s -> 6).noOcclusion().sound(SoundType.METAL),
            SupportBlock.Kind.REPAIR));

    private ModBlocks() {
    }
}
