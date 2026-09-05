package com.dreamcast.client.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Баллистика стрелы BowAimbot: формула заряда, подбор питча и yaw.
 */
class BowAimLogicTest {

	@Test
	void chargeVelocityGrowsWithTicksAndCapsAtThree() {
		assertTrue(BowAimLogic.chargeVelocity(5) < BowAimLogic.chargeVelocity(15));
		assertEquals(3.0, BowAimLogic.chargeVelocity(100), 1.0e-9);
		assertEquals(3.0, BowAimLogic.chargeVelocity(20), 1.0e-9);
		assertTrue(BowAimLogic.chargeVelocity(0) <= 0.001);
	}

	@Test
	void flatShotSolvesToSlightlyNegativePitch() {
		// Цель на том же уровне в 10 блоках: стрела падает — нужен подъём
		Double pitch = BowAimLogic.solvePitch(10.0, 0.0, 3.0);
		assertNotNull(pitch);
		assertTrue(pitch < 0.0 && pitch > -45.0,
				"питч должен быть вверх, но полого: " + pitch);
		// Проверка: симуляция этим питчем попадает в цель
		double error = BowAimLogic.simulateError(pitch, 10.0, 0.0, 3.0);
		assertTrue(Math.abs(error) < 0.5, "ошибка попадания велика: " + error);
	}

	@Test
	void highTargetNeedsSteepPitch() {
		Double flat = BowAimLogic.solvePitch(8.0, 0.0, 3.0);
		Double high = BowAimLogic.solvePitch(8.0, 6.0, 3.0);
		assertNotNull(flat);
		assertNotNull(high);
		assertTrue(high < flat, "цель выше — питч круче: " + high + " vs " + flat);
	}

	@Test
	void unreachableTargetReturnsNull() {
		// Слабый выстрел на 200 блоков — заведомый недолёт
		assertNull(BowAimLogic.solvePitch(200.0, 0.0, 0.4));
	}

	@Test
	void yawMatchesGameConvention() {
		// +X — восток: yaw = -90 в конвенции Minecraft
		assertEquals(-90.0F, BowAimLogic.yawTo(5.0, 0.0), 0.01F);
		// +Z — юг: yaw = 0
		assertEquals(0.0F, BowAimLogic.yawTo(0.0, 5.0), 0.01F);
	}
}
