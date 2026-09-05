package com.dreamcast.client.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Разбор командной строки: префикс, имя команды и аргументы.
 *
 * Чистая логика без Minecraft-классов — покрыта ArgsParserTest.
 * Правила:
 * <ul>
 *   <li>командой считается сообщение, начинающееся ровно с одного префикса
 *       («..текст» — это обычный текст с точкой);</li>
 *   <li>аргументы разделяются пробелами, «лишние» пробелы игнорируются;</li>
 *   <li>аргумент в двойных кавычках считается одним аргументом
 *       (нужно для ников с пробелами в .friend и текста в .search).</li>
 * </ul>
 */
public final class ArgsParser {

	/** Разобранная команда: имя в нижнем регистре + аргументы. */
	public record Parsed(String name, List<String> args) {
	}

	private ArgsParser() {
	}

	/**
	 * Разбирает сообщение; вернёт null, если это не команда.
	 *
	 * @param message полный текст сообщения из чата
	 * @param prefix  текущий префикс команд (например «.»)
	 */
	public static Parsed parse(String message, String prefix) {
		if (message == null || prefix == null || prefix.isEmpty()) {
			return null;
		}
		if (!message.startsWith(prefix)) {
			return null;
		}
		String rest = message.substring(prefix.length());
		// «..something» — экранирование префикса, не команда
		if (rest.startsWith(prefix)) {
			return null;
		}
		List<String> tokens = tokenize(rest.trim());
		if (tokens.isEmpty()) {
			return null;
		}
		String name = tokens.get(0).toLowerCase(java.util.Locale.ROOT);
		return new Parsed(name, List.copyOf(tokens.subList(1, tokens.size())));
	}

	/** Режет строку на аргументы по пробелам с поддержкой кавычек. */
	public static List<String> tokenize(String input) {
		List<String> tokens = new ArrayList<>();
		if (input == null) {
			return tokens;
		}
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;
		boolean hasToken = false;
		for (int i = 0; i < input.length(); i++) {
			char character = input.charAt(i);
			if (character == '"') {
				inQuotes = !inQuotes;
				hasToken = true;
				continue;
			}
			if (Character.isWhitespace(character) && !inQuotes) {
				if (hasToken) {
					tokens.add(current.toString());
					current.setLength(0);
					hasToken = false;
				}
				continue;
			}
			current.append(character);
			hasToken = true;
		}
		if (hasToken) {
			tokens.add(current.toString());
		}
		return tokens;
	}
}
