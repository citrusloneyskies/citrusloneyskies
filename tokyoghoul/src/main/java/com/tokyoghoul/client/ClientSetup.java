package com.tokyoghoul.client;

import com.tokyoghoul.TokyoGhoul;
import com.tokyoghoul.client.render.CharacterRenderer;
import com.tokyoghoul.client.render.FxRenderers;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Characters;
import com.tokyoghoul.registry.TGEntities;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/** Client registration (mod bus) and per-frame hooks (Forge bus). */
public final class ClientSetup {
    private static EntityRendererProvider.Context context;
    private static final Map<String, CharacterRenderer<AbstractClientPlayer>> PLAYER_RENDERERS = new HashMap<>();

    private ClientSetup() {
    }

    @Mod.EventBusSubscriber(modid = TokyoGhoul.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            for (CharacterDef d : Characters.all()) {
                e.registerEntityRenderer(TGEntities.typeOf(d.id), ctx -> new CharacterRenderer<>(ctx, d));
            }
            e.registerEntityRenderer(TGEntities.RC_PROJECTILE.get(), FxRenderers.Projectile::new);
            e.registerEntityRenderer(TGEntities.GROUND_SPIKE.get(), FxRenderers.Spike::new);
        }

        @SubscribeEvent
        public static void layers(EntityRenderersEvent.AddLayers e) {
            context = e.getContext();
            PLAYER_RENDERERS.clear();
        }

        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            KeyBindings.register(e);
        }

        @SubscribeEvent
        public static void overlays(RegisterGuiOverlaysEvent e) {
            e.registerAboveAll("fighter_hud", Hud.INSTANCE);
        }
    }

    @Mod.EventBusSubscriber(modid = TokyoGhoul.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {
        }

        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent e) {
            if (e.phase == TickEvent.Phase.END) KeyBindings.tick();
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
            ClientState.clear();
        }

        /** A player who picked a character is drawn as that character (with kagune, kakugan and quinque). */
        @SubscribeEvent
        public static void renderPlayer(RenderPlayerEvent.Pre e) {
            if (context == null || !(e.getEntity() instanceof AbstractClientPlayer p)) return;
            CharacterDef def = ClientState.character(p);
            if (def == null) return;
            CharacterRenderer<AbstractClientPlayer> r = PLAYER_RENDERERS.computeIfAbsent(def.id, id -> new CharacterRenderer<>(context, def));
            e.setCanceled(true);
            float yaw = Mth.lerp(e.getPartialTick(), p.yRotO, p.getYRot());
            int depth = com.tokyoghoul.util.Safe.depth(e.getPoseStack());
            try {
                r.render(p, yaw, e.getPartialTick(), e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight());
            } catch (Exception | LinkageError err) {
                com.tokyoghoul.util.Safe.report("renderPlayer", err);
                com.tokyoghoul.util.Safe.restore(e.getPoseStack(), depth);
            }
        }
    }
}
