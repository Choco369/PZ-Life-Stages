package pz.playablechildren;

import zombie.characters.IsoZombie;
import zombie.iso.IsoObject;

public final class SavedZombieStages {
    private static final String KEY = "PZLifeStagesReanimatedStage";

    private SavedZombieStages() {}

    public static LifeStage find(IsoObject object) {
        if (object == null || !object.hasModData()) return null;
        Object value = object.getModData().rawget(KEY);
        if (!(value instanceof String name)) return null;
        return switch (name) {
            case "CHILD" -> LifeStage.CHILD;
            case "TEEN" -> LifeStage.TEEN;
            case "ELDER" -> LifeStage.ELDER;
            case "ADULT" -> LifeStage.ADULT;
            default -> null;
        };
    }

    public static void remember(IsoZombie zombie, LifeStage stage) {
        if (zombie == null || stage == null) return;
        if (stage != LifeStage.ADULT || find(zombie) != null
                || StageBits.stageOf(zombie.getPersistentOutfitID()) != LifeStage.ADULT)
            zombie.getModData().rawset(KEY, stage.name());
    }

    public static void restore(IsoZombie zombie) {
        LifeStage stage = find(zombie);
        if (zombie != null && stage != null)
            zombie.setPersistentOutfitID(StageBits.withStage(zombie.getPersistentOutfitID(), stage), zombie.isPersistentOutfitInit());
    }
}
