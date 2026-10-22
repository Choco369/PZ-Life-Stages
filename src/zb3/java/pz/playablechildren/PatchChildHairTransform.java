package pz.playablechildren;

import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.This;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.visual.HumanVisual;

/** Calibrated CHILD and TEEN hair fitting. */
public final class PatchChildHairTransform {
    public static final float SCALE = 1.17762368F;
    /** Measured Teen head shell is roughly 94-96% of the adult bind-pose head. */
    public static final float TEEN_HAIR_SCALE = 0.95F;
    public static final float NORMAL_X = 0.00009510F;
    public static final float NORMAL_Y = -0.1123738F;
    public static final float NORMAL_Z = 0.01345086F;
    public static final float HAT_X = 0.00009510F;
    public static final float HAT_Y = -0.1243738F;
    public static final float HAT_Z = -0.00454914F;
    public static final float RUNTIME_LIFT_Y = 0.12000000F;

    private static final Map<Object, String> MODEL_NAMES =
        Collections.synchronizedMap(new WeakHashMap<>());

    private static final class StyleAdjustment {
        final float x, y, z, scale;
        StyleAdjustment(float x, float y, float z, float scale) {
            this.x = x; this.y = y; this.z = z; this.scale = scale;
        }
    }

    private static final Map<String, StyleAdjustment> STYLE_ADJUSTMENTS = new HashMap<>();
    /** CHILD calibration.  Playable girls still share the CHILD MaleBody head, so
     *  the established child map keeps its historical male->female fallback rules. */
    private static final Map<String, StyleAdjustment> USER_ADJUSTMENTS = new HashMap<>();

    /**
     * FIXED40: deltas exported after the local-space correction in FIXED38.
     * These are intentionally ADDITIVE on top of the established CHILD base fit,
     * exactly like the live tuner.  Keeping them separate avoids reinterpreting
     * an exported delta as an absolute replacement.
     */
    private static final Map<String, StyleAdjustment> CHILD_STYLE_EXTRA_ADJUSTMENTS = new HashMap<>();

    /**
     * FIXED41: baked CHILD hat/compressed-mesh deltas exported after FIXED40.
     * Keys are exact rendered mesh tokens (the HAT:: key from the tuner), so
     * these never bleed into the normal/no-hat hairstyle fit.  CHILD girls
     * share the same child head; exact matching meshes reuse the same values,
     * with an F_Hair_* -> M_Hair_* twin fallback when names correspond.
     */
    private static final Map<String, StyleAdjustment> CHILD_HAT_EXTRA_ADJUSTMENTS = new HashMap<>();

    /**
     * A handful of pre-FIXED38 fits were authored in the old model-axis system and
     * can push these meshes completely out of view after switching to true local-space.
     * For these selectable styles only, ignore the legacy fixed/user fit and start
     * from the native/generic placement so they can be calibrated again safely.
     */
    private static final Set<String> LOCALSPACE_LEGACY_RESET_STYLES = new HashSet<>();

