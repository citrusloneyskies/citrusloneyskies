package com.lastbastion.siege.raid;

import com.lastbastion.entity.misc.SupplyCrateEntity;
import com.lastbastion.entity.zombie.Affix;
import com.lastbastion.entity.zombie.RiftEntity;
import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.entity.zombie.ZombieKind;
import com.lastbastion.entity.zombie.ZombieRank;
import com.lastbastion.entity.zombie.ZombieSpawner;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEntities;
import com.lastbastion.registry.ModItems;
import com.lastbastion.siege.SiegeData;
import com.lastbastion.siege.SiegeManager;
import com.lastbastion.siege.SiegeRules;
import com.lastbastion.siege.WavePlanner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

/**
 * War events: mid-wave twists that pull the defenders away from their walls.
 * <ul>
 *   <li><b>Supply Drop</b> - a crate parachutes onto the battlefield. Whoever reaches it first claims credits and
 *   gear; left alone it self-destructs after 90 seconds.</li>
 *   <li><b>Corruption Rift</b> - a portal tears open and pours out zombies until it is destroyed. While one
 *   stands, the wave cannot end.</li>
 *   <li><b>Warlord</b> - a named elite leads a warband. It has its own boss bar, rallies zombies around it and
 *   drops a Warforged Token (a free weapon upgrade).</li>
 * </ul>
 * Events are planned when a wave starts (scaled by the world's {@link SiegeRules}), fired on the wave clock
 * and reported to every defender's HUD as objectives with a direction and distance.
 */
public final class RaidEvents {
    public enum Kind {
        SUPPLY_DROP("Supply Drop", 0x7CFF9B), WARLORD("Warlord", 0xFFB000), RIFT("Corruption Rift", 0xB050FF);
        public final String title;
        public final int color;

        Kind(String title, int color) {
            this.title = title;
            this.color = color;
        }
    }

    /** HUD objective types (see {@link #objectives}). */
    public static final int OBJ_CRATE = 0, OBJ_RIFT = 1, OBJ_WARLORD = 2, OBJ_DOWNED = 3;
    /** Ints per objective: type, x, y, z, value, entity id. */
    public static final int OBJ_STRIDE = 6;

    private static final String[] WARLORD_NAMES = {
            "Gorrak the Unbroken", "Vexis Bonegrinder", "Old Mother Rot", "Skarn of the Pit", "the Hollow King", "Maw-Who-Hungers",
            "Grisk Ironjaw", "Thul the Butcher", "Kesh Hollowheart", "Ulric the Barrow-Lord", "Sister Ashes", "Bruul Gravetooth",
            "the Pale Marshal", "Morrow the Twice-Buried", "Dreg, Eater of Walls", "Varga Nine-Fingers"};

    private RaidEvents() {
    }

    private static int first(Kind k) {
        return switch (k) {
            case SUPPLY_DROP -> 0;
            case WARLORD -> 1;
            case RIFT -> 3;
        };
    }

    private static boolean enabled(SiegeRules r, Kind k) {
        return switch (k) {
            case SUPPLY_DROP -> r.on(SiegeRules.SUPPLY_DROPS);
            case WARLORD -> r.on(SiegeRules.WARLORDS);
            case RIFT -> r.on(SiegeRules.RIFTS);
        };
    }

    // ------------------------------------------------------------------ planning

    /** Plan this wave's war events. Each event debuts on a fixed wave, after which they're drawn at random. */
    public static void plan(ServerLevel level, SiegeData d) {
        d.raidPlan.clear();
        SiegeRules r = d.rules;
        int start = r.i(SiegeRules.EVENT_START);
        if (!r.on(SiegeRules.WAR_EVENTS) || d.wave < start) return;
        RandomSource rnd = level.random;
        int w = d.wave - start;
        List<Kind> kinds = new ArrayList<>();
        if (r.isBossWave(d.wave)) {
            // boss waves are hard enough: only help arrives
            if (enabled(r, Kind.SUPPLY_DROP)) kinds.add(Kind.SUPPLY_DROP);
        } else {
            for (Kind k : Kind.values()) if (enabled(r, k) && first(k) == w) kinds.add(k);
            double expected = (w < 5 ? 1 : w < 12 ? 1.5 : 2.5) * r.get(SiegeRules.EVENT_RATE);
            int n = (int) Math.floor(expected) + (rnd.nextDouble() < expected - Math.floor(expected) ? 1 : 0);
            List<Kind> pool = new ArrayList<>();
            for (Kind k : Kind.values()) if (enabled(r, k) && first(k) <= w) pool.add(k);
            Collections.shuffle(pool, new Random(rnd.nextLong()));
            // up to two of a kind on very busy settings
            int guard = 0;
            while (kinds.size() < n && !pool.isEmpty() && guard++ < 20) {
                Kind k = pool.get(guard % pool.size());
                if (Collections.frequency(kinds, k) < (r.get(SiegeRules.EVENT_RATE) > 1.5 ? 2 : 1)) kinds.add(k);
            }
        }
        int duration = WavePlanner.duration(r, d.wave);
        for (int i = 0; i < kinds.size(); i++) {
            float t = 0.15f + 0.55f * (i + rnd.nextFloat() * 0.8f) / kinds.size();
            d.raidPlan.add(new int[]{kinds.get(i).ordinal(), Math.max(60, (int) (duration * t))});
        }
        d.raidPlan.sort(Comparator.comparingInt(a -> a[1]));
        d.setDirty();
    }

