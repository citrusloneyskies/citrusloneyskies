package com.lastbastion.client;

import com.lastbastion.LastBastion;
import com.lastbastion.client.model.BossModels;
import com.lastbastion.client.model.HeroModels;
import com.lastbastion.client.model.MiscModels;
import com.lastbastion.client.model.TurretModels;
import com.lastbastion.client.model.ZombieModels;
import com.lastbastion.client.render.CoreCrystalRenderer;
import com.lastbastion.entity.boss.BossKind;
import com.lastbastion.entity.hero.HeroKind;
import com.lastbastion.entity.turret.TurretKind;
import com.lastbastion.entity.zombie.ZombieKind;
import com.lastbastion.registry.ModBlockEntities;
import com.lastbastion.registry.ModEntities;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = LastBastion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
    public static final KeyMapping HERO_COMMAND = new KeyMapping("key.lastbastion.hero_command", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, "key.categories.lastbastion");

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        for (ZombieKind k : ZombieKind.values()) e.registerEntityRenderer(ModEntities.ZOMBIES.get(k).get(), ctx -> ZombieModels.renderer(ctx, k));
        for (BossKind k : BossKind.values()) e.registerEntityRenderer(ModEntities.BOSSES.get(k).get(), ctx -> BossModels.renderer(ctx, k));
        for (TurretKind k : TurretKind.values()) e.registerEntityRenderer(ModEntities.TURRETS.get(k).get(), ctx -> TurretModels.renderer(ctx, k));
        for (HeroKind k : HeroKind.values()) e.registerEntityRenderer(ModEntities.HEROES.get(k).get(), ctx -> HeroModels.renderer(ctx, k));
        e.registerEntityRenderer(ModEntities.KAGE_CLONE.get(), HeroModels::cloneRenderer);
        e.registerEntityRenderer(ModEntities.PHYLACTERY.get(), BossModels::crystalRenderer);
        e.registerEntityRenderer(ModEntities.PROJECTILE.get(), MiscModels::projectileRenderer);
        e.registerEntityRenderer(ModEntities.HAZARD.get(), NoopRenderer::new);
        e.registerEntityRenderer(ModEntities.DROP_POD.get(), MiscModels::podRenderer);
        e.registerEntityRenderer(ModEntities.TITAN_EXO.get(), MiscModels::exoRenderer);
        e.registerEntityRenderer(ModEntities.DRONE.get(), MiscModels::droneRenderer);
        e.registerBlockEntityRenderer(ModBlockEntities.SIEGE_CORE.get(), CoreCrystalRenderer::new);
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("siege_hud", SiegeHud.INSTANCE);
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
        e.register(HERO_COMMAND);
    }
}
