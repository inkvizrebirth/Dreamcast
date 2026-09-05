package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import com.dreamcast.client.util.KeyOwnership;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * AutoJump — прыгает сам. Держит клавишу прыжка, пока модуль включён
 * (опция «только в движении» не даёт скакать на месте).
 */
public class AutoJumpModule extends Module {

	private final BooleanSetting onlyMoving = bool("only_moving", "Только в движении", true);

	private boolean holding;

	public AutoJumpModule() {
		super("auto_jump", "AutoJump", "Автоматический прыжок",
				ModuleCategory.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onDisable() {
		release();
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.screen != null) {
			release();
			return;
		}
		boolean moving = !onlyMoving.isEnabled()
				|| client.options.keyUp.isDown() || client.options.keyLeft.isDown()
				|| client.options.keyRight.isDown() || client.options.keyDown.isDown();
		if (moving) {
			if (!holding) {
				KeyOwnership.hold(client, client.options.keyJump, this);
				holding = true;
			}
		} else {
			release();
		}
	}

	private void release() {
		Minecraft client = Minecraft.getInstance();
		if (client != null && holding) {
			KeyOwnership.releaseHold(client, client.options.keyJump, this);
		}
		holding = false;
	}
}
