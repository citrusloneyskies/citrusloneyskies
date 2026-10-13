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
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
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
import net.minecraft.world.entity.EntityDimensions;
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
 * creative world, poses every zombie (rank I and V), hero, turret (Lv1, Lv3 and both Lv5
 * specialisations) and boss, plus a row of test visual effects, photographs each up close, then lets a
 * real wave play out and photographs the battle before quitting. Used by CI to check the models.
 */
@Mod.EventBusSubscriber(modid = LastBastion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Showcase {
    private static final boolean ENABLED = System.getenv("LB_SHOWCASE") != null;
    private static int stage, ticks, shot;
    private static final int GROUND = -60;
    static final double ZOMBIE_Z = 0, HERO_Z = 20, TURRET_Z = 40, BOSS_Z = 64, VFX_Z = -24;

    private record Shot(String name, Vec3 pos, float yaw, float pitch) {
    }

    private static final List<Shot> SHOTS = new ArrayList<>();
    private static final List<Mob> POSED = new ArrayList<>();

    private Showcase() {
    }

    static double zombieX(int i) {
        return i * 5.0;
    }

    static double heroX(int i) {
        return i * 7.0;
    }

    static double turretX(int i) {
        return i * 11.0;
    }

    static double bossX(int i) {
        return i * 18.0;
    }

    private static void planShots() {
        SHOTS.clear();
        int n = 1;
        double y = GROUND;
        SHOTS.add(new Shot(String.format("%02d_vfx", n++), new Vec3(12, y + 6, VFX_Z - 15), 0, 16));
        SHOTS.add(new Shot(String.format("%02d_core", n++), new Vec3(-10, y + 2.4, -5.2), 0, 14));
        ZombieKind[] zk = ZombieKind.values();
        for (int i = 0; i < zk.length; i += 2) {
            double cx = (zombieX(i) + zombieX(Math.min(zk.length - 1, i + 1)) + 1.8) / 2;
            String label = zk[i].name().toLowerCase() + (i + 1 < zk.length ? "_" + zk[i + 1].name().toLowerCase() : "");
            SHOTS.add(new Shot(String.format("%02d_zombies_%s", n++, label), new Vec3(cx, y + 1.9, ZOMBIE_Z - 5.4), 0, 12));
        }
        HeroKind[] hk = HeroKind.values();
        for (int i = 0; i < hk.length; i++) {
            EntityDimensions d = hk[i].type().get().getDimensions();
            double dist = Math.max(3.4, d.height * 1.55 + d.width);
            SHOTS.add(new Shot(String.format("%02d_hero_%s", n++, hk[i].name().toLowerCase()), new Vec3(heroX(i), y + d.height * 0.62 + 0.3, HERO_Z - dist), 0, 10));
        }
        TurretKind[] tk = TurretKind.values();
        for (int i = 0; i < tk.length; i++) {
            SHOTS.add(new Shot(String.format("%02d_turret_%s_1_3", n++, tk[i].id), new Vec3(turretX(i) + 1.2, y + 1.7, TURRET_Z - 3.6), 0, 14));
            SHOTS.add(new Shot(String.format("%02d_turret_%s_5ab", n++, tk[i].id), new Vec3(turretX(i) + 6.45, y + 1.9, TURRET_Z - 4.0), 0, 12));
        }
        BossKind[] bk = BossKind.values();
        for (int i = 0; i < bk.length; i++) {
            EntityDimensions d = bk[i].type().get().getDimensions();
            double dist = Math.max(6, d.height * 1.7 + d.width);
            SHOTS.add(new Shot(String.format("%02d_boss_%s", n++, bk[i].name().toLowerCase()), new Vec3(bossX(i), y + d.height * 0.6 + 0.5, BOSS_Z - dist), 0, 10));
        }
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
                    planShots();
                    MinecraftServer server = mc.getSingleplayerServer();
                    if (server != null) server.execute(() -> buildScene(server));
                    mc.options.hideGui = true;
                    stage = 2;
                    ticks = 0;
                }
            }
            case 2 -> {
                if (ticks > 90) {
                    stage = 3;
                    ticks = 0;
                    shot = 0;
                    moveTo(mc, SHOTS.get(0));
                }
            }
            case 3 -> {
                if (ticks == 6 && SHOTS.get(shot).name().endsWith("_vfx")) {
                    MinecraftServer server = mc.getSingleplayerServer();
                    if (server != null) server.execute(() -> vfxRow(server.overworld()));
                }
                if (ticks == 45) {
                    grab(mc, SHOTS.get(shot).name());
                } else if (ticks > 48) {
                    shot++;
                    ticks = 0;
                    if (shot >= SHOTS.size()) {
                        stage = 4;
                        MinecraftServer server = mc.getSingleplayerServer();
                        if (server != null) server.execute(() -> startBattle(server));
                        moveTo(mc, new Shot("battle", new Vec3(-10, GROUND + 12, -20), 0, 30));
                    } else {
                        moveTo(mc, SHOTS.get(shot));
                    }
                }
            }
            case 4 -> {
                if (ticks == 160) grab(mc, "90_battle_a");
                if (ticks == 300) grab(mc, "91_battle_b");
                if (ticks == 301) moveTo(mc, new Shot("battle2", new Vec3(-22, GROUND + 4, 0), -90, 12));
                if (ticks == 420) grab(mc, "92_battle_c");
                if (ticks == 421) moveTo(mc, new Shot("battle3", new Vec3(-10, GROUND + 3, -12), 0, 8));
                if (ticks == 520) grab(mc, "93_battle_d");
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
        ZombieKind[] zk = ZombieKind.values();
        for (int i = 0; i < zk.length; i++) {
            double x = zombieX(i);
            SiegeZombie z = ZombieSpawner.spawn(level, zk[i], ZombieRank.ROTTEN, 0, new Vec3(x, GROUND, ZOMBIE_Z), false, false);
            if (z != null) pose(z, level, x, GROUND, ZOMBIE_Z, false);
            SiegeZombie z5 = ZombieSpawner.spawn(level, zk[i], ZombieRank.ABYSSAL, i % 4 == 0 ? 1 << (i % 10) : 0, new Vec3(x + 1.8, GROUND, ZOMBIE_Z), false, false);
            if (z5 != null) pose(z5, level, x + 1.8, GROUND, ZOMBIE_Z, false);
        }
        HeroKind[] hk = HeroKind.values();
        for (int i = 0; i < hk.length; i++) {
            HeroEntity h = hk[i].type().get().create(level);
            if (h == null) continue;
            h.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.ZERO), MobSpawnType.EVENT, null, null);
            h.setLevelAndXp(5, 700);
            pose(h, level, heroX(i), GROUND, HERO_Z, true);
        }
        TurretKind[] tk = TurretKind.values();
        for (int i = 0; i < tk.length; i++) {
            double x0 = turretX(i);
            double[] xs = {x0, x0 + 2.4, x0 + 5.0, x0 + 7.9};
            int[] lv = {1, 3, 5, 5};
            int[] br = {0, 0, 1, 2};
            for (int j = 0; j < 4; j++) {
                TurretEntity t = tk[i].type().get().create(level);
                if (t == null) continue;
                t.setTurretLevel(lv[j]);
                t.setBranch(br[j]);
                pose(t, level, xs[j], GROUND, TURRET_Z, false);
                t.setAim(180, 0);
                level.addFreshEntity(t);
            }
        }
        BossKind[] bk = BossKind.values();
        for (int i = 0; i < bk.length; i++) {
            SiegeBoss b = bk[i].type().get().create(level);
            if (b == null) continue;
            b.setHealth(b.getMaxHealth());
            pose(b, level, bossX(i), GROUND, BOSS_Z, true);
        }
        BlockPos core = new BlockPos(-10, GROUND, 0);
        level.setBlock(core, ModBlocks.SIEGE_CORE.get().defaultBlockState(), 3);
        SiegeManager.tryPlaceCore(level, core, null);
    }

    /** A row of long-lived test effects so the VFX renderer can be checked in one picture. */
    private static void vfxRow(ServerLevel l) {
        double y = GROUND, z = VFX_Z;
        int life = 200;
        Vfx.beam(l, new Vec3(-2, y + 1.5, z), new Vec3(4, y + 1.5, z), 0xC86BFF, 0.3f, life);
        Vfx.arc(l, new Vec3(-2, y + 4, z), new Vec3(4, y + 3, z), 0xF4F06A, 0.08f, life, 0.8f);
        Vfx.slash(l, new Vec3(7, y + 1.5, z), 180, 0, 160, 2.2f, 0xFFE8A0, 0xFFFFFF, 30, 0.4f);
        Vfx.slash(l, new Vec3(7, y + 2.5, z), 180, 80, 140, 1.6f, 0xFF4030, 0xFFE0C0, 30, 0.35f);
        Vfx.pillar(l, new Vec3(11, y, z), 0xFFF2B0, 0.8f, 8, life);
        Vfx.dome(l, new Vec3(15, y, z), 0x6FE3FF, 2.4f, life, null);
        Vfx.vortex(l, new Vec3(20, y, z), 0xE8FFF8, 0x9FFFE8, 1.6f, 5, life, null);
        Vfx.sigil(l, new Vec3(25, y + 0.02, z), 0xC86BFF, 2.4f, life, 1.5f);
        Vfx.teleCircle(l, new Vec3(1, y + 0.02, z + 5), 2.5f, life, Vfx.DANGER, true);
        Vfx.teleLine(l, new Vec3(6, y + 0.02, z + 3), new Vec3(10, y + 0.02, z + 9), 2f, life, Vfx.DANGER_DARK, true);
        Vfx.teleCone(l, new Vec3(14, y + 0.02, z + 3), new Vec3(14, y, z + 9), 6, 30, life, 0xFF8A20, true);
        Vfx.teleDonut(l, new Vec3(22, y + 0.02, z + 6), 1.5f, 3.5f, life, Vfx.DANGER, true);
        Vfx.ring(l, new Vec3(28, y + 0.05, z + 4), 0xFFB040, 3, life, 1);
        Vfx.flash(l, new Vec3(28, y + 2, z), 0xFFB040, 1.5f, life);
        Vfx.wave(l, new Vec3(30, y, z - 2), new Vec3(30, y, z + 8), 0x2F7BFF, 4, 2.5f, life);
        Vfx.spray(l, FxStyle.FLAME, new Vec3(-4, y + 1, z), 0xFFD060, 0xFF3010, 0.5f, 30, 0.1f, 1);
        Vfx.spray(l, FxStyle.STAR, new Vec3(11, y + 3, z), 0xFFFFFF, 0xFFE08A, 0.3f, 20, 0.2f, 0);
        Vfx.jet(l, FxStyle.FLAME, new Vec3(-4, y + 1, z + 6), new Vec3(-4, y + 1, z + 12), 0xFFE070, 0xFF3A08, 0.5f, 40, 12);
    }

    private static void startBattle(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (Mob m : POSED) {
            if (m instanceof SiegeBoss || m instanceof SiegeZombie || m instanceof TurretEntity) m.discard();
        }
        for (Entity e : level.getAllEntities()) {
            if (e instanceof HeroEntity h) {
                h.setNoAi(false);
                h.teleportTo(-10 + level.random.nextGaussian() * 4, GROUND, level.random.nextGaussian() * 4);
            }
        }
        level.setDayTime(18000);
        BlockPos core = new BlockPos(-10, GROUND, 0);
        TurretKind[] tk = TurretKind.values();
        for (int i = 0; i < tk.length; i++) {
            TurretEntity t = tk[i].type().get().create(level);
            if (t == null) continue;
            double a = Math.PI * 2 * i / tk.length;
            t.moveTo(core.getX() + 0.5 + Math.cos(a) * 6, GROUND, core.getZ() + 0.5 + Math.sin(a) * 6);
            t.setTurretLevel(3 + i % 3);
            t.setBranch(1 + i % 2);
            level.addFreshEntity(t);
        }
        var d = SiegeManager.data(level);
        d.wave = 15;
        SiegeManager.startWave(level, d, false);
        ZombieKind[] zk = ZombieKind.values();
        for (int i = 0; i < 24; i++) {
            ZombieKind k = zk[i % zk.length];
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