    /** TEEN calibration is deliberately independent from CHILD. Teen boys use
     *  TeenBody while teen girls use TeenFemaleBody. Normal/style values stay
     *  sex-specific; exact matching rendered HAT meshes may reuse the male starting
     *  value on female, with female exports overriding that copied approximation.
     *
     *  FIXED74 mirrors the proven CHILD architecture: normal hair is keyed by the
     *  selectable style ID, while hat/compressed variants live in a separate map.
     *  Some older tuner exports recorded a selectable style while a hat was worn
     *  because that hairstyle kept the same render mesh; those are kept in a
     *  dedicated hat-style fallback map and only activate in a real hat context. */
    private static final Map<String, StyleAdjustment> TEEN_MALE_STYLE_EXTRA_ADJUSTMENTS = new HashMap<>();
    private static final Map<String, StyleAdjustment> TEEN_FEMALE_STYLE_EXTRA_ADJUSTMENTS = new HashMap<>();
    private static final Map<String, StyleAdjustment> TEEN_MALE_HAT_MODEL_EXTRA_ADJUSTMENTS = new HashMap<>();
    private static final Map<String, StyleAdjustment> TEEN_FEMALE_HAT_MODEL_EXTRA_ADJUSTMENTS = new HashMap<>();
    private static final Map<String, StyleAdjustment> TEEN_MALE_HAT_STYLE_EXTRA_ADJUSTMENTS = new HashMap<>();
    private static final Map<String, StyleAdjustment> TEEN_FEMALE_HAT_STYLE_EXTRA_ADJUSTMENTS = new HashMap<>();
    private static final Map<String, StyleAdjustment> TEEN_FEMALE_HAT_DELTAS = new HashMap<>();
    // Exact incremental exports: CHILD FEMALE only, including male-named shared meshes.
    private static final Map<String, StyleAdjustment> CHILD_FEMALE_HAT_117W = new HashMap<>();
    static {
        CHILD_FEMALE_HAT_117W.put("child_harry_f_britacat1", new StyleAdjustment(0.0000F,0.0200F,0.0040F,0.9880F));
        CHILD_FEMALE_HAT_117W.put("child_harry_f_ponytailmediumcat1", new StyleAdjustment(0.0000F,-0.0200F,-0.0300F,0.9770F));
        CHILD_FEMALE_HAT_117W.put("child_harry_f_throttlekittycat1", new StyleAdjustment(0.0000F,0.0000F,0.0000F,1.0200F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_alanwakeg1", new StyleAdjustment(0.0000F,-0.0440F,-0.0330F,1.0320F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_ashg1", new StyleAdjustment(0.0000F,-0.0270F,-0.0100F,1.0400F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_ashg4", new StyleAdjustment(0.0000F,-0.0420F,-0.0100F,1.0440F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_erenbung1", new StyleAdjustment(0.0000F,0.0000F,0.0020F,0.9910F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_herobung1", new StyleAdjustment(0.0000F,0.0000F,0.0000F,0.9930F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_kusanagig1", new StyleAdjustment(0.0000F,-0.0600F,-0.0110F,1.0750F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_leonlongg1", new StyleAdjustment(0.0000F,-0.0050F,0.0030F,1.0140F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_logang1", new StyleAdjustment(0.0000F,-0.0480F,-0.0120F,1.0580F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_messybobg1", new StyleAdjustment(0.0000F,-0.0180F,0.0000F,1.0300F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_partedsideg1", new StyleAdjustment(0.0000F,-0.0010F,0.0000F,1.0010F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_partedsidemediumg1", new StyleAdjustment(0.0000F,-0.0060F,0.0000F,1.0000F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_redfieldg1", new StyleAdjustment(0.0000F,0.0030F,0.0030F,0.9820F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_redfieldlowg1", new StyleAdjustment(0.0000F,-0.0040F,0.0000F,1.0080F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_rickg1", new StyleAdjustment(0.0000F,-0.0310F,-0.0080F,1.0340F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_shaggycurlyg1", new StyleAdjustment(0.0000F,0.0000F,0.0050F,0.9910F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_surferg1", new StyleAdjustment(0.0000F,-0.1170F,0.0140F,1.1210F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_toukastraightg4", new StyleAdjustment(0.0000F,-0.0460F,-0.0010F,1.0380F));
        CHILD_FEMALE_HAT_117W.put("child_spn_m_valentineg1", new StyleAdjustment(0.0000F,0.0000F,0.0000F,0.9960F));
        CHILD_FEMALE_HAT_117W.put("f_hair_bound_g1", new StyleAdjustment(0.0000F,0.0400F,0.2100F,1.0000F));
        CHILD_FEMALE_HAT_117W.put("f_hair_centrepartinglong_g1", new StyleAdjustment(0.0000F,0.0100F,0.0200F,1.0200F));
        CHILD_FEMALE_HAT_117W.put("f_hair_grungeyparting_g1", new StyleAdjustment(0.0000F,0.0000F,0.3000F,1.1100F));
        CHILD_FEMALE_HAT_117W.put("f_hair_hat", new StyleAdjustment(0.0000F,0.0000F,-0.0400F,1.0000F));
        CHILD_FEMALE_HAT_117W.put("f_hair_hatpunk_g1", new StyleAdjustment(0.0000F,0.0200F,0.1600F,1.0000F));
        CHILD_FEMALE_HAT_117W.put("f_hair_long_g1", new StyleAdjustment(0.0000F,0.0000F,0.2500F,1.0700F));
        CHILD_FEMALE_HAT_117W.put("f_hair_longcurly_g1", new StyleAdjustment(0.0000F,0.0000F,0.2200F,1.0500F));
        CHILD_FEMALE_HAT_117W.put("f_hair_mullet_g1", new StyleAdjustment(0.0000F,0.1500F,0.1100F,1.0000F));
        CHILD_FEMALE_HAT_117W.put("f_hair_mulletcurly_g1", new StyleAdjustment(0.0000F,0.0100F,-0.0100F,1.0150F));
        CHILD_FEMALE_HAT_117W.put("f_hair_rachel_g1", new StyleAdjustment(0.0000F,0.0200F,0.1900F,1.0350F));
        CHILD_FEMALE_HAT_117W.put("f_hair_rachelcurly_g1", new StyleAdjustment(0.0000F,0.0200F,0.2100F,1.0500F));
        CHILD_FEMALE_HAT_117W.put("kate_hair_hatcurly", new StyleAdjustment(0.0000F,-0.1200F,0.0200F,1.0000F));
        CHILD_FEMALE_HAT_117W.put("m_hair_ponytail", new StyleAdjustment(0.0000F,0.0000F,0.0100F,1.0000F));
        CHILD_FEMALE_HAT_117W.put("m_hair_shorthat", new StyleAdjustment(0.0000F,0.0000F,0.0000F,0.9980F));
    }

    private static final Map<String, StyleAdjustment> CHILD_FEMALE_HAT_117X = new HashMap<>();
    /** 117Z incremental CHILD male exports. Only the exact four Spongie meshes
     * occur in both sex catalogs; female twins and female-only meshes stay frozen. */
    private static final Map<String, StyleAdjustment> CHILD_HAT_117Z = Map.of(
        "child_spn_m_alanwakeg1", new StyleAdjustment(0,0,.025F,1),
        "child_spn_m_curtainsg1", new StyleAdjustment(0,-.050F,0,1.055F),
        "child_spn_m_partedg1", new StyleAdjustment(0,-.060F,0,1.060F),
        "child_spn_m_partedmediumg1", new StyleAdjustment(0,-.040F,-.005F,1.035F),
        "m_hair_fabian_g1", new StyleAdjustment(0,0,.005F,1),
        "m_hair_fabiancurly_g1", new StyleAdjustment(0,0,.005F,1),
        "m_hair_longbraids02_g1", new StyleAdjustment(0,0,.195F,1.170F));
    private static final Set<String> CHILD_FEMALE_SHARED_117Z = Set.of(
        "child_spn_m_alanwakeg1", "child_spn_m_curtainsg1",
        "child_spn_m_partedg1", "child_spn_m_partedmediumg1");
    private static final Set<String> CHILD_MALE_HARRY_NATIVE_117Z = Set.of(
        "child_harry_m_aiteron", "child_harry_m_aiteroncat1",
        "child_harry_m_soulfilcher", "child_harry_m_soulfilchercat1",
        "child_harry_m_obiwancannoli", "child_harry_m_obiwancannolicat1");
    static {
        CHILD_FEMALE_HAT_117X.put("bob_hair_ponytailbraided", new StyleAdjustment(0.0000F,0.0000F,0.0000F,0.9900F));
        CHILD_FEMALE_HAT_117X.put("child_spn_m_alanwakeg1", new StyleAdjustment(0.0000F,0.0000F,-0.0300F,1.0000F));
        CHILD_FEMALE_HAT_117X.put("f_hair_bob_g1", new StyleAdjustment(0.0050F,0.0150F,0.2450F,1.0700F));
        CHILD_FEMALE_HAT_117X.put("f_hair_bobcurly_g1", new StyleAdjustment(0.0000F,0.0200F,0.1950F,1.0150F));
        CHILD_FEMALE_HAT_117X.put("f_hair_buffont_g1", new StyleAdjustment(0.0000F,0.0000F,-0.0200F,1.0000F));
        CHILD_FEMALE_HAT_117X.put("f_hair_greasedback_g1", new StyleAdjustment(0.0000F,-0.0100F,0.2150F,1.2050F));
        CHILD_FEMALE_HAT_117X.put("f_hair_grungey02_g1", new StyleAdjustment(0.0000F,0.0000F,0.3000F,1.0950F));
        CHILD_FEMALE_HAT_117X.put("f_hair_grungey_g1", new StyleAdjustment(0.0000F,0.0000F,0.1500F,1.1300F));
        CHILD_FEMALE_HAT_117X.put("f_hair_grungeybehindears_g1", new StyleAdjustment(0.0000F,-0.0100F,0.1550F,1.1350F));
        CHILD_FEMALE_HAT_117X.put("f_hair_longcurly_g1", new StyleAdjustment(0.0000F,0.0050F,0.0000F,0.9950F));
        CHILD_FEMALE_HAT_117X.put("f_hair_mullet_g1", new StyleAdjustment(0.0000F,0.0000F,0.1600F,1.1200F));
        CHILD_FEMALE_HAT_117X.put("f_hair_overeye_g1", new StyleAdjustment(0.0000F,0.0050F,0.2500F,1.0700F));
        CHILD_FEMALE_HAT_117X.put("f_hair_overeyecurly_g1", new StyleAdjustment(0.0000F,0.0000F,0.2900F,1.0950F));
        CHILD_FEMALE_HAT_117X.put("m_hair_ponytail", new StyleAdjustment(0.0000F,-0.0100F,-0.0150F,0.9900F));
        CHILD_FEMALE_HAT_117X.put("m_hair_shorthat", new StyleAdjustment(0.0000F,-0.0100F,0.0050F,1.0000F));
    }

    /** 117Y: exact intersection of the registered CHILD male/female raw meshes
     * and the frozen 117W/117X full-hat exports. The prior fixed/user/hat layers
     * already use the identical shared mesh fit; apply each late female delta
     * once to that common base. Never infer a twin from a style label or M/F name.
     * These tokens do not register or expose any additional hairstyle. */
    private static final Set<String> CHILD_SHARED_HAT_EXPORT_MODELS_117Y = Set.of(
        "bob_hair_ponytailbraided", "child_spn_m_alanwakeg1",
        "child_spn_m_ashg1", "child_spn_m_ashg4", "child_spn_m_erenbung1",
        "child_spn_m_herobung1", "child_spn_m_kusanagig1", "child_spn_m_leonlongg1",
        "child_spn_m_logang1", "child_spn_m_messybobg1", "child_spn_m_partedsideg1",
        "child_spn_m_partedsidemediumg1", "child_spn_m_redfieldg1",
        "child_spn_m_redfieldlowg1", "child_spn_m_rickg1", "child_spn_m_shaggycurlyg1",
        "child_spn_m_surferg1", "child_spn_m_toukastraightg4",
        "child_spn_m_valentineg1", "m_hair_ponytail", "m_hair_shorthat");

    private static final Map<String, StyleAdjustment> HAT_EXPORT_117V = Map.of(
        "CHILD|m_hair_bound_g1", new StyleAdjustment(0,.019F,.145F,1),
        "TEEN_MALE|child_spn_m_alanwakeg1", new StyleAdjustment(0,0,0,.996F),
        "TEEN_MALE|child_spn_m_spikedmessy", new StyleAdjustment(0,-.080F,.001F,1.080F),
        "TEEN_MALE|m_hair_bound_g1", new StyleAdjustment(0,.025F,.039F,1.045F),
        "TEEN_FEMALE|f_hair_bound_g1", new StyleAdjustment(0,-.135F,-.115F,1.010F));
    private static final StyleAdjustment TEEN_MALE_OBI_WAN_HAT_DELTA =
        new StyleAdjustment(0F, -0.005F, 0F, 1.035F);

    static {
        // User convention mapped to model-local axes:
        // left/right = X, up/down = Y, forward/back (face/nape) = Z.
        add("child_harry_m_soulfilcher", 0, 0, 0, 1.040F);
        add("child_harry_m_obiwancannoli", 0, 0, 0, 1.020F);

        // Spongie visible names -> actual bundled model names.
        addMF("makoto",             0, 0, -0.030F, 1.000F); // Tonto / P3Hero
        addMF("carlos",             0, 0, -0.020F, 1.000F); // Oliveira
        addMF("valentine",     -0.010F, 0,  0.000F, 1.030F);
        addMF("redfield",           0, 0,  0.000F, 1.030F);
        addMF("goth",               0, 0, -0.018F, 1.050F); // Coletas goticas
        addMF("rick",               0, 0,  0.000F, 1.020F); // Grimes
        addMF("mitsuruponytail",    0, 0, -0.020F, 1.000F); // Emperatriz (atado)
        addMF("leon",               0, 0, -0.015F, 1.000F); // Kennedy
        addMF("erenbun",            0, 0,  0.000F, 1.020F); // Coleta Yeager
        addMF("erenparted",         0, 0, -0.008F, 1.000F); // Raya al medio
        addMF("mikasashort",        0, 0, -0.022F, 1.000F);
        addMF("marie",              0, 0,  0.000F, 1.020F);
        addMF("rickshow",           0,-0.012F,0.000F, 1.000F);
        addMF("kotoneponytail",     0, 0,  0.000F, 1.020F);
        addMF("yu",                 0, 0,  0.000F, 1.020F);
        addMF("parted",             0, 0,  0.000F, 1.020F);
        addMF("partedmedium",       0, 0, -0.015F, 1.000F);
        addMF("curtains",           0, 0, -0.015F, 1.000F);
        addMF("rei",                0, 0, -0.015F, 1.000F);
        addMF("sideswept",          0, 0, -0.018F, 1.020F);
        addMF("yukaribun",          0, 0, -0.015F, 1.000F);

        // Known-good physical vanilla mesh, only a tiny backward nudge requested.
        add("child_hair_partingcentrelong", 0, 0, -0.008F, 1.000F);
    }


    static {
        // FIXED28: baked final CHILD-BOY calibration exported from FIXED27.
        // Do not alter these values: female shared styles inherit them as a base.
        addUserStyle("Baldspot", +0.0000F, -0.2700F, +0.0400F, 1.1250F); // Coronilla
        addUserStyle("Braids", +0.0000F, -0.1650F, +0.0200F, 1.0300F); // Trenzas
        addUserStyle("Buffont", +0.0000F, -0.2150F, +0.0250F, 1.0700F); // Bouffant
        addUserStyle("CentreParting", +0.0000F, -0.3000F, +0.0400F, 1.1500F); // Raya al medio
        // Legacy Child_* meshes are removed in FIXED29. Preserve only the user's
        // final positional deltas by applying them to the corresponding vanilla source models.
        addUserStyle("Short", +0.0000F, -0.0050F, +0.0000F, 1.0050F);
        addUserStyle("ShortAfroCurly", +0.0000F, -0.0050F, +0.0000F, 1.0050F);
        addUserStyle("CentrePartingLong", +0.0000F, -0.0250F, +0.0200F, 1.0250F);
        addUserStyle("LeftParting", +0.0000F, -0.0050F, +0.0000F, 1.0050F);
        addUserStyle("RightParting", +0.0000F, -0.0050F, +0.0000F, 1.0050F);
        addUserStyle("GreasedBack", +0.0000F, -0.0300F, +0.0000F, 1.0150F);
        addUserStyle("Donny", +0.0000F, -0.0100F, +0.0050F, 1.0100F);

        // Harry internal meshes: preserve the values exported by the four tuning backups.
        // Aiteron was already calibrated under its original source ID; map that fit to the bundled child mesh too.
        addUser("child_harry_m_aiteron", +0.0000F, -0.1550F, +0.0100F, 1.0200F);
        addUser("child_harry_m_obiwancannoli", +0.0000F, -0.1150F, +0.0400F, 1.0850F);
        addUser("child_harry_m_soulfilcher", +0.0000F, -0.0900F, +0.0300F, 1.0400F);
        addUser("child_harry_f_brita", +0.0000F, -0.2810F, +0.0580F, 1.1530F);
        addUser("child_harry_f_grunge01", +0.0000F, -0.1950F, +0.0550F, 1.2000F);
        addUser("child_harry_f_merigold", +0.0000F, -0.1350F, +0.0450F, 1.1100F);
        addUser("child_harry_f_ponytaillong", +0.0000F, -0.1850F, +0.0550F, 1.1900F);
        addUser("child_harry_f_ponytailmedium", +0.0000F, -0.2040F, +0.0550F, 1.2080F);
        addUser("child_harry_f_throttlekitty", +0.0000F, -0.0100F, +0.0000F, 1.0150F);

        addUser("child_dante", +0.0000F, -0.0100F, +0.0000F, 1.0050F); // Child_Dante | Dante
        addUser("child_danteparted", +0.0000F, -0.0450F, +0.0100F, 1.0350F); // Child_DanteParted | Dante con raya
        addUserStyle("Cornrows", +0.0000F, -0.2450F, +0.0350F, 1.1050F); // Trenzas africanas
        addUserStyle("CrewCut", +0.0000F, -0.1250F, +0.0100F, 1.0000F); // Militar
        addUserStyle("Fabian", +0.0000F, -0.1700F, +0.0150F, 1.0300F); // Melena larga
        addUserStyle("FabianCurly", +0.0000F, -0.1850F, +0.0200F, 1.0450F); // Melena larga (Rizado)
        addUserStyle("FlatTop", +0.0000F, -0.1450F, +0.0100F, 1.0200F); // Tapa plana
        addUserStyle("Fresh", +0.0000F, -0.1900F, +0.0200F, 1.0550F); // Rapado
        addUserStyle("Grungey", +0.0000F, -0.1650F, +0.0200F, 1.0350F); // Grungey
        addUserStyle("GrungeyBehindEars", +0.0000F, -0.2150F, +0.0250F, 1.0800F); // Grungey tras las orejas
        addUserStyle("Hat", +0.0000F, -0.3000F, +0.0650F, 1.1700F); // Corto
        addUserStyle("LibertySpikes", +0.0000F, -0.1400F, +0.0000F, 1.0100F); // Pinchos
        addUserStyle("LongBraids", +0.0000F, -0.1350F, +0.0100F, 1.0100F); // Trenzas largas
        addUserStyle("LongBraids02", +0.0000F, -0.1750F, +0.0200F, 1.0350F); // Trenzas muy largas
        addUserStyle("M_Aiteron", +0.0000F, -0.1550F, +0.0100F, 1.0200F); // IGUI_Hair_M_Aiteron
        addUserStyle("M_Hair_Daper", +0.0000F, -0.3000F, +0.0000F, 1.1450F); // IGUI_Hair_M_Hair_Daper
        addUserStyle("M_Hair_Geralt", +0.0000F, -0.3000F, +0.0150F, 1.1500F); // IGUI_Hair_M_Hair_Geralt
        addUserStyle("M_Hair_James", +0.0000F, -0.2600F, +0.0050F, 1.1000F); // IGUI_Hair_M_Hair_James
        addUserStyle("M_Hair_Ranger", +0.0000F, -0.2250F, -0.0050F, 1.0800F); // IGUI_Hair_M_Hair_Ranger
        addUserStyle("M_ObiWanCannoli", +0.0000F, -0.2050F, +0.0300F, 1.0700F); // IGUI_Hair_M_ObiWanCannoli
        addUserStyle("M_Soulfilcher", +0.0000F, -0.2650F, +0.0400F, 1.1200F); // IGUI_Hair_M_Soulfilcher
        addUserStyle("M_Yaki_Afro", +0.0000F, -0.1350F, +0.0150F, 1.0250F); // IGUI_Hair_M_Yaki_Afro
        addUserStyle("M_Yaki_AfroBun", +0.0000F, -0.2700F, +0.0400F, 1.1300F); // IGUI_Hair_M_Yaki_AfroBun
        addUserStyle("M_Yaki_BraidedPonytail", +0.0000F, -0.2500F, +0.0400F, 1.1150F); // IGUI_Hair_M_Yaki_BraidedPonytail
        addUserStyle("M_Yaki_CheerLeader", +0.0000F, -0.2950F, +0.0450F, 1.1550F); // IGUI_Hair_M_Yaki_CheerLeader
        addUserStyle("M_Yaki_Classy", +0.0000F, -0.2050F, +0.0250F, 1.0700F); // IGUI_Hair_M_Yaki_Classy
        addUserStyle("M_Yaki_ClippedHair", +0.0000F, -0.2400F, +0.0300F, 1.1050F); // IGUI_Hair_M_Yaki_ClippedHair
        addUserStyle("M_Yaki_CroftHair", +0.0000F, -0.2250F, +0.0250F, 1.0850F); // IGUI_Hair_M_Yaki_CroftHair
        addUserStyle("M_Yaki_DoubleBraids", +0.0000F, -0.2550F, +0.0400F, 1.1150F); // IGUI_Hair_M_Yaki_DoubleBraids
        addUserStyle("M_Yaki_Dreadlocks", +0.0000F, -0.2200F, +0.0350F, 1.0900F); // IGUI_Hair_M_Yaki_Dreadlocks
        addUserStyle("M_Yaki_HairBun", +0.0000F, -0.2250F, +0.0350F, 1.0950F); // IGUI_Hair_M_Yaki_HairBun
        addUserStyle("M_Yaki_HalfUpShaved", +0.0000F, -0.2000F, +0.0300F, 1.0700F); // IGUI_Hair_M_Yaki_HalfUpShaved
        addUserStyle("M_Yaki_LowPonytail", +0.0000F, -0.2400F, +0.0400F, 1.1100F); // IGUI_Hair_M_Yaki_LowPonytail
        addUserStyle("M_Yaki_Pigtails", +0.0000F, -0.2200F, +0.0300F, 1.0850F); // IGUI_Hair_M_Yaki_Pigtails
        addUserStyle("M_Yaki_Rochefort", +0.0000F, -0.1550F, +0.0200F, 1.0300F); // IGUI_Hair_M_Yaki_Rochefort
        addUserStyle("M_Yaki_SideBraid", +0.0000F, -0.2000F, +0.0300F, 1.0650F); // IGUI_Hair_M_Yaki_SideBraid
        addUserStyle("M_Yaki_TiedHalfDown", +0.0000F, -0.2450F, +0.0350F, 1.1100F); // IGUI_Hair_M_Yaki_TiedHalfDown
        addUserStyle("M_Yaki_Undercut", +0.0000F, -0.2600F, +0.0400F, 1.1150F); // IGUI_Hair_M_Yaki_Undercut
        addUserStyle("M_Yaki_UndercutCurly", +0.0000F, -0.2150F, +0.0250F, 1.0850F); // IGUI_Hair_M_Yaki_UndercutCurly
        addUserStyle("M_Yaki_Yaki", +0.0000F, -0.2250F, +0.0300F, 1.0850F); // IGUI_Hair_M_Yaki_Yaki
        addUserStyle("Messy", +0.0000F, -0.2600F, +0.0350F, 1.1150F); // Revuelto
        addUserStyle("MessyCurly", +0.0000F, -0.2300F, +0.0300F, 1.0900F); // Revuelto (Rizado)
        addUserStyle("Metal", +0.0000F, -0.1550F, +0.0200F, 1.0250F); // Metalero
        addUserStyle("MohawkFan", +0.0000F, -0.1350F, +0.0050F, 1.0000F); // Mohicano
        addUserStyle("MohawkFlat", +0.0000F, -0.1800F, +0.0150F, 1.0400F); // Mohicano plano
        addUserStyle("MohawkShort", +0.0000F, -0.1300F, +0.0100F, 1.0000F); // Mohicano corto
        addUserStyle("MohawkSpike", +0.0000F, -0.1250F, +0.0150F, 1.0000F); // Mohicano pinchos
        addUserStyle("Mullet", +0.0000F, -0.2350F, +0.0250F, 1.0950F); // Salmonete
        addUserStyle("MulletCurly", +0.0000F, -0.2700F, +0.0400F, 1.1250F); // Salmonete (Rizado)
        addUser("child_harry_m_obiwancannoli", +0.0000F, -0.1150F, +0.0400F, 1.0850F); // PCH_Harry_M_ObiWanCannoli | Obi Wan Cannoli
        addUser("child_harry_m_soulfilcher", +0.0000F, -0.0900F, +0.0300F, 1.0400F); // PCH_Harry_M_Soulfilcher | Soulfilcher
        addUser("child_spn_m_agent", +0.0000F, -0.0700F, +0.0250F, 1.0600F); // PCH_Spn_M_Agent | Agent
        addUser("child_spn_m_aigis", +0.0000F, -0.0100F, +0.0150F, 1.0100F); // PCH_Spn_M_Aigis | Aeon
        addUser("child_spn_m_aigisalt", +0.0000F, +0.0000F, +0.0150F, 1.0000F); // PCH_Spn_M_AigisAlt | Aeon Alt
        addUser("child_spn_m_alanwake", +0.0000F, -0.0750F, +0.0200F, 1.0650F); // PCH_Spn_M_AlanWake | Wake
        addUser("child_spn_m_ash", +0.0000F, -0.0350F, +0.0150F, 1.0250F); // PCH_Spn_M_Ash | Groovy
        addUser("child_spn_m_ashbowl", +0.0000F, -0.0300F, +0.0200F, 1.0250F); // PCH_Spn_M_AshBowl | Thick Bowl Cut
        addUser("child_spn_m_ashshort", +0.0000F, -0.0600F, +0.0200F, 1.0500F); // PCH_Spn_M_AshShort | Groovy Short
        addUser("child_spn_m_ashslick", +0.0000F, -0.0700F, +0.0200F, 1.0600F); // PCH_Spn_M_AshSlick | Groovy Slick
        addUser("child_spn_m_ashley", +0.0000F, -0.0100F, +0.0100F, 1.0000F); // PCH_Spn_M_Ashley | Ashley
        addUser("child_spn_m_bobslick", +0.0000F, -0.0750F, +0.0250F, 1.0650F); // PCH_Spn_M_BobSlick | Slicked Back Bob
        addUser("child_spn_m_carlos", +0.0000F, -0.0300F, +0.0400F, 1.0250F); // PCH_Spn_M_Carlos | Oliveira
        addUser("child_spn_m_curtains", +0.0000F, -0.0700F, +0.0400F, 1.0600F); // PCH_Spn_M_Curtains | Curtains
        addUser("child_spn_m_curtainshigh", +0.0000F, -0.0750F, +0.0250F, 1.0600F); // PCH_Spn_M_CurtainsHigh | High Curtains
        addUser("child_spn_m_danteparted", +0.0000F, -0.0600F, +0.0100F, 1.0500F); // PCH_Spn_M_DanteParted | Dante
        addUser("child_spn_m_erenbun", +0.0000F, -0.1250F, +0.0350F, 1.0950F); // PCH_Spn_M_ErenBun | Coleta Yeager
        addUser("child_spn_m_erenparted", +0.0000F, -0.1100F, +0.0350F, 1.1000F); // PCH_Spn_M_ErenParted | Raya al medio largo
        addUser("child_spn_m_frank", +0.0000F, -0.0800F, +0.0200F, 1.0550F); // PCH_Spn_M_Frank | Frank
        addUser("child_spn_m_goth", +0.0000F, -0.1100F, +0.0550F, 1.0550F); // PCH_Spn_M_Goth | Coletas g�ticas
        addUser("child_spn_m_hero", +0.0000F, -0.1050F, +0.0350F, 1.1000F); // PCH_Spn_M_Hero | Hero
        addUser("child_spn_m_herobun", +0.0000F, -0.1650F, +0.0500F, 1.1550F); // PCH_Spn_M_HeroBun | Hero Bun
        addUser("child_spn_m_herotail", +0.0000F, -0.1100F, +0.0350F, 1.1000F); // PCH_Spn_M_HeroTail | zDoNotPickMe
        addUser("child_spn_m_jeanette", +0.0000F, -0.1100F, +0.0350F, 1.1000F); // PCH_Spn_M_Jeanette | Jeanette
        addUser("child_spn_m_kotone", +0.0000F, -0.0650F, +0.0150F, 1.0550F); // PCH_Spn_M_Kotone | Tonto XXII
        addUser("child_spn_m_kotoneponytail", +0.0000F, -0.0950F, +0.0300F, 1.0700F); // PCH_Spn_M_KotonePonytail | Tonto XXII (Atado)
        addUser("child_spn_m_kusanagi", +0.0000F, -0.0500F, +0.0350F, 1.0350F); // PCH_Spn_M_Kusanagi | Kusanagi
        addUser("child_spn_m_leon", +0.0000F, -0.0850F, +0.0450F, 1.0750F); // PCH_Spn_M_Leon | Kennedy
        addUser("child_spn_m_leonlong", +0.0000F, -0.0450F, +0.0200F, 1.0350F); // PCH_Spn_M_LeonLong | Kennedy Longer
        addUser("child_spn_m_leonre4", +0.0000F, -0.0800F, +0.0350F, 1.0700F); // PCH_Spn_M_LeonRE4 | Kennedy Long
        addUser("child_spn_m_logan", +0.0000F, -0.0150F, +0.0100F, 1.0050F); // PCH_Spn_M_Logan | Logan
        addUser("child_spn_m_marie", +0.0000F, -0.0700F, +0.0250F, 1.0450F); // PCH_Spn_M_Marie | Marie
        addUser("child_spn_m_messybob", +0.0000F, -0.0350F, +0.0150F, 1.0250F); // PCH_Spn_M_MessyBob | Messy Bob
        addUser("child_spn_m_mikasashort", +0.0000F, -0.0250F, +0.0350F, 1.0150F); // PCH_Spn_M_MikasaShort | Mikasa corto
        addUser("child_spn_m_mitsuruponytail", +0.0000F, -0.0950F, +0.0500F, 1.0850F); // PCH_Spn_M_MitsuruPonytail | Emperatriz (Atado)
        addUser("child_spn_m_overeyeshaggy", +0.0000F, -0.0950F, +0.0200F, 1.0750F); // PCH_Spn_M_OvereyeShaggy | Over Eye Shaggy
        addUser("child_spn_m_overeyeshaggyleft", +0.0000F, -0.0950F, +0.0200F, 1.0750F); // PCH_Spn_M_OvereyeShaggyLeft | Over Eye Shaggy Left
        addUser("child_spn_m_makoto", +0.0000F, -0.0500F, +0.0550F, 1.0450F); // PCH_Spn_M_P3Hero | Tonto
        addUser("child_spn_m_parted", +0.0000F, -0.0700F, +0.0200F, 1.0350F); // PCH_Spn_M_Parted | Encantador
        addUser("child_spn_m_partedmedium", +0.0000F, -0.0550F, +0.0300F, 1.0450F); // PCH_Spn_M_PartedMedium | Medio encantador
        addUser("child_spn_m_partedside", +0.0000F, -0.0700F, +0.0200F, 1.0600F); // PCH_Spn_M_PartedSide | Side Parted
        addUser("child_spn_m_partedsidemedium", +0.0000F, -0.0800F, +0.0250F, 1.0700F); // PCH_Spn_M_PartedSideMedium | Side Parted Medium
        addUser("child_spn_m_partedtail", +0.0000F, -0.1100F, +0.0350F, 1.1000F); // PCH_Spn_M_PartedTail | Parted Tail
        addUser("child_spn_m_power", +0.0000F, -0.0450F, +0.0150F, 1.0450F); // PCH_Spn_M_Power | Power
        addUser("child_spn_m_powertied", +0.0000F, -0.0650F, +0.0300F, 1.0550F); // PCH_Spn_M_PowerTied | Power Tied
        addUser("child_spn_m_princely", +0.0000F, -0.1350F, +0.0400F, 1.1250F); // PCH_Spn_M_Princely | Princely
        addUser("child_spn_m_princelyshort", +0.0000F, -0.0550F, +0.0250F, 1.0450F); // PCH_Spn_M_PrincelyShort | Princely Cropped
        addUser("child_spn_m_redfield", +0.0000F, -0.1500F, +0.0450F, 1.1150F); // PCH_Spn_M_Redfield | Redfield
        addUser("child_spn_m_redfieldlow", +0.0000F, -0.0750F, +0.0400F, 1.0650F); // PCH_Spn_M_RedfieldLow | Redfield Low Tail
        addUser("child_spn_m_redfieldmessy", +0.0000F, -0.0850F, +0.0250F, 1.0750F); // PCH_Spn_M_RedfieldMessy | Redfield Messy Tail
        addUser("child_spn_m_rei", +0.0000F, -0.0050F, +0.0250F, 1.0000F); // PCH_Spn_M_Rei | Rei
        addUser("child_spn_m_rick", +0.0000F, -0.0300F, +0.0150F, 1.0000F); // PCH_Spn_M_Rick | Grimes
        addUser("child_spn_m_rickshow", +0.0000F, -0.0450F, +0.0150F, 1.0450F); // PCH_Spn_M_RickShow | Hacia atr�s engominado
        addUser("child_spn_m_shaggy", +0.0000F, -0.0250F, +0.0000F, 1.0100F); // PCH_Spn_M_Shaggy | Shaggy
        addUser("child_spn_m_shaggycurly", +0.0000F, -0.0200F, +0.0000F, 1.0100F); // PCH_Spn_M_ShaggyCurly | Shaggy (Curly)
        addUser("child_spn_m_sideswept", +0.0000F, -0.0800F, +0.0500F, 1.0500F); // PCH_Spn_M_Sideswept | Peinado hacia un lado
        addUser("child_spn_m_spikedforward", +0.0000F, -0.0600F, +0.0300F, 1.0500F); // PCH_Spn_M_SpikedForward | Spiked Forward
        addUser("child_spn_m_spikedmessy", +0.0000F, -0.1050F, +0.0300F, 1.0900F); // PCH_Spn_M_SpikedMessy | Spiked Messy
        addUser("child_spn_m_spikedup", +0.0000F, -0.0900F, +0.0250F, 1.0700F); // PCH_Spn_M_SpikedUp | Spiked Up
        addUser("child_spn_m_straightcutbob", +0.0000F, -0.0650F, +0.0200F, 1.0550F); // PCH_Spn_M_StraightCutBob | Straight Cut Bob
        addUser("child_spn_m_surfer", +0.0000F, -0.0500F, +0.0100F, 1.0350F); // PCH_Spn_M_Surfer | Surfer
        addUser("child_spn_m_taurie", +0.0000F, -0.0800F, +0.0250F, 1.0700F); // PCH_Spn_M_Taurie | Taurie
        addUser("child_spn_m_touka", +0.0000F, -0.0600F, +0.0250F, 1.0500F); // PCH_Spn_M_Touka | Over Eye Fluffy
        addUser("child_spn_m_toukastraight", +0.0000F, -0.0650F, +0.0250F, 1.0550F); // PCH_Spn_M_ToukaStraight | Over Eye Straight
        addUser("child_spn_m_valentine", +0.0050F, -0.0550F, +0.0250F, 1.0200F); // PCH_Spn_M_Valentine | Valentine
        addUser("child_spn_m_vergil", +0.0000F, -0.0350F, +0.0100F, 1.0250F); // PCH_Spn_M_Vergil | Vergil
        addUser("child_spn_m_vi", +0.0000F, -0.0900F, +0.0250F, 1.0750F); // PCH_Spn_M_Vi | Vi
        addUser("child_spn_m_yu", +0.0000F, -0.0850F, +0.0350F, 1.0550F); // PCH_Spn_M_Yu | Corte taz�n
        addUser("child_spn_m_yukari", -0.0050F, -0.0450F, +0.0150F, 1.0250F); // PCH_Spn_M_Yukari | Amantes
        addUser("child_spn_m_yukarialt", +0.0000F, -0.0550F, +0.0250F, 1.0450F); // PCH_Spn_M_YukariAlt | Amantes corto
        addUser("child_spn_m_yukaribun", +0.0000F, -0.0750F, +0.0450F, 1.0650F); // PCH_Spn_M_YukariBun | Amantes (Atado)
        addUser("child_spn_m_zarina", +0.0000F, -0.0900F, +0.0300F, 1.0800F); // PCH_Spn_M_Zarina | Zarina
        addUser("child_spn_m_zarinacurly", +0.0000F, -0.1000F, +0.0300F, 1.0900F); // PCH_Spn_M_ZarinaCurly | Zarina (Curly)
        addUserStyle("PonyTail", +0.0000F, -0.2850F, +0.0400F, 1.1400F); // Coleta
        addUserStyle("PonyTailBraids", +0.0000F, -0.2450F, +0.0350F, 1.1050F); // Coleta con trenzas
        addUserStyle("ShortHatCurly", +0.0000F, -0.2200F, +0.0550F, 1.1000F); // Sencillo (Rizado)
        addUserStyle("Spike", +0.0000F, -0.2300F, +0.0300F, 1.0950F); // Puntiagudo

        // FIXED30: latest in-game export supplied by the user after testing FIXED29.
        // These entries intentionally come last so they override older calibration values.
        // Extra Character Hairs (Krezmick) remains an external/optional mod; PCH only
        // remembers the child-fit correction when those original models are present.
        addUserStyle("F_Hair_Cortana", +0.0000F, -0.2680F, -0.0020F, 1.1320F);
        addUserStyle("F_Hair_Lexie", +0.0000F, -0.3000F, +0.0210F, 1.1780F);
        addUserStyle("F_Hair_Maria", +0.0000F, -0.3000F, +0.0050F, 1.1630F);
        addUserStyle("F_Hair_PonyTailBangs", +0.0000F, -0.3000F, +0.0080F, 1.1570F);
        addUserStyle("F_Hair_PonyTailGeralt", +0.0000F, -0.3000F, +0.0080F, 1.1640F);
        addUserStyle("F_Hair_PonyTailMary", +0.0000F, -0.3000F, +0.0230F, 1.1700F);
        addUserStyle("F_Hair_PonyTail_Raider", +0.0000F, -0.3000F, +0.0170F, 1.1490F);
        addUserStyle("F_Hair_Selene", +0.0000F, -0.2710F, +0.0000F, 1.1350F);
        addUserStyle("F_Hair_SmoothLong", +0.0000F, -0.3000F, +0.0080F, 1.1590F);

        addUser("child_harry_f_merigold", +0.0000F, -0.1050F, +0.0200F, 1.1200F);
        addUser("child_harry_m_aiteron", +0.0000F, +0.0860F, +0.0050F, 1.0330F);
        addUser("child_spn_m_dante", +0.0000F, -0.0270F, +0.0000F, 1.0230F);
        addUser("child_spn_m_danteparted", +0.0000F, -0.0020F, +0.0000F, 1.0010F);
        addUserStyle("Short", +0.0000F, -0.2090F, +0.0280F, 1.0630F);


        // FIXED31: final SIN GORRA export supplied after FIXED30 testing.
        // These are EXTRA corrections and intentionally override every older entry above.
        // Picard/Fraile is excluded from the child catalog and is therefore not imported.
        addUser("child_harry_m_aiteron", +0.0000F, -0.1350F, +0.0000F, 1.0000F); // Aiteron
        addUser("child_spn_m_danteparted", +0.0000F, -0.0290F, +0.0060F, 1.0170F); // Dante
        addUserStyle("Back", +0.0000F, -0.3000F, +0.0590F, 1.1690F); // Recogido
        addUserStyle("Bob", +0.0000F, -0.2450F, +0.0400F, 1.1150F); // Bob
        addUserStyle("BobCurly", +0.0000F, -0.2450F, +0.0450F, 1.1150F); // Bob (Rizado)
        addUserStyle("Braids", +0.0000F, -0.2310F, +0.0400F, 1.0910F); // Trenzas
        addUserStyle("Buffont", +0.0000F, -0.2570F, +0.0370F, 1.1050F); // Bouffant
        addUserStyle("Bun", +0.0000F, -0.2950F, +0.0620F, 1.1750F); // Moño
        addUserStyle("BunCurly", +0.0000F, -0.2950F, +0.0620F, 1.1750F); // Moño (Rizado)
        addUserStyle("CentreParting", +0.0000F, -0.2540F, +0.0360F, 1.1100F); // Raya al medio
        addUserStyle("CentrePartingLong", +0.0000F, -0.2490F, +0.0390F, 1.1060F); // Largo con raya al medio
        addUserStyle("Cornrows", +0.0000F, -0.2400F, +0.0360F, 1.0990F); // Trenzas africanas
        addUserStyle("Demi", +0.0000F, -0.1310F, +0.0200F, 1.0200F); // Militar
        addUserStyle("Donny", +0.0000F, -0.1500F, +0.0150F, 1.0100F); // Flequillo
        addUserStyle("FabianCurly", +0.0000F, -0.1790F, +0.0200F, 1.0330F); // Melena larga (Rizado)
        addUserStyle("FlatTop", +0.0000F, -0.1410F, +0.0110F, 1.0200F); // Tapa plana
        addUserStyle("Fresh", +0.0000F, -0.1280F, +0.0060F, 1.0030F); // Rapado
        addUserStyle("GreasedBack", +0.0000F, -0.2900F, +0.0390F, 1.1290F); // Engominado atrás
        addUserStyle("Grungey", +0.0000F, -0.2990F, +0.0450F, 1.1510F); // Grungey
        addUserStyle("Grungey02", +0.0000F, -0.3000F, +0.0460F, 1.1610F); // Grungey largo
        addUserStyle("GrungeyBehindEars", +0.0000F, -0.2220F, +0.0270F, 1.0820F); // Grungey tras las orejas
        addUserStyle("GrungeyParted", +0.0000F, -0.2070F, +0.0300F, 1.0870F); // Grungey separado
        addUserStyle("Hat", +0.0000F, -0.2220F, +0.0530F, 1.0970F); // Corto
        addUserStyle("Kate", +0.0000F, -0.2920F, +0.0560F, 1.1650F); // Largo
        addUserStyle("KateCurly", +0.0000F, -0.2980F, +0.0560F, 1.1650F); // Salmonete (Rizado)
        addUserStyle("LeftParting", +0.0000F, -0.2400F, +0.0350F, 1.0920F); // Raya a la izquierda
        addUserStyle("LibertySpikes", +0.0000F, -0.1440F, +0.0000F, 1.0100F); // Pinchos
        addUserStyle("Long", +0.0000F, -0.2480F, +0.0400F, 1.1200F); // Media melena
        addUserStyle("LongBraids", +0.0000F, -0.1960F, +0.0270F, 1.0630F); // Trenzas largas
        addUserStyle("LongBraids02", +0.0000F, -0.2000F, +0.0270F, 1.0560F); // Trenzas muy largas
        addUserStyle("Longcurly", +0.0000F, -0.2480F, +0.0400F, 1.1200F); // Media melena (Rizado)
        addUserStyle("MessyCurly", +0.0000F, -0.2310F, +0.0280F, 1.0900F); // Revuelto (Rizado)
        addUserStyle("Metal", +0.0000F, +0.0070F, -0.0060F, 0.9960F); // Metalero
        addUserStyle("MohawkFan", +0.0000F, -0.1400F, +0.0100F, 1.0000F); // Mohicano
        addUserStyle("MohawkFlat", +0.0000F, -0.2000F, +0.0220F, 1.0610F); // Mohicano plano
        addUserStyle("MohawkShort", +0.0000F, -0.1720F, +0.0190F, 1.0340F); // Mohicano corto
        addUserStyle("MohawkSpike", +0.0000F, -0.1300F, +0.0120F, 1.0000F); // Mohicano pinchos
        addUserStyle("MulletCurly", +0.0000F, -0.2600F, +0.0400F, 1.1220F); // Salmonete (Rizado)
        addUserStyle("OverEye", +0.0000F, -0.2700F, +0.0410F, 1.1370F); // Flequillo derecho
        addUserStyle("OverEyeCurly", +0.0000F, -0.2720F, +0.0450F, 1.1410F); // Flequillo (Rizado)
        addUserStyle("OverLeftEye", +0.0000F, -0.2390F, +0.0480F, 1.1130F); // Flequillo izquierdo
        addUserStyle("PonyTailBraids", +0.0000F, -0.2560F, +0.0390F, 1.1150F); // Coleta con trenzas
        addUserStyle("Rachel", +0.0010F, -0.1600F, +0.0160F, 1.0400F); // Rachel
        addUserStyle("RachelCurly", +0.0000F, -0.1600F, +0.0160F, 1.0400F); // Rachel (Rizado)
        addUserStyle("Recede", +0.0000F, -0.2300F, +0.0340F, 1.0900F); // Entradas
        addUserStyle("RightParting", +0.0000F, -0.2400F, +0.0350F, 1.0920F); // Raya a la derecha
        addUserStyle("ShortAfroCurly", +0.0000F, -0.1800F, +0.0200F, 1.0300F); // Afro corto
        addUserStyle("ShortCurly", +0.0000F, -0.2000F, +0.0320F, 1.0600F); // Sencillo (Rizado)
        addUserStyle("ShortHatCurly", +0.0000F, -0.2220F, +0.0550F, 1.1000F); // Sencillo (Rizado)
        addUserStyle("Spike", +0.0000F, -0.2100F, +0.0300F, 1.0800F); // Puntiagudo
        addUserStyle("TopCurls", +0.0000F, -0.3000F, +0.0600F, 1.1560F); // Rizado alto


        // FIXED31 exact runtime model tokens for vanilla/PZ geometry.
        // addUserStyle() is kept as a readable fallback, but many PZ style IDs do not
        // equal their mesh filenames (Braids -> bob_hair_braided01, Hat -> m_hair_shorthat, etc.).
        // These exact aliases make the exported final tuning survive restart reliably.
        addUser("f_hair_back", +0.0000F, -0.3000F, +0.0590F, 1.1690F); // PCH_Vanilla_Back -> skinned/hair/f_hair_back
        addUser("f_hair_bob", +0.0000F, -0.2450F, +0.0400F, 1.1150F); // PCH_Vanilla_Bob -> skinned/hair/f_hair_bob
        addUser("kate_hair_bobcurly", +0.0000F, -0.2450F, +0.0450F, 1.1150F); // PCH_Vanilla_BobCurly -> skinned/hair/kate_hair_bobcurly
        addUser("bob_hair_braided01", +0.0000F, -0.2310F, +0.0400F, 1.0910F); // PCH_Vanilla_Braids -> skinned/hair/bob_hair_braided01
        addUser("bob_hair_buffont", +0.0000F, -0.2570F, +0.0370F, 1.1050F); // PCH_Vanilla_Buffont -> skinned/hair/bob_hair_buffont
        addUser("f_hair_bun", +0.0000F, -0.2950F, +0.0620F, 1.1750F); // PCH_Vanilla_Bun -> skinned/hair/f_hair_bun
        addUser("kate_hair_buncurly", +0.0000F, -0.2950F, +0.0620F, 1.1750F); // PCH_Vanilla_BunCurly -> skinned/hair/kate_hair_buncurly
        addUser("bob_hair_partingcentre", +0.0000F, -0.2540F, +0.0360F, 1.1100F); // PCH_Vanilla_CentreParting -> skinned/hair/bob_hair_partingcentre
        addUser("bob_hair_partingcentrelong", +0.0000F, -0.2490F, +0.0390F, 1.1060F); // PCH_Vanilla_CentrePartingLong -> skinned/hair/bob_hair_partingcentrelong
        addUser("bob_hair_cornrows", +0.0000F, -0.2400F, +0.0360F, 1.0990F); // PCH_Vanilla_Cornrows -> skinned/hair/bob_hair_cornrows
        addUser("f_hair_demi", +0.0000F, -0.1310F, +0.0200F, 1.0200F); // PCH_Vanilla_Demi -> skinned/hair/f_hair_demi
        addUser("m_hair_donny", +0.0000F, -0.1500F, +0.0150F, 1.0100F); // PCH_Vanilla_Donny -> skinned/hair/m_hair_donny
        addUser("bob_hair_fabiancurly", +0.0000F, -0.1790F, +0.0200F, 1.0330F); // PCH_Vanilla_FabianCurly -> skinned/hair/bob_hair_fabiancurly
        addUser("bob_hair_flattop", +0.0000F, -0.1410F, +0.0110F, 1.0200F); // PCH_Vanilla_FlatTop -> skinned/hair/bob_hair_flattop
        addUser("bob_hair_fresh", +0.0000F, -0.1280F, +0.0060F, 1.0030F); // PCH_Vanilla_Fresh -> skinned/hair/bob_hair_fresh
        addUser("bob_hair_greasedback", +0.0000F, -0.2900F, +0.0390F, 1.1290F); // PCH_Vanilla_GreasedBack -> skinned/hair/bob_hair_greasedback
        addUser("bob_hair_grungey", +0.0000F, -0.2990F, +0.0450F, 1.1510F); // PCH_Vanilla_Grungey -> skinned/hair/bob_hair_grungey
        addUser("kate_hair_grungey02", +0.0000F, -0.3000F, +0.0460F, 1.1610F); // PCH_Vanilla_Grungey02 -> skinned/hair/kate_hair_grungey02
        addUser("bob_hair_grungeybehindear", +0.0000F, -0.2220F, +0.0270F, 1.0820F); // PCH_Vanilla_GrungeyBehindEars -> skinned/hair/bob_hair_grungeybehindear
        addUser("kate_hair_grungeyparting", +0.0000F, -0.2070F, +0.0300F, 1.0870F); // PCH_Vanilla_GrungeyParted -> skinned/hair/kate_hair_grungeyparting
        addUser("m_hair_shorthat", +0.0000F, -0.2220F, +0.0530F, 1.0970F); // PCH_Vanilla_Hat -> skinned/hair/m_hair_shorthat
        addUser("f_hair_kate", +0.0000F, -0.2920F, +0.0560F, 1.1650F); // PCH_Vanilla_Kate -> skinned/hair/f_hair_kate
        addUser("kate_hair_katecurly", +0.0000F, -0.2980F, +0.0560F, 1.1650F); // PCH_Vanilla_KateCurly -> skinned/hair/kate_hair_katecurly
        addUser("bob_hair_partingleft", +0.0000F, -0.2400F, +0.0350F, 1.0920F); // PCH_Vanilla_LeftParting -> skinned/hair/bob_hair_partingleft
        addUser("bob_hair_libertyspikes", +0.0000F, -0.1440F, +0.0000F, 1.0100F); // PCH_Vanilla_LibertySpikes -> skinned/hair/bob_hair_libertyspikes
        addUser("kate_hair_long", +0.0000F, -0.2480F, +0.0400F, 1.1200F); // PCH_Vanilla_Long -> skinned/hair/kate_hair_long
        addUser("bob_hair_longbraided", +0.0000F, -0.1960F, +0.0270F, 1.0630F); // PCH_Vanilla_LongBraids -> skinned/hair/bob_hair_longbraided
        addUser("bob_hair_fabianbraided", +0.0000F, -0.2000F, +0.0270F, 1.0560F); // PCH_Vanilla_LongBraids02 -> skinned/hair/bob_hair_fabianbraided
        addUser("kate_hair_longcurly", +0.0000F, -0.2480F, +0.0400F, 1.1200F); // PCH_Vanilla_Longcurly -> skinned/hair/kate_hair_longcurly
        addUser("bob_hair_messycurly", +0.0000F, -0.2310F, +0.0280F, 1.0900F); // PCH_Vanilla_MessyCurly -> skinned/hair/bob_hair_messycurly
        addUser("m_hair_metal", +0.0000F, +0.0070F, -0.0060F, 0.9960F); // PCH_Vanilla_Metal -> skinned/hair/m_hair_metal
        addUser("bob_hair_mohawkfan", +0.0000F, -0.1400F, +0.0100F, 1.0000F); // PCH_Vanilla_MohawkFan -> skinned/hair/bob_hair_mohawkfan
        addUser("bob_hair_mohawkflat", +0.0000F, -0.2000F, +0.0220F, 1.0610F); // PCH_Vanilla_MohawkFlat -> skinned/hair/bob_hair_mohawkflat
        addUser("bob_hair_mohawkshort", +0.0000F, -0.1720F, +0.0190F, 1.0340F); // PCH_Vanilla_MohawkShort -> skinned/hair/bob_hair_mohawkshort
        addUser("bob_hair_mohawkspike", +0.0000F, -0.1300F, +0.0120F, 1.0000F); // PCH_Vanilla_MohawkSpike -> skinned/hair/bob_hair_mohawkspike
        addUser("bob_hair_mulletcurly", +0.0000F, -0.2600F, +0.0400F, 1.1220F); // PCH_Vanilla_MulletCurly -> skinned/hair/bob_hair_mulletcurly
        addUser("f_hair_overeye", +0.0000F, -0.2700F, +0.0410F, 1.1370F); // PCH_Vanilla_OverEye -> skinned/hair/f_hair_overeye
        addUser("kate_hair_overeyecurly", +0.0000F, -0.2720F, +0.0450F, 1.1410F); // PCH_Vanilla_OverEyeCurly -> skinned/hair/kate_hair_overeyecurly
        addUser("kate_hair_overeyeleft", +0.0000F, -0.2390F, +0.0480F, 1.1130F); // PCH_Vanilla_OverLeftEye -> skinned/hair/kate_hair_overeyeleft
        addUser("bob_hair_ponytailbraided", +0.0000F, -0.2560F, +0.0390F, 1.1150F); // PCH_Vanilla_PonyTailBraids -> skinned/hair/bob_hair_ponytailbraided
        addUser("kate_hair_rachel", +0.0010F, -0.1600F, +0.0160F, 1.0400F); // PCH_Vanilla_Rachel -> skinned/hair/kate_hair_rachel
        addUser("kate_hair_rachelcurly", +0.0000F, -0.1600F, +0.0160F, 1.0400F); // PCH_Vanilla_RachelCurly -> skinned/hair/kate_hair_rachelcurly
        addUser("m_hair_recede", +0.0000F, -0.2300F, +0.0340F, 1.0900F); // PCH_Vanilla_Recede -> skinned/hair/m_hair_recede
        addUser("bob_hair_partingright", +0.0000F, -0.2400F, +0.0350F, 1.0920F); // PCH_Vanilla_RightParting -> skinned/hair/bob_hair_partingright
        addUser("bob_hair_shortcurly", +0.0000F, -0.1800F, +0.0200F, 1.0300F); // PCH_Vanilla_ShortAfroCurly -> skinned/hair/bob_hair_shortcurly
        addUser("kate_hair_shortcurly", +0.0000F, -0.2000F, +0.0320F, 1.0600F); // PCH_Vanilla_ShortCurly -> skinned/hair/kate_hair_shortcurly
        addUser("bob_hair_shorthatcurly", +0.0000F, -0.2220F, +0.0550F, 1.1000F); // PCH_Vanilla_ShortHatCurly -> skinned/hair/bob_hair_shorthatcurly
        addUser("bob_hair_spike", +0.0000F, -0.2100F, +0.0300F, 1.0800F); // PCH_Vanilla_Spike -> skinned/hair/bob_hair_spike
        addUser("kate_hair_topmop", +0.0000F, -0.3000F, +0.0600F, 1.1560F); // PCH_Vanilla_TopCurls -> skinned/hair/kate_hair_topmop
    }

    static {
        // FIXED37: latest CHILD tuning progress supplied after FIXED36.
        // These values remain CHILD-only; TEEN starts from its own independent maps.
        addUser("child_harry_m_aiteron", +0.0000F, +0.1200F, +0.0000F, 1.0000F); // PCH_Harry_M_Aiteron | Aiteron
        addUser("ttrp_alan", +0.0000F, -0.2510F, +0.0400F, 1.1160F); // PCH_TTRP_M_Alan | Alan
        addUser("ttrp_alistair", +0.0020F, -0.3000F, +0.0660F, 1.0850F); // PCH_TTRP_M_Alistair | Alistair
        addUser("ttrp_anakin", +0.0000F, -0.2020F, +0.0270F, 1.0290F); // PCH_TTRP_M_Anakin | Anakin
        addUser("ttrp_anderson", +0.0000F, -0.2030F, +0.0260F, 1.0700F); // PCH_TTRP_M_Anderson | Anderson
        addUser("ttrp_astarion", +0.0000F, -0.1900F, +0.0080F, 1.0500F); // PCH_TTRP_M_Astarion | Astarion
        addUser("ttrp_beau", +0.0000F, -0.1500F, +0.0000F, 1.0300F); // PCH_TTRP_M_Beau | Beau
        addUser("ttrp_bedhead", +0.0000F, -0.1960F, +0.0340F, 1.0610F); // PCH_TTRP_M_Bedhead | Bedhead
        addUser("ttrp_billy", +0.0000F, -0.2130F, +0.0240F, 1.0800F); // PCH_TTRP_M_Billy | Billy
        addUser("ttrp_boylover", +0.0000F, -0.2100F, +0.0400F, 1.0400F); // PCH_TTRP_M_Boylover | Boylover
        addUser("ttrp_bridgerton", +0.0000F, -0.2890F, +0.0450F, 1.1400F); // PCH_TTRP_M_Bridgerton | Bridgerton
        addUser("ttrp_broccolitop", +0.0000F, -0.2150F, +0.0350F, 1.0810F); // PCH_TTRP_M_BroccoliTop | BroccoliTop
        addUser("ttrp_buddyboy", +0.0000F, -0.3000F, +0.0700F, 1.0800F); // PCH_TTRP_M_BuddyBoy | BuddyBoy
        addUser("ttrp_bunny", +0.0000F, -0.2150F, +0.0450F, 1.0300F); // PCH_TTRP_M_Bunny | Bunny
        addUser("ttrp_bushhead", +0.0000F, -0.2430F, +0.0350F, 1.0450F); // PCH_TTRP_M_BushHead | BushHead
        addUser("ttrp_butch", +0.0000F, -0.2800F, +0.0590F, 1.0500F); // PCH_TTRP_M_Butch | Butch
        addUser("ttrp_buzz", +0.0000F, -0.3000F, +0.0500F, 1.1300F); // PCH_TTRP_M_Buzz | Buzz
        addUser("ttrp_candyking", +0.0000F, -0.2500F, +0.0360F, 1.1100F); // PCH_TTRP_M_CandyKing | CandyKing
        addUser("ttrp_centerpart", +0.0000F, -0.2400F, +0.0210F, 1.1000F); // PCH_TTRP_M_Centerpart | Centerpart
        addUser("ttrp_charming", +0.0000F, -0.2400F, +0.0290F, 1.1000F); // PCH_TTRP_M_Charming | Charming
        addUser("ttrp_dixon", +0.0000F, -0.1660F, +0.0080F, 1.0220F); // PCH_TTRP_M_Dixon | Dixon
        addUser("ttrp_ruder", -0.0020F, -0.2310F, +0.0610F, 1.0670F); // PCH_TTRP_M_Ruder | Ruder
        addUser("ttrp_slickedbusiness", +0.0000F, -0.2500F, +0.0430F, 1.0550F); // PCH_TTRP_M_SlickedBusiness | SlickedBusiness
        addUser("ttrp_spikyshort", +0.0000F, -0.2000F, +0.0200F, 1.0630F); // PCH_TTRP_M_Spikyshort | Spikyshort
        addUser("ttrp_surferboy", +0.0000F, -0.1620F, +0.0120F, 1.0000F); // PCH_TTRP_M_SurferBoy | SurferBoy
        addUser("ttrp_surferlongbun", +0.0000F, -0.1320F, +0.0040F, 1.0050F); // PCH_TTRP_M_SurferLongBun | SurferLongBun
        addUser("ttrp_thebrandon", +0.0000F, -0.1800F, +0.0360F, 1.0600F); // PCH_TTRP_M_TheBrandon | TheBrandon
        addUser("ttrp_thegeoff", +0.0000F, -0.2400F, +0.0520F, 1.1000F); // PCH_TTRP_M_TheGeoff | TheGeoff
        addUser("ttrp_trentan", +0.0000F, -0.1780F, +0.0270F, 1.0490F); // PCH_TTRP_M_Trentan | Trentan
        addUser("ttrp_trevor", +0.0000F, -0.1920F, +0.0280F, 1.0630F); // PCH_TTRP_M_Trevor | Trevor
        addUser("ttrp_tryndamere", +0.0000F, -0.3000F, +0.0560F, 1.0900F); // PCH_TTRP_M_Tryndamere | Tryndamere
        addUser("ttrp_tuckedcenterpart", +0.0000F, -0.2220F, +0.0300F, 1.0860F); // PCH_TTRP_M_TuckedCenterPart | TuckedCenterPart
        addUser("ttrp_twink", +0.0000F, -0.1880F, +0.0340F, 1.0050F); // PCH_TTRP_M_Twink | Twink
        addUser("ttrp_uggy", +0.0000F, -0.3000F, +0.0670F, 1.0880F); // PCH_TTRP_M_Uggy | Uggy
        addUser("ttrp_victor", +0.0000F, -0.1490F, +0.0060F, 1.0200F); // PCH_TTRP_M_Victor | Victor
        addUser("ttrp_vladimir", +0.0010F, -0.1960F, +0.0350F, 1.0300F); // PCH_TTRP_M_Vladimir | Vladimir
        addUser("ttrp_alex", +0.0000F, -0.1750F, +0.0300F, 1.0500F); // PCH_TTRP_F_Alex | Alex
        addUser("ttrp_alma", +0.0000F, -0.3000F, +0.1000F, 1.1990F); // PCH_TTRP_F_Alma | Alma
        addUser("ttrp_almostkaren", +0.0000F, -0.2290F, +0.0410F, 1.0440F); // PCH_TTRP_F_AlmostKaren | AlmostKaren
        addUser("ttrp_amalia", +0.0000F, -0.2420F, +0.0400F, 1.0650F); // PCH_TTRP_F_Amalia | Amalia
        addUser("ttrp_ambi", +0.0000F, -0.2670F, +0.0550F, 1.1170F); // PCH_TTRP_F_Ambi | Ambi
        addUser("ttrp_angela", +0.0000F, -0.2970F, +0.0600F, 1.1280F); // PCH_TTRP_F_Angela | Angela
        addUser("ttrp_aoba", +0.0000F, -0.1600F, +0.0200F, 1.0400F); // PCH_TTRP_F_Aoba | Aoba
        addUser("ttrp_aponi", +0.0000F, -0.3000F, +0.0640F, 1.1220F); // PCH_TTRP_F_Aponi | Aponi
        addUser("ttrp_arabella", +0.0000F, -0.2910F, +0.0600F, 1.1000F); // PCH_TTRP_F_Arabella | Arabella
        addUser("ttrp_ari", +0.0020F, -0.3000F, +0.0560F, 1.1090F); // PCH_TTRP_F_Ari | Ari
        addUser("ttrp_armani", +0.0000F, -0.3000F, +0.0580F, 1.1280F); // PCH_TTRP_F_Armani | Armani
        addUser("ttrp_aurora", +0.0000F, -0.2770F, +0.0490F, 1.1370F); // PCH_TTRP_F_Aurora | Aurora
        addUser("ttrp_autumn", +0.0000F, -0.2800F, +0.0600F, 1.0800F); // PCH_TTRP_F_Autumn | Autumn
        addUser("ttrp_ava", +0.0000F, -0.2430F, +0.0380F, 1.1030F); // PCH_TTRP_F_Ava | Ava
        addUser("ttrp_aylani", +0.0000F, -0.2200F, +0.0460F, 1.0530F); // PCH_TTRP_F_Aylani | Aylani
        addUser("ttrp_becky", +0.0020F, -0.1200F, +0.0000F, 1.0050F); // PCH_TTRP_F_Becky | Becky
        addUser("ttrp_bella", +0.0000F, -0.2600F, +0.0400F, 1.1200F); // PCH_TTRP_F_Bella | Bella
        addUser("ttrp_beth", +0.0000F, -0.1400F, +0.0000F, 1.0000F); // PCH_TTRP_F_Beth | Beth
        addUser("ttrp_betsy", +0.0000F, -0.1600F, +0.0000F, 1.0000F); // PCH_TTRP_F_Betsy | Betsy
        addUser("ttrp_blossom", +0.0000F, -0.1000F, +0.0000F, 1.0000F); // PCH_TTRP_F_Blossom | Blossom
        addUser("ttrp_bunmaam", +0.0000F, -0.1200F, +0.0400F, 1.0000F); // PCH_TTRP_F_BunMaam | BunMaam
        addUser("ttrp_camilla", +0.0000F, -0.1200F, +0.0000F, 1.0000F); // PCH_TTRP_F_Camilla | Camilla
        addUser("ttrp_caroline", +0.0000F, -0.1400F, +0.0200F, 1.0000F); // PCH_TTRP_F_Caroline | Caroline
        addUser("ttrp_caroliney", +0.0000F, -0.1200F, +0.0000F, 1.0000F); // PCH_TTRP_F_Caroliney | Caroliney
        addUser("ttrp_cassie", +0.0000F, -0.1000F, +0.0400F, 1.0000F); // PCH_TTRP_F_Cassie | Cassie
    }



    static {
        // FIXED40: latest CHILD / SIN GORRA export supplied after validating
        // the FIXED38 local-space hair movement correction. Duplicate style IDs
        // were collapsed using the LAST occurrence in the export.
        addChildStyleExtra("PCH_Harry_F_BRITA", +0.0000F, -0.0030F, -0.0640F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Aigis", +0.0000F, -0.0190F, +0.0020F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_AigisAlt", +0.0000F, -0.0180F, +0.0000F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Carlos", +0.0000F, -0.0070F, -0.0070F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Goth", +0.0000F, -0.0100F, -0.0250F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_MitsuruPonytail", +0.0000F, -0.0050F, -0.0230F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_P3Hero", +0.0000F, -0.0120F, -0.0050F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Redfield", +0.0000F, -0.0140F, -0.0360F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Rick", +0.0000F, -0.0530F, -0.0140F, 1.0390F);
        addChildStyleExtra("PCH_Spn_M_Valentine", +0.0000F, -0.0120F, -0.0170F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Back", +0.0000F, -0.0140F, -0.0700F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Bob", +0.0000F, +0.0000F, -0.0450F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_BobCurly", +0.0000F, +0.0000F, -0.0500F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Braids", +0.0000F, -0.0050F, -0.0540F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Buffont", +0.0000F, +0.0000F, -0.0600F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Bun", +0.0000F, -0.0110F, -0.0710F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_BunCurly", +0.0000F, -0.0080F, -0.0680F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_CentreParting", +0.0000F, -0.0060F, -0.0630F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_CentrePartingLong", +0.0000F, +0.0000F, -0.0600F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Cornrows", +0.0000F, +0.0000F, -0.0600F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Demi", +0.0000F, +0.0000F, -0.0280F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_FlatTop", +0.0000F, -0.0100F, -0.0330F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Fresh", +0.0000F, -0.0040F, -0.0300F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_GreasedBack", +0.0000F, +0.0000F, -0.0650F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Grungey", +0.0000F, +0.0000F, -0.0650F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Grungey02", +0.0000F, +0.0000F, -0.0700F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_GrungeyBehindEars", +0.0000F, +0.0000F, -0.0450F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_GrungeyParted", +0.0000F, +0.0000F, -0.0500F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Kate", +0.0000F, -0.0050F, -0.0700F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_KateCurly", +0.0000F, -0.0100F, -0.0750F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_LeftParting", +0.0000F, -0.0020F, -0.0600F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_LibertySpikes", +0.0000F, +0.0000F, -0.0240F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Long", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_LongBraids", +0.0000F, +0.0020F, -0.0470F, 0.9910F);
        addChildStyleExtra("PCH_Vanilla_LongBraids02", +0.0000F, +0.0140F, -0.0520F, 0.9840F);
        addChildStyleExtra("PCH_Vanilla_Longcurly", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_MohawkFan", +0.0000F, +0.0000F, -0.0300F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_MohawkFlat", +0.0000F, -0.0030F, -0.0450F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_MohawkShort", +0.0000F, +0.0000F, -0.0400F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_MohawkSpike", +0.0000F, +0.0000F, -0.0300F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_OverEye", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_OverEyeCurly", +0.0000F, +0.0000F, -0.0600F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_OverLeftEye", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_PonyTail", +0.0000F, +0.0000F, -0.0650F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_PonyTailBraids", +0.0000F, +0.0000F, -0.0650F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Rachel", +0.0000F, -0.0050F, -0.0350F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_RachelCurly", +0.0000F, -0.0080F, -0.0390F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_RightParting", +0.0000F, -0.0020F, -0.0570F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_ShortCurly", +0.0000F, -0.0050F, -0.0500F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Spike", +0.0000F, -0.0040F, -0.0500F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_TopCurls", +0.0000F, -0.1080F, -0.0810F, 1.0710F);
        addChildStyleExtra("PCH_Spn_M_ErenBun", +0.0000F, -0.0050F, -0.0320F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_ErenParted", +0.0000F, -0.0070F, -0.0240F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Leon", +0.0000F, -0.0040F, -0.0170F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Marie", +0.0000F, -0.0070F, -0.0140F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Mikasa", +0.0000F, -0.0110F, +0.0030F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_MikasaShort", +0.0000F, -0.0040F, +0.0010F, 1.0000F);
        addChildStyleExtra("F_Hair_Cortana", +0.0000F, +0.2700F, +0.2540F, 1.0020F);
        addChildStyleExtra("F_Hair_Lexie", +0.0000F, +0.2810F, +0.2130F, 0.9600F);
        addChildStyleExtra("F_Hair_Maria", +0.0000F, +0.2910F, +0.2730F, 1.0000F);
        addChildStyleExtra("F_Hair_PonyTailBangs", +0.0000F, +0.2910F, +0.2820F, 1.0000F);
        addChildStyleExtra("F_Hair_PonyTailGeralt", +0.0000F, +0.2850F, +0.2800F, 1.0000F);
        addChildStyleExtra("F_Hair_PonyTailMary", +0.0000F, +0.2810F, +0.2720F, 1.0060F);
        addChildStyleExtra("F_Hair_PonyTail_Raider", +0.0000F, +0.2900F, +0.2840F, 1.0140F);
        addChildStyleExtra("F_Hair_Selene", +0.0000F, +0.2700F, +0.2500F, 1.0000F);
        addChildStyleExtra("F_Hair_SmoothLong", +0.0000F, +0.2990F, +0.2720F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Taurie", +0.0000F, -0.0090F, -0.0170F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_AfroBun", +0.0000F, +0.2800F, +0.2200F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_BraidedPonytail", +0.0000F, -0.0100F, -0.0670F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_CheerLeader", +0.0000F, +0.3000F, +0.2470F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_Classy", +0.0000F, +0.0000F, -0.0360F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_ClippedHair", +0.0000F, +0.2500F, +0.1970F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_CroftHair", +0.0000F, -0.0040F, -0.0580F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_DoubleBraids", +0.0000F, -0.0100F, -0.0600F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_Dreadlocks", +0.0000F, +0.2200F, +0.1700F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_HairBun", +0.0000F, +0.2300F, +0.1800F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_HalfUpShaved", +0.0000F, -0.0090F, -0.0450F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_LowPonytail", +0.0000F, -0.0060F, -0.1020F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_Pigtails", +0.0000F, +0.2280F, +0.1750F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_Rochefort", +0.0000F, -0.0100F, -0.0330F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_SideBraid", +0.0000F, -0.0060F, -0.0430F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_TiedHalfDown", +0.0000F, +0.2500F, +0.2040F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_Undercut", +0.0000F, +0.0040F, -0.0620F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_UndercutCurly", +0.0000F, -0.0130F, -0.0480F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_Yaki", +0.0000F, -0.0050F, -0.0550F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Agent", +0.0000F, +0.0000F, -0.0190F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_AlanWake", +0.0000F, -0.0010F, -0.0140F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Ash", +0.0000F, +0.0000F, -0.0160F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_AshBowl", +0.0000F, -0.0070F, +0.0000F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_AshShort", +0.0000F, -0.0050F, -0.0200F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_AshSlick", +0.0000F, -0.0030F, -0.0170F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Ashley", +0.0000F, -0.0070F, +0.0000F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_BobSlick", +0.0000F, -0.0030F, -0.0200F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Curtains", +0.0000F, -0.0040F, -0.0140F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_CurtainsHigh", +0.0000F, +0.0000F, -0.0110F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Dante", +0.0000F, -0.0020F, -0.0020F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_DanteParted", +0.0000F, -0.0160F, -0.0040F, 1.0100F);
        addChildStyleExtra("PCH_Spn_M_Frank", +0.0000F, -0.0080F, -0.0150F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Hero", +0.0000F, -0.0080F, -0.0250F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_HeroBun", +0.0000F, -0.0010F, -0.0410F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_HeroTail", +0.0000F, -0.0050F, -0.0260F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Jeanette", +0.0000F, -0.0020F, -0.0290F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Kotone", +0.0000F, -0.0050F, -0.0140F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_KotonePonytail", +0.0000F, -0.0110F, -0.0160F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Kusanagi", +0.0000F, +0.0000F, -0.0210F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_LeonLong", +0.0000F, -0.0010F, +0.0000F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_LeonRE4", +0.0000F, -0.0020F, -0.0180F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Logan", +0.0000F, -0.0030F, -0.0020F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_MessyBob", +0.0000F, -0.0060F, +0.0000F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_OvereyeShaggy", +0.0000F, +0.0000F, -0.0150F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Parted", +0.0000F, -0.0010F, -0.0190F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_PartedMedium", +0.0000F, -0.0040F, -0.0200F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_PartedSide", +0.0000F, +0.0030F, -0.0080F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_PartedSideMedium", +0.0000F, -0.0040F, -0.0160F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_PartedTail", +0.0000F, -0.0050F, -0.0200F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Power", +0.0000F, -0.0070F, +0.0000F, 0.9900F);
        addChildStyleExtra("PCH_Spn_M_PowerTied", +0.0000F, -0.0050F, -0.0150F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Princely", +0.0000F, -0.0050F, -0.0360F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_PrincelyShort", +0.0000F, +0.0060F, -0.0120F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_RedfieldLow", +0.0000F, -0.0040F, -0.0180F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_RedfieldMessy", +0.0000F, -0.0040F, -0.0180F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Rei", +0.0000F, -0.0140F, +0.0000F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_RickShow", +0.0000F, -0.0130F, -0.0100F, 1.0100F);
        addChildStyleExtra("PCH_Spn_M_Shaggy", +0.0000F, -0.0050F, +0.0000F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_ShaggyCurly", +0.0000F, -0.0090F, +0.0000F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Sideswept", +0.0000F, -0.0040F, -0.0230F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_SpikedForward", +0.0000F, -0.0090F, -0.0200F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_SpikedMessy", +0.0000F, -0.0040F, -0.0280F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_SpikedUp", +0.0000F, +0.0000F, -0.0190F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_StraightCutBob", +0.0000F, -0.0020F, -0.0050F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Surfer", +0.0000F, -0.0060F, -0.0050F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Touka", +0.0000F, -0.0080F, -0.0100F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_ToukaStraight", +0.0000F, -0.0030F, -0.0130F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Vergil", +0.0000F, -0.0030F, -0.0060F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Vi", +0.0000F, +0.0000F, -0.0060F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Yu", +0.0000F, -0.0070F, -0.0200F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Yukari", +0.0000F, +0.0000F, -0.0100F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_YukariAlt", +0.0000F, -0.0080F, -0.0190F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_YukariBun", +0.0000F, -0.0040F, -0.0170F, 1.0070F);
        addChildStyleExtra("PCH_Spn_M_Zarina", +0.0000F, -0.0040F, -0.0210F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_ZarinaCurly", +0.0000F, -0.0070F, -0.0220F, 1.0000F);

        // Styles reported invisible after FIXED38. None of these were intentionally
        // removed from the catalog. Harry MegaMullet and Yaki VeryLongParted remain
        // the separately removed styles.
        LOCALSPACE_LEGACY_RESET_STYLES.add("pch_harry_f_grunge01");
        LOCALSPACE_LEGACY_RESET_STYLES.add("pch_harry_f_ponytaillong");
        LOCALSPACE_LEGACY_RESET_STYLES.add("pch_harry_f_merigold");
        LOCALSPACE_LEGACY_RESET_STYLES.add("pch_harry_f_ponytailmedium");
        LOCALSPACE_LEGACY_RESET_STYLES.add("pch_harry_f_throttlekitty");
    }

    static {
        // FIXED41: latest CHILD deltas supplied after FIXED40.
        // SIN GORRA values are composed with the already-baked FIXED40 extras.
        addChildStyleExtra("M_Hair_Daper", +0.0000F, +0.2930F, +0.2810F, 1.0030F);
        addChildStyleExtra("M_Hair_Geralt", +0.0000F, +0.2880F, +0.2660F, 1.0000F);
        addChildStyleExtra("M_Hair_James", +0.0000F, +0.2550F, +0.2290F, 1.0000F);
        addChildStyleExtra("M_Hair_Ranger", +0.0000F, +0.2200F, +0.1940F, 0.9890F);
        addChildStyleExtra("PCH_Harry_F_Grunge01", +0.0000F, +0.0000F, +0.0020F, 1.2050F);
        addChildStyleExtra("PCH_Harry_F_Merigold", +0.0000F, +0.0000F, +0.0020F, 1.1940F);
        addChildStyleExtra("PCH_Harry_F_PonytailLong", +0.0000F, +0.0000F, +0.0020F, 1.1970F);
        addChildStyleExtra("PCH_Harry_F_PonytailMedium", +0.0000F, +0.0000F, +0.0030F, 1.2960F);
        addChildStyleExtra("PCH_Harry_F_ThrottleKitty", +0.0000F, +0.0000F, +0.0000F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Frank", +0.0000F, -0.0070F, -0.0180F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_OvereyeShaggyLeft", +0.0000F, +0.0000F, -0.0150F, 1.0000F);
        addChildStyleExtra("PCH_Spn_M_Power", +0.0000F, -0.0040F, -0.0040F, 0.9900F);
        addChildStyleExtra("PCH_Spn_M_PowerTied", +0.0000F, -0.0050F, -0.0160F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Back", +0.0000F, -0.0260F, -0.0790F, 1.0050F);
        addChildStyleExtra("PCH_Vanilla_Bun", +0.0000F, -0.0160F, -0.0760F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_BunCurly", +0.0000F, -0.0130F, -0.0780F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_CrewCut", +0.0000F, +0.0000F, -0.0310F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Donny", +0.0000F, +0.0000F, -0.0230F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Fabian", +0.0000F, +0.0110F, -0.0360F, 0.9830F);
        addChildStyleExtra("PCH_Vanilla_FabianCurly", +0.0000F, +0.0040F, -0.0450F, 0.9930F);
        addChildStyleExtra("PCH_Vanilla_Hat", +0.0000F, -0.0060F, -0.0550F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Messy", +0.0000F, +0.0000F, -0.0650F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_MessyCurly", +0.0000F, -0.0020F, -0.0560F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Metal", +0.0000F, -0.1050F, -0.0120F, 0.9800F);
        addChildStyleExtra("PCH_Vanilla_Mullet", +0.0000F, -0.0060F, -0.0530F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_MulletCurly", +0.0000F, +0.0000F, -0.0700F, 0.9920F);
        addChildStyleExtra("PCH_Vanilla_Recede", +0.0000F, -0.0060F, -0.0530F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_Short", +0.0000F, -0.0010F, -0.0530F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_ShortAfroCurly", +0.0000F, -0.0030F, -0.0360F, 1.0000F);
        addChildStyleExtra("PCH_Vanilla_ShortHatCurly", +0.0000F, -0.0110F, -0.0570F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_Classy", +0.0000F, +0.0000F, -0.0360F, 1.0000F);
        addChildStyleExtra("PCH_Yaki_DoubleBraids", +0.0000F, -0.0100F, -0.0600F, 1.0000F);

        // FIXED41: exact rendered compressed/hat meshes.  Shared raw mesh keys
        // automatically apply to both CHILD sexes; F_Hair_* twins may inherit
        // the matching M_Hair_* key through findChildHatExtra().
        addChildHatExtra("bob_hair_ponytailbraided", +0.0000F, +0.0000F, +0.0000F, 0.9930F);
        addChildHatExtra("bob_hair_shorthatcurly", +0.0000F, -0.0070F, -0.0610F, 0.9960F);
        addChildHatExtra("child_spn_m_agentg1", +0.0000F, +0.0000F, -0.0380F, 1.0000F);
        addChildHatExtra("child_spn_m_aigisg1", +0.0000F, -0.0190F, -0.0230F, 1.0240F);
        addChildHatExtra("child_spn_m_alanwakeg1", +0.0000F, +0.0300F, +0.0020F, 0.9840F);
        addChildHatExtra("child_spn_m_ashg1", +0.0000F, +0.0000F, -0.0200F, 1.0000F);
        addChildHatExtra("child_spn_m_ashg4", +0.0000F, +0.0000F, -0.0200F, 1.0000F);
        addChildHatExtra("child_spn_m_ashleyg1", +0.0000F, +0.0000F, -0.0230F, 1.0040F);
        addChildHatExtra("child_spn_m_ashslickg1", +0.0000F, +0.0000F, -0.0350F, 1.0000F);
        addChildHatExtra("child_spn_m_bobslickg1", +0.0000F, +0.0000F, -0.0370F, 1.0000F);
        addChildHatExtra("child_spn_m_carlosg1", +0.0000F, -0.0300F, -0.0350F, 1.0290F);
        addChildHatExtra("child_spn_m_curtainsg1", +0.0000F, +0.0000F, -0.0340F, 1.0000F);
        addChildHatExtra("child_spn_m_curtainshighg1", +0.0000F, +0.0000F, -0.0400F, 1.0000F);
        addChildHatExtra("child_spn_m_danteg1", +0.0000F, -0.0450F, -0.0140F, 1.0500F);
        addChildHatExtra("child_spn_m_erenbung1", +0.0000F, +0.0000F, -0.0590F, 1.0000F);
        addChildHatExtra("child_spn_m_erenpartedg1", +0.0000F, +0.0000F, -0.0400F, 1.0000F);
        addChildHatExtra("child_spn_m_gothg1", +0.0000F, +0.0000F, -0.0500F, 1.0000F);
        addChildHatExtra("child_spn_m_herobung1", +0.0000F, +0.0000F, -0.0700F, 1.0000F);
        addChildHatExtra("child_spn_m_herog1", +0.0000F, +0.0000F, -0.0470F, 1.0000F);
        addChildHatExtra("child_spn_m_herotailg1", +0.0000F, +0.0000F, -0.0500F, 1.0000F);
        addChildHatExtra("child_spn_m_jeanetteg1", +0.0000F, +0.0000F, -0.0530F, 1.0000F);
        addChildHatExtra("child_spn_m_kotoneg1", +0.0000F, +0.0000F, -0.0260F, 1.0000F);
        addChildHatExtra("child_spn_m_kotoneponytailg1", +0.0000F, +0.0000F, -0.0440F, 1.0000F);
        addChildHatExtra("child_spn_m_kusanagig1", +0.0000F, -0.0010F, -0.0350F, 0.9910F);
        addChildHatExtra("child_spn_m_leong1", +0.0000F, +0.0010F, -0.0440F, 1.0000F);
        addChildHatExtra("child_spn_m_leonlongg1", +0.0000F, -0.0310F, -0.0380F, 1.0340F);
        addChildHatExtra("child_spn_m_logang1", +0.0000F, +0.0000F, -0.0120F, 1.0000F);
        addChildHatExtra("child_spn_m_makotog1", +0.0000F, -0.0110F, -0.0360F, 1.0150F);
        addChildHatExtra("child_spn_m_marieg4", +0.0000F, +0.0000F, -0.0350F, 1.0000F);
        addChildHatExtra("child_spn_m_messybobg1", +0.0000F, +0.0000F, -0.0250F, 1.0000F);
        addChildHatExtra("child_spn_m_mikasag1", +0.0000F, +0.0070F, -0.0090F, 1.0000F);
        addChildHatExtra("child_spn_m_mikasashortg1", +0.0000F, +0.0310F, -0.0110F, 0.9520F);
        addChildHatExtra("child_spn_m_mitsuruponytailg1", +0.0000F, +0.0000F, -0.0440F, 1.0000F);
        addChildHatExtra("child_spn_m_overeyeshaggyg1", +0.0000F, +0.0000F, -0.0390F, 1.0080F);
        addChildHatExtra("child_spn_m_overeyeshaggyleftg1", +0.0000F, +0.0000F, -0.0380F, 1.0070F);
        addChildHatExtra("child_spn_m_partedg1", +0.0000F, +0.0050F, -0.0350F, 1.0000F);
        addChildHatExtra("child_spn_m_partedmediumg1", +0.0000F, -0.0120F, -0.0290F, 1.0140F);
        addChildHatExtra("child_spn_m_partedsideg1", +0.0000F, +0.0000F, -0.0350F, 0.9970F);
        addChildHatExtra("child_spn_m_partedsidemediumg1", +0.0000F, +0.0000F, -0.0390F, 1.0080F);
        addChildHatExtra("child_spn_m_partedtailg1", +0.0000F, +0.0000F, -0.0450F, 1.0000F);
        addChildHatExtra("child_spn_m_powerg1", +0.0000F, +0.0000F, -0.0230F, 1.0020F);
        addChildHatExtra("child_spn_m_powertiedg1", +0.0000F, +0.0000F, -0.0410F, 1.0000F);
        addChildHatExtra("child_spn_m_princelyg1", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildHatExtra("child_spn_m_princelyg4", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildHatExtra("child_spn_m_redfieldg1", +0.0000F, -0.0060F, -0.0680F, 1.0000F);
        addChildHatExtra("child_spn_m_redfieldlowg1", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildHatExtra("child_spn_m_redfieldmessyg4", +0.0000F, +0.0000F, -0.0430F, 1.0000F);
        addChildHatExtra("child_spn_m_reig1", +0.0000F, +0.0060F, -0.0200F, 1.0000F);
        addChildHatExtra("child_spn_m_rickg1", +0.0000F, +0.0080F, -0.0170F, 1.0000F);
        addChildHatExtra("child_spn_m_rickshowg1", +0.0000F, +0.0000F, -0.0200F, 1.0000F);
        addChildHatExtra("child_spn_m_shaggycurlyg1", +0.0000F, -0.0500F, -0.0170F, 1.0670F);
        addChildHatExtra("child_spn_m_shaggyg1", +0.0000F, -0.0290F, -0.0130F, 1.0500F);
        addChildHatExtra("child_spn_m_sidesweptg1", +0.0000F, +0.0000F, -0.0450F, 1.0000F);
        addChildHatExtra("child_spn_m_straightcutbobg1", +0.0000F, +0.0000F, -0.0360F, 1.0000F);
        addChildHatExtra("child_spn_m_surferg1", +0.0000F, +0.0820F, -0.0360F, 0.9350F);
        addChildHatExtra("child_spn_m_taurieg1", +0.0000F, +0.0000F, -0.0350F, 1.0000F);
        addChildHatExtra("child_spn_m_toukag1", +0.0000F, +0.0000F, -0.0400F, 1.0000F);
        addChildHatExtra("child_spn_m_toukastraightg4", +0.0000F, +0.0020F, -0.0380F, 1.0000F);
        addChildHatExtra("child_spn_m_valentineg1", +0.0000F, +0.0370F, -0.0360F, 0.9780F);
        addChildHatExtra("child_spn_m_vig1", +0.0000F, +0.0000F, -0.0400F, 1.0000F);
        addChildHatExtra("child_spn_m_yug1", +0.0000F, +0.0070F, -0.0450F, 1.0000F);
        addChildHatExtra("child_spn_m_yukarialtg1", +0.0000F, +0.0010F, -0.0390F, 1.0180F);
        addChildHatExtra("child_spn_m_yukarig1", +0.0000F, -0.0100F, -0.0200F, 1.0150F);
        addChildHatExtra("child_spn_m_zarinag1", +0.0000F, +0.0000F, -0.0470F, 1.0000F);
        addChildHatExtra("m_hair_braids_g1", +0.0000F, +0.0050F, +0.1320F, 1.0000F);
        addChildHatExtra("m_hair_buffont_g1", +0.0000F, +0.0050F, +0.1330F, 1.0000F);
        addChildHatExtra("m_hair_centrepartinglong_g1", +0.0000F, +0.0030F, +0.1200F, 1.0000F);
        addChildHatExtra("m_hair_fabian_g1", +0.0000F, +0.0060F, +0.0830F, 0.9700F);
        addChildHatExtra("m_hair_fabiancurly_g1", +0.0000F, +0.0050F, +0.0750F, 0.9660F);
        addChildHatExtra("m_hair_geralt_hat", +0.0000F, -0.0050F, +0.1500F, 1.0050F);
        addChildHatExtra("m_hair_greasedback_g1", +0.0000F, +0.0080F, +0.1450F, 1.0000F);
        addChildHatExtra("m_hair_grungey_g1", +0.0000F, +0.0000F, +0.1200F, 1.0000F);
        addChildHatExtra("m_hair_grungeybehindears_g1", +0.0000F, +0.0070F, +0.1310F, 1.0000F);
        addChildHatExtra("m_hair_longbraided_g1", +0.0000F, +0.0000F, +0.1270F, 1.0000F);
        addChildHatExtra("m_hair_longbraids02_g1", +0.0000F, +0.0030F, +0.0250F, 0.9260F);
        addChildHatExtra("m_hair_metal_g1", +0.0000F, +0.0160F, -0.2000F, 0.5000F);
        addChildHatExtra("m_hair_mullet_g1", +0.0000F, -0.1400F, +0.0000F, 1.0000F);
        addChildHatExtra("m_hair_mulletcurly_g1", +0.0000F, +0.0000F, +0.1400F, 1.0000F);
        addChildHatExtra("m_hair_ponytail", +0.0000F, +0.0000F, -0.0080F, 0.9920F);
        addChildHatExtra("m_hair_ranger", +0.0000F, +0.2200F, +0.1400F, 0.9220F);
        addChildHatExtra("m_hair_shorthat", +0.0000F, -0.0090F, -0.0610F, 1.0000F);
        addChildHatExtra("pch_yaki_afrobun_g1", +0.0000F, +0.0000F, +0.1300F, 1.0000F);
        addChildHatExtra("pch_yaki_braidedponytail_g1", +0.0000F, -0.1200F, +0.0050F, 1.0000F);
        addChildHatExtra("pch_yaki_cheerleader_g1", +0.0000F, +0.0050F, +0.1300F, 0.9900F);
        addChildHatExtra("pch_yaki_classy_g1", +0.0000F, -0.1550F, -0.0010F, 1.0150F);
        addChildHatExtra("pch_yaki_clippedhair_g1", +0.0000F, +0.0070F, +0.1410F, 1.0080F);
        addChildHatExtra("pch_yaki_crofthair_g1", +0.0000F, -0.1020F, +0.0070F, 0.9910F);
        addChildHatExtra("pch_yaki_doublebraids_g1", +0.0000F, -0.1590F, +0.0060F, 1.0300F);
        addChildHatExtra("pch_yaki_dreadlocks_g1", +0.0000F, +0.0140F, +0.1710F, 1.0250F);
        addChildHatExtra("pch_yaki_hairbun_g1", +0.0000F, +0.0050F, +0.1360F, 1.0040F);
        addChildHatExtra("pch_yaki_halfupshaved_g1", +0.0000F, -0.1620F, +0.0110F, 1.0000F);
        addChildHatExtra("pch_yaki_lowponytail_g1", +0.0000F, -0.1100F, -0.0140F, 1.0000F);
        addChildHatExtra("pch_yaki_pigtails_g1", +0.0000F, +0.0050F, +0.1200F, 1.0000F);
        addChildHatExtra("pch_yaki_rochefort_g1", +0.0000F, -0.1100F, +0.0000F, 1.0000F);
        addChildHatExtra("pch_yaki_sidebraid_g1", +0.0000F, -0.1200F, +0.0000F, 1.0000F);
        addChildHatExtra("pch_yaki_tiedhalfdown_g1", +0.0000F, +0.0000F, +0.1620F, 1.0000F);
        addChildHatExtra("pch_yaki_undercut_g1", +0.0000F, -0.1320F, +0.0000F, 1.0000F);
        addChildHatExtra("pch_yaki_undercutcurly_g1", +0.0000F, -0.1350F, +0.0000F, 1.0000F);
        addChildHatExtra("pch_yaki_yaki_g1", +0.0000F, -0.1150F, +0.0000F, 1.0000F);
        addChildHatExtra("child_harry_f_britacat1", +0.0000F, -0.0030F, -0.0640F, 1.0000F);
        addChildHatExtra("child_harry_f_grunge01cat1", +0.0000F, +0.0000F, +0.0020F, 1.2050F);
        addChildHatExtra("child_harry_f_merigoldcat1", +0.0000F, +0.0000F, +0.0020F, 1.1940F);
        addChildHatExtra("child_harry_f_ponytaillongcat1", +0.0000F, +0.0000F, +0.0020F, 1.1970F);
        addChildHatExtra("child_harry_f_ponytailmediumcat1", +0.0000F, +0.0000F, +0.0030F, 1.2960F);
        addChildHatExtra("child_harry_f_throttlekittycat1", +0.0000F, +0.0000F, +0.0000F, 1.0000F);
        addChildHatExtra("child_spn_m_aigisaltg4", +0.0000F, -0.0180F, +0.0000F, 1.0000F);
        addChildHatExtra("child_spn_m_aigisg4", +0.0000F, -0.0190F, +0.0020F, 1.0000F);
        addChildHatExtra("child_spn_m_ashg2", +0.0000F, +0.0000F, -0.0160F, 1.0000F);
        addChildHatExtra("child_spn_m_ashleyg4", +0.0000F, -0.0070F, +0.0000F, 1.0000F);
        addChildHatExtra("child_spn_m_danteg4", +0.0000F, -0.0020F, -0.0020F, 1.0000F);
        addChildHatExtra("child_spn_m_erenpartedg4", +0.0000F, -0.0070F, -0.0240F, 1.0000F);
        addChildHatExtra("child_spn_m_gothg2", +0.0000F, -0.0100F, -0.0250F, 1.0000F);
        addChildHatExtra("child_spn_m_herobung2", +0.0000F, -0.0010F, -0.0410F, 1.0000F);
        addChildHatExtra("child_spn_m_herog4", +0.0000F, -0.0080F, -0.0250F, 1.0000F);
        addChildHatExtra("child_spn_m_jeanetteg2", +0.0000F, -0.0020F, -0.0290F, 1.0000F);
        addChildHatExtra("child_spn_m_kotoneponytailg2", +0.0000F, -0.0110F, -0.0160F, 1.0000F);
        addChildHatExtra("child_spn_m_leonlongg4", +0.0000F, -0.0010F, +0.0000F, 1.0000F);
        addChildHatExtra("child_spn_m_mitsuruponytailg2", +0.0000F, -0.0050F, -0.0230F, 1.0000F);
        addChildHatExtra("child_spn_m_powerg3", +0.0000F, -0.0040F, -0.0040F, 0.9900F);
        addChildHatExtra("child_spn_m_powerg4", +0.0000F, -0.0040F, -0.0040F, 0.9900F);
        addChildHatExtra("child_spn_m_redfieldg2", +0.0000F, -0.0140F, -0.0360F, 1.0000F);
        addChildHatExtra("child_spn_m_redfieldlowg4", +0.0000F, -0.0040F, -0.0180F, 1.0000F);
        addChildHatExtra("child_spn_m_redfieldmessyg2", +0.0000F, -0.0040F, -0.0180F, 1.0000F);
        addChildHatExtra("child_spn_m_reig4", +0.0000F, -0.0140F, +0.0000F, 1.0000F);
        addChildHatExtra("child_spn_m_shaggycurlyg4", +0.0000F, -0.0090F, +0.0000F, 1.0000F);
        addChildHatExtra("child_spn_m_shaggyg4", +0.0000F, -0.0050F, +0.0000F, 1.0000F);
        addChildHatExtra("child_spn_m_taurieg3", +0.0000F, -0.0090F, -0.0170F, 1.0000F);
        addChildHatExtra("child_spn_m_toukag4", +0.0000F, -0.0080F, -0.0100F, 1.0000F);
        addChildHatExtra("child_spn_m_valentineg4", +0.0000F, -0.0120F, -0.0170F, 1.0000F);
        addChildHatExtra("child_spn_m_yukaribung2", +0.0000F, -0.0040F, -0.0170F, 1.0070F);
        addChildHatExtra("f_hair_bob_g1", +0.0000F, +0.0000F, -0.0450F, 1.0000F);
        addChildHatExtra("f_hair_bob_g2", +0.0000F, +0.0000F, -0.0450F, 1.0000F);
        addChildHatExtra("f_hair_bob_g3", +0.0000F, +0.0000F, -0.0450F, 1.0000F);
        addChildHatExtra("f_hair_bob_g4", +0.0000F, +0.0000F, -0.0450F, 1.0000F);
        addChildHatExtra("f_hair_bobcurly_g1", +0.0000F, +0.0000F, -0.0500F, 1.0000F);
        addChildHatExtra("f_hair_bobcurly_g2", +0.0000F, +0.0000F, -0.0500F, 1.0000F);
        addChildHatExtra("f_hair_bobcurly_g3", +0.0000F, +0.0000F, -0.0500F, 1.0000F);
        addChildHatExtra("f_hair_bobcurly_g4", +0.0000F, +0.0000F, -0.0500F, 1.0000F);
        addChildHatExtra("f_hair_bound_g1", +0.0000F, -0.0160F, -0.0760F, 1.0000F);
        addChildHatExtra("f_hair_bun_g2", +0.0000F, -0.0160F, -0.0760F, 1.0000F);
        addChildHatExtra("f_hair_buncurly_g2", +0.0000F, -0.0130F, -0.0780F, 1.0000F);
        addChildHatExtra("f_hair_grungey02_g1", +0.0000F, +0.0000F, -0.0700F, 1.0000F);
        addChildHatExtra("f_hair_grungey02_g2", +0.0000F, +0.0000F, -0.0700F, 1.0000F);
        addChildHatExtra("f_hair_grungey02_g3", +0.0000F, +0.0000F, -0.0700F, 1.0000F);
        addChildHatExtra("f_hair_grungeyparting_g1", +0.0000F, +0.0000F, -0.0500F, 1.0000F);
        addChildHatExtra("f_hair_grungeyparting_g2", +0.0000F, +0.0000F, -0.0500F, 1.0000F);
        addChildHatExtra("f_hair_grungeyparting_g3", +0.0000F, +0.0000F, -0.0500F, 1.0000F);
        addChildHatExtra("f_hair_long_g1", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildHatExtra("f_hair_long_g2", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildHatExtra("f_hair_long_g3", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildHatExtra("f_hair_longcurly_g1", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildHatExtra("f_hair_longcurly_g2", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildHatExtra("f_hair_longcurly_g3", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildHatExtra("f_hair_mullet_g2", +0.0000F, -0.0050F, -0.0700F, 1.0000F);
        addChildHatExtra("f_hair_mulletcurly_g2", +0.0000F, -0.0100F, -0.0750F, 1.0000F);
        addChildHatExtra("f_hair_overeye_g1", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildHatExtra("f_hair_overeye_g2", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildHatExtra("f_hair_overeye_g3", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildHatExtra("f_hair_overeyecurly_g1", +0.0000F, +0.0000F, -0.0600F, 1.0000F);
        addChildHatExtra("f_hair_overeyecurly_g2", +0.0000F, +0.0000F, -0.0600F, 1.0000F);
        addChildHatExtra("f_hair_overeyecurly_g3", +0.0000F, +0.0000F, -0.0600F, 1.0000F);
        addChildHatExtra("f_hair_overeyeleft_g3", +0.0000F, +0.0000F, -0.0550F, 1.0000F);
        addChildHatExtra("f_hair_rachel_g1", +0.0000F, -0.0050F, -0.0350F, 1.0000F);
        addChildHatExtra("f_hair_rachel_g2", +0.0000F, -0.0050F, -0.0350F, 1.0000F);
        addChildHatExtra("f_hair_rachel_g3", +0.0000F, -0.0050F, -0.0350F, 1.0000F);
        addChildHatExtra("f_hair_rachel_g4", +0.0000F, -0.0050F, -0.0350F, 1.0000F);
        addChildHatExtra("f_hair_rachelcurly_g1", +0.0000F, -0.0080F, -0.0390F, 1.0000F);
        addChildHatExtra("f_hair_rachelcurly_g2", +0.0000F, -0.0080F, -0.0390F, 1.0000F);
        addChildHatExtra("f_hair_rachelcurly_g3", +0.0000F, -0.0080F, -0.0390F, 1.0000F);
        addChildHatExtra("f_hair_rachelcurly_g4", +0.0000F, -0.0080F, -0.0390F, 1.0000F);
        addChildHatExtra("f_hair_topcurls_g2", +0.0000F, -0.1080F, -0.0810F, 1.0710F);
        addChildHatExtra("m_hair_braids_g2", +0.0000F, +0.0050F, +0.1200F, 1.0000F);
        addChildHatExtra("m_hair_braids_g3", +0.0000F, -0.0050F, -0.0540F, 1.0000F);
        addChildHatExtra("m_hair_braids_g4", +0.0000F, -0.0050F, -0.0540F, 1.0000F);
        addChildHatExtra("m_hair_buffont_g2", +0.0000F, +0.0000F, -0.0600F, 1.0000F);
        addChildHatExtra("m_hair_centreparting_g2", +0.0000F, -0.0060F, -0.0630F, 1.0000F);
        addChildHatExtra("m_hair_centrepartinglong_g2", +0.0000F, +0.0000F, -0.0600F, 1.0000F);
        addChildHatExtra("m_hair_centrepartinglong_g3", +0.0000F, +0.0000F, -0.0600F, 1.0000F);
        addChildHatExtra("m_hair_centrepartinglong_g4", +0.0000F, +0.0000F, -0.0600F, 1.0000F);
        addChildHatExtra("m_hair_donny_g3", +0.0000F, +0.0000F, -0.0230F, 1.0000F);
        addChildHatExtra("m_hair_fabian_g2", +0.0000F, +0.0110F, -0.0360F, 0.9830F);
        addChildHatExtra("m_hair_fabian_g3", +0.0000F, +0.0110F, -0.0360F, 0.9830F);
        addChildHatExtra("m_hair_fabianbraided_g3", +0.0000F, +0.0020F, -0.0470F, 0.9910F);
        addChildHatExtra("m_hair_fabiancurly_g2", +0.0000F, +0.0040F, -0.0450F, 0.9930F);
        addChildHatExtra("m_hair_fabiancurly_g3", +0.0000F, +0.0040F, -0.0450F, 0.9930F);
        addChildHatExtra("m_hair_greasedback_g2", +0.0000F, +0.0000F, -0.0650F, 1.0000F);
        addChildHatExtra("m_hair_grungey_g2", +0.0000F, +0.0000F, -0.0650F, 1.0000F);
        addChildHatExtra("m_hair_grungey_g3", +0.0000F, +0.0000F, -0.0650F, 1.0000F);
        addChildHatExtra("m_hair_grungeybehindears_g2", +0.0000F, +0.0000F, -0.0450F, 1.0000F);
        addChildHatExtra("m_hair_hatpunk_g1", +0.0000F, +0.0000F, -0.0300F, 1.0000F);
        addChildHatExtra("m_hair_libertyspikes_g1", +0.0000F, +0.0000F, -0.0240F, 1.0000F);
        addChildHatExtra("m_hair_libertyspikes_g2", +0.0000F, +0.0000F, -0.0240F, 1.0000F);
        addChildHatExtra("m_hair_longbraided_g2", +0.0000F, +0.0020F, -0.0470F, 0.9910F);
        addChildHatExtra("m_hair_longbraids02_g2", +0.0000F, +0.0140F, -0.0520F, 0.9840F);
        addChildHatExtra("m_hair_metal_g2", +0.0000F, -0.1050F, -0.0120F, 0.9800F);
        addChildHatExtra("m_hair_partingleft_g2", +0.0000F, -0.0020F, -0.0600F, 1.0000F);
        addChildHatExtra("m_hair_partingright_g2", +0.0000F, -0.0020F, -0.0570F, 1.0000F);
        addChildHatExtra("m_hair_recede_g3", +0.0000F, -0.0060F, -0.0530F, 1.0000F);
    }

    static {
        // FIXED74: baked TEEN hair calibration exported by the user.
        // These are ADDITIVE live-tuner extras on top of TEEN_HAIR_SCALE,
        // deliberately isolated by sex and by normal/hat namespace.
        addTeenStyleExtra(false, "PCH_Vanilla_Buffont", +0.0000F, +0.0050F, +0.0000F, 1.0850F); // Bouffant
        addTeenStyleExtra(false, "PCH_Vanilla_CentreParting", +0.0000F, -0.1900F, -0.0070F, 1.2990F); // Raya al medio
        addTeenStyleExtra(false, "PCH_Vanilla_CentrePartingLong", +0.0000F, -0.0030F, +0.0040F, 1.1030F); // Largo con raya al medio
        addTeenStyleExtra(false, "PCH_Vanilla_Donny", +0.0000F, +0.0000F, +0.0060F, 1.0910F); // Flequillo
        addTeenStyleExtra(false, "PCH_Vanilla_Fabian", +0.0000F, -0.0580F, -0.0030F, 1.1520F); // Melena larga
        addTeenStyleExtra(false, "PCH_Vanilla_FabianCurly", +0.0000F, -0.0910F, -0.0060F, 1.1840F); // Melena larga (Rizado)
        addTeenStyleExtra(false, "PCH_Vanilla_GreasedBack", +0.0000F, -0.1340F, -0.0030F, 1.2250F); // Engominado atrás
        addTeenStyleExtra(false, "PCH_Vanilla_Grungey", +0.0000F, -0.1180F, -0.0010F, 1.2260F); // Grungey
        addTeenStyleExtra(false, "PCH_Vanilla_GrungeyBehindEars", +0.0000F, -0.1500F, -0.0030F, 1.2600F); // Grungey tras las orejas
        addTeenStyleExtra(false, "PCH_Vanilla_Hat", +0.0000F, -0.1800F, +0.0000F, 1.2930F); // Corto
        addTeenStyleExtra(false, "PCH_Vanilla_LeftParting", +0.0000F, -0.1200F, +0.0000F, 1.2200F); // Raya a la izquierda
        addTeenStyleExtra(false, "PCH_Vanilla_LibertySpikes", +0.0000F, +0.1150F, +0.0000F, 0.9810F); // Pinchos
        addTeenStyleExtra(false, "PCH_Vanilla_Messy", +0.0000F, -0.1220F, -0.0040F, 1.2280F); // Revuelto
        addTeenStyleExtra(false, "PCH_Vanilla_MessyCurly", +0.0000F, -0.1360F, -0.0050F, 1.2450F); // Revuelto (Rizado)
        addTeenStyleExtra(false, "PCH_Vanilla_Metal", +0.0000F, -0.0240F, +0.0010F, 1.1280F); // Metalero
        addTeenStyleExtra(false, "PCH_Vanilla_MohawkFan", +0.0000F, +0.0410F, +0.0000F, 1.0000F); // Mohicano
        addTeenStyleExtra(false, "PCH_Vanilla_MohawkFlat", +0.0000F, +0.0500F, +0.0070F, 1.0420F); // Mohicano plano
        addTeenStyleExtra(false, "PCH_Vanilla_MohawkShort", +0.0000F, +0.0000F, +0.0000F, 1.0950F); // Mohicano corto
        addTeenStyleExtra(false, "PCH_Vanilla_MohawkSpike", +0.0000F, +0.0650F, +0.0000F, 1.0150F); // Mohicano pinchos
        addTeenStyleExtra(false, "PCH_Vanilla_Mullet", +0.0000F, -0.1400F, -0.0060F, 1.2470F); // Salmonete
        addTeenStyleExtra(false, "PCH_Vanilla_MulletCurly", +0.0000F, -0.1800F, -0.0060F, 1.2840F); // Salmonete (Rizado)
        addTeenStyleExtra(false, "PCH_Vanilla_PonyTail", +0.0000F, -0.2000F, +0.0000F, 1.3140F); // Coleta
        addTeenStyleExtra(false, "PCH_Vanilla_PonyTailBraids", +0.0000F, -0.1800F, +0.0000F, 1.2930F); // Coleta con trenzas
        addTeenStyleExtra(false, "PCH_Vanilla_Recede", +0.0000F, -0.1780F, -0.0030F, 1.2910F); // Entradas
        addTeenStyleExtra(false, "PCH_Vanilla_RightParting", +0.0000F, -0.1550F, -0.0010F, 1.2590F); // Raya a la derecha
        addTeenStyleExtra(false, "PCH_Vanilla_Short", +0.0000F, -0.1600F, -0.0080F, 1.2600F); // Sencillo
        addTeenStyleExtra(false, "PCH_Vanilla_ShortAfroCurly", +0.0000F, +0.0000F, +0.0000F, 1.0750F); // Afro corto
        addTeenStyleExtra(false, "PCH_Vanilla_Spike", +0.0000F, -0.1150F, -0.0010F, 1.2200F); // Puntiagudo
        addTeenStyleExtra(false, "PCH_Harry_M_Aiteron", +0.0000F, +0.0000F, +0.0000F, 1.0300F); // Aiteron
        addTeenStyleExtra(false, "PCH_Harry_M_ObiWanCannoli", +0.0000F, +0.0000F, +0.0010F, 1.1340F); // Obi Wan Cannoli
        addTeenStyleExtra(false, "PCH_Harry_M_Soulfilcher", +0.0000F, +0.0000F, +0.0010F, 1.1350F); // Soulfilcher
        addTeenStyleExtra(false, "PCH_Spn_M_Aigis", +0.0000F, +0.0350F, +0.0000F, 1.0000F); // Aeon
        addTeenStyleExtra(false, "PCH_Spn_M_AigisAlt", +0.0000F, +0.0350F, +0.0000F, 1.0000F); // Aeon Alt
        addTeenStyleExtra(false, "PCH_Spn_M_Carlos", +0.0000F, +0.0100F, +0.0050F, 1.0150F); // Oliveira
        addTeenStyleExtra(false, "PCH_Spn_M_ErenBun", +0.0000F, -0.0520F, +0.0050F, 1.0850F); // Coleta Yeager
        addTeenStyleExtra(false, "PCH_Spn_M_Goth", +0.0000F, -0.1010F, +0.0100F, 1.1520F); // Coletas g�ticas
        addTeenStyleExtra(false, "PCH_Spn_M_Leon", +0.0000F, -0.0350F, +0.0070F, 1.0670F); // Kennedy
        addTeenStyleExtra(false, "PCH_Spn_M_MitsuruPonytail", +0.0000F, -0.0300F, +0.0080F, 1.0640F); // Emperatriz (Atado)
        addTeenStyleExtra(false, "PCH_Spn_M_P3Hero", +0.0000F, -0.0250F, +0.0150F, 1.0550F); // Tonto
        addTeenStyleExtra(false, "PCH_Spn_M_Redfield", +0.0000F, -0.0370F, +0.0160F, 1.0690F); // Redfield
        addTeenStyleExtra(false, "PCH_Spn_M_Rick", +0.0000F, +0.0000F, +0.0050F, 1.0270F); // Grimes
        addTeenStyleExtra(false, "PCH_Spn_M_Valentine", +0.0000F, +0.0000F, +0.0000F, 1.0350F); // Valentine
        addTeenStyleExtra(false, "PCH_Vanilla_Braids", +0.0000F, -0.0750F, +0.0000F, 1.1800F); // Trenzas
        addTeenStyleExtra(false, "PCH_Vanilla_Cornrows", +0.0000F, -0.1130F, +0.0010F, 1.2200F); // Trenzas africanas
        addTeenStyleExtra(false, "PCH_Vanilla_LongBraids", +0.0000F, +0.0000F, -0.0020F, 1.1050F); // Trenzas largas
        addTeenStyleExtra(false, "PCH_Vanilla_LongBraids02", +0.0000F, -0.0170F, +0.0060F, 1.1130F); // Trenzas muy largas
        addTeenStyleExtra(false, "PCH_Vanilla_ShortHatCurly", +0.0000F, -0.1800F, -0.0050F, 1.2950F); // Sencillo (Rizado)
        addTeenStyleExtra(false, "PCH_Spn_M_Agent", +0.0000F, -0.0090F, +0.0080F, 1.0400F); // Agent
        addTeenStyleExtra(false, "PCH_Spn_M_Curtains", +0.0000F, +0.0090F, +0.0200F, 1.0300F); // Curtains
        addTeenStyleExtra(false, "PCH_Spn_M_Dante", +0.0000F, +0.0470F, +0.0040F, 0.9870F); // Dante desalineado
        addTeenStyleExtra(false, "PCH_Spn_M_DanteParted", +0.0000F, +0.0100F, +0.0010F, 1.0200F); // Dante
        addTeenStyleExtra(false, "PCH_Spn_M_ErenParted", +0.0000F, -0.0090F, +0.0120F, 1.0430F); // Raya al medio largo
        addTeenStyleExtra(false, "PCH_Spn_M_Kotone", +0.0000F, -0.0160F, +0.0000F, 1.0500F); // Tonto XXII
        addTeenStyleExtra(false, "PCH_Spn_M_KotonePonytail", +0.0000F, -0.0650F, +0.0100F, 1.1000F); // Tonto XXII (Atado)
        addTeenStyleExtra(false, "PCH_Spn_M_Marie", +0.0000F, -0.0720F, +0.0000F, 1.1060F); // Marie
        addTeenStyleExtra(false, "PCH_Spn_M_Mikasa", +0.0000F, +0.0360F, +0.0000F, 1.0000F); // Mikasa
        addTeenStyleExtra(false, "PCH_Spn_M_MikasaShort", +0.0000F, -0.0370F, +0.0000F, 1.0750F); // Mikasa corto
        addTeenStyleExtra(false, "PCH_Spn_M_Parted", +0.0000F, -0.0320F, +0.0090F, 1.0670F); // Encantador
        addTeenStyleExtra(false, "PCH_Spn_M_PartedMedium", +0.0000F, -0.0080F, +0.0000F, 1.0400F); // Medio encantador
        addTeenStyleExtra(false, "PCH_Spn_M_PartedSide", +0.0000F, -0.0080F, +0.0070F, 1.0400F); // Side Parted
        addTeenStyleExtra(false, "PCH_Spn_M_PartedSideMedium", +0.0000F, -0.0640F, +0.0060F, 1.0980F); // Side Parted Medium
        addTeenStyleExtra(false, "PCH_Spn_M_Rei", +0.0000F, +0.0280F, +0.0040F, 0.9990F); // Rei
        addTeenStyleExtra(false, "PCH_Spn_M_RickShow", +0.0000F, -0.0310F, +0.0000F, 1.0600F); // Hacia atr�s engominado
        addTeenStyleExtra(false, "PCH_Spn_M_Shaggy", +0.0000F, +0.0220F, +0.0040F, 1.0000F); // Shaggy
        addTeenStyleExtra(false, "PCH_Spn_M_ShaggyCurly", +0.0000F, +0.0210F, +0.0080F, 1.0000F); // Shaggy (Curly)
        addTeenStyleExtra(false, "PCH_Spn_M_Sideswept", +0.0000F, -0.0280F, +0.0130F, 1.0610F); // Peinado hacia un lado
        addTeenStyleExtra(false, "PCH_Spn_M_Vergil", +0.0000F, +0.0120F, -0.0030F, 1.0120F); // Vergil
        addTeenStyleExtra(false, "PCH_Spn_M_Yu", +0.0000F, -0.0100F, +0.0200F, 1.0500F); // Corte taz�n
        addTeenStyleExtra(false, "PCH_Spn_M_Yukari", +0.0000F, -0.0120F, +0.0000F, 1.0400F); // Amantes
        addTeenStyleExtra(false, "PCH_Spn_M_YukariAlt", +0.0000F, -0.0020F, -0.0060F, 1.0300F); // Amantes corto
        addTeenStyleExtra(false, "PCH_Spn_M_YukariBun", +0.0000F, -0.0570F, +0.0140F, 1.0900F); // Amantes (Atado)
        addTeenStyleExtra(false, "PCH_Spn_M_Ash", +0.0000F, +0.0320F, +0.0050F, 1.0000F); // Groovy
        addTeenStyleExtra(false, "PCH_Spn_M_AshBowl", +0.0000F, +0.0350F, +0.0100F, 1.0000F); // Thick Bowl Cut
        addTeenStyleExtra(false, "PCH_Spn_M_AshShort", +0.0000F, +0.0070F, +0.0100F, 1.0250F); // Groovy Short
        addTeenStyleExtra(false, "PCH_Spn_M_AshSlick", +0.0000F, +0.0170F, +0.0030F, 1.0120F); // Groovy Slick
        addTeenStyleExtra(false, "PCH_Spn_M_CurtainsHigh", +0.0000F, -0.0510F, +0.0030F, 1.0790F); // High Curtains
        addTeenStyleExtra(false, "PCH_Spn_M_Hero", +0.0000F, -0.0300F, +0.0020F, 1.0600F); // Hero
        addTeenStyleExtra(false, "PCH_Spn_M_HeroBun", +0.0000F, -0.0440F, +0.0110F, 1.0800F); // Hero Bun
        addTeenStyleExtra(false, "PCH_Spn_M_HeroTail", +0.0000F, -0.0350F, +0.0100F, 1.0650F); // zDoNotPickMe
        addTeenStyleExtra(false, "PCH_Spn_M_Kusanagi", +0.0000F, +0.0320F, +0.0200F, 1.0020F); // Kusanagi
        addTeenStyleExtra(false, "PCH_Spn_M_LeonLong", +0.0000F, -0.0070F, +0.0090F, 1.0400F); // Kennedy Longer
        addTeenStyleExtra(false, "PCH_Spn_M_LeonRE4", +0.0000F, -0.0220F, +0.0150F, 1.0520F); // Kennedy Long
        addTeenStyleExtra(false, "PCH_Spn_M_Logan", +0.0000F, +0.0310F, +0.0080F, 1.0000F); // Logan
        addTeenStyleExtra(false, "PCH_Spn_M_PartedTail", +0.0000F, +0.0290F, +0.0210F, 1.0070F); // Parted Tail
        addTeenStyleExtra(false, "PCH_Spn_M_Princely", +0.0000F, +0.0300F, +0.0100F, 1.0040F); // Princely
        addTeenStyleExtra(false, "PCH_Spn_M_PrincelyShort", +0.0000F, -0.0390F, +0.0040F, 1.0700F); // Princely Cropped
        addTeenStyleExtra(false, "PCH_Spn_M_RedfieldLow", +0.0000F, -0.0320F, +0.0150F, 1.0580F); // Redfield Low Tail
        addTeenStyleExtra(false, "PCH_Spn_M_SpikedForward", +0.0000F, -0.0480F, +0.0030F, 1.0780F); // Spiked Forward
        addTeenStyleExtra(false, "PCH_Spn_M_SpikedUp", +0.0000F, -0.0250F, +0.0070F, 1.0500F); // Spiked Up
        addTeenStyleExtra(false, "PCH_Spn_M_Surfer", +0.0000F, +0.0020F, +0.0000F, 1.0250F); // Surfer
        addTeenStyleExtra(false, "PCH_Spn_M_Touka", +0.0000F, +0.0010F, +0.0160F, 1.0280F); // Over Eye Fluffy
        addTeenStyleExtra(false, "PCH_Spn_M_ToukaStraight", +0.0000F, +0.0060F, +0.0050F, 1.0200F); // Over Eye Straight
        addTeenStyleExtra(false, "PCH_Spn_M_AlanWake", +0.0000F, +0.0200F, +0.0080F, 1.0140F); // Wake
        addTeenStyleExtra(false, "PCH_Spn_M_Jeanette", +0.0000F, -0.0500F, +0.0150F, 1.0850F); // Jeanette
        addTeenStyleExtra(false, "PCH_Spn_M_MessyBob", +0.0000F, +0.0300F, +0.0100F, 1.0000F); // Messy Bob
        addTeenStyleExtra(false, "PCH_Spn_M_RedfieldMessy", +0.0000F, -0.0230F, +0.0130F, 1.0600F); // Redfield Messy Tail
        addTeenStyleExtra(false, "PCH_Spn_M_SpikedMessy", +0.0000F, -0.0160F, +0.0080F, 1.0450F); // Spiked Messy
        addTeenStyleExtra(false, "PCH_Spn_M_StraightCutBob", +0.0000F, +0.0300F, +0.0150F, 1.0050F); // Straight Cut Bob
        addTeenStyleExtra(false, "PCH_Spn_M_Ashley", +0.0000F, +0.0300F, +0.0000F, 1.0000F); // Ashley
        addTeenStyleExtra(false, "PCH_Spn_M_Frank", +0.0000F, -0.0070F, +0.0060F, 1.0250F); // Frank
        addTeenStyleExtra(false, "PCH_Spn_M_OvereyeShaggy", +0.0000F, -0.0020F, +0.0050F, 1.0250F); // Over Eye Shaggy
        addTeenStyleExtra(false, "PCH_Spn_M_OvereyeShaggyLeft", +0.0000F, -0.0020F, +0.0050F, 1.0250F); // Over Eye Shaggy Left
        addTeenStyleExtra(false, "PCH_Spn_M_Power", +0.0000F, +0.0340F, +0.0110F, 1.0020F); // Power
        addTeenStyleExtra(false, "PCH_Spn_M_PowerTied", +0.0000F, +0.0070F, +0.0090F, 1.0300F); // Power Tied
        addTeenStyleExtra(false, "PCH_Spn_M_Taurie", +0.0000F, -0.0100F, +0.0070F, 1.0400F); // Taurie
        addTeenStyleExtra(false, "PCH_Spn_M_Vi", +0.0000F, -0.0410F, +0.0040F, 1.0700F); // Vi
        addTeenStyleExtra(false, "PCH_Spn_M_Zarina", +0.0000F, -0.0200F, +0.0070F, 1.0500F); // Zarina
        addTeenStyleExtra(false, "PCH_Spn_M_ZarinaCurly", +0.0000F, -0.0800F, +0.0000F, 1.1130F); // Zarina (Curly)
        // FIXED90: user-approved TEEN FEMALE normal-hair calibration export (2026-09-28).
        // Isolated to TEEN_FEMALE_STYLE_EXTRA_ADJUSTMENTS; CHILD, TEEN male and hat fits are untouched.
        // FIXED97: user-approved TEEN MALE normal/no-hat tuner deltas, pre-composed with the existing baked base.
        // The tuner exports live deltas (not absolute replacements); these late entries store the final composed fit.
        addTeenStyleExtra(false, "PCH_Spn_M_BobSlick", +0.0000F, -0.0020F, +0.0000F, 1.0410F); // Slicked Back Bob (tuner delta; no prior per-style base)
        addTeenStyleExtra(false, "PCH_Vanilla_CentreParting", +0.0000F, -0.1890F, -0.0070F, 1.2990F); // Raya al medio = base(-0.190,-0.007,1.299) + tuner delta(+0.001,1.000)
        addTeenStyleExtra(false, "PCH_Vanilla_Hat", +0.0000F, -0.1750F, +0.0000F, 1.2930F); // Corto = base(-0.180,1.293) + tuner delta(+0.005,1.000)
        addTeenStyleExtra(false, "PCH_Vanilla_Messy", +0.0000F, -0.1220F, -0.0090F, 1.2280F); // Revuelto = base(-0.122,-0.004,1.228) + tuner delta(z=-0.005)
        addTeenStyleExtra(false, "PCH_Vanilla_ShortHatCurly", +0.0000F, -0.2110F, -0.0050F, 1.3468F); // Sencillo (Rizado) = base(-0.180,-0.005,1.295) + tuner delta(-0.031,x1.040)

        addTeenStyleExtra(true, "PCH_Harry_F_BRITA", +0.0000F, -0.1180F, +0.0000F, 1.0400F); // BRITA
        addTeenStyleExtra(true, "PCH_Harry_F_Grunge01", +0.0000F, +0.0000F, +0.0020F, 1.2500F); // Grunge 01
        addTeenStyleExtra(true, "PCH_Harry_F_Merigold", +0.0000F, +0.0000F, +0.0000F, 1.0360F); // Merigold
        addTeenStyleExtra(true, "PCH_Harry_F_PonytailLong", +0.0000F, +0.0000F, +0.0020F, 1.2390F); // Ponytail Long
        addTeenStyleExtra(true, "PCH_Harry_F_PonytailMedium", +0.0000F, +0.0000F, +0.0010F, 1.1320F); // Ponytail Medium
        addTeenStyleExtra(true, "PCH_Harry_F_ThrottleKitty", +0.0000F, +0.0000F, +0.0000F, 1.0310F); // ThrottleKitty
        addTeenStyleExtra(true, "PCH_Spn_M_Agent", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Agent
        addTeenStyleExtra(true, "PCH_Spn_M_Aigis", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Aeon
        addTeenStyleExtra(true, "PCH_Spn_M_AigisAlt", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Aeon Alt
        addTeenStyleExtra(true, "PCH_Spn_M_Carlos", +0.0000F, +0.0440F, +0.0000F, 0.9740F); // Oliveira
        addTeenStyleExtra(true, "PCH_Spn_M_Curtains", +0.0000F, +0.0030F, +0.0000F, 1.0170F); // Curtains
        addTeenStyleExtra(true, "PCH_Spn_M_CurtainsHigh", +0.0000F, +0.0160F, +0.0000F, 1.0020F); // High Curtains
        addTeenStyleExtra(true, "PCH_Spn_M_Dante", +0.0000F, +0.0360F, +0.0000F, 0.9890F); // Dante desalineado
        addTeenStyleExtra(true, "PCH_Spn_M_DanteParted", +0.0000F, +0.0130F, +0.0000F, 1.0050F); // Dante
        addTeenStyleExtra(true, "PCH_Spn_M_ErenBun", +0.0000F, -0.0010F, +0.0000F, 1.0230F); // Coleta Yeager
        addTeenStyleExtra(true, "PCH_Spn_M_ErenParted", +0.0000F, +0.0000F, +0.0000F, 1.0210F); // Raya al medio largo
        addTeenStyleExtra(true, "PCH_Spn_M_Goth", +0.0000F, +0.0000F, +0.0000F, 1.0240F); // Coletas góticas
        addTeenStyleExtra(true, "PCH_Spn_M_Hero", +0.0000F, +0.0180F, +0.0000F, 1.0000F); // Hero
        addTeenStyleExtra(true, "PCH_Spn_M_HeroBun", +0.0000F, +0.0230F, +0.0080F, 1.0000F); // Hero Bun
        addTeenStyleExtra(true, "PCH_Spn_M_HeroTail", +0.0000F, +0.0210F, +0.0000F, 1.0000F); // zDoNotPickMe
        addTeenStyleExtra(true, "PCH_Spn_M_Kotone", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Tonta XXII
        addTeenStyleExtra(true, "PCH_Spn_M_KotonePonytail", +0.0000F, +0.0000F, +0.0020F, 1.0210F); // Tonta XXII (Atado)
        addTeenStyleExtra(true, "PCH_Spn_M_Kusanagi", +0.0000F, +0.0320F, +0.0000F, 0.9880F); // Kusanagi
        addTeenStyleExtra(true, "PCH_Spn_M_Leon", +0.0000F, +0.0000F, +0.0000F, 1.0210F); // Kennedy
        addTeenStyleExtra(true, "PCH_Spn_M_LeonLong", +0.0000F, +0.0210F, +0.0000F, 1.0000F); // Kennedy Longer
        addTeenStyleExtra(true, "PCH_Spn_M_LeonRE4", +0.0000F, +0.0140F, +0.0120F, 1.0050F); // Kennedy Long
        addTeenStyleExtra(true, "PCH_Spn_M_Marie", +0.0000F, +0.0000F, +0.0000F, 1.0180F); // Marie
        addTeenStyleExtra(true, "PCH_Spn_M_Mikasa", +0.0000F, +0.0020F, +0.0000F, 1.0080F); // Mikasa
        addTeenStyleExtra(true, "PCH_Spn_M_MikasaShort", +0.0000F, +0.0000F, +0.0000F, 1.0190F); // Mikasa corto
        addTeenStyleExtra(true, "PCH_Spn_M_MitsuruPonytail", +0.0000F, +0.0000F, +0.0000F, 1.0220F); // Emperatriz (Atado)
        addTeenStyleExtra(true, "PCH_Spn_M_P3Hero", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Tonta
        addTeenStyleExtra(true, "PCH_Spn_M_Parted", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Encantadora
        addTeenStyleExtra(true, "PCH_Spn_M_PartedMedium", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Medio encantadora
        addTeenStyleExtra(true, "PCH_Spn_M_PartedSide", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Side Parted
        addTeenStyleExtra(true, "PCH_Spn_M_PartedSideMedium", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Side Parted Medium
        addTeenStyleExtra(true, "PCH_Spn_M_Princely", +0.0000F, +0.0380F, +0.0000F, 0.9830F); // Princely
        addTeenStyleExtra(true, "PCH_Spn_M_PrincelyShort", +0.0000F, +0.0220F, +0.0000F, 1.0000F); // Princely Cropped
        addTeenStyleExtra(true, "PCH_Spn_M_Redfield", +0.0000F, +0.0000F, +0.0000F, 1.0230F); // Redfield
        addTeenStyleExtra(true, "PCH_Spn_M_Rei", +0.0000F, +0.0720F, +0.0000F, 0.9530F); // Rei
        addTeenStyleExtra(true, "PCH_Spn_M_Rick", +0.0000F, +0.0000F, +0.0000F, 1.0160F); // Grimes
        addTeenStyleExtra(true, "PCH_Spn_M_RickShow", +0.0000F, +0.0000F, +0.0000F, 1.0140F); // Hacia atrás engominado
        addTeenStyleExtra(true, "PCH_Spn_M_Shaggy", +0.0000F, +0.0000F, +0.0000F, 1.0090F); // Shaggy
        addTeenStyleExtra(true, "PCH_Spn_M_ShaggyCurly", +0.0000F, +0.0000F, +0.0000F, 1.0100F); // Shaggy (Curly)
        addTeenStyleExtra(true, "PCH_Spn_M_Sideswept", +0.0000F, +0.0000F, +0.0000F, 1.0240F); // Peinado hacia un lado
        addTeenStyleExtra(true, "PCH_Spn_M_SpikedForward", +0.0000F, +0.0000F, +0.0000F, 1.0150F); // Spiked Forward
        addTeenStyleExtra(true, "PCH_Spn_M_SpikedUp", +0.0000F, +0.0000F, +0.0000F, 1.0140F); // Spiked Up
        addTeenStyleExtra(true, "PCH_Spn_M_Surfer", +0.0000F, +0.0000F, +0.0000F, 1.0130F); // Surfer
        addTeenStyleExtra(true, "PCH_Spn_M_Valentine", +0.0000F, -0.0060F, +0.0000F, 1.0300F); // Valentine
        addTeenStyleExtra(true, "PCH_Spn_M_Vergil", +0.0000F, +0.0000F, -0.0050F, 1.0110F); // Vergil
        addTeenStyleExtra(true, "PCH_Spn_M_Yu", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Corte tazón
        addTeenStyleExtra(true, "PCH_Spn_M_Yukari", +0.0000F, +0.0000F, +0.0000F, 1.0170F); // Amantes
        addTeenStyleExtra(true, "PCH_Spn_M_YukariAlt", +0.0000F, +0.0000F, +0.0000F, 1.0160F); // Amantes corto
        addTeenStyleExtra(true, "PCH_Spn_M_YukariBun", +0.0000F, +0.0000F, +0.0000F, 1.0190F); // Amantes (Atado)
        addTeenStyleExtra(true, "PCH_Vanilla_Back", +0.0000F, -0.1500F, +0.0000F, 1.2600F); // Recogido
        addTeenStyleExtra(true, "PCH_Vanilla_Braids", +0.0000F, -0.0060F, +0.0000F, 1.0900F); // Trenzas
        addTeenStyleExtra(true, "PCH_Vanilla_CentreParting", +0.0000F, -0.0750F, -0.0050F, 1.1650F); // Raya al medio
        addTeenStyleExtra(true, "PCH_Vanilla_Cornrows", +0.0000F, -0.0760F, -0.0100F, 1.1700F); // Trenzas africanas
        addTeenStyleExtra(true, "PCH_Vanilla_FlatTop", +0.0000F, +0.0000F, -0.0150F, 1.1000F); // Tapa plana
        addTeenStyleExtra(true, "PCH_Vanilla_KateCurly", +0.0000F, -0.1000F, +0.0000F, 1.2100F); // Salmonete (Rizado)
        addTeenStyleExtra(true, "PCH_Vanilla_LeftParting", +0.0000F, -0.0400F, +0.0000F, 1.1250F); // Raya a la izquierda
        addTeenStyleExtra(true, "PCH_Vanilla_LongBraids", +0.0000F, +0.0000F, -0.0050F, 1.0900F); // Trenzas largas
        addTeenStyleExtra(true, "PCH_Vanilla_LongBraids02", +0.0000F, -0.0400F, -0.0100F, 1.1200F); // Trenzas muy largas
        addTeenStyleExtra(true, "PCH_Vanilla_PonyTail", +0.0000F, -0.0850F, -0.0050F, 1.1800F); // Coleta
        addTeenStyleExtra(true, "PCH_Vanilla_Rachel", +0.0000F, +0.0000F, +0.0000F, 1.0950F); // Rachel
        addTeenStyleExtra(true, "PCH_Vanilla_RachelCurly", +0.0000F, +0.0000F, +0.0000F, 1.0950F); // Rachel (Rizado)
        addTeenStyleExtra(true, "PCH_Vanilla_RightParting", +0.0000F, -0.0700F, -0.0050F, 1.1600F); // Raya a la derecha
        addTeenStyleExtra(true, "PCH_Vanilla_ShortCurly", +0.0000F, +0.0050F, +0.0000F, 1.0750F); // Sencillo (Rizado)
        addTeenStyleExtra(true, "PCH_Vanilla_Spike", +0.0000F, -0.0500F, -0.0050F, 1.1450F); // Puntiagudo
        addTeenStyleExtra(true, "PCH_Vanilla_TopCurls", +0.0000F, -0.2000F, +0.0000F, 1.3100F); // Rizado alto
        addTeenStyleExtra(true, "PCH_Vanilla_Bob", +0.0000F, -0.0640F, +0.0000F, 1.1700F); // Bob
        addTeenStyleExtra(true, "PCH_Vanilla_BobCurly", +0.0000F, -0.0650F, +0.0000F, 1.1700F); // Bob (Rizado)
        addTeenStyleExtra(true, "PCH_Vanilla_Buffont", +0.0000F, -0.0090F, +0.0000F, 1.0900F); // Bouffant
        addTeenStyleExtra(true, "PCH_Vanilla_Bun", +0.0000F, -0.1420F, +0.0000F, 1.2600F); // Moño
        addTeenStyleExtra(true, "PCH_Vanilla_BunCurly", +0.0000F, +0.0000F, +0.0000F, 1.0300F); // Moño (Rizado)
        addTeenStyleExtra(true, "PCH_Vanilla_CentrePartingLong", +0.0000F, -0.0320F, -0.0050F, 1.1200F); // Largo con raya al medio
        addTeenStyleExtra(true, "PCH_Vanilla_GreasedBack", +0.0000F, -0.0750F, -0.0100F, 1.1500F); // Engominado atrás
        addTeenStyleExtra(true, "PCH_Vanilla_Grungey", +0.0000F, -0.0450F, -0.0050F, 1.1400F); // Grungey
        addTeenStyleExtra(true, "PCH_Vanilla_Grungey02", +0.0000F, -0.1150F, -0.0100F, 1.2200F); // Grungey largo
        addTeenStyleExtra(true, "PCH_Vanilla_GrungeyBehindEars", +0.0000F, -0.0550F, -0.0050F, 1.1500F); // Grungey tras las orejas
        addTeenStyleExtra(true, "PCH_Vanilla_GrungeyParted", +0.0000F, -0.1300F, -0.0050F, 1.2400F); // Grungey separado
        addTeenStyleExtra(true, "PCH_Vanilla_Kate", +0.0000F, -0.1200F, -0.0050F, 1.2300F); // Largo
        addTeenStyleExtra(true, "PCH_Vanilla_LibertySpikes", +0.0000F, +0.0000F, +0.0000F, 1.0900F); // Pinchos
        addTeenStyleExtra(true, "PCH_Vanilla_Long", +0.0000F, -0.0510F, +0.0000F, 1.1500F); // Media melena
        addTeenStyleExtra(true, "PCH_Vanilla_Longcurly", +0.0000F, -0.0420F, +0.0000F, 1.1400F); // Media melena (Rizado)
        addTeenStyleExtra(true, "PCH_Vanilla_MohawkFan", +0.0000F, +0.0150F, -0.0150F, 1.0300F); // Mohicano
        addTeenStyleExtra(true, "PCH_Vanilla_MohawkFlat", +0.0000F, -0.0060F, -0.0050F, 1.0900F); // Mohicano plano
        addTeenStyleExtra(true, "PCH_Vanilla_MohawkShort", +0.0000F, +0.0190F, -0.0050F, 1.0600F); // Mohicano corto
        addTeenStyleExtra(true, "PCH_Vanilla_MohawkSpike", +0.0000F, +0.0110F, +0.0000F, 1.0700F); // Mohicano pinchos
        addTeenStyleExtra(true, "PCH_Vanilla_OverEye", +0.0000F, -0.1200F, -0.0050F, 1.2300F); // Flequillo derecho
        addTeenStyleExtra(true, "PCH_Vanilla_OverEyeCurly", +0.0000F, -0.1300F, -0.0050F, 1.2400F); // Flequillo (Rizado)
        addTeenStyleExtra(true, "PCH_Vanilla_OverLeftEye", +0.0000F, -0.1000F, +0.0000F, 1.2100F); // Flequillo izquierdo
        addTeenStyleExtra(true, "PCH_Vanilla_PonyTailBraids", +0.0000F, -0.0950F, -0.0050F, 1.1900F); // Coleta con trenzas
        // FIXED92: additional user-approved TEEN FEMALE normal/no-hat calibrations (2026-09-28).
        // Normal hair only. Hat/compressed-hair maps, CHILD, body, scale and headwear remain untouched.
        addTeenStyleExtra(true, "PCH_Spn_M_AlanWake", +0.0000F, +0.0200F, +0.0000F, 1.0000F); // Wake
        addTeenStyleExtra(true, "PCH_Spn_M_Ash", +0.0000F, +0.0190F, +0.0000F, 1.0000F); // Groovy
        addTeenStyleExtra(true, "PCH_Spn_M_AshBowl", +0.0000F, +0.0200F, +0.0000F, 1.0000F); // Thick Bowl Cut
        addTeenStyleExtra(true, "PCH_Spn_M_AshShort", +0.0000F, +0.0180F, +0.0000F, 1.0000F); // Groovy Short
        addTeenStyleExtra(true, "PCH_Spn_M_AshSlick", +0.0000F, +0.0180F, +0.0000F, 1.0000F); // Groovy Slick
        addTeenStyleExtra(true, "PCH_Spn_M_Ashley", +0.0000F, +0.0370F, +0.0000F, 0.9800F); // Ashley
        addTeenStyleExtra(true, "PCH_Spn_M_BobSlick", +0.0000F, +0.0200F, +0.0000F, 1.0000F); // Slicked Back Bob
        addTeenStyleExtra(true, "PCH_Spn_M_Frank", +0.0000F, -0.0060F, -0.0050F, 1.0100F); // Frank
        addTeenStyleExtra(true, "PCH_Spn_M_Jeanette", +0.0000F, +0.0190F, +0.0000F, 1.0000F); // Jeanette
        addTeenStyleExtra(true, "PCH_Spn_M_Logan", +0.0000F, +0.0200F, +0.0000F, 1.0000F); // Logan
        addTeenStyleExtra(true, "PCH_Spn_M_MessyBob", +0.0000F, +0.0180F, +0.0000F, 1.0000F); // Messy Bob
        addTeenStyleExtra(true, "PCH_Spn_M_OvereyeShaggy", +0.0000F, +0.0210F, +0.0000F, 0.9950F); // Over Eye Shaggy
        addTeenStyleExtra(true, "PCH_Spn_M_PartedTail", +0.0000F, +0.0210F, +0.0000F, 1.0000F); // Parted Tail
        addTeenStyleExtra(true, "PCH_Spn_M_Power", +0.0000F, +0.0320F, +0.0000F, 0.9850F); // Power
        addTeenStyleExtra(true, "PCH_Spn_M_PowerTied", +0.0000F, +0.0190F, +0.0000F, 1.0000F); // Power Tied
        addTeenStyleExtra(true, "PCH_Spn_M_RedfieldLow", +0.0000F, +0.0200F, +0.0150F, 1.0000F); // Redfield Low Tail
        addTeenStyleExtra(true, "PCH_Spn_M_RedfieldMessy", +0.0000F, +0.0200F, +0.0000F, 1.0000F); // Redfield Messy Tail
        addTeenStyleExtra(true, "PCH_Spn_M_SpikedMessy", +0.0000F, +0.0170F, +0.0000F, 1.0000F); // Spiked Messy
        addTeenStyleExtra(true, "PCH_Spn_M_StraightCutBob", +0.0000F, +0.0290F, +0.0000F, 0.9900F); // Straight Cut Bob
        addTeenStyleExtra(true, "PCH_Spn_M_Taurie", +0.0000F, +0.0180F, +0.0000F, 1.0000F); // Taurie
        addTeenStyleExtra(true, "PCH_Spn_M_Touka", +0.0000F, +0.0200F, +0.0000F, 1.0000F); // Over Eye Fluffy
        addTeenStyleExtra(true, "PCH_Spn_M_ToukaStraight", +0.0000F, +0.0280F, +0.0000F, 0.9850F); // Over Eye Straight
        addTeenStyleExtra(true, "PCH_Spn_M_Vi", +0.0000F, +0.0200F, +0.0000F, 1.0000F); // Vi
        addTeenStyleExtra(true, "PCH_Spn_M_Zarina", +0.0000F, +0.0200F, +0.0000F, 1.0000F); // Zarina
        addTeenStyleExtra(true, "PCH_Spn_M_ZarinaCurly", +0.0000F, +0.0190F, +0.0000F, 1.0000F); // Zarina (Curly)
        // FIXED91: experimental TEEN FEMALE hat/compressed-hair calibration.
        // Intentionally mirrors the 69 user-approved FIXED90 normal-hair deltas so it can be tested in-game.
        // No body/headwear/world-scale values are changed here.
        addTeenHatStyleExtra(true, "PCH_Harry_F_BRITA", +0.0000F, -0.1180F, +0.0000F, 1.0400F); // BRITA
        addTeenHatStyleExtra(true, "PCH_Harry_F_Grunge01", +0.0000F, +0.0000F, +0.0020F, 1.2500F); // Grunge 01
        addTeenHatStyleExtra(true, "PCH_Harry_F_Merigold", +0.0000F, +0.0000F, +0.0000F, 1.0360F); // Merigold
        addTeenHatStyleExtra(true, "PCH_Harry_F_PonytailLong", +0.0000F, +0.0000F, +0.0020F, 1.2390F); // Ponytail Long
        addTeenHatStyleExtra(true, "PCH_Harry_F_PonytailMedium", +0.0000F, +0.0000F, +0.0010F, 1.1320F); // Ponytail Medium
        addTeenHatStyleExtra(true, "PCH_Harry_F_ThrottleKitty", +0.0000F, +0.0000F, +0.0000F, 1.0310F); // ThrottleKitty
        addTeenHatStyleExtra(true, "PCH_Spn_M_Agent", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Agent
        addTeenHatStyleExtra(true, "PCH_Spn_M_Aigis", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Aeon
        addTeenHatStyleExtra(true, "PCH_Spn_M_AigisAlt", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Aeon Alt
        addTeenHatStyleExtra(true, "PCH_Spn_M_Carlos", +0.0000F, +0.0440F, +0.0000F, 0.9740F); // Oliveira
        addTeenHatStyleExtra(true, "PCH_Spn_M_Curtains", +0.0000F, +0.0030F, +0.0000F, 1.0170F); // Curtains
        addTeenHatStyleExtra(true, "PCH_Spn_M_CurtainsHigh", +0.0000F, +0.0160F, +0.0000F, 1.0020F); // High Curtains
        addTeenHatStyleExtra(true, "PCH_Spn_M_Dante", +0.0000F, +0.0360F, +0.0000F, 0.9890F); // Dante desalineado
        addTeenHatStyleExtra(true, "PCH_Spn_M_DanteParted", +0.0000F, +0.0130F, +0.0000F, 1.0050F); // Dante
        addTeenHatStyleExtra(true, "PCH_Spn_M_ErenBun", +0.0000F, -0.0010F, +0.0000F, 1.0230F); // Coleta Yeager
        addTeenHatStyleExtra(true, "PCH_Spn_M_ErenParted", +0.0000F, +0.0000F, +0.0000F, 1.0210F); // Raya al medio largo
        addTeenHatStyleExtra(true, "PCH_Spn_M_Goth", +0.0000F, +0.0000F, +0.0000F, 1.0240F); // Coletas góticas
        addTeenHatStyleExtra(true, "PCH_Spn_M_Hero", +0.0000F, +0.0180F, +0.0000F, 1.0000F); // Hero
        addTeenHatStyleExtra(true, "PCH_Spn_M_HeroBun", +0.0000F, +0.0230F, +0.0080F, 1.0000F); // Hero Bun
        addTeenHatStyleExtra(true, "PCH_Spn_M_HeroTail", +0.0000F, +0.0210F, +0.0000F, 1.0000F); // zDoNotPickMe
        addTeenHatStyleExtra(true, "PCH_Spn_M_Kotone", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Tonta XXII
        addTeenHatStyleExtra(true, "PCH_Spn_M_KotonePonytail", +0.0000F, +0.0000F, +0.0020F, 1.0210F); // Tonta XXII (Atado)
        addTeenHatStyleExtra(true, "PCH_Spn_M_Kusanagi", +0.0000F, +0.0320F, +0.0000F, 0.9880F); // Kusanagi
        addTeenHatStyleExtra(true, "PCH_Spn_M_Leon", +0.0000F, +0.0000F, +0.0000F, 1.0210F); // Kennedy
        addTeenHatStyleExtra(true, "PCH_Spn_M_LeonLong", +0.0000F, +0.0210F, +0.0000F, 1.0000F); // Kennedy Longer
        addTeenHatStyleExtra(true, "PCH_Spn_M_LeonRE4", +0.0000F, +0.0140F, +0.0120F, 1.0050F); // Kennedy Long
        addTeenHatStyleExtra(true, "PCH_Spn_M_Marie", +0.0000F, +0.0000F, +0.0000F, 1.0180F); // Marie
        addTeenHatStyleExtra(true, "PCH_Spn_M_Mikasa", +0.0000F, +0.0020F, +0.0000F, 1.0080F); // Mikasa
        addTeenHatStyleExtra(true, "PCH_Spn_M_MikasaShort", +0.0000F, +0.0000F, +0.0000F, 1.0190F); // Mikasa corto
        addTeenHatStyleExtra(true, "PCH_Spn_M_MitsuruPonytail", +0.0000F, +0.0000F, +0.0000F, 1.0220F); // Emperatriz (Atado)
        addTeenHatStyleExtra(true, "PCH_Spn_M_P3Hero", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Tonta
        addTeenHatStyleExtra(true, "PCH_Spn_M_Parted", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Encantadora
        addTeenHatStyleExtra(true, "PCH_Spn_M_PartedMedium", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Medio encantadora
        addTeenHatStyleExtra(true, "PCH_Spn_M_PartedSide", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Side Parted
        addTeenHatStyleExtra(true, "PCH_Spn_M_PartedSideMedium", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Side Parted Medium
        addTeenHatStyleExtra(true, "PCH_Spn_M_Princely", +0.0000F, +0.0380F, +0.0000F, 0.9830F); // Princely
        addTeenHatStyleExtra(true, "PCH_Spn_M_PrincelyShort", +0.0000F, +0.0220F, +0.0000F, 1.0000F); // Princely Cropped
        addTeenHatStyleExtra(true, "PCH_Spn_M_Redfield", +0.0000F, +0.0000F, +0.0000F, 1.0230F); // Redfield
        addTeenHatStyleExtra(true, "PCH_Spn_M_Rei", +0.0000F, +0.0720F, +0.0000F, 0.9530F); // Rei
        addTeenHatStyleExtra(true, "PCH_Spn_M_Rick", +0.0000F, +0.0000F, +0.0000F, 1.0160F); // Grimes
        addTeenHatStyleExtra(true, "PCH_Spn_M_RickShow", +0.0000F, +0.0000F, +0.0000F, 1.0140F); // Hacia atrás engominado
        addTeenHatStyleExtra(true, "PCH_Spn_M_Shaggy", +0.0000F, +0.0000F, +0.0000F, 1.0090F); // Shaggy
        addTeenHatStyleExtra(true, "PCH_Spn_M_ShaggyCurly", +0.0000F, +0.0000F, +0.0000F, 1.0100F); // Shaggy (Curly)
        addTeenHatStyleExtra(true, "PCH_Spn_M_Sideswept", +0.0000F, +0.0000F, +0.0000F, 1.0240F); // Peinado hacia un lado
        addTeenHatStyleExtra(true, "PCH_Spn_M_SpikedForward", +0.0000F, +0.0000F, +0.0000F, 1.0150F); // Spiked Forward
        addTeenHatStyleExtra(true, "PCH_Spn_M_SpikedUp", +0.0000F, +0.0000F, +0.0000F, 1.0140F); // Spiked Up
        addTeenHatStyleExtra(true, "PCH_Spn_M_Surfer", +0.0000F, +0.0000F, +0.0000F, 1.0130F); // Surfer
        addTeenHatStyleExtra(true, "PCH_Spn_M_Valentine", +0.0000F, -0.0060F, +0.0000F, 1.0300F); // Valentine
        addTeenHatStyleExtra(true, "PCH_Spn_M_Vergil", +0.0000F, +0.0000F, -0.0050F, 1.0110F); // Vergil
        addTeenHatStyleExtra(true, "PCH_Spn_M_Yu", +0.0000F, +0.0000F, +0.0000F, 1.0200F); // Corte tazón
        addTeenHatStyleExtra(true, "PCH_Spn_M_Yukari", +0.0000F, +0.0000F, +0.0000F, 1.0170F); // Amantes
        addTeenHatStyleExtra(true, "PCH_Spn_M_YukariAlt", +0.0000F, +0.0000F, +0.0000F, 1.0160F); // Amantes corto
        addTeenHatStyleExtra(true, "PCH_Spn_M_YukariBun", +0.0000F, +0.0000F, +0.0000F, 1.0190F); // Amantes (Atado)
        addTeenHatStyleExtra(true, "PCH_Vanilla_Back", +0.0000F, -0.1500F, +0.0000F, 1.2600F); // Recogido
        addTeenHatStyleExtra(true, "PCH_Vanilla_Braids", +0.0000F, -0.0060F, +0.0000F, 1.0900F); // Trenzas
        addTeenHatStyleExtra(true, "PCH_Vanilla_CentreParting", +0.0000F, -0.0750F, -0.0050F, 1.1650F); // Raya al medio
        addTeenHatStyleExtra(true, "PCH_Vanilla_Cornrows", +0.0000F, -0.0760F, -0.0100F, 1.1700F); // Trenzas africanas
        addTeenHatStyleExtra(true, "PCH_Vanilla_FlatTop", +0.0000F, +0.0000F, -0.0150F, 1.1000F); // Tapa plana
        addTeenHatStyleExtra(true, "PCH_Vanilla_KateCurly", +0.0000F, -0.1000F, +0.0000F, 1.2100F); // Salmonete (Rizado)
        addTeenHatStyleExtra(true, "PCH_Vanilla_LeftParting", +0.0000F, -0.0400F, +0.0000F, 1.1250F); // Raya a la izquierda
        addTeenHatStyleExtra(true, "PCH_Vanilla_LongBraids", +0.0000F, +0.0000F, -0.0050F, 1.0900F); // Trenzas largas
        addTeenHatStyleExtra(true, "PCH_Vanilla_LongBraids02", +0.0000F, -0.0400F, -0.0100F, 1.1200F); // Trenzas muy largas
        addTeenHatStyleExtra(true, "PCH_Vanilla_PonyTail", +0.0000F, -0.0850F, -0.0050F, 1.1800F); // Coleta
        addTeenHatStyleExtra(true, "PCH_Vanilla_Rachel", +0.0000F, +0.0000F, +0.0000F, 1.0950F); // Rachel
        addTeenHatStyleExtra(true, "PCH_Vanilla_RachelCurly", +0.0000F, +0.0000F, +0.0000F, 1.0950F); // Rachel (Rizado)
        addTeenHatStyleExtra(true, "PCH_Vanilla_RightParting", +0.0000F, -0.0700F, -0.0050F, 1.1600F); // Raya a la derecha
        addTeenHatStyleExtra(true, "PCH_Vanilla_ShortCurly", +0.0000F, +0.0050F, +0.0000F, 1.0750F); // Sencillo (Rizado)
        addTeenHatStyleExtra(true, "PCH_Vanilla_Spike", +0.0000F, -0.0500F, -0.0050F, 1.1450F); // Puntiagudo
        addTeenHatStyleExtra(true, "PCH_Vanilla_TopCurls", +0.0000F, -0.2000F, +0.0000F, 1.3100F); // Rizado alto
        addTeenHatModelExtra(false, "child_spn_m_taurieg1", +0.0000F, +0.0330F, +0.0000F, 1.0000F); // Taurie
        addTeenHatModelExtra(false, "child_spn_m_vig1", +0.0000F, +0.0210F, -0.0050F, 1.0230F); // Vi
        addTeenHatModelExtra(false, "child_spn_m_zarinag1", +0.0000F, -0.0660F, -0.0130F, 1.0980F); // Zarina (Curly)
        addTeenHatModelExtra(false, "m_hair_shorthat", +0.0000F, -0.0190F, +0.0150F, 1.0980F); // Frank
        addTeenHatModelExtra(false, "bob_hair_cornrows", +0.0000F, -0.1020F, -0.0000F, 1.2000F); // Trenzas africanas
        addTeenHatModelExtra(false, "bob_hair_mohawkshort", +0.0000F, -0.0700F, -0.0050F, 1.1600F); // Mohicano plano
        addTeenHatModelExtra(false, "bob_hair_ponytailbraided", +0.0000F, -0.1550F, -0.0050F, 1.2600F); // Coleta con trenzas
        addTeenHatModelExtra(false, "bob_hair_shorthatcurly", +0.0000F, -0.1200F, -0.0050F, 1.2130F); // Afro corto
        addTeenHatModelExtra(false, "child_harry_m_aiteroncat1", +0.0000F, +0.0000F, +0.0000F, 1.0220F); // Aiteron
        addTeenHatModelExtra(false, "child_harry_m_soulfilchercat1", +0.0000F, +0.0000F, +0.0000F, 1.0210F); // Soulfilcher
        addTeenHatModelExtra(false, "m_hair_braids_g1", +0.0000F, -0.0060F, +0.0650F, 1.1800F); // Trenzas
        addTeenHatModelExtra(false, "m_hair_buffont_g1", +0.0000F, +0.0100F, -0.0600F, 1.0250F); // Bouffant
        addTeenHatModelExtra(false, "m_hair_centrepartinglong_g1", +0.0000F, -0.0100F, +0.1750F, 1.2800F); // Largo con raya al medio
        addTeenHatModelExtra(false, "m_hair_fabian_g1", +0.0000F, -0.0010F, +0.0500F, 1.1600F); // Melena larga
        addTeenHatModelExtra(false, "m_hair_fabiancurly_g1", +0.0000F, -0.0020F, +0.0750F, 1.1800F); // Melena larga (Rizado)
        addTeenHatModelExtra(false, "m_hair_greasedback_g1", +0.0000F, -0.0100F, +0.1900F, 1.3000F); // Engominado atrás
        addTeenHatModelExtra(false, "m_hair_grungey_g1", +0.0000F, -0.0150F, +0.2050F, 1.3200F); // Grungey
        addTeenHatModelExtra(false, "m_hair_grungeybehindears_g1", +0.0000F, -0.0100F, +0.1650F, 1.2800F); // Grungey tras las orejas
        addTeenHatModelExtra(false, "m_hair_libertyspikes_g1", +0.0000F, +0.0060F, -0.0300F, 1.0000F); // Pinchos
        addTeenHatModelExtra(false, "m_hair_longbraided_g1", +0.0000F, +0.0010F, +0.0000F, 1.1000F); // Trenzas largas
        addTeenHatModelExtra(false, "m_hair_mullet_g1", +0.0000F, -0.0380F, +0.0000F, 1.1400F); // Salmonete
        addTeenHatModelExtra(false, "m_hair_mulletcurly_g1", +0.0000F, +0.0030F, +0.1000F, 1.2000F); // Salmonete (Rizado)
        addTeenHatModelExtra(false, "m_hair_ponytail", +0.0000F, -0.1550F, -0.0050F, 1.2640F); // Coleta
        addTeenHatModelExtra(false, "m_hair_shorthat", +0.0000F, -0.1550F, -0.0050F, 1.2600F); // Corto

        // Same raw mesh token = useful starting point even across the two TEEN heads.
        // Explicit TEEN Female measurements below replace the copied value per model.
        TEEN_FEMALE_HAT_MODEL_EXTRA_ADJUSTMENTS.putAll(TEEN_MALE_HAT_MODEL_EXTRA_ADJUSTMENTS);
        addTeenHatModelExtra(true, "bob_hair_ponytailbraided", +0.0000F, -0.0000F, +0.0000F, 1.0580F); // Coleta con trenzas
        addTeenHatModelExtra(true, "f_hair_bob_g1", +0.0000F, +0.0050F, +0.0030F, 1.0970F); // Bob
        addTeenHatModelExtra(true, "f_hair_buffont_g1", +0.0000F, +0.0000F, -0.0460F, 1.0450F); // Bouffant
        addTeenHatModelExtra(true, "m_hair_ponytail", +0.0000F, +0.0300F, -0.0100F, 0.9430F); // Coleta

        addTeenHatStyleExtra(false, "PCH_Harry_M_ObiWanCannoli", +0.0000F, +0.0000F, +0.0000F, 1.0050F); // Obi Wan Cannoli
        addTeenHatStyleExtra(false, "PCH_Spn_M_Agent", +0.0000F, -0.0300F, -0.0080F, 1.0700F); // Agent
        addTeenHatStyleExtra(false, "PCH_Spn_M_Aigis", +0.0000F, +0.0050F, -0.0100F, 1.0350F); // Aeon
        addTeenHatStyleExtra(false, "PCH_Spn_M_AigisAlt", +0.0000F, +0.0050F, -0.0100F, 1.0330F); // Aeon Alt
        addTeenHatStyleExtra(false, "PCH_Spn_M_AlanWake", +0.0000F, -0.0150F, -0.0130F, 1.0600F); // Wake
        addTeenHatStyleExtra(false, "PCH_Spn_M_Ash", +0.0000F, -0.0500F, -0.0160F, 1.0900F); // Groovy
        addTeenHatStyleExtra(false, "PCH_Spn_M_AshBowl", +0.0000F, -0.0500F, -0.0090F, 1.0800F); // Thick Bowl Cut
        addTeenHatStyleExtra(false, "PCH_Spn_M_AshShort", +0.0000F, -0.0400F, -0.0150F, 1.0800F); // Groovy Short
        addTeenHatStyleExtra(false, "PCH_Spn_M_AshSlick", +0.0000F, -0.0700F, -0.0210F, 1.1100F); // Groovy Slick
        addTeenHatStyleExtra(false, "PCH_Spn_M_Ashley", +0.0000F, +0.0100F, -0.0080F, 1.0300F); // Ashley
        addTeenHatStyleExtra(false, "PCH_Spn_M_BobSlick", +0.0000F, -0.0450F, -0.0130F, 1.0800F); // Slicked Back Bob
        addTeenHatStyleExtra(false, "PCH_Spn_M_Carlos", +0.0000F, +0.0100F, -0.0090F, 1.0180F); // Oliveira
        addTeenHatStyleExtra(false, "PCH_Spn_M_Curtains", +0.0000F, -0.0400F, -0.0120F, 1.0800F); // Curtains
        addTeenHatStyleExtra(false, "PCH_Spn_M_CurtainsHigh", +0.0000F, -0.1200F, -0.0220F, 1.1700F); // High Curtains
        addTeenHatStyleExtra(false, "PCH_Spn_M_Dante", +0.0000F, -0.0250F, -0.0160F, 1.0700F); // Dante desalineado
        addTeenHatStyleExtra(false, "PCH_Spn_M_DanteParted", +0.0000F, -0.0370F, -0.0080F, 1.0800F); // Dante
        addTeenHatStyleExtra(false, "PCH_Spn_M_ErenBun", +0.0000F, -0.1230F, -0.0170F, 1.1600F); // Coleta Yeager
        addTeenHatStyleExtra(false, "PCH_Spn_M_ErenParted", +0.0000F, +0.0010F, -0.0080F, 1.0400F); // Raya al medio largo
        addTeenHatStyleExtra(false, "PCH_Spn_M_Frank", +0.0000F, -0.1700F, -0.0090F, 1.2800F); // Frank
        addTeenHatStyleExtra(false, "PCH_Spn_M_Goth", +0.0000F, -0.1200F, -0.0210F, 1.1700F); // Coletas g�ticas
        addTeenHatStyleExtra(false, "PCH_Spn_M_Hero", +0.0000F, -0.0800F, -0.0160F, 1.1300F); // Hero
        addTeenHatStyleExtra(false, "PCH_Spn_M_HeroBun", +0.0000F, -0.0950F, -0.0200F, 1.1400F); // Hero Bun
        addTeenHatStyleExtra(false, "PCH_Spn_M_HeroTail", +0.0000F, -0.0850F, -0.0160F, 1.1300F); // zDoNotPickMe
        addTeenHatStyleExtra(false, "PCH_Spn_M_Jeanette", +0.0000F, -0.1550F, -0.0270F, 1.1900F); // Jeanette
        addTeenHatStyleExtra(false, "PCH_Spn_M_Kotone", +0.0000F, -0.0560F, -0.0150F, 1.1000F); // Tonto XXII
        addTeenHatStyleExtra(false, "PCH_Spn_M_KotonePonytail", +0.0000F, -0.0450F, -0.0120F, 1.0900F); // Tonto XXII (Atado)
        addTeenHatStyleExtra(false, "PCH_Spn_M_Kusanagi", +0.0000F, -0.1350F, -0.0210F, 1.1800F); // Kusanagi
        addTeenHatStyleExtra(false, "PCH_Spn_M_Leon", +0.0000F, -0.1450F, -0.0230F, 1.1900F); // Kennedy
        addTeenHatStyleExtra(false, "PCH_Spn_M_LeonLong", +0.0000F, -0.1000F, -0.0190F, 1.1500F); // Kennedy Longer
        addTeenHatStyleExtra(false, "PCH_Spn_M_LeonRE4", +0.0000F, -0.1350F, -0.0230F, 1.1800F); // Kennedy Long
        addTeenHatStyleExtra(false, "PCH_Spn_M_Logan", +0.0000F, -0.0500F, -0.0150F, 1.0900F); // Logan
        addTeenHatStyleExtra(false, "PCH_Spn_M_Marie", +0.0000F, -0.0170F, -0.0110F, 1.0500F); // Marie
        addTeenHatStyleExtra(false, "PCH_Spn_M_MessyBob", +0.0000F, -0.0200F, -0.0150F, 1.0700F); // Messy Bob
        addTeenHatStyleExtra(false, "PCH_Spn_M_Mikasa", +0.0000F, +0.0000F, -0.0100F, 1.0400F); // Mikasa
        addTeenHatStyleExtra(false, "PCH_Spn_M_MikasaShort", +0.0000F, -0.0160F, -0.0110F, 1.0500F); // Mikasa corto
        addTeenHatStyleExtra(false, "PCH_Spn_M_MitsuruPonytail", +0.0000F, -0.0550F, -0.0140F, 1.1000F); // Emperatriz (Atado)
        addTeenHatStyleExtra(false, "PCH_Spn_M_OvereyeShaggy", +0.0000F, -0.0100F, -0.0110F, 1.0500F); // Over Eye Shaggy
        addTeenHatStyleExtra(false, "PCH_Spn_M_OvereyeShaggyLeft", +0.0000F, -0.0450F, -0.0110F, 1.0900F); // Over Eye Shaggy Left
        addTeenHatStyleExtra(false, "PCH_Spn_M_P3Hero", +0.0000F, -0.0050F, -0.0100F, 1.0470F); // Tonto
        addTeenHatStyleExtra(false, "PCH_Spn_M_Parted", +0.0000F, -0.0810F, -0.0180F, 1.1300F); // Encantador
        addTeenHatStyleExtra(false, "PCH_Spn_M_PartedMedium", +0.0000F, -0.0980F, -0.0200F, 1.1500F); // Medio encantador
        addTeenHatStyleExtra(false, "PCH_Spn_M_PartedSide", +0.0000F, -0.0550F, -0.0150F, 1.1000F); // Side Parted
        addTeenHatStyleExtra(false, "PCH_Spn_M_PartedSideMedium", +0.0000F, -0.0700F, -0.0170F, 1.1200F); // Side Parted Medium
        addTeenHatStyleExtra(false, "PCH_Spn_M_PartedTail", +0.0000F, -0.0550F, -0.0120F, 1.1000F); // Parted Tail
        addTeenHatStyleExtra(false, "PCH_Spn_M_Power", +0.0000F, -0.0300F, -0.0130F, 1.0800F); // Power
        addTeenHatStyleExtra(false, "PCH_Spn_M_PowerTied", +0.0000F, -0.0550F, -0.0120F, 1.1000F); // Power Tied
        addTeenHatStyleExtra(false, "PCH_Spn_M_Princely", +0.0000F, -0.0650F, -0.0130F, 1.1100F); // Princely
        addTeenHatStyleExtra(false, "PCH_Spn_M_PrincelyShort", +0.0000F, -0.0950F, -0.0180F, 1.1400F); // Princely Cropped
        addTeenHatStyleExtra(false, "PCH_Spn_M_Redfield", +0.0000F, -0.1000F, -0.0240F, 1.1400F); // Redfield
        addTeenHatStyleExtra(false, "PCH_Spn_M_RedfieldLow", +0.0000F, -0.0550F, -0.0150F, 1.1000F); // Redfield Low Tail
        addTeenHatStyleExtra(false, "PCH_Spn_M_RedfieldMessy", +0.0000F, -0.0800F, -0.0230F, 1.1200F); // Redfield Messy Tail
        addTeenHatStyleExtra(false, "PCH_Spn_M_Rei", +0.0000F, -0.0200F, -0.0080F, 1.0600F); // Rei
        addTeenHatStyleExtra(false, "PCH_Spn_M_Rick", +0.0000F, -0.0700F, -0.0160F, 1.1200F); // Grimes
        addTeenHatStyleExtra(false, "PCH_Spn_M_RickShow", +0.0000F, +0.0180F, -0.0040F, 1.0200F); // Hacia atr�s engominado
        addTeenHatStyleExtra(false, "PCH_Spn_M_Shaggy", +0.0000F, -0.0400F, -0.0120F, 1.0800F); // Shaggy
        addTeenHatStyleExtra(false, "PCH_Spn_M_ShaggyCurly", +0.0000F, -0.0350F, -0.0130F, 1.0800F); // Shaggy (Curly)
        addTeenHatStyleExtra(false, "PCH_Spn_M_Sideswept", +0.0000F, -0.0950F, -0.0140F, 1.1300F); // Peinado hacia un lado
        addTeenHatStyleExtra(false, "PCH_Spn_M_SpikedForward", +0.0000F, -0.1500F, -0.0030F, 1.2500F); // Spiked Forward
        addTeenHatStyleExtra(false, "PCH_Spn_M_SpikedMessy", +0.0000F, -0.1050F, +0.0000F, 1.2000F); // Spiked Messy
        addTeenHatStyleExtra(false, "PCH_Spn_M_SpikedUp", +0.0000F, -0.2300F, -0.0130F, 1.3200F); // Spiked Up
        addTeenHatStyleExtra(false, "PCH_Spn_M_StraightCutBob", +0.0000F, -0.0100F, -0.0150F, 1.0500F); // Straight Cut Bob
        addTeenHatStyleExtra(false, "PCH_Spn_M_Surfer", +0.0000F, -0.1600F, -0.0200F, 1.1900F); // Surfer
        addTeenHatStyleExtra(false, "PCH_Spn_M_Taurie", +0.0000F, -0.0550F, -0.0170F, 1.1000F); // Taurie
        addTeenHatStyleExtra(false, "PCH_Spn_M_Touka", +0.0000F, -0.0550F, -0.0220F, 1.1000F); // Over Eye Fluffy
        addTeenHatStyleExtra(false, "PCH_Spn_M_ToukaStraight", +0.0000F, -0.0800F, -0.0210F, 1.1200F); // Over Eye Straight
        addTeenHatStyleExtra(false, "PCH_Spn_M_Valentine", +0.0000F, -0.1150F, -0.0220F, 1.1600F); // Valentine
        addTeenHatStyleExtra(false, "PCH_Spn_M_Vergil", +0.0000F, -0.0560F, -0.0100F, 1.1000F); // Vergil
        addTeenHatStyleExtra(false, "PCH_Spn_M_Vi", +0.0000F, -0.0150F, -0.0090F, 1.0500F); // Vi
        addTeenHatStyleExtra(false, "PCH_Spn_M_Yu", +0.0000F, +0.0000F, +0.0000F, 1.0300F); // Corte taz�n
        addTeenHatStyleExtra(false, "PCH_Spn_M_Yukari", +0.0000F, -0.0400F, -0.0180F, 1.0900F); // Amantes
        addTeenHatStyleExtra(false, "PCH_Spn_M_YukariAlt", +0.0000F, -0.0250F, -0.0140F, 1.0700F); // Amantes corto
        addTeenHatStyleExtra(false, "PCH_Spn_M_YukariBun", +0.0000F, -0.0400F, -0.0150F, 1.0800F); // Amantes (Atado)
        addTeenHatStyleExtra(false, "PCH_Spn_M_Zarina", +0.0000F, -0.0450F, -0.0140F, 1.0800F); // Zarina
        addTeenHatStyleExtra(false, "PCH_Spn_M_ZarinaCurly", +0.0000F, +0.0000F, -0.0090F, 1.0400F); // Zarina (Curly)
    }

    static {
        for (Map.Entry<String, StyleAdjustment> entry : TEEN_FEMALE_STYLE_EXTRA_ADJUSTMENTS.entrySet()) {
            TEEN_FEMALE_HAT_STYLE_EXTRA_ADJUSTMENTS.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    static {
        addTeenFemaleHatDelta("bob_hair_cornrows", +0.0000F, -0.0300F, -0.0100F, 1.0000F);
        addTeenFemaleHatDelta("bob_hair_mohawkshort", +0.0000F, -0.0300F, -0.0100F, 1.0000F);
        addTeenFemaleHatDelta("child_harry_f_brita", +0.0000F, +0.0000F, +0.0000F, 0.9800F);
        addTeenFemaleHatDelta("child_harry_f_grunge01", +0.0000F, +0.0000F, +0.0000F, 0.9600F);
        addTeenFemaleHatDelta("child_harry_f_grunge01cat1", +0.000000F, -0.015000F, -0.145000F, +0.877850F);
        addTeenFemaleHatDelta("child_harry_f_merigold", +0.0000F, +0.0000F, +0.0000F, 0.9750F);
        addTeenFemaleHatDelta("child_harry_f_ponytaillong", +0.0000F, +0.0000F, +0.0000F, 0.9700F);
        addTeenFemaleHatDelta("child_harry_f_ponytailmedium", +0.0000F, +0.0000F, +0.0000F, 0.9650F);
        addTeenFemaleHatDelta("child_harry_f_throttlekitty", +0.0000F, +0.0000F, +0.0000F, 0.9600F);
        addTeenFemaleHatDelta("child_spn_m_agentg1", +0.0000F, +0.0000F, -0.0200F, 1.0050F);
        addTeenFemaleHatDelta("child_spn_m_aigisaltg4", +0.0000F, +0.0000F, -0.0100F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_aigisg1", +0.0000F, +0.0000F, -0.0120F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_aigisg4", +0.0000F, +0.0000F, -0.0100F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_ashg4", +0.000000F, +0.000000F, -0.020000F, +1.000000F);
        addTeenFemaleHatDelta("child_spn_m_ashleyg1", +0.0000F, +0.0000F, -0.0100F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_ashleyg4", +0.0000F, +0.0000F, -0.0100F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_carlosg1", +0.000000F, +0.000000F, -0.015000F, +1.015000F);
        addTeenFemaleHatDelta("child_spn_m_curtainsg1", +0.0000F, -0.0500F, -0.0200F, 1.0500F);
        addTeenFemaleHatDelta("child_spn_m_curtainshighg1", +0.000000F, -0.080000F, -0.020000F, +1.085000F);
        addTeenFemaleHatDelta("child_spn_m_danteg4", +0.0000F, -0.0500F, -0.0100F, 1.0600F);
        addTeenFemaleHatDelta("child_spn_m_erenbung1", +0.000000F, -0.005000F, -0.020000F, +1.000000F);
        addTeenFemaleHatDelta("child_spn_m_erenpartedg4", +0.0000F, +0.0000F, -0.0100F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_gothg1", +0.0000F, +0.0000F, -0.0100F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_herog4", +0.0000F, -0.0500F, -0.0200F, 1.0550F);
        addTeenFemaleHatDelta("child_spn_m_kotoneponytailg1", +0.0000F, +0.0000F, -0.0200F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_kusanagig1", +0.000000F, +0.000000F, -0.015000F, +1.010000F);
        addTeenFemaleHatDelta("child_spn_m_leong1", +0.000000F, -0.095000F, -0.040000F, +1.080375F);
        addTeenFemaleHatDelta("child_spn_m_leonlongg4", +0.0000F, -0.0700F, -0.0200F, 1.0800F);
        addTeenFemaleHatDelta("child_spn_m_makotog1", +0.0000F, +0.0080F, -0.0120F, 0.9950F);
        addTeenFemaleHatDelta("child_spn_m_marieg4", +0.0000F, +0.0000F, -0.0100F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_mikasashortg1", +0.0000F, +0.0000F, -0.0100F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_mitsuruponytailg1", +0.0000F, +0.0000F, -0.0200F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_overeyeshaggyg1", +0.000000F, +0.000000F, -0.015000F, +1.015000F);
        addTeenFemaleHatDelta("child_spn_m_partedg1", +0.000000F, -0.025000F, -0.025000F, +1.025000F);
        addTeenFemaleHatDelta("child_spn_m_partedmediumg1", +0.000000F, -0.005000F, -0.020000F, +1.015000F);
        addTeenFemaleHatDelta("child_spn_m_partedsideg1", +0.0000F, +0.0000F, -0.0200F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_partedsidemediumg1", +0.0000F, +0.0000F, -0.0200F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_powerg4", +0.0000F, +0.0000F, -0.0100F, 1.0100F);
        addTeenFemaleHatDelta("child_spn_m_powertiedg1", +0.000000F, -0.020000F, -0.015000F, +1.030200F);
        addTeenFemaleHatDelta("child_spn_m_princelyg4", +0.0000F, -0.0700F, -0.0200F, 1.0800F);
        addTeenFemaleHatDelta("child_spn_m_redfieldg1", +0.000000F, +0.000000F, -0.015000F, +1.000000F);
        addTeenFemaleHatDelta("child_spn_m_redfieldlowg4", +0.0000F, +0.0000F, -0.0300F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_redfieldmessyg4", +0.0000F, +0.0000F, -0.0200F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_reig4", +0.0000F, -0.0600F, -0.0200F, 1.0600F);
        addTeenFemaleHatDelta("child_spn_m_rickg1", +0.000000F, -0.020000F, -0.020000F, +1.035250F);
        addTeenFemaleHatDelta("child_spn_m_shaggycurlyg4", +0.0000F, +0.0000F, -0.0200F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_shaggyg4", +0.0000F, +0.0000F, -0.0200F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_sidesweptg1", +0.000000F, -0.010000F, -0.015000F, +1.010000F);
        addTeenFemaleHatDelta("child_spn_m_surferg1", +0.0000F, +0.0000F, -0.0200F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_taurieg1", +0.000000F, -0.010000F, -0.015000F, +1.000000F);
        addTeenFemaleHatDelta("child_spn_m_toukag4", +0.0000F, -0.0500F, -0.0200F, 1.0500F);
        addTeenFemaleHatDelta("child_spn_m_toukastraightg4", +0.0000F, -0.0600F, -0.0200F, 1.0700F);
        addTeenFemaleHatDelta("child_spn_m_valentineg1", +0.0000F, +0.0000F, -0.0270F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_valentineg4", +0.0000F, +0.0000F, -0.0100F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_vig1", +0.0000F, -0.0200F, -0.0100F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_yug1", +0.0000F, +0.0100F, -0.0100F, 1.0000F);
        addTeenFemaleHatDelta("child_spn_m_zarinag1", +0.000000F, +0.000000F, -0.010000F, +0.985000F);
        addTeenFemaleHatDelta("f_hair_bob_g4", -0.0000F, +0.0700F, +0.0750F, 1.0000F);
        addTeenFemaleHatDelta("f_hair_bobcurly_g1", +0.0000F, +0.0700F, +0.0700F, 1.0000F);
        addTeenFemaleHatDelta("f_hair_bobcurly_g4", +0.0000F, +0.0600F, +0.0750F, 1.0000F);
        addTeenFemaleHatDelta("f_hair_bound_g1", +0.0000F, +0.1400F, +0.1600F, 1.0000F);
        addTeenFemaleHatDelta("f_hair_centrepartinglong_g1", +0.0000F, +0.0400F, +0.0300F, 1.0000F);
        addTeenFemaleHatDelta("f_hair_centrepartinglong_g4", +0.0000F, +0.0300F, +0.0600F, 1.0200F);
        addTeenFemaleHatDelta("f_hair_greasedback_g1", +0.0000F, +0.0800F, +0.0850F, 1.0000F);
        addTeenFemaleHatDelta("f_hair_grungey02_g1", +0.0000F, +0.1150F, +0.1350F, 1.0000F);
        addTeenFemaleHatDelta("f_hair_grungey_g1", +0.0000F, +0.0400F, +0.0700F, 1.0150F);
        addTeenFemaleHatDelta("f_hair_grungeybehindears_g1", +0.0000F, +0.0600F, +0.0800F, 1.0000F);
        addTeenFemaleHatDelta("f_hair_grungeyparting_g1", +0.0000F, +0.1250F, +0.1450F, 1.0000F);
        addTeenFemaleHatDelta("f_hair_hat", +0.0000F, +0.0400F, +0.0200F, 1.0000F);
        addTeenFemaleHatDelta("f_hair_long_g1", +0.0000F, +0.0600F, +0.0600F, 1.0000F);
        addTeenFemaleHatDelta("f_hair_longcurly_g1", +0.0000F, +0.0500F, +0.0500F, 1.0000F);
        addTeenFemaleHatDelta("f_hair_mullet_g1", +0.0000F, +0.1200F, +0.1400F, 1.0000F);
        addTeenFemaleHatDelta("f_hair_mulletcurly_g1", +0.0000F, +0.0960F, +0.1100F, 1.0000F);
        addTeenFemaleHatDelta("f_hair_overeye_g1", +0.0000F, +0.1150F, +0.1250F, 1.0000F);
        addTeenFemaleHatDelta("f_hair_overeyecurly_g1", +0.0000F, +0.1250F, +0.1400F, 1.0000F);
        addTeenFemaleHatDelta("f_hair_rachel_g1", +0.000000F, +0.004000F, +0.028000F, +1.030200F);
        addTeenFemaleHatDelta("f_hair_rachel_g4", +0.0000F, -0.0100F, +0.1200F, 1.1050F);
        addTeenFemaleHatDelta("f_hair_rachelcurly_g1", +0.0000F, +0.0040F, +0.0070F, 1.0100F);
        addTeenFemaleHatDelta("f_hair_rachelcurly_g4", +0.0000F, -0.0100F, +0.1400F, 1.1350F);
        addTeenFemaleHatDelta("kate_hair_hatcurly", +0.000000F, -0.152000F, -0.007000F, +1.165000F);
        addTeenFemaleHatDelta("m_hair_hatlong", +0.0000F, +0.0700F, +0.0800F, 1.0000F);
        addTeenFemaleHatDelta("m_hair_hatlongcurly", +0.0000F, +0.1100F, +0.1200F, 1.0000F);
        addTeenFemaleHatDelta("m_hair_ponytail", +0.0000F, +0.0300F, +0.0200F, 1.0600F);
        addTeenFemaleHatDelta("m_hair_shorthat", +0.0000F, -0.0150F, -0.0100F, 1.0000F);
        addTeenFemaleHatDelta("child_harry_f_merigoldcat1", +0.000000F, -0.005000F, +0.055000F, +1.050000F);
        addTeenFemaleHatDelta("child_harry_f_ponytaillongcat1", +0.000000F, -0.015000F, -0.065000F, +0.950000F);
        addTeenFemaleHatDelta("child_harry_f_ponytailmediumcat1", +0.000000F, -0.015000F, +0.000000F, +1.000000F);
        addTeenFemaleHatDelta("child_harry_f_throttlekittycat1", +0.000000F, -0.010000F, +0.020000F, +1.025000F);
        addTeenFemaleHatDelta("child_spn_m_alanwakeg1", +0.000000F, +0.000000F, -0.015000F, +1.000000F);
        addTeenFemaleHatDelta("child_spn_m_ashg1", +0.000000F, +0.000000F, -0.015000F, +1.000000F);
        addTeenFemaleHatDelta("child_spn_m_ashslickg1", +0.000000F, +0.000000F, -0.015000F, +1.000000F);
        addTeenFemaleHatDelta("child_spn_m_bobslickg1", +0.000000F, +0.000000F, -0.015000F, +1.000000F);
        addTeenFemaleHatDelta("child_spn_m_danteg1", +0.000000F, -0.010000F, -0.015000F, +1.015000F);
        addTeenFemaleHatDelta("child_spn_m_erenpartedg1", +0.000000F, +0.000000F, -0.015000F, +1.000000F);
        addTeenFemaleHatDelta("child_spn_m_herobung1", +0.000000F, +0.000000F, -0.025000F, +1.000000F);
        addTeenFemaleHatDelta("child_spn_m_herog1", +0.000000F, +0.000000F, -0.010000F, +1.010000F);
        addTeenFemaleHatDelta("child_spn_m_herotailg1", +0.000000F, +0.000000F, -0.010000F, +1.010000F);
        addTeenFemaleHatDelta("child_spn_m_jeanetteg1", +0.000000F, -0.060000F, -0.025000F, +1.060000F);
        addTeenFemaleHatDelta("child_spn_m_kotoneg1", +0.000000F, +0.000000F, -0.015000F, +1.000000F);
        addTeenFemaleHatDelta("child_spn_m_leonlongg1", +0.000000F, -0.035000F, -0.015000F, +1.050000F);
        addTeenFemaleHatDelta("child_spn_m_logang1", +0.000000F, +0.000000F, -0.015000F, +1.000000F);
        addTeenFemaleHatDelta("child_spn_m_messybobg1", +0.000000F, +0.000000F, -0.015000F, +1.000000F);
        addTeenFemaleHatDelta("child_spn_m_mikasag1", +0.000000F, +0.000000F, -0.015000F, +1.000000F);
        addTeenFemaleHatDelta("child_spn_m_partedtailg1", +0.000000F, -0.045000F, -0.015000F, +1.055000F);
        addTeenFemaleHatDelta("child_spn_m_powerg1", +0.000000F, +0.000000F, -0.020000F, +1.010000F);
        addTeenFemaleHatDelta("child_spn_m_princelyg1", +0.000000F, -0.085000F, -0.020000F, +1.085000F);
        addTeenFemaleHatDelta("child_spn_m_redfieldlowg1", +0.000000F, -0.025000F, -0.030000F, +1.035000F);
        addTeenFemaleHatDelta("child_spn_m_reig1", +0.000000F, -0.005000F, -0.020000F, +1.010000F);
        addTeenFemaleHatDelta("child_spn_m_rickshowg1", +0.000000F, +0.055000F, -0.020000F, +0.960000F);
        addTeenFemaleHatDelta("child_spn_m_shaggycurlyg1", +0.000000F, +0.005000F, -0.015000F, +1.010000F);
        addTeenFemaleHatDelta("child_spn_m_shaggyg1", +0.000000F, +0.015000F, -0.015000F, +1.010000F);
        addTeenFemaleHatDelta("child_spn_m_straightcutbobg1", +0.000000F, +0.000000F, -0.015000F, +1.000000F);
        addTeenFemaleHatDelta("child_spn_m_toukag1", +0.000000F, +0.005000F, -0.020000F, +1.005000F);
        addTeenFemaleHatDelta("child_spn_m_yukarialtg1", +0.000000F, +0.005000F, -0.020000F, +1.000000F);
        addTeenFemaleHatDelta("child_spn_m_yukarig1", +0.000000F, +0.000000F, -0.020000F, +1.000000F);
    }

    private static void addTeenFemaleHatDelta(String model, float x, float y, float z, float scale) {
        TEEN_FEMALE_HAT_DELTAS.put(model, new StyleAdjustment(x, y, z, scale));
    }

    private PatchChildHairTransform() {}

    private static void add(String key, float x, float y, float z, float scale) {
        STYLE_ADJUSTMENTS.put(key.toLowerCase(Locale.ENGLISH), new StyleAdjustment(x,y,z,scale));
    }
    private static void addMF(String core, float x, float y, float z, float scale) {
        add("child_spn_m_" + core, x,y,z,scale);
        add("child_spn_f_" + core, x,y,z,scale);
    }

    private static void addUser(String key, float x, float y, float z, float scale) {
        USER_ADJUSTMENTS.put(key.toLowerCase(Locale.ENGLISH), new StyleAdjustment(x,y,z,scale));
    }
    private static void addChildStyleExtra(String styleId, float x, float y, float z, float scale) {
        CHILD_STYLE_EXTRA_ADJUSTMENTS.put(styleId.toLowerCase(Locale.ENGLISH), new StyleAdjustment(x,y,z,scale));
    }
    private static void addChildHatExtra(String rawModelToken, float x, float y, float z, float scale) {
        CHILD_HAT_EXTRA_ADJUSTMENTS.put(rawModelToken.toLowerCase(Locale.ENGLISH), new StyleAdjustment(x,y,z,scale));
    }
    private static void addTeenStyleExtra(boolean female, String styleId, float x, float y, float z, float scale) {
        (female ? TEEN_FEMALE_STYLE_EXTRA_ADJUSTMENTS : TEEN_MALE_STYLE_EXTRA_ADJUSTMENTS)
            .put(styleId.toLowerCase(Locale.ENGLISH), new StyleAdjustment(x,y,z,scale));
    }
    private static void addTeenHatModelExtra(boolean female, String rawModelToken, float x, float y, float z, float scale) {
        (female ? TEEN_FEMALE_HAT_MODEL_EXTRA_ADJUSTMENTS : TEEN_MALE_HAT_MODEL_EXTRA_ADJUSTMENTS)
            .put(rawModelToken.toLowerCase(Locale.ENGLISH), new StyleAdjustment(x,y,z,scale));
    }
    private static void addTeenHatStyleExtra(boolean female, String styleId, float x, float y, float z, float scale) {
        (female ? TEEN_FEMALE_HAT_STYLE_EXTRA_ADJUSTMENTS : TEEN_MALE_HAT_STYLE_EXTRA_ADJUSTMENTS)
            .put(styleId.toLowerCase(Locale.ENGLISH), new StyleAdjustment(x,y,z,scale));
    }

    /** Add aliases for styles whose external model token is normally derived from the style ID. */
    private static void addUserStyle(String styleId, float x, float y, float z, float scale) {
        String id = styleId.toLowerCase(Locale.ENGLISH);
        addUser(id, x,y,z,scale);
        if (!id.startsWith("m_") && !id.startsWith("f_") && !id.startsWith("child_")) {
            addUser("m_" + id, x,y,z,scale);
            addUser("f_" + id, x,y,z,scale);
            addUser("m_hair_" + id, x,y,z,scale);
            addUser("f_hair_" + id, x,y,z,scale);
        } else if (id.startsWith("m_") && !id.startsWith("m_hair_")) {
            addUser("m_hair_" + id.substring(2), x,y,z,scale);
        } else if (id.startsWith("f_") && !id.startsWith("f_hair_")) {
            addUser("f_hair_" + id.substring(2), x,y,z,scale);
        }
        // Vanilla uses PartingCentre/PartingLeft/PartingRight model tokens.
        if (id.equals("centreparting")) {
            addUser("m_hair_partingcentre", x,y,z,scale);
            addUser("f_hair_partingcentre", x,y,z,scale);
        }
        if (id.equals("leftparting")) {
            addUser("m_hair_partingleft", x,y,z,scale);
            addUser("f_hair_partingleft", x,y,z,scale);
        }
        if (id.equals("rightparting")) {
            addUser("m_hair_partingright", x,y,z,scale);
            addUser("f_hair_partingright", x,y,z,scale);
        }
    }

    public static Object publicField(Object object, String name) {
        return ReflectionAccess.tryGet(object, name, null);
    }
    public static Object invokeNoArg(Object object, String name) {
        if (object == null) return null;
        try {
            Method m = ReflectionAccess.method(object.getClass(), name);
            return m == null ? null : m.invoke(object);
        }
        catch (Throwable ignored) { return null; }
    }
    public static boolean invokeChildIdentity(String methodName, Object argument) {
        if (argument == null) return false;
        if (argument instanceof IsoGameCharacter character) {
            if ("isChild".equals(methodName)) return ChildIdentity.isChild(character);
            if ("isTeen".equals(methodName)) return ChildIdentity.isTeen(character);
        }
        if (argument instanceof HumanVisual visual) {
            if ("isChildVisual".equals(methodName)) return ChildIdentity.isChildVisual(visual);
            if ("isTeenVisual".equals(methodName)) return ChildIdentity.isTeenVisual(visual);
        }
        return false;
    }
    public static boolean isChildBodyModel(Object modelInstance) {
        Object current = modelInstance;
        for (int depth = 0; current != null && depth < 12; depth++) {
            Object model = publicField(current, "model");
            Object name = publicField(model, "name");
            if (name != null) {
                String s = String.valueOf(name).replace('\\','/').toLowerCase(Locale.ENGLISH);
                if (s.contains("playablechildren/malebody") || s.contains("playablechildren/femalebody")) return true;
            }
            current = publicField(current, "parent");
        }
        return false;
    }

    public static boolean isTeenBodyModel(Object modelInstance) {
        Object current = modelInstance;
        for (int depth = 0; current != null && depth < 12; depth++) {
            Object model = publicField(current, "model");
            Object name = publicField(model, "name");
            if (name != null) {
                String s = String.valueOf(name).replace('\\','/').toLowerCase(Locale.ENGLISH);
                if (s.contains("playablechildren/teenbody") || s.contains("playablechildren/teenfemalebody")) return true;
            }
            current = publicField(current, "parent");
        }
        return false;
    }

    private static Object ownerVisual(Object modelInstance) {
        Object owner = invokeNoArg(modelInstance, "getOwner");
        if (owner == null) return null;
        Object visual = invokeNoArg(owner, "getVisual");
        if (visual == null) visual = invokeNoArg(owner, "getHumanVisual");
        return visual;
    }

    public static boolean isChildInstance(Object modelInstance) {
        if (isChildBodyModel(modelInstance)) return true;
        Object character = publicField(modelInstance, "character");
        if (character != null && invokeChildIdentity("isChild", character)) return true;
        Object visual = ownerVisual(modelInstance);
        return visual != null && invokeChildIdentity("isChildVisual", visual);
    }

    public static boolean isTeenInstance(Object modelInstance) {
        if (isTeenBodyModel(modelInstance)) return true;
        Object character = publicField(modelInstance, "character");
        if (character != null && invokeChildIdentity("isTeen", character)) return true;
        Object visual = ownerVisual(modelInstance);
        return visual != null && invokeChildIdentity("isTeenVisual", visual);
    }

    /** Resolve once from the AnimatedModel owner shared by body, hair and headwear. */
    public static LifeStage resolveRootStage(Object modelInstance) {
        Object current = modelInstance;
        for (int depth = 0; current != null && depth < 12; depth++) {
            Object owner = invokeNoArg(current, "getOwner");
            LifeStage cached = AnimatedModelStages.stageOf(owner);
            if (cached != LifeStage.ADULT) return cached;
            current = publicField(current, "parent");
        }
        if (isTeenInstance(modelInstance)) return LifeStage.TEEN;
        if (isChildInstance(modelInstance)) return LifeStage.CHILD;
        return LifeStage.ADULT;
    }

    /** 0=adult/unknown, 1=child, 2=teen. */
    private static int stageOfInstance(Object modelInstance) {
        LifeStage stage = resolveRootStage(modelInstance);
        if (stage == LifeStage.TEEN) return 2;
        if (stage == LifeStage.CHILD) return 1;
        return 0;
    }

    private static boolean isFemaleInstance(Object modelInstance) {
        if (isTeenBodyModel(modelInstance)) {
            Object current = modelInstance;
            for (int depth = 0; current != null && depth < 12; depth++) {
                Object model = publicField(current, "model");
                Object name = publicField(model, "name");
                if (name != null) {
                    String s = String.valueOf(name).replace('\\','/').toLowerCase(Locale.ENGLISH);
                    if (s.contains("playablechildren/teenfemalebody")) return true;
                    if (s.contains("playablechildren/teenbody")) return false;
                }
                current = publicField(current, "parent");
            }
        }
        Object character = publicField(modelInstance, "character");
        Object female = invokeNoArg(character, "isFemale");
        if (female instanceof Boolean) return ((Boolean)female).booleanValue();
        Object visual = ownerVisual(modelInstance);
        female = invokeNoArg(visual, "isFemale");
        return female instanceof Boolean && ((Boolean)female).booleanValue();
    }

    public static String modelName(Object modelInstance) {
        Object model=publicField(modelInstance,"model");
        if(model==null) return "";
        synchronized(MODEL_NAMES){
            if(MODEL_NAMES.containsKey(model)) return MODEL_NAMES.get(model);
        }
        Object name=publicField(model,"name");
        String normalized=name==null?"":String.valueOf(name).replace('\\','/').toLowerCase(Locale.ENGLISH);
        synchronized(MODEL_NAMES){ MODEL_NAMES.put(model,normalized); }
        return normalized;
    }
    public static boolean isHair(String modelName) {
        if(modelName==null || modelName.isEmpty()) return false;
        if(modelName.contains("child_")) return false;
        return modelName.contains("skinned/hair/") || modelName.contains("skinned/hair&") || modelName.contains("/hair/");
    }
    public static boolean isAnyHair(String modelName) {
        if(modelName==null || modelName.isEmpty()) return false;
        String token = modelToken(modelName);
        return modelName.contains("skinned/hair/") || modelName.contains("skinned/hair&") || modelName.contains("/hair/")
            || token.startsWith("child_hair_")
            || token.startsWith("child_dante")
            || token.startsWith("child_donny")
            || token.startsWith("child_spn_")
            || token.startsWith("child_harry_");
    }

    public static boolean isHatVariant(String modelName) {
        if(modelName==null) return false;
        String s=modelName.toLowerCase(Locale.ENGLISH);
        if(s.contains("hat") || s.contains("cat1")) return true;
        for(int i=1;i<=7;i++) if(s.contains("g"+i)) return true;
        return false;
    }

    private static String rawModelToken(String modelName) {
        if(modelName==null) return "";
        String s=modelName.toLowerCase(Locale.ENGLISH).replace('\\','/');
        int slash=s.lastIndexOf('/');
        if(slash>=0) s=s.substring(slash+1);
        int cut=s.length();
        for(char c : new char[]{'&','|','#','?',';'}) {
            int i=s.indexOf(c); if(i>=0 && i<cut) cut=i;
        }
        return s.substring(0,cut);
    }
    private static String modelToken(String modelName) {
        String s = rawModelToken(modelName);
        // Hat/compressed variants inherit the normal hairstyle correction first.
        // A separate FIXED31 hat delta can then be tuned against the exact raw token.
        if(s.endsWith("cat1")) s=s.substring(0,s.length()-4);
        else if(s.length()>2) {
            int n=s.length();
            char a=s.charAt(n-2), b=s.charAt(n-1);
            if(a=='g' && b>='1' && b<='7') s=s.substring(0,n-2);
        }
        return s;
    }
    private static StyleAdjustment findAdjustment(String modelName) {
        return STYLE_ADJUSTMENTS.get(modelToken(modelName));
    }

    /**
     * User-approved FIXED27 correction. Matching female models inherit the male
     * correction unless a female-specific value is added later. This is safe for
     * playable girls because both sexes use the same MaleBody head geometry.
     */
    private static StyleAdjustment findUserAdjustment(String modelName) {
        String token = modelToken(modelName);
        StyleAdjustment a = USER_ADJUSTMENTS.get(token);
        if (a != null) return a;

        if (token.startsWith("child_spn_f_")) {
            String core = token.substring("child_spn_f_".length());
            if (core.equals("goth2")) core = "goth"; // Spongie names the female selectable style F_Goth2.
            a = USER_ADJUSTMENTS.get("child_spn_m_" + core);
            if (a != null) return a;
        }

        // PCH bundles Yaki's MALE geometry under a private PCH_Yaki_* model name.
        // Reuse the approved M_Yaki_* calibration for that internal copy.
        if (token.startsWith("pch_yaki_")) {
            a = USER_ADJUSTMENTS.get("m_yaki_" + token.substring("pch_yaki_".length()));
            if (a != null) return a;
        }

        // External hair packs often use paired M_/F_ model names. Reuse the male
        // correction for the female twin unless we later add an exact F_ override.
        if (token.startsWith("f_")) {
            a = USER_ADJUSTMENTS.get("m_" + token.substring(2));
            if (a != null) return a;
        }

        // Last-resort aliases for vanilla/external model naming.
        if (token.startsWith("m_hair_") || token.startsWith("f_hair_")) {
            a = USER_ADJUSTMENTS.get(token.substring(7));
            if (a != null) return a;
        }
        return null;
    }


    public static HumanVisual humanVisualOf(IsoGameCharacter character) {
        if (character instanceof zombie.core.skinnedmodel.visual.IHumanVisual) {
            return ((zombie.core.skinnedmodel.visual.IHumanVisual) character).getHumanVisual();
        }
        return null;
    }

    private static String selectedHairStyle(Object modelInstance) {
        // IsoGameCharacter itself does NOT declare getHumanVisual() in B42.20.4;
        // IsoPlayer/IsoZombie expose it through IHumanVisual.  Going through the
        // interface keeps this fast-path allocation-free without linking against a
        // method that does not exist on the base class.
        IsoGameCharacter character = ownerCharacter(modelInstance);
        HumanVisual liveVisual = humanVisualOf(character);
        if (liveVisual != null) {
            String value = liveVisual.getHairModel();
            if (value != null && !value.isEmpty()) return value;
        }
        Object visual = ownerVisual(modelInstance);
        Object value = invokeNoArg(visual, "getHairModel");
        return value == null ? "" : String.valueOf(value);
    }

    private static String childCalibrationStyle(Object modelInstance) {
        String style = selectedHairStyle(modelInstance);
        if (style == null || style.isEmpty()) return "";
        IsoGameCharacter character = ownerCharacter(modelInstance);
        boolean female = character != null && character.isFemale();
        return PatchChildHairRegistry.childCalibrationStyle(style, female);
    }

    private static StyleAdjustment findChildStyleExtra(Object modelInstance, String modelName) {
        if (isHatVariant(modelName)) return null; // SIN GORRA namespace only.
        String style = childCalibrationStyle(modelInstance);
        if (style == null || style.isEmpty()) return null;
        return CHILD_STYLE_EXTRA_ADJUSTMENTS.get(style.toLowerCase(Locale.ENGLISH));
    }

    private static StyleAdjustment findChildHatExtra(String modelName) {
        if (!isHatVariant(modelName)) return null;
        String raw = rawModelToken(modelName);
        StyleAdjustment a = CHILD_HAT_EXTRA_ADJUSTMENTS.get(raw);
        if (a != null) return a;

        // CHILD girls still use the shared child head.  When a female-only
        // registry entry renders the corresponding F_Hair_* compressed mesh,
        // inherit the approved male hat correction only for the exact twin name.
        if (raw.startsWith("f_hair_")) {
            a = CHILD_HAT_EXTRA_ADJUSTMENTS.get("m_hair_" + raw.substring("f_hair_".length()));
            if (a != null) return a;
        }
        if (raw.startsWith("child_spn_f_")) {
            String core = raw.substring("child_spn_f_".length());
            if (core.startsWith("goth2")) core = "goth" + core.substring("goth2".length());
            a = CHILD_HAT_EXTRA_ADJUSTMENTS.get("child_spn_m_" + core);
            if (a != null) return a;
        }
        return null;
    }

    private static boolean resetLegacyLocalSpaceFit(Object modelInstance) {
        String style = selectedHairStyle(modelInstance);
        return style != null && LOCALSPACE_LEGACY_RESET_STYLES.contains(style.toLowerCase(Locale.ENGLISH));
    }

    /**
     * TEEN calibration is stage+sex specific. Normal and hat values are exclusive
     * namespaces. Female HAT model values start from exact matching male mesh tokens
     * only where available, then female-specific exports override that approximation.
     */
    private static Map<String, StyleAdjustment> teenStyleMap(boolean female) {
        return female ? TEEN_FEMALE_STYLE_EXTRA_ADJUSTMENTS : TEEN_MALE_STYLE_EXTRA_ADJUSTMENTS;
    }

    private static Map<String, StyleAdjustment> teenHatModelMap(boolean female) {
        return female ? TEEN_FEMALE_HAT_MODEL_EXTRA_ADJUSTMENTS : TEEN_MALE_HAT_MODEL_EXTRA_ADJUSTMENTS;
    }

    private static Map<String, StyleAdjustment> teenHatStyleMap(boolean female) {
        return female ? TEEN_FEMALE_HAT_STYLE_EXTRA_ADJUSTMENTS : TEEN_MALE_HAT_STYLE_EXTRA_ADJUSTMENTS;
    }

    /**
     * FIXED79: TEEN uses the same canonical live-style resolution that fixed active
     * CHILD zombies in FIXED55/56. Natural/reanimated teen zombies may expose a
     * vanilla/source style (for example Short) while the approved teen calibration
     * table is keyed by the integrated PCH alias (PCH_Vanilla_Short). The registry
     * also reverses a uniquely persisted hat alternate back to its canonical parent.
     * This is lookup-only and does not make CHILD/TEEN calibration values inherit.
     */
    private static String teenCalibrationStyle(Object modelInstance, boolean female) {
        String style = selectedHairStyle(modelInstance);
        if (style == null || style.isEmpty()) return "";
        return PatchChildHairRegistry.childCalibrationStyle(style, female);
    }

    /**
     * Return the actual headwear category owned by the character-creator AnimatedModel.
     * UI3DModel previews are backed by AnimatedModel.setSurvivorDesc(), so there is no
     * IsoGameCharacter to inspect, but AnimatedModel still keeps the copied ItemVisuals.
     * Returning null means this is not a standard AnimatedModel preview and callers may
     * use a conservative legacy fallback.
     */
    private static String previewHatCategory(Object modelInstance, boolean female) {
        Object current = modelInstance;
        for (int depth = 0; current != null && depth < 12; depth++) {
            Object owner = invokeNoArg(current, "getOwner");
            if (owner != null && "zombie.core.skinnedmodel.advancedanimation.AnimatedModel".equals(owner.getClass().getName())) {
                Object visuals = publicField(owner, "itemVisuals");
                if (visuals == null) return "none";
                Object hatVisual = invokeNoArg(visuals, "findHat");
                if (hatVisual == null) return "none";
                if (hatVisual instanceof zombie.core.skinnedmodel.visual.ItemVisual visual
                        && PatchChildFemaleHeadwear.hairHat(visual, resolveRootStage(modelInstance), female) == null) return "none";
                Object clothing = invokeNoArg(hatVisual, "getClothingItem");
                Object category = publicField(clothing, "hatCategory");
                return category == null ? "hat" : safeText(category);
            }
            current = publicField(current, "parent");
        }
        return null;
    }

    private static boolean teenHatContext(Object modelInstance, String modelName, boolean female) {
        // Live gameplay: use the actual worn item, never infer from G1/Hat model names.
        IsoGameCharacter character = ownerCharacter(modelInstance);
        if (character != null) {
            if (PatchChildFemaleHeadwear.usesNormalHair(character.getItemVisuals().findHat(), female)) return false;
            return !"none".equals(wornHatCategory(character));
        }

        // FIXED96: character-creator preview. AnimatedModel owns the preview ItemVisuals,
        // so findHat() gives the same truth PZ used to choose the alternate hairstyle.
        // This fixes normal TEEN styles whose no-hat render mesh is named G1/ShortHat.
        String previewHat = previewHatCategory(modelInstance, female);
        if (previewHat != null) return !"none".equals(previewHat);

        // Defensive fallback for non-standard wrappers supplied by other mods/previews.
        if (isHatVariant(modelName)) return true;
        String style = teenCalibrationStyle(modelInstance, female);
        if (style == null || style.isEmpty()) return false;
        return teenHatStyleMap(female).containsKey(style.toLowerCase(Locale.ENGLISH));
    }

    private static StyleAdjustment findTeenStyleExtra(Object modelInstance, String modelName, boolean female) {
        if (teenHatContext(modelInstance, modelName, female)) return null;
        String style = teenCalibrationStyle(modelInstance, female);
        if (style == null || style.isEmpty()) return null;
        return teenStyleMap(female).get(style.toLowerCase(Locale.ENGLISH));
    }

    private static StyleAdjustment findTeenHatExtra(Object modelInstance, String modelName, boolean female) {
        if (!teenHatContext(modelInstance, modelName, female)) return null;

        StyleAdjustment a = teenHatModelMap(female).get(rawModelToken(modelName));
        if (a != null) return a;

        String style = teenCalibrationStyle(modelInstance, female);
        if (style == null || style.isEmpty()) return null;
        return teenHatStyleMap(female).get(style.toLowerCase(Locale.ENGLISH));
    }

    private static StyleAdjustment findTeenHatDelta(Object modelInstance, String modelName, boolean female) {
        if (!teenHatContext(modelInstance, modelName, female)) return null;
        if (!female) return rawModelToken(modelName).equals("child_harry_m_obiwancannolicat1")
            ? TEEN_MALE_OBI_WAN_HAT_DELTA : null;
        return TEEN_FEMALE_HAT_DELTAS.get(rawModelToken(modelName));
    }

    private static final ThreadLocal<zombie.core.skinnedmodel.visual.ItemVisuals> HAT_VISUALS_117V =
        ThreadLocal.withInitial(zombie.core.skinnedmodel.visual.ItemVisuals::new);

    private static boolean childWearsFullHat117V(IsoGameCharacter character) {
        zombie.core.skinnedmodel.visual.ItemVisuals visuals = HAT_VISUALS_117V.get();
        visuals.clear();
        try {
            // Local players collect wornItems here; the no-argument getter is the remote cache.
            character.getItemVisuals(visuals);
            zombie.core.skinnedmodel.visual.ItemVisual visual = visuals.findHat();
            return visual != null && PatchChildFemaleHeadwear.hairHat(visual, LifeStage.CHILD, character.isFemale()) != null;
        } catch (Throwable ignored) {
            return false;
        } finally {
            visuals.clear();
        }
    }

    private static StyleAdjustment childFemaleHatExport117X(Object modelInstance, String modelName, int stage) {
        if (stage != 1) return null;
        StyleAdjustment adjustment = CHILD_FEMALE_HAT_117X.get(rawModelToken(modelName));
        if (adjustment == null) return null;
        return childHatExportContext117Y(modelInstance, modelName) ? adjustment : null;
    }

    private static StyleAdjustment childFemaleHatExport117W(Object modelInstance, String modelName, int stage) {
        if (stage != 1) return null;
        StyleAdjustment adjustment = CHILD_FEMALE_HAT_117W.get(rawModelToken(modelName));
        if (adjustment == null) return null;
        return childHatExportContext117Y(modelInstance, modelName) ? adjustment : null;
    }

    private static boolean childHatExportContext117Y(Object modelInstance, String modelName) {
        IsoGameCharacter character = ownerCharacter(modelInstance);
        boolean female = character != null ? character.isFemale() : isFemaleInstance(modelInstance);
        if (!female && !CHILD_SHARED_HAT_EXPORT_MODELS_117Y.contains(rawModelToken(modelName))) return false;
        if (character != null) return childWearsFullHat117V(character);
        String category = previewHatCategory(modelInstance, female);
        // Preserve the existing female wrapper fallback. Male transfer requires
        // real worn-item context, so a bare G1/Hat name never enables new values.
        return category == null ? female && isHatVariant(modelName) : !"none".equals(category);
    }

    private static boolean childMaleHarryNative117Z(Object instance, String modelName, int stage) {
        if (stage != 1 || !CHILD_MALE_HARRY_NATIVE_117Z.contains(rawModelToken(modelName))) return false;
        IsoGameCharacter character = ownerCharacter(instance);
        return character != null ? !character.isFemale() : !isFemaleInstance(instance);
    }

    private static StyleAdjustment childHatExport117Z(Object instance, String modelName, int stage) {
        if (stage != 1) return null;
        String raw = rawModelToken(modelName);
        StyleAdjustment value = CHILD_HAT_117Z.get(raw);
        if (value == null) return null;
        IsoGameCharacter character = ownerCharacter(instance);
        boolean female = character != null ? character.isFemale() : isFemaleInstance(instance);
        if (female && !CHILD_FEMALE_SHARED_117Z.contains(raw)) return null;
        if (character != null) return childWearsFullHat117V(character) ? value : null;
        String category = previewHatCategory(instance, female);
        return category != null && !"none".equals(category) ? value : null;
    }

    private static StyleAdjustment hatExport117V(Object modelInstance, String modelName, int stage, boolean female) {
        if (stage != 1 && stage != 2) return null;
        boolean hat;
        if (stage == 2) hat = teenHatContext(modelInstance, modelName, female);
        else {
            IsoGameCharacter character = ownerCharacter(modelInstance);
            if (character != null) {
                hat = childWearsFullHat117V(character);
            } else {
                String category = previewHatCategory(modelInstance, false);
                hat = category == null ? isHatVariant(modelName) : !"none".equals(category);
            }
        }
        return hat ? HAT_EXPORT_117V.get((stage == 1 ? "CHILD" : female ? "TEEN_FEMALE" : "TEEN_MALE") + "|" + rawModelToken(modelName)) : null;
    }

    // DEV118B: incremental live-tuner exports over the delivered 118A fits.
    // SIN uses the selected canonical style; HAT uses the exact rendered mesh.
    // Neither the existing layers nor base/Cat1 routing are replaced.
    private static final Map<String, StyleAdjustment> HARRY_EXPORT_118B = Map.ofEntries(
        Map.entry("CHILD|SIN|pch_harry_f_brita", new StyleAdjustment(0,.096F,.002F,1.088F)),
        Map.entry("CHILD|SIN|pch_harry_f_grunge01", new StyleAdjustment(0,-.005F,.081F,1.120F)),
        Map.entry("CHILD|SIN|pch_harry_f_merigold", new StyleAdjustment(0,0,.005F,1.071F)),
        Map.entry("CHILD|SIN|pch_harry_f_ponytaillong", new StyleAdjustment(0,.003F,.041F,1.100F)),
        Map.entry("CHILD|SIN|pch_harry_f_ponytailmedium", new StyleAdjustment(0,-.003F,.008F,1.067F)),
        Map.entry("CHILD|SIN|pch_harry_f_throttlekitty", new StyleAdjustment(0,-.005F,.032F,1.098F)),
        Map.entry("CHILD|SIN|pch_harry_m_aiteron", new StyleAdjustment(0,-.005F,.044F,1.096F)),
        Map.entry("CHILD|SIN|pch_harry_m_obiwancannoli", new StyleAdjustment(0,0,.184F,1.241F)),
        Map.entry("CHILD|SIN|pch_harry_m_soulfilcher", new StyleAdjustment(0,-.006F,.203F,1.257F)),
        Map.entry("CHILD|HAT|child_harry_f_brita", new StyleAdjustment(0,.078F,.005F,1.081F)),
        Map.entry("CHILD|HAT|child_harry_f_britacat1", new StyleAdjustment(0,.013F,0,1.165F)),
        Map.entry("CHILD|HAT|child_harry_f_grunge01", new StyleAdjustment(0,.002F,-.064F,.983F)),
        Map.entry("CHILD|HAT|child_harry_f_grunge01cat1", new StyleAdjustment(0,-.003F,.064F,1.101F)),
        Map.entry("CHILD|HAT|child_harry_f_merigold", new StyleAdjustment(0,0,.012F,1.070F)),
        Map.entry("CHILD|HAT|child_harry_f_merigoldcat1", new StyleAdjustment(0,0,.026F,1.087F)),
        Map.entry("CHILD|HAT|child_harry_f_ponytaillong", new StyleAdjustment(0,0,.044F,1.072F)),
        Map.entry("CHILD|HAT|child_harry_f_ponytaillongcat1", new StyleAdjustment(0,0,.060F,1.118F)),
        Map.entry("CHILD|HAT|child_harry_f_ponytailmedium", new StyleAdjustment(0,-.013F,.091F,1.104F)),
        Map.entry("CHILD|HAT|child_harry_f_ponytailmediumcat1", new StyleAdjustment(0,.011F,.139F,1.168F)),
        Map.entry("CHILD|HAT|child_harry_f_throttlekitty", new StyleAdjustment(0,-.007F,.093F,1.129F)),
        Map.entry("CHILD|HAT|child_harry_f_throttlekittycat1", new StyleAdjustment(0,-.001F,.141F,1.193F)),
        Map.entry("CHILD|HAT|child_harry_m_aiteron", new StyleAdjustment(0,-.010F,.160F,1.210F)),
        Map.entry("CHILD|HAT|child_harry_m_obiwancannoli", new StyleAdjustment(0,-.010F,.240F,1.280F)),
        Map.entry("CHILD|HAT|child_harry_m_soulfilcher", new StyleAdjustment(0,-.015F,.300F,1.350F)),
        Map.entry("TEEN_MALE|SIN|pch_harry_m_aiteron", new StyleAdjustment(0,0,0,1.066F)),
        Map.entry("TEEN_MALE|SIN|pch_harry_m_obiwancannoli", new StyleAdjustment(0,0,.013F,1.074F)),
        Map.entry("TEEN_MALE|SIN|pch_harry_m_soulfilcher", new StyleAdjustment(0,-.007F,.050F,1.109F)),
        Map.entry("TEEN_MALE|HAT|child_harry_m_aiteroncat1", new StyleAdjustment(0,-.002F,.007F,1.085F)),
        Map.entry("TEEN_MALE|HAT|child_harry_m_aiteron", new StyleAdjustment(0,0,0,1.050F)),
        Map.entry("TEEN_MALE|HAT|child_harry_m_obiwancannoli", new StyleAdjustment(0,0,.069F,1.145F)),
        Map.entry("TEEN_MALE|HAT|child_harry_m_obiwancannolicat1", new StyleAdjustment(0,0,.142F,1.190F)),
        Map.entry("TEEN_MALE|HAT|child_harry_m_soulfilchercat1", new StyleAdjustment(0,0,.158F,1.225F)),
        Map.entry("TEEN_FEMALE|SIN|pch_harry_f_brita", new StyleAdjustment(0,.055F,-.002F,1.121F)),
        Map.entry("TEEN_FEMALE|SIN|pch_harry_f_grunge01", new StyleAdjustment(0,-.010F,.006F,1.055F)),
        Map.entry("TEEN_FEMALE|SIN|pch_harry_f_merigold", new StyleAdjustment(0,-.005F,.101F,1.170F)),
        Map.entry("TEEN_FEMALE|SIN|pch_harry_f_ponytaillong", new StyleAdjustment(0,-.010F,.009F,1.070F)),
        Map.entry("TEEN_FEMALE|SIN|pch_harry_f_ponytailmedium", new StyleAdjustment(0,0,.026F,1.094F)),
        Map.entry("TEEN_FEMALE|SIN|pch_harry_f_throttlekitty", new StyleAdjustment(0,-.015F,-.004F,1.065F)),
        Map.entry("TEEN_FEMALE|HAT|child_harry_f_brita", new StyleAdjustment(0,.080F,-.004F,1.093F)),
        Map.entry("TEEN_FEMALE|HAT|child_harry_f_britacat1", new StyleAdjustment(0,.040F,0,1.139F)),
        Map.entry("TEEN_FEMALE|HAT|child_harry_f_grunge01", new StyleAdjustment(0,-.010F,.010F,1.062F)),
        Map.entry("TEEN_FEMALE|HAT|child_harry_f_grunge01cat1", new StyleAdjustment(0,.010F,.077F,1.126F)),
        Map.entry("TEEN_FEMALE|HAT|child_harry_f_merigold", new StyleAdjustment(0,-.005F,.089F,1.157F)),
        Map.entry("TEEN_FEMALE|HAT|child_harry_f_merigoldcat1", new StyleAdjustment(0,.004F,.027F,1.092F)),
        Map.entry("TEEN_FEMALE|HAT|child_harry_f_ponytaillong", new StyleAdjustment(0,-.010F,.011F,1.071F)),
        Map.entry("TEEN_FEMALE|HAT|child_harry_f_ponytaillongcat1", new StyleAdjustment(0,.010F,.022F,1.071F)),
        Map.entry("TEEN_FEMALE|HAT|child_harry_f_ponytailmedium", new StyleAdjustment(0,-.010F,.037F,1.096F)),
        Map.entry("TEEN_FEMALE|HAT|child_harry_f_ponytailmediumcat1", new StyleAdjustment(0,.001F,.109F,1.173F)),
        Map.entry("TEEN_FEMALE|HAT|child_harry_f_throttlekitty", new StyleAdjustment(0,-.010F,.007F,1.070F)),
        Map.entry("TEEN_FEMALE|HAT|child_harry_f_throttlekittycat1", new StyleAdjustment(0,.004F,.028F,1.088F)));

    // Incremental live exports over DEV118B; exact base/Cat1 rows remain independent.
    private static final Map<String, StyleAdjustment> HARRY_EXPORT_118C = Map.ofEntries(
        Map.entry("CHILD|HAT|child_harry_m_aiteron", new StyleAdjustment(0,0,0,1.010F)),
        Map.entry("CHILD|HAT|child_harry_m_aiteroncat1", new StyleAdjustment(0,-.005F,.056F,1.114F)),
        Map.entry("CHILD|HAT|child_harry_m_obiwancannoli", new StyleAdjustment(0,-.001F,.135F,1.175F)),
        Map.entry("CHILD|HAT|child_harry_m_obiwancannolicat1", new StyleAdjustment(0,-.005F,.201F,1.250F)),
        Map.entry("CHILD|HAT|child_harry_m_soulfilcher", new StyleAdjustment(0,.025F,-.077F,.870F)),
        Map.entry("CHILD|HAT|child_harry_m_soulfilchercat1", new StyleAdjustment(0,-.005F,.184F,1.230F)));

    private static boolean uncalibratedChildMaleCarl118B(Object instance) {
        IsoGameCharacter character = ownerCharacter(instance);
        zombie.core.skinnedmodel.visual.ItemVisual hat = null;
        if (character != null) {
            zombie.core.skinnedmodel.visual.ItemVisuals visuals = HAT_VISUALS_117V.get();
            visuals.clear();
            try { character.getItemVisuals(visuals); hat = visuals.findHat(); }
            finally { visuals.clear(); }
        } else {
            Object current = instance;
            for (int depth = 0; current != null && depth < 12; depth++) {
                Object owner = invokeNoArg(current, "getOwner");
                if (owner instanceof zombie.core.skinnedmodel.advancedanimation.AnimatedModel) {
                    Object visuals = publicField(owner, "itemVisuals");
                    Object found = invokeNoArg(visuals, "findHat");
                    if (found instanceof zombie.core.skinnedmodel.visual.ItemVisual visual) hat = visual;
                    break;
                }
                current = publicField(current, "parent");
            }
        }
        zombie.core.skinnedmodel.population.ClothingItem clothing = hat == null ? null : hat.getClothingItem();
        if (clothing == null) return false;
        String model = rawModelToken(clothing.getModel(false));
        return model.equals("carlhat_child") || model.equals("carlhat_adult")
            || model.equals("carlhat_child.x") || model.equals("carlhat_adult.x");
    }

    private static StyleAdjustment harryExport118B(Object instance, String name, int stage,
            Map<String, StyleAdjustment> exports, boolean preserve118BCarl) {
        if (stage != 1 && stage != 2) return null;
        String raw = rawModelToken(name);
        if (!raw.startsWith("child_harry_")) return null;
        IsoGameCharacter character = ownerCharacter(instance);
        boolean female = character == null ? isFemaleInstance(instance) : character.isFemale();
        boolean hat;
        if (character != null) {
            // Local players' no-argument item visuals can be a stale remote cache.
            // Query real WornItems, as the CHILD rendering path already does.
            zombie.core.skinnedmodel.visual.ItemVisuals visuals = HAT_VISUALS_117V.get();
            visuals.clear();
            try {
                character.getItemVisuals(visuals);
                zombie.core.skinnedmodel.visual.ItemVisual visual = visuals.findHat();
                hat = visual != null && PatchChildFemaleHeadwear.hairHat(visual,
                    stage == 1 ? LifeStage.CHILD : LifeStage.TEEN, female) != null;
            } finally { visuals.clear(); }
        } else if (stage == 2) hat = teenHatContext(instance, name, female);
        else {
            String category = previewHatCategory(instance, female);
            hat = category == null ? isHatVariant(name) : !"none".equals(category);
        }
        String ns = stage == 1 ? "CHILD" : female ? "TEEN_FEMALE" : "TEEN_MALE";
        if (hat) {
            // The user has not calibrated these three CHILD male styles with Carl.
            // Retain their released fit even when Carl shares a base mesh with other hats.
            if (preserve118BCarl && stage == 1 && !female && uncalibratedChildMaleCarl118B(instance)) return null;
            return exports.get(ns + "|HAT|" + raw);
        }
        if (raw.endsWith("cat1")) return null;
        String selected = selectedHairStyle(instance);
        String canonical = PatchChildHairRegistry.childCalibrationStyle(selected, female).toLowerCase(Locale.ENGLISH);
        if (!canonical.equals("pch_harry_" + raw.substring("child_harry_".length()))) return null;
        return exports.get(ns + "|SIN|" + canonical);
    }

    private static float calibrationUnitScale(String modelName) {
        // These FBX meshes bake a 100x unit conversion into their skinning offsets.
        return switch (rawModelToken(modelName)) {
            case "child_harry_f_grunge01", "child_harry_f_grunge01cat1",
                 "child_harry_f_merigold", "child_harry_f_merigoldcat1",
                 "child_harry_f_ponytaillong", "child_harry_f_ponytaillongcat1",
                 "child_harry_f_ponytailmedium", "child_harry_f_ponytailmediumcat1",
                 "child_harry_f_throttlekitty", "child_harry_f_throttlekittycat1",
                 "child_harry_m_aiteron", "child_harry_m_aiteroncat1",
                 "child_harry_m_obiwancannoli", "child_harry_m_obiwancannolicat1",
                 "child_harry_m_soulfilcher", "child_harry_m_soulfilchercat1" -> .01F;
            case "child_harry_m_ponytailmedium" -> 1.0F / 101.92864F;
            default -> 1.0F;
        };
    }

    public static void invokeMatrix(Object matrix,String name)throws Exception { matrix.getClass().getMethod(name).invoke(matrix); }
    public static void invokeTranslate(Object matrix,float x,float y,float z)throws Exception { matrix.getClass().getMethod("translate",float.class,float.class,float.class).invoke(matrix,x,y,z); }
    public static void invokeScale(Object matrix,float scale)throws Exception { matrix.getClass().getMethod("scale",float.class).invoke(matrix,scale); }

    public static boolean isStaticModel(Object modelInstance) {
        Object model = publicField(modelInstance, "model");
        Object value = publicField(model, "isStatic");
        return value instanceof Boolean && ((Boolean)value).booleanValue();
    }

    private static IsoGameCharacter ownerCharacter(Object modelInstance) {
        // Render hot path: real PZ hair instances are ModelInstance objects. Use their
        // public fields and AnimationPlayer API directly instead of reflective field/method
        // lookup on every frame. Child hair sub-models share the root character's player.
        if (modelInstance instanceof zombie.core.skinnedmodel.model.ModelInstance) {
            zombie.core.skinnedmodel.model.ModelInstance current = (zombie.core.skinnedmodel.model.ModelInstance)modelInstance;
            for (int depth = 0; current != null && depth < 12; depth++, current = current.parent) {
                if (current.character != null) return current.character;
                zombie.core.skinnedmodel.animation.AnimationPlayer player = current.animPlayer;
                if (player != null) {
                    IsoGameCharacter character = player.getIsoGameCharacter();
                    if (character != null) return character;
                }
            }
            return null;
        }

        // Defensive fallback for non-standard wrappers supplied by previews/mods. This path
        // is not used by normal live IsoPlayer/IsoZombie hair rendering.
        Object current = modelInstance;
        for (int depth = 0; current != null && depth < 12; depth++) {
            Object character = publicField(current, "character");
            if (character instanceof IsoGameCharacter) return (IsoGameCharacter)character;
            Object animPlayer = publicField(current, "animPlayer");
            Object animCharacter = invokeNoArg(animPlayer, "getIsoGameCharacter");
            if (animCharacter instanceof IsoGameCharacter) return (IsoGameCharacter)animCharacter;
            Object owner = invokeNoArg(current, "getOwner");
            if (owner instanceof IsoGameCharacter) return (IsoGameCharacter)owner;
            Object ownerPlayer = invokeNoArg(owner, "getAnimationPlayer");
            Object ownerCharacter = invokeNoArg(ownerPlayer, "getIsoGameCharacter");
            if (ownerCharacter instanceof IsoGameCharacter) return (IsoGameCharacter)ownerCharacter;
            current = publicField(current, "parent");
        }
        return null;
    }


    private static String safeText(Object value) {
        return value == null ? "-" : String.valueOf(value).replace('\n', ' ').replace('\r', ' ');
    }



    private static String wornHatCategory(IsoGameCharacter character) {
        Object visuals = invokeNoArg(character, "getItemVisuals");
        Object hatVisual = invokeNoArg(visuals, "findHat");
        if (hatVisual == null) return "none";
        Object clothing = invokeNoArg(hatVisual, "getClothingItem");
        Object category = publicField(clothing, "hatCategory");
        return category == null ? "hat" : safeText(category);
    }

    /**
     * Applies the complete fitted-hair transform in MESH-LOCAL space.
     *
     * B42.20.4 skinning evaluates each palette matrix as:
     *   p' = R * p + t
     * with t stored at FloatBuffer indices 3/7/11.
     *
     * FIXED26-FIXED37 added the requested X/Y/Z directly to t.  That looked
     * correct in the bind/idle pose, but the delta stayed in model axes when
     * Head rotated.  During push, bend and creator idle animations the hair
     * therefore appeared to slide forward/backward away from the skull.
     *
     * FIXED38 treats the calibration as a local mesh transform (B * T * S):
     *   t' = t + R * delta
     *   R' = R * uniformScale
     * This makes the fitted offset rotate with the animated bones.  In bind
     * pose skin matrices are effectively identity, so existing tuning numbers
     * retain their established neutral-pose meaning.
     */
    public static boolean paletteAffine(Object renderData, float x, float y, float z, float scale) {
        Object raw = publicField(renderData, "matrixPalette");
        if (!(raw instanceof FloatBuffer)) return false;
        FloatBuffer palette = (FloatBuffer)raw;
        int limit = palette.limit();
        if (limit < 16) return false;

        int oldPos = palette.position();
        try {
            for (int base = 0; base + 15 < limit; base += 16) {
                // Capture the current 3x3 before scaling.  A local translation
                // must be rotated by the bone's current linear transform.
                float m00 = palette.get(base + 0);
                float m01 = palette.get(base + 1);
                float m02 = palette.get(base + 2);
                float m10 = palette.get(base + 4);
                float m11 = palette.get(base + 5);
                float m12 = palette.get(base + 6);
                float m20 = palette.get(base + 8);
                float m21 = palette.get(base + 9);
                float m22 = palette.get(base + 10);

                float dx = m00 * x + m01 * y + m02 * z;
                float dy = m10 * x + m11 * y + m12 * z;
                float dz = m20 * x + m21 * y + m22 * z;

                // B * T * S: uniform scale changes only the linear 3x3, never
                // the skeleton translation/attachment position.
                if (scale != 1.0F) {
                    palette.put(base + 0,  m00 * scale);
                    palette.put(base + 1,  m01 * scale);
                    palette.put(base + 2,  m02 * scale);
                    palette.put(base + 4,  m10 * scale);
                    palette.put(base + 5,  m11 * scale);
                    palette.put(base + 6,  m12 * scale);
                    palette.put(base + 8,  m20 * scale);
                    palette.put(base + 9,  m21 * scale);
                    palette.put(base + 10, m22 * scale);
                }

                if (dx != 0.0F) palette.put(base + 3,  palette.get(base + 3)  + dx);
                if (dy != 0.0F) palette.put(base + 7,  palette.get(base + 7)  + dy);
                if (dz != 0.0F) palette.put(base + 11, palette.get(base + 11) + dz);
            }
            return true;
        } finally {
            palette.position(Math.min(oldPos, palette.limit()));
        }
    }

    /** Applies the complete hair transform to the real skinned render data. */
    public static void applyPalette(Object renderData) {
        if (renderData == null) return;
        Object modelInstance = publicField(renderData, "modelInstance");
        if (modelInstance == null || isStaticModel(modelInstance)) return;

        String name = modelName(modelInstance);
        int stage = stageOfInstance(modelInstance);
        boolean female = stage == 2 && isFemaleInstance(modelInstance);

        if (stage != 1 && stage != 2) return;

        // CHILD keeps the established fitted base + approved CHILD tuning.
        // TEEN starts from the native hair placement and only receives its own
        // future teen calibration plus the live tuner.  This is the key barrier
        // that prevents a child correction from moving the same hairstyle on a teen.
        StyleAdjustment fixed = stage == 1 ? findAdjustment(name) : null;
        StyleAdjustment user = stage == 1 ? findUserAdjustment(name) : null;
        StyleAdjustment childExtra = stage == 1 && isAnyHair(name) ? findChildStyleExtra(modelInstance, name) : null;
        StyleAdjustment childHatExtra = stage == 1 && isAnyHair(name) ? findChildHatExtra(name) : null;
        StyleAdjustment teenExtra = stage == 2 && isAnyHair(name) ? findTeenStyleExtra(modelInstance, name, female) : null;
        StyleAdjustment teenHatExtra = stage == 2 && isAnyHair(name) ? findTeenHatExtra(modelInstance, name, female) : null;
        StyleAdjustment teenHatDelta = stage == 2 && isAnyHair(name) ? findTeenHatDelta(modelInstance, name, female) : null;
        StyleAdjustment exported = isAnyHair(name) ? hatExport117V(modelInstance, name, stage, female) : null;
        StyleAdjustment exportedW = isAnyHair(name) ? childFemaleHatExport117W(modelInstance, name, stage) : null;
        StyleAdjustment exportedX = isAnyHair(name) ? childFemaleHatExport117X(modelInstance, name, stage) : null;
        StyleAdjustment exportedZ = isAnyHair(name) ? childHatExport117Z(modelInstance, name, stage) : null;
        StyleAdjustment exportedB = isAnyHair(name) ? harryExport118B(modelInstance, name, stage, HARRY_EXPORT_118B, true) : null;
        StyleAdjustment exportedC = stage == 1 && isAnyHair(name) ? harryExport118B(modelInstance, name, stage, HARRY_EXPORT_118C, false) : null;
        // These six 100x-native Harry FBX meshes already overlap the CHILD head.
        // Their obsolete pre-local-space fixed/user rows moved them 9-12 units away.
        if (childMaleHarryNative117Z(modelInstance, name, stage)) { fixed = null; user = null; }
        if (stage == 1 && isAnyHair(name) && resetLegacyLocalSpaceFit(modelInstance)) {
            fixed = null;
            user = null;
        }
        boolean generic = stage == 1 && isHair(name);
        boolean teenGeneric = stage == 2 && isAnyHair(name);

        if (!generic && !teenGeneric && fixed == null && user == null && childExtra == null && childHatExtra == null
                && teenExtra == null && teenHatExtra == null && teenHatDelta == null && exported == null && exportedW == null && exportedX == null && exportedZ == null && exportedB == null && exportedC == null) return;

        boolean hat = isHatVariant(name);
        // Aggregate all calibration layers first.  Calling paletteAffine once is
        // important now that translation is truly mesh-local: otherwise a scale
        // from an earlier layer would also scale the numeric offset of a later
        // layer and change the user's established tuning values.
        float tx = 0.0F, ty = 0.0F, tz = 0.0F, totalScale = 1.0F;

        if (generic) {
            tx += hat ? HAT_X : NORMAL_X;
            ty += (hat ? HAT_Y : NORMAL_Y) + RUNTIME_LIFT_Y;
            tz += hat ? HAT_Z : NORMAL_Z;
            totalScale *= SCALE;
        }
        if (teenGeneric) totalScale *= TEEN_HAIR_SCALE;

        if (fixed != null) {
            tx += fixed.x; ty += fixed.y; tz += fixed.z;
            totalScale *= fixed.scale;
        }

        if (user != null) {
            tx += user.x; ty += user.y; tz += user.z;
            totalScale *= user.scale;
        }

        if (childExtra != null) {
            tx += childExtra.x; ty += childExtra.y; tz += childExtra.z;
            totalScale *= childExtra.scale;
        }

        if (childHatExtra != null) {
            tx += childHatExtra.x; ty += childHatExtra.y; tz += childHatExtra.z;
            totalScale *= childHatExtra.scale;
        }

        if (teenExtra != null) {
            tx += teenExtra.x; ty += teenExtra.y; tz += teenExtra.z;
            totalScale *= teenExtra.scale;
        }

        if (teenHatExtra != null) {
            tx += teenHatExtra.x; ty += teenHatExtra.y; tz += teenHatExtra.z;
            totalScale *= teenHatExtra.scale;
        }

        float calibrationUnit = calibrationUnitScale(name);
        if (teenHatDelta != null) {
            tx += teenHatDelta.x * calibrationUnit; ty += teenHatDelta.y * calibrationUnit; tz += teenHatDelta.z * calibrationUnit;
            totalScale *= teenHatDelta.scale;
        }
        if (exported != null) {
            tx += exported.x * calibrationUnit; ty += exported.y * calibrationUnit; tz += exported.z * calibrationUnit;
            totalScale *= exported.scale;
        }
        if (exportedW != null) {
            tx += exportedW.x * calibrationUnit; ty += exportedW.y * calibrationUnit; tz += exportedW.z * calibrationUnit;
            totalScale *= exportedW.scale;
        }
        if (exportedX != null) {
            tx += exportedX.x * calibrationUnit; ty += exportedX.y * calibrationUnit; tz += exportedX.z * calibrationUnit;
            totalScale *= exportedX.scale;
        }
        if (exportedZ != null) {
            tx += exportedZ.x * calibrationUnit; ty += exportedZ.y * calibrationUnit; tz += exportedZ.z * calibrationUnit;
            totalScale *= exportedZ.scale;
        }
        if (exportedB != null) {
            tx += exportedB.x * calibrationUnit; ty += exportedB.y * calibrationUnit; tz += exportedB.z * calibrationUnit;
            totalScale *= exportedB.scale;
        }
        if (exportedC != null) {
            tx += exportedC.x * calibrationUnit; ty += exportedC.y * calibrationUnit; tz += exportedC.z * calibrationUnit;
            totalScale *= exportedC.scale;
        }

        boolean changed = false;
        if (tx != 0.0F || ty != 0.0F || tz != 0.0F || totalScale != 1.0F) {
            changed = paletteAffine(renderData, tx, ty, tz, totalScale);
        }
    }

    /**
     * Static hair still renders from xfrm, so keep the old transform route only
     * for genuinely static models.  Non-static/skinned hair is handled above by
     * the matrix palette patch and must NOT be transformed here a second time.
     */
    public static void applyStatic(Object renderData) {
        if (renderData == null) return;
        Object modelInstance = publicField(renderData, "modelInstance");
        if (modelInstance == null || !isStaticModel(modelInstance)) return;

        String name = modelName(modelInstance);
        int stage = stageOfInstance(modelInstance);
        boolean female = stage == 2 && isFemaleInstance(modelInstance);

        if (stage != 1 && stage != 2) return;
        StyleAdjustment fixed = stage == 1 ? findAdjustment(name) : null;
        StyleAdjustment user = stage == 1 ? findUserAdjustment(name) : null;
        StyleAdjustment childExtra = stage == 1 && isAnyHair(name) ? findChildStyleExtra(modelInstance, name) : null;
        StyleAdjustment childHatExtra = stage == 1 && isAnyHair(name) ? findChildHatExtra(name) : null;
        StyleAdjustment teenExtra = stage == 2 && isAnyHair(name) ? findTeenStyleExtra(modelInstance, name, female) : null;
        StyleAdjustment teenHatExtra = stage == 2 && isAnyHair(name) ? findTeenHatExtra(modelInstance, name, female) : null;
        StyleAdjustment teenHatDelta = stage == 2 && isAnyHair(name) ? findTeenHatDelta(modelInstance, name, female) : null;
        StyleAdjustment exported = isAnyHair(name) ? hatExport117V(modelInstance, name, stage, female) : null;
        StyleAdjustment exportedW = isAnyHair(name) ? childFemaleHatExport117W(modelInstance, name, stage) : null;
        StyleAdjustment exportedX = isAnyHair(name) ? childFemaleHatExport117X(modelInstance, name, stage) : null;
        StyleAdjustment exportedZ = isAnyHair(name) ? childHatExport117Z(modelInstance, name, stage) : null;
        StyleAdjustment exportedB = isAnyHair(name) ? harryExport118B(modelInstance, name, stage, HARRY_EXPORT_118B, true) : null;
        StyleAdjustment exportedC = stage == 1 && isAnyHair(name) ? harryExport118B(modelInstance, name, stage, HARRY_EXPORT_118C, false) : null;
        if (childMaleHarryNative117Z(modelInstance, name, stage)) { fixed = null; user = null; }
        if (stage == 1 && isAnyHair(name) && resetLegacyLocalSpaceFit(modelInstance)) {
            fixed = null;
            user = null;
        }
        boolean generic = stage == 1 && isHair(name);
        boolean teenGeneric = stage == 2 && isAnyHair(name);

        if (!generic && !teenGeneric && fixed == null && user == null && childExtra == null && childHatExtra == null
                && teenExtra == null && teenHatExtra == null && teenHatDelta == null && exported == null && exportedW == null && exportedX == null && exportedZ == null && exportedB == null && exportedC == null) return;

        Object xfrm = publicField(renderData, "xfrm");
        if (xfrm == null) return;

        boolean hat = isHatVariant(name);
        float tx = generic ? (hat ? HAT_X : NORMAL_X) : 0;
        float ty = generic ? ((hat ? HAT_Y : NORMAL_Y) + RUNTIME_LIFT_Y) : 0;
        float tz = generic ? (hat ? HAT_Z : NORMAL_Z) : 0;
        float scale = generic ? SCALE : (teenGeneric ? TEEN_HAIR_SCALE : 1);
        try {
            invokeMatrix(xfrm, "transpose");
            if (tx != 0 || ty != 0 || tz != 0) invokeTranslate(xfrm, tx, ty, tz);
            if (scale != 1) invokeScale(xfrm, scale);
            if (fixed != null) {
                if (fixed.x != 0 || fixed.y != 0 || fixed.z != 0) invokeTranslate(xfrm, fixed.x, fixed.y, fixed.z);
                if (fixed.scale != 1) invokeScale(xfrm, fixed.scale);
            }
            if (user != null) {
                if (user.x != 0 || user.y != 0 || user.z != 0) invokeTranslate(xfrm, user.x, user.y, user.z);
                if (user.scale != 1) invokeScale(xfrm, user.scale);
            }
            if (childExtra != null) {
                if (childExtra.x != 0 || childExtra.y != 0 || childExtra.z != 0) invokeTranslate(xfrm, childExtra.x, childExtra.y, childExtra.z);
                if (childExtra.scale != 1) invokeScale(xfrm, childExtra.scale);
            }
            if (childHatExtra != null) {
                if (childHatExtra.x != 0 || childHatExtra.y != 0 || childHatExtra.z != 0) invokeTranslate(xfrm, childHatExtra.x, childHatExtra.y, childHatExtra.z);
                if (childHatExtra.scale != 1) invokeScale(xfrm, childHatExtra.scale);
            }
            if (teenExtra != null) {
                if (teenExtra.x != 0 || teenExtra.y != 0 || teenExtra.z != 0) invokeTranslate(xfrm, teenExtra.x, teenExtra.y, teenExtra.z);
                if (teenExtra.scale != 1) invokeScale(xfrm, teenExtra.scale);
            }
            if (teenHatExtra != null) {
                if (teenHatExtra.x != 0 || teenHatExtra.y != 0 || teenHatExtra.z != 0) invokeTranslate(xfrm, teenHatExtra.x, teenHatExtra.y, teenHatExtra.z);
                if (teenHatExtra.scale != 1) invokeScale(xfrm, teenHatExtra.scale);
            }
            if (teenHatDelta != null) {
                if (teenHatDelta.x != 0 || teenHatDelta.y != 0 || teenHatDelta.z != 0) invokeTranslate(xfrm, teenHatDelta.x, teenHatDelta.y, teenHatDelta.z);
                if (teenHatDelta.scale != 1) invokeScale(xfrm, teenHatDelta.scale);
            }
            if (exported != null) {
                if (exported.x != 0 || exported.y != 0 || exported.z != 0) invokeTranslate(xfrm, exported.x, exported.y, exported.z);
                if (exported.scale != 1) invokeScale(xfrm, exported.scale);
            }
            if (exportedW != null) {
                if (exportedW.x != 0 || exportedW.y != 0 || exportedW.z != 0) invokeTranslate(xfrm, exportedW.x, exportedW.y, exportedW.z);
                if (exportedW.scale != 1) invokeScale(xfrm, exportedW.scale);
            }
            if (exportedX != null) {
                if (exportedX.x != 0 || exportedX.y != 0 || exportedX.z != 0) invokeTranslate(xfrm, exportedX.x, exportedX.y, exportedX.z);
                if (exportedX.scale != 1) invokeScale(xfrm, exportedX.scale);
            }
            if (exportedZ != null) {
                if (exportedZ.x != 0 || exportedZ.y != 0 || exportedZ.z != 0) invokeTranslate(xfrm, exportedZ.x, exportedZ.y, exportedZ.z);
                if (exportedZ.scale != 1) invokeScale(xfrm, exportedZ.scale);
            }
            if (exportedB != null) {
                if (exportedB.x != 0 || exportedB.y != 0 || exportedB.z != 0) invokeTranslate(xfrm, exportedB.x, exportedB.y, exportedB.z);
                if (exportedB.scale != 1) invokeScale(xfrm, exportedB.scale);
            }
            if (exportedC != null) {
                if (exportedC.x != 0 || exportedC.y != 0 || exportedC.z != 0) invokeTranslate(xfrm, exportedC.x, exportedC.y, exportedC.z);
                if (exportedC.scale != 1) invokeScale(xfrm, exportedC.scale);
            }
            invokeMatrix(xfrm, "transpose");
            Object properties = publicField(renderData, "properties");
            if (properties != null) {
                for (Method m : properties.getClass().getMethods()) {
                    if (!m.getName().equals("SetMatrix4") || m.getParameterCount() != 2) continue;
                    Class<?>[] pt = m.getParameterTypes();
                    if (pt[0] == String.class && pt[1].isAssignableFrom(xfrm.getClass())) {
                        m.invoke(properties, "transform", xfrm);
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    @Patch(className="zombie.core.skinnedmodel.advancedanimation.AnimatedModel$AnimatedModelInstanceRenderData", methodName="initMatrixPalette")
    public static final class RenderHairPaletteTransform {
        private RenderHairPaletteTransform() {}
        @OnExit public static void exit(@This Object renderData) { PatchChildHairTransform.applyPalette(renderData); }
    }

    @Patch(className="zombie.core.skinnedmodel.advancedanimation.AnimatedModel$AnimatedModelInstanceRenderData", methodName="transformToParent")
    public static final class RenderHairTransform {
        private RenderHairTransform() {}
        @OnExit public static void exit(@This Object renderData) { PatchChildHairTransform.applyStatic(renderData); }
    }

    /**
     * Vanilla and Fluffy keep HumanVisual.hairModel on the canonical/base style and
     * choose G1/G4/Hat alternates transiently inside AnimatedModel.addHeadHair().
     * Older PCH paths could leave a hidden alternate persisted as the selected style;
     * rebuilding the model cannot fix that because vanilla faithfully renders the bad
     * selected ID again.  On headwear removal, restore only a UNIQUE canonical parent
     * discovered from the live HairStyles alternate graph. Shared generic alternates are
     * never guessed. Vanilla already calls resetModelNextFrame(), so no duplicate rebuild.
     */
    @Patch(className="zombie.characters.IsoGameCharacter", methodName="setWornItem", strictMatch=true)
    public static final class MinorHatRemovalRefresh {
        private MinorHatRemovalRefresh() {}

        @OnExit
        public static void exit(@This IsoGameCharacter character,
                                @Patch.Argument(0) zombie.scripting.objects.ItemBodyLocation location,
                                @Patch.Argument(1) zombie.inventory.InventoryItem item,
                                @Patch.Argument(2) boolean forceDropTooHeavy) {
            if (character == null || item != null) return;
            if (location != zombie.scripting.objects.ItemBodyLocation.HAT && location != zombie.scripting.objects.ItemBodyLocation.FULL_HAT) return;
            LifeStage stage = LifeStageResolver.stageOf(character);
            if (stage != LifeStage.CHILD && stage != LifeStage.TEEN) return;

            boolean female = character.isFemale();
            restoreCanonicalHair(humanVisualOf(character), female);
            if (character.getDescriptor() != null) {
                restoreCanonicalHair(character.getDescriptor().getHumanVisual(), female);
            }
        }
    }

    public static boolean restoreCanonicalHair(HumanVisual visual, boolean female) {
        if (visual == null) return false;
        String current = visual.getHairModel();
        if (current == null || current.isEmpty()) return false;
        String canonical = PatchChildHairRegistry.canonicalBaseForAlternate(current, female);
        if (canonical == null || canonical.isEmpty() || canonical.equalsIgnoreCase(current)) return false;
        visual.setHairModel(canonical);
        return true;
    }
}
