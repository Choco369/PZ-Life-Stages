package pz.playablechildren;

import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.Return;
import me.zed_0xff.zombie_buddy.annotations.Patch.This;
import zombie.characters.IsoZombie;

public final class PatchChildZombieVoice {
   private PatchChildZombieVoice() {
   }

   @Patch(warmUp = true, className = "zombie.characters.IsoZombie", methodName = "getVoiceSoundName")
   public static final class GetVoiceSoundName {
      @OnExit
      public static void exit(@Return(readOnly = false) String var0, @This IsoZombie var1) {
         var0 = ChildZombiePolicy.voiceFor(ChildIdentity.isZombieChild(var1), var0);
      }
   }
}
