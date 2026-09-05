package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import com.dreamcast.client.system.FriendsManager;
import org.lwjgl.glfw.GLFW;

/**
 * Friends — система друзей клиента (как в Meteor).
 *
 * Пока модуль включён:
 * <ul>
 *   <li>KillAura и TriggerBot не атакуют друзей;</li>
 *   <li>ESP/Tracers красят друзей зелёным;</li>
 *   <li>Nametags вешают друзьям метку.</li>
 * </ul>
 *
 * Список правится командой «.friend add|remove|list» и хранится в
 * config/dreamcast/friends.json. Выключение модуля — быстрый способ
 * «разрешить бить всех», не чистя список.
 */
public class FriendsModule extends Module {

	private final BooleanSetting protectFromAura = bool("protect_aura", "KillAura не бьёт друзей", true);
	private final BooleanSetting highlight = bool("highlight", "Подсвечивать в ESP/Tracers", true);

	public FriendsModule() {
		super("friends", "Friends", "Список друзей: их не бьёт аура и видно в ESP",
				ModuleCategory.MISC, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected boolean defaultEnabled() {
		return true;
	}

	/** Единая точка проверки «друг ли это» для всех модулей. */
	public static boolean isFriend(String name) {
		FriendsModule module = com.dreamcast.client.module.ModuleManager.find(FriendsModule.class);
		if (module == null || !module.isEnabled()) {
			return false;
		}
		return FriendsManager.isFriend(name);
	}

	public static boolean auraProtects() {
		FriendsModule module = com.dreamcast.client.module.ModuleManager.find(FriendsModule.class);
		return module != null && module.isEnabled() && module.protectFromAura.isEnabled();
	}

	public static boolean highlights() {
		FriendsModule module = com.dreamcast.client.module.ModuleManager.find(FriendsModule.class);
		return module != null && module.isEnabled() && module.highlight.isEnabled();
	}
}
