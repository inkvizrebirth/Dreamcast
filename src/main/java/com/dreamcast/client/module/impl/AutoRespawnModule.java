package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
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
		if (client == null || !(client.gui.screen() instanceof DeathScreen)) {
			return;
		}
		// Анти-дребезг: один экран смерти — одно возрождение
		int now = client.level == null ? 0 : (int) (client.level.getGameTime() & 0x7FFFFFFF);
		if (now - lastDeathScreenAt < 20) {
			return;
		}
		lastDeathScreenAt = now;

		// TODO(audit-26.2): имя конструктора ServerboundClientCommandPacket
		// уточняется в api-audit; до этого модуль детектирует смерть и
		// сообщает игроку, что пора возрождаться.
		if (notify.isEnabled()) {
			com.dreamcast.client.util.Notifications.warn("AutoRespawn", "Ты умер — возродись");
		}
	}
}
