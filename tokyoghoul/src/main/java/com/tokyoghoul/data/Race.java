package com.tokyoghoul.data;

/** What a character's body is made of. Decides diet, kagune, regeneration and which ranks they earn. */
public enum Race {
    HUMAN(false, false),
    GHOUL(true, true),
    /** Half-ghouls (Kaneki, Eto, Haise): both diets work, one kakugan eye. */
    ONE_EYED(true, true),
    /** Quinx: humans with a surgically implanted kakuhou. Kagune, but human food and CCG ranks. */
    QUINX(false, true);

    /** Needs human flesh and is resistant to ordinary weapons. */
    public final boolean ghoulBody;
    /** Has a kagune at all. */
    public final boolean hasKagune;

    Race(boolean ghoulBody, boolean hasKagune) {
        this.ghoulBody = ghoulBody;
        this.hasKagune = hasKagune;
    }

    /** Ghoul ratings (C..SSS) for ghouls and half-ghouls, CCG ranks for humans and Quinx. */
    public boolean usesGhoulRatings() {
        return this == GHOUL || this == ONE_EYED;
    }
}
