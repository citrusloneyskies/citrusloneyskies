package com.lastbastion.test;

import com.lastbastion.LastBastion;
import com.lastbastion.entity.boss.BossKind;
import com.lastbastion.entity.boss.SiegeBoss;
import com.lastbastion.entity.hero.HeroEntity;
import com.lastbastion.entity.hero.HeroKind;
import com.lastbastion.entity.hero.HeroMode;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.entity.turret.TurretKind;
import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.entity.zombie.ZombieKind;
import com.lastbastion.entity.zombie.ZombieRank;
import com.lastbastion.entity.zombie.ZombieSpawner;
import com.lastbastion.registry.ModBlocks;
import com.lastbastion.siege.SiegeData;
import com.lastbastion.siege.SiegeManager;
import com.lastbastion.siege.SiegeState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Server-side smoke tests run in CI (runGameTestServer). They exercise every entity's AI, every
 * turret's firing logic, every hero's skills and every boss's moveset in a live world.
 */
@GameTestHolder(LastBastion.MODID)
@PrefixGameTestTemplate(false)
public final class SiegeGameTests {
    private SiegeGameTests() {
    }

    private static BlockPos setup(GameTestHelper h) {
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) h.setBlock(x, 0, z, Blocks.STONE);
        BlockPos rel = new BlockPos(16, 1, 16);
        h.setBlock(rel, ModBlocks.SIEGE_CORE.get());
        BlockPos core = h.absolutePos(rel);
        ServerLevel level = h.getLevel();
        SiegeData d = SiegeManager.data(level);
        d.corePos = null;
        d.state = SiegeState.INACTIVE;
        Component err = SiegeManager.tryPlaceCore(level, core, null);
        if (err != null) h.fail("core placement failed: " + err.getString());
        SiegeManager.forcePresence = true;
        return core;
    }

    private static void teardown(GameTestHelper h, BlockPos core) {
        SiegeManager.onCoreRemoved(h.getLevel(), core);
        SiegeManager.forcePresence = false;
    }

    private static Vec3 ring(BlockPos core, double r, int i, int n) {
        double a = Math.PI * 2 * i / n;
        return new Vec3(core.getX() + 0.5 + Math.cos(a) * r, core.getY(), core.getZ() + 0.5 + Math.sin(a) * r);
    }

    @GameTest(template = "arena", timeoutTicks = 400, batch = "siege_all")
    public static void everythingFights(GameTestHelper h) {
        BlockPos core = setup(h);
        ServerLevel level = h.getLevel();
        for (int i = 0; i < TurretKind.values().length; i++) {
            TurretKind k = TurretKind.values()[i];
            TurretEntity t = k.type().get().create(level);
            Vec3 p = ring(core, 4, i, TurretKind.values().length);
            t.moveTo(p.x, p.y, p.z);
            t.setTurretLevel(1 + i % 5);
            level.addFreshEntity(t);
        }
        for (int i = 0; i < HeroKind.values().length; i++) {
            HeroKind k = HeroKind.values()[i];
            HeroEntity hero = k.type().get().create(level);
            Vec3 p = ring(core, 7, i, HeroKind.values().length);
            hero.moveTo(p.x, p.y + (k.flying ? 3 : 0), p.z);
            hero.finalizeSpawn(level, level.getCurrentDifficultyAt(core), MobSpawnType.EVENT, null, null);
            hero.setMode(i % 2 == 0 ? HeroMode.GUARD : HeroMode.HUNT);
            hero.setLevelAndXp(1 + i % 5, 0);
            level.addFreshEntity(hero);
        }
        for (int i = 0; i < ZombieKind.values().length; i++) {
            ZombieKind k = ZombieKind.values()[i];
            ZombieSpawner.spawn(level, k, ZombieRank.byId(i % 5), i % 3 == 0 ? 1 << (i % 10) : 0, ring(core, 12, i, ZombieKind.values().length), false, true);
        }
        for (int i = 0; i < BossKind.values().length; i++) {
            SiegeBoss.spawn(level, BossKind.values()[i], ring(core, 14, i, BossKind.values().length), 5 + i * 5, 1);
        }
        h.runAfterDelay(380, () -> {
            teardown(h, core);
            h.succeed();
        });
    }

    @GameTest(template = "arena", timeoutTicks = 200, batch = "siege_core")
    public static void zombiesDamageCore(GameTestHelper h) {
        BlockPos core = setup(h);
        ServerLevel level = h.getLevel();
        for (int i = 0; i < 3; i++) ZombieSpawner.spawn(level, ZombieKind.SHAMBLER, ZombieRank.ROTTEN, 0, ring(core, 3, i, 3), false, false);
        h.runAfterDelay(160, () -> {
            SiegeData d = SiegeManager.data(level);
            boolean hurt = d.coreHp < d.maxHp() || d.state == SiegeState.INACTIVE;
            teardown(h, core);
            if (hurt) h.succeed();
            else h.fail("zombies never damaged the core (hp " + d.coreHp + ")");
        });
    }

    @GameTest(template = "arena", timeoutTicks = 300, batch = "siege_turret")
    public static void turretKillsZombie(GameTestHelper h) {
        BlockPos core = setup(h);
        ServerLevel level = h.getLevel();
        TurretEntity t = TurretKind.BALLISTA.type().get().create(level);
        t.moveTo(core.getX() + 3.5, core.getY(), core.getZ() + 0.5);
        t.setTurretLevel(3);
        level.addFreshEntity(t);
        SiegeZombie z = ZombieSpawner.spawn(level, ZombieKind.SHAMBLER, ZombieRank.ROTTEN, 0,
                new Vec3(core.getX() + 12.5, core.getY(), core.getZ() + 0.5), false, false, 1f, 1f);
        h.succeedWhen(() -> {
            if (z != null && z.isAlive()) h.fail("zombie still alive");
            teardown(h, core);
        });
    }

    @GameTest(template = "arena", timeoutTicks = 400, batch = "siege_wave")
    public static void waveSpawnsAndTracks(GameTestHelper h) {
        BlockPos core = setup(h);
        ServerLevel level = h.getLevel();
        SiegeData d = SiegeManager.data(level);
        d.wave = 10;
        SiegeManager.startWave(level, d, false);
        h.runAfterDelay(300, () -> {
            int tracked = d.tracked.size();
            int queued = d.queue.size();
            teardown(h, core);
            if (tracked > 0 || queued > 0 || d.waveKilled > 0) h.succeed();
            else h.fail("wave 10 spawned nothing");
        });
    }

    @GameTest(template = "arena", timeoutTicks = 60, batch = "siege_shop")
    public static void shopPurchaseAndUpgrade(GameTestHelper h) {
        BlockPos core = setup(h);
        ServerLevel level = h.getLevel();
        net.minecraft.server.level.ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.moveTo(core.getX() + 2.5, core.getY(), core.getZ() + 0.5);
        SiegeData d = SiegeManager.data(level);
        d.credits.put(p.getUUID(), 2000);
        com.lastbastion.siege.ShopCatalog.purchase(p, "turret_ballista", 0);
        com.lastbastion.siege.ShopCatalog.purchase(p, "upgrade_fortify", 0);
        int left = d.credits(p.getUUID());
        boolean hasKit = p.getInventory().contains(new net.minecraft.world.item.ItemStack(
                com.lastbastion.registry.ModItems.TURRET_KITS.get(TurretKind.BALLISTA).get()));
        int fort = d.level(com.lastbastion.siege.CoreUpgrade.FORTIFY);
        teardown(h, core);
        d.upgrades[com.lastbastion.siege.CoreUpgrade.FORTIFY.ordinal()] = 0;
        if (left == 2000 - 150 - 300 && hasKit && fort == 1) h.succeed();
        else h.fail("shop mismatch: credits=" + left + " kit=" + hasKit + " fortify=" + fort);
    }
}
