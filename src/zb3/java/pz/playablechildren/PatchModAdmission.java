package pz.playablechildren;

import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.*;
import zombie.gameStates.GameLoadingState;
import zombie.gameStates.GameState;
import zombie.gameStates.GameStateMachine.StateAction;

public final class PatchModAdmission {
    private PatchModAdmission() {}

    @Patch(className="zombie.gameStates.GameLoadingState", methodName="enter", strictMatch=false)
    public static final class Enter {
        @OnEnter(skipOn=true) public static boolean enter(@This GameLoadingState state) {
            return ModAdmission.begin(state);
        }
    }

    @Patch(className="zombie.gameStates.GameLoadingState", methodName="render", strictMatch=false)
    public static final class Render {
        @OnEnter(skipOn=true) public static boolean enter(@This GameLoadingState state) {
            if (!ModAdmission.blocked(state)) return false;
            ModAdmission.render(state);
            return true;
        }
    }

    @Patch(className="zombie.gameStates.GameLoadingState", methodName="update", strictMatch=false)
    public static final class Update {
        @OnEnter(skipOn=true) public static boolean enter(@This GameLoadingState state) {
            return ModAdmission.blocked(state);
        }
        @OnExit public static void exit(@This GameLoadingState state, @Return(readOnly=false) StateAction result) {
            if (ModAdmission.blocked(state)) result = ModAdmission.update(state);
        }
    }

    @Patch(className="zombie.gameStates.GameLoadingState", methodName="exit", strictMatch=false)
    public static final class Exit {
        @OnEnter(skipOn=true) public static boolean enter(@This GameLoadingState state) {
            if (!ModAdmission.blocked(state)) return false;
            ModAdmission.cancel(state);
            return true;
        }
    }

    @Patch(className="zombie.gameStates.GameLoadingState", methodName="redirectState", strictMatch=false)
    public static final class Redirect {
        @OnEnter(skipOn=true) public static boolean enter(@This GameLoadingState state) {
            return ModAdmission.blocked(state);
        }
        @OnExit public static void exit(@This GameLoadingState state, @Return(readOnly=false) GameState result) {
            if (ModAdmission.blocked(state)) result = ModAdmission.backToMenu(state);
        }
    }

    @Patch(className="zombie.iso.IsoWorld", methodName="init", strictMatch=false)
    public static final class World {
        @OnEnter public static void enter() { ModAdmission.requireAllowed(); }
    }
}
