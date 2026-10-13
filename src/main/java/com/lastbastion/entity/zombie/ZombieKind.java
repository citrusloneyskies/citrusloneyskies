package com.lastbastion.entity.zombie;

import com.lastbastion.registry.ModEntities;
import net.minecraft.world.entity.EntityType;

import java.util.function.Supplier;

/**
 * The siege zombie archetypes (twelve regulars, the Crawler, the Incinerator and the elite Abomination). Stats are rank I values at wave 1; {@link ZombieRank}
 * and the wave scaling in {@code WavePlanner} multiply them.
 */
public enum ZombieKind {
    //            id            hp   dmg  speed  armor kbRes coreDmg credits cost unlock  role
    SHAMBLER   ("shambler",    20,  3.0, 0.23,  2,   0.0,   4,     5,   1.0f,  1, "Slow, relentless foot soldier."),
    RUNNER     ("runner",      14,  2.5, 0.34,  0,   0.0,   3,     6,   1.2f,  2, "Sprints and pounces; fragile."),
    SPITTER    ("spitter",     18,  4.0, 0.24,  0,   0.0,   3,     8,   2.0f,  3, "Lobs corrosive bile at turrets from range."),
    BRUTE      ("brute",       70,  8.0, 0.19,  6,   0.8,  12,    18,   4.0f,  4, "Hulking tank; smashes walls three times faster."),
    BLOATER    ("bloater",     34, 14.0, 0.17,  0,   0.3,  40,    12,   2.5f,  5, "Explodes into a toxic cloud. Kill it at range!"),
    LEAPER     ("leaper",      18,  3.5, 0.30,  0,   0.0,   4,     9,   1.5f,  6, "Vaults over walls with huge leaps."),
    BULWARK    ("bulwark",     40,  5.0, 0.20, 10,   0.6,   6,    16,   4.0f,  7, "Tower shield blocks most frontal projectiles."),
    BURROWER   ("burrower",    26,  4.0, 0.25,  2,   0.2,   6,    12,   2.5f,  8, "Tunnels underground, surfacing beside the core."),
    SHRIEKER   ("shrieker",    30,  2.0, 0.23,  0,   0.0,   2,    16,   4.0f,  9, "Screams to enrage zombies and jam turrets."),
    WRAITH     ("wraith",      24,  5.0, 0.26,  0,   1.0,   7,    15,   3.5f, 11, "Ethereal; phases through walls, resists physical."),
    WRECKER    ("wrecker",     55,  6.0, 0.21,  6,   0.7,   8,    20,   4.5f, 12, "Hunts turrets with a siege hammer."),
    NECROMANCER("necromancer", 40,  3.0, 0.22,  2,   0.0,   3,    28,   6.0f, 14, "Raises the dead and mends the horde."),
    CRAWLER    ("crawler",     12,  2.5, 0.21,  0,   0.0,   3,     5,   0.8f,  3, "Legless and low; clings to ankles and slows."),
    INCINERATOR("incinerator", 45,  3.0, 0.22,  4,   0.3,   8,    18,   4.0f, 10, "Hoses turrets with fire; its fuel tanks burst on death."),
    ABOMINATION("abomination",180, 12.0, 0.18,  8,   1.0,  30,    60,  12.0f, 16, "Elite stitched colossus. Hurls crawlers and slams the ground.");

    public final String id;
    public final float hp, damage, speed, armor, kbResist;
    public final int coreDamage, credits, unlockWave;
    public final float cost;
    public final String role;

    ZombieKind(String id, float hp, double damage, double speed, float armor, double kbResist, int coreDamage, int credits,
               float cost, int unlockWave, String role) {
        this.id = id;
        this.hp = hp;
        this.damage = (float) damage;
        this.speed = (float) speed;
        this.armor = armor;
        this.kbResist = (float) kbResist;
        this.coreDamage = coreDamage;
        this.credits = credits;
        this.cost = cost;
        this.unlockWave = unlockWave;
        this.role = role;
    }

    public Supplier<? extends EntityType<? extends SiegeZombie>> type() {
        return ModEntities.zombie(this);
    }
}
