package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import com.dreamcast.client.settings.IntSetting;
import com.dreamcast.client.util.AutoEatLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * AutoEat — сам поест, когда надо.
 *
 * Два условия (логика — AutoEatLogic, покрыта тестами):
 * <ul>
 *   <li>HP ниже порога и есть золотое яблоко — ест золотое (реген и
 *       поглощение важнее сытости);</li>
 *   <li>голод ниже порога — ест лучшую по насыщенности еду.</li>
 * </ul>
 *
 * Модуль переключается на слот с едой, использует предмет до конца и
 * возвращает прежний слот. Пока игрок сам что-то использует (лук, щит),
 * AutoEat не вмешивается.
 */
public class AutoEatModule extends Module {

	private final IntSetting hungerThreshold = intSetting("hunger", "Есть при голоде ≤", 14, 0, 20);
	private final IntSetting gappleHealth = intSetting("gapple_health", "Золотое яблоко при HP ≤ (0=выкл)", 10, 0, 20);
	private final BooleanSetting offhand = bool("offhand", "Предлагать еду во вторую руку", false);

	private int previousSlot = -1;
	private boolean eating;

	public AutoEatModule() {
		super("auto_eat", "AutoEat", "Автоматически ест при голоде или низком HP",
				ModuleCategory.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onDisable() {
		restoreSlot();
		eating = false;
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || client.gameMode == null || client.gui.screen() != null) {
			return;
		}

		// Уже едим — ждём конца использования предмета
		if (eating) {
			if (!player.isUsingItem()) {
				eating = false;
				restoreSlot();
			}
			return;
		}
		// Не мешаем другому использованию (лук, тотем, щит)
		if (player.isUsingItem()) {
			return;
		}

		Inventory inventory = player.getInventory();
		List<AutoEatLogic.Food> foods = collectFoods(inventory);
		boolean hasGolden = foods.stream().anyMatch(AutoEatLogic::isGolden);

		float health = player.getHealth();
		int hunger = player.getFoodData().getFoodLevel();
		if (!AutoEatLogic.shouldEat(health, gappleHealth.get(), hunger, hungerThreshold.get(), hasGolden)) {
			return;
		}

		boolean goldenMode = gappleHealth.get() > 0 && health <= gappleHealth.get() && hasGolden;
		int slot = AutoEatLogic.bestSlot(foods, goldenMode);
		if (slot < 0) {
			return;
		}

		previousSlot = inventory.getSelectedSlot();
		inventory.setSelectedSlot(slot);
		client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
		eating = true;
	}

	private void restoreSlot() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player != null && previousSlot >= 0 && player.getInventory().getSelectedSlot() != previousSlot) {
			player.getInventory().setSelectedSlot(previousSlot);
		}
		previousSlot = -1;
	}

	/** Вся еда хотбара (слоты 0..8) в терминах чистой логики. */
	private List<AutoEatLogic.Food> collectFoods(Inventory inventory) {
		List<AutoEatLogic.Food> foods = new ArrayList<>();
		for (int slot = 0; slot < 9; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			FoodProperties properties = stack.get(DataComponents.FOOD);
			if (properties == null) {
				continue;
			}
			String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
			foods.add(new AutoEatLogic.Food(slot, path, properties.nutrition(), properties.saturation()));
		}
		return foods;
	}

	/** Настройка «еда во вторую руку» — зарезервирована под будущий свап. */
	public boolean wantsOffhand() {
		return offhand.isEnabled();
	}
}
