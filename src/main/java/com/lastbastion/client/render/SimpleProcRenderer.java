package com.lastbastion.client.render;

import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.ProcModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.function.BiConsumer;

/** Renders a procedural model for non-living entities (projectiles, drop pods). */
public class SimpleProcRenderer<T extends Entity> extends EntityRenderer<T> {
    public interface Transform<T> {
        void apply(T entity, PoseStack pose, float pt);
    }

    private final ModelSpec.Baked baked;
    private final ProcModel<T> model;
    private final Transform<T> transform;
    private final boolean fullBright;

    public SimpleProcRenderer(EntityRendererProvider.Context ctx, ModelSpec.Baked baked, ProcModel.Animator<T> animator, Transform<T> transform,
                              boolean fullBright) {
        super(ctx);
        this.baked = baked;
        this.model = new ProcModel<>(baked, animator);
        this.transform = transform;
        this.fullBright = fullBright;
    }

    @Override
    public void render(T entity, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        transform.apply(entity, pose, pt);
        pose.scale(-1, -1, 1);
        pose.translate(0, -1.501, 0);
        model.setupAnim(entity, 0, 0, entity.tickCount + pt, 0, 0);
        int l = fullBright ? LightTexture.FULL_BRIGHT : light;
        model.renderRaw(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(baked.texture.texture())), l, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        if (baked.texture.glow() != null) {
            model.renderRaw(pose, buffers.getBuffer(RenderType.eyes(baked.texture.glow())), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        }
        pose.popPose();
        super.render(entity, yaw, pt, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return baked.texture.texture();
    }
}
