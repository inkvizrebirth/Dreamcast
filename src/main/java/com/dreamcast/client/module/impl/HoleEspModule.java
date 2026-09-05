package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BooleanSetting;
import com.dreamcast.client.settings.ColorSetting;
import com.dreamcast.client.settings.IntSetting;
import com.dreamcast.client.util.HoleScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * HoleESP — подсвечивает «дырки» 1×1, в которых не берут кристаллы:
 * идеальные укрытия в PvP. Логика поиска — HoleScanner (покрыта тестами).
 *
 * Безопасные дырки (бедрок/обсидиан со всех сторон) красятся отдельно от
 * обычных (где есть обычный камень — такие взрываются кристаллом).
 */
public class HoleEspModule extends Module {

	private final IntSetting radius = intSetting("radius", "Радиус, блоков", 8, 3, 16);
	private final IntSetting vertical = intSetting("vertical", "По вертикали, блоков", 3, 1, 8);
	private final BooleanSetting onlySafe = bool("only_safe", "Только безопасные", false);
	private final ColorSetting safeColor = colorSetting("safe_color", "Безопасные", 0xFF57E389);
	private final ColorSetting unsafeColor = colorSetting("unsafe_color", "Обычные", 0xFF5C9DFF);

	/** Одна дырка для рендера: координаты + безопасность. */
	public record HoleBox(float x, float y, float z, boolean safe) {
	}

	private volatile List<HoleBox> holes = List.of();
	private int scanCooldown;

	public HoleEspModule() {
		super("hole_esp", "HoleESP", "Подсветка безопасных дырок 1×1",
				ModuleCategory.RENDER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onDisable() {
		holes = List.of();
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || client.level == null) {
			holes = List.of();
			return;
		}
		// Сканирование — дорогое: раз в 10 тиков достаточно
		if (--scanCooldown > 0) {
			return;
		}
		scanCooldown = 10;

		BlockPos center = player.blockPosition();
		HoleScanner.Probe probe = new HoleScanner.Probe() {
			@Override
			public boolean isSolid(int x, int y, int z) {
				BlockState state = client.level.getBlockState(new BlockPos(x, y, z));
				return !state.getCollisionShape(client.level, new BlockPos(x, y, z)).isEmpty();
			}

			@Override
			public boolean isSafeMaterial(int x, int y, int z) {
				BlockState state = client.level.getBlockState(new BlockPos(x, y, z));
				return state.is(net.minecraft.world.level.block.Blocks.OBSIDIAN)
						|| state.is(net.minecraft.world.level.block.Blocks.BEDROCK);
			}
		};

		List<HoleScanner.Hole> found = HoleScanner.scan(probe,
				center.getX(), center.getY(), center.getZ(),
				radius.get(), vertical.get(), onlySafe.isEnabled());

		List<HoleBox> boxes = new ArrayList<>(found.size());
		for (HoleScanner.Hole hole : found) {
			boxes.add(new HoleBox(hole.x(), hole.y(), hole.z(), hole.safe()));
		}
		holes = List.copyOf(boxes);
	}

	public List<HoleBox> currentHoles() {
		return holes;
	}

	public int colorFor(boolean safe) {
		return safe ? safeColor.get() : unsafeColor.get();
	}
}
