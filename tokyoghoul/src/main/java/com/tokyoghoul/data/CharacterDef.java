package com.tokyoghoul.data;

import com.tokyoghoul.move.MoveDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One playable / spawnable character. The same definition drives the select book, the player's
 * moveset and stats, the NPC entity (health, level, AI) and the procedural model.
 */
public final class CharacterDef {
    /** Groups on the select screen (JJK splits Sorcerers / Cursed Spirits / Curse Users the same way). */
    public enum Group {
        ANTEIKU("Anteiku"), AOGIRI("Aogiri Tree"), CCG("CCG"), QUINX("Quinx Squad"), OTHERS("Others");

        public final String title;

        Group(String title) {
            this.title = title;
        }
    }

    public final String id;
    public final String name;
    public final Group group;
    public final Faction faction;
    public final Race race;
    public KaguneType kagune = KaguneType.NONE, kagune2 = KaguneType.NONE;
    public int kaguneColor = 0xB01020, kaguneColor2 = 0xB01020;
    /** Rinkaku tentacle count / Ukaku wing size / Bikaku tail count. */
    public int kaguneCount = 4;
    public final Look look;
    /** Base Rc pool at level 1 (scales with rank). */
    public int rcBase = 100;
    /** Extra max health for players, in half-hearts. */
    public float hpBonus;
    /** NPC stats. */
    public int npcLevel = 3;
    public float npcHealth = 80, npcSpeed = 0.32f, npcArmor = 4;
    /** Item ids (namespace tokyoghoul) given when selected / held by the NPC. Empty for none. */
    public String quinque = "", quinque2 = "", mask = "";
    /** Short canon blurb for the select screen. */
    public String blurb = "";
    /** Shown in the select book (false for NPC-only types such as civilians). */
    public boolean selectable = true;
    /** Named, unique characters spawn as rare bosses; generic ones as common mobs. */
    public boolean boss = true;
    private final List<MoveDef> moves = new ArrayList<>();
    private MoveDef ultimate;

    public CharacterDef(String id, String name, Group group, Faction faction, Race race, Look look) {
        this.id = id;
        this.name = name;
        this.group = group;
        this.faction = faction;
        this.race = race;
        this.look = look;
    }

    public CharacterDef kagune(KaguneType type, int color, int count) {
        this.kagune = type;
        this.kaguneColor = color;
        this.kaguneCount = count;
        return this;
    }

    public CharacterDef kagune2(KaguneType type, int color) {
        this.kagune2 = type;
        this.kaguneColor2 = color;
        return this;
    }

    public CharacterDef rc(int base) {
        this.rcBase = base;
        return this;
    }

    public CharacterDef hp(float bonus) {
        this.hpBonus = bonus;
        return this;
    }

    public CharacterDef npc(int level, float health, float speed, float armor) {
        this.npcLevel = level;
        this.npcHealth = health;
        this.npcSpeed = speed;
        this.npcArmor = armor;
        return this;
    }

    public CharacterDef quinque(String main, String second) {
        this.quinque = main;
        this.quinque2 = second;
        return this;
    }

    public CharacterDef mask(String item) {
        this.mask = item;
        return this;
    }

    public CharacterDef blurb(String b) {
        this.blurb = b;
        return this;
    }

    public CharacterDef hidden() {
        this.selectable = false;
        return this;
    }

    public CharacterDef generic() {
        this.boss = false;
        return this;
    }

    public CharacterDef move(MoveDef m) {
        moves.add(m);
        return this;
    }

    public CharacterDef ultimate(MoveDef m) {
        this.ultimate = m;
        return this;
    }

    /** Character-specific moves (after the three universal attacks), unlock order. */
    public List<MoveDef> moves() {
        return Collections.unmodifiableList(moves);
    }

    public MoveDef ultimate() {
        return ultimate;
    }

    public boolean hasKagune() {
        return kagune != KaguneType.NONE;
    }

    /** The full rotation shown with R: universal attacks, character moves, then the ultimate. */
    public List<MoveDef> rotation() {
        List<MoveDef> out = new ArrayList<>(Moves.UNIVERSAL);
        out.addAll(moves);
        if (ultimate != null) out.add(ultimate);
        return out;
    }

    public String key() {
        return "character.tokyoghoul." + id;
    }

    /** Display name; translatable, falling back to the canon name. */
    public net.minecraft.network.chat.MutableComponent title() {
        return net.minecraft.network.chat.Component.translatableWithFallback(key(), name);
    }
}
