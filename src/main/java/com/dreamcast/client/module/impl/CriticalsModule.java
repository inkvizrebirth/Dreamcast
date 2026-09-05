package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import com.dreamcast.client.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Criticals — критические удары (×1.5 урона) каждым ударом.
 *
 * Крит засчитывается, когда игрок бьёт в воздухе. Режимы:
 * <ul>
 *   <li><b>Пакет</b> — серия мини-пакетов позиции с «отрывом» от земли:
 *       сервер видит прыжок, клиент не двигается вовсе (классика NCP);</li>
 *   <li><b>Мини-прыжок</b> — микро-импульс вверх: заметен глазу, но работает
 *       там, где пакеты режут;</li>
 *   <li><b>Прыжок</b> — полноценный прыжок в момент удара.</li>
 * </ul>
 *
 * Крит не делается, когда он невозможен или вреден: в воде, на лестнице,
 * в полёте на элитрах, в транспорте, при падении и когда сам бьющий уже
 * в воздухе (повторный крит невозможен).
 */
public class CriticalsModule extends Module {

	private final ModeSetting mode = mode("mode", "Режим", "packet",
			ModeSetting.option("packet", "Пакет"),
			ModeSetting.option("mini", "Мини-прыжок"),
			ModeSetting.option("jump", "Прыжок"));

	private final BooleanSetting onlyAura = bool("only_aura", "Только для ударов KillAura", false);

	/** Классические NCP-смещения «прыжка на месте». */
	private static final double[] PACKET_OFFSETS = {0.0625, 0.0, 0.0125, 0.0};
	private static final boolean[] PACKET_GROUND = {true, false, false, false};

	public CriticalsModule() {
		super("criticals", "Criticals", "Критический урон каждым ударом (пакетный мини-прыжок)",
				ModuleCategory.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
	}

	/** Хук из миксина MultiPlayerGameMode#attack — срабатывает на каждый удар игрока. */
	public static void onAttack(LocalPlayer player) {
		CriticalsModule module = self();
		if (module == null || !module.isEnabled() || player == null) {
			return;
		}
		module.doCrit(player);
	}

	private static CriticalsModule self() {
		return com.dreamcast.client.module.ModuleManager.find(CriticalsModule.class);
	}

	private void doCrit(LocalPlayer player) {
		if (!canCrit(player)) {
			return;
		}
		if (onlyAura.isEnabled()) {
			KillAuraModule aura = com.dreamcast.client.module.ModuleManager.find(KillAuraModule.class);
			boolean auraHit = aura != null && aura.isEnabled() && aura.currentTarget() != null;
			if (!auraHit) {
				return;
			}
		}
		Minecraft client = Minecraft.getInstance();
		switch (mode.getValue()) {
			case "packet" -> {
				if (client.getConnection() == null) {
					return;
				}
				Vec3 pos = player.position();
				for (int i = 0; i < PACKET_OFFSETS.length; i++) {
					client.getConnection().send(new ServerboundMovePlayerPacket.Pos(
							pos.x, pos.y + PACKET_OFFSETS[i], pos.z,
							PACKET_GROUND[i], player.horizontalCollision));
				}
			}
			case "mini" -> {
				Vec3 motion = player.getDeltaMovement();
				player.setDeltaMovement(motion.x, 0.085, motion.z);
			}
			case "jump" -> player.jumpFromGround();
			default -> {
			}
		}
	}

	/** Крит возможен только с земли и не в «особых» состояниях. */
	private static boolean canCrit(LocalPlayer player) {
		return player.onGround()
				&& !player.isInWater()
				&& !player.isInLava()
				&& !player.onClimbable()
				&& !player.isFallFlying()
				&& !player.isPassenger()
				&& player.fallDistance <= 0.0F;
	}
}
