package com.tokyoghoul.client.render;

import com.tokyoghoul.client.model.CharacterModels;
import com.tokyoghoul.client.model.ModelSpec;
import com.tokyoghoul.client.model.Paint;
import com.tokyoghoul.client.model.Paint.Mat;
import com.tokyoghoul.data.Look;
import com.tokyoghoul.item.MaskItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/** Worn masks and the CCG coat as procedural humanoid armour models. */
public final class WornClientExt implements IClientItemExtensions {
    public static final WornClientExt INSTANCE = new WornClientExt();
    private static final Map<String, HumanoidModel<LivingEntity>> CACHE = new HashMap<>();

    private WornClientExt() {
    }

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
        if (!(stack.getItem() instanceof MaskItem m)) return original;
        String id = ForgeRegistries.ITEMS.getKey(m).getPath();
        return CACHE.computeIfAbsent(id, k -> {
            ModelSpec s = new ModelSpec("worn_" + k, 4);
            var head = s.root.child("head", 0, 0, 0);
            s.root.child("hat", 0, 0, 0);
            var body = s.root.child("body", 0, 0, 0);
            var ra = s.root.child("right_arm", -5, 2, 0);
            var la = s.root.child("left_arm", 5, 2, 0);
            var rl = s.root.child("right_leg", -1.9f, 12, 0);
            var ll = s.root.child("left_leg", 1.9f, 12, 0);
            if (m.getType() == ArmorItem.Type.HELMET) {
                Look l = Look.of().mask(m.mask, m.color, m.accent);
                CharacterModels.maskInto(head, l, 8, 8, 8);
                // the mask group is named "mask"; worn masks are always visible
            } else {
                Paint coat = Paint.of(Mat.CLOTH, m.color).trim(m.accent, 0.4f);
                body.box(-4.4f, -0.4f, -2.4f, 8.8f, 12.6f, 4.8f, coat);
                body.box(-1.2f, -0.2f, -2.6f, 2.4f, 9, 0.3f, Paint.of(Mat.CLOTH, m.accent));
                ra.box(-3.4f, -2.4f, -2.4f, 4.8f, 10, 4.8f, coat);
                la.box(-1.4f, -2.4f, -2.4f, 4.8f, 10, 4.8f, coat);
                rl.box(-2.5f, -0.2f, -2.5f, 5, 8, 5, coat);
                ll.box(-2.5f, -0.2f, -2.5f, 5, 8, 5, coat);
            }
            return new HumanoidModel<>(s.bake().root);
        });
    }
}
