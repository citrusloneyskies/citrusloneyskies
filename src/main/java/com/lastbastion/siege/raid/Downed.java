package com.lastbastion.siege.raid;

import com.lastbastion.LastBastion;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.network.DownedPacket;
import com.lastbastion.network.Net;
import com.lastbastion.siege.SiegeData;
import com.lastbastion.siege.SiegeManager;
import com.lastbastion.siege.SiegeRules;
import com.lastbastion.siege.SiegeState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Co-op downed state. During a wave, a player who would die while a teammate is still standing nearby is
 * downed instead: they crawl, can't fight, and bleed out unless a teammate crouches beside them for three
 * seconds. Zombies lose interest in downed players, but stray blows still hurry the bleed-out. Everyone
 * downed is picked back up when the wave ends; if the whole party is down, the bleed-out runs its course.
 */
@Mod.EventBusSubscriber(modid = LastBastion.MODID)
public final class Downed {
    public static final int REVIVE_TICKS = 60;
    private static final UUID CRAWL_ID = UUID.fromString("6f2f3b8e-3e9d-4c1a-9a51-2d7c0b0c4e11");

    static final class State {
        int bleed, maxBleed, revive, hitCooldown, alone;
        /** Did the horde put this player down? Only then does picking them up pay a bounty. */
        boolean bounty;
        @Nullable
        UUID reviver;
    }

    private static final Map<UUID, State> DOWNED = new HashMap<>();
    /** Players whose rescue already paid out this wave: one bounty per downed player per wave, so it can't be farmed. */
    private static final Set<UUID> PAID = new HashSet<>();
    /** Set while we kill a player on purpose, so our own death hook lets it through. */
    private static boolean executing;

    private Downed() {
    }

    public static boolean isDowned(Player p) {
        return DOWNED.containsKey(p.getUUID());
    }

