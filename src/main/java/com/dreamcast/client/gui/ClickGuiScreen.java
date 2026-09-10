package com.dreamcast.client.gui;

import com.dreamcast.client.automation.*;
import com.dreamcast.client.camera.FreeCamController;
import com.dreamcast.client.camera.FreeLookController;
import com.dreamcast.client.gui.theme.DreamcastUi;
import com.dreamcast.client.region.RegionManager;
import com.dreamcast.client.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Full-screen, smooth node editor and launcher for Baritone workflows. */
public final class ClickGuiScreen extends Screen {
	private static final int TOP=48, LEFT=184, RIGHT=224, NW=154, NH=70;
	private static final int TEXT=0xFFF4F5FA, MUTED=0xFF9297A8, PANEL=0xF20D0F15;
	private static final int SURFACE=0xF0171A23, BORDER=0x26FFFFFF, ACCENT=0xFF7C6CFF;
	private final Screen parent;
	private AutomationConfig editing;
	private AutomationNode selected, dragging, linkFrom;
	private AutomationConfig contextMenuFor;
	private Box contextMenuBox;
	private String linkOutput, focusedField, message="";
	private String paletteQuery="";
	private String addMenuQuery="";
	private boolean addMenuOpen, addConfigChoiceOpen, draggingFreeCamHeight;
	private String openOptionList;
	private float dragDx, dragDy;
	private float paletteScroll,paletteTarget,addMenuScroll,addMenuTarget;
	private float inspectorScroll,inspectorTarget;
	private long messageAt;
	private final Map<String,Float> animations=new HashMap<>();
	private long openedAt;
	private record Box(int x,int y,int w,int h){boolean has(double px,double py){return px>=x&&px<x+w&&py>=y&&py<y+h;}}

	public ClickGuiScreen(){this(null);}
	public ClickGuiScreen(Screen parent){super(Component.literal("Dreamcast Automator"));this.parent=parent;}
	@Override public void added(){super.added();openedAt=Util.getMillis();animations.clear();}
	public static void open(){Minecraft c=Minecraft.getInstance();if(c!=null&&(c.gui.screen()==null||c.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen))c.gui.setScreen(new ClickGuiScreen());}
	@Override public boolean isPauseScreen(){return false;}

