package pz.playablechildren;

import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.Argument;
import me.zed_0xff.zombie_buddy.Patch.Return;
import me.zed_0xff.zombie_buddy.Patch.OnEnter;
import me.zed_0xff.zombie_buddy.Patch.OnExit;
import me.zed_0xff.zombie_buddy.Patch.This;
import me.zed_0xff.zombie_buddy.Patch.Thrown;
import zombie.PersistentOutfits;

public final class PatchExactZombieRooms {
    private PatchExactZombieRooms() {}

    @Patch(className="zombie.characters.ZombiesZoneDefinition", methodName="initOutfit", strictMatch=true)
    public static final class Capture {
        @OnExit public static void exit(@Argument(0) KahluaTable table, @Return Object outfit) {
            ExactZombieRooms.capture(outfit,table);
        }
    }
    @Patch(className="zombie.characters.ZombiesZoneDefinition", methodName="getRandomDefaultOutfit", strictMatch=true)
    public static final class DefaultScope {
        @OnEnter public static void enter(@Argument(1) String room) { ExactZombieRooms.enterRoom(room); }
        @OnExit(onThrowable=Throwable.class) public static void exit() { ExactZombieRooms.exitScope(); }
    }
    @Patch(className="zombie.characters.ZombiesZoneDefinition", methodName="pickDefinition", strictMatch=true)
    public static final class ZoneScope {
        @OnEnter public static void enter(@Argument(0) int x,@Argument(1) int y,@Argument(2) int z) {
            ExactZombieRooms.enterPoint(x,y,z);
        }
        @OnExit(onThrowable=Throwable.class) public static void exit() { ExactZombieRooms.exitScope(); }
    }
    @Patch(className="zombie.characters.ZombiesZoneDefinition", methodName="getRandomOutfitInSetList", strictMatch=true)
    public static final class Candidates {
        @OnEnter public static void enter(@Argument(value=0,readOnly=false) ArrayList<?> pool) {
            pool = ExactZombieRooms.filter(pool);
        }
    }
    @Patch(className="zombie.characters.ZombiesZoneDefinition", methodName="registerCustomOutfits")
    public static final class RegistryScope {
        @OnEnter public static void enter() throws ReflectiveOperationException { ExactZombieRooms.beginRegistry(); }
        @OnExit(onThrowable=Throwable.class) public static void exit(@Thrown Throwable failure) { ExactZombieRooms.endRegistry(failure); }
    }
    @Patch(className="zombie.PersistentOutfits", methodName="registerOutfitter", strictMatch=true)
    public static final class RegistryCalls {
        @OnEnter(skipOn=true) public static boolean enter(@This PersistentOutfits target,
                @Argument(0) String name,@Argument(1) boolean seed,@Argument(2) PersistentOutfits.IOutfitter dresser) {
            return ExactZombieRooms.queueRegistration(target,name,seed,dresser);
        }
    }
}
