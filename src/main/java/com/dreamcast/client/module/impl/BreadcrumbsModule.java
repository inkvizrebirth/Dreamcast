package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.ColorSetting;
import com.dreamcast.client.settings.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Breadcrumbs — «хлебные крошки»: линия всего пройденного пути.
 *
 * Незаменима в пещерах и Незере: видно, откуда пришёл и где петлял.
 * Точки пишутся раз в несколько тиков и живут заданное время, потом
 * растворяются с хвоста.
 */
public class BreadcrumbsModule extends Module {

	private final IntSetting maxPoints = intSetting("points", "Длина следа, точек", 300, 50, 1000);
	private final IntSetting sampleTicks = intSetting("sample", "Шаг записи, тиков", 3, 1, 20);
	private final ColorSetting color = colorSetting("color", "Цвет", 0xFFFFC66C);

	/** Точка следа. */
	public record Crumb(float x, float y, float z) {
	}

	private final Deque<Crumb> crumbs = new ArrayDeque<>();
	private volatile List<Crumb> snapshot = List.of();
	private int tickCounter;

	public BreadcrumbsModule() {
		super("breadcrumbs", "Breadcrumbs", "Линия пройденного пути",
				ModuleCategory.RENDER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onEnable() {
		synchronized (crumbs) {
			crumbs.clear();
		}
		snapshot = List.of();
	}

	@Override
	protected void onDisable() {
		synchronized (crumbs) {
			crumbs.clear();
		}
		snapshot = List.of();
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null) {
			return;
		}
		if (++tickCounter < sampleTicks.get()) {
			return;
		}
		tickCounter = 0;

		Vec3 pos = player.position();
		synchronized (crumbs) {
			crumbs.addLast(new Crumb((float) pos.x, (float) pos.y, (float) pos.z));
			while (crumbs.size() > maxPoints.get()) {
				crumbs.removeFirst();
			}
			snapshot = List.copyOf(crumbs);
		}
	}

	/** Снапшот следа для world-рендера. */
	public List<Crumb> currentCrumbs() {
		return snapshot;
	}

	public int trailColor() {
		return color.get();
	}

	/** Число точек — для HUD/отладки. */
	public int size() {
		List<Crumb> current = snapshot;
		return current == null ? 0 : current.size();
	}
}
