package pz.playablechildren;

import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.Return;
import me.zed_0xff.zombie_buddy.annotations.Patch.This;
import zombie.core.skinnedmodel.visual.HumanVisual;

/** Temporary, age-scoped exclusion. Original optional catalogs and adult styles stay intact. */
public final class PatchMinorHairBlock {
    private PatchMinorHairBlock() {}

    public static boolean isBlockedPack(String style) {
        // TTRP's supplied XML uses this unprefixed ID for its hidden Griffith alternate.
        return style != null && (style.equalsIgnoreCase("M_GriffithHat")
            || style.regionMatches(true,0,"M_Yaki_",0,7)
            || style.regionMatches(true,0,"F_Yaki_",0,7)
            || style.regionMatches(true,0,"PCH_Yaki_",0,9)
            || style.regionMatches(true,0,"M_TTRP_",0,7)
            || style.regionMatches(true,0,"F_TTRP_",0,7)
            || style.regionMatches(true,0,"PCH_TTRP_",0,9));
    }

    public static String allowedStyle(String style, LifeStage stage, boolean female) {
        if (!isBlockedPack(style)) return style;
        if (stage == LifeStage.CHILD) return "PCH_Vanilla_Short";
        if (stage == LifeStage.TEEN) return female ? "Bob" : "Short";
        return style;
    }

    public static String allowedStyle(HumanVisual visual, String style) {
        // Most render lookups avoid stage resolution entirely.
        if (visual == null || !isBlockedPack(style)) return style;
        return allowedStyle(style, LifeStageResolver.stageOf(visual), visual.isFemale());
    }

    @Patch(className="zombie.core.skinnedmodel.visual.HumanVisual", methodName="getHairModel", strictMatch=false)
    public static final class HairModel {
        private HairModel() {}
        @OnExit public static void exit(@This HumanVisual visual, @Return(readOnly=false) String style) {
            style = allowedStyle(visual, style);
        }
    }
}

