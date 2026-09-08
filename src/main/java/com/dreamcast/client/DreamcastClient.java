package com.dreamcast.client;

import com.dreamcast.client.automation.ActionRecorder;
import com.dreamcast.client.automation.AutomationManager;
import com.dreamcast.client.automation.AutomationRunner;
import com.dreamcast.client.camera.FreeCamController;
import com.dreamcast.client.gui.ClickGuiScreen;
import com.dreamcast.client.gui.RecordingHudElement;
import com.dreamcast.client.render.RegionRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/** Minimal entry point for the automation-only Dreamcast client. */
public final class DreamcastClient implements ClientModInitializer {
	public static final String MOD_ID="dreamcast", MOD_NAME="Dreamcast", MOD_VERSION="1.0.0", LOGO_TEXT="DREAMCAST";
	public static final Logger LOGGER= LogUtils.getLogger();
	public static final KeyMapping.Category KEY_CATEGORY=KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID,"automator"));

	@Override public void onInitializeClient(){
		AutomationManager.load();
		KeyMapping open=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.dreamcast.automator",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_SHIFT,KEY_CATEGORY));
		KeyMapping freeCam=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.dreamcast.region_freecam",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F7,KEY_CATEGORY));
		KeyMapping stopRecording=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.dreamcast.stop_recording",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F6,KEY_CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(client->{
			while(open.consumeClick())ClickGuiScreen.open();
			while(freeCam.consumeClick())FreeCamController.getInstance().toggle();
			while(stopRecording.consumeClick())ActionRecorder.stop();
			FreeCamController.getInstance().tick(client);
			ActionRecorder.tick();
			AutomationRunner.tick();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{AutomationRunner.stop("Соединение с миром закрыто");ActionRecorder.stop();});
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID,"recording_bar"),new RecordingHudElement());
		new RegionRenderer().register();
		Runtime.getRuntime().addShutdownHook(new Thread(AutomationManager::save,"dreamcast-automation-save"));
		LOGGER.info("{} Automator {} готов. Меню — правый Shift.",MOD_NAME,MOD_VERSION);
	}
}
