package com.dreamcast.client.module;

import com.dreamcast.client.DreamcastClient;
import com.dreamcast.client.config.ConfigManager;
import com.dreamcast.client.module.impl.AutoMineModule;
import com.dreamcast.client.module.impl.AntiAFKModule;
import com.dreamcast.client.module.impl.AutoArmorModule;
import com.dreamcast.client.module.impl.AutoClickerModule;
import com.dreamcast.client.module.impl.AutoEatModule;
import com.dreamcast.client.module.impl.AutoFishModule;
import com.dreamcast.client.module.impl.AutoJumpModule;
import com.dreamcast.client.module.impl.AutoRespawnModule;
import com.dreamcast.client.module.impl.AutoToolModule;
import com.dreamcast.client.module.impl.BlinkModule;
import com.dreamcast.client.module.impl.BowAimbotModule;
import com.dreamcast.client.module.impl.BreadcrumbsModule;
import com.dreamcast.client.module.impl.ChestStealerModule;
import com.dreamcast.client.module.impl.CriticalsModule;
import com.dreamcast.client.module.impl.ElytraBoostModule;
import com.dreamcast.client.module.impl.FlightModule;
import com.dreamcast.client.module.impl.FriendsModule;
import com.dreamcast.client.module.impl.FullBrightModule;
import com.dreamcast.client.module.impl.HoleEspModule;
import com.dreamcast.client.module.impl.JesusModule;
import com.dreamcast.client.module.impl.LongJumpModule;
import com.dreamcast.client.module.impl.MiddleClickFriendModule;
import com.dreamcast.client.module.impl.NoHurtCamModule;
import com.dreamcast.client.module.impl.NoRotateModule;
import com.dreamcast.client.module.impl.NukerModule;
import com.dreamcast.client.module.impl.SneakModule;
import com.dreamcast.client.module.impl.SpeedModule;
import com.dreamcast.client.module.impl.StepModule;
import com.dreamcast.client.module.impl.TracersModule;
import com.dreamcast.client.module.impl.TriggerBotModule;
import com.dreamcast.client.module.impl.VelocityModule;
import com.dreamcast.client.module.impl.ZoomModule;
import com.dreamcast.client.module.impl.AutoTotemModule;
import com.dreamcast.client.module.impl.AutoWalkModule;
import com.dreamcast.client.module.impl.BlockEspModule;
import com.dreamcast.client.module.impl.SpiderModule;
import com.dreamcast.client.module.impl.ClickGuiModule;
import com.dreamcast.client.module.impl.EspModule;
import com.dreamcast.client.module.impl.FreeCamModule;
import com.dreamcast.client.module.impl.FreeLookModule;
import com.dreamcast.client.module.impl.HandShaderModule;
import com.dreamcast.client.module.impl.HudInfoModule;
import com.dreamcast.client.module.impl.JumpEffectModule;
import com.dreamcast.client.module.impl.HitParticlesModule;
import com.dreamcast.client.module.impl.HitSoundsModule;
import com.dreamcast.client.module.impl.MacroModule;
import com.dreamcast.client.module.impl.AutoBuffModule;
import com.dreamcast.client.module.impl.NoSlowModule;
import com.dreamcast.client.module.impl.NametagsModule;
import com.dreamcast.client.module.impl.ScaffoldModule;
import com.dreamcast.client.module.impl.KillAuraModule;
import com.dreamcast.client.module.impl.MediaPlayerModule;
import com.dreamcast.client.module.impl.NoBlindModule;
import com.dreamcast.client.module.impl.NoFallDamageModule;
import com.dreamcast.client.module.impl.NoFovModule;
import com.dreamcast.client.module.impl.SprintModule;
import com.dreamcast.client.module.impl.TrailsModule;
import com.dreamcast.client.module.impl.ViewModelModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Реестр всех модулей клиента. */
public final class ModuleManager {

	private static final List<Module> MODULES = new ArrayList<>();

	private ModuleManager() {
	}

