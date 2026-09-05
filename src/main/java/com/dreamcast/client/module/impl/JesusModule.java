package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Jesus — ходьба по воде (и лаве).
 *
 * Пока игрок на поверхности жидкости и не приседает, модуль гасит падение
 * и чуть поддергивает его вверх: получается «твёрдая» поверхность, по которой
 * можно бежать. Присед — нырнуть, как в ванили. Под водой модуль не мешает:
 * работает только когда голова над поверхностью.
 */
public class JesusModule extends Module {

	private final BooleanSetting lava = bool("lava", "Работать на лаве", true);

	/** Мини-импульс, удерживающий игрока на гребне поверхности. */
	private static final double BUOYANCY = 0.06;

	public JesusModule() {
		super("jesus", "Jesus", "Ходьба по воде и лаве",
				ModuleCategory.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || player.isPassenger() || player.isFallFlying()) {
			return;
		}
		boolean inWater = player.isInWater();
		boolean inLava = player.isInLava();
		if (!inWater && !inLava) {
			return;
		}
		if (inLava && !inWater && !lava.isEnabled()) {
			return;
		}
		// Полное погружение — не наш случай, там игрок плавает сам
		if (player.isUnderWater()) {
			return;
		}
		// Присел — значит, игрок сам хочет вниз
		if (player.isShiftKeyDown()) {
			return;
		}

		Vec3 motion = player.getDeltaMovement();
		if (motion.y < BUOYANCY) {
			player.setDeltaMovement(motion.x, BUOYANCY, motion.z);
		}
		// Падение в «поверхность» не должно накапливать урон
		if (player.fallDistance > 1.0F) {
			player.fallDistance = 0.0F;
		}
	}
}
