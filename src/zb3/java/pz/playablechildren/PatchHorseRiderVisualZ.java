package pz.playablechildren;

import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.Argument;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.This;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.ModelCamera;
import zombie.core.skinnedmodel.ModelCameraRenderData;
import zombie.core.skinnedmodel.ModelManager.ModelSlot;

/**
 * Visual-only mounted CHILD/TEEN correction.
 *
 * Production baselines are baked into Java so positioning is correct from game
 * start, before the DEV right-click tuner is ever opened. CHILD uses Z +0.0700;
 * FIXED84 adds the accepted TEEN baseline Z +0.0200. Forward/side/screen offsets
 * remain zero for both stages.
 */
public final class PatchHorseRiderVisualZ {
   public static final float CHILD_MOUNT_VISUAL_Z_OFFSET = 0.07F;
   public static final float TEEN_MOUNT_VISUAL_Z_OFFSET = 0.02F;

   /** Public because ZombieBuddy inlines advice bytecode into ModelCameraRenderData. */
   public static void applyMountedChildVisualOffset(ModelCameraRenderData renderData, ModelSlot modelSlot) {
      IsoGameCharacter character = modelSlot == null ? null : modelSlot.character;
      if (character == null || !character.getVariableBoolean("HorseRiding")) return;
      LifeStage stage = LifeStageResolver.stageOf(character);
      boolean isChild = stage == LifeStage.CHILD;
      boolean isTeen = stage == LifeStage.TEEN;
      if (!isChild && !isTeen) return;

      Float z = (Float)ReflectionAccess.tryGet(renderData, "z", Float.NaN);
      if (z == null || z.isNaN()) return;

      ReflectionAccess.trySet(renderData, "z", z + (isChild ? CHILD_MOUNT_VISUAL_Z_OFFSET : TEEN_MOUNT_VISUAL_Z_OFFSET));

   }

   @Patch(className = "zombie.core.skinnedmodel.ModelCameraRenderData", methodName = "init", strictMatch = true)
   public static final class ModelCameraInit {
      private ModelCameraInit() {}

      @OnExit
      public static void exit(@This ModelCameraRenderData renderData, @Argument(0) ModelCamera camera, @Argument(1) ModelSlot modelSlot) {
         PatchHorseRiderVisualZ.applyMountedChildVisualOffset(renderData, modelSlot);
      }
   }
}
