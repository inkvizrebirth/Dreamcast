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
		if (true) {
			// TODO(audit-26.2): поле крюка на игроке (player.fishing) переехало;
			// вернём подсечку сразу после уточнения имени в api-audit
			return;
		}
		// Логика подсечки восстановится после api-audit (см. TODO выше).
	}
}
