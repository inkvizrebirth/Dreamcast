package com.dreamcast.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Lightweight chat feedback used by non-cheat menu screens. */
public final class Notifications {
	public enum Type { INFO, OK, WARN, ERROR }
	private Notifications() { }
	public static void push(String title,String message,Type type){
		Minecraft c=Minecraft.getInstance();if(c==null||c.gui==null)return;
		String color=switch(type){case OK->"§a";case WARN->"§e";case ERROR->"§c";default->"§b";};
		c.gui.hud.getChat().addClientSystemMessage(Component.literal(color+"["+title+"] §f"+message));
	}
	public static void info(String t,String m){push(t,m,Type.INFO);}public static void ok(String t,String m){push(t,m,Type.OK);}
	public static void warn(String t,String m){push(t,m,Type.WARN);}public static void error(String t,String m){push(t,m,Type.ERROR);}
}
