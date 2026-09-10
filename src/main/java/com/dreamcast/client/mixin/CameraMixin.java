package com.dreamcast.client.mixin;

import com.dreamcast.client.camera.FreeCamController;
import com.dreamcast.client.camera.FreeLookController;
import com.dreamcast.client.region.RegionManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces Camera#update with the fixed top-down region-editor transform. */
@Mixin(Camera.class)
public final class CameraMixin {
	private static final float TOP_DOWN_PITCH = 90.0F;
	private static final float FIXED_YAW = 0.0F;

	@Inject(method = "update", at = @At("HEAD"), cancellable = true)
	private void dreamcast$updateFreeCamera(DeltaTracker deltaTracker, CallbackInfo ci) {
		if (!RegionManager.getInstance().freeCamActive) return;
		Vec3 pos = FreeCamController.getInstance().position();
		CameraAccessor accessor = (CameraAccessor) (Object) this;
		accessor.dreamcast$setPosition(pos.x, pos.y, pos.z);
		accessor.dreamcast$setRotation(TOP_DOWN_PITCH, FIXED_YAW);
		ci.cancel();
		return;
	}

	@Inject(method = "update", at = @At("TAIL"))
	private void dreamcast$updateFreeLook(DeltaTracker deltaTracker, CallbackInfo ci) {
		if (!FreeLookController.getInstance().isActive()) return;
		FreeLookController.getInstance().applyCamera((Camera) (Object) this);
	}
}
