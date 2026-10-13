package com.lastbastion.entity.hero;

public enum HeroMode {
    GUARD("Guard Core", "Stays near the core and intercepts anything that approaches it."),
    HUNT("Hunt Freely", "Roams the battlefield hunting zombies, prioritising bosses and champions."),
    FOLLOW("Follow Me", "Escorts you and fights whatever threatens you.");

    public final String title, desc;

    HeroMode(String title, String desc) {
        this.title = title;
        this.desc = desc;
    }

    public static HeroMode byId(int i) {
        return i >= 0 && i < values().length ? values()[i] : GUARD;
    }
}
