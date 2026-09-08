package com.dreamcast.client.automation;

import com.dreamcast.client.DreamcastClient;
import com.dreamcast.client.baritone.BaritoneBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Tick-driven workflow interpreter. It never blocks Minecraft's render thread. */
public final class AutomationRunner {
	private static AutomationConfig active;
	private static AutomationNode current;
	private static final Map<String, String> VARIABLES = new HashMap<>();
	private static long enteredAt;
	private static boolean dispatched;
	private static KeyMapping heldKey;
	private static int progress;
	private static int previousSlot = -1;
	private static String status = "Готов";

	private AutomationRunner() { }

	public static void start(AutomationConfig config) {
		if (config == null || config.nodes == null || config.nodes.isEmpty()) return;
		stop(null);
		active = config;
		BaritoneBridge.configureLegit(config.legit);
		VARIABLES.clear();
		current = config.nodes.stream().filter(node -> node.type == AutomationNodeType.START).findFirst()
				.orElse(config.nodes.get(0));
		enteredAt = System.currentTimeMillis();
		dispatched = false;
		status = "Запущен: " + config.name;
		notify(status);
	}

	public static void stop(String reason) {
		if (active != null) BaritoneBridge.stop();
		cleanupAction();
		active = null;
		current = null;
		dispatched = false;
		status = reason == null ? "Готов" : reason;
		if (reason != null) notify(reason);
	}

	public static boolean isRunning() { return active != null; }
	public static boolean isRunning(AutomationConfig config) { return active == config; }
	public static String status() { return status; }
	public static String currentNodeId() { return current == null ? null : current.id; }

	public static void tick() {
		if (active == null || current == null) return;
		try {
			switch (current.type) {
				case START -> next("next");
				case STOP -> finish("Сценарий завершён");
				case SET_VARIABLE -> {
					String name = current.value("name").trim();
					if (!name.isEmpty()) VARIABLES.put(name, resolve(current.value("value")));
					next("next");
				}
				case CONDITION -> next(testCondition() ? "true" : "false");
				case WAIT -> {
					long duration = Math.max(0L, Math.round(number(resolve(current.value("seconds"))) * 1000.0));
					status = "Ожидание " + current.value("seconds") + " сек.";
					if (System.currentTimeMillis() - enteredAt >= duration) next("next");
				}
				case GOTO -> runGoto();
				case MINE -> runMine();
				case SEARCH -> runCommandAndWait("goto " + resolve(current.value("block")), "Ищу " + current.value("block"));
				case FOLLOW -> runCommandAndWait("follow " + resolve(current.value("entity")) + " " + resolve(current.value("name")), "Следую за целью");
				case EXPLORE -> runCommandAndWait("explore " + resolve(current.value("radius")), "Исследую область");
				case FARM -> runCommandAndWait("farm " + resolve(current.value("radius")), "Собираю урожай");
				case OPEN -> runOpen();
				case USE -> runUse();
				case COORDINATE_CHECK -> next(testCoordinate() ? "true" : "false");
				case CONTAINER_CHECK -> next(testContainer() ? "true" : "false");
				case PLAYER_COUNT_CHECK -> next(testPlayerCount() ? "true" : "false");
				case CHAT -> {
					Minecraft client = Minecraft.getInstance();
					if (client.getConnection() == null) fail("Нет подключения к миру");
					else { client.getConnection().sendChat(resolve(current.value("message"))); next("next"); }
				}
				case SELECT_SLOT -> { selectSlot(); next("next"); }
				case MOVE_ITEM -> moveItem();
				case QUICK_MOVE -> { containerClick("quick"); next("next"); }
				case DROP_ITEM -> { containerClick("drop"); next("next"); }
				case TAKE_CONTAINER -> takeContainer();
				case EAT -> eat();
				case FOOD_CHECK -> next(testFood() ? "true" : "false");
				case HEALTH_CHECK -> next(testHealth() ? "true" : "false");
				case ITEM_CHECK -> next(hasItem() ? "true" : "false");
				case LOOK -> look();
				case MOVE -> holdMovement(false);
				case SNEAK -> holdMovement(true);
				case JUMP -> { Minecraft.getInstance().player.jumpFromGround(); next("next"); }
				case ATTACK -> attack();
				case INTERACT -> interact();
				case COMMAND -> {
					if (!dispatched) {
						dispatched = true;
						String command = resolve(current.value("command"));
						if (!BaritoneBridge.command(command)) fail("Команда не отправлена");
						else next("next");
					}
				}
			}
		} catch (RuntimeException error) {
			DreamcastClient.LOGGER.error("Ошибка сценария {}", active.name, error);
			fail("Ошибка в узле «" + current.type.title() + "»");
		}
	}

