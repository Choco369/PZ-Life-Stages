package pz.playablechildren;

import java.util.Locale;
import se.krka.kahlua.vm.KahluaTable;
import zombie.characters.IsoGameCharacter;
import zombie.characters.SurvivorDesc;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.iso.objects.IsoDeadBody;
import zombie.scripting.objects.CharacterProfession;

public final class LifeStageResolver {
    private LifeStageResolver() {}

    public static LifeStage stageOf(SurvivorDesc desc) {
        if (desc == null) return LifeStage.ADULT;
        LifeStage dynamic = PlayerLifeStages.find(desc);
        if (dynamic != null) return dynamic;
        CharacterProfession profession = desc.getCharacterProfession();
        return stageForProfessionName(profession == null ? null : profession.toString());
    }
    public static LifeStage stageForProfessionName(String value) {
        if (TeenPolicy.isTeenProfessionName(value)) return LifeStage.TEEN;
        if (ChildPolicy.isChildProfessionName(value)) return LifeStage.CHILD;
        if (value != null && value.equalsIgnoreCase("PlayableChildren:Elder")) return LifeStage.ELDER;
        return LifeStage.ADULT;
    }
    public static LifeStage stageOf(IsoGameCharacter character) {
        if (character == null) return LifeStage.ADULT;
        LifeStage dynamic = PlayerLifeStages.find(character);
        if (dynamic != null) return dynamic;
        if (character instanceof zombie.characters.IsoZombie) {
            LifeStage saved = SavedZombieStages.find(character);
            if (saved != null) return saved;
            LifeStage marked = PatchChildReanimation.stageFromBits(character.getPersistentOutfitID());
            if (marked != LifeStage.ADULT) return marked;
        }
        return stageOf(character.getDescriptor());
    }
    public static LifeStage stageOf(IsoDeadBody body) {
        if (body == null) return LifeStage.ADULT;
        LifeStage lifecycle = stageFromLifecycleModData(body);
        if (lifecycle != null) return lifecycle;
        LifeStage saved = SavedZombieStages.find(body);
        if (saved != null) return saved;
        Integer markedId = (Integer)ReflectionAccess.tryGet(body, "persistentOutfitId", Integer.valueOf(0));
        LifeStage marked = PatchChildReanimation.stageFromBits(markedId == null ? 0 : markedId.intValue());
        if (marked != LifeStage.ADULT) return marked;
        return stageOf(body.getDescriptor());
    }

    private static LifeStage stageFromLifecycleModData(IsoDeadBody body) {
        try {
            Object raw = body.getModData().rawget("PlayableChildrenLifecycle");
            if (!(raw instanceof KahluaTable lifecycle)) return null;
            if (!Boolean.TRUE.equals(lifecycle.rawget("initialized"))) return null;
            Object stage = lifecycle.rawget("stage");
            if (stage == null) return null;
            return LifeStage.valueOf(String.valueOf(stage).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }
    public static LifeStage stageOf(HumanVisual visual) {
        if (visual == null) return LifeStage.ADULT;
        Object owner = ChildIdentity.ownerOf(visual);
        if (owner instanceof SurvivorDesc) return stageOf((SurvivorDesc)owner);
        if (owner instanceof IsoGameCharacter) return stageOf((IsoGameCharacter)owner);
        if (owner instanceof IsoDeadBody) return stageOf((IsoDeadBody)owner);
        if (owner instanceof AnimatedModel) {
            LifeStage cached = AnimatedModelStages.stageOf(owner);
            if (cached != LifeStage.ADULT) return cached;
        }
        if (ChildIdentity.isTeenVisual(visual)) return LifeStage.TEEN;
        return ChildIdentity.isChildVisual(visual) ? LifeStage.CHILD : LifeStage.ADULT;
    }
    public static float scaleFor(LifeStage stage) {
        if (stage == LifeStage.CHILD) return ChildPolicy.CHILD_SCALE;
        if (stage == LifeStage.TEEN) return TeenPolicy.TEEN_SCALE;
        return 1.0F;
    }
}
