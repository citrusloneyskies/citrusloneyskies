package com.tokyoghoul.item;

import com.tokyoghoul.data.KaguneType;

/**
 * Quinques: weapons forged from a ghoul's kakuhou. Each one carries the right-click technique it is
 * known for (IXA's lance, Narukami's lightning...) which anyone holding it can use, like the
 * technique-bearing items of Jujutsu Craft.
 */
public enum QuinqueKind {
    IXA("ixa", "IXA", KaguneType.KOUKAKU, Style.LANCE, 9, -2.6f, "ixa_lance", 0xDCE2EA, 0x7A8A9A),
    NARUKAMI("narukami", "Narukami", KaguneType.UKAKU, Style.CASE_BLASTER, 6, -2.4f, "narukami", 0x2A2E3A, 0x8AD8FF),
    YUKIMURA("yukimura", "Yukimura 1/3", KaguneType.KOUKAKU, Style.SWORD, 8, -2.2f, "yukimura_slash", 0xB8C0CC, 0x2A2A33),
    KURA("kura", "Kura", KaguneType.KOUKAKU, Style.CLUB, 9, -2.8f, "kura_smash", 0x5A4A3A, 0xB0A080),
    DOUJIMA("doujima", "Doujima 1/2", KaguneType.KOUKAKU, Style.GREATCLUB, 11, -3.0f, "doujima_swing", 0x4A4A55, 0xA02030),
    SCORPION("scorpion", "Scorpion 1/56", KaguneType.BIKAKU, Style.KNIFE, 5, -1.6f, "scorpion_knives", 0xC03040, 0x2A2A2A),
    JASON13("jason13", "13's Jason", KaguneType.RINKAKU, Style.SCYTHE, 10, -2.8f, "jason_scythe", 0xA01020, 0x2A2A2A),
    ONIYAMADA("oniyamada", "Oniyamada", KaguneType.KOUKAKU, Style.CLUB, 10, -2.8f, "oniyamada", 0x3A3A3A, 0xE0C060),
    FUEGUCHI("fueguchi", "Fueguchi One", KaguneType.RINKAKU, Style.WHIP, 7, -2.2f, "fueguchi_one", 0xC86A3A, 0x3A2A20),
    AMATSU("amatsu", "Amatsu", KaguneType.UKAKU, Style.SPEAR, 7, -2.4f, "amatsu_volley", 0x6A8ACF, 0xE8E8F0),
    BLADE("quinque_blade", "Standard Quinque", KaguneType.BIKAKU, Style.SWORD, 6, -2.4f, "quinque_thrust", 0xC8CCD4, 0x3A3A44),
    UKAKU("quinque_ukaku", "Ukaku Quinque", KaguneType.UKAKU, Style.CASE_BLASTER, 6, -2.4f, "amatsu_volley", 0x7A4AC0, 0xD0C0F0),
    KOUKAKU("quinque_koukaku", "Koukaku Quinque", KaguneType.KOUKAKU, Style.SHIELD_BLADE, 7, -2.6f, "investigator_guard", 0x6A3A9A, 0xC8C8D0),
    RINKAKU("quinque_rinkaku", "Rinkaku Quinque", KaguneType.RINKAKU, Style.WHIP, 7, -2.3f, "fueguchi_one", 0xB01020, 0x2A2A2A),
    BIKAKU("quinque_bikaku", "Bikaku Quinque", KaguneType.BIKAKU, Style.GREATSWORD, 8, -2.6f, "quinque_spin", 0x9A3A2A, 0xD0D0D8);

    /** Silhouettes for the 3D models. */
    public enum Style {LANCE, SWORD, GREATSWORD, CLUB, GREATCLUB, KNIFE, SCYTHE, WHIP, SPEAR, CASE_BLASTER, SHIELD_BLADE}

    public final String id, name;
    public final KaguneType type;
    public final Style style;
    public final float damage, speed;
    /** Move id used on right-click. */
    public final String move;
    public final int color, accent;

    QuinqueKind(String id, String name, KaguneType type, Style style, float damage, float speed, String move, int color, int accent) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.style = style;
        this.damage = damage;
        this.speed = speed;
        this.move = move;
        this.color = color;
        this.accent = accent;
    }
}
