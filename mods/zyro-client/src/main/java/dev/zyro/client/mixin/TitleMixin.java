package dev.zyro.client.mixin;

import dev.zyro.client.ZyroClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** "Zyro Client 1.0.0 • 1.21.11" instead of "Minecraft 1.21.11 - Singleplayer" (all versions). */
@Mixin(Minecraft.class)
public abstract class TitleMixin {
    @Inject(method = "createTitle", at = @At("HEAD"), cancellable = true)
    private void zyro$title(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ZyroClient.windowTitle());
    }
}
