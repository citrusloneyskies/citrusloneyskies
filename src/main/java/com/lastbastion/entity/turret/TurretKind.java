package com.lastbastion.entity.turret;

import com.lastbastion.combat.Element;
import com.lastbastion.registry.ModEntities;
import net.minecraft.world.entity.EntityType;

import java.util.function.Supplier;

/**
 * The ten turret families. All are balanced around cost-efficiency (damage per credit is similar),
 * but each brings a distinct status effect and battlefield role. Turrets level 1 to 5; every level
 * changes the model. Level 3 unlocks a perk; at level 5 the owner picks one of two specialisations,
 * each with its own model and abilities.
 */
public enum TurretKind {
    //          id          name                 elem               dmg  range cd  cost hp
    BALLISTA("ballista", "Ballista", Element.PHYSICAL, 7.0f, 18, 24, 150, 80, 0x8A6A3A,
            "Heavy bolts that cause Bleeding. Long range single-target.",
            "Twin Bolts: a second bow fires at another target.",
            "Siegebreaker", "Barbed harpoons pierce 6 enemies, pin them in place and shred armor. Slower, far heavier shots.",
            "Repeater Scorpion", "A revolving magazine fires 3-bolt bursts that ricochet to a nearby foe."),
    FLAME("flame", "Inferno Turret", Element.FIRE, 2.2f, 8, 5, 200, 100, 0xFF6A00,
            "Short-range flamethrower cone. Ignites and Scorches everything in front.",
            "Napalm: leaves burning ground.",
            "Soulfire Maw", "A dragon-skull nozzle breathes a longer cone of blue soulfire that burns even fire-immune foes.",
            "Napalm Launcher", "Lobs volleys of incendiary canisters that burst into wide lakes of fire. +6 range."),
    FROST("frost", "Cryo Spire", Element.ICE, 4.0f, 14, 20, 175, 80, 0x8FE3FF,
            "Ice shards stack Chill; at 5 stacks targets are Frozen solid.",
            "Splinter: shards split into two more on impact.",
            "Absolute Zero", "A permafrost aura chills everything nearby and a frost nova freezes whole groups every 6 seconds.",
            "Glacial Lance", "Charges a piercing lance of ice down a line. Shatters Frozen foes for triple damage."),
    TIDE("tide", "Tidecaller", Element.WATER, 3.5f, 14, 16, 175, 90, 0x2F6BFF,
            "Water orbs knock back and Soak (Soaked foes take +50% lightning damage).",
            "Breaker Wave: every 4th shot is a crashing wave.",
            "Maelstrom", "Summons whirlpools that drag in and trap groups of enemies.",
            "Leviathan's Wrath", "A tidal wave sweeps down the enemy's lane, hurling everything back the way it came."),
    TESLA("tesla", "Tesla Coil", Element.LIGHTNING, 6.0f, 13, 30, 250, 80, 0xF4F06A,
            "Chain lightning that arcs between enemies and Shocks (stuns) them.",
            "Arc Amplifier: chains to 5 targets.",
            "Stormcaller", "Calls lightning from the sky onto up to 6 enemies every 8 seconds (twice as often in a Thunderstorm).",
            "Arc Reactor", "Chains to 8 targets with no damage falloff, and its fire rate builds up the longer it keeps firing."),
    VENOM("venom", "Venom Lobber", Element.POISON, 3.0f, 16, 34, 200, 80, 0x7BD940,
            "Lobs acid that Corrodes armor and leaves caustic pools.",
            "Caustic Flood: larger, longer-lasting pools.",
            "Plague Cauldron", "Impacts release choking poison clouds, and corrosion spreads to nearby foes when a victim dies.",
            "Blightspore Hive", "Fires three homing spore pods per volley that burst into slowing spore swarms."),
    MORTAR("mortar", "Siege Mortar", Element.EXPLOSIVE, 14.0f, 30, 64, 300, 110, 0x5A5A5A,
            "Long-range artillery with big splash and Concussion. Cannot fire within 6 blocks.",
            "Cluster Shells: shells split into bomblets.",
            "Bombardment Battery", "Four tubes. Every 12 seconds it lays down a 6-shell creeping barrage.",
            "Grand Cannon", "One titanic shell: 2.2x damage, a huge blast that knocks enemies flat. +10 range, slower."),
    PRISM("prism", "Arcane Prism", Element.ARCANE, 0.55f, 16, 2, 300, 80, 0xC86BFF,
            "Continuous beam that ramps up to 2.5x damage on the same target and Marks it.",
            "Refraction: beam splits to a second target.",
            "Annihilation Lance", "The beam pierces everything in its path and ramps up twice as fast.",
            "Kaleidoscope", "Four orbiting prisms split the beam across four targets, Marking them all."),
    GALE("gale", "Gale Tower", Element.WIND, 3.0f, 12, 30, 175, 90, 0xCFFFF0,
            "Wind blades hurl enemies away from the core and Disorient them.",
            "Tornado: spawns twisters that lift and spin groups.",
            "Cyclone Wall", "A constant ring of wind repels every non-boss enemy that comes close.",
            "Tempest Eye", "Releases a great tornado that roams outward, sweeping enemies back toward where they spawned."),
    SANCTUM("sanctum", "Sanctum Obelisk", Element.HOLY, 5.0f, 14, 25, 350, 100, 0xFFF2B0,
            "Holy bolts (+50% vs undead) that Purify. Heals the core, turrets, heroes and players nearby.",
            "Blessing: allies in the aura gain Aegis (damage reduction).",
            "Divine Judgement", "A pillar of light smites the strongest foe in range every 12 seconds.",
            "Seraph's Sanctuary", "Doubles healing, raises a barrier on the core that absorbs damage, and revives downed allies in its aura.");

