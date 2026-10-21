package com.tokyoghoul.network;

import com.tokyoghoul.player.PlayerActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** All packets. */
public final class Packets {
    private Packets() {
    }

    /** Key actions, the counterpart of JJK's Key*Message classes (R, Z press/release, X, C, G, B, M). */
    public enum Action {CYCLE_NEXT, CYCLE_PREV, USE_PRESS, USE_RELEASE, RESET, TOGGLE_KAGUNE, GUARD_PRESS, GUARD_RELEASE, DODGE, REGEN_PRESS, REGEN_RELEASE}

    public record Key(Action action) {
        public static void encode(Key m, FriendlyByteBuf b) {
            b.writeEnum(m.action);
        }

        public static Key decode(FriendlyByteBuf b) {
            return new Key(b.readEnum(Action.class));
        }

        public static void handle(Key m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p != null) PlayerActions.key(p, m.action);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Character chosen in the Ghoul Registry book. */
    public record Select(String id) {
        public static void encode(Select m, FriendlyByteBuf b) {
            b.writeUtf(m.id, 64);
        }

        public static Select decode(FriendlyByteBuf b) {
            return new Select(b.readUtf(64));
        }

        public static void handle(Select m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p != null) PlayerActions.select(p, m.id);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** The owning player's HUD data (JJK's PlayerVariablesSyncMessage). */
    public record PlayerData(String character, float rc, float maxRc, int level, float fame, int kakuja, int selected, boolean satiated,
                             int[] cooldowns, int strongest) {
        public static void encode(PlayerData m, FriendlyByteBuf b) {
            b.writeUtf(m.character, 64);
            b.writeFloat(m.rc);
            b.writeFloat(m.maxRc);
            b.writeVarInt(m.level);
            b.writeFloat(m.fame);
            b.writeVarInt(m.kakuja);
            b.writeVarInt(m.selected);
            b.writeBoolean(m.satiated);
            b.writeVarIntArray(m.cooldowns);
            b.writeVarInt(m.strongest);
        }

        public static PlayerData decode(FriendlyByteBuf b) {
            return new PlayerData(b.readUtf(64), b.readFloat(), b.readFloat(), b.readVarInt(), b.readFloat(), b.readVarInt(), b.readVarInt(),
                    b.readBoolean(), b.readVarIntArray(), b.readVarInt());
        }

        public static void handle(PlayerData m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.tokyoghoul.client.ClientPackets.playerData(m)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** What other clients need to draw a player: character and visual flags (kagune out, kakuja, guard, mask). */
    public record Visual(int entity, String character, int flags) {
        public static void encode(Visual m, FriendlyByteBuf b) {
            b.writeVarInt(m.entity);
            b.writeUtf(m.character, 64);
            b.writeVarInt(m.flags);
        }

        public static Visual decode(FriendlyByteBuf b) {
            return new Visual(b.readVarInt(), b.readUtf(64), b.readVarInt());
        }

        public static void handle(Visual m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.tokyoghoul.client.ClientPackets.visual(m)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** One-shot animation for a player (NPCs use synced entity data). */
    public record Anim(int entity, int anim) {
        public static void encode(Anim m, FriendlyByteBuf b) {
            b.writeVarInt(m.entity);
            b.writeVarInt(m.anim);
        }

        public static Anim decode(FriendlyByteBuf b) {
            return new Anim(b.readVarInt(), b.readVarInt());
        }

        public static void handle(Anim m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.tokyoghoul.client.ClientPackets.anim(m)));
            ctx.get().setPacketHandled(true);
        }
    }
}
