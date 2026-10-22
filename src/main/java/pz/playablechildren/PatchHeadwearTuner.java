package pz.playablechildren;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.OnExit;
import me.zed_0xff.zombie_buddy.Patch.This;

/**
 * Universal CHILD/TEEN headwear fitting.
 *
 * FIXED89:
 * - Adult/elder remain completely vanilla.
 * - CHILD/TEEN use the original clothing item's vanilla/custom geometry.
 * - All true ClothingItem hats attached to Bip01_Head are detected, not only
 *   filename heuristics, so helmets, caps, hats and third-party headwear can
 *   share the same stage fit.
 * - Legacy Child_BaseballCap assets/items were removed in FIXED94; universal fitting is the only path.
 * - The baseline scales remain the measured FIXED45 values. Carl Hat's
 *   Child/Adult meshes independently show ~1.30 local-size ratio, consistent
 *   with the measured CHILD local head-shell enlargement.
 */
public final class PatchHeadwearTuner {
    /** CHILD local head shell is approximately 123-127% of adult. */
    public static final float CHILD_HEADWEAR_SCALE = 1.25F;
    /** TEEN local head shell is approximately 94-96% of adult. */
    public static final float TEEN_HEADWEAR_SCALE = 0.95F;
    /** CarlHat_Child is ~1.304x CarlHat_Adult. TEEN uses the Adult fit; normalize Child variant to it. */
    public static final float CARL_CHILD_TO_ADULT_SCALE = 0.76687115F;

    /** Baked headwear/accessory deltas exported by the in-game tuner.
     *  These are additive/multiplicative on top of the universal CHILD 1.25 baseline.
     *  Most CHILD fits are shared; explicitly labelled male-only fits are resolved by sex. */
    private static final class HeadwearFit {
        final float x,y,z,scale,rx,ry,rz;
        HeadwearFit(float x,float y,float z,float scale,float rx,float ry,float rz){
            this.x=x; this.y=y; this.z=z; this.scale=scale;
            this.rx=rx; this.ry=ry; this.rz=rz;
        }
    }

