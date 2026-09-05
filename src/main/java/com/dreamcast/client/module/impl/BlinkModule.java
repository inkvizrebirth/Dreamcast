package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.IntSetting;
import com.dreamcast.client.util.Notifications;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import org.lwjgl.glfw.GLFW;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Blink — «залипание» в сети: исходящие пакеты копятся в очереди и не
 * уходят на сервер, пока модуль включён.
 *
 * Для сервера игрок замирает там, где был: движение, удары, чат — всё
 * отправляется разом при выключении. Классическое применение — разведка
 * из безопасной точки (в связке с FreeCam) и мгновенные «рывки» по прямой.
 *
 * Очередь ограничена: при переполнении пакеты сливаются сами, иначе
 * сервер разорвал бы соединение по таймауту.
 */
public class BlinkModule extends Module {

	private final IntSetting capacity = intSetting("capacity", "Предел очереди", 500, 50, 2000);

	private static final Queue<Packet<?>> QUEUE = new ConcurrentLinkedQueue<>();
	private static volatile boolean flushing;

	public BlinkModule() {
		super("blink", "Blink", "Задерживает исходящие пакеты до выключения",
				ModuleCategory.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	/**
	 * Хук из миксина ClientPacketListener#send: true — пакет перехвачен
	 * (в очередь), false — отправить как обычно.
	 */
	public static boolean intercept(Packet<?> packet) {
		if (flushing) {
			return false;
		}
		BlinkModule module = com.dreamcast.client.module.ModuleManager.find(BlinkModule.class);
		if (module == null || !module.isEnabled()) {
			return false;
		}
		if (QUEUE.size() >= module.capacity.get()) {
			module.flush();
			return false;
		}
		QUEUE.add(packet);
		return true;
	}

	/** Выход из мира: копить пакеты для мёртвого соединения бессмысленно. */
	public static void onDisconnect() {
		QUEUE.clear();
	}

	@Override
	protected void onDisable() {
		flush();
	}

	/** Отправляет всё накопленное в исходном порядке. */
	public void flush() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.getConnection() == null) {
			QUEUE.clear();
			return;
		}
		int sent = 0;
		flushing = true;
		try {
			Packet<?> packet;
			while ((packet = QUEUE.poll()) != null) {
				client.getConnection().send(packet);
				sent++;
			}
		} finally {
			flushing = false;
		}
		if (sent > 0) {
			Notifications.info("Blink", "Отправлено пакетов: " + sent);
		}
	}
}
