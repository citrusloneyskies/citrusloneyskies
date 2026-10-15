package com.lastbastion.siege;

import com.lastbastion.entity.boss.BossKind;
import com.lastbastion.entity.boss.SiegeBoss;
import com.lastbastion.entity.zombie.Affix;
import com.lastbastion.entity.zombie.ZombieKind;
import com.lastbastion.entity.zombie.ZombieRank;
import com.lastbastion.entity.zombie.ZombieSpawner;
import com.lastbastion.siege.raid.RaidEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Locale;

/**
 * /siege status               - show siege info
 * /siege start                - start the next wave now (op)
 * /siege setwave &lt;n&gt;         - jump to a wave (op)
 * /siege credits &lt;player&gt; &lt;n&gt; - give credits (op)
 * /siege spawn &lt;type&gt; [rank] [champion] - spawn a zombie at you (op)
 * /siege boss &lt;boss&gt;          - spawn a boss at you (op)
 * /siege defeat               - destroy the core (op)
 * /siege rules                - open the siege rules screen
 * /siege rules list           - print every rule
 * /siege rules set &lt;rule&gt; &lt;value&gt; / preset &lt;name&gt; / reset (op)
 * /siege event &lt;supply_drop|warlord|rift&gt; - trigger a war event now (op)
 */