	/** Регистрация модулей. Новые модули добавляются здесь одной строкой. */
	public static void init() {
		if (!MODULES.isEmpty()) {
			DreamcastClient.LOGGER.warn("Повторная инициализация модулей проигнорирована");
			return;
		}
		// Порядок регистрации = порядок тика = приоритет действий:
		// тотем важнее воды, вода важнее баффов, баффы важнее строения,
		// строение важнее удара — так модули не перетягивают инвентарь
		register(new AutoTotemModule());
		register(new NoFallDamageModule());
		register(new AutoEatModule());
		register(new AutoBuffModule());
		register(new AutoArmorModule());
		register(new ScaffoldModule());
		register(new NukerModule());
		register(new KillAuraModule());
		register(new TriggerBotModule());
		register(new AutoClickerModule());
		register(new BowAimbotModule());
		register(new CriticalsModule());
		register(new VelocityModule());
		register(new HudInfoModule());
		register(new ClickGuiModule());
		register(new FreeCamModule());
		register(new FreeLookModule());
		register(new BlinkModule());
		register(new AutoMineModule());
		register(new AutoToolModule());
		register(new AutoWalkModule());
		register(new SprintModule());
		register(new SpeedModule());
		register(new FlightModule());
		register(new JesusModule());
		register(new LongJumpModule());
		register(new StepModule());
		register(new SneakModule());
		register(new AutoJumpModule());
		register(new ElytraBoostModule());
		register(new NoFovModule());
		register(new ZoomModule());
		register(new NoBlindModule());
		register(new FullBrightModule());
		register(new NoHurtCamModule());
		register(new HandShaderModule());
		register(new ViewModelModule());
		register(new MediaPlayerModule());
		register(new TrailsModule());
		register(new BreadcrumbsModule());
		register(new EspModule());
		register(new TracersModule());
		register(new BlockEspModule());
		register(new HoleEspModule());
		register(new JumpEffectModule());
		register(new SpiderModule());
		register(new NametagsModule());
		register(new NoSlowModule());
		register(new MacroModule());
		register(new HitSoundsModule());
		register(new HitParticlesModule());
		register(new ChestStealerModule());
		register(new AutoFishModule());
		register(new AutoRespawnModule());
		register(new AntiAFKModule());
		register(new NoRotateModule());
		register(new FriendsModule());
		register(new MiddleClickFriendModule());
	}

	public static void register(Module module) {
		if (MODULES.stream().anyMatch(existing -> existing.getId().equals(module.getId()))) {
			throw new IllegalArgumentException("Модуль уже зарегистрирован: " + module.getId());
		}
		MODULES.add(module);
		// Подтягиваем сохранённые значения из конфига
		ConfigManager.applyTo(module);
		DreamcastClient.LOGGER.info("Модуль зарегистрирован: {} ({})", module.getName(), module.getCategory().getDisplayName());
	}

	public static List<Module> getAll() {
		return Collections.unmodifiableList(MODULES);
	}

	public static List<Module> getByCategory(ModuleCategory category) {
		return MODULES.stream()
				.filter(module -> module.getCategory() == category)
				.toList();
	}

	/**
	 * Поиск модуля по id или имени — для команд чата («.toggle kill_aura»).
	 * Регистр не важен, пробелы в имени игнорируются.
	 */
	public static Module byId(String idOrName) {
		if (idOrName == null || idOrName.isBlank()) {
			return null;
		}
		String needle = idOrName.trim().toLowerCase(java.util.Locale.ROOT);
		String compact = needle.replace(" ", "").replace("_", "");
		for (Module module : MODULES) {
			if (module.getId().equalsIgnoreCase(needle)) {
				return module;
			}
		}
		for (Module module : MODULES) {
			if (module.getName().toLowerCase(java.util.Locale.ROOT).equals(needle)) {
				return module;
			}
		}
		for (Module module : MODULES) {
			String moduleId = module.getId().replace("_", "");
			String moduleName = module.getName().toLowerCase(java.util.Locale.ROOT).replace(" ", "");
			if (moduleId.equalsIgnoreCase(compact) || moduleName.equals(compact)) {
				return module;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Module> T getModule(Class<T> type) {
		for (Module module : MODULES) {
			if (type.isInstance(module)) {
				return (T) module;
			}
		}
		throw new IllegalStateException("Модуль не найден: " + type.getName());
	}

	/**
	 * То же, что {@link #getModule(Class)}, но вместо исключения возвращает null.
	 *
	 * Нужно коду, который вызывается до инициализации модулей или вообще без них —
	 * например, миксинам рендера: падать из-за них игра не должна.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Module> T find(Class<T> type) {
		for (Module module : MODULES) {
			if (type.isInstance(module)) {
				return (T) module;
			}
		}
		return null;
	}

	/** Вызывается каждый тик клиента: обрабатывает клавиши модулей и их логику. */
	public static void tick() {
		for (Module module : MODULES) {
			try {
				while (module.getKeyMapping().consumeClick()) {
					module.onBindPressed();
				}

				if (module.isEnabled()) {
					module.tick();
				}
			} catch (RuntimeException error) {
				DreamcastClient.LOGGER.error("Модуль {} аварийно остановлен", module.getId(), error);
				try {
					module.setEnabledSilently(false);
				} catch (RuntimeException cleanupError) {
					error.addSuppressed(cleanupError);
					DreamcastClient.LOGGER.error("Не удалось безопасно выключить модуль {}", module.getId(), cleanupError);
				}
				com.dreamcast.client.util.Notifications.error(
						module.getName(), "Остановлен из-за внутренней ошибки; подробности в latest.log");
			}
		}
	}
}
