package com.lastbastion.entity.hero;

import com.lastbastion.LastBastion;
import com.lastbastion.combat.SiegeAlly;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModItems;
import com.lastbastion.siege.SiegeData;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

/**
 * Companion bookkeeping for heroes and devils, which no longer need a Siege Core: who each player last struck and
 * who last struck them (so summons fight their summoner's fights, whatever weapon or turret did the damage), sigils
 * returned from fallen or dismissed summons, the per-player summon limits, and summons following their summoner
 * through portals.
 */
@Mod.EventBusSubscriber(modid = LastBastion.MODID)
public final class Companions {
    /** Ticks a fallen summon needs before its sigil can be used again. */
    public static final String TAG_RECOVER = "lbRecover", TAG_RECOVER_TOTAL = "lbRecoverTotal";

    private record Mark(int entity, long time) {
    }

    private static final Map<UUID, Mark> STRUCK = new HashMap<>();
    private static final Map<UUID, Mark> STRUCK_BY = new HashMap<>();

    private Companions() {
    }

    // ------------------------------------------------------------------ who is fighting whom

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttack(LivingAttackEvent e) {
        if (e.isCanceled() || !(e.getEntity().level() instanceof ServerLevel level)) return;
        Entity src = e.getSource().getEntity();
        long now = level.getGameTime();
        if (src instanceof ServerPlayer p && !(p instanceof FakePlayer) && e.getEntity() != p) STRUCK.put(p.getUUID(), new Mark(e.getEntity().getId(), now));
        if (e.getEntity() instanceof ServerPlayer p && !(p instanceof FakePlayer) && src instanceof LivingEntity && src != p) {
            STRUCK_BY.put(p.getUUID(), new Mark(src.getId(), now));
        }
    }

    /** The mob {@code owner} hit within the last {@code ticks}, if it's still around. */
    @Nullable
    public static LivingEntity struck(ServerLevel level, Player owner, int ticks) {
        return lookup(level, STRUCK.get(owner.getUUID()), ticks);
    }

    /** The mob that hit {@code owner} within the last {@code ticks}. */
    @Nullable
    public static LivingEntity struckBy(ServerLevel level, Player owner, int ticks) {
        return lookup(level, STRUCK_BY.get(owner.getUUID()), ticks);
    }

    @Nullable
    private static LivingEntity lookup(ServerLevel level, @Nullable Mark m, int ticks) {
        if (m == null || level.getGameTime() - m.time() > ticks) return null;
        return level.getEntity(m.entity()) instanceof LivingEntity le && le.isAlive() ? le : null;
    }

    /** May a summon of {@code owner} attack {@code e} on its owner's say-so? */
    public static boolean fairGame(LivingEntity e, @Nullable Player owner) {
        if (!e.isAlive() || e instanceof Player || e instanceof SiegeAlly || e instanceof ArmorStand) return false;
        if (owner != null && e instanceof TamableAnimal t && owner.getUUID().equals(t.getOwnerUUID())) return false;
        return owner == null || e != owner.getVehicle();
    }

    // ------------------------------------------------------------------ sigils

    /** Sigil item for a hero or devil kind. */
    public static ItemStack sigil(HeroKind k) {
        return new ItemStack(ModItems.sigil(k));
    }

    public static int recoveryTicks(HeroKind k) {
        return k.devil ? 20 * 60 * 10 : 20 * (60 + k.cost / 40);
    }

    /** Ticks until this sigil can be used again (0 = ready). */
    public static long recoveryLeft(ItemStack s, long now) {
        if (!s.hasTag()) return 0;
        return Math.max(0, s.getTag().getLong(TAG_RECOVER) - now);
    }

    public static float recoveryFraction(ItemStack s, long now) {
        long left = recoveryLeft(s, now);
        if (left <= 0) return 1f;
        int total = Math.max(1, s.getTag().getInt(TAG_RECOVER_TOTAL));
        return 1f - Math.min(1f, left / (float) total);
    }

    /** A sigil carrying this summon's level and experience, optionally recovering from a defeat. */
    public static ItemStack sigilFor(HeroKind k, int level, int xp, long recoverUntil, int recoverTotal) {
        ItemStack s = sigil(k);
        s.getOrCreateTag().putInt("lbLevel", Math.max(1, level));
        s.getOrCreateTag().putInt("lbXp", xp);
        if (recoverUntil > 0) {
            s.getOrCreateTag().putLong(TAG_RECOVER, recoverUntil);
            s.getOrCreateTag().putInt(TAG_RECOVER_TOTAL, recoverTotal);
        }
        return s;
    }

