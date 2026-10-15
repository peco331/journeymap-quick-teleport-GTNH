"""Compile runtime fixtures separately; exercise real hook classes and installed JM bytecode."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
import json, os, sys

def _setting(env, key):
    value = os.environ.get(env)
    cfg = ROOT / 'paths.local.json'
    if not value and cfg.exists():
        value = json.loads(cfg.read_text(encoding='utf-8-sig')).get(key)
    if not value:
        sys.exit(f"Missing setting '{key}': set {env} or create paths.local.json (see paths.example.json)")
    return Path(value)

JAVA = _setting('JMQTP_JDK', 'jdk') / 'bin'
LIB = _setting('JMQTP_LIBRARY_ROOT', 'libraryRoot') / 'org/ow2/asm'
SERVER = _setting('JMQTP_SERVER_ROOT', 'serverRoot')
FORGE = next(SERVER.glob('forge-1.7.10-*-universal.jar'))
LAUNCH = SERVER / 'libraries/net/minecraft/launchwrapper/1.15/launchwrapper-1.15.jar'
JM = _setting('JMQTP_CLIENT_ROOT', 'clientRoot') / 'mods/journeymap-1.7.10-5.2.23-unlimited.jar'
BUILD = ROOT / 'build/test-fixtures'
BUILD.mkdir(parents=True, exist_ok=True)

fixtures = {
'org/lwjgl/input/Mouse.java': '''package org.lwjgl.input;
public class Mouse { public static int x=0,y=0,ex=400,ey=300;
public static int getX(){return x;} public static int getY(){return y;}
public static int getEventX(){return ex;} public static int getEventY(){return ey;} }''',
'net/minecraft/util/IChatComponent.java': 'package net.minecraft.util; public interface IChatComponent {}',
'net/minecraft/util/ChatComponentText.java': '''package net.minecraft.util;
public class ChatComponentText implements IChatComponent { public String text; public ChatComponentText(String t){text=t;} }''',
'net/minecraft/entity/Entity.java': '''package net.minecraft.entity; public class Entity {
public int dimension; public double posY=80; public String notice; public String lastChatMessage;
public void addChatMessage(net.minecraft.util.IChatComponent c){notice=((net.minecraft.util.ChatComponentText)c).text;}
public void sendChatMessage(String msg){lastChatMessage=msg;} }''',
'net/minecraft/client/gui/FontRenderer.java': '''package net.minecraft.client.gui;
public class FontRenderer {
public int func_78256_a(String s){return s.length()*6;}
public int func_78261_a(String s,int x,int y,int c){return 0;}
public int func_78276_b(String s,int x,int y,int c){return 0;} }''',
'net/minecraft/client/gui/GuiButton.java': '''package net.minecraft.client.gui; public class GuiButton {}''',
'net/minecraft/client/Minecraft.java': '''package net.minecraft.client;
public class Minecraft { public Object thePlayer, theWorld; public int displayWidth=800,displayHeight=600;
public net.minecraft.client.gui.FontRenderer fontRenderer=new net.minecraft.client.gui.FontRenderer(); }''',
'journeymap/client/forge/helper/ForgeHelper.java': '''package journeymap.client.forge.helper;
public class ForgeHelper {public static final ForgeHelper INSTANCE=new ForgeHelper();
public String getEntityName(net.minecraft.entity.Entity e){return "tester";} }''',
'journeymap/client/model/Waypoint.java': '''package journeymap.client.model;
public class Waypoint {public enum Type{Normal} public int x,y,z,dimension; public String name="TestWP";
public static Waypoint at(int x,int y,int z,Type t,int d){Waypoint p=new Waypoint();p.x=x;p.y=y;p.z=z;p.dimension=d;return p;}
public int getX(){return x;} public int getY(){return y;} public int getZ(){return z;}
public String getName(){return name;} public java.util.Collection<Integer> getDimensions(){return java.util.Arrays.asList(dimension);}
public boolean isTeleportReady(){return y>=0;} }''',
'journeymap/client/waypoint/WaypointStore.java': '''package journeymap.client.waypoint;
public class WaypointStore {static final WaypointStore INSTANCE=new WaypointStore();
public final java.util.Collection<Object> points=new java.util.ArrayList<Object>();
public static WaypointStore instance(){return INSTANCE;}public java.util.Collection<Object> getAll(){return points;} }''',
'journeymap/client/command/CmdTeleportWaypoint.java': '''package journeymap.client.command;
public class CmdTeleportWaypoint {public static boolean allowed=true;public static int sent;
public static journeymap.client.model.Waypoint last;public journeymap.client.model.Waypoint point;
public CmdTeleportWaypoint(journeymap.client.model.Waypoint p){point=p;}
public static boolean isPermitted(net.minecraft.client.Minecraft mc){return allowed;}
public void run(){sent++;last=point;} }''',
'journeymap/client/render/draw/DrawUtil.java': '''package journeymap.client.render.draw;
public class DrawUtil { public static int rects;
public static void drawRectangle(double x,double y,double w,double h,int color,int alpha){rects++;} }''',
'journeymap/client/ui/component/JmUI.java': '''package journeymap.client.ui.component; public class JmUI {}''',
'journeymap/client/ui/fullscreen/Fullscreen.java': '''package journeymap.client.ui.fullscreen;
public class Fullscreen extends journeymap.client.ui.component.JmUI {}''',
'journeymap/client/ui/UIManager.java': '''package journeymap.client.ui;
public class UIManager {public static int closed;public static int editorOpened;public static journeymap.client.model.Waypoint editorWaypoint;
static final UIManager INSTANCE=new UIManager();
public static UIManager getInstance(){return INSTANCE;}public void closeAll(){closed++;}
public void openWaypointEditor(journeymap.client.model.Waypoint w,boolean isNew,journeymap.client.ui.component.JmUI parent){editorOpened++;editorWaypoint=w;} }''',
}
paths = []
for name, content in fixtures.items():
    path = BUILD / 'src' / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding='utf-8')
    paths.append(str(path))
classes = BUILD / 'classes'
classes.mkdir(exist_ok=True)
asm = [LIB / f'{module}/9.7.1/{module}-9.7.1.jar' for module in ('asm','asm-tree','asm-analysis')]
cp = ';'.join(map(str, [ROOT/'build/classes', *asm, FORGE, LAUNCH]))
subprocess.run([str(JAVA/'javac.exe'), '--release','8','-encoding','UTF-8','-cp',cp,'-d',str(classes),*paths,str(ROOT/'tests/MenuTest.java')],check=True)
subprocess.run([str(JAVA/'java.exe'),'-Xverify:all','-cp',str(classes)+';'+cp,'local.jmquicktp.MenuTest',str(JM)],check=True)
