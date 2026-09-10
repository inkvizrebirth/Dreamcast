package com.dreamcast.client.gui.theme;

import com.dreamcast.client.DreamcastClient;
import com.dreamcast.client.gui.screens.DreamcastScreen;
import com.dreamcast.client.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

/**
 * Единый визуальный язык внешних экранов Dreamcast.
 *
 * <p>Класс намеренно не хранит покадровые коллекции: фон, сетка и иконки
 * процедурные и дешёвые. Это исключает старую проблему, когда hover главного
 * меню провоцировал большое число аллокаций и просадку FPS.</p>
 */
public final class DreamcastUi {

	public static final int VIOLET = 0xFF7C6CFF;
	public static final int CYAN = 0xFF45E3FF;
	public static final int TEXT = 0xFFF5F5FA;
	public static final int TEXT_SECONDARY = 0xFFA9A9B7;
	public static final int TEXT_DIM = 0xFF747482;
	public static final int GLASS = 0xD90A0B10;
	public static final int GLASS_HOVER = 0xEC171923;
	public static final int BORDER = 0x32FFFFFF;

	private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
			DreamcastClient.MOD_ID, "textures/gui/main_menu_background.png");
	private static final int BACKGROUND_WIDTH = 1920;
	private static final int BACKGROUND_HEIGHT = 1080;

	public enum Icon {
		WORLD,
		SERVERS,
		SETTINGS,
		ACCOUNT,
		MODULES,
		TELEGRAM,
		POWER
	}

	private DreamcastUi() {
	}

	/** Background cover, restrained cursor parallax and one coherent glass veil. */
	public static void drawBackdrop(GuiGraphicsExtractor graphics, int width, int height,
	                                float mouseX, float mouseY, float darkness) {
		if (width <= 0 || height <= 0) {
			return;
		}

		float cover = Math.max(width / (float) BACKGROUND_WIDTH, height / (float) BACKGROUND_HEIGHT);
		int sourceWidth = Math.max(1, Math.min(BACKGROUND_WIDTH, Math.round(width / cover)));
		int sourceHeight = Math.max(1, Math.min(BACKGROUND_HEIGHT, Math.round(height / cover)));
		float availableX = Math.max(0.0F, BACKGROUND_WIDTH - sourceWidth);
		float availableY = Math.max(0.0F, BACKGROUND_HEIGHT - sourceHeight);
		float cursorX = clamp01(mouseX / Math.max(1.0F, width));
		float cursorY = clamp01(mouseY / Math.max(1.0F, height));
		float u = availableX * (0.42F + cursorX * 0.16F);
		float v = availableY * (0.42F + cursorY * 0.16F);

		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
				0, 0, u, v, width, height,
				sourceWidth, sourceHeight, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);

		float shade = Math.max(0.0F, Math.min(0.82F, darkness));
		graphics.fill(0, 0, width, height, RenderUtils.withAlpha(0xFF03040A, shade));
		graphics.fillGradient(0, 0, width, height,
				RenderUtils.withAlpha(0xFF050511, 0.22F),
				RenderUtils.withAlpha(0xFF000106, 0.66F));

		// Vignette without a shader: four broad, very cheap edge bands.
		int edge = Math.max(24, Math.min(width, height) / 7);
		for (int i = 0; i < 4; i++) {
			int inset = edge * i / 4;
			float alpha = 0.09F * (1.0F - i / 4.0F);
			graphics.fill(inset, inset, width - inset, inset + 1,
					RenderUtils.withAlpha(0xFF000000, alpha));
			graphics.fill(inset, height - inset - 1, width - inset, height - inset,
					RenderUtils.withAlpha(0xFF000000, alpha));
		}

		drawAmbient(graphics, width, height, mouseX, mouseY, Util.getMillis());
	}

	/** Slow ambient layer. It advances from time, never from mouse events. */
	public static void drawAmbient(GuiGraphicsExtractor graphics, int width, int height,
	                               float mouseX, float mouseY, long now) {
		int accent = ClientTheme.accent(now);
		int step = Math.max(38, width / 18);
		int offset = (int) ((now / 90L) % step);
		for (int x = -step + offset; x < width + step; x += step) {
			graphics.fill(x, 0, x + 1, height, RenderUtils.withAlpha(accent, 0.018F));
		}
		for (int y = 18; y < height; y += step) {
			graphics.fill(0, y, width, y + 1, 0x08FFFFFF);
		}

		// Deterministic particles: no allocation and a fixed upper bound.
		for (int i = 0; i < 18; i++) {
			float phase = now * (0.00010F + i * 0.000003F) + i * 1.713F;
			int x = Math.floorMod(i * 137 + (int) (Math.sin(phase) * 24.0F), Math.max(1, width));
			int y = Math.floorMod(i * 83 - (int) (now / (70L + i * 3L)), Math.max(1, height));
			int size = i % 5 == 0 ? 2 : 1;
			float cursorDistance = distance(x, y, mouseX, mouseY);
			float glow = 0.12F + 0.18F * (1.0F - Math.min(1.0F, cursorDistance / 120.0F));
			graphics.fill(x, y, x + size, y + size, RenderUtils.withAlpha(accent, glow));
		}
	}

	public static void drawPageTitle(GuiGraphicsExtractor graphics, Font font, String title,
	                                 String subtitle, int width, int accent) {
		RenderUtils.textBold(graphics, font, title, 20, 15, TEXT);
		if (subtitle != null && !subtitle.isBlank()) {
			RenderUtils.textFlat(graphics, font, subtitle, 20, 15 + font.lineHeight + 3, TEXT_SECONDARY);
		}
		int lineWidth = Math.min(120, Math.max(42, RenderUtils.widthBold(font, title)));
		for (int i = 0; i < lineWidth; i++) {
			graphics.fill(20 + i, 39, 21 + i, 40,
					RenderUtils.withAlpha(RenderUtils.mix(accent, CYAN, i / (float) lineWidth), 0.75F));
		}
		graphics.fill(width - 118, 19, width - 20, 20, 0x18FFFFFF);
		RenderUtils.textFlat(graphics, font, "DREAMCAST", width - 20 - RenderUtils.width(font, "DREAMCAST"), 8,
				TEXT_DIM);
	}

	/** Circular main-menu control with a true circular visual and hit area. */
	public static void drawRoundButton(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY,
	                                   int radius, Icon icon, String label, int accent,
	                                   float hover, float appear, boolean focused) {
		float p = smoothstep(appear);
		if (p <= 0.002F) {
			return;
		}
		float h = clamp01(hover);
		int animatedRadius = Math.max(2, Math.round(radius * (0.72F + 0.28F * p) * (1.0F + h * 0.06F)));
		int cy = centerY + Math.round((1.0F - p) * 12.0F);

		if (h > 0.01F || focused) {
			RenderUtils.fillCircle(graphics, centerX, cy, animatedRadius + 7,
					RenderUtils.withAlpha(accent, (0.05F + h * 0.13F + (focused ? 0.08F : 0.0F)) * p));
		}
		RenderUtils.fillCircle(graphics, centerX, cy + 3, animatedRadius + 2,
				RenderUtils.withAlpha(0xFF000000, 0.35F * p));
		RenderUtils.fillCircle(graphics, centerX, cy, animatedRadius + 1,
				RenderUtils.withAlpha(focused ? accent : RenderUtils.mix(BORDER, accent, h), p));
		RenderUtils.fillCircle(graphics, centerX, cy, animatedRadius,
				RenderUtils.withAlpha(RenderUtils.mix(GLASS, GLASS_HOVER, h), p));

		// Thin highlight crescent.
		int highlightWidth = Math.max(8, animatedRadius);
		graphics.fill(centerX - highlightWidth / 2, cy - animatedRadius + 5,
				centerX + highlightWidth / 2, cy - animatedRadius + 6,
				RenderUtils.withAlpha(RenderUtils.mix(0xFFFFFFFF, accent, 0.4F), (0.15F + h * 0.45F) * p));

		drawIcon(graphics, icon, centerX, cy, animatedRadius, accent, p, h);
		int labelColor = RenderUtils.withAlpha(RenderUtils.mix(TEXT_SECONDARY, TEXT, h), p);
		RenderUtils.textCentered(graphics, font, label, centerX, cy + animatedRadius + 8, labelColor, false);
	}

	private static void drawIcon(GuiGraphicsExtractor graphics, Icon icon, int cx, int cy, int radius,
	                             int accent, float appear, float hover) {
		int color = RenderUtils.withAlpha(RenderUtils.mix(TEXT, accent, 0.22F + hover * 0.45F), appear);
		int dim = RenderUtils.withAlpha(RenderUtils.mix(TEXT_SECONDARY, accent, hover * 0.6F), appear);
		int s = Math.max(7, Math.round(radius * 0.42F));
		switch (icon) {
			case WORLD -> {
				RenderUtils.fillRoundedBorder(graphics, cx - s, cy - s + 1, s * 2, s * 2, 4, color,
						RenderUtils.withAlpha(accent, 0.13F * appear));
				graphics.fill(cx - s + 4, cy + 1, cx + s - 3, cy + 3, dim);
				graphics.fill(cx - 1, cy - s + 4, cx + 1, cy + s - 3, dim);
			}
			case SERVERS -> {
				RenderUtils.fillCircle(graphics, cx - 6, cy - 5, 4, color);
				RenderUtils.fillCircle(graphics, cx + 6, cy - 5, 4, color);
				RenderUtils.fillRounded(graphics, cx - 12, cy + 1, 11, 7, 3, dim);
				RenderUtils.fillRounded(graphics, cx + 1, cy + 1, 11, 7, 3, dim);
			}
			case SETTINGS -> {
				RenderUtils.fillCircle(graphics, cx, cy, s, color);
				RenderUtils.fillCircle(graphics, cx, cy, Math.max(3, s - 4), GLASS_HOVER);
				graphics.fill(cx - 1, cy - s - 4, cx + 2, cy - s + 2, color);
				graphics.fill(cx - 1, cy + s - 2, cx + 2, cy + s + 4, color);
				graphics.fill(cx - s - 4, cy - 1, cx - s + 2, cy + 2, color);
				graphics.fill(cx + s - 2, cy - 1, cx + s + 4, cy + 2, color);
			}
			case ACCOUNT -> {
				RenderUtils.fillCircle(graphics, cx, cy - 6, 6, color);
				RenderUtils.fillRounded(graphics, cx - 11, cy + 2, 22, 9, 5, dim);
			}
			case MODULES -> {
				for (int row = 0; row < 2; row++) {
					for (int col = 0; col < 2; col++) {
						RenderUtils.fillRounded(graphics, cx - 10 + col * 12, cy - 10 + row * 12,
								8, 8, 2, row == col ? color : dim);
					}
				}
			}
			case TELEGRAM -> {
				// Original paper-plane glyph, built from short pixel lines.
				drawLine(graphics, cx - 12, cy - 5, cx + 12, cy - 11, color, 2);
				drawLine(graphics, cx + 12, cy - 11, cx + 3, cy + 12, color, 2);
				drawLine(graphics, cx + 3, cy + 12, cx - 2, cy + 3, dim, 2);
				drawLine(graphics, cx - 2, cy + 3, cx - 12, cy - 5, dim, 2);
				drawLine(graphics, cx - 2, cy + 3, cx + 12, cy - 11, color, 1);
			}
			case POWER -> {
				RenderUtils.fillCircle(graphics, cx, cy + 2, s, color);
				RenderUtils.fillCircle(graphics, cx, cy + 2, Math.max(3, s - 3), GLASS_HOVER);
				graphics.fill(cx - 2, cy - s - 3, cx + 2, cy + 3, color);
			}
		}
	}

	private static void drawLine(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1,
	                             int color, int thickness) {
		int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
		if (steps == 0) {
			graphics.fill(x0, y0, x0 + thickness, y0 + thickness, color);
			return;
		}
		for (int i = 0; i <= steps; i++) {
			float t = i / (float) steps;
			int x = Math.round(x0 + (x1 - x0) * t);
			int y = Math.round(y0 + (y1 - y0) * t);
			graphics.fill(x, y, x + thickness, y + thickness, color);
		}
	}

	public static void drawScaledTextCentered(GuiGraphicsExtractor graphics, Font font, String text,
	                                          float centerX, float y, float scale, int color, boolean bold) {
		float safeScale = Math.max(0.1F, scale);
		int textWidth = bold ? RenderUtils.widthBold(font, text) : RenderUtils.width(font, text);
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, y);
		graphics.pose().scale(safeScale, safeScale);
		graphics.text(font, bold ? RenderUtils.styledBold(text) : RenderUtils.styled(text),
				-textWidth / 2, 0, color, false);
		graphics.pose().popMatrix();
	}

	/**
	 * Allowlist for vanilla screens which may receive the Dreamcast background.
	 * Container/gameplay screens are explicitly excluded.
	 */
	public static boolean shouldThemeExternal(Screen screen) {
		if (screen == null || screen instanceof DreamcastScreen || screen instanceof AbstractContainerScreen<?>
				|| screen instanceof ChatScreen || screen.isInGameUi()) {
			return false;
		}
		String name = screen.getClass().getName();
		return name.startsWith("net.minecraft.client.gui.screens.options.")
				|| name.startsWith("net.minecraft.client.gui.screens.worldselection.")
				|| name.startsWith("net.minecraft.client.gui.screens.multiplayer.")
				|| name.startsWith("net.minecraft.client.gui.screens.packs.")
				|| name.equals("net.minecraft.client.gui.screens.ManageServerScreen")
				|| name.equals("net.minecraft.client.gui.screens.ConfirmScreen")
				|| name.contains("LanguageSelect")
				|| name.contains("Accessibility");
	}

	public static void drawExternalBackdrop(GuiGraphicsExtractor graphics) {
		Minecraft client = Minecraft.getInstance();
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		float mouseX = width * 0.5F;
		float mouseY = height * 0.5F;
		if (client != null && client.getWindow() != null) {
			mouseX = (float) (client.mouseHandler.xpos() * width / Math.max(1, client.getWindow().getScreenWidth()));
			mouseY = (float) (client.mouseHandler.ypos() * height / Math.max(1, client.getWindow().getScreenHeight()));
		}
		drawBackdrop(graphics, width, height, mouseX, mouseY, 0.54F);
	}

	public static float smoothstep(float value) {
		float t = clamp01(value);
		return t * t * (3.0F - 2.0F * t);
	}

	public static float smootherstep(float value) {
		float t = clamp01(value);
		return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
	}

	public static float distance(float x0, float y0, float x1, float y1) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	public static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}
}
