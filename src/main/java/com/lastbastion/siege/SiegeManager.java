package com.lastbastion.siege;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.config.SiegeConfig;
import com.lastbastion.entity.boss.BossKind;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.entity.boss.SiegeBoss;
import com.lastbastion.entity.hero.HeroEntity;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.entity.zombie.ZombieKind;
import com.lastbastion.entity.zombie.ZombieRank;
import com.lastbastion.entity.zombie.ZombieSpawner;
import com.lastbastion.network.CreditPopupPacket;
import com.lastbastion.network.HudSyncPacket;
import com.lastbastion.network.Net;
import com.lastbastion.registry.ModBlocks;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

/** Drives the siege: night detection, wave spawning, completion, defeat, credits and HUD sync. */
public final class SiegeManager {
    private SiegeManager() {
    }

    public static SiegeData data(ServerLevel level) {
        return SiegeData.get(level.getServer());
    }

    private static boolean isCoreLevel(ServerLevel level, SiegeData d) {
        return d.hasCore() && level.dimension().location().toString().equals(d.coreDim);
    }

    @Nullable
    public static BlockPos corePos(ServerLevel level) {
        SiegeData d = data(level);
        return isCoreLevel(level, d) ? d.corePos : null;
    }

    public static NightModifier modifier(ServerLevel level) {
        SiegeData d = data(level);
        return d.state == SiegeState.WAVE ? d.modifier : NightModifier.NONE;
    }

    public static int wave(ServerLevel level) {
        return data(level).wave;
    }

    public static boolean isNight(ServerLevel level) {
        long t = level.getDayTime() % 24000L;
        return t >= 13000L && t < 23000L;
    }

