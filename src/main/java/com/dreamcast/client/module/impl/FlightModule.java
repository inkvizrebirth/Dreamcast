package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import com.dreamcast.client.settings.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Flight — полёт на серверах.
 *
 * Клиент выдаёт себе разрешение на полёт (mayfly) и управляет скоростью:
 * ванильный «креативный» полёт с ускорением сверх стандартного. На выключении
 * все флаги способностей возвращаются как были — игрок не остаётся висеть.
 *
 * Честно предупреждаем: сервер с валидацией движения такой полёт видит.
 * Это инструмент для одиночной игры и снисходительных серверов.
 */
public class FlightModule extends Module {

	private final IntSetting speed = intSetting("speed", "Ускорение, %", 150, 100, 400);
	private final BooleanSetting hover = bool("hover", "Зависать без движения", true);
	private final BooleanSetting keepFlying = bool("keep_flying", "Не терять полёт при посадке", true);

	private boolean savedFlying;
	private boolean savedMayFly;

	public FlightModule() {
		super("flight", "Flight", "Полёт с настраиваемой скоростью",
				ModuleCategory.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onEnable() {
		LocalPlayer player = player();
		if (player == null) {
			return;
		}
		Abilities abilities = player.getAbilities();
		savedFlying = abilities.flying;
		savedMayFly = abilities.mayfly;
		abilities.mayfly = true;
	}

	@Override
	protected void onDisable() {
		LocalPlayer player = player();
		if (player == null) {
			return;
		}
		// Возвращаем способности как были: игрок не остаётся висеть в воздухе.
		// Сервер получит новое состояние обычным пакетом способностей в следующем тике.
		Abilities abilities = player.getAbilities();
		abilities.mayfly = savedMayFly;
		abilities.flying = savedFlying && savedMayFly;
	}

	@Override
	public void tick() {
		LocalPlayer player = player();
		if (player == null) {
			return;
		}
		Abilities abilities = player.getAbilities();
		abilities.mayfly = true;
		if (!abilities.flying && (hover.isEnabled() || keepFlying.isEnabled())) {
			abilities.flying = true;
		}

		Vec3 motion = player.getDeltaMovement();

		// Ускорение: множитель к горизонтальной скорости сверх ванильного полёта
		float percent = speed.get() / 100.0F;
		if (percent > 1.0F && (motion.x != 0.0 || motion.z != 0.0)) {
			player.setDeltaMovement(motion.x * percent, motion.y, motion.z * percent);
			motion = player.getDeltaMovement();
		}

		// Зависание: без клавиш игрок не сползает вниз по инерции
		if (hover.isEnabled() && !hasMovementInput()) {
			player.setDeltaMovement(motion.x, 0.0, motion.z);
		}
	}

	private static boolean hasMovementInput() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.options == null) {
			return false;
		}
		return client.options.keyUp.isDown() || client.options.keyDown.isDown()
				|| client.options.keyLeft.isDown() || client.options.keyRight.isDown();
	}

	private static LocalPlayer player() {
		Minecraft client = Minecraft.getInstance();
		return client == null ? null : client.player;
	}
}
