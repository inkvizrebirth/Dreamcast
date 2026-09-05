package com.dreamcast.client.util;

/**
 * Математика модуля Speed (strafe-разгон). Чистая логика — покрыта
 * SpeedMathTest.
 *
 * Strafe не «добавляет» скорость к ванильной, а приводит горизонтальную
 * составляющую к целевой: направление движения сохраняется, модуль скорости
 * становится равным заданному. Так разгон ведёт себя предсказуемо и на земле,
 * и в воздухе.
 */
public final class SpeedMath {

	private SpeedMath() {
	}

	/**
	 * Приводит горизонтальную скорость к целевой.
	 *
	 * @return {x, y, z} нового движения (y не меняется)
	 */
	public static double[] strafe(double motionX, double motionY, double motionZ, double targetSpeed) {
		double horizontal = Math.sqrt(motionX * motionX + motionZ * motionZ);
		if (horizontal < 1.0e-9 || targetSpeed <= 0.0) {
			return new double[]{motionX, motionY, motionZ};
		}
		double factor = targetSpeed / horizontal;
		return new double[]{motionX * factor, motionY, motionZ * factor};
	}

	/** Горизонтальная скорость (блоков/тик) из вектора движения. */
	public static double horizontalSpeed(double motionX, double motionZ) {
		return Math.sqrt(motionX * motionX + motionZ * motionZ);
	}

	/** Скорость в блоках в секунду (20 тиков/с) — для HUD. */
	public static double blocksPerSecond(double motionX, double motionZ) {
		return horizontalSpeed(motionX, motionZ) * 20.0;
	}

	/**
	 * Есть ли ввод движения: при нулевом вводе разгоняться некуда —
	 * иначе Speed тащил бы стоящего игрока в сторону последнего нажатия.
	 */
	public static boolean hasInput(float forward, float sideways) {
		return Math.abs(forward) > 1.0e-4 || Math.abs(sideways) > 1.0e-4;
	}
}
