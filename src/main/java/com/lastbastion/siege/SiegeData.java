package com.lastbastion.siege;

import com.lastbastion.config.SiegeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.*;

/** Persistent siege state for the world (stored with the overworld). */
public class SiegeData extends SavedData {
    public static final String NAME = "lastbastion_siege";
    public static final UUID SHARED_WALLET = new UUID(0L, 0L);

    public SiegeState state = SiegeState.INACTIVE;
    @Nullable
    public BlockPos corePos;
    public String coreDim = "minecraft:overworld";
    @Nullable
    public UUID coreOwner;
    public float coreHp;
    public float shield;
    public int wave = 1;
    public int bestWave = 0;
    public NightModifier modifier = NightModifier.NONE;
    public NightModifier lastModifier = NightModifier.NONE;
    public final int[] upgrades = new int[CoreUpgrade.values().length];
    public final Map<UUID, Integer> credits = new HashMap<>();
    public final Map<UUID, Integer> kills = new HashMap<>();
    public final Set<UUID> tracked = new HashSet<>();
    public final List<WavePlanner.Group> queue = new ArrayList<>();
    public final List<FallenHero> fallenHeroes = new ArrayList<>();
    public int waveTick;
    public int waveTotal;
    public int waveKilled;
    public int pulseCooldown;
    /** Core Protocol cooldowns in ticks, indexed by {@link CoreAbility#ordinal()}. */
    public final int[] abilityCd = new int[CoreAbility.values().length];
    public int aegisTicks;
    public int overdriveTicks;
    public int lastHitTick;
    public boolean calledEarly;
    public boolean bossAlive;
    /** This world's siege rules (defaults come from the server config). */
    public final SiegeRules rules = new SiegeRules();
    /** The next wave, planned as soon as the last one ends so the war room can show intel on it. */
    public final List<WavePlanner.Group> nextQueue = new ArrayList<>();
    public NightModifier nextModifier = NightModifier.NONE;
    /** Wave number and party size {@link #nextQueue} was planned for (0 = not planned yet). */
    public int nextWave, nextPlayers;
    /** True once the core's hull has taken damage this wave (no perfect-defence bonus). */
    public boolean coreHit;
    /** War events still to come this wave: {kind ordinal, wave tick}. */
    public final List<int[]> raidPlan = new ArrayList<>();
    /** Live war-event entities: rifts, warlords, supply crates. */
    public final Set<UUID> raidEntities = new HashSet<>();
    /** Ticks of preparation left before the next wave (Timer trigger). */
    public int prepTicks;
    /** The victory wave has been cleared (the siege continues as endless). */
    public boolean victorious;
    /** Wave tick each tracked enemy was last seen loaded (not saved: after a restart everything gets a fresh grace). */
    public final transient Map<UUID, Integer> trackedSeen = new HashMap<>();
    /** Spawn groups that found no ground this wave (for the "the horde can't reach the core" warning). */
    public transient int spawnStalls;
    /** Wave watchdog: a fingerprint of the wave's progress and when it last changed. */
    public transient int progressSig, progressTick;

    /** A sigil waiting for its owner: {@code recover} ticks of recovery start when it is delivered. */
    public record FallenHero(UUID owner, int heroKind, int level, int xp, int recover) {
    }

    /** A summon out in the world (loaded or not), for the per-player limits. */
    public record Summon(UUID owner, int kind, long lastSeen) {
    }

    /** Every bound hero and devil by entity UUID, so limits hold even while a summon sits in an unloaded chunk. */
    public final Map<UUID, Summon> summons = new HashMap<>();

