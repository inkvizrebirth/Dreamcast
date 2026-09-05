package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

/**
 * AutoTool — правильный инструмент под каждый блок.
 *
 * В момент начала копания (хук в MultiPlayerGameMode#startDestroyBlock)
 * ищет в хотбаре инструмент с максимальной скоростью добычи этого блока
 * и переключается на него. Скорость считается ванильным
 * {@code ItemStack#getDestroySpeed} — то есть учитывает материал, уровень
 * инструмента и зачарования вроде «Эффективности».
 */
public class AutoToolModule extends Module {

	private final BooleanSetting requireFaster = bool("require_faster",
			"Только если быстрее текущего", true);

	public AutoToolModule() {
		super("auto_tool", "AutoTool", "Сам выбирает лучший инструмент для блока",
				ModuleCategory.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
	}

	/** Хук из миксина: игрок начал копать блок. */
	public static void onBlockBreak(BlockPos pos) {
		AutoToolModule module = com.dreamcast.client.module.ModuleManager.find(AutoToolModule.class);
		if (module == null || !module.isEnabled() || pos == null) {
			return;
		}
		module.switchToBest(pos);
	}

	private void switchToBest(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || client.level == null || client.screen != null) {
			return;
		}
		BlockState state = client.level.getBlockState(pos);
		if (state.isAir()) {
			return;
		}
		Inventory inventory = player.getInventory();
		float currentSpeed = inventory.getSelectedItem().getDestroySpeed(state);
		int bestSlot = -1;
		float bestSpeed = currentSpeed;
		for (int slot = 0; slot < 9; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			float speed = stack.getDestroySpeed(state);
			if (speed > bestSpeed + 1.0e-4F) {
				bestSpeed = speed;
				bestSlot = slot;
			}
		}
		if (bestSlot >= 0 && (!requireFaster.isEnabled() || bestSpeed > currentSpeed)) {
			inventory.setSelectedSlot(bestSlot);
		}
	}
}
