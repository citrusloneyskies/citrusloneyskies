package com.tokyoghoul.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tokyoghoul.client.model.ModelSpec;
import com.tokyoghoul.client.model.QuinqueModels;
import com.tokyoghoul.item.QuinqueItem;
import com.tokyoghoul.item.QuinqueKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.EnumMap;
import java.util.Map;

/** 3D quinques in hand, on the ground, in frames and in the inventory. */
public final class QuinqueClientExt implements IClientItemExtensions {
    public static final QuinqueClientExt INSTANCE = new QuinqueClientExt();
    private Renderer renderer;

    private QuinqueClientExt() {
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) renderer = new Renderer();
        return renderer;
    }

    static final class Renderer extends BlockEntityWithoutLevelRenderer {
        private final Map<QuinqueKind, ModelSpec.Baked> models = new EnumMap<>(QuinqueKind.class);

        Renderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        ModelSpec.Baked model(QuinqueKind k) {
            return models.computeIfAbsent(k, kind -> {
                ModelSpec s = new ModelSpec("quinque_" + kind.id, 5);
                QuinqueModels.build(kind, s);
                return s.bake();
            });
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            if (!(stack.getItem() instanceof QuinqueItem q)) return;
            int depth = com.tokyoghoul.util.Safe.depth(pose);
            try {
                QuinqueKind k = q.kind;
                ModelSpec.Baked b = model(k);
                float len = QuinqueModels.length(k);
                pose.pushPose();
                switch (ctx) {
                    case GUI, FIXED -> {
                        // diagonal like a sword icon, fitted to the slot
                        pose.translate(0.5f, 0.5f, 0.5f);
                        pose.mulPose(Axis.ZP.rotationDegrees(-45));
                        float s = 1.25f / (len / 16f);
                        pose.scale(s, s, s);
                        pose.translate(0, len / 2f / 16f, 0);
                        pose.mulPose(Axis.XP.rotationDegrees(180));
                    }
                    case GROUND -> {
                        pose.translate(0.5f, 0.3f, 0.5f);
                        pose.mulPose(Axis.ZP.rotationDegrees(90));
                        pose.scale(0.6f, 0.6f, 0.6f);
                        pose.mulPose(Axis.XP.rotationDegrees(180));
                    }
                    default -> {
                        // hands: grip at the hand, blade pointing up and forward
                        pose.translate(0.5f, 0.35f, 0.5f);
                        pose.mulPose(Axis.XP.rotationDegrees(180));
                        boolean first = ctx.firstPerson();
                        float s = first ? 0.7f : 0.85f;
                        pose.scale(s, s, s);
                        pose.mulPose(Axis.XP.rotationDegrees(first ? 10 : -5));
                    }
                }
                b.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(b.texture.texture())), light, overlay);
                if (b.texture.glow() != null) {
                    b.root.render(pose, buffers.getBuffer(RenderType.eyes(b.texture.glow())), LightTexture.FULL_BRIGHT, overlay, 1, 1, 1, 1);
                }
                pose.popPose();
            } catch (Exception | LinkageError err) {
                com.tokyoghoul.util.Safe.report("QuinqueRenderer", err);
                com.tokyoghoul.util.Safe.restore(pose, depth);
            }
        }
    }
}
