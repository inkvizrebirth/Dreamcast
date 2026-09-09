package com.dreamcast.client.automation;

/** Building blocks available in the visual automation editor. */
public enum AutomationNodeType {
	START("Старт", "Точка входа", 0xFF66D9A3),
	GOTO("Идти", "Baritone: путь к координатам", 0xFF6C8CFF),
	MINE("Добывать", "Baritone: добыча блока", 0xFFFFB45E),
	SEARCH("Искать", "Найти ближайший блок", 0xFF4ED6C8),
	OPEN("Открыть", "Подойти и открыть блок", 0xFFFF9F68),
	USE("Использовать", "Использовать предмет в руке", 0xFF8BD86C),
	FOLLOW("Следовать", "Следовать за игроком или мобом", 0xFF72A7FF),
	EXPLORE("Исследовать", "Исследовать область мира", 0xFF58C4DD),
	FARM("Фармить", "Автоматически собирать урожай", 0xFF7FD36B),
	COORDINATE_CHECK("Проверка координат", "Ветка по положению игрока", 0xFFE7C85B),
	CHAT("Сообщение", "Отправить сообщение в чат", 0xFFAE8CFF),
	CHAT_SEND("Отправить в чат", "Отправить сообщение игрокам", 0xFFB28CFF),
	CHAT_COMMAND("Команда чата", "Отправить slash-команду", 0xFF9F86FF),
	CHAT_WAIT("Ждать чат", "Ждать входящее сообщение", 0xFFBC9CFF),
	CHAT_CHECK("Проверка чата", "Ветка по входящему сообщению", 0xFFD0AEFF),
	SELECT_SLOT("Выбрать слот", "Переключить слот хотбара", 0xFF60C5F1),
	MOVE_ITEM("Переместить предмет", "Перенести между слотами меню", 0xFF4FC7A5),
	QUICK_MOVE("Быстро переместить", "Shift-клик по слоту", 0xFF62D6B0),
	DROP_ITEM("Выбросить предмет", "Выбросить один предмет или стак", 0xFFFF7A7A),
	TAKE_CONTAINER("Забрать из контейнера", "Перенести содержимое в инвентарь", 0xFF4ED7B2),
	EAT("Поесть", "Выбрать и съесть подходящую еду", 0xFFFFB85C),
	FOOD_CHECK("Проверка голода", "Ветка по уровню сытости", 0xFFE4C75A),
	HEALTH_CHECK("Проверка здоровья", "Ветка по здоровью игрока", 0xFFFF6F82),
	ITEM_CHECK("Проверка предмета", "Есть ли предмет в инвентаре", 0xFF65D3C8),
	CONTAINER_CHECK("Проверка контейнера", "Открыт ли контейнер: чужой или свой", 0xFFD98BFF),
	PLAYER_COUNT_CHECK("Проверка игроков", "Ветка по числу игроков рядом", 0xFF66C7FF),
	LOOK("Посмотреть", "Повернуть камеру к углам или точке", 0xFFA98CFF),
	MOVE("Двигаться", "Удерживать направление заданное время", 0xFF6F9DFF),
	JUMP("Прыгнуть", "Обычный прыжок игрока", 0xFF82D6FF),
	SNEAK("Красться", "Удерживать приседание", 0xFF8E9AAF),
	ATTACK("Ударить", "Атаковать цель под прицелом", 0xFFFF6575),
	INTERACT("Взаимодействовать", "Правый клик по цели под прицелом", 0xFF7DDC91),
	WAIT("Ожидание", "Пауза перед следующим действием", 0xFFB38CFF),
	COMMAND("Команда", "Любая команда Baritone", 0xFF55D6E8),
	SET_VARIABLE("Переменная", "Записать значение", 0xFFFF7CAC),
	CONDITION("Условие", "Ветка Да / Нет", 0xFFFFD166),
	PARALLEL("Параллельный поток", "Запустить независимую ветку рядом с текущей", 0xFF9B8CFF),
	SET_FLAG("Установить флаг", "Поднять именованный флаг для других веток", 0xFF5EE0C0),
	WAIT_FLAG("Ожидание флага", "Ждать, пока другая ветка не поднимет флаг", 0xFF5EBFE0),
	PLAYBACK("Воспроизведение записи", "Повторяет ранее записанные движения игрока", 0xFFC7A6FF),
	STOP("Стоп", "Завершить сценарий", 0xFFFF6B78);

	public enum Category {
		NAVIGATION("Движение"), INVENTORY("Инвентарь"), CHECKS("Условия"),
		CONTROL("Управление"), MISC("Прочее");
		private final String label;
		Category(String label) { this.label = label; }
		public String label() { return label; }
	}

	private final String title;
	private final String description;
	private final int color;

	AutomationNodeType(String title, String description, int color) {
		this.title = title;
		this.description = description;
		this.color = color;
	}

	public String title() { return title; }
	public String description() { return description; }
	public int color() { return color; }
	public Category category() {
		return switch (this) {
			case GOTO, MINE, SEARCH, OPEN, FOLLOW, EXPLORE, FARM, COMMAND -> Category.NAVIGATION;
			case SELECT_SLOT, USE, MOVE_ITEM, QUICK_MOVE, DROP_ITEM, TAKE_CONTAINER, EAT -> Category.INVENTORY;
			case CONDITION, COORDINATE_CHECK, FOOD_CHECK, HEALTH_CHECK, ITEM_CHECK, CONTAINER_CHECK, PLAYER_COUNT_CHECK -> Category.CHECKS;
			case LOOK, MOVE, JUMP, SNEAK, ATTACK, INTERACT -> Category.CONTROL;
			case CHAT, CHAT_SEND, CHAT_COMMAND, CHAT_WAIT, CHAT_CHECK, WAIT, SET_VARIABLE, START, STOP, PARALLEL, SET_FLAG, WAIT_FLAG, PLAYBACK -> Category.MISC;
		};
	}
	public boolean branching() {
		return this == CONDITION || this == COORDINATE_CHECK || this == FOOD_CHECK
				|| this == HEALTH_CHECK || this == ITEM_CHECK || this == CONTAINER_CHECK
				|| this == PLAYER_COUNT_CHECK || this == CHAT_WAIT || this == CHAT_CHECK || this == PARALLEL;
	}
}
