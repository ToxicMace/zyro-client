package dev.zyro.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.zyro.client.module.Zoom;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 26.3: the camera owns field-of-view: Camera#calculateFov(float) -> float. */
@Mixin(Camera.class)
public abstract class ZoomMixin {
    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float zyro$zoom(float fov) {
        return fov * Zoom.factor();
    }
}
