package com.tokyoghoul.client;

import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Characters;
import com.tokyoghoul.entity.TGCharacterEntity;
import com.tokyoghoul.network.Packets;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/** Client copy of the local player's HUD data and every player's visual state (NPCs sync their own). */
public final class ClientState {
    @Nullable
    public static Packets.PlayerData self;
    private static final Map<Integer, String> CHARACTER = new HashMap<>();
    private static final Map<Integer, Integer> FLAGS = new HashMap<>();
    private static final Map<Integer, int[]> ANIM = new HashMap<>();

    private ClientState() {
    }

    public static void clear() {
        self = null;
        CHARACTER.clear();
        FLAGS.clear();
        ANIM.clear();
    }

    static void visual(int id, String character, int flags) {
        CHARACTER.put(id, character);
        FLAGS.put(id, flags);
    }

    static void anim(Entity e, int anim) {
        ANIM.put(e.getId(), new int[]{anim, e.tickCount});
    }

    @Nullable
    public static CharacterDef character(Entity e) {
        if (e instanceof TGCharacterEntity npc) return npc.fighterDef();
        return Characters.get(CHARACTER.get(e.getId()));
    }

    public static int flags(Entity e) {
        if (e instanceof TGCharacterEntity npc) return npc.flags();
        Integer f = FLAGS.get(e.getId());
        return f == null ? 0 : f;
    }

    public static int anim(Entity e) {
        if (e instanceof TGCharacterEntity npc) return npc.animId();
        int[] a = ANIM.get(e.getId());
        return a == null ? 0 : a[0];
    }

    public static float animTime(Entity e, float pt) {
        if (e instanceof TGCharacterEntity npc) return npc.animTime(pt);
        int[] a = ANIM.get(e.getId());
        return a == null ? 1000 : e.tickCount - a[1] + pt;
    }
}
