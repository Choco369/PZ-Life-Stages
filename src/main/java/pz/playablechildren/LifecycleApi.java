package pz.playablechildren;

import me.zed_0xff.zombie_buddy.Exposer;
import zombie.Lua.LuaEventManager;

/** Reliable Lua-class bridge for lifecycle operations. */
@Exposer.LuaClass(name = "PlayableChildren.Lifecycle")
public final class LifecycleApi {
    private LifecycleApi() {}

    public static boolean ensureWakeEvent() {
        LuaEventManager.AddEvent("OnPlayableChildrenWakeUp");
        return true;
    }

    public static boolean setLifeStage(Object value, String stageName) {
        return LuaApi.setPlayableChildrenLifeStage(value, stageName);
    }

    public static boolean setAgeContext(Object value, String stageName, double progress) {
        return AgeGameplayApi.setContext(value, stageName, progress);
    }

    public static float profileFactor(Object value, String effectName) {
        return LuaApi.getPlayableChildrenAgeFactor(value, effectName);
    }

    public static boolean killNaturally(Object value) {
        return LuaApi.killPlayableChildrenNaturally(value);
    }

    public static boolean ping() {
        return true;
    }
}
