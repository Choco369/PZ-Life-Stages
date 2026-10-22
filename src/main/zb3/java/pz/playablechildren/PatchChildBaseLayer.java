package pz.playablechildren;

import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.Return;
import me.zed_0xff.zombie_buddy.annotations.Patch.This;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;

/** Permanent non-inventory modesty/protection layers for CHILD and TEEN stages. */
public final class PatchChildBaseLayer {
    public static final String MALE_TOP = "PlayableChildren.BaseLayer_MaleVest";
    public static final String FEMALE_TOP = "PlayableChildren.BaseLayer_FemaleSwimsuit";
    public static final String BOTTOM = "PlayableChildren.BaseLayer_BoxersBlack";
    private PatchChildBaseLayer() {}

    public static ItemVisual find(ItemVisuals visuals, String itemType) {
        if (visuals == null || itemType == null) return null;
        for (int i = 0; i < visuals.size(); i++) {
            ItemVisual v = visuals.get(i);
            if (v != null && itemType.equals(v.getItemType())) return v;
        }
        return null;
    }

    public static void remove(ItemVisuals visuals, String itemType) {
        if (visuals == null || itemType == null) return;
        for (int i = visuals.size() - 1; i >= 0; i--) {
            ItemVisual v = visuals.get(i);
            if (v != null && itemType.equals(v.getItemType())) visuals.remove(i);
        }
    }

    public static ItemVisual ensureOne(HumanVisual humanVisual, ItemVisuals visuals, String itemType) {
        ItemVisual found = find(visuals, itemType);
        if (found != null) return found;
        return humanVisual == null ? null : humanVisual.addBodyVisualFromItemType(itemType);
    }

    public static void ensure(HumanVisual humanVisual, ItemVisuals visuals) {
        if (humanVisual == null || visuals == null) return;
        LifeStage stage = LifeStageResolver.stageOf(humanVisual);
        if (stage != LifeStage.CHILD && stage != LifeStage.TEEN) return;

        final boolean female = humanVisual.isFemale();

        if (female) {
            // Female minors are texture-only by design. Do not add a second 3D/bodyVisual
            // underwear layer: it can clip or hide vanilla trousers. Their permanent
            // protection is provided by PatchFemaleMinorSkinTexture, which swaps only
            // the body skin texture for mod-owned CHILD/TEEN female variants.
            remove(visuals, MALE_TOP);
            remove(visuals, FEMALE_TOP);
            remove(visuals, BOTTOM);
            return;
        }

        remove(visuals, FEMALE_TOP);

        if (stage == LifeStage.CHILD) {
            // Existing child-boy protection: white undershirt + black boxers.
            ItemVisual top = ensureOne(humanVisual, visuals, MALE_TOP);
            if (top != null) top.setTint(ImmutableColor.white);
        } else {
            // TEEN boy: boxer protection only. At this stage an uncovered upper
            // torso is intentional; do not force the child undershirt onto him.
            remove(visuals, MALE_TOP);
        }

        ItemVisual boxers = ensureOne(humanVisual, visuals, BOTTOM);
        if (boxers != null) boxers.setTint(ImmutableColor.black);
    }

    @Patch(className = "zombie.core.skinnedmodel.visual.HumanVisual", methodName = "getBodyVisuals")
    public static final class GetBodyVisuals {
        @OnExit
        public static void exit(@This HumanVisual humanVisual, @Return ItemVisuals visuals) {
            ensure(humanVisual, visuals);
        }
    }
}
