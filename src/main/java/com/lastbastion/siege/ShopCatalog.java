package com.lastbastion.siege;

import com.lastbastion.entity.hero.HeroKind;
import com.lastbastion.entity.turret.TurretKind;
import com.lastbastion.item.ConsumableItem;
import com.lastbastion.item.GrenadeItem;
import com.lastbastion.item.GunItem;
import com.lastbastion.item.SupportCallItem;
import com.lastbastion.item.weapon.Arsenal;
import com.lastbastion.item.weapon.WeaponKind;
import com.lastbastion.registry.ModBlocks;
import com.lastbastion.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.*;
import java.util.function.Supplier;

/** Everything the Siege Core sells. Shared between the client screen and the server purchase handler. */
public final class ShopCatalog {
    public enum Category {
        TURRETS("Turrets", 0xFFB000), HEROES("Heroes", 0xFFD966), ARSENAL("Arsenal", 0xFF5050), ARMORY("Armory", 0x6FA8FF), SUPPORT("Support", 0x7FE8FF),
        SUPPLIES("Supplies", 0x7CFF9B), FORTIFY("Fortify", 0xC8A070), CORE("Core", 0xC86BFF);
        public final String title;
        public final int color;

        Category(String t, int c) {
            title = t;
            color = c;
        }
    }

    public static final class Entry {
        public final String id;
        public final Category category;
        public final String name;
        public final int baseCost;
        public final Supplier<ItemStack> icon;
        public final List<String> lines;
        public final Supplier<ItemStack> reward;
        public final CoreUpgrade upgrade;

        Entry(String id, Category category, String name, int baseCost, Supplier<ItemStack> icon, Supplier<ItemStack> reward,
              CoreUpgrade upgrade, String... lines) {
            this.id = id;
            this.category = category;
            this.name = name;
            this.baseCost = baseCost;
            this.icon = icon;
            this.reward = reward;
            this.upgrade = upgrade;
            this.lines = List.of(lines);
        }

        /**
         * Current cost (core upgrades scale with level; repairs scale with wave; weapon upgrades depend on
         * the weapon in hand). -1 = sold out / maxed, -2 = not applicable right now.
         */
        public int cost(int[] upgrades, int wave, ItemStack held) {
            if (id.equals(UPGRADE_WEAPON)) {
                if (held == null || !(held.getItem() instanceof Arsenal.Upgradeable)) return -2;
                return Arsenal.upgradeCost(held);
            }
            if (upgrade != null) {
                int lvl = upgrades[upgrade.ordinal()];
                return lvl >= upgrade.maxLevel ? -1 : upgrade.costs[lvl];
            }
            if (id.equals("core_repair_now")) return 150 + 25 * wave;
            return baseCost;
        }
    }

    public static final String UPGRADE_WEAPON = "upgrade_weapon";
    private static List<Entry> entries;
    private static Map<String, Entry> byId;

    private ShopCatalog() {
    }

    public static List<Entry> all() {
        if (entries == null) build();
        return entries;
    }

    public static Entry get(String id) {
        if (byId == null) build();
        return byId.get(id);
    }

    private static Supplier<ItemStack> stack(Supplier<? extends net.minecraft.world.level.ItemLike> item, int count) {
        return () -> new ItemStack(item.get(), count);
    }

