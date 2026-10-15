package com.lastbastion.combat;

/**
 * Damage elements. Every hit dealt through {@link SiegeDamage} carries one; zombies, bosses and
 * champion affixes resist or are weak to specific elements, and several status effects amplify
 * particular elements (Soaked boosts Lightning, Frozen makes targets shatter on Physical/Explosive).
 */
public enum Element {
    PHYSICAL(0xD8D8D8),
    FIRE(0xFF7A1A),
    ICE(0x8FE3FF),
    WATER(0x3A7BFF),
    LIGHTNING(0xF4F06A),
    POISON(0x7BD940),
    EXPLOSIVE(0xFFB347),
    ARCANE(0xC86BFF),
    WIND(0xCFFFF0),
    HOLY(0xFFF2B0),
    SHADOW(0x5A2A7A),
    /** Hellfire wielded by the Devils: pierces armour and burns the damned (undead take extra). Nothing is immune. */
    INFERNAL(0xE0301A);

    public final int color;

    Element(int color) {
        this.color = color;
    }

    public float r() { return ((color >> 16) & 0xFF) / 255f; }
    public float g() { return ((color >> 8) & 0xFF) / 255f; }
    public float b() { return (color & 0xFF) / 255f; }
}