    public static List<ServerPlayer> playersNear(ServerLevel level, BlockPos core, double radius) {
        List<ServerPlayer> out = new ArrayList<>();
        Vec3 c = Vec3.atCenterOf(core);
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.position().distanceToSqr(c) <= radius * radius) out.add(p);
        }
        return out;
    }

    // ------------------------------------------------------------------ core placement

    /** @return null on success, otherwise the reason the core can't be placed. */
    @Nullable
    public static Component tryPlaceCore(ServerLevel level, BlockPos pos, @Nullable ServerPlayer placer) {
        SiegeData d = data(level);
        if (d.hasCore() && d.corePos != null && !(d.corePos.equals(pos) && isCoreLevel(level, d))) {
            return Component.translatable("msg.lastbastion.core_exists", d.corePos.toShortString());
        }
        d.state = SiegeState.PREPARING;
        d.corePos = pos.immutable();
        d.coreDim = level.dimension().location().toString();
        d.coreOwner = placer != null ? placer.getUUID() : null;
        d.coreHp = d.maxHp();
        d.shield = d.maxShield();
        d.queue.clear();
        d.tracked.clear();
        d.calledEarly = false;
        wasNight = isNight(level);
        d.setDirty();
        Vec3 c = Vec3.atCenterOf(pos);
        Fx.skyBeam(level, c, 0x6FE3FF, 40, 1.5);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 0.8f);
        for (ServerPlayer p : playersNear(level, pos, 64)) {
            title(p, Component.literal("THE SIEGE BEGINS").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.literal("Defend the core. Wave " + d.wave + " arrives at nightfall.").withStyle(ChatFormatting.GRAY));
        }
        return null;
    }

    public static void onCoreRemoved(ServerLevel level, BlockPos pos) {
        SiegeData d = data(level);
        if (!isCoreLevel(level, d) || !pos.equals(d.corePos)) return;
        clearEnemies(level, d);
        d.state = SiegeState.INACTIVE;
        d.corePos = null;
        d.setDirty();
    }

    public static boolean canBreakCore(ServerLevel level) {
        return data(level).state != SiegeState.WAVE;
    }

    // ------------------------------------------------------------------ tick

    private static boolean wasNight;
    /** GameTests have no players; this lets the siege progress anyway. */
    public static boolean forcePresence;

    public static void tick(ServerLevel level) {
        SiegeData d = data(level);
        if (!isCoreLevel(level, d) || d.corePos == null) return;
        BlockPos core = d.corePos;
        if (!level.isLoaded(core)) return;
        if (!level.getBlockState(core).is(ModBlocks.SIEGE_CORE.get())) {
            // core vanished (e.g. /setblock) - shut the siege down quietly
            onCoreRemoved(level, core);
            return;
        }
        boolean present = forcePresence || !playersNear(level, core, SiegeConfig.PLAYER_PRESENCE_RADIUS.get()).isEmpty();
        long gt = level.getGameTime();
        boolean night = isNight(level);

        // passive core systems
        if (gt % 20 == 0) {
            float maxShield = d.maxShield();
            if (maxShield > 0 && level.getGameTime() - d.lastHitTick > 100) d.shield = Math.min(maxShield, d.shield + maxShield * 0.04f);
            if (d.state == SiegeState.PREPARING) d.coreHp = Math.min(d.maxHp(), d.coreHp + d.maxHp() * 0.005f);
            if (d.pulseCooldown > 0) d.pulseCooldown = Math.max(0, d.pulseCooldown - 20);
            for (int i = 0; i < d.abilityCd.length; i++) d.abilityCd[i] = Math.max(0, d.abilityCd[i] - 20);
            if (d.aegisTicks > 0) Vfx.dome(level, Vec3.atBottomCenterOf(core), 0x8FD8FF, 3.4f, 22, null);
            d.setDirty();
        }
        if (d.aegisTicks > 0) d.aegisTicks--;
        if (d.overdriveTicks > 0) d.overdriveTicks--;
        if (d.state == SiegeState.WAVE && present) {
            if (gt % 30 == 0) sentinel(level, d, core);
            if (gt % 40 == 0 && d.coreHp < d.maxHp() * 0.25f) {
                Vfx.ring(level, Vec3.atBottomCenterOf(core).add(0, 0.1, 0), 0xFF3020, 4.5f, 16, 0.6f);
                level.playSound(null, core, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.5f, 0.5f);
            }
        }
        int beacon = d.level(CoreUpgrade.BEACON);
        if (beacon > 0 && gt % 40 == 0) {
            for (LivingEntity e : SiegeDamage.defendersAround(level, Vec3.atCenterOf(core), 16)) {
                if (e instanceof TurretEntity) continue;
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, beacon - 1, true, false));
                if (beacon >= 2) e.addEffect(new MobEffectInstance(ModEffects.RALLY.get(), 60, 0, true, false));
            }
        }

        if (d.state == SiegeState.PREPARING) {
            if (present && night && !wasNight) startWave(level, d, false);
        } else if (d.state == SiegeState.WAVE && present) {
            tickWave(level, d, core);
        }
        wasNight = night;

        if (gt % 10 == 0) syncHud(level, d);
    }

    // ------------------------------------------------------------------ waves

    public static void callWaveEarly(ServerLevel level, ServerPlayer by) {
        SiegeData d = data(level);
        if (d.state != SiegeState.PREPARING || !isCoreLevel(level, d)) return;
        if (!isNight(level) && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT)) {
            long day = level.getDayTime();
            level.setDayTime(day - day % 24000L + 13000L);
        }
        wasNight = true;
        startWave(level, d, true);
        broadcast(level, d, Component.literal(by.getGameProfile().getName() + " sounded the war horn early! (+25% wave bonus)").withStyle(ChatFormatting.GOLD));
    }

    public static void startWave(ServerLevel level, SiegeData d, boolean early) {
        BlockPos core = d.corePos;
        if (core == null) return;
        int players = Math.max(1, playersNear(level, core, SiegeConfig.PLAYER_PRESENCE_RADIUS.get()).size());
        d.modifier = rollModifier(level, d);
        d.lastModifier = d.modifier;
        d.queue.clear();
        d.queue.addAll(WavePlanner.plan(d.wave, players, d.modifier, level.random));
        d.tracked.clear();
        d.waveTick = 0;
        d.waveTotal = WavePlanner.count(d.queue);
        d.waveKilled = 0;
        d.calledEarly = early;
        d.state = SiegeState.WAVE;
        d.setDirty();
        if (d.modifier == NightModifier.STORM) level.setWeatherParameters(0, 12000, true, true);
        boolean boss = WavePlanner.isBossWave(d.wave);
        Component t = Component.literal("WAVE " + d.wave).withStyle(boss ? ChatFormatting.DARK_RED : ChatFormatting.RED, ChatFormatting.BOLD);
        Component sub = d.modifier != NightModifier.NONE
                ? Component.literal(d.modifier.title + " - " + d.modifier.desc).withStyle(s -> s.withColor(d.modifier.color))
                : Component.literal(boss ? "A great evil approaches..." : d.waveTotal + " undead rise from the earth").withStyle(ChatFormatting.GRAY);
        for (ServerPlayer p : playersNear(level, core, 256)) {
            title(p, t, sub);
            p.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 2f, boss ? 0.7f : 1f);
        }
    }

    private static NightModifier rollModifier(ServerLevel level, SiegeData d) {
        if (!SiegeConfig.NIGHT_MODIFIERS.get() || d.wave < 6 || level.random.nextFloat() > 0.35f || d.lastModifier != NightModifier.NONE) {
            return NightModifier.NONE;
        }
        NightModifier[] all = NightModifier.values();
        return all[1 + level.random.nextInt(all.length - 1)];
    }

    private static void tickWave(ServerLevel level, SiegeData d, BlockPos core) {
        d.waveTick++;
        int maxAlive = SiegeConfig.MAX_ALIVE.get();
        while (!d.queue.isEmpty() && d.queue.get(0).tick <= d.waveTick && d.tracked.size() < maxAlive) {
            WavePlanner.Group g = d.queue.remove(0);
            spawnGroup(level, d, core, g);
        }
        if (d.waveTick % 20 == 0) {
            d.tracked.removeIf(id -> {
                Entity e = level.getEntity(id);
                return e == null || !e.isAlive();
            });
            d.bossAlive = d.tracked.stream().map(level::getEntity).anyMatch(e -> e instanceof SiegeBoss);
            if (d.queue.isEmpty() && d.tracked.isEmpty()) {
                completeWave(level, d, core);
                return;
            }
            // stragglers glow so they can be hunted down
            if (d.queue.isEmpty() && d.waveTick % 600 == 0 && d.tracked.size() <= 6) {
                for (UUID id : d.tracked) {
                    if (level.getEntity(id) instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 620, 0, false, false));
                }
            }
            d.setDirty();
        }
        if (d.modifier == NightModifier.STORM && d.waveTick % 90 == 0) stormStrike(level, d);
        if (d.modifier == NightModifier.BLOOD_MOON && d.waveTick % 40 == 0) {
            for (UUID id : d.tracked) {
                if (level.getEntity(id) instanceof SiegeZombie z) {
                    z.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false));
                    z.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false));
                }
            }
        }
    }

    private static void stormStrike(ServerLevel level, SiegeData d) {
        List<UUID> ids = new ArrayList<>(d.tracked);
        if (ids.isEmpty()) return;
        Entity e = level.getEntity(ids.get(level.random.nextInt(ids.size())));
        if (!(e instanceof LivingEntity target)) return;
        Vec3 p = target.position();
        Fx.bolt(level, p.add(level.random.nextGaussian() * 3, 30, level.random.nextGaussian() * 3), p, 0xEFF6FF, 1.4f);
        level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 3f, 1f);
        SiegeDamage.areaDamage(level, p, 3, 12 * WavePlanner.damageScale(d.wave), Element.LIGHTNING, null, null, true);
    }

    private static void spawnGroup(ServerLevel level, SiegeData d, BlockPos core, WavePlanner.Group g) {
        int radius = SiegeConfig.SPAWN_RADIUS.get();
        float hp = WavePlanner.hpScale(d.wave), dmg = WavePlanner.damageScale(d.wave);
        if (d.modifier == NightModifier.BLOOD_MOON) dmg *= 1.25f;
        for (WavePlanner.Unit u : g.units) {
            double a = g.angle + (level.random.nextDouble() - 0.5) * 0.3;
            Vec3 pos = findSpawn(level, core, a, radius + level.random.nextInt(7) - 3);
            if (pos == null) continue;
            Entity spawned;
            if (u.isBoss()) {
                BossKind bk = BossKind.values()[u.kind() - WavePlanner.BOSS_OFFSET];
                int players = Math.max(1, playersNear(level, core, SiegeConfig.PLAYER_PRESENCE_RADIUS.get()).size());
                spawned = SiegeBoss.spawn(level, bk, pos, d.wave, players);
                if (spawned != null) announceBoss(level, core, bk);
            } else {
                spawned = ZombieSpawner.spawn(level, ZombieKind.values()[u.kind()], ZombieRank.byId(u.rank()), u.affixes(), pos, true, true, hp, dmg);
            }
            if (spawned != null) d.tracked.add(spawned.getUUID());
        }
        d.setDirty();
    }

    private static void announceBoss(ServerLevel level, BlockPos core, BossKind bk) {
        for (ServerPlayer p : playersNear(level, core, 256)) {
            title(p, Component.literal(bk.title).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    Component.literal("BOSS INCOMING").withStyle(ChatFormatting.RED));
            p.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1f, 0.8f);
        }
    }

    /** Find a surface spawn point at the given angle/radius, shrinking inward if chunks aren't loaded. */
    @Nullable
    public static Vec3 findSpawn(ServerLevel level, BlockPos core, double angle, int radius) {
        for (int r = radius; r >= 12; r -= 4) {
            for (int attempt = 0; attempt < 4; attempt++) {
                double a = angle + (attempt == 0 ? 0 : (level.random.nextDouble() - 0.5) * 0.6);
                int x = core.getX() + (int) Math.round(Math.cos(a) * r);
                int z = core.getZ() + (int) Math.round(Math.sin(a) * r);
                BlockPos col = new BlockPos(x, core.getY(), z);
                if (!level.isLoaded(col)) continue;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos p = new BlockPos(x, y, z);
                if (Math.abs(y - core.getY()) > 40) continue;
                if (!level.getFluidState(p.below()).isEmpty() && attempt < 3) continue;
                if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) {
                    return new Vec3(x + 0.5, y, z + 0.5);
                }
            }
        }
        return null;
    }

    private static void completeWave(ServerLevel level, SiegeData d, BlockPos core) {
        int cleared = d.wave;
        float bonusBase = (50 + 20 * cleared) * (d.calledEarly ? 1.25f : 1f);
        List<ServerPlayer> players = playersNear(level, core, SiegeConfig.PLAYER_PRESENCE_RADIUS.get());
        for (ServerPlayer p : players) {
            int got = award(level, d, p.getUUID(), bonusBase, null, false);
            title(p, Component.literal("WAVE " + cleared + " CLEARED").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                    Component.literal("+" + got + " credits").withStyle(ChatFormatting.YELLOW));
            p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1f, 1f);
        }
        if (d.modifier == NightModifier.STORM) level.setWeatherParameters(6000, 0, false, false);
        d.bestWave = Math.max(d.bestWave, cleared);
        d.wave = cleared + 1;
        d.state = SiegeState.PREPARING;
        d.modifier = NightModifier.NONE;
        d.queue.clear();
        d.tracked.clear();
        d.calledEarly = false;
        d.setDirty();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof HeroEntity h) h.onWaveCleared();
        }
        Fx.skyBeam(level, Vec3.atCenterOf(core), 0x7CFF9B, 30, 1.2);
    }

    // ------------------------------------------------------------------ core damage / defeat

    public static void damageCore(ServerLevel level, float amount, @Nullable LivingEntity attacker) {
        SiegeData d = data(level);
        if (!isCoreLevel(level, d) || d.corePos == null || amount <= 0) return;
        Vec3 c = Vec3.atCenterOf(d.corePos);
        if (d.aegisTicks > 0) {
            Vfx.flash(level, c.add(0, 1.6, 0), 0x8FD8FF, 1.2f, 4);
            level.playSound(null, d.corePos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1f, 1.4f);
            return;
        }
        long now = level.getGameTime();
        boolean fx = now - lastCoreFx > 6;
        if (fx) lastCoreFx = now;
        d.lastHitTick = (int) now;
        if (d.shield > 0 && fx) {
            Vfx.dome(level, Vec3.atBottomCenterOf(d.corePos), 0x6FE3FF, 3.2f, 10, null);
            Vfx.spray(level, FxStyle.SHARD, c.add(0, 0.8, 0), 0xBFF4FF, 0x6FE3FF, 0.2f, 6, 0.3f, 0.3f);
        }
        if (d.shield <= 0 && amount > 0 && fx) {
            Vfx.flash(level, c.add(0, 1.6, 0), 0xFF4030, 1.4f, 5);
            Vfx.spray(level, FxStyle.SPARK, c.add(0, 1.4, 0), 0xFFE0A0, 0xFF6030, 0.18f, 10, 0.35f, 0.4f);
            if (amount > d.maxHp() * 0.03f) Vfx.shake(level, c, 0.35f, 12, 6);
        }
        if (d.shield > 0) {
            float absorbed = Math.min(d.shield, amount);
            d.shield -= absorbed;
            amount -= absorbed;
            level.sendParticles(Fx.dust(0x6FE3FF, 1.5f), c.x, c.y + 0.5, c.z, 10, 0.8, 0.8, 0.8, 0);
            level.playSound(null, d.corePos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1f, 0.6f);
        }
        if (amount > 0) {
            d.coreHp -= amount;
            level.playSound(null, d.corePos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 0.6f, 1.6f);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, c.x, c.y + 0.8, c.z, 3, 0.4, 0.3, 0.4, 0.1);
        }
        int pulse = d.level(CoreUpgrade.PULSE);
        if (pulse > 0 && d.pulseCooldown <= 0 && d.coreHp < d.maxHp() * 0.5f) {
            d.pulseCooldown = 1200;
            float dmg = 20 + 25 * pulse;
            Fx.shockwave(level, c.add(0, -0.4, 0), 6 + pulse * 2, 0x6FE3FF);
            Fx.sound(level, c, SoundEvents.WARDEN_SONIC_BOOM, 2f, 1.2f);
            for (LivingEntity e : SiegeDamage.enemiesAround(level, c, 6 + pulse * 2)) {
                SiegeDamage.deal(e, null, null, dmg, Element.ARCANE, false);
                SiegeDamage.push(e, c, 1.4, 0.5);
            }
        }
        d.setDirty();
        if (d.coreHp <= 0) defeat(level, d);
    }

    public static void healCore(ServerLevel level, float amount) {
        SiegeData d = data(level);
        if (!d.hasCore()) return;
        d.coreHp = Math.min(d.maxHp(), d.coreHp + amount);
        d.setDirty();
    }

    public static void defeat(ServerLevel level, SiegeData d) {
        BlockPos core = d.corePos;
        if (core == null) return;
        Vec3 c = Vec3.atCenterOf(core);
        int reached = d.wave;
        Vfx.sphere(level, c.add(0, 1.5, 0), 0x6FE3FF, 9f, 24);
        Vfx.pillar(level, Vec3.atBottomCenterOf(core), 0xFF4030, 2.2f, 30, 40);
        Vfx.spray(level, FxStyle.SHARD, c.add(0, 1.5, 0), 0xBFF4FF, 0x6FE3FF, 0.45f, 60, 0.8f, 0.4f);
        Vfx.screenFlash(level, c, 0xFF4030, 0.45f, 120, 30);
        Vfx.shake(level, c, 1.6f, 80, 30);
        Fx.explosion(level, c, 6);
        Fx.shockwave(level, c, 10, 0x8B0000);
        level.playSound(null, core, SoundEvents.WITHER_DEATH, SoundSource.BLOCKS, 2f, 0.6f);
        for (ServerPlayer p : playersNear(level, core, 256)) {
            title(p, Component.literal("THE CORE HAS FALLEN").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    Component.literal("You held until wave " + reached + ". The siege restarts from wave 1.").withStyle(ChatFormatting.GRAY));
        }
        clearEnemies(level, d);
        if (SiegeConfig.HARSH_DEFEAT.get()) {
            List<Entity> doomed = new ArrayList<>();
            for (Entity e : level.getAllEntities()) {
                if (e instanceof TurretEntity || e instanceof HeroEntity) doomed.add(e);
            }
            for (Entity e : doomed) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, e.getX(), e.getY() + 0.5, e.getZ(), 15, 0.4, 0.5, 0.4, 0.02);
                e.discard();
            }
            d.fallenHeroes.clear();
            Arrays.fill(d.upgrades, 0);
        }
        double keep = SiegeConfig.SALVAGE_FRACTION.get();
        d.credits.replaceAll((k, v) -> Math.max(SiegeConfig.STARTING_CREDITS.get(), (int) (v * keep)));
        d.bestWave = Math.max(d.bestWave, reached - 1);
        d.wave = 1;
        d.state = SiegeState.INACTIVE;
        d.modifier = NightModifier.NONE;
        d.corePos = null;
        d.queue.clear();
        d.tracked.clear();
        d.setDirty();
        level.setBlock(core, Blocks.AIR.defaultBlockState(), 3);
        ItemEntity drop = new ItemEntity(level, c.x, c.y, c.z, new ItemStack(ModItems.SIEGE_CORE.get()));
        drop.setGlowingTag(true);
        level.addFreshEntity(drop);
        syncHud(level, d);
    }

    private static void clearEnemies(ServerLevel level, SiegeData d) {
        for (UUID id : d.tracked) {
            Entity e = level.getEntity(id);
            if (e != null) e.discard();
        }
        if (d.corePos != null) {
            Vec3 c = Vec3.atCenterOf(d.corePos);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(160),
                    e -> e instanceof SiegeEnemy)) {
                level.sendParticles(ParticleTypes.SOUL, e.getX(), e.getY() + 1, e.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
                e.discard();
            }
        }
        d.tracked.clear();
        d.queue.clear();
    }

    // ------------------------------------------------------------------ credits & kills

    public static void onEnemyKilled(ServerLevel level, LivingEntity dead, @Nullable Entity killer) {
        if (!(dead instanceof SiegeEnemy se)) return;
        SiegeData d = data(level);
        boolean tracked = d.tracked.remove(dead.getUUID());
        if (tracked) d.waveKilled++;
        UUID credit = dead.getPersistentData().hasUUID(SiegeDamage.TAG_CREDIT) ? dead.getPersistentData().getUUID(SiegeDamage.TAG_CREDIT) : null;
        if (credit == null) credit = SiegeDamage.creditOwner(killer);
        Entity lastHitter = dead.getPersistentData().hasUUID(SiegeDamage.TAG_KILLER) ? level.getEntity(dead.getPersistentData().getUUID(SiegeDamage.TAG_KILLER)) : killer;
        if (lastHitter instanceof HeroEntity hero) hero.addXp(se.heroXp());
        if (lastHitter instanceof TurretEntity turret) turret.addKill();
        int value = se.creditValue();
        if (dead instanceof SiegeZombie z && z.isMinion()) value = Math.max(1, value / 3);
        if (credit != null) {
            award(level, d, credit, value, dead.position().add(0, dead.getBbHeight(), 0), true);
            d.kills.merge(credit, 1, Integer::sum);
        } else if (d.corePos != null) {
            // environmental kills (traps, storms) split among defenders present
            List<ServerPlayer> ps = playersNear(level, d.corePos, SiegeConfig.PLAYER_PRESENCE_RADIUS.get());
            for (ServerPlayer p : ps) award(level, d, p.getUUID(), value / (float) ps.size(), dead.position().add(0, dead.getBbHeight(), 0), true);
        }
        d.setDirty();
    }

    /** Apply all credit multipliers and pay the player. Returns the amount paid. */
    public static int award(ServerLevel level, SiegeData d, UUID player, float base, @Nullable Vec3 popupAt, boolean kill) {
        float mult = SiegeConfig.CREDIT_MULTIPLIER.get().floatValue() * (1f + 0.1f * d.level(CoreUpgrade.TREASURY));
        if (d.state == SiegeState.WAVE) mult *= d.modifier.creditMult;
        if (kill && popupAt != null && com.lastbastion.block.SupportBlock.harvesterNear(popupAt)) mult *= 1.25f;
        int amount = Math.max(1, Math.round(base * mult));
        d.addCredits(player, amount);
        if (popupAt != null && level.getPlayerByUUID(player) instanceof ServerPlayer sp) {
            Net.sendTo(sp, new CreditPopupPacket(popupAt, amount));
        }
        return amount;
    }

    public static int credits(ServerLevel level, UUID player) {
        return data(level).credits(player);
    }

    public static boolean spend(ServerLevel level, ServerPlayer player, int amount) {
        SiegeData d = data(level);
        if (player.isCreative()) return true;
        boolean ok = d.spend(player.getUUID(), amount);
        if (ok) syncHud(level, d);
        return ok;
    }

    public static void untrack(ServerLevel level, Entity e) {
        SiegeData d = data(level);
        if (d.tracked.remove(e.getUUID())) d.setDirty();
    }

    public static void track(ServerLevel level, Entity e) {
        SiegeData d = data(level);
        if (d.state == SiegeState.WAVE) {
            d.tracked.add(e.getUUID());
            d.setDirty();
        }
    }

    // ------------------------------------------------------------------ messaging

    private static long lastCoreFx;

    /** Visual/strength tier of the core, 1..5, from the total number of core upgrade levels bought. */
    public static int coreTier(SiegeData d) {
        int sum = 0;
        for (int u : d.upgrades) sum += u;
        return Math.min(5, 1 + sum / 4);
    }

    /** True while Turret Overdrive is running. */
    public static boolean overdrive(ServerLevel level) {
        return data(level).overdriveTicks > 0;
    }

    /** The core's passive defence: an arc of energy at the nearest enemy, stronger with each tier. */
    private static void sentinel(ServerLevel level, SiegeData d, BlockPos core) {
        int tier = coreTier(d);
        Vec3 top = Vec3.atBottomCenterOf(core).add(0, 2.4, 0);
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : SiegeDamage.enemiesAround(level, top, 6 + tier)) {
            double dd = e.distanceToSqr(top);
            if (dd < bestD) {
                bestD = dd;
                best = e;
            }
        }
        if (best == null) return;
        Vfx.arc(level, top, best.position().add(0, best.getBbHeight() * 0.5, 0), 0x6FE3FF, 0.06f, 6, 0.5f);
        Vfx.flash(level, best.position().add(0, best.getBbHeight() * 0.5, 0), 0x6FE3FF, 0.6f, 4);
        SiegeDamage.deal(best, null, null, 5f + 3f * tier, Element.ARCANE, false);
        level.playSound(null, core, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.6f);
    }

    /** Trigger a Core Protocol. Returns the feedback message. */
    public static Component useAbility(ServerLevel level, ServerPlayer p, CoreAbility a) {
        SiegeData d = data(level);
        BlockPos core = corePos(level);
        if (core == null || d.state == SiegeState.INACTIVE) return Component.literal("No active Siege Core.").withStyle(ChatFormatting.RED);
        Vec3 c = Vec3.atBottomCenterOf(core);
        if (p.position().distanceToSqr(c) > 48 * 48) return Component.literal("You must be within 48 blocks of the core.").withStyle(ChatFormatting.RED);
        if (!a.unlocked(d.upgrades)) {
            return Component.literal(a.title + " requires " + (a.requires == null ? "" : a.requires.title) + " " + a.requiredLevel + ".").withStyle(ChatFormatting.RED);
        }
        int cd = d.abilityCd[a.ordinal()];
        if (cd > 0) return Component.literal(a.title + " is recharging (" + (cd + 19) / 20 + "s).").withStyle(ChatFormatting.YELLOW);
        Vec3 top = c.add(0, 2.4, 0);
        int tier = coreTier(d);
        switch (a) {
            case NOVA -> {
                Vfx.sphere(level, top, 0x6FE3FF, 8f, 14);
                Vfx.ring(level, c.add(0, 0.1, 0), 0x6FE3FF, 8, 16, 1.6f);
                Vfx.flash(level, top, 0xFFFFFF, 3f, 8);
                Vfx.shake(level, c, 0.6f, 20, 10);
                level.playSound(null, core, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 2f, 1.3f);
                for (LivingEntity e : SiegeDamage.enemiesAround(level, c, 8)) {
                    float dmg = 40 + 15 * tier;
                    if (e instanceof SiegeBoss) dmg *= 0.5f;
                    SiegeDamage.deal(e, p, p, dmg, Element.ARCANE, false);
                    SiegeDamage.push(e, c, 1.8, 0.6);
                }
            }
            case AEGIS -> {
                d.shield = Math.min(d.maxShield(), d.shield + d.maxShield() * 0.5f);
                d.aegisTicks = 120;
                Vfx.dome(level, c, 0x8FD8FF, 3.4f, 24, null);
                Vfx.pillar(level, c, 0x8FD8FF, 1.6f, 10, 20);
                level.playSound(null, core, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2f, 1.2f);
            }
            case OVERDRIVE -> {
                d.overdriveTicks = 240;
                for (Entity e : level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(c, c).inflate(32), e -> e instanceof TurretEntity)) {
                    Vfx.arc(level, top, e.position().add(0, e.getBbHeight() * 0.7, 0), 0xFFB040, 0.07f, 10, 0.6f);
                    Vfx.spray(level, FxStyle.SPARK, e.position().add(0, e.getBbHeight() * 0.7, 0), 0xFFE0A0, 0xFFB040, 0.2f, 10, 0.3f, 0.6f);
                }
                level.playSound(null, core, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 2f, 1.5f);
            }
            case LANCE -> {
                List<LivingEntity> foes = new ArrayList<>(SiegeDamage.enemiesAround(level, c, 36));
                foes.sort((x, y) -> Float.compare(y.getHealth(), x.getHealth()));
                for (int i = 0; i < Math.min(6, foes.size()); i++) {
                    LivingEntity e = foes.get(i);
                    Vec3 at = e.position().add(0, e.getBbHeight() * 0.5, 0);
                    Vfx.beam(level, top, at, 0xFFE070, 0.35f, 14, 0.05f);
                    Vfx.flash(level, at, 0xFFF4C0, 1.6f, 8);
                    SiegeDamage.deal(e, p, p, (60 + 20 * tier) * (e instanceof SiegeBoss ? 0.6f : 1f), Element.HOLY, false);
                }
                Vfx.flash(level, top, 0xFFE070, 2.5f, 10);
                level.playSound(null, core, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 2f, 0.7f);
                level.playSound(null, core, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 1f, 1.6f);
            }
            case RALLY -> {
                for (LivingEntity e : SiegeDamage.defendersAround(level, c, 24)) {
                    if (e instanceof TurretEntity) continue;
                    e.heal(e.getMaxHealth() * 0.4f);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
                    e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
                    Vfx.spray(level, FxStyle.STAR, e.position().add(0, 1, 0), 0xFFE0E0, 0xFF6060, 0.25f, 8, 0.15f, 0.8f);
                }
                Vfx.ring(level, c.add(0, 0.1, 0), 0xFF6060, 24, 20, 1.2f);
                level.playSound(null, core, SoundEvents.RAID_HORN.value(), SoundSource.BLOCKS, 2f, 1.2f);
            }
        }
        d.abilityCd[a.ordinal()] = a.cooldownSeconds * 20;
        d.setDirty();
        syncHud(level, d);
        broadcast(level, d, Component.literal(p.getName().getString() + " activated " + a.title + "!").withStyle(s -> s.withColor(a.color)));
        return Component.literal(a.title + " activated.").withStyle(s -> s.withColor(a.color));
    }

    public static void syncHud(ServerLevel level, SiegeData d) {
        long t = level.getDayTime() % 24000L;
        int toNight = (int) (t < 13000 ? 13000 - t : 24000 - t + 13000);
        int remaining = d.tracked.size();
        for (WavePlanner.Group g : d.queue) remaining += g.units.size();
        d.waveTotal = Math.max(d.waveTotal, d.waveKilled + remaining);
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            boolean here = d.hasCore() && p.level() == level && d.corePos != null
                    && p.position().distanceToSqr(Vec3.atCenterOf(d.corePos)) < 400 * 400;
            Net.sendTo(p, new HudSyncPacket(here ? d.state.ordinal() : 0, d.wave, d.coreHp, d.maxHp(), d.shield, d.maxShield(),
                    remaining, d.waveTotal, d.modifier.ordinal(), d.credits(p.getUUID()), toNight,
                    here && d.corePos != null ? d.corePos : BlockPos.ZERO, d.bestWave, d.upgrades.clone(), cooldownSeconds(d), d.aegisTicks));
        }
    }

    private static int[] cooldownSeconds(SiegeData d) {
        int[] out = new int[d.abilityCd.length];
        for (int i = 0; i < out.length; i++) out[i] = (d.abilityCd[i] + 19) / 20;
        return out;
    }

    public static void broadcast(ServerLevel level, SiegeData d, Component msg) {
        if (d.corePos == null) return;
        for (ServerPlayer p : playersNear(level, d.corePos, 256)) p.sendSystemMessage(msg);
    }

    public static void title(ServerPlayer p, Component title, Component subtitle) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    public static void playAt(ServerLevel level, Vec3 at, SoundEvent s, float v, float p) {
        level.playSound(null, at.x, at.y, at.z, s, SoundSource.NEUTRAL, v, p);
    }
}
