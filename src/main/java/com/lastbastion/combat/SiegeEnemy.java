package com.lastbastion.combat;

/** Marker + stats for anything that attacks the Siege Core (zombies, bosses, summoned minions). */
public interface SiegeEnemy {
    /** Multiplier applied to incoming damage of the given element (1 = normal, 0 = immune). */
    float elementMultiplier(Element element);

    /** Credits awarded for the kill (before night modifiers). */
    int creditValue();

    /** Hero experience granted to the hero landing the killing blow. */
    default int heroXp() {
        return Math.max(1, creditValue() / 4);
    }

    /** True for wave spawns that count towards wave completion. */
    boolean isWaveMob();

    /** Arcane-warded champions shrug off status effects. */
    default boolean immuneToStatus() {
        return false;
    }

    /** Burrowed/phased enemies can't be targeted by turrets or heroes. */
    default boolean isUntargetable() {
        return false;
    }

    /** Bosses and champions are prioritised by hero AI and get unique HUD treatment. */
    default boolean isHighPriority() {
        return false;
    }
}
