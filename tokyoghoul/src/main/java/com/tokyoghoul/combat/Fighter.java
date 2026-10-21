package com.tokyoghoul.combat;

import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Faction;

/** Implemented by the mod's NPCs; players get the same view through {@link Fighters}. */
public interface Fighter {
    CharacterDef fighterDef();

    FighterState fighterState();

    int fighterLevel();

    Faction fighterFaction();
}
