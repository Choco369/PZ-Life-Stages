package pz.playablechildren;

import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.Argument;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.Return;
import me.zed_0xff.zombie_buddy.annotations.Patch.This;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.visual.HumanVisual;

public final class PatchBodyModel {
    private PatchBodyModel() {}

    public static Model bodyFor(Model original, IsoGameCharacter character, ModelManager manager) {
        if (character == null) return original;
        LifeStage stage = LifeStageResolver.stageOf(character);
        Model replacement = null;
        if (stage == LifeStage.TEEN) replacement = TeenModel.resolve(manager, character.isFemale());
        else if (stage == LifeStage.CHILD) replacement = ChildModel.resolve(manager, character.isFemale());
        return replacement != null ? replacement : original;
    }

    public static Model visualBodyFor(Model original, HumanVisual visual) {
        if (visual == null) return original;
        LifeStage stage = LifeStageResolver.stageOf(visual);
        Model replacement = null;
        if (stage == LifeStage.TEEN) replacement = TeenModel.resolve(ModelManager.instance, visual.isFemale());
        else if (stage == LifeStage.CHILD) replacement = ChildModel.resolve(ModelManager.instance, visual.isFemale());
        return replacement != null ? replacement : original;
    }

    @Patch(className = "zombie.core.skinnedmodel.ModelManager", methodName = "create")
    public static final class Create {
        @OnExit
        public static void exit(@This ModelManager manager) {
            ChildModel.preload(manager);
            TeenModel.preload(manager);
        }
    }

    @Patch(className = "zombie.core.skinnedmodel.ModelManager", methodName = "getBodyModel")
    public static final class GetBodyModel {
        @OnExit
        public static void exit(@This ModelManager manager,
                                @Argument(0) IsoGameCharacter character,
                                @Return(readOnly = false) Model result) {
            result = PatchBodyModel.bodyFor(result, character, manager);
        }
    }

    @Patch(className = "zombie.core.skinnedmodel.visual.HumanVisual", methodName = "getModel")
    public static final class GetVisualModel {
        @OnExit
        public static void exit(@This HumanVisual visual,
                                @Return(readOnly = false) Model result) {
            result = PatchBodyModel.visualBodyFor(result, visual);
        }
    }
}
