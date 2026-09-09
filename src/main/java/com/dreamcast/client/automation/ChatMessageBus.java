package com.dreamcast.client.automation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Thread-safe bridge between Minecraft chat events and chat workflow nodes. */
@Environment(EnvType.CLIENT)
public final class ChatMessageBus {
	private static final ChatMessageBus INSTANCE = new ChatMessageBus();
	private static final int MAX_MESSAGES = 128;
	private final AtomicLong sequence = new AtomicLong();
	private final ConcurrentLinkedDeque<Entry> messages = new ConcurrentLinkedDeque<>();

	private ChatMessageBus() { }

	/**
	 * Returns the global chat event bus.
	 *
	 * @return singleton event bus
	 */
	public static ChatMessageBus getInstance() {
		return INSTANCE;
	}

	/**
	 * Records one displayed chat line. Duplicate delivery from a packet hook is
	 * harmless because matching is sequence-based and bounded.
	 *
	 * @param message displayed line text
	 */
	public void record(String message) {
		if (message == null || message.isBlank()) return;
		messages.addLast(new Entry(sequence.incrementAndGet(), message));
		while (messages.size() > MAX_MESSAGES) messages.pollFirst();
	}

	/**
	 * Returns the most recently assigned event sequence.
	 *
	 * @return latest sequence, or zero when no chat has been received
	 */
	public long latestSequence() {
		return sequence.get();
	}

	/**
	 * Checks messages received after a cursor entered a wait node.
	 *
	 * @param pattern text or regular expression to match
	 * @param afterSequence exclusive lower sequence bound
	 * @param mode {@code regex} for regex matching, otherwise substring matching
	 * @return true when a matching message exists
	 */
	public boolean hasMatchSince(String pattern, long afterSequence, String mode) {
		if (pattern == null || pattern.isBlank()) return false;
		for (Entry entry : messages) {
			if (entry.sequence > afterSequence && matches(entry.text, pattern, mode)) return true;
		}
		return false;
	}

	/**
	 * Checks the bounded recent chat history.
	 *
	 * @param pattern text or regular expression to match
	 * @param mode {@code regex} for regex matching, otherwise substring matching
	 * @return true when a matching recent line exists
	 */
	public boolean hasRecentMatch(String pattern, String mode) {
		if (pattern == null || pattern.isBlank()) return false;
		for (Entry entry : messages) if (matches(entry.text, pattern, mode)) return true;
		return false;
	}

	private static boolean matches(String text, String pattern, String mode) {
		if ("regex".equalsIgnoreCase(mode)) {
			try {
				return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text).find();
			} catch (PatternSyntaxException ignored) {
				// A malformed expression remains useful as a literal chat search.
			}
		}
		return text.toLowerCase(Locale.ROOT).contains(pattern.toLowerCase(Locale.ROOT));
	}

	private record Entry(long sequence, String text) { }
}
