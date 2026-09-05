package com.dreamcast.client.command.impl;

import com.dreamcast.client.command.Command;
import com.dreamcast.client.command.CommandManager;
import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleManager;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;
import java.util.Locale;

/**
 * «.bind <модуль> <клавиша>» — назначить клавишу модулю.
 *
 * Клавиша задаётся коротким именем: «x», «5», «f6», «rshift», «space»,
 * «mouse.middle» и т.п. Специальное значение «none» снимает бинд.
 */
public class BindCommand extends Command {

	public BindCommand() {
		super("bind", "Назначить клавишу модулю", ".bind <модуль> <клавиша|none>");
	}

	@Override
	public void execute(List<String> args) {
		if (args.size() < 2) {
			usage();
			return;
		}
		Module module = ModuleManager.byId(args.get(0));
		if (module == null) {
			error("Модуль «" + args.get(0) + "» не найден.");
			return;
		}
		String keyName = args.get(1).toLowerCase(Locale.ROOT);
		if (keyName.equals("none") || keyName.equals("null")) {
			module.setBindByName("key.keyboard.unknown");
			reply("Бинд модуля §b" + module.getName() + "§7 снят.");
			com.dreamcast.client.config.ConfigManager.save();
			return;
		}
		InputConstants.Key key = resolveKey(keyName);
		if (key == null) {
			error("Не понял клавишу «" + args.get(1) + "». Примеры: x, 5, f6, rshift, space, mouse.middle.");
			return;
		}
		module.setBind(key);
		com.dreamcast.client.config.ConfigManager.save();
		reply("§b" + module.getName() + "§7 теперь на §f" + module.getBindLabel() + "§7.");
	}

	/** Переводит короткое пользовательское имя в клавишу GLFW. */
	static InputConstants.Key resolveKey(String shortName) {
		String name = shortName.toLowerCase(Locale.ROOT).trim();
		if (name.isEmpty()) {
			return null;
		}
		String glfwName;
		if (name.startsWith("mouse.")) {
			glfwName = "key." + name; // key.mouse.left / key.mouse.middle / key.mouse.right
		} else if (name.length() == 1 && (Character.isLetterOrDigit(name.charAt(0)))) {
			glfwName = "key.keyboard." + name;
		} else {
			// Составные имена GLFW: rshift → right.shift, lctrl → left.control и т.д.
			String mapped = switch (name) {
				case "rshift" -> "key.keyboard.right.shift";
				case "lshift" -> "key.keyboard.left.shift";
				case "rctrl" -> "key.keyboard.right.control";
				case "lctrl" -> "key.keyboard.left.control";
				case "ralt" -> "key.keyboard.right.alt";
				case "lalt" -> "key.keyboard.left.alt";
				case "esc", "escape" -> "key.keyboard.escape";
				case "enter", "return" -> "key.keyboard.enter";
				case "space" -> "key.keyboard.space";
				case "tab" -> "key.keyboard.tab";
				case "caps", "capslock" -> "key.keyboard.caps.lock";
				case "backspace" -> "key.keyboard.backspace";
				case "delete", "del" -> "key.keyboard.delete";
				case "insert", "ins" -> "key.keyboard.insert";
				case "home" -> "key.keyboard.home";
				case "end" -> "key.keyboard.end";
				case "pgup", "pageup" -> "key.keyboard.page.up";
				case "pgdn", "pagedown" -> "key.keyboard.page.down";
				case "up" -> "key.keyboard.up";
				case "down" -> "key.keyboard.down";
				case "left" -> "key.keyboard.left";
				case "right" -> "key.keyboard.right";
				case "minus" -> "key.keyboard.minus";
				case "equals", "equal" -> "key.keyboard.equal";
				case "comma" -> "key.keyboard.comma";
				case "period", "dot" -> "key.keyboard.period";
				case "slash" -> "key.keyboard.slash";
				case "semicolon" -> "key.keyboard.semicolon";
				case "apostrophe", "quote" -> "key.keyboard.apostrophe";
				case "lbracket", "[" -> "key.keyboard.left.bracket";
				case "rbracket", "]" -> "key.keyboard.right.bracket";
				case "backslash", "\\" -> "key.keyboard.backslash";
				case "grave", "`" -> "key.keyboard.grave.accent";
				default -> name.startsWith("f") && name.length() > 1 && name.chars().skip(1).allMatch(Character::isDigit)
						? "key.keyboard." + name
						: name.startsWith("key.") ? name : null;
			};
			if (mapped == null) {
				return null;
			}
			glfwName = mapped;
		}
		InputConstants.Key key = InputConstants.getKey(glfwName);
		// getKey возвращает UNKNOWN для несуществующих имён — это не годится
		return key == InputConstants.UNKNOWN ? null : key;
	}
}