    public static SiegeData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(SiegeData::load, SiegeData::new, NAME);
    }

    public boolean hasCore() {
        return corePos != null && state != SiegeState.INACTIVE;
    }

    public int level(CoreUpgrade u) {
        return upgrades[u.ordinal()];
    }

    public float maxHp() {
        return SiegeConfig.BASE_CORE_HEALTH.get() * rules.f(SiegeRules.CORE_HEALTH) * (1f + 0.3f * level(CoreUpgrade.FORTIFY));
    }

    public float maxShield() {
        return maxHp() * 0.15f * level(CoreUpgrade.BARRIER);
    }

    private UUID wallet(UUID player) {
        return SiegeConfig.SHARED_CREDITS.get() ? SHARED_WALLET : player;
    }

    public int credits(UUID player) {
        return credits.computeIfAbsent(wallet(player), k -> SiegeConfig.STARTING_CREDITS.get());
    }

    public void addCredits(UUID player, int amount) {
        UUID w = wallet(player);
        // long maths and a ceiling: huge multipliers late in a siege can't wrap a wallet round to zero
        credits.put(w, (int) Math.max(0, Math.min(1_000_000_000L, (long) credits(player) + amount)));
        setDirty();
    }

    public boolean spend(UUID player, int amount) {
        if (credits(player) < amount) return false;
        addCredits(player, -amount);
        return true;
    }

    // ------------------------------------------------------------------ persistence

    public static SiegeData load(CompoundTag tag) {
        SiegeData d = new SiegeData();
        d.state = SiegeState.byId(tag.getInt("state"));
        if (tag.contains("core")) d.corePos = NbtUtils.readBlockPos(tag.getCompound("core"));
        d.coreDim = tag.getString("dim");
        if (tag.hasUUID("owner")) d.coreOwner = tag.getUUID("owner");
        d.coreHp = tag.getFloat("hp");
        d.shield = tag.getFloat("shield");
        d.wave = Math.max(1, tag.getInt("wave"));
        d.bestWave = tag.getInt("best");
        d.modifier = NightModifier.byId(tag.getInt("mod"));
        d.lastModifier = NightModifier.byId(tag.getInt("lastMod"));
        int[] up = tag.getIntArray("upgrades");
        System.arraycopy(up, 0, d.upgrades, 0, Math.min(up.length, d.upgrades.length));
        for (Tag t : tag.getList("credits", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.credits.put(c.getUUID("id"), c.getInt("v"));
            d.kills.put(c.getUUID("id"), c.getInt("k"));
        }
        for (Tag t : tag.getList("tracked", Tag.TAG_INT_ARRAY)) d.tracked.add(NbtUtils.loadUUID(t));
        for (Tag t : tag.getList("queue", Tag.TAG_COMPOUND)) d.queue.add(WavePlanner.Group.load((CompoundTag) t));
        for (Tag t : tag.getList("fallen", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.fallenHeroes.add(new FallenHero(c.getUUID("o"), c.getInt("k"), c.getInt("l"), c.getInt("x"), c.contains("r") ? c.getInt("r") : -1));
        }
        for (Tag t : tag.getList("summons", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            if (c.hasUUID("e") && c.hasUUID("o")) d.summons.put(c.getUUID("e"), new Summon(c.getUUID("o"), c.getInt("k"), c.getLong("t")));
        }
        d.waveTick = tag.getInt("waveTick");
        d.waveTotal = tag.getInt("waveTotal");
        d.waveKilled = tag.getInt("waveKilled");
        d.pulseCooldown = tag.getInt("pulseCd");
        int[] cds = tag.getIntArray("abilityCd");
        System.arraycopy(cds, 0, d.abilityCd, 0, Math.min(cds.length, d.abilityCd.length));
        d.calledEarly = tag.getBoolean("early");
        if (tag.contains("rules")) d.rules.load(tag.getCompound("rules"));
        for (Tag t : tag.getList("nextQueue", Tag.TAG_COMPOUND)) d.nextQueue.add(WavePlanner.Group.load((CompoundTag) t));
        d.nextModifier = NightModifier.byId(tag.getInt("nextMod"));
        d.nextWave = tag.getInt("nextWave");
        d.nextPlayers = tag.getInt("nextPlayers");
        d.coreHit = tag.getBoolean("coreHit");
        int[] plan = tag.getIntArray("raidPlan");
        for (int i = 0; i + 1 < plan.length; i += 2) d.raidPlan.add(new int[]{plan[i], plan[i + 1]});
        for (Tag t : tag.getList("raidEntities", Tag.TAG_INT_ARRAY)) d.raidEntities.add(NbtUtils.loadUUID(t));
        d.prepTicks = tag.getInt("prepTicks");
        d.victorious = tag.getBoolean("victorious");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("state", state.ordinal());
        if (corePos != null) tag.put("core", NbtUtils.writeBlockPos(corePos));
        tag.putString("dim", coreDim);
        if (coreOwner != null) tag.putUUID("owner", coreOwner);
        tag.putFloat("hp", coreHp);
        tag.putFloat("shield", shield);
        tag.putInt("wave", wave);
        tag.putInt("best", bestWave);
        tag.putInt("mod", modifier.ordinal());
        tag.putInt("lastMod", lastModifier.ordinal());
        tag.putIntArray("upgrades", upgrades);
        ListTag cl = new ListTag();
        Set<UUID> ids = new HashSet<>(credits.keySet());
        ids.addAll(kills.keySet());
        for (UUID id : ids) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", id);
            c.putInt("v", credits.getOrDefault(id, SiegeConfig.STARTING_CREDITS.get()));
            c.putInt("k", kills.getOrDefault(id, 0));
            cl.add(c);
        }
        tag.put("credits", cl);
        ListTag tl = new ListTag();
        for (UUID u : tracked) tl.add(NbtUtils.createUUID(u));
        tag.put("tracked", tl);
        ListTag ql = new ListTag();
        for (WavePlanner.Group g : queue) ql.add(g.save());
        tag.put("queue", ql);
        ListTag fl = new ListTag();
        for (FallenHero f : fallenHeroes) {
            CompoundTag c = new CompoundTag();
            c.putUUID("o", f.owner());
            c.putInt("k", f.heroKind());
            c.putInt("l", f.level());
            c.putInt("x", f.xp());
            c.putInt("r", f.recover());
            fl.add(c);
        }
        tag.put("fallen", fl);
        ListTag sl = new ListTag();
        for (Map.Entry<UUID, Summon> e : summons.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("e", e.getKey());
            c.putUUID("o", e.getValue().owner());
            c.putInt("k", e.getValue().kind());
            c.putLong("t", e.getValue().lastSeen());
            sl.add(c);
        }
        tag.put("summons", sl);
        tag.putInt("waveTick", waveTick);
        tag.putInt("waveTotal", waveTotal);
        tag.putInt("waveKilled", waveKilled);
        tag.putInt("pulseCd", pulseCooldown);
        tag.putIntArray("abilityCd", abilityCd);
        tag.putBoolean("early", calledEarly);
        tag.put("rules", rules.save());
        ListTag nq = new ListTag();
        for (WavePlanner.Group g : nextQueue) nq.add(g.save());
        tag.put("nextQueue", nq);
        tag.putInt("nextMod", nextModifier.ordinal());
        tag.putInt("nextWave", nextWave);
        tag.putInt("nextPlayers", nextPlayers);
        tag.putBoolean("coreHit", coreHit);
        int[] plan = new int[raidPlan.size() * 2];
        for (int i = 0; i < raidPlan.size(); i++) {
            plan[i * 2] = raidPlan.get(i)[0];
            plan[i * 2 + 1] = raidPlan.get(i)[1];
        }
        tag.putIntArray("raidPlan", plan);
        ListTag rl = new ListTag();
        for (UUID u : raidEntities) rl.add(NbtUtils.createUUID(u));
        tag.put("raidEntities", rl);
        tag.putInt("prepTicks", prepTicks);
        tag.putBoolean("victorious", victorious);
        return tag;
    }
}
