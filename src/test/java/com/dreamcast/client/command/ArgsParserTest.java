package com.dreamcast.client.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Парсер команд чата: префикс, экранирование, кавычки, лишние пробелы.
 */
class ArgsParserTest {

	@Test
	void parsesCommandAndArgs() {
		ArgsParser.Parsed parsed = ArgsParser.parse(".toggle sprint on", ".");
		assertEquals("toggle", parsed.name());
		assertEquals(List.of("sprint", "on"), parsed.args());
	}

	@Test
	void commandNameIsLowercased() {
		ArgsParser.Parsed parsed = ArgsParser.parse(".TOGGLE Sprint", ".");
		assertEquals("toggle", parsed.name());
		assertEquals(List.of("Sprint"), parsed.args());
	}

	@Test
	void plainMessageIsNotACommand() {
		assertNull(ArgsParser.parse("привет всем", "."));
	}

	@Test
	void doubledPrefixEscapesCommand() {
		// «..toggle» — текст с точкой, не команда
		assertNull(ArgsParser.parse("..toggle sprint", "."));
	}

	@Test
	void barePrefixIsNotACommand() {
		assertNull(ArgsParser.parse(".", "."));
		assertNull(ArgsParser.parse(".   ", "."));
	}

	@Test
	void extraWhitespaceIsIgnored() {
		ArgsParser.Parsed parsed = ArgsParser.parse(".friend   add    Notch", ".");
		assertEquals("friend", parsed.name());
		assertEquals(List.of("add", "Notch"), parsed.args());
	}

	@Test
	void quotedArgumentKeepsSpaces() {
		ArgsParser.Parsed parsed = ArgsParser.parse(".search \"kill aura\"", ".");
		assertEquals(List.of("kill aura"), parsed.args());
	}

	@Test
	void customPrefixWorks() {
		ArgsParser.Parsed parsed = ArgsParser.parse("!panic", "!");
		assertEquals("panic", parsed.name());
		assertTrue(parsed.args().isEmpty());
		assertNull(ArgsParser.parse(".panic", "!"));
	}

	@Test
	void tokenizeHandlesEmptyQuotes() {
		assertEquals(List.of("a", ""), ArgsParser.tokenize("a \"\""));
	}
}
