package pz.playablechildren;

import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.Return;
import zombie.characters.IsoZombie;

public final class PatchChildZombieBiometrics {
   private PatchChildZombieBiometrics() {
   }

   @Patch(className = "zombie.VirtualZombieManager", methodName = "createRealZombieAlways")
   public static final class CreateRealZombie {
      @OnExit
      public static void exit(@Return IsoZombie var0) {
         if (ChildIdentity.isZombieChild(var0)) {
            var0.setHealth(ChildZombiePolicy.adjustHealth(var0.getHealth()));
            var0.setWeight(ChildZombiePolicy.adjustWeight(var0.getWeight()));
         }
      }
   }
}
