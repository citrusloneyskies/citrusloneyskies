package com.lastbastion.client.render.weapon;

import com.lastbastion.item.weapon.MeleeWeaponItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Client hooks for melee weapons: 3D renderer, custom arm poses and first-person swing animation. */
public final class WeaponClientExt implements IClientItemExtensions {
    public static final WeaponClientExt INSTANCE = new WeaponClientExt();
    private static HumanoidModel.ArmPose oneHanded, twoHanded;

    private WeaponClientExt() {
    }

    /**
     * Custom arm poses extend a vanilla enum, so they must exist before Minecraft first switches over it
     * (at the first player render); creating them later overflows vanilla's pose tables and crashes.
     * Called from the mod constructor.
     */
    public static void init() {
        pose(true);
    }

    static HumanoidModel.ArmPose pose(boolean two) {
        if (oneHanded == null) {
            oneHanded = HumanoidModel.ArmPose.create("LASTBASTION_MELEE", false, (m, e, arm) -> WeaponAnimations.armPose(m, e, arm, false));
            twoHanded = HumanoidModel.ArmPose.create("LASTBASTION_MELEE_2H", true, (m, e, arm) -> WeaponAnimations.armPose(m, e, arm, true));
        }
        return two ? twoHanded : oneHanded;
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return WeaponRenderer.get();
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (hand != InteractionHand.MAIN_HAND || !(stack.getItem() instanceof MeleeWeaponItem w)) return null;
        return pose(w.kind.weaponClass.twoHanded);
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick, float equip,
                                           float swing) {
        if (!(stack.getItem() instanceof MeleeWeaponItem w)) return false;
        WeaponAnimations.firstPerson(pose, player, arm, stack, w.kind, equip);
        return true;
    }
}
