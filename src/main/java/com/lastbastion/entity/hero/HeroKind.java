package com.lastbastion.entity.hero;

import com.lastbastion.registry.ModEntities;
import net.minecraft.world.entity.EntityType;

import java.util.function.Supplier;

/** The ten summonable heroes, from the scrappy dire wolf to the King of Olympus. */
public enum HeroKind {
    ASHFANG("ashfang", "Ashfang the Dire Wolf", 350, 80, 7, 4, 0.40, false, 0x8A8F99,
            "A smoke-grey dire wolf with ember eyes.",
            "Rending Bite (Bleed) / Pounce / Howl of the Pack (allies: speed + damage, Runners flee)"),
    SYLVA("sylva", "Sylva the Huntress", 600, 70, 8, 4, 0.32, false, 0x3F8F4A,
            "An elven ranger with a recurve bow of living wood.",
            "Quick Shot / Piercing Arrow / Arrow Rain / Evasive Roll / Hunter's Mark"),
    BASTION("bastion", "Bastion, the Stone Colossus", 900, 320, 12, 16, 0.22, false, 0x8C8577,
            "A mossy granite golem with a heart of glowing amber.",
            "Granite Fists / Taunt (draws aggro) / Earthshatter / Stoneskin"),
    ALDRIC("aldric", "Sir Aldric, Knight of Dawn", 1300, 170, 14, 14, 0.31, false, 0xFFD966,
            "A paladin in gilded white plate wielding Dawnbringer, a sword of living light.",
            "Three-strike Combo / Radiant Dash / Judgement (leaping plunge, pillars of light) / Aegis of Dawn"),
    MORGANA("morgana", "Morgana the Stormweaver", 1800, 120, 12, 6, 0.28, false, 0x7A4AE0,
            "An archmage in storm-violet robes, orbited by arcane runes.",
            "Arcane Bolt / Chain Lightning / Frost Nova / Blink / Meteor"),
    KAGE("kage", "Kage the Ronin", 2600, 160, 18, 8, 0.37, false, 0x3A3A48,
            "A masked swordsman with a crimson-edged katana.",
            "Swift Slashes / Iaido Flash / Shadow Clones / Thousand Cuts"),
    AEGIS7("aegis7", "AEGIS-7 War Mech", 3600, 420, 4, 20, 0.24, false, 0x5B6E7A,
            "A 5-block bipedal war machine with twin rotary cannons and a missile pod.",
            "Twin Miniguns / Missile Swarm / Seismic Stomp / Overdrive"),
    IGNIS("ignis", "Ignis the Phoenix", 5000, 240, 16, 8, 0.30, true, 0xFF7A1A,
            "An immortal firebird whose wings trail living flame.",
            "Flame Bolts / Flame Dive / Wings of Ember (heal) / Rebirth (revives once per wave)"),
    VORTHAX("vorthax", "Vorthax the Elder Dragon", 7000, 650, 22, 18, 0.32, true, 0xB02020,
            "An ancient crimson dragon, horns like a crown of obsidian.",
            "Dragonfire Breath / Tail Sweep / Wing Gust / Terrifying Roar (fear) / Meteor Storm"),
    ZEUS("zeus", "Zeus, King of Olympus", 10000, 800, 40, 20, 0.30, true, 0xF4F06A,
            "The Thunderer himself, crowned in laurels, wielding the Master Bolt and the Aegis.",
            "Thunderbolt Javelin / Chain Storm / Olympian Smite (boss slayer) / Aegis Shield (team) / Wrath of the Sky");

    public final String id, title;
    public final int cost;
    public final float hp, damage, armor, speed;
    public final boolean flying;
    public final int color;
    public final String desc, moves;

    HeroKind(String id, String title, int cost, float hp, float damage, float armor, double speed, boolean flying, int color,
             String desc, String moves) {
        this.id = id;
        this.title = title;
        this.cost = cost;
        this.hp = hp;
        this.damage = damage;
        this.armor = armor;
        this.speed = (float) speed;
        this.flying = flying;
        this.color = color;
        this.desc = desc;
        this.moves = moves;
    }

    public Supplier<? extends EntityType<? extends HeroEntity>> type() {
        return ModEntities.hero(this);
    }
}
