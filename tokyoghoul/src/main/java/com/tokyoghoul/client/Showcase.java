package com.tokyoghoul.client;

import com.tokyoghoul.TokyoGhoul;
import com.tokyoghoul.client.screen.SelectScreen;
import com.tokyoghoul.combat.FighterState;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Characters;
import com.tokyoghoul.entity.TGCharacterEntity;
import com.tokyoghoul.item.QuinqueKind;
import com.tokyoghoul.move.Anims;
import com.tokyoghoul.network.Packets.Action;
import com.tokyoghoul.move.MoveDef;
import com.tokyoghoul.player.PlayerActions;
import com.tokyoghoul.player.PlayerCaps;
import com.tokyoghoul.registry.TGEntities;
import com.tokyoghoul.registry.TGItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Developer showcase, enabled with {@code TG_SHOWCASE=1}: builds a flat world, lines up every character
 * with its kagune out, shows transformations and mid-move poses, quinques and masks on stands, the
 * player as Kaneki with the HUD, the Ghoul Registry screen and a live NPC battle, photographing each.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoul.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Showcase {
    private static final boolean ENABLED = System.getenv("TG_SHOWCASE") != null;
    private static final int GROUND = -60;
    private static final double ROW_Z = 0, FORM_Z = 20, MOVE_Z = 40, GEAR_Z = 60;
    private static int stage, ticks, shot;

    private record Shot(String name, Vec3 pos, float yaw, float pitch, Runnable before) {
    }

    private static final List<Shot> SHOTS = new ArrayList<>();
    private static final List<TGCharacterEntity> POSED = new ArrayList<>();

    private Showcase() {
    }

    private static double charX(int i) {
        return i * 4.0;
    }

    private static void planShots() {
        SHOTS.clear();
        List<CharacterDef> all = Characters.all();
        for (int i = 0; i < all.size(); i++) {
            CharacterDef d = all.get(i);
            float h = 1.8f * d.look.height;
            SHOTS.add(new Shot("10_" + d.id, new Vec3(charX(i), GROUND + h * 0.6 + 0.2, ROW_Z - (h * 1.3 + 1.6)), 0, 8, null));
        }
        String[] forms = {"kaneki", "eto", "yoshimura", "shinohara", "jason", "tatara"};
        for (int i = 0; i < forms.length; i++) {
            CharacterDef d = Characters.get(forms[i]);
            float h = 1.8f * d.look.height * (d.ultimate() == null ? 1f : d.ultimate().formScale);
            SHOTS.add(new Shot("20_form_" + forms[i], new Vec3(i * 8.0 + 0.5, GROUND + h * 0.6 + 0.3, FORM_Z - (h * 1.3 + 2.0)), 0, 10, null));
        }
        // mid-move poses: re-trigger the animation just before each photo
        String[][] moves = {{"kaneki", "rinkaku_pierce"}, {"kaneki", "tentacle_lash"}, {"touka", "shard_barrage"}, {"tsukiyama", "gourmet_guard"},
                {"nishiki", "tail_strike"}, {"arima", "ixa_lance"}, {"juuzou", "jason_scythe"}, {"amon", "doujima_crush"}, {"urie", "koukaku_blades"},
                {"saiko", "massive_rinkaku"}};
        for (int i = 0; i < moves.length; i++) {
            String[] m = moves[i];
            double x = i * 7.0;
            SHOTS.add(new Shot("30_move_" + m[0] + "_" + m[1], new Vec3(x + 3.2, GROUND + 2.2, MOVE_Z - 5.5), 25, 12, () -> poseMove(m[0], m[1])));
        }
        SHOTS.add(new Shot("40_quinques", new Vec3(16, GROUND + 1.8, GEAR_Z - 5.2), 0, 10, null));
        SHOTS.add(new Shot("41_masks", new Vec3(13, GROUND + 2.0, GEAR_Z + 6 - 4.6), 0, 8, null));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (!ENABLED || e.phase != TickEvent.Phase.END) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            ticks++;
            switch (stage) {
                case 0 -> {
                    if (ticks > 100) {
                        createWorld(mc);
                        stage = 1;
                        ticks = 0;
                    }
                }
                case 1 -> {
                    if (mc.level != null && mc.player != null && ticks > 60) {
                        planShots();
                        MinecraftServer server = mc.getSingleplayerServer();
                        if (server != null) server.execute(() -> buildScene(server));
                        mc.options.hideGui = true;
                        stage = 2;
                        ticks = 0;
                    }
                }
                case 2 -> {
                    if (ticks > 100) {
                        stage = 3;
                        ticks = 0;
                        shot = 0;
                        moveTo(mc, SHOTS.get(0));
                    }
                }
                case 3 -> {
                    Shot cur = SHOTS.get(shot);
                    if (ticks == 30 && cur.before() != null) {
                        MinecraftServer server = mc.getSingleplayerServer();
                        if (server != null) server.execute(cur.before());
                    }
                    if (ticks == (cur.before() != null ? 38 : 40)) grab(mc, cur.name());
                    if (ticks > 42) {
                        shot++;
                        ticks = 0;
                        if (shot >= SHOTS.size()) {
                            stage = 4;
                            playerScene(mc);
                        } else {
                            moveTo(mc, SHOTS.get(shot));
                        }
                    }
                }
                case 4 -> {
                    // the player as Kaneki with the HUD, kagune out, seen in third person
                    if (ticks == 50) {
                        mc.options.hideGui = false;
                        mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                    }
                    if (ticks == 90) grab(mc, "50_player_kaneki");
                    if (ticks == 95) mc.setScreen(new SelectScreen());
                    if (ticks == 130) grab(mc, "51_registry_screen");
                    if (ticks == 135) {
                        mc.setScreen(null);
                        mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                        mc.options.hideGui = true;
                        MinecraftServer server = mc.getSingleplayerServer();
                        if (server != null) server.execute(() -> startBattle(server));
                        moveTo(mc, new Shot("battle", new Vec3(0.5, GROUND + 6, -112), 0, 16, null));
                    }
                    if (ticks == 260) grab(mc, "60_battle_a");
                    if (ticks == 380) grab(mc, "61_battle_b");
                    if (ticks == 500) grab(mc, "62_battle_c");
                    if (ticks == 520) {
                        stage = 5;
                        ticks = 0;
                        MinecraftServer server = mc.getSingleplayerServer();
                        if (server != null) server.execute(() -> playtestSetup(server, mc.player.getUUID()));
                    }
                }
                case 5 -> playtest(mc);
                default -> {
                }
            }
        } catch (Exception | LinkageError err) {
            com.tokyoghoul.util.Safe.report("Showcase", err);
        }
    }

    // ------------------------------------------------------------------ playtest

    /**
     * Plays as Kaneki against a live CCG investigator using the real key packets (the same path as
     * pressing C, X, R, Z, G and B), photographing each technique and logging the outcome.
     */
    private static void playtest(Minecraft mc) {
        if (mc.player == null) return;
        int t = ticks;
        if (t == 20) {
            mc.options.hideGui = false;
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        }
        if (t >= 20 && t % 2 == 0) faceTarget(mc);
        switch (t) {
            case 40 -> key(Action.TOGGLE_KAGUNE);
            case 60 -> {
                key(Action.RESET);
                key(Action.USE_PRESS);
            }
            case 62 -> key(Action.USE_RELEASE);
            case 66 -> grab(mc, "70_play_attack");
            case 80 -> {
                key(Action.CYCLE_NEXT);
                key(Action.USE_PRESS);
            }
            case 82 -> key(Action.USE_RELEASE);
            case 110 -> {
                key(Action.CYCLE_NEXT);
                key(Action.USE_PRESS);
            }
            case 128 -> grab(mc, "71_play_barrage");
            case 150 -> key(Action.USE_RELEASE);
            case 170 -> {
                key(Action.CYCLE_NEXT);
                key(Action.USE_PRESS);
            }
            case 172 -> key(Action.USE_RELEASE);
            case 180 -> grab(mc, "72_play_rinkaku_pierce");
            case 210 -> {
                key(Action.CYCLE_NEXT);
                key(Action.USE_PRESS);
            }
            case 212 -> key(Action.USE_RELEASE);
            case 219 -> grab(mc, "73_play_tentacle_lash");
            case 250 -> {
                key(Action.CYCLE_NEXT);
                key(Action.USE_PRESS);
            }
            case 252 -> key(Action.USE_RELEASE);
            case 268 -> grab(mc, "74_play_kagune_grab");
            case 310 -> key(Action.GUARD_PRESS);
            case 325 -> grab(mc, "75_play_guard");
            case 340 -> key(Action.GUARD_RELEASE);
            case 345 -> key(Action.DODGE);
            case 360 -> {
                // X then Shift+R wraps to the ultimate: Kakuja Centipede
                key(Action.RESET);
                key(Action.CYCLE_PREV);
                key(Action.USE_PRESS);
            }
            case 362 -> key(Action.USE_RELEASE);
            case 410 -> grab(mc, "76_play_kakuja");
            case 420 -> {
                key(Action.RESET);
                key(Action.CYCLE_NEXT);
                key(Action.CYCLE_NEXT);
                key(Action.CYCLE_NEXT);
                key(Action.USE_PRESS);
            }
            case 422 -> key(Action.USE_RELEASE);
            case 432 -> grab(mc, "77_play_kakuja_pierce");
            case 480 -> {
                MinecraftServer server = mc.getSingleplayerServer();
                if (server != null) server.execute(() -> playtestReport(server, mc.player.getUUID()));
            }
            case 500 -> {
                TokyoGhoul.LOGGER.info("[Showcase] complete");
                mc.stop();
            }
            default -> {
            }
        }
    }

    private static void key(Action a) {
        com.tokyoghoul.network.TGNet.CHANNEL.sendToServer(new com.tokyoghoul.network.Packets.Key(a));
    }

    private static void faceTarget(Minecraft mc) {
        if (mc.level == null) return;
        TGCharacterEntity near = null;
        double best = 40 * 40;
        for (var e : mc.level.entitiesForRendering()) {
            if (e instanceof TGCharacterEntity t && t.isAlive() && t.fighterDef().id.equals("investigator")) {
                double d = t.distanceToSqr(mc.player);
                if (d < best) {
                    best = d;
                    near = t;
                }
            }
        }
        if (near != null) mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, near.getEyePosition());
    }

    private static int playtestErrors;

    private static void playtestSetup(MinecraftServer server, java.util.UUID id) {
        ServerLevel level = server.overworld();
        level.setDayTime(6000);
        for (var e : level.getAllEntities()) {
            if (e instanceof TGCharacterEntity t && !t.isNoAi()) t.discard();
        }
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p == null) return;
        p.setGameMode(GameType.SURVIVAL);
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 20000, 3, false, false));
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 20000, 2, false, false));
        var d = PlayerCaps.get(p);
        d.kakuja = 100;
        d.rc = d.maxRc();
        d.state.kaguneToggled = false;
        d.state.form = null;
        d.state.formTicks = 0;
        p.connection.teleport(0.5, GROUND, -150.5, 0, 10);
        TGCharacterEntity inv = place(level, "investigator", 0.5, -145.5, false);
        if (inv != null) {
            inv.setNoAi(false);
            inv.setFighterLevel(4);
            inv.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(400);
            inv.setHealth(400);
            inv.setTarget(p);
        }
        playtestErrors = com.tokyoghoul.util.Safe.ERRORS.get();
        com.tokyoghoul.network.Sync.playerData(p);
    }

    private static void playtestReport(MinecraftServer server, java.util.UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        String inv = "dead";
        for (var e : server.overworld().getAllEntities()) {
            if (e instanceof TGCharacterEntity t && t.fighterDef().id.equals("investigator") && !t.isNoAi()) {
                inv = String.format("%.0f/%.0f HP", t.getHealth(), t.getMaxHealth());
            }
        }
        var d = p == null ? null : PlayerCaps.get(p);
        TokyoGhoul.LOGGER.info("[Playtest] investigator {} | player HP {} | Rc {}/{} | fame {} kills {} | kakuja form {} | new errors {}", inv,
                p == null ? "?" : String.format("%.1f", p.getHealth()), d == null ? 0 : (int) d.rc, d == null ? 0 : (int) d.maxRc(),
                d == null ? 0 : (int) d.fame, d == null ? 0 : d.kills, d != null && d.state.transformed(),
                com.tokyoghoul.util.Safe.ERRORS.get() - playtestErrors);
    }

    private static void createWorld(Minecraft mc) {
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        LevelSettings settings = new LevelSettings("tg_showcase_" + System.currentTimeMillis(), GameType.CREATIVE, false, Difficulty.NORMAL, true, rules,
                WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(0L, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }

    private static TGCharacterEntity place(ServerLevel level, String id, double x, double z, boolean kagune) {
        var type = TGEntities.typeOf(id);
        if (type == null) return null;
        TGCharacterEntity e = type.create(level);
        if (e == null) return null;
        e.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.ZERO), net.minecraft.world.entity.MobSpawnType.EVENT, null, null);
        e.moveTo(x, GROUND, z, 180f, 0f);
        e.setYHeadRot(180f);
        e.setYBodyRot(180f);
        e.setNoAi(true);
        e.fighterState().kaguneToggled = kagune;
        level.addFreshEntity(e);
        POSED.add(e);
        return e;
    }

    private static void buildScene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        level.setDayTime(6000);
        level.getGameRules().getRule(com.tokyoghoul.world.TGGameRules.ENCOUNTERS).set(false, server);
        for (ServerPlayer p : level.players()) {
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }
        List<CharacterDef> all = Characters.all();
        for (int i = 0; i < all.size(); i++) place(level, all.get(i).id, charX(i) + 0.5, ROW_Z + 0.5, true);
        String[] forms = {"kaneki", "eto", "yoshimura", "shinohara", "jason", "tatara"};
        for (int i = 0; i < forms.length; i++) {
            TGCharacterEntity e = place(level, forms[i], i * 8.0 + 0.5, FORM_Z + 0.5, true);
            if (e != null) {
                FighterState s = e.fighterState();
                s.form = e.fighterDef().ultimate();
                s.formTicks = 100000;
                e.playAnim(Anims.TRANSFORM);
            }
        }
        String[] movers = {"kaneki", "kaneki", "touka", "tsukiyama", "nishiki", "arima", "juuzou", "amon", "urie", "saiko"};
        for (int i = 0; i < movers.length; i++) {
            TGCharacterEntity e = place(level, movers[i], i * 7.0 + 0.5, MOVE_Z + 0.5, true);
            if (e != null) e.addTag("tg_mover_" + i);
        }
        // quinques on stands
        QuinqueKind[] qk = QuinqueKind.values();
        for (int i = 0; i < qk.length; i++) {
            ArmorStand st = stand(level, 2.5 + i * 2.0, GEAR_Z, "{RightArm:[-60f,0f,0f],LeftArm:[-20f,0f,-10f]}");
            if (st != null) st.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(TGItems.QUINQUES.get(qk[i]).get()));
        }
        int i = 0;
        for (var m : TGItems.MASKS.values()) {
            ArmorStand st = stand(level, 2.5 + i * 2.0, GEAR_Z + 6, "{}");
            if (st != null) {
                st.setItemSlot(EquipmentSlot.HEAD, new ItemStack(m.get()));
                if (i % 3 == 0) st.setItemSlot(EquipmentSlot.CHEST, new ItemStack(TGItems.CCG_COAT.get()));
            }
            i++;
        }
    }

    private static void poseMove(String id, String move) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) return;
        MoveDef d = Characters.move(move);
        for (TGCharacterEntity e : POSED) {
            if (!e.fighterDef().id.equals(id) || e.getTags().stream().noneMatch(t -> t.startsWith("tg_mover_"))) continue;
            if (d != null) e.playAnim(d.anim);
        }
    }

    private static ArmorStand stand(ServerLevel level, double x, double z, String pose) {
        ArmorStand st = EntityType.ARMOR_STAND.create(level);
        if (st == null) return null;
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ShowArms", true);
        tag.putBoolean("NoBasePlate", true);
        tag.putBoolean("NoGravity", true);
        try {
            tag.put("Pose", net.minecraft.nbt.TagParser.parseTag(pose));
        } catch (Exception ignored) {
        }
        st.load(tag);
        st.moveTo(x, GROUND, z, 180, 0);
        st.setYBodyRot(180);
        st.setYHeadRot(180);
        level.addFreshEntity(st);
        return st;
    }

    private static void playerScene(Minecraft mc) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) return;
        var id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) return;
            p.getInventory().add(new ItemStack(TGItems.GHOUL_REGISTRY.get()));
            PlayerActions.select(p, "kaneki");
            var d = PlayerCaps.get(p);
            d.level = 5;
            d.fame = 1200;
            d.kakuja = 64;
            d.rc = d.maxRc() * 0.7;
            d.state.kaguneToggled = true;
            com.tokyoghoul.network.Sync.visual(p);
            com.tokyoghoul.network.Sync.playerData(p);
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            p.connection.teleport(0.5, GROUND, -80.5, 180, 15);
        });
    }

    private static void startBattle(MinecraftServer server) {
        ServerLevel level = server.overworld();
        level.setDayTime(18000);
        String[] ghouls = {"kaneki", "ayato", "nishiki", "rinkaku_ghoul", "ukaku_ghoul"};
        String[] ccg = {"arima", "amon", "juuzou", "investigator", "urie"};
        for (int i = 0; i < ghouls.length; i++) {
            TGCharacterEntity g = place(level, ghouls[i], -6 + i * 3, -100, false);
            TGCharacterEntity c = place(level, ccg[i], -6 + i * 3, -92, false);
            if (g != null) g.setNoAi(false);
            if (c != null) c.setNoAi(false);
        }
    }

    private static void moveTo(Minecraft mc, Shot s) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) return;
        var id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) p.connection.teleport(s.pos().x, s.pos().y, s.pos().z, s.yaw(), s.pitch());
        });
    }

    private static void grab(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> TokyoGhoul.LOGGER.info("[Showcase] {}", msg.getString()));
    }
}