    /** Called every wave tick: fire due events and forget finished ones. */
    public static void tick(ServerLevel level, SiegeData d, BlockPos core) {
        while (!d.raidPlan.isEmpty() && d.raidPlan.get(0)[1] <= d.waveTick) {
            int[] ev = d.raidPlan.remove(0);
            fire(level, d, core, Kind.values()[Mth.clamp(ev[0], 0, Kind.values().length - 1)]);
            d.setDirty();
        }
        if (d.waveTick % 20 == 0 && !d.raidEntities.isEmpty()) {
            if (d.raidEntities.removeIf(id -> SiegeManager.gone(level, d, id))) d.setDirty();
        }
    }

    /** Fire a war event right now (also used by {@code /siege event}). */
    public static boolean fire(ServerLevel level, SiegeData d, BlockPos core, Kind kind) {
        double angle = level.random.nextDouble() * Math.PI * 2;
        return switch (kind) {
            case SUPPLY_DROP -> supplyDrop(level, d, core, angle);
            case RIFT -> rift(level, d, core, angle);
            case WARLORD -> warlord(level, d, core, angle);
        };
    }

    // ------------------------------------------------------------------ supply drop

    private static boolean supplyDrop(ServerLevel level, SiegeData d, BlockPos core, double angle) {
        Vec3 ground = SiegeManager.findSpawn(level, core, angle, 15 + level.random.nextInt(11));
        if (ground == null) return false;
        SupplyCrateEntity crate = ModEntities.SUPPLY_CRATE.get().create(level);
        if (crate == null) return false;
        double top = Math.min(level.getMaxBuildHeight() - 4, ground.y + 42);
        crate.moveTo(ground.x, top, ground.z, level.random.nextFloat() * 360f, 0);
        crate.setWave(d.wave);
        level.addFreshEntity(crate);
        d.raidEntities.add(crate.getUUID());
        Vfx.teleCircle(level, ground.add(0, 0.05, 0), 2.4f, 160, Kind.SUPPLY_DROP.color, false);
        Vfx.pillar(level, ground, Kind.SUPPLY_DROP.color, 0.3f, 48, 80);
        announce(level, d, Component.literal("⚑ SUPPLY DROP ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                .append(Component.literal("inbound " + where(core, ground) + ". Claim it before it self-destructs!").withStyle(ChatFormatting.GRAY)),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2f);
        return true;
    }

    // ------------------------------------------------------------------ corruption rift

    private static boolean rift(ServerLevel level, SiegeData d, BlockPos core, double angle) {
        int radius = Math.max(14, (int) (d.rules.i(SiegeRules.SPAWN_RADIUS) * 0.62));
        Vec3 at = SiegeManager.findSpawn(level, core, angle, radius);
        if (at == null) return false;
        RiftEntity rift = ModEntities.RIFT.get().create(level);
        if (rift == null) return false;
        float yaw = (float) Math.toDegrees(angle) + 90f;
        rift.moveTo(at.x, at.y, at.z, yaw, 0);
        rift.setYBodyRot(yaw);
        rift.setYHeadRot(yaw);
        rift.setup(d.wave, Math.max(1, SiegeManager.presentPlayers(level, core)), d.rules);
        level.addFreshEntity(rift);
        SiegeManager.track(level, rift);
        d.raidEntities.add(rift.getUUID());
        Vfx.pillar(level, at, Kind.RIFT.color, 1.4f, 40, 30);
        Vfx.ring(level, at.add(0, 0.1, 0), Kind.RIFT.color, 6, 20, 1);
        Vfx.shake(level, at, 0.6f, 48, 24);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.4f, 0.7f);
        announce(level, d, Component.literal("☠ CORRUPTION RIFT ").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("tears open " + where(core, at) + ". Close it: the wave can't end while it stands!").withStyle(ChatFormatting.GRAY)),
                SoundEvents.WITHER_AMBIENT, 0.6f);
        return true;
    }

