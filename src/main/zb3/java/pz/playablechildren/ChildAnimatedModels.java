package pz.playablechildren;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;

public final class ChildAnimatedModels {
   private static final Map<AnimatedModel, Boolean> CHILD_MODELS = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<AnimatedModel, Boolean> PLAYABLE_MODELS = Collections.synchronizedMap(new WeakHashMap<>());

   private ChildAnimatedModels() {
   }

   public static boolean shouldScaleFromContext(boolean var0, String var1, String var2) {
      return var0 || "onground".equals(var2) && ChildPolicy.isChildOutfitName(var1);
   }

   public static void updateFrom(AnimatedModel var0, BaseVisual var1) {
      HumanVisual var2 = var1 instanceof HumanVisual ? (HumanVisual)var1 : null;
      boolean var3 = var2 != null && ChildIdentity.isChildVisual(var2);
      String var4 = null;
      if (var2 != null) {
         Outfit var5 = var2.getOutfit();
         var4 = var5 == null ? null : var5.getName();
      }

      boolean var6 = shouldScaleFromContext(var3, var4, var0.getState());
      LifeStage stage = var2 == null ? LifeStage.ADULT : LifeStageResolver.stageOf(var2);
      if (stage == LifeStage.ADULT && var6) stage = LifeStage.CHILD;
      AnimatedModelStages.remember(var0, stage);
      setChild(var0, stage == LifeStage.CHILD);
      setPlayableChild(var0, var6 && var2 != null && ChildIdentity.isPlayableChildVisual(var2));
   }

   public static void setChild(AnimatedModel var0, boolean var1) {
      if (var1) {
         CHILD_MODELS.put(var0, Boolean.TRUE);
      } else {
         CHILD_MODELS.remove(var0);
      }
   }

   public static boolean isChild(AnimatedModel var0) {
      return var0 != null && Boolean.TRUE.equals(CHILD_MODELS.get(var0));
   }

   public static void setPlayableChild(AnimatedModel var0, boolean var1) {
      if (var1) {
         PLAYABLE_MODELS.put(var0, Boolean.TRUE);
      } else {
         PLAYABLE_MODELS.remove(var0);
      }
   }

   public static boolean isPlayableChild(AnimatedModel var0) {
      return var0 != null && Boolean.TRUE.equals(PLAYABLE_MODELS.get(var0));
   }
}
