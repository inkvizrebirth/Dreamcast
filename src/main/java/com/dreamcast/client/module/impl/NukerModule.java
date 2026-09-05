package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.BlockListSetting;
import com.dreamcast.client.settings.BooleanSetting;
import com.dreamcast.client.settings.IntSetting;
import com.dreamcast.client.settings.ModeSetting;
import com.dreamcast.client.util.KeyOwnership;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Nuker — сносит все блоки вокруг игрока.
 *
 * Каждый тик выбирает ближайший подходящий блок в радиусе и копает его
 * штатными вызовами gameMode (startDestroyBlock / continueDestroyBlock) —
 * ровно тем же путём, которым копает игрок. Фильтр режимов:
 * <ul>
 *   <li><b>Все</b> — любой разрушимый блок;</li>
 *   <li><b>Список</b> — только блоки из выбранного списка (как BlockESP).</li>
 * </ul>
 *
 * Бедрок и прочие неразрушимые блоки (скорость добычи &lt; 0) не трогаются.
 */
public class NukerModule extends Module {

	private final ModeSetting filter = mode("filter", "Фильтр", "all",
			ModeSetting.option("all", "Все блоки"),
			ModeSetting.option("list", "По списку"));

	private final IntSetting radius = intSetting("radius", "Радиус, блоков", 4, 1, 5);
	private final BooleanSetting swingHand = bool("swing", "Махать рукой", true);

	private final BlockListSetting blocks = addSetting(new BlockListSetting("blocks", "Блоки",
			"dirt", "grass_block", "stone", "cobblestone"));

	private BlockPos current;

	public NukerModule() {
		super("nuker", "Nuker", "Сносит блоки вокруг игрока",
				ModuleCategory.WORLD, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onDisable() {
		current = null;
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || client.level == null || client.gameMode == null || client.screen != null) {
			current = null;
			return;
		}

		BlockPos target = findTarget(client, player);
		if (target == null) {
			current = null;
			return;
		}

		if (!target.equals(current)) {
			current = target;
			client.gameMode.startDestroyBlock(target, Direction.UP);
		} else {
			client.gameMode.continueDestroyBlock(target, Direction.UP);
		}
		if (swingHand.isEnabled()) {
			player.swing(InteractionHand.MAIN_HAND);
		}
	}

	private BlockPos findTarget(Minecraft client, LocalPlayer player) {
		Vec3 eyes = player.getEyePosition();
		int range = radius.get();
		double maxSqr = (double) (range + 1) * (range + 1);
		BlockPos center = player.blockPosition();
		BlockPos best = null;
		double bestScore = Double.MAX_VALUE;

		for (BlockPos pos : BlockPos.betweenClosed(
				center.getX() - range, center.getY() - range, center.getZ() - range,
				center.getX() + range, center.getY() + range, center.getZ() + range)) {
			var state = client.level.getBlockState(pos);
			if (state.isAir() || !state.getFluidState().isEmpty()) {
				continue;
			}
			if (state.getDestroySpeed(client.level, pos) < 0.0F) {
				continue; // неразрушимое
			}
			if (filter.is("list")) {
				String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
				if (!blocks.isSelected(path)) {
					continue;
				}
			}
			// Ближе к уровню глаз — в приоритете (копать под собой неудобно)
			double dx = pos.getX() + 0.5 - eyes.x;
			double dy = pos.getY() + 0.5 - eyes.y;
			double dz = pos.getZ() + 0.5 - eyes.z;
			double distanceSqr = dx * dx + dy * dy + dz * dz;
			if (distanceSqr > maxSqr) {
				continue;
			}
			if (distanceSqr < bestScore) {
				bestScore = distanceSqr;
				best = pos.immutable();
			}
		}
		return best;
	}
}
