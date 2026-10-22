package pz.playablechildren;

import me.zed_0xff.zombie_buddy.Exposer;

@Exposer.LuaClass(name = "PlayableChildren.Hair")
public final class HairApi {
    private HairApi() {}

    public static boolean ensureHairRegistry() {
        return PatchChildHairRegistry.ensureRegistered();
    }

    public static boolean isVanillaHairStyle(String styleId, boolean female) {
        return PatchChildHairRegistry.isVanillaStyle(styleId, female);
    }

    public static String toPchVanillaHair(String styleId, boolean female) {
        return PatchChildHairRegistry.toVanillaAlias(styleId, female);
    }

    public static boolean isYakiHairStyle(String styleId) {
        return PatchChildHairRegistry.isYakiSource(styleId);
    }

    public static String toPchYakiHair(String styleId) {
        return PatchChildHairRegistry.toYakiAlias(styleId);
    }

    public static boolean hasHairStyle(String styleId, boolean female) {
        return PatchChildHairRegistry.hasStyle(styleId, female);
    }

    public static String canonicalBaseHairStyle(String styleId, boolean female) {
        return PatchChildHairRegistry.canonicalBaseForAlternate(styleId, female);
    }
}
