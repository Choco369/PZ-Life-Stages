package pz.playablechildren;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.iso.objects.IsoDeadBody;
import zombie.scripting.objects.CharacterProfession;

/** Stage identity helpers. Child behavior remains distinct from adolescent behavior. */
public final class ChildIdentity {
    private ChildIdentity() {}

    public static boolean isPlayableChild(SurvivorDesc desc) {
        if (desc == null) return false;
        CharacterProfession p = desc.getCharacterProfession();
        return p != null && ChildPolicy.isChildProfessionName(p.toString());
    }
    public static boolean isPlayableTeen(SurvivorDesc desc) {
        if (desc == null) return false;
        CharacterProfession p = desc.getCharacterProfession();
        return p != null && TeenPolicy.isTeenProfessionName(p.toString());
    }
    public static boolean isChild(SurvivorDesc desc) {
        if (desc == null) return false;
        LifeStage dynamic = PlayerLifeStages.find(desc);
        return dynamic != null ? dynamic == LifeStage.CHILD : isPlayableChild(desc);
    }
    public static boolean isTeen(SurvivorDesc desc) {
        if (desc == null) return false;
        LifeStage dynamic = PlayerLifeStages.find(desc);
        return dynamic != null ? dynamic == LifeStage.TEEN : isPlayableTeen(desc);
    }

    public static boolean isPlayableChild(IsoGameCharacter c) {
        return c instanceof IsoPlayer && isPlayableChild(c.getDescriptor());
    }
    public static boolean isPlayableTeen(IsoGameCharacter c) {
        return c instanceof IsoPlayer && isPlayableTeen(c.getDescriptor());
    }
    public static boolean isZombieChild(IsoGameCharacter c) {
        return c instanceof IsoZombie && ChildPolicy.hasChildOutfitBit(c.getPersistentOutfitID());
    }
    public static boolean isZombieTeen(IsoGameCharacter c) {
        return c instanceof IsoZombie && TeenPolicy.hasTeenOutfitBit(c.getPersistentOutfitID());
    }
    public static boolean isChild(IsoGameCharacter c) {
        if (c == null) return false;
        LifeStage dynamic = PlayerLifeStages.find(c);
        return dynamic != null ? dynamic == LifeStage.CHILD : isPlayableChild(c) || isZombieChild(c);
    }
    public static boolean isTeen(IsoGameCharacter c) {
        if (c == null) return false;
        LifeStage dynamic = PlayerLifeStages.find(c);
        return dynamic != null ? dynamic == LifeStage.TEEN : isPlayableTeen(c) || isZombieTeen(c);
    }

    public static boolean isZombieChild(IsoDeadBody body) {
        return body != null && LifeStageResolver.stageOf(body) == LifeStage.CHILD;
    }
    public static boolean isZombieTeen(IsoDeadBody body) {
        return body != null && LifeStageResolver.stageOf(body) == LifeStage.TEEN;
    }
    public static boolean isChild(IsoDeadBody body) {
        return body != null && LifeStageResolver.stageOf(body) == LifeStage.CHILD;
    }
    public static boolean isTeen(IsoDeadBody body) {
        return body != null && LifeStageResolver.stageOf(body) == LifeStage.TEEN;
    }

    public static Object ownerOf(HumanVisual visual) {
        return visual == null ? null : ReflectionAccess.tryGet(visual, "owner", null);
    }
    public static boolean isChildVisual(HumanVisual visual) {
        Object owner = ownerOf(visual);
        if (owner instanceof SurvivorDesc) return isChild((SurvivorDesc)owner);
        if (owner instanceof IsoGameCharacter) return isChild((IsoGameCharacter)owner);
        if (owner instanceof IsoDeadBody) return isChild((IsoDeadBody)owner);
        return owner instanceof AnimatedModel && ChildAnimatedModels.isChild((AnimatedModel)owner);
    }
    public static boolean isTeenVisual(HumanVisual visual) {
        Object owner = ownerOf(visual);
        if (owner instanceof SurvivorDesc) return isTeen((SurvivorDesc)owner);
        if (owner instanceof IsoGameCharacter) return isTeen((IsoGameCharacter)owner);
        if (owner instanceof IsoDeadBody) return isTeen((IsoDeadBody)owner);
        return false;
    }
    public static boolean isPlayableChildVisual(HumanVisual visual) {
        Object owner = ownerOf(visual);
        if (owner instanceof SurvivorDesc) return isPlayableChild((SurvivorDesc)owner);
        if (owner instanceof IsoGameCharacter) return isPlayableChild((IsoGameCharacter)owner);
        if (owner instanceof IsoDeadBody) return isPlayableChild(((IsoDeadBody)owner).getDescriptor());
        return owner instanceof AnimatedModel && ChildAnimatedModels.isPlayableChild((AnimatedModel)owner);
    }
}
