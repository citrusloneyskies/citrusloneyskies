package com.tokyoghoul.player;

import com.tokyoghoul.combat.Combat;
import com.tokyoghoul.combat.Fighter;
import com.tokyoghoul.combat.Fighters;
import com.tokyoghoul.combat.Fx;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Rank;
import com.tokyoghoul.network.Sync;
import com.tokyoghoul.world.TGGameRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Fame and rank, rebuilt from Jujutsu Craft's WhenEntityDieProcedure: defeating an enemy of level L is
 * worth (L+1)^2+1 fame to whoever landed the kill (the "MVP") and a tenth of that to everyone nearby who
 * was in the fight. Fame plus the strongest enemy defeated decide the rank, and the rank is the
 * player's level.
 */
public final class Progression {
    private Progression() {
    }

    /** Level of an enemy for fame purposes (JJK reads the Strength effect amplifier of mobs). */
    public static int enemyLevel(LivingEntity e) {
        if (e instanceof Fighter || e instanceof Player) return Fighters.level(e);
        if (e instanceof WitherBoss || e instanceof EnderDragon) return 7;
        if (e instanceof Enemy) return Math.max(0, (int) (e.getMaxHealth() / 40));
        return -1;
    }

    public static void onKill(LivingEntity victim, LivingEntity killer) {
        int lv = enemyLevel(victim);
        if (lv < 0) return;
        double base = (lv + 1) * (lv + 1) + 1;
        for (Player p : victim.level().players()) {
            if (!(p instanceof ServerPlayer sp) || p.isSpectator() || p.distanceToSqr(victim) > 32 * 32) continue;
            boolean mvp = p == killer;
            if (!mvp && !Combat.engaged(p, victim) && !Combat.engaged(victim, p)) continue;
            TGPlayerData d = PlayerCaps.get(p);
            double fame = mvp ? base : Math.max(1, Math.round(base / 10));
            d.fame += fame;
            if (mvp) {
                d.kills++;
                d.strongestDefeated = Math.max(d.strongestDefeated, lv);
                if (Fighters.race(p).hasKagune && Fighters.ghoulBody(victim)) d.kakuja = Math.min(100, d.kakuja + 2);
            }
            Component name = victim.getDisplayName();
            p.displayClientMessage(Component.literal(mvp ? "[MVP] " : "").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable("msg.tokyoghoul.fame", name, (int) fame).withStyle(ChatFormatting.YELLOW)), false);
            checkRank(sp, d);
            Sync.playerData(sp);
        }
    }

    /** Recompute the rank; announce promotions with a title like JJK's grade advancements. */
    public static void checkRank(ServerPlayer p, TGPlayerData d) {
        int diff = p.level().getGameRules().getInt(TGGameRules.FAME_DIFFICULTY);
        int lv = Rank.levelFor(d.fame, d.strongestDefeated, diff);
        if (lv > d.level) {
            d.level = lv;
            CharacterDef def = d.def();
            Component rank = Component.literal(d.rankName()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            p.connection.send(new ClientboundSetTitleTextPacket(rank));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("msg.tokyoghoul.promoted").withStyle(ChatFormatting.GRAY)));
            p.sendSystemMessage(Component.translatable("msg.tokyoghoul.rank_up", rank));
            if (def != null) {
                for (var m : def.rotation()) {
                    if (m.minRank == lv) {
                        p.sendSystemMessage(Component.translatable("msg.tokyoghoul.unlocked", m.title().withStyle(ChatFormatting.AQUA)));
                    }
                }
            }
            Fx.sound(p.level(), p.position(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            d.rc = d.maxRc();
            PlayerEvents.applyAttributes(p, d);
        }
    }
}
