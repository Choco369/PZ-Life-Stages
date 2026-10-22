package pz.playablechildren;

import java.util.Locale;
import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.Argument;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.Return;
import me.zed_0xff.zombie_buddy.annotations.Patch.This;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;

/**
 * CHILD female geometry parity and sex-scoped TEEN partial-headwear hair selection.
 *
 * CHILD girls intentionally use the same CHILD head/body shape as CHILD boys.
 * Vanilla ClothingItem may still select distinct female hat, trousers and shorts
 * meshes.  Instead of
 * intercepting ClothingItem.getModel(), let vanilla build the item normally and
 * then swap ONLY the finished CHILD-female headwear/lower-body ModelInstance to
 * the exact male geometry. Headwear remains static, while trousers and shorts
 * must be loaded as skinned/animated meshes before their geometry is assigned.
 *
 * ItemVisual, texture choice, tint, owner, attachment and the shared CHILD
 * render-space fitting remain untouched.
 */
public final class PatchChildFemaleHeadwear {
    private PatchChildFemaleHeadwear() {}

    public static boolean usesNormalHair(ItemVisual visual) {
        return usesNormalHair(visual, true);
    }

    public static boolean usesNormalHair(ItemVisual visual, boolean female) {
        ClothingItem item = visual == null ? null : visual.getClothingItem();
        if (item == null) return false;
        String model = normalise(female ? item.getFemaleModel() : item.getModel(false));
        model = model.substring(model.lastIndexOf('/') + 1);
        if (model.endsWith(".x")) model = model.substring(0, model.length() - 2);
        if (!female && model.equals("m_visorhat")) model = "f_visor";
        else if (!female && model.startsWith("m_")) model = "f_" + model.substring(2);
        return switch (model) {
            case "f_bandana", "f_sweatband", "f_sweatbandimp", "f_earmuffs",
                 "f_earmuff_protectors", "f_visor", "f_weddingveil",
                 "f_jokeknife", "f_jokearrow", "f_headbopper_antlers",
                 "f_headbopper_goldstar", "f_headmirrorup", "f_headmirrordown",
                 "f_bunnyears" -> true;
            default -> false;
        };
    }

    public static ItemVisual hairHat(ItemVisual visual, LifeStage stage, boolean female) {
        if (stage == LifeStage.TEEN) return usesNormalHair(visual, female) ? null : visual;
        if (stage != LifeStage.CHILD) return visual;
        if (usesNormalHair(visual, false) || usesNormalHair(visual, true)) return null;
        ClothingItem item = visual == null ? null : visual.getClothingItem();
        String male = item == null ? "" : normalise(item.getModel(false));
        // CHILD girls use the male headwear geometry too; teddy ears are Child-only.
        return male.endsWith("/m_furryears") || male.endsWith("/m_furryears.x") ? null : visual;
    }

    public static ItemVisual previewHairHat(AnimatedModel owner, ItemVisual visual) {
        if (owner == null) return visual;
        ModelInstance root = owner.getModelInstance();
        LifeStage stage = root == null ? LifeStageResolver.stageOf(owner.getHumanVisual())
            : PatchChildHairTransform.resolveRootStage(root);
        String body = root == null || root.model == null ? "" : normalise(root.model.name);
        boolean female = body.contains("playablechildren/teenfemalebody")
            || (!body.contains("playablechildren/teenbody") && owner.isFemale());
        return hairHat(visual, stage, female);
    }

    @Patch(className = "zombie.core.skinnedmodel.population.PopTemplateManager", methodName = "addHeadHair")
    public static final class WorldHeadHair {
        private WorldHeadHair() {}

        @Patch.OnEnter
        public static void enter(@Argument(0) IsoGameCharacter character,
                                 @Argument(value = 3, readOnly = false) ItemVisual hat) {
            if (character != null) hat = hairHat(hat, LifeStageResolver.stageOf(character), character.isFemale());
        }
    }

    @Patch(className = "zombie.core.skinnedmodel.advancedanimation.AnimatedModel", methodName = "addHeadHair")
    public static final class PreviewHeadHair {
        private PreviewHeadHair() {}

        @Patch.OnEnter
        public static void enter(@This AnimatedModel owner,
                                 @Argument(value = 0, readOnly = false) ItemVisual hat) {
            hat = previewHairHat(owner, hat);
        }
    }

    private static String normalise(String value) {
        return value == null ? "" : value.replace('\\', '/').toLowerCase(Locale.ENGLISH);
    }

    private static boolean isTrueHeadwear(ClothingItem item) {
        return item != null && item.isHat() && "Bip01_Head".equalsIgnoreCase(item.attachBone);
    }

