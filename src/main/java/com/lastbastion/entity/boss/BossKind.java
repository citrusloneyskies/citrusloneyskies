package com.lastbastion.entity.boss;

import com.lastbastion.registry.ModEntities;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;

import java.util.function.Supplier;

/** The six siege bosses, in the order they first appear (waves 5, 10, 15, 20, 25, 30). */
public enum BossKind {
    GOREHOUND("gorehound", "Gorehound the Butcher", 1100, 14, 0.27, 12, 450, BossEvent.BossBarColor.RED,
            "A giggling mountain of stitched flesh with a rusted cleaver and a chain hook. Cleaver combos, hook pulls and wall-smashing rampages; at half health his belly splits into Gut Spill and Butcher's Frenzy."),
    PLAGUE_MOTHER("plague_mother", "The Plague Mother", 1700, 10, 0.2, 8, 750, BossEvent.BossBarColor.GREEN,
            "A corpse-queen riding a bloated spider abdomen. Spews bile cones, births broods, rains acid on marked circles and leaps; at half health her abdomen bursts."),
    MALGRAVE("malgrave", "Malgrave the Dread Knight", 2300, 20, 0.26, 20, 1100, BossEvent.BossBarColor.PURPLE,
            "A fallen paladin and duellist. Swears an Oath of Ruin on the strongest defender, ripostes blows that land on his Black Guard, and at half health rises in soulfire with Execution Leaps."),
    OSSUARY("ossuary_colossus", "The Ossuary Colossus", 3600, 28, 0.2, 24, 1500, BossEvent.BossBarColor.WHITE,
            "A cathedral of fused bone that marches on the core. Boulders, quake stomps with aftershocks, travelling bone spikes and a crushing grasp; at half health its ribs open on a soul-furnace beam."),
    VARKHUL("varkhul", "Varkhul the Lich King", 3200, 18, 0.24, 14, 2100, BossEvent.BossBarColor.BLUE,
            "A floating lich-king who never lets you close. Soul volleys, frost novas, blizzards of ice spears and glacial tombs; phylactery wards at 75% and 40%, then armies and Soul Harvest."),
    MORVATHRAX("morvathrax", "Morvathrax the Rotwing", 5200, 24, 0.3, 16, 3000, BossEvent.BossBarColor.PINK,
            "An undead wyvern of bone and plague-fire. Strafing runs, bone barrages and craters from the sky; at 40% its wings tear and it fights on the ground with breath, tail, claws and pounces.");

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
