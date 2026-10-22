package pz.playablechildren;
import me.zed_0xff.zombie_buddy.Exposer;
import zombie.characters.IsoGameCharacter;
import zombie.inventory.InventoryItem;
@Exposer.LuaClass(name="PlayableChildren.Clothing")
public final class ClothingApi {
 private ClothingApi(){}
 public static boolean canWear(Object p,Object item){
  zombie.scripting.objects.Item script=item instanceof InventoryItem i?i.getScriptItem():item instanceof zombie.scripting.objects.Item s?s:null;
  return !ClothingRuntime.blocksScript(p,script);
 }
 public static boolean canUseAppearance(Object p,String fullType){return !ClothingRuntime.blocksType(p,fullType);}
 public static boolean bindAppearancePreview(Object preview,Object source){
  if(!(preview instanceof zombie.characters.SurvivorDesc desc)||source==null)return false;
  desc.setFemale(ClothingRuntime.female(source));PlayerLifeStages.remember(desc,ClothingRuntime.stage(source));return true;
 }
 public static boolean isMinor(Object p){LifeStage s=ClothingRuntime.stage(p);return s==LifeStage.CHILD||s==LifeStage.TEEN;}
 public static boolean reconcile(Object p){return p instanceof IsoGameCharacter c&&ClothingRuntime.reconcile(c);}
}
