package com.dreamcast.client.render;

import com.dreamcast.client.camera.FreeCamController;
import com.dreamcast.client.region.RegionManager;
import com.dreamcast.client.util.RenderUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Draws an adaptive, top-down polygon overview in the lower-right HUD corner. */
@Environment(EnvType.CLIENT)
public final class RegionMinimapRenderer implements HudElement {
	private static final int SIZE = 120;
	private static final int MARGIN = 10;
	private static final int PADDING = 6;
	private static final int GREEN = 0xFF00FF00;
	private static final int YELLOW = 0xFFFFFF00;
	private static final int BLUE = 0xFF00AAFF;

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		RegionManager manager = RegionManager.getInstance();
		if (!manager.freeCamActive && !manager.showRegion) return;
		int x = graphics.guiWidth() - SIZE - MARGIN;
		int y = graphics.guiHeight() - SIZE - MARGIN;
		RenderUtils.fillRounded(graphics, x, y, SIZE, SIZE, 4, 0x99000000);
		List<BlockPos> corners = List.copyOf(manager.regionCorners);
		if (corners.isEmpty()) return;
		Bounds bounds = Bounds.of(corners);
		float scale = Math.min((SIZE - PADDING * 2F) / (bounds.maxX - bounds.minX + 1F), (SIZE - PADDING * 2F) / (bounds.maxZ - bounds.minZ + 1F));
		for (int i = 0; i < corners.size(); i++) {
			Point a = point(corners.get(i), bounds, scale, x, y);
			Point b = point(corners.get((i + 1) % corners.size()), bounds, scale, x, y);
			line(graphics, a.x, a.y, b.x, b.y, YELLOW, 2);
		}
		for (BlockPos corner : corners) {
			Point point = point(corner, bounds, scale, x, y);
			graphics.fill(point.x - 2, point.y - 2, point.x + 2, point.y + 2, GREEN);
		}
		Minecraft client = Minecraft.getInstance();
		for (RegionManager.VariableMarker marker : manager.getAllMarkers()) {
			Point point = point(marker.position, bounds, scale, x, y);
			RenderUtils.fillCircle(graphics, point.x, point.y, 3, BLUE);
			if (client != null) RenderUtils.textFlat(graphics, client.font, marker.name, point.x + 5, point.y - 4, 0xFFFFFFFF);
		}
		if (manager.freeCamActive) {
			Vec3 camera = FreeCamController.getInstance().position();
			int cx = toScreen(camera.x, bounds.minX, scale, x), cy = toScreen(camera.z, bounds.minZ, scale, y);
			line(graphics, cx - 3, cy, cx + 3, cy, 0xFFFFFFFF, 1);
			line(graphics, cx, cy - 3, cx, cy + 3, 0xFFFFFFFF, 1);
		}
	}

	private static Point point(BlockPos pos, Bounds bounds, float scale, int x, int y) { return new Point(toScreen(pos.getX(), bounds.minX, scale, x), toScreen(pos.getZ(), bounds.minZ, scale, y)); }
	private static int toScreen(double value, int minimum, float scale, int origin) { return origin + PADDING + Math.round((float) (value - minimum) * scale); }
	private static void line(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color, int width) { int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)); for (int i = 0; i <= steps; i++) { float progress = steps == 0 ? 0F : i / (float) steps; int x = Math.round(x0 + (x1 - x0) * progress), y = Math.round(y0 + (y1 - y0) * progress); graphics.fill(x, y, x + width, y + width, color); } }
	private record Point(int x, int y) { }
	private record Bounds(int minX, int maxX, int minZ, int maxZ) { private static Bounds of(List<BlockPos> corners) { int minX = corners.stream().mapToInt(BlockPos::getX).min().orElse(0), maxX = corners.stream().mapToInt(BlockPos::getX).max().orElse(0), minZ = corners.stream().mapToInt(BlockPos::getZ).min().orElse(0), maxZ = corners.stream().mapToInt(BlockPos::getZ).max().orElse(0); return new Bounds(minX, maxX, minZ, maxZ); } }
}
