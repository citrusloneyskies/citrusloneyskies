package com.tokyoghoul.network;

import com.tokyoghoul.combat.FighterState;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.entity.TGCharacterEntity;
import com.tokyoghoul.move.MoveDef;
import com.tokyoghoul.player.PlayerCaps;
import com.tokyoghoul.player.TGPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

/** Server-side helpers that mirror fighter state to clients. */
public final class Sync {
    public static final int FLAG_KAGUNE = TGCharacterEntity.FLAG_KAGUNE, FLAG_FORM = TGCharacterEntity.FLAG_FORM,
            FLAG_GUARD = TGCharacterEntity.FLAG_GUARD, FLAG_MASK = TGCharacterEntity.FLAG_MASK;

    private Sync() {
    }

    /** Send, tolerating players without a live connection (fake and GameTest players). */
    public static void send(PacketDistributor.PacketTarget target, Object msg) {
        try {
            TGNet.CHANNEL.send(target, msg);
        } catch (NullPointerException | IllegalStateException ignored) {
            // no network channel: nothing to sync to
        }
    }

    public static void anim(LivingEntity e, int anim) {
        if (e.level().isClientSide) return;
        if (e instanceof TGCharacterEntity npc) {
            npc.playAnim(anim);
        } else if (e instanceof ServerPlayer p) {
            PlayerCaps.get(p).state.anim = anim;
            send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), new Packets.Anim(p.getId(), anim));
        }
    }

    public static int playerFlags(Player p) {
        TGPlayerData d = PlayerCaps.get(p);
        FighterState s = d.state;
        int f = 0;
        if (s.kaguneTicks > 0 || s.kaguneToggled) f |= FLAG_KAGUNE;
        if (s.transformed()) f |= FLAG_FORM;
        if (s.guardingNow()) f |= FLAG_GUARD;
        if (s.kaguneTicks > 0 || s.kaguneToggled || s.transformed()) f |= FLAG_MASK;
        return f;
    }

    public static void visual(LivingEntity e) {
        if (e.level().isClientSide) return;
        if (e instanceof TGCharacterEntity npc) {
            npc.updateFlags();
        } else if (e instanceof ServerPlayer p) {
            send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), visualPacket(p));
        }
    }

    public static Packets.Visual visualPacket(Player p) {
        TGPlayerData d = PlayerCaps.get(p);
        return new Packets.Visual(p.getId(), d.character, playerFlags(p));
    }

    public static void playerData(Player player) {
        if (!(player instanceof ServerPlayer p)) return;
        TGPlayerData d = PlayerCaps.get(p);
        CharacterDef def = d.def();
        long now = p.level().getGameTime();
        int[] cds;
        if (def != null) {
            List<MoveDef> rot = def.rotation();
            cds = new int[rot.size()];
            for (int i = 0; i < rot.size(); i++) cds[i] = d.state.cooldownLeft(rot.get(i), now);
        } else {
            cds = new int[0];
        }
        send(PacketDistributor.PLAYER.with(() -> p), new Packets.PlayerData(d.character, (float) d.rc, (float) d.maxRc(), d.level,
                (float) d.fame, d.kakuja, d.selected, d.satiated, cds, d.strongestDefeated));
    }
}
