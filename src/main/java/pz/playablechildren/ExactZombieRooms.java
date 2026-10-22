package pz.playablechildren;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.characters.ZombiesZoneDefinition;
import zombie.iso.IsoWorld;
import zombie.PersistentOutfits;

/** Only our explicitly marked entries have exact-room eligibility. The native
 * parser/caches, unmarked rules, selector probabilities and RNG stay untouched. */
public final class ExactZombieRooms {
    private static final String INVALID = "\u0000";
    private static final Map<Object,String> ROOMS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Frame> CONTEXT = new ThreadLocal<>();
    private record Frame(String room, Frame previous) {}
    private static final ThreadLocal<RegistryFrame> REGISTRY = new ThreadLocal<>();
    private record Registration(PersistentOutfits target, boolean seed, PersistentOutfits.IOutfitter dresser) {}
    private record RegistryFrame(ArrayList<String> names, Map<String,Object> legacy,
            Map<String,Object> nativeMap, Map<String,Registration> queued, RegistryFrame previous) {}
    private ExactZombieRooms() {}

    public static void capture(Object outfit, KahluaTable source) {
        if (outfit == null) return;
        Object mark = source == null ? null : source.rawget("PZLS_exactRoom");
        if (mark == null) ROOMS.remove(outfit);
        else ROOMS.put(outfit, mark instanceof String room && !room.isEmpty() ? room : INVALID);
    }

    public static void enterRoom(String room) { CONTEXT.set(new Frame(room, CONTEXT.get())); }
    public static void enterPoint(int x, int y, int z) {
        String name = null;
        try {
            var world = IsoWorld.instance;
            var square = world == null || world.currentCell == null ? null : world.currentCell.getGridSquare(x,y,z);
            var room = square == null ? null : square.getRoom();
            if (room != null) name = room.getName();
        } catch (RuntimeException unavailable) {
            // Establish an unknown context; the original method still runs and
            // produces its own error/return. Do not invent a room or retry it.
        }
        enterRoom(name);
    }
    public static void exitScope() {
        Frame frame = CONTEXT.get();
        if (frame == null || frame.previous == null) CONTEXT.remove();
        else CONTEXT.set(frame.previous);
    }

    public static ArrayList<?> filter(ArrayList<?> pool) {
        if (pool == null) return null;
        Frame context = CONTEXT.get();
        String current = context == null ? null : context.room;
        ArrayList<Object> filtered = null;
        for (int i=0; i<pool.size(); i++) {
            Object outfit = pool.get(i);
            String required = ROOMS.get(outfit);
            boolean allowed = required == null || (!INVALID.equals(required) && required.equals(current));
            if (!allowed && filtered == null) filtered = new ArrayList<>(pool.subList(0,i));
            if (allowed && filtered != null) filtered.add(outfit);
        }
        return filtered == null ? pool : filtered;
    }

    /** Selection-only rows must not insert/duplicate native persistent outfit
     * registrations: saved IDs encode the registry index, not the outfit name.
     * Parse with native code, then retain exactly the unmarked registration
     * sequence (including legacy duplicates and its last-wins metadata).
     * Selector lists remain intact. No callbacks or RNG are replaced. */
    @SuppressWarnings("unchecked")
    public static void beginRegistry() throws ReflectiveOperationException {
        ReflectionAccess.method(ZombiesZoneDefinition.class,"checkDirty").invoke(null);
        var zones = (ArrayList<?>) ReflectionAccess.field(ZombiesZoneDefinition.class,"s_zoneList").get(null);
        var nativeMap = (Map<String,Object>) ReflectionAccess.field(ZombiesZoneDefinition.class,"s_customOutfitMap").get(null);
        var names = new ArrayList<String>();
        var legacy = new LinkedHashMap<String,Object>();
        boolean marked = false;
        for (Object zone : zones) {
            for (Object outfit : (ArrayList<?>) ReflectionAccess.field(zone.getClass(),"outfits").get(zone)) {
                if (ROOMS.containsKey(outfit)) { marked = true; continue; }
                String name = (String) ReflectionAccess.field(outfit.getClass(),"customName").get(outfit);
                names.add(name);
                legacy.put(name,outfit);
            }
        }
        // Without our rows the native body registers directly, as before.
        REGISTRY.set(new RegistryFrame(marked ? names : null,legacy,nativeMap,new LinkedHashMap<>(),REGISTRY.get()));
    }

    public static boolean queueRegistration(PersistentOutfits target, String name,
            boolean seed, PersistentOutfits.IOutfitter dresser) {
        RegistryFrame frame = REGISTRY.get();
        if (frame == null || frame.names == null) return false;
        frame.queued.putIfAbsent(name,new Registration(target,seed,dresser));
        return true;
    }

    public static void endRegistry(Throwable failure) {
        RegistryFrame frame = REGISTRY.get();
        if (frame == null) return;
        // Calls below must run native registerOutfitter, never re-queue. Even
        // nested scopes or an exceptional native body restore the outer frame.
        REGISTRY.remove();
        try {
            if (failure == null && frame.names != null) {
                for (String name : frame.names) {
                    Registration call = frame.queued.get(name);
                    if (call == null) throw new IllegalStateException("Missing native outfit registration: " + name);
                    call.target.registerOutfitter(name,call.seed,call.dresser);
                }
                frame.nativeMap.clear();
                frame.nativeMap.putAll(frame.legacy);
            }
        } finally {
            if (frame.previous == null) REGISTRY.remove();
            else REGISTRY.set(frame.previous);
        }
    }

    public static boolean isAvailable() {
        try {
            Class<?> type = ZombiesZoneDefinition.class;
            type.getDeclaredMethod("initOutfit", KahluaTableImpl.class);
            type.getDeclaredMethod("getRandomDefaultOutfit", boolean.class,String.class);
            type.getDeclaredMethod("pickDefinition", int.class,int.class,int.class,boolean.class,boolean.class);
            type.getDeclaredMethod("getRandomOutfitInSetList", ArrayList.class,boolean.class);
            type.getDeclaredMethod("registerCustomOutfits");
            if (ReflectionAccess.method(type,"checkDirty") == null
                    || ReflectionAccess.field(type,"s_zoneList") == null
                    || ReflectionAccess.field(type,"s_customOutfitMap") == null) return false;
            Class<?> zone = Class.forName(type.getName()+"$ZZDZone");
            Class<?> outfit = Class.forName(type.getName()+"$ZZDOutfit");
            if (ReflectionAccess.field(zone,"outfits") == null
                    || ReflectionAccess.field(outfit,"customName") == null) return false;
            PersistentOutfits.class.getDeclaredMethod("registerOutfitter",String.class,boolean.class,PersistentOutfits.IOutfitter.class);
            return true;
        } catch (ReflectiveOperationException | LinkageError incompatible) { return false; }
    }
}
