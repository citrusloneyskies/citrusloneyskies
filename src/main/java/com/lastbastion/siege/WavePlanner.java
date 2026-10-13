package com.lastbastion.siege;

import com.lastbastion.config.SiegeConfig;
import com.lastbastion.entity.boss.BossKind;
import com.lastbastion.entity.zombie.Affix;
import com.lastbastion.entity.zombie.ZombieKind;
import com.lastbastion.entity.zombie.ZombieRank;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the composition and spawn schedule for a wave.
 *
 * <h3>Difficulty model</h3>
 * <ul>
 *   <li><b>Threat budget</b> = (18 x 1.12^(w-1) + 8(w-1)) x difficulty x player factor. Each zombie costs
 *       points (type cost x rank cost multiplier x 2.5 for champions).</li>
 *   <li><b>Ranks</b> drift upward: the centre rank is (w-1)/7, with a gaussian spread, so wave 8 sees mostly
 *       Feral, wave 15 Armored, wave 22 Corrupted, wave 29+ Abyssal.</li>
 *   <li><b>Health/damage scaling</b>: +7% HP and +3.5% damage per wave on top of rank multipliers.</li>
 *   <li><b>Unlocks</b>: a new archetype appears roughly every wave until wave 14; its debut wave weights it x2.</li>
 *   <li><b>Champions</b> from wave 4: 2% + 0.5%/wave (max 15%), 1-3 affixes as waves climb.</li>
 *   <li><b>Fronts</b>: waves come from 1 direction at first, then 2, 3, 4 and finally all 360 degrees with flanking groups.</li>
 *   <li><b>Bosses</b> every 5th wave, cycling through six bosses; after wave 30 several bosses arrive together.</li>
 * </ul>
 */
public final class WavePlanner {
    public static final int BOSS_OFFSET = 100;

    public record Unit(int kind, int rank, int affixes) {
        public boolean isBoss() {
            return kind >= BOSS_OFFSET;
        }

        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("k", kind);
            t.putInt("r", rank);
            t.putInt("a", affixes);
            return t;
        }

