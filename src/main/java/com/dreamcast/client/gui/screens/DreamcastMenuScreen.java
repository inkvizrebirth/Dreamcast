package com.dreamcast.client.gui.screens;

import com.dreamcast.client.DreamcastClient;
import com.dreamcast.client.gui.ClickGuiScreen;
import com.dreamcast.client.gui.theme.ClientTheme;
import com.dreamcast.client.gui.theme.DreamcastUi;
import com.dreamcast.client.gui.theme.DreamcastUi.Icon;
import com.dreamcast.client.util.FileOpener;
import com.dreamcast.client.util.RenderUtils;
import com.dreamcast.client.util.ViaIntegration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cinematic Dreamcast home screen.
 *
 * <p>On the first visit in a client session the real clock appears in the
 * centre, glides into the header and reveals the circular navigation. Later
 * returns use a short reveal and do not replay the long intro.</p>
 */
public final class DreamcastMenuScreen extends DreamcastScreen {

	private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(
			"EEEE, d MMMM", Locale.forLanguageTag("ru"));
	private static boolean introPlayed;
	private static long cachedSecond = Long.MIN_VALUE;
	private static String cachedClock = "00:00";
	private static String cachedDate = "";

	private static final class RoundAction {
		final String label;
		final Icon icon;
		final Runnable action;
		float hover;
		float appear;
		int centerX;
		int centerY;
		int radius;

		RoundAction(String label, Icon icon, Runnable action) {
			this.label = label;
			this.icon = icon;
			this.action = action;
		}
	}

	private final List<RoundAction> actions = new ArrayList<>();
	private long introStarted;
	private boolean fullIntro;
	private int keyboardFocus;

	public DreamcastMenuScreen() {
		super(DreamcastClient.MOD_NAME);
		actions.add(new RoundAction("Миры", Icon.WORLD,
				() -> this.minecraft.gui.setScreen(new DreamcastWorldsScreen(this))));
		actions.add(new RoundAction("Серверы", Icon.SERVERS,
				() -> this.minecraft.gui.setScreen(new DreamcastServersScreen(this))));
		actions.add(new RoundAction("Настройки", Icon.SETTINGS,
				() -> this.minecraft.gui.setScreen(new DreamcastSettingsScreen(this))));
		actions.add(new RoundAction("Аккаунты", Icon.ACCOUNT,
				() -> this.minecraft.gui.setScreen(new DreamcastAltsScreen(this))));
		actions.add(new RoundAction("Автоматизатор", Icon.MODULES,
				() -> this.minecraft.gui.setScreen(new ClickGuiScreen(this))));
		actions.add(new RoundAction("Telegram", Icon.TELEGRAM,
				() -> FileOpener.openUrl("https://t.me/inkviz01")));
		actions.add(new RoundAction("Выход", Icon.POWER,
				() -> this.minecraft.stop()));
	}

