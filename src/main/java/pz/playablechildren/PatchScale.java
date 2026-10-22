package pz.playablechildren;

import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.Argument;
import me.zed_0xff.zombie_buddy.Patch.OnEnter;
import me.zed_0xff.zombie_buddy.Patch.OnExit;
import me.zed_0xff.zombie_buddy.Patch.This;
import zombie.characters.IsoGameCharacter;
import zombie.characters.SurvivorDesc;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.iso.Vector3;
import zombie.ui.UI3DModel;

public final class PatchScale {
    /** FIXED82 production visual scale used only while a CHILD is mounted.
     *  Baked from the in-game tuner export so it is active before the DEV menu opens. */
    public static final float MOUNTED_CHILD_RENDER_SCALE = 0.833F;
    /** FIXED84 production mounted TEEN scale baked from the accepted in-game export.
     *  This only applies while HorseRiding is true; normal TEEN remains 0.89. */
    public static final float MOUNTED_TEEN_RENDER_SCALE = 0.94F;
    /** FIXED79: ZombieBuddy advice is transplanted into ModelSlotRenderData.
     *  Fields referenced directly by transplanted advice must therefore be public,
     *  otherwise the target class triggers IllegalAccessError at render time. */

    private PatchScale() {}

    public static float scaleFor(IsoGameCharacter character) {
        return LifeStageResolver.scaleFor(LifeStageResolver.stageOf(character));
    }

    public static float scaleFor(SurvivorDesc desc) {
        return LifeStageResolver.scaleFor(LifeStageResolver.stageOf(desc));
    }

    @Patch(className = "zombie.core.skinnedmodel.model.ModelSlotRenderData", methodName = "init")
    public static final class ModelSlotInit {
        @OnExit
        public static void exit(@This ModelSlotRenderData data) {
            float scale = scaleFor(data.character);

            // FIXED82: use the baked mounted CHILD scale only while riding a horse.
            // This is deliberately render-only: the normal CHILD scale remains 0.72
            // everywhere else, and VectorToWorldCoords/preview/lifecycle are untouched.
            // As soon as HorseRiding becomes false, this path falls back to 0.72.
            if (data.character != null && data.character.getVariableBoolean("HorseRiding")) {
                LifeStage stage = LifeStageResolver.stageOf(data.character);
                if (stage == LifeStage.CHILD) {
                    scale = MOUNTED_CHILD_RENDER_SCALE;
                } else if (stage == LifeStage.TEEN) {
                    scale = MOUNTED_TEEN_RENDER_SCALE;
                }
            }

            if (scale != 1.0F) data.finalScale = scale;
        }
    }

    @Patch(className = "zombie.core.skinnedmodel.advancedanimation.AnimatedModel", methodName = "setModelData")
    public static final class AnimatedModelData {
        @OnEnter
        public static void enter(@This AnimatedModel model, @Argument(0) BaseVisual visual) {
            ChildAnimatedModels.updateFrom(model, visual);
        }

        @OnExit
        public static void exit(@This AnimatedModel model, @Argument(0) BaseVisual visual) {
            model.setScale(LifeStageResolver.scaleFor(AnimatedModelStages.stageOf(model)));
        }
    }

    @Patch(className = "zombie.ui.UI3DModel", methodName = "setSurvivorDesc")
    public static final class PreviewDescriptor {
        @OnExit
        public static void exit(@This UI3DModel ui, @Argument(0) SurvivorDesc desc) {
            AnimatedModel model = (AnimatedModel) ReflectionAccess.tryGet(ui, "animatedModel", (AnimatedModel) null);
            if (model != null) {
                LifeStage stage = LifeStageResolver.stageOf(desc);
                AnimatedModelStages.remember(model, stage);
                model.setScale(LifeStageResolver.scaleFor(stage));
            }
        }
    }

    @Patch(className = "zombie.core.skinnedmodel.model.Model", methodName = "vectorToWorldCoords")
    public static final class VectorToWorldCoords {
        @OnEnter
        public static void enter(@Argument(0) IsoGameCharacter character, @Argument(1) Vector3 vector) {
            float scale = scaleFor(character);
            if (scale == 1.0F) return;
            vector.x *= scale;
            vector.y *= scale;
            vector.z *= scale;
        }
    }
}
