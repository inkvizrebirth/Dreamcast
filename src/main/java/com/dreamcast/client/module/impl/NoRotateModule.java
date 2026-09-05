package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

/**
 * NoRotate — защита от серверного доворота.
 *
 * Некоторые серверы (и плагины вроде телепортов/античитов) принудительно
 * разворачивают игрока пакетом позиции. Модуль запоминает углы до обработки
 * пакета и возвращает их после — камера остаётся там, куда смотрел игрок.
 *
 * Хук — ClientPacketListenerMixin (handleMovePlayer, HEAD/TAIL).
 */
public class NoRotateModule extends Module {

	private static float savedYaw;
	private static float savedPitch;
	private static boolean hasSaved;

	public NoRotateModule() {
		super("no_rotate", "NoRotate", "Игнорирует серверный доворот камеры",
				ModuleCategory.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
	}

	/** Запоминает углы до обработки серверного пакета позиции. */
	public static void saveRotation(float yaw, float pitch) {
		NoRotateModule module = com.dreamcast.client.module.ModuleManager.find(NoRotateModule.class);
		if (module == null || !module.isEnabled()) {
			hasSaved = false;
			return;
		}
		savedYaw = yaw;
		savedPitch = pitch;
		hasSaved = true;
	}

	/** Возвращает игроку его углы после обработки пакета. */
	public static void restoreRotation(LocalPlayer player) {
		if (!hasSaved || player == null) {
			return;
		}
		hasSaved = false;
		player.setYRot(savedYaw);
		player.setXRot(savedPitch);
	}

	@Override
	protected void onDisable() {
		hasSaved = false;
	}
}
