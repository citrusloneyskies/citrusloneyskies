package com.lastbastion.siege;

import javax.annotation.Nullable;

/**
 * Core Protocols: active abilities any defender can trigger from the core (or with the Protocols key
 * within 48 blocks of it). Each has a shared cooldown; most are unlocked by a core upgrade.
 */
public enum CoreAbility {
    NOVA("Nova Burst", 90, null, 0, 0x6FE3FF,
            "The core erupts: enemies within 8 blocks take heavy arcane damage and are hurled back."),
    AEGIS("Aegis Protocol", 180, CoreUpgrade.BARRIER, 1, 0x8FD8FF,
            "Instantly restores half the barrier and makes the core invulnerable for 6 seconds."),
    OVERDRIVE("Turret Overdrive", 150, CoreUpgrade.OVERCLOCK, 1, 0xFFB040,
            "Every turret within 32 blocks fires 50% faster for 12 seconds."),
    LANCE("Lance of Dawn", 120, CoreUpgrade.PULSE, 1, 0xFFE070,
            "The core fires searing beams at the six toughest enemies within 36 blocks."),
    RALLY("Rally Call", 150, CoreUpgrade.BEACON, 1, 0xFF6060,
            "Heals every defender within 24 blocks for 40% of their health and grants speed and strength.");

    public final String title;
    public final int cooldownSeconds;
    @Nullable
    public final CoreUpgrade requires;
    public final int requiredLevel;
    public final int color;
    public final String desc;

    CoreAbility(String title, int cooldownSeconds, @Nullable CoreUpgrade requires, int requiredLevel, int color, String desc) {
        this.title = title;
        this.cooldownSeconds = cooldownSeconds;
        this.requires = requires;
        this.requiredLevel = requiredLevel;
        this.color = color;
        this.desc = desc;
    }

    public boolean unlocked(int[] upgrades) {
        return requires == null || (requires.ordinal() < upgrades.length && upgrades[requires.ordinal()] >= requiredLevel);
    }

    public static CoreAbility byName(String n) {
        for (CoreAbility a : values()) if (a.name().equals(n)) return a;
        return null;
    }
}
