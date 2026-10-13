package com.lastbastion.client.fx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;

/** Render types for VFX geometry and additive particles. */
public final class VfxRenderTypes extends RenderType {
    private VfxRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort,
                           Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }

    /** Additive glow (beams, slashes, rings): never writes depth, brightens what is behind it. */
    public static final RenderType ADDITIVE = create("lastbastion_vfx_additive", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
            262144, false, false, CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setWriteMaskState(COLOR_WRITE)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    /** Alpha-blended overlay (ground telegraphs that must read on bright and dark ground alike). */
    public static final RenderType TRANSLUCENT = create("lastbastion_vfx_translucent", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
            262144, false, false, CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setWriteMaskState(COLOR_WRITE)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    /** Additive particle sheet for glowing particles. */
    public static final ParticleRenderType PARTICLE_ADDITIVE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder b, TextureManager tm) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator t) {
            t.end();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "LASTBASTION_PARTICLE_ADDITIVE";
        }
    };
}
