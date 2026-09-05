package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.ModeSetting;
import com.dreamcast.client.util.KeyOwnership;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.lwjgl.glfw.GLFW;

/**
 * Sneak — постоянное приседание.
 *
 * <ul>
 *   <li><b>Обычный</b> — клиент реально держит Shift: ниже хитбокс,
 *       медленнее ходьба, не падаешь с краёв;</li>
 *   <li><b>Пакетный</b> — серверу уходит «игрок присел», а клиент стоит
 *       прямо: защита от падения с краёв работает, скорость не теряется.</li>
 * </ul>
 */
public class SneakModule extends Module {

	private final ModeSetting mode = mode("mode", "Режим", "packet",
			ModeSetting.option("packet", "Пакетный"),
			ModeSetting.option("vanilla", "Обычный"));

	private boolean holding;
	private boolean sentPacket;

	public SneakModule() {
		super("sneak", "Sneak", "Постоянное приседание (обычное или пакетное)",
				ModuleCategory.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onEnable() {
		if (mode.is("packet")) {
			sendSneak(true);
		} else {
			holdShift(true);
		}
	}

	@Override
	protected void onDisable() {
		holdShift(false);
		sendSneak(false);
	}

	@Override
	public void onSettingsChanged() {
		// Смена режима на лету: выключаем старый, включаем новый
		if (!isEnabled()) {
			return;
		}
		holdShift(false);
		sendSneak(false);
		if (mode.is("packet")) {
			sendSneak(true);
		} else {
			holdShift(true);
		}
	}

	private void holdShift(boolean on) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		if (on && !holding) {
			KeyOwnership.hold(client, client.options.keyShift, this);
			holding = true;
		} else if (!on && holding) {
			KeyOwnership.releaseHold(client, client.options.keyShift, this);
			holding = false;
		}
	}

	private void sendSneak(boolean sneaking) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || client.getConnection() == null || sentPacket == sneaking) {
			return;
		}
		client.getConnection().send(new ServerboundPlayerActionPacket(
				sneaking
						? ServerboundPlayerActionPacket.Action.START_SNEAKING
						: ServerboundPlayerActionPacket.Action.STOP_SNEAKING,
				net.minecraft.core.BlockPos.ZERO,
				net.minecraft.core.Direction.DOWN));
		sentPacket = sneaking;
	}
}
