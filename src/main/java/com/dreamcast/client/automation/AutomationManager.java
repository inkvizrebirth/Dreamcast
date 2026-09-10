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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
				if (node.type == AutomationNodeType.GOTO) {
					node.values.putIfAbsent("coordinate_mode", "manual");
					node.values.putIfAbsent("marker", "");
					node.values.putIfAbsent("parkour", "false");
					node.values.putIfAbsent("parkour_profile", "universal");
				}
				if (node.type == AutomationNodeType.MOVE) {
					node.values.putIfAbsent("destination_mode", "direction");
					node.values.putIfAbsent("x", "${player.x}"); node.values.putIfAbsent("y", "${player.y}"); node.values.putIfAbsent("z", "${player.z}");
					node.values.putIfAbsent("marker", ""); node.values.putIfAbsent("sprint", "");
					node.values.putIfAbsent("parkour", "false"); node.values.putIfAbsent("parkour_profile", "universal");
				}
				if (node.type == AutomationNodeType.TIMER) node.values.putIfAbsent("seconds", "5");
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

	/**
	 * Creates a persisted editable copy of a built-in or existing workflow.
	 *
	 * @param source workflow to copy
	 * @return the new workflow, or {@code null} when {@code source} is null
	 */
	public static AutomationConfig copyOf(AutomationConfig source) {
		if (source == null) return null;
		AutomationConfig copy = new AutomationConfig();
		copy.id = UUID.randomUUID().toString();
		copy.name = (source.name == null ? "Конфиг" : source.name) + " — копия";
		copy.legit = source.legit;
		copy.builtIn = false;
		copy.nodes = new ArrayList<>();
		copy.links = new ArrayList<>();
		Map<String, String> ids = new HashMap<>();
		if (source.nodes != null) {
			for (AutomationNode original : source.nodes) {
				if (original == null) continue;
				AutomationNode node = new AutomationNode();
				node.id = UUID.randomUUID().toString();
				node.type = original.type;
				node.x = original.x;
				node.y = original.y;
				node.values = original.values == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(original.values);
				copy.nodes.add(node);
				ids.put(original.id, node.id);
			}
		}
		if (source.links != null) {
			for (AutomationLink link : source.links) {
				if (link == null) continue;
				String from = ids.get(link.from), to = ids.get(link.to);
				if (from != null && to != null) copy.links.add(new AutomationLink(from, link.output, to));
			}
		}
		CONFIGS.add(copy);
		save();
		return copy;
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
