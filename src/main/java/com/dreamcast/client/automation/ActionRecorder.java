package com.dreamcast.client.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Captures the player's own held movement keys and look direction, tick by
 * tick, while they play normally — purely observational, it never writes to
 * any input. On stop() the captured frames become a single PLAYBACK node in
 * a new saved config, replayed later by {@link AutomationRunner}.
 */
public final class ActionRecorder {

	/** Hard cap so a forgotten recording can't grow forever: 20 tps * 60 * 10 = 10 minutes. */
	private static final int MAX_FRAMES = 12000;

	private static boolean active;
	private static final List<String> FRAMES = new ArrayList<>();

	private ActionRecorder() { }

	public static boolean isActive() { return active; }

	public static void start() {
		active = true;
		FRAMES.clear();
		notify("Запись начата — двигайтесь как обычно. Остановить: клавиша записи ещё раз или из паузы.");
	}

	public static void tick() {
		if (!active) return;
		Minecraft c = Minecraft.getInstance();
		if (c == null || c.player == null || c.options == null) return;
		int mask = (c.options.keyUp.isDown() ? 1 : 0)
				| (c.options.keyDown.isDown() ? 2 : 0)
				| (c.options.keyLeft.isDown() ? 4 : 0)
				| (c.options.keyRight.isDown() ? 8 : 0)
				| (c.options.keyJump.isDown() ? 16 : 0)
				| (c.options.keyShift.isDown() ? 32 : 0);
		FRAMES.add(mask + "," + fmt(c.player.getYRot()) + "," + fmt(c.player.getXRot()));
		if (FRAMES.size() >= MAX_FRAMES) stop();
	}

	public static void stop() {
		if (!active) return;
		active = false;
		if (FRAMES.isEmpty()) {
			notify("Запись пуста, конфиг не создан");
			return;
		}
		AutomationConfig config = new AutomationConfig("Запись " + (AutomationManager.all().size() + 1));
		AutomationNode start = config.nodes.get(0);
		AutomationNode stopNode = config.nodes.get(1);
		AutomationNode playback = new AutomationNode(AutomationNodeType.PLAYBACK, start.x + 160F, start.y);
		playback.values.put("frames", String.join(";", FRAMES));
		config.nodes.add(playback);
		config.links.clear();
		config.links.add(new AutomationLink(start.id, "next", playback.id));
		config.links.add(new AutomationLink(playback.id, "next", stopNode.id));
		AutomationManager.add(config);
		int frameCount = FRAMES.size();
		FRAMES.clear();
		notify("Запись сохранена: " + config.name + " (" + frameCount + " кадров)");
	}

	private static String fmt(float value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private static void notify(String message) {
		Minecraft c = Minecraft.getInstance();
		if (c != null && c.gui != null) {
			c.gui.hud.getChat().addClientSystemMessage(Component.literal("§b[Dreamcast] §f" + message));
		}
	}
}
