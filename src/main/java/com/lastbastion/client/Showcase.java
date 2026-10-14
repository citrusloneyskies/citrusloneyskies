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
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import com.lastbastion.registry.ModItems;
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
    static final double ZOMBIE_Z = 0, HERO_Z = 20, TURRET_Z = 40, BOSS_Z = 64, VFX_Z = -24, ARSENAL_Z = 100;

    /** A photo: camera position and angles; {@code hotbar} >= 0 shows the GUI with that hotbar slot selected. */
    private record Shot(String name, Vec3 pos, float yaw, float pitch, int hotbar) {
        Shot(String name, Vec3 pos, float yaw, float pitch) {
            this(name, pos, yaw, pitch, -1);
        }
    }

    private static final List<Shot> SHOTS = new ArrayList<>();
    private static final List<Mob> POSED = new ArrayList<>();

    private Showcase() {
    }

    static double zombieX(int i) {
        return i * 8.0;
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
        SHOTS.add(new Shot(String.format("%02d_core", n++), new Vec3(-10, y + 2.6, -7.2), 0, 8));
        SHOTS.add(new Shot(String.format("%02d_core_close", n++), new Vec3(-10, y + 2.2, -3.4), 0, 4));
        ZombieKind[] zk = ZombieKind.values();
        for (int i = 0; i < zk.length; i++) {
            EntityDimensions d = zk[i].type().get().getDimensions();
            double h = d.height * 1.18;
            double dist = Math.max(4.2, h * 1.45 + 2.6);
            SHOTS.add(new Shot(String.format("%02d_zombie_%s", n++, zk[i].id), new Vec3(zombieX(i) + 1.7, y + h * 0.55 + 0.35, ZOMBIE_Z - dist), 0, 10));
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
            if (bk[i] == BossKind.MORVATHRAX || bk[i] == BossKind.PLAGUE_MOTHER) {
                // three-quarter view from above so the body, legs and wings read
                double dd = bk[i] == BossKind.MORVATHRAX ? 13 : 8;
                SHOTS.add(new Shot(String.format("%02d_boss_%s", n++, bk[i].name().toLowerCase()),
                        new Vec3(bossX(i) + dd * 0.72, y + d.height + (bk[i] == BossKind.MORVATHRAX ? 5 : 2.5), BOSS_Z - dd * 0.72), 45, 24));
                continue;
            }
            SHOTS.add(new Shot(String.format("%02d_boss_%s", n++, bk[i].name().toLowerCase()), new Vec3(bossX(i), y + d.height * 0.6 + 0.5, BOSS_Z - dist), 0, 10));
        }
        // arsenal: weapon racks, the frame wall, armour sets front and back, and first-person views
        SHOTS.add(new Shot(String.format("%02d_arsenal_melee", n++), new Vec3(10.5, y + 1.9, ARSENAL_Z - 5.2), 0, 12));
        SHOTS.add(new Shot(String.format("%02d_arsenal_guns", n++), new Vec3(10.5, y + 1.9, ARSENAL_Z + 6 - 5.2), 0, 12));
        SHOTS.add(new Shot(String.format("%02d_arsenal_frames", n++), new Vec3(10.5, y + 2.5, ARSENAL_Z + 14 - 6.5), 0, 4));
        SHOTS.add(new Shot(String.format("%02d_armor_sets", n++), new Vec3(7.5, y + 1.4, ARSENAL_Z + 24 - 5.5), 0, 6));
        SHOTS.add(new Shot(String.format("%02d_armor_sets_back", n++), new Vec3(7.5, y + 1.4, ARSENAL_Z + 24 + 5.5), 180, 6));
        int[] fp = {6, 7, 3, 0};
        for (int slot : fp) {
            SHOTS.add(new Shot(String.format("%02d_firstperson_%d", n++, slot), new Vec3(-10, y, -14), 0, 4, slot));
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
                Shot cur = SHOTS.get(shot);
                if (ticks == 2 && mc.player != null) {
                    mc.options.hideGui = cur.hotbar() < 0;
                    if (cur.hotbar() >= 0) mc.player.getInventory().selected = cur.hotbar();
                }
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
                        mc.options.hideGui = true;
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
            double gap = Math.max(1.5, zk[i].type().get().getDimensions().width + 0.7);
            ZombieRank[] ranks = {ZombieRank.ROTTEN, ZombieRank.ARMORED, ZombieRank.ABYSSAL};
            for (int j = 0; j < 3; j++) {
                double x = zombieX(i) + 1.7 + (j - 1) * gap;
                SiegeZombie z = ZombieSpawner.spawn(level, zk[i], ranks[j], 0, new Vec3(x, GROUND, ZOMBIE_Z), false, false);
                if (z != null) pose(z, level, x, GROUND, ZOMBIE_Z, false);
            }
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
        // fully upgraded and battered, so the photo shows every attachment, the cracks and the barrier
        var cd = SiegeManager.data(level);
        for (com.lastbastion.siege.CoreUpgrade u : com.lastbastion.siege.CoreUpgrade.values()) cd.upgrades[u.ordinal()] = u.maxLevel;
        cd.coreHp = cd.maxHp() * 0.5f;
        cd.shield = cd.maxShield();
        SiegeManager.syncHud(level, cd);
        buildArsenal(level);
    }

    private static ArmorStand stand(ServerLevel level, double x, double z, float yaw, String pose) {
        ArmorStand st = EntityType.ARMOR_STAND.create(level);
        if (st == null) return null;
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ShowArms", true);
        tag.putBoolean("NoBasePlate", true);
        tag.putBoolean("NoGravity", true);
        tag.putBoolean("Invulnerable", true);
        try {
            tag.put("Pose", net.minecraft.nbt.TagParser.parseTag(pose));
        } catch (Exception ignored) {
        }
        st.load(tag);
        st.moveTo(x, GROUND, z, yaw, 0);
        st.setYBodyRot(yaw);
        st.setYHeadRot(yaw);
        level.addFreshEntity(st);
        return st;
    }

    /** Weapon racks (armour stands), a wall of item frames and the armour sets on stands. */
    private static void buildArsenal(ServerLevel level) {
        String hold = "{RightArm:[-70f,0f,0f],LeftArm:[-20f,0f,-10f]}";
        com.lastbastion.item.weapon.WeaponKind[] wk = com.lastbastion.item.weapon.WeaponKind.values();
        for (int i = 0; i < wk.length; i++) {
            ArmorStand st = stand(level, 3 + i * 2.2, ARSENAL_Z, 180, hold);
            if (st != null) st.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.MELEE.get(wk[i]).get()));
        }
        String aim = "{RightArm:[-90f,-10f,0f],LeftArm:[-80f,40f,0f]}";
        com.lastbastion.item.GunItem.GunKind[] gk = com.lastbastion.item.GunItem.GunKind.values();
        for (int i = 0; i < gk.length; i++) {
            ArmorStand st = stand(level, 3 + i * 2.2, ARSENAL_Z + 6, 180, aim);
            if (st != null) st.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GUNS.get(gk[i]).get()));
        }
        // frame wall: a stone wall with two rows of frames facing the camera (north)
        int wz = (int) Math.floor(ARSENAL_Z + 14);
        for (int x = 1; x <= 20; x++) for (int yy = 0; yy < 5; yy++) level.setBlock(new BlockPos(x, GROUND + yy, wz), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 3);
        for (int i = 0; i < 16; i++) {
            BlockPos at = new BlockPos(3 + (i % 8) * 2, GROUND + (i < 8 ? 3 : 1), wz - 1);
            ItemFrame f = new ItemFrame(level, at, Direction.NORTH);
            f.setItem(i < 8 ? new ItemStack(ModItems.MELEE.get(wk[i]).get()) : new ItemStack(ModItems.GUNS.get(gk[i - 8]).get()), false);
            level.addFreshEntity(f);
        }
        com.lastbastion.item.armor.ArmorSet[] sets = com.lastbastion.item.armor.ArmorSet.values();
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < sets.length; i++) {
            ArmorStand st = stand(level, 2.5 + i * 2.0, ARSENAL_Z + 24, 180, "{RightArm:[-10f,0f,10f],LeftArm:[-10f,0f,-10f]}");
            if (st == null) continue;
            for (int j = 0; j < 4; j++) st.setItemSlot(slots[j], new ItemStack(ModItems.ARMOR.get(sets[i]).get(j).get()));
            st.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.MELEE.get(wk[(i + 2) % wk.length]).get()));
        }
        // hotbar for the first-person shots
        for (ServerPlayer p : level.players()) {
            p.getInventory().setItem(0, new ItemStack(ModItems.MELEE.get(com.lastbastion.item.weapon.WeaponKind.DAWNBREAKER).get()));
            p.getInventory().setItem(1, new ItemStack(ModItems.MELEE.get(com.lastbastion.item.weapon.WeaponKind.VOIDEDGE).get()));
            p.getInventory().setItem(2, new ItemStack(ModItems.MELEE.get(com.lastbastion.item.weapon.WeaponKind.EARTHSHAKER).get()));
            p.getInventory().setItem(3, new ItemStack(ModItems.MELEE.get(com.lastbastion.item.weapon.WeaponKind.SOULREAPER).get()));
            p.getInventory().setItem(4, new ItemStack(ModItems.MELEE.get(com.lastbastion.item.weapon.WeaponKind.STORMPIERCER).get()));
            p.getInventory().setItem(5, new ItemStack(ModItems.GUNS.get(com.lastbastion.item.GunItem.GunKind.STARPIERCER).get()));
            p.getInventory().setItem(6, new ItemStack(ModItems.MELEE.get(com.lastbastion.item.weapon.WeaponKind.DAWNBREAKER).get()));
            p.getInventory().setItem(7, new ItemStack(ModItems.GUNS.get(com.lastbastion.item.GunItem.GunKind.RAVAGER).get()));
            p.getInventory().setItem(8, new ItemStack(ModItems.GUNS.get(com.lastbastion.item.GunItem.GunKind.HELLFIRE).get()));
            for (int j = 0; j < 4; j++) p.setItemSlot(slots[j], new ItemStack(ModItems.ARMOR.get(com.lastbastion.item.armor.ArmorSet.VANGUARD).get(j).get()));
        }
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
