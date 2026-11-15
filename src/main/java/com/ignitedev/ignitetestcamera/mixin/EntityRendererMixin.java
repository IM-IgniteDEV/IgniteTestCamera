package com.ignitedev.ignitetestcamera.mixin;

import com.ignitedev.ignitetestcamera.IgniteTestCamera;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true, require = 0)
    private void itcam$hideNametags(Entity entity, double squaredDistance, CallbackInfoReturnable<Boolean> cir) {
        if (IgniteTestCamera.controller().isNametagsHidden()) {
            cir.setReturnValue(false);
        }
    }
}
