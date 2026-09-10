package com.dreamcast.client.mixin;

import com.dreamcast.client.gui.theme.ClientTheme;
import com.dreamcast.client.gui.theme.DreamcastUi;
import com.dreamcast.client.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rounded glass tabs for the vanilla frontend screens. */
@Mixin(targets = "net.minecraft.client.gui.components.tabs.MenuTabBar$MenuTabButton")
public abstract class ExternalTabButtonMixin {

	@Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true, require = 1)
	private void dreamcast$glassTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
	                               float partialTick, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		Screen screen = client == null ? null : client.gui.screen();
		if (!DreamcastUi.shouldThemeExternal(screen)) {
			return;
		}

		AbstractWidget widget = (AbstractWidget) (Object) this;
		if (!widget.visible) {
			ci.cancel();
			return;
		}
		TabButton tab = (TabButton) (Object) this;
		boolean selected = tab.isSelected();
		boolean highlighted = widget.isHoveredOrFocused();
		int accent = ClientTheme.accent();
		int x = widget.getX();
		int y = widget.getY();
		int width = widget.getWidth();
		int height = widget.getHeight();
		int radius = Math.min(7, height / 2);
		int border = selected || highlighted ? accent : 0x30FFFFFF;
		int fill = selected ? 0xE8171924 : highlighted ? 0xDB12141E : 0xB908090E;
		RenderUtils.fillRoundedBorder(graphics, x + 1, y + 1, Math.max(1, width - 2),
				Math.max(1, height - 2), radius, border, fill);
		if (selected && width > 14) {
			graphics.fill(x + 7, y + height - 2, x + width - 7, y + height - 1,
					RenderUtils.withAlpha(accent, 0.92F));
		}
		String label = RenderUtils.clamp(client.font, widget.getMessage().getString(), width - 10);
		RenderUtils.textCentered(graphics, client.font, label, x + width / 2,
				y + Math.max(1, (height - client.font.lineHeight) / 2),
				widget.active ? DreamcastUi.TEXT : DreamcastUi.TEXT_DIM, false);
		ci.cancel();
	}
}
