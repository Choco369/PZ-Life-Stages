package pz.playablechildren;
import me.zed_0xff.zombie_buddy.Exposer;
import zombie.characters.IsoGameCharacter;
import zombie.inventory.InventoryItem;
@Exposer.LuaClass(name="PlayableChildren.Substances")
public final class SubstanceApi {
 private SubstanceApi(){}
 public static boolean canConsume(Object character,Object item){
  return !(character instanceof IsoGameCharacter p && item instanceof InventoryItem i && SubstanceRuntime.blocksConsumption(p,i));
 }
 public static boolean canUseRecipe(Object character,String fullId){
  return !(character instanceof IsoGameCharacter p && SubstanceRuntime.blocksRecipe(p,fullId));
 }
 public static boolean blocksTrait(String stage,String traitId){
  if(stage==null)return false;
  try{return SubstancePolicy.blockedTrait(LifeStage.valueOf(stage),traitId);}catch(IllegalArgumentException e){return false;}
 }
 public static boolean isCannabis(Object item){return item instanceof InventoryItem i && SubstanceRuntime.kind(i)==SubstanceCatalog.Kind.CANNABIS;}
 public static void markPreparedFood(java.util.ArrayList<InventoryItem> inputs,java.util.ArrayList<InventoryItem> outputs){SubstanceRuntime.markPreparedFood(inputs,outputs);}
}
