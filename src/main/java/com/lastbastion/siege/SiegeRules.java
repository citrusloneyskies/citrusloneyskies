package com.lastbastion.siege;

import com.lastbastion.config.SiegeConfig;
import com.lastbastion.entity.boss.BossKind;
import com.lastbastion.entity.zombie.ZombieKind;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.util.*;
import java.util.function.DoubleSupplier;

/**
 * The rules of one world's siege. A fresh siege copies its defaults from the server config; the core's
 * owner (or an operator) can then retune every one of them from the war room or with {@code /siege rules}.
 *
 * <p>Each rule is a {@link Rule} descriptor (key, label, section, range, step, description) so the
 * persistence, the network sync, the command and the rules screen all work from this one table.
 */
public final class SiegeRules {
    public enum Section {
        DIFFICULTY("Difficulty"), PACING("Pacing"), TWISTS("Night Twists & War Events"), ROSTER("Enemy Roster"),
        STAKES("Economy & Stakes");
        public final String title;

        Section(String title) {
            this.title = title;
        }
    }

    public enum Type {NUMBER, INTEGER, TOGGLE, CHOICE}

    public static final class Rule {
        public final int index;
        public final String key, label, desc;
        public final Section section;
        public final Type type;
        public final double min, max, step;
        public final String[] choices;
        private final DoubleSupplier def;

        private Rule(String key, String label, Section section, Type type, double min, double max, double step, String[] choices,
                     DoubleSupplier def, String desc) {
            this.index = ALL.size();
            this.key = key;
            this.label = label;
            this.section = section;
            this.type = type;
            this.min = min;
            this.max = max;
            this.step = step;
            this.choices = choices;
            this.def = def;
            this.desc = desc;
            ALL.add(this);
            BY_KEY.put(key, this);
        }

        public double defaultValue() {
            return clamp(def.getAsDouble());
        }

        public double clamp(double v) {
            if (Double.isNaN(v)) return defaultValue();
            v = Mth.clamp(v, min, max);
            if (type != Type.NUMBER) v = Math.round(v);
            return v;
        }

        /** Human-readable value for the screen and the command. */
        public String format(double v) {
            return switch (type) {
                case TOGGLE -> v >= 0.5 ? "On" : "Off";
                case CHOICE -> choices[(int) Mth.clamp(Math.round(v), 0, choices.length - 1)];
                case INTEGER -> v == 0 && zeroLabel() != null ? zeroLabel() : String.valueOf((long) v) + unit();
                case NUMBER -> key.endsWith("_pct") ? Math.round(v) + "%" : String.format(Locale.ROOT, "x%.2f", v);
            };
        }

        private String unit() {
            return key.endsWith("_seconds") ? "s" : key.endsWith("_pct") ? "%" : "";
        }

        private String zeroLabel() {
            return switch (key) {
                case "boss_every" -> "Never";
                case "victory_wave" -> "Endless";
                default -> null;
            };
        }
    }

    public static final List<Rule> ALL = new ArrayList<>();
    private static final Map<String, Rule> BY_KEY = new LinkedHashMap<>();

    private static Rule num(String key, String label, Section s, double min, double max, double step, DoubleSupplier def, String desc) {
        return new Rule(key, label, s, Type.NUMBER, min, max, step, null, def, desc);
    }

    private static Rule integer(String key, String label, Section s, double min, double max, double step, DoubleSupplier def, String desc) {
        return new Rule(key, label, s, Type.INTEGER, min, max, step, null, def, desc);
    }

    private static Rule toggle(String key, String label, Section s, boolean def, String desc) {
        return new Rule(key, label, s, Type.TOGGLE, 0, 1, 1, null, () -> def ? 1 : 0, desc);
    }

    private static Rule toggle(String key, String label, Section s, java.util.function.BooleanSupplier def, String desc) {
        return new Rule(key, label, s, Type.TOGGLE, 0, 1, 1, null, () -> def.getAsBoolean() ? 1 : 0, desc);
    }

    private static Rule choice(String key, String label, Section s, String[] choices, int def, String desc) {
        return new Rule(key, label, s, Type.CHOICE, 0, choices.length - 1, 1, choices, () -> def, desc);
    }

    // ------------------------------------------------------------------ the rules

