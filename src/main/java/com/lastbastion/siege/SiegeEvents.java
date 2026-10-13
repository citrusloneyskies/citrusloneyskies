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

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        SiegeCommands.register(e.getDispatcher());
    }
}
