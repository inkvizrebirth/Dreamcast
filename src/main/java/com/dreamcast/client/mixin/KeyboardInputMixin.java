package com.dreamcast.client.mixin;

import com.dreamcast.client.region.RegionManager;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses movement input sent to the player while the detached camera owns WASD. */
@Mixin(KeyboardInput.class)
public final class KeyboardInputMixin {
	@Shadow public Input keyPresses;
	@Shadow protected Vec2 moveVector;

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void dreamcast$redirectMovementToCamera(CallbackInfo ci) {
		if (!RegionManager.getInstance().freeCamActive) return;
		keyPresses = Input.EMPTY;
		moveVector = Vec2.ZERO;
		ci.cancel();
	}
}
