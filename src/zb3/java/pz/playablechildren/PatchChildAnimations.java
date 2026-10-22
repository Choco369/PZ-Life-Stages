package pz.playablechildren;

import java.util.Iterator;
import java.util.List;
import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.Argument;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.Return;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;

public final class PatchChildAnimations {
   private PatchChildAnimations() {
   }

   public static boolean isChildAnimation(AnimNode var0) {
      return var0 != null && var0.animName != null && var0.animName.contains("PlayableChild");
   }

   @Patch(warmUp = true, className = "zombie.core.skinnedmodel.advancedanimation.AnimState", methodName = "getAnimNodes")
   public static final class GetAnimNodes {
      @OnExit
      public static void exit(@Argument(0) IAnimationVariableSource var0, @Return List<AnimNode> var1) {
         if (var1 != null && var1.size() == 2 && var0 instanceof IsoGameCharacter var2) {
            boolean var3 = false;

            for (AnimNode var5 : var1) {
               if (PatchChildAnimations.isChildAnimation(var5)) {
                  var3 = true;
                  break;
               }
            }

            if (var3) {
               boolean var7 = ChildIdentity.isChild(var2);
               Iterator var8 = var1.iterator();

               while (var8.hasNext()) {
                  AnimNode var6 = (AnimNode)var8.next();
                  if (PatchChildAnimations.isChildAnimation(var6) != var7) {
                     var8.remove();
                  }
               }
            }
         }
      }
   }
}
