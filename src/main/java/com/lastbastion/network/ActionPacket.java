package com.lastbastion.network;

import com.lastbastion.entity.hero.HeroEntity;
import com.lastbastion.entity.hero.HeroMode;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.siege.ShopCatalog;
import com.lastbastion.siege.SiegeData;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** All client -> server UI actions: shop purchases, turret console, hero orders, calling the next wave. */
public record ActionPacket(int action, int target, String id) {
    public static final int BUY = 0, TURRET_UPGRADE = 1, TURRET_SELL = 2, TURRET_REPAIR = 3, TURRET_MODE = 4,
            HERO_MODE = 5, HERO_DISMISS = 6, CALL_WAVE = 7, ALL_HEROES_MODE = 8, REFRESH_CORE = 9, CORE_ABILITY = 10, OPEN_RULES = 11,
            RECALL_HEROES = 12;

    private static int recall(ServerLevel level, ServerPlayer p, HeroEntity h) {
        net.minecraft.world.phys.Vec3 at = com.lastbastion.entity.hero.Companions.safeSpotNear(level, h, p.position());
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, h.getX(), h.getY() + 1, h.getZ(), 12, 0.4, 0.6, 0.4, 0.02);
        h.interruptSkill();
        h.teleportTo(at.x, at.y, at.z);
        h.getNavigation().stop();
        h.setMode(HeroMode.FOLLOW);
        com.lastbastion.fx.Vfx.spray(level, com.lastbastion.fx.FxStyle.STAR, at.add(0, 1, 0), 0xFFFFFF, h.kind.color, 0.25f, 12, 0.2f, 0);
        return 1;
    }

    public ActionPacket(FriendlyByteBuf b) {
        this(b.readVarInt(), b.readVarInt(), b.readUtf(64));
    }

    public void encode(FriendlyByteBuf b) {
        b.writeVarInt(action);
        b.writeVarInt(target);
        b.writeUtf(id, 64);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        ctx.get().setPacketHandled(true);
        if (p == null) return;
        ServerLevel level = p.serverLevel();
        switch (action) {
            case BUY -> {
                Component msg = ShopCatalog.purchase(p, id, target);
                p.displayClientMessage(msg, true);
                SiegeManager.syncHud(level, SiegeManager.data(level));
                if (id.equals("revive") && SiegeManager.corePos(level) != null) {
                    Net.sendTo(p, OpenScreenPacket.core(SiegeManager.corePos(level), fallenFor(p)));
                }
            }
            case REFRESH_CORE -> {
                if (SiegeManager.corePos(level) != null) Net.sendTo(p, OpenScreenPacket.core(SiegeManager.corePos(level), fallenFor(p)));
            }
            case TURRET_UPGRADE, TURRET_SELL, TURRET_REPAIR, TURRET_MODE -> {
                Entity e = level.getEntity(target);
                if (!(e instanceof TurretEntity t) || p.distanceToSqr(t) > 12 * 12) return;
                if (!t.canManage(p)) {
                    p.displayClientMessage(Component.literal("Only the turret's owner can do that.").withStyle(ChatFormatting.RED), true);
                    return;
                }
                switch (action) {
                    case TURRET_UPGRADE -> t.upgrade(p, id);
                    case TURRET_SELL -> t.sell(p);
                    case TURRET_REPAIR -> t.repair(p);
                    default -> t.cycleMode();
                }
                SiegeManager.syncHud(level, SiegeManager.data(level));
            }
            case HERO_MODE, HERO_DISMISS -> {
                Entity e = level.getEntity(target);
                if (!(e instanceof HeroEntity h) || !p.getUUID().equals(h.getOwnerId())) return;
                if (action == HERO_DISMISS) {
                    com.lastbastion.entity.hero.Companions.returnSigil(level, h, false);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, h.getX(), h.getY() + 1, h.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
                    h.discard();
                    p.displayClientMessage(Component.literal(h.kind.shortName() + " returns to " + (h.kind.devil ? "the pact." : "the sigil.")), true);
                } else {
                    try {
                        h.setMode(HeroMode.valueOf(id));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            case RECALL_HEROES -> {
                int n = 0;
                List<HeroEntity> mine = new ArrayList<>(com.lastbastion.entity.hero.Companions.owned(level, p.getUUID(), false));
                mine.addAll(com.lastbastion.entity.hero.Companions.owned(level, p.getUUID(), true));
                for (HeroEntity h : mine) n += recall(level, p, h);
                p.displayClientMessage(Component.literal(n == 0 ? "No summons in this dimension." : n + " summons recalled to your side.").withStyle(ChatFormatting.AQUA), true);
            }
            case ALL_HEROES_MODE -> {
                HeroMode m;
                try {
                    m = HeroMode.valueOf(id);
                } catch (IllegalArgumentException ex) {
                    return;
                }
                int n = 0;
                for (Entity e : level.getAllEntities()) {
                    if (e instanceof HeroEntity h && p.getUUID().equals(h.getOwnerId()) && !(h instanceof com.lastbastion.entity.hero.KageCloneEntity)) {
                        h.setMode(m);
                        n++;
                    }
                }
                p.displayClientMessage(Component.literal(n + " summons ordered to: " + m.title).withStyle(ChatFormatting.AQUA), true);
            }
            case CALL_WAVE -> SiegeManager.callWaveEarly(level, p);
            case OPEN_RULES -> SiegeManager.openRules(p);
            case CORE_ABILITY -> {
                com.lastbastion.siege.CoreAbility a = com.lastbastion.siege.CoreAbility.byName(id);
                if (a != null) p.displayClientMessage(SiegeManager.useAbility(level, p, a), true);
            }
            default -> {
            }
        }
    }

    /** Kept for the screen packet; recovering sigils are now read from the client's own inventory. */
    public static int[] fallenFor(ServerPlayer p) {
        com.lastbastion.entity.hero.Companions.deliverMail(p);
        return new int[0];
    }
}
