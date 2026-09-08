package com.dreamcast.client.render;

import com.dreamcast.client.region.RegionManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Emits region editor gizmos during Fabric's post-translucent level pass. */
@Environment(EnvType.CLIENT)
public final class RegionRenderer {
	private static final int CORNER_GREEN = 0xFF00FF00;
	private static final int BOUNDARY_YELLOW = 0xFFFFFF00;
	private static final int MARKER_BLUE = 0xFF00AAFF;
	private static final float CORNER_LINE_WIDTH = 2.5F;
	private static final float BOUNDARY_LINE_WIDTH = 3.0F;
	private static final float MARKER_HEIGHT = 3.0F;
	private static final int MAX_FILL_CELLS = 4096;

	/** Registers the world render callback once during client initialization. */
	public void register() {
		// 26.2 renamed WorldRenderEvents.AFTER_TRANSLUCENT to this level-render equivalent.
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(context -> render());
	}

	/** Renders all configured editor elements through Minecraft 26.2's Gizmo renderer. */
	public void render() {
		RegionManager manager = RegionManager.getInstance();
		if (!manager.freeCamActive && !manager.showRegion) return;
		List<BlockPos> corners = List.copyOf(manager.regionCorners);
		if (corners.isEmpty()) return;
		float pulse = 0.1F + 0.2F * ((System.currentTimeMillis() % 1000L) / 1000.0F);
		GizmoStyle cornerStyle = GizmoStyle.strokeAndFill(CORNER_GREEN, CORNER_LINE_WIDTH, withAlpha(CORNER_GREEN, pulse));
		for (BlockPos corner : corners) Gizmos.cuboid(corner, cornerStyle).setAlwaysOnTop();
		for (int i = 0; i < corners.size(); i++) {
			BlockPos from = corners.get(i);
			BlockPos to = corners.get((i + 1) % corners.size());
			Gizmos.line(center(from), center(to), BOUNDARY_YELLOW, BOUNDARY_LINE_WIDTH).setAlwaysOnTop();
		}
		renderPolygonFill(corners);
		for (RegionManager.VariableMarker marker : manager.getAllMarkers()) {
			Vec3 base = center(marker.position);
			Gizmos.line(base, base.add(0.0D, MARKER_HEIGHT, 0.0D), MARKER_BLUE, CORNER_LINE_WIDTH).setAlwaysOnTop();
			Gizmos.billboardTextOverBlock(marker.name, marker.position, 0, MARKER_BLUE, 0.02F).setAlwaysOnTop();
		}
	}

	/**
	 * Triangulates a simple X/Z polygon using ear clipping.
	 *
	 * @param polygon ordered corner list
	 * @return triangle index triples; empty when the polygon is invalid
	 */
	public List<int[]> triangulate(List<BlockPos> polygon) {
		if (polygon == null || polygon.size() < 3) return List.of();
		List<Integer> remaining = new ArrayList<>();
		for (int i = 0; i < polygon.size(); i++) remaining.add(i);
		boolean clockwise = signedArea(polygon) < 0.0D;
		List<int[]> triangles = new ArrayList<>();
		while (remaining.size() > 3) {
			boolean clipped = false;
			for (int i = 0; i < remaining.size(); i++) {
				int previous = remaining.get((i + remaining.size() - 1) % remaining.size());
				int current = remaining.get(i);
				int next = remaining.get((i + 1) % remaining.size());
				if (!isConvex(polygon.get(previous), polygon.get(current), polygon.get(next), clockwise)) continue;
				boolean containsVertex = false;
				for (int candidate : remaining) if (candidate != previous && candidate != current && candidate != next
						&& pointInTriangle(polygon.get(candidate), polygon.get(previous), polygon.get(current), polygon.get(next))) { containsVertex = true; break; }
				if (containsVertex) continue;
				triangles.add(new int[]{previous, current, next});
				remaining.remove(i);
				clipped = true;
				break;
			}
			if (!clipped) return List.of(); // Self-intersection or collinear-only input.
		}
		triangles.add(new int[]{remaining.get(0), remaining.get(1), remaining.get(2)});
		return List.copyOf(triangles);
	}

	private void renderPolygonFill(List<BlockPos> corners) {
		if (corners.size() < 3 || triangulate(corners).isEmpty()) return;
		int minX = corners.stream().mapToInt(BlockPos::getX).min().orElse(0);
		int maxX = corners.stream().mapToInt(BlockPos::getX).max().orElse(0);
		int minZ = corners.stream().mapToInt(BlockPos::getZ).min().orElse(0);
		int maxZ = corners.stream().mapToInt(BlockPos::getZ).max().orElse(0);
		long area = (long) (maxX - minX + 1) * (maxZ - minZ + 1);
		if (area > MAX_FILL_CELLS) return;
		double y = corners.stream().mapToInt(BlockPos::getY).min().orElse(0) + 0.01D;
		GizmoStyle fill = GizmoStyle.fill(0x1400FF00);
		RegionManager manager = RegionManager.getInstance();
		for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
			if (manager.isPointInPolygon(x + 0.5D, z + 0.5D)) {
				Gizmos.rect(new Vec3(x, y, z), new Vec3(x + 1.0D, y, z + 1.0D), Direction.UP, fill);
			}
		}
	}

	private static Vec3 center(BlockPos pos) { return Vec3.atCenterOf(pos); }
	private static int withAlpha(int color, float alpha) { return ((int) (Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F) << 24) | (color & 0x00FFFFFF); }
	private static double signedArea(List<BlockPos> polygon) { double area = 0.0D; for (int i = 0; i < polygon.size(); i++) { BlockPos a = polygon.get(i), b = polygon.get((i + 1) % polygon.size()); area += (double) a.getX() * b.getZ() - (double) b.getX() * a.getZ(); } return area * 0.5D; }
	private static boolean isConvex(BlockPos a, BlockPos b, BlockPos c, boolean clockwise) { double cross = (double) (b.getX() - a.getX()) * (c.getZ() - b.getZ()) - (double) (b.getZ() - a.getZ()) * (c.getX() - b.getX()); return clockwise ? cross < 0.0D : cross > 0.0D; }
	private static boolean pointInTriangle(BlockPos p, BlockPos a, BlockPos b, BlockPos c) { double ab = cross(p, a, b), bc = cross(p, b, c), ca = cross(p, c, a); return (ab >= 0 && bc >= 0 && ca >= 0) || (ab <= 0 && bc <= 0 && ca <= 0); }
	private static double cross(BlockPos p, BlockPos a, BlockPos b) { return (double) (p.getX() - b.getX()) * (a.getZ() - b.getZ()) - (double) (p.getZ() - b.getZ()) * (a.getX() - b.getX()); }
}
