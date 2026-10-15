package com.lastbastion.item.armor;

/**
 * The siege armour sets. Each piece gives armour on its own; wearing all four pieces of one set
 * activates the set bonus (see {@code ArmorSets}). Defence values are helmet, chest, legs, boots.
 */
public enum ArmorSet {
    //            id               title                     tier  defence           tough  kb    cost
    SCAVENGER("scavenger", "Scavenger Rig", 1, new int[]{2, 5, 4, 2}, 0f, 0f, 400, 0xB08A5A,
            "Scrounger", "+20% credits from kills and +10% movement speed.",
            "Patched leather, scrap plate and a lot of optimism."),
    SHADOWSTALKER("shadowstalker", "Shadowstalker Leathers", 2, new int[]{3, 6, 5, 2}, 1f, 0f, 1300, 0x6A50A0,
            "Evasion", "20% chance to slip out of any attack. Critical hits deal +30% damage.",
            "Oiled leather and a hood that drinks the light."),
    ARCANIST("arcanist", "Arcanist Regalia", 3, new int[]{2, 6, 5, 3}, 1f, 0f, 1600, 0x8A5AFF,
            "Arcane Flow", "Weapon specials recharge 30% faster, Fury builds 50% faster, guns reload 30% faster, and every 8 seconds an arcane nova bursts around you in combat.",
            "Robes stitched with living runes. The crystal follows you on its own."),
    VANGUARD("vanguard", "Vanguard Plate", 3, new int[]{4, 9, 7, 4}, 3f, 0.25f, 1800, 0x3A6AC8,
            "Bulwark", "Take 15% less damage and ignore knockback. Dropping below 35% health raises a 10-heart barrier (40s cooldown).",
            "The kingdom's last line, still standing."),
    SUNFORGED("sunforged", "Sunforged Paladin", 4, new int[]{4, 9, 7, 4}, 4f, 0.1f, 2800, 0xFFD966,
            "Aura of Dawn", "Undead within 5 blocks burn for holy damage every second. Your kills heal you and nearby allies. Immune to Bleeding and Corrosion.",
            "Forged at noon on the longest day, and it still remembers the sun."),
    TITAN("titan", "Titan Exo-Frame", 4, new int[]{5, 10, 8, 4}, 5f, 0.15f, 3200, 0x50C8FF,
            "Exo Systems", "Jump again in mid-air to fire jump jets (two boosts). No fall damage, and long falls end in a shockwave slam.",
            "Military power armour salvaged from a dead world's last war.");

    /** Siege armour is forged for the horde: ten times the protection of the listed base values. */
    public static final int STRENGTH = 10;

    public final String id, title;
    public final int tier;
    public final int[] defence;
    public final float toughness, knockbackResistance;
    public final int cost, color;
    public final String bonus, bonusDesc, lore;

    ArmorSet(String id, String title, int tier, int[] defence, float toughness, float kb, int cost, int color, String bonus, String bonusDesc,
             String lore) {
        this.id = id;
        this.title = title;
        this.tier = tier;
        this.defence = new int[defence.length];
        for (int i = 0; i < defence.length; i++) this.defence[i] = defence[i] * STRENGTH;
        this.toughness = toughness * STRENGTH;
        this.knockbackResistance = kb;
        this.cost = cost;
        this.color = color;
        this.bonus = bonus;
        this.bonusDesc = bonusDesc;
        this.lore = lore;
    }

    /** Piece names, helmet to boots. */
    public String pieceName(int slot) {
        return switch (this) {
            case SCAVENGER -> new String[]{"Scavenger Goggles", "Scavenger Jacket", "Scavenger Trousers", "Scavenger Boots"}[slot];
            case SHADOWSTALKER -> new String[]{"Shadowstalker Hood", "Shadowstalker Jerkin", "Shadowstalker Leggings", "Shadowstalker Treads"}[slot];
            case ARCANIST -> new String[]{"Arcanist Circlet", "Arcanist Robe", "Arcanist Skirts", "Arcanist Slippers"}[slot];
            case VANGUARD -> new String[]{"Vanguard Greathelm", "Vanguard Cuirass", "Vanguard Greaves", "Vanguard Sabatons"}[slot];
            case SUNFORGED -> new String[]{"Sunforged Crown-Helm", "Sunforged Breastplate", "Sunforged Legplates", "Sunforged Sabatons"}[slot];
            case TITAN -> new String[]{"Titan Helm Unit", "Titan Torso Frame", "Titan Leg Actuators", "Titan Stabilisers"}[slot];
        };
    }

    public static final String[] SLOT_IDS = {"helmet", "chestplate", "leggings", "boots"};
}
