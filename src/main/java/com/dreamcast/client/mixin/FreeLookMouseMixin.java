package com.dreamcast.client.mixin;

import com.dreamcast.client.camera.FreeLookController;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Redirects mouse rotation into the detached FreeLook orbit. */
@Environment(EnvType.CLIENT)
@Mixin(MouseHandler.class)
public final class FreeLookMouseMixin {
	@Shadow private double accumulatedDX;
	@Shadow private double accumulatedDY;

	@Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
	private void dreamcast$orbitInsteadOfPlayer(double delta, CallbackInfo ci) {
		FreeLookController controller = FreeLookController.getInstance();
		if (!controller.isActive()) return;
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.gui == null || client.gui.screen() != null) return;
		double sensitivity = (double) client.options.sensitivity().get() * 0.6D + 0.2D;
		double scale = sensitivity * sensitivity * sensitivity * 8.0D;
		double deltaX = accumulatedDX * scale;
		double deltaY = accumulatedDY * scale;
		if ((Boolean) client.options.invertMouseX().get()) deltaX = -deltaX;
		if ((Boolean) client.options.invertMouseY().get()) deltaY = -deltaY;
		controller.onMouseTurn(deltaX, deltaY);
		ci.cancel();
	}
}
