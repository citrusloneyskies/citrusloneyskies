package com.lastbastion.siege;

import com.lastbastion.LastBastion;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The siege's advancements. Each is a JSON advancement with a single impossible criterion (see
 * {@code data/lastbastion/advancements}), granted from code at the moment it is earned.
 */
public final class Milestones {
    public static final String ROOT = "root", FIRST_NIGHT = "first_night", WAVE_10 = "wave_10", WAVE_25 = "wave_25", WAVE_50 = "wave_50",
            PERFECT = "perfect_defence", VICTORY = "victory", BOSS = "kingslayer", WARLORD = "warlords_bane", RIFT = "seal_the_rift",
            SUPPLY = "special_delivery", HERO = "answer_the_call", DEVIL = "pact_in_blood", LEGION = "legion", REVIVE = "not_today",
            SPECIALIST = "specialist", MASTERWORK = "masterwork";

    private Milestones() {
    }

    public static void award(@Nullable ServerPlayer p, String id) {
        if (p == null || p instanceof FakePlayer) return;
        // any milestone means you're part of a siege: make sure the tree's root is unlocked too (co-defenders never place a core)
        if (!ROOT.equals(id)) award(p, ROOT);
        Advancement adv = p.server.getAdvancements().getAdvancement(new ResourceLocation(LastBastion.MODID, id));
        if (adv == null) return;
        AdvancementProgress progress = p.getAdvancements().getOrStartProgress(adv);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) p.getAdvancements().award(adv, criterion);
    }

    /** Everyone taking part in the siege (in the core's dimension and within the presence radius). */
    public static void awardSiege(ServerLevel level, String id) {
        if (SiegeManager.corePos(level) == null) return;
        for (ServerPlayer p : level.players()) if (SiegeManager.withinSiege(level, p)) award(p, id);
    }

    /** The wave milestones, for the defenders who saw the wave through. */
    public static void waveCleared(List<ServerPlayer> defenders, int cleared, boolean perfect, boolean victory) {
        for (ServerPlayer p : defenders) {
            award(p, FIRST_NIGHT);
            if (cleared >= 10) award(p, WAVE_10);
            if (cleared >= 25) award(p, WAVE_25);
            if (cleared >= 50) award(p, WAVE_50);
            if (perfect) award(p, PERFECT);
            if (victory) award(p, VICTORY);
        }
    }
}