	@Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
		if(minecraft!=null&&minecraft.level==null)DreamcastUi.drawBackdrop(g,width,height,mx,my,0.64F);
		else{g.fill(0,0,width,height,0xD906070B);try{g.blurBeforeThisStratum();}catch(IllegalStateException ignored){}}
	}
	@Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
		super.extractRenderState(g,mx,my,delta);paletteScroll+=(paletteTarget-paletteScroll)*.22F;addMenuScroll+=(addMenuTarget-addMenuScroll)*.22F;inspectorScroll+=(inspectorTarget-inspectorScroll)*.22F;float appear=Math.min(1F,(Util.getMillis()-openedAt)/260F);g.pose().pushMatrix();g.pose().translate(0,(1-smooth(appear))*7);
		if(editing==null)drawLibrary(g,mx,my);else drawEditor(g,mx,my);
		if(!message.isEmpty()&&Util.getMillis()-messageAt<2600)drawToast(g);
		g.pose().popMatrix();
	}

	private void top(GuiGraphicsExtractor g,String subtitle){
		RenderUtils.fillGlassPanel(g,0,0,width,TOP,0,BORDER,PANEL,PANEL,Util.getMillis());
		String brand="DREAMCAST";RenderUtils.textBold(g,font,brand,width-20-RenderUtils.widthBold(font,brand),10,TEXT);
		RenderUtils.textFlat(g,font,subtitle,width-20-RenderUtils.width(font,subtitle),27,MUTED);
	}
	private void drawLibrary(GuiGraphicsExtractor g,int mx,int my){
		top(g,"Автоматизатор Baritone");Box add=new Box(20,12,142,25);button(g,add,"+  Новый конфиг",add.has(mx,my),ACCENT,true);
		int cw=Math.max(260,Math.min(370,(width-60)/2));
		int y=64;
		RenderUtils.textBold(g,font,"ПРОФИЛИ BARITONE",20,y,TEXT);
		RenderUtils.textFlat(g,font,"Готовые AutoWalk, AutoMine и AutoFarm — можно запускать или скопировать",20,y+18,MUTED);
		y+=34;
		y=drawLibraryCards(g,mx,my,AutomationDefaults.all(),true,y,cw);
		y+=18;
		RenderUtils.textBold(g,font,"МОИ КОНФИГИ",20,y,TEXT);
		RenderUtils.textFlat(g,font,"Сценарии из JSON и собственные нодовые цепочки",20,y+18,MUTED);
		y+=34;
		if(AutomationManager.all().isEmpty()){
			RenderUtils.textFlat(g,font,"Пока нет пользовательских конфигов — нажмите «Новый конфиг»",20,y+10,MUTED);
		} else drawLibraryCards(g,mx,my,AutomationManager.all(),false,y,cw);
		if(contextMenuFor!=null&&contextMenuBox!=null){Box menu=contextMenuBox;RenderUtils.fillGlassPanel(g,menu.x,menu.y,menu.w,menu.h,8,BORDER,PANEL,PANEL,Util.getMillis());RenderUtils.textFlat(g,font,"Легитная ротация",menu.x+10,menu.y+12,TEXT);RenderUtils.drawToggle(g,menu.x+164,menu.y+10,26,16,contextMenuFor.legit?1F:0F,0xFF66D9A3);}
		RenderUtils.textFlat(g,font,AutomationRunner.status(),20,height-18,AutomationRunner.isRunning()?0xFF66D9A3:MUTED);
		if(addConfigChoiceOpen)drawConfigChoice(g,mx,my);
	}

	private int drawLibraryCards(GuiGraphicsExtractor g,int mx,int my,List<AutomationConfig> configs,boolean builtIn,int startY,int cw){
		int x=20,y=startY;
		for(AutomationConfig c:configs){
			if(x+cw>width-20){x=20;y+=118;}
			Box card=new Box(x,y,cw,104);float ch=anim("card:"+c.id,card.has(mx,my));
			RenderUtils.drawSoftShadow(g,x,y,cw,104,12,4);
			RenderUtils.fillGlassPanel(g,x,y,cw,104,12,RenderUtils.mix(BORDER,builtIn?0xFF55D6E8:ACCENT,ch*.45F),RenderUtils.mix(PANEL,0xFF1B1E28,ch*.45F),RenderUtils.mix(PANEL,0xFF1B1E28,ch*.45F),Util.getMillis());
			RenderUtils.fillCircle(g,x+22,y+24,6+ch*1.5F,builtIn?0xFF55D6E8:ACCENT);
			RenderUtils.textBold(g,font,trim(c.name,cw-70),x+38,y+18,TEXT);
			RenderUtils.textFlat(g,font,builtIn?"Встроенный профиль • Baritone":c.nodes.size()+" действий  •  "+c.links.size()+" связей",x+18,y+43,MUTED);
			Box run=new Box(x+18,y+70,72,22);
			boolean running=AutomationRunner.isRunning(c);button(g,run,running?"Стоп":"Запуск",run.has(mx,my),running?0xFFFF6B78:0xFF66D9A3,true);drawRunIcon(g,run,running);
			if(builtIn){
				Box copy=new Box(x+98,y+70,92,22);button(g,copy,"Скопировать",copy.has(mx,my),ACCENT,false);
			} else {
				Box edit=new Box(x+98,y+70,92,22),del=new Box(x+cw-72,y+70,54,22);
				button(g,edit,"Редактор",edit.has(mx,my),ACCENT,false);button(g,del,"Удалить",del.has(mx,my),0xFFFF6B78,false);
				if(c==contextMenuFor){int menuY=card.y+108+36>height?card.y-40:card.y+108;contextMenuBox=new Box(Math.min(card.x,Math.max(0,width-200)),menuY,200,36);}
			}
			x+=cw+14;
		}
		return y+104;
	}

	private void drawEditor(GuiGraphicsExtractor g,int mx,int my){
		top(g,"Конструктор автоматизации");Box back=new Box(14,12,58,24),freeLook=new Box(width-658,12,82,24),corner=new Box(width-568,12,82,24),marker=new Box(width-478,12,82,24),region=new Box(width-388,12,80,24),add=new Box(width-300,12,78,24),save=new Box(width-214,12,76,24),run=new Box(width-130,12,72,24),stop=new Box(width-52,12,38,24);
		button(g,back,"Назад",back.has(mx,my),MUTED,false);button(g,add,"Добавить",add.has(mx,my),ACCENT,true);button(g,save,"Сохранить",save.has(mx,my),ACCENT,false);
		boolean freeLookActive=FreeLookController.getInstance().isActive();button(g,freeLook,freeLookActive?"Look ✓":"FreeLook",freeLook.has(mx,my),0xFFC18CFF,freeLookActive);
		boolean freeCam=RegionManager.getInstance().freeCamActive;button(g,region,freeCam?"FreeCam ✓":"FreeCam",region.has(mx,my),0xFF4ED6C8,freeCam);
		RegionManager regionState=RegionManager.getInstance();button(g,corner,"+ блок",corner.has(mx,my),0xFF66D9A3,regionState.addCornerMode);button(g,marker,"+ метка",marker.has(mx,my),0xFF00AAFF,regionState.addMarkerMode);
		button(g,run,"Запуск",run.has(mx,my),0xFF66D9A3,true);button(g,stop,"■",stop.has(mx,my),0xFFFF6B78,false);
		Box name=nameBox();field(g,name,editing.name,"name", "name".equals(focusedField),mx,my);
		Box legit=legitBox();button(g,legit,editing.legit?"✓  Легит":"Легит",legit.has(mx,my),0xFF66D9A3,editing.legit);
		drawCanvas(g,mx,my);drawPalette(g,mx,my);drawInspector(g,mx,my);drawFreeCamSlider(g,mx,my);if(addMenuOpen)drawAddMenu(g,mx,my);
	}
	private void drawFreeCamSlider(GuiGraphicsExtractor g,int mx,int my){
		if(minecraft==null||minecraft.player==null)return;
		int x=10,y=30,h=120;float min=(float)minecraft.player.getY()+2F,max=(float)minecraft.player.getY()+64F;
		float value=clamp(RegionManager.getInstance().freeCamHeight,min,max),ratio=(value-min)/(max-min);
		RenderUtils.fillRounded(g,x,y,12,h,5,0xE02A2D35);int knobY=y+h-Math.round(ratio*h);
		boolean hover=mx>=x-5&&mx<=x+17&&my>=y&&my<=y+h;RenderUtils.fillRounded(g,x-3,knobY-4,18,8,3,hover?0xFF5BC8FF:0xFFFFFFFF);
		RenderUtils.textFlat(g,font,Integer.toString(Math.round(value)),x+20,knobY-4,TEXT);
	}
	private void updateFreeCamSlider(double mouseY){
		if(minecraft==null||minecraft.player==null)return;float min=(float)minecraft.player.getY()+2F,max=(float)minecraft.player.getY()+64F;
		float ratio=clamp((150F-(float)mouseY)/120F,0F,1F);FreeCamController.getInstance().setHeight(min+(max-min)*ratio);
	}
	private void drawConfigChoice(GuiGraphicsExtractor g,int mx,int my){
		Box menu=configChoiceBox();RenderUtils.fillGlassPanel(g,menu.x,menu.y,menu.w,menu.h,8,BORDER,PANEL,PANEL,Util.getMillis());
		Box record=new Box(menu.x+8,menu.y+8,menu.w-16,22),builder=new Box(menu.x+8,menu.y+34,menu.w-16,22);
		button(g,record,"Записать действия",record.has(mx,my),MUTED,false);button(g,builder,"Конструктор",builder.has(mx,my),ACCENT,true);
	}
	private void drawAddMenu(GuiGraphicsExtractor g,int mx,int my){
		Box menu=addMenuBox();RenderUtils.fillGlassPanel(g,menu.x,menu.y,menu.w,menu.h,8,BORDER,PANEL,PANEL,Util.getMillis());
		field(g,new Box(menu.x+10,menu.y+10,menu.w-20,24),addMenuQuery,"add-menu-search","add-menu-search".equals(focusedField),mx,my);
		int listTop=menu.y+44;g.enableScissor(menu.x+1,listTop,menu.x+menu.w-1,menu.y+menu.h-8);int y=listTop+4-Math.round(addMenuScroll);
		for(AutomationNodeType.Category category:AutomationNodeType.Category.values()){
			List<AutomationNodeType> types=addMenuTypes(category);if(types.isEmpty())continue;
			RenderUtils.textFlat(g,font,category.label(),menu.x+12,y,MUTED);y+=20;
			for(AutomationNodeType t:types){Box row=new Box(menu.x+8,y,menu.w-16,38);float h=anim("add-menu:"+t.name(),row.has(mx,my)&&my>=listTop);RenderUtils.fillRounded(g,row.x,row.y,row.w,row.h,8,RenderUtils.mix(0x00000000,0x22FFFFFF,h));RenderUtils.fillCircle(g,menu.x+21,y+19,5+h,t.color());RenderUtils.textFlat(g,font,t.title(),menu.x+34,y+8,TEXT);RenderUtils.textFlat(g,font,trim(t.description(),menu.w-52),menu.x+34,y+22,MUTED);y+=43;}
		}
		g.disableScissor();
	}
	private void drawCanvas(GuiGraphicsExtractor g,int mx,int my){
		int right=width-RIGHT;g.enableScissor(LEFT,TOP,right,height);g.fill(LEFT,TOP,right,height,0xD9090B10);
		for(int x=LEFT+14;x<right;x+=24)for(int y=TOP+14;y<height;y+=24)RenderUtils.fillCircle(g,x,y,1,0x18FFFFFF);
		for(AutomationLink l:editing.links){AutomationNode a=editing.node(l.from),b=editing.node(l.to);if(a!=null&&b!=null){Box ab=nodeBox(a),bb=nodeBox(b);curve(g,ab.x+ab.w,outputY(a,l.output),bb.x,bb.y+NH/2,"false".equals(l.output)?0xFFFF6B78:a.type.color());}}
		if(linkFrom!=null){Box b=nodeBox(linkFrom);curve(g,b.x+b.w,outputY(linkFrom,linkOutput),mx,my,linkFrom.type.color());}
		for(AutomationNode n:editing.nodes)node(g,n,mx,my);g.disableScissor();
		Box plus=new Box(LEFT+18,height-54,36,36);float ph=anim("plus",plus.has(mx,my));RenderUtils.drawSoftShadow(g,plus.x-Math.round(ph),plus.y-Math.round(ph),plus.w+Math.round(ph*2),plus.h+Math.round(ph*2),12,4);
		RenderUtils.fillRounded(g,plus.x-Math.round(ph),plus.y-Math.round(ph),plus.w+Math.round(ph*2),plus.h+Math.round(ph*2),12,RenderUtils.mix(ACCENT,0xFFA59CFF,ph));RenderUtils.textCentered(g,font,"+",plus.x+18,plus.y+12,0xFFFFFFFF,false);
	}
	private void node(GuiGraphicsExtractor g,AutomationNode n,int mx,int my){
		Box b=nodeBox(n);boolean hover=b.has(mx,my),active=n==selected||AutomationRunner.currentNodeIds().contains(n.id);float h=anim("node:hover:"+n.id,hover),a=anim("node:active:"+n.id,active);
		RenderUtils.drawSoftShadow(g,b.x,b.y,b.w,b.h,10,3+Math.round(h*2));RenderUtils.fillGlassPanel(g,b.x,b.y,b.w,b.h,10,RenderUtils.mix(BORDER,n.type.color(),a),RenderUtils.mix(SURFACE,0xFF242834,h*.62F),RenderUtils.mix(SURFACE,0xFF242834,h*.62F),Util.getMillis());
		RenderUtils.fillRoundedTop(g,b.x+1,b.y+1,b.w-2,25,9,RenderUtils.mix(0xFF20232E,n.type.color(),.14F+a*.16F));
		RenderUtils.fillCircle(g,b.x+13,b.y+13,4,n.type.color());RenderUtils.textBold(g,font,n.type.title(),b.x+24,b.y+8,TEXT);
		RenderUtils.textFlat(g,font,summary(n),b.x+12,b.y+36,MUTED);RenderUtils.fillCircle(g,b.x,b.y+NH/2,5,0xFF646979);
		// Порт "true" (y+39) — новая параллельная ветка, "false" (y+57) — продолжение
		// текущей: см. AutomationRunner.runParallel(), где эти строки завязаны на порядок вызовов.
		if(n.type!=AutomationNodeType.STOP){if(n.type==AutomationNodeType.PARALLEL){port(g,b.x+b.w,b.y+39,n.type.color(),"Ветка");port(g,b.x+b.w,b.y+57,0xFFFF6B78,"Далее");}else if(branch(n)){port(g,b.x+b.w,b.y+39,0xFF66D9A3,"Да");port(g,b.x+b.w,b.y+57,0xFFFF6B78,"Нет");}else port(g,b.x+b.w,b.y+48,n.type.color(),"");}
	}
	private void drawPalette(GuiGraphicsExtractor g,int mx,int my){
		RenderUtils.fillGlassPanel(g,0,TOP,LEFT,height-TOP,0,BORDER,PANEL,PANEL,Util.getMillis());RenderUtils.textBold(g,font,"ДЕЙСТВИЯ",16,TOP+17,TEXT);RenderUtils.textFlat(g,font,"Добавьте узел на поле",16,TOP+34,MUTED);
		Box search=new Box(10,TOP+42,LEFT-20,24);field(g,search,paletteQuery,"palette-search","palette-search".equals(focusedField),mx,my);
		List<AutomationNodeType> types=paletteTypes();g.enableScissor(0,TOP+72,LEFT-1,height);int y=TOP+78-Math.round(paletteScroll);for(AutomationNodeType t:types){Box row=new Box(10,y,LEFT-20,38);float h=anim("palette:"+t.name(),row.has(mx,my)&&my>=TOP+72);
			RenderUtils.fillRounded(g,row.x,row.y,row.w,row.h,8,RenderUtils.mix(0x00000000,0x22FFFFFF,h));RenderUtils.fillCircle(g,23,y+19,5+h,t.color());
			RenderUtils.textFlat(g,font,t.title(),36,y+8,TEXT);RenderUtils.textFlat(g,font,trim(t.description(),132),36,y+22,MUTED);y+=43;}g.disableScissor();
	}
	private void drawInspector(GuiGraphicsExtractor g,int mx,int my){
		int x=width-RIGHT;RenderUtils.fillGlassPanel(g,x,TOP,width-x,height-TOP,0,BORDER,PANEL,PANEL,Util.getMillis());RenderUtils.textBold(g,font,"ПАРАМЕТРЫ",x+16,TOP+17,TEXT);
		if(selected==null){RenderUtils.textFlat(g,font,"Выберите действие на поле",x+16,TOP+43,MUTED);RenderUtils.textFlat(g,font,"Связь: выход → вход",x+16,TOP+59,MUTED);return;}
		RenderUtils.fillCircle(g,x+20,TOP+48,5,selected.type.color());RenderUtils.textBold(g,font,selected.type.title(),x+34,TOP+42,TEXT);
		g.enableScissor(x+1,TOP+68,width,height-50);int y=TOP+76-Math.round(inspectorScroll);
		if(selected.type==AutomationNodeType.PLAYBACK){String raw=selected.value("frames");int frameCount=raw.isEmpty()?0:raw.split(";").length;
			RenderUtils.textFlat(g,font,"Записано кадров: "+frameCount,x+16,y,MUTED);y+=16;
			RenderUtils.textFlat(g,font,"Создаётся записью, вручную не редактируется",x+16,y,MUTED);y+=33;}
		for(String key:inspectorKeys()){
			if(selected.type==AutomationNodeType.PLAYBACK&&"frames".equals(key))continue;
			String id="value:"+key;String value=selected.value(key);RenderUtils.textFlat(g,font,label(key),x+16,y,MUTED);
			Box f=new Box(x+16,y+13,RIGHT-32,24);
			if(isOptionField(key))button(g,f,optionListLabel(key,value),f.has(mx,my)||id.equals(openOptionList),ACCENT,false);
			else field(g,f,value,id,id.equals(focusedField),mx,my);
			y+=49;}
		g.disableScissor();
		if(selected.type!=AutomationNodeType.START){Box duplicate=new Box(x+16,height-42,92,25),del=new Box(x+116,height-42,92,25);button(g,duplicate,"Дублировать",duplicate.has(mx,my),ACCENT,false);button(g,del,"Удалить действие",del.has(mx,my),0xFFFF6B78,false);}
		if(openOptionList!=null&&selected!=null)drawOptionList(g,mx,my);
	}

	private List<String> inspectorKeys(){
		List<String> keys=new ArrayList<>();
		if(selected==null||selected.values==null)return keys;
		if(selected.type==AutomationNodeType.GOTO){
			addKey(keys,"coordinate_mode");
			if("variable".equalsIgnoreCase(selected.value("coordinate_mode")))addKey(keys,"marker");
			else {addKey(keys,"x");addKey(keys,"y");addKey(keys,"z");}
			addKey(keys,"sprint");addKey(keys,"path");
		} else if(selected.type==AutomationNodeType.MOVE){
			addKey(keys,"destination_mode");
			if("variable".equalsIgnoreCase(selected.value("destination_mode")))addKey(keys,"marker");
			else if("manual".equalsIgnoreCase(selected.value("destination_mode"))){addKey(keys,"x");addKey(keys,"y");addKey(keys,"z");}
			else {addKey(keys,"direction");addKey(keys,"seconds");}
			addKey(keys,"sprint");addKey(keys,"parkour");addKey(keys,"parkour_profile");
		}
		for(String key:selected.values.keySet()){
			if(selected.type==AutomationNodeType.GOTO&&(("variable".equalsIgnoreCase(selected.value("coordinate_mode"))&&List.of("x","y","z").contains(key))||("manual".equalsIgnoreCase(selected.value("coordinate_mode"))&&"marker".equals(key))))continue;
			if(selected.type==AutomationNodeType.MOVE){
				String mode=selected.value("destination_mode");
				if("direction".equalsIgnoreCase(mode)&&List.of("x","y","z","marker").contains(key))continue;
				if("manual".equalsIgnoreCase(mode)&&"marker".equals(key))continue;
				if("variable".equalsIgnoreCase(mode)&&List.of("direction","seconds","x","y","z").contains(key))continue;
			}
			addKey(keys,key);
		}
		return keys;
	}

	private void addKey(List<String> keys,String key){if(selected.values.containsKey(key)&&!keys.contains(key))keys.add(key);}

	private boolean isOptionField(String key){
		return (selected.type==AutomationNodeType.GOTO&&List.of("sprint","path","coordinate_mode","marker","parkour","parkour_profile").contains(key))
				||(selected.type==AutomationNodeType.COORDINATE_CHECK&&"marker".equals(key))
				||((selected.type==AutomationNodeType.CHAT_WAIT||selected.type==AutomationNodeType.CHAT_CHECK)&&"mode".equals(key))
				||(selected.type==AutomationNodeType.MOVE&&List.of("destination_mode","marker","sprint","parkour","parkour_profile","direction").contains(key))
				||(selected.type==AutomationNodeType.OPEN&&"require_sign".equals(key));
	}

	private String optionListLabel(String key,String value){
		if("marker".equals(key))return value.isBlank()?"Выберите переменную":value;
		if("mode".equals(key))return "regex".equalsIgnoreCase(value)?"Регулярное выражение":"Содержит текст";
		if("parkour".equals(key))return Boolean.parseBoolean(value)?"Baritone-паркур":"Обычное движение";
		if("require_sign".equals(key))return Boolean.parseBoolean(value)?"Требовать табличку":"Без фильтра таблички";
		if("coordinate_mode".equals(key))return "variable".equalsIgnoreCase(value)?"Переменная":"Ручной ввод";
		if("destination_mode".equals(key))return switch(value.toLowerCase(Locale.ROOT)){case "variable"->"Метка";case "manual"->"Координаты";default->"Направление";};
		if("path".equals(key))return"curved".equalsIgnoreCase(value)?"Кривой":"Прямой";
		if("parkour_profile".equals(key))return switch(value.toLowerCase(Locale.ROOT)){case "h2h","neo","head_to_head","head-to-head"->"H2H Neo / 45°";case "universal","adaptive","aggressive"->"Универсальный adaptive";default->"Сбалансированный";};
		if("direction".equals(key))return switch(value.toLowerCase(Locale.ROOT)){case "back","backward"->"Назад";case "left"->"Влево";case "right"->"Вправо";case "forward_left","left_45","45_left","strafe_left"->"45° влево";case "forward_right","right_45","45_right","strafe_right"->"45° вправо";default->"Вперёд";};
		boolean sprint=value.contains("sprint"),jump=value.contains("jump");
		if(!sprint&&!jump)return"—";
		return(sprint?"Спринт":"")+(sprint&&jump?", ":"")+(jump?"Прыгать":"");
	}

	private Box optionListBox(){
		int rows=2;
		if("value:marker".equals(openOptionList))rows=Math.min(7,RegionManager.getInstance().getAllMarkers().size()+1);
		if("value:parkour_profile".equals(openOptionList))rows=3;
		if("value:direction".equals(openOptionList))rows=8;
		if("value:destination_mode".equals(openOptionList))rows=3;
		return new Box(width/2-110,height/2-70,220,Math.max(140,rows*32+38));
	}

	private void drawOptionList(GuiGraphicsExtractor g,int mx,int my){
		Box b=optionListBox();RenderUtils.fillGlassPanel(g,b.x,b.y,b.w,b.h,10,BORDER,PANEL,PANEL,Util.getMillis());
		if("value:sprint".equals(openOptionList)){
			String raw=selected.value("sprint");
			RenderUtils.textBold(g,font,"Спринт и прыжки",b.x+14,b.y+12,TEXT);
			Box sprintRow=new Box(b.x+14,b.y+34,b.w-28,28),jumpRow=new Box(b.x+14,b.y+66,b.w-28,28);
			button(g,sprintRow,(raw.contains("sprint")?"✓  ":"")+"Спринт",sprintRow.has(mx,my),0xFF66D9A3,raw.contains("sprint"));
			button(g,jumpRow,(raw.contains("jump")?"✓  ":"")+"Прыгать",jumpRow.has(mx,my),0xFF66D9A3,raw.contains("jump"));
			RenderUtils.textFlat(g,font,"Клик мимо — закрыть",b.x+14,b.y+b.h-16,MUTED);
		}else if("value:path".equals(openOptionList)){
			String raw=selected.value("path");
			RenderUtils.textBold(g,font,"Маршрут",b.x+14,b.y+12,TEXT);
			Box straightRow=new Box(b.x+14,b.y+34,b.w-28,28),curvedRow=new Box(b.x+14,b.y+66,b.w-28,28);
			button(g,straightRow,"Прямой",straightRow.has(mx,my),ACCENT,!"curved".equalsIgnoreCase(raw));
			button(g,curvedRow,"Кривой",curvedRow.has(mx,my),ACCENT,"curved".equalsIgnoreCase(raw));
		}else if("value:coordinate_mode".equals(openOptionList)){
			Box manual=new Box(b.x+14,b.y+34,b.w-28,28),variable=new Box(b.x+14,b.y+66,b.w-28,28);button(g,manual,"Ручной ввод",manual.has(mx,my),ACCENT,"manual".equals(selected.value("coordinate_mode")));button(g,variable,"Переменная",variable.has(mx,my),ACCENT,"variable".equals(selected.value("coordinate_mode")));
		}else if("value:destination_mode".equals(openOptionList)){
			String value=selected.value("destination_mode");
			String[] values={"direction","manual","variable"};String[] labels={"Направление","Координаты","Метка"};
			for(int i=0;i<values.length;i++){Box row=new Box(b.x+14,b.y+34+i*32,b.w-28,28);button(g,row,labels[i],row.has(mx,my),ACCENT,values[i].equalsIgnoreCase(value));}
		}else if("value:mode".equals(openOptionList)){
			Box contains=new Box(b.x+14,b.y+34,b.w-28,28),regex=new Box(b.x+14,b.y+66,b.w-28,28);button(g,contains,"Содержит текст",contains.has(mx,my),ACCENT,!"regex".equalsIgnoreCase(selected.value("mode")));button(g,regex,"Регулярное выражение",regex.has(mx,my),ACCENT,"regex".equalsIgnoreCase(selected.value("mode")));
		}else if("value:parkour".equals(openOptionList)){
			Box normal=new Box(b.x+14,b.y+34,b.w-28,28),parkour=new Box(b.x+14,b.y+66,b.w-28,28);button(g,normal,"Обычное движение",normal.has(mx,my),ACCENT,!Boolean.parseBoolean(selected.value("parkour")));button(g,parkour,"Baritone-паркур",parkour.has(mx,my),0xFF6FE0C2,Boolean.parseBoolean(selected.value("parkour")));
		}else if("value:require_sign".equals(openOptionList)){
			Box any=new Box(b.x+14,b.y+34,b.w-28,28),required=new Box(b.x+14,b.y+66,b.w-28,28);button(g,any,"Без фильтра",any.has(mx,my),ACCENT,!Boolean.parseBoolean(selected.value("require_sign")));button(g,required,"Только с табличкой",required.has(mx,my),0xFFFFD166,Boolean.parseBoolean(selected.value("require_sign")));
		}else if("value:marker".equals(openOptionList)){
			int y=b.y+34,shown=0;for(var marker:RegionManager.getInstance().getAllMarkers()){
				if(shown++>=6)break;
				Box row=new Box(b.x+14,y,b.w-28,28);button(g,row,marker.name,row.has(mx,my),ACCENT,marker.name.equals(selected.value("marker")));y+=32;
			}
			Box add=new Box(b.x+14,y,b.w-28,28);button(g,add,"+ Добавить метку",add.has(mx,my),0xFF00AAFF,false);
		}else if("value:parkour_profile".equals(openOptionList)){
			String value=selected.value("parkour_profile");
			String[] profiles={"balanced","universal","h2h"};String[] labels={"Сбалансированный","Универсальный adaptive","H2H Neo / 45°"};
			for(int i=0;i<profiles.length;i++){Box row=new Box(b.x+14,b.y+34+i*32,b.w-28,28);button(g,row,labels[i],row.has(mx,my),0xFF6FE0C2,profiles[i].equalsIgnoreCase(value));}
		}else if("value:direction".equals(openOptionList)){
			String[] values={"forward","back","left","right","45_left","45_right","back_left","back_right"};String[] labels={"Вперёд","Назад","Влево","Вправо","45° влево","45° вправо","Назад-влево","Назад-вправо"};
			for(int i=0;i<values.length;i++){Box row=new Box(b.x+14,b.y+34+i*32,b.w-28,28);button(g,row,labels[i],row.has(mx,my),ACCENT,values[i].equalsIgnoreCase(selected.value("direction")));}
		}
	}

	@Override public boolean mouseClicked(MouseButtonEvent e,boolean dbl){double mx=e.x(),my=e.y();RenderUtils.addClickWave(mx,my);return editing==null?clickLibrary(mx,my,e.button()):clickEditor(mx,my,e.button());}
	private boolean clickLibrary(double mx,double my,int button){
		if(contextMenuFor!=null&&contextMenuBox!=null){Box menu=contextMenuBox;Box toggle=new Box(menu.x+156,menu.y+5,36,26);if(toggle.has(mx,my)){contextMenuFor.legit=!contextMenuFor.legit;AutomationManager.save();contextMenuFor=null;return true;}if(!menu.has(mx,my)){contextMenuFor=null;contextMenuBox=null;}}
		if(addConfigChoiceOpen){Box menu=configChoiceBox();if(!menu.has(mx,my)){addConfigChoiceOpen=false;return true;}if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT){if(new Box(menu.x+8,menu.y+8,menu.w-16,22).has(mx,my)){addConfigChoiceOpen=false;ActionRecorder.start();if(minecraft!=null)minecraft.gui.setScreen(null);return true;}if(new Box(menu.x+8,menu.y+34,menu.w-16,22).has(mx,my)){editing=AutomationManager.create();addConfigChoiceOpen=false;return true;}}return true;}
		int cw=Math.max(260,Math.min(370,(width-60)/2));
		if(button==GLFW.GLFW_MOUSE_BUTTON_RIGHT){
			int y=libraryCustomStartY(cw);
			int rx=20,ry=y;
			for(AutomationConfig c:AutomationManager.all()){
				if(rx+cw>width-20){rx=20;ry+=118;}
				if(new Box(rx,ry,cw,104).has(mx,my)){contextMenuFor=c;return true;}
				rx+=cw+14;
			}
			return true;
		}
		if(button!=GLFW.GLFW_MOUSE_BUTTON_LEFT)return true;if(new Box(20,12,142,25).has(mx,my)){addConfigChoiceOpen=true;return true;}
		int y=64+34;
		for(AutomationConfig c:AutomationDefaults.all()){
			int x=20;
			// Built-ins use the same two-column packing as the renderer.
			for(AutomationConfig ignored:AutomationDefaults.all()){
				if(ignored==c)break;
				x+=cw+14;
				if(x+cw>width-20){x=20;y+=118;}
			}
			if(new Box(x+18,y+70,72,22).has(mx,my)){if(AutomationRunner.isRunning(c))AutomationRunner.stop("Остановлено пользователем");else AutomationRunner.start(c);return true;}
			if(new Box(x+98,y+70,92,22).has(mx,my)){AutomationConfig copy=AutomationManager.copyOf(c);editing=copy;return true;}
		}
		int customY=libraryCustomStartY(cw),x=20;
		for(AutomationConfig c:new ArrayList<>(AutomationManager.all())){
			if(x+cw>width-20){x=20;customY+=118;}
			if(new Box(x+18,customY+70,72,22).has(mx,my)){if(AutomationRunner.isRunning(c))AutomationRunner.stop("Остановлено пользователем");else AutomationRunner.start(c);return true;}
			if(new Box(x+98,customY+70,92,22).has(mx,my)){editing=c;return true;}
			if(new Box(x+cw-72,customY+70,54,22).has(mx,my)){AutomationManager.remove(c);return true;}
			x+=cw+14;
		}
		return true;
	}
	private int libraryCustomStartY(int cw){
		int columns=Math.max(1,(width-26)/(cw+14));
		int rows=(AutomationDefaults.all().size()+columns-1)/columns;
		return 64+34+(rows-1)*118+104+18+34;
	}
	private boolean clickEditor(double mx,double my,int button){
		if(openOptionList!=null&&selected!=null){Box b=optionListBox();
			if(!b.has(mx,my)){openOptionList=null;return true;}
			if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT){
				Box row1=new Box(b.x+14,b.y+34,b.w-28,28),row2=new Box(b.x+14,b.y+66,b.w-28,28);
				if("value:sprint".equals(openOptionList)){
					String raw=selected.value("sprint");boolean sprint=raw.contains("sprint"),jump=raw.contains("jump");
					if(row1.has(mx,my))sprint=!sprint;else if(row2.has(mx,my))jump=!jump;
					selected.values.put("sprint",(sprint?"sprint":"")+(sprint&&jump?",":"")+(jump?"jump":""));
					return true;
				}else if("value:path".equals(openOptionList)){
					if(row1.has(mx,my)){selected.values.put("path","straight");openOptionList=null;return true;}
					if(row2.has(mx,my)){selected.values.put("path","curved");openOptionList=null;return true;}
				}else if("value:coordinate_mode".equals(openOptionList)){
					if(row1.has(mx,my)){selected.values.put("coordinate_mode","manual");openOptionList=null;return true;}if(row2.has(mx,my)){selected.values.put("coordinate_mode","variable");openOptionList=null;return true;}
				}else if("value:destination_mode".equals(openOptionList)){
					String[] modes={"direction","manual","variable"};
					for(int i=0;i<modes.length;i++)if(new Box(b.x+14,b.y+34+i*32,b.w-28,28).has(mx,my)){selected.values.put("destination_mode",modes[i]);openOptionList=null;return true;}
				}else if("value:mode".equals(openOptionList)){
					if(row1.has(mx,my)){selected.values.put("mode","contains");openOptionList=null;return true;}if(row2.has(mx,my)){selected.values.put("mode","regex");openOptionList=null;return true;}
				}else if("value:parkour".equals(openOptionList)){
					if(row1.has(mx,my)){selected.values.put("parkour","false");openOptionList=null;return true;}if(row2.has(mx,my)){selected.values.put("parkour","true");openOptionList=null;return true;}
				}else if("value:require_sign".equals(openOptionList)){
					if(row1.has(mx,my)){selected.values.put("require_sign","false");openOptionList=null;return true;}if(row2.has(mx,my)){selected.values.put("require_sign","true");openOptionList=null;return true;}
				}else if("value:marker".equals(openOptionList)){
					int y=b.y+34,shown=0;for(var marker:RegionManager.getInstance().getAllMarkers()){
						if(shown++>=6)break;
						if(new Box(b.x+14,y,b.w-28,28).has(mx,my)){selected.values.put("marker",marker.name);if(selected.type==AutomationNodeType.GOTO)selected.values.put("coordinate_mode","variable");openOptionList=null;return true;}y+=32;
					}
					if(new Box(b.x+14,y,b.w-28,28).has(mx,my)){activateMarkerCapture();return true;}
				}else if("value:parkour_profile".equals(openOptionList)){
					String[] profiles={"balanced","universal","h2h"};
					for(int i=0;i<profiles.length;i++)if(new Box(b.x+14,b.y+34+i*32,b.w-28,28).has(mx,my)){selected.values.put("parkour_profile",profiles[i]);openOptionList=null;return true;}
				}else if("value:direction".equals(openOptionList)){
					String[] directions={"forward","back","left","right","45_left","45_right","back_left","back_right"};
					for(int i=0;i<directions.length;i++)if(new Box(b.x+14,b.y+34+i*32,b.w-28,28).has(mx,my)){selected.values.put("direction",directions[i]);openOptionList=null;return true;}
				}
			}
			return true;}
		if(addMenuOpen){Box menu=addMenuBox();if(!menu.has(mx,my)){addMenuOpen=false;focusedField=null;return true;}if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT){Box search=new Box(menu.x+10,menu.y+10,menu.w-20,24);if(search.has(mx,my)){focusedField="add-menu-search";return true;}int listTop=menu.y+44,y=listTop+4-Math.round(addMenuScroll);for(AutomationNodeType.Category category:AutomationNodeType.Category.values()){List<AutomationNodeType> types=addMenuTypes(category);if(types.isEmpty())continue;y+=20;for(AutomationNodeType t:types){if(new Box(menu.x+8,y,menu.w-16,38).has(mx,my)&&my>=listTop&&my<menu.y+menu.h-8){addNode(t);return true;}y+=43;}}}return true;}
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT){
			if(new Box(10,30,12,120).has(mx,my)){draggingFreeCamHeight=true;updateFreeCamSlider(my);return true;}
			if(new Box(14,12,58,24).has(mx,my)){save();editing=null;selected=null;return true;}if(new Box(width-658,12,82,24).has(mx,my)){FreeLookController.getInstance().toggle();toast(FreeLookController.getInstance().isActive()?"FreeLook включён":"FreeLook выключен");return true;}if(new Box(width-568,12,82,24).has(mx,my)){RegionManager r=RegionManager.getInstance();r.addCornerMode=!r.addCornerMode;if(r.addCornerMode)r.addMarkerMode=false;return true;}if(new Box(width-478,12,82,24).has(mx,my)){RegionManager r=RegionManager.getInstance();r.addMarkerMode=!r.addMarkerMode;if(r.addMarkerMode)r.addCornerMode=false;return true;}if(new Box(width-388,12,80,24).has(mx,my)){FreeCamController.getInstance().toggle();toast(RegionManager.getInstance().freeCamActive?"FreeCam региона включён":"FreeCam региона выключен");return true;}if(new Box(width-300,12,78,24).has(mx,my)){addMenuOpen=true;addMenuScroll=addMenuTarget=0;focusedField=null;return true;}if(new Box(width-214,12,76,24).has(mx,my)){save();toast("Конфиг сохранён");return true;}
			if(new Box(width-130,12,72,24).has(mx,my)){save();AutomationRunner.start(editing);toast("Сценарий запущен");return true;}if(new Box(width-52,12,38,24).has(mx,my)){AutomationRunner.stop("Остановлено пользователем");return true;}
			if(nameBox().has(mx,my)){focusedField="name";return true;}if(new Box(10,TOP+42,LEFT-20,24).has(mx,my)){focusedField="palette-search";return true;}}
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&legitBox().has(mx,my)){editing.legit=!editing.legit;return true;}
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&selected!=null&&selected.type!=AutomationNodeType.START&&new Box(width-RIGHT+16,height-42,92,25).has(mx,my)){duplicateSelected();return true;}
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&selected!=null&&selected.type!=AutomationNodeType.START&&new Box(width-RIGHT+116,height-42,92,25).has(mx,my)){removeSelected();return true;}
		if(selected!=null){int y=TOP+76-Math.round(inspectorScroll);for(String key:inspectorKeys()){if(selected.type==AutomationNodeType.PLAYBACK&&"frames".equals(key))continue;if(new Box(width-RIGHT+16,y+13,RIGHT-32,24).has(mx,my)&&my>=TOP+68&&my<height-50){if(isOptionField(key))openOptionList="value:"+key;else focusedField="value:"+key;return true;}y+=49;}}focusedField=null;
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&new Box(LEFT+18,height-54,36,36).has(mx,my)){toast("Выберите действие слева");return true;}
		int py=TOP+78-Math.round(paletteScroll);for(AutomationNodeType t:paletteTypes()){if(new Box(10,py,LEFT-20,38).has(mx,my)&&my>=TOP+72){AutomationNode n=new AutomationNode(t,Math.max(24,width/2F-LEFT),80+editing.nodes.size()*18F);editing.nodes.add(n);selected=n;inspectorScroll=inspectorTarget=0;return true;}py+=43;}
		for(int i=editing.nodes.size()-1;i>=0;i--){AutomationNode n=editing.nodes.get(i);Box b=nodeBox(n);if(button==GLFW.GLFW_MOUSE_BUTTON_RIGHT&&b.has(mx,my)&&n.type!=AutomationNodeType.START){selected=n;removeSelected();return true;}if(button!=GLFW.GLFW_MOUSE_BUTTON_LEFT)continue;
			if(Math.hypot(mx-b.x,my-(b.y+NH/2.0))<=8&&linkFrom!=null){editing.links.removeIf(l->l.to.equals(n.id));editing.links.add(new AutomationLink(linkFrom.id,linkOutput,n.id));linkFrom=null;return true;}
			String out=hitOutput(n,mx,my);if(out!=null){linkFrom=n;linkOutput=out;return true;}if(b.has(mx,my)){if(selected!=n)inspectorScroll=inspectorTarget=0;selected=n;dragging=n;dragDx=(float)mx-n.x;dragDy=(float)my-n.y;return true;}}
		selected=null;linkFrom=null;return true;
	}
	@Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(draggingFreeCamHeight){updateFreeCamSlider(e.y());return true;}if(dragging!=null){float maxX=width-LEFT-RIGHT-NW-8,maxY=height-TOP-NH-8;float clampedX=clamp((float)e.x()-dragDx,8,maxX),clampedY=clamp((float)e.y()-dragDy,8,maxY);dragging.x=clamp(Math.round(clampedX/8F)*8F,8,maxX);dragging.y=clamp(Math.round(clampedY/8F)*8F,8,maxY);return true;}return super.mouseDragged(e,dx,dy);}
	@Override public boolean mouseReleased(MouseButtonEvent e){if(draggingFreeCamHeight){draggingFreeCamHeight=false;return true;}if(dragging!=null){dragging=null;return true;}return super.mouseReleased(e);}
	@Override public boolean mouseScrolled(double mx,double my,double sx,double sy){if(editing!=null&&addMenuOpen&&addMenuBox().has(mx,my)){int max=Math.max(0,addMenuContentHeight()-(addMenuBox().h-52));addMenuTarget=clamp(addMenuTarget-(float)sy*48,0,max);return true;}if(editing!=null&&mx<LEFT){int count=paletteTypes().size(),max=Math.max(0,count*43-(height-TOP-78));paletteTarget=clamp(paletteTarget-(float)sy*48,0,max);return true;}if(editing!=null&&mx>width-RIGHT&&selected!=null){int max=Math.max(0,selected.values.size()*49-(height-TOP-128));inspectorTarget=clamp(inspectorTarget-(float)sy*48,0,max);return true;}return super.mouseScrolled(mx,my,sx,sy);}
	@Override public boolean charTyped(CharacterEvent e){if(focusedField==null||!e.isAllowedChatCharacter())return super.charTyped(e);String s=e.codepointAsString();
		if("name".equals(focusedField)&&editing.name.length()<48)editing.name+=s;else if("palette-search".equals(focusedField)&&paletteQuery.length()<48)paletteQuery+=s;else if("add-menu-search".equals(focusedField)&&addMenuQuery.length()<48)addMenuQuery+=s;else if(focusedField.startsWith("value:")&&selected!=null){String k=focusedField.substring(6),v=selected.value(k);if(v.length()<120)selected.values.put(k,v+s);}return true;}
	@Override public boolean keyPressed(KeyEvent e){if(e.key()==GLFW.GLFW_KEY_ESCAPE&&addMenuOpen){addMenuOpen=false;focusedField=null;return true;}if(focusedField!=null){if(e.key()==GLFW.GLFW_KEY_BACKSPACE){if("name".equals(focusedField))editing.name=cut(editing.name);else if("palette-search".equals(focusedField))paletteQuery=cut(paletteQuery);else if("add-menu-search".equals(focusedField))addMenuQuery=cut(addMenuQuery);else if(focusedField.startsWith("value:")&&selected!=null){String k=focusedField.substring(6);selected.values.put(k,cut(selected.value(k)));}return true;}
			if(e.key()==GLFW.GLFW_KEY_ENTER||e.key()==GLFW.GLFW_KEY_KP_ENTER||e.key()==GLFW.GLFW_KEY_ESCAPE){focusedField=null;return true;}return true;}
		if((e.key()==GLFW.GLFW_KEY_DELETE||e.key()==GLFW.GLFW_KEY_BACKSPACE)&&selected!=null&&selected.type!=AutomationNodeType.START){removeSelected();return true;}
		if(e.key()==GLFW.GLFW_KEY_ESCAPE&&editing!=null){save();editing=null;selected=null;openOptionList=null;return true;}return super.keyPressed(e);}
	@Override public void onClose(){if(editing!=null)save();if(parent!=null&&minecraft!=null)minecraft.gui.setScreen(parent);else super.onClose();}

	private void save(){if(editing!=null){editing.name=editing.name==null||editing.name.isBlank()?"Без названия":editing.name.trim();AutomationManager.save();}}
	private void removeSelected(){if(selected==null||selected.type==AutomationNodeType.START)return;String id=selected.id;editing.nodes.remove(selected);editing.links.removeIf(l->id.equals(l.from)||id.equals(l.to));selected=null;focusedField=null;openOptionList=null;}
	private void duplicateSelected(){if(selected==null||selected.type==AutomationNodeType.START)return;AutomationNode duplicate=new AutomationNode(selected.type,selected.x+20F,selected.y+20F);duplicate.values=selected.values==null?new LinkedHashMap<>():new LinkedHashMap<>(selected.values);editing.nodes.add(duplicate);selected=duplicate;focusedField=null;inspectorScroll=inspectorTarget=0;}
	private Box nameBox(){return new Box(82,12,Math.max(120,Math.min(260,width-430)),24);}
	private Box legitBox(){Box n=nameBox();return new Box(n.x+n.w+8,12,72,24);}
	private List<AutomationNodeType> paletteTypes(){String query=paletteQuery.toLowerCase(Locale.ROOT);List<AutomationNodeType> types=new ArrayList<>();for(AutomationNodeType t:AutomationNodeType.values()){if(t!=AutomationNodeType.START&&(query.isEmpty()||t.title().toLowerCase(Locale.ROOT).contains(query)||t.description().toLowerCase(Locale.ROOT).contains(query)))types.add(t);}return types;}
	private Box configChoiceBox(){return new Box(20,42,190,64);}
	private Box addMenuBox(){return new Box(Math.max(0,Math.min(width-260,width-300)),Math.max(0,Math.min(40,height-320)),260,320);}
	private List<AutomationNodeType> addMenuTypes(AutomationNodeType.Category category){String query=addMenuQuery.toLowerCase(Locale.ROOT);List<AutomationNodeType> types=new ArrayList<>();for(AutomationNodeType t:AutomationNodeType.values())if(t.category()==category&&(query.isEmpty()||t.title().toLowerCase(Locale.ROOT).contains(query)||t.description().toLowerCase(Locale.ROOT).contains(query)))types.add(t);return types;}
	private int addMenuContentHeight(){int height=0;for(AutomationNodeType.Category category:AutomationNodeType.Category.values()){List<AutomationNodeType> types=addMenuTypes(category);if(!types.isEmpty())height+=20+types.size()*43;}return height;}
	private void addNode(AutomationNodeType type){AutomationNode n=new AutomationNode(type,Math.max(24,width/2F-LEFT),80+editing.nodes.size()*18F);editing.nodes.add(n);selected=n;inspectorScroll=inspectorTarget=0;addMenuOpen=false;addMenuQuery="";focusedField=null;}
	private void activateMarkerCapture(){
		RegionManager region=RegionManager.getInstance();
		region.addMarkerMode=true;
		region.addCornerMode=false;
		openOptionList=null;
		focusedField=null;
		save();
		FreeCamController.getInstance().activate();
		toast("FreeCam включён: наведите камеру и нажмите ПКМ по блоку");
		if(minecraft!=null)minecraft.gui.setScreen(null);
	}
	private Box nodeBox(AutomationNode n){return new Box(LEFT+Math.round(n.x),TOP+Math.round(n.y),NW,NH);}
	private int outputY(AutomationNode n,String out){Box b=nodeBox(n);return branch(n)?b.y+("false".equals(out)?57:39):b.y+48;}
	private String hitOutput(AutomationNode n,double mx,double my){if(n.type==AutomationNodeType.STOP)return null;Box b=nodeBox(n);if(Math.abs(mx-(b.x+b.w))>9)return null;if(branch(n)){if(Math.abs(my-(b.y+39))<=9)return "true";if(Math.abs(my-(b.y+57))<=9)return "false";}else if(Math.abs(my-(b.y+48))<=9)return "next";return null;}
	private static boolean branch(AutomationNode n){return n.type.branching();}
	private String summary(AutomationNode n){return switch(n.type){case START->"Начало потока";case STOP->"Конец потока";case GOTO->"variable".equalsIgnoreCase(n.value("coordinate_mode"))?"Метка: "+n.value("marker"):trim(n.value("x")+", "+n.value("y")+", "+n.value("z"),128);case OPEN->trim(n.value("x")+", "+n.value("y")+", "+n.value("z")+(Boolean.parseBoolean(n.value("require_sign"))?" • Табличка: "+n.value("sign_text"):""),128);case MINE->trim(n.value("block")+" × "+n.value("count"),128);case SEARCH->trim(n.value("block"),128);case USE,INTERACT->"Рука: "+n.value("hand");case SELECT_SLOT->"Слот "+n.value("slot");case PICKUP->trim("Предмет: "+n.value("item"),128);case MOVE_ITEM->n.value("from")+" → "+n.value("to");case QUICK_MOVE->"Слот "+n.value("slot");case DROP_ITEM->"Слот "+n.value("slot")+" • "+n.value("amount");case TAKE_CONTAINER->"Пауза "+n.value("delay_ticks")+" тик.";case EAT->trim(n.value("food"),128);case FOOD_CHECK,HEALTH_CHECK->n.value("operator")+" "+n.value("value");case ITEM_CHECK->trim(n.value("item")+" × "+n.value("count"),128);case CONTAINER_CHECK->"Состояние: "+n.value("state");case PLAYER_COUNT_CHECK->"Игроков рядом "+n.value("operator")+" "+n.value("value");case LOOK->"point".equals(n.value("mode"))?"К точке":"Yaw "+n.value("yaw")+" • Pitch "+n.value("pitch");case MOVE->"variable".equalsIgnoreCase(n.value("destination_mode"))?"Метка: "+n.value("marker"):"manual".equalsIgnoreCase(n.value("destination_mode"))?trim(n.value("x")+", "+n.value("y")+", "+n.value("z"),128):n.value("direction")+" • "+n.value("seconds")+" сек."+(Boolean.parseBoolean(n.value("parkour"))?" • "+optionListLabel("parkour_profile",n.value("parkour_profile")):"");case JUMP->"Обычный прыжок";case SNEAK->n.value("seconds")+" сек.";case ATTACK->n.value("swings")+" удар.";case FOLLOW->trim(n.value("entity")+" "+n.value("name"),128);case EXPLORE,FARM->"Радиус "+n.value("radius");case COORDINATE_CHECK->n.value("axis")+" "+n.value("operator")+" "+n.value("value");case CHAT,CHAT_SEND->trim(n.value("message"),128);case CHAT_COMMAND->trim(n.value("command"),128);case CHAT_WAIT,CHAT_CHECK->trim(n.value("pattern"),128);case WAIT,TIMER->n.value("seconds")+" сек.";case COMMAND->trim(n.value("command"),128);case SET_VARIABLE->trim(n.value("name")+" = "+n.value("value"),128);case CONDITION->trim(n.value("left")+" "+n.value("operator")+" "+n.value("right"),128);case PARALLEL->"Ветка + далее";case SET_FLAG->"Флаг: "+n.value("flag");case WAIT_FLAG->"Жду флаг: "+n.value("flag");case PLAYBACK->{String raw=n.value("frames");yield (raw.isEmpty()?0:raw.split(";").length)+" кадров";}};}
	private String label(String k){return switch(k){case"x","y","z"->"Координата "+k.toUpperCase();case"coordinate_mode"->"Идти к";case"marker"->"Метка";case"axis"->"Ось: x / y / z";case"block"->"ID блока";case"item"->"ID блока/предмета";case"count"->"Количество";case"slot"->"Слот / слот хотбара";case"from"->"Из слота меню";case"to"->"В слот меню";case"amount"->"one / stack";case"delay_ticks"->"Пауза между кликами";case"food"->"ID еды или any";case"state"->"Состояние: open / closed";case"restore_slot"->"Вернуть слот: true / false";case"require_sign"->"Фильтр по табличке";case"sign_text"->"Текст на табличке";case"parkour"->"Паркур Baritone";case"parkour_profile"->"Профиль паркура";case"mode"->"Режим сопоставления";case"yaw"->"Yaw";case"pitch"->"Pitch";case"direction"->"Направление";case"swings"->"Количество ударов";case"hand"->"Рука: main / off";case"entity"->"Тип сущности";case"radius"->"Радиус";case"message"->"Сообщение";case"pattern"->"Фрагмент или regex";case"timeout"->"Таймаут, сек.";case"seconds"->"Секунды";case"command"->"Команда чата";case"name"->"Имя / переменная";case"flag"->"Имя флага";case"value"->"Значение";case"left"->"Левая часть";case"operator"->"Оператор: == != > >= < <= contains";case"right"->"Правая часть";default->k;};}

	private void field(GuiGraphicsExtractor g,Box b,String value,String key,boolean focus,int mx,int my){float h=anim("field:"+key,b.has(mx,my)||focus);RenderUtils.fillRoundedBorder(g,b.x,b.y,b.w,b.h,7,RenderUtils.mix(BORDER,ACCENT,h),RenderUtils.mix(0xB30A0C12,0xD31B1D28,h*.45F));String shown=trim(value,b.w-18);if(focus&&(Util.getMillis()/500L)%2==0)shown+="|";RenderUtils.textFlat(g,font,shown,b.x+9,b.y+7,RenderUtils.mix(MUTED,TEXT,.45F+h*.55F));}
	private void button(GuiGraphicsExtractor g,Box b,String text,boolean hover,int color,boolean filled){float h=anim("button:"+text+":"+b.x+":"+b.y,hover);int bg=filled?RenderUtils.mix(color,0xFFFFFFFF,h*.12F):RenderUtils.mix(0xA9151720,color,.06F+h*.20F);int lift=Math.round(h);RenderUtils.fillGlassPanel(g,b.x,b.y-lift,b.w,b.h,7,RenderUtils.mix(filled?color:BORDER,0xFFFFFFFF,h*.18F),bg,bg,Util.getMillis());RenderUtils.textCentered(g,font,text,b.x+b.w/2,b.y-lift+(b.h-font.lineHeight)/2+1,filled?0xFF0B0C10:TEXT,false);}
	private void port(GuiGraphicsExtractor g,int x,int y,int color,String text){RenderUtils.fillCircle(g,x,y,6,0xFF090A0E);RenderUtils.fillCircle(g,x,y,4,color);if(!text.isEmpty())RenderUtils.textFlat(g,font,text,x-30,y-4,color);}
	private void drawRunIcon(GuiGraphicsExtractor g,Box b,boolean running){int cx=b.x+14,cy=b.y+b.h/2,color=0xFF0B0C10;if(running){g.fill(cx-4,cy-4,cx+4,cy+4,color);}else{for(int dy=-4;dy<=4;dy++){int half=4-Math.abs(dy);if(half<=0)continue;g.fill(cx-3,cy+dy,cx-3+half,cy+dy+1,color);}}}
	private void drawToast(GuiGraphicsExtractor g){long now=Util.getMillis(),age=now-messageAt;float p=Math.min(1F,age/180F),out=age>2200?Math.max(0F,(2600-age)/400F):1F,alpha=smooth(p)*out;int w=RenderUtils.width(font,message)+28,x=(width-w)/2,y=height-48+Math.round((1-smooth(p))*12);RenderUtils.drawSoftShadow(g,x,y,w,28,9,4);RenderUtils.fillGlassPanel(g,x,y,w,28,9,RenderUtils.withAlpha(BORDER,alpha),RenderUtils.withAlpha(0xFA171A22,alpha),RenderUtils.withAlpha(0xFA171A22,alpha),now);RenderUtils.textCentered(g,font,message,width/2,y+10,RenderUtils.withAlpha(TEXT,alpha),false);}
	private static void curve(GuiGraphicsExtractor g,int x0,int y0,int x1,int y1,int color){float c=Math.max(30,Math.abs(x1-x0)*.45F),px=x0,py=y0,phase=(Util.getMillis()%1200L)/1200F;for(int i=1;i<=28;i++){float t=i/28F,u=1-t,x=u*u*u*x0+3*u*u*t*(x0+c)+3*u*t*t*(x1-c)+t*t*t*x1,y=u*u*u*y0+3*u*u*t*y0+3*u*t*t*y1+t*t*t*y1;line(g,Math.round(px),Math.round(py),Math.round(x),Math.round(y),color);if(Math.abs(t-phase)<.025F)RenderUtils.fillCircle(g,x,y,3,RenderUtils.mix(color,0xFFFFFFFF,.55F));px=x;py=y;}}
	private static void line(GuiGraphicsExtractor g,int x0,int y0,int x1,int y1,int color){int steps=Math.max(Math.abs(x1-x0),Math.abs(y1-y0));if(steps==0){g.fill(x0,y0,x0+2,y0+2,color);return;}for(int i=0;i<=steps;i++){float t=i/(float)steps;int x=Math.round(x0+(x1-x0)*t),y=Math.round(y0+(y1-y0)*t);g.fill(x,y,x+2,y+2,color);}}
	private String trim(String s,int px){return RenderUtils.clamp(font,s==null?"":s,px);}private static String cut(String s){return s==null||s.isEmpty()?"":s.substring(0,s.length()-1);}private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}private void toast(String s){message=s;messageAt=Util.getMillis();}
	private float anim(String key,boolean target){float v=animations.getOrDefault(key,target?1F:0F),to=target?1F:0F;v+= (to-v)*.18F;if(Math.abs(v-to)<.005F)v=to;animations.put(key,v);return v;}
	private static float smooth(float t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
}