    public static final float[] DAMAGE = {1f, 1.5f, 2.2f, 3.1f, 4.4f};
    public static final float[] RANGE = {0f, 1f, 2f, 3f, 4f};
    public static final float[] COOLDOWN = {1f, 0.92f, 0.85f, 0.78f, 0.7f};
    public static final float[] HEALTH = {1f, 1.4f, 1.9f, 2.5f, 3.2f};
    public static final float[] UPGRADE_COST = {0.8f, 1.5f, 2.6f, 4.2f};
    public static final float[] SCALE = {1f, 1.05f, 1.1f, 1.16f, 1.25f};
    public static final int MAX_LEVEL = 5;

    public final String id, title;
    public final Element element;
    public final float damage;
    public final int range, cooldown, cost, hp, color;
    public final String desc, perk3, perk5;
    public final String branchA, branchADesc, branchB, branchBDesc;

    TurretKind(String id, String title, Element element, float damage, int range, int cooldown, int cost, int hp, int color,
               String desc, String perk3, String branchA, String branchADesc, String branchB, String branchBDesc) {
        this.id = id;
        this.title = title;
        this.element = element;
        this.damage = damage;
        this.range = range;
        this.cooldown = cooldown;
        this.cost = cost;
        this.hp = hp;
        this.color = color;
        this.desc = desc;
        this.perk3 = perk3;
        this.branchA = branchA;
        this.branchADesc = branchADesc;
        this.branchB = branchB;
        this.branchBDesc = branchBDesc;
        this.perk5 = "choose " + branchA + " or " + branchB;
    }

    /** Credits to go from {@code level} to {@code level + 1}. */
    public int upgradeCost(int level) {
        return level >= MAX_LEVEL ? 0 : Math.round(cost * UPGRADE_COST[level - 1] / 5f) * 5;
    }

    public int totalInvested(int level) {
        int t = cost;
        for (int l = 1; l < level; l++) t += upgradeCost(l);
        return t;
    }

    public float damageAt(int level) {
        return damage * DAMAGE[level - 1];
    }

    public float rangeAt(int level) {
        return range + RANGE[level - 1];
    }

    public int cooldownAt(int level) {
        return Math.max(1, Math.round(cooldown * COOLDOWN[level - 1]));
    }

    public float hpAt(int level) {
        return hp * HEALTH[level - 1];
    }

    public Supplier<? extends EntityType<? extends TurretEntity>> type() {
        return ModEntities.turret(this);
    }
}