    private static final HeadwearFit CHILD_FIT_ARMYHELMETBLUE = new HeadwearFit(+0.0200F,+0.0000F,+0.0000F,1.0000F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_BANDANABLACK = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,0.9710F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_BASEBALLCAP_WESTMAPLECOUNTRYCLUB = new HeadwearFit(+0.0030F,+0.0000F,+0.0000F,1.0290F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_BASEBALLCAPREDKY = new HeadwearFit(-0.0040F,+0.0000F,+0.0000F,1.0440F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_BASEBALLHELMET_GREEN = new HeadwearFit(+0.0180F,+0.0000F,+0.0000F,1.0000F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_BASEBALLHELMET_RED = new HeadwearFit(+0.0210F,+0.0000F,+0.0000F,1.0000F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_BEANYHATWHITE = new HeadwearFit(-0.0170F,+0.0000F,+0.0000F,1.0780F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_BERETGREY = new HeadwearFit(+0.0150F,-0.0000F,+0.0000F,1.0000F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_BONNIEHAT_GREEN = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.0300F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_CAPOTAIN = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.0520F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_CHEFHAT_WHITE = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.0680F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_COWBOYHAT_COWANGUS = new HeadwearFit(-0.0180F,+0.0000F,+0.0000F,1.1460F,+0.0F,+6.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_EARMUFF_WHITE = new HeadwearFit(-0.0060F,+0.0000F,+0.0000F,1.1500F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_FEDORAINDY = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.0650F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_FISHERMANRAINHAT = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.0510F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_GOLFHAT_WHITE = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.0540F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_HOCKEYHELMET = new HeadwearFit(+0.0140F,+0.0000F,+0.0000F,1.0000F,+0.0F,+0.0F,+0.0F);
    /** User-calibrated Carl Hat (Hat_Carl) CHILD delta, shared by male/female. */
    private static final HeadwearFit CHILD_FIT_HAT_CARL = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.0250F,-10.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_JAYCHICKENHAT = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.0660F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_RACCOONHAT = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.0580F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_NEWSPAPERHAT = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.0380F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_STRAWHAT = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.0420F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_SUMMERHATSTRIPE = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.0460F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_TINFOILHAT = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.0540F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_WINTERHATGREY = new HeadwearFit(+0.0110F,+0.0000F,+0.0000F,0.9110F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_MALE_FIT_BASEBALLHELMET_BLUE = new HeadwearFit(+0.0200F,-0.0000F,+0.0000F,1.0300F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_MALE_FIT_BERETARMY = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.0500F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_MALE_FIT_BUCKET_WHITE = new HeadwearFit(-0.0100F,+0.0000F,+0.0000F,1.1450F,+0.0F,+2.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_VISOR = new HeadwearFit(-0.0210F,-0.0010F,+0.0100F,1.0600F,+0.0F,+0.0F,+0.0F);

    private static final HeadwearFit CHILD_FIT_GLASSES_3D = new HeadwearFit(-0.0200F,+0.0000F,+0.0000F,1.5100F,+0.0F,+12.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_GLASSES_AVIATORS = new HeadwearFit(+0.0000F,+0.0000F,+0.0100F,1.3000F,+0.0F,+8.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_GLASSES_CAT_EYE = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.2750F,+0.0F,+6.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_GLASSES_GROUCHO = new HeadwearFit(+0.0000F,+0.0000F,-0.0050F,1.3750F,+0.0F,+4.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_GLASSES_NORMAL = new HeadwearFit(+0.0000F,+0.0000F,-0.0050F,1.3750F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_GLASSES_READING = new HeadwearFit(+0.0350F,-0.0000F,-0.0100F,1.2000F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_GLASSES_SAFETY = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.3150F,+0.0F,+4.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_GLASSES_SUN = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.3300F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_MONOCLE = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.5100F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit CHILD_FIT_EYEPATCH = new HeadwearFit(+0.0100F,+0.0000F,+0.0000F,1.2590F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_GLASSES_ALL = new HeadwearFit(+0.0050F,+0.0000F,+0.0150F,1.2300F,+0.0F,+12.0F,+0.0F);

    /** FIXED109: user-calibrated TEEN headwear deltas.
     *  TEEN female inherits TEEN male by family unless a female-specific override exists.
     *  Values remain tuner deltas: they compose on top of the existing stage baseline. */
    private static final HeadwearFit TEEN_FIT_ARMYHELMET = new HeadwearFit(+0.0400F,-0.0000F,+0.0000F,1.0200F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_BANDANA_RAG = new HeadwearFit(+0.0000F,+0.0020F,+0.0000F,1.1850F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_BANDANA = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.2100F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_BASEBALLCAP_FRONT = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.2000F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_BASEBALLCAP_REVERSE = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1900F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_BASEBALLHELMET = new HeadwearFit(+0.0200F,+0.0000F,+0.0000F,1.1200F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_BEANY = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1500F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_BERET = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.2400F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_BICYCLEHELMET = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1500F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_BOONIE_CAMO = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.2200F,+0.0F,+2.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_BOONIE_GREEN = new HeadwearFit(-0.0150F,+0.0000F,+0.0160F,1.3250F,+2.0F,+6.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_BUCKET = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.2800F,+0.0F,+2.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_CAPOTAIN = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1700F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_CHEF = new HeadwearFit(-0.0100F,+0.0000F,+0.0000F,1.3300F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_COWBOY = new HeadwearFit(+0.0000F,+0.0000F,+0.0100F,1.2400F,+0.0F,+4.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_CRASH_NORMAL = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1650F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_CRASH_STARS = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1600F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_FEDORA = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.2300F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_FIREMAN = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1700F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_GOLF = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.2500F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_HARDHAT = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1250F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_CARL = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,0.9210F,-10.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_HEADBANDIMP = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.2100F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_HOCKEY = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.2450F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_JAYCHICKEN = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.2800F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_JOCKEY = new HeadwearFit(+0.0000F,+0.0000F,+0.0100F,1.1850F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_M1 = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1500F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_SHOWERCAP = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.2100F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_NEWSPAPER = new HeadwearFit(-0.0100F,+0.0000F,+0.0000F,1.2900F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_PEAKCAP = new HeadwearFit(+0.0100F,+0.0000F,+0.0000F,1.2300F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_PIRATE = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1700F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_POLICE = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1800F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_SANTA = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1500F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_STOVEPIPE = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1600F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_STRAW = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.2400F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_SUMMER = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1800F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_SWEATBAND = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.2300F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_TINFOIL = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.2600F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_VISOR = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1700F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_WINTER = new HeadwearFit(+0.0200F,+0.0000F,+0.0000F,1.0600F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_WITCH = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1900F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FIT_WOOLY = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.1400F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_MALE_FIT_BASEBALLCAP_PIZZAWHIRLED = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.0200F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_MALE_FIT_BASEBALLCAP = new HeadwearFit(-0.0080F,+0.0000F,+0.0060F,1.0860F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_MALE_FIT_CRASH = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.0320F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_MALE_FIT_TACTICAL = new HeadwearFit(+0.0100F,+0.0000F,+0.0000F,1.2260F,+0.0F,+0.0F,+0.0F);

    // TEEN female explicit user overrides. All other families inherit TEEN male deltas.
    private static final HeadwearFit TEEN_FEMALE_FIT_ARMYHELMET = new HeadwearFit(+0.0100F,+0.0000F,+0.0000F,1.1900F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FEMALE_FIT_BASEBALLCAP_FRONT = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.3000F,+0.0F,+0.0F,+0.0F);
    private static final HeadwearFit TEEN_FEMALE_FIT_BASEBALLCAP_REVERSE = new HeadwearFit(+0.0000F,+0.0000F,+0.0000F,1.2700F,+0.0F,+0.0F,+0.0F);

    private static final Map<String, HeadwearFit> CHILD_MALE_DELTAS = new HashMap<>();
    private static final Map<String, HeadwearFit> TEEN_MALE_DELTAS = new HashMap<>();
    private static final Map<String, HeadwearFit> TEEN_FEMALE_DELTAS = new HashMap<>();

    static {
        CHILD_MALE_DELTAS.put("buckethatfishing", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.055000F, +0.000000F, +0.000000F, +0.000000F));
        CHILD_MALE_DELTAS.put("peakcapyacht", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.025000F, +0.000000F, +0.000000F, +0.000000F));
        CHILD_MALE_DELTAS.put("piratehat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.055000F, +0.000000F, +0.000000F, +0.000000F));
        CHILD_MALE_DELTAS.put("policehatblue", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030000F, +0.000000F, +0.000000F, +0.000000F));
        CHILD_MALE_DELTAS.put("policehatranger", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030000F, +0.000000F, +0.000000F, +0.000000F));
        CHILD_MALE_DELTAS.put("stovepipehat_black", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.045000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("armyhelmet_desert", new HeadwearFit(-0.020000F, +0.000000F, +0.000000F, +1.226000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("armyhelmetblue", new HeadwearFit(-0.030000F, +0.000000F, +0.000000F, +1.226000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("beanyhatwhite", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.086300F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("capotain", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.050000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("cowboy_cowsimmental", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.248450F, +0.000000F, +4.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("fishermanrainhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.255000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("raccoonhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.257450F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("partyhat", new HeadwearFit(+0.065000F, +0.000000F, +0.015000F, +1.000000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("partyhat_white", new HeadwearFit(+0.060000F, +0.000000F, +0.000000F, +1.000000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("piratehat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.040000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("stovepipehat_black", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.050000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("stovepipehatstars", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.055000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("summerhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.045500F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("summerhat_hide", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.035000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("surgicalcapblue", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.235000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("visor_white", new HeadwearFit(-0.005000F, +0.000000F, +0.000000F, +1.240000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("wizardhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.225700F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("woolyhat_white", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.050000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("bandana_skull", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("bandanared", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("beanyhatwhite", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.165500F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("bonniehat_camogreen", new HeadwearFit(+0.000000F, +0.000000F, +0.015000F, +1.085000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("bonniehat_camoolivedrab", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.095000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("bonniehat_green", new HeadwearFit(+0.011000F, +0.000000F, +0.000000F, +1.038375F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("capotain", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("cowboy_cowsimmental", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.280000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("crashhelmet_normal", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.118250F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("crashhelmet_police", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.123575F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("crashhelmet_tactical", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.091800F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("earmuff_white", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.255000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("raccoonhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.309100F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("fedora", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.075000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("fedoradelmonte", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.055000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("fishermanrainhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.344150F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("golfhat_tartan02", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.065900F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("golfhat_white", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.065900F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("hardhatminer", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.130000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("headbandimp_green", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("jaychickenhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.166300F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("m1helmet01", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.140000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("newspaperhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("partyhat", new HeadwearFit(+0.060000F, +0.000000F, +0.000000F, +1.000000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("partyhat_white", new HeadwearFit(+0.055000F, +0.005000F, -0.005000F, +1.000000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("peakcapyacht", new HeadwearFit(-0.005000F, +0.000000F, +0.000000F, +1.025000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("piratehat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.095000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("policehatsheriff", new HeadwearFit(-0.010000F, +0.000000F, +0.000000F, +1.125000F, +0.000000F, +2.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("santahat_green", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("santahat_red", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.070000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("stovepipehat_black", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("stovepipehatstars", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.110000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("summerhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.065000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("summerhat_hide", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.075000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("summerhat_tarp", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.070000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("summerhatflowers", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.070000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("tactical_helmet_blue", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.282350F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("visorstripe_red", new HeadwearFit(-0.053000F, +0.000000F, +0.015000F, +1.325171F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("witcheshat", new HeadwearFit(-0.010000F, +0.000000F, +0.000000F, +1.107750F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("wizardhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.270000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("woolyhat_white", new HeadwearFit(+0.005000F, +0.000000F, +0.005000F, +1.055000F, +0.000000F, +0.000000F, +0.000000F));
        CHILD_MALE_DELTAS.put("buckethat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.055000F, +0.000000F, +0.000000F, +0.000000F));
        CHILD_MALE_DELTAS.put("buckethat_white", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.055000F, +0.000000F, +0.000000F, +0.000000F));
        CHILD_MALE_DELTAS.put("peakedcaparmy", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.025000F, +0.000000F, +0.000000F, +0.000000F));
        CHILD_MALE_DELTAS.put("peakcaparmy", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.025000F, +0.000000F, +0.000000F, +0.000000F));
        CHILD_MALE_DELTAS.put("policehat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030000F, +0.000000F, +0.000000F, +0.000000F));
        CHILD_MALE_DELTAS.put("policehatgrey", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030000F, +0.000000F, +0.000000F, +0.000000F));
        CHILD_MALE_DELTAS.put("policehatsheriff", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030000F, +0.000000F, +0.000000F, +0.000000F));
        CHILD_MALE_DELTAS.put("stovepipehat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.045000F, +0.000000F, +0.000000F, +0.000000F));
        CHILD_MALE_DELTAS.put("stovepipehat_brown", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.045000F, +0.000000F, +0.000000F, +0.000000F));
        CHILD_MALE_DELTAS.put("stovepipehatstars", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.045000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("beany", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.065000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("cowboyhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.230000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("cowboyhat_cowangus", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.230000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("cowboyhat_black", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.230000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("cowboyhat_cowholstein", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.230000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("cowboyhat_blue", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.230000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("cowboyhat_green", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.230000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("cowboyhat_pink", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.230000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("cowboyhat_yellow", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.230000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("cowboyhat_white", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.230000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("cowboyhat_brown", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.230000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("fishingrainhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.255000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("showercap", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.037400F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("surgicalcapgreen", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.235000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("visorhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.240000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("visorstripe_black", new HeadwearFit(-0.010000F, +0.000000F, +0.000000F, +1.072600F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("visorstripe_red", new HeadwearFit(-0.010000F, +0.000000F, +0.000000F, +1.072600F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("witcheshat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.190000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("woolyhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.050000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("armyhelmet", new HeadwearFit(-0.025000F, +0.000000F, +0.000000F, +1.226000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("baseballcap_pizzawhirled", new HeadwearFit(-0.008000F, +0.000000F, +0.006000F, +1.248900F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("baseballhelmet_green", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.015000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("bonniehat_camodcu", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.020000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("bonniehat_camogreen", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.015000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("bonniehat_camoolivedrab", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.020000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("bonniehat_camoreddawn", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.020000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("bonniehat_camotigerstripes", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("fedora", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("fedoradelmonte", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.035000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("firemanhat_red", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.025000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("hardhatminer", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.025000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("hockeyhelmet", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("jaychickenhat", new HeadwearFit(+0.005000F, +0.000000F, +0.000000F, +1.110000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("policehatblue", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.055000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("policehatgrey", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.050000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("policehatranger", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("policehatsheriff", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.040000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("santahat_green", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.050000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("stovepipehat_brown", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.050000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("summerhat_tarp", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.045000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("cea_glasses", new HeadwearFit(-0.015000F, +0.000000F, +0.000000F, +1.000000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_MALE_DELTAS.put("cea_bandage_left", new HeadwearFit(-0.001F, 0.003F, 0F, 1.155F, 18F, 0F, 0F));
        TEEN_MALE_DELTAS.put("cea_bandage_right", new HeadwearFit(-0.001F, 0.003F, 0F, 1.155F, 18F, 0F, 0F));
        TEEN_MALE_DELTAS.put("cea_leather_left", new HeadwearFit(0.001F, 0.003F, 0F, 1.180F, 8F, 0F, 0F));
        TEEN_MALE_DELTAS.put("cea_leather_right", new HeadwearFit(0.001F, 0.003F, 0F, 1.180F, 8F, 0F, 0F));
        TEEN_FEMALE_DELTAS.put("m1armyhelmet", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.140000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("bandana", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("bandanagreen", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("bandanablack", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("bandanablue", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("bandanawhite", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("bandana_rag", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("sweatbandimp", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("headbandimp_black", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("headbandimp_blue", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("headbandimp_red", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("headbandimp_white", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("sweatband_leather", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("sweatband_rag", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("beany", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.165500F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("cowboyhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030400F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("cowboyhat_cowangus", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030400F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("cowboyhat_black", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030400F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("cowboyhat_cowholstein", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030400F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("cowboyhat_blue", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030400F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("cowboyhat_green", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030400F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("cowboyhat_pink", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030400F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("cowboyhat_yellow", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030400F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("cowboyhat_white", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.011200F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("cowboyhat_brown", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030400F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("crashhelmet", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.065000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("crashhelmet_stars", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.102275F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("earmuffs", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.255000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("earmuff_protectors", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.255000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("fishingrainhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.344150F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("golfhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.065900F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("golfhat_tartan", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.065900F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("golfhat_tartanwhite", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.065900F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("golfhat_tartan03", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.065900F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("hardhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.130000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("hardhatblue", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.130000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("hardhatred", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.130000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("hardhatwhite", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.130000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("peakedcaparmy", new HeadwearFit(-0.005000F, +0.000000F, +0.000000F, +1.025000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("peakcaparmy", new HeadwearFit(-0.005000F, +0.000000F, +0.000000F, +1.025000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("policehat", new HeadwearFit(-0.010000F, +0.000000F, +0.000000F, +1.125000F, +0.000000F, +2.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("policehatgrey", new HeadwearFit(-0.010000F, +0.000000F, +0.000000F, +1.125000F, +0.000000F, +2.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("policehatblue", new HeadwearFit(-0.010000F, +0.000000F, +0.000000F, +1.125000F, +0.000000F, +2.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("policehatranger", new HeadwearFit(-0.010000F, +0.000000F, +0.000000F, +1.125000F, +0.000000F, +2.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("summerflowerhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.070000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("visor", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.292850F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("visor_white", new HeadwearFit(-0.083000F, +0.000000F, +0.015000F, +1.484192F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("visorstripe_black", new HeadwearFit(-0.053000F, +0.000000F, +0.015000F, +1.325171F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("woolyhat", new HeadwearFit(+0.005000F, +0.000000F, +0.005000F, +1.055000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("baseballhelmet_blue", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.035000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("beretgrey", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.085000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("bicyclehelmet", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.112925F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("bonniehat_camodcu", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.125000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("bonniehat_camoreddawn", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.105000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("bonniehat_camotigerstripes", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("bonniehat_desert", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.105000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("buckethat_white", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.035000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("buckethatfishing", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.030000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("chefhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("chefhat_spiffo", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("chefhat_black", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("chefhat_blue", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("chefhat_green", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("chefhat_red", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("chefhat_white", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("chefhat_yellow", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("firemanhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.095000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("firemanhat_red", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.095000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("headmirror", new HeadwearFit(-0.001000F, +0.000000F, +0.000000F, +1.407000F, +14.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("stovepipehat_brown", new HeadwearFit(-0.005000F, +0.000000F, +0.000000F, +1.125000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("strawhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.090000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("summerhatstripe", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("surgicalcapblue", new HeadwearFit(+0.000000F, +0.000000F, +0.005000F, +1.175000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("sweatband_skull", new HeadwearFit(+0.005000F, +0.000000F, +0.000000F, +1.000000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("tactical_helmet_gray", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.290000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("tactical_helmet_green", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.285000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("winterhatblue", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.020000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("baseballhelmet_red", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.035000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("baseballhelmet_green", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.035000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("baseballhelmet", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.035000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("beret", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.085000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("summerhatpink", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.060000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("surgicalcapgreen", new HeadwearFit(+0.000000F, +0.000000F, +0.005000F, +1.175000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("sweatband_white", new HeadwearFit(+0.005000F, +0.000000F, +0.000000F, +1.000000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("sweatband", new HeadwearFit(+0.005000F, +0.000000F, +0.000000F, +1.000000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("winterhatgreen", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.020000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("winterhatgrey", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.020000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("winterhat_sheepskin", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.020000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("winterhat", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.020000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("hockeyhelmet", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.045000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("jockeyhelmet_no1", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.090000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("jockeyhelmet_riding", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("jockeyhelmet_no2", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("jockeyhelmet_no3", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("jockeyhelmet_no4", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("jockeyhelmet_no5", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("jockeyhelmet_no6", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
        TEEN_FEMALE_DELTAS.put("jockeyhelmet", new HeadwearFit(+0.000000F, +0.000000F, +0.000000F, +1.115000F, +0.000000F, +0.000000F, +0.000000F));
    }

    private static String canonicalHeadwearToken(String token){
        String s=token==null?"":token.toLowerCase(Locale.ENGLISH);
        if(s.startsWith("m_")||s.startsWith("f_")) s=s.substring(2);
        return s;
    }

    private static HeadwearFit childHeadwearFit(String token, boolean female){
        String s=canonicalHeadwearToken(token);
        if(!female && s.equals("baseballhelmet_blue")) return CHILD_MALE_FIT_BASEBALLHELMET_BLUE;
        if(!female && s.equals("beretarmy")) return CHILD_MALE_FIT_BERETARMY;
        if(!female && s.equals("buckethat_white")) return CHILD_MALE_FIT_BUCKET_WHITE;
        if(s.startsWith("visor")) return CHILD_FIT_VISOR;
        switch(s){
            case "armyhelmetblue": return CHILD_FIT_ARMYHELMETBLUE;
            case "bandanablack": return CHILD_FIT_BANDANABLACK;
            case "baseballcap_westmaplecountryclub": return CHILD_FIT_BASEBALLCAP_WESTMAPLECOUNTRYCLUB;
            case "baseballcapredky": return CHILD_FIT_BASEBALLCAPREDKY;
            case "baseballhelmet_green": return CHILD_FIT_BASEBALLHELMET_GREEN;
            case "baseballhelmet_red": return CHILD_FIT_BASEBALLHELMET_RED;
            case "beanyhatwhite": return CHILD_FIT_BEANYHATWHITE;
            case "beretgrey": return CHILD_FIT_BERETGREY;
            case "bonniehat_green": return CHILD_FIT_BONNIEHAT_GREEN;
            case "capotain": return CHILD_FIT_CAPOTAIN;
            case "chefhat_white": return CHILD_FIT_CHEFHAT_WHITE;
            case "cowboyhat_cowangus": return CHILD_FIT_COWBOYHAT_COWANGUS;
            case "earmuff_white": return CHILD_FIT_EARMUFF_WHITE;
            case "fedoraindy": return CHILD_FIT_FEDORAINDY;
            case "fishermanrainhat": return CHILD_FIT_FISHERMANRAINHAT;
            case "golfhat_white": return CHILD_FIT_GOLFHAT_WHITE;
            case "hockeyhelmet": return CHILD_FIT_HOCKEYHELMET;
            case "hat_carl": return CHILD_FIT_HAT_CARL;
            case "jaychickenhat": return CHILD_FIT_JAYCHICKENHAT;
            case "raccoonhat": return CHILD_FIT_RACCOONHAT;
            case "newspaperhat": return CHILD_FIT_NEWSPAPERHAT;
            case "strawhat": return CHILD_FIT_STRAWHAT;
            case "summerhatstripe": return CHILD_FIT_SUMMERHATSTRIPE;
            case "tinfoilhat": return CHILD_FIT_TINFOILHAT;
            case "winterhatgrey": return CHILD_FIT_WINTERHATGREY;
            default: break;
        }
        // Family fallbacks: different item/color variants that render the same hat family
        // inherit the user's single measured fit. More specific special models above win.
        if(s.startsWith("armyhelmet")) return CHILD_FIT_ARMYHELMETBLUE;
        if(s.startsWith("bandana")) return CHILD_FIT_BANDANABLACK;
        if(s.startsWith("baseballcapredky")) return CHILD_FIT_BASEBALLCAPREDKY;
        if(s.startsWith("baseballcap")) return CHILD_FIT_BASEBALLCAP_WESTMAPLECOUNTRYCLUB;
        if(s.startsWith("baseballhelmet_green")) return CHILD_FIT_BASEBALLHELMET_GREEN;
        if(s.startsWith("baseballhelmet_red")) return CHILD_FIT_BASEBALLHELMET_RED;
        if(s.startsWith("beanyhat") || s.startsWith("beaniehat")) return CHILD_FIT_BEANYHATWHITE;
        if(s.startsWith("beret")) return CHILD_FIT_BERETGREY;
        if(s.startsWith("bonniehat") || s.startsWith("booniehat")) return CHILD_FIT_BONNIEHAT_GREEN;
        if(s.startsWith("chefhat")) return CHILD_FIT_CHEFHAT_WHITE;
        if(s.startsWith("cowboyhat")) return CHILD_FIT_COWBOYHAT_COWANGUS;
        if(s.startsWith("earmuff")) return CHILD_FIT_EARMUFF_WHITE;
        if(s.startsWith("fedora")) return CHILD_FIT_FEDORAINDY;
        if(s.startsWith("fishermanrainhat")) return CHILD_FIT_FISHERMANRAINHAT;
        if(s.startsWith("golfhat")) return CHILD_FIT_GOLFHAT_WHITE;
        if(s.startsWith("hockeyhelmet")) return CHILD_FIT_HOCKEYHELMET;
        if(s.startsWith("jaychickenhat")) return CHILD_FIT_JAYCHICKENHAT;
        if(s.contains("raccoonhat")) return CHILD_FIT_RACCOONHAT;
        if(s.startsWith("newspaperhat")) return CHILD_FIT_NEWSPAPERHAT;
        if(s.startsWith("strawhat")) return CHILD_FIT_STRAWHAT;
        if(s.startsWith("summerhat")) return CHILD_FIT_SUMMERHATSTRIPE;
        if(s.startsWith("tinfoilhat")) return CHILD_FIT_TINFOILHAT;
        if(s.startsWith("winterhat")) return CHILD_FIT_WINTERHATGREY;
        return null;
    }

    private static String itemTypeToken(Object modelInstance){
        try{
            Object visual=callNoArgs(modelInstance,"getItemVisual");
            Object value=callNoArgs(visual,"getItemType");
            return value==null?"":String.valueOf(value).toLowerCase(Locale.ENGLISH);
        }catch(Throwable ignored){ return ""; }
    }

    private static boolean reverseBaseballCap(String token,String itemType){
        String s=canonicalHeadwearToken(token);
        String item=itemType==null?"":itemType.toLowerCase(Locale.ENGLISH);
        return s.contains("reverse") || item.contains("baseballcap") && item.contains("_reverse");
    }

    private static HeadwearFit teenMaleHeadwearFit(String token,String itemType){
        String s=canonicalHeadwearToken(token);
        // Exact measured tokens win when two members of the same visual family were calibrated separately.
        if(s.equals("baseballcap_pizzawhirled")) return TEEN_MALE_FIT_BASEBALLCAP_PIZZAWHIRLED;
        if(s.equals("bandana_rag")) return TEEN_FIT_BANDANA_RAG;
        if(s.equals("bandanawhite")) return TEEN_FIT_BANDANA;
        if(s.equals("bonniehat_camoolivedrab")) return TEEN_FIT_BOONIE_CAMO;
        if(s.equals("bonniehat_green")) return TEEN_FIT_BOONIE_GREEN;
        if(s.equals("crashhelmet_stars")) return TEEN_FIT_CRASH_STARS;
        if(s.equals("crashhelmet_normal")) return TEEN_FIT_CRASH_NORMAL;
        if(s.equals("hat_carl")) return TEEN_FIT_CARL;

        // Family propagation. These families share the same or sufficiently similar vanilla geometry.
        if(s.startsWith("armyhelmet")) return TEEN_FIT_ARMYHELMET;
        if(s.startsWith("bandanamask")) return null; // face covering, not the calibrated head-bandana family
        if(s.startsWith("bandana")) return s.contains("rag") ? TEEN_FIT_BANDANA_RAG : TEEN_FIT_BANDANA;
        if(s.startsWith("baseballcap")) return reverseBaseballCap(s,itemType) ? TEEN_FIT_BASEBALLCAP_REVERSE : TEEN_FIT_BASEBALLCAP_FRONT;
        if(s.startsWith("baseballhelmet")) return TEEN_FIT_BASEBALLHELMET;
        if(s.startsWith("beany")||s.startsWith("beanie")) return TEEN_FIT_BEANY;
        if(s.startsWith("beret")) return TEEN_FIT_BERET;
        if(s.startsWith("bicyclehelmet")) return TEEN_FIT_BICYCLEHELMET;
        if(s.startsWith("bonniehat_camo")||s.startsWith("booniehat_camo")) return TEEN_FIT_BOONIE_CAMO;
        if(s.startsWith("bonniehat")||s.startsWith("booniehat")) return TEEN_FIT_BOONIE_GREEN;
        if(s.startsWith("buckethat")) return TEEN_FIT_BUCKET;
        if(s.startsWith("capotain")) return TEEN_FIT_CAPOTAIN;
        if(s.startsWith("chefhat")) return TEEN_FIT_CHEF;
        if(s.startsWith("cowboyhat")) return TEEN_FIT_COWBOY;
        if(s.startsWith("crashhelmet")&&!s.startsWith("crashhelmetfull")) return s.contains("stars") ? TEEN_FIT_CRASH_STARS : TEEN_FIT_CRASH_NORMAL;
        if(s.startsWith("fedora")) return TEEN_FIT_FEDORA;
        if(s.startsWith("firemanhat")) return TEEN_FIT_FIREMAN;
        if(s.startsWith("golfhat")) return TEEN_FIT_GOLF;
        if(s.startsWith("hardhat")) return TEEN_FIT_HARDHAT;
        if(s.startsWith("headbandimp")) return TEEN_FIT_HEADBANDIMP;
        if(s.startsWith("hockeyhelmet")||s.startsWith("icehockeyhelmet")) return TEEN_FIT_HOCKEY;
        if(s.startsWith("jaychickenhat")) return TEEN_FIT_JAYCHICKEN;
        if(s.startsWith("jockeyhelmet")) return TEEN_FIT_JOCKEY;
        if(s.startsWith("m1helmet")) return TEEN_FIT_M1;
        if(s.startsWith("showercap")) return TEEN_FIT_SHOWERCAP;
        if(s.startsWith("newspaperhat")) return TEEN_FIT_NEWSPAPER;
        if(s.startsWith("peakcap")) return TEEN_FIT_PEAKCAP;
        if(s.startsWith("piratehat")) return TEEN_FIT_PIRATE;
        if(s.startsWith("policehat")) return TEEN_FIT_POLICE;
        if(s.startsWith("santahat")) return TEEN_FIT_SANTA;
        if(s.startsWith("stovepipehat")) return TEEN_FIT_STOVEPIPE;
        if(s.startsWith("strawhat")) return TEEN_FIT_STRAW;
        if(s.startsWith("summerhat")) return TEEN_FIT_SUMMER;
        if(s.startsWith("sweatband")) return TEEN_FIT_SWEATBAND;
        if(s.startsWith("tinfoilhat")) return TEEN_FIT_TINFOIL;
        if(s.startsWith("visorstripe")) return TEEN_FIT_VISOR;
        if(s.startsWith("winterhat")) return TEEN_FIT_WINTER;
        if(s.startsWith("witcheshat")) return TEEN_FIT_WITCH;
        if(s.startsWith("woolyhat")) return TEEN_FIT_WOOLY;
        return null;
    }

    private static HeadwearFit teenFemaleHeadwearFit(String token,String itemType){
        String s=canonicalHeadwearToken(token);
        if(s.startsWith("armyhelmet")) return TEEN_FEMALE_FIT_ARMYHELMET;
        if(s.startsWith("baseballcap")) return reverseBaseballCap(s,itemType) ? TEEN_FEMALE_FIT_BASEBALLCAP_REVERSE : TEEN_FEMALE_FIT_BASEBALLCAP_FRONT;
        return teenMaleHeadwearFit(s,itemType);
    }

    private static String canonicalAccessoryToken(String token){
        String s=canonicalHeadwearToken(token);
        if(s.endsWith(".x")) s=s.substring(0,s.length()-2);
        return s;
    }

    private static boolean eyewearName(String name){
        String s=canonicalAccessoryToken(rawToken(name));
        return s.contains("glasses") || s.contains("eyeglass") || s.contains("eyewear")
            || s.contains("goggle") || s.contains("monocle");
    }

    private static HeadwearFit childAccessoryFit(String token,String itemType){
        String s=canonicalAccessoryToken(token);
        String item=itemType==null?"":itemType.toLowerCase(Locale.ENGLISH);
        String family=s+" "+item;
        if(family.contains("3dglasses")) return CHILD_FIT_GLASSES_3D;
        if(family.contains("aviator")) return CHILD_FIT_GLASSES_AVIATORS;
        if(family.contains("cateye")||family.contains("catseye")) return CHILD_FIT_GLASSES_CAT_EYE;
        if(family.contains("groucho")) return CHILD_FIT_GLASSES_GROUCHO;
        if(family.contains("reading")||family.contains("halfmoon")) return CHILD_FIT_GLASSES_READING;
        if(family.contains("goggle")) return CHILD_FIT_GLASSES_SAFETY;
        if(family.contains("monocle")) return CHILD_FIT_MONOCLE;
        if(family.contains("sun")||family.contains("shade")||family.contains("jackieo")
                ||family.contains("newwave")||family.contains("oakley")||family.contains("shooting")
                ||family.contains("venetian")||family.contains("xray")) return CHILD_FIT_GLASSES_SUN;
        return CHILD_FIT_GLASSES_NORMAL;
    }

    /** Cache classification by rendered model token to avoid repeated reflection per frame. */
    private static final ConcurrentHashMap<String, Boolean> HEADWEAR_CACHE = new ConcurrentHashMap<>();

    // CHILD-female geometry parity is handled after vanilla model creation by
    // PatchChildFemaleHeadwear. Only explicitly labelled CHILD Male exports are sex-specific here.

    private PatchHeadwearTuner() {}
    private static String rawToken(String name){
        if(name==null) return "";
        String s=name.toLowerCase(Locale.ENGLISH).replace('\\','/');
        int slash=s.lastIndexOf('/'); if(slash>=0) s=s.substring(slash+1);
        int cut=s.length();
        for(char c:new char[]{'&','|','#','?',';'}){ int i=s.indexOf(c); if(i>=0&&i<cut) cut=i; }
        return s.substring(0,cut);
    }

    private static boolean clothingPath(String name){
        if(name==null||name.isEmpty()) return false;
        String path=name.toLowerCase(Locale.ENGLISH).replace('\\','/');
        return path.contains("/clothes/") || path.startsWith("static/clothes/") || path.startsWith("skinned/clothes/");
    }

    /** Fast filename path for common vanilla/mod headwear. */
    private static boolean headwearName(String name){
        if(!clothingPath(name)) return false;
        String s=rawToken(name);
        if(s.startsWith("hat_")) return true;
        boolean modelPrefix=s.startsWith("m_")||s.startsWith("f_");
        if(!modelPrefix) return s.endsWith("_hat")||s.contains("hat_")||s.startsWith("baseballcap")
            ||s.contains("helmet")||s.contains("hardhat")||s.contains("fireman")
            ||s.contains("beret")||s.contains("fedora")||s.contains("cowboy")
            ||s.contains("beany")||s.contains("beanie")||s.contains("wooly")
            ||s.contains("bonnie")||s.contains("raccoon")||s.contains("visor")
            ||s.contains("cap_")||s.endsWith("cap");
        return s.endsWith("hat")||s.contains("hat_")||s.contains("baseballcap")
            ||s.contains("helmet")||s.contains("hardhat")||s.contains("fireman")
            ||s.contains("beret")||s.contains("fedora")||s.contains("cowboy")
            ||s.contains("beany")||s.contains("beanie")||s.contains("wooly")
            ||s.contains("bonnie")||s.contains("raccoon")||s.contains("visor")
            ||s.contains("cap_")||s.endsWith("cap");
    }

    /** Face/head accessories that should not receive the generic hat baseline. */
    private static boolean accessoryName(String name){
        if(name==null||name.isEmpty()) return false;
        String s=rawToken(name);
        return s.contains("glasses") || s.contains("eyeglass") || s.contains("eyewear")
            || s.contains("goggle") || s.contains("monocle") || s.contains("eyepatch")
            || s.contains("face_mask") || s.startsWith("mask_") || s.contains("facemask")
            || s.contains("earring") || s.contains("ear_ring") || s.contains("nose_ring");
    }

    private static Object callNoArgs(Object target, String methodName){
        if(target==null) return null;
        try{
            Method method=ReflectionAccess.method(target.getClass(),methodName);
            return method==null?null:method.invoke(target);
        }catch(Throwable ignored){ return null; }
    }

    /**
     * Reliable fallback for every real ClothingItem hat, including third-party hats whose
     * model filename does not contain a predictable token (CarlHat is one such example).
     */
    private static boolean clothingItemHeadwear(Object modelInstance, String modelName){
        String key=modelName==null?"":modelName.toLowerCase(Locale.ENGLISH);
        Boolean cached=HEADWEAR_CACHE.get(key);
        if(cached!=null) return cached;

        boolean result=headwearName(modelName);
        if(!result && modelInstance!=null){
            try{
                Object visual=callNoArgs(modelInstance,"getItemVisual");
                Object clothing=callNoArgs(visual,"getClothingItem");
                if(clothing!=null){
                    Object hat=callNoArgs(clothing,"isHat");
                    String attach=String.valueOf(ReflectionAccess.tryGet(clothing,"attachBone",""));
                    result=Boolean.TRUE.equals(hat) && "Bip01_Head".equalsIgnoreCase(attach);
                }
            }catch(Throwable ignored){}
        }
        if(!key.isEmpty()) HEADWEAR_CACHE.putIfAbsent(key,result);
        return result;
    }

    /** 0=adult/unknown, 1=child, 2=teen. */
    private static int stageOf(Object mi){
        LifeStage stage=PatchChildHairTransform.resolveRootStage(mi);
        if(stage==LifeStage.TEEN) return 2;
        if(stage==LifeStage.CHILD) return 1;
        return 0;
    }

    /**
     * Resolve TEEN sex from the same model-instance tree that owns the headwear.
     * The preview has no IsoGameCharacter, so the TeenBody/TeenFemaleBody parent
     * is the authoritative path there; gameplay falls back to character.isFemale().
     */
    private static boolean isMinorFemale(Object modelInstance){
        Object current=modelInstance;
        for(int depth=0; current!=null && depth<12; depth++){
            String n=PatchChildHairTransform.modelName(current);
            if(n.contains("playablechildren/teenfemalebody")) return true;
            if(n.contains("playablechildren/teenbody")) return false;
            Object character=ReflectionAccess.tryGet(current,"character",null);
            Object female=callNoArgs(character,"isFemale");
            if(female instanceof Boolean) return ((Boolean)female).booleanValue();
            Object owner=callNoArgs(current,"getOwner");
            Object visual=callNoArgs(owner,"getVisual");
            if(visual==null) visual=callNoArgs(owner,"getHumanVisual");
            female=callNoArgs(visual,"isFemale");
            if(female instanceof Boolean) return ((Boolean)female).booleanValue();
            current=ReflectionAccess.tryGet(current,"parent",null);
        }
        return false;
    }

    // DEV117V exported increments retain the exact live ordering, including rotations.
    private static final HeadwearFit CHILD_FEMALE_BANDAGE_RIGHT_117W = new HeadwearFit(-.004F,.003F,-.001F,1,0,0,0);
    private static final HeadwearFit CHILD_FEMALE_CARLHAT_BANDAGE_RIGHT_117W = new HeadwearFit(-.001F,0,0,.980F,0,0,0);
    private static final HeadwearFit CHILD_FEMALE_CARLHAT_BANDAGE_RIGHT_117X = new HeadwearFit(-.005F,0,0,1,0,0,0);
    // DEV117Y: incremental export, both CHILD sexes, Right + Carl Hat only.
    private static final float CHILD_CARLHAT_BANDAGE_RIGHT_117Y_SCALE = .983F;
    private static final HeadwearFit TEEN_MALE_CRASHHELMET_STARS_117X = new HeadwearFit(0,-.025F,0,1,0,0,0);

    private static final Map<String, HeadwearFit> EXPORT_117V = new HashMap<>();
    static {
        EXPORT_117V.put("CHILD_MALE|surgicalcapblue", new HeadwearFit(0,0,0,1.020F,0,0,0));
        EXPORT_117V.put("CHILD_MALE|tactical_helmet_blue", new HeadwearFit(.015F,0,0,1.040F,0,0,0));
        EXPORT_117V.put("TEEN_MALE|bandana_rag", new HeadwearFit(.010F,0,.005F,.950F,0,0,0));
        EXPORT_117V.put("TEEN_MALE|crashhelmet_stars", new HeadwearFit(0,.025F,0,1,0,0,0));
        EXPORT_117V.put("TEEN_MALE|hat_carl", new HeadwearFit(0,0,0,1.015F,0,0,0));
        EXPORT_117V.put("TEEN_FEMALE|bandana_skull", new HeadwearFit(0,0,.030F,1.045F,0,0,0));
        EXPORT_117V.put("TEEN_FEMALE|bandanagreen", new HeadwearFit(0,0,.040F,1.030F,0,0,0));
        EXPORT_117V.put("TEEN_FEMALE|bandanared", new HeadwearFit(-.010F,-.005F,0,1.040F,0,0,0));
        EXPORT_117V.put("TEEN_FEMALE|bandanawhite", new HeadwearFit(0,0,.045F,1.030F,0,0,0));
        EXPORT_117V.put("TEEN_FEMALE|firemanhat_white", new HeadwearFit(0,0,0,1.125F,0,0,0));
        EXPORT_117V.put("CHILD|cea_bandage_left", new HeadwearFit(.002F,0,.002F,1.220F,-18,0,0));
        EXPORT_117V.put("CHILD|cea_bandage_right", new HeadwearFit(.001F,.002F,-.002F,1.270F,12,0,-2));
        EXPORT_117V.put("CHILD|cea_leather_left", new HeadwearFit(.004F,.001F,0,1.220F,-12,0,0));
        EXPORT_117V.put("CHILD|cea_leather_right", new HeadwearFit(.006F,0,0,1.200F,6,0,0));
        EXPORT_117V.put("TEEN_MALE|cea_bandage_left", new HeadwearFit(0,-.005F,0,.997F,-45,-2,-2));
        EXPORT_117V.put("TEEN_MALE|cea_bandage_right", new HeadwearFit(0,-.005F,0,.997F,-5,0,-2));
        EXPORT_117V.put("TEEN_MALE|cea_leather_left", new HeadwearFit(-.001F,-.002F,0,.993F,-18,0,0));
        EXPORT_117V.put("TEEN_MALE|cea_leather_right", new HeadwearFit(-.007F,-.001F,0,1.009F,0,0,-4));
        EXPORT_117V.put("TEEN_FEMALE|f_eyepatchleft.x", new HeadwearFit(.006F,0,0,1.240F,0,0,0));
        EXPORT_117V.put("TEEN_FEMALE|f_eyepatchright.x", new HeadwearFit(.009F,.003F,0,1.750F,0,0,0));
    }

    private static final ThreadLocal<zombie.core.skinnedmodel.visual.ItemVisuals> CARL_WORN_VISUALS =
        ThreadLocal.withInitial(zombie.core.skinnedmodel.visual.ItemVisuals::new);

    private static boolean isCarlHatVisual(Object visual) {
        String type=String.valueOf(callNoArgs(visual,"getItemType")).toLowerCase(Locale.ENGLISH);
        if(type.equals("hat_carl.hat_carl") || type.equals("hat_carl.hat_carl_child")) return true;
        Object clothing=callNoArgs(visual,"getClothingItem");
        String model=rawToken(String.valueOf(ReflectionAccess.tryGet(clothing,"maleModel","")));
        return model.equals("carlhat_adult") || model.equals("carlhat_child");
    }

    private static boolean wearingCarlHat(Object modelInstance) {
        Object current=modelInstance;
        for(int depth=0; current!=null && depth<12; depth++){
            Object character=ReflectionAccess.tryGet(current,"character",null);
            if(character==null){
                Object player=ReflectionAccess.tryGet(current,"animPlayer",null);
                character=callNoArgs(player,"getIsoGameCharacter");
            }
            if(character instanceof zombie.characters.IsoGameCharacter liveCharacter){
                // IsoPlayer.getItemVisuals() is its remote cache, not local wornItems.
                // The collecting overload is the game's correct local/remote/zombie route.
                zombie.core.skinnedmodel.visual.ItemVisuals worn=CARL_WORN_VISUALS.get();
                try{
                    liveCharacter.getItemVisuals(worn);
                    return isCarlHatVisual(worn.findHat());
                }catch(Throwable ignored){ return false; }
                finally{ worn.clear(); }
            }
            Object visuals=callNoArgs(character,"getItemVisuals");
            if(visuals==null){
                Object owner=callNoArgs(current,"getOwner");
                visuals=callNoArgs(owner,"getItemVisuals");
                if(visuals==null) visuals=ReflectionAccess.tryGet(owner,"itemVisuals",null);
            }
            if(visuals!=null){
                Object visual=callNoArgs(visuals,"findHat");
                return isCarlHatVisual(visual);
            }
            current=ReflectionAccess.tryGet(current,"parent",null);
        }
        return false;
    }

    public static void apply(Object renderData){
        if(renderData==null) return;
        Object mi=PatchChildHairTransform.publicField(renderData,"modelInstance");
        if(mi==null) return;
        int stage=stageOf(mi);
        if(stage==0) return; // ADULT/ELDER: fully vanilla and zero classification cost.

        boolean female=isMinorFemale(mi);
        boolean teenFemale=stage==2 && female;
        String name=PatchChildHairTransform.modelName(mi);
        String token=rawToken(name);
        String raw=token;
        boolean carlEye=token.equals("cea_bandage") || token.equals("cea_leather") || token.equals("cea_glasses");
        boolean splitCarlEye=carlEye && (stage==1 || stage==2 && !female);
        if(splitCarlEye){
            // Left/Right share a texture. Identify the side from the mesh, not its body slot.
            int material=name.indexOf('&');
            String mesh=rawToken(material<0 ? name : name.substring(0,material));
            if(mesh.endsWith("_left")) raw=token+"_left";
            else if(mesh.endsWith("_right")) raw=token+"_right";
        }
        String baseRaw=raw;
        boolean carlPatch=splitCarlEye && (token.equals("cea_bandage") || token.equals("cea_leather"));
        if(carlPatch && wearingCarlHat(mi)) raw="carlhat::"+raw;
        boolean isAccessory=accessoryName(name) || splitCarlEye;
        boolean isHeadwear=!isAccessory && clothingItemHeadwear(mi,name);
        if(!isHeadwear&&!isAccessory) return;
        // Baked eyewear fits compose on a neutral 1.0 accessory baseline.
        // Carl Hat has explicit Adult/Child geometries. CHILD behaviour stays exactly as before.
        // For TEEN, the Adult geometry is the tested target; if the Child variant is equipped,
        // normalize its ~1.304x geometry back to the Adult fit instead of applying the generic 0.95 baseline.
        boolean carlAdult=token.equals("carlhat_adult");
        boolean carlChild=token.equals("carlhat_child");
        boolean explicitCarlFit=carlAdult||carlChild;
        float baseScale;
        if(isAccessory){
            baseScale=1.0F;
        }else if(carlAdult){
            baseScale=1.0F;
        }else if(carlChild){
            baseScale=(stage==2)?CARL_CHILD_TO_ADULT_SCALE:1.0F;
        }else{
            // TEEN male keeps the neutral 1.00 stage baseline; TEEN female keeps
            // the measured 0.95 stage baseline. FIXED109 family deltas compose on
            // top of those baselines, matching how the in-game tuner was calibrated.
            baseScale=(stage==1)?CHILD_HEADWEAR_SCALE:(teenFemale?TEEN_HEADWEAR_SCALE:1.0F);
        }
        String itemType=itemTypeToken(mi);
        HeadwearFit baked=null;
        HeadwearFit extra=null;
        String accessoryToken=canonicalAccessoryToken(token);
        if(stage==1 && isAccessory && (accessoryToken.equals("eyepatchleft")||accessoryToken.equals("eyepatchright"))){
            baked=CHILD_FIT_EYEPATCH;
        }else if(isAccessory && eyewearName(name)){
            baked=stage==1 ? childAccessoryFit(token,itemType) : TEEN_FIT_GLASSES_ALL;
        }else if(isHeadwear && !explicitCarlFit){
            if(stage==1) baked=childHeadwearFit(token, female);
            else if(stage==2) baked=teenFemale ? teenFemaleHeadwearFit(token,itemType) : teenMaleHeadwearFit(token,itemType);
            if(stage==2 && !teenFemale){
                if(accessoryToken.startsWith("baseballcap")) extra=TEEN_MALE_FIT_BASEBALLCAP;
                else if(accessoryToken.startsWith("crashhelmet")&&!accessoryToken.startsWith("crashhelmetfull")) extra=TEEN_MALE_FIT_CRASH;
                else if(accessoryToken.startsWith("armyhelmet")||accessoryToken.startsWith("tactical_helmet")) extra=TEEN_MALE_FIT_TACTICAL;
            }
            HeadwearFit delta=stage==1
                ? (female ? null : CHILD_MALE_DELTAS.get(accessoryToken))
                : (teenFemale ? TEEN_FEMALE_DELTAS : TEEN_MALE_DELTAS).get(accessoryToken);
            if(delta!=null) extra=delta;
            // Exact replacement: all TEEN male cowboys match the calibrated reference.
            if(stage==2 && !female && accessoryToken.startsWith("cowboy")){
                baked=null;
                extra=TEEN_MALE_DELTAS.get("cowboy_cowsimmental");
            }
        }
        if(stage==2 && !female && isAccessory && carlEye)
            extra=TEEN_MALE_DELTAS.get(token.equals("cea_glasses") ? token : baseRaw);
        String exportStage=stage==1 ? (isAccessory ? "CHILD" : female ? "CHILD_FEMALE" : "CHILD_MALE")
            : female ? "TEEN_FEMALE" : "TEEN_MALE";
        HeadwearFit exported=EXPORT_117V.get(exportStage+"|"+(isAccessory ? baseRaw : accessoryToken));
        HeadwearFit exportedW=stage==1 && female && carlPatch && baseRaw.equals("cea_bandage_right")
            ? raw.startsWith("carlhat::") ? CHILD_FEMALE_CARLHAT_BANDAGE_RIGHT_117W : CHILD_FEMALE_BANDAGE_RIGHT_117W
            : null;
        HeadwearFit exportedX=stage==1 && female && carlPatch && baseRaw.equals("cea_bandage_right") && raw.startsWith("carlhat::")
            ? CHILD_FEMALE_CARLHAT_BANDAGE_RIGHT_117X
            : stage==2 && !female && !isAccessory && accessoryToken.equals("crashhelmet_stars")
                ? TEEN_MALE_CRASHHELMET_STARS_117X : null;
        float exportedYScale=stage==1 && carlPatch && baseRaw.equals("cea_bandage_right") && raw.startsWith("carlhat::")
            ? CHILD_CARLHAT_BANDAGE_RIGHT_117Y_SCALE : 1.0F;
        if(baseScale==1.0F&&baked==null&&extra==null&&exported==null&&exportedW==null&&exportedX==null&&exportedYScale==1.0F) return;
        Object xfrm=PatchChildHairTransform.publicField(renderData,"xfrm");
        if(xfrm==null) return;
        try{
            PatchChildHairTransform.invokeMatrix(xfrm,"transpose");
            if(baked!=null){
                if(baked.x!=0||baked.y!=0||baked.z!=0) PatchChildHairTransform.invokeTranslate(xfrm,baked.x,baked.y,baked.z);
                if(baked.rx!=0) xfrm.getClass().getMethod("rotateX",float.class).invoke(xfrm,(float)Math.toRadians(baked.rx));
                if(baked.ry!=0) xfrm.getClass().getMethod("rotateY",float.class).invoke(xfrm,(float)Math.toRadians(baked.ry));
                if(baked.rz!=0) xfrm.getClass().getMethod("rotateZ",float.class).invoke(xfrm,(float)Math.toRadians(baked.rz));
            }
            if(extra!=null){
                if(extra.x!=0||extra.y!=0||extra.z!=0) PatchChildHairTransform.invokeTranslate(xfrm,extra.x,extra.y,extra.z);
                if(extra.rx!=0) xfrm.getClass().getMethod("rotateX",float.class).invoke(xfrm,(float)Math.toRadians(extra.rx));
                if(extra.ry!=0) xfrm.getClass().getMethod("rotateY",float.class).invoke(xfrm,(float)Math.toRadians(extra.ry));
                if(extra.rz!=0) xfrm.getClass().getMethod("rotateZ",float.class).invoke(xfrm,(float)Math.toRadians(extra.rz));
            }
            if(exported!=null){
                if(exported.x!=0||exported.y!=0||exported.z!=0) PatchChildHairTransform.invokeTranslate(xfrm,exported.x,exported.y,exported.z);
                if(exported.rx!=0) xfrm.getClass().getMethod("rotateX",float.class).invoke(xfrm,(float)Math.toRadians(exported.rx));
                if(exported.ry!=0) xfrm.getClass().getMethod("rotateY",float.class).invoke(xfrm,(float)Math.toRadians(exported.ry));
                if(exported.rz!=0) xfrm.getClass().getMethod("rotateZ",float.class).invoke(xfrm,(float)Math.toRadians(exported.rz));
            }
            if(exportedW!=null){
                if(exportedW.x!=0||exportedW.y!=0||exportedW.z!=0) PatchChildHairTransform.invokeTranslate(xfrm,exportedW.x,exportedW.y,exportedW.z);
            }
            if(exportedX!=null){
                if(exportedX.x!=0||exportedX.y!=0||exportedX.z!=0) PatchChildHairTransform.invokeTranslate(xfrm,exportedX.x,exportedX.y,exportedX.z);
            }
            float finalScale=baseScale*(baked!=null?baked.scale:1.0F)*(extra!=null?extra.scale:1.0F)*(exported!=null?exported.scale:1.0F)*(exportedW!=null?exportedW.scale:1.0F)*(exportedX!=null?exportedX.scale:1.0F)*exportedYScale;
            if(finalScale!=1.0F) PatchChildHairTransform.invokeScale(xfrm,finalScale);
            PatchChildHairTransform.invokeMatrix(xfrm,"transpose");
        }catch(Throwable ignored){}
    }

    // FIXED101: CHILD-female model parity is handled post-construction by
    // PatchChildFemaleHeadwear, following the proven pre-FIXED99 pattern.
    // This tuner now only owns render-space fitting; it never rewrites ClothingItem model selection.

    @Patch(className="zombie.core.skinnedmodel.advancedanimation.AnimatedModel$AnimatedModelInstanceRenderData", methodName="transformToParent")
    public static final class RenderHeadwearTune{
        private RenderHeadwearTune(){}
        @OnExit public static void exit(@This Object renderData){ PatchHeadwearTuner.apply(renderData); }
    }
}
