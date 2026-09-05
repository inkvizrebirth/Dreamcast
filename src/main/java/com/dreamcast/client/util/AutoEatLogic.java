package com.dreamcast.client.util;

import java.util.List;

/**
 * Логика AutoEat: когда пора есть и что именно. Чистая логика — покрыта
 * AutoEatLogicTest.
 *
 * Правила как в Meteor AutoEat:
 * <ul>
 *   <li>«золотое» условие: HP ниже порога — едим золотое яблоко (даже сытым);</li>
 *   <li>«голодное» условие: шкала еды ниже порога — едим лучшую еду;</li>
 *   <li>лучшая еда — с максимальной насыщенностью, при равенстве — сытнее;
 *       золотые яблоки в приоритете только в «золотом» режиме.</li>
 * </ul>
 */
public final class AutoEatLogic {

	/** Еда в слоте: минимум данных, чтобы логика не знала про Minecraft. */
	public record Food(int slot, String registryPath, int nutrition, float saturation) {
	}

	private AutoEatLogic() {
	}

	public static boolean isGolden(Food food) {
		return food.registryPath() != null
				&& (food.registryPath().equals("golden_apple") || food.registryPath().equals("enchanted_golden_apple"));
	}

	/**
	 * Пора ли есть прямо сейчас.
	 *
	 * @param health            текущее HP
	 * @param gappleHealth      HP-порог «золотого» режима (0 — режим выключен)
	 * @param food              уровень шкалы еды 0..20
	 * @param hungerThreshold   порог голода (0 — по голоду не едим)
	 * @param hasGolden         есть ли золотое яблоко в инвентаре
	 */
	public static boolean shouldEat(float health, float gappleHealth, int food, int hungerThreshold,
	                                boolean hasGolden) {
		if (gappleHealth > 0.0f && health <= gappleHealth && hasGolden) {
			return true;
		}
		return hungerThreshold > 0 && food <= hungerThreshold;
	}

	/**
	 * Лучший слот для еды. В «золотом» режиме — сначала зачарованное, потом
	 * обычное золотое; иначе — максимум насыщенности, при равенстве сытнее.
	 *
	 * @return индекс слота или -1, если есть нечего
	 */
	public static int bestSlot(List<Food> foods, boolean goldenMode) {
		int best = -1;
		double bestScore = -1.0;
		for (Food candidate : foods) {
			double score = score(candidate, goldenMode);
			if (score > bestScore) {
				bestScore = score;
				best = candidate.slot();
			}
		}
		return best;
	}

	static double score(Food food, boolean goldenMode) {
		if (food == null || food.nutrition() <= 0) {
			return -1.0;
		}
		double base = food.saturation() * 100.0 + food.nutrition();
		if (!goldenMode) {
			// В обычном режиме золотые яблоки бережём: чуть хуже лучшей еды
			if (food.registryPath() != null && food.registryPath().equals("golden_apple")) {
				base = Math.min(base, 39.0);
			}
			if (food.registryPath() != null && food.registryPath().equals("enchanted_golden_apple")) {
				base = Math.min(base, 38.0);
			}
		} else {
			if (food.registryPath() != null && food.registryPath().equals("enchanted_golden_apple")) {
				base += 100000.0;
			} else if (food.registryPath() != null && food.registryPath().equals("golden_apple")) {
				base += 50000.0;
			} else {
				// В «золотом» режиме обычная еда не годится вовсе
				return -1.0;
			}
		}
		return base;
	}
}