    public static final String[] PRESETS = {"Casual", "Normal", "Veteran", "Nightmare", "Custom"};
    public static final int CASUAL = 0, NORMAL = 1, VETERAN = 2, NIGHTMARE = 3, CUSTOM = 4;

    public static final Rule PRESET = choice("preset", "Difficulty preset", Section.DIFFICULTY, PRESETS, NORMAL,
            "Sets every difficulty multiplier at once. Changing any of them by hand switches to Custom.");
    public static final Rule ENEMY_HEALTH = num("enemy_health", "Enemy health", Section.DIFFICULTY, 0.25, 5, 0.05,
            () -> Math.sqrt(SiegeConfig.DIFFICULTY.get()), "Multiplies the health of every zombie (bosses have their own multiplier).");
    public static final Rule ENEMY_DAMAGE = num("enemy_damage", "Enemy damage", Section.DIFFICULTY, 0.25, 5, 0.05,
            () -> Math.sqrt(SiegeConfig.DIFFICULTY.get()), "Multiplies the damage zombies and bosses deal.");
    public static final Rule WAVE_SIZE = num("wave_size", "Horde size", Section.DIFFICULTY, 0.25, 5, 0.05,
            SiegeConfig.DIFFICULTY::get, "Multiplies the threat budget of every wave: more and stronger zombies.");
    public static final Rule GROWTH = num("growth", "Escalation", Section.DIFFICULTY, 0.5, 2, 0.05, () -> 1,
            "How steeply waves grow from one night to the next.");
    public static final Rule BOSS_HEALTH = num("boss_health", "Boss health", Section.DIFFICULTY, 0.25, 5, 0.05,
            () -> Math.sqrt(SiegeConfig.DIFFICULTY.get()), "Multiplies the health of bosses.");
    public static final Rule CHAMPIONS = num("champions", "Champion frequency", Section.DIFFICULTY, 0, 5, 0.1, () -> 1,
            "How often zombies spawn as affixed champions (0 = never).");
    public static final Rule CORE_HEALTH = num("core_health", "Core durability", Section.DIFFICULTY, 0.25, 5, 0.05, () -> 1,
            "Multiplies the Siege Core's health.");

    public static final String[] TRIGGERS = {"Nightfall", "Timer", "War horn only"};
    public static final int TRIGGER_NIGHT = 0, TRIGGER_TIMER = 1, TRIGGER_MANUAL = 2;
    public static final Rule TRIGGER = choice("trigger", "Waves begin at", Section.PACING, TRIGGERS, TRIGGER_NIGHT,
            "Nightfall: each night brings a wave. Timer: a wave follows every preparation phase, day or night. War horn only: waves start when you sound the horn.");
    public static final Rule PREP_SECONDS = integer("prep_seconds", "Preparation time", Section.PACING, 20, 1200, 10, () -> 180,
            "Seconds between waves in Timer mode.");
    public static final Rule BOSS_EVERY = integer("boss_every", "Boss wave every", Section.PACING, 0, 20, 1, () -> 5,
            "A boss leads every Nth wave (0 = never).");
    public static final Rule VICTORY_WAVE = integer("victory_wave", "Victory at wave", Section.PACING, 0, 500, 5, () -> 0,
            "Clearing this wave wins the siege (the horde keeps coming if you want more). 0 = endless.");
    public static final Rule MAX_ALIVE = integer("max_alive", "Max enemies alive", Section.PACING, 10, 400, 5, SiegeConfig.MAX_ALIVE::get,
            "Hard cap on simultaneously living siege enemies (lower it on weak servers).");
    public static final Rule SPAWN_RADIUS = integer("spawn_radius", "Spawn distance", Section.PACING, 16, 96, 2, SiegeConfig.SPAWN_RADIUS::get,
            "How far from the core the horde rises.");
    public static final Rule WAVE_LENGTH = num("wave_length", "Wave duration", Section.PACING, 0.4, 3, 0.1, () -> 1,
            "Stretches (or compresses) how long a wave keeps sending groups.");

