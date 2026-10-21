package com.tokyoghoul.test;

import com.tokyoghoul.TokyoGhoul;
import com.tokyoghoul.combat.Combat;
import com.tokyoghoul.combat.FighterState;
import com.tokyoghoul.combat.Fighters;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Characters;
import com.tokyoghoul.data.Faction;
import com.tokyoghoul.data.Rank;
import com.tokyoghoul.entity.TGCharacterEntity;
import com.tokyoghoul.move.MoveDef;
import com.tokyoghoul.move.MoveRunner;
import com.tokyoghoul.player.PlayerActions;
import com.tokyoghoul.player.PlayerCaps;
import com.tokyoghoul.player.TGPlayerData;
import com.tokyoghoul.registry.TGEntities;
import com.tokyoghoul.registry.TGItems;
import com.tokyoghoul.util.Safe;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Server-side smoke tests (runGameTestServer in CI): every character spawns and lands every move in
 * its kit on an enemy without errors, NPCs fight on their own, players can pick characters and use
 * their techniques, and faction / rank rules hold.
 */
@GameTestHolder(TokyoGhoul.MODID)
@PrefixGameTestTemplate(false)
public final class TGGameTests {
    private TGGameTests() {
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) h.setBlock(x, 0, z, Blocks.STONE);
    }

    /** A sparring partner the character's faction will fight. */
    private static CharacterDef enemyOf(CharacterDef d) {
        if (d.faction == Faction.CCG) return Characters.RINKAKU_GHOUL;
        if (d.faction == Faction.HUMAN) return Characters.RINKAKU_GHOUL;
        return Characters.INVESTIGATOR;
    }

    /** One test per character: run every move in its rotation (plus AI melee) against a dummy. */
    @GameTestGenerator
    public static Collection<TestFunction> movesets() {
        List<TestFunction> out = new ArrayList<>();
        for (CharacterDef d : Characters.all()) {
            out.add(new TestFunction("moveset_" + d.id, "moveset_" + d.id, TokyoGhoul.MODID + ":arena", 1400, 5, true, h -> moveset(h, d)));
        }
        return out;
    }

    private static void moveset(GameTestHelper h, CharacterDef d) {
        floor(h);
        int errors = Safe.ERRORS.get();
        TGCharacterEntity fighter = h.spawn(TGEntities.typeOf(d.id), new BlockPos(14, 1, 16));
        TGCharacterEntity dummy = h.spawn(TGEntities.typeOf(enemyOf(d).id), new BlockPos(18, 1, 16));
        dummy.setNoAi(true);
        var hp = dummy.getAttribute(Attributes.MAX_HEALTH);
        hp.setBaseValue(1000);
        dummy.setHealth(1000);
        fighter.setFighterLevel(10);
        fighter.setTarget(dummy);
        List<MoveDef> moves = new ArrayList<>(d.rotation());
        for (MoveDef m : com.tokyoghoul.data.Moves.AI_MELEE) if (!moves.contains(m)) moves.add(m);
        int[] index = {0};
        int[] waited = {0};
        h.succeedWhen(() -> {
            if (Safe.ERRORS.get() != errors) h.fail("errors were logged while running " + d.id + "'s moves");
            if (!dummy.isAlive()) {
                // respawn the dummy if a big move finished it
                h.fail("dummy died early (moves too strong?)");
            }
            FighterState s = fighter.fighterState();
            if (index[0] >= moves.size()) {
                if (dummy.getHealth() >= 1000) h.fail(d.id + " never damaged the dummy");
                return;
            }
            if (s.busy() || s.stun > 0) {
                if (++waited[0] > 300) h.fail(d.id + " stuck in " + (s.move == null ? "stun" : s.move.id));
                throw new GameTestAssertException("running");
            }
            waited[0] = 0;
            // keep the dummy in front and reset cooldowns so every move can fire
            var spot = h.absoluteVec(new net.minecraft.world.phys.Vec3(18.5, 1, 16.5));
            dummy.teleportTo(spot.x, spot.y, spot.z);
            if (dummy.getHealth() < 300) dummy.setHealth(1000);
            s.cooldowns.clear();
            s.globalReady = 0;
            s.formTicks = 0;
            s.form = null;
            MoveDef m = moves.get(index[0]);
            MoveRunner.Fail f = MoveRunner.start(fighter, m, dummy);
            // the NPC's own brain may have started something this tick; just try again
            if (f == MoveRunner.Fail.BUSY) throw new GameTestAssertException("busy");
            if (f != MoveRunner.Fail.NONE) h.fail(d.id + " could not start " + m.id + ": " + f);
            index[0]++;
            throw new GameTestAssertException("next move");
        });
    }

    /** Two hostile NPCs left alone fight each other until one takes damage. */
    @GameTest(template = "arena", timeoutTicks = 600, batch = "ai")
    public static void npcsFight(GameTestHelper h) {
        floor(h);
        int errors = Safe.ERRORS.get();
        TGCharacterEntity ghoul = h.spawn(TGEntities.typeOf("ayato"), new BlockPos(10, 1, 16));
        TGCharacterEntity ccg = h.spawn(TGEntities.typeOf("amon"), new BlockPos(22, 1, 16));
        ghoul.setFighterLevel(7);
        ccg.setFighterLevel(7);
        h.succeedWhen(() -> {
            if (Safe.ERRORS.get() != errors) h.fail("errors were logged during the fight");
            if (ghoul.getHealth() >= ghoul.getMaxHealth() && ccg.getHealth() >= ccg.getMaxHealth()) {
                throw new GameTestAssertException("no damage yet");
            }
        });
    }

    /** Factions: CCG hunts ghouls, Anteiku leaves humans alone, allies cannot hurt each other. */
    @GameTest(template = "arena", timeoutTicks = 40, batch = "rules")
    public static void factionRules(GameTestHelper h) {
        floor(h);
        TGCharacterEntity arima = h.spawn(TGEntities.typeOf("arima"), new BlockPos(8, 1, 8));
        TGCharacterEntity amon = h.spawn(TGEntities.typeOf("amon"), new BlockPos(10, 1, 8));
        TGCharacterEntity touka = h.spawn(TGEntities.typeOf("touka"), new BlockPos(12, 1, 8));
        TGCharacterEntity civ = h.spawn(TGEntities.typeOf("civilian"), new BlockPos(14, 1, 8));
        TGCharacterEntity ayato = h.spawn(TGEntities.typeOf("ayato"), new BlockPos(16, 1, 8));
        for (TGCharacterEntity e : List.of(arima, amon, touka, civ, ayato)) e.setNoAi(true);
        if (!Combat.canHurt(arima, touka)) h.fail("CCG must hurt Anteiku ghouls");
        if (Combat.canHurt(arima, amon)) h.fail("CCG allies must not hurt each other");
        if (Combat.canHurt(touka, civ)) h.fail("Anteiku must not hurt civilians unprovoked");
        if (!Combat.canHurt(ayato, civ)) h.fail("Aogiri hunts humans");
        if (!Combat.canHurt(ayato, touka)) h.fail("Aogiri and Anteiku fight");
        if (!arima.hunts(touka) || arima.hunts(civ)) h.fail("CCG target selection is wrong");
        if (Rank.levelFor(0, 0, 1) != 1 || Rank.levelFor(5000, 10, 1) != Rank.COUNT || Rank.levelFor(5000, 0, 1) != 2) {
            h.fail("rank thresholds are wrong");
        }
        h.succeed();
    }

    /** A player picks a character, gets their gear and can use techniques and the R rotation. */
    @GameTest(template = "arena", timeoutTicks = 200, batch = "player")
    public static void playerPicksCharacter(GameTestHelper h) {
        try {
            playerTest(h);
        } catch (RuntimeException e) {
            if (!(e instanceof GameTestAssertException)) TokyoGhoul.LOGGER.error("player test crashed", e);
            throw e;
        }
    }

    private static void playerTest(GameTestHelper h) {
        floor(h);
        int errors = Safe.ERRORS.get();
        ServerPlayer p = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.fromString("7c0e7a3e-1d3b-4c3a-9b8e-2f6d5a4c3b21"), "tg_test"));
        p.getInventory().clearContent();
        PlayerCaps.get(p).character = "";
        p.getInventory().add(new ItemStack(TGItems.GHOUL_REGISTRY.get()));
        PlayerActions.select(p, "arima");
        TGPlayerData d = PlayerCaps.get(p);
        if (!"arima".equals(d.character)) h.fail("selection failed");
        if (!p.getInventory().contains(new ItemStack(TGItems.QUINQUES.get(com.tokyoghoul.item.QuinqueKind.IXA).get()))) h.fail("IXA not given");
        if (Fighters.faction(p) != Faction.CCG) h.fail("player faction should follow the character");
        PlayerActions.cycle(p, d, d.def(), 1);
        if (d.selected != 1) h.fail("R should move to the next unlocked technique");
        d.level = Rank.COUNT;
        d.rc = d.maxRc();
        TGCharacterEntity ghoul = h.spawn(TGEntities.typeOf("bikaku_ghoul"), new BlockPos(18, 1, 16));
        ghoul.setNoAi(true);
        var spot = h.absoluteVec(new net.minecraft.world.phys.Vec3(16.5, 1, 16.5));
        p.teleportTo(spot.x, spot.y, spot.z);
        MoveDef lance = d.def().moves().get(0);
        if (MoveRunner.start(p, lance, ghoul) != MoveRunner.Fail.NONE) h.fail("player could not use " + lance.id);
        // switch to a ghoul: diet, kakuja gate and Kaneki's ultimate
        PlayerActions.select(p, "kaneki");
        d.level = Rank.COUNT;
        d.rc = d.maxRc();
        d.state.cooldowns.clear();
        d.state.clearMove();
        d.state.globalReady = 0;
        if (MoveRunner.canStart(p, d.def().ultimate()) != MoveRunner.Fail.KAKUJA) h.fail("kakuja should gate the ultimate");
        d.kakuja = 100;
        if (MoveRunner.canStart(p, d.def().ultimate()) != MoveRunner.Fail.NONE) h.fail("ultimate should be ready");
        h.runAfterDelay(60, () -> {
            if (Safe.ERRORS.get() != errors) h.fail("errors were logged");
            h.succeed();
        });
    }
}
