package com.lastbastion.client.render.armor;

import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.gear.ArmorModels;
import com.lastbastion.item.armor.ArmorSet;
import com.lastbastion.item.armor.SiegeArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/** Supplies the procedural 3D worn model for every piece of siege armour (with an emissive pass). */
public final class ArmorClientExt implements IClientItemExtensions {
    public static final ArmorClientExt INSTANCE = new ArmorClientExt();
    private static final Map<String, Worn> CACHE = new HashMap<>();

    private ArmorClientExt() {
    }

    /** Humanoid armour model built from a {@link ModelSpec}; glowing texels get a full-bright pass. */
    public static final class Worn extends HumanoidModel<LivingEntity> {
        @Nullable
        private final ResourceLocation glow;

        Worn(ModelSpec.Baked baked) {
            super(baked.root);
            this.glow = baked.texture.glow();
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        int lbCrashDepth = com.lastbastion.util.Safe.depth(pose);
        try {
            super.renderToBuffer(pose, vc, light, overlay, r, g, b, a);
            if (glow != null) {
                VertexConsumer gv = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.eyes(glow));
                super.renderToBuffer(pose, gv, LightTexture.FULL_BRIGHT, overlay, 1f, 1f, 1f, a);
            }
                } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("ArmorClientExt.renderToBuffer", lbCrash);
            com.lastbastion.util.Safe.restore(pose, lbCrashDepth);
        }
    }
    }

    public static Worn model(ArmorSet set, int slot) {
        String key = set.id + "_" + ArmorSet.SLOT_IDS[slot];
        return CACHE.computeIfAbsent(key, k -> {
            ModelSpec s = new ModelSpec("armor_" + k, 4);
            ArmorModels.build(set, slot, s);
            return new Worn(s.bake());
        });
    }

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
        if (!(stack.getItem() instanceof SiegeArmorItem a)) return original;
        return model(a.set, a.slotIndex());
    }
}
