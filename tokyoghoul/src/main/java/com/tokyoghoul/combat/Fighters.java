package com.tokyoghoul.combat;

import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Faction;
import com.tokyoghoul.data.Race;
import com.tokyoghoul.player.PlayerCaps;
import com.tokyoghoul.player.TGPlayerData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * One view over players and NPCs: character, faction, level, Rc and combat state. Combat, moves and
 * AI only talk to fighters through here, so a player and an NPC playing the same character behave
 * the same.
 */
public final class Fighters {
    private Fighters() {
    }

    @Nullable
    public static FighterState state(LivingEntity e) {
        if (e instanceof Fighter f) return f.fighterState();
        TGPlayerData d = PlayerCaps.getOrNull(e);
        return d == null ? null : d.state;
    }

    @Nullable
    public static CharacterDef def(LivingEntity e) {
        if (e instanceof Fighter f) return f.fighterDef();
        TGPlayerData d = PlayerCaps.getOrNull(e);
        return d == null ? null : d.def();
    }

    /** Faction, or null for entities outside the faction system (vanilla monsters, animals). */
    @Nullable
    public static Faction faction(LivingEntity e) {
        if (e instanceof Fighter f) return f.fighterFaction();
        if (e instanceof Player) {
            CharacterDef d = def(e);
            return d == null ? Faction.HUMAN : d.faction;
        }
        if (e instanceof AbstractVillager) return Faction.HUMAN;
        return null;
    }

    public static int level(LivingEntity e) {
        if (e instanceof Fighter f) return f.fighterLevel();
        TGPlayerData d = PlayerCaps.getOrNull(e);
        return d == null ? 1 : d.level;
    }

    public static boolean ghoulBody(LivingEntity e) {
        CharacterDef d = def(e);
        return d != null && d.race.ghoulBody;
    }

    public static Race race(LivingEntity e) {
        CharacterDef d = def(e);
        return d == null ? Race.HUMAN : d.race;
    }

    /** Outgoing damage multiplier: rank/level, transformation and buffs. */
    public static float damageMult(LivingEntity e) {
        float m;
        // players grow 22% per rank; NPCs start softer so rank-1 players survive a level-3 investigator
        if (e instanceof Player) m = 1f + 0.22f * (level(e) - 1);
        else m = 0.6f + 0.1f * (level(e) - 1);
        FighterState s = state(e);
        if (s != null) {
            if (s.transformed()) m *= s.form.formDamage;
            if (s.buff != null && s.buffTicks > 0) m *= 1.35f;
        }
        return m;
    }

    /** Incoming damage multiplier from transformations and buffs. */
    public static float armorMult(LivingEntity e) {
        FighterState s = state(e);
        if (s == null) return 1f;
        float m = 1f;
        if (s.transformed()) m *= s.form.formArmor;
        if (s.buff != null && s.buffTicks > 0) m *= 0.8f;
        return m;
    }

    public static int kaguneColor(LivingEntity e) {
        CharacterDef d = def(e);
        return d == null ? 0xB01020 : d.kaguneColor;
    }

    /** Spend Rc (players only; NPCs pace themselves with cooldowns, like JJK's mobs). */
    public static boolean spendRc(LivingEntity e, double cost) {
        if (!(e instanceof Player p)) return true;
        if (p.getAbilities().instabuild) return true;
        TGPlayerData d = PlayerCaps.get(p);
        if (d.rc < cost) return false;
        d.rc -= cost;
        return true;
    }

    public static void addRc(LivingEntity e, double amount) {
        TGPlayerData d = PlayerCaps.getOrNull(e);
        if (d != null) d.rc = Math.max(0, Math.min(d.maxRc(), d.rc + amount));
    }
}
