package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.IntSetting;
import com.dreamcast.client.util.ArmorRating;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * AutoArmor — всегда лучшая броня из инвентаря.
 *
 * Каждые N тиков сравнивает надетое с содержимым инвентаря (рейтинг по
 * материалу, см. ArmorRating) и донадевает лучшее кликами контейнера —
 * теми же пакетами, что отправляет настоящий игрок. Обмен идёт в три клика
 * (взять лучшее → поменять с надетым → убрать старое на место лучшего)
 * по одному клику в тик: сервер не видит «нечеловеческой» скорости.
 *
 * Элитру модуль не трогает: это осознанный выбор игрока.
 */
public class AutoArmorModule extends Module {

	private final IntSetting delay = intSetting("delay", "Пауза между кликами, тиков", 2, 1, 10);

	/** Слоты брони в порядке проверки. */
	private static final EquipmentSlot[] SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	/** Инвентарные индексы слотов брони в InventoryMenu: 5..8 (голова..ботинки). */
	private static final int[] MENU_ARMOR_SLOTS = {5, 6, 7, 8};

	private int clickCooldown;

	/** Текущий обмен: -1 — не идёт. */
	private int swapInventorySlot = -1;
	private int swapMenuSlot = -1;
	private int swapStep;

	public AutoArmorModule() {
		super("auto_armor", "AutoArmor", "Автоматически надевает лучшую броню",
				ModuleCategory.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onDisable() {
		resetSwap();
	}

	private void resetSwap() {
		swapInventorySlot = -1;
		swapMenuSlot = -1;
		swapStep = 0;
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || client.gameMode == null || client.gui.screen() != null) {
			return;
		}
		// Чужой контейнер открыт — наши клики улетели бы не туда
		if (player.containerMenu != player.inventoryMenu) {
			return;
		}
		if (clickCooldown > 0) {
			clickCooldown--;
			return;
		}

		int containerId = player.inventoryMenu.containerId;

		// Обмен уже начат — доводим его до конца
		if (swapInventorySlot >= 0) {
			stepSwap(client, player, containerId);
			return;
		}

		// Ищем слот, где есть броня получше
		Inventory inventory = player.getInventory();
		for (int i = 0; i < SLOTS.length; i++) {
			EquipmentSlot slot = SLOTS[i];
			String currentPath = registryPath(player.getItemBySlot(slot));
			int bestSlot = -1;
			double bestScore = currentPath == null ? 0.0 : ArmorRating.score(currentPath);
			for (int inv = 0; inv < 36; inv++) {
				ItemStack stack = inventory.getItem(inv);
				if (stack.isEmpty()) {
					continue;
				}
				String path = registryPath(stack);
				if (ArmorRating.slotOf(path) != armorSlotFor(slot)) {
					continue;
				}
				double score = ArmorRating.score(path);
				if (score > bestScore) {
					bestScore = score;
					bestSlot = inv;
				}
			}
			if (bestSlot >= 0) {
				swapInventorySlot = bestSlot;
				swapMenuSlot = MENU_ARMOR_SLOTS[i];
				swapStep = 0;
				stepSwap(client, player, containerId);
				return;
			}
		}
	}

	/** Один клик обмена за вызов. */
	private void stepSwap(Minecraft client, LocalPlayer player, int containerId) {
		switch (swapStep) {
			case 0 -> {
				// Берём лучшую броню на курсор
				client.gameMode.handleContainerInput(containerId, menuSlotOf(swapInventorySlot), 0,
						ContainerInput.PICKUP, player);
				clickCooldown = Math.max(1, delay.get());
				swapStep = 1;
			}
			case 1 -> {
				// Кладём на место надетой — старая уходит на курсор
				client.gameMode.handleContainerInput(containerId, swapMenuSlot, 0,
						ContainerInput.PICKUP, player);
				clickCooldown = Math.max(1, delay.get());
				swapStep = 2;
			}
			case 2 -> {
				// Старую броню — в освободившийся слот инвентаря
				client.gameMode.handleContainerInput(containerId, menuSlotOf(swapInventorySlot), 0,
						ContainerInput.PICKUP, player);
				clickCooldown = Math.max(1, delay.get());
				resetSwap();
			}
			default -> resetSwap();
		}
	}

	/** Инвентарный слот 0..35 → индекс слота в InventoryMenu (9..44). */
	private static int menuSlotOf(int inventorySlot) {
		return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
	}

	private static ArmorRating.Slot armorSlotFor(EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> ArmorRating.Slot.HEAD;
			case CHEST -> ArmorRating.Slot.CHEST;
			case LEGS -> ArmorRating.Slot.LEGS;
			case FEET -> ArmorRating.Slot.FEET;
			default -> ArmorRating.Slot.NONE;
		};
	}

	static String registryPath(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
	}
}