        public static Unit load(CompoundTag t) {
            return new Unit(t.getInt("k"), t.getInt("r"), t.getInt("a"));
        }
    }

    public static final class Group {
        public final int tick;
        public final float angle;
        public final List<Unit> units;

        public Group(int tick, float angle, List<Unit> units) {
            this.tick = tick;
            this.angle = angle;
            this.units = units;
        }

        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("t", tick);
            t.putFloat("a", angle);
            ListTag l = new ListTag();
            for (Unit u : units) l.add(u.save());
            t.put("u", l);
            return t;
        }

        public static Group load(CompoundTag t) {
            List<Unit> us = new ArrayList<>();
            for (Tag x : t.getList("u", Tag.TAG_COMPOUND)) us.add(Unit.load((CompoundTag) x));
            return new Group(t.getInt("t"), t.getFloat("a"), us);
        }
    }

    private WavePlanner() {
    }

    public static float hpScale(int wave) {
        return (float) ((1 + 0.07 * (wave - 1)) * Math.sqrt(SiegeConfig.DIFFICULTY.get()));
    }

    public static float damageScale(int wave) {
        return (float) ((1 + 0.035 * (wave - 1)) * Math.sqrt(SiegeConfig.DIFFICULTY.get()));
    }

    public static float budget(int wave, int players, NightModifier mod) {
        double b = 18 * Math.pow(1.12, wave - 1) + 8 * (wave - 1);
        b *= SiegeConfig.DIFFICULTY.get();
        b *= 1 + 0.5 * Math.max(0, players - 1);
        if (mod == NightModifier.HORDE) b *= 1.6;
        return (float) b;
    }

    public static boolean isBossWave(int wave) {
        return wave % 5 == 0;
    }

    public static List<BossKind> bossesFor(int wave) {
        List<BossKind> out = new ArrayList<>();
        if (!isBossWave(wave)) return out;
        int idx = (wave / 5 - 1);
        int count = wave <= 30 ? 1 : Math.min(3, 1 + (wave - 30) / 15);
        for (int i = 0; i < count; i++) out.add(BossKind.values()[(idx + i * 2) % BossKind.values().length]);
        return out;
    }

    private static float weight(ZombieKind k, int wave) {
        float w = switch (k) {
            case SHAMBLER -> 10f * Math.max(0.3f, 1f - wave * 0.03f);
            case RUNNER -> 6f;
            case SPITTER -> 4f;
            case BRUTE -> 2.5f;
            case BLOATER -> 2.5f;
            case LEAPER -> 3.5f;
            case BULWARK -> 2.5f;
            case BURROWER -> 2.5f;
            case SHRIEKER -> 1.2f;
            case WRAITH -> 2.5f;
            case WRECKER -> 2f;
            case NECROMANCER -> 1f;
            case CRAWLER -> 4.5f;
            case INCINERATOR -> 1.8f;
            case ABOMINATION -> 0.8f;
        };
        if (k.unlockWave == wave) w *= 2f;
        return w;
    }

    public static List<Group> plan(int wave, int players, NightModifier mod, RandomSource rnd) {
        float budget = budget(wave, players, mod);
        List<Unit> units = new ArrayList<>();
        List<BossKind> bosses = bossesFor(wave);
        if (!bosses.isEmpty()) budget *= 0.65f;

        double rankCentre = (wave - 1) / 7.0 - (mod == NightModifier.HORDE ? 1.0 : 0.0);
        float champChance = wave >= 4 ? Math.min(0.15f, 0.02f + 0.005f * wave) : 0f;
        if (mod == NightModifier.ELITE) champChance *= 3f;
        int maxShriekers = 1 + wave / 10, maxNecros = 1 + wave / 12, maxAboms = wave / 8, maxIncin = 1 + wave / 7;
        int shriekers = 0, necros = 0, aboms = 0, incins = 0;

        List<ZombieKind> pool = new ArrayList<>();
        for (ZombieKind k : ZombieKind.values()) if (k.unlockWave <= wave) pool.add(k);

        int guard = 0;
        while (budget > 0.9f && guard++ < 2000) {
            ZombieKind kind = pick(pool, wave, rnd);
            if (kind == ZombieKind.SHRIEKER && shriekers >= maxShriekers) continue;
            if (kind == ZombieKind.NECROMANCER && necros >= maxNecros) continue;
            if (kind == ZombieKind.ABOMINATION && aboms >= maxAboms) continue;
            if (kind == ZombieKind.INCINERATOR && incins >= maxIncin) continue;
            int rank = Mth.clamp((int) Math.round(rankCentre + rnd.nextGaussian() * 0.8), 0, 4);
            int affixes = 0;
            if (rnd.nextFloat() < champChance) {
                int n = 1 + (wave >= 12 ? 1 : 0) + (wave >= 24 ? 1 : 0);
                for (int i = 0; i < n; i++) affixes |= Affix.values()[rnd.nextInt(Affix.values().length)].bit();
            }
            float cost = kind.cost * ZombieRank.byId(rank).costMult * (affixes != 0 ? 2.5f : 1f);
            while (cost > budget * 1.25f && rank > 0) {
                rank--;
                cost = kind.cost * ZombieRank.byId(rank).costMult * (affixes != 0 ? 2.5f : 1f);
            }
            if (cost > budget * 1.25f && affixes != 0) {
                affixes = 0;
                cost = kind.cost * ZombieRank.byId(rank).costMult;
            }
            if (cost > budget * 1.25f && kind != ZombieKind.SHAMBLER) continue;
            budget -= cost;
            if (kind == ZombieKind.SHRIEKER) shriekers++;
            if (kind == ZombieKind.NECROMANCER) necros++;
            if (kind == ZombieKind.ABOMINATION) aboms++;
            if (kind == ZombieKind.INCINERATOR) incins++;
            units.add(new Unit(kind.ordinal(), rank, affixes));
        }

        // Light ordering: weaker units early, heavies and specials later, with noise.
        units.sort(Comparator.comparingDouble(u -> ZombieKind.values()[u.kind()].cost * (1 + u.rank() * 0.4) + rnd.nextFloat() * 3));

        int duration = Math.min(2400 + wave * 60, 5400);
        int fronts = wave < 3 ? 1 : wave < 6 ? 2 : wave < 10 ? 3 : wave < 15 ? 4 : 6;
        float base = rnd.nextFloat() * Mth.TWO_PI;
        float[] frontAngles = new float[fronts];
        for (int i = 0; i < fronts; i++) frontAngles[i] = base + Mth.TWO_PI * i / fronts + (rnd.nextFloat() - 0.5f) * 0.4f;

        List<Group> groups = new ArrayList<>();
        int groupSize = 3 + wave / 5;
        int nGroups = Math.max(1, (int) Math.ceil(units.size() / (double) groupSize));
        for (int g = 0; g < nGroups; g++) {
            List<Unit> chunk = new ArrayList<>(units.subList(g * groupSize, Math.min(units.size(), (g + 1) * groupSize)));
            if (chunk.isEmpty()) continue;
            int tick = (int) (duration * Math.pow(g / (double) nGroups, 0.85)) + 40;
            float angle = frontAngles[g % fronts] + (rnd.nextFloat() - 0.5f) * 0.35f;
            if (wave >= 10 && rnd.nextFloat() < 0.25f) angle = rnd.nextFloat() * Mth.TWO_PI;
            groups.add(new Group(tick, angle, chunk));
        }
        for (int i = 0; i < bosses.size(); i++) {
            List<Unit> b = new ArrayList<>();
            b.add(new Unit(BOSS_OFFSET + bosses.get(i).ordinal(), 0, 0));
            groups.add(new Group(duration / 3 + i * 400, frontAngles[i % fronts], b));
        }
        groups.sort(Comparator.comparingInt(g -> g.tick));
        return groups;
    }

    private static ZombieKind pick(List<ZombieKind> pool, int wave, RandomSource rnd) {
        float total = 0;
        for (ZombieKind k : pool) total += weight(k, wave);
        float r = rnd.nextFloat() * total;
        for (ZombieKind k : pool) {
            r -= weight(k, wave);
            if (r <= 0) return k;
        }
        return pool.get(0);
    }

    /** Count of enemies a wave will field (for UI previews). */
    public static int count(List<Group> groups) {
        int n = 0;
        for (Group g : groups) n += g.units.size();
        return n;
    }

    public static List<Group> shuffleCopy(List<Group> in, RandomSource r) {
        List<Group> copy = new ArrayList<>(in);
        Collections.shuffle(copy, new java.util.Random(r.nextLong()));
        return copy;
    }
}
