package com.dreamcast.client.command.impl;

import com.dreamcast.client.command.Command;
import com.dreamcast.client.system.FriendsManager;

import java.util.List;
import java.util.Locale;

/**
 * «.friend add|remove|list|clear [ник]» — список друзей.
 *
 * Друзья: KillAura/TriggerBot их не бьют, ESP красит в зелёный,
 * Nametags вешает метку.
 */
public class FriendCommand extends Command {

	public FriendCommand() {
		super("friend", "Управление списком друзей", ".friend add|remove|list|clear [ник]");
	}

	@Override
	public void execute(List<String> args) {
		String action = args.isEmpty() ? "list" : args.get(0).toLowerCase(Locale.ROOT);
		switch (action) {
			case "add" -> {
				if (args.size() < 2) {
					usage();
					return;
				}
				String name = args.get(1);
				if (FriendsManager.add(name)) {
					reply("§a" + name + "§7 добавлен(а) в друзья.");
				} else {
					error(name + " уже в друзьях.");
				}
			}
			case "remove", "del" -> {
				if (args.size() < 2) {
					usage();
					return;
				}
				String name = args.get(1);
				if (FriendsManager.remove(name)) {
					reply("§c" + name + "§7 убран(а) из друзей.");
				} else {
					error(name + " не было в друзьях.");
				}
			}
			case "clear" -> {
				FriendsManager.clear();
				reply("Список друзей очищен.");
			}
			case "list" -> {
				List<String> friends = FriendsManager.list();
				if (friends.isEmpty()) {
					reply("Список друзей пуст. Добавь: §f.friend add <ник>");
					return;
				}
				reply("§7Друзья (§f" + friends.size() + "§7): §f" + String.join("§7, §f", friends));
			}
			default -> usage();
		}
	}
}
