package local.jmquicktp;

import java.lang.reflect.*;
import java.util.Collection;

/**
 * Teleport actions for the fullscreen context menu. Reflection keeps this class
 * independent of development mappings and of the JourneyMap jar at compile time.
 */
final class MapTeleport {
    private MapTeleport() { }

    /** Directly teleports the player to the target block, matching modern JourneyMap behavior. */
    static void teleport(Object screen, int x, int z, int dimension, boolean caveView, Object existingWaypoint) throws ReflectiveOperationException {
        Object mc = field(screen, "field_146297_k", "mc");
        Object player = field(mc, "field_71439_g", "thePlayer");
        Object world = field(mc, "field_71441_e", "theWorld");
        if (player == null || world == null) return;

        Class<?> command = Class.forName("journeymap.client.command.CmdTeleportWaypoint");
        if (!(Boolean)command.getMethod("isPermitted", Class.forName("net.minecraft.client.Minecraft")).invoke(null, mc)) {
            notify(player, "地图传送：需要开启作弊或获得 /tp 权限。");
            return;
        }

        int targetY;
        if (existingWaypoint != null && (Boolean)invoke(existingWaypoint, "isTeleportReady", new Class<?>[0])) {
            targetY = ((Number)invoke(existingWaypoint, "getY", new Class<?>[0])).intValue();
        } else {
            Object waypoint = findWaypoint(x, z, dimension);
            if (waypoint != null && (Boolean)invoke(waypoint, "isTeleportReady", new Class<?>[0])) {
                targetY = ((Number)invoke(waypoint, "getY", new Class<?>[0])).intValue();
            } else {
                int currentY = (int)Math.floor(((Number)field(player, "field_70163_u", "posY")).doubleValue());
                if (caveView) {
                    targetY = currentY;
                } else {
                    Object chunk = invoke(world, new String[]{"func_72964_e", "getChunkFromChunkCoords"},
                        new Class<?>[]{int.class, int.class}, x >> 4, z >> 4);
                    boolean empty = (Boolean)invoke(chunk, new String[]{"func_76621_g", "isEmpty"}, new Class<?>[0]);
                    int surfaceY = empty ? -1 : ((Number)invoke(chunk, new String[]{"func_76626_d", "getHeightValue"},
                        new Class<?>[]{int.class, int.class}, x & 15, z & 15)).intValue();
                    int height = ((Number)invoke(world, new String[]{"func_72940_L", "getActualHeight"}, new Class<?>[0])).intValue();
                    boolean clear = surfaceY >= 1 && surfaceY <= height - 2 &&
                        (Boolean)invoke(world, new String[]{"func_147437_c", "isAirBlock"},
                            new Class<?>[]{int.class, int.class, int.class}, x, surfaceY, z) &&
                        (Boolean)invoke(world, new String[]{"func_147437_c", "isAirBlock"},
                            new Class<?>[]{int.class, int.class, int.class}, x, surfaceY + 1, z);
                    targetY = clear ? surfaceY : (currentY > 0 ? currentY : 64);
                }
            }
        }

        Object helper = Class.forName("journeymap.client.forge.helper.ForgeHelper").getField("INSTANCE").get(null);
        String playerName = (String)invoke(helper, "getEntityName",
            new Class<?>[]{Class.forName("net.minecraft.entity.Entity")}, player);

        // Directly execute teleport command without prompting chat input, mirroring modern JourneyMap.
        String tpCommand = String.format("/tp %s %d %d %d", playerName, x, targetY, z);
        invoke(player, new String[]{"func_71165_d", "sendChatMessage"}, new Class<?>[]{String.class}, tpCommand);

        // Close fullscreen map
        Object manager = Class.forName("journeymap.client.ui.UIManager").getMethod("getInstance").invoke(null);
        invoke(manager, "closeAll", new Class<?>[0]);
    }

