package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import com.dreamcast.client.settings.ColorSetting;
import com.dreamcast.client.settings.IntSetting;
import com.dreamcast.client.system.FriendsManager;
import com.dreamcast.client.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracers — лучи от игрока к сущностям.
 *
 * Мгновенно видно, кто где: линия тянется от нижней точки экрана (или от
 * ног игрока — режим «Мир») к ногам цели. Цвет: свой для друзей, свой для
 * враждебных, общий для остальных.
 *
 * Данные собираются в тике и рисуются world-рендером (WorldRenderHook).
 */
public class TracersModule extends Module {

	private final BooleanSetting players = bool("players", "Игроки", true);
	private final BooleanSetting mobs = bool("mobs", "Враждебные мобы", true);
	private final BooleanSetting creatures = bool("creatures", "Живность", false);
	private final IntSetting distance = intSetting("distance", "Дальность, блоков", 64, 8, 128);
	private final ColorSetting color = colorSetting("color", "Цвет", 0xFF7C6CFF);
	private final ColorSetting friendColor = colorSetting("friend_color", "Цвет друзей", 0xFF6BE08A);
	private final ColorSetting mobColor = colorSetting("mob_color", "Цвет мобов", 0xFFFF6C6C);
	private final BooleanSetting fromFeet = bool("from_feet", "Луч от ног (а не от экрана)", false);

	/** Одна линия трассера в мировых координатах. */
	public record TracerLine(float x, float y, float z, int color) {
	}

	/** Снапшот для рендера: атомарно подменяется каждый тик. */
	private volatile List<TracerLine> lines = List.of();

	public TracersModule() {
		super("tracers", "Tracers", "Лучи к игрокам и мобам сквозь стены",
				ModuleCategory.RENDER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onDisable() {
		lines = List.of();
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || client.level == null) {
			lines = List.of();
			return;
		}
		List<TracerLine> collected = new ArrayList<>();
		double maxSqr = (double) distance.get() * distance.get();
		for (Entity entity : client.level.entitiesForRendering()) {
			if (!isTarget(player, entity)) {
				continue;
			}
			Vec3 pos = entity.position();
			double distanceSqr = player.distanceToSqr(entity);
			if (distanceSqr > maxSqr) {
				continue;
			}
			collected.add(new TracerLine((float) pos.x, (float) pos.y, (float) pos.z, colorFor(entity)));
		}
		lines = List.copyOf(collected);
	}

	/** Снапшот линий для world-рендера. */
	public List<TracerLine> currentLines() {
		return lines;
	}

	public boolean linesFromFeet() {
		return fromFeet.isEnabled();
	}

	private int colorFor(Entity entity) {
		if (entity instanceof Player && FriendsManager.isFriend(entity.getName().getString())) {
			return friendColor.get();
		}
		if (entity.getType().getCategory() == MobCategory.MONSTER) {
			return mobColor.get();
		}
		return color.get();
	}

	private boolean isTarget(LocalPlayer player, Entity entity) {
		if (entity == player || !entity.isAlive() || entity.isSpectator() || entity instanceof ArmorStand) {
			return false;
		}
		if (entity instanceof Player) {
			return players.isEnabled();
		}
		if (entity.getType().getCategory() == MobCategory.MONSTER) {
			return mobs.isEnabled();
		}
		return entity instanceof LivingEntity && creatures.isEnabled();
	}

	/** Плавный цвет по дистанции — зарезервировано для будущих тем. */
	int distanceFade(int base, double distance, double maxDistance) {
		float t = (float) Math.min(1.0, distance / Math.max(1.0, maxDistance));
		return RenderUtils.withAlpha(base, 1.0F - t * 0.6F);
	}
}
