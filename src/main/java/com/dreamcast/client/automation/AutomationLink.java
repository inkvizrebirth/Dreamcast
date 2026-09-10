package com.dreamcast.client.automation;

/** Directed connection between two node ports. */
public final class AutomationLink {
	public String from;
	public String output;
	public String to;

	public AutomationLink() {
		// Gson
	}

	public AutomationLink(String from, String output, String to) {
		this.from = from;
		this.output = output;
		this.to = to;
	}
}
