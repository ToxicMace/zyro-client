package dev.zyro.client.mixin;

import dev.zyro.client.ZyroClient;
import dev.zyro.client.screen.ZyroTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** 1.21.11: screens are switched by Minecraft#setScreen. */
@Mixin(Minecraft.class)
public abstract class ScreenSwapMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen zyro$menu(Screen screen) {
        if (screen instanceof TitleScreen && ZyroClient.CONFIG != null && ZyroClient.CONFIG.customMenu) {
            return new ZyroTitleScreen();
        }
        return screen;
    }
}
