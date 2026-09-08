package com.dreamcast.client.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
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
}
