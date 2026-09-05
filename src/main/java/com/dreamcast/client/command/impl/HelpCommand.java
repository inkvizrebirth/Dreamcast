package com.dreamcast.client.command.impl;

import com.dreamcast.client.command.Command;
import com.dreamcast.client.command.CommandManager;

import java.util.List;

/**
 * «.help [команда]» — список команд или подсказка по конкретной.
 */
public class HelpCommand extends Command {

	public HelpCommand() {
		super("help", "Список команд клиента", ".help [команда]");
	}

	@Override
	public void execute(List<String> args) {
		if (!args.isEmpty()) {
			Command command = CommandManager.find(args.get(0));
			if (command == null) {
				error("Команда «" + args.get(0) + "» не найдена.");
				return;
			}
			reply("§b" + CommandManager.getPrefix() + command.getName() + "§7 — " + command.getDescription());
			reply("§7Использование: §f" + command.getUsage());
			return;
		}

		reply("§bКоманды " + com.dreamcast.client.DreamcastClient.MOD_NAME + "§7 (" + CommandManager.getAll().size() + "):");
		for (Command command : CommandManager.getAll()) {
			reply("§8" + CommandManager.getPrefix() + "§f" + command.getName() + " §7— " + command.getDescription());
		}
		reply("§7Подробнее: §f" + CommandManager.getPrefix() + "help <команда>");
	}
}
