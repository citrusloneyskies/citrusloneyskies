package com.tokyoghoul.data;

/**
 * Progression tiers, the counterpart of Jujutsu Craft's sorcerer grades. Fame from defeating enemies
 * raises the rank; the rank is the player's level, which scales Rc, health and damage and unlocks moves.
 * Like the JJK grades, the upper ranks also demand having beaten an enemy of a minimum level.
 */
public final class Rank {
    /** Number of ranks; levels run 1..COUNT. */
    public static final int COUNT = 7;
    private static final String[] GHOUL = {"Unrated", "C Rate", "B Rate", "A Rate", "S Rate", "SS Rate", "SSS Rate"};
    private static final String[] CCG = {"Academy Student", "Rank 3", "Rank 2", "Rank 1", "First Class", "Associate Special Class",
            "Special Class"};
    /** Fame needed for each level (times the fame-difficulty game rule). */
    private static final int[] FAME = {0, 50, 200, 500, 1000, 2000, 4000};
    /** Strongest defeated enemy level needed for each level. */
    private static final int[] DEFEATED = {0, 0, 2, 3, 5, 7, 9};

    private Rank() {
    }

    public static String name(Race race, int level) {
        int i = Math.max(1, Math.min(COUNT, level)) - 1;
        return race.usesGhoulRatings() ? GHOUL[i] : CCG[i];
    }

    public static int fameFor(int level) {
        return FAME[Math.max(1, Math.min(COUNT, level)) - 1];
    }

    public static int defeatedFor(int level) {
        return DEFEATED[Math.max(1, Math.min(COUNT, level)) - 1];
    }

    /** Highest level reachable with this much fame and this strongest defeat. */
    public static int levelFor(double fame, int strongestDefeated, int difficulty) {
        int lv = 1;
        for (int l = 2; l <= COUNT; l++) {
            if (fame >= fameFor(l) * (double) Math.max(1, difficulty) && strongestDefeated >= defeatedFor(l)) lv = l;
        }
        return lv;
    }
}
