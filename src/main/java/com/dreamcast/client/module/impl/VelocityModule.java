package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.IntSetting;
import com.dreamcast.client.settings.ModeSetting;
import com.dreamcast.client.util.VelocityMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Velocity (анти-нокбэк) — гасит или ослабляет отбрасывание от ударов
 * и взрывов.
 *
 * Работает «по факту применения»: серверные пакеты motion/explosion проходят
 * как обычно, но сразу после них модуль корректирует вектор скорости нашего
 * игрока (хук в ClientPacketListenerMixin). Так не ломается обработка пакетов
 * для других сущностей и не требуется подмена входящих данных.
 *
 * Проценты как в Meteor: 0 — нокбэк погашен полностью, 100 — как в ванили.
 */
public class VelocityModule extends Module {

	private final ModeSetting mode = mode("mode", "Режим", "percent",
			ModeSetting.option("percent", "Проценты"),
			ModeSetting.option("cancel", "Полное гашение"));

	private final IntSetting horizontal = intSetting("horizontal", "Горизонталь, %", 0, 0, 200);
	private final IntSetting vertical = intSetting("vertical", "Вертикаль, %", 0, 0, 200);

	/** Нокбэк в воде почти не мешает — можно не трогать. */
	private final com.dreamcast.client.settings.BooleanSetting ignoreWater =
			bool("ignore_water", "Не работать в воде", false);

	public VelocityModule() {
		super("velocity", "Velocity", "Анти-нокбэк: гасит отбрасывание от ударов и взрывов",
				ModuleCategory.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
	}

	// ------------------------------------------------------------------
	// Хуки из миксина ClientPacketListener
	// ------------------------------------------------------------------

	/** Серверный motion-пакет применён к сущности с этим id. */
	public static void onMotionApplied(int entityId) {
		VelocityModule module = self();
		if (module == null || !module.isEnabled()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || player.getId() != entityId) {
			return;
		}
		module.applyToPlayer(player);
	}

	/** Взрыв обработан; knockback относится к нашему игроку. */
	public static void onExplosionApplied(boolean knockbackForPlayer) {
		if (!knockbackForPlayer) {
			return;
		}
		VelocityModule module = self();
		if (module == null || !module.isEnabled()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player != null) {
			module.applyToPlayer(player);
		}
	}

	private static VelocityModule self() {
		return com.dreamcast.client.module.ModuleManager.find(VelocityModule.class);
	}

	private void applyToPlayer(LocalPlayer player) {
		if (ignoreWater.isEnabled() && (player.isInWater() || player.isInLava())) {
			return;
		}
		Vec3 motion = player.getDeltaMovement();
		if (!VelocityMath.worthApplying(motion.x, motion.y, motion.z)) {
			return;
		}
		double[] result = mode.is("cancel")
				? VelocityMath.cancel()
				: VelocityMath.scale(motion.x, motion.y, motion.z, horizontal.get(), vertical.get());
		player.setDeltaMovement(result[0], result[1], result[2]);
	}
}
