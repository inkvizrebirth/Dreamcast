package com.dreamcast.client.camera;

import com.dreamcast.client.mixin.CameraAccessor;
import com.dreamcast.client.region.RegionManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Owns the optional third-person orbit camera. The controller never changes
 * the player's rotation or movement input, so Baritone continues to operate
 * on the real player state while the view is freely rotated around it.
 */
@Environment(EnvType.CLIENT)
public final class FreeLookController {
	private static final FreeLookController INSTANCE = new FreeLookController();
	private static final double DEFAULT_DISTANCE = 6.0D;
	private static final double MIN_DISTANCE = 1.5D;
	private static final double COLLISION_PADDING = 0.25D;
	private static final float INITIAL_ELEVATION = 24.0F;
	private static final float MOUSE_ROTATION_SCALE = 0.15F;
	private static final float MIN_ELEVATION = -80.0F;
	private static final float MAX_ELEVATION = 80.0F;
	private static final float FULL_TURN = 360.0F;

	private volatile boolean active;
	private float orbitYaw;
	private float orbitElevation = INITIAL_ELEVATION;

	private FreeLookController() { }

	/**
	 * Returns the process-wide client FreeLook controller.
	 *
	 * @return singleton controller
	 */
	public static FreeLookController getInstance() {
		return INSTANCE;
	}

	/**
	 * Enables FreeLook around the local player. Region-editor FreeCam and
	 * FreeLook are mutually exclusive because they use the same camera hook.
	 */
	public void activate() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || RegionManager.getInstance().freeCamActive) return;
		orbitYaw = player.getYRot();
		orbitElevation = INITIAL_ELEVATION;
		active = true;
	}

	/** Disables FreeLook and returns camera ownership to vanilla. */
	public void deactivate() {
		active = false;
	}

	/** Toggles FreeLook if a local player is available. */
	public void toggle() {
		if (active) deactivate(); else activate();
	}

	/**
	 * Returns whether the custom orbit transform is currently active.
	 *
	 * @return true while FreeLook owns the view transform
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Drops the mode when the client leaves the world.
	 *
	 * @param client current Minecraft instance
	 */
	public void tick(Minecraft client) {
		if (active && (client == null || client.player == null || client.level == null)) deactivate();
	}

	/**
	 * Applies mouse movement to the orbit angles instead of the player entity.
	 * The supplied values use the same sensitivity-scaled units as vanilla
	 * {@code Entity#turn}.
	 *
	 * @param deltaX horizontal mouse delta
	 * @param deltaY vertical mouse delta
	 */
	public void onMouseTurn(double deltaX, double deltaY) {
		if (!active) return;
		orbitYaw = wrap(orbitYaw + (float) deltaX * MOUSE_ROTATION_SCALE);
		orbitElevation = clamp(orbitElevation - (float) deltaY * MOUSE_ROTATION_SCALE, MIN_ELEVATION, MAX_ELEVATION);
	}

	/**
	 * Installs the detached camera transform for the current frame.
	 *
	 * @param camera camera being updated by Minecraft
	 */
	public void applyCamera(Camera camera) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (!active || player == null) return;

		Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
		double yawRadians = Math.toRadians(orbitYaw);
		double elevationRadians = Math.toRadians(orbitElevation);
		double horizontal = Math.cos(elevationRadians) * DEFAULT_DISTANCE;
		Vec3 desired = center.add(
				-Math.sin(yawRadians) * horizontal,
				Math.sin(elevationRadians) * DEFAULT_DISTANCE,
				Math.cos(yawRadians) * horizontal);
		Vec3 position = resolveCollision(client, center, desired);

		CameraAccessor accessor = (CameraAccessor) (Object) camera;
		camera.setEntity(player);
		accessor.dreamcast$setDetached(true);
		accessor.dreamcast$setInitialized(true);
		accessor.dreamcast$setPosition(position.x, position.y, position.z);
		double dx = center.x - position.x;
		double dy = center.y - position.y;
		double dz = center.z - position.z;
		float lookYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float lookPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
		accessor.dreamcast$setRotation(lookPitch, lookYaw);
	}

	private static Vec3 resolveCollision(Minecraft client, Vec3 center, Vec3 desired) {
		if (client.level == null) return desired;
		HitResult hit = client.level.clip(new ClipContext(center, desired, ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, client.player));
		if (hit.getType() == HitResult.Type.MISS) return desired;
		Vec3 direction = desired.subtract(center);
		double length = direction.length();
		if (length <= MIN_DISTANCE) return desired;
		return center.add(direction.scale(Math.max(MIN_DISTANCE, Math.min(length, center.distanceTo(hit.getLocation()) - COLLISION_PADDING)) / length));
	}

	private static float wrap(float value) {
		while (value >= 180.0F) value -= FULL_TURN;
		while (value < -180.0F) value += FULL_TURN;
		return value;
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}
