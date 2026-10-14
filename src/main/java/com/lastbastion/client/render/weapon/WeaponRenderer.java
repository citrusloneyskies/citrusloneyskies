package com.lastbastion.client.render.weapon;

import com.lastbastion.LastBastion;
import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.gear.WeaponModels;
import com.lastbastion.item.weapon.Arsenal;
import com.lastbastion.item.weapon.MeleeWeaponItem;
import com.lastbastion.item.weapon.WeaponKind;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * Renders the arsenal's 3D weapons in hand, on the ground and in item frames, and their pre-rendered
 * sprite in inventories (falling back to the 3D model if the sprite model is missing).
 */
public final class WeaponRenderer extends BlockEntityWithoutLevelRenderer {
    private static WeaponRenderer instance;
    private final Map<WeaponKind, ModelSpec.Baked> models = new EnumMap<>(WeaponKind.class);

    private WeaponRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static WeaponRenderer get() {
        if (instance == null) instance = new WeaponRenderer();
        return instance;
    }

    public static ResourceLocation iconModel(WeaponKind k) {
        return new ResourceLocation(LastBastion.MODID, "item/icon/" + k.id);
    }

    ModelSpec.Baked model(WeaponKind k) {
        return models.computeIfAbsent(k, kind -> {
            ModelSpec s = new ModelSpec("weapon_" + kind.id, WeaponModels.density(kind));
            WeaponModels.build(kind, s);
            return s.bake();
        });
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof MeleeWeaponItem w)) return;
        WeaponKind k = w.kind;
        if (ctx == ItemDisplayContext.GUI && renderIcon(stack, k, pose, buffers, light, overlay)) return;
        ModelSpec.Baked b = model(k);
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        PoseXf xf = new PoseXf(pose);
        switch (ctx) {
            case THIRD_PERSON_RIGHT_HAND -> WeaponPoses.thirdPerson(xf, k, false);
            case THIRD_PERSON_LEFT_HAND -> WeaponPoses.thirdPerson(xf, k, true);
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> WeaponPoses.firstPersonModel(xf, k);
            case GROUND -> WeaponPoses.ground(xf, k);
            case GUI -> WeaponPoses.gui(xf, k);
            default -> WeaponPoses.fixed(xf, k);
        }
        b.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(b.texture.texture())), light, overlay);
        if (b.texture.glow() != null) {
            float pulse = Arsenal.ascended(stack) ? 0.85f + 0.15f * (float) Math.sin(System.currentTimeMillis() / 180.0) : 1f;
            b.root.render(pose, buffers.getBuffer(RenderType.eyes(b.texture.glow())), LightTexture.FULL_BRIGHT, overlay, pulse, pulse, pulse, 1f);
        }
        pose.popPose();
    }

    /** Inventory sprite (baked from the 3D model) drawn like a regular generated item. */
    private boolean renderIcon(ItemStack stack, WeaponKind k, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel icon = mc.getModelManager().getModel(iconModel(k));
        if (icon == null || icon == mc.getModelManager().getMissingModel()) return false;
        mc.getItemRenderer().renderModelLists(icon, stack, light, overlay, pose,
                ItemRenderer.getFoilBufferDirect(buffers, Sheets.translucentCullBlockSheet(), true, stack.hasFoil()));
        return true;
    }
}
