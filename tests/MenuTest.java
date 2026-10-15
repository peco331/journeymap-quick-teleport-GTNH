package local.jmquicktp;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.lang.reflect.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import journeymap.client.command.CmdTeleportWaypoint;
import journeymap.client.model.Waypoint;
import journeymap.client.waypoint.WaypointStore;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.ui.UIManager;
import org.lwjgl.input.Mouse;

/** Drives the real hook classes against JourneyMap-shaped stand-ins, then the installed bytecode. */
public class MenuTest {
    static int checks;
    static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); checks++; }

    public static class Chunk { boolean empty; int y = 72;
        public boolean isEmpty() { return empty; }
        public int getHeightValue(int x, int z) { return y; } }
    public static class World { Chunk chunk = new Chunk(); int cx, cz; boolean clear = true;
        public Chunk getChunkFromChunkCoords(int x, int z) { cx = x; cz = z; return chunk; }
        public int getActualHeight() { return 256; }
        public boolean isAirBlock(int x, int y, int z) { return clear; } }
    public static class Coord { public int x = -33, z = 47; }
    public static class MapType { public int dimension; public boolean surface = true;
        public boolean isUnderground() { return !surface; } }
    public static class Grid { MapType type = new MapType(); Coord coord = new Coord(); double px = -1, py = -1; int dw, dh;
        public double centerBlockX = 0, centerBlockZ = 0; public int zoom = 0;
        public MapType getMapType() { return type; }
        public Coord getBlockUnderMouse(double x, double y, int w, int h) { px = x; py = y; dw = w; dh = h; return coord; } }
    public static class Parent { public Minecraft mc = new Minecraft(); public int width = 400, height = 300; }
    public static class Chat { public boolean hidden; public boolean isHidden() { return hidden; } }
    public static class BlockInfoLayer { public Coord lastCoord = new Coord(); }
    public static class WaypointLayer { public Object selected; }
    public static class LayerDelegate { public List<Object> layers = new ArrayList<Object>();
        public LayerDelegate() { layers.add(new BlockInfoLayer()); layers.add(new WaypointLayer()); } }
    public static class Screen extends Parent {
        public Grid gridRenderer = new Grid();
        public LayerDelegate layerDelegate = new LayerDelegate();
        public boolean button;
        public Chat chat;
        public String command;
        public int mx, my;
        public boolean isMouseOverButton(int x, int y) { return button; }
        public void openChat(String text) { command = text; chat = new Chat(); } }

    static Screen fresh() {
        CmdTeleportWaypoint.allowed = true; CmdTeleportWaypoint.sent = 0; CmdTeleportWaypoint.last = null;
        UIManager.closed = 0; UIManager.editorOpened = 0; UIManager.editorWaypoint = null;
        WaypointStore.instance().points.clear();
        DrawUtil.rects = 0;
        Mouse.x = 0; Mouse.y = 0; Mouse.ex = 400; Mouse.ey = 300;
        Screen screen = new Screen();
        screen.mc.thePlayer = new Entity();
        screen.mc.theWorld = new World();
        return screen;
    }

    public static void main(String[] args) throws Exception {
        Screen s = fresh();
        check(!MapMenu.keyTyped(s, 19), "other keys pass through without menu");
        check(!MapMenu.mouseClicked(s, 200, 149, 0), "left click passes through without menu");
        check(MapMenu.mouseClicked(s, 200, 149, 1), "right click opens menu");

        s.mx = 210; s.my = 170;
        MapMenu.draw(s);
        check(DrawUtil.rects >= 5, "menu painted over map");
        check(MapMenu.mouseClicked(s, 210, 170, 0), "menu item click consumed");
        check("/tp tester -33 72 47".equals(((Entity)s.mc.thePlayer).lastChatMessage), "direct teleport command sent without chat prompt");
        check(s.command == null, "never prompts openChat");
        check(UIManager.closed == 1, "map closed after command");

        // Test second menu item: Create Waypoint
        s = fresh();
        MapMenu.mouseClicked(s, 200, 149, 1);
        check(MapMenu.mouseClicked(s, 210, 184, 0), "second item click consumed");
        check(UIManager.editorOpened == 1 && UIManager.editorWaypoint.x == -33 && UIManager.editorWaypoint.y == 72, "waypoint editor opens on second item");

        // Test escape key to close menu
        s = fresh();
        MapMenu.mouseClicked(s, 200, 149, 1);
        check(MapMenu.keyTyped(s, 1), "escape consumed to close menu");
        check(!MapMenu.keyTyped(s, 1), "escape passes through once closed");

        // Test other key closes menu and passes through
        s = fresh();
        MapMenu.mouseClicked(s, 200, 149, 1);
        check(!MapMenu.keyTyped(s, 20), "other key dismisses menu and keeps JourneyMap binding");

        // Test outside click
        s = fresh();
        MapMenu.mouseClicked(s, 200, 149, 1);
        check(MapMenu.mouseClicked(s, 60, 260, 0), "click outside menu consumed");
        check(((Entity)s.mc.thePlayer).lastChatMessage == null, "outside click does not teleport");

        // Test right-click again dismisses menu
        s = fresh();
        MapMenu.mouseClicked(s, 200, 149, 1);
        check(MapMenu.mouseClicked(s, 200, 149, 1), "right click dismisses open menu");
        s.mx = 60; s.my = 260;
        MapMenu.draw(s);
        check(DrawUtil.rects == 0, "dismissed menu is not drawn");

        // Test chat input safety
        s = fresh();
        s.chat = new Chat();
        check(!MapMenu.mouseClicked(s, 200, 149, 1), "visible map chat keeps right click");
        s.chat.hidden = true;
        check(MapMenu.mouseClicked(s, 200, 149, 1), "hidden chat allows menu");
        MapMenu.close(s);

        // Test toolbar safety
        s = fresh();
        s.button = true;
        check(!MapMenu.mouseClicked(s, 200, 149, 1), "toolbar right click ignored");

        // Test dimension check
        s = fresh();
        s.gridRenderer.type.dimension = -1;
        check(!MapMenu.mouseClicked(s, 200, 149, 1), "other dimension rejected");
        check(((Entity)s.mc.thePlayer).notice != null, "dimension notice shown");

        // Test unloaded chunk: direct teleport with safe height, NO openChat!
        s = fresh();
        ((World)s.mc.theWorld).chunk.empty = true;
        MapTeleport.teleport(s, -33, 47, 0, false, null);
        check("/tp tester -33 80 47".equals(((Entity)s.mc.thePlayer).lastChatMessage), "unloaded chunk teleports directly at player height without chat prompt");
        check(s.command == null, "no openChat on unloaded chunk");

        // Test cave view: directly teleports at player height
        s = fresh();
        MapTeleport.teleport(s, -33, 47, 0, true, null);
        check("/tp tester -33 80 47".equals(((Entity)s.mc.thePlayer).lastChatMessage), "cave view directly teleports at current player height");
        check(s.command == null, "no openChat on cave view");

        // Test permission denied
        s = fresh();
        CmdTeleportWaypoint.allowed = false;
        MapTeleport.teleport(s, -33, 47, 0, false, null);
        check(((Entity)s.mc.thePlayer).lastChatMessage == null, "permission respected");

        // Test existing waypoint teleport
        s = fresh();
        ((World)s.mc.theWorld).chunk.empty = true;
        Waypoint wp = Waypoint.at(-33, 91, 47, Waypoint.Type.Normal, 0);
        MapTeleport.teleport(s, -33, 47, 0, false, wp);
        check("/tp tester -33 91 47".equals(((Entity)s.mc.thePlayer).lastChatMessage), "stored waypoint height teleports directly");

        // Test geometric mapping fallback without layerDelegate
        s = fresh();
        s.layerDelegate = null;
        s.gridRenderer.centerBlockX = 100.0;
        s.gridRenderer.centerBlockZ = 200.0;
        s.gridRenderer.zoom = 1; // blockSize = 2
        // mouse at center: (200, 150) with width=400, height=300
        MapMenu.mouseClicked(s, 200, 150, 1);
        s.mx = 210; s.my = 170;
        MapMenu.mouseClicked(s, 210, 170, 0);
        check("/tp tester 100 72 200".equals(((Entity)s.mc.thePlayer).lastChatMessage), "geometric fallback maps screen center to centerBlock");

        testTransformer(args[0]);
        System.out.println("PASS: " + checks + " checks; direct teleport, right-click menu, event coordinates, fixtures and actual JourneyMap bytecode");
    }

    static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[8192]; int n;
        while ((n = in.read(b)) != -1) out.write(b, 0, n);
        return out.toByteArray();
    }

    static void testTransformer(String jar) throws Exception {
        MapTransformer transformer = new MapTransformer();
        byte[] original;
        try (ZipFile zip = new ZipFile(jar)) {
            InputStream in = zip.getInputStream(zip.getEntry(MapTransformer.TARGET.replace('.', '/') + ".class"));
            original = read(in);
        }
        byte[] patched = transformer.transform(MapTransformer.TARGET, MapTransformer.TARGET, original);
        check(!Arrays.equals(patched, original), "actual fullscreen transformed");
        check(Arrays.equals(patched, transformer.transform(MapTransformer.TARGET, MapTransformer.TARGET, patched)), "idempotent");
        check(transformer.transform("other.Screen", "other.Screen", original) == original, "other classes untouched");
        ClassNode node = new ClassNode();
        new ClassReader(patched).accept(node, 0);
        int mouse = 0, key = 0, draw = 0, close = 0;
        for (Object o : node.methods) {
            MethodNode m = (MethodNode)o;
            new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name, m);
            for (AbstractInsnNode i : m.instructions.toArray()) {
                if (i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals("local/jmquicktp/MapMenu")) {
                    String name = ((MethodInsnNode)i).name;
                    if (name.equals("mouseClicked")) mouse++;
                    else if (name.equals("keyTyped")) key++;
                    else if (name.equals("draw")) draw++;
                    else if (name.equals("close")) close++;
                }
            }
        }
        check(mouse == 1 && key == 1 && draw == 1 && close == 1,
            "four hooks on the real methods, all passing ASM data flow verification");

        // JVM executes every injected branch and its stack-map frame.
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        String internal = MapTransformer.TARGET.replace('.', '/');
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internal, null, "java/lang/Object", null);
        cw.visitField(Opcodes.ACC_PUBLIC, "calls", "I", null, null).visitEnd();
        cw.visitField(Opcodes.ACC_PUBLIC, "chat", "Ljava/lang/Object;", null, null).visitEnd();
        MethodVisitor ctor = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitCode(); ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(Opcodes.RETURN); ctor.visitMaxs(0, 0); ctor.visitEnd();
        voidMethod(cw, "func_73869_a", "(CI)V");
        voidMethod(cw, "func_73864_a", "(III)V");
        voidMethod(cw, "func_73863_a", "(IIF)V");
        voidMethod(cw, "func_146281_b", "()V");
        cw.visitEnd();
        byte[] synthetic = transformer.transform(MapTransformer.TARGET, MapTransformer.TARGET, cw.toByteArray());
        Class<?> testClass = new ClassLoader(MenuTest.class.getClassLoader()) {
            Class<?> load() { return defineClass(MapTransformer.TARGET, synthetic, 0, synthetic.length); }
        }.load();
        Object screen = testClass.getConstructor().newInstance();
        testClass.getMethod("func_73869_a", char.class, int.class).invoke(screen, 't', 20);
        testClass.getMethod("func_73864_a", int.class, int.class, int.class).invoke(screen, 200, 149, 0);
        testClass.getMethod("func_73863_a", int.class, int.class, float.class).invoke(screen, 400, 300, 0.0F);
        testClass.getMethod("func_146281_b").invoke(screen);
        check(testClass.getField("calls").getInt(screen) == 4, "all original paths still run with JVM verification");
    }

    static void voidMethod(ClassWriter cw, String name, String desc) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, name, desc, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(Opcodes.DUP);
        mv.visitFieldInsn(Opcodes.GETFIELD, MapTransformer.TARGET.replace('.', '/'), "calls", "I");
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitInsn(Opcodes.IADD);
        mv.visitFieldInsn(Opcodes.PUTFIELD, MapTransformer.TARGET.replace('.', '/'), "calls", "I");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