	@Override
	public void added() {
		super.added();
		this.fullIntro = !introPlayed;
		introPlayed = true;
		this.introStarted = Util.getMillis();
		this.keyboardFocus = 0;
		for (RoundAction action : actions) {
			action.hover = 0.0F;
			action.appear = 0.0F;
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		long now = Util.getMillis();
		long elapsed = fullIntro ? Math.max(0L, now - introStarted) : Math.max(0L, now - introStarted + 2_200L);
		int accent = ClientTheme.accent(now);
		DreamcastUi.drawBackdrop(graphics, width, height, mouseX, mouseY, 0.34F);
		refreshClock(now);
		drawLogoFeature(graphics, mouseX, mouseY, elapsed, accent);

		float clockIn = DreamcastUi.smootherstep(elapsed / 520.0F);
		float clockMove = DreamcastUi.smootherstep((elapsed - 1_250L) / 850.0F);
		float clockScale = lerp(2.45F, 1.28F, clockMove);
		float clockY = lerp(height * 0.47F, 18.0F, clockMove);

		// Low-cost halo: a few circles, no blur shader or per-frame texture work.
		float halo = clockIn * (1.0F - 0.55F * clockMove);
		if (halo > 0.01F) {
			RenderUtils.fillCircle(graphics, width / 2.0F, clockY + 12.0F, 62.0F * halo,
					RenderUtils.withAlpha(accent, 0.035F * halo));
		}
		DreamcastUi.drawScaledTextCentered(graphics, font, cachedClock, width / 2.0F, clockY,
				clockScale, RenderUtils.withAlpha(DreamcastUi.TEXT, clockIn), true);

		int dateY = Math.round(clockY + font.lineHeight * clockScale + 7.0F);
		RenderUtils.textCentered(graphics, font, cachedDate, width / 2, dateY,
				RenderUtils.withAlpha(DreamcastUi.TEXT_SECONDARY, clockIn * (0.88F + 0.12F * clockMove)), false);
		if (clockMove > 0.45F) {
			float brandAlpha = DreamcastUi.smoothstep((clockMove - 0.45F) / 0.55F);
			String brand = DreamcastClient.LOGO_TEXT;
			RenderUtils.drawTrackedBold(graphics, font, brand,
					width / 2 - RenderUtils.trackedWidthBold(font, brand, 3) / 2,
					dateY + font.lineHeight + 4,
					RenderUtils.withAlpha(accent, brandAlpha * 0.82F), 3);
		}

		drawNavigation(graphics, mouseX, mouseY, elapsed);

		String version = DreamcastClient.MOD_NAME + " " + DreamcastClient.MOD_VERSION
				+ "  ·  Minecraft " + ViaIntegration.currentVersionLabel();
		RenderUtils.textFlat(graphics, font, version, 8, height - 13, 0xB8A9A9B7);
		String hint = fullIntro && elapsed < 1_900L ? "ESC — пропустить" : "стрелки / Enter";
		RenderUtils.textFlat(graphics, font, hint,
				width - 8 - RenderUtils.width(font, hint), height - 13, 0x8F8D8D9A);
		RenderUtils.drawClickWaves(graphics, accent);
	}

	private void drawLogoFeature(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
	                            long elapsed, int accent) {
		// On compact GUI scales the navigation needs all available width; keep the
		// mark as a wide-screen brand element instead of letting it collide with it.
		if (width < 900) {
			return;
		}

		float appear = DreamcastUi.smootherstep((elapsed - 620L) / 820.0F);
		if (appear <= 0.002F) {
			return;
		}
		int size = Math.max(104, Math.min(220, Math.min(width, height) / 4));
		int margin = Math.max(42, width / 18);
		int centerX = width - margin - size / 2;
		int centerY = Math.max(size / 2 + 8, height / 2 - size / 2 - 34);
		float cursorOffset = (mouseX - width * 0.5F) / Math.max(1.0F, width) * 7.0F;
		centerX += Math.round(cursorOffset);
		centerY += Math.round((mouseY - height * 0.5F) / Math.max(1.0F, height) * 4.0F);

		float pulse = 0.5F + 0.5F * (float) Math.sin(Util.getMillis() / 1_400.0F);
		RenderUtils.fillCircle(graphics, centerX, centerY, size * 0.46F,
				RenderUtils.withAlpha(accent, (0.035F + pulse * 0.025F) * appear));
		RenderUtils.fillCircle(graphics, centerX, centerY, size * 0.30F,
				RenderUtils.withAlpha(0xFF0A1024, 0.20F * appear));
		DreamcastUi.drawLogo(graphics, centerX, centerY, size, 0.92F * appear);

		String label = "AUTOMATION CORE";
		RenderUtils.drawTracked(graphics, font, label,
				centerX - RenderUtils.trackedWidth(font, label, 2) / 2,
				centerY + size / 2 + 10,
				RenderUtils.withAlpha(DreamcastUi.TEXT_SECONDARY, 0.72F * appear), 2);
	}

	private void drawNavigation(GuiGraphicsExtractor graphics, int mouseX, int mouseY, long elapsed) {
		int count = actions.size();
		// Four columns also fit Minecraft's 320px minimum GUI width and keep
		// the third row from falling behind the footer on short windows.
		int columns = width >= 620 ? count : width >= 300 ? 4 : 3;
		int rows = (count + columns - 1) / columns;
		int radius = Math.max(20, Math.min(29, width / Math.max(16, columns * 5)));
		// Labels such as "Серверы" and "Настройки" must not visually merge on
		// medium GUI scales. Keep a semantic gap even when the circles shrink.
		int horizontalStep = Math.max(70, radius * 2 + Math.max(14, radius / 2));
		int verticalStep = radius * 2 + 28;
		int blockHeight = rows * verticalStep - 28;
		int startY = Math.max(104, height / 2 - blockHeight / 2 + 34);

		for (int row = 0; row < rows; row++) {
			int first = row * columns;
			int inRow = Math.min(columns, count - first);
			int rowWidth = (inRow - 1) * horizontalStep;
			int startX = width / 2 - rowWidth / 2;
			for (int col = 0; col < inRow; col++) {
				int index = first + col;
				RoundAction action = actions.get(index);
				action.centerX = startX + col * horizontalStep;
				action.centerY = startY + row * verticalStep;
				action.radius = radius;

				float reveal = DreamcastUi.smootherstep((elapsed - 1_520L - index * 65L) / 520.0F);
				action.appear = ease(action.appear, reveal, 0.24F);
				float distance = DreamcastUi.distance(mouseX, mouseY, action.centerX, action.centerY);
				float near = DreamcastUi.clamp01(1.0F - (distance - radius) / 38.0F);
				float targetHover = distance <= radius ? 1.0F : near * 0.42F;
				action.hover = ease(action.hover, targetHover, 0.20F);
			}
		}

		drawNavigationRails(graphics, columns, rows, count, elapsed);
		int firstAccent = ClientTheme.first();
		int secondAccent = ClientTheme.second();
		for (int index = 0; index < count; index++) {
			RoundAction action = actions.get(index);
			int actionAccent = RenderUtils.mix(firstAccent, secondAccent,
					index / (float) Math.max(1, count - 1));
			DreamcastUi.drawRoundButton(graphics, font, action.centerX, action.centerY, radius,
					action.icon, action.label, actionAccent, action.hover, action.appear,
					index == keyboardFocus);
		}
	}

	private void drawNavigationRails(GuiGraphicsExtractor graphics, int columns, int rows,
	                                 int count, long elapsed) {
		float appear = DreamcastUi.smootherstep((elapsed - 1_410L) / 520.0F);
		if (appear <= 0.002F) {
			return;
		}
		int railColor = RenderUtils.withAlpha(ClientTheme.accent(), 0.20F * appear);
		for (int row = 0; row < rows; row++) {
			int first = row * columns;
			int inRow = Math.min(columns, count - first);
			if (inRow < 2) {
				continue;
			}
			RoundAction start = actions.get(first);
			RoundAction end = actions.get(first + inRow - 1);
			graphics.fill(start.centerX, start.centerY, end.centerX + 1, end.centerY + 1, railColor);
			for (int index = first; index < first + inRow; index++) {
				RoundAction action = actions.get(index);
				RenderUtils.fillCircle(graphics, action.centerX, action.centerY, 2.0F,
						RenderUtils.withAlpha(0xFFFFFFFF, 0.34F * appear));
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		RenderUtils.addClickWave(event.x(), event.y());
		if (event.button() != 0) {
			return super.mouseClicked(event, doubleClick);
		}
		for (int i = 0; i < actions.size(); i++) {
			RoundAction action = actions.get(i);
			if (action.appear > 0.72F
					&& DreamcastUi.distance((float) event.x(), (float) event.y(), action.centerX, action.centerY)
					<= action.radius + 2.0F) {
				keyboardFocus = i;
				playClick();
				action.action.run();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (fullIntro && Util.getMillis() - introStarted < 2_100L && event.key() == GLFW.GLFW_KEY_ESCAPE) {
			fullIntro = false;
			introStarted = Util.getMillis() - 2_200L;
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_RIGHT || event.key() == GLFW.GLFW_KEY_DOWN
				|| event.key() == GLFW.GLFW_KEY_TAB) {
			keyboardFocus = (keyboardFocus + 1) % actions.size();
			playClick();
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_UP) {
			keyboardFocus = (keyboardFocus - 1 + actions.size()) % actions.size();
			playClick();
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
			playClick();
			actions.get(keyboardFocus).action.run();
			return true;
		}
		return super.keyPressed(event);
	}

	private static void refreshClock(long now) {
		long second = now / 1_000L;
		if (second == cachedSecond) {
			return;
		}
		cachedSecond = second;
		var time = Instant.now().atZone(ZoneId.systemDefault());
		cachedClock = CLOCK_FORMAT.format(time);
		cachedDate = DATE_FORMAT.format(time);
		if (!cachedDate.isEmpty()) {
			cachedDate = Character.toUpperCase(cachedDate.charAt(0)) + cachedDate.substring(1);
		}
	}

	private static float lerp(float from, float to, float progress) {
		return from + (to - from) * DreamcastUi.clamp01(progress);
	}
}