public final class SiegeCommands {
    private SiegeCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("siege")
                .then(Commands.literal("status").executes(c -> {
                    ServerLevel l = c.getSource().getLevel();
                    SiegeData data = SiegeManager.data(l);
                    String msg = data.hasCore()
                            ? String.format("Core at %s | %s | Wave %d | Core %.0f/%.0f | Shield %.0f | Enemies %d | Best %d | %s",
                            data.corePos.toShortString(), data.state, data.wave, data.coreHp, data.maxHp(), data.shield, data.tracked.size(),
                            data.bestWave, data.modifier.title)
                            : "No Siege Core placed. Craft one and place it to begin. Best wave: " + data.bestWave;
                    c.getSource().sendSuccess(() -> Component.literal(msg).withStyle(ChatFormatting.GOLD), false);
                    return 1;
                }))
                .then(Commands.literal("start").requires(s -> s.hasPermission(2)).executes(c -> {
                    ServerLevel l = c.getSource().getLevel();
                    SiegeData data = SiegeManager.data(l);
                    if (data.state != SiegeState.PREPARING) {
                        c.getSource().sendFailure(Component.literal("No core, or a wave is already running."));
                        return 0;
                    }
                    SiegeManager.startWave(l, data, false);
                    return 1;
                }))
                .then(Commands.literal("setwave").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("wave", IntegerArgumentType.integer(1, 999)).executes(c -> {
                            SiegeData data = SiegeManager.data(c.getSource().getLevel());
                            data.wave = IntegerArgumentType.getInteger(c, "wave");
                            data.nextWave = 0;
                            data.setDirty();
                            c.getSource().sendSuccess(() -> Component.literal("Next wave set to " + data.wave), true);
                            return 1;
                        })))
                .then(Commands.literal("credits").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-1_000_000, 1_000_000)).executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                    int amt = IntegerArgumentType.getInteger(c, "amount");
                                    SiegeData data = SiegeManager.data(c.getSource().getLevel());
                                    data.addCredits(p.getUUID(), amt);
                                    SiegeManager.syncHud(c.getSource().getLevel(), data);
                                    c.getSource().sendSuccess(() -> Component.literal("Gave " + amt + " credits to " + p.getGameProfile().getName()), true);
                                    return 1;
                                }))))
                .then(Commands.literal("spawn").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(ZombieKind.values()).map(k -> k.id), b))
                                .executes(c -> spawn(c.getSource(), StringArgumentType.getString(c, "type"), 0, false))
                                .then(Commands.argument("rank", IntegerArgumentType.integer(1, 5))
                                        .executes(c -> spawn(c.getSource(), StringArgumentType.getString(c, "type"), IntegerArgumentType.getInteger(c, "rank") - 1, false))
                                        .then(Commands.literal("champion")
                                                .executes(c -> spawn(c.getSource(), StringArgumentType.getString(c, "type"), IntegerArgumentType.getInteger(c, "rank") - 1, true))))))
                .then(Commands.literal("boss").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("boss", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(BossKind.values()).map(k -> k.id), b))
                                .executes(c -> {
                                    String id = StringArgumentType.getString(c, "boss");
                                    BossKind k = Arrays.stream(BossKind.values()).filter(b -> b.id.equals(id)).findFirst().orElse(null);
                                    if (k == null) {
                                        c.getSource().sendFailure(Component.literal("Unknown boss " + id));
                                        return 0;
                                    }
                                    SiegeBoss.spawn(c.getSource().getLevel(), k, c.getSource().getPosition(), Math.max(5, SiegeManager.wave(c.getSource().getLevel())), 1);
                                    return 1;
                                })))
                .then(Commands.literal("rules")
                        .executes(c -> {
                            SiegeManager.openRules(c.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.literal("list").executes(c -> {
                            SiegeRules r = SiegeManager.data(c.getSource().getLevel()).rules;
                            StringBuilder b = new StringBuilder("Siege rules:");
                            for (SiegeRules.Rule rule : SiegeRules.ALL) b.append("\n  ").append(rule.key).append(" = ").append(rule.format(r.get(rule)));
                            c.getSource().sendSuccess(() -> Component.literal(b.toString()).withStyle(ChatFormatting.GRAY), false);
                            return 1;
                        }))
                        .then(Commands.literal("set").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("rule", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(SiegeRules.ALL.stream().map(r -> r.key), b))
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg()).executes(c -> {
                                            SiegeRules.Rule rule = SiegeRules.byKey(StringArgumentType.getString(c, "rule"));
                                            if (rule == null) {
                                                c.getSource().sendFailure(Component.literal("Unknown rule. Try /siege rules list"));
                                                return 0;
                                            }
                                            ServerLevel l = c.getSource().getLevel();
                                            SiegeData data = SiegeManager.data(l);
                                            data.rules.set(rule, DoubleArgumentType.getDouble(c, "value"));
                                            data.nextWave = 0;
                                            data.setDirty();
                                            SiegeManager.syncHud(l, data);
                                            c.getSource().sendSuccess(() -> Component.literal(rule.label + " = " + rule.format(data.rules.get(rule))), true);
                                            return 1;
                                        }))))
                        .then(Commands.literal("preset").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("preset", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(SiegeRules.PRESETS).limit(4).map(n -> n.toLowerCase(Locale.ROOT)), b))
                                        .executes(c -> {
                                            String name = StringArgumentType.getString(c, "preset");
                                            for (int i = 0; i < 4; i++) {
                                                if (SiegeRules.PRESETS[i].equalsIgnoreCase(name)) {
                                                    SiegeData data = SiegeManager.data(c.getSource().getLevel());
                                                    data.rules.set(SiegeRules.PRESET, i);
                                                    data.nextWave = 0;
                                                    data.setDirty();
                                                    String n = SiegeRules.PRESETS[i];
                                                    c.getSource().sendSuccess(() -> Component.literal("Difficulty preset: " + n), true);
                                                    return 1;
                                                }
                                            }
                                            c.getSource().sendFailure(Component.literal("Presets: casual, normal, veteran, nightmare"));
                                            return 0;
                                        })))
                        .then(Commands.literal("reset").requires(s -> s.hasPermission(2)).executes(c -> {
                            SiegeData data = SiegeManager.data(c.getSource().getLevel());
                            data.rules.reset();
                            data.nextWave = 0;
                            data.setDirty();
                            c.getSource().sendSuccess(() -> Component.literal("Siege rules reset to the server defaults."), true);
                            return 1;
                        })))
                .then(Commands.literal("event").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("event", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(RaidEvents.Kind.values()).map(k -> k.name().toLowerCase(Locale.ROOT)), b))
                                .executes(c -> {
                                    ServerLevel l = c.getSource().getLevel();
                                    SiegeData data = SiegeManager.data(l);
                                    if (SiegeManager.corePos(l) == null) {
                                        c.getSource().sendFailure(Component.literal("War events need an active Siege Core."));
                                        return 0;
                                    }
                                    RaidEvents.Kind k;
                                    try {
                                        k = RaidEvents.Kind.valueOf(StringArgumentType.getString(c, "event").toUpperCase(Locale.ROOT));
                                    } catch (IllegalArgumentException ex) {
                                        c.getSource().sendFailure(Component.literal("Events: supply_drop, warlord, rift"));
                                        return 0;
                                    }
                                    boolean ok = RaidEvents.fire(l, data, SiegeManager.corePos(l), k);
                                    if (!ok) c.getSource().sendFailure(Component.literal("No room to place the event near the core."));
                                    return ok ? 1 : 0;
                                })))
                .then(Commands.literal("defeat").requires(s -> s.hasPermission(2)).executes(c -> {
                    ServerLevel l = c.getSource().getLevel();
                    SiegeData data = SiegeManager.data(l);
                    if (!data.hasCore()) return 0;
                    SiegeManager.defeat(l, data);
                    return 1;
                })));
    }

    private static int spawn(CommandSourceStack src, String type, int rank, boolean champion) {
        ZombieKind k = Arrays.stream(ZombieKind.values()).filter(z -> z.id.equals(type.toLowerCase(Locale.ROOT))).findFirst().orElse(null);
        if (k == null) {
            src.sendFailure(Component.literal("Unknown zombie type " + type));
            return 0;
        }
        int aff = 0;
        if (champion) {
            var r = src.getLevel().random;
            aff = Affix.values()[r.nextInt(Affix.values().length)].bit() | Affix.values()[r.nextInt(Affix.values().length)].bit();
        }
        ZombieSpawner.spawn(src.getLevel(), k, ZombieRank.byId(rank), aff, src.getPosition(), false, true);
        return 1;
    }
}
