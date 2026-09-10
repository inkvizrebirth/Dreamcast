package com.dreamcast.client.gui;

import com.dreamcast.client.automation.ActionRecorder;
import com.dreamcast.client.gui.theme.ClientTheme;
import com.dreamcast.client.gui.theme.DreamcastUi;
import com.dreamcast.client.util.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

/**
 * Top bar shown while {@link ActionRecorder} is capturing input. There is no
 * click handling in this HUD layer (Fabric's HudElement is render-only), so
 * the animated stop pill shows the currently bound stop-recording key.
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

		KeyMapping stopMapping = KeyMapping.get("key.dreamcast.stop_recording");
		String stopKey = stopMapping == null ? "?" : stopMapping.getTranslatedKeyMessage().getString();
		String stopLabel = "Остановить — " + stopKey;
		int stopWidth = RenderUtils.width(client.font, stopLabel) + 20;
		int stopX = width - stopWidth - 10;
		float pulse = 0.5F + 0.5F * (float) Math.sin(now / 180.0);
		int stopAccent = RenderUtils.mix(0xFFFF5264, 0xFFFFC16B, pulse);
		RenderUtils.fillGlassPanel(graphics, stopX, 4, stopWidth, 18, 6, RenderUtils.withAlpha(stopAccent, 0.80F), stopAccent, stopAccent, now);
		RenderUtils.fillCircle(graphics, stopX + 8, 13, 2.0F + pulse, RenderUtils.withAlpha(0xFFFFFFFF, 0.45F + 0.35F * pulse));
		RenderUtils.textCentered(graphics, client.font, stopLabel, stopX + stopWidth / 2, 9, 0xFF0B0C10, false);
	}
}
