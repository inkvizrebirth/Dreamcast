package com.dreamcast.client.gui.screens;

import com.dreamcast.client.DreamcastClient;
import com.dreamcast.client.gui.theme.DreamcastUi;
import com.dreamcast.client.util.Notifications;
import com.dreamcast.client.util.RenderUtils;
import com.dreamcast.client.util.ViaIntegration;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.EventLoopGroupHolder;
import org.jspecify.annotations.Nullable;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Full-screen responsive multiplayer gallery with isolated, cancellable pings. */
public final class DreamcastServersScreen extends DreamcastScreen {

	private static final int ACCENT = 0xFF45E3FF;
	private static final int OUTER_MARGIN = 14;
	private static final int GRID_TOP = 52;
	private static final int GRID_BOTTOM = 47;
	private static final int CARD_GAP = 9;
	private static final int MIN_CARD_WIDTH = 112;
	private static final int MAX_COLUMNS = 7;

	private static final class ServerCard {
		final ServerData data;
		final FaviconTexture icon;
		float hover;
		float appear;
		float flash;
		int x = -1;
		int y;
		int width;
		int height;
		byte @Nullable [] uploadedIcon;

		ServerCard(ServerData data, FaviconTexture icon) {
			this.data = data;
			this.icon = icon;
			this.flash = 1.0F;
		}

		boolean contains(double mouseX, double mouseY) {
			return x >= 0 && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}
	}

	private record GridLayout(int x, int y, int width, int height, int columns,
	                          int cardSize, int visibleRows) {
	}

	private final Screen parent;
	private final ServerStatusPinger pinger = new ServerStatusPinger();
	private final List<ServerCard> cards = new ArrayList<>();
	private int selected = -1;
	private int scrollRow;
	private boolean confirmDelete;
	private float versionPillHover;
	private int @Nullable [] versionPillBounds;
	private String lastAddedIp;
	private volatile boolean alive = true;
	private GridLayout lastLayout;

	public DreamcastServersScreen(Screen parent) {
		super("Сетевая игра");
		this.parent = parent;
	}

	public DreamcastServersScreen(Screen parent, String highlightIp) {
		this(parent);
		this.lastAddedIp = highlightIp;
	}

	@Override
	protected void init() {
		super.init();
		alive = true;
		reloadServers();
	}

	private void reloadServers() {
		closeIcons();
		cards.clear();
		selected = -1;
		scrollRow = 0;
		confirmDelete = false;
		ServerList servers = new ServerList(this.minecraft);
		servers.load();
		for (int i = 0; i < servers.size(); i++) {
			ServerData data = servers.get(i);
			ServerCard card = new ServerCard(data,
					FaviconTexture.forServer(this.minecraft.getTextureManager(), data.ip));
			if (data.ip.equals(lastAddedIp)) {
				selected = i;
			}
			cards.add(card);
		}
		pingAll();
	}

	private void pingAll() {
		for (ServerCard card : cards) {
			ping(card);
		}
	}

	private void ping(ServerCard card) {
		ServerData data = card.data;
		data.setState(ServerData.State.PINGING);
		data.motd = Component.empty();
		data.status = Component.empty();
		CompletableFuture.runAsync(() -> {
			try {
				pinger.pingServer(data,
						() -> this.minecraft.execute(() -> {
							if (alive) {
								uploadChangedIcons();
							}
						}),
						() -> this.minecraft.execute(() -> {
							if (!alive) {
								return;
							}
							data.setState(data.protocol == SharedConstants.getCurrentVersion().protocolVersion()
									? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE);
							uploadChangedIcons();
						}),
						EventLoopGroupHolder.remote(this.minecraft.options.useNativeTransport()));
			} catch (UnknownHostException error) {
				this.minecraft.execute(() -> applyPingFailure(card, "адрес не найден"));
			} catch (Throwable error) {
				this.minecraft.execute(() -> applyPingFailure(card, "нет соединения"));
				DreamcastClient.LOGGER.warn("Пинг {} не удался", data.ip, error);
			}
		});
	}

	private void applyPingFailure(ServerCard card, String message) {
		if (!alive) {
			return;
		}
		card.data.setState(ServerData.State.UNREACHABLE);
		card.data.motd = Component.literal(message);
	}

