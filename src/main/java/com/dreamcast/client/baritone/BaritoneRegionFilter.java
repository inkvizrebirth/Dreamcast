package com.dreamcast.client.baritone;

import com.dreamcast.client.region.RegionManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;

import java.util.List;

/** Filters Baritone candidate positions through the active X/Z polygon. */
@Environment(EnvType.CLIENT)
public final class BaritoneRegionFilter {
	private BaritoneRegionFilter() { }
	/** @param pos candidate position @return whether it is inside the active region */
	public static boolean allows(BlockPos pos) { RegionManager manager=RegionManager.getInstance(); return manager.regionCorners.isEmpty() || manager.isPointInPolygon(pos.getX()+.5D,pos.getZ()+.5D); }
	/** @param candidates positions supplied by a scanner @return permitted positions */
	public static List<BlockPos> filterBlockPositions(List<BlockPos> candidates) { return candidates==null?List.of():candidates.stream().filter(BaritoneRegionFilter::allows).toList(); }
}
