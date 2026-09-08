package com.dreamcast.client.gui;

import com.dreamcast.client.automation.ActionRecorder;
import com.dreamcast.client.gui.theme.ClientTheme;
import com.dreamcast.client.gui.theme.DreamcastUi;
import com.dreamcast.client.util.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

/**
 * Top bar shown while {@link ActionRecorder} is capturing input. There is no
 * click handling in this HUD layer (Fabric's HudElement is render-only), so
 * the "stop" pill is a visual reminder of the F6 keybind, not a real button.
 */
public final class RecordingHudElement implements HudElement {

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (!ActionRecorder.isActive()) return;
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null) return;
		int width = graphics.guiWidth();
		long now = Util.getMillis();
		int accent = ClientTheme.accent(now);

		RenderUtils.fillGlassPanel(graphics, 0, 0, width, 26, 0, DreamcastUi.BORDER, 0xE60A0B12, 0xE60A0B12, now);

		String label = "● Записано действий: " + ActionRecorder.frameCount();
		RenderUtils.textFlat(graphics, client.font, label, 12, 9, RenderUtils.withAlpha(accent, 1.0F));

		String stopLabel = "Остановить — F6";
		int stopWidth = RenderUtils.width(client.font, stopLabel) + 20;
		int stopX = width - stopWidth - 10;
		RenderUtils.fillGlassPanel(graphics, stopX, 4, stopWidth, 18, 6, DreamcastUi.BORDER, 0xFFFF6B78, 0xFFFF6B78, now);
		RenderUtils.textCentered(graphics, client.font, stopLabel, stopX + stopWidth / 2, 9, 0xFF0B0C10, false);
	}
}
