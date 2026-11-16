package com.ignitedev.ignitetestcamera.mixin;

import com.ignitedev.ignitetestcamera.IgniteTestCamera;
import com.ignitedev.ignitetestcamera.camera.CameraController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// players and mobs label through this override, not EntityRenderer.hasLabel, and it skips labels while hudHidden is on
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void itcam$hideNametags(LivingEntity entity, double squaredDistance, CallbackInfoReturnable<Boolean> cir) {
        if (IgniteTestCamera.controller().isNametagsHidden()) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;isHudEnabled()Z"))
    private boolean itcam$labelsWithHiddenHud() {
        CameraController controller = IgniteTestCamera.controller();
        return controller.isHudHideRequested() || MinecraftClient.isHudEnabled();
    }
}
