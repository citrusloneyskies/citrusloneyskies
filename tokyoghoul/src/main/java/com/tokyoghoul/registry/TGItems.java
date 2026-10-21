package com.tokyoghoul.registry;

import com.tokyoghoul.TokyoGhoul;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Characters;
import com.tokyoghoul.data.KaguneType;
import com.tokyoghoul.data.Look;
import com.tokyoghoul.item.GhoulFoodItem;
import com.tokyoghoul.item.KakuhouItem;
import com.tokyoghoul.item.MaskItem;
import com.tokyoghoul.item.QuinqueItem;
import com.tokyoghoul.item.QuinqueKind;
import com.tokyoghoul.item.UtilityItems;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TGItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TokyoGhoul.MODID);

    public static final RegistryObject<Item> GHOUL_REGISTRY = ITEMS.register("ghoul_registry", UtilityItems.GhoulRegistry::new);
    public static final RegistryObject<Item> THERMOS = ITEMS.register("anteiku_thermos", UtilityItems.Thermos::new);
    public static final RegistryObject<Item> SUPPRESSANT = ITEMS.register("rc_suppressant", UtilityItems.Suppressant::new);
    public static final RegistryObject<Item> HUMAN_FLESH = ITEMS.register("human_flesh", () -> new GhoulFoodItem(GhoulFoodItem.Kind.FLESH));
    public static final RegistryObject<Item> COFFEE = ITEMS.register("anteiku_coffee", () -> new GhoulFoodItem(GhoulFoodItem.Kind.COFFEE));
    public static final RegistryObject<Item> RC_CELLS = ITEMS.register("rc_cells", () -> new GhoulFoodItem(GhoulFoodItem.Kind.RC_CELLS));

    public static final Map<KaguneType, RegistryObject<Item>> KAKUHOU = new EnumMap<>(KaguneType.class);
    public static final Map<QuinqueKind, RegistryObject<Item>> QUINQUES = new EnumMap<>(QuinqueKind.class);
    /** Mask item id -> item. */
    public static final Map<String, RegistryObject<Item>> MASKS = new LinkedHashMap<>();
    public static final RegistryObject<Item> CCG_COAT;
    public static final Map<String, RegistryObject<Item>> EGGS = new LinkedHashMap<>();

    static {
        for (KaguneType t : KaguneType.values()) {
            if (t == KaguneType.NONE) continue;
            KAKUHOU.put(t, ITEMS.register("kakuhou_" + t.id, () -> new KakuhouItem(t)));
        }
        for (QuinqueKind k : QuinqueKind.values()) QUINQUES.put(k, ITEMS.register(k.id, () -> new QuinqueItem(k)));
        mask("mask_kaneki", Look.Mask.KANEKI, 0x121214, 0xE8E2D6);
        mask("mask_rabbit", Look.Mask.RABBIT, 0x2A2140, 0x9B7BD8);
        mask("mask_gourmet", Look.Mask.GOURMET, 0xE8E0F0, 0x6A3AB0);
        mask("mask_nishiki", Look.Mask.NISHIKI, 0x2A2D52, 0x8A8FD0);
        mask("mask_crow", Look.Mask.CROW, 0x141418, 0x6A6A70);
        mask("mask_owl", Look.Mask.OWL, 0x2A2A2E, 0xD8D0B8);
        mask("mask_black_rabbit", Look.Mask.BLACK_RABBIT, 0x101014, 0xC0C0D0);
        mask("mask_jason", Look.Mask.JASON, 0xF0EEE6, 0x8A1A1A);
        mask("mask_tatara", Look.Mask.TATARA, 0xA01818, 0x2A0A0A);
        mask("mask_bandage", Look.Mask.BANDAGE, 0xD8D0C0, 0x7A1A2A);
        mask("mask_clown", Look.Mask.CLOWN, 0xF0F0F0, 0xC23B8E);
        mask("mask_hannya", Look.Mask.HANNYA, 0x8A1A1A, 0xE8E8E8);
        CCG_COAT = ITEMS.register("ccg_coat", () -> new MaskItem(Look.Mask.NONE, 0xE8E4DA, 0x1A1A1A, ArmorItem.Type.CHESTPLATE));
        for (CharacterDef d : Characters.all()) {
            int a = d.faction.color, b = d.hasKagune() ? d.kaguneColor : d.look.topColor;
            EGGS.put(d.id, ITEMS.register(d.id + "_spawn_egg", () -> new ForgeSpawnEggItem(() -> TGEntities.typeOf(d.id), a, b, new Item.Properties())));
        }
    }

    private static void mask(String id, Look.Mask m, int color, int accent) {
        MASKS.put(id, ITEMS.register(id, () -> new MaskItem(m, color, accent)));
    }

    public static Item kakuhou(KaguneType t) {
        RegistryObject<Item> r = KAKUHOU.get(t == KaguneType.NONE ? KaguneType.RINKAKU : t);
        return r.get();
    }

    private TGItems() {
    }
}
