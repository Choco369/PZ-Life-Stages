package pz.playablechildren;

import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.Argument;
import me.zed_0xff.zombie_buddy.Patch.OnEnter;
import me.zed_0xff.zombie_buddy.Patch.OnExit;
import me.zed_0xff.zombie_buddy.Patch.This;
import zombie.characters.IsoGameCharacter;
import zombie.core.physics.BallisticsTarget;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import zombie.core.skinnedmodel.model.SkeletonBone;

/** Own vanilla-boundary implementation, DEV118R. Halflings is acknowledged as
 * inspiration for size-aware targeting and child attachment-outline handling.
 * This does not replace the native target lifecycle or track render scopes.
 */
public final class PatchLifeStageTargets {
    private PatchLifeStageTargets() {}

    // Public bridges are required: ZombieBuddy inserts advice into game classes.
    public static void adjustPreparedSkeleton(BallisticsTarget target) {
        Object owner = ReflectionAccess.tryGet(target, "isoGameCharacter", null);
        if (!(owner instanceof IsoGameCharacter character)) return;
        TargetGeometry.adjustPackedPositions(LifeStageResolver.stageOf(character),
            BallisticsTarget.boneTransformData, SkeletonBone.count());
    }

    public static boolean omitAttachment(Model model, ModelSlotRenderData slot) {
        if (model == null || slot == null || !model.isStatic || model.effect == null) return false;
        return TargetGeometry.omitOutlineAttachment(LifeStageResolver.stageOf(slot.character),
            model.isStatic, model.effect.getName());
    }

    @Patch(className = "zombie.core.physics.BallisticsTarget", methodName = "getBoneTransforms")
    public static final class PreparedSkeleton {
        @OnExit
        public static void exit(@This BallisticsTarget target) {
            // Native packing ran successfully and overwrote the used records.
            // Its subsequent native submission handles add AND update paths.
            PatchLifeStageTargets.adjustPreparedSkeleton(target);
        }
    }

    @Patch(className = "zombie.core.skinnedmodel.model.Model", methodName = "DrawChar")
    public static final class AttachmentDraw {
        @OnEnter(skipOn = true)
        public static boolean enter(@This Model model, @Argument(0) ModelSlotRenderData slot) {
            // Identify the actual native outline shader, not a mutable global
            // "inside outline" flag. Normal models/textures remain untouched.
            return PatchLifeStageTargets.omitAttachment(model, slot);
        }
    }
}