    private static synchronized void build() {
        List<Entry> l = new ArrayList<>();
        for (TurretKind k : TurretKind.values()) {
            l.add(new Entry("turret_" + k.id, Category.TURRETS, k.title, k.cost, stack(ModItems.TURRET_KITS.get(k), 1), stack(ModItems.TURRET_KITS.get(k), 1), null,
                    k.desc, "Element: " + k.element.name() + "   Dmg " + k.damage + "   Range " + k.range,
                    "Lv3: " + k.perk3, "Lv5: " + k.perk5, "Upgrade to Lv5 total: " + k.totalInvested(TurretKind.MAX_LEVEL) + " credits"));
        }
        for (HeroKind k : HeroKind.values()) {
            l.add(new Entry("hero_" + k.id, Category.HEROES, k.title, k.cost, stack(ModItems.HERO_SIGILS.get(k), 1), stack(ModItems.HERO_SIGILS.get(k), 1), null,
                    k.desc, "HP " + (int) k.hp + "   Damage " + (int) k.damage + "   Role: " + k.role.name().charAt(0) + k.role.name().substring(1).toLowerCase(), k.moves));
        }
        l.add(new Entry(UPGRADE_WEAPON, Category.ARSENAL, "Upgrade Held Weapon", 0, () -> new ItemStack(net.minecraft.world.item.Items.SMITHING_TABLE), null, null,
                "Raises the arsenal weapon in your main hand by one Mk (up to Mk V).",
                "Each Mk: more damage. Mk III awakens a second passive; Mk V ascends the special ability.",
                "Cost: 40% / 70% / 110% / 160% of the weapon's price for Mk II / III / IV / V."));
        for (WeaponKind k : WeaponKind.values()) {
            l.add(new Entry("melee_" + k.id, Category.ARSENAL, k.title, k.cost, stack(ModItems.MELEE.get(k), 1), stack(ModItems.MELEE.get(k), 1), null,
                    String.format("%.0f damage, %.1f attacks/s, reach %.1f, cleaves %d.", k.damage, k.attacksPerSecond, k.reach, k.cleave),
                    "Special - " + k.special + ": " + k.specialDesc,
                    "Passive - " + k.passive + ": " + k.passiveDesc,
                    "Mk III - " + k.awakened + ": " + k.awakenedDesc));
        }
        for (GunItem.GunKind g : GunItem.GunKind.values()) {
            l.add(new Entry("gun_" + g.name().toLowerCase(Locale.ROOT), Category.ARSENAL, g.title, g.cost,
                    stack(ModItems.GUNS.get(g), 1), stack(ModItems.GUNS.get(g), 1), null, g.desc, "Damage " + g.damage));
        }
        for (com.lastbastion.item.armor.ArmorSet set : com.lastbastion.item.armor.ArmorSet.values()) {
            int[] d = set.defence;
            l.add(new Entry("armor_" + set.id, Category.ARMORY, set.title, set.cost, stack(ModItems.ARMOR.get(set).get(1), 1), null, null,
                    "Full set: " + set.pieceName(0) + ", " + set.pieceName(1) + ", " + set.pieceName(2) + ", " + set.pieceName(3) + ".",
                    "Armor " + (d[0] + d[1] + d[2] + d[3]) + (set.toughness > 0 ? "   Toughness " + (int) (set.toughness * 4) : "")
                            + (set.knockbackResistance > 0 ? "   Knockback resist " + Math.round(set.knockbackResistance * 400) + "%" : "") + "   Unbreakable",
                    "Set bonus - " + set.bonus + ": " + set.bonusDesc,
                    set.lore));
        }
        l.add(new Entry("bastion_shield", Category.ARSENAL, "Bastion Tower Shield", 120, () -> new ItemStack(Items.SHIELD), () -> {
            ItemStack s = new ItemStack(Items.SHIELD);
            s.enchant(Enchantments.UNBREAKING, 3);
            s.setHoverName(Component.literal("Bastion Tower Shield").withStyle(ChatFormatting.GOLD));
            return s;
        }, null, "An unbreaking shield for the front line."));

        int[] supportCosts = {1500, 900, 700, 2500};
        for (SupportCallItem.Kind k : SupportCallItem.Kind.values()) {
            l.add(new Entry("support_" + k.name().toLowerCase(Locale.ROOT), Category.SUPPORT, k.title, supportCosts[k.ordinal()],
                    stack(ModItems.SUPPORT.get(k), 1), stack(ModItems.SUPPORT.get(k), 1), null, k.desc));
        }
        int[] grenadeCosts = {120, 140, 140, 180, 200};
        int[] grenadeCounts = {4, 4, 4, 4, 2};
        for (GrenadeItem.Kind k : GrenadeItem.Kind.values()) {
            l.add(new Entry("grenade_" + k.name().toLowerCase(Locale.ROOT), Category.SUPPORT, k.title + " x" + grenadeCounts[k.ordinal()],
                    grenadeCosts[k.ordinal()], stack(ModItems.GRENADES.get(k), 1), stack(ModItems.GRENADES.get(k), grenadeCounts[k.ordinal()]), null, k.desc));
        }
        int[] consCosts = {40, 120, 120, 300};
        int[] consCounts = {4, 2, 2, 1};
        for (ConsumableItem.Kind k : ConsumableItem.Kind.values()) {
            l.add(new Entry("supply_" + k.name().toLowerCase(Locale.ROOT), Category.SUPPLIES, k.title + (consCounts[k.ordinal()] > 1 ? " x" + consCounts[k.ordinal()] : ""),
                    consCosts[k.ordinal()], stack(ModItems.CONSUMABLES.get(k), 1), stack(ModItems.CONSUMABLES.get(k), consCounts[k.ordinal()]), null, k.desc));
        }
        l.add(new Entry("golden_apples", Category.SUPPLIES, "Golden Apples x3", 180, () -> new ItemStack(Items.GOLDEN_APPLE), () -> new ItemStack(Items.GOLDEN_APPLE, 3), null,
                "Absorption and regeneration in a pinch."));
        l.add(new Entry("totem", Category.SUPPLIES, "Totem of Last Stand", 650, () -> new ItemStack(Items.TOTEM_OF_UNDYING), () -> new ItemStack(Items.TOTEM_OF_UNDYING), null,
                "Cheat death once."));
        l.add(new Entry("arrows", Category.SUPPLIES, "Arrows x64", 30, () -> new ItemStack(Items.ARROW), () -> new ItemStack(Items.ARROW, 64), null, "For the traditionalists."));

        l.add(new Entry("barricade_wood", Category.FORTIFY, "Wooden Barricade x16", 40, stack(ModBlocks.BARRICADE_WOOD, 1), stack(ModBlocks.BARRICADE_WOOD, 16), null,
                "Cheap stakes and planks. Breach time: low."));
        l.add(new Entry("barricade_stone", Category.FORTIFY, "Stone Rampart x16", 120, stack(ModBlocks.BARRICADE_STONE, 1), stack(ModBlocks.BARRICADE_STONE, 16), null,
                "Mortared stone. Degrades to wood when breached."));
        l.add(new Entry("barricade_iron", Category.FORTIFY, "Iron Bulwark x8", 200, stack(ModBlocks.BARRICADE_IRON, 1), stack(ModBlocks.BARRICADE_IRON, 8), null,
                "Riveted plate with spikes that wound attackers. Degrades to stone."));
        l.add(new Entry("barricade_runic", Category.FORTIFY, "Runic Ward x4", 360, stack(ModBlocks.BARRICADE_RUNIC, 1), stack(ModBlocks.BARRICADE_RUNIC, 4), null,
                "Arcane-etched obsidian that zaps anything breaching it. Degrades to iron."));
        l.add(new Entry("spikes", Category.FORTIFY, "Spike Trap x4", 120, stack(ModBlocks.SPIKE_TRAP, 1), stack(ModBlocks.SPIKE_TRAP, 4), null,
                "Slows, wounds and bleeds zombies walking over it."));
        l.add(new Entry("landmines", Category.FORTIFY, "Landmine x4", 160, stack(ModBlocks.LANDMINE, 1), stack(ModBlocks.LANDMINE, 4), null,
                "30 explosive damage under the first zombie to step on it."));
        l.add(new Entry("medic_shrine", Category.FORTIFY, "Medic Shrine", 500, stack(ModBlocks.MEDIC_SHRINE, 1), stack(ModBlocks.MEDIC_SHRINE, 1), null,
                "Heals players and heroes within 7 blocks and cleanses ailments."));
        l.add(new Entry("soul_harvester", Category.FORTIFY, "Soul Harvester", 800, stack(ModBlocks.SOUL_HARVESTER, 1), stack(ModBlocks.SOUL_HARVESTER, 1), null,
                "+25% credits for kills within 16 blocks."));
        l.add(new Entry("repair_pylon", Category.FORTIFY, "Repair Pylon", 450, stack(ModBlocks.REPAIR_PYLON, 1), stack(ModBlocks.REPAIR_PYLON, 1), null,
                "Repairs turrets within 8 blocks."));

        for (CoreUpgrade u : CoreUpgrade.values()) {
            l.add(new Entry("upgrade_" + u.name().toLowerCase(Locale.ROOT), Category.CORE, u.title, u.costs[0], stack(ModItems.SIEGE_CORE, 1), null, u,
                    u.desc, "Max level " + u.maxLevel));
        }
        l.add(new Entry("core_repair_now", Category.CORE, "Emergency Core Repair", 150, () -> new ItemStack(Items.AMETHYST_SHARD), null, null,
                "Immediately restores 25% of the core's health. Price rises each wave."));

        entries = Collections.unmodifiableList(l);
        Map<String, Entry> m = new HashMap<>();
        for (Entry e : l) m.put(e.id, e);
        byId = m;
    }

