package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import com.dreamcast.client.settings.IntSetting;
import com.dreamcast.client.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * LongJump — дальний прыжок-рывок.
 *
 * Включение (или клавиша) даёт один мощный импульс в сторону взгляда.
 * Режимы:
 * <ul>
 *   <li><b>Толчок</b> — импульс с земли, работает только стоя на опоре;</li>
 *   <li><b>Полёт</b> — короткий управляемый рывок в воздухе (2 тика тяги),
 *       удобен с элитрами и для перелёта ям.</li>
 * </ul>
 */
public class LongJumpModule extends Module {

	private final ModeSetting mode = mode("mode", "Режим", "boost",
			ModeSetting.option("boost", "Толчок"),
			ModeSetting.option("fly", "Полёт"));

	private final IntSetting power = intSetting("power", "Мощность, %", 130, 50, 300);
	private final BooleanSetting autoDisable = bool("auto_disable", "Выключаться после рывка", true);

	private int flyTicks;

	public LongJumpModule() {
		super("long_jump", "LongJump", "Дальний прыжок-рывок в сторону взгляда",
				ModuleCategory.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onEnable() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null) {
			return;
		}
		double factor = power.get() / 100.0;
		if (mode.is("boost")) {
			if (!player.onGround()) {
				return;
			}
			// Импульс по направлению взгляда + сильный подрыв вверх.
			// Сначала прыжок (он ставит y), затем наш вектор — иначе ваниль
			// перезатрёт вертикальную составляющую
			double yaw = Math.toRadians(player.getYRot());
			double forwardX = -Mth.sin((float) yaw);
			double forwardZ = Mth.cos((float) yaw);
			player.jumpFromGround();
			player.setDeltaMovement(forwardX * 0.9 * factor, 0.52 * factor, forwardZ * 0.9 * factor);
			finish();
		} else {
			flyTicks = 10;
		}
	}

	@Override
	public void tick() {
		if (flyTicks <= 0) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null) {
			flyTicks = 0;
			return;
		}
		flyTicks--;
		if (player.onGround() && flyTicks < 8) {
			// Приземлились — рывок закончен
			flyTicks = 0;
			finish();
			return;
		}
		double yaw = Math.toRadians(player.getYRot());
		double forwardX = -Mth.sin((float) yaw);
		double forwardZ = Mth.cos((float) yaw);
		Vec3 motion = player.getDeltaMovement();
		double factor = power.get() / 100.0 * 0.35;
		player.setDeltaMovement(
				motion.x + forwardX * factor * 0.4,
				Math.max(motion.y, -0.05),
				motion.z + forwardZ * factor * 0.4);
		if (flyTicks == 0) {
			finish();
		}
	}

	@Override
	protected void onDisable() {
		flyTicks = 0;
	}

	private void finish() {
		if (autoDisable.isEnabled()) {
			setEnabledSilently(false);
		}
	}
}
