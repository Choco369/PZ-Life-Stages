package pz.playablechildren;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import zombie.characters.IsoGameCharacter;
import zombie.characters.SurvivorDesc;

/** Runtime life-stage identity for players whose profession intentionally remains unchanged. */
public final class PlayerLifeStages {
    private static final Map<IsoGameCharacter, LifeStage> CHARACTERS =
        Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<SurvivorDesc, LifeStage> DESCRIPTORS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private PlayerLifeStages() {}

    public static void remember(IsoGameCharacter character, LifeStage stage) {
        if (character == null || stage == null) return;
        CHARACTERS.put(character, stage);
        remember(character.getDescriptor(), stage);
    }

    public static void remember(SurvivorDesc descriptor, LifeStage stage) {
        if (descriptor == null || stage == null) return;
        DESCRIPTORS.put(descriptor, stage);
    }

    public static LifeStage find(IsoGameCharacter character) {
        return character == null ? null : CHARACTERS.get(character);
    }

    public static LifeStage find(SurvivorDesc descriptor) {
        return descriptor == null ? null : DESCRIPTORS.get(descriptor);
    }

    public static LifeStage stageOf(IsoGameCharacter character) {
        LifeStage stage = find(character);
        return stage == null ? LifeStage.ADULT : stage;
    }

    public static LifeStage stageOf(SurvivorDesc descriptor) {
        LifeStage stage = find(descriptor);
        return stage == null ? LifeStage.ADULT : stage;
    }

    public static void forget(IsoGameCharacter character) {
        if (character == null) return;
        CHARACTERS.remove(character);
        forget(character.getDescriptor());
    }

    public static void forget(SurvivorDesc descriptor) {
        if (descriptor != null) DESCRIPTORS.remove(descriptor);
    }
}

