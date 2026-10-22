package pz.playablechildren;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Stable life-stage identity for visuals copied into an AnimatedModel. */
public final class AnimatedModelStages {
    private static final Map<Object, LifeStage> STAGES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private AnimatedModelStages() {}

    public static void remember(Object owner, LifeStage stage) {
        if (owner == null) return;
        if (stage == null || stage == LifeStage.ADULT) STAGES.remove(owner);
        else STAGES.put(owner, stage);
    }

    public static LifeStage stageOf(Object owner) {
        if (owner == null) return LifeStage.ADULT;
        LifeStage stage = STAGES.get(owner);
        return stage == null ? LifeStage.ADULT : stage;
    }

    public static void forget(Object owner) {
        if (owner != null) STAGES.remove(owner);
    }
}
