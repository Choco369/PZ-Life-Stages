package pz.playablechildren;

import me.zed_0xff.zombie_buddy.Exposer;

@Exposer.LuaClass(name="PlayableChildren.ExactZombieRooms")
public final class ExactZombieRoomsApi {
    private ExactZombieRoomsApi() {}
    public static boolean isAvailable() { return ExactZombieRooms.isAvailable(); }
}
