package com.lastbastion.client;

import com.lastbastion.LastBastion;
import com.lastbastion.entity.boss.BossKind;
import com.lastbastion.entity.boss.SiegeBoss;
import com.lastbastion.entity.hero.HeroEntity;
import com.lastbastion.entity.hero.HeroKind;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.entity.turret.TurretKind;
import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.entity.zombie.ZombieKind;
import com.lastbastion.entity.zombie.ZombieRank;
import com.lastbastion.entity.zombie.ZombieSpawner;
import com.lastbastion.registry.ModBlocks;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Developer showcase, enabled with the environment variable {@code LB_SHOWCASE=1}. Creates a flat
 * creative world, lines up every zombie (rank I and V), hero, turret (Lv1 and Lv5) and boss, takes
 * screenshots from preset camera positions, then lets a real wave play out and photographs the
 * battle before quitting. Used by CI to visually verify the procedural models.
 */
@Mod.EventBusSubscriber(modid = LastBastion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Showcase {
    private static final boolean ENABLED = System.getenv("LB_SHOWCASE") != null;
    private static int stage, ticks, shot;
    private static final int GROUND = -60;
    private record Shot(String name, Vec3 pos, float yaw, float pitch) {
    }

    private static final List<Shot> SHOTS = new ArrayList<>();
    private static final List<Mob> POSED = new ArrayList<>();

    static {
        double y = GROUND;
        SHOTS.add(new Shot("01_zombies_rank1_a", new Vec3(6.6, y + 2.5, -9), 0, 10));
        SHOTS.add(new Shot("02_zombies_rank1_b", new Vec3(19.8, y + 2.5, -9), 0, 10));
        SHOTS.add(new Shot("03_zombies_rank5_a", new Vec3(6.6, y + 2.8, 5), 0, 10));
        SHOTS.add(new Shot("04_zombies_rank5_b", new Vec3(19.8, y + 2.8, 5), 0, 10));
        SHOTS.add(new Shot("05_heroes_a", new Vec3(8.5, y + 3, 18), 0, 10));
        SHOTS.add(new Shot("06_heroes_b", new Vec3(35, y + 5, 13), 0, 12));
        SHOTS.add(new Shot("07_turrets_a", new Vec3(5, y + 2.4, 39), 0, 14));
        SHOTS.add(new Shot("08_turrets_b", new Vec3(17.5, y + 2.4, 39), 0, 14));
        SHOTS.add(new Shot("09_bosses_a", new Vec3(7, y + 5, 58), 0, 10));
        SHOTS.add(new Shot("10_bosses_b", new Vec3(35, y + 7, 55), 0, 12));
        SHOTS.add(new Shot("11_bosses_c", new Vec3(63, y + 6, 58), 0, 8));
        SHOTS.add(new Shot("12_core", new Vec3(-10, y + 2.2, -5), 0, 12));
    }

    private Showcase() {
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (!ENABLED || e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        switch (stage) {
            case 0 -> {
                if (ticks > 100) {
                    createWorld(mc);
                    stage = 1;
                    ticks = 0;
                }
            }
            case 1 -> {
                if (mc.level != null && mc.player != null && ticks > 60) {
                    MinecraftServer server = mc.getSingleplayerServer();
                    if (server != null) server.execute(() -> buildScene(server));
                    mc.options.hideGui = true;
                    stage = 2;
                    ticks = 0;
                }
            }
            case 2 -> {
                if (ticks > 80) {
                    stage = 3;
                    ticks = 0;
                    shot = 0;
                    moveTo(mc, SHOTS.get(0));
                }
            }
            case 3 -> {
                if (ticks == 50) {
                    grab(mc, SHOTS.get(shot).name());
                } else if (ticks > 55) {
                    shot++;
                    ticks = 0;
                    if (shot >= SHOTS.size()) {
                        stage = 4;
                        MinecraftServer server = mc.getSingleplayerServer();
                        if (server != null) server.execute(() -> startBattle(server));
                        moveTo(mc, new Shot("battle", new Vec3(-10, GROUND + 18, -28), 0, 28));
                    } else {
                        moveTo(mc, SHOTS.get(shot));
                    }
                }
            }
            case 4 -> {
                if (ticks == 200) grab(mc, "13_battle_a");
                if (ticks == 400) grab(mc, "14_battle_b");
                if (ticks == 401) moveTo(mc, new Shot("battle2", new Vec3(-26, GROUND + 5, 0), -90, 10));
                if (ticks == 520) grab(mc, "15_battle_c");
                if (ticks == 530) {
                    LastBastion.LOGGER.info("[Showcase] complete");
                    mc.stop();
                }
            }
            default -> {
            }
        }
    }

    private static void createWorld(Minecraft mc) {
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        LevelSettings settings = new LevelSettings("lb_showcase_" + System.currentTimeMillis(), GameType.CREATIVE, false,
                Difficulty.NORMAL, true, rules, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(0L, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }

    private static <T extends Mob> T pose(T mob, ServerLevel level, double x, double y, double z, boolean add) {
        mob.moveTo(x, y, z, 180f, 0f);
        mob.setYHeadRot(180f);
        mob.setYBodyRot(180f);
        mob.setNoAi(true);
        if (add) level.addFreshEntity(mob);
        POSED.add(mob);
        return mob;
    }

    private static void buildScene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        for (ServerPlayer p : level.players()) {
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }
        int i = 0;
        for (ZombieKind k : ZombieKind.values()) {
            double x = i * 2.4;
            SiegeZombie z = ZombieSpawner.spawn(level, k, ZombieRank.ROTTEN, 0, new Vec3(x, GROUND, 0), false, false);
            if (z != null) pose(z, level, x, GROUND, 0, false);
            SiegeZombie z5 = ZombieSpawner.spawn(level, k, ZombieRank.ABYSSAL, i % 4 == 0 ? 1 << (i % 10) : 0, new Vec3(x, GROUND, 14), false, false);
            if (z5 != null) pose(z5, level, x, GROUND, 14, false);
            i++;
        }
        double[] heroX = {0, 3.5, 7, 11, 14.5, 18, 24, 31, 38, 48};
        i = 0;
        for (HeroKind k : HeroKind.values()) {
            HeroEntity h = k.type().get().create(level);
            if (h == null) continue;
            h.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.ZERO), MobSpawnType.EVENT, null, null);
            h.setLevelAndXp(5, 700);
            pose(h, level, heroX[i], GROUND + (k.flying ? 1.5 : 0), 28, true);
            i++;
        }
        i = 0;
        for (TurretKind k : TurretKind.values()) {
            TurretEntity t1 = k.type().get().create(level);
            TurretEntity t5 = k.type().get().create(level);
            if (t1 == null || t5 == null) continue;
            t1.setTurretLevel(1);
            t5.setTurretLevel(5);
            pose(t1, level, i * 2.5, GROUND, 44, true);
            pose(t5, level, i * 2.5, GROUND, 49, true);
            i++;
        }
        i = 0;
        for (BossKind k : BossKind.values()) {
            SiegeBoss b = k.type().get().create(level);
            if (b == null) continue;
            b.setHealth(b.getMaxHealth());
            pose(b, level, i * 14, GROUND + (k == BossKind.MORVATHRAX ? 2 : 0), 72, true);
            i++;
        }
        BlockPos core = new BlockPos(-10, GROUND, 0);
        level.setBlock(core, ModBlocks.SIEGE_CORE.get().defaultBlockState(), 3);
        SiegeManager.tryPlaceCore(level, core, null);
    }

    private static void startBattle(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (Mob m : POSED) {
            if (m instanceof SiegeBoss || m instanceof SiegeZombie || m instanceof TurretEntity) m.discard();
        }
        for (Entity e : level.getAllEntities()) {
            if (e instanceof HeroEntity h) {
                h.setNoAi(false);
                h.teleportTo(-10 + level.random.nextGaussian() * 4, GROUND + (h.kind.flying ? 3 : 0), level.random.nextGaussian() * 4);
            }
        }
        BlockPos core = new BlockPos(-10, GROUND, 0);
        for (int i = 0; i < TurretKind.values().length; i++) {
            TurretEntity t = TurretKind.values()[i].type().get().create(level);
            if (t == null) continue;
            double a = Math.PI * 2 * i / TurretKind.values().length;
            t.moveTo(core.getX() + 0.5 + Math.cos(a) * 6, GROUND, core.getZ() + 0.5 + Math.sin(a) * 6);
            t.setTurretLevel(3 + i % 3);
            level.addFreshEntity(t);
        }
        var d = SiegeManager.data(level);
        d.wave = 15;
        SiegeManager.startWave(level, d, false);
        for (int i = 0; i < 24; i++) {
            ZombieKind k = ZombieKind.values()[i % ZombieKind.values().length];
            double a = Math.PI * 2 * i / 24;
            ZombieSpawner.spawn(level, k, ZombieRank.byId(i % 5), 0, new Vec3(core.getX() + Math.cos(a) * 16, GROUND, core.getZ() + Math.sin(a) * 16), true, true);
        }
        SiegeBoss.spawn(level, BossKind.MALGRAVE, new Vec3(core.getX() + 14, GROUND, core.getZ()), 15, 1);
    }

    private static void moveTo(Minecraft mc, Shot s) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) return;
        var id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) p.connection.teleport(s.pos().x, s.pos().y, s.pos().z, s.yaw(), s.pitch());
        });
    }

    private static void grab(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                msg -> LastBastion.LOGGER.info("[Showcase] {}", msg.getString()));
    }
}
