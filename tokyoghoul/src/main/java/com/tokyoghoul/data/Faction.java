package com.tokyoghoul.data;

/**
 * Who fights whom. Mirrors Jujutsu Craft's Sorcerer / Cursed Spirit / Curse User split: members of one
 * faction never hurt each other, the CCG hunts every ghoul, Aogiri hunts the CCG, humans and Anteiku,
 * and Anteiku only fights back. A player belongs to the faction of the character they picked.
 */
public enum Faction {
    /** Ordinary people and players who have not picked a character yet. */
    HUMAN("human", 0xD8D8D8, false),
    /** Commission of Counter Ghoul: investigators and Quinx. */
    CCG("ccg", 0xE8E8F0, false),
    /** The Anteiku cafe ghouls: they only eat what they must and fight back when hunted. */
    ANTEIKU("anteiku", 0x8B5A2B, true),
    /** Aogiri Tree: militant ghouls that attack humans, investigators and soft ghouls. */
    AOGIRI("aogiri", 0x6A0F1A, true),
    /** Unaffiliated ghouls roaming at night. */
    WILD("wild", 0x5A3A6A, true),
    /** The Clowns: chaotic ghouls who attack everyone that is not a clown. */
    CLOWNS("clowns", 0xC23B8E, true);

    public final String id;
    public final int color;
    public final boolean ghoul;

    Faction(String id, int color, boolean ghoul) {
        this.id = id;
        this.color = color;
        this.ghoul = ghoul;
    }

    /** True if an entity of faction {@code a} attacks entities of faction {@code b} on sight. */
    public boolean hunts(Faction b) {
        if (this == b) return false;
        return switch (this) {
            case HUMAN -> false;
            case CCG -> b.ghoul;
            case ANTEIKU -> false;
            case AOGIRI -> b == HUMAN || b == CCG || b == ANTEIKU;
            case WILD -> b == HUMAN || b == CCG;
            case CLOWNS -> true;
        };
    }

    /**
     * True if the two factions' attacks hurt each other at all (allies' area attacks pass through each
     * other). Different ghoul groups always can; anything else needs a hunter. Fights that start the other
     * way (someone provoking a neutral) are allowed separately by the combat code's target check.
     */
    public boolean canHurt(Faction b) {
        if (this == b) return false;
        return hunts(b) || b.hunts(this) || (ghoul && b.ghoul);
    }

    public String key() {
        return "faction.tokyoghoul." + id;
    }

    public static Faction byId(String id) {
        for (Faction f : values()) if (f.id.equals(id)) return f;
        return HUMAN;
    }
}
