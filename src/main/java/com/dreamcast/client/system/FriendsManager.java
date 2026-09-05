package com.dreamcast.client.system;

import com.dreamcast.client.DreamcastClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Список друзей клиента (как в Meteor/LiquidBounce).
 *
 * Друзья не бьются KillAura/TriggerBot, подсвечиваются в ESP отдельным
 * цветом и получают метку в Nametags. Хранилище — JSON рядом с конфигом:
 * {@code config/dreamcast/friends.json}. Имя хранится в нижнем регистре —
 * ники в Minecraft регистронезависимы при сравнении.
 */
public final class FriendsManager {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir()
			.resolve(DreamcastClient.MOD_ID).resolve("friends.json");

	private static final Set<String> FRIENDS = new LinkedHashSet<>();

	private FriendsManager() {
	}

	public static synchronized void load() {
		FRIENDS.clear();
		if (!Files.exists(PATH)) {
			return;
		}
		try (BufferedReader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
			JsonObject root = GSON.fromJson(reader, JsonObject.class);
			if (root == null || !root.has("friends") || !root.get("friends").isJsonArray()) {
				return;
			}
			JsonArray array = root.getAsJsonArray("friends");
			for (JsonElement element : array) {
				if (element.isJsonPrimitive()) {
					String name = element.getAsString();
					if (name != null && !name.isBlank()) {
						FRIENDS.add(normalize(name));
					}
				}
			}
		} catch (IOException | RuntimeException exception) {
			DreamcastClient.LOGGER.error("Не удалось прочитать список друзей {}", PATH, exception);
		}
	}

	public static synchronized void save() {
		try {
			Files.createDirectories(PATH.getParent());
			JsonObject root = new JsonObject();
			JsonArray array = new JsonArray();
			for (String friend : FRIENDS) {
				array.add(friend);
			}
			root.add("friends", array);
			Files.writeString(PATH, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			DreamcastClient.LOGGER.error("Не удалось сохранить список друзей {}", PATH, exception);
		}
	}

	private static String normalize(String name) {
		return name.trim().toLowerCase(Locale.ROOT);
	}

	public static boolean isFriend(String name) {
		if (name == null || name.isBlank()) {
			return false;
		}
		synchronized (FriendsManager.class) {
			return FRIENDS.contains(normalize(name));
		}
	}

	/** Добавляет друга; вернёт false, если он уже был в списке. */
	public static boolean add(String name) {
		if (name == null || name.isBlank()) {
			return false;
		}
		boolean added;
		synchronized (FriendsManager.class) {
			added = FRIENDS.add(normalize(name));
		}
		if (added) {
			save();
		}
		return added;
	}

	/** Убирает друга; вернёт false, если его не было. */
	public static boolean remove(String name) {
		if (name == null || name.isBlank()) {
			return false;
		}
		boolean removed;
		synchronized (FriendsManager.class) {
			removed = FRIENDS.remove(normalize(name));
		}
		if (removed) {
			save();
		}
		return removed;
	}

	public static synchronized void clear() {
		if (!FRIENDS.isEmpty()) {
			FRIENDS.clear();
			save();
		}
	}

	public static synchronized List<String> list() {
		return List.copyOf(FRIENDS);
	}

	public static synchronized int count() {
		return FRIENDS.size();
	}

	/** Для тестов: наполнить список без файлового ввода-вывода. */
	static void setForTests(Collection<String> names) {
		synchronized (FriendsManager.class) {
			FRIENDS.clear();
			for (String name : names) {
				FRIENDS.add(normalize(name));
			}
		}
	}
}
