package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import com.dreamcast.client.settings.IntSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Zoom — оптический прицел на удержании клавиши (как OptiFine C).
 *
 * Пока зажата клавиша модуля, FOV плавно уходит к целевому; отпустил —
 * плавно возвращается. Плавность важна: резкий скачок FOV дезориентирует
 * и палится на записях. Хук — CameraMixin#getFov (та же точка, что у NoFOV).
 */
public class ZoomModule extends Module {

	private final IntSetting targetFov = intSetting("fov", "FOV приближения", 30, 5, 80);
	private final IntSetting smoothTicks = intSetting("smooth", "Плавность, тиков", 4, 1, 20);
	private final BooleanSetting holdOnly = bool("hold_only", "Только пока зажата клавиша", true);

	/** Текущий FOV (null — модуль не вмешивается). */
	private Float currentFov;
	private boolean activeLastTick;

	public ZoomModule() {
		super("zoom", "Zoom", "Приближение камеры на удержании клавиши",
				ModuleCategory.RENDER, GLFW.GLFW_KEY_UNKNOWN);
	}

	/**
	 * Клавиша Zoom в режиме удержания только «будит» модуль — дальше всё
	 * решает зажатие (isDown). В режиме переключения работает как обычный
	 * тумблер. Выключается модуль через ClickGUI.
	 */
	@Override
	protected void onBindPressed() {
		if (holdOnly.isEnabled()) {
			setEnabled(true);
		} else {
			toggle();
		}
	}

	@Override
	protected void onDisable() {
		currentFov = null;
		activeLastTick = false;
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		boolean wantActive;
		if (holdOnly.isEnabled()) {
			wantActive = getKeyMapping().isDown();
		} else {
			// Режим переключения: активен с момента включения модуля
			wantActive = true;
		}
		activeLastTick = wantActive;

		float target = wantActive ? targetFov.get() : client.options.fov().get().floatValue();
		if (currentFov == null) {
			currentFov = target;
		} else {
			float step = Math.max(0.5F, Math.abs(target - currentFov) / Math.max(1, smoothTicks.get()));
			currentFov += Math.signum(target - currentFov) * Math.min(step, Math.abs(target - currentFov));
		}
		if (!wantActive && Math.abs(currentFov - target) < 0.5F) {
			// Вернулись к игровому FOV — дальше не вмешиваемся
			currentFov = null;
		}
	}

	/** Значение для CameraMixin: null — не подменять FOV. */
	public Float currentFov() {
		return currentFov;
	}
}
