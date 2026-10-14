package com.lastbastion.client;

import com.lastbastion.LastBastion;
import com.lastbastion.client.screen.HeroCommandScreen;
import com.lastbastion.entity.misc.TitanExoEntity;
import com.lastbastion.network.ExoInputPacket;
import com.lastbastion.network.Net;
import com.lastbastion.siege.NightModifier;
import com.lastbastion.siege.SiegeState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** Client-side gameplay hooks: exosuit controls, credit popups, siege fog and the hero command key. */
@Mod.EventBusSubscriber(modid = LastBastion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {
    private static boolean lastFire, lastMissile, lastBoost;

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.isPaused()) com.lastbastion.client.fx.VfxManager.tick();
        ClientSiegeState.tick();
        while (ClientSetup.HERO_COMMAND.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new HeroCommandScreen());
        }
        while (ClientSetup.PROTOCOLS.consumeClick()) {
            if (mc.screen == null && ClientSiegeState.active()) mc.setScreen(new com.lastbastion.client.screen.ProtocolScreen());
        }
        if (mc.player.getVehicle() instanceof TitanExoEntity exo) {
            boolean fire = mc.options.keyAttack.isDown() && mc.screen == null;
            boolean missile = mc.options.keyUse.isDown() && mc.screen == null;
            boolean boost = mc.options.keyJump.isDown() && mc.screen == null;
            exo.boostInput = boost;
            if (fire != lastFire || missile != lastMissile || boost != lastBoost || mc.player.tickCount % 20 == 0) {
                Net.toServer(new ExoInputPacket(fire, missile, boost));
                lastFire = fire;
                lastMissile = missile;
                lastBoost = boost;
            }
        } else if (lastFire || lastMissile || lastBoost) {
            lastFire = lastMissile = lastBoost = false;
        }
    }

    /** While piloting the exosuit, attack/use drive the weapons instead of punching. */
    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof TitanExoEntity) {
            e.setCanceled(true);
            e.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        com.lastbastion.client.fx.VfxManager.render(e);
        renderSpeech(e);
        if (ClientSiegeState.POPUPS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Camera cam = e.getCamera();
        Vec3 cp = cam.getPosition();
        PoseStack pose = e.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        Font font = mc.font;
        float pt = e.getPartialTick();
        for (ClientSiegeState.Popup p : ClientSiegeState.POPUPS) {
            float age = p.age + pt;
            float alpha = Math.max(0, 1f - age / 30f);
            pose.pushPose();
            pose.translate(p.pos.x - cp.x, p.pos.y - cp.y + 0.3 + age * 0.03, p.pos.z - cp.z);
            pose.mulPose(cam.rotation());
            float s = 0.03f * (1f + Math.min(1, age / 4f) * 0.3f);
            pose.scale(-s, -s, s);
            String text = "+" + p.amount;
            Matrix4f m = pose.last().pose();
            int a = (int) (alpha * 255) << 24;
            font.drawInBatch(text, -font.width(text) / 2f, 0, a | 0xFFD966, false, m, buf, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        buf.endBatch();
    }

    /** Floating captions for hero and boss lines. */
    private static void renderSpeech(RenderLevelStageEvent e) {
        if (ClientSiegeState.SPEECH.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Camera cam = e.getCamera();
        Vec3 cp = cam.getPosition();
        PoseStack pose = e.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        Font font = mc.font;
        float pt = e.getPartialTick();
        for (ClientSiegeState.Speech s : ClientSiegeState.SPEECH) {
            net.minecraft.world.entity.Entity ent = mc.level.getEntity(s.entity);
            if (ent == null || ent.distanceToSqr(cp) > 48 * 48) continue;
            float age = s.age + pt;
            float alpha = Math.min(1f, Math.min(age / 4f, (70 - age) / 10f));
            if (alpha <= 0.02f) continue;
            Vec3 p = ent.getPosition(pt).add(0, ent.getBbHeight() + 0.55 + Math.min(age, 6) * 0.02, 0);
            pose.pushPose();
            pose.translate(p.x - cp.x, p.y - cp.y, p.z - cp.z);
            pose.mulPose(cam.rotation());
            float sc = 0.026f;
            pose.scale(-sc, -sc, sc);
            Matrix4f m = pose.last().pose();
            int a = (int) (alpha * 255) << 24;
            String text = "\u201C" + s.text + "\u201D";
            float x = -font.width(text) / 2f;
            int c = s.color;
            int r = ((c >> 16) & 255) + (255 - ((c >> 16) & 255)) * 45 / 100, g = ((c >> 8) & 255) + (255 - ((c >> 8) & 255)) * 45 / 100,
                    bl = (c & 255) + (255 - (c & 255)) * 45 / 100;
            font.drawInBatch(text, x, 0, a | (r << 16) | (g << 8) | bl, false, m, buf, Font.DisplayMode.NORMAL,
                    ((int) (alpha * 0x90) << 24), LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        buf.endBatch();
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles e) {
        float[] o = com.lastbastion.client.fx.CameraShake.offsets((float) e.getPartialTick());
        if (o == null) return;
        e.setYaw(e.getYaw() + o[0]);
        e.setPitch(e.getPitch() + o[1]);
        e.setRoll(e.getRoll() + o[2]);
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog e) {
        if (ClientSiegeState.state() != SiegeState.WAVE || ClientSiegeState.modifier() != NightModifier.FOG) return;
        e.setNearPlaneDistance(2f);
        e.setFarPlaneDistance(Math.min(e.getFarPlaneDistance(), 36f));
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor e) {
        if (ClientSiegeState.state() != SiegeState.WAVE) return;
        NightModifier m = ClientSiegeState.modifier();
        if (m == NightModifier.BLOOD_MOON) {
            e.setRed(Math.min(1f, e.getRed() * 0.6f + 0.35f));
            e.setGreen(e.getGreen() * 0.3f);
            e.setBlue(e.getBlue() * 0.3f);
        } else if (m == NightModifier.FOG) {
            e.setRed(0.45f);
            e.setGreen(0.5f);
            e.setBlue(0.5f);
        } else if (m == NightModifier.NECROTIC) {
            e.setRed(e.getRed() * 0.7f + 0.1f);
            e.setGreen(e.getGreen() * 0.5f);
            e.setBlue(e.getBlue() * 0.8f + 0.15f);
        }
    }
}