    // ------------------------------------------------------------------ warlord

    private static ZombieKind warlordKind(SiegeRules r, int wave, RandomSource rnd) {
        List<ZombieKind> pool = new ArrayList<>();
        ZombieKind[] candidates = wave < 7 ? new ZombieKind[]{ZombieKind.BRUTE}
                : wave < 12 ? new ZombieKind[]{ZombieKind.BRUTE, ZombieKind.BULWARK}
                : wave < 16 ? new ZombieKind[]{ZombieKind.BRUTE, ZombieKind.BULWARK, ZombieKind.WRECKER}
                : new ZombieKind[]{ZombieKind.BRUTE, ZombieKind.WRECKER, ZombieKind.ABOMINATION, ZombieKind.INCINERATOR};
        for (ZombieKind k : candidates) if (r.zombieEnabled(k)) pool.add(k);
        if (pool.isEmpty()) for (ZombieKind k : ZombieKind.values()) if (r.zombieEnabled(k) && k.unlockWave <= Math.max(wave, 1)) pool.add(k);
        return pool.isEmpty() ? ZombieKind.BRUTE : pool.get(rnd.nextInt(pool.size()));
    }

    private static ZombieKind escortKind(SiegeRules r, int wave, RandomSource rnd) {
        ZombieKind[] infantry = {ZombieKind.SHAMBLER, ZombieKind.RUNNER, ZombieKind.SPITTER, ZombieKind.LEAPER, ZombieKind.CRAWLER, ZombieKind.BULWARK};
        List<ZombieKind> pool = new ArrayList<>();
        for (ZombieKind k : infantry) if (k.unlockWave <= wave && r.zombieEnabled(k)) pool.add(k);
        if (pool.isEmpty()) for (ZombieKind k : ZombieKind.values()) if (k.unlockWave <= wave && r.zombieEnabled(k)) pool.add(k);
        return pool.isEmpty() ? ZombieKind.SHAMBLER : pool.get(rnd.nextInt(pool.size()));
    }

