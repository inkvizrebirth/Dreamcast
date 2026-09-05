package com.dreamcast.client.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Velocity (анти-нокбэк): проценты по осям, полное гашение, фильтр нулей.
 */
class VelocityMathTest {

	@Test
	void zeroPercentCancelsMotion() {
		double[] scaled = VelocityMath.scale(1.2, 0.45, -0.8, 0, 0);
		assertArrayEquals(new double[]{0.0, 0.0, 0.0}, scaled, 1.0e-9);
	}

	@Test
	void hundredPercentKeepsMotion() {
		double[] scaled = VelocityMath.scale(1.2, 0.45, -0.8, 100, 100);
		assertArrayEquals(new double[]{1.2, 0.45, -0.8}, scaled, 1.0e-9);
	}

	@Test
	void axesAreScaledIndependently() {
		double[] scaled = VelocityMath.scale(2.0, 1.0, -4.0, 50, 25);
		assertArrayEquals(new double[]{1.0, 0.25, -2.0}, scaled, 1.0e-9);
	}

	@Test
	void percentIsClampedToSafeRange() {
		double[] scaled = VelocityMath.scale(1.0, 1.0, 1.0, -50, 999);
		assertArrayEquals(new double[]{0.0, 2.0, 0.0}, scaled, 1.0e-9,
				"отрицательные проценты — ноль, выше 200 — не amplifируется бесконечно");
	}

	@Test
	void cancelIsAlwaysZero() {
		assertArrayEquals(new double[]{0.0, 0.0, 0.0}, VelocityMath.cancel(), 0.0);
	}

	@Test
	void zeroMotionIsNotWorthApplying() {
		assertFalse(VelocityMath.worthApplying(0.0, 0.0, 0.0));
		assertTrue(VelocityMath.worthApplying(0.0, 0.08, 0.0));
	}
}
