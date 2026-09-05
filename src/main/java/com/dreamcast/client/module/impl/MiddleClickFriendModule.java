package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.system.FriendsManager;
import com.dreamcast.client.util.Notifications;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

/**
 * MiddleClickFriend — добавить/убрать друга средней кнопкой мыши.
 *
 * Навёл прицел на игрока, нажал колесо — он в списке (или наоборот).
 * Работает, когда модуль Friends включён и не открыт экран.
 */
public class MiddleClickFriendModule extends Module {

	private boolean wasPressed;

	public MiddleClickFriendModule() {
		super("middle_click_friend", "MiddleClickFriend", "Средний клик по игроку — добавить в друзья",
				ModuleCategory.MISC, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected boolean defaultEnabled() {
		return true;
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.mouseHandler == null) {
			return;
		}
		boolean pressed = client.mouseHandler.isMiddlePressed() && client.screen == null;
		boolean clicked = pressed && !wasPressed;
		wasPressed = pressed;
		if (!clicked) {
			return;
		}
		Entity target = client.crosshairPickEntity;
		if (!(target instanceof Player) || target == client.player) {
			return;
		}
		String name = target.getName().getString();
		if (FriendsManager.isFriend(name)) {
			FriendsManager.remove(name);
			Notifications.info("Друзья", name + " убран(а) из друзей");
		} else {
			FriendsManager.add(name);
			Notifications.ok("Друзья", name + " теперь друг");
		}
	}
}
