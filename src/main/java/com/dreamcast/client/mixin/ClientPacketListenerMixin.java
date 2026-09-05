package com.dreamcast.client.mixin;

import com.dreamcast.client.module.impl.BlinkModule;
import com.dreamcast.client.module.impl.NoRotateModule;
import com.dreamcast.client.module.impl.VelocityModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundExplosionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Пакетные хуки сетевого слоя: Blink, Velocity (анти-нокбэк) и NoRotate.
 *
 * Все инджекты — «мягкие» (require = 0): если в следующей версии имя метода
 * поменяется, модуль просто перестанет работать, а игра продолжит запускаться.
 *
 * <ul>
 *   <li>{@code send(Packet)} — исходящие пакеты. Blink ставит их в очередь
 *       и не отправляет, пока включён;</li>
 *   <li>{@code handleSetEntityMotion} — серверный нокбэк (удар/взрыв/удочка).
 *       Velocity гасит или ослабляет его для нашего игрока;</li>
 *   <li>{@code handleExplosion} — knockback взрыва (в 26.2 приходит отдельно
 *       от motion-пакета);</li>
 *   <li>{@code handleMovePlayer} — серверный доворот/телепорт. NoRotate
 *       возвращает игроку его собственные углы после обработки.</li>
 * </ul>
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V",
			at = @At("HEAD"), cancellable = true, require = 0)
	private void dreamcast$blinkSend(Packet<?> packet, CallbackInfo ci) {
		if (BlinkModule.intercept(packet)) {
			ci.cancel();
		}
	}

	/**
	 * Вторая перегрузка send — с листенером подтверждения. Дескриптор указан
	 * строкой: если тип листенера в 26.2 отличается, инджект молча пропустится
	 * (require = 0), а основной send(Packet) продолжит перехватываться.
	 */
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
			at = @At("HEAD"), cancellable = true, require = 0)
	private void dreamcast$blinkSendWithListener(Packet<?> packet, CallbackInfo ci) {
		if (BlinkModule.intercept(packet)) {
			ci.cancel();
		}
	}

	@Inject(method = "handleSetEntityMotion", at = @At("TAIL"), require = 0)
	private void dreamcast$velocityMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
		VelocityModule.onMotionApplied(packet.getEntityId());
	}

	@Inject(method = "handleExplosion", at = @At("TAIL"), require = 0)
	private void dreamcast$velocityExplosion(ClientboundExplosionPacket packet, CallbackInfo ci) {
		VelocityModule.onExplosionApplied(packet.playerKnockback().isPresent());
	}

	@Inject(method = "handleMovePlayer", at = @At("HEAD"), require = 0)
	private void dreamcast$noRotateSave(CallbackInfo ci) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) {
			NoRotateModule.saveRotation(player.getYRot(), player.getXRot());
		}
	}

	@Inject(method = "handleMovePlayer", at = @At("TAIL"), require = 0)
	private void dreamcast$noRotateRestore(CallbackInfo ci) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) {
			NoRotateModule.restoreRotation(player);
		}
	}
}
