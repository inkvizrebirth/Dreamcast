package com.dreamcast.client.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Поиск «дырок» 1×1 для HoleESP (как в Meteor). Чистая логика — покрыта
 * HoleScannerTest: мир передаётся через интерфейс-пробник, поэтому тесты
 * не тянут классы Minecraft.
 *
 * Дыркой считается колонка, где:
 * <ul>
 *   <li>два блока на уровне ног и головы проходимы;</li>
 *   <li>пол твёрдый;</li>
 *   <li>все четыре соседа твёрдые (в режиме «безопасные» — только
 *       неразрушимые: бедрок/обсидиан).</li>
 * </ul>
 */
public final class HoleScanner {

	/**
	 * Пробник мира: что известно о блоке в координатах.
	 */
	public interface Probe {
		/** Блок непроходим (в нём нельзя стоять). */
		boolean isSolid(int x, int y, int z);

		/** Блок «безопасного» материала (бедрок, обсидиан). */
		boolean isSafeMaterial(int x, int y, int z);
	}

	private HoleScanner() {
	}

	/** Одна найденная дырка: координаты блока на уровне ног. */
	public record Hole(int x, int y, int z, boolean safe) {
	}

	/**
	 * Сканирует область вокруг точки.
	 *
	 * @param cx,cy,cz    центр сканирования (обычно позиция игрока, y — уровень ног)
	 * @param radius      радиус по XZ
	 * @param vertical    радиус по Y (в обе стороны)
	 * @param requireSafe искать только безопасные дырки
	 */
	public static List<Hole> scan(Probe probe, int cx, int cy, int cz,
	                              int radius, int vertical, boolean requireSafe) {
		List<Hole> holes = new ArrayList<>();
		for (int x = cx - radius; x <= cx + radius; x++) {
			for (int z = cz - radius; z <= cz + radius; z++) {
				for (int y = cy - vertical; y <= cy + vertical; y++) {
					Hole hole = check(probe, x, y, z, requireSafe);
					if (hole != null) {
						holes.add(hole);
					}
				}
			}
		}
		return holes;
	}

	/** Проверяет одну колонку; null — это не дырка. */
	public static Hole check(Probe probe, int x, int y, int z, boolean requireSafe) {
		if (probe.isSolid(x, y, z) || probe.isSolid(x, y + 1, z)) {
			return null;
		}
		if (!probe.isSolid(x, y - 1, z)) {
			return null;
		}
		boolean allSafe = true;
		boolean anySolidSide = false;
		int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (int[] side : sides) {
			int sx = x + side[0];
			int sz = z + side[1];
			if (!probe.isSolid(sx, y, sz)) {
				return null;
			}
			anySolidSide = true;
			if (!probe.isSafeMaterial(sx, y, sz)) {
				allSafe = false;
			}
		}
		if (!anySolidSide) {
			return null;
		}
		if (requireSafe && !allSafe) {
			return null;
		}
		return new Hole(x, y, z, allSafe);
	}
}
