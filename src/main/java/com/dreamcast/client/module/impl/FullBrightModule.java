package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.IntSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * FullBright — полная яркость: пещеры, океан и ночь видны как днём.
 *
 * Работает через игровую настройку гаммы (SimpleOption): на включении
 * запоминает значение игрока и выкручивает максимум, на выключении —
 * возвращает как было. В меню настроек гамма при этом «залипает» на
 * максимуме — это ожидаемо, после выключения вернётся.
 */
public class FullBrightModule extends Module {

	private final IntSetting gamma = intSetting("gamma", "Гамма, ×", 12, 2, 16);

	private double savedGamma = -1.0;

	public FullBrightModule() {
		super("full_bright", "FullBright", "Полная яркость в любых условиях",
				ModuleCategory.RENDER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onEnable() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.options == null) {
			return;
		}
		if (savedGamma < 0.0) {
			savedGamma = client.options.gamma().get();
		}
	}

	@Override
	protected void onDisable() {
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.options != null && savedGamma >= 0.0) {
			client.options.gamma().set(savedGamma);
			savedGamma = -1.0;
		}
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.options == null) {
			return;
		}
		// Игрок мог подвинуть ползунок гаммы руками — держим наше значение
		double wanted = gamma.get();
		if (Math.abs(client.options.gamma().get() - wanted) > 0.01) {
			client.options.gamma().set(wanted);
		}
	}
}
