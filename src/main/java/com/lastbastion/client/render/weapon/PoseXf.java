package com.lastbastion.client.render.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/** {@link WeaponPoses.Xf} backed by a PoseStack. */
public final class PoseXf implements WeaponPoses.Xf {
    private final PoseStack pose;

    public PoseXf(PoseStack pose) {
        this.pose = pose;
    }

    @Override
    public void translate(float x, float y, float z) {
        pose.translate(x, y, z);
    }

    @Override
    public void rotX(float deg) {
        if (deg != 0) pose.mulPose(Axis.XP.rotationDegrees(deg));
    }

    @Override
    public void rotY(float deg) {
        if (deg != 0) pose.mulPose(Axis.YP.rotationDegrees(deg));
    }

    @Override
    public void rotZ(float deg) {
        if (deg != 0) pose.mulPose(Axis.ZP.rotationDegrees(deg));
    }

    @Override
    public void scale(float x, float y, float z) {
        pose.scale(x, y, z);
    }
}
