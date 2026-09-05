package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/**
 * ElytraBoost — автоматический разгон элитр фейерверками.
 *
 * Пока игрок летит на элитрах, модуль каждые N тиков запускает ракету:
 * ищет её в руке, иначе переключается на слот с ракетами в хотбаре,
 * запускает и возвращает прежний слот. Полёт без ручного спама ПКМ.
 */
public class ElytraBoostModule extends Module {

	private final IntSetting delay = intSetting("delay", "Интервал, тиков", 6, 2, 40);

	private int cooldown;
	private int restoreSlot = -1;

	public ElytraBoostModule() {
		super("elytra_boost", "ElytraBoost", "Авто-разгон элитр фейерверками",
				ModuleCategory.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || client.gameMode == null || client.screen != null) {
			restore(player);
			return;
		}
		if (!player.isFallFlying()) {
			restore(player);
			cooldown = 0;
			return;
		}
		if (player.isUsingItem()) {
			return;
		}

		Inventory inventory = player.getInventory();

		// 1) Ракета уже в руке — просто запускаем
		if (inventory.getSelectedItem().getItem() == Items.FIREWORK_ROCKET) {
			if (--cooldown <= 0) {
				cooldown = delay.get();
				client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
			}
			return;
		}
		if (player.getItemInHand(InteractionHand.OFF_HAND).getItem() == Items.FIREWORK_ROCKET) {
			if (--cooldown <= 0) {
				cooldown = delay.get();
				client.gameMode.useItem(player, InteractionHand.OFF_HAND);
			}
			return;
		}

		// 2) Ищем ракеты в хотбаре, переключаемся и запускаем
		for (int slot = 0; slot < 9; slot++) {
			if (inventory.getItem(slot).getItem() == Items.FIREWORK_ROCKET) {
				if (restoreSlot < 0) {
					restoreSlot = inventory.getSelectedSlot();
				}
				inventory.setSelectedSlot(slot);
				cooldown = delay.get();
				client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
				return;
			}
		}
		restore(player);
	}

	@Override
	protected void onDisable() {
		Minecraft client = Minecraft.getInstance();
		restore(client == null ? null : client.player);
	}

	private void restore(LocalPlayer player) {
		if (player != null && restoreSlot >= 0 && player.getInventory().getSelectedSlot() != restoreSlot) {
			player.getInventory().setSelectedSlot(restoreSlot);
		}
		restoreSlot = -1;
	}

	/** Свободный слот под ракеты — для будущего автопополнения. */
	static boolean isRocket(ItemStack stack) {
		return stack.getItem() == Items.FIREWORK_ROCKET;
	}
}
