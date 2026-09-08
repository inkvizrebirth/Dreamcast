package com.dreamcast.client.region;

import com.dreamcast.client.DreamcastClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/** Owns the client-side polygon region and named world positions. */
@Environment(EnvType.CLIENT)
public final class RegionManager {
	private static volatile RegionManager instance;
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("dreamcast").resolve("regions.json");
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(BlockPos.class, new BlockPosAdapter()).setPrettyPrinting().create();

	/** Ordered polygon corners. A copy-on-write list makes renderer snapshots safe. */
	public final List<BlockPos> regionCorners = new CopyOnWriteArrayList<>();
	/** Global named positions. A copy-on-write list makes renderer snapshots safe. */
	public final List<VariableMarker> variableMarkers = new CopyOnWriteArrayList<>();
	/** Whether the detached top-down camera is active. */
	public volatile boolean freeCamActive;
	/** Current free-camera Y coordinate. */
	public volatile float freeCamHeight;
	/** Whether the next primary click adds a polygon corner. */
	public volatile boolean addCornerMode;
	/** Whether the next primary click creates a marker. */
	public volatile boolean addMarkerMode;
	/** Name currently being entered by the marker dialog. */
	public volatile String pendingVariableName = "";

	private final AtomicLong revision = new AtomicLong();
	private final Object fileLock = new Object();

	private RegionManager() { }

	/**
	 * Returns the global region state holder.
	 *
	 * @return the thread-safe singleton
	 */
	public static RegionManager getInstance() {
		RegionManager result = instance;
		if (result == null) {
			synchronized (RegionManager.class) {
				result = instance;
				if (result == null) instance = result = new RegionManager();
			}
		}
		return result;
	}

	/**
	 * Adds a corner when it is not already present.
	 *
	 * @param pos immutable block position to add
	 */
	public void addCorner(BlockPos pos) {
		if (pos == null || regionCorners.contains(pos)) return;
		regionCorners.add(pos.immutable());
		requestSave();
	}

	/** Removes the most recently added corner. */
	public void removeLastCorner() {
		if (regionCorners.isEmpty()) return;
		regionCorners.removeLast();
		requestSave();
	}

	/** Removes every polygon corner. */
	public void clearCorners() {
		if (regionCorners.isEmpty()) return;
		regionCorners.clear();
		requestSave();
	}

	/**
	 * Creates or replaces a named world marker.
	 *
	 * @param name non-blank marker name
	 * @param pos immutable marker position
	 */
	public void addVariableMarker(String name, BlockPos pos) {
		String normalized = name == null ? "" : name.trim();
		if (normalized.isEmpty() || pos == null) return;
		removeMarkerInternal(normalized);
		variableMarkers.add(new VariableMarker(normalized, pos.immutable(), System.currentTimeMillis()));
		requestSave();
	}

	/**
	 * Removes a marker by its name.
	 *
	 * @param name marker name
	 */
	public void removeVariableMarker(String name) {
		if (name == null || !removeMarkerInternal(name.trim())) return;
		requestSave();
	}

	/**
	 * Returns a stable, immutable marker snapshot.
	 *
	 * @return all known markers
	 */
	public List<VariableMarker> getAllMarkers() {
		return List.copyOf(variableMarkers);
	}

	/**
	 * Finds a marker by its exact name.
	 *
	 * @param name marker name
	 * @return matching marker when present
	 */
	public Optional<VariableMarker> getMarkerByName(String name) {
		if (name == null) return Optional.empty();
		return variableMarkers.stream().filter(marker -> marker.name.equals(name)).findFirst();
	}

	/**
	 * Tests a point against the X/Z polygon using the ray-crossing algorithm.
	 * Boundary points are considered inside.
	 *
	 * @param x world X coordinate
	 * @param z world Z coordinate
	 * @return true if the point is inside the configured region
	 */
	public boolean isPointInPolygon(double x, double z) {
		List<BlockPos> corners = List.copyOf(regionCorners);
		if (corners.size() < 3) return false;
		boolean inside = false;
		for (int i = 0, j = corners.size() - 1; i < corners.size(); j = i++) {
			BlockPos a = corners.get(i);
			BlockPos b = corners.get(j);
			if (onSegment(x, z, a, b)) return true;
			boolean crosses = (a.getZ() > z) != (b.getZ() > z)
					&& x < (double) (b.getX() - a.getX()) * (z - a.getZ()) / (b.getZ() - a.getZ()) + a.getX();
			if (crosses) inside = !inside;
		}
		return inside;
	}