    /** Exact vanilla lower-body slots that must follow the shared CHILD MaleBody. */
    public static boolean usesMaleLowerBodyModel(ItemBodyLocation location) {
        return location == ItemBodyLocation.PANTS
            || location == ItemBodyLocation.PANTS_SKINNY
            || location == ItemBodyLocation.PANTS_EXTRA
            || location == ItemBodyLocation.SHORT_PANTS
            || location == ItemBodyLocation.SHORTS_SHORT;
    }

    /** Headwear is static; lower-body clothing must stay attached to the animation skeleton. */
    public static boolean requiresStaticModel(boolean trueHeadwear, ItemBodyLocation location) {
        if (usesMaleLowerBodyModel(location)) return false;
        return trueHeadwear;
    }

    private static ItemBodyLocation bodyLocation(ItemVisual itemVisual) {
        Item scriptItem = itemVisual == null ? null : itemVisual.getScriptItem();
        return scriptItem == null ? null : scriptItem.getBodyLocation();
    }

    private static boolean usesMaleGeometry(ItemVisual itemVisual, ClothingItem item) {
        if (isTrueHeadwear(item)) return true;
        return usesMaleLowerBodyModel(bodyLocation(itemVisual));
    }

    private static Model fittedMaleModel(ItemVisual itemVisual, ClothingItem item, boolean alternate) {
        if (itemVisual == null || item == null || !usesMaleGeometry(itemVisual, item)) return null;

        String modelName = alternate ? item.getAltModel(false) : null;
        if (modelName == null || modelName.isBlank()) modelName = item.getModel(false);
        if (modelName == null || modelName.isBlank()) return null;
        modelName = normalise(modelName);

        String textureName = itemVisual.getTextureChoice(item);
        String shader = item.shader;
        ModelManager manager = ModelManager.instance;
        if (manager == null) return null;

        boolean staticModel = requiresStaticModel(isTrueHeadwear(item), bodyLocation(itemVisual));
        Model fitted = manager.getLoadedModel(modelName, textureName, staticModel, shader);
        if (fitted == null) {
            if (staticModel) {
                fitted = manager.loadStaticModel(modelName, textureName, shader);
            } else {
                manager.loadAdditionalModel(modelName, textureName, false, shader);
                fitted = manager.getLoadedModel(modelName, textureName, false, shader);
            }
        }
        return fitted;
    }

    /** Public bridge: ZombieBuddy inlines advice into vanilla target classes. */
    public static void swap(ModelInstance instance, ItemVisual itemVisual, ClothingItem item, boolean alternate) {
        if (instance == null) return;
        Model fitted = fittedMaleModel(itemVisual, item, alternate);
        if (fitted == null || instance.model == fitted) return;

        // Preserve the vanilla-created instance and everything attached to it;
        // replace geometry/material backing only, exactly like the old stable cap path.
        instance.model = fitted;
        instance.tex = fitted.tex;
        instance.data = fitted.tag instanceof SkinningData ? (SkinningData) fitted.tag : null;
        instance.scale = 1.0F;
    }

    /** World/gameplay path. Patch only the 5-argument implementation overload. */
    @Patch(className = "zombie.core.skinnedmodel.population.PopTemplateManager", methodName = "addClothingItem")
    public static final class WorldClothingItem {
        private WorldClothingItem() {}

        @OnExit
        public static void exit(@Argument(0) IsoGameCharacter character,
                                @Argument(2) ItemVisual itemVisual,
                                @Argument(3) ClothingItem item,
                                @Argument(4) boolean alternate,
                                @Return ModelInstance instance) {
            if (character == null || !character.isFemale()) return;
            if (LifeStageResolver.stageOf(character) != LifeStage.CHILD) return;
            swap(instance, itemVisual, item, alternate);
        }
    }

    /** Character-creator / AnimatedModel path. AnimatedModel uses getModel(female), never alt here. */
    @Patch(className = "zombie.core.skinnedmodel.advancedanimation.AnimatedModel", methodName = "addClothingItem")
    public static final class AnimatedClothingItem {
        private AnimatedClothingItem() {}

        @OnExit
        public static void exit(@This AnimatedModel animatedModel,
                                @Argument(0) ItemVisual itemVisual,
                                @Argument(1) ClothingItem item) {
            if (animatedModel == null || itemVisual == null || item == null) return;
            BaseVisual baseVisual = animatedModel.getVisual();
            if (!(baseVisual instanceof HumanVisual humanVisual)) return;
            if (!humanVisual.isFemale() || LifeStageResolver.stageOf(humanVisual) != LifeStage.CHILD) return;

            ModelInstance root = animatedModel.getModelInstance();
            if (root == null) return;
            for (ModelInstance sub : root.sub) {
                if (sub != null && sub.getItemVisual() == itemVisual) {
                    swap(sub, itemVisual, item, false);
                    return;
                }
            }
        }
    }
}
