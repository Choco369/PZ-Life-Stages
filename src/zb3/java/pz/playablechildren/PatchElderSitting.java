package pz.playablechildren;

import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnEnter;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.This;
import zombie.characters.CharacterStat;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoObject;

/** Adds a percentage of vanilla's actual sitting recovery, never a flat stamina grant. */
public final class PatchElderSitting {
    private static final ThreadLocal<Float> BEFORE = new ThreadLocal<>();
    private PatchElderSitting() {}

    public static void before(IsoPlayer player) {
        BEFORE.set(player.getStats().get(CharacterStat.ENDURANCE));
    }

    public static void after(IsoPlayer player) {
        Float before = BEFORE.get();
        BEFORE.remove();
        if (before == null || PlayerLifeStages.stageOf(player) != LifeStage.ELDER
            || !player.isSittingOnFurniture()) return;
        IsoObject object = player.getSitOnFurnitureObject();
        String spriteName = object != null && object.getSprite() != null
            ? object.getSprite().getName() : null;
        float multiplier = ElderSittingPolicy.multiplier(LifeStage.ELDER, true, spriteName);
        float current = player.getStats().get(CharacterStat.ENDURANCE);
        float extra = ElderSittingPolicy.extraRecovery(before, current, multiplier);
        if (extra > 0.0f) player.getStats().add(CharacterStat.ENDURANCE, extra);
    }

    @Patch(className = "zombie.characters.IsoPlayer",
           methodName = "updateEnduranceWhileSitting", strictMatch = false)
    public static final class SittingRecovery {
        @OnEnter
        public static void enter(@This IsoPlayer player) {
            PatchElderSitting.before(player);
        }

        @OnExit
        public static void exit(@This IsoPlayer player) {
            PatchElderSitting.after(player);
        }
    }
}