    /** Server-side purchase. Returns a message for the player. */
    public static Component purchase(ServerPlayer player, String id, int arg) {
        ServerLevel level = player.serverLevel();
        SiegeData d = SiegeManager.data(level);
        if (SiegeManager.corePos(level) == null || !SiegeManager.corePos(level).closerToCenterThan(player.position(), 16)) {
            return Component.literal("You must be at the Siege Core to trade.").withStyle(ChatFormatting.RED);
        }
        if (id.equals("revive")) return revive(player, d, arg);
        Entry e = get(id);
        if (e == null) return Component.literal("Unknown item.").withStyle(ChatFormatting.RED);
        ItemStack held = player.getMainHandItem();
        int cost = e.cost(d.upgrades, d.wave, held);
        if (cost == -2) return Component.literal("Hold an arsenal weapon in your main hand to upgrade it.").withStyle(ChatFormatting.YELLOW);
        if (cost < 0) return Component.literal("Already at maximum level.").withStyle(ChatFormatting.YELLOW);
        if (!SiegeManager.spend(level, player, cost)) {
            return Component.literal("Not enough credits (" + cost + " needed).").withStyle(ChatFormatting.RED);
        }
        if (e.upgrade != null) {
            d.upgrades[e.upgrade.ordinal()]++;
            if (e.upgrade == CoreUpgrade.FORTIFY) d.coreHp += SiegeManagerHooks.fortifyBonus(d);
            if (e.upgrade == CoreUpgrade.BARRIER) d.shield = d.maxShield();
            d.setDirty();
            SiegeManager.syncHud(level, d);
            com.lastbastion.combat.Fx.skyBeam(level, net.minecraft.world.phys.Vec3.atCenterOf(SiegeManager.corePos(level)), 0xC86BFF, 30, 1.2);
            return Component.literal(e.upgrade.title + " upgraded to level " + d.upgrades[e.upgrade.ordinal()] + "!").withStyle(ChatFormatting.LIGHT_PURPLE);
        }
        if (id.equals(UPGRADE_WEAPON)) {
            int mk = Arsenal.mk(held) + 1;
            Arsenal.setMk(held, mk);
            String title = held.getItem() instanceof Arsenal.Upgradeable u ? u.displayTitle() : "Weapon";
            level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.2f);
            level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.4f);
            com.lastbastion.fx.Vfx.spray(level, com.lastbastion.fx.FxStyle.STAR, player.position().add(0, 1.2, 0), 0xFFFFFF, 0xFFD966, 0.25f, 20, 0.25f, 0.8f);
            com.lastbastion.fx.Vfx.ring(level, player.position().add(0, 0.1, 0), 0xFFD966, 2.5f, 14, 0.8f);
            String extra = mk == 3 ? " Awakened!" : mk == Arsenal.MAX_MK ? " Ascended!" : "";
            return Component.literal(title + " upgraded to Mk " + Arsenal.roman(mk) + "." + extra).withStyle(Arsenal.mkColor(mk));
        }
        if (id.equals("core_repair_now")) {
            SiegeManager.healCore(level, d.maxHp() * 0.25f);
            SiegeManager.syncHud(level, d);
            return Component.literal("Core repaired.").withStyle(ChatFormatting.GREEN);
        }
        if (e.category == Category.ARMORY) {
            for (var piece : ModItems.ARMOR.get(com.lastbastion.item.armor.ArmorSet.valueOf(id.substring(6).toUpperCase(Locale.ROOT)))) {
                give(player, new ItemStack(piece.get()));
            }
        } else {
            give(player, e.reward.get());
        }
        player.playNotifySound(net.minecraft.sounds.SoundEvents.VILLAGER_YES, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.2f);
        return Component.literal("Purchased " + e.name + " for " + cost + " credits.").withStyle(ChatFormatting.GREEN);
    }

    public static int reviveCost(SiegeData.FallenHero f) {
        return HeroKind.values()[f.heroKind()].cost / 2;
    }

    private static Component revive(ServerPlayer player, SiegeData d, int index) {
        List<SiegeData.FallenHero> mine = new ArrayList<>();
        for (SiegeData.FallenHero f : d.fallenHeroes) if (f.owner().equals(player.getUUID())) mine.add(f);
        if (index < 0 || index >= mine.size()) return Component.literal("No such fallen hero.").withStyle(ChatFormatting.RED);
        SiegeData.FallenHero f = mine.get(index);
        int cost = reviveCost(f);
        if (!SiegeManager.spend(player.serverLevel(), player, cost)) return Component.literal("Not enough credits (" + cost + ").").withStyle(ChatFormatting.RED);
        d.fallenHeroes.remove(f);
        d.setDirty();
        HeroKind k = HeroKind.values()[f.heroKind()];
        ItemStack sigil = new ItemStack(ModItems.HERO_SIGILS.get(k).get());
        sigil.getOrCreateTag().putInt("lbLevel", f.level());
        sigil.getOrCreateTag().putInt("lbXp", f.xp());
        give(player, sigil);
        return Component.literal(k.title + " is ready to return to battle!").withStyle(ChatFormatting.GOLD);
    }

    private static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    /** Small helpers kept separate so the catalog stays declarative. */
    static final class SiegeManagerHooks {
        static float fortifyBonus(SiegeData d) {
            return d.maxHp() * 0.3f / (1f + 0.3f * d.level(CoreUpgrade.FORTIFY)) ;
        }
    }
}
