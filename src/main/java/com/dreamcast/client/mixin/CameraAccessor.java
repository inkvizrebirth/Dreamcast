package com.dreamcast.client.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes Camera's protected transform methods to the free-camera mixin. */
@Mixin(Camera.class)
public interface CameraAccessor {
	/**
	 * Sets the camera world position.
	 *
	 * @param x world X
	 * @param y world Y
	 * @param z world Z
	 */
	@Invoker("setPosition") void dreamcast$setPosition(double x, double y, double z);

	/**
	 * Sets the camera pitch and yaw.
	 *
	 * @param pitch vertical angle
	 * @param yaw horizontal angle
	 */
	@Invoker("setRotation") void dreamcast$setRotation(float pitch, float yaw);

	/**
	 * Marks the camera as detached so vanilla renders the player in third person.
	 *
	 * @param detached whether the camera is detached from the entity eye position
	 */
	@Accessor("detached") void dreamcast$setDetached(boolean detached);

	/**
	 * Marks the camera as initialized after a custom transform is installed.
	 *
	 * @param initialized whether the camera has a valid transform
	 */
	@Accessor("initialized") void dreamcast$setInitialized(boolean initialized);
}
