package com.tokyoghoul.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tokyoghoul.client.model.ModelSpec;
import com.tokyoghoul.client.model.Paint;
import com.tokyoghoul.entity.GroundSpike;
import com.tokyoghoul.entity.RcProjectile;
import com.tokyoghoul.move.MoveDef;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Renderers for the move effect entities: glowing shards/bolts/knives and erupting spikes. */
public final class FxRenderers {
    private FxRenderers() {
    }

    /** Emits a glowing elongated octahedron (crystal) into an additive buffer. */
    static void crystal(PoseStack pose, VertexConsumer vc, float len, float rad, int color, float alpha) {
        Matrix4f m = pose.last().pose();
        float r = ((color >> 16) & 255) / 255f, g = ((color >> 8) & 255) / 255f, b = (color & 255) / 255f;
        float[][] ring = {{rad, 0}, {0, rad}, {-rad, 0}, {0, -rad}};
        float tip = len * 0.6f, tail = -len * 0.4f;
        for (int i = 0; i < 4; i++) {
            float[] a = ring[i], c = ring[(i + 1) % 4];
            // front pyramid
            vc.vertex(m, 0, 0, tip).color(r, g, b, alpha).endVertex();
            vc.vertex(m, a[0], a[1], 0).color(r * 0.7f, g * 0.7f, b * 0.7f, alpha).endVertex();
            vc.vertex(m, c[0], c[1], 0).color(r * 0.7f, g * 0.7f, b * 0.7f, alpha).endVertex();
            vc.vertex(m, 0, 0, tip).color(r, g, b, alpha).endVertex();
            // back pyramid
            vc.vertex(m, 0, 0, tail).color(r * 0.4f, g * 0.4f, b * 0.4f, alpha * 0.6f).endVertex();
            vc.vertex(m, c[0], c[1], 0).color(r * 0.7f, g * 0.7f, b * 0.7f, alpha).endVertex();
            vc.vertex(m, a[0], a[1], 0).color(r * 0.7f, g * 0.7f, b * 0.7f, alpha).endVertex();
            vc.vertex(m, 0, 0, tail).color(r * 0.4f, g * 0.4f, b * 0.4f, alpha * 0.6f).endVertex();
        }
    }

    public static class Projectile extends EntityRenderer<RcProjectile> {
        public Projectile(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public void render(RcProjectile e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pt, e.yRotO, e.getYRot())));
            pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(pt, e.xRotO, e.getXRot())));
            VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
            int c = e.color();
            MoveDef.Shot shot = e.shot();
            switch (shot) {
                case KNIFE, BULLET -> {
                    crystal(pose, vc, 0.55f, 0.05f, 0xD8DCE4, 0.9f);
                    crystal(pose, vc, 0.35f, 0.08f, c, 0.5f);
                }
                case SPEAR -> {
                    crystal(pose, vc, 1.4f, 0.09f, c, 0.95f);
                    crystal(pose, vc, 1.0f, 0.16f, 0xFFFFFF, 0.25f);
                }
                case LIGHTNING -> {
                    float jit = (e.tickCount % 3) * 0.04f;
                    pose.mulPose(Axis.ZP.rotationDegrees(e.tickCount * 37));
                    crystal(pose, vc, 1.2f + jit, 0.06f, 0xFFFFFF, 0.9f);
                    crystal(pose, vc, 1.0f, 0.18f, c, 0.6f);
                }
                case FLAME -> {
                    crystal(pose, vc, 0.8f, 0.25f, 0xFF8A1A, 0.7f);
                    crystal(pose, vc, 0.5f, 0.12f, 0xFFF0A0, 0.9f);
                }
                default -> {
                    pose.mulPose(Axis.ZP.rotationDegrees((e.getId() * 53) % 360));
                    crystal(pose, vc, 0.9f, 0.13f, c, 0.95f);
                    crystal(pose, vc, 0.6f, 0.22f, 0xFFFFFF, 0.2f);
                }
            }
            pose.popPose();
            super.render(e, yaw, pt, pose, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(RcProjectile e) {
            return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
        }
    }

    public static class Spike extends EntityRenderer<GroundSpike> {
        private static ModelSpec.Baked model;

        public Spike(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        private static ModelSpec.Baked model() {
            if (model == null) {
                ModelSpec s = new ModelSpec("ground_spike", 4);
                Paint p = Paint.of(Paint.Mat.CHITIN, 0xE8E8E8).pattern(Paint.Pattern.VEINS, 0xFFFFFF, 0.8f).accentGlow().gloss(0.6f);
                var root = s.root.child("spike", 0, 24, 0);
                root.taper(0, 0, 0, 7, 7, 0.6f, 0.6f, 34, 6, p);
                root.spike(2.5f, -2, 0, 3, 14, 0, 0, -22, p);
                root.spike(-2.5f, -2, 1, 2.6f, 11, 15, 0, 25, p);
                root.spike(0, -1, -2.5f, 2.4f, 9, -25, 0, 0, p);
                model = s.bake();
            }
            return model;
        }

        @Override
        public void render(GroundSpike e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
            float rise = GroundSpike.rise(e.age() + pt);
            if (rise <= 0.01f) return;
            ModelSpec.Baked b = model();
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(e.getYRot()));
            pose.translate(0, -(1 - rise) * 2.2f, 0);
            pose.scale(-1, -1, 1);
            pose.translate(0, -1.5f, 0);
            int c = e.color();
            float r = ((c >> 16) & 255) / 255f, g = ((c >> 8) & 255) / 255f, bl = (c & 255) / 255f;
            b.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(b.texture.texture())), light, OverlayTexture.NO_OVERLAY, r, g, bl, 1);
            if (b.texture.glow() != null) {
                b.root.render(pose, buffers.getBuffer(RenderType.eyes(b.texture.glow())), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, bl, 1);
            }
            pose.popPose();
        }

        @Override
        public ResourceLocation getTextureLocation(GroundSpike e) {
            return model().texture.texture();
        }
    }
}
