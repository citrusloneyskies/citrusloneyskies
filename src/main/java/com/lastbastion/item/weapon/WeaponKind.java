package com.lastbastion.item.weapon;

import com.lastbastion.combat.Element;

import static com.lastbastion.item.weapon.SwingStyle.*;

/**
 * The melee arsenal. Every weapon has its own combo (a sequence of {@link SwingStyle}s ending in a
 * finisher), a right-click special that spends Fury, a passive, and Mk II-V upgrades bought at the
 * core (Mk III awakens a second passive, Mk V ascends the special). Stats are Mk I values.
 */
public enum WeaponKind {
    //  id                    title                       class               tier elem             dmg  aps  reach cleave cost
    MILITIA_LONGSWORD("militia_longsword", "Militia Longsword", WeaponClass.LONGSWORD, 1, Element.PHYSICAL, 9f, 1.6f, 3.4f, 3, 250,
            0xD8E0EA, 0xFFFFFF, new SwingStyle[]{SLASH_R, SLASH_L, THRUST},
            "Riposte", 8, 30, "Raise your guard for a moment. Any blow that lands is parried and answered with a counter-cut for triple damage.",
            "Drilled", "Every third swing is a lunging thrust that ignores armor.",
            "Veteran's Edge", "Parrying grants 4s of +25% damage.",
            "Riposte reflects projectiles and staggers everything in front of you.",
            "Plain steel, honest weight. Every defender's first blade."),
    FROSTFANG("frostfang", "Frostfang", WeaponClass.DAGGER, 2, Element.ICE, 6f, 2.6f, 2.9f, 1, 850,
            0x9FEAFF, 0xFFFFFF, new SwingStyle[]{SLASH_R, SLASH_L, THRUST, SLASH_R, UPPERCUT},
            "Shadow Step", 10, 40, "Blink behind the nearest enemy you are looking at and drive the fang into its back for 4x damage, freezing it solid.",
            "Rime", "Hits stack Chill; five stacks Freeze the target. Striking a Frozen enemy shatters it for +100% damage.",
            "Cold Blood", "Kills refund 40% of Shadow Step's cooldown.",
            "Shadow Step leaves a frost nova where you vanished.",
            "A sliver of glacier heart that never melts. It drinks warmth from whatever it cuts."),
    BONECLEAVER("bonecleaver", "Bonecleaver", WeaponClass.AXE, 2, Element.PHYSICAL, 13f, 1.0f, 3.6f, 4, 800,
            0xE8DCC0, 0xFF5040, new SwingStyle[]{DIAG_DOWN, DIAG_UP, SPIN},
            "Whirlwind", 12, 50, "Spin like a storm of bone for 2.5 seconds, carving everything around you. You can move while spinning.",
            "Butcher", "Hits cause Bleeding; finishers hit everything around you.",
            "Bloodlust", "Each bleeding enemy nearby gives +6% attack speed (max +30%).",
            "Whirlwind lasts twice as long and pulls enemies inward.",
            "Hewn from the jaw of something that should not have been that large."),
    VOIDEDGE("voidedge", "Voidedge", WeaponClass.KATANA, 3, Element.ARCANE, 11f, 1.8f, 3.5f, 3, 1100,
            0xB04AFF, 0xF0D8FF, new SwingStyle[]{SLASH_R, DIAG_UP, SLASH_L, DIAG_DOWN},
            "Phase Rend", 9, 40, "Dash through enemies in a straight line. Every enemy you pass is torn by a void rift that detonates a second later.",
            "Edge of Nothing", "Critical hits leave a void mark; marked enemies take +20% damage from everything.",
            "Echo Blade", "Finishers release a crescent of void energy that flies 8 blocks.",
            "Phase Rend can be chained twice before going on cooldown.",
            "A blade folded from a sliver of the space between stars. It hums when it is hungry."),
    STORMPIERCER("stormpiercer", "Stormpiercer", WeaponClass.SPEAR, 3, Element.LIGHTNING, 12f, 1.3f, 5.0f, 4, 1200,
            0xF4F06A, 0x9FD8FF, new SwingStyle[]{THRUST, THRUST, SLASH_R, THRUST},
            "Thunder Javelin", 10, 45, "Hurl the spear. It explodes in a lightning strike that chains through 6 enemies, then returns to your hand.",
            "Conductor", "Thrusts pierce a whole line of enemies and Shock them.",
            "Static Charge", "Every 4th hit calls a lightning bolt onto the target.",
            "The javelin calls three lightning strikes and chains to 10 enemies.",
            "Forged in a thunderhead. Your hair stands on end when you lift it."),
    EARTHSHAKER("earthshaker", "Earthshaker", WeaponClass.HAMMER, 3, Element.PHYSICAL, 20f, 0.75f, 3.4f, 5, 1250,
            0xFF8A30, 0xFFE0A0, new SwingStyle[]{OVERHEAD, SLASH_R, OVERHEAD},
            "Tectonic Leap", 12, 50, "Leap forward and crash down. The impact knocks enemies into the air and stuns them.",
            "Shockwave", "Overhead smashes send a shockwave along the ground that hits everything near the impact.",
            "Magma Core", "Slams leave burning ground for 4 seconds.",
            "The landing splits the earth in three fissures that erupt in magma.",
            "A dwarven siege hammer with a molten heart. It was never meant to be lifted by one person."),
    DAWNBREAKER("dawnbreaker", "Dawnbreaker", WeaponClass.GREATSWORD, 4, Element.HOLY, 18f, 0.9f, 4.0f, 5, 1600,
            0xFFE070, 0xFFFFFF, new SwingStyle[]{SLASH_R, SLASH_L, OVERHEAD},
            "Radiant Judgement", 14, 60, "Hold to gather the dawn, then release: a pillar of sunlight erupts ahead, burning the undead and healing allies.",
            "Sunfire", "Deals +50% damage to undead. Finishers release a wave of holy light.",
            "Beacon of Hope", "Every enemy killed heals you and allies within 6 blocks.",
            "Radiant Judgement strikes three times in a line and leaves consecrated ground.",
            "Quenched at first light on the last day of the old kingdom."),
    SOULREAPER("soulreaper", "Soulreaper", WeaponClass.SCYTHE, 4, Element.SHADOW, 15f, 1.1f, 4.6f, 6, 1800,
            0x5AFFD8, 0xC8FFF0, new SwingStyle[]{SLASH_L, SLASH_R, SPIN},
            "Harvest", 10, 0, "Consume stored souls to heal and unleash a spinning vortex of scythes. More souls, bigger harvest.",
            "Reaper", "Kills store souls (max 20). Each hit drains life.",
            "Death's Reach", "Finishers pull every enemy in reach toward you.",
            "Harvest's vortex drags enemies inward and executes anything below 15% health.",
            "It does not cut flesh. It cuts the thread that holds the soul to it.");

