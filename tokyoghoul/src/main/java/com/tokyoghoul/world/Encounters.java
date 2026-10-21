package com.tokyoghoul.world;

import com.tokyoghoul.combat.Fighters;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Characters;
import com.tokyoghoul.data.Faction;
import com.tokyoghoul.entity.TGCharacterEntity;
import com.tokyoghoul.player.PlayerCaps;
import com.tokyoghoul.registry.TGEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Who shows up around players. Jujutsu Craft spawns curses at night and gates stronger spawns
 * (SpawnLevel1..5) on the strongest player's level; here:
 * <ul>
 * <li>humans, CCG and Anteiku players: wild ghouls and Aogiri squads hunt them after dark;</li>
 * <li>ghoul players: CCG squads (investigators, Quinx, and eventually Arima) come for them, day or night;</li>
 * <li>by day civilians wander about, and Anteiku regulars drift through;</li>
 * <li>named characters appear as rare bosses once the strongest player's rank can face them.</li>
 * </ul>
 */
public final class Encounters {
    private static final int PERIOD = 400;

    private Encounters() {
    }

    /** JJK's STRONGEST_PLAYER map variable. */
    public static int strongestPlayer(MinecraftServer server) {
        int best = 1;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) best = Math.max(best, PlayerCaps.get(p).level);
        return best;
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % PERIOD != 0) return;
        ServerLevel level = server.overworld();
        if (!level.getGameRules().getBoolean(TGGameRules.ENCOUNTERS) || level.getDifficulty() == Difficulty.PEACEFUL) return;
        int strongest = strongestPlayer(server);
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.isCreative()) continue;
            RandomSource r = level.random;
            if (nearbyFighters(level, p) >= 8) continue;
            boolean night = level.isNight();
            Faction f = Fighters.faction(p);
            if (f == null) f = Faction.HUMAN;
            if (f.ghoul && r.nextFloat() < (night ? 0.18f : 0.25f)) {
                spawnSquad(level, p, Faction.CCG, strongest, r);
            } else if (!f.ghoul && night && r.nextFloat() < 0.3f) {
                spawnSquad(level, p, r.nextFloat() < 0.4f ? Faction.AOGIRI : Faction.WILD, strongest, r);
            } else if (f.ghoul && night && f != Faction.AOGIRI && r.nextFloat() < 0.12f) {
                spawnSquad(level, p, Faction.AOGIRI, strongest, r);
            }
            if (!night && r.nextFloat() < 0.2f) spawn(level, p, Characters.CIVILIAN, r, 20, 40);
            if (r.nextFloat() < 0.04f) {
                CharacterDef boss = pickBoss(Faction.ANTEIKU, strongest, r);
                if (boss != null) spawn(level, p, boss, r, 24, 48);
            }
        }
    }

    private static int nearbyFighters(ServerLevel level, ServerPlayer p) {
        return level.getEntitiesOfClass(TGCharacterEntity.class, new AABB(p.blockPosition()).inflate(64), e -> true).size();
    }

    /** A squad: 1-3 rank-and-file, sometimes led by a named character appropriate to the server's strength. */
    private static void spawnSquad(ServerLevel level, ServerPlayer p, Faction f, int strongest, RandomSource r) {
        List<CharacterDef> grunts = new ArrayList<>();
        switch (f) {
            case CCG -> grunts.add(Characters.INVESTIGATOR);
            case AOGIRI -> grunts.add(Characters.AOGIRI_GRUNT);
            default -> {
                grunts.add(Characters.UKAKU_GHOUL);
                grunts.add(Characters.KOUKAKU_GHOUL);
                grunts.add(Characters.RINKAKU_GHOUL);
                grunts.add(Characters.BIKAKU_GHOUL);
            }
        }
        int n = 1 + r.nextInt(Math.min(3, 1 + strongest / 2));
        BlockPos anchor = null;
        for (int i = 0; i < n; i++) {
            TGCharacterEntity e = spawn(level, p, grunts.get(r.nextInt(grunts.size())), r, 18, 34);
            if (e != null && anchor == null) anchor = e.blockPosition();
        }
        float bossChance = 0.08f + 0.05f * strongest;
        if (r.nextFloat() < bossChance) {
            CharacterDef boss = pickBoss(f == Faction.WILD ? null : f, strongest, r);
            if (boss != null) {
                TGCharacterEntity e = spawn(level, p, boss, r, 22, 36);
                if (e != null && e.fighterDef().npcLevel >= 7) {
                    p.sendSystemMessage(Component.translatable("msg.tokyoghoul.boss_near", e.getDisplayName()).withStyle(ChatFormatting.DARK_RED));
                }
            }
        }
    }

    /** Named characters of a faction whose level the strongest player can face (JJK's SpawnLevel gates). */
    @Nullable
    public static CharacterDef pickBoss(@Nullable Faction f, int strongest, RandomSource r) {
        int cap = 3 + (int) Math.ceil(strongest * 1.1);
        List<CharacterDef> pool = new ArrayList<>();
        for (CharacterDef d : Characters.all()) {
            if (!d.boss || d.npcLevel > cap) continue;
            // a null faction means "any hostile ghoul" (wild raids can bring Rize, Aogiri or the Clowns)
            if (f == null ? !d.faction.ghoul || d.faction == Faction.ANTEIKU : d.faction != f) continue;
            pool.add(d);
        }
        return pool.isEmpty() ? null : pool.get(r.nextInt(pool.size()));
    }

    @Nullable
    public static TGCharacterEntity spawn(ServerLevel level, ServerPlayer near, CharacterDef def, RandomSource r, int minDist, int maxDist) {
        EntityType<TGCharacterEntity> type = TGEntities.typeOf(def.id);
        if (type == null) return null;
        for (int attempt = 0; attempt < 8; attempt++) {
            double a = r.nextDouble() * Math.PI * 2;
            int d = minDist + r.nextInt(Math.max(1, maxDist - minDist));
            int x = (int) (near.getX() + Math.cos(a) * d), z = (int) (near.getZ() + Math.sin(a) * d);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos.below()).isSolid() || level.getFluidState(pos.below()).isSource()) continue;
            if (Math.abs(y - near.getY()) > 20) continue;
            TGCharacterEntity e = type.create(level);
            if (e == null) return null;
            e.moveTo(x + 0.5, y, z + 0.5, r.nextFloat() * 360, 0);
            if (!level.noCollision(e)) continue;
            e.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            level.addFreshEntityWithPassengers(e);
            return e;
        }
        return null;
    }
}
