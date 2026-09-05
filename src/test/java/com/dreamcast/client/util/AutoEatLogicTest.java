package com.dreamcast.client.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AutoEat: когда есть и что именно выбирать.
 */
class AutoEatLogicTest {

	private static final AutoEatLogic.Food STEAK =
			new AutoEatLogic.Food(0, "cooked_beef", 8, 0.8f);
	private static final AutoEatLogic.Food BREAD =
			new AutoEatLogic.Food(1, "bread", 5, 0.6f);
	private static final AutoEatLogic.Food GOLDEN =
			new AutoEatLogic.Food(2, "golden_apple", 4, 1.2f);
	private static final AutoEatLogic.Food ENCHANTED =
			new AutoEatLogic.Food(3, "enchanted_golden_apple", 4, 1.2f);

	@Test
	void hungerTriggersEating() {
		assertTrue(AutoEatLogic.shouldEat(20.0f, 0.0f, 10, 14, false));
		assertFalse(AutoEatLogic.shouldEat(20.0f, 0.0f, 18, 14, false));
	}

	@Test
	void gappleModeTriggersOnLowHealthEvenWhenFull() {
		assertTrue(AutoEatLogic.shouldEat(8.0f, 10.0f, 20, 14, true));
		// Нет золотого — режим не срабатывает
		assertFalse(AutoEatLogic.shouldEat(8.0f, 10.0f, 20, 0, false));
	}

	@Test
	void normalModePrefersBestSaturationAndSavesGapples() {
		int slot = AutoEatLogic.bestSlot(List.of(STEAK, BREAD, GOLDEN), false);
		assertEquals(0, slot, "стейк сытнее хлеба, а золотое бережём");
	}

	@Test
	void goldenModePrefersEnchantedThenGolden() {
		int slot = AutoEatLogic.bestSlot(List.of(STEAK, GOLDEN, ENCHANTED), true);
		assertEquals(3, slot, "зачарованное золотое важнее обычного");
	}

	@Test
	void goldenModeRejectsNormalFood() {
		assertEquals(-1, AutoEatLogic.bestSlot(List.of(STEAK, BREAD), true));
	}

	@Test
	void emptyInventoryMeansNothingToEat() {
		assertEquals(-1, AutoEatLogic.bestSlot(List.of(), false));
	}

	@Test
	void goldenDetection() {
		assertTrue(AutoEatLogic.isGolden(GOLDEN));
		assertTrue(AutoEatLogic.isGolden(ENCHANTED));
		assertFalse(AutoEatLogic.isGolden(STEAK));
	}
}
