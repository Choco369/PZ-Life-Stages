package pz.playablechildren;

import java.util.Locale;

/** Constants and persistent marker for the adolescent stage. */
public final class TeenPolicy {
    public static final String PROFESSION_ID = "PlayableChildren:Teen";
    public static final String PROFESSION_CANONICAL_ID = "playablechildren:teen";
    public static final float TEEN_SCALE = 0.89F;
    // Separate high bit from CHILD (0x40000000). B42.20.4 uses the low 16 bits
    // for ReanimatedPlayer indexing, so the teen marker deliberately stays high.
    public static final int TEEN_PERSISTENT_OUTFIT_BIT = StageBits.TEEN;

    private TeenPolicy() {}

    public static boolean isTeenProfessionName(String value) {
        return value != null && PROFESSION_CANONICAL_ID.equals(value.toLowerCase(Locale.ENGLISH));
    }
    public static boolean isTeenOutfitName(String value) {
        if (value == null) return false;
        String s = value.toLowerCase(Locale.ENGLISH);
        return s.equals("teenzombie01")
            || s.equals("teenzombie_shellsuit")
            || s.equals("teenzombie_hospitalpatient");
    }
    public static boolean hasTeenOutfitBit(int id) { return StageBits.isTeen(id); }
}