	private static void selectSlot() {
		Minecraft c=Minecraft.getInstance();requirePlayer(c);
		c.player.getInventory().setSelectedSlot(Math.max(0,Math.min(8,(int)number(resolve(current.value("slot")))-1)));
	}

	private static void moveItem() {
		Minecraft c=Minecraft.getInstance();requirePlayer(c);int from=(int)number(resolve(current.value("from"))),to=(int)number(resolve(current.value("to")));
		if(from<0||to<0||from>=c.player.containerMenu.slots.size()||to>=c.player.containerMenu.slots.size())throw new IllegalArgumentException("Неверный индекс слота");
		long delay=active.legit?110L:50L;if(System.currentTimeMillis()-enteredAt<progress*delay)return;int id=c.player.containerMenu.containerId;
		c.gameMode.handleContainerInput(id,progress==0?from:progress==1?to:from,0,ContainerInput.PICKUP,c.player);
		progress++;status="Перемещаю предмет: "+Math.min(progress,3)+"/3";if(progress>=3)next("next");
	}

	private static void containerClick(String mode) {
		Minecraft c=Minecraft.getInstance();requirePlayer(c);int slot=(int)number(resolve(current.value("slot")));
		if(slot<0||slot>=c.player.containerMenu.slots.size())throw new IllegalArgumentException("Неверный индекс слота");
		ContainerInput input="drop".equals(mode)?ContainerInput.THROW:ContainerInput.QUICK_MOVE;
		int button="stack".equalsIgnoreCase(current.value("amount"))?1:0;
		c.gameMode.handleContainerInput(c.player.containerMenu.containerId,slot,button,input,c.player);
	}

	private static void takeContainer() {
		Minecraft c=Minecraft.getInstance();requirePlayer(c);if(c.player.containerMenu==c.player.inventoryMenu){next("next");return;}
		int delay=Math.max(1,(int)number(resolve(current.value("delay_ticks"))));if((System.currentTimeMillis()-enteredAt)/50<progress*delay)return;
		int count=Math.max(0,c.player.containerMenu.slots.size()-36);for(int i=0;i<count;i++){Slot s=c.player.containerMenu.getSlot(i);if(!s.getItem().isEmpty()){c.gameMode.handleContainerInput(c.player.containerMenu.containerId,i,0,ContainerInput.QUICK_MOVE,c.player);progress++;status="Забираю предметы: "+progress;return;}}next("next");
	}

	private static void eat() {
		Minecraft c=Minecraft.getInstance();requirePlayer(c);
		if(!dispatched){int best=-1,nutrition=-1;String wanted=resolve(current.value("food"));for(int i=0;i<9;i++){ItemStack stack=c.player.getInventory().getItem(i);FoodProperties food=stack.get(DataComponents.FOOD);String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();if(food!=null&&("any".equalsIgnoreCase(wanted)||id.equals(wanted))&&food.nutrition()>nutrition){best=i;nutrition=food.nutrition();}}
			if(best<0){fail("Подходящей еды нет в хотбаре");return;}previousSlot=c.player.getInventory().getSelectedSlot();c.player.getInventory().setSelectedSlot(best);c.gameMode.useItem(c.player,InteractionHand.MAIN_HAND);dispatched=true;status="Ем";return;}
		if(c.player.isUsingItem())return;if(Boolean.parseBoolean(current.value("restore_slot"))&&previousSlot>=0)c.player.getInventory().setSelectedSlot(previousSlot);previousSlot=-1;next("next");
	}

	private static boolean hasItem() {
		Minecraft c=Minecraft.getInstance();requirePlayer(c);String wanted=resolve(current.value("item"));int need=Math.max(1,(int)number(resolve(current.value("count")))),found=0;
		for(ItemStack stack:c.player.getInventory().getNonEquipmentItems()){if(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(wanted))found+=stack.getCount();}return found>=need;
	}

