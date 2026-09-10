package com.dreamcast.client.gui.screens;

import com.dreamcast.client.DreamcastClient;
import com.dreamcast.client.gui.theme.DreamcastUi;
import com.dreamcast.client.util.RenderUtils;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.validation.ContentValidationException;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** Full-screen responsive world gallery with square, rounded preview cards. */
public final class DreamcastWorldsScreen extends DreamcastScreen {

	private static final int ACCENT = 0xFF7C6CFF;
	private static final int OUTER_MARGIN = 14;
	private static final int GRID_TOP = 52;
	private static final int GRID_BOTTOM = 47;
	private static final int CARD_GAP = 9;
	private static final int MIN_CARD_WIDTH = 112;
	private static final int MAX_COLUMNS = 7;
	private static final DateTimeFormatter LAST_PLAYED_FORMAT = DateTimeFormatter.ofPattern(
			"d MMM yyyy · HH:mm", Locale.forLanguageTag("ru"));

	private static final class WorldCard {
		final LevelSummary summary;
		final FaviconTexture icon;
		float hover;
		float appear;
		int x = -1;
		int y;
		int width;
		int height;

		WorldCard(LevelSummary summary, FaviconTexture icon) {
			this.summary = summary;
			this.icon = icon;
		}

		boolean contains(double mouseX, double mouseY) {
			return x >= 0 && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}
	}

	private record GridLayout(int x, int y, int width, int height, int columns,
	                          int cardSize, int visibleRows) {
	}

	private final Screen parent;
	private final List<WorldCard> cards = new ArrayList<>();
	private final com.dreamcast.client.util.Generation generation = new com.dreamcast.client.util.Generation();
	private int selected = -1;
	private int scrollRow;
	private boolean loading = true;
	private boolean failure;
	private boolean confirmDelete;
	private GridLayout lastLayout;

	public DreamcastWorldsScreen(Screen parent) {
		super("Одиночная игра");
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		if (loading && !failure && cards.isEmpty()) {
			loadWorlds();
		}
	}

	private void loadWorlds() {
		loading = true;
		final com.dreamcast.client.util.Generation.Ticket ticket = generation.start();
		CompletableFuture.supplyAsync(() -> {
			var candidates = this.minecraft.getLevelSource().findLevelCandidates();
			return this.minecraft.getLevelSource().loadLevelSummaries(candidates).join();
		}).whenComplete((summaries, error) -> this.minecraft.execute(() -> {
			if (!generation.valid(ticket)) {
				return;
			}
			closeIcons();
			cards.clear();
			selected = -1;
			scrollRow = 0;
			confirmDelete = false;
			if (error != null) {
				DreamcastClient.LOGGER.error("Не удалось прочитать список миров", error);
				failure = true;
				loading = false;
				return;
			}

			List<LevelSummary> sorted = new ArrayList<>(summaries);
			sorted.sort(Comparator.comparingLong(LevelSummary::getLastPlayed).reversed());
			for (LevelSummary summary : sorted) {
				cards.add(new WorldCard(summary,
						FaviconTexture.forWorld(this.minecraft.getTextureManager(), summary.getLevelId())));
			}
			if (!cards.isEmpty()) {
				selected = 0;
			}
			loading = false;
			uploadIcons();
		}));
	}