    public static final Rule NIGHT_TWISTS = toggle("night_twists", "Special nights", Section.TWISTS, SiegeConfig.NIGHT_MODIFIERS::get,
            "Blood Moons, fog, storms, hordes and more.");
    public static final Rule TWIST_CHANCE = integer("twist_chance_pct", "Special night chance", Section.TWISTS, 5, 100, 5, () -> 35,
            "Chance that a wave is a special night (never twice in a row unless 100%).");
    public static final Rule TWIST_START = integer("twist_start", "Special nights from wave", Section.TWISTS, 1, 100, 1, () -> 6,
            "First wave that can roll a special night.");
    public static final Map<NightModifier, Rule> TWISTS = new EnumMap<>(NightModifier.class);
    public static final Rule WAR_EVENTS = toggle("war_events", "War events", Section.TWISTS, SiegeConfig.WAR_EVENTS::get,
            "Supply drops, Corruption Rifts and Warlords erupt in the middle of waves.");
    public static final Rule EVENT_RATE = num("event_rate", "War event frequency", Section.TWISTS, 0.25, 3, 0.25, () -> 1,
            "Multiplies how many war events each wave throws at you.");
    public static final Rule EVENT_START = integer("event_start", "War events from wave", Section.TWISTS, 1, 100, 1, () -> 3,
            "First wave that can have war events.");
    public static final Rule SUPPLY_DROPS = toggle("supply_drops", "Supply drops", Section.TWISTS, true,
            "Crates parachute onto the battlefield: grab them for credits and gear before they self-destruct.");
    public static final Rule RIFTS = toggle("rifts", "Corruption Rifts", Section.TWISTS, true,
            "Portals that pour out zombies until destroyed. The wave can't end while one stands.");
    public static final Rule WARLORDS = toggle("warlords", "Warlords", Section.TWISTS, true,
            "Named elite zombies leading a warband; they rally the horde and drop Warforged Tokens.");

    public static final Map<ZombieKind, Rule> ZOMBIES = new EnumMap<>(ZombieKind.class);
    public static final Map<BossKind, Rule> BOSSES = new EnumMap<>(BossKind.class);
    public static final Rule BREAK_BLOCKS = toggle("break_blocks", "Zombies breach blocks", Section.ROSTER, SiegeConfig.ZOMBIES_BREAK_BLOCKS::get,
            "Zombies tear through blocks in their way (barricades can always be breached).");

    public static final Rule CREDITS = num("credits", "Credit rewards", Section.STAKES, 0.1, 10, 0.05, () -> SiegeConfig.CREDIT_MULTIPLIER.get(),
            "Multiplies every credit reward.");
    public static final Rule DOWNED = toggle("downed", "Downed & revive", Section.STAKES, SiegeConfig.DOWNED::get,
            "With teammates nearby, a fatal blow downs you instead: crawl and wait for a teammate to revive you.");
    public static final Rule BLEED_OUT = integer("bleed_out_seconds", "Bleed-out time", Section.STAKES, 5, 300, 5, SiegeConfig.BLEED_OUT_SECONDS::get,
            "How long a downed player survives without help.");
    public static final Rule HARSH_DEFEAT = toggle("harsh_defeat", "Harsh defeat", Section.STAKES, SiegeConfig.HARSH_DEFEAT::get,
            "When the core falls, turrets and heroes fall with it and core upgrades are lost.");
    public static final Rule SALVAGE = integer("salvage_pct", "Credits kept on defeat", Section.STAKES, 0, 100, 5,
            () -> SiegeConfig.SALVAGE_FRACTION.get() * 100, "Share of every wallet that survives a defeat.");
    public static final Rule PERFECT_BONUS = integer("perfect_bonus_pct", "Perfect defence bonus", Section.STAKES, 0, 300, 10, () -> 50,
            "Extra wave reward when the core's hull takes no damage all wave.");

    static {
        for (NightModifier m : NightModifier.values()) {
            if (m == NightModifier.NONE) continue;
            TWISTS.put(m, toggle("twist_" + m.name().toLowerCase(Locale.ROOT), m.title, Section.TWISTS, true, m.desc));
        }
        for (ZombieKind k : ZombieKind.values()) {
            String name = k.id.substring(0, 1).toUpperCase(Locale.ROOT) + k.id.substring(1);
            ZOMBIES.put(k, toggle("zombie_" + k.id, name + "s", Section.ROSTER, true, k.role + " (from wave " + k.unlockWave + ")"));
        }
        for (BossKind b : BossKind.values()) {
            BOSSES.put(b, toggle("boss_" + b.id, b.title, Section.ROSTER, true, "Include this boss in boss waves."));
        }
    }