    /**
     * Send a summon back to its sigil: into the owner's inventory if they're online, otherwise into the world's
     * mailbox (delivered at their next login). {@code fallen} starts the recovery timer.
     */
    public static void returnSigil(ServerLevel level, HeroEntity h, boolean fallen) {
        UUID owner = h.getOwnerId();
        if (owner == null) return;
        int total = fallen ? recoveryTicks(h.kind) : 0;
        long until = fallen ? level.getGameTime() + total : 0;
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(owner);
        if (p != null) {
            ItemStack s = sigilFor(h.kind, h.heroLevel(), h.xp(), until, total);
            if (!p.getInventory().add(s)) p.drop(s, false);
        } else {
            SiegeData d = SiegeManager.data(level);
            d.fallenHeroes.add(new SiegeData.FallenHero(owner, h.kind.ordinal(), h.heroLevel(), h.xp()));
            d.setDirty();
        }
    }

    /** Hand over sigils waiting in the mailbox (fallen while offline, or from older saves). */
    public static void deliverMail(ServerPlayer p) {
        SiegeData d = SiegeManager.data(p.serverLevel());
        boolean any = false;
        for (Iterator<SiegeData.FallenHero> it = d.fallenHeroes.iterator(); it.hasNext(); ) {
            SiegeData.FallenHero f = it.next();
            if (!f.owner().equals(p.getUUID()) || f.heroKind() < 0 || f.heroKind() >= HeroKind.values().length) continue;
            HeroKind k = HeroKind.values()[f.heroKind()];
            int total = recoveryTicks(k);
            ItemStack s = sigilFor(k, f.level(), f.xp(), p.serverLevel().getGameTime() + total, total);
            if (!p.getInventory().add(s)) p.drop(s, false);
            it.remove();
            any = true;
        }
        if (any) {
            d.setDirty();
            p.sendSystemMessage(Component.literal("Your fallen summons' sigils have returned to you. They are recovering.").withStyle(ChatFormatting.GOLD));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) deliverMail(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        STRUCK.remove(e.getEntity().getUUID());
        STRUCK_BY.remove(e.getEntity().getUUID());
    }

    // ------------------------------------------------------------------ limits

    /** Heroes (not devils, not Kage's clones) this player currently has in the level. */
    public static List<HeroEntity> owned(ServerLevel level, UUID owner, boolean devils) {
        List<HeroEntity> out = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof HeroEntity h && h.isAlive() && !(h instanceof KageCloneEntity) && owner.equals(h.getOwnerId()) && h.kind.devil == devils) out.add(h);
        }
        return out;
    }

    /** Like {@link #owned} but across every dimension (summon limits can't be dodged through a portal). */
    public static List<HeroEntity> ownedAnywhere(net.minecraft.server.MinecraftServer server, UUID owner, boolean devils) {
        List<HeroEntity> out = new ArrayList<>();
        for (ServerLevel l : server.getAllLevels()) out.addAll(owned(l, owner, devils));
        return out;
    }

    // ------------------------------------------------------------------ travelling with the owner

    /** Summons on Follow orders step through the portal right behind their summoner. */
    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ServerLevel from = p.server.getLevel(e.getFrom()), to = p.server.getLevel(e.getTo());
        if (from == null || to == null) return;
        List<HeroEntity> followers = new ArrayList<>();
        for (Entity en : from.getAllEntities()) {
            if (en instanceof HeroEntity h && h.isAlive() && !(h instanceof KageCloneEntity) && p.getUUID().equals(h.getOwnerId()) && h.mode() == HeroMode.FOLLOW) {
                followers.add(h);
            }
        }
        for (HeroEntity h : followers) {
            Vec3 at = safeSpotNear(to, h, p.position());
            Entity moved = h.changeDimension(to, new ITeleporter() {
                @Override
                public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
                    return repositionEntity.apply(false);
                }

                @Override
                public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                    return new PortalInfo(at, Vec3.ZERO, entity.getYRot(), entity.getXRot());
                }

                @Override
                public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceWorld, ServerLevel destWorld) {
                    return false;
                }
            });
            if (moved != null) Vfx.spray(to, FxStyle.STAR, at.add(0, 1, 0), 0xFFFFFF, h.kind.color, 0.25f, 12, 0.2f, 0);
        }
    }

    /** A spot near {@code around} where {@code e} fits without suffocating (falls back to {@code around}). */
    public static Vec3 safeSpotNear(ServerLevel level, Entity e, Vec3 around) {
        double[][] offsets = {{1.5, 0}, {-1.5, 0}, {0, 1.5}, {0, -1.5}, {2.5, 2.5}, {-2.5, 2.5}, {2.5, -2.5}, {-2.5, -2.5}, {0, 0}};
        for (double dy : new double[]{0, 1, -1, 2}) {
            for (double[] o : offsets) {
                Vec3 p = around.add(o[0], dy, o[1]);
                AABB box = e.getType().getDimensions().makeBoundingBox(p);
                if (level.noCollision(e, box) && !level.getBlockState(net.minecraft.core.BlockPos.containing(p).below()).isAir()) return p;
            }
        }
        return around;
    }

    /** Server stopping: drop the combat marks. */
    public static void reset() {
        STRUCK.clear();
        STRUCK_BY.clear();
    }
}
