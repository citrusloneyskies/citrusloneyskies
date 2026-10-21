package com.tokyoghoul.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Characters;
import com.tokyoghoul.player.PlayerActions;
import com.tokyoghoul.player.PlayerCaps;
import com.tokyoghoul.player.PlayerEvents;
import com.tokyoghoul.player.Progression;
import com.tokyoghoul.player.TGPlayerData;
import com.tokyoghoul.network.Sync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/** {@code /tg}: set characters, ranks, fame and kakuja for testing or server events. */
public final class TGCommand {
    private TGCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("tg").requires(s -> s.hasPermission(2))
                .then(Commands.literal("character").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Characters.selectable().stream().map(x -> x.id), b))
                        .then(Commands.argument("targets", EntityArgument.players()).executes(c -> character(c.getSource(),
                                StringArgumentType.getString(c, "id"), EntityArgument.getPlayers(c, "targets"))))
                        .executes(c -> character(c.getSource(), StringArgumentType.getString(c, "id"),
                                java.util.List.of(c.getSource().getPlayerOrException())))))
                .then(Commands.literal("level").then(Commands.argument("level", IntegerArgumentType.integer(1, 7))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            TGPlayerData data = PlayerCaps.get(p);
                            data.level = IntegerArgumentType.getInteger(c, "level");
                            data.rc = data.maxRc();
                            PlayerEvents.applyAttributes(p, data);
                            Sync.playerData(p);
                            c.getSource().sendSuccess(() -> Component.literal("Rank set to " + data.rankName()), true);
                            return 1;
                        })))
                .then(Commands.literal("fame").then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            TGPlayerData data = PlayerCaps.get(p);
                            data.fame += IntegerArgumentType.getInteger(c, "amount");
                            data.strongestDefeated = Math.max(data.strongestDefeated, 10);
                            Progression.checkRank(p, data);
                            Sync.playerData(p);
                            return 1;
                        })))
                .then(Commands.literal("kakuja").then(Commands.argument("progress", IntegerArgumentType.integer(0, 100))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            PlayerCaps.get(p).kakuja = IntegerArgumentType.getInteger(c, "progress");
                            Sync.playerData(p);
                            return 1;
                        })))
                .then(Commands.literal("rc").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    TGPlayerData data = PlayerCaps.get(p);
                    data.rc = data.maxRc();
                    Sync.playerData(p);
                    return 1;
                }))
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    TGPlayerData data = PlayerCaps.get(p);
                    data.character = "";
                    data.level = 1;
                    data.fame = 0;
                    data.kakuja = 0;
                    data.strongestDefeated = 0;
                    PlayerEvents.applyAttributes(p, data);
                    Sync.playerData(p);
                    Sync.visual(p);
                    return 1;
                })));
    }

    private static int character(CommandSourceStack src, String id, Collection<ServerPlayer> players) {
        CharacterDef def = Characters.get(id);
        if (def == null) {
            src.sendFailure(Component.literal("Unknown character " + id));
            return 0;
        }
        for (ServerPlayer p : players) {
            if (!p.getInventory().contains(new net.minecraft.world.item.ItemStack(com.tokyoghoul.registry.TGItems.GHOUL_REGISTRY.get()))) {
                p.getInventory().add(new net.minecraft.world.item.ItemStack(com.tokyoghoul.registry.TGItems.GHOUL_REGISTRY.get()));
            }
            PlayerActions.select(p, id);
        }
        return players.size();
    }
}
