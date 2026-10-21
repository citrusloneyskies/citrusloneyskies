package com.tokyoghoul.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tokyoghoul.client.ClientState;
import com.tokyoghoul.client.model.CharacterAnimator;
import com.tokyoghoul.client.model.CharacterModels;
import com.tokyoghoul.client.model.ModelSpec;
import com.tokyoghoul.client.model.ProcModel;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.move.MoveDef;
import com.tokyoghoul.network.Sync;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Renders a character's procedural model for NPCs and for players who picked that character (they
 * become the character, like a morph). Adds the kagune layer, the held quinque and an emissive pass for
 * kakugan, ukaku crystals and kakuja veins.
 */
public class CharacterRenderer<T extends LivingEntity> extends LivingEntityRenderer<T, CharacterRenderer.CharModel<T>> {
    private final CharacterDef def;
    private final ModelSpec.Baked baked;

    public CharacterRenderer(EntityRendererProvider.Context ctx, CharacterDef def) {
        this(ctx, def, CharacterModels.baked(def));
    }

    private CharacterRenderer(EntityRendererProvider.Context ctx, CharacterDef def, ModelSpec.Baked baked) {
        super(ctx, new CharModel<>(baked), 0.5f * def.look.height);
        this.def = def;
        this.baked = baked;
        addLayer(new KaguneLayer<>(this));
        addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
        if (baked.texture.glow() != null) addLayer(new Glow<>(this, baked.texture.glow()));
    }

    @Override
    public ResourceLocation getTextureLocation(T e) {
        return baked.texture.texture();
    }

    public CharacterDef def() {
        return def;
    }

    @Override
    protected void scale(T e, PoseStack pose, float pt) {
        // NPC getScale() covers look height and transformation; players compute the same from synced flags
        float s;
        if (e instanceof Player) {
            s = def.look.height;
            if ((ClientState.flags(e) & Sync.FLAG_FORM) != 0 && def.ultimate() != null && def.ultimate().kind == MoveDef.Kind.FORM) {
                s *= def.ultimate().formScale;
            }
        } else {
            s = e.getScale();
        }
        pose.scale(s, s, s);
    }

    @Override
    protected boolean shouldShowName(T e) {
        if (e instanceof Player) return super.shouldShowName(e);
        return super.shouldShowName(e) && e.distanceToSqr(this.entityRenderDispatcher.camera.getPosition()) < 24 * 24;
    }

    /** The procedural body with a hand anchor so the item-in-hand layer can hold quinques. */
    public static class CharModel<T extends LivingEntity> extends ProcModel<T> implements ArmedModel {
        public CharModel(ModelSpec.Baked baked) {
            super(baked, CharacterAnimator::animate);
        }

        @Override
        public void translateToHand(HumanoidArm arm, PoseStack pose) {
            String s = arm == HumanoidArm.RIGHT ? "r_" : "l_";
            baked.root.translateAndRotate(pose);
            p("pelvis").translateAndRotate(pose);
            p("chest").translateAndRotate(pose);
            p(s + "arm").translateAndRotate(pose);
            p(s + "fore").translateAndRotate(pose);
            p(s + "hand").translateAndRotate(pose);
            // vanilla's item layer then moves 10px down the arm; start that far above the grip
            pose.translate(0, -8.8f / 16f, 0);
        }
    }

    /** Full-bright pass for glowing texels. */
    static class Glow<T extends LivingEntity> extends RenderLayer<T, CharModel<T>> {
        private final ResourceLocation tex;

        Glow(CharacterRenderer<T> parent, ResourceLocation tex) {
            super(parent);
            this.tex = tex;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float ls, float la, float pt, float age, float yaw, float pitch) {
            if (e.isInvisible()) return;
            VertexConsumer vc = buffers.getBuffer(RenderType.eyes(tex));
            getParentModel().renderRaw(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        }
    }
}
