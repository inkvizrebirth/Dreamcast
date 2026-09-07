package com.dreamcast.client.automation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Serializable visual workflow. */
public final class AutomationConfig {
	public String id = UUID.randomUUID().toString();
	public String name = "Новый конфиг";
	public boolean legit;
	public List<AutomationNode> nodes = new ArrayList<>();
	public List<AutomationLink> links = new ArrayList<>();

	public AutomationConfig() {
	}

	public AutomationConfig(String name) {
		this.name = name;
		nodes.add(new AutomationNode(AutomationNodeType.START, 100, 150));
		nodes.add(new AutomationNode(AutomationNodeType.STOP, 430, 150));
		links.add(new AutomationLink(nodes.get(0).id, "next", nodes.get(1).id));
	}

	public AutomationNode node(String nodeId) {
		if (nodeId == null || nodes == null) return null;
		for (AutomationNode node : nodes) if (nodeId.equals(node.id)) return node;
		return null;
	}
}
