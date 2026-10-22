package pz.playablechildren;
import me.zed_0xff.zombie_buddy.Exposer;
import zombie.characters.IsoPlayer;
/** The lifecycle is authoritative at explicit initialization/transition points. */
@Exposer.LuaClass(name="PlayableChildren.Nutrition")
public final class NutritionApi {
 private NutritionApi(){}
 public static boolean synchronize(Object value,String stageName){
  LifeStage stage=NutritionState.parse(stageName);
  return value instanceof IsoPlayer p && stage!=null && NutritionState.prepare(p,stage);
 }
}
