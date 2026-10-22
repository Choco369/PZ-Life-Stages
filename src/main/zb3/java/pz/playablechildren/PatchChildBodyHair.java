package pz.playablechildren;
import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnEnter;
import me.zed_0xff.zombie_buddy.annotations.Patch.This;
import me.zed_0xff.zombie_buddy.annotations.Patch.Return;
import zombie.core.skinnedmodel.visual.HumanVisual;
public final class PatchChildBodyHair {
 private PatchChildBodyHair(){}
 public static boolean blocked(HumanVisual visual){return visual!=null&&LifeStageResolver.stageOf(visual)==LifeStage.CHILD;}
 @Patch(className="zombie.core.skinnedmodel.visual.HumanVisual",methodName="getBodyHairIndex")
 public static final class Read {
  @OnExit public static void exit(@This HumanVisual visual,@Return(readOnly=false) int result){if(blocked(visual))result=-1;}
 }
 @Patch(className="zombie.core.skinnedmodel.visual.HumanVisual",methodName="setBodyHairIndex")
 public static final class Write {
  @OnExit public static void exit(@This HumanVisual visual){if(blocked(visual))ReflectionAccess.trySet(visual,"bodyHair",-1);}
 }
 @Patch(className="zombie.core.skinnedmodel.visual.HumanVisual",methodName="getSkinTexture")
 public static final class Texture {
  @OnEnter public static void enter(@This HumanVisual visual){if(blocked(visual))ReflectionAccess.trySet(visual,"bodyHair",-1);}
 }
}
