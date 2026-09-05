package com.dreamcast.client.module.impl;

import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.settings.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Step — автоподъём на блоки без прыжка (как лестница).
 *
 * Когда игрок на земле упирается в стену и продолжает идти вперёд, модуль
 * ищет в колонке перед ним опору в пределах высоты шага и мгновенно
 * поднимает игрока на неё. Сервер узнаёт о новой позиции обычным пакетом
 * движения в том же тике.
 *
 * Высота 1 блок практически неотличима от ванильного auto-jump; большие
 * значения заметны античитам — используйте с умом.
 */
public class StepModule extends Module {

	private final IntSetting height = intSetting("height", "Высота шага, блоков ×10", 10, 10, 25);

	public StepModule() {
		super("step", "Step", "Автоподъём на блоки без прыжка",
				ModuleCategory.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client == null ? null : client.player;
		if (player == null || client.level == null) {
			return;
		}
		if (!player.onGround() || !player.horizontalCollision || player.isPassenger()) {
			return;
		}
		// Шаг только туда, куда игрок реально идёт
		if (player.zza <= 0.0F && player.xxa == 0.0F) {
			return;
		}

		Vec3 feet = player.position();
		Vec3 look = player.getViewVector(1.0F);
		double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
		if (horizontal < 1.0e-4) {
			return; // смотрим вертикально — «перед» не определить
		}
		double dirX = look.x / horizontal;
		double dirZ = look.z / horizontal;

		// Колонка на полшага впереди: туда мы уперлись
		BlockPos front = BlockPos.containing(feet.x + dirX * 0.65, feet.y, feet.z + dirZ * 0.65);
		int feetY = Mth.floor(feet.y);
		int maxY = Mth.floor(feet.y + height.get() / 10.0);

		for (int y = feetY; y <= maxY; y++) {
			BlockPos candidate = new BlockPos(front.getX(), y, front.getZ());
			if (!isSolid(client, candidate)) {
				continue;
			}
			// Над опорой нужен рост игрока в два блока
			if (!isPassable(client, candidate.above()) || !isPassable(client, candidate.above(2))) {
				continue;
			}
			double newY = candidate.getY() + 1.0;
			if (newY <= feet.y + 0.05) {
				continue; // это не подъём
			}
			// В новом положении сам игрок не должен оказаться в блоках
			BlockPos selfFeet = BlockPos.containing(feet.x, newY + 0.1, feet.z);
			if (!isPassable(client, selfFeet) || !isPassable(client, selfFeet.above())) {
				continue;
			}
			player.setPos(feet.x, newY, feet.z);
			player.fallDistance = 0.0F;
			return;
		}
	}

	private static boolean isSolid(Minecraft client, BlockPos pos) {
		return !client.level.getBlockState(pos).getCollisionShape(client.level, pos).isEmpty();
	}

	private static boolean isPassable(Minecraft client, BlockPos pos) {
		return client.level.getBlockState(pos).getCollisionShape(client.level, pos).isEmpty();
	}
}
