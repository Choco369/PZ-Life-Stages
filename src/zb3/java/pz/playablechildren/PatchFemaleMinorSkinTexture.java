package pz.playablechildren;

import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.Return;
import me.zed_0xff.zombie_buddy.annotations.Patch.This;
import zombie.core.skinnedmodel.visual.HumanVisual;

/**
 * Robust female CHILD/TEEN protection for B42.21.
 *
 * Uses mod-owned variants of the vanilla female body skins, so protection is
 * part of the body texture itself and never exists as removable/worn clothing
 * or as a second bodyVisual mesh that can interfere with vanilla trousers.
 */
public final class PatchFemaleMinorSkinTexture {
    public static final String CHILD_PREFIX = "PZLS_ChildFemale_";
    public static final String TEEN_PREFIX = "PZLS_TeenFemale_";

    private PatchFemaleMinorSkinTexture() {}

    public static String protectedTextureFor(HumanVisual visual, String original) {
        if (visual == null || original == null || original.isEmpty()) return original;
        if (!visual.isFemale()) return original;

        LifeStage stage = LifeStageResolver.stageOf(visual);
        if (stage != LifeStage.CHILD && stage != LifeStage.TEEN) return original;

        if (original.startsWith(CHILD_PREFIX) || original.startsWith(TEEN_PREFIX)) return original;

        return switch (original) {
            case "FemaleBody01", "FemaleBody02", "FemaleBody03", "FemaleBody04", "FemaleBody05" ->
                (stage == LifeStage.CHILD ? CHILD_PREFIX : TEEN_PREFIX) + original;
            default -> original;
        };
    }

    @Patch(className = "zombie.core.skinnedmodel.visual.HumanVisual", methodName = "getSkinTexture")
    public static final class GetSkinTexture {
        @OnExit
        public static void exit(@This HumanVisual visual,
                                @Return(readOnly = false) String result) {
            result = protectedTextureFor(visual, result);
        }
    }
}
