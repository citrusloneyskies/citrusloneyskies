package com.lastbastion.entity.hero;

public enum HeroMode {
    GUARD("Guard", "Holds the Siege Core and intercepts anything that approaches it. With no core nearby, guards the spot where you gave the order."),
    HUNT("Hunt", "Roams the battlefield hunting the undead and hostile monsters, bosses and champions first. Away from a core it hunts around you."),
    FOLLOW("Follow Me", "Escorts you everywhere (even through portals), attacks whatever you attack and defends you when you're hit.");

    public final String title, desc;

    HeroMode(String title, String desc) {
        this.title = title;
        this.desc = desc;
    }

    public static HeroMode byId(int i) {
        return i >= 0 && i < values().length ? values()[i] : GUARD;
    }
}
