package com.tokyoghoul.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.function.Function;

/**
 * Entity model backed by a baked {@link ModelSpec}. Animation is delegated to an {@link Animator};
 * the renderer sets {@link #level} (turret level / zombie rank / hero level) for level-gated parts
 * and an RGB tint before each frame.
 */
public class ProcModel<T extends Entity> extends EntityModel<T> {
    @FunctionalInterface
    public interface Animator<T extends Entity> {
        void animate(ProcModel<T> m, T e, float limbSwing, float limbAmount, float age, float headYaw, float headPitch, float pt);
    }

    public final ModelSpec.Baked baked;
    private final Animator<T> animator;
    public int level = 1;
    public float tintR = 1, tintG = 1, tintB = 1, alpha = 1;
    public float partialTick;
    /** Per-model scratch object for animation helpers (e.g. a cached {@link Poser}). */
    public Object poserCache;

    public ProcModel(ModelSpec.Baked baked, Animator<T> animator) {
        this(baked, animator, RenderType::entityCutoutNoCull);
    }

    public ProcModel(ModelSpec.Baked baked, Animator<T> animator, Function<ResourceLocation, RenderType> renderType) {
        super(renderType);
        this.baked = baked;
        this.animator = animator;
    }

    public ModelPart p(String name) {
        return baked.get(name);
    }

    public ModelPart opt(String name) {
        return baked.opt(name);
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbAmount, float partialTick) {
        this.partialTick = partialTick;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
        baked.root.getAllParts().forEach(ModelPart::resetPose);
        applyGates();
        animator.animate(this, entity, limbSwing, limbAmount, age, headYaw, headPitch, partialTick);
    }

    /** Show/hide level-gated parts for the current {@link #level}. */
    public void applyGates() {
        for (Map.Entry<ModelPart, int[]> g : baked.levelGates.entrySet()) {
            g.getKey().visible = level >= g.getValue()[0] && level <= g.getValue()[1];
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        int crashDepth = com.tokyoghoul.util.Safe.depth(pose);
        try {
        baked.root.render(pose, vc, light, overlay, Math.min(1f, r * tintR), Math.min(1f, g * tintG), Math.min(1f, b * tintB), Math.min(1f, a * alpha));
            } catch (Exception | LinkageError | StackOverflowError | AssertionError err) {
            com.tokyoghoul.util.Safe.report("ProcModel.renderToBuffer", err);
            com.tokyoghoul.util.Safe.restore(pose, crashDepth);
        }
    }

    /** Render with explicit colour (used by glow layers and block entity renderers). */
    public void renderRaw(PoseStack pose, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        baked.root.render(pose, vc, light, overlay, r, g, b, a);
    }
}