    // preset table: health, damage, size, growth, boss health, champions, bleed-out, harsh defeat, salvage %, credits
    private static final double[][] PRESET_VALUES = {
            {0.6, 0.6, 0.7, 0.85, 0.6, 0.5, 60, 0, 75, 1.25},
            {1, 1, 1, 1, 1, 1, 30, 1, 25, 1},
            {1.35, 1.25, 1.25, 1.1, 1.3, 1.5, 20, 1, 15, 1},
            {1.8, 1.6, 1.6, 1.2, 1.7, 2.5, 15, 1, 10, 0.9},
    };
    private static final Rule[] PRESET_RULES = {ENEMY_HEALTH, ENEMY_DAMAGE, WAVE_SIZE, GROWTH, BOSS_HEALTH, CHAMPIONS, BLEED_OUT, HARSH_DEFEAT, SALVAGE, CREDITS};

    // ------------------------------------------------------------------ instance

    private final double[] values = new double[ALL.size()];

    public SiegeRules() {
        reset();
    }

    public void reset() {
        for (Rule r : ALL) values[r.index] = r.defaultValue();
        values[PRESET.index] = matchingPreset();
    }

    public double get(Rule r) {
        return values[r.index];
    }

    public float f(Rule r) {
        return (float) values[r.index];
    }

    public int i(Rule r) {
        return (int) Math.round(values[r.index]);
    }

    public boolean on(Rule r) {
        return values[r.index] >= 0.5;
    }

    /** Set a rule (clamped). Applying a preset fills its multipliers; retuning one of them switches to Custom. */
    public void set(Rule r, double v) {
        v = r.clamp(v);
        values[r.index] = v;
        if (r == PRESET) {
            int p = (int) v;
            if (p < PRESET_VALUES.length) {
                for (int k = 0; k < PRESET_RULES.length; k++) values[PRESET_RULES[k].index] = PRESET_RULES[k].clamp(PRESET_VALUES[p][k]);
            }
            return;
        }
        int idx = presetSlot(r);
        if (idx >= 0) {
            int p = i(PRESET);
            if (p >= PRESET_VALUES.length || Math.abs(PRESET_VALUES[p][idx] - v) > 1e-6) values[PRESET.index] = CUSTOM;
        }
    }

    private static int presetSlot(Rule r) {
        for (int k = 0; k < PRESET_RULES.length; k++) if (PRESET_RULES[k] == r) return k;
        return -1;
    }

    /** The preset whose values all match the current ones (Custom if none does). */
    private int matchingPreset() {
        outer:
        for (int p = 0; p < PRESET_VALUES.length; p++) {
            for (int k = 0; k < PRESET_RULES.length; k++) {
                if (Math.abs(PRESET_RULES[k].clamp(PRESET_VALUES[p][k]) - values[PRESET_RULES[k].index]) > 1e-6) continue outer;
            }
            return p;
        }
        return CUSTOM;
    }

    @javax.annotation.Nullable
    public static Rule byKey(String key) {
        return BY_KEY.get(key);
    }

    // ------------------------------------------------------------------ derived helpers

    public boolean zombieEnabled(ZombieKind k) {
        return on(ZOMBIES.get(k));
    }

    public boolean bossEnabled(BossKind k) {
        return on(BOSSES.get(k));
    }

    public boolean twistEnabled(NightModifier m) {
        Rule r = TWISTS.get(m);
        return r != null && on(r);
    }

    public boolean isBossWave(int wave) {
        int every = i(BOSS_EVERY);
        if (every <= 0 || wave % every != 0) return false;
        for (BossKind b : BossKind.values()) if (bossEnabled(b)) return true;
        return false;
    }

    public int trigger() {
        return i(TRIGGER);
    }

    // ------------------------------------------------------------------ persistence / sync

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        for (Rule r : ALL) t.putDouble(r.key, values[r.index]);
        return t;
    }

    /** Load saved values; rules missing from the tag (added in an update) keep their defaults. */
    public void load(CompoundTag t) {
        for (Rule r : ALL) if (t.contains(r.key)) values[r.index] = r.clamp(t.getDouble(r.key));
    }

    public static SiegeRules fromTag(CompoundTag t) {
        SiegeRules r = new SiegeRules();
        r.load(t);
        return r;
    }

    public SiegeRules copy() {
        return fromTag(save());
    }
}
