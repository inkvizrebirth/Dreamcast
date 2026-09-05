package com.dreamcast.client.command.impl;

import com.dreamcast.client.command.Command;
import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleCategory;
import com.dreamcast.client.module.ModuleManager;

import java.util.List;
import java.util.Locale;

/**
 * «.modules [категория]» — список модулей, включённые подсвечены.
 */
public class ModulesCommand extends Command {

	public ModulesCommand() {
		super("modules", "Список модулей по категориям", ".modules [категория]");
	}

	@Override
	public void execute(List<String> args) {
		if (!args.isEmpty()) {
			String needle = args.get(0).toLowerCase(Locale.ROOT);
			ModuleCategory category = null;
			for (ModuleCategory candidate : ModuleCategory.values()) {
				if (candidate.name().toLowerCase(Locale.ROOT).equals(needle)
						|| candidate.getDisplayName().toLowerCase(Locale.ROOT).equals(needle)) {
					category = candidate;
					break;
				}
			}
			if (category == null) {
				error("Категория «" + args.get(0) + "» не найдена.");
				return;
			}
			printCategory(category);
			return;
		}
		for (ModuleCategory category : ModuleCategory.values()) {
			printCategory(category);
		}
	}

	private void printCategory(ModuleCategory category) {
		List<Module> modules = ModuleManager.getByCategory(category);
		if (modules.isEmpty()) {
			return;
		}
		StringBuilder line = new StringBuilder("§b" + category.getDisplayName() + "§8:§7 ");
		for (int i = 0; i < modules.size(); i++) {
			Module module = modules.get(i);
			line.append(module.isEnabled() ? "§a" : "§7").append(module.getName());
			if (i < modules.size() - 1) {
				line.append("§8, ");
			}
		}
		reply(line.toString());
	}
}