    public final String id, title;
    public final WeaponClass weaponClass;
    public final int tier;
    public final Element element;
    public final float damage, attacksPerSecond, reach;
    public final int cleave, cost;
    public final int color, color2;
    public final SwingStyle[] combo;
    public final String special;
    public final int specialCooldown;
    /** Fury spent by the special (0 = uses its own resource). */
    public final int specialFury;
    public final String specialDesc, passive, passiveDesc, awakened, awakenedDesc, ascendedDesc, lore;

    WeaponKind(String id, String title, WeaponClass weaponClass, int tier, Element element, float damage, float aps, float reach, int cleave,
               int cost, int color, int color2, SwingStyle[] combo, String special, int specialCooldown, int specialFury, String specialDesc,
               String passive, String passiveDesc, String awakened, String awakenedDesc, String ascendedDesc, String lore) {
        this.id = id;
        this.title = title;
        this.weaponClass = weaponClass;
        this.tier = tier;
        this.element = element;
        this.damage = damage;
        this.attacksPerSecond = aps;
        this.reach = reach;
        this.cleave = cleave;
        this.cost = cost;
        this.color = color;
        this.color2 = color2;
        this.combo = combo;
        this.special = special;
        this.specialCooldown = specialCooldown;
        this.specialFury = specialFury;
        this.specialDesc = specialDesc;
        this.passive = passive;
        this.passiveDesc = passiveDesc;
        this.awakened = awakened;
        this.awakenedDesc = awakenedDesc;
        this.ascendedDesc = ascendedDesc;
        this.lore = lore;
    }

    /** Ticks between swings. */
    public int swingTicks() {
        return Math.max(4, Math.round(20f / attacksPerSecond));
    }

    public static WeaponKind byId(int i) {
        WeaponKind[] v = values();
        return v[Math.floorMod(i, v.length)];
    }

    /** Weapon families: drive the held pose, animation weight and model scale. */
    public enum WeaponClass {
        LONGSWORD(false, 1.0f), DAGGER(false, 0.7f), AXE(true, 1.15f), KATANA(false, 1.0f), SPEAR(true, 1.3f), HAMMER(true, 1.2f),
        GREATSWORD(true, 1.25f), SCYTHE(true, 1.35f);

        public final boolean twoHanded;
        /** Animation weight: heavier weapons swing slower and further. */
        public final float heft;

        WeaponClass(boolean twoHanded, float heft) {
            this.twoHanded = twoHanded;
            this.heft = heft;
        }
    }
}
