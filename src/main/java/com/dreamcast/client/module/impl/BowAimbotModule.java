package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import com.dreamcast.client.settings.IntSetting;
import com.dreamcast.client.system.FriendsManager;
import com.dreamcast.client.util.BowAimLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * BowAimbot — наводит лук точно в цель с поправкой на баллистику.
 *
 * Пока игрок натягивает лук, модуль каждый тик считает, под каким углом
 * стрела долетит до выбранной цели (гравитация и сопротивление воздуха —
 * ванильные), и доворачивает прицел. Стреляет игрок сам: отпустить тетиву —
 * его решение.
 */
public class BowAimbotModule extends Module {

	private final IntSetting range = intSetting("range", "Дальность, блоков", 20, 5, 60);
	private final BooleanSetting players = bool("players", "Игроки", true);
	private final BooleanSetting mobs = bool("mobs", "Враждебные мобы", false);
	private final BooleanSetting skipFriends = bool("skip_friends", "Не целиться в друзей", true);
	private final BooleanSetting throughWalls = bool("walls", "Через стены", false);

	public BowAimbotModule() {
		super("bow_aimbot", "BowAimbot", "Автонаведение лука с поправкой на баллистику",
				ModuleCategory.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || client.level == null) {
			return;
		}
		if (!player.isUsingItem()) {
			return;
		}
		ItemStack using = player.getUseItem();
		if (using.getItem() != Items.BOW) {
			return;
		}
		Entity target = findTarget(client, player);
		if (target == null) {
			return;
		}

		Vec3 eye = player.getEyePosition();
		AABB box = target.getBoundingBox();
		// Целимся в центр туловища — самая прощающая точка хитбокса
		double targetX = (box.minX + box.maxX) * 0.5;
		double targetY = box.minY + (box.maxY - box.minY) * 0.55;
		double targetZ = (box.minZ + box.maxZ) * 0.5;

		double dx = targetX - eye.x;
		double dy = targetY - eye.y;
		double dz = targetZ - eye.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);

		double velocity = BowAimLogic.chargeVelocity(player.getTicksUsingItem());
		Double pitch = BowAimLogic.solvePitch(horizontal, dy, velocity);
		if (pitch == null) {
			return; // недолёт — оставляем игрока в покое
		}

		player.setYRot(BowAimLogic.yawTo(dx, dz));
		player.setXRot(Mth.clamp(pitch.floatValue(), -90.0F, 90.0F));
	}

	private Entity findTarget(Minecraft client, LocalPlayer player) {
		Entity best = null;
		double bestDistanceSqr = Double.MAX_VALUE;
		double maxSqr = (double) range.get() * range.get();
		for (Entity entity : client.level.entitiesForRendering()) {
			if (!isValid(player, entity)) {
				continue;
			}
			double distanceSqr = player.distanceToSqr(entity);
			if (distanceSqr <= maxSqr && distanceSqr < bestDistanceSqr) {
				bestDistanceSqr = distanceSqr;
				best = entity;
			}
		}
		return best;
	}

	private boolean isValid(LocalPlayer player, Entity entity) {
		if (entity == player || !entity.isAlive() || entity.isSpectator() || entity instanceof ArmorStand) {
			return false;
		}
		if (skipFriends.isEnabled() && FriendsManager.isFriend(entity.getName().getString())) {
			return false;
		}
		if (entity instanceof Player) {
			if (!players.isEnabled()) {
				return false;
			}
		} else if (entity.getType().getCategory() == MobCategory.MONSTER) {
			if (!mobs.isEnabled()) {
				return false;
			}
		} else if (!(entity instanceof LivingEntity)) {
			return false;
		} else {
			return false;
		}
		return throughWalls.isEnabled() || player.hasLineOfSight(entity);
	}

	/** Путь предмета в реестре (для отладки и тестов). */
	static String registryPath(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
	}
}
