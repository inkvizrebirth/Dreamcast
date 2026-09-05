package com.dreamcast.client.command.impl;

import com.dreamcast.client.command.Command;
import com.dreamcast.client.module.Module;
import com.dreamcast.client.module.ModuleManager;

import java.util.List;

/**
 * «.panic» — мгновенно выключает все модули, кроме ClickGUI и HUD.
 * Кнопка «выключи всё», когда кто-то идёт к монитору.
 */
public class PanicCommand extends Command {

	public PanicCommand() {
		super("panic", "Выключить все модули разом", ".panic");
	}

	@Override
	public void execute(List<String> args) {
		int disabled = 0;
		for (Module module : ModuleManager.getAll()) {
			if (module.isEnabled() && !module.isAlwaysEnabled()) {
				module.setEnabledSilently(false);
				disabled++;
			}
		}
		com.dreamcast.client.config.ConfigManager.save();
		if (disabled == 0) {
			reply("Всё и так выключено.");
		} else {
			reply("§cПаника! Выключено модулей: §f" + disabled + "§7.");
		}
	}
}
