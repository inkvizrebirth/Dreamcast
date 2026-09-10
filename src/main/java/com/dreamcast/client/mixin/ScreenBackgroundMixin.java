package com.dreamcast.client.mixin;

import com.dreamcast.client.gui.theme.DreamcastUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives allowed vanilla configuration/creation screens the same atmospheric
 * background while explicitly leaving inventories and other gameplay GUIs alone.
 */
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {

	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true, require = 0)
	private void dreamcast$externalBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
	                                          float partialTick, CallbackInfo ci) {
		Screen self = (Screen) (Object) this;
		if (DreamcastUi.shouldThemeExternal(self)) {
			DreamcastUi.drawBackdrop(graphics, self.width, self.height, mouseX, mouseY, 0.54F);
			ci.cancel();
		}
	}

	@Inject(method = "extractMenuBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
			at = @At("HEAD"), cancellable = true, require = 0)
	private void dreamcast$externalMenuBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		Screen self = (Screen) (Object) this;
		if (DreamcastUi.shouldThemeExternal(self)) {
			DreamcastUi.drawExternalBackdrop(graphics);
			ci.cancel();
		}
	}

	@Inject(method = "extractMenuBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIII)V",
			at = @At("HEAD"), cancellable = true, require = 0)
	private void dreamcast$externalMenuRegion(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
	                                        CallbackInfo ci) {
		Screen self = (Screen) (Object) this;
		if (DreamcastUi.shouldThemeExternal(self)) {
			// The full backdrop is already drawn by the screen. Keep list regions
			// transparent so vanilla tiled dirt cannot cover it.
			RenderRegion.fill(graphics, x, y, width, height);
			ci.cancel();
		}
	}

	/** Kept nested to make the injected method itself allocation-free. */
	private static final class RenderRegion {
		private static void fill(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
			graphics.fill(x, y, width, height, 0x5205060B);
		}
	}
}
