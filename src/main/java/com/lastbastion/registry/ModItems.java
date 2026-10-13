package com.lastbastion.registry;

import com.lastbastion.LastBastion;
import com.lastbastion.entity.hero.HeroKind;
import com.lastbastion.entity.turret.TurretKind;
import com.lastbastion.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LastBastion.MODID);

    public static final RegistryObject<Item> SIEGE_CORE = ITEMS.register("siege_core",
            () -> new BlockItem(ModBlocks.SIEGE_CORE.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final Map<TurretKind, RegistryObject<Item>> TURRET_KITS = new EnumMap<>(TurretKind.class);
    public static final Map<HeroKind, RegistryObject<Item>> HERO_SIGILS = new EnumMap<>(HeroKind.class);
    public static final Map<GunItem.GunKind, RegistryObject<Item>> GUNS = new EnumMap<>(GunItem.GunKind.class);
    public static final Map<GrenadeItem.Kind, RegistryObject<Item>> GRENADES = new EnumMap<>(GrenadeItem.Kind.class);
    public static final Map<ConsumableItem.Kind, RegistryObject<Item>> CONSUMABLES = new EnumMap<>(ConsumableItem.Kind.class);
    public static final Map<SupportCallItem.Kind, RegistryObject<Item>> SUPPORT = new EnumMap<>(SupportCallItem.Kind.class);
    public static final RegistryObject<Item> VOIDEDGE = ITEMS.register("voidedge", () -> new VoidedgeItem(new Item.Properties()));

    static {
        for (TurretKind k : TurretKind.values()) {
            TURRET_KITS.put(k, ITEMS.register("turret_kit_" + k.id, () -> new TurretKitItem(new Item.Properties().stacksTo(16), k)));
        }
        for (HeroKind k : HeroKind.values()) {
            HERO_SIGILS.put(k, ITEMS.register("sigil_" + k.id, () -> new HeroSigilItem(new Item.Properties().stacksTo(1), k)));
        }
        for (GunItem.GunKind k : GunItem.GunKind.values()) {
            GUNS.put(k, ITEMS.register("gun_" + k.name().toLowerCase(Locale.ROOT), () -> new GunItem(new Item.Properties(), k)));
        }
        for (GrenadeItem.Kind k : GrenadeItem.Kind.values()) {
            GRENADES.put(k, ITEMS.register("grenade_" + k.name().toLowerCase(Locale.ROOT), () -> new GrenadeItem(new Item.Properties(), k)));
        }
        for (ConsumableItem.Kind k : ConsumableItem.Kind.values()) {
            CONSUMABLES.put(k, ITEMS.register(k.name().toLowerCase(Locale.ROOT), () -> new ConsumableItem(new Item.Properties(), k)));
        }
        for (SupportCallItem.Kind k : SupportCallItem.Kind.values()) {
            SUPPORT.put(k, ITEMS.register("call_" + k.name().toLowerCase(Locale.ROOT), () -> new SupportCallItem(new Item.Properties(), k)));
        }
    }

    public static final RegistryObject<Item> BARRICADE_WOOD = block("barricade_wood", ModBlocks.BARRICADE_WOOD);
    public static final RegistryObject<Item> BARRICADE_STONE = block("barricade_stone", ModBlocks.BARRICADE_STONE);
    public static final RegistryObject<Item> BARRICADE_IRON = block("barricade_iron", ModBlocks.BARRICADE_IRON);
    public static final RegistryObject<Item> BARRICADE_RUNIC = block("barricade_runic", ModBlocks.BARRICADE_RUNIC);
    public static final RegistryObject<Item> SPIKE_TRAP = block("spike_trap", ModBlocks.SPIKE_TRAP);
    public static final RegistryObject<Item> LANDMINE = block("landmine", ModBlocks.LANDMINE);
    public static final RegistryObject<Item> MEDIC_SHRINE = block("medic_shrine", ModBlocks.MEDIC_SHRINE);
    public static final RegistryObject<Item> SOUL_HARVESTER = block("soul_harvester", ModBlocks.SOUL_HARVESTER);
    public static final RegistryObject<Item> REPAIR_PYLON = block("repair_pylon", ModBlocks.REPAIR_PYLON);

    private static RegistryObject<Item> block(String name, RegistryObject<net.minecraft.world.level.block.Block> b) {
        return ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties()));
    }

    private ModItems() {
    }
}
