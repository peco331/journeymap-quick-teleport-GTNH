package local.jmquicktp;

import java.util.List;

/**
 * Right-click context menu on the JourneyMap fullscreen map, mirroring the popup
 * menu newer JourneyMap versions show. Kept in one static owner slot because only
 * one fullscreen map is ever displayed at a time.
 */
public final class MapMenu {
    private static final int ITEM_HEIGHT = 14;
    private static final int TITLE_HEIGHT = 13;
    private static final int PAD = 3;
    private static final int LABEL_X = PAD + 3;

    private static Object owner;
    private static int anchorX;
    private static int anchorY;
    private static int blockX;
    private static int blockZ;
    private static int dimension;
    private static boolean caveView;
    private static Object hoveredWaypoint;
    private static String menuTitle;
    private static String[] labels;

    private MapMenu() { }

    /** @return true when the click belongs to the menu and must not reach JourneyMap. */
    public static boolean mouseClicked(Object screen, int mouseX, int mouseY, int button) {
        try {
            if (owner == screen) {
                int index = button == 0 ? hitTest(screen, mouseX, mouseY) : -1;
                Object targetScreen = owner;
                int x = blockX, z = blockZ, dim = dimension;
                boolean cave = caveView;
                Object waypoint = hoveredWaypoint;
                close(screen);
                if (index == 0) MapTeleport.teleport(targetScreen, x, z, dim, cave, waypoint);
                else if (index == 1) MapTeleport.newWaypoint(targetScreen, x, z, dim);
                return true;
            }
            if (button != 1 || screen == null) return false;
            if (chatVisible(screen)) return false;

            Object mc = MapTeleport.field(screen, "field_146297_k", "mc");
            Object player = MapTeleport.field(mc, "field_71439_g", "thePlayer");
            if (player == null) return false;

            int guiW = ((Number)MapTeleport.field(screen, "field_146294_l", "width")).intValue();
            int guiH = ((Number)MapTeleport.field(screen, "field_146295_m", "height")).intValue();
            if (mouseX < 0 || mouseY < 0 || mouseX >= guiW || mouseY >= guiH) return false;

            if ((Boolean)MapTeleport.invoke(screen, "isMouseOverButton",
                new Class<?>[]{int.class, int.class}, mouseX, mouseY)) return false;

            Object grid = MapTeleport.field(screen, "gridRenderer");
            Object mapType = MapTeleport.invoke(grid, "getMapType", new Class<?>[0]);
            int playerDim = ((Number)MapTeleport.field(player, "field_71093_bK", "dimension")).intValue();
            if (mapType == null || ((Number)MapTeleport.field(mapType, "dimension")).intValue() != playerDim) {
                MapTeleport.notify(player, "地图传送：请切回当前维度的地图。");
                return false;
            }

            // Resolve target coordinates from JourneyMap's internal layer delegate first,
            // which accurately reflects what is shown at the bottom of the fullscreen map.
            int resolvedX = 0;
            int resolvedZ = 0;
            boolean resolved = false;
            Object activeWaypoint = null;

            try {
                Object layerDelegate = MapTeleport.field(screen, "layerDelegate");
                if (layerDelegate != null) {
                    List<?> layers = (List<?>)MapTeleport.field(layerDelegate, "layers");
                    if (layers != null && layers.size() >= 2) {
                        // layers[1] is WaypointLayer
                        Object waypointLayer = layers.get(1);
                        activeWaypoint = MapTeleport.field(waypointLayer, "selected");

                        // layers[0] is BlockInfoLayer
                        Object blockInfoLayer = layers.get(0);
                        Object lastCoord = MapTeleport.field(blockInfoLayer, "lastCoord");
                        if (lastCoord != null) {
                            resolvedX = ((Number)MapTeleport.field(lastCoord, "x")).intValue();
                            resolvedZ = ((Number)MapTeleport.field(lastCoord, "z")).intValue();
                            resolved = true;
                        }
                    }
                }
            } catch (Throwable ignored) { }

            // Mathematical fallback directly mapping GUI coordinates to world coordinates
            if (!resolved) {
                int dw = ((Number)MapTeleport.field(mc, "field_71443_c", "displayWidth")).intValue();
                int dh = ((Number)MapTeleport.field(mc, "field_71440_d", "displayHeight")).intValue();
                double centerBlockX = ((Number)MapTeleport.field(grid, "centerBlockX")).doubleValue();
                double centerBlockZ = ((Number)MapTeleport.field(grid, "centerBlockZ")).doubleValue();
                int zoom = ((Number)MapTeleport.field(grid, "zoom")).intValue();
                double blockSize = Math.pow(2.0, (double)zoom);

                double relX = (mouseX - guiW / 2.0) / (double)guiW;
                double relY = (mouseY - guiH / 2.0) / (double)guiH;
                resolvedX = (int)Math.floor(centerBlockX + (relX * dw) / blockSize);
                resolvedZ = (int)Math.floor(centerBlockZ + (relY * dh) / blockSize);
            }

            owner = screen;
            anchorX = mouseX;
            anchorY = mouseY;
            blockX = resolvedX;
            blockZ = resolvedZ;
            dimension = playerDim;
            caveView = (Boolean)MapTeleport.invoke(mapType, "isUnderground", new Class<?>[0]);
            hoveredWaypoint = activeWaypoint;

            if (activeWaypoint != null) {
                try {
                    menuTitle = (String)MapTeleport.invoke(activeWaypoint, "getName", new Class<?>[0]);
                } catch (Throwable t) {
                    menuTitle = blockX + ", " + blockZ;
                }
            } else {
                menuTitle = blockX + ", " + blockZ;
            }

            labels = new String[]{"传送到此处", "新建导航点"};
            return true;
        } catch (Throwable error) {
            close(screen);
            System.err.println("[JMQuickTP] Map menu click failed: " + error);
            return false;
        }
    }

