package com.dreamcast.client.command.impl;

import com.dreamcast.client.command.Command;
import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * «.search <текст>» — поиск модулей по названию и описанию.
 */
public class SearchCommand extends Command {

	public SearchCommand() {
		super("search", "Поиск модулей по тексту", ".search <текст>");
	}

	@Override
	public void execute(List<String> args) {
		if (args.isEmpty()) {
			usage();
			return;
		}
		String needle = String.join(" ", args).toLowerCase(Locale.ROOT);
		List<Module> found = new ArrayList<>();
		for (Module module : ModuleManager.getAll()) {
			boolean matches = module.getName().toLowerCase(Locale.ROOT).contains(needle)
					|| module.getId().contains(needle)
					|| module.getDescription().toLowerCase(Locale.ROOT).contains(needle);
			if (matches) {
				found.add(module);
			}
		}
		if (found.isEmpty()) {
			reply("Ничего не найдено по запросу «§f" + needle + "§7».");
			return;
		}
		StringBuilder line = new StringBuilder("§7Найдено (§f" + found.size() + "§7): ");
		for (int i = 0; i < found.size(); i++) {
			Module module = found.get(i);
			line.append(module.isEnabled() ? "§a" : "§f").append(module.getName());
			if (i < found.size() - 1) {
				line.append("§8, ");
			}
		}
		reply(line.toString());
	}
}
