package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import com.dreamcast.client.settings.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * AntiAFK — не даёт серверу выкинуть за бездействие.
 *
 * Каждые N секунд делает случайное «человеческое» действие: поворот,
 * взмах рукой, прыжок или короткий шажок. Набор действий настраивается.
 */
public class AntiAFKModule extends Module {

	private final IntSetting interval = intSetting("interval", "Интервал, секунд", 30, 5, 300);
	private final BooleanSetting rotate = bool("rotate", "Повороты", true);
	private final BooleanSetting swing = bool("swing", "Взмахи рукой", true);
	private final BooleanSetting jump = bool("jump", "Прыжки", false);

	private static final Random RANDOM = new Random();

	private int countdown;

	public AntiAFKModule() {
		super("anti_afk", "AntiAFK", "Имитирует активность против АФК-кика",
				ModuleCategory.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onEnable() {
		countdown = interval.get() * 20;
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || client.screen != null) {
			return;
		}
		if (--countdown > 0) {
			return;
		}
		countdown = Math.max(20, interval.get() * 20);

		int action = RANDOM.nextInt(3);
		if (action == 0 && rotate.isEnabled()) {
			player.setYRot(player.getYRot() + 25.0F + RANDOM.nextFloat() * 90.0F);
		} else if (action == 1 && swing.isEnabled()) {
			player.swing(InteractionHand.MAIN_HAND);
		} else if (action == 2 && jump.isEnabled() && player.onGround()) {
			player.jumpFromGround();
		} else {
			// Запасное действие, если выбранные выключены — просто поворот
			player.setYRot(player.getYRot() + 40.0F);
		}
	}
}
