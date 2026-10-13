package com.lastbastion.client.render;

import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.ProcModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Generic renderer for every procedural mob: drives level-gated parts, per-entity tint, scale,
 * translucency, pre-transforms (e.g. rising out of the ground) and a full-bright emissive layer.
 */
public class ProcRenderer<T extends Mob> extends MobRenderer<T, ProcModel<T>> {
    public interface Pre<T> {
        void apply(T entity, PoseStack pose, float pt);
    }

    public interface Tint<T> {
        /** Fill rgba[0..3] for this entity (defaults are 1). */
        void apply(T entity, float[] rgba);
    }

    private final ModelSpec.Baked baked;
    private float baseScale = 1f;
    private ToIntFunction<T> levelFn = e -> 1;
    private Function<T, Float> scaleFn = e -> 1f;
    @Nullable
    private Tint<T> tint;
    @Nullable
    private Pre<T> pre;
    private boolean translucent;
    @Nullable
    private Function<T, Float> glowAlpha;
    private final float[] rgba = new float[4];
    private float flip = 90f;

    public ProcRenderer(EntityRendererProvider.Context ctx, ModelSpec.Baked baked, ProcModel.Animator<T> animator, float shadow) {
        this(ctx, baked, animator, shadow, false);
    }

    public ProcRenderer(EntityRendererProvider.Context ctx, ModelSpec.Baked baked, ProcModel.Animator<T> animator, float shadow, boolean translucent) {
        super(ctx, translucent ? new ProcModel<>(baked, animator, RenderType::entityTranslucent) : new ProcModel<>(baked, animator), shadow);
        this.baked = baked;
        this.translucent = translucent;
        if (baked.texture.glow() != null) addLayer(new GlowLayer<>(this, baked.texture.glow()));
    }

    public ProcRenderer<T> scale(float s) {
        this.baseScale = s;
        return this;
    }

    public ProcRenderer<T> level(ToIntFunction<T> f) {
        this.levelFn = f;
        return this;
    }

    public ProcRenderer<T> scaleBy(Function<T, Float> f) {
        this.scaleFn = f;
        return this;
    }

    public ProcRenderer<T> tint(Tint<T> t) {
        this.tint = t;
        return this;
    }

    public ProcRenderer<T> pre(Pre<T> p) {
        this.pre = p;
        return this;
    }

    public ProcRenderer<T> glowAlpha(Function<T, Float> f) {
        this.glowAlpha = f;
        return this;
    }

    /** Disable the vanilla sideways death topple (for models with their own death animation). */
    public ProcRenderer<T> noDeathFlip() {
        this.flip = 0f;
        return this;
    }

    @Override
    protected float getFlipDegrees(T entity) {
        return flip;
    }

    public ProcRenderer<T> layer(RenderLayer<T, ProcModel<T>> layer) {
        addLayer(layer);
        return this;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return baked.texture.texture();
    }

    @Override
    protected void scale(T entity, PoseStack pose, float pt) {
        float s = baseScale * scaleFn.apply(entity);
        pose.scale(s, s, s);
    }

    @Override
    public void render(T entity, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        model.level = levelFn.applyAsInt(entity);
        rgba[0] = rgba[1] = rgba[2] = rgba[3] = 1f;
        if (tint != null) tint.apply(entity, rgba);
        model.tintR = rgba[0];
        model.tintG = rgba[1];
        model.tintB = rgba[2];
        model.alpha = rgba[3];
        pose.pushPose();
        if (pre != null) pre.apply(entity, pose, pt);
        super.render(entity, yaw, pt, pose, buffers, light);
        pose.popPose();
    }

    @Nullable
    @Override
    protected RenderType getRenderType(T entity, boolean visible, boolean translucentInvis, boolean glowing) {
        if (translucent && visible) return RenderType.entityTranslucent(getTextureLocation(entity));
        return super.getRenderType(entity, visible, translucentInvis, glowing);
    }

    float glowAlphaFor(T e) {
        return glowAlpha != null ? glowAlpha.apply(e) : 1f;
    }

    /** Emissive overlay: glowing eyes, runes, cores and crystals ignore lighting. */
    public static class GlowLayer<T extends Mob> extends RenderLayer<T, ProcModel<T>> {
        private final ResourceLocation tex;
        private final ProcRenderer<T> owner;

        public GlowLayer(ProcRenderer<T> parent, ResourceLocation tex) {
            super(parent);
            this.owner = parent;
            this.tex = tex;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbAmount, float pt,
                           float age, float yaw, float pitch) {
            if (entity.isInvisible()) return;
            VertexConsumer vc = buffers.getBuffer(RenderType.eyes(tex));
            float a = owner.glowAlphaFor(entity);
            getParentModel().renderRaw(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, a, a, a, 1f);
        }
    }

    public static <T extends Mob> RenderLayerParent<T, ProcModel<T>> parent(ProcRenderer<T> r) {
        return r;
    }
}