    /** Opens JourneyMap's waypoint editor for the target block. */
    static void newWaypoint(Object screen, int x, int z, int dimension) throws ReflectiveOperationException {
        Object mc = field(screen, "field_146297_k", "mc");
        Object world = field(mc, "field_71441_e", "theWorld");
        Object player = field(mc, "field_71439_g", "thePlayer");
        int currentY = player != null ? (int)Math.floor(((Number)field(player, "field_70163_u", "posY")).doubleValue()) : 64;

        Object chunk = invoke(world, new String[]{"func_72964_e", "getChunkFromChunkCoords"},
            new Class<?>[]{int.class, int.class}, x >> 4, z >> 4);
        int y = currentY;
        if (!(Boolean)invoke(chunk, new String[]{"func_76621_g", "isEmpty"}, new Class<?>[0])) {
            int surfaceY = ((Number)invoke(chunk, new String[]{"func_76626_d", "getHeightValue"},
                new Class<?>[]{int.class, int.class}, x & 15, z & 15)).intValue();
            if (surfaceY > 0) y = surfaceY;
        }

        Class<?> waypointClass = Class.forName("journeymap.client.model.Waypoint");
        Class<?> typeClass = Class.forName("journeymap.client.model.Waypoint$Type");
        Object waypoint = waypointClass.getMethod("at", int.class, int.class, int.class, typeClass, int.class)
            .invoke(null, x, y, z, typeClass.getField("Normal").get(null), dimension);
        Object manager = Class.forName("journeymap.client.ui.UIManager").getMethod("getInstance").invoke(null);
        manager.getClass().getMethod("openWaypointEditor", waypointClass, boolean.class,
            Class.forName("journeymap.client.ui.component.JmUI"))
            .invoke(manager, waypoint, true,
                Class.forName("journeymap.client.ui.fullscreen.Fullscreen").getConstructor().newInstance());
    }

    static Object findWaypoint(int x, int z, int dimension) throws ReflectiveOperationException {
        Object store = Class.forName("journeymap.client.waypoint.WaypointStore").getMethod("instance").invoke(null);
        for (Object point : (Collection<?>)invoke(store, "getAll", new Class<?>[0])) {
            if (((Number)invoke(point, "getX", new Class<?>[0])).intValue() == x &&
                ((Number)invoke(point, "getZ", new Class<?>[0])).intValue() == z &&
                ((Collection<?>)invoke(point, "getDimensions", new Class<?>[0])).contains(dimension) &&
                (Boolean)invoke(point, "isTeleportReady", new Class<?>[0])) return point;
        }
        return null;
    }

    static void notify(Object player, String message) throws ReflectiveOperationException {
        Class<?> component = Class.forName("net.minecraft.util.IChatComponent");
        Object text = Class.forName("net.minecraft.util.ChatComponentText").getConstructor(String.class).newInstance(message);
        invoke(player, new String[]{"func_145747_a", "addChatMessage"}, new Class<?>[]{component}, text);
    }

    static Object field(Object object, String... names) throws ReflectiveOperationException {
        for (Class<?> c = object.getClass(); c != null; c = c.getSuperclass()) {
            for (String name : names) {
                try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f.get(object); }
                catch (NoSuchFieldException ignored) { }
            }
        }
        throw new NoSuchFieldException(object.getClass().getName() + ": " + names[0]);
    }

    static Object invoke(Object object, String name, Class<?>[] types, Object... args) throws ReflectiveOperationException {
        return invoke(object, new String[]{name}, types, args);
    }

    static Object invoke(Object object, String[] names, Class<?>[] types, Object... args) throws ReflectiveOperationException {
        for (Class<?> c = object.getClass(); c != null; c = c.getSuperclass()) {
            for (String name : names) {
                try { Method m = c.getDeclaredMethod(name, types); m.setAccessible(true); return m.invoke(object, args); }
                catch (NoSuchMethodException ignored) { }
            }
        }
        throw new NoSuchMethodException(object.getClass().getName() + ": " + names[0]);
    }
}
