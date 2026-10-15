package com.lastbastion.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SiegeConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue STARTING_CREDITS;
    public static final ForgeConfigSpec.DoubleValue CREDIT_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue DIFFICULTY;
    public static final ForgeConfigSpec.IntValue SPAWN_RADIUS;
    public static final ForgeConfigSpec.IntValue BUILD_RADIUS;
    public static final ForgeConfigSpec.BooleanValue ZOMBIES_BREAK_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue MAX_BREAKABLE_HARDNESS;
    public static final ForgeConfigSpec.BooleanValue SHARED_CREDITS;
    public static final ForgeConfigSpec.BooleanValue HARSH_DEFEAT;
    public static final ForgeConfigSpec.DoubleValue SALVAGE_FRACTION;
    public static final ForgeConfigSpec.IntValue BASE_CORE_HEALTH;
    public static final ForgeConfigSpec.IntValue MAX_ALIVE;
    public static final ForgeConfigSpec.BooleanValue NIGHT_MODIFIERS;
    public static final ForgeConfigSpec.IntValue PLAYER_PRESENCE_RADIUS;
    public static final ForgeConfigSpec.BooleanValue WAR_EVENTS;
    public static final ForgeConfigSpec.BooleanValue DOWNED;
    public static final ForgeConfigSpec.IntValue BLEED_OUT_SECONDS;
    public static final ForgeConfigSpec.EnumValue<RuleEditing> RULES_EDITING;
    public static final ForgeConfigSpec.BooleanValue SUMMON_ANYWHERE;
    public static final ForgeConfigSpec.IntValue MAX_HEROES;
    public static final ForgeConfigSpec.BooleanValue DEVIL_BLOOD_PRICE;

    /** Who may change a world's siege rules in game. */
    public enum RuleEditing {OWNER_OR_OP, OP_ONLY, ANYONE, LOCKED}

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("economy");
        STARTING_CREDITS = b.comment("Credits each player starts a fresh siege with.").defineInRange("startingCredits", 400, 0, 1_000_000);
        CREDIT_MULTIPLIER = b.comment("Multiplier applied to every credit reward.").defineInRange("creditMultiplier", 1.0, 0.05, 100.0);
        SHARED_CREDITS = b.comment("If true, all players share one credit pool instead of personal wallets.").define("sharedCredits", false);
        b.pop();

        b.push("siege");
        DIFFICULTY = b.comment("Global difficulty multiplier for wave size, enemy health and damage. 0.5 = casual, 1 = normal, 1.5 = brutal.")
                .defineInRange("difficulty", 1.0, 0.1, 10.0);
        SPAWN_RADIUS = b.comment("Distance from the core at which waves spawn (360 degrees around it).").defineInRange("spawnRadius", 44, 16, 96);
        BUILD_RADIUS = b.comment("Turrets, structures and hero summons must be within this many blocks of the core.").defineInRange("buildRadius", 40, 8, 128);
        ZOMBIES_BREAK_BLOCKS = b.comment("Zombies breach any block in their way (barricades are always breakable).").define("zombiesBreakBlocks", true);
        MAX_BREAKABLE_HARDNESS = b.comment("Blocks harder than this are never breached by regular zombies (obsidian = 50).")
                .defineInRange("maxBreakableHardness", 25.0, 0.0, 1000.0);
        HARSH_DEFEAT = b.comment("On defeat, all turrets and heroes are destroyed and the siege restarts from wave 1.").define("harshDefeat", true);
        SALVAGE_FRACTION = b.comment("Fraction of credits kept after a defeat.").defineInRange("salvageFraction", 0.25, 0.0, 1.0);
        BASE_CORE_HEALTH = b.comment("Base Siege Core health before Fortification upgrades.").defineInRange("baseCoreHealth", 600, 50, 100_000);
        MAX_ALIVE = b.comment("Hard cap for simultaneously alive siege enemies.").defineInRange("maxAlive", 90, 10, 400);
        NIGHT_MODIFIERS = b.comment("Enable special nights (Blood Moon, Fog, Storm, Horde...) from wave 6 onwards.").define("nightModifiers", true);
        PLAYER_PRESENCE_RADIUS = b.comment("The siege only progresses while a player is within this distance of the core.")
                .defineInRange("playerPresenceRadius", 160, 16, 1024);
        WAR_EVENTS = b.comment("Default for new sieges: mid-wave war events (supply drops, corruption rifts, warlords).").define("warEvents", true);
        RULES_EDITING = b.comment("Who may change a world's siege rules from the war room or /siege rules.",
                "Every value above is only the default for new sieges; the rules screen overrides them per world.")
                .defineEnum("rulesEditing", RuleEditing.OWNER_OR_OP);
        b.pop();

        b.push("coop");
        DOWNED = b.comment("Default for new sieges: with a teammate nearby, a fatal blow during a wave downs a player instead of killing them.")
                .define("downedState", true);
        BLEED_OUT_SECONDS = b.comment("Default for new sieges: how long a downed player can wait for a revive.").defineInRange("bleedOutSeconds", 30, 5, 300);
        b.pop();

        b.push("summons");
        SUMMON_ANYWHERE = b.comment("Heroes and devils can be summoned anywhere. Without a Siege Core they follow, guard or hunt for their summoner.")
                .define("summonAnywhere", true);
        MAX_HEROES = b.comment("Maximum heroes one player can have in the world at once (devils are always limited to one per player).")
                .defineInRange("maxHeroesPerPlayer", 6, 1, 64);
        DEVIL_BLOOD_PRICE = b.comment("Summoning a devil costs half of the summoner's current health (the blood pact).").define("devilBloodPrice", true);
        b.pop();
        SPEC = b.build();
    }

    private SiegeConfig() {
    }
}
