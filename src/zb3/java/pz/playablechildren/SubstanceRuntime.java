package pz.playablechildren;
import java.util.*;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Food;
import zombie.characters.traits.CharacterTraits;
import zombie.scripting.objects.CharacterTrait;
import java.lang.ref.WeakReference;
public final class SubstanceRuntime {
 private SubstanceRuntime(){}
 private static final Map<CharacterTraits,WeakReference<IsoGameCharacter>> TRAIT_OWNERS=Collections.synchronizedMap(new WeakHashMap<>());
 private static final ThreadLocal<Map<CharacterTraits,Integer>> RESTORING=ThreadLocal.withInitial(IdentityHashMap::new);
 public static void rememberTraits(IsoGameCharacter owner,CharacterTraits traits){if(owner!=null&&traits!=null)TRAIT_OWNERS.put(traits,new WeakReference<>(owner));}
 public static void beginRestore(CharacterTraits traits){RESTORING.get().merge(traits,1,Integer::sum);}
 public static void endRestore(CharacterTraits traits){var restoring=RESTORING.get();int left=restoring.getOrDefault(traits,1)-1;if(left<=0)restoring.remove(traits);else restoring.put(traits,left);if(restoring.isEmpty())RESTORING.remove();}
 public static boolean blocksAcquisition(CharacterTraits traits,CharacterTrait trait,boolean value){
  if(!value||traits==null||trait==null||RESTORING.get().containsKey(traits)||traits.get(trait))return false;
  WeakReference<IsoGameCharacter> owner=TRAIT_OWNERS.get(traits);
  return owner!=null && blocksTrait(owner.get(),trait.toString());
 }
 public static Set<String> activeMods(){
  List<String> ids=ZomboidFileSystem.instance.getModIDs();
  return ids==null?Set.of():new HashSet<>(ids);
 }
 public static SubstanceCatalog.Kind kind(InventoryItem item){
  if(item==null)return SubstanceCatalog.Kind.NONE;
  Set<String> mods=activeMods();
  SubstanceCatalog.Kind kind=SubstanceCatalog.kind(item.getFullType(),item instanceof Food f?f.getOnEat():null,mods);
  if(kind==SubstanceCatalog.Kind.NARCOTIC)return kind;
  if(item.hasModData()){
   Object marker=item.getModData().rawget("PZLifeStagesSubstance");
   if("NARCOTIC".equals(marker))return SubstanceCatalog.Kind.NARCOTIC;
   if("CANNABIS".equals(marker))kind=SubstanceCatalog.Kind.CANNABIS;
  }
  List<String> ingredients=item.getExtraItems();
  List<String> all=new ArrayList<>();if(ingredients!=null)all.addAll(ingredients);
  if(item instanceof Food f && f.getSpices()!=null)all.addAll(f.getSpices());
  for(String id:all){
   SubstanceCatalog.Kind ingredient=SubstanceCatalog.kind(id,null,mods);
   if(ingredient==SubstanceCatalog.Kind.NARCOTIC)return ingredient;
   if(ingredient==SubstanceCatalog.Kind.CANNABIS)kind=ingredient;
  }
  return kind;
 }
 public static void inheritFood(InventoryItem result,InventoryItem source){
  if(!(result instanceof Food)||source==null)return;
  SubstanceCatalog.Kind k=kind(source);
  if(k==SubstanceCatalog.Kind.NONE)return;
  if(kind(result)==SubstanceCatalog.Kind.NARCOTIC)k=SubstanceCatalog.Kind.NARCOTIC;
  result.getModData().rawset("PZLifeStagesSubstance",k.name());
 }
 public static void markPreparedFood(List<InventoryItem> inputs,List<InventoryItem> outputs){
  if(inputs==null||outputs==null)return;
  for(InventoryItem output:outputs)if(output instanceof Food)for(InventoryItem input:inputs)inheritFood(output,input);
 }
 public static boolean blocksConsumption(IsoGameCharacter character,InventoryItem item){
  if(character==null)return false;
  LifeStage stage=LifeStageResolver.stageOf(character);
  return (stage==LifeStage.CHILD || stage==LifeStage.TEEN) && SubstancePolicy.blocked(stage,kind(item));
 }
 public static boolean blocksRecipe(IsoGameCharacter character,String fullRecipeId){
  if(character==null)return false;
  LifeStage stage=LifeStageResolver.stageOf(character);
  return (stage==LifeStage.CHILD || stage==LifeStage.TEEN) && SubstanceCatalog.blockedRecipe(fullRecipeId,activeMods());
 }
 public static boolean blocksTrait(IsoGameCharacter character,String traitId){
  return character!=null && SubstancePolicy.blockedTrait(LifeStageResolver.stageOf(character),traitId);
 }
}
