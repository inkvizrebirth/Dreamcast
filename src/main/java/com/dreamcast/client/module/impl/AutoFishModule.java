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
		if (player == null || client.gameMode == null || client.gui.screen() != null) {
			return;
		}
		// Без удочки в руке ловить нечего
		if (player.getMainHandItem().getItem() != Items.FISHING_ROD) {
			phase = Phase.IDLE;
			return;
		}

		// Фаза паузы: ждём и забрасываем заново
		if (phase == Phase.WAITING_RECAST) {
			if (--countdown <= 0) {
				client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
				player.swing(InteractionHand.MAIN_HAND);
				lastHookMotionY = 0.0;
				phase = Phase.IDLE;
			}
			return;
		}

		// Публичное поле крюка на игроке (Player.fishing в 26.2)
		var hook = player.fishing;
		if (hook == null || hook.isRemoved()) {
			phase = Phase.IDLE;
			return;
		}

		// Поклёвка — поплавок резко дёргается вниз
		double motionY = hook.getDeltaMovement().y;
		if (motionY < -0.02 && lastHookMotionY >= -0.02) {
			// Подсечка: тот же ПКМ, что вытаскивает рыбу
			client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
			player.swing(InteractionHand.MAIN_HAND);
			phase = Phase.WAITING_RECAST;
			countdown = recastDelay.get();
			com.dreamcast.client.util.Notifications.ok("AutoFish", "Поймал — закидываю снова");
		}
		lastHookMotionY = motionY;
	}
}
