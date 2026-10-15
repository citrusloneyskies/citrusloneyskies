package com.lastbastion.siege;

/** Permanent (per-siege) upgrades purchased at the core. Reset on defeat. */
public enum CoreUpgrade {
    FORTIFY("Fortified Core", 5, new int[]{300, 600, 1100, 1800, 3000}, "+30% core max health per level."),
    BARRIER("Arcane Barrier", 3, new int[]{500, 1200, 2600}, "A regenerating shield over the core (15/30/45% of max health)."),
    PULSE("Retaliation Pulse", 3, new int[]{450, 1000, 2200}, "When struck below 50% health the core erupts, blasting nearby zombies (60s cooldown)."),
    TREASURY("War Treasury", 3, new int[]{400, 900, 1800}, "+7% credits from all kills and wave bonuses per level."),
    OVERCLOCK("Turret Overclock", 3, new int[]{600, 1400, 3000}, "All turrets fire 6% faster per level."),
    BEACON("Rally Beacon", 2, new int[]{700, 1800}, "Heroes and players near the core regenerate and deal +10% damage per level.");

    public final String title;
    public final int maxLevel;
    public final int[] costs;
    public final String desc;

    CoreUpgrade(String title, int maxLevel, int[] costs, String desc) {
        this.title = title;
        this.maxLevel = maxLevel;
        this.costs = costs;
        this.desc = desc;
    }
}
