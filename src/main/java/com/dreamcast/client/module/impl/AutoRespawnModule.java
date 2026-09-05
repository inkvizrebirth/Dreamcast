package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import org.lwjgl.glfw.GLFW;

/**
 * AutoRespawn — мгновенное возрождение.
 *
 * Как только появляется экран смерти, модуль сам отправляет серверу
 * «возродить меня» — без клика по кнопке. Полезно на мини-играх и фермах,
 * где каждая секунда респауна на счету.
 */
public class AutoRespawnModule extends Module {

	private final BooleanSetting notify = bool("notify", "Уведомлять о смерти", true);

	private int lastDeathScreenAt;

	public AutoRespawnModule() {
		super("auto_respawn", "AutoRespawn", "Возрождается автоматически",
				ModuleCategory.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || !(client.screen instanceof DeathScreen)) {
			return;
		}
		// Анти-дребезг: один экран смерти — одно возрождение
		int now = client.level == null ? 0 : (int) (client.level.getGameTime() & 0x7FFFFFFF);
		if (now - lastDeathScreenAt < 20) {
			return;
		}
		lastDeathScreenAt = now;

		LocalPlayer player = client.player;
		if (player == null || client.getConnection() == null) {
			return;
		}
		client.getConnection().send(new ServerboundClientCommandPacket(
				player, ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
		if (notify.isEnabled()) {
			com.dreamcast.client.util.Notifications.warn("AutoRespawn", "Возрождение…");
		}
	}
}
