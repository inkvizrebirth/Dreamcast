package com.dreamcast.client.mixin;

import com.dreamcast.client.region.RegionManager;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Last-line client protection against stale interaction packets during FreeCam. */
@Mixin(Connection.class)
public final class FreeCamPacketMixin {
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
	private void dreamcast$blockInteractionPackets(Packet<?> packet, CallbackInfo ci) {
		if (!RegionManager.getInstance().freeCamActive) return;
		if (packet instanceof ServerboundUseItemOnPacket
				|| packet instanceof ServerboundUseItemPacket
				|| packet instanceof ServerboundPlayerActionPacket
				|| packet instanceof ServerboundInteractPacket
				|| packet instanceof ServerboundSwingPacket) ci.cancel();
	}
}
