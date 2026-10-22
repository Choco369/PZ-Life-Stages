package pz.playablechildren;
import java.util.*;
import java.lang.ref.WeakReference;
import zombie.characters.IsoGameCharacter;
import zombie.characters.SurvivorDesc;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.characters.WornItems.WornItems;
import zombie.inventory.InventoryItem;
public final class ClothingRuntime {
 private ClothingRuntime(){}
 private static final Map<WornItems,WeakReference<Object>> OWNERS=Collections.synchronizedMap(new WeakHashMap<>());
 private static final ThreadLocal<Set<IsoGameCharacter>> RECONCILING=ThreadLocal.withInitial(()->Collections.newSetFromMap(new IdentityHashMap<>()));
 public static boolean minor(IsoGameCharacter p){if(p==null)return false;LifeStage s=LifeStageResolver.stageOf(p);return s==LifeStage.CHILD||s==LifeStage.TEEN;}
 public static LifeStage stage(Object owner){
  if(owner instanceof IsoGameCharacter c)return LifeStageResolver.stageOf(c);
  if(owner instanceof SurvivorDesc d)return LifeStageResolver.stageOf(d);
  if(owner instanceof HumanVisual v)return LifeStageResolver.stageOf(v);
  return LifeStage.ADULT;
 }
 public static boolean female(Object owner){
  if(owner instanceof IsoGameCharacter c)return c.isFemale();
  if(owner instanceof SurvivorDesc d)return d.isFemale();
  return owner instanceof HumanVisual v&&v.isFemale();
 }
 public static boolean blocksType(Object owner,String fullType){return ClothingPolicy.blocksAppearance(stage(owner),female(owner),fullType);}
 public static boolean blocksScript(Object owner,zombie.scripting.objects.Item script){return script!=null&&(ClothingPolicy.blocks(stage(owner),ClothingOrigins.owner(script))||blocksType(owner,script.getFullName())||AppearanceTextureRestrictions.blocks(owner,script.getClothingItemAsset()));}
 public static boolean blocksWear(SurvivorDesc desc,InventoryItem item){return desc!=null&&item!=null&&blocksScript(desc,item.getScriptItem());}
 public static boolean blocksWear(IsoGameCharacter p,InventoryItem item){return p!=null&&item!=null&&blocksScript(p,item.getScriptItem());}
 public static void remember(IsoGameCharacter p,WornItems worn){if(p!=null&&worn!=null)OWNERS.put(worn,new WeakReference<>(p));}
 public static void remember(SurvivorDesc d,WornItems worn){if(d!=null&&worn!=null)OWNERS.put(worn,new WeakReference<>(d));}
 public static boolean blocksWear(WornItems worn,InventoryItem item){var ref=OWNERS.get(worn);return ref!=null&&item!=null&&blocksScript(ref.get(),item.getScriptItem());}
 public static boolean reconcile(SurvivorDesc desc,WornItems worn){
  if(desc==null||worn==null)return false;remember(desc,worn);boolean changed=false;
  var items=worn.getItems();
  for(int i=items.size()-1;i>=0;i=Math.min(i-1,items.size()-1)){var entry=items.get(i);if(blocksWear(desc,entry.getItem())){worn.setItem(entry.getLocation(),null);changed=true;}}
  return changed;
 }
 public static boolean reconcile(IsoGameCharacter p){
  if(!minor(p)||!RECONCILING.get().add(p))return false;
  boolean changed=false;
  try{
   WornItems worn=p.getWornItems();if(worn==null)return false;remember(p,worn);
   var items=worn.getItems();
   for(int i=items.size()-1;i>=0;i=Math.min(i-1,items.size()-1)){
    var entry=items.get(i);
    InventoryItem item=entry.getItem();if(item==null)continue;
    if(item.getModData().rawget("HCS_Hidden")!=null){item.getModData().rawset("HCS_Hidden",null);changed=true;}
    if(!blocksWear(p,item))continue;
    if(p instanceof zombie.characters.IsoZombie zombie)ClothingVisuals.requestCover(zombie.getHumanVisual(),(zombie.core.skinnedmodel.visual.ItemVisual)ReflectionAccess.tryGet(item,"visual",null));
    p.setWornItem(entry.getLocation(),null,false);
    if(item.getContainer()==null&&p.getInventory()!=null&&!p.getInventory().contains(item,true))p.getInventory().AddItem(item);
    changed=true;
   }
   if(changed)p.resetModelNextFrame();
   return changed;
  }finally{Set<IsoGameCharacter> active=RECONCILING.get();active.remove(p);if(active.isEmpty())RECONCILING.remove();}
 }
}
