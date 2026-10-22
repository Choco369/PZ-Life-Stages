package pz.playablechildren;

import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.Model;

/**
 * Adolescent bodies. The male teen keeps the child-male skeleton/weights with
 * teen morphology, while the female teen uses the dedicated female body line.
 */
public final class TeenModel {
    public static final String MALE_BODY_ID = "skinned/PlayableChildren/TeenBody";
    public static final String FEMALE_BODY_ID = "skinned/PlayableChildren/TeenFemaleBody";

    private static Model maleBody;
    private static Model femaleBody;

    private TeenModel() {}

    public static synchronized Model resolve(ModelManager manager, boolean female) {
        if (female) {
            femaleBody = load(manager, femaleBody, FEMALE_BODY_ID);
            return femaleBody;
        }
        maleBody = load(manager, maleBody, MALE_BODY_ID);
        return maleBody;
    }

    public static synchronized void preload(ModelManager manager) {
        resolve(manager, false);
        resolve(manager, true);
    }

    private static Model load(ModelManager manager, Model cached, String id) {
        if (cached != null || manager == null) return cached;
        Model model = manager.getLoadedModel(id, null, false, "basicEffect");
        if (model == null) {
            manager.loadAdditionalModel(id, null, false, "basicEffect");
            model = manager.getLoadedModel(id, null, false, "basicEffect");
        }
        return model;
    }
}
