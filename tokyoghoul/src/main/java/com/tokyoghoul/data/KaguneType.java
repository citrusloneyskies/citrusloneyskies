package com.tokyoghoul.data;

/** The four kagune families (plus chimeras that carry two). */
public enum KaguneType {
    NONE("none"),
    /** Wing type: crystalline shards from the shoulders. Fast, ranged, burns Rc quickly. */
    UKAKU("ukaku"),
    /** Armour type: dense plates from the shoulder blades. Shields, blades and drills. */
    KOUKAKU("koukaku"),
    /** Scale type: tentacles from the lower back. Brutal power and regeneration. */
    RINKAKU("rinkaku"),
    /** Tail type: a single tail from the coccyx. Balanced reach and defence. */
    BIKAKU("bikaku");

    public final String id;

    KaguneType(String id) {
        this.id = id;
    }

    public String key() {
        return "kagune.tokyoghoul." + id;
    }

    public static KaguneType byId(String id) {
        for (KaguneType k : values()) if (k.id.equals(id)) return k;
        return NONE;
    }
}
