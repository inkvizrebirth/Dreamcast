package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * ChestStealer — мгновенно забирает всё из открытого контейнера.
 *
 * Пока открыт сундук (или любой не-инвентарный контейнер), модуль
 * шифт-кликает его слоты по одному с настраиваемой паузой — содержимое
 * уезжает в инвентарь теми же пакетами, что и ручные клики. Игровые слоты
 * игрока (последние 36) не трогаются.
 */
public class ChestStealerModule extends Module {

	private final IntSetting delay = intSetting("delay", "Пауза между кликами, тиков", 2, 0, 10);

	private int clickCooldown;

	public ChestStealerModule() {
		super("chest_stealer", "ChestStealer", "Забирает всё из открытых сундуков",
				ModuleCategory.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || client.gameMode == null) {
			return;
		}
		// Работаем только с «чужими» контейнерами: инвентарь игрока не трогаем
		if (player.containerMenu == player.inventoryMenu) {
			return;
		}
		if (clickCooldown > 0) {
			clickCooldown--;
			return;
		}

		var menu = player.containerMenu;
		int containerSlots = menu.slots.size() - 36; // слоты игрока всегда в конце
		if (containerSlots <= 0) {
			return;
		}
		for (int i = 0; i < containerSlots; i++) {
			Slot slot = menu.getSlot(i);
			ItemStack stack = slot.getItem();
			if (stack.isEmpty()) {
				continue;
			}
			client.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.QUICK_MOVE, player);
			clickCooldown = Math.max(1, delay.get());
			return; // один клик за тик — как человек
		}
	}
}
