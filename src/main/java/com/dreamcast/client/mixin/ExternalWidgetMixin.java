package com.dreamcast.client.mixin;

import com.dreamcast.client.gui.theme.ClientTheme;
import com.dreamcast.client.gui.theme.DreamcastUi;
import com.dreamcast.client.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the Dreamcast glass edge to vanilla widgets on allowlisted external screens only. */
@Mixin(AbstractWidget.class)
public abstract class ExternalWidgetMixin {

	@Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
	private void dreamcast$styleExternalWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
	                                           float partialTick, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		Screen screen = client == null ? null : client.gui.screen();
		if (!DreamcastUi.shouldThemeExternal(screen)) {
			return;
		}
		AbstractWidget widget = (AbstractWidget) (Object) this;
		if (!widget.visible || widget.getWidth() <= 2 || widget.getHeight() <= 2) {
			return;
		}
		// Buttons and sliders have full replacements in dedicated mixins. This
		// generic pass only gives text fields a matching focus edge.
		if (!(widget instanceof EditBox field) || !field.isBordered()) {
			return;
		}

		boolean highlighted = widget.isHoveredOrFocused();
		int accent = ClientTheme.accent();
		float alpha = widget.active ? 1.0F : 0.38F;
		int radius = Math.min(7, widget.getHeight() / 2);
		int x = widget.getX();
		int y = widget.getY();
		int width = widget.getWidth();
		int height = widget.getHeight();
		RenderUtils.fillRoundedBorder(graphics, x, y, width, height, radius,
				RenderUtils.withAlpha(highlighted ? accent : 0xFFFFFFFF,
						(highlighted ? 0.70F : 0.15F) * alpha), 0x00000000);
		if (highlighted && width > 12) {
			graphics.fill(x + 6, y, x + width - 6, y + 1,
					RenderUtils.withAlpha(accent, 0.55F * alpha));
		}
	}
}
