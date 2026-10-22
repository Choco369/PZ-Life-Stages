package pz.playablechildren;

public final class ChildZombiePolicy {
   public static final float HEALTH_MULTIPLIER = 0.75F;
   public static final float MAX_HEALTH = 1.15F;
   public static final float WEIGHT_MULTIPLIER = 0.5F;

   private ChildZombiePolicy() {
   }

   public static float adjustHealth(float var0) {
      return Math.min(var0 * 0.75F, 1.15F);
   }

   public static float adjustWeight(float var0) {
      return var0 * 0.5F;
   }

   public static String voiceFor(boolean var0, String var1) {
      return var0 && var1 != null ? var1.replace("MaleZombie", "FemaleZombie") : var1;
   }

   public static boolean canHaveBuildingKey(boolean var0, boolean var1) {
      return var0 ? false : var1;
   }

   public static boolean canRemoveCorpseItem(boolean var0, String var1, String var2) {
      return !var0 ? true : !"Clothing".equals(var1) && !ChildPolicy.isChildOutfitName(var2);
   }
}
