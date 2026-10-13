package com.lastbastion.entity.boss;

import com.lastbastion.registry.ModEntities;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;

import java.util.function.Supplier;

/** The six siege bosses, in the order they first appear (waves 5, 10, 15, 20, 25, 30). */
public enum BossKind {
    GOREHOUND("gorehound", "Gorehound the Butcher", 1100, 14, 0.27, 12, 450, BossEvent.BossBarColor.RED,
            "A mountain of stitched flesh wielding a rusted cleaver and a chain hook. Hooks defenders in, carves through walls, and enrages at half health."),
    PLAGUE_MOTHER("plague_mother", "The Plague Mother", 1700, 10, 0.2, 8, 750, BossEvent.BossBarColor.GREEN,
            "A bloated broodmother dripping with contagion. Births swarms, spews toxic bile and calls down acid rain."),
    MALGRAVE("malgrave", "Malgrave the Dread Knight", 2300, 20, 0.26, 20, 1100, BossEvent.BossBarColor.PURPLE,
            "A fallen paladin in blackened plate. Greatsword combos, shadow charges and, at half health, soulfire ascension."),
    OSSUARY("ossuary_colossus", "The Ossuary Colossus", 3600, 28, 0.2, 24, 1500, BossEvent.BossBarColor.WHITE,
            "A titan of fused bone. Hurls boulders, raises bone spikes and makes straight for the core."),
    VARKHUL("varkhul", "Varkhul the Lich King", 3200, 18, 0.24, 14, 2100, BossEvent.BossBarColor.BLUE,
            "Floating necromancer-king. Shielded by phylactery crystals that must be shattered; blinks, freezes and raises armies."),
    MORVATHRAX("morvathrax", "Morvathrax the Rotwing", 5200, 24, 0.3, 16, 3000, BossEvent.BossBarColor.PINK,
            "An undead dragon of rotting bone and plague-fire. Strafes with breath, dives, then lands to fight claw and tail.");

    public final String id;
    public final String title;
    public final float hp, damage, speed, armor;
    public final int credits;
    public final BossEvent.BossBarColor bar;
    public final String lore;

    BossKind(String id, String title, float hp, float damage, double speed, float armor, int credits, BossEvent.BossBarColor bar, String lore) {
        this.id = id;
        this.title = title;
        this.hp = hp;
        this.damage = damage;
        this.speed = (float) speed;
        this.armor = armor;
        this.credits = credits;
        this.bar = bar;
        this.lore = lore;
    }

    public Supplier<? extends EntityType<? extends SiegeBoss>> type() {
        return ModEntities.boss(this);
    }
}
