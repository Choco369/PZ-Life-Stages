package pz.playablechildren;

import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.Return;
import me.zed_0xff.zombie_buddy.annotations.Patch.This;
import zombie.core.skinnedmodel.visual.HumanVisual;

public final class PatchChildBeard {
   private PatchChildBeard() {
   }

   public static boolean mustBeBeardless(HumanVisual var0) {
      return var0 != null && !var0.isFemale() && ChildIdentity.isChildVisual(var0);
   }

   public static void clearStoredBeard(HumanVisual var0) {
      if (var0 != null) {
         ReflectionAccess.trySet(var0, "beardModel", "");
      }
   }

   @Patch(className = "zombie.core.skinnedmodel.visual.HumanVisual", methodName = "getBeardModel")
   public static final class GetBeardModel {
      @OnExit
      public static void exit(@This HumanVisual var0, @Return(readOnly = false) String var1) {
         if (PatchChildBeard.mustBeBeardless(var0)) {
            PatchChildBeard.clearStoredBeard(var0);
            var1 = "";
         }
      }
   }

   @Patch(className = "zombie.core.skinnedmodel.visual.HumanVisual", methodName = "setBeardModel")
   public static final class SetBeardModel {
      @OnExit
      public static void exit(@This HumanVisual var0) {
         if (PatchChildBeard.mustBeBeardless(var0)) {
            PatchChildBeard.clearStoredBeard(var0);
         }
      }
   }
}
