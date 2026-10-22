package pz.playablechildren;

import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.Return;
import me.zed_0xff.zombie_buddy.annotations.Patch.This;
import zombie.core.skinnedmodel.visual.HumanVisual;

/**
 * ELDER visual route that keeps the vanilla adult meshes intact.
 *
 * Instead of relying on ModelInstanceTextureCreator's post-bake hook, return a
 * mod-owned aged variant of the same vanilla body texture only while the visual
 * resolves to LifeStage.ELDER. CHILD/TEEN/ADULT texture selection is untouched.
 */
public final class PatchElderSkinTexture {
    public static final String PREFIX = "PZLS_Elder_";

    private PatchElderSkinTexture() {}

    public static String elderTextureFor(HumanVisual visual, String original) {
        if (visual == null || original == null || original.isEmpty()) return original;
        if (LifeStageResolver.stageOf(visual) != LifeStage.ELDER) return original;
        if (original.startsWith(PREFIX)) return original;

        return switch (original) {
            case "FemaleBody01", "FemaleBody02", "FemaleBody03", "FemaleBody04", "FemaleBody05",
                 "MaleBody01", "MaleBody01a", "MaleBody02", "MaleBody02a", "MaleBody03", "MaleBody03a",
                 "MaleBody04", "MaleBody04a", "MaleBody05", "MaleBody05a" -> PREFIX + original;
            default -> original;
        };
    }

    @Patch(className = "zombie.core.skinnedmodel.visual.HumanVisual", methodName = "getSkinTexture")
    public static final class GetSkinTexture {
        @OnExit
        public static void exit(@This HumanVisual visual,
                                @Return(readOnly = false) String result) {
            result = elderTextureFor(visual, result);
        }
    }
}
