package com.lastbastion.client.render.weapon;

import com.lastbastion.item.GunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Client hooks for firearms: 3D renderer, aiming arm pose and the first-person kick/reload animation. */
public final class GunClientExt implements IClientItemExtensions {
    public static final GunClientExt INSTANCE = new GunClientExt();
    private static HumanoidModel.ArmPose aim, aimOneHand;

    private GunClientExt() {
    }

    static HumanoidModel.ArmPose pose(boolean oneHanded) {
        if (aim == null) {
            aim = HumanoidModel.ArmPose.create("LASTBASTION_GUN", true, (m, e, arm) -> GunAnimations.armPose(m, e, arm, false));
            aimOneHand = HumanoidModel.ArmPose.create("LASTBASTION_GAUNTLET", false, (m, e, arm) -> GunAnimations.armPose(m, e, arm, true));
        }
        return oneHanded ? aimOneHand : aim;
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return WeaponRenderer.get();
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem g)) return null;
        return pose(g.kind == GunItem.GunKind.STORMFIST);
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick, float equip,
                                           float swing) {
        if (!(stack.getItem() instanceof GunItem g)) return false;
        GunAnimations.firstPerson(pose, player, arm, stack, g, equip);
        return true;
    }
}
