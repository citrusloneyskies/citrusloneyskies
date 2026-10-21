package com.tokyoghoul.player;

import com.tokyoghoul.combat.FighterState;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Characters;
import com.tokyoghoul.data.Rank;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

/**
 * Everything the mod remembers about a player, the counterpart of Jujutsu Craft's PlayerVariables
 * ({@code PlayerCurseTechnique}, {@code PlayerCursePower}, {@code PlayerLevel}, {@code PlayerFame},
 * {@code PlayerSelectCurseTechnique}...). Saved with the player and copied across death.
 */
public final class TGPlayerData {
    /** Selected character id, empty until the Ghoul Registry book is used. */
    public String character = "";
    public double rc = 0;
    public int level = 1;
    public double fame = 0;
    /** Level of the strongest enemy this player has defeated (gates the upper ranks). */
    public int strongestDefeated = 0;
    /** Kakuja progress 0..100: eating kakuhou and devouring ghouls. Ultimates need 100. */
    public int kakuja = 0;
    /** Index into the character's rotation selected with R. */
    public int selected = 0;
    /** Infinite satiety from the Anteiku thermos (JJK's Insect). */
    public boolean satiated;
    /** Starter kit given (book + thermos), once per player. */
    public boolean starterGiven;
    public int kills;

    /** Not saved. */
    public final transient FighterState state = new FighterState();

    @Nullable
    public CharacterDef def() {
        return Characters.get(character);
    }

    public boolean hasCharacter() {
        return def() != null;
    }

    /** Max Rc: the character's base pool, +50% per level above 1 (JJK: CursePowerFormer * level factor). */
    public double maxRc() {
        CharacterDef d = def();
        double base = d == null ? 50 : d.rcBase;
        return base * (1 + 0.5 * (level - 1));
    }

    public String rankName() {
        CharacterDef d = def();
        return Rank.name(d == null ? com.tokyoghoul.data.Race.HUMAN : d.race, level);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("character", character);
        t.putDouble("rc", rc);
        t.putInt("level", level);
        t.putDouble("fame", fame);
        t.putInt("strongest", strongestDefeated);
        t.putInt("kakuja", kakuja);
        t.putInt("selected", selected);
        t.putBoolean("satiated", satiated);
        t.putBoolean("starter", starterGiven);
        t.putInt("kills", kills);
        return t;
    }

    public void load(CompoundTag t) {
        character = t.getString("character");
        rc = t.getDouble("rc");
        level = Math.max(1, t.getInt("level"));
        fame = t.getDouble("fame");
        strongestDefeated = t.getInt("strongest");
        kakuja = t.getInt("kakuja");
        selected = t.getInt("selected");
        satiated = t.getBoolean("satiated");
        starterGiven = t.getBoolean("starter");
        kills = t.getInt("kills");
    }

    public void copyFrom(TGPlayerData o) {
        load(o.save());
    }
}