	/**
	 * Enumerates positions in a spherical radius whose X/Z coordinates pass the region filter.
	 *
	 * @param center center of the search
	 * @param radius non-negative block radius
	 * @return immutable matching position list
	 */
	public List<BlockPos> getBlocksWithinRegion(BlockPos center, int radius) {
		if (center == null || radius < 0 || regionCorners.size() < 3) return List.of();
		long radiusSquared = (long) radius * radius;
		List<BlockPos> result = new ArrayList<>();
		for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
			for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
				if (!isPointInPolygon(x + 0.5D, z + 0.5D)) continue;
				for (int y = center.getY() - radius; y <= center.getY() + radius; y++) {
					long dx = x - center.getX(), dy = y - center.getY(), dz = z - center.getZ();
					if (dx * dx + dy * dy + dz * dz <= radiusSquared) result.add(new BlockPos(x, y, z));
				}
			}
		}
		return List.copyOf(result);
	}

	/** Schedules a non-blocking save of a consistent state snapshot. */
	public void saveToFile() {
		requestSave();
	}

	/** Loads saved polygon state. Invalid files leave the current state unchanged. */
	public void loadFromFile() {
		if (!Files.isRegularFile(FILE)) return;
		try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
			RegionFile data = GSON.fromJson(reader, RegionFile.class);
			if (data == null) return;
			regionCorners.clear();
			if (data.corners != null) data.corners.stream().filter(pos -> pos != null).map(BlockPos::immutable).forEach(regionCorners::add);
			variableMarkers.clear();
			if (data.markers != null) for (MarkerFile marker : data.markers) {
				if (marker != null && marker.name != null && !marker.name.isBlank()) {
					variableMarkers.add(new VariableMarker(marker.name, new BlockPos(marker.x, marker.y, marker.z), marker.createdAt));
				}
			}
			revision.incrementAndGet();
		} catch (Exception exception) {
			DreamcastClient.LOGGER.error("Unable to load Dreamcast regions", exception);
		}
	}

	private boolean removeMarkerInternal(String name) {
		return variableMarkers.removeIf(marker -> marker.name.equals(name));
	}

	private void requestSave() {
		long snapshotRevision = revision.incrementAndGet();
		RegionFile snapshot = new RegionFile(List.copyOf(regionCorners), variableMarkers.stream()
				.map(MarkerFile::new).sorted(Comparator.comparing(marker -> marker.name)).toList());
		CompletableFuture.runAsync(() -> writeSnapshot(snapshotRevision, snapshot));
	}

	private void writeSnapshot(long snapshotRevision, RegionFile snapshot) {
		synchronized (fileLock) {
			if (snapshotRevision != revision.get()) return;
			try {
				Files.createDirectories(FILE.getParent());
				try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
					GSON.toJson(snapshot, writer);
				}
			} catch (IOException exception) {
				DreamcastClient.LOGGER.error("Unable to save Dreamcast regions", exception);
			}
		}
	}

	private static boolean onSegment(double x, double z, BlockPos a, BlockPos b) {
		double cross = (x - a.getX()) * (b.getZ() - a.getZ()) - (z - a.getZ()) * (b.getX() - a.getX());
		if (Math.abs(cross) > 1.0E-7D) return false;
		return x >= Math.min(a.getX(), b.getX()) && x <= Math.max(a.getX(), b.getX())
				&& z >= Math.min(a.getZ(), b.getZ()) && z <= Math.max(a.getZ(), b.getZ());
	}

	/** Immutable persisted named block position. */
	public static final class VariableMarker {
		public final String name;
		public final BlockPos position;
		public final long createdAt;

		/**
		 * Creates a marker.
		 *
		 * @param name marker identifier
		 * @param position block position
		 * @param createdAt creation timestamp in milliseconds
		 */
		public VariableMarker(String name, BlockPos position, long createdAt) {
			this.name = name;
			this.position = position.immutable();
			this.createdAt = createdAt;
		}
	}

	private static final class RegionFile {
		private List<BlockPos> corners = List.of();
		private List<MarkerFile> markers = List.of();
		private RegionFile() { }
		private RegionFile(List<BlockPos> corners, List<MarkerFile> markers) { this.corners = corners; this.markers = markers; }
	}

	private static final class MarkerFile {
		private String name;
		private int x, y, z;
		private long createdAt;
		private MarkerFile() { }
		private MarkerFile(VariableMarker marker) { name = marker.name; x = marker.position.getX(); y = marker.position.getY(); z = marker.position.getZ(); createdAt = marker.createdAt; }
	}

	private static final class BlockPosAdapter extends TypeAdapter<BlockPos> {
		@Override public void write(JsonWriter out, BlockPos pos) throws IOException {
			if (pos == null) { out.nullValue(); return; }
			out.beginObject().name("x").value(pos.getX()).name("y").value(pos.getY()).name("z").value(pos.getZ()).endObject();
		}
		@Override public BlockPos read(JsonReader in) throws IOException {
			if (in.peek() == com.google.gson.stream.JsonToken.NULL) { in.nextNull(); return null; }
			int x = 0, y = 0, z = 0;
			in.beginObject(); while (in.hasNext()) switch (in.nextName()) { case "x" -> x = in.nextInt(); case "y" -> y = in.nextInt(); case "z" -> z = in.nextInt(); default -> in.skipValue(); } in.endObject();
			return new BlockPos(x, y, z);
		}
	}
}
