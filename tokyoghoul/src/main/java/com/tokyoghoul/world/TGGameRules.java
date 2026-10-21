package com.tokyoghoul.world;

import net.minecraft.world.level.GameRules;

/** World options, like Jujutsu Craft's jujutsuPVP / jujutsuGainFame / jujutsuUpgradeDifficulty rules. */
public final class TGGameRules {
    public static GameRules.Key<GameRules.BooleanValue> PVP;
    public static GameRules.Key<GameRules.IntegerValue> FAME_DIFFICULTY;
    public static GameRules.Key<GameRules.BooleanValue> ENCOUNTERS;
    public static GameRules.Key<GameRules.BooleanValue> GHOUL_DIET;
    public static GameRules.Key<GameRules.BooleanValue> STARTER_KIT;

    private TGGameRules() {
    }

    public static void register() {
        PVP = GameRules.register("tgPvp", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
        FAME_DIFFICULTY = GameRules.register("tgFameDifficulty", GameRules.Category.PLAYER, GameRules.IntegerValue.create(1));
        ENCOUNTERS = GameRules.register("tgEncounters", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
        GHOUL_DIET = GameRules.register("tgGhoulDiet", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
        STARTER_KIT = GameRules.register("tgStarterKit", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    }
}
