package pz.playablechildren;

import java.util.*;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.gameStates.GameLoadingState;
import zombie.gameStates.GameState;
import zombie.gameStates.GameStateMachine.StateAction;
import zombie.gameStates.MainScreenState;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.network.GameClient;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.ui.UIManager;

public final class ModAdmission {
    private static final Set<String> DENIED = Set.of(
        "DrugMod", "ZomboRut", "ZomboWin", "ZomboLewdFramework", "ZombolewdDefeat",
        "ZombolewdDefeatStrip", "WMakeUp", "Malegen", "BiaRPClosetLewd",
        "FantasyWorkshopVS42_4.0", "NakedZombies", "WaifuZombie", "WaifuZombieSound",
        "[MT] Female", "seifukuBodies", "Project_Thiccener", "RealisticNudeBody",
        "3344471334", "triceratops_playermodels", "tricebody", "DetailedPlayerBodyModelReshaped",
        "DpbmFrockinSplendorReshaped", "DpbmPogoClothingReshaped",
        "DpbmSpongieOpenJacketsReshaped", "DpbmVanillaClothesReshaped"
    );
    private static final Map<GameLoadingState, Notice> BLOCKED = Collections.synchronizedMap(new WeakHashMap<>());

    private ModAdmission() {}

    private static final class Notice {
        final List<String> mods;
        boolean ready;
        Notice(List<String> mods) { this.mods = mods; }
    }

    public static List<String> conflicts() {
        List<String> result = new ArrayList<>();
        for (String id : new ArrayList<>(ZomboidFileSystem.instance.getModIDs())) {
            if (DENIED.contains(id) && !result.contains(id)) result.add(id);
        }
        Collections.sort(result);
        return List.copyOf(result);
    }

    public static boolean begin(GameLoadingState state) {
        List<String> conflicts = conflicts();
        if (conflicts.isEmpty()) {
            BLOCKED.remove(state);
            return false;
        }
        BLOCKED.put(state, new Notice(conflicts));
        GameWindow.okToSaveOnExit = false;
        DebugLog.log("[PZ Life Stages] Loading blocked. Disable incompatible mods and restart: " + String.join(", ", conflicts));
        return true;
    }

    public static boolean blocked(GameLoadingState state) {
        return BLOCKED.containsKey(state);
    }

    public static void requireAllowed() {
        List<String> conflicts = conflicts();
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("PZ Life Stages: incompatible mods active. Disable and restart: " + String.join(", ", conflicts));
        }
    }

    public static StateAction update(GameLoadingState state) {
        Notice notice = BLOCKED.get(state);
        if (notice == null) return StateAction.Remain;
        boolean pressed = GameKeyboard.isKeyDownRaw(1) || Mouse.isButtonDown(0)
            || GameWindow.activatedJoyPad != null
                && (GameWindow.activatedJoyPad.isAPressed() || GameWindow.activatedJoyPad.isBPressed());
        if (!pressed) notice.ready = true;
        return pressed && notice.ready ? StateAction.Continue : StateAction.Remain;
    }

    public static void cancel(GameLoadingState state) {
        if (blocked(state) && GameClient.client && GameClient.connection != null) GameClient.instance.Shutdown();
    }

    public static GameState backToMenu(GameLoadingState state) {
        BLOCKED.remove(state);
        return new MainScreenState();
    }

    private static List<String> lines(List<String> mods, int columns) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String mod : mods) {
            if (!current.isEmpty() && current.length() + mod.length() + 2 > columns) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) current.append(", ");
            current.append(mod);
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    public static void render(GameLoadingState state) {
        Notice notice = BLOCKED.get(state);
        if (notice == null) return;
        Core core = Core.getInstance();
        core.StartFrame();
        core.EndFrame();
        boolean previous = UIManager.useUiFbo;
        UIManager.useUiFbo = false;
        try {
            core.StartFrameUI();
            try {
                int width = core.getScreenWidth();
                int height = core.getScreenHeight();
                SpriteRenderer.instance.renderi(null, 0, 0, width, height, 0, 0, 0, 1, null);
                boolean spanish = Translator.getLanguage() != null && "ES".equals(Translator.getLanguage().name());
                UIFont font = UIFont.NewSmall;
                int step = Math.max(20, TextManager.instance.getFontHeight(font) + 8);
                List<String> lines = lines(notice.mods, Math.max(40, Math.min(100, (width - 80) / 10)));
                double y = Math.max(24, (height - (lines.size() + 6) * step) / 2.0);
                TextManager.instance.DrawStringCentre(UIFont.NewLarge, width / 2.0, y,
                    spanish ? "PZ Life Stages - Mods incompatibles" : "PZ Life Stages - Incompatible mods", 1, .7, .3, 1);
                y += step * 2;
                for (String line : lines) {
                    TextManager.instance.DrawStringCentre(font, width / 2.0, y, line, 1, 1, 1, 1);
                    y += step;
                }
                y += step;
                TextManager.instance.DrawStringCentre(font, width / 2.0, y,
                    spanish ? "Desactiva los mods indicados y reinicia el juego." : "Disable the listed mods and restart the game.", 1, 1, 1, 1);
                TextManager.instance.DrawStringCentre(font, width / 2.0, y + step,
                    spanish ? "ESC / clic / mando: volver al menu. La partida no se ha cargado." : "ESC / click / controller: return to menu. The world has not been loaded.", 1, 1, 1, 1);
            } finally {
                core.EndFrameUI();
            }
        } finally {
            UIManager.useUiFbo = previous;
        }
    }
}