    private static boolean warlord(ServerLevel level, SiegeData d, BlockPos core, double angle) {
        SiegeRules r = d.rules;
        int ring = r.i(SiegeRules.SPAWN_RADIUS);
        Vec3 at = SiegeManager.findSpawn(level, core, angle, ring);
        if (at == null) return false;
        int w = d.wave;
        RandomSource rnd = level.random;
        int rank = Mth.clamp((int) Math.round((w - 1) / 7.0) + 1, 1, 4);
        Affix[] allowed = {Affix.VAMPIRIC, Affix.MOLTEN, Affix.FROSTBOUND, Affix.SHIELDED, Affix.HASTED, Affix.REFLECTIVE, Affix.WARDED};
        int affixes = 0;
        int n = 1 + (w >= 15 ? 1 : 0) + (w >= 30 ? 1 : 0);
        for (int i = 0; i < n; i++) affixes |= allowed[rnd.nextInt(allowed.length)].bit();
        float hp = WavePlanner.hpScale(r, w), dmg = WavePlanner.damageScale(r, w);
        SiegeZombie lord = ZombieSpawner.spawn(level, warlordKind(r, w, rnd), ZombieRank.byId(rank), affixes, at, true, true, hp, dmg);
        if (lord == null) return false;
        String name = WARLORD_NAMES[rnd.nextInt(WARLORD_NAMES.length)];
        lord.makeWarlord(name);
        d.raidEntities.add(lord.getUUID());
        int escorts = Math.min(8, 2 + w / 6);
        int escortRank = Mth.clamp((int) Math.round((w - 1) / 7.0), 0, 4);
        for (int i = 0; i < escorts; i++) {
            Vec3 p = SiegeManager.findSpawn(level, core, angle + (rnd.nextDouble() - 0.5) * 0.3, ring + rnd.nextInt(5) - 2);
            if (p != null) ZombieSpawner.spawn(level, escortKind(r, w, rnd), ZombieRank.byId(escortRank), 0, p, true, true, hp, dmg);
        }
        Vfx.pillar(level, at, Kind.WARLORD.color, 1.1f, 30, 40);
        Vfx.shake(level, at, 0.4f, 40, 16);
        for (ServerPlayer p : SiegeManager.playersNear(level, core, 256)) {
            SiegeManager.title(p, Component.literal("WARLORD").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.literal(name + " leads a warband " + where(core, at) + "!").withStyle(ChatFormatting.YELLOW));
            p.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.6f, 0.6f);
        }
        return true;
    }

    /** A Warlord fell: drop its Warforged Token and tell everyone who did it. */
    public static void onWarlordSlain(ServerLevel level, SiegeZombie lord, DamageSource source) {
        Vec3 at = lord.position();
        com.lastbastion.siege.Milestones.awardSiege(level, com.lastbastion.siege.Milestones.WARLORD);
        dropToken(level, at.add(0, 0.6, 0), 1);
        Vfx.pillar(level, at, Kind.WARLORD.color, 1.2f, 30, 40);
        Vfx.ring(level, at.add(0, 0.1, 0), Kind.WARLORD.color, 6, 18, 1);
        Vfx.spray(level, FxStyle.STAR, at.add(0, 1.5, 0), 0xFFFFFF, Kind.WARLORD.color, 0.35f, 30, 0.35f, 1);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 1.5f, 1f);
        Entity killer = source.getEntity();
        String by = killer == null ? "the defenders" : killer.getDisplayName().getString();
        SiegeData d = SiegeManager.data(level);
        String name = lord.hasCustomName() ? lord.getCustomName().getString() : "The Warlord";
        SiegeManager.broadcast(level, d, Component.literal("⚔ " + name + " has fallen to " + by + "! ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("A Warforged Token lies where it died.").withStyle(ChatFormatting.YELLOW)));
    }

    /** Drop Warforged Tokens (glowing, long-lived). */
    public static void dropToken(ServerLevel level, Vec3 at, int count) {
        ItemEntity it = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(ModItems.WARFORGED_TOKEN.get(), count));
        it.setGlowingTag(true);
        it.lifespan = 20 * 60 * 30;
        it.setDeltaMovement(0, 0.25, 0);
        level.addFreshEntity(it);
    }

    // ------------------------------------------------------------------ lifecycle

    /** Remove every live war-event entity (defeat, core removed). */
    public static void clear(ServerLevel level, SiegeData d) {
        for (UUID id : new java.util.ArrayList<>(d.raidEntities)) {
            Entity e = level.getEntity(id);
            if (e != null) e.discard();
        }
        d.raidEntities.clear();
        d.raidPlan.clear();
        d.setDirty();
    }

    /** HUD objectives: live war events and downed teammates, OBJ_STRIDE ints each. */
    public static int[] objectives(ServerLevel level, SiegeData d) {
        List<Integer> out = new ArrayList<>();
        for (UUID id : new java.util.ArrayList<>(d.raidEntities)) {
            Entity e = level.getEntity(id);
            if (e == null || !e.isAlive()) continue;
            int type, value;
            if (e instanceof SupplyCrateEntity c) {
                type = OBJ_CRATE;
                value = c.secondsLeft();
            } else if (e instanceof RiftEntity rf) {
                type = OBJ_RIFT;
                value = pct(rf);
            } else if (e instanceof SiegeZombie z && z.isWarlord()) {
                type = OBJ_WARLORD;
                value = pct(z);
            } else {
                continue;
            }
            add(out, type, e, value);
        }
        Downed.objectives(level, out);
        int[] a = new int[out.size()];
        for (int i = 0; i < a.length; i++) a[i] = out.get(i);
        return a;
    }

    static void add(List<Integer> out, int type, Entity e, int value) {
        out.add(type);
        out.add(Mth.floor(e.getX()));
        out.add(Mth.floor(e.getY()));
        out.add(Mth.floor(e.getZ()));
        out.add(value);
        out.add(e.getId());
    }

    private static int pct(LivingEntity e) {
        return Math.round(100f * e.getHealth() / Math.max(1f, e.getMaxHealth()));
    }

    // ------------------------------------------------------------------ messaging

    private static void announce(ServerLevel level, SiegeData d, Component msg, @Nullable SoundEvent sound, float pitch) {
        if (d.corePos == null) return;
        for (ServerPlayer p : SiegeManager.playersNear(level, d.corePos, 256)) {
            p.sendSystemMessage(msg);
            if (sound != null) p.playNotifySound(sound, SoundSource.HOSTILE, 1f, pitch);
        }
    }

    /** "to the north-east, 22 blocks out" relative to the core. */
    public static String where(BlockPos core, Vec3 at) {
        double dx = at.x - (core.getX() + 0.5), dz = at.z - (core.getZ() + 0.5);
        return "to the " + compass(dx, dz) + ", " + (int) Math.round(Math.sqrt(dx * dx + dz * dz)) + " blocks out";
    }

    /** Compass name of a horizontal offset (north = -Z, east = +X). */
    public static String compass(double dx, double dz) {
        String[] dirs = {"south", "south-west", "west", "north-west", "north", "north-east", "east", "south-east"};
        double a = Math.toDegrees(Math.atan2(-dx, dz));
        int i = Math.floorMod((int) Math.round(a / 45.0), 8);
        return dirs[i];
    }
}
