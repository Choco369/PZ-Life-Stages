package pz.playablechildren;
import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.*;
import zombie.characters.IsoGameCharacter;
import zombie.characters.WornItems.WornItems;
import zombie.inventory.InventoryItem;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.characters.IsoPlayer;
import zombie.iso.objects.IsoDeadBody;
import zombie.core.skinnedmodel.visual.BaseVisual;
/** Native advice delegates policy; keeps ordinary adult calls unchanged. */
public final class PatchClothingRestrictions {
 private PatchClothingRestrictions(){}
 @Patch(className="zombie.characters.IsoGameCharacter",methodName="setWornItem",strictMatch=true)
 public static final class Wear {
  @OnEnter(skipOn=true) public static boolean enter(@This IsoGameCharacter p,@Argument(1) InventoryItem item){return ClothingRuntime.blocksWear(p,item);}
 }
 @Patch(className="zombie.characters.IsoGameCharacter",methodName="getWornItems",strictMatch=false)
 public static final class Owner {
  @OnExit public static void exit(@This IsoGameCharacter p,@Return WornItems worn){ClothingRuntime.remember(p,worn);ClothingRuntime.reconcile(p);}
 }
 @Patch(className="zombie.characters.IsoGameCharacter",methodName="setWornItems",strictMatch=true)
 public static final class Bulk {
  @OnExit public static void exit(@This IsoGameCharacter p,@Argument(0) WornItems loaded){ClothingRuntime.reconcile(p);}
 }
 @Patch(className="zombie.characters.WornItems.WornItems",methodName="setItem",strictMatch=true)
 public static final class Direct {
  @OnEnter(skipOn=true) public static boolean enter(@This WornItems worn,@Argument(1) InventoryItem item){return ClothingRuntime.blocksWear(worn,item);}
 }
 @Patch(className="zombie.characters.SurvivorDesc",methodName="setWornItem",strictMatch=true)
 public static final class CreatorWear {
  @OnEnter(skipOn=true) public static boolean enter(@This SurvivorDesc desc,@Argument(1) InventoryItem item){return ClothingRuntime.blocksWear(desc,item);}
 }
 @Patch(className="zombie.characters.SurvivorDesc",methodName="getWornItems",strictMatch=false)
 public static final class CreatorOwner {
  @OnExit public static void exit(@This SurvivorDesc desc,@Return WornItems worn){ClothingRuntime.reconcile(desc,worn);}
 }
 @Patch(className="zombie.core.skinnedmodel.visual.HumanVisual",methodName="addClothingItem",strictMatch=true)
 public static final class AddVisual {
  @OnEnter(skipOn=true) public static boolean enter(@This HumanVisual owner,@Argument(1) Object definition){return ClothingVisuals.denyAdd(owner,definition);}
 }
 @Patch(className="zombie.core.skinnedmodel.visual.HumanVisual",methodName="getBodyVisuals",strictMatch=false)
 public static final class BodyVisuals {
  @OnExit public static void exit(@This HumanVisual owner,@Return ItemVisuals visuals){ClothingVisuals.filter(owner,visuals);}
 }
 @Patch(className="zombie.core.skinnedmodel.visual.HumanVisual",methodName="addBodyVisualFromItemType",strictMatch=true)
 public static final class BodyItem {
  @OnEnter(skipOn=true) public static boolean enter(@This HumanVisual owner,@Argument(0) String type){return ClothingVisuals.blocks(owner,zombie.scripting.ScriptManager.instance.getItem(type));}
 }
 @Patch(className="zombie.core.skinnedmodel.visual.HumanVisual",methodName="addBodyVisualFromClothingItemName",strictMatch=true)
 public static final class BodyAsset {
  @OnEnter(skipOn=true) public static boolean enter(@This HumanVisual owner,@Argument(0) String name){return ClothingVisuals.blocks(owner,name);}
 }
 @Patch(className="zombie.characters.IsoZombie",methodName="getItemVisuals",strictMatch=false)
 public static final class ZombieVisualReturn {
  @OnExit public static void exit(@This IsoZombie zombie,@Field(value="itemVisuals",readOnly=true) final ItemVisuals visuals,@AllArguments Object[] arguments){if(arguments.length==0)ClothingVisuals.filter(zombie.getHumanVisual(),visuals);}
 }
 @Patch(className="zombie.characters.IsoZombie",methodName="getItemVisuals",strictMatch=true)
 public static final class ZombieVisualOutput {
  @OnExit public static void exit(@This IsoZombie zombie,@Argument(0) ItemVisuals visuals){ClothingVisuals.filter(zombie.getHumanVisual(),visuals);}
 }
 @Patch(className="zombie.iso.objects.IsoDeadBody",methodName="getItemVisuals",strictMatch=true)
 public static final class CorpseVisualOutput {
  @OnExit public static void exit(@This IsoDeadBody body,@Argument(0) ItemVisuals visuals){ClothingVisuals.filter(body.getHumanVisual(),visuals);}
 }
 @Patch(className="zombie.characters.SurvivorDesc",methodName="getItemVisuals",strictMatch=true)
 public static final class CreatorVisualOutput {
  @OnExit public static void exit(@This SurvivorDesc desc,@Argument(0) ItemVisuals visuals){ClothingVisuals.filter(desc.getHumanVisual(),visuals);}
 }
 @Patch(className="zombie.characters.IsoPlayer",methodName="getItemVisuals",strictMatch=false)
 public static final class PlayerVisualReturn {
  @OnExit public static void exit(@This IsoPlayer player,@Field(value="remotePlayerItemVisuals",readOnly=true) final ItemVisuals visuals,@AllArguments Object[] arguments){if(arguments.length==0)ClothingVisuals.filter(player.getHumanVisual(),visuals);}
 }
 @Patch(className="zombie.characters.IsoPlayer",methodName="getItemVisuals",strictMatch=true)
 public static final class PlayerVisualOutput {
  @OnExit public static void exit(@This IsoPlayer player,@Argument(0) ItemVisuals visuals){ClothingVisuals.filter(player.getHumanVisual(),visuals);}
 }
 @Patch(className="zombie.core.skinnedmodel.advancedanimation.AnimatedModel",methodName="setModelData",strictMatch=true)
 public static final class PreviewVisualInput {
  @OnEnter public static void enter(@Argument(0) BaseVisual owner,@Argument(1) ItemVisuals visuals){if(owner instanceof HumanVisual human)ClothingVisuals.filter(human,visuals);}
 }
}
