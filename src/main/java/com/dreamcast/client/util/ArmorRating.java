package com.dreamcast.client.util;

/**
 * Оценка брони для AutoArmor. Чистая логика — покрыта ArmorRatingTest.
 *
 * Чтобы не зависеть от внутренних API предметов (в 26.2 классы брони
 * переезжали вместе с «экипировочными ассетами»), рейтинг считается по
 * пути предмета в реестре: материал даёт базовые очки, редкие материалы
 * ранжируются как в ванили. Для модовой брони с неизвестным материалом —
 * нейтральный низкий балл: такой предмет наденется только в пустой слот.
 */
public final class ArmorRating {

	/** Слот брони в терминах ванили. */
	public enum Slot {
		HEAD, CHEST, LEGS, FEET, NONE
	}

	private ArmorRating() {
	}

	/** Определяет слот по пути предмета в реестре (например «diamond_helmet»). */
	public static Slot slotOf(String registryPath) {
		if (registryPath == null) {
			return Slot.NONE;
		}
		String path = registryPath.toLowerCase(java.util.Locale.ROOT);
		if (path.endsWith("_helmet") || path.equals("turtle_helmet") || path.equals("turtle_shell")) {
			return Slot.HEAD;
		}
		if (path.endsWith("_chestplate")) {
			return Slot.CHEST;
		}
		if (path.endsWith("_leggings")) {
			return Slot.LEGS;
		}
		if (path.endsWith("_boots")) {
			return Slot.FEET;
		}
		return Slot.NONE;
	}

	/**
	 * Очки предмета: чем выше, тем лучше. Элитра и не-броня получают 0 —
	 * AutoArmor их не трогает (элитра — выбор игрока, не «лучшая броня»).
	 */
	public static double score(String registryPath) {
		Slot slot = slotOf(registryPath);
		if (slot == Slot.NONE) {
			return 0.0;
		}
		String path = registryPath.toLowerCase(java.util.Locale.ROOT);
		double material;
		if (path.startsWith("netherite_")) {
			material = 100.0;
		} else if (path.startsWith("diamond_")) {
			material = 80.0;
		} else if (path.startsWith("turtle_")) {
			// Черепаший шлем ≈ железо по защите
			material = 58.0;
		} else if (path.startsWith("iron_")) {
			material = 60.0;
		} else if (path.startsWith("chainmail_")) {
			material = 45.0;
		} else if (path.startsWith("golden_")) {
			material = 35.0;
		} else if (path.startsWith("leather_")) {
			material = 20.0;
		} else {
			// Модовая броня: наденем только если слот пуст
			material = 10.0;
		}
		// Небольшая надбавка за слот, чтобы при равном материале порядок был стабильным
		return material + slotBonus(slot);
	}

	private static double slotBonus(Slot slot) {
		return switch (slot) {
			case CHEST -> 0.3;
			case LEGS -> 0.2;
			case HEAD -> 0.1;
			case FEET -> 0.0;
			case NONE -> 0.0;
		};
	}

	/**
	 * Стоит ли менять текущий предмет на кандидата.
	 * Пустой слот (current == null/blank) заполняется любой бронёй нужного слота.
	 */
	public static boolean isUpgrade(String currentPath, String candidatePath) {
		double candidate = score(candidatePath);
		if (candidate <= 0.0) {
			return false;
		}
		double current = currentPath == null ? 0.0 : score(currentPath);
		return candidate > current + 1.0e-9;
	}
}
