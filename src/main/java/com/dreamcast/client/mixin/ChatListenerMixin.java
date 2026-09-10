package com.dreamcast.client.mixin;

import com.dreamcast.client.automation.ChatMessageBus;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures displayed system, player and disguised chat for workflow nodes. */
@Environment(EnvType.CLIENT)
@Mixin(ChatListener.class)
public final class ChatListenerMixin {
	@Inject(method = "handleSystemMessage", at = @At("HEAD"))
	private void dreamcast$recordSystem(Component message, boolean overlay, CallbackInfo ci) {
		ChatMessageBus.getInstance().record(message.getString());
	}

	@Inject(method = "handlePlayerChatMessage", at = @At("HEAD"))
	private void dreamcast$recordPlayer(PlayerChatMessage message, GameProfile profile, ChatType.Bound bound, CallbackInfo ci) {
		ChatMessageBus.getInstance().record(bound.decorate(message.decoratedContent()).getString());
	}

	@Inject(method = "handleDisguisedChatMessage", at = @At("HEAD"))
	private void dreamcast$recordDisguised(Component message, ChatType.Bound bound, CallbackInfo ci) {
		ChatMessageBus.getInstance().record(bound.decorate(message).getString());
	}
}
