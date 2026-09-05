package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import org.lwjgl.glfw.GLFW;

/**
 * CameraTweaks — спокойная камера.
 *
 * <ul>
 *   <li><b>NoHurtCam</b> — убирает тряску/наклон камеры при получении урона
 *       (мешает целиться в PvP);</li>
 *   <li><b>NoBob</b> — убирает покачивание камеры при ходьбе.</li>
 * </ul>
 *
 * Хуки — приватные bobHurt/bobView в GameRenderer (GameRendererMixin).
 */
public class NoHurtCamModule extends Module {

	private final BooleanSetting noHurtCam = bool("no_hurt_cam", "Без тряски при уроне", true);
	private final BooleanSetting noBob = bool("no_bob", "Без покачивания при ходьбе", false);

	public NoHurtCamModule() {
		super("no_hurt_cam", "CameraTweaks", "Отключает тряску при уроне и покачивание камеры",
				ModuleCategory.RENDER, GLFW.GLFW_KEY_UNKNOWN);
	}

	public boolean noHurtCam() {
		return noHurtCam.isEnabled();
	}

	public boolean noBob() {
		return noBob.isEnabled();
	}
}
