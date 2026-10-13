package com.lastbastion.entity.zombie;

/** Tiers within each zombie type. Higher ranks are tougher and visibly mutated (armor, veins, abyssal fire). */
public enum ZombieRank {
    ROTTEN("Rotten", 0xA0A0A0, 1.0f, 1.0f, 1.00f, 0, 1.0f, 1.0f, 1.00f),
    FERAL("Feral", 0x7FD36B, 1.5f, 1.25f, 1.05f, 2, 1.5f, 1.6f, 1.03f),
    ARMORED("Armored", 0x6FA8FF, 2.2f, 1.55f, 1.08f, 5, 2.1f, 2.4f, 1.07f),
    CORRUPTED("Corrupted", 0xC070FF, 3.2f, 1.9f, 1.12f, 8, 2.9f, 3.4f, 1.12f),
    ABYSSAL("Abyssal", 0xFF5050, 4.6f, 2.4f, 1.18f, 12, 4.0f, 4.6f, 1.18f);

    public final String title;
    public final int color;
    public final float hpMult, dmgMult, speedMult;
    public final int bonusArmor;
    public final float creditMult, costMult, scale;

    ZombieRank(String title, int color, float hp, float dmg, float speed, int armor, float credits, float cost, float scale) {
        this.title = title;
        this.color = color;
        this.hpMult = hp;
        this.dmgMult = dmg;
        this.speedMult = speed;
        this.bonusArmor = armor;
        this.creditMult = credits;
        this.costMult = cost;
        this.scale = scale;
    }

    public static ZombieRank byId(int i) {
        return values()[Math.max(0, Math.min(values().length - 1, i))];
    }
}
