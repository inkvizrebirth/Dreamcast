package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import com.dreamcast.client.settings.IntSetting;
import com.dreamcast.client.settings.ModeSetting;
import com.dreamcast.client.util.SpeedMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Speed — разгон сверх ванильного бега.
 *
 * <ul>
 *   <li><b>Strafe</b> — держит постоянную целевую скорость: направление
 *       движения сохраняется, модуль скорости приводится к заданному.
 *       Работает и на земле, и в воздухе;</li>
 *   <li><b>BunnyHop</b> — автопрыжок + strafe: «кроличьи прыжки» как в
 *       старых клиентах, чуть легитнее на вид.</li>
 * </ul>
 *
 * Скорость задана в % от ванильного спринт-бега (≈0.286 блока/тик):
 * 100 % — ваниль, 130 % — заметно быстрее.
 */
public class SpeedModule extends Module {

	private final ModeSetting mode = mode("mode", "Режим", "strafe",
			ModeSetting.option("strafe", "Strafe"),
			ModeSetting.option("bunny", "BunnyHop"));

	private final IntSetting speedPercent = intSetting("speed", "Скорость, % от бега", 115, 100, 300);
	private final BooleanSetting onlyOnGround = bool("only_ground", "Только на земле", false);

	/** Ванильная горизонтальная скорость спринта, блоков/тик. */
	private static final double SPRINT_SPEED = 0.2858;

	public SpeedModule() {
		super("speed", "Speed", "Разгон: постоянная целевая скорость движения",
				ModuleCategory.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || player.isPassenger() || player.isFallFlying()) {
			return;
		}
		if (onlyOnGround.isEnabled() && !player.onGround()) {
			return;
		}
		Vec3 motion = player.getDeltaMovement();
		// Без ввода не разгоняем: иначе стоящий игрок поедет по инерции
		if (!SpeedMath.hasInput(player.zza, player.xxa)) {
			return;
		}

		if (mode.is("bunny") && player.onGround()) {
			player.jumpFromGround();
			motion = player.getDeltaMovement();
		}

		double target = SPRINT_SPEED * speedPercent.get() / 100.0;
		double horizontal = SpeedMath.horizontalSpeed(motion.x, motion.z);
		if (horizontal < 1.0e-6) {
			return;
		}
		// Разгоняем только если текущая скорость ниже целевой — тормозить
		// модуль не должен (иначе прыжки с гор превращаются в тычок)
		if (horizontal >= target) {
			return;
		}
		double[] strafe = SpeedMath.strafe(motion.x, motion.y, motion.z, target);
		player.setDeltaMovement(strafe[0], strafe[1], strafe[2]);
	}
}
