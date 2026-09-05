package com.dreamcast.client.module;

/**
 * Категории модулей (вкладки в левой части ClickGUI).
 *
 * Набор вкладок как в Meteor Client: Бой, Движение, Игрок, Мир, Рендер,
 * Прочее и HUD.
 */
public enum ModuleCategory {

	HUD("HUD", 0xFF45E3FF, "◎"),
	COMBAT("Бой", 0xFFFF5C7A, "✖"),
	MOVEMENT("Движение", 0xFF8DE06C, "»"),
	PLAYER("Игрок", 0xFFE0A55C, "☺"),
	WORLD("Мир", 0xFFC58CFF, "▣"),
	RENDER("Рендер", 0xFF7C6CFF, "◆"),
	MISC("Прочее", 0xFFFFC66C, "≡");

	private final String displayName;
	private final int accent;
	/** Глиф категории для бокового меню ClickGUI. */
	private final String glyph;

	ModuleCategory(String displayName, int accent, String glyph) {
		this.displayName = displayName;
		this.accent = accent;
		this.glyph = glyph;
	}

	public String getDisplayName() {
		return displayName;
	}

	/** Акцентный цвет категории (используется в меню и в HUD). */
	public int getAccent() {
		return accent;
	}

	/** Односимвольная иконка категории. */
	public String getGlyph() {
		return glyph;
	}
}
