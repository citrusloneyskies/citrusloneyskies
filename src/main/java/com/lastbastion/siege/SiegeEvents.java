package com.lastbastion.siege;

import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.misc.TitanExoEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Forge event wiring for the siege. */
public final class SiegeEvents {
    private SiegeEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.level instanceof ServerLevel sl) SiegeManager.tick(sl);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent e) {
        if (e.isCanceled()) return;
        if (e.getEntity() instanceof SiegeEnemy && e.getEntity().level() instanceof ServerLevel sl
                && e.getEntity().getPersistentData().contains("lb_plague")
                && sl.getEntity(e.getEntity().getPersistentData().getInt("lb_plague")) instanceof com.lastbastion.entity.turret.TurretEntity t && t.isAlive()) {
            t.spreadPlague(sl, e.getEntity());
        }
        if (e.getEntity() instanceof SiegeEnemy && e.getEntity().level() instanceof ServerLevel sl) {
            SiegeManager.onEnemyKilled(sl, e.getEntity(), e.getSource().getEntity());
        }
    }

    /** The Titan Exosuit takes the hits for its pilot. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (e.getEntity() instanceof Player p && p.getVehicle() instanceof TitanExoEntity exo) {
            e.setCanceled(true);
            exo.hurt(e.getSource(), e.getAmount());
        }
    }

    /** No sleeping through the siege: nightfall is when the horde comes. */
    @SubscribeEvent
    public static void onSleep(net.minecraftforge.event.entity.player.PlayerSleepInBedEvent e) {
        if (!(e.getEntity().level() instanceof ServerLevel sl)) return;
        net.minecraft.core.BlockPos core = SiegeManager.corePos(sl);
        if (core != null && core.closerToCenterThan(e.getEntity().position(), 256)) {
            e.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
            e.getEntity().displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "You cannot rest while the Siege Core stands - the horde comes at nightfall.")
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        SiegeCommands.register(e.getDispatcher());
    }
}
