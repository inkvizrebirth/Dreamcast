package com.dreamcast.client.session;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Счётчик сессии: убийства (игроков и мобов) и количество ударов.
 *
 * Удары приходят из хука атаки, а убийство засчитывается на следующем тике,
 * если цель удара перестала существовать/умерла. Всё живёт только в памяти
 * и сбрасывается при смене мира или игрока (см. HUD-элемент «Сессия»).
 */
public final class SessionStats {

	private static long attacks;
	private static long kills;

	/** Последняя атакованная цель и время удара. */
	private static Entity lastTarget;
	private static long lastAttackAt;

	private SessionStats() {
	}

	public static synchronized void registerAttack(Entity target) {
		attacks++;
		lastTarget = target;
		lastAttackAt = System.currentTimeMillis();
	}

	/** Вызывается каждый тик: проверяет, не умерла ли последняя цель. */
	public static synchronized void tick() {
		if (lastTarget == null) {
			return;
		}
		// Цель пропала/умерла в течение 3 секунд после удара — это убийство
		if (System.currentTimeMillis() - lastAttackAt > 3000) {
			lastTarget = null;
			return;
		}
		boolean dead = !lastTarget.isAlive() || lastTarget.isRemoved();
		if (dead) {
			kills++;
			lastTarget = null;
		}
	}

	/** Сброс при смене мира/игрока. */
	public static synchronized void reset() {
		attacks = 0;
		kills = 0;
		lastTarget = null;
		lastAttackAt = 0;
	}

	public static synchronized long getAttacks() {
		return attacks;
	}

	public static synchronized long getKills() {
		return kills;
	}

	/** Текущая сессия привязана к этому игроку/миру? */
	public static boolean sameSession(Minecraft client) {
		Player player = client == null ? null : client.player;
		return player != null;
	}
}
