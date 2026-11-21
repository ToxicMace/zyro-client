package dev.zyro.client.mixin;

import dev.zyro.client.ZyroClient;
import dev.zyro.client.screen.ZyroTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    /** "Zyro Client 1.0.0 • 1.21.11" instead of "Minecraft 1.21.11 - Singleplayer". */
    @Inject(method = "createTitle", at = @At("HEAD"), cancellable = true)
    private void zyro$title(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ZyroClient.windowTitle());
    }

    /** Every time the game wants the vanilla title screen, show the Zyro menu instead. */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen zyro$menu(Screen screen) {
        if (screen instanceof TitleScreen && ZyroClient.CONFIG != null && ZyroClient.CONFIG.customMenu) {
            return new ZyroTitleScreen();
        }
        return screen;
    }
}
