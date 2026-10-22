package pz.playablechildren;

public final class ChildPolicy {
   public static final String PROFESSION_ID = "PlayableChildren:Child";
   public static final String PROFESSION_CANONICAL_ID = "playablechildren:child";
   public static final String CHILD_OUTFIT_PREFIX = "ChildZombie";
   public static final float CHILD_SCALE = 0.72F;
   public static final int CHILD_PERSISTENT_OUTFIT_BIT = StageBits.CHILD;
   public static final int STAGE_PERSISTENT_OUTFIT_MASK = StageBits.MASK;

   private ChildPolicy() {
   }

   public static boolean isChildProfessionName(String var0) {
      return var0 != null && "playablechildren:child".equals(var0.toLowerCase());
   }

   public static boolean hasChildOutfitBit(int var0) {
      return StageBits.isChild(var0);
   }

   public static boolean isChildOutfitName(String var0) {
      return var0 != null && var0.startsWith(CHILD_OUTFIT_PREFIX);
   }

   public static float scaleFor(boolean var0) {
      return var0 ? 0.72F : 1.0F;
   }

   public static boolean canEnterVehicle(boolean var0) {
      // Seating is allowed for every age. Driver eligibility is checked by DrivingPolicy.
      return true;
   }


}
