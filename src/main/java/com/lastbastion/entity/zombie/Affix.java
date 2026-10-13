package com.lastbastion.entity.zombie;

/**
 * Champion modifiers. Champions roll 1-3 affixes, glow gold, show their affixes in their name,
 * and are worth 3x credits.
 */
public enum Affix {
    VAMPIRIC("Vampiric", 0xB0102A, "Heals for 40% of damage dealt."),
    MOLTEN("Molten", 0xFF7A1A, "Immune to fire, leaves burning ground, attacks ignite."),
    FROSTBOUND("Frostbound", 0x8FE3FF, "Immune to ice, chills defenders around it."),
    SHIELDED("Shielded", 0xFFE38A, "Projects a regenerating barrier worth 35% of its health."),
    HASTED("Hasted", 0xCFFFF0, "Moves and attacks 40% faster."),
    SPLITTING("Splitting", 0x7BD940, "Bursts into two Runners on death."),
    BLINKING("Blinking", 0xC86BFF, "Teleports toward the core every few seconds."),
    REFLECTIVE("Reflective", 0xD8D8D8, "Reflects 20% of damage back to turrets and heroes."),
    WARDED("Warded", 0x5A2A7A, "Immune to all status effects."),
    JUGGERNAUT("Juggernaut", 0x8B5A2B, "Double health, unstoppable, larger.");

    public final String title;
    public final int color;
    public final String desc;

    Affix(String title, int color, String desc) {
        this.title = title;
        this.color = color;
        this.desc = desc;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public boolean in(int mask) {
        return (mask & bit()) != 0;
    }
}
