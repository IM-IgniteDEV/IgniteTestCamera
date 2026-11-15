package com.ignitedev.ignitetestcamera.mixin;

import com.ignitedev.ignitetestcamera.IgniteTestCamera;
import com.ignitedev.ignitetestcamera.camera.CameraController;
import com.ignitedev.ignitetestcamera.camera.CameraPose;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    public abstract Quaternionf getRotation();

    @Inject(method = "update", at = @At("TAIL"))
    private void itcam$applyFreeCamera(World area, Entity focusedEntity, boolean thirdPerson,
                                       boolean inverseView, float tickDelta, CallbackInfo ci) {
        CameraController controller = IgniteTestCamera.controller();
        if (!controller.isActive()) {
            return;
        }
        CameraPose pose = controller.sample();
        if (pose == null) {
            return;
        }
        this.setPos(pose.x(), pose.y(), pose.z());
        this.setRotation(pose.yaw(), pose.pitch());
        if (pose.roll() != 0.0f) {
            // roll around the view axis; vanilla has no roll, so fold it into the rotation quaternion
            this.getRotation().rotateZ((float) Math.toRadians(pose.roll()));
        }
        controller.setLastApplied(pose);
    }
}
