package com.tokyoghoul.registry;

import com.tokyoghoul.TokyoGhoul;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Characters;
import com.tokyoghoul.entity.GroundSpike;
import com.tokyoghoul.entity.RcProjectile;
import com.tokyoghoul.entity.TGCharacterEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** One entity type per character (so each has its own spawn egg, /summon id and model) plus the FX entities. */
public final class TGEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TokyoGhoul.MODID);

    public static final RegistryObject<EntityType<RcProjectile>> RC_PROJECTILE = ENTITIES.register("rc_projectile",
            () -> EntityType.Builder.<RcProjectile>of(RcProjectile::new, MobCategory.MISC).sized(0.4f, 0.4f).clientTrackingRange(8)
                    .updateInterval(1).build("rc_projectile"));
    public static final RegistryObject<EntityType<GroundSpike>> GROUND_SPIKE = ENTITIES.register("ground_spike",
            () -> EntityType.Builder.<GroundSpike>of(GroundSpike::new, MobCategory.MISC).sized(0.8f, 2.4f).clientTrackingRange(8)
                    .updateInterval(4).build("ground_spike"));

    /** Character id -> entity type. */
    public static final Map<String, RegistryObject<EntityType<TGCharacterEntity>>> CHARACTERS = new LinkedHashMap<>();
    private static Map<EntityType<?>, CharacterDef> byType;

    static {
        // base player size; TGCharacterEntity.getScale() applies the character's height and transformations
        for (CharacterDef d : Characters.all()) {
            CHARACTERS.put(d.id, ENTITIES.register(d.id, () -> EntityType.Builder.<TGCharacterEntity>of(TGCharacterEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f).clientTrackingRange(10).build(d.id)));
        }
    }

    private TGEntities() {
    }

    @Nullable
    public static CharacterDef defOf(EntityType<?> type) {
        if (byType == null) {
            Map<EntityType<?>, CharacterDef> m = new IdentityHashMap<>();
            for (Map.Entry<String, RegistryObject<EntityType<TGCharacterEntity>>> e : CHARACTERS.entrySet()) {
                m.put(e.getValue().get(), Characters.get(e.getKey()));
            }
            byType = m;
        }
        return byType.get(type);
    }

    @Nullable
    public static EntityType<TGCharacterEntity> typeOf(String characterId) {
        RegistryObject<EntityType<TGCharacterEntity>> r = CHARACTERS.get(characterId);
        return r == null ? null : r.get();
    }

    public static void attributes(EntityAttributeCreationEvent e) {
        for (RegistryObject<EntityType<TGCharacterEntity>> r : CHARACTERS.values()) {
            e.put(r.get(), TGCharacterEntity.createAttributes().build());
        }
    }
}