	/** Reads files off-thread, uploads GPU textures only on the client thread. */
	private void uploadIcons() {
		final com.dreamcast.client.util.Generation.Ticket ticket = generation.start();
		List<WorldCard> snapshot = new ArrayList<>(cards);
		List<WorldCard> ready = new ArrayList<>();
		List<NativeImage> images = new ArrayList<>();
		CompletableFuture.runAsync(() -> {
			for (WorldCard card : snapshot) {
				Path iconFile = card.summary.getIcon();
				if (iconFile == null || !Files.isRegularFile(iconFile)) {
					continue;
				}
				try (InputStream stream = Files.newInputStream(iconFile)) {
					NativeImage image = NativeImage.read(stream);
					if (image.getWidth() == 64 && image.getHeight() == 64) {
						ready.add(card);
						images.add(image);
					} else {
						image.close();
					}
				} catch (Throwable error) {
					DreamcastClient.LOGGER.warn("Некорректная иконка мира {}", card.summary.getLevelId(), error);
				}
			}
		}).whenComplete((unused, error) -> this.minecraft.execute(() -> {
			if (!generation.valid(ticket)) {
				images.forEach(NativeImage::close);
				return;
			}
			for (int i = 0; i < ready.size(); i++) {
				try {
					ready.get(i).icon.upload(images.get(i));
				} catch (Throwable uploadError) {
					images.get(i).close();
				}
			}
		}));
	}

	@Override
	public void removed() {
		generation.invalidate();
		closeIcons();
		super.removed();
	}

