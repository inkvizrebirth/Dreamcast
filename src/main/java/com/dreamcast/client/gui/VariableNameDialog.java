package com.dreamcast.client.gui;

import com.dreamcast.client.region.RegionManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Small modal screen which names a block position before it becomes a global marker. */
@Environment(EnvType.CLIENT)
public final class VariableNameDialog extends Screen {
	private final Screen parent;
	private final BlockPos position;
	private EditBox input;

	/**
	 * Creates the naming dialog.
	 *
	 * @param parent screen to restore after cancellation
	 * @param position selected marker block
	 */
	public VariableNameDialog(Screen parent, BlockPos position) {
		super(Component.literal("Имя переменной"));
		this.parent = parent;
		this.position = position.immutable();
	}

	@Override protected void init() {
		int left = width / 2 - 110, top = height / 2 - 40;
		input = addRenderableWidget(new EditBox(font, left, top, 220, 20, Component.literal("Имя переменной")));
		input.setMaxLength(64);
		input.setValue(RegionManager.getInstance().pendingVariableName);
		input.setFocused(true);
		addRenderableWidget(Button.builder(Component.literal("Сохранить"), button -> save()).bounds(left, top + 30, 106, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Отмена"), button -> onClose()).bounds(left + 114, top + 30, 106, 20).build());
	}

	@Override public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, 0xB0000000);
		graphics.fill(width / 2 - 124, height / 2 - 62, width / 2 + 124, height / 2 + 70, 0xF0181A22);
		graphics.text(font, "Метка: " + position.getX() + ", " + position.getY() + ", " + position.getZ(), width / 2 - 110, height / 2 - 50, 0xFFFFFFFF, false);
	}

	@Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) { save(); return true; }
		return super.keyPressed(event);
	}

	@Override public void onClose() {
		RegionManager.getInstance().pendingVariableName = input == null ? "" : input.getValue();
		if (minecraft != null) minecraft.gui.setScreen(parent);
	}
	@Override public boolean isPauseScreen() { return false; }

	private void save() {
		String name = input.getValue().trim();
		if (name.isEmpty()) return;
		RegionManager.getInstance().addVariableMarker(name, position);
		RegionManager.getInstance().pendingVariableName = "";
		onClose();
	}
}
