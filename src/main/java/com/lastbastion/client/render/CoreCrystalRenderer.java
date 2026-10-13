package com.lastbastion.client.render;

import com.lastbastion.block.SiegeCoreBlockEntity;
import com.lastbastion.client.ClientSiegeState;
import com.lastbastion.client.model.MiscModels;
import com.lastbastion.client.model.ModelSpec;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * The Siege Core's floating crystal: spins and bobs, its gyroscopic rings counter-rotate, it pulses
 * faster and redder as core health drops, and a translucent barrier bubble appears while the
 * Arcane Barrier holds.
 */
public class CoreCrystalRenderer implements BlockEntityRenderer<SiegeCoreBlockEntity> {
    public CoreCrystalRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(SiegeCoreBlockEntity be, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ModelSpec.Baked b = MiscModels.core();
        float t = be.ticks + pt;
        float hpFrac = ClientSiegeState.coreFraction();
        float danger = 1f - hpFrac;
        float speed = 1f + danger * 3f;
        float r = 1f, g = 1f - danger * 0.7f, bl = 1f - danger * 0.8f;
        pose.pushPose();
        pose.translate(0.5, 1.55 + Mth.sin(t * 0.08f) * 0.08, 0.5);
        var crystal = b.get("crystal");
        crystal.yRot = t * 0.03f * speed;
        float pulse = 1f + Mth.sin(t * 0.15f * speed) * 0.06f;
        crystal.xScale = crystal.yScale = crystal.zScale = pulse;
        b.get("ring1").yRot = -t * 0.05f;
        b.get("ring1").xRot = Mth.sin(t * 0.02f) * 0.4f;
        b.get("ring2").yRot = t * 0.04f;
        b.get("ring2").zRot = Mth.cos(t * 0.03f) * 0.5f;
        var barrier = b.get("barrier");
        barrier.visible = false;
        var glow = b.texture.glow();
        crystal.render(pose, buffers.getBuffer(RenderType.eyes(glow)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, bl, 1f);
        b.get("ring1").render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(b.texture.texture())), light, OverlayTexture.NO_OVERLAY);
        b.get("ring2").render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(b.texture.texture())), light, OverlayTexture.NO_OVERLAY);
        b.get("ring1").render(pose, buffers.getBuffer(RenderType.eyes(glow)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        b.get("ring2").render(pose, buffers.getBuffer(RenderType.eyes(glow)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        if (ClientSiegeState.shieldFraction() > 0.01f) {
            barrier.visible = true;
            pose.mulPose(Axis.YP.rotationDegrees(t * 0.5f));
            float s = 1f + Mth.sin(t * 0.1f) * 0.02f;
            pose.scale(s, s, s);
            float a = 0.18f + 0.15f * ClientSiegeState.shieldFraction();
            barrier.render(pose, buffers.getBuffer(RenderType.entityTranslucent(b.texture.texture())), LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, 0.6f, 0.9f, 1f, a);
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(SiegeCoreBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