    /** Escape closes the menu; any other key only dismisses it and keeps JourneyMap's binding. */
    public static boolean keyTyped(Object screen, int key) {
        if (owner != screen) return false;
        close(screen);
        return key == 1;
    }

    public static void draw(Object screen) {
        if (owner != screen) return;
        try {
            Object mc = MapTeleport.field(screen, "field_146297_k", "mc");
            Object font = MapTeleport.field(mc, "field_71466_p", "fontRenderer");
            int[] box = bounds(screen, font);
            int x = box[0], y = box[1], w = box[2], h = box[3];
            int mouseX = ((Number)MapTeleport.field(screen, "mx")).intValue();
            int mouseY = ((Number)MapTeleport.field(screen, "my")).intValue();
            rectangle(x + 2, y + 2, w, h, 0x000000, 120);          // drop shadow
            rectangle(x, y, w, h, 0x101010, 240);                  // background
            rectangle(x, y, w, 1, 0xFFFFFF, 70);                   // border
            rectangle(x, y + h - 1, w, 1, 0xFFFFFF, 70);
            rectangle(x, y, 1, h, 0xFFFFFF, 70);
            rectangle(x + w - 1, y, 1, h, 0xFFFFFF, 70);
            text(font, menuTitle != null ? menuTitle : (blockX + ", " + blockZ), x + LABEL_X, y + 3, 0x9E9E9E, false);
            rectangle(x + 2, y + TITLE_HEIGHT, w - 4, 1, 0xFFFFFF, 50);
            for (int i = 0; i < labels.length; i++) {
                int itemY = y + TITLE_HEIGHT + 1 + i * ITEM_HEIGHT;
                boolean hover = mouseX >= x + 1 && mouseX < x + w - 1 && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
                if (hover) rectangle(x + 1, itemY, w - 2, ITEM_HEIGHT, 0xFFFFFF, 50);
                text(font, labels[i], x + LABEL_X, itemY + 3, hover ? 0xFFFFA0 : 0xE6E6E6, true);
            }
        } catch (Throwable error) {
            close(screen);
            System.err.println("[JMQuickTP] Map menu draw failed: " + error);
        }
    }

    public static void close(Object screen) {
        if (owner != screen) return;
        owner = null;
        labels = null;
        hoveredWaypoint = null;
        menuTitle = null;
    }

    private static int hitTest(Object screen, int mouseX, int mouseY) throws ReflectiveOperationException {
        Object mc = MapTeleport.field(screen, "field_146297_k", "mc");
        Object font = MapTeleport.field(mc, "field_71466_p", "fontRenderer");
        int[] box = bounds(screen, font);
        int x = box[0], y = box[1], w = box[2];
        if (mouseX < x + 1 || mouseX >= x + w - 1) return -1;
        int offset = mouseY - (y + TITLE_HEIGHT + 1);
        if (offset < 0) return -1;
        int index = offset / ITEM_HEIGHT;
        return (labels != null && index < labels.length) ? index : -1;
    }

    private static int[] bounds(Object screen, Object font) throws ReflectiveOperationException {
        String titleStr = menuTitle != null ? menuTitle : (blockX + ", " + blockZ);
        int width = width(font, titleStr);
        if (labels != null) {
            for (String label : labels) width = Math.max(width, width(font, label));
        }
        width += 2 * LABEL_X;
        int count = labels != null ? labels.length : 2;
        int height = TITLE_HEIGHT + count * ITEM_HEIGHT + 1;
        int screenW = ((Number)MapTeleport.field(screen, "field_146294_l", "width")).intValue();
        int screenH = ((Number)MapTeleport.field(screen, "field_146295_m", "height")).intValue();
        int x = Math.max(0, Math.min(anchorX, screenW - width));
        int y = Math.max(0, Math.min(anchorY, screenH - height));
        return new int[]{x, y, width, height};
    }

    private static boolean chatVisible(Object screen) throws ReflectiveOperationException {
        Object chat = MapTeleport.field(screen, "chat");
        return chat != null && !(Boolean)MapTeleport.invoke(chat, "isHidden", new Class<?>[0]);
    }

    private static int width(Object font, String label) throws ReflectiveOperationException {
        return ((Number)MapTeleport.invoke(font, new String[]{"func_78256_a", "getStringWidth"},
            new Class<?>[]{String.class}, label)).intValue();
    }

    private static void text(Object font, String label, int x, int y, int color, boolean shadow) throws ReflectiveOperationException {
        MapTeleport.invoke(font, shadow ? new String[]{"func_78261_a", "drawStringWithShadow"}
            : new String[]{"func_78276_b", "drawString"}, new Class<?>[]{String.class, int.class, int.class, int.class},
            label, x, y, color);
    }

    private static void rectangle(double x, double y, double width, double height, int color, int alpha)
        throws ReflectiveOperationException {
        Class<?> draw = Class.forName("journeymap.client.render.draw.DrawUtil");
        draw.getMethod("drawRectangle", double.class, double.class, double.class, double.class, int.class, int.class)
            .invoke(null, x, y, width, height, color, alpha);
    }
}
