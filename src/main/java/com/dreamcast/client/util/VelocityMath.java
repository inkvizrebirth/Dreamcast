package com.dreamcast.client.util;

/**
 * Математика анти-нокбэка (Velocity): как преобразовать пришедший от сервера
 * импульс. Чистая логика — покрыта VelocityMathTest.
 *
 * Проценты работают как в Meteor: 0 — импульс полностью погашен,
 * 100 — остаётся как есть, значения больше 100 усиливают (для «отталкивания»).
 */
public final class VelocityMath {

	private VelocityMath() {
	}

	/**
	 * Масштабирует импульс по горизонтали и вертикали.
	 *
	 * @return массив {x, y, z} нового импульса
	 */
	public static double[] scale(double x, double y, double z, int horizontalPercent, int verticalPercent) {
		double horizontal = clampPercent(horizontalPercent) / 100.0;
		double vertical = clampPercent(verticalPercent) / 100.0;
		return new double[]{x * horizontal, y * vertical, z * horizontal};
	}

	/** Полное гашение импульса (режим «Cancel»). */
	public static double[] cancel() {
		return new double[]{0.0, 0.0, 0.0};
	}

	/**
	 * Нужно ли применять модуль к импульсу: полностью пропущенный импульс
	 * (все нули) трогать бессмысленно — не создаём лишний setDeltaMovement.
	 */
	public static boolean worthApplying(double x, double y, double z) {
		return Math.abs(x) > 1.0e-9 || Math.abs(y) > 1.0e-9 || Math.abs(z) > 1.0e-9;
	}

	private static double clampPercent(int percent) {
		return Math.max(0.0, Math.min(200.0, percent));
	}
}
