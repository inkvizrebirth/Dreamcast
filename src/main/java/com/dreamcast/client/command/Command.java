package com.dreamcast.client.command;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * База команды чата (префикс «.» — как в Meteor).
 *
 * Команды не знают про парсинг: {@link CommandManager} режет строку на
 * аргументы сам (см. {@link ArgsParser}), а команда получает готовый список.
 * Ответы печатаются в чат фирменным префиксом клиента.
 */
public abstract class Command {

	private final String name;
	private final String description;
	private final String usage;

	protected Command(String name, String description, String usage) {
		this.name = name;
		this.description = description;
		this.usage = usage;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	/** Пример вызова, например «.toggle &lt;модуль&gt; [on|off]». */
	public String getUsage() {
		return usage;
	}

	/** Выполняет команду. args — аргументы БЕЗ имени команды. */
	public abstract void execute(List<String> args);

	// ------------------------------------------------------------------
	// Помощники ответа
	// ------------------------------------------------------------------

	protected static void reply(String message) {
		CommandManager.feedback(message);
	}

	protected static void error(String message) {
		CommandManager.feedback("§c" + message);
	}

	/** Печатает подсказку по использованию команды. */
	protected void usage() {
		error("Использование: " + usage);
	}

	protected static Minecraft client() {
		return Minecraft.getInstance();
	}

	protected static void chat(String text) {
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.player != null) {
			client.player.displayClientMessage(Component.literal(text), false);
		}
	}
}
