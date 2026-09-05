package com.dreamcast.client.util;

/**
 * Баллистика стрелы для BowAimbot. Чистая логика — покрыта BowAimLogicTest.
 *
 * Модель полёта повторяет ванильную: каждый тик скорость стрелы умножается
 * на сопротивление воздуха (0.99), а к вертикальной составляющей применяется
 * гравитация (0.05 блока/тик²). Точного аналитического решения с драгом нет,
 * поэтому нужный угол подбирается моделированием — это дёшево (десятки
 * итераций) и точно в пределах тика.
 */
public final class BowAimLogic {

	/** Гравитация стрелы ванили. */
	public static final double GRAVITY = 0.05;

	/** Сопротивление воздуха стрелы ванили. */
	public static final double DRAG = 0.99;

	/** Максимальная скорость fully-charged лука (charge=1 → 3.0 блока/тик). */
	public static final double MAX_VELOCITY = 3.0;

	/** Сколько тиков заряда дают такую скорость выстрела (ванильная формула). */
	public static double chargeVelocity(int ticksUsing) {
		double fraction = ticksUsing / 20.0;
		fraction = (fraction * fraction + fraction * 2.0) / 3.0;
		if (fraction > 1.0) {
			fraction = 1.0;
		}
		return fraction * MAX_VELOCITY;
	}

	/**
	 * Подбирает питч (в градусах, отрицательный — вверх), при котором стрела
	 * долетит до цели.
	 *
	 * @param horizontalDistance горизонтальная дальность, блоков
	 * @param heightDifference   высота цели минус высота глаз (плюс — цель выше)
	 * @param velocity           скорость выстрела (см. {@link #chargeVelocity})
	 * @return питч в градусах или null, если недолёт при любом угле
	 */
	public static Double solvePitch(double horizontalDistance, double heightDifference, double velocity) {
		if (velocity <= 0.05 || horizontalDistance <= 0.0) {
			return null;
		}
		double bestPitch = Double.NaN;
		double bestError = Double.MAX_VALUE;
		// Перебор от настильного к навесному: у баллистики два решения,
		// берём настильное — оно быстрее долетает и меньше «светит» аимбот
		for (double pitch = 0.0; pitch >= -89.0; pitch -= 0.25) {
			double error = simulateError(pitch, horizontalDistance, heightDifference, velocity);
			if (Double.isNaN(error)) {
				continue; // недолёт
			}
			if (Math.abs(error) < bestError) {
				bestError = Math.abs(error);
				bestPitch = pitch;
			}
		}
		if (Double.isNaN(bestPitch)) {
			// Ни один угол не долетает до цели — стрелять бессмысленно
			return null;
		}
		return bestPitch;
	}

	/**
	 * Пролёт стрелы до горизонтальной дистанции; возвращает разницу
	 * «высота стрелы − высота цели» (NaN — стрела не долетела).
	 */
	static double simulateError(double pitchDegrees, double horizontalDistance,
	                            double heightDifference, double velocity) {
		double radians = Math.toRadians(pitchDegrees);
		double horizontal = velocity * Math.cos(radians);
		// Питч в Minecraft отрицательный при взгляде вверх — для стрельбы
		// вверх вертикальная скорость должна быть положительной
		double vertical = -velocity * Math.sin(radians);
		double x = 0.0;
		double y = 0.0;
		for (int tick = 0; tick < 200; tick++) {
			x += horizontal;
			y += vertical;
			horizontal *= DRAG;
			vertical = vertical * DRAG - GRAVITY;
			if (x >= horizontalDistance) {
				return y - heightDifference;
			}
			if (vertical < -3.0) {
				// Стрела камнем падает вниз — дальше бессмысленно
				return Double.NaN;
			}
		}
		return Double.NaN;
	}

	/**
	 * Yaw на точку цели (как в KillAura): из дельты координат в игровой yaw.
	 */
	public static float yawTo(double dx, double dz) {
		return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
	}
}
