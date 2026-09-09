package com.dreamcast.client.automation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** A draggable action card on the automation canvas. */
public final class AutomationNode {
	public String id;
	public AutomationNodeType type;
	public float x;
	public float y;
	public Map<String, String> values = new LinkedHashMap<>();

	public AutomationNode() {
		// Gson
	}

	public AutomationNode(AutomationNodeType type, float x, float y) {
		this.id = UUID.randomUUID().toString();
		this.type = type;
		this.x = x;
		this.y = y;
		applyDefaults();
	}

	private void applyDefaults() {
		switch (type) {
			case GOTO -> {
				values.put("x", "${player.x}");
				values.put("y", "${player.y}");
				values.put("z", "${player.z}");
				values.put("sprint", "");
				values.put("path", "straight");
				values.put("coordinate_mode", "manual"); values.put("marker", "");
			}
			case MINE -> {
				values.put("block", "minecraft:diamond_ore");
				values.put("count", "1");
			}
			case SEARCH -> values.put("block", "minecraft:chest");
			case OPEN -> {
				values.put("x", "${player.x}"); values.put("y", "${player.y}"); values.put("z", "${player.z}");
			}
			case USE -> { values.put("slot", "1"); values.put("hand", "main"); }
			case FOLLOW -> { values.put("entity", "player"); values.put("name", ""); }
			case EXPLORE -> values.put("radius", "128");
			case FARM -> values.put("radius", "32");
			case COORDINATE_CHECK -> {
				values.put("axis", "y"); values.put("operator", ">="); values.put("value", "64");
				values.put("marker", "");
			}
			case CHAT -> values.put("message", "Готово!");
			case CHAT_SEND -> values.put("message", "Готово!");
			case CHAT_COMMAND -> values.put("command", "spawnpoint");
			case CHAT_WAIT, CHAT_CHECK -> {
				values.put("pattern", "готово");
				values.put("mode", "contains");
				if (type == AutomationNodeType.CHAT_WAIT) values.put("timeout", "30");
			}
			case SELECT_SLOT -> values.put("slot", "1");
			case MOVE_ITEM -> { values.put("from", "0"); values.put("to", "1"); }
			case QUICK_MOVE -> values.put("slot", "0");
			case DROP_ITEM -> { values.put("slot", "0"); values.put("amount", "stack"); }
			case TAKE_CONTAINER -> values.put("delay_ticks", "2");
			case EAT -> { values.put("food", "any"); values.put("restore_slot", "true"); }
			case FOOD_CHECK -> { values.put("operator", "<="); values.put("value", "14"); }
			case HEALTH_CHECK -> { values.put("operator", "<="); values.put("value", "10"); }
			case ITEM_CHECK -> { values.put("item", "minecraft:bread"); values.put("count", "1"); }
			case CONTAINER_CHECK -> values.put("state", "open");
			case PLAYER_COUNT_CHECK -> { values.put("radius", "16"); values.put("operator", ">="); values.put("value", "1"); }
			case LOOK -> { values.put("mode", "angles"); values.put("yaw", "0"); values.put("pitch", "0"); values.put("x", "0"); values.put("y", "64"); values.put("z", "0"); }
			case MOVE -> { values.put("direction", "forward"); values.put("seconds", "1"); }
			case SNEAK -> values.put("seconds", "1");
			case ATTACK -> values.put("swings", "1");
			case INTERACT -> values.put("hand", "main");
			case WAIT -> values.put("seconds", "1");
			case COMMAND -> values.put("command", "#goto 0 64 0");
			case SET_VARIABLE -> {
				values.put("name", "my_value");
				values.put("value", "0");
			}
			case CONDITION -> {
				values.put("left", "${my_value}");
				values.put("operator", "==");
				values.put("right", "0");
			}
			case SET_FLAG -> values.put("flag", "мой_флаг");
			case WAIT_FLAG -> values.put("flag", "мой_флаг");
			case PLAYBACK -> values.put("frames", "");
			default -> { }
		}
	}

	public String value(String key) {
		return values == null ? "" : values.getOrDefault(key, "");
	}
}
