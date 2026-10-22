package pz.playablechildren;
import java.util.*;
import zombie.characters.IsoZombie;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.visual.*;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
public final class ClothingVisuals {
 private ClothingVisuals(){}
 private static final ThreadLocal<Set<ItemVisuals>> FILTERING=ThreadLocal.withInitial(()->Collections.newSetFromMap(new IdentityHashMap<>()));
 private record Cover(ItemVisual damage){}
 private static final Map<HumanVisual,Cover> NEED_COVER=Collections.synchronizedMap(new WeakHashMap<>());
 public static void requestCover(HumanVisual owner,ItemVisual old){
  if(owner==null)return;ItemVisual damage=null;
  if(old!=null){damage=new ItemVisual();damage.copyBlood(old);damage.copyDirt(old);damage.copyHoles(old);}
  synchronized(NEED_COVER){if(damage!=null||!NEED_COVER.containsKey(owner))NEED_COVER.put(owner,new Cover(damage));}
 }
 public static Item script(Object definition){
  if(definition instanceof Item item)return item;
  if(definition instanceof ClothingItem asset){
   Item found=asset.mame==null?null:ScriptManager.instance.getItemForClothingItem(asset.mame);
   if(found!=null)return found;
   for(Item item:ScriptManager.instance.getAllItems())if(item.getClothingItemAsset()==asset)return item;
  }
  if(definition instanceof String name)return ScriptManager.instance.getItemForClothingItem(name);
  return null;
 }
 public static boolean blocks(HumanVisual owner,Object definition){return owner!=null&&(ClothingRuntime.blocksScript(owner,script(definition))||(definition instanceof ClothingItem asset&&AppearanceTextureRestrictions.blocks(owner,asset)));}
 public static boolean denyAdd(HumanVisual owner,Object definition){
  boolean deny=blocks(owner,definition);
  if(deny&&ChildIdentity.ownerOf(owner) instanceof IsoZombie)requestCover(owner,null);
  return deny;
 }
 private static Item script(ItemVisual visual){
  Item item=visual.getScriptItem();
  return item!=null?item:script(visual.getClothingItemName());
 }
 public static boolean filter(HumanVisual owner,ItemVisuals visuals){
  if(owner==null||visuals==null)return false;
  LifeStage stage=LifeStageResolver.stageOf(owner);if(stage!=LifeStage.CHILD&&stage!=LifeStage.TEEN){NEED_COVER.remove(owner);return false;}
  Set<ItemVisuals> active=FILTERING.get();if(!active.add(visuals))return false;
  try{
   boolean changed=false;ItemVisual removed=null;
   for(int i=visuals.size()-1;i>=0;i--){ItemVisual visual=visuals.get(i);if(visual!=null&&(ClothingRuntime.blocksScript(owner,script(visual))||ClothingRuntime.blocksType(owner,visual.getItemType()))){removed=visual;visuals.remove(i);changed=true;}}
   if(ChildIdentity.ownerOf(owner) instanceof IsoZombie&&visuals!=ReflectionAccess.tryGet(owner,"bodyVisuals",null)){
    Cover pending=NEED_COVER.get(owner);
    if(changed||pending!=null){int before=visuals.size();cover(owner,visuals,removed!=null?removed:pending==null?null:pending.damage);changed|=before!=visuals.size();}
   }
   return changed;
  }finally{active.remove(visuals);if(active.isEmpty())FILTERING.remove();}
 }
 private static void cover(HumanVisual owner,ItemVisuals visuals,ItemVisual old){
  boolean top=false,bottom=false;
  for(ItemVisual v:visuals){Item item=v==null?null:script(v);if(item==null||item.getBodyLocation()==null)continue;
   String slot=item.getBodyLocation().toString().toLowerCase(Locale.ROOT);
   boolean whole=slot.contains("dress")||slot.contains("fullsuit")||slot.contains("full_suit")||slot.contains("boilersuit")||slot.contains("torso1legs1");
   top|=whole||slot.contains("shirt")||slot.contains("tanktop")||slot.contains("sweater")||slot.contains("jacket");
   bottom|=whole||slot.contains("pants")||slot.contains("skirt");
  }
  if(!top)addFallback(owner,visuals,"Base.Tshirt_DefaultTEXTURE_TINT",old);
  if(!bottom)addFallback(owner,visuals,"Base.Trousers_Denim",old);
 }
 private static void addFallback(HumanVisual owner,ItemVisuals visuals,String type,ItemVisual old){
  Item script=ScriptManager.instance.getItem(type);if(script==null||blocks(owner,script))return;
  ItemVisual visual=new ItemVisual();visual.setItemType(type);
  if(old!=null){visual.copyBlood(old);visual.copyDirt(old);visual.copyHoles(old);}
  visuals.add(visual);
 }
 public static boolean reconcileZombie(IsoZombie zombie){return zombie!=null&&filter(zombie.getHumanVisual(),zombie.getItemVisuals());}
}
