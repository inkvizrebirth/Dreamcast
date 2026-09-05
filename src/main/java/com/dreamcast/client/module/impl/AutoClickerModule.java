package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import com.dreamcast.client.settings.IntSetting;
import com.dreamcast.client.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * AutoClicker — кликает сам, с человеческим разбросом CPS.
 *
 * ЛКМ-режим бьёт то, что под прицелом (или просто машет рукой — для
 * «удержания» удара), ПКМ-режим использует предмет (удочки, жемчуг,
 * зелья — всё, что спамится правой кнопкой).
 *
 * CPS плавает между минимумом и максимумом — фиксированный темп кликов
 * это самый простой детект любого античита.
 */
public class AutoClickerModule extends Module {

	private final ModeSetting button = mode("button", "Кнопка", "left",
			ModeSetting.option("left", "ЛКМ"),
			ModeSetting.option("right", "ПКМ"),
			ModeSetting.option("both", "Обе"));

	private final IntSetting minCps = intSetting("min_cps", "Минимум CPS", 8, 1, 20);
	private final IntSetting maxCps = intSetting("max_cps", "Максимум CPS", 12, 1, 20);
	private final BooleanSetting onlyHolding = bool("only_holding", "Только пока зажата ЛКМ", false);

	private static final Random RANDOM = new Random();

	private int ticksToNextClick;

	public AutoClickerModule() {
		super("auto_clicker", "AutoClicker", "Автоклик с человеческим разбросом CPS",
				ModuleCategory.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onEnable() {
		scheduleNext();
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || client.gameMode == null || client.gui.screen() != null) {
			return;
		}
		if (onlyHolding.isEnabled() && !client.options.keyAttack.isDown()) {
			return;
		}
		if (--ticksToNextClick > 0) {
			return;
		}
		scheduleNext();

		String mode = button.getValue();
		if (mode.equals("left") || mode.equals("both")) {
			clickLeft(client, player);
		}
		if (mode.equals("right") || mode.equals("both")) {
			clickRight(client, player);
		}
	}

	private void clickLeft(Minecraft client, LocalPlayer player) {
		Entity target = client.crosshairPickEntity;
		if (target != null && target.isAlive()) {
			client.gameMode.attack(player, target);
		}
		player.swing(InteractionHand.MAIN_HAND);
	}

	private void clickRight(Minecraft client, LocalPlayer player) {
		// Не начинаем новое использование, пока не закончилось прежнее
		if (player.isUsingItem()) {
			return;
		}
		client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
	}

	private void scheduleNext() {
		int min = Math.min(minCps.get(), maxCps.get());
		int max = Math.max(minCps.get(), maxCps.get());
		int cps = min + RANDOM.nextInt(Math.max(1, max - min + 1));
		ticksToNextClick = Math.max(1, 20 / cps);
	}
}
