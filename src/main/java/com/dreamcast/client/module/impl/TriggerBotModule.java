package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import com.dreamcast.client.settings.IntSetting;
import com.dreamcast.client.system.FriendsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

/**
 * TriggerBot — бьёт цель, которая уже под прицелом.
 *
 * В отличие от KillAura не наводит камеру: игрок целится сам, модуль лишь
 * наносит удар с задержкой (или мгновенно). Удобно как «помощь прицелу»
 * в легитной игре и как анти-промах.
 */
public class TriggerBotModule extends Module {

	private final IntSetting delay = intSetting("delay", "Задержка, тиков", 4, 0, 20);
	private final BooleanSetting players = bool("players", "Игроки", true);
	private final BooleanSetting mobs = bool("mobs", "Враждебные мобы", true);
	private final BooleanSetting animals = bool("animals", "Живность", false);
	private final BooleanSetting skipFriends = bool("skip_friends", "Не бить друзей", true);
	private final BooleanSetting fullCharge = bool("full_charge", "Ждать полную силу удара", true);

	private int cooldown;

	public TriggerBotModule() {
		super("trigger_bot", "TriggerBot", "Автоматический удар по цели под прицелом",
				ModuleCategory.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
			return;
		}
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		Entity target = client.crosshairPickEntity;
		if (target == null || !isValid(player, target)) {
			return;
		}
		if (fullCharge.isEnabled() && player.getAttackStrengthScale(0.0F) < 0.95F) {
			return;
		}
		client.gameMode.attack(player, target);
		player.swing(InteractionHand.MAIN_HAND);
		cooldown = Math.max(1, delay.get());
	}

	private boolean isValid(LocalPlayer player, Entity target) {
		if (target == player || !target.isAlive() || target.isSpectator() || target instanceof ArmorStand) {
			return false;
		}
		if (skipFriends.isEnabled() && FriendsManager.isFriend(target.getName().getString())) {
			return false;
		}
		if (target instanceof Player) {
			return players.isEnabled();
		}
		if (target.getType().getCategory() == MobCategory.MONSTER) {
			return mobs.isEnabled();
		}
		return target instanceof LivingEntity && animals.isEnabled();
	}
}
