package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/**
 * AutoFish — рыбалка без игрока.
 *
 * Клюёт поклёвка — поплавок дёргается вниз (скачок вертикальной скорости),
 * это видно клиенту напрямую. Модуль тут же подсоздаёт леску (ПКМ), ждёт
 * паузу и забрасывает снова. Удочка должна быть в руке.
 */
public class AutoFishModule extends Module {

	private final IntSetting recastDelay = intSetting("recast", "Пауза до заброса, тиков", 12, 4, 40);

	private enum Phase {
		IDLE, WAITING_RECAST
	}

	private Phase phase = Phase.IDLE;
	private int countdown;
	private double lastHookMotionY;

	public AutoFishModule() {
		super("auto_fish", "AutoFish", "Сам подсекает и перезакидывает удочку",
				ModuleCategory.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onDisable() {
		phase = Phase.IDLE;
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || client.gameMode == null || client.screen != null) {
			return;
		}
		if (player.getMainHandItem().getItem() != Items.FISHING_ROD) {
			phase = Phase.IDLE;
			return;
		}

		if (phase == Phase.WAITING_RECAST) {
			if (--countdown <= 0) {
				// Заброс: второе использование удочки после подсечки
				client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
				phase = Phase.IDLE;
			}
			return;
		}

		var hook = player.fishing;
		if (hook == null) {
			lastHookMotionY = 0.0;
			return;
		}
		// Поклёвка: поплавок резко уходит под воду
		double motionY = hook.getDeltaMovement().y;
		boolean bite = motionY < -0.035 && lastHookMotionY > -0.02;
		lastHookMotionY = motionY;
		if (!bite) {
			return;
		}
		// Подсечка — то же действие, что и ПКМ с удочкой
		client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
		phase = Phase.WAITING_RECAST;
		countdown = Math.max(2, recastDelay.get());
	}
}
