package com.lastbastion.entity.hero;

import com.lastbastion.registry.ModEntities;
import net.minecraft.world.entity.EntityType;

import java.util.function.Supplier;

/**
 * The ten summonable heroes, from the scrappy dire wolf to the King of Olympus. Every hero fights on
 * the ground where zombies can reach them; Zeus takes to the sky only for the few seconds of his ultimate.
 */
public enum HeroKind {
    ASHFANG("ashfang", "Ashfang the Dire Wolf", 350, 90, 7, 4, 0.40, 0x8A8F99, HeroEntity.Role.ASSASSIN,
            "A smoke-grey dire wolf with ember eyes and a scarred muzzle.",
            "Rending Bites / Pounce / Hamstring / Howl of the Pack / ULT Alpha's Hunt"),
    SYLVA("sylva", "Sylva the Huntress", 600, 75, 8, 4, 0.33, 0x3F8F4A, HeroEntity.Role.RANGED,
            "An elven ranger in a leaf-woven cloak, with a recurve bow of living wood.",
            "Aimed Shot / Piercing Arrow / Arrow Rain / Snare Trap / Evasive Roll / Hunter's Mark / ULT Wild Hunt"),
    BASTION("bastion", "Bastion, the Stone Colossus", 900, 340, 12, 16, 0.22, 0x8C8577, HeroEntity.Role.TANK,
            "A mossy granite golem with a heart of glowing amber.",
            "Granite Fists / Taunt / Earthshatter / Boulder Toss / Stoneskin / ULT Mountain's Wrath"),
    ALDRIC("aldric", "Sir Aldric, Knight of Dawn", 1300, 180, 14, 14, 0.31, 0xFFD966, HeroEntity.Role.BRUISER,
            "A paladin in gilded white plate wielding Dawnbringer, a sword of living light, and a sunburst shield.",
            "Dawn Combo / Shield Bash / Radiant Dash / Consecration / Aegis of Dawn / ULT Dawnbreak"),
    MORGANA("morgana", "Morgana the Stormweaver", 1800, 125, 12, 6, 0.29, 0x7A4AE0, HeroEntity.Role.RANGED,
            "An archmage in storm-violet robes, orbited by arcane runes.",
            "Arcane Bolts / Chain Lightning / Frost Nova / Gravity Well / Blink / Meteor / ULT Tempest"),
    KAGE("kage", "Kage the Ronin", 2600, 165, 18, 8, 0.38, 0x3A3A48, HeroEntity.Role.ASSASSIN,
            "A masked swordsman in a straw kasa with a crimson-edged katana.",
            "Four-Cut Combo / Iaido Flash / Parry & Riposte / Smoke Step / Shadow Clones / ULT Thousand Cuts"),
    AEGIS7("aegis7", "AEGIS-7 War Mech", 3600, 440, 4, 20, 0.24, 0x5B6E7A, HeroEntity.Role.TANK,
            "A 4-block bipedal war machine with twin rotary cannons and a shoulder missile pod.",
            "Twin Miniguns / Missile Swarm / Seismic Stomp / Repulsor Shield / Overdrive / ULT Orbital Lance"),
    ASTERION("asterion", "Asterion, the Minotaur King", 5000, 560, 26, 16, 0.30, 0x9A5A2E, HeroEntity.Role.BRUISER,
            "A towering bull-headed warlord in bronze, wielding a colossal double-bladed labrys.",
            "Labrys Cleaves / Gore / Bull Rush / War Stomp / Earthsplitter / Bloodrage / ULT King of the Labyrinth"),
    ANUBIS("anubis", "Anubis, Judge of the Dead", 7000, 480, 24, 14, 0.30, 0xE0B040, HeroEntity.Role.SUPPORT,
            "The jackal-headed god of the dead, bearing the was-sceptre and the scales of judgement.",
            "Sceptre Strikes / Sand Bolts / Weighing of Hearts (execute) / Sandstorm / Soul Siphon / Curse of the Pharaoh / ULT Hall of Judgement"),
    ZEUS("zeus", "Zeus, King of Olympus", 10000, 820, 40, 20, 0.30, 0xF4F06A, HeroEntity.Role.RANGED,
            "The Thunderer himself, crowned in laurels, wielding the Master Bolt and the Aegis.",
            "Thunderbolt Javelins / Thunderclap / Chain Storm / Olympian Smite / Aegis of Olympus / ULT Wrath of the Sky (brief flight)");

    public final String id, title;
    public final int cost;
    public final float hp, damage, armor, speed;
    public final int color;
    public final HeroEntity.Role role;
    public final String desc, moves;

    HeroKind(String id, String title, int cost, float hp, float damage, float armor, double speed, int color, HeroEntity.Role role,
             String desc, String moves) {
        this.id = id;
        this.title = title;
        this.cost = cost;
        this.hp = hp;
        this.damage = damage;
        this.armor = armor;
        this.speed = (float) speed;
        this.color = color;
        this.role = role;
        this.desc = desc;
        this.moves = moves;
    }

    public Supplier<? extends EntityType<? extends HeroEntity>> type() {
        return ModEntities.hero(this);
    }
}
