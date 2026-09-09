package com.dreamcast.client.interaction;

import com.dreamcast.client.camera.FreeCamController;
import com.dreamcast.client.gui.VariableNameDialog;
import com.dreamcast.client.region.RegionManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Routes free-camera clicks to region editing instead of world interaction. */
@Environment(EnvType.CLIENT)
public final class RegionInteractionHandler implements HudElement {
	private static final double RAYCAST_DISTANCE = 64.0D;
	private volatile Optional<BlockPos> cachedRaycast = Optional.empty();

	/** Registers the client-tick click handler. */
	public void register() {
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private void tick(Minecraft client) {
		RegionManager manager = RegionManager.getInstance();
		if (!manager.freeCamActive || client.level == null || client.gui.screen() != null) {
			cachedRaycast = Optional.empty();
			return;
		}
		// Camera movement is tick-based, so one raycast per client tick is enough.
		// The HUD renderer reads this immutable result instead of touching the world.
		cachedRaycast = raycastNow(client);
		if (manager.addCornerMode) {
			if (client.options.keyAttack.consumeClick()) raycast().ifPresent(manager::addCorner);
			if (client.options.keyUse.consumeClick()) manager.removeLastCorner();
		} else if (manager.addMarkerMode && client.options.keyAttack.consumeClick()) {
			raycast().ifPresent(pos -> client.gui.setScreen(new VariableNameDialog(null, pos)));
		}
	}

	/**
	 * Casts a vertical ray from the detached camera.
	 *
	 * @return hit block position when a block is targeted
	 */
	public Optional<BlockPos> raycast() {
		return cachedRaycast;
	}

	private Optional<BlockPos> raycastNow(Minecraft client) {
		Vec3 from = FreeCamController.getInstance().position();
		HitResult result = client.level.clip(new ClipContext(from, from.add(0.0D, -RAYCAST_DISTANCE, 0.0D), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, (net.minecraft.world.entity.Entity) null));
		return result instanceof BlockHitResult block && result.getType() == HitResult.Type.BLOCK ? Optional.of(block.getBlockPos().immutable()) : Optional.empty();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		RegionManager manager = RegionManager.getInstance();
		if (!manager.freeCamActive || !manager.addCornerMode) return;
		int x = graphics.guiWidth() / 2, y = graphics.guiHeight() / 2;
		int color = raycast().isPresent() ? 0xFF00FF00 : 0xFFFF0000;
		graphics.fill(x - 6, y, x + 7, y + 1, color);
		graphics.fill(x, y - 6, x + 1, y + 7, color);
	}
}
