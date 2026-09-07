package com.dreamcast.client.mixin;

import com.dreamcast.client.gui.theme.ClientTheme;
import com.dreamcast.client.gui.theme.DreamcastUi;
import com.dreamcast.client.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dreamcast slider body while retaining vanilla input/value semantics. */
@Mixin(AbstractSliderButton.class)
public abstract class ExternalSliderMixin {

	@Shadow
	protected double value;

	@Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true, require = 1)
	private void dreamcast$glassSlider(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
	                                  float partialTick, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		Screen screen = client == null ? null : client.gui.screen();
		if (!DreamcastUi.shouldThemeExternal(screen)) {
			return;
		}
		AbstractSliderButton slider = (AbstractSliderButton) (Object) this;
		if (!slider.visible) {
			ci.cancel();
			return;
		}
		boolean highlighted = slider.isHoveredOrFocused();
		float enabled = slider.active ? 1.0F : 0.38F;
		int accent = ClientTheme.accent();
		int x = slider.getX();
		int y = slider.getY();
		int width = slider.getWidth();
		int height = slider.getHeight();
		int radius = Math.min(7, height / 2);
		RenderUtils.fillRoundedBorder(graphics, x, y, width, height, radius,
				RenderUtils.withAlpha(highlighted ? accent : 0xFFFFFFFF,
						(highlighted ? 0.70F : 0.15F) * enabled),
				RenderUtils.withAlpha(highlighted ? 0xFF171A25 : 0xFF090A10, 0.90F * enabled));
		int trackX = x + 7;
		int trackY = y + height - 4;
		int trackWidth = Math.max(8, width - 14);
		RenderUtils.fillRounded(graphics, trackX, trackY, trackWidth, 2, 1, 0x35FFFFFF);
		int filled = Math.max(2, (int) Math.round(trackWidth * Math.max(0.0, Math.min(1.0, value))));
		RenderUtils.fillRounded(graphics, trackX, trackY, filled, 2, 1,
				RenderUtils.withAlpha(accent, 0.92F * enabled));
		int knobX = trackX + filled - 2;
		RenderUtils.fillCircle(graphics, knobX, trackY + 1, 3.0F,
				RenderUtils.withAlpha(DreamcastUi.TEXT, enabled));

		String label = RenderUtils.clamp(client.font, slider.getMessage().getString(), width - 12);
		RenderUtils.textCentered(graphics, client.font, label, x + width / 2,
				y + Math.max(2, (height - client.font.lineHeight) / 2 - 1),
				RenderUtils.withAlpha(DreamcastUi.TEXT, enabled), false);
		ci.cancel();
	}
}
