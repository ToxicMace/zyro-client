package dev.zyro.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.zyro.client.module.Zoom;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float zyro$zoom(float fov) {
        return fov * Zoom.factor();
    }
}
