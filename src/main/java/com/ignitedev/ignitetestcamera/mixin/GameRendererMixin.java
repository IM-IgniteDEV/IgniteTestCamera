package com.ignitedev.ignitetestcamera.mixin;

import com.ignitedev.ignitetestcamera.IgniteTestCamera;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void itcam$overrideFov(Camera camera, float tickDelta, boolean changingFov,
                                   CallbackInfoReturnable<Float> cir) {
        Float fov = IgniteTestCamera.controller().effectiveFov();
        if (fov != null) {
            cir.setReturnValue(fov);
        }
    }
}