    // ------------------------------------------------------------------ going down

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent e) {
        if (executing || !(e.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        if (e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // the void and /kill are final
            clear(p, false);
            return;
        }
        State s = DOWNED.get(p.getUUID());
        if (s != null) {
            // a fatal blow on someone already down only hurries the bleed-out
            e.setCanceled(true);
            p.setHealth(1f);
            s.bleed -= 60;
            return;
        }
        if (!eligible(p)) return;
        e.setCanceled(true);
        down(p, byHorde(e.getSource()));
    }

    /** A blow from the horde (or its projectiles), as opposed to a fall, a teammate's stray shot or a lava dip. */
    private static boolean byHorde(net.minecraft.world.damagesource.DamageSource src) {
        return com.lastbastion.combat.SiegeDamage.isEnemy(src.getEntity()) || com.lastbastion.combat.SiegeDamage.isEnemy(src.getDirectEntity());
    }

    private static boolean eligible(ServerPlayer p) {
        if (p.isCreative() || p.isSpectator()) return false;
        ServerLevel level = p.serverLevel();
        SiegeData d = SiegeManager.data(level);
        if (d.state != SiegeState.WAVE || !d.rules.on(SiegeRules.DOWNED) || SiegeManager.corePos(level) == null) return false;
        if (!SiegeManager.withinSiege(level, p)) return false;
        return standingTeammates(level, p) > 0;
    }

    /** Teammates within the siege who are alive and not downed. */
    private static int standingTeammates(ServerLevel level, ServerPlayer p) {
        int n = 0;
        for (ServerPlayer o : level.players()) {
            if (o == p || o.isSpectator() || !o.isAlive() || isDowned(o)) continue;
            if (SiegeManager.withinSiege(level, o)) n++;
        }
        return n;
    }

    /** Down a player (public for tests and commands). */
    public static void down(ServerPlayer p) {
        down(p, true);
    }

    private static void down(ServerPlayer p, boolean bounty) {
        ServerLevel level = p.serverLevel();
        SiegeData d = SiegeManager.data(level);
        State s = new State();
        s.bounty = bounty;
        s.maxBleed = s.bleed = d.rules.i(SiegeRules.BLEED_OUT) * 20;
        DOWNED.put(p.getUUID(), s);
        p.setHealth(1f);
        p.clearFire();
        p.stopUsingItem();
        p.setForcedPose(Pose.SWIMMING);
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(CRAWL_ID) == null) {
            speed.addTransientModifier(new AttributeModifier(CRAWL_ID, "Downed", -0.6, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
        for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(40), m -> m.getTarget() == p)) m.setTarget(null);
        Vec3 at = p.position();
        Vfx.ring(level, at.add(0, 0.05, 0), 0xFF3030, 2.4f, 16, 0.4f);
        Vfx.spray(level, FxStyle.DROP, at.add(0, 0.6, 0), 0xC01010, 0x5A0808, 0.15f, 14, 0.2f, 0.4f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.2f, 0.6f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1f, 0.5f);
        SiegeManager.title(p, Component.literal("DOWNED").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                Component.literal("Hold on - a teammate can crouch beside you to revive you.").withStyle(ChatFormatting.GRAY));
        SiegeManager.broadcast(level, d, Component.literal("✖ " + p.getGameProfile().getName() + " is down! ").withStyle(ChatFormatting.RED)
                .append(Component.literal("Crouch beside them for 3 seconds to revive.").withStyle(ChatFormatting.GRAY)));
        sync(p, s);
    }

    // ------------------------------------------------------------------ per tick

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        State s = DOWNED.get(p.getUUID());
        if (s == null) return;
        ServerLevel level = p.serverLevel();
        SiegeData d = SiegeManager.data(level);
        if (d.state != SiegeState.WAVE || !p.isAlive()) {
            revive(p, null);
            return;
        }
        if (p.getHealth() < 1f) p.setHealth(1f);
        if (p.getForcedPose() != Pose.SWIMMING) p.setForcedPose(Pose.SWIMMING);
        if (s.hitCooldown > 0) s.hitCooldown--;
        s.bleed--;
        // the reviver: the nearest crouching, standing teammate within reach
        ServerPlayer helper = null;
        double best = 2.4 * 2.4;
        for (ServerPlayer o : level.players()) {
            if (o == p || !o.isAlive() || o.isSpectator() || isDowned(o) || !o.isShiftKeyDown()) continue;
            double dd = o.distanceToSqr(p);
            if (dd < best) {
                best = dd;
                helper = o;
            }
        }
        if (helper != null) {
            if (s.reviver == null || !s.reviver.equals(helper.getUUID())) s.revive = 0;
            s.reviver = helper.getUUID();
            s.revive++;
            if (s.revive % 10 == 0) {
                Vfx.spray(level, FxStyle.STAR, p.position().add(0, 0.5, 0), 0xFFFFFF, 0xFFE070, 0.15f, 4, 0.08f, 0.6f);
                helper.displayClientMessage(Component.literal("Reviving " + p.getGameProfile().getName() + "  " + bar(s.revive, REVIVE_TICKS))
                        .withStyle(ChatFormatting.GOLD), true);
            }
            if (s.revive >= REVIVE_TICKS) {
                revive(p, helper);
                return;
            }
        } else {
            s.revive = Math.max(0, s.revive - 2);
            if (s.revive == 0) s.reviver = null;
        }
        // nobody left standing: the party is wiped, the bleed-out finishes quickly
        if (p.tickCount % 20 == 0) {
            s.alone = standingTeammates(level, p) == 0 ? s.alone + 1 : 0;
            if (s.alone >= 3) s.bleed = Math.min(s.bleed, 0);
            Vfx.pillar(level, p.position(), 0xFF3030, 0.15f, 6, 22);
            p.displayClientMessage(Component.literal("DOWNED - bleeding out in " + Math.max(0, (s.bleed + 19) / 20) + "s"
                    + (s.revive > 0 ? "   being revived " + bar(s.revive, REVIVE_TICKS) : "")).withStyle(ChatFormatting.RED), true);
        }
        if (p.tickCount % 5 == 0) sync(p, s);
        if (s.bleed <= 0) bleedOut(p);
    }

    private static String bar(int v, int max) {
        int n = Math.round(10f * v / max);
        return "▐" + "█".repeat(Math.max(0, n)) + "░".repeat(Math.max(0, 10 - n)) + "▌";
    }

    // ------------------------------------------------------------------ getting up / bleeding out

    /** Stand a downed player back up ({@code by} = the reviver, or null at wave end). */
    public static void revive(ServerPlayer p, @Nullable ServerPlayer by) {
        State was = DOWNED.get(p.getUUID());
        if (!clear(p, true)) return;
        ServerLevel level = p.serverLevel();
        p.setHealth(Math.max(p.getHealth(), p.getMaxHealth() * (by != null ? 0.35f : 0.5f)));
        p.invulnerableTime = 40;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        Vec3 at = p.position();
        Vfx.pillar(level, at, 0xFFE070, 0.8f, 8, 24);
        Vfx.spray(level, FxStyle.STAR, at.add(0, 1, 0), 0xFFFFFF, 0xFFE070, 0.25f, 18, 0.25f, 0.8f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7f, 1.3f);
        if (by != null) {
            com.lastbastion.siege.Milestones.award(by, com.lastbastion.siege.Milestones.REVIVE);
            SiegeData d = SiegeManager.data(level);
            int paid = was.bounty && PAID.add(p.getUUID()) ? SiegeManager.award(level, d, by.getUUID(), 20 + 2 * d.wave, by.position().add(0, 2, 0), false) : 0;
            SiegeManager.broadcast(level, d, Component.literal("✚ " + by.getGameProfile().getName() + " revived " + p.getGameProfile().getName()
                    + (paid > 0 ? "! (+" + paid + " credits)" : "!")).withStyle(ChatFormatting.GREEN));
            by.displayClientMessage(Component.literal("Revived " + p.getGameProfile().getName() + "!").withStyle(ChatFormatting.GREEN), true);
        }
    }

    private static void bleedOut(ServerPlayer p) {
        if (!clear(p, true)) return;
        SiegeData d = SiegeManager.data(p.serverLevel());
        SiegeManager.broadcast(p.serverLevel(), d, Component.literal("☠ " + p.getGameProfile().getName() + " bled out.").withStyle(ChatFormatting.DARK_RED));
        executing = true;
        try {
            p.kill();
        } finally {
            executing = false;
        }
    }

    /** Leave the downed state without dying. Returns false if the player wasn't downed. */
    private static boolean clear(ServerPlayer p, boolean notify) {
        State s = DOWNED.remove(p.getUUID());
        if (s == null) return false;
        p.setForcedPose(null);
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(CRAWL_ID);
        if (notify) Net.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), new DownedPacket(p.getId(), false, 0, 1, 0));
        return true;
    }

    /** Wave over (or the core fell): everyone gets back up, and next wave's rescues pay again. */
    public static void reviveAll(ServerLevel level) {
        for (ServerPlayer p : new ArrayList<>(level.players())) {
            if (isDowned(p)) revive(p, null);
            PAID.remove(p.getUUID());
        }
    }

    private static void sync(ServerPlayer p, State s) {
        Net.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), new DownedPacket(p.getId(), true, s.bleed, s.maxBleed, s.revive));
    }

    /** HUD objectives for downed teammates. */
    static void objectives(ServerLevel level, List<Integer> out) {
        for (ServerPlayer p : level.players()) {
            State s = DOWNED.get(p.getUUID());
            if (s != null) RaidEvents.add(out, RaidEvents.OBJ_DOWNED, p, Math.max(0, (s.bleed + 19) / 20));
        }
    }

    // ------------------------------------------------------------------ restrictions while downed

    /** Downed players take no direct damage; hits only shorten the bleed-out. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttacked(LivingAttackEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            State s = DOWNED.get(p.getUUID());
            if (s != null && !e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                e.setCanceled(true);
                if (s.hitCooldown <= 0 && e.getSource().getEntity() != null) {
                    s.hitCooldown = 10;
                    s.bleed -= 40;
                }
            }
        }
        if (e.getSource().getEntity() instanceof ServerPlayer attacker && isDowned(attacker)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent e) {
        if (e.getNewTarget() instanceof Player p && !p.level().isClientSide && isDowned(p)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        if (isDowned(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem e) {
        if (!e.getLevel().isClientSide && isDowned(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!e.getLevel().isClientSide && isDowned(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUseEntity(PlayerInteractEvent.EntityInteract e) {
        if (!e.getLevel().isClientSide && isDowned(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (isDowned(e.getPlayer())) e.setCanceled(true);
    }

    /** Disconnecting or leaving the dimension stands the player back up rather than killing them. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) clear(p, true);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) clear(p, true);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) clear(p, true);
    }

    /** Server stopping: forget everything (the crawl modifier is transient anyway). */
    public static void reset() {
        DOWNED.clear();
        PAID.clear();
    }

    /** Players currently downed (for tests). */
    public static Set<UUID> downedIds() {
        return Collections.unmodifiableSet(DOWNED.keySet());
    }
}
