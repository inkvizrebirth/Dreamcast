package com.dreamcast.client.automation;

import com.dreamcast.client.DreamcastClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Loads and saves workflows independently of the legacy module settings. */
public final class AutomationManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type CONFIG_LIST = new TypeToken<List<AutomationConfig>>() { }.getType();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir()
			.resolve("dreamcast").resolve("automations.json");
	private static final List<AutomationConfig> CONFIGS = new ArrayList<>();

	private AutomationManager() { }

	public static void load() {
		CONFIGS.clear();
		if (!Files.isRegularFile(FILE)) return;
		try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
			List<AutomationConfig> loaded = GSON.fromJson(reader, CONFIG_LIST);
			if (loaded != null) CONFIGS.addAll(loaded);
			normalize();
		} catch (Exception error) {
			DreamcastClient.LOGGER.error("Не удалось загрузить сценарии автоматизатора", error);
		}
	}

	private static void normalize() {
		CONFIGS.removeIf(config -> config == null);
		for (AutomationConfig config : CONFIGS) {
			if (config.nodes == null) config.nodes = new ArrayList<>();
			if (config.links == null) config.links = new ArrayList<>();
			for (AutomationNode node : config.nodes) {
				if (node.values == null) node.values = new java.util.LinkedHashMap<>();
			}
		}
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(CONFIGS, CONFIG_LIST, writer);
			}
		} catch (Exception error) {
			DreamcastClient.LOGGER.error("Не удалось сохранить сценарии автоматизатора", error);
		}
	}

	public static List<AutomationConfig> all() {
		return Collections.unmodifiableList(CONFIGS);
	}

	public static AutomationConfig create() {
		AutomationConfig config = new AutomationConfig("Конфиг " + (CONFIGS.size() + 1));
		CONFIGS.add(config);
		save();
		return config;
	}

	/** Adds an already-built config (e.g. one produced by {@link ActionRecorder}) and persists it. */
	public static void add(AutomationConfig config) {
		if (config == null) return;
		CONFIGS.add(config);
		save();
	}

	public static void remove(AutomationConfig config) {
		if (AutomationRunner.isRunning(config)) AutomationRunner.stop("Конфиг удалён");
		CONFIGS.remove(config);
		save();
	}
}
