package com.dreamcast.client.command.impl;

import com.dreamcast.client.command.Command;
import com.dreamcast.client.config.ConfigManager;

import java.util.List;
import java.util.Locale;

/**
 * «.config save|load|list|delete <имя>» — именованные профили настроек.
 *
 * Каждый профиль — отдельный JSON в {@code config/dreamcast/configs/}.
 * Основный файл {@code dreamcast.json} продолжает жить как «default».
 */
public class ConfigCommand extends Command {

	public ConfigCommand() {
		super("config", "Профили настроек клиента", ".config save|load|list|delete <имя>");
	}

	@Override
	public void execute(List<String> args) {
		String action = args.isEmpty() ? "list" : args.get(0).toLowerCase(Locale.ROOT);
		switch (action) {
			case "save" -> {
				if (args.size() < 2) {
					usage();
					return;
				}
				String name = sanitize(args.get(1));
				if (name == null) {
					error("Имя профиля должно быть из букв, цифр, «-» и «_».");
					return;
				}
				if (ConfigManager.saveTo(name)) {
					reply("Конфиг сохранён как §b" + name + "§7.");
				} else {
					error("Не удалось сохранить конфиг «" + name + "». Подробности в логе.");
				}
			}
			case "load" -> {
				if (args.size() < 2) {
					usage();
					return;
				}
				String name = sanitize(args.get(1));
				if (name == null) {
					error("Имя профиля должно быть из букв, цифр, «-» и «_».");
					return;
				}
				if (ConfigManager.loadFrom(name)) {
					reply("Конфиг §b" + name + "§7 загружен.");
				} else {
					error("Конфиг «" + name + "» не найден или повреждён.");
				}
			}
			case "delete", "del" -> {
				if (args.size() < 2) {
					usage();
					return;
				}
				String name = sanitize(args.get(1));
				if (name != null && ConfigManager.delete(name)) {
					reply("Конфиг §c" + name + "§7 удалён.");
				} else {
					error("Конфиг «" + args.get(1) + "» не найден.");
				}
			}
			case "list" -> {
				List<String> configs = ConfigManager.listProfiles();
				if (configs.isEmpty()) {
					reply("Сохранённых профилей нет. Создай: §f.config save <имя>");
					return;
				}
				reply("§7Профили (§f" + configs.size() + "§7): §f" + String.join("§7, §f", configs));
			}
			default -> usage();
		}
	}

	/** Имя профиля: только безопасные символы (чтобы не выйти за папку конфигов). */
	static String sanitize(String name) {
		if (name == null) {
			return null;
		}
		String trimmed = name.trim();
		if (trimmed.isEmpty() || trimmed.length() > 32) {
			return null;
		}
		for (int i = 0; i < trimmed.length(); i++) {
			char character = trimmed.charAt(i);
			boolean ok = Character.isLetterOrDigit(character) || character == '-' || character == '_';
			if (!ok) {
				return null;
			}
		}
		return trimmed;
	}
}