	private void uploadChangedIcons() {
		for (ServerCard card : cards) {
			byte[] bytes = card.data.getIconBytes();
			if (Arrays.equals(bytes, card.uploadedIcon)) {
				continue;
			}
			if (bytes == null) {
				card.icon.clear();
				card.uploadedIcon = null;
				continue;
			}
			try {
				card.icon.upload(NativeImage.read(bytes));
				card.uploadedIcon = Arrays.copyOf(bytes, bytes.length);
			} catch (Throwable error) {
				card.data.setIconBytes(null);
				card.icon.clear();
				card.uploadedIcon = null;
			}
		}
	}

	@Override
	public void tick() {
		super.tick();
		pinger.tick();
		uploadChangedIcons();
	}

	@Override
	public void removed() {
		alive = false;
		pinger.removeAll();
		closeIcons();
		super.removed();
	}

	private void closeIcons() {
		for (ServerCard card : cards) {
			if (!card.icon.isClosed()) {
				card.icon.close();
			}
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		drawDarkBackdrop(graphics);
		String subtitle = switch (state()) {
			case "empty" -> "серверов пока нет";
			case "pinging" -> "обновляем состояние серверов…";
			default -> cards.size() + " сервер(ов) · двойной клик для подключения";
		};
		DreamcastUi.drawPageTitle(graphics, font, "Сетевая игра", subtitle, width, ACCENT);
		drawVersionPill(graphics, mouseX, mouseY);

		GridLayout layout = computeLayout();
		lastLayout = layout;
		drawGlassPanel(graphics, layout.x(), layout.y(), layout.width(), layout.height(), 13, 1.0F, ACCENT);
		for (ServerCard card : cards) {
			card.x = -1;
		}
		if (cards.isEmpty()) {
			drawEmptyState(graphics, layout);
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

	private void drawCard(GuiGraphicsExtractor graphics, ServerCard card, int index,
	                      int x, int y, int size, int mouseX, int mouseY) {
		card.x = x;
		card.y = y;
		card.width = size;
		card.height = size;
		boolean inside = card.contains(mouseX, mouseY);
		card.hover = ease(card.hover, inside ? 1.0F : 0.0F, 0.20F);
		card.appear = ease(card.appear, 1.0F, 0.18F);
		card.flash = ease(card.flash, 0.0F, 0.055F);
		float appear = DreamcastUi.smoothstep(card.appear);
		boolean selectedCard = index == selected;
		int cardY = y + Math.round((1.0F - appear) * 9.0F) - Math.round(card.hover * 2.0F);
		int fill = RenderUtils.mix(0xE20A0B10, 0xF0191B25, card.hover);
		int border = selectedCard
				? RenderUtils.withAlpha(ACCENT, 0.72F + card.hover * 0.24F)
				: RenderUtils.mix(0x24FFFFFF, ACCENT, card.hover * 0.42F);
		if (card.data.ip.equals(lastAddedIp) && card.flash > 0.02F) {
			border = RenderUtils.mix(border, 0xFF8DE06C, card.flash * 0.65F);
		}
		if (card.hover > 0.02F || selectedCard) {
			RenderUtils.fillRounded(graphics, x - 3, cardY - 3, size + 6, size + 7, 13,
					RenderUtils.withAlpha(ACCENT, (selectedCard ? 0.07F : 0.025F) + card.hover * 0.07F));
		}
		RenderUtils.fillRoundedBorder(graphics, x, cardY, size, size, 11,
				RenderUtils.withAlpha(border, appear), RenderUtils.withAlpha(fill, appear));

		int previewSize = Math.max(38, Math.min(size - 16, size - 49));
		int previewX = x + (size - previewSize) / 2;
		int previewY = cardY + 8;
		int stateColor = stateColor(card.data);
		RenderUtils.fillRounded(graphics, previewX - 3, previewY - 3, previewSize + 6, previewSize + 6,
				9, RenderUtils.withAlpha(stateColor, 0.16F * appear));
		// Gradient stays visible as an intentional fallback if the favicon is transparent/missing.
		RenderUtils.fillRounded(graphics, previewX, previewY, previewSize, previewSize, 7,
				RenderUtils.withAlpha(RenderUtils.mix(0xFF171A28, ACCENT, 0.18F), appear),
				RenderUtils.withAlpha(RenderUtils.mix(0xFF080A10, DreamcastUi.VIOLET, 0.12F), appear));
		graphics.blit(RenderPipelines.GUI_TEXTURED, card.icon.textureLocation(),
				previewX, previewY, 0.0F, 0.0F, previewSize, previewSize, 64, 64);
		maskRoundedImageCorners(graphics, previewX, previewY, previewSize, 6,
				RenderUtils.withAlpha(0xFF0E1018, appear));
		RenderUtils.fillRoundedBorder(graphics, previewX - 1, previewY - 1, previewSize + 2, previewSize + 2,
				8, RenderUtils.withAlpha(stateColor, 0.55F * appear), 0x00000000);

		int textY = previewY + previewSize + 5;
		String name = RenderUtils.clamp(font, card.data.name, size - 14);
		RenderUtils.textCentered(graphics, font, name, x + size / 2, textY,
				RenderUtils.withAlpha(selectedCard ? 0xFFFFFFFF : DreamcastUi.TEXT, appear), false);
		String status = statusLabel(card.data, size - 14);
		RenderUtils.textCentered(graphics, font, status, x + size / 2,
				Math.min(cardY + size - font.lineHeight - 5, textY + font.lineHeight + 2),
				RenderUtils.withAlpha(stateColor, appear), false);
	}

	private void drawActions(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		chips.clear();
		ServerData selection = selected >= 0 && selected < cards.size() ? cards.get(selected).data : null;
		if (confirmDelete && selection != null) {
			chips.add(chip("Удалить сервер", this::deleteSelected, true));
			chips.add(chip("Отмена", () -> confirmDelete = false));
		} else {
			Chip join = chip("Подключиться", () -> joinServer(selection));
			join.enabled = selection != null;
			chips.add(join);
			chips.add(chip("Добавить", this::addServer));
			Chip edit = chip("Изменить", () -> editServer(selection));
			edit.enabled = selection != null;
			chips.add(edit);
			Chip delete = chip("Удалить", () -> confirmDelete = true);
			delete.enabled = selection != null;
			delete.danger = true;
			chips.add(delete);
			chips.add(chip("Обновить", this::reopen));
		}
		chips.add(chip("Назад", this::onClose));
		drawChipRow(graphics, width / 2, height - 34, 20, 5, ACCENT, mouseX, mouseY);
	}

	private void drawVersionPill(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		String label = "◆ " + ViaIntegration.currentVersionLabel();
		int pillWidth = Math.min(180, RenderUtils.width(font, label) + 18);
		int pillHeight = 17;
		int pillX = width - pillWidth - 18;
		int pillY = 27;
		boolean inside = mouseX >= pillX && mouseX < pillX + pillWidth
				&& mouseY >= pillY && mouseY < pillY + pillHeight;
		versionPillHover = ease(versionPillHover, inside ? 1.0F : 0.0F, 0.22F);
		boolean available = ViaIntegration.available();
		int color = available ? 0xFF8DE06C : DreamcastUi.TEXT_DIM;
		RenderUtils.fillRoundedBorder(graphics, pillX, pillY, pillWidth, pillHeight, pillHeight / 2,
				RenderUtils.mix(0x26FFFFFF, color, versionPillHover * 0.7F),
				RenderUtils.mix(0xD90C0C10, RenderUtils.withAlpha(color, 0xFF),
						0.08F + 0.13F * versionPillHover));
		RenderUtils.textCentered(graphics, font, RenderUtils.clamp(font, label, pillWidth - 12),
				pillX + pillWidth / 2, pillY + (pillHeight - font.lineHeight) / 2,
				available ? color : DreamcastUi.TEXT_DIM, false);
		if (inside) {
			graphics.setTooltipForNextFrame(Component.literal(available
					? "Выбрать версию подключения через ViaFabricPlus"
					: "ViaFabricPlus недоступен"), mouseX, mouseY);
		}
		versionPillBounds = new int[]{pillX, pillY, pillWidth, pillHeight};
	}

	private void drawEmptyState(GuiGraphicsExtractor graphics, GridLayout layout) {
		int centerX = layout.x() + layout.width() / 2;
		int centerY = layout.y() + layout.height() / 2;
		RenderUtils.fillCircle(graphics, centerX, centerY - 18, 17,
				RenderUtils.withAlpha(ACCENT, 0.12F));
		RenderUtils.textCentered(graphics, font, "+", centerX, centerY - 23, ACCENT, false);
		RenderUtils.textCentered(graphics, font, "Список серверов пуст", centerX, centerY + 6,
				DreamcastUi.TEXT, false);
		RenderUtils.textCentered(graphics, font, "Добавь адрес — здесь появится карточка", centerX, centerY + 20,
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

	private String state() {
		if (cards.isEmpty()) {
			return "empty";
		}
		for (ServerCard card : cards) {
			if (card.data.state() == ServerData.State.PINGING) {
				return "pinging";
			}
		}
		return "ready";
	}

	private int stateColor(ServerData data) {
		return switch (data.state()) {
			case PINGING -> 0xFFFFC66C;
			case SUCCESSFUL -> 0xFF8DE06C;
			case INCOMPATIBLE, UNREACHABLE -> 0xFFFF8095;
			default -> DreamcastUi.TEXT_DIM;
		};
	}

	private String statusLabel(ServerData data, int maxWidth) {
		String value = switch (data.state()) {
			case PINGING -> "проверяем…";
			case UNREACHABLE -> "нет соединения";
			case INCOMPATIBLE -> data.version.getString();
			case SUCCESSFUL -> {
				String players = data.players == null ? "онлайн"
						: data.players.online() + "/" + data.players.max();
				String ping = data.ping > 0L ? " · " + data.ping + " мс" : "";
				yield players + ping;
			}
			default -> data.ip;
		};
		return RenderUtils.clamp(font, value, maxWidth);
	}

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
		if (ViaIntegration.available() && versionPillBounds != null
				&& event.x() >= versionPillBounds[0] && event.x() < versionPillBounds[0] + versionPillBounds[2]
				&& event.y() >= versionPillBounds[1] && event.y() < versionPillBounds[1] + versionPillBounds[3]) {
			playClick();
			this.minecraft.gui.setScreen(new DreamcastVersionSelectScreen(this));
			return true;
		}
		for (int i = 0; i < cards.size(); i++) {
			ServerCard card = cards.get(i);
			if (card.contains(event.x(), event.y())) {
				selected = i;
				confirmDelete = false;
				playClick();
				if (doubleClick) {
					joinServer(card.data);
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
			this.minecraft.gui.setScreen(new DreamcastServersScreen(parent, lastAddedIp));
		}
	}

	private void joinServer(@Nullable ServerData data) {
		if (data == null) {
			return;
		}
		com.dreamcast.client.util.AltsManager.rememberServer(data);
		ConnectScreen.startConnecting(this, this.minecraft, ServerAddress.parseString(data.ip), data, false, null);
	}

	private void addServer() {
		ServerData editing = new ServerData("", "", ServerData.Type.OTHER);
		this.minecraft.gui.setScreen(new ManageServerScreen(this, Component.literal("Добавить сервер"), result -> {
			if (result) {
				ServerList servers = new ServerList(this.minecraft);
				servers.load();
				ServerData existing = servers.get(editing.ip);
				if (existing != null) {
					existing.copyNameIconFrom(editing);
				} else {
					servers.add(editing, false);
				}
				servers.save();
				lastAddedIp = editing.ip;
				Notifications.ok("Серверы", "Сервер добавлен: " + editing.name);
			}
			this.minecraft.gui.setScreen(new DreamcastServersScreen(parent, result ? editing.ip : lastAddedIp));
		}, editing));
	}

	private void editServer(@Nullable ServerData data) {
		if (data == null) {
			return;
		}
		ServerData editing = new ServerData(data.name, data.ip, ServerData.Type.OTHER);
		editing.copyFrom(data);
		this.minecraft.gui.setScreen(new ManageServerScreen(this, Component.literal("Изменить сервер"), result -> {
			if (result) {
				data.name = editing.name;
				data.ip = editing.ip;
				data.copyFrom(editing);
				ServerList servers = new ServerList(this.minecraft);
				servers.load();
				servers.save();
			}
			reopen();
		}, editing));
	}

	private void deleteSelected() {
		if (selected < 0 || selected >= cards.size()) {
			confirmDelete = false;
			return;
		}
		ServerData victim = cards.get(selected).data;
		ServerList servers = new ServerList(this.minecraft);
		servers.load();
		servers.remove(victim);
		servers.save();
		confirmDelete = false;
		Notifications.info("Серверы", "Сервер удалён: " + victim.name);
		reopen();
	}
}
