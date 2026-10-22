package pz.playablechildren;

import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.Model;

/** Shared child body model. Both sexes intentionally use the same child body mesh. */
public final class ChildModel {
    public static final String MALE_BODY_ID = "skinned/PlayableChildren/MaleBody";

    private static Model maleBody;

    private ChildModel() {}

    public static synchronized Model resolve(ModelManager manager, boolean female) {
        maleBody = load(manager, maleBody, MALE_BODY_ID);
        return maleBody;
    }

    public static synchronized void preload(ModelManager manager) {
        resolve(manager, false);
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
