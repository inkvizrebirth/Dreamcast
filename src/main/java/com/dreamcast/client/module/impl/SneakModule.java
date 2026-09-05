package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.util.KeyOwnership;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Sneak — постоянное приседание: клиент держит Shift, пока модуль включён.
 *
 * Ниже хитбокс, медленнее ходьба, игрок не падает с краёв. Клавиша Shift
 * при этом остаётся у игрока: ручное нажатие работает поверх модуля.
 *
 * Пакетный режим («сервер думает, что ты присел, а ты бежишь») вернётся
 * после уточнения имени ServerboundPlayerActionPacket.Action в 26.2.
 */
public class SneakModule extends Module {

	private boolean holding;

	public SneakModule() {
		super("sneak", "Sneak", "Постоянное приседание",
				ModuleCategory.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onEnable() {
		holdShift(true);
	}

	@Override
	protected void onDisable() {
		holdShift(false);
	}

	@Override
	public void tick() {
		// GUI: присед не удерживаем, чтобы не мешать экранам
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return;
		}
		holdShift(client.gui.screen() == null);
	}

	private void holdShift(boolean on) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		if (on && !holding) {
			KeyOwnership.hold(client, client.options.keyShift, this);
			holding = true;
		} else if (!on && holding) {
			KeyOwnership.releaseHold(client, client.options.keyShift, this);
			holding = false;
		}
	}
}
