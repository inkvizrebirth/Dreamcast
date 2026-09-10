package com.dreamcast.client.mixin;

import com.dreamcast.client.region.RegionManager;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cancels all player world actions while free-camera editing is active. */
@Mixin(MultiPlayerGameMode.class)
public final class FreeCamInteractionMixin {
	@Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
	private void dreamcast$destroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) { if (RegionManager.getInstance().freeCamActive) cir.setReturnValue(false); }
	@Inject(method = {"startDestroyBlock", "continueDestroyBlock"}, at = @At("HEAD"), cancellable = true)
	private void dreamcast$blockBreaking(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) { if (RegionManager.getInstance().freeCamActive) cir.setReturnValue(false); }
	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void dreamcast$blockUse(net.minecraft.client.player.LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) { if (RegionManager.getInstance().freeCamActive) cir.setReturnValue(InteractionResult.PASS); }
	@Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
	private void dreamcast$itemUse(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) { if (RegionManager.getInstance().freeCamActive) cir.setReturnValue(InteractionResult.PASS); }
	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	private void dreamcast$attack(Player player, Entity target, CallbackInfo ci) { if (RegionManager.getInstance().freeCamActive) ci.cancel(); }
	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void dreamcast$interact(Player player, Entity target, EntityHitResult hit, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) { if (RegionManager.getInstance().freeCamActive) cir.setReturnValue(InteractionResult.PASS); }
}
