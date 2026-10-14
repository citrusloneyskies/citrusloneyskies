package com.lastbastion.client.render;

import com.lastbastion.block.SiegeCoreBlockEntity;
import com.lastbastion.client.ClientSiegeState;
import com.lastbastion.client.model.CoreModel;
import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.siege.CoreUpgrade;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * Renders the Siege Core monument. The crystal spins, bobs and pulses faster as the core is hurt,
 * shifting from cyan through amber to red while cracks spread and orbiting shards break away; rune
 * rings counter-rotate; the cradle claws breathe; purchased upgrades appear as hardware, cogs spin with
 * Overclock and the shield dome is drawn by the VFX renderer.
 */
public class CoreCrystalRenderer implements BlockEntityRenderer<SiegeCoreBlockEntity> {
    public CoreCrystalRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(SiegeCoreBlockEntity be, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ModelSpec.Baked b = CoreModel.baked();
        float t = be.ticks + pt;
        float hp = ClientSiegeState.active() ? ClientSiegeState.coreFraction() : 1f;
        float danger = 1f - hp;
        float speed = 1f + danger * 3f;
        int[] up = ClientSiegeState.upgrades();
        // reset the animated parts
        b.root.getAllParts().forEach(ModelPart::resetPose);
        ModelPart crystal = b.get("crystal"), ring1 = b.get("ring1"), ring2 = b.get("ring2"), ring3 = b.get("ring3"), shards = b.get("shards");
        crystal.y += Mth.sin(t * 0.08f) * 1.2f;
        crystal.yRot = t * 0.03f * speed;
        float pulse = 1f + Mth.sin(t * 0.15f * speed) * (0.04f + danger * 0.05f);
        crystal.xScale = crystal.zScale = pulse;
        crystal.yScale = pulse;
        if (hp < 0.25f) crystal.x += (Mth.sin(t * 2.3f) * 0.25f);
        ModelPart light2 = b.get("core_light");
        light2.xScale = light2.yScale = light2.zScale = 0.9f + Mth.sin(t * 0.3f * speed) * 0.15f;
        b.get("crack1").visible = hp < 0.6f;
        b.get("crack2").visible = hp < 0.3f;
        ring1.yRot = -t * 0.05f;
        ring1.xRot = Mth.sin(t * 0.02f) * 0.25f;
        ring2.yRot = t * 0.04f;
        ring2.zRot = Mth.cos(t * 0.03f) * 0.3f;
        ring3.xRot = t * 0.07f;
        ring3.yRot = t * 0.02f;
        shards.yRot = t * 0.06f;
        int shardCount = Math.max(0, Math.round(6 * hp + 0.49f));
        for (int i = 0; i < 6; i++) {
            ModelPart sh = b.get("shard" + i);
            sh.visible = i < shardCount;
            sh.y += Mth.sin(t * 0.1f + i) * 1.5f;
            sh.yRot = t * 0.1f + i;
        }
        for (int i = 0; i < 4; i++) b.get("claw" + i).xRot = Mth.sin(t * 0.05f) * 0.06f;
        // upgrade hardware
        int fort = lvl(up, CoreUpgrade.FORTIFY), bar = lvl(up, CoreUpgrade.BARRIER), pul = lvl(up, CoreUpgrade.PULSE);
        int tre = lvl(up, CoreUpgrade.TREASURY), oc = lvl(up, CoreUpgrade.OVERCLOCK), bea = lvl(up, CoreUpgrade.BEACON);
        b.get("fort1").visible = fort >= 1;
        b.get("fort3").visible = fort >= 3;
        b.get("fort5").visible = fort >= 5;
        b.get("barrier1").visible = bar >= 1;
        b.get("pulse1").visible = pul >= 1;
        b.get("pulse3").visible = pul >= 3;
        b.get("treasury1").visible = tre >= 1;
        b.get("treasury3").visible = tre >= 3;
        b.get("gear_r").visible = b.get("gear_l").visible = oc >= 1;
        b.get("gear_r").xRot = t * 0.05f * (1 + oc);
        b.get("gear_l").xRot = -t * 0.05f * (1 + oc);
        b.get("halo").visible = bea >= 1;
        b.get("halo").yRot = t * 0.02f;
        b.get("beacon2").visible = bea >= 2;

        float r = 1f, g = 1f - danger * 0.75f, bl = 1f - danger * 0.9f;
        if (hp < 0.25f) {
            float flick = 0.75f + 0.25f * Mth.sin(t * 0.9f);
            g *= flick;
            bl *= flick;
        }
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.scale(-1, -1, 1);
        b.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(b.texture.texture())), light, OverlayTexture.NO_OVERLAY);
        if (b.texture.glow() != null) {
            b.root.render(pose, buffers.getBuffer(RenderType.eyes(b.texture.glow())), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, bl, 1f);
        }
        pose.popPose();
    }

    private static int lvl(int[] up, CoreUpgrade u) {
        return u.ordinal() < up.length ? up[u.ordinal()] : 0;
    }

    @Override
    public boolean shouldRenderOffScreen(SiegeCoreBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 160;
    }
}