	private void closeIcons() {
		for (WorldCard card : cards) {
			if (!card.icon.isClosed()) {
				card.icon.close();
			}
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		drawDarkBackdrop(graphics);
		String subtitle = loading ? "загружаем миры…" : cards.size() + " мир(ов) · двойной клик для входа";
		DreamcastUi.drawPageTitle(graphics, font, "Одиночная игра", subtitle, width, ACCENT);

		GridLayout layout = computeLayout();
		lastLayout = layout;
		drawGlassPanel(graphics, layout.x(), layout.y(), layout.width(), layout.height(), 13, 1.0F, ACCENT);

		for (WorldCard card : cards) {
			card.x = -1;
		}
		if (failure) {
			drawEmptyState(graphics, layout, "Не удалось прочитать список миров",
					"Подробности сохранены в latest.log", 0xFFFF8095);
		} else if (loading) {
			drawLoading(graphics, layout);
		} else if (cards.isEmpty()) {
			drawEmptyState(graphics, layout, "Здесь пока пусто",
					"Создай первый мир — он появится отдельной карточкой", DreamcastUi.TEXT);
		} else {
			drawCards(graphics, layout, mouseX, mouseY);
		}

		drawActions(graphics, mouseX, mouseY);
		RenderUtils.drawClickWaves(graphics, ACCENT);
	}

	private GridLayout computeLayout() {
		int panelX = OUTER_MARGIN;
		int panelY = GRID_TOP;
		int panelWidth = Math.max(80, width - OUTER_MARGIN * 2);
		int panelHeight = Math.max(64, height - GRID_TOP - GRID_BOTTOM);
		int innerWidth = Math.max(60, panelWidth - 18);
		int columns = Math.max(1, Math.min(MAX_COLUMNS,
				(innerWidth + CARD_GAP) / (MIN_CARD_WIDTH + CARD_GAP)));
		int cardSize = Math.max(82, (innerWidth - CARD_GAP * (columns - 1)) / columns);
		int visibleRows = Math.max(1, (Math.max(1, panelHeight - 16) + CARD_GAP) / (cardSize + CARD_GAP));
		int totalRows = (cards.size() + columns - 1) / columns;
		scrollRow = Math.max(0, Math.min(Math.max(0, totalRows - visibleRows), scrollRow));
		return new GridLayout(panelX, panelY, panelWidth, panelHeight, columns, cardSize, visibleRows);
	}

	private void drawCards(GuiGraphicsExtractor graphics, GridLayout layout, int mouseX, int mouseY) {
		int innerX = layout.x() + 9;
		int innerY = layout.y() + 8;
		int firstIndex = scrollRow * layout.columns();
		int endIndex = Math.min(cards.size(), firstIndex + layout.visibleRows() * layout.columns());
		graphics.enableScissor(layout.x() + 2, layout.y() + 2,
				layout.x() + layout.width() - 2, layout.y() + layout.height() - 2);
		for (int index = firstIndex; index < endIndex; index++) {
			int local = index - firstIndex;
			int column = local % layout.columns();
			int row = local / layout.columns();
			int x = innerX + column * (layout.cardSize() + CARD_GAP);
			int y = innerY + row * (layout.cardSize() + CARD_GAP);
			drawCard(graphics, cards.get(index), index, x, y, layout.cardSize(), mouseX, mouseY);
		}
		graphics.disableScissor();

		int totalRows = (cards.size() + layout.columns() - 1) / layout.columns();
		if (totalRows > layout.visibleRows()) {
			drawGridScrollbar(graphics, layout, totalRows);
		}
	}

	private void drawCard(GuiGraphicsExtractor graphics, WorldCard card, int index,
	                      int x, int y, int size, int mouseX, int mouseY) {
		card.x = x;
		card.y = y;
		card.width = size;
		card.height = size;
		boolean inside = card.contains(mouseX, mouseY);
		card.hover = ease(card.hover, inside ? 1.0F : 0.0F, 0.20F);
		card.appear = ease(card.appear, 1.0F, 0.18F);
		float appear = DreamcastUi.smoothstep(card.appear);
		boolean selectedCard = index == selected;
		int cardY = y + Math.round((1.0F - appear) * 9.0F) - Math.round(card.hover * 2.0F);
		int fill = RenderUtils.mix(0xE20A0B10, 0xF0191B25, card.hover);
		int border = selectedCard
				? RenderUtils.withAlpha(ACCENT, 0.72F + card.hover * 0.24F)
				: RenderUtils.mix(0x24FFFFFF, ACCENT, card.hover * 0.42F);
		if (card.hover > 0.02F || selectedCard) {
			RenderUtils.fillRounded(graphics, x - 3, cardY - 3, size + 6, size + 7, 13,
					RenderUtils.withAlpha(ACCENT, (selectedCard ? 0.08F : 0.025F) + card.hover * 0.07F));
		}
		RenderUtils.fillRoundedBorder(graphics, x, cardY, size, size, 11,
				RenderUtils.withAlpha(border, appear), RenderUtils.withAlpha(fill, appear));

		int previewSize = Math.max(38, Math.min(size - 16, size - 47));
		int previewX = x + (size - previewSize) / 2;
		int previewY = cardY + 8;
		int imageBackdrop = RenderUtils.withAlpha(0xFF111522, appear);
		RenderUtils.fillRounded(graphics, previewX - 2, previewY - 2, previewSize + 4, previewSize + 4,
				8, RenderUtils.withAlpha(RenderUtils.mix(ACCENT, DreamcastUi.CYAN, index % 5 / 4.0F), 0.20F * appear));
		graphics.blit(RenderPipelines.GUI_TEXTURED, card.icon.textureLocation(),
				previewX, previewY, 0.0F, 0.0F, previewSize, previewSize, 64, 64);
		maskRoundedImageCorners(graphics, previewX, previewY, previewSize, 6, imageBackdrop);
		RenderUtils.fillRoundedBorder(graphics, previewX - 1, previewY - 1, previewSize + 2, previewSize + 2,
				7, RenderUtils.withAlpha(0x66FFFFFF, appear), 0x00000000);

		int textY = previewY + previewSize + 5;
		String name = RenderUtils.clamp(font, card.summary.getLevelName(), size - 14);
		RenderUtils.textCentered(graphics, font, name, x + size / 2, textY,
				RenderUtils.withAlpha(selectedCard ? 0xFFFFFFFF : DreamcastUi.TEXT, appear), false);
		String details = shortDate(card.summary.getLastPlayed());
		if (!card.summary.isCompatible()) {
			details = "другая версия";
		} else if (card.summary.isLocked()) {
			details = "мир занят";
		}
		int detailColor = !card.summary.isCompatible() ? 0xFFFF8095 : DreamcastUi.TEXT_DIM;
		RenderUtils.textCentered(graphics, font, RenderUtils.clamp(font, details, size - 14),
				x + size / 2, Math.min(cardY + size - font.lineHeight - 5, textY + font.lineHeight + 2),
				RenderUtils.withAlpha(detailColor, appear), false);
	}

	private void drawActions(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		chips.clear();
		if (confirmDelete && selected >= 0) {
			chips.add(chip("Удалить мир", this::deleteSelected, true));
			chips.add(chip("Отмена", () -> confirmDelete = false));
		} else {
			LevelSummary selection = selected >= 0 && selected < cards.size() ? cards.get(selected).summary : null;
			Chip play = chip("Играть", () -> joinWorld(selection));
			play.enabled = selection != null && selection.primaryActionActive();
			chips.add(play);
			chips.add(chip("Создать", this::createWorld));
			Chip edit = chip("Изменить", () -> editWorld(selection));
			edit.enabled = selection != null;
			chips.add(edit);
			Chip delete = chip("Удалить", () -> confirmDelete = true);
			delete.enabled = selection != null;
			delete.danger = true;
			chips.add(delete);
		}
		chips.add(chip("Назад", this::onClose));
		drawChipRow(graphics, width / 2, height - 34, 20, 5, ACCENT, mouseX, mouseY);
	}

	private void drawLoading(GuiGraphicsExtractor graphics, GridLayout layout) {
		int centerX = layout.x() + layout.width() / 2;
		int centerY = layout.y() + layout.height() / 2;
		long now = net.minecraft.util.Util.getMillis();
		for (int i = 0; i < 3; i++) {
			float pulse = 0.35F + 0.65F * (float) Math.max(0.0,
					Math.sin(now / 210.0 - i * 0.75));
			RenderUtils.fillCircle(graphics, centerX - 12 + i * 12, centerY - 7, 3.0F,
					RenderUtils.withAlpha(ACCENT, pulse));
		}
		RenderUtils.textCentered(graphics, font, "Читаем сохранения", centerX, centerY + 8,
				DreamcastUi.TEXT_SECONDARY, false);
	}

	private void drawEmptyState(GuiGraphicsExtractor graphics, GridLayout layout,
	                            String title, String subtitle, int titleColor) {
		int centerX = layout.x() + layout.width() / 2;
		int centerY = layout.y() + layout.height() / 2;
		RenderUtils.fillCircle(graphics, centerX, centerY - 18, 17,
				RenderUtils.withAlpha(ACCENT, 0.12F));
		RenderUtils.textCentered(graphics, font, "+", centerX, centerY - 23, ACCENT, false);
		RenderUtils.textCentered(graphics, font, title, centerX, centerY + 6, titleColor, false);
		RenderUtils.textCentered(graphics, font, subtitle, centerX, centerY + 20,
				DreamcastUi.TEXT_DIM, false);
	}

	private void drawGridScrollbar(GuiGraphicsExtractor graphics, GridLayout layout, int totalRows) {
		int trackX = layout.x() + layout.width() - 5;
		int trackY = layout.y() + 8;
		int trackHeight = layout.height() - 16;
		RenderUtils.fillRounded(graphics, trackX, trackY, 2, trackHeight, 1, 0x28FFFFFF);
		int thumb = Math.max(16, trackHeight * layout.visibleRows() / totalRows);
		int maxScroll = Math.max(1, totalRows - layout.visibleRows());
		int thumbY = trackY + (trackHeight - thumb) * scrollRow / maxScroll;
		RenderUtils.fillRounded(graphics, trackX, thumbY, 2, thumb, 1,
				RenderUtils.withAlpha(ACCENT, 0.85F));
	}

	/** Paints only the outside pixels of four image corners, simulating a rounded clip. */
	private static void maskRoundedImageCorners(GuiGraphicsExtractor graphics, int x, int y,
	                                            int size, int radius, int color) {
		for (int row = 0; row < radius; row++) {
			float dy = radius - row - 0.5F;
			int inset = Math.max(0, (int) Math.ceil(radius - Math.sqrt(Math.max(0.0F, radius * radius - dy * dy))));
			if (inset > 0) {
				graphics.fill(x, y + row, x + inset, y + row + 1, color);
				graphics.fill(x + size - inset, y + row, x + size, y + row + 1, color);
				graphics.fill(x, y + size - row - 1, x + inset, y + size - row, color);
				graphics.fill(x + size - inset, y + size - row - 1, x + size, y + size - row, color);
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		RenderUtils.addClickWave(event.x(), event.y());
		if (clickChips(event)) {
			return true;
		}
		for (int i = 0; i < cards.size(); i++) {
			WorldCard card = cards.get(i);
			if (card.contains(event.x(), event.y())) {
				selected = i;
				confirmDelete = false;
				playClick();
				if (doubleClick) {
					joinWorld(card.summary);
				}
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		GridLayout layout = lastLayout != null ? lastLayout : computeLayout();
		int totalRows = (cards.size() + layout.columns() - 1) / layout.columns();
		int max = Math.max(0, totalRows - layout.visibleRows());
		scrollRow = Math.max(0, Math.min(max, scrollRow - (int) Math.signum(scrollY)));
		return true;
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.gui.setScreen(parent);
		}
	}

	private void reopen() {
		if (this.minecraft != null) {
			this.minecraft.gui.setScreen(new DreamcastWorldsScreen(parent));
		}
	}

	private void joinWorld(@Nullable LevelSummary summary) {
		if (summary == null || !summary.primaryActionActive()) {
			return;
		}
		if (summary instanceof LevelSummary.SymlinkLevelSummary) {
			this.minecraft.gui.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(this::reopen));
			return;
		}
		this.minecraft.createWorldOpenFlows().openWorld(summary.getLevelId(), this::reopen);
	}

	private void createWorld() {
		CreateWorldScreen.openFresh(this.minecraft, this::reopen);
	}

	private void editWorld(@Nullable LevelSummary summary) {
		if (summary == null) {
			return;
		}
		String levelId = summary.getLevelId();
		LevelStorageSource.LevelStorageAccess access;
		try {
			access = this.minecraft.getLevelSource().validateAndCreateAccess(levelId);
		} catch (IOException error) {
			SystemToast.onWorldAccessFailure(this.minecraft, levelId);
			DreamcastClient.LOGGER.error("Не удалось открыть мир {}", levelId, error);
			return;
		} catch (ContentValidationException error) {
			DreamcastClient.LOGGER.warn("{}", error.getMessage());
			this.minecraft.gui.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(this::reopen));
			return;
		}

		try {
			EditWorldScreen editScreen = EditWorldScreen.create(this.minecraft, access, result -> {
				access.safeClose();
				reopen();
			});
			this.minecraft.gui.setScreen(editScreen);
		} catch (NbtException | ReportedNbtException | IOException error) {
			access.safeClose();
			SystemToast.onWorldAccessFailure(this.minecraft, levelId);
			DreamcastClient.LOGGER.error("Не удалось прочитать данные мира {}", levelId, error);
			reopen();
		}
	}

	private void deleteSelected() {
		if (selected < 0 || selected >= cards.size()) {
			confirmDelete = false;
			return;
		}
		String levelId = cards.get(selected).summary.getLevelId();
		try (LevelStorageSource.LevelStorageAccess access = this.minecraft.getLevelSource().createAccess(levelId)) {
			access.deleteLevel();
		} catch (IOException error) {
			SystemToast.onWorldDeleteFailure(this.minecraft, levelId);
			DreamcastClient.LOGGER.error("Не удалось удалить мир {}", levelId, error);
		}
		confirmDelete = false;
		reopen();
	}

	private static String shortDate(long millis) {
		if (millis <= 0L) {
			return "дата неизвестна";
		}
		ZonedDateTime time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
		return LAST_PLAYED_FORMAT.format(time);
	}
}
