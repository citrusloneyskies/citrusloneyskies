package com.lastbastion.client.render.weapon;

import com.lastbastion.LastBastion;
import com.lastbastion.client.ClientSetup;
import com.lastbastion.item.GunItem;
import com.lastbastion.network.GunReloadPacket;
import com.lastbastion.network.Net;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client side of the firearms: camera recoil, the first-person kick/reload/charge animation, the
 * two-handed aiming arm pose in third person, and the Reload key.
 */
@Mod.EventBusSubscriber(modid = LastBastion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GunAnimations {
    private static float recoil;
    private static float lastRecoil;
    private static int ticks;

    private GunAnimations() {
    }

    /** The local player fired: kick the view model and the camera. */
    public static void shot(GunItem.GunKind kind, float strength) {
        recoil = Math.min(1f, recoil + 0.55f * strength);
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        float kick = switch (kind) {
            case THUNDERBOOM -> 4.5f;
            case HELLFIRE -> 5.5f;
            case STARPIERCER -> 3.5f;
            case RAVAGER -> 0.9f;
            case DRAGONBREATH -> 0.2f;
            default -> 1.6f;
        } * strength;
        p.setXRot(p.getXRot() - kick);
        p.setYRot(p.getYRot() + (p.getRandom().nextFloat() - 0.5f) * kick * 0.4f);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        lastRecoil = recoil;
        recoil = Math.max(0, recoil * 0.62f - 0.02f);
        while (ClientSetup.RELOAD.consumeClick()) {
            if (mc.player != null && mc.screen == null && mc.player.getMainHandItem().getItem() instanceof GunItem g && g.kind.mag > 0) {
                Net.toServer(new GunReloadPacket(0));
            }
        }
    }

    static float recoil(float pt) {
        return lastRecoil + (recoil - lastRecoil) * pt;
    }

    public static void firstPerson(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, GunItem gun, float equip) {
        boolean left = arm == HumanoidArm.LEFT;
        float pt = Minecraft.getInstance().getFrameTime();
        pose.translate((left ? -1 : 1) * 0.56F, -0.52F + WeaponAnimations.equip(player, stack, equip) * -0.6F, -0.72F);
        boolean main = player.getMainHandItem() == stack;
        float reload = 0;
        if (main && GunItem.reloading(stack) && stack.getTag() != null) {
            long end = stack.getTag().getLong(GunItem.TAG_RELOAD);
            float total = Math.max(1, gun.reloadLength(stack));
            reload = 1f - Math.max(0, (end - player.level().getGameTime() - pt) / total);
        }
        float charge = 0;
        if (main && player.isUsingItem() && player.getUseItem() == stack && gun.kind == GunItem.GunKind.STARPIERCER) {
            charge = Math.min(1f, (player.getTicksUsingItem() + pt) / 20f);
        }
        GunPoses.firstPerson(new PoseXf(pose), gun.kind.name(), left, main ? recoil(pt) : 0, reload, charge);
    }

    /** Two-handed aim (the gauntlet is one-handed), following the head, with a little recoil. */
    public static void armPose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm, boolean oneHanded) {
        boolean right = arm == HumanoidArm.RIGHT;
        if (oneHanded) {
            ModelPart a = right ? model.rightArm : model.leftArm;
            a.xRot = -(float) Math.PI / 2f + model.head.xRot;
            a.yRot = model.head.yRot;
        } else {
            AnimationUtils.animateCrossbowHold(model.rightArm, model.leftArm, model.head, right);
        }
        if (entity == Minecraft.getInstance().player) {
            float r = recoil(Minecraft.getInstance().getFrameTime());
            (right ? model.rightArm : model.leftArm).xRot -= 0.25f * r;
        }
    }
}
