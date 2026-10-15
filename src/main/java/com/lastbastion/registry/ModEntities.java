package com.lastbastion.registry;

import com.lastbastion.LastBastion;
import com.lastbastion.entity.boss.*;
import com.lastbastion.entity.hero.*;
import com.lastbastion.entity.misc.DropPodEntity;
import com.lastbastion.entity.misc.SentryDroneEntity;
import com.lastbastion.entity.misc.TitanExoEntity;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.entity.turret.TurretKind;
import com.lastbastion.entity.zombie.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LastBastion.MODID);

    public static final Map<ZombieKind, RegistryObject<? extends EntityType<? extends SiegeZombie>>> ZOMBIES = new EnumMap<>(ZombieKind.class);
    public static final Map<BossKind, RegistryObject<? extends EntityType<? extends SiegeBoss>>> BOSSES = new EnumMap<>(BossKind.class);
    public static final Map<TurretKind, RegistryObject<EntityType<TurretEntity>>> TURRETS = new EnumMap<>(TurretKind.class);
    public static final Map<HeroKind, RegistryObject<? extends EntityType<? extends HeroEntity>>> HEROES = new EnumMap<>(HeroKind.class);

    public static final RegistryObject<EntityType<SiegeProjectile>> PROJECTILE = reg("projectile", SiegeProjectile::new, MobCategory.MISC, 0.35f, 0.35f, 8);
    public static final RegistryObject<EntityType<HazardZone>> HAZARD = reg("hazard_zone", HazardZone::new, MobCategory.MISC, 0.5f, 0.5f, 10);
    public static final RegistryObject<EntityType<PhylacteryEntity>> PHYLACTERY = reg("phylactery", PhylacteryEntity::new, MobCategory.MONSTER, 0.8f, 1.4f, 10);
    public static final RegistryObject<EntityType<KageCloneEntity>> KAGE_CLONE = reg("kage_clone", KageCloneEntity::new, MobCategory.MISC, 0.6f, 1.85f, 10);
    public static final RegistryObject<EntityType<SentryDroneEntity>> DRONE = reg("sentry_drone", SentryDroneEntity::new, MobCategory.MISC, 0.6f, 0.5f, 10);
    public static final RegistryObject<EntityType<DropPodEntity>> DROP_POD = reg("drop_pod", DropPodEntity::new, MobCategory.MISC, 1.6f, 3.0f, 16);
    public static final RegistryObject<EntityType<TitanExoEntity>> TITAN_EXO = reg("titan_exosuit", TitanExoEntity::new, MobCategory.MISC, 1.6f, 3.4f, 10);
    public static final RegistryObject<EntityType<com.lastbastion.entity.misc.SupplyCrateEntity>> SUPPLY_CRATE =
            reg("supply_crate", com.lastbastion.entity.misc.SupplyCrateEntity::new, MobCategory.MISC, 1.0f, 1.0f, 16);
    public static final RegistryObject<EntityType<RiftEntity>> RIFT = reg("corruption_rift", RiftEntity::new, MobCategory.MONSTER, 1.6f, 3.4f, 16);

    static {
        ZOMBIES.put(ZombieKind.SHAMBLER, reg("shambler", ShamblerZombie::new, MobCategory.MONSTER, 0.6f, 1.95f, 10));
        ZOMBIES.put(ZombieKind.RUNNER, reg("runner", RunnerZombie::new, MobCategory.MONSTER, 0.6f, 1.8f, 10));
        ZOMBIES.put(ZombieKind.SPITTER, reg("spitter", SpitterZombie::new, MobCategory.MONSTER, 0.7f, 1.95f, 10));
        ZOMBIES.put(ZombieKind.BRUTE, reg("brute", BruteZombie::new, MobCategory.MONSTER, 1.3f, 2.9f, 10));
        ZOMBIES.put(ZombieKind.BLOATER, reg("bloater", BloaterZombie::new, MobCategory.MONSTER, 1.2f, 2.2f, 10));
        ZOMBIES.put(ZombieKind.LEAPER, reg("leaper", LeaperZombie::new, MobCategory.MONSTER, 0.6f, 2.0f, 10));
        ZOMBIES.put(ZombieKind.BULWARK, reg("bulwark", BulwarkZombie::new, MobCategory.MONSTER, 0.8f, 2.1f, 10));
        ZOMBIES.put(ZombieKind.BURROWER, reg("burrower", BurrowerZombie::new, MobCategory.MONSTER, 0.7f, 1.6f, 10));
        ZOMBIES.put(ZombieKind.SHRIEKER, reg("shrieker", ShriekerZombie::new, MobCategory.MONSTER, 0.6f, 2.1f, 10));
        ZOMBIES.put(ZombieKind.WRAITH, reg("wraith", WraithZombie::new, MobCategory.MONSTER, 0.6f, 2.0f, 10));
        ZOMBIES.put(ZombieKind.WRECKER, reg("wrecker", WreckerZombie::new, MobCategory.MONSTER, 0.9f, 2.3f, 10));
        ZOMBIES.put(ZombieKind.NECROMANCER, reg("necromancer", NecromancerZombie::new, MobCategory.MONSTER, 0.6f, 2.1f, 10));
        ZOMBIES.put(ZombieKind.CRAWLER, reg("crawler", CrawlerZombie::new, MobCategory.MONSTER, 0.8f, 0.7f, 10));
        ZOMBIES.put(ZombieKind.INCINERATOR, reg("incinerator", IncineratorZombie::new, MobCategory.MONSTER, 0.75f, 2.0f, 10));
        ZOMBIES.put(ZombieKind.ABOMINATION, reg("abomination", AbominationZombie::new, MobCategory.MONSTER, 1.6f, 3.2f, 10));

        BOSSES.put(BossKind.GOREHOUND, reg("gorehound", GorehoundBoss::new, MobCategory.MONSTER, 1.8f, 3.6f, 16));
        BOSSES.put(BossKind.PLAGUE_MOTHER, reg("plague_mother", PlagueMotherBoss::new, MobCategory.MONSTER, 3.0f, 2.8f, 16));
        BOSSES.put(BossKind.MALGRAVE, reg("malgrave", MalgraveBoss::new, MobCategory.MONSTER, 1.1f, 3.0f, 16));
        BOSSES.put(BossKind.OSSUARY, reg("ossuary_colossus", OssuaryBoss::new, MobCategory.MONSTER, 3.2f, 8.0f, 16));
        BOSSES.put(BossKind.VARKHUL, reg("varkhul", VarkhulBoss::new, MobCategory.MONSTER, 1.0f, 3.0f, 16));
        BOSSES.put(BossKind.MORVATHRAX, reg("morvathrax", MorvathraxBoss::new, MobCategory.MONSTER, 5.0f, 3.5f, 20));

        for (TurretKind k : TurretKind.values()) {
            TURRETS.put(k, reg("turret_" + k.id, (EntityType<TurretEntity> t, net.minecraft.world.level.Level l) -> new TurretEntity(t, l, k),
                    MobCategory.MISC, 0.95f, 1.6f, 10));
        }

        HEROES.put(HeroKind.ASHFANG, reg("ashfang", AshfangHero::new, MobCategory.CREATURE, 1.15f, 1.35f, 10));
        HEROES.put(HeroKind.SYLVA, reg("sylva", SylvaHero::new, MobCategory.CREATURE, 0.6f, 1.9f, 10));
        HEROES.put(HeroKind.BASTION, reg("bastion", BastionHero::new, MobCategory.CREATURE, 1.7f, 3.3f, 10));
        HEROES.put(HeroKind.ALDRIC, reg("aldric", AldricHero::new, MobCategory.CREATURE, 0.75f, 2.05f, 10));
        HEROES.put(HeroKind.MORGANA, reg("morgana", MorganaHero::new, MobCategory.CREATURE, 0.6f, 1.95f, 10));
        HEROES.put(HeroKind.KAGE, reg("kage", KageHero::new, MobCategory.CREATURE, 0.6f, 1.9f, 10));
        HEROES.put(HeroKind.AEGIS7, reg("aegis7", Aegis7Hero::new, MobCategory.CREATURE, 2.0f, 4.3f, 12));
        HEROES.put(HeroKind.ASTERION, reg("asterion", AsterionHero::new, MobCategory.CREATURE, 1.4f, 3.1f, 12));
        HEROES.put(HeroKind.ANUBIS, reg("anubis", AnubisHero::new, MobCategory.CREATURE, 0.9f, 2.7f, 12));
        HEROES.put(HeroKind.ZEUS, reg("zeus", ZeusHero::new, MobCategory.CREATURE, 0.95f, 2.8f, 12));

        HEROES.put(HeroKind.LUCIFER, reg("lucifer", LuciferDevil::new, MobCategory.CREATURE, 0.9f, 3.0f, 16));
        HEROES.put(HeroKind.BEELZEBUB, reg("beelzebub", BeelzebubDevil::new, MobCategory.CREATURE, 1.2f, 2.7f, 16));
        HEROES.put(HeroKind.ABADDON, reg("abaddon", AbaddonDevil::new, MobCategory.CREATURE, 1.4f, 3.6f, 16));
        HEROES.put(HeroKind.LEVIATHAN, reg("leviathan", LeviathanDevil::new, MobCategory.CREATURE, 2.0f, 3.2f, 16));
        HEROES.put(HeroKind.MOLOCH, reg("moloch", MolochDevil::new, MobCategory.CREATURE, 1.5f, 3.4f, 16));
    }

    public static final RegistryObject<EntityType<HellspawnEntity>> IMP = reg("imp",
            (EntityType<HellspawnEntity> t, net.minecraft.world.level.Level l) -> new HellspawnEntity(t, l, HellspawnEntity.Variant.IMP),
            MobCategory.CREATURE, 0.6f, 1.1f, 10);
    public static final RegistryObject<EntityType<HellspawnEntity>> HELLHOUND = reg("hellhound",
            (EntityType<HellspawnEntity> t, net.minecraft.world.level.Level l) -> new HellspawnEntity(t, l, HellspawnEntity.Variant.HELLHOUND),
            MobCategory.CREATURE, 1.1f, 1.2f, 10);
    public static final RegistryObject<EntityType<HellspawnEntity>> FIEND = reg("fiend",
            (EntityType<HellspawnEntity> t, net.minecraft.world.level.Level l) -> new HellspawnEntity(t, l, HellspawnEntity.Variant.FIEND),
            MobCategory.CREATURE, 1.4f, 2.6f, 10);

    private static <T extends Entity> RegistryObject<EntityType<T>> reg(String name, EntityType.EntityFactory<T> factory, MobCategory cat,
                                                                       float w, float h, int range) {
        return ENTITIES.register(name, () -> EntityType.Builder.of(factory, cat).sized(w, h).clientTrackingRange(range)
                .build(LastBastion.MODID + ":" + name));
    }

    public static Supplier<? extends EntityType<? extends SiegeZombie>> zombie(ZombieKind k) {
        return ZOMBIES.get(k);
    }

    public static Supplier<? extends EntityType<? extends SiegeBoss>> boss(BossKind k) {
        return BOSSES.get(k);
    }

    public static Supplier<? extends EntityType<? extends TurretEntity>> turret(TurretKind k) {
        return TURRETS.get(k);
    }

    public static Supplier<? extends EntityType<? extends HeroEntity>> hero(HeroKind k) {
        return HEROES.get(k);
    }

    public static void attributes(EntityAttributeCreationEvent e) {
        ZOMBIES.forEach((k, t) -> e.put(t.get(), SiegeZombie.createAttributes(k).build()));
        BOSSES.forEach((k, t) -> e.put(t.get(), SiegeBoss.createAttributes(k).build()));
        TURRETS.forEach((k, t) -> e.put(t.get(), TurretEntity.createAttributes(k).build()));
        HEROES.forEach((k, t) -> e.put(t.get(), (k.devil ? DevilEntity.createDevilAttributes(k) : HeroEntity.createAttributes(k)).build()));
        e.put(IMP.get(), HellspawnEntity.createAttributes(HellspawnEntity.Variant.IMP).build());
        e.put(HELLHOUND.get(), HellspawnEntity.createAttributes(HellspawnEntity.Variant.HELLHOUND).build());
        e.put(FIEND.get(), HellspawnEntity.createAttributes(HellspawnEntity.Variant.FIEND).build());
        e.put(KAGE_CLONE.get(), HeroEntity.createAttributes(HeroKind.KAGE).build());
        e.put(PHYLACTERY.get(), PhylacteryEntity.createAttributes().build());
        e.put(DRONE.get(), SentryDroneEntity.createAttributes().build());
        e.put(TITAN_EXO.get(), TitanExoEntity.createAttributes().build());
        e.put(RIFT.get(), RiftEntity.createAttributes().build());
    }

    private ModEntities() {
    }
}
