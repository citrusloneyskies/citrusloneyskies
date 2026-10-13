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
    public int lastHitTick;
    public boolean calledEarly;
    public boolean bossAlive;

    public record FallenHero(UUID owner, int heroKind, int level, int xp) {
    }

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
        return SiegeConfig.BASE_CORE_HEALTH.get() * (1f + 0.3f * level(CoreUpgrade.FORTIFY));
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
        credits.put(w, Math.max(0, credits(player) + amount));
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
            d.fallenHeroes.add(new FallenHero(c.getUUID("o"), c.getInt("k"), c.getInt("l"), c.getInt("x")));
        }
        d.waveTick = tag.getInt("waveTick");
        d.waveTotal = tag.getInt("waveTotal");
        d.waveKilled = tag.getInt("waveKilled");
        d.pulseCooldown = tag.getInt("pulseCd");
        d.calledEarly = tag.getBoolean("early");
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
            fl.add(c);
        }
        tag.put("fallen", fl);
        tag.putInt("waveTick", waveTick);
        tag.putInt("waveTotal", waveTotal);
        tag.putInt("waveKilled", waveKilled);
        tag.putInt("pulseCd", pulseCooldown);
        tag.putBoolean("early", calledEarly);
        return tag;
    }
}
