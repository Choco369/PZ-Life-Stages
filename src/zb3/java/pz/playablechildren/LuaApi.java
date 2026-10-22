package pz.playablechildren;

import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorDesc;
import zombie.iso.objects.IsoDeadBody;

public final class LuaApi {
   private LuaApi() {
   }

   @LuaMethod(name = "canPlayableChildEnterVehicle", global = true)
   public static boolean canPlayableChildEnterVehicle(Object var0) {
      return var0 instanceof IsoGameCharacter var1 ? ChildPolicy.canEnterVehicle(ChildIdentity.isChild(var1)) : true;
   }

   @LuaMethod(name = "isPlayableChildDescriptor", global = true)
   public static boolean isPlayableChildDescriptor(Object var0) {
      return var0 instanceof SurvivorDesc var1 && ChildIdentity.isChild(var1);
   }

   @LuaMethod(name = "isPlayableChildCharacter", global = true)
   public static boolean isPlayableChildCharacter(Object var0) {
      return var0 instanceof IsoGameCharacter var1 && ChildIdentity.isChild(var1);
   }

   @LuaMethod(name = "isChildZombieDeadBody", global = true)
   public static boolean isChildZombieDeadBody(Object var0) {
      return var0 instanceof IsoDeadBody var1 && ChildIdentity.isZombieChild(var1);
   }

   @LuaMethod(name = "ensurePlayableChildrenWakeEvent", global = true)
   public static boolean ensurePlayableChildrenWakeEvent() {
      zombie.Lua.LuaEventManager.AddEvent("OnPlayableChildrenWakeUp");
      return true;
   }

   @LuaMethod(name = "setPlayableChildrenLifeStage", global = true)
   public static boolean setPlayableChildrenLifeStage(Object value, String stageName) {
      if (!(value instanceof IsoPlayer player) || stageName == null) return false;
      try {
         LifeStage stage = LifeStage.valueOf(stageName.trim().toUpperCase(java.util.Locale.ROOT));
         PlayerLifeStages.remember(player, stage);
         return true;
      } catch (IllegalArgumentException ignored) {
         return false;
      }
   }

   @LuaMethod(name = "setPlayableChildrenAgeContext", global = true)
   public static boolean setPlayableChildrenAgeContext(Object value, String stageName, double progress) {
      return AgeGameplayApi.setContext(value, stageName, progress);
   }

   @LuaMethod(name = "getPlayableChildrenAgeFactor", global = true)
   public static float getPlayableChildrenAgeFactor(Object value, String effectName) {
      if (!(value instanceof IsoGameCharacter character) || effectName == null) return 1.0f;
      try {
         AgeProfile.Effect effect = AgeProfile.Effect.valueOf(
            effectName.trim().toUpperCase(java.util.Locale.ROOT));
         return AgeGameplayApi.factor(character, effect);
      } catch (IllegalArgumentException ignored) {
         return 1.0f;
      }
   }

   @LuaMethod(name = "killPlayableChildrenNaturally", global = true)
   public static boolean killPlayableChildrenNaturally(Object value) {
      if (!(value instanceof IsoPlayer player)) return false;
      if (LifeStageResolver.stageOf(player) != LifeStage.ELDER || player.isDead()) return false;
      player.Kill(null, false);
      return true;
   }
}