	private static void look() {
		Minecraft c=Minecraft.getInstance();requirePlayer(c);float yaw,pitch;
		if("point".equalsIgnoreCase(current.value("mode"))){Vec3 eye=c.player.getEyePosition();double dx=number(resolve(current.value("x")))-eye.x,dy=number(resolve(current.value("y")))-eye.y,dz=number(resolve(current.value("z")))-eye.z;yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));}
		else{yaw=(float)number(resolve(current.value("yaw")));pitch=(float)number(resolve(current.value("pitch")));}
		float yd=wrap(yaw-c.player.getYRot()),pd=pitch-c.player.getXRot(),speed=active.legit?12:28;c.player.setYRot(c.player.getYRot()+clamp(yd,-speed,speed));c.player.setXRot(c.player.getXRot()+clamp(pd,-speed,speed));
		if(Math.abs(yd)<=1.5&&Math.abs(pd)<=1.5)next("next");else status="Плавно поворачиваюсь";
	}

	private static void holdMovement(boolean sneak) {
		Minecraft c=Minecraft.getInstance();requirePlayer(c);if(!dispatched){heldKey=sneak?c.options.keyShift:switch(current.value("direction").toLowerCase(Locale.ROOT)){case"back","backward"->c.options.keyDown;case"left"->c.options.keyLeft;case"right"->c.options.keyRight;default->c.options.keyUp;};heldKey.setDown(true);dispatched=true;enteredAt=System.currentTimeMillis();}
		long ms=Math.max(0,Math.round(number(resolve(current.value("seconds")))*1000));if(System.currentTimeMillis()-enteredAt>=ms){releaseHeld();next("next");}
	}

	private static void attack() {
		Minecraft c=Minecraft.getInstance();requirePlayer(c);int wanted=Math.max(1,(int)number(resolve(current.value("swings"))));if(System.currentTimeMillis()-enteredAt<progress*250L)return;Entity target=c.crosshairPickEntity;if(target!=null&&target.isAlive())c.gameMode.attack(c.player,target);c.player.swing(InteractionHand.MAIN_HAND);progress++;if(progress>=wanted)next("next");
	}

	private static void interact() {
		Minecraft c=Minecraft.getInstance();requirePlayer(c);InteractionHand hand="off".equalsIgnoreCase(current.value("hand"))?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
		if(c.hitResult instanceof BlockHitResult block)c.gameMode.useItemOn(c.player,hand,block);else if(c.crosshairPickEntity!=null)c.gameMode.interact(c.player,c.crosshairPickEntity,new EntityHitResult(c.crosshairPickEntity),hand);else c.gameMode.useItem(c.player,hand);next("next");
	}

	private static boolean compare(double left,String op,double right){return switch(op){case">"->left>right;case">="->left>=right;case"<"->left<right;case"!="->left!=right;case"=="->left==right;default->left<=right;};}
	private static void requirePlayer(Minecraft c){if(c==null||c.player==null||c.gameMode==null)throw new IllegalStateException("Игрок недоступен");}
	private static void releaseHeld(){if(heldKey!=null){heldKey.setDown(false);heldKey=null;}}
	private static void cleanupAction(){releaseHeld();Minecraft c=Minecraft.getInstance();if(previousSlot>=0&&c!=null&&c.player!=null){c.player.stopUsingItem();c.player.getInventory().setSelectedSlot(previousSlot);}previousSlot=-1;progress=0;}

	private static void runGoto() {
		if (!dispatched) {
			int x = (int) Math.floor(number(resolve(current.value("x"))));
			int y = (int) Math.floor(number(resolve(current.value("y"))));
			int z = (int) Math.floor(number(resolve(current.value("z"))));
			dispatched = true;
			enteredAt = System.currentTimeMillis();
			status = "Иду к " + x + ", " + y + ", " + z;
			if (!BaritoneBridge.goal(x, y, z, false)) fail("Baritone не принял маршрут");
			return;
		}
		if (System.currentTimeMillis() - enteredAt > 600 && !BaritoneBridge.isPathing()) next("next");
	}

	private static void runMine() {
		if (!dispatched) {
			String block = resolve(current.value("block")).trim();
			int count = Math.max(0, (int) number(resolve(current.value("count"))));
			dispatched = true;
			enteredAt = System.currentTimeMillis();
			status = "Добываю " + block + " × " + count;
			if (!BaritoneBridge.mine(block, count, false, active.legit)) fail("Baritone не начал добычу");
			return;
		}
		if (System.currentTimeMillis() - enteredAt > 800 && !BaritoneBridge.isPathing()) next("next");
	}

	private static void runCommandAndWait(String command, String label) {
		if (!dispatched) {
			dispatched = true; enteredAt = System.currentTimeMillis(); status = label;
			if (!BaritoneBridge.command(command)) fail("Baritone не принял команду");
			return;
		}
		if (System.currentTimeMillis() - enteredAt > 800 && !BaritoneBridge.isPathing()) next("next");
	}

	private static void runUse() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.gameMode == null) { fail("Игрок недоступен"); return; }
		int slot = Math.max(1, Math.min(9, (int) number(resolve(current.value("slot"))))) - 1;
		client.player.getInventory().setSelectedSlot(slot);
		InteractionHand hand = "off".equalsIgnoreCase(current.value("hand")) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		client.gameMode.useItem(client.player, hand);
		next("next");
	}

	private static void runOpen() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.gameMode == null) { fail("Игрок недоступен"); return; }
		int x=(int)number(resolve(current.value("x"))), y=(int)number(resolve(current.value("y"))), z=(int)number(resolve(current.value("z")));
		if (!dispatched) {
			dispatched=true; enteredAt=System.currentTimeMillis(); status="Подхожу к блоку";
			if(!BaritoneBridge.goal(x,y,z,false)) fail("Baritone не принял маршрут");
			return;
		}
		if (System.currentTimeMillis()-enteredAt<600 || BaritoneBridge.isPathing()) return;
		Vec3 eye=client.player.getEyePosition(), target=new Vec3(x+.5,y+.5,z+.5);
		double dx=target.x-eye.x, dy=target.y-eye.y, dz=target.z-eye.z, flat=Math.hypot(dx,dz);
		float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz)), pitch=(float)-Math.toDegrees(Math.atan2(dy,flat));
		if(active.legit){
			float yd=wrap(yaw-client.player.getYRot()), pd=pitch-client.player.getXRot();
			client.player.setYRot(client.player.getYRot()+clamp(yd,-16,16));client.player.setXRot(client.player.getXRot()+clamp(pd,-13,13));
			if(Math.abs(yd)>2||Math.abs(pd)>2){status="Плавно навожусь";return;}
		}else{client.player.setYRot(yaw);client.player.setXRot(pitch);}
		BlockPos pos=new BlockPos(x,y,z);BlockHitResult hit=new BlockHitResult(target,Direction.UP,pos,false);
		client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,hit);next("next");
	}

	private static boolean testCoordinate() {
		Minecraft client=Minecraft.getInstance();if(client.player==null)return false;
		double actual=switch(current.value("axis").toLowerCase(Locale.ROOT)){case"x"->client.player.getX();case"z"->client.player.getZ();default->client.player.getY();};
		double expected=number(resolve(current.value("value")));String op=current.value("operator");
		return switch(op){case">"->actual>expected;case">="->actual>=expected;case"<"->actual<expected;case"<="->actual<=expected;case"!="->actual!=expected;default->Math.abs(actual-expected)<.5;};
	}

	private static boolean testFood() {
		Minecraft client = Minecraft.getInstance();
		requirePlayer(client);
		return compare(client.player.getFoodData().getFoodLevel(), current.value("operator"), number(resolve(current.value("value"))));
	}

	private static boolean testHealth() {
		Minecraft client = Minecraft.getInstance();
		requirePlayer(client);
		return compare(client.player.getHealth(), current.value("operator"), number(resolve(current.value("value"))));
	}

	private static boolean testContainer() {
		Minecraft client = Minecraft.getInstance();
		requirePlayer(client);
		boolean open = client.player.containerMenu != client.player.inventoryMenu;
		boolean expected = "open".equalsIgnoreCase(current.value("state"));
		return open == expected;
	}

	private static boolean testPlayerCount() {
		Minecraft client = Minecraft.getInstance();
		requirePlayer(client);
		if (client.level == null) return false;
		double radius = number(resolve(current.value("radius")));
		long count = client.level.players().stream()
				.filter(player -> player != client.player && player.distanceTo(client.player) <= radius)
				.count();
		return compare(count, current.value("operator"), number(resolve(current.value("value"))));
	}

	private static boolean testCondition() {
		String left = resolve(current.value("left"));
		String right = resolve(current.value("right"));
		String operator = current.value("operator").trim().toLowerCase(Locale.ROOT);
		return switch (operator) {
			case "!=", "≠" -> !left.equals(right);
			case ">" -> number(left) > number(right);
			case ">=" -> number(left) >= number(right);
			case "<" -> number(left) < number(right);
			case "<=" -> number(left) <= number(right);
			case "contains", "содержит" -> left.contains(right);
			default -> left.equals(right);
		};
	}

	private static String resolve(String source) {
		String result = source == null ? "" : source;
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.player != null) {
			result = result.replace("${player.x}", format(client.player.getX()))
					.replace("${player.y}", format(client.player.getY()))
					.replace("${player.z}", format(client.player.getZ()))
					.replace("${player.health}", format(client.player.getHealth()));
			result = result.replace("${player.food}", Integer.toString(client.player.getFoodData().getFoodLevel()))
					.replace("${player.yaw}", format(client.player.getYRot()))
					.replace("${player.pitch}", format(client.player.getXRot()))
					.replace("${player.armor}", Integer.toString(client.player.getArmorValue()))
					.replace("${player.air}", Integer.toString(client.player.getAirSupply()))
					.replace("${player.slot}", Integer.toString(client.player.getInventory().getSelectedSlot()+1))
					.replace("${player.on_ground}", Boolean.toString(client.player.onGround()))
					.replace("${player.sneaking}", Boolean.toString(client.player.isShiftKeyDown()))
					.replace("${player.item}", client.player.getMainHandItem().getItem().toString())
					.replace("${world.time}", Long.toString(client.level == null ? 0 : client.level.getGameTime()));
			Map<String,Integer> counts=new HashMap<>();for(ItemStack stack:client.player.getInventory().getNonEquipmentItems()){String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();counts.merge(id,stack.getCount(),Integer::sum);}
			for(Map.Entry<String,Integer> entry:counts.entrySet())result=result.replace("${inventory."+entry.getKey()+"}",Integer.toString(entry.getValue()));
			if (client.level != null && result.contains("${nearest_player.")) {
				var nearest = client.level.players().stream()
						.filter(player -> player != client.player)
						.min(java.util.Comparator.comparingDouble(player -> player.distanceTo(client.player)))
						.orElse(null);
				String nearestName = nearest == null ? "" : nearest.getGameProfile().name();
				String nearestDistance = nearest == null ? "99999" : format(nearest.distanceTo(client.player));
				result = result.replace("${nearest_player.name}", nearestName)
						.replace("${nearest_player.distance}", nearestDistance);
			}
		}
		for (Map.Entry<String, String> entry : VARIABLES.entrySet()) {
			result = result.replace("${" + entry.getKey() + "}", entry.getValue());
		}
		return result;
	}

	private static String format(double value) {
		return Math.rint(value) == value ? Long.toString((long) value) : String.format(Locale.ROOT, "%.2f", value);
	}

	private static double number(String value) {
		try { return Double.parseDouble(value.trim().replace(',', '.')); }
		catch (NumberFormatException ignored) { return 0.0; }
	}
	private static float wrap(float value){while(value>180)value-=360;while(value<-180)value+=360;return value;}
	private static float clamp(float value,float min,float max){return Math.max(min,Math.min(max,value));}

	private static void next(String output) {
		if (active == null || current == null) return;
		AutomationNode target = null;
		for (AutomationLink link : active.links) {
			if (current.id.equals(link.from) && output.equals(link.output)) {
				target = active.node(link.to);
				break;
			}
		}
		if (target == null) {
			finish("Сценарий завершён: выход «" + output + "» не подключён");
			return;
		}
		current = target;
		releaseHeld();
		enteredAt = System.currentTimeMillis();
		dispatched = false;
		progress = 0;
		status = current.type.title();
	}

	private static void finish(String message) {
		cleanupAction();
		active = null;
		current = null;
		dispatched = false;
		status = message;
		notify(message);
	}

	private static void fail(String message) {
		cleanupAction();
		BaritoneBridge.stop();
		active = null;
		current = null;
		dispatched = false;
		status = message;
		notify("Ошибка: "+message);
	}
	private static void notify(String message){Minecraft c=Minecraft.getInstance();if(c!=null&&c.gui!=null)c.gui.hud.getChat().addClientSystemMessage(net.minecraft.network.chat.Component.literal("§b[Dreamcast] §f"+message));}
}
