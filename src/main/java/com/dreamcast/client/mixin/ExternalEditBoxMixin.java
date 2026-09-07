package com.dreamcast.client.mixin;

import com.dreamcast.client.gui.theme.ClientTheme;
import com.dreamcast.client.gui.theme.DreamcastUi;
import com.dreamcast.client.util.RenderUtils;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces the vanilla text-field sprite without touching editing semantics. */
@Mixin(EditBox.class)
public abstract class ExternalEditBoxMixin {

	@Redirect(
			method = "extractWidgetRenderState",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"),
			require = 1
	)
	private void dreamcast$glassField(GuiGraphicsExtractor graphics, RenderPipeline pipeline,
	                                  Identifier sprite, int x, int y, int width, int height) {
		Minecraft client = Minecraft.getInstance();
		Screen screen = client == null ? null : client.gui.screen();
		if (!DreamcastUi.shouldThemeExternal(screen)) {
			graphics.blitSprite(pipeline, sprite, x, y, width, height);
			return;
		}

		EditBox field = (EditBox) (Object) this;
		boolean highlighted = field.isHoveredOrFocused();
		float enabled = field.active ? 1.0F : 0.38F;
		int accent = ClientTheme.accent();
		int radius = Math.min(7, height / 2);
		RenderUtils.fillRoundedBorder(graphics, x, y, width, height, radius,
				RenderUtils.withAlpha(highlighted ? accent : 0xFFFFFFFF,
						(highlighted ? 0.72F : 0.16F) * enabled),
				RenderUtils.withAlpha(highlighted ? 0xFF141925 : 0xFF080A10, 0.92F * enabled));
	}
}
