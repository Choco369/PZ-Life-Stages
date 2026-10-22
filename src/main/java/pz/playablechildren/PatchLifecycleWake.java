package pz.playablechildren;

import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.Argument;
import me.zed_0xff.zombie_buddy.Patch.OnExit;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;

/** Emits the lifecycle check only after vanilla has completed wake-up state teardown. */
public final class PatchLifecycleWake {
    private PatchLifecycleWake() {}

    /**
     * Keep the injected advice tiny. ZombieBuddy copies advice bytecode into
     * SleepingEvent, so calls that depend on a compile-time stub must not be
     * embedded there. The helper lives in our own class and calls the exact
     * B42.20.4 one-argument overload: triggerEvent(String, Object).
     */
    public static void fireWakeEvent(IsoGameCharacter character) {
        if (character instanceof IsoPlayer) {
            LuaEventManager.triggerEvent("OnPlayableChildrenWakeUp", (Object)character);
        }
    }

    @Patch(
        className = "zombie.ai.sadisticAIDirector.SleepingEvent",
        methodName = "wakeUp",
        strictMatch = true
    )
    public static final class WakeUpTwoArguments {
        @OnExit
        public static void exit(
            @Argument(0) IsoGameCharacter character,
            @Argument(1) boolean remote
        ) {
            PatchLifecycleWake.fireWakeEvent(character);
        }
    }
}

