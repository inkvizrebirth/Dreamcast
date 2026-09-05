package com.dreamcast.client.command;

import com.dreamcast.client.DreamcastClient;
import com.dreamcast.client.command.impl.BindCommand;
import com.dreamcast.client.command.impl.ConfigCommand;
import com.dreamcast.client.command.impl.FriendCommand;
import com.dreamcast.client.command.impl.HelpCommand;
import com.dreamcast.client.command.impl.ModulesCommand;
import com.dreamcast.client.command.impl.PanicCommand;
import com.dreamcast.client.command.impl.SearchCommand;
import com.dreamcast.client.command.impl.ToggleCommand;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Команды клиента в чате (как в Meteor: «.toggle sprint», «.friend add X»).
 *
 * Перехват делается через Fabric-событие ALLOW_CHAT: сообщение, начинающееся
 * с префикса, разбирается и не уходит на сервер. Обычный чат не затрагивается.
 */
public final class CommandManager {

	/** Префикс команд. Смена префикса на леты — командой .prefix не предусмотрена. */
	private static String prefix = ".";

	private static final List<Command> COMMANDS = new ArrayList<>();

	private CommandManager() {
	}

	public static void init() {
		if (!COMMANDS.isEmpty()) {
			return;
		}
		register(new HelpCommand());
		register(new ToggleCommand());
		register(new BindCommand());
		register(new FriendCommand());
		register(new ConfigCommand());
		register(new ModulesCommand());
		register(new SearchCommand());
		register(new PanicCommand());

		// Чат-сообщения с префиксом превращаем в команды и не отправляем на сервер
		ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
			ArgsParser.Parsed parsed = ArgsParser.parse(message, prefix);
			if (parsed == null) {
				return true;
			}
			dispatch(parsed);
			return false;
		});

		DreamcastClient.LOGGER.info("Команды клиента готовы, префикс «{}» ({} шт.)", prefix, COMMANDS.size());
	}

	public static void register(Command command) {
		COMMANDS.add(command);
	}

	public static List<Command> getAll() {
		return Collections.unmodifiableList(COMMANDS);
	}

	public static String getPrefix() {
		return prefix;
	}

	private static void dispatch(ArgsParser.Parsed parsed) {
		for (Command command : COMMANDS) {
			if (command.getName().equalsIgnoreCase(parsed.name())) {
				try {
					command.execute(parsed.args());
				} catch (RuntimeException error) {
					DreamcastClient.LOGGER.error("Команда {} упала", parsed.name(), error);
					feedback("§cКоманда завершилась с ошибкой: " + error.getClass().getSimpleName());
				}
				return;
			}
		}
		feedback("§cНеизвестная команда «" + parsed.name() + "». " + prefix + "help — список команд.");
	}

	/**
	 * Единый вывод ответа команды — в чат системным сообщением.
	 *
	 * В 26.2 у Player нет displayClientMessage; вместо него
	 * ChatListener.handleSystemMessage(Component, boolean).
	 */
	public static void feedback(String message) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.gui == null) {
			return;
		}
		String tag = "§8[§b" + DreamcastClient.MOD_NAME + "§8]§r ";
		client.gui.chatListener().handleSystemMessage(Component.literal(tag + message), false);
	}

	/** Поиск команды по имени или префиксу имени — для подсказок. */
	public static Command find(String name) {
		String needle = name == null ? "" : name.toLowerCase(Locale.ROOT);
		for (Command command : COMMANDS) {
			if (command.getName().equals(needle)) {
				return command;
			}
		}
		return null;
	}
}
