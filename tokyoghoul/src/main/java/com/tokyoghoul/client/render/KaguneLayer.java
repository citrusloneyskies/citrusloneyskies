package com.tokyoghoul.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tokyoghoul.client.ClientState;
import com.tokyoghoul.client.model.CharacterModels;
import com.tokyoghoul.client.model.KaguneModels;
import com.tokyoghoul.client.model.ModelSpec;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.KaguneType;
import com.tokyoghoul.network.Sync;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Draws the kagune on a character while it is released (or during a kakuja), anchored to the lower back
 * or the shoulder blades of the animated body and posed for the current move.
 */
public class KaguneLayer<T extends LivingEntity> extends RenderLayer<T, CharacterRenderer.CharModel<T>> {
    public KaguneLayer(CharacterRenderer<T> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float ls, float la, float pt, float age, float yaw, float pitch) {
        if (e.isInvisible()) return;
        CharacterDef def = ClientState.character(e);
        if (def == null || !def.hasKagune()) return;
        int flags = ClientState.flags(e);
        boolean form = (flags & Sync.FLAG_FORM) != 0;
        if ((flags & Sync.FLAG_KAGUNE) == 0 && !form) return;
        int anim = ClientState.anim(e);
        float t = ClientState.animTime(e, pt);
        draw(pose, buffers, light, e, def, def.kagune, def.kaguneCount, def.kaguneColor, anim, t, age, form);
        if (def.kagune2 != KaguneType.NONE) draw(pose, buffers, light, e, def, def.kagune2, 2, def.kaguneColor2, anim, t, age + 37, form);
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, int light, T e, CharacterDef def, KaguneType type, int count, int color, int anim,
                      float t, float age, boolean form) {
        ModelSpec.Baked b = KaguneModels.baked(type, count);
        KaguneModels.animate(b, type, count, anim, t, age, form);
        float[] dm = CharacterModels.dims(def.look);
        var m = getParentModel();
        int passes = KaguneModels.onArms(type) && count >= 2 ? 2 : 1;
        for (int pass = 0; pass < passes; pass++) {
            pose.pushPose();
            m.baked.root.translateAndRotate(pose);
            m.p("pelvis").translateAndRotate(pose);
            if (KaguneModels.onArms(type)) {
                String side = pass == 0 ? "r_" : "l_";
                m.p("chest").translateAndRotate(pose);
                m.p(side + "arm").translateAndRotate(pose);
                m.p(side + "fore").translateAndRotate(pose);
            } else if (KaguneModels.fromLowerBack(type)) {
                pose.translate(0, -1.6f / 16f, (dm[5] / 2 + 0.2f) / 16f);
            } else {
                m.p("chest").translateAndRotate(pose);
                pose.translate(0, -dm[1] * 0.72f / 16f, (dm[2] / 2 + 0.2f) / 16f);
            }
            render(pose, buffers, light, b, color, age);
            pose.popPose();
        }
    }

    private static void render(PoseStack pose, MultiBufferSource buffers, int light, ModelSpec.Baked b, int color, float age) {
        float r = ((color >> 16) & 255) / 255f, g = ((color >> 8) & 255) / 255f, bl = (color & 255) / 255f;
        b.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(b.texture.texture())), light, OverlayTexture.NO_OVERLAY, r, g, bl, 1f);
        if (b.texture.glow() != null) {
            float pulse = 0.75f + 0.25f * Mth.sin(age * 0.2f);
            b.root.render(pose, buffers.getBuffer(RenderType.eyes(b.texture.glow())), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    r * pulse, g * pulse, bl * pulse, 1f);
        }
    }
}
