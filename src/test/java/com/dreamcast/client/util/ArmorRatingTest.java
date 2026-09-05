package com.dreamcast.client.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AutoArmor: определение слота, ранжирование материалов и решение об апгрейде.
 */
class ArmorRatingTest {

	@Test
	void slotIsDetectedFromRegistryPath() {
		assertEquals(ArmorRating.Slot.HEAD, ArmorRating.slotOf("diamond_helmet"));
		assertEquals(ArmorRating.Slot.HEAD, ArmorRating.slotOf("turtle_helmet"));
		assertEquals(ArmorRating.Slot.CHEST, ArmorRating.slotOf("iron_chestplate"));
		assertEquals(ArmorRating.Slot.LEGS, ArmorRating.slotOf("golden_leggings"));
		assertEquals(ArmorRating.Slot.FEET, ArmorRating.slotOf("leather_boots"));
		assertEquals(ArmorRating.Slot.NONE, ArmorRating.slotOf("elytra"));
		assertEquals(ArmorRating.Slot.NONE, ArmorRating.slotOf("diamond_sword"));
	}

	@Test
	void materialsAreRankedLikeVanilla() {
		assertTrue(ArmorRating.score("netherite_chestplate") > ArmorRating.score("diamond_chestplate"));
		assertTrue(ArmorRating.score("diamond_chestplate") > ArmorRating.score("iron_chestplate"));
		assertTrue(ArmorRating.score("iron_helmet") > ArmorRating.score("chainmail_helmet"));
		assertTrue(ArmorRating.score("chainmail_boots") > ArmorRating.score("golden_boots"));
		assertTrue(ArmorRating.score("golden_leggings") > ArmorRating.score("leather_leggings"));
		// Черепаший шлем лучше золотого, но хуже железного
		assertTrue(ArmorRating.score("turtle_helmet") > ArmorRating.score("golden_helmet"));
		assertTrue(ArmorRating.score("turtle_helmet") < ArmorRating.score("iron_helmet"));
	}

	@Test
	void nonArmorScoresZero() {
		assertEquals(0.0, ArmorRating.score("elytra"), 0.0);
		assertEquals(0.0, ArmorRating.score("shield"), 0.0);
		assertEquals(0.0, ArmorRating.score(null), 0.0);
	}

	@Test
	void upgradeDecision() {
		assertTrue(ArmorRating.isUpgrade("iron_chestplate", "diamond_chestplate"));
		assertTrue(ArmorRating.isUpgrade(null, "leather_boots"), "в пустой слот наденем даже кожу");
		assertFalse(ArmorRating.isUpgrade("diamond_helmet", "golden_helmet"));
		assertFalse(ArmorRating.isUpgrade("diamond_helmet", "diamond_helmet"));
		assertFalse(ArmorRating.isUpgrade("iron_chestplate", "elytra"), "элитру не навязываем");
	}
}
