package com.dreamcast.client.automation;

import java.util.List;

/** Built-in starter profiles. They are kept in memory and are never written to automations.json. */
public final class AutomationDefaults {
	private static final List<AutomationConfig> PROFILES = List.of(autoWalk(), autoMine(), autoFarm());

	private AutomationDefaults() { }

	/**
	 * Returns the three bundled starter profiles.
	 *
	 * @return immutable list of built-in workflows
	 */
	public static List<AutomationConfig> all() {
		return PROFILES;
	}

	private static AutomationConfig autoWalk() {
		AutomationConfig config = linear("AutoWalk");
		AutomationNode move = new AutomationNode(AutomationNodeType.MOVE, 250, 150);
		move.values.put("direction", "forward");
		move.values.put("seconds", "10");
		move.values.put("parkour", "true");
		move.values.put("parkour_profile", "universal");
		link(config, move);
		return config;
	}

	private static AutomationConfig autoMine() {
		AutomationConfig config = linear("AutoMine");
		AutomationNode mine = new AutomationNode(AutomationNodeType.MINE, 250, 150);
		mine.values.put("block", "minecraft:iron_ore");
		mine.values.put("count", "16");
		link(config, mine);
		return config;
	}

	private static AutomationConfig autoFarm() {
		AutomationConfig config = linear("AutoFarm");
		AutomationNode farm = new AutomationNode(AutomationNodeType.FARM, 250, 150);
		farm.values.put("radius", "32");
		link(config, farm);
		return config;
	}

	private static AutomationConfig linear(String name) {
		AutomationConfig config = new AutomationConfig(name);
		config.builtIn = true;
		config.nodes.clear();
		config.links.clear();
		config.nodes.add(new AutomationNode(AutomationNodeType.START, 80, 150));
		config.nodes.add(new AutomationNode(AutomationNodeType.STOP, 500, 150));
		return config;
	}

	private static void link(AutomationConfig config, AutomationNode action) {
		AutomationNode start = config.nodes.get(0);
		AutomationNode stop = config.nodes.get(1);
		config.nodes.add(action);
		config.links.add(new AutomationLink(start.id, "next", action.id));
		config.links.add(new AutomationLink(action.id, "next", stop.id));
	}
}
