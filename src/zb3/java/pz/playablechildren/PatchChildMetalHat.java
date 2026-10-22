package pz.playablechildren;

import java.util.Locale;
import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.Argument;
import me.zed_0xff.zombie_buddy.annotations.Patch.This;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;

/** CHILD-only creation-time geometry overrides: Metalero G1 with full hats,
 * and Harry Soulfilcher with Carl Hat only. Selected IDs, shared registry
 * aliases, other ages and no-hat hair stay unchanged; real skinning is loaded. */
public final class PatchChildMetalHat {
    private PatchChildMetalHat() {}
    private static final ThreadLocal<ItemVisuals> WORN = ThreadLocal.withInitial(ItemVisuals::new);

    private static String token(String model) {
        if (model == null) return "";
        String token = model.replace('\\', '/').toLowerCase(Locale.ROOT);
        token = token.substring(token.lastIndexOf('/') + 1);
        if (token.endsWith(".x")) token = token.substring(0, token.length() - 2);
        return token;
    }

    private static boolean candidate(String model) {
        String raw = token(model);
        return raw.equals("m_hair_metal_g1") || raw.equals("child_harry_m_soulfilcher");
    }

    private static String withHat(String model, ItemVisual hat, boolean female) {
        if (hat == null || PatchChildFemaleHeadwear.hairHat(hat, LifeStage.CHILD, female) == null) return model;
        if (token(model).equals("m_hair_metal_g1")) return "skinned/hair/m_hair_fabian_G1";
        var clothing = hat.getClothingItem();
        String hatModel = clothing == null ? "" : token(clothing.getModel(female));
        if (hatModel.equals("carlhat_child") || hatModel.equals("carlhat_adult"))
            return "skinned/hair/child_harry_m_soulfilchercat1";
        return model;
    }

    public static String worldModel(IsoGameCharacter character, String model) {
        if (!candidate(model) || character == null || LifeStageResolver.stageOf(character) != LifeStage.CHILD) return model;
        ItemVisuals visuals = WORN.get();
        visuals.clear();
        try {
            // The collecting overload reads local wornItems, not remote cache.
            character.getItemVisuals(visuals);
            ItemVisual hat = visuals.findHat();
            return withHat(model, hat, character.isFemale());
        } finally {
            visuals.clear();
        }
    }

    public static String previewModel(AnimatedModel owner, String model) {
        if (!candidate(model) || owner == null) return model;
        ModelInstance root = owner.getModelInstance();
        LifeStage stage = root == null ? LifeStageResolver.stageOf(owner.getHumanVisual())
            : PatchChildHairTransform.resolveRootStage(root);
        if (stage != LifeStage.CHILD) return model;
        // AnimatedModel.getItemVisuals(out) only clears out in B42.21; the
        // actual preview wardrobe is the private itemVisuals field.
        Object raw = ReflectionAccess.tryGet(owner, "itemVisuals", null);
        ItemVisual hat = raw instanceof ItemVisuals visuals ? visuals.findHat() : null;
        return withHat(model, hat, owner.isFemale());
    }

    @Patch(className = "zombie.core.skinnedmodel.population.PopTemplateManager", methodName = "addHeadHairItem")
    public static final class World {
        private World() {}
        @Patch.OnEnter
        public static void enter(@Argument(0) IsoGameCharacter character,
                                 @Argument(value = 2, readOnly = false) String model) {
            model = worldModel(character, model);
        }
    }

    @Patch(className = "zombie.core.skinnedmodel.advancedanimation.AnimatedModel", methodName = "addHeadHairItem")
    public static final class Preview {
        private Preview() {}
        @Patch.OnEnter
        public static void enter(@This AnimatedModel owner,
                                 @Argument(value = 0, readOnly = false) String model) {
            model = previewModel(owner, model);
        }
    }
}
