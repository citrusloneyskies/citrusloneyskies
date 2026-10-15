package com.lastbastion.siege;

/**
 * Special nights rolled from wave 6 onwards (35% chance, never twice in a row). Each twists the
 * rules and pays out a credit bonus.
 */
public enum NightModifier {
    NONE("Clear Night", 0xBFC6D9, 1.0f, "An ordinary, dreadful night."),
    BLOOD_MOON("Blood Moon", 0xD01818, 1.5f, "Zombies move 20% faster and hit 25% harder."),
    FOG("Graveyard Fog", 0x9AA6A6, 1.15f, "Turret range reduced by 25%. Zombies are hard to see."),
    STORM("Thunderstorm", 0xF4F06A, 1.2f, "Lightning strikes the battlefield. Tesla turrets +30% damage."),
    HORDE("Horde Night", 0x7BD940, 1.5f, "60% more zombies, but they are less evolved."),
    ELITE("Elite Vanguard", 0xFFB000, 1.4f, "Champion chance tripled."),
    NECROTIC("Necrotic Tide", 0x6B3FA0, 1.3f, "Fallen zombies have a 30% chance to rise again.");

    public final String title;
    public final int color;
    public final float creditMult;
    public final String desc;

    NightModifier(String title, int color, float creditMult, String desc) {
        this.title = title;
        this.color = color;
        this.creditMult = creditMult;
        this.desc = desc;
    }

    public static NightModifier byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : NONE;
    }
}
