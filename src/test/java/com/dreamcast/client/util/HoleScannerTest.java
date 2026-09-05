package com.dreamcast.client.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HoleESP: поиск дырок 1×1 на синтетическом мире из «сетов» блоков.
 */
class HoleScannerTest {

	/** Пробник по набору твёрдых и безопасных позиций. */
	private static HoleScanner.Probe world(Set<String> solid, Set<String> safe) {
		return new HoleScanner.Probe() {
			@Override
			public boolean isSolid(int x, int y, int z) {
				return solid.contains(x + "," + y + "," + z);
			}

			@Override
			public boolean isSafeMaterial(int x, int y, int z) {
				return safe.contains(x + "," + y + "," + z);
			}
		};
	}

	private static String pos(int x, int y, int z) {
		return x + "," + y + "," + z;
	}

	/** Дырка в (0,10,0): пол y=9, стены по четырём сторонам. */
	private static Set<String> fullHole(String material) {
		return Set.of(
				pos(0, 9, 0),
				pos(1, 10, 0), pos(-1, 10, 0), pos(0, 10, 1), pos(0, 10, -1));
	}

	@Test
	void findsClassicObsidianHole() {
		Set<String> solid = fullHole("obsidian");
		Set<String> safe = Set.of(pos(1, 10, 0), pos(-1, 10, 0), pos(0, 10, 1), pos(0, 10, -1));
		HoleScanner.Hole hole = HoleScanner.check(world(solid, safe), 0, 10, 0, false);
		assertNotNull(hole);
		assertTrue(hole.safe(), "обсидиан со всех сторон — безопасная дырка");
	}

	@Test
	void cobbleHoleIsUnsafe() {
		Set<String> solid = fullHole("cobble");
		HoleScanner.Hole hole = HoleScanner.check(world(solid, Set.of()), 0, 10, 0, false);
		assertNotNull(hole);
		assertTrue(!hole.safe(), "булыжник — обычная дырка");
		// В режиме «только безопасные» такая не проходит
		assertNull(HoleScanner.check(world(solid, Set.of()), 0, 10, 0, true));
	}

	@Test
	void missingSideIsNotAHole() {
		Set<String> solid = Set.of(
				pos(0, 9, 0),
				pos(1, 10, 0), pos(-1, 10, 0), pos(0, 10, 1)); // нет четвёртой стены
		assertNull(HoleScanner.check(world(solid, Set.of()), 0, 10, 0, false));
	}

	@Test
	void noFloorIsNotAHole() {
		Set<String> solid = Set.of(
				pos(1, 10, 0), pos(-1, 10, 0), pos(0, 10, 1), pos(0, 10, -1));
		assertNull(HoleScanner.check(world(solid, Set.of()), 0, 10, 0, false));
	}

	@Test
	void blockedHeadIsNotAHole() {
		Set<String> solid = new java.util.HashSet<>(fullHole("obsidian"));
		solid.add(pos(0, 11, 0)); // блок над головой
		assertNull(HoleScanner.check(world(solid, Set.of()), 0, 10, 0, false));
	}

	@Test
	void scanCollectsAllHolesInRadius() {
		Set<String> solid = new java.util.HashSet<>(fullHole("obsidian"));
		// Вторая дырка рядом в (3,10,0)
		solid.add(pos(3, 9, 0));
		solid.add(pos(4, 10, 0));
		solid.add(pos(2, 10, 0));
		solid.add(pos(3, 10, 1));
		solid.add(pos(3, 10, -1));
		List<HoleScanner.Hole> holes = HoleScanner.scan(world(solid, Set.of()),
				0, 10, 0, 4, 1, false);
		assertEquals(2, holes.size(), "обе дырки найдены: " + holes);
	}
}
