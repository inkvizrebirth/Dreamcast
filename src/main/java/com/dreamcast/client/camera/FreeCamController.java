package com.dreamcast.client.camera;

import com.dreamcast.client.region.RegionManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Controls the detached, fixed top-down camera used by the region editor. */
@Environment(EnvType.CLIENT)
public final class FreeCamController {
	private static final float NORMAL_SPEED = 0.5F;
	private static final float SPRINT_SPEED = 1.5F;
	private static final float SPAWN_Y_OFFSET = 2.0F;
	private static final FreeCamController INSTANCE = new FreeCamController();

	private volatile double freeCamX;
	private volatile double freeCamZ;

	private FreeCamController() { }

	/**
	 * Returns the client camera controller.
	 *
	 * @return controller instance
	 */
	public static FreeCamController getInstance() {
		return INSTANCE;
	}

	/** Enables free camera at the local player's position. */
	public void activate() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null) return;
		RegionManager manager = RegionManager.getInstance();
		freeCamX = player.getX();
		freeCamZ = player.getZ();
		manager.freeCamHeight = (float) player.getY() + SPAWN_Y_OFFSET;
		manager.freeCamActive = true;
	}

	/** Disables free camera and returns normal camera ownership to Minecraft. */
	public void deactivate() {
		RegionManager.getInstance().freeCamActive = false;
	}

	/** Toggles the free-camera state. */
	public void toggle() {
		if (RegionManager.getInstance().freeCamActive) deactivate(); else activate();
	}

	/**
	 * Moves the detached camera from the current standard movement key states.
	 *
	 * @param client current Minecraft instance
	 */
	public void tick(Minecraft client) {
		RegionManager manager = RegionManager.getInstance();
		if (!manager.freeCamActive || client == null || client.player == null) return;
		float speed = client.options.keyShift.isDown() ? SPRINT_SPEED : NORMAL_SPEED;
		if (client.options.keyUp.isDown()) freeCamZ -= speed;
		if (client.options.keyDown.isDown()) freeCamZ += speed;
		if (client.options.keyLeft.isDown()) freeCamX -= speed;
		if (client.options.keyRight.isDown()) freeCamX += speed;
	}

	/**
	 * Returns the current detached camera location.
	 *
	 * @return immutable camera position
	 */
	public Vec3 position() {
		return new Vec3(freeCamX, RegionManager.getInstance().freeCamHeight, freeCamZ);
	}

	/**
	 * Updates the camera height, normally from the editor slider.
	 *
	 * @param height absolute world Y coordinate
	 */
	public void setHeight(float height) {
		RegionManager.getInstance().freeCamHeight = height;
	}
}
