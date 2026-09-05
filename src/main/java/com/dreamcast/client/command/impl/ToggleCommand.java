package com.dreamcast.client.command.impl;

import com.dreamcast.client.command.Command;
import com.dreamcast.client.command.CommandManager;
import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleManager;

import java.util.List;
import java.util.Locale;

/**
 * «.toggle <модуль> [on|off]» — включить/выключить модуль (как в Meteor).
 * Без второго аргумента — переключение.
 */
public class ToggleCommand extends Command {

	public ToggleCommand() {
		super("toggle", "Включить или выключить модуль", ".toggle <модуль> [on|off]");
	}

	@Override
	public void execute(List<String> args) {
		if (args.isEmpty()) {
			usage();
			return;
		}
		Module module = ModuleManager.byId(args.get(0));
		if (module == null) {
			error("Модуль «" + args.get(0) + "» не найден. "
					+ CommandManager.getPrefix() + "modules — список модулей.");
			return;
		}
		if (module.isAlwaysEnabled()) {
			error("Модуль «" + module.getName() + "» нельзя выключить.");
			return;
		}
		if (args.size() >= 2) {
			String wanted = args.get(1).toLowerCase(Locale.ROOT);
			switch (wanted) {
				case "on", "вкл", "true", "1" -> module.setEnabled(true);
				case "off", "выкл", "false", "0" -> module.setEnabled(false);
				default -> error("Ожидалось on или off, получено «" + args.get(1) + "».");
			}
			return;
		}
		module.toggle();
	}
}
