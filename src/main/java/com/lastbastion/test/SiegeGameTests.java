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
            t.setBranch(1 + i % 2);
            level.addFreshEntity(t);
        }
        for (int i = 0; i < HeroKind.values().length; i++) {
            HeroKind k = HeroKind.values()[i];
            HeroEntity hero = k.type().get().create(level);
            Vec3 p = ring(core, 7, i, HeroKind.values().length);
            hero.moveTo(p.x, p.y, p.z);
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

    /** Every level-5 specialisation fires (charged specials included) against a mixed group without errors. */
    @GameTest(template = "arena", timeoutTicks = 460, batch = "siege_specs")
    public static void turretSpecialisations(GameTestHelper h) {
        BlockPos core = setup(h);
        ServerLevel level = h.getLevel();
        TurretKind[] kinds = TurretKind.values();
        for (int i = 0; i < kinds.length * 2; i++) {
            TurretEntity t = kinds[i / 2].type().get().create(level);
            Vec3 p = ring(core, 5, i, kinds.length * 2);
            t.moveTo(p.x, p.y, p.z);
            t.setTurretLevel(5);
            t.setBranch(1 + i % 2);
            level.addFreshEntity(t);
        }
        for (int i = 0; i < 16; i++) {
            ZombieKind k = ZombieKind.values()[i % ZombieKind.values().length];
            ZombieSpawner.spawn(level, k, ZombieRank.byId(2 + i % 3), 0, ring(core, 13, i, 16), false, true);
        }
        h.runAfterDelay(440, () -> {
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
        net.minecraft.server.level.ServerPlayer p = net.minecraftforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "siege_tester"));
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

    @GameTest(template = "arena", timeoutTicks = 80, batch = "siege_protocols")
    public static void coreProtocols(GameTestHelper h) {
        BlockPos core = setup(h);
        ServerLevel level = h.getLevel();
        SiegeData d = SiegeManager.data(level);
        for (com.lastbastion.siege.CoreUpgrade u : com.lastbastion.siege.CoreUpgrade.values()) d.upgrades[u.ordinal()] = 1;
        net.minecraft.server.level.ServerPlayer p = net.minecraftforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "protocol_tester"));
        p.moveTo(core.getX() + 2.5, core.getY(), core.getZ() + 0.5);
        for (int i = 0; i < 4; i++) ZombieSpawner.spawn(level, ZombieKind.SHAMBLER, ZombieRank.ROTTEN, 0, ring(core, 5, i, 4), false, false);
        StringBuilder fails = new StringBuilder();
        for (com.lastbastion.siege.CoreAbility a : com.lastbastion.siege.CoreAbility.values()) {
            SiegeManager.useAbility(level, p, a);
            if (d.abilityCd[a.ordinal()] <= 0) fails.append(a.name()).append(" did not trigger; ");
        }
        float before = d.coreHp;
        SiegeManager.damageCore(level, 50, null);
        if (d.coreHp != before) fails.append("aegis did not block damage; ");
        com.lastbastion.siege.CoreAbility again = com.lastbastion.siege.CoreAbility.NOVA;
        int cd = d.abilityCd[again.ordinal()];
        SiegeManager.useAbility(level, p, again);
        if (d.abilityCd[again.ordinal()] != cd) fails.append("cooldown ignored; ");
        teardown(h, core);
        java.util.Arrays.fill(d.upgrades, 0);
        java.util.Arrays.fill(d.abilityCd, 0);
        d.aegisTicks = 0;
        d.overdriveTicks = 0;
        if (fails.length() == 0) h.succeed();
        else h.fail(fails.toString());
    }

    /** A fake player with the item in hand and its attribute modifiers applied (fake players never tick). */
    private static net.minecraft.server.level.ServerPlayer armedPlayer(ServerLevel level, String name, Vec3 at, net.minecraft.world.item.ItemStack held) {
        net.minecraft.server.level.ServerPlayer p = net.minecraftforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name));
        p.moveTo(at.x, at.y, at.z, 0, 0);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held);
        p.getAttributes().addTransientAttributeModifiers(held.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
        return p;
    }

    private static void face(net.minecraft.server.level.ServerPlayer p, Vec3 target) {
        Vec3 d = target.subtract(p.position());
        float yaw = (float) (Math.atan2(d.z, d.x) * 180.0 / Math.PI) - 90f;
        p.setYRot(yaw);
        p.setYHeadRot(yaw);
        p.setXRot(0);
    }

    /** Every melee weapon lands its full combo on a zombie in front of it and fires its special without errors. */
    @GameTest(template = "arena", timeoutTicks = 240, batch = "siege_melee")
    public static void meleeArsenal(GameTestHelper h) {
        BlockPos core = setup(h);
        ServerLevel level = h.getLevel();
        StringBuilder fails = new StringBuilder();
        com.lastbastion.item.weapon.WeaponKind[] kinds = com.lastbastion.item.weapon.WeaponKind.values();
        for (int i = 0; i < kinds.length; i++) {
            com.lastbastion.item.weapon.WeaponKind k = kinds[i];
            Vec3 at = ring(core, 7, i, kinds.length);
            net.minecraft.world.item.ItemStack w = new net.minecraft.world.item.ItemStack(com.lastbastion.registry.ModItems.MELEE.get(k).get());
            com.lastbastion.item.weapon.Arsenal.setMk(w, 5);
            net.minecraft.server.level.ServerPlayer p = armedPlayer(level, "melee_" + i, at, w);
            Vec3 zp = at.add(new Vec3(core.getX() + 0.5 - at.x, 0, core.getZ() + 0.5 - at.z).normalize().scale(1.6));
            SiegeZombie z = ZombieSpawner.spawn(level, ZombieKind.SHAMBLER, ZombieRank.ROTTEN, 0, zp, false, false, 1f, 1f);
            if (z == null) {
                fails.append(k.id).append(": no zombie; ");
                continue;
            }
            z.setNoAi(true);
            face(p, z.position());
            float hp = z.getHealth();
            for (int s = 0; s < k.combo.length; s++) com.lastbastion.item.weapon.MeleeCombat.swing(level, p, true);
            if (z.isAlive() && z.getHealth() >= hp) fails.append(k.id).append(" never hit; ");
            com.lastbastion.item.weapon.MeleeWeaponItem.setFury(w, 100, level.getGameTime());
            com.lastbastion.item.weapon.MeleeWeaponItem.setSouls(w, 10);
            if (k == com.lastbastion.item.weapon.WeaponKind.DAWNBREAKER) com.lastbastion.item.weapon.MeleeCombat.releaseCharge(level, p, w, k, 30);
            else com.lastbastion.item.weapon.MeleeCombat.special(level, p, w, k);
        }
        h.runAfterDelay(200, () -> {
            teardown(h, core);
            if (fails.length() == 0) h.succeed();
            else h.fail(fails.toString());
        });
    }

    /** Buying a weapon upgrade raises the held weapon's Mk and charges the right price. */
    @GameTest(template = "arena", timeoutTicks = 60, batch = "siege_arsenal_shop")
    public static void weaponUpgradePurchase(GameTestHelper h) {
        BlockPos core = setup(h);
        ServerLevel level = h.getLevel();
        net.minecraft.world.item.ItemStack w = new net.minecraft.world.item.ItemStack(
                com.lastbastion.registry.ModItems.MELEE.get(com.lastbastion.item.weapon.WeaponKind.MILITIA_LONGSWORD).get());
        net.minecraft.server.level.ServerPlayer p = armedPlayer(level, "smith_tester", new Vec3(core.getX() + 2.5, core.getY(), core.getZ() + 0.5), w);
        SiegeData d = SiegeManager.data(level);
        d.credits.put(p.getUUID(), 5000);
        int price = com.lastbastion.item.weapon.Arsenal.upgradeCost(p.getMainHandItem());
        com.lastbastion.siege.ShopCatalog.purchase(p, com.lastbastion.siege.ShopCatalog.UPGRADE_WEAPON, 0);
        int mk = com.lastbastion.item.weapon.Arsenal.mk(p.getMainHandItem());
        int left = d.credits(p.getUUID());
        com.lastbastion.siege.ShopCatalog.purchase(p, "melee_frostfang", 0);
        boolean bought = p.getInventory().countItem(com.lastbastion.registry.ModItems.MELEE.get(com.lastbastion.item.weapon.WeaponKind.FROSTFANG).get()) == 1;
        teardown(h, core);
        if (mk == 2 && price > 0 && left == 5000 - price && bought) h.succeed();
        else h.fail("upgrade mismatch: mk=" + mk + " price=" + price + " credits=" + left + " frostfang=" + bought);
    }

    /** Every gun's primary and alternate fire damage a zombie in front of it; magazines empty and reload. */
    @GameTest(template = "arena", timeoutTicks = 200, batch = "siege_guns")
    public static void gunArsenal(GameTestHelper h) {
        BlockPos core = setup(h);
        ServerLevel level = h.getLevel();
        StringBuilder fails = new StringBuilder();
        com.lastbastion.item.GunItem.GunKind[] kinds = com.lastbastion.item.GunItem.GunKind.values();
        java.util.Map<SiegeZombie, Float> targets = new java.util.HashMap<>();
        java.util.Map<SiegeZombie, String> names = new java.util.HashMap<>();
        for (int i = 0; i < kinds.length; i++) {
            com.lastbastion.item.GunItem.GunKind k = kinds[i];
            Vec3 at = ring(core, 9, i, kinds.length);
            net.minecraft.world.item.ItemStack gun = new net.minecraft.world.item.ItemStack(com.lastbastion.registry.ModItems.GUNS.get(k).get());
            net.minecraft.server.level.ServerPlayer p = armedPlayer(level, "gunner_" + i, at, gun);
            Vec3 out = new Vec3(at.x - core.getX() - 0.5, 0, at.z - core.getZ() - 0.5).normalize();
            SiegeZombie z = ZombieSpawner.spawn(level, ZombieKind.BRUTE, ZombieRank.ROTTEN, 0, at.add(out.scale(4)), false, false, 4f, 1f);
            if (z == null) {
                fails.append(k.name()).append(": no zombie; ");
                continue;
            }
            z.setNoAi(true);
            face(p, z.position());
            targets.put(z, z.getHealth());
            names.put(z, k.name());
            com.lastbastion.item.GunItem g = (com.lastbastion.item.GunItem) gun.getItem();
            if (k.automatic) {
                for (int t = 0; t < 8; t++) g.onUseTick(level, p, gun, g.getUseDuration(gun) - t * k.cooldown);
            } else if (k == com.lastbastion.item.GunItem.GunKind.STARPIERCER) {
                g.releaseUsing(gun, level, p, g.getUseDuration(gun) - 25);
            } else {
                int before = g.ammo(gun);
                g.use(level, p, net.minecraft.world.InteractionHand.MAIN_HAND);
                if (g.ammo(gun) != before - 1 && !(k.mag == 1 && com.lastbastion.item.GunItem.reloading(gun))) {
                    fails.append(k.name()).append(" did not use ammo; ");
                }
            }
            p.setShiftKeyDown(true);
            p.getCooldowns().removeCooldown(g);
            if (!k.automatic && k != com.lastbastion.item.GunItem.GunKind.STARPIERCER) {
                gun.getOrCreateTag().remove(com.lastbastion.item.GunItem.TAG_RELOAD);
                gun.getOrCreateTag().putInt(com.lastbastion.item.GunItem.TAG_AMMO, g.magSize(gun));
            }
            g.use(level, p, net.minecraft.world.InteractionHand.MAIN_HAND);
            p.setShiftKeyDown(false);
        }
        h.runAfterDelay(150, () -> {
            for (java.util.Map.Entry<SiegeZombie, Float> e : targets.entrySet()) {
                SiegeZombie z = e.getKey();
                if (z.isAlive() && z.getHealth() >= e.getValue()) fails.append(names.get(z)).append(" never hit; ");
            }
            teardown(h, core);
            if (fails.length() == 0) h.succeed();
            else h.fail(fails.toString());
        });
    }

    /** Every armour set: pieces add their armour, the full set activates its bonus, and the shop sells whole sets. */
    @GameTest(template = "arena", timeoutTicks = 60, batch = "siege_armor")
    public static void armorSets(GameTestHelper h) {
        BlockPos core = setup(h);
        ServerLevel level = h.getLevel();
        StringBuilder fails = new StringBuilder();
        net.minecraft.world.entity.EquipmentSlot[] slots = {net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
        for (com.lastbastion.item.armor.ArmorSet set : com.lastbastion.item.armor.ArmorSet.values()) {
            net.minecraft.server.level.ServerPlayer p = net.minecraftforge.common.util.FakePlayerFactory.get(level,
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "armor_" + set.id));
            p.moveTo(core.getX() + 3.5, core.getY() + 2, core.getZ() + 0.5);
            double before = p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            for (int i = 0; i < 4; i++) {
                net.minecraft.world.item.ItemStack piece = new net.minecraft.world.item.ItemStack(com.lastbastion.registry.ModItems.ARMOR.get(set).get(i).get());
                p.setItemSlot(slots[i], piece);
                p.getAttributes().addTransientAttributeModifiers(piece.getAttributeModifiers(slots[i]));
            }
            double gained = p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) - before;
            int expected = set.defence[0] + set.defence[1] + set.defence[2] + set.defence[3];
            if (Math.abs(gained - expected) > 0.01) fails.append(set.id).append(" armor ").append(gained).append("!=").append(expected).append("; ");
            if (com.lastbastion.item.armor.ArmorSets.fullSet(p) != set) fails.append(set.id).append(" set not detected; ");
            switch (set) {
                case SCAVENGER -> {
                    if (com.lastbastion.item.armor.ArmorSets.creditMult(p) <= 1f) fails.append("scavenger bonus missing; ");
                }
                case SHADOWSTALKER -> {
                    if (com.lastbastion.item.armor.ArmorSets.critMult(p) <= 1.5f) fails.append("shadowstalker bonus missing; ");
                }
                case ARCANIST -> {
                    if (com.lastbastion.item.armor.ArmorSets.cooldownMult(p) >= 1f) fails.append("arcanist bonus missing; ");
                }
                case TITAN -> {
                    com.lastbastion.item.armor.ArmorSets.jumpJet(p);
                    if (p.getPersistentData().getInt("lb_jets") != 1) fails.append("titan jets did not fire; ");
                }
                default -> {
                }
            }
        }
        net.minecraft.server.level.ServerPlayer buyer = net.minecraftforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "armor_buyer"));
        buyer.moveTo(core.getX() + 2.5, core.getY(), core.getZ() + 0.5);
        SiegeData d = SiegeManager.data(level);
        d.credits.put(buyer.getUUID(), 10000);
        com.lastbastion.siege.ShopCatalog.purchase(buyer, "armor_vanguard", 0);
        int pieces = 0;
        for (var piece : com.lastbastion.registry.ModItems.ARMOR.get(com.lastbastion.item.armor.ArmorSet.VANGUARD)) {
            pieces += buyer.getInventory().countItem(piece.get());
        }
        if (pieces != 4) fails.append("bought vanguard set has ").append(pieces).append(" pieces; ");
        if (d.credits(buyer.getUUID()) != 10000 - com.lastbastion.item.armor.ArmorSet.VANGUARD.cost) fails.append("wrong armour price; ");
        teardown(h, core);
        if (fails.length() == 0) h.succeed();
        else h.fail(fails.toString());
    }

    /** Every war event spawns, shows up as an objective, and pays out: crate claims, warlord tokens, rifts close. */
    @GameTest(template = "arena", timeoutTicks = 120, batch = "siege_raid")
    public static void warEvents(GameTestHelper h) {
        BlockPos core = setup(h);
        ServerLevel level = h.getLevel();
        SiegeData d = SiegeManager.data(level);
        d.wave = 9;
        SiegeManager.startWave(level, d, false);
        d.raidPlan.clear();
        StringBuilder fails = new StringBuilder();
        for (com.lastbastion.siege.raid.RaidEvents.Kind k : com.lastbastion.siege.raid.RaidEvents.Kind.values()) {
            if (!com.lastbastion.siege.raid.RaidEvents.fire(level, d, core, k)) fails.append(k).append(" did not fire; ");
        }
        com.lastbastion.entity.misc.SupplyCrateEntity crate = null;
        com.lastbastion.entity.zombie.RiftEntity rift = null;
        SiegeZombie lord = null;
        for (java.util.UUID id : d.raidEntities) {
            net.minecraft.world.entity.Entity e = level.getEntity(id);
            if (e instanceof com.lastbastion.entity.misc.SupplyCrateEntity c) crate = c;
            if (e instanceof com.lastbastion.entity.zombie.RiftEntity r) rift = r;
            if (e instanceof SiegeZombie z && z.isWarlord()) lord = z;
        }
        if (crate == null || rift == null || lord == null) fails.append("missing event entity crate=").append(crate != null)
                .append(" rift=").append(rift != null).append(" warlord=").append(lord != null).append("; ");
        int[] obj = com.lastbastion.siege.raid.RaidEvents.objectives(level, d);
        if (obj.length < 3 * com.lastbastion.siege.raid.RaidEvents.OBJ_STRIDE) fails.append("objectives missing (").append(obj.length).append("); ");
        if (rift != null && !d.tracked.contains(rift.getUUID())) fails.append("rift not tracked; ");
        net.minecraft.server.level.ServerPlayer p = net.minecraftforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "raid_tester"));
        p.moveTo(core.getX() + 2.5, core.getY(), core.getZ() + 0.5);
        int before = d.credits(p.getUUID());
        if (crate != null) {
            crate.moveTo(core.getX() + 4.5, core.getY(), core.getZ() + 0.5);
            crate.claim(p);
            if (d.credits(p.getUUID()) <= before) fails.append("crate paid nothing; ");
            if (crate.isAlive()) fails.append("crate not consumed; ");
        }
        if (lord != null) {
            lord.hurt(level.damageSources().playerAttack(p), Float.MAX_VALUE);
            boolean token = !level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, lord.getBoundingBox().inflate(4),
                    it -> it.getItem().is(com.lastbastion.registry.ModItems.WARFORGED_TOKEN.get())).isEmpty();
            if (!token) fails.append("warlord dropped no token; ");
        }
        if (rift != null) {
            rift.hurt(level.damageSources().playerAttack(p), Float.MAX_VALUE);
            if (rift.isAlive()) fails.append("rift survived; ");
        }
        h.runAfterDelay(40, () -> {
            com.lastbastion.siege.raid.RaidEvents.clear(level, d);
            teardown(h, core);
            if (fails.length() == 0) h.succeed();
            else h.fail(fails.toString());
        });
    }

    /** Rules: presets fill multipliers, tweaks switch to Custom, values survive a save, and the planner obeys the roster. */
    @GameTest(template = "arena", timeoutTicks = 40, batch = "siege_rules")
    public static void siegeRules(GameTestHelper h) {
        com.lastbastion.siege.SiegeRules r = new com.lastbastion.siege.SiegeRules();
        StringBuilder fails = new StringBuilder();
        r.set(com.lastbastion.siege.SiegeRules.PRESET, com.lastbastion.siege.SiegeRules.NIGHTMARE);
        if (r.get(com.lastbastion.siege.SiegeRules.ENEMY_HEALTH) < 1.5) fails.append("nightmare preset not applied; ");
        r.set(com.lastbastion.siege.SiegeRules.ENEMY_HEALTH, 2.2);
        if (r.i(com.lastbastion.siege.SiegeRules.PRESET) != com.lastbastion.siege.SiegeRules.CUSTOM) fails.append("tweak kept preset; ");
        r.set(com.lastbastion.siege.SiegeRules.MAX_ALIVE, 99999);
        if (r.i(com.lastbastion.siege.SiegeRules.MAX_ALIVE) != 400) fails.append("max alive not clamped; ");
        com.lastbastion.siege.SiegeRules copy = com.lastbastion.siege.SiegeRules.fromTag(r.save());
        if (Math.abs(copy.get(com.lastbastion.siege.SiegeRules.ENEMY_HEALTH) - 2.2) > 1e-6) fails.append("save/load lost a value; ");
        for (ZombieKind k : ZombieKind.values()) r.set(com.lastbastion.siege.SiegeRules.ZOMBIES.get(k), k == ZombieKind.RUNNER ? 1 : 0);
        var plan = com.lastbastion.siege.WavePlanner.plan(r, 1, 1, com.lastbastion.siege.NightModifier.NONE, h.getLevel().random);
        for (var g : plan) for (var u : g.units) if (!u.isBoss() && u.kind() != ZombieKind.RUNNER.ordinal()) fails.append("disabled zombie planned; ");
        if (com.lastbastion.siege.WavePlanner.count(plan) == 0) fails.append("empty roster plan; ");
        r.set(com.lastbastion.siege.SiegeRules.BOSS_EVERY, 3);
        for (BossKind b : BossKind.values()) r.set(com.lastbastion.siege.SiegeRules.BOSSES.get(b), b == BossKind.MALGRAVE ? 1 : 0);
        if (!r.isBossWave(6) || r.isBossWave(5)) fails.append("boss interval ignored; ");
        var bosses = com.lastbastion.siege.WavePlanner.bossesFor(r, 9);
        if (bosses.size() != 1 || bosses.get(0) != BossKind.MALGRAVE) fails.append("boss roster ignored ").append(bosses).append("; ");
        r.set(com.lastbastion.siege.SiegeRules.BOSS_EVERY, 0);
        if (r.isBossWave(10)) fails.append("bosses not disabled; ");
        if (fails.length() == 0) h.succeed();
        else h.fail(fails.toString());
    }

    /** The war room forecast matches the wave that actually starts. */
    @GameTest(template = "arena", timeoutTicks = 40, batch = "siege_intel")
    public static void waveIntel(GameTestHelper h) {
        BlockPos core = setup(h);
        ServerLevel level = h.getLevel();
        SiegeData d = SiegeManager.data(level);
        StringBuilder fails = new StringBuilder();
        if (d.nextWave != d.wave || d.nextQueue.isEmpty()) fails.append("no forecast after placing the core; ");
        int forecast = com.lastbastion.siege.WavePlanner.count(d.nextQueue);
        SiegeManager.syncHud(level, d);
        SiegeManager.startWave(level, d, false);
        if (d.waveTotal != forecast) fails.append("wave differs from forecast ").append(d.waveTotal).append(" vs ").append(forecast).append("; ");
        teardown(h, core);
        if (fails.length() == 0) h.succeed();
        else h.fail(fails.toString());
    }

    /** Downed players crawl, ignore damage, can be revived, and stand up when the wave ends. */
    @GameTest(template = "arena", timeoutTicks = 40, batch = "siege_downed")
    public static void downedRevive(GameTestHelper h) {
        BlockPos core = setup(h);
        ServerLevel level = h.getLevel();
        SiegeData d = SiegeManager.data(level);
        SiegeManager.startWave(level, d, false);
        net.minecraft.server.level.ServerPlayer p = net.minecraftforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "downed_tester"));
        p.moveTo(core.getX() + 3.5, core.getY(), core.getZ() + 0.5);
        StringBuilder fails = new StringBuilder();
        com.lastbastion.siege.raid.Downed.down(p);
        if (!com.lastbastion.siege.raid.Downed.isDowned(p)) fails.append("not downed; ");
        if (p.getForcedPose() != net.minecraft.world.entity.Pose.SWIMMING) fails.append("not crawling; ");
        float hp = p.getHealth();
        p.hurt(level.damageSources().generic(), 50f);
        if (p.getHealth() < hp || !p.isAlive()) fails.append("downed player took damage; ");
        com.lastbastion.siege.raid.Downed.revive(p, null);
        if (com.lastbastion.siege.raid.Downed.isDowned(p)) fails.append("revive failed; ");
        if (p.getForcedPose() != null) fails.append("pose not restored; ");
        if (p.getHealth() < p.getMaxHealth() * 0.3f) fails.append("revived too weak; ");
        com.lastbastion.siege.raid.Downed.down(p);
        com.lastbastion.siege.raid.Downed.reviveAll(level);
        com.lastbastion.siege.raid.Downed.revive(p, null);
        if (com.lastbastion.siege.raid.Downed.isDowned(p)) fails.append("still downed after wave end; ");
        teardown(h, core);
        if (fails.length() == 0) h.succeed();
        else h.fail(fails.toString());
    }

    /**
     * Every devil runs every skill and its ultimate against a horde and a boss (and picks skills on its own in
     * between), calls up hellspawn, and ends idle in a clean state: visible, mortal and on the ground.
     */
    @GameTest(template = "arena", timeoutTicks = 1700, batch = "siege_devils")
    public static void devilsUnleashed(GameTestHelper h) {
        BlockPos core = setup(h);
        ServerLevel level = h.getLevel();
        java.util.List<HeroEntity> devils = new java.util.ArrayList<>();
        java.util.List<HeroKind> kinds = java.util.Arrays.stream(HeroKind.values()).filter(k -> k.devil).toList();
        for (int i = 0; i < kinds.size(); i++) {
            HeroEntity d = kinds.get(i).type().get().create(level);
            Vec3 p = ring(core, 6, i, kinds.size());
            d.moveTo(p.x, p.y, p.z);
            d.finalizeSpawn(level, level.getCurrentDifficultyAt(core), MobSpawnType.EVENT, null, null);
            d.setMode(HeroMode.HUNT);
            d.setLevelAndXp(5, 0);
            level.addFreshEntity(d);
            d.onSummoned(level);
            devils.add(d);
        }
        if (devils.size() != 5) h.fail("expected five devils, found " + devils.size());
        java.util.function.Consumer<Integer> horde = n -> {
            for (int i = 0; i < n; i++) {
                ZombieKind k = ZombieKind.values()[i % ZombieKind.values().length];
                ZombieSpawner.spawn(level, k, ZombieRank.byId(i % 4), 0, ring(core, 12, i, n), false, true);
            }
        };
        horde.accept(28);
        SiegeBoss.spawn(level, BossKind.GOREHOUND, ring(core, 13, 3, 8), 10, 1);
        int[] casts = {6, 7, 8, 9, 10, 11};
        StringBuilder fails = new StringBuilder();
        for (int j = 0; j < casts.length; j++) {
            int id = casts[j];
            int at = 40 + j * 120;
            h.runAfterDelay(at, () -> {
                for (HeroEntity d : devils) if (d.isAlive()) d.forceSkill(id);
            });
            // fresh enemies and ready skills, so the devils also choose their own moves in between
            h.runAfterDelay(at + 60, () -> {
                horde.accept(10);
                for (HeroEntity d : devils) d.resetCooldowns();
            });
        }
        h.runAfterDelay(700, () -> {
            boolean spawn = !level.getEntitiesOfClass(com.lastbastion.entity.hero.HellspawnEntity.class, new net.minecraft.world.phys.AABB(core).inflate(40)).isEmpty();
            if (!spawn) fails.append("Legions of Hell called up no hellspawn; ");
        });
        int ult = 40 + casts.length * 120;
        h.runAfterDelay(ult, () -> {
            horde.accept(16);
            for (HeroEntity d : devils) if (d.isAlive()) d.forceSkill(HeroEntity.ULT);
        });
        h.runAfterDelay(ult + 175, () -> {
            for (HeroEntity d : devils) {
                if (d instanceof com.lastbastion.entity.hero.MolochDevil m && !m.molten()) fails.append("Moloch's ultimate did not leave him molten; ");
            }
            // clear the field so the devils settle, then everyone must end idle and clean
            for (net.minecraft.world.entity.Entity e : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    new net.minecraft.world.phys.AABB(core).inflate(48), e -> e instanceof com.lastbastion.combat.SiegeEnemy)) {
                e.discard();
            }
            h.succeedWhen(() -> {
                if (fails.length() > 0) throw new net.minecraft.gametest.framework.GameTestAssertException(fails.toString());
                for (HeroEntity d : devils) {
                    String n = d.kind.shortName();
                    if (!d.isAlive()) throw new net.minecraft.gametest.framework.GameTestAssertException(n + " died");
                    if (d.activeSkill() != 0) throw new net.minecraft.gametest.framework.GameTestAssertException(n + " still busy");
                    if (d.isInvisible() || d.isInvulnerable() || d.isNoGravity()) {
                        throw new net.minecraft.gametest.framework.GameTestAssertException(n + " left a skill stuck (invisible/invulnerable/floating)");
                    }
                    if (d instanceof com.lastbastion.entity.hero.LuciferDevil l && l.soaring()) throw new net.minecraft.gametest.framework.GameTestAssertException("Lucifer stuck in flight");
                    if (d instanceof com.lastbastion.entity.hero.LeviathanDevil l && l.submerged()) throw new net.minecraft.gametest.framework.GameTestAssertException("Leviathan stuck underground");
                    if (d instanceof com.lastbastion.entity.hero.BeelzebubDevil b && b.dissolved()) throw new net.minecraft.gametest.framework.GameTestAssertException("Beelzebub stuck as a swarm");
                }
                teardown(h, core);
            });
        });
    }

    /** Pacts: bound anywhere without a core, the blood price, one devil per soul, recovering pacts, return on death. */
    @GameTest(template = "arena", timeoutTicks = 60, batch = "siege_pact")
    public static void devilPact(GameTestHelper h) {
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) h.setBlock(x, 0, z, Blocks.STONE);
        ServerLevel level = h.getLevel();
        SiegeData d = SiegeManager.data(level);
        StringBuilder fails = new StringBuilder();
        if (d.corePos != null) fails.append("a core is still active; ");
        net.minecraft.server.level.ServerPlayer p = net.minecraftforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "pact_tester"));
        BlockPos at = h.absolutePos(new BlockPos(8, 1, 8));
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.setHealth(20f);
        net.minecraft.world.InteractionHand hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        net.minecraft.world.item.ItemStack pact = new net.minecraft.world.item.ItemStack(com.lastbastion.registry.ModItems.sigil(HeroKind.LUCIFER));
        p.setItemInHand(hand, pact);
        if (!pact.getItem().use(level, p, hand).getResult().consumesAction()) fails.append("pact refused without a core; ");
        var mine = com.lastbastion.entity.hero.Companions.ownedAnywhere(level.getServer(), p.getUUID(), true);
        HeroEntity lucifer = mine.size() == 1 ? mine.get(0) : null;
        if (lucifer == null || lucifer.kind != HeroKind.LUCIFER) fails.append("Lucifer not bound (").append(mine.size()).append(" devils); ");
        else if (lucifer.mode() != HeroMode.FOLLOW) fails.append("devil should follow its summoner away from a core; ");
        if (p.getHealth() > 10.5f || !p.isAlive()) fails.append("no blood price (health ").append(p.getHealth()).append("); ");
        // one pact per soul
        net.minecraft.world.item.ItemStack second = new net.minecraft.world.item.ItemStack(com.lastbastion.registry.ModItems.sigil(HeroKind.MOLOCH));
        p.setItemInHand(hand, second);
        if (second.getItem().use(level, p, hand).getResult().consumesAction()) fails.append("a second pact was accepted; ");
        if (com.lastbastion.entity.hero.Companions.ownedAnywhere(level.getServer(), p.getUUID(), true).size() != 1) fails.append("second devil appeared; ");
        // heroes are a separate limit
        net.minecraft.world.item.ItemStack sigil = new net.minecraft.world.item.ItemStack(com.lastbastion.registry.ModItems.sigil(HeroKind.ALDRIC));
        p.setItemInHand(hand, sigil);
        if (!sigil.getItem().use(level, p, hand).getResult().consumesAction()) fails.append("hero sigil refused while bound to a devil; ");
        // a recovering pact cannot be used
        net.minecraft.world.item.ItemStack tired = com.lastbastion.entity.hero.Companions.sigilFor(HeroKind.MOLOCH, 1, 0, level.getGameTime() + 1000, 1000);
        p.setItemInHand(hand, tired);
        if (tired.getItem().use(level, p, hand).getResult().consumesAction()) fails.append("recovering pact was usable; ");
        // a fallen devil's pact goes back to its summoner (mailbox for a player who isn't online), recovering
        if (lucifer != null) {
            lucifer.kill();
            if (lucifer.isAlive()) fails.append("Lucifer survived /kill; ");
            boolean mailed = d.fallenHeroes.stream().anyMatch(f -> f.owner().equals(p.getUUID()) && f.heroKind() == HeroKind.LUCIFER.ordinal());
            if (!mailed) fails.append("fallen pact not returned; ");
        }
        d.fallenHeroes.removeIf(f -> f.owner().equals(p.getUUID()));
        for (HeroEntity hero : com.lastbastion.entity.hero.Companions.ownedAnywhere(level.getServer(), p.getUUID(), false)) hero.discard();
        if (fails.length() == 0) h.succeed();
        else h.fail(fails.toString());
    }
}
