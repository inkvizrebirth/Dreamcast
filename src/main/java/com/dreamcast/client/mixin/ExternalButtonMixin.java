package com.dreamcast.client.mixin;

import com.dreamcast.client.gui.theme.ClientTheme;
import com.dreamcast.client.gui.theme.DreamcastUi;
import com.dreamcast.client.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fully replaces vanilla button sprites on allowlisted external screens. */
@Mixin(AbstractButton.class)
public abstract class ExternalButtonMixin {

	@Inject(method = "extractWidgetRenderState", at = @At("HEAD"), require = 1)
	private void dreamcast$glassButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
	                                  float partialTick, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		Screen screen = client == null ? null : client.gui.screen();
		if (!DreamcastUi.shouldThemeExternal(screen)) {
			return;
		}
		AbstractButton button = (AbstractButton) (Object) this;
		if (!button.visible) {
			return;
		}
		boolean highlighted = button.isHoveredOrFocused();
		float enabled = button.active ? 1.0F : 0.38F;
		int accent = ClientTheme.accent();
		int x = button.getX();
		int y = button.getY();
		int width = button.getWidth();
		int height = button.getHeight();
		int radius = Math.min(7, height / 2);
		int fill = RenderUtils.mix(0xDF090A10, 0xEB171A25, highlighted ? 1.0F : 0.0F);
		int border = highlighted ? accent : 0x32FFFFFF;
		RenderUtils.fillRoundedBorder(graphics, x, y, width, height, radius,
				RenderUtils.withAlpha(border, enabled), RenderUtils.withAlpha(fill, enabled));
		if (highlighted && width > 12) {
			graphics.fill(x + 6, y, x + width - 6, y + 1,
					RenderUtils.withAlpha(accent, 0.68F * enabled));
		}
	}

	/**
	 * In 26.2 the concrete button content calls this method after the base
	 * widget method has started. Suppressing only the sprite keeps the label,
	 * narration, cursor requests and input semantics completely vanilla.
	 */
	@Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true, require = 1)
	private void dreamcast$hideVanillaSprite(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		Screen screen = client == null ? null : client.gui.screen();
		if (DreamcastUi.shouldThemeExternal(screen)) {
			ci.cancel();
		}
	}
}
