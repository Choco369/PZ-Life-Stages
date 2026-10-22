package pz.playablechildren;
import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.*;
import zombie.characters.IsoGameCharacter;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.inventory.InventoryItem;
import zombie.characters.traits.CharacterTraits;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.inventory.types.Food;
public final class PatchSubstanceRestrictions {
 private PatchSubstanceRestrictions(){}
 @Patch(className="zombie.characters.IsoGameCharacter",methodName="Eat",strictMatch=true)
 public static final class Eat {
  @OnEnter(skipOn=true) public static boolean enter(@This IsoGameCharacter p,@Argument(0) InventoryItem item){return SubstanceRuntime.blocksConsumption(p,item);}
 }
 @Patch(className="zombie.characters.IsoGameCharacter",methodName="EatOnClient",strictMatch=true)
 public static final class ClientEat {
  @OnEnter(skipOn=true) public static boolean enter(@This IsoGameCharacter p,@Argument(0) InventoryItem item){return SubstanceRuntime.blocksConsumption(p,item);}
 }
 @Patch(className="zombie.characters.BodyDamage.BodyDamage",methodName="JustTookPill",strictMatch=true)
 public static final class Pill {
  @OnEnter(skipOn=true) public static boolean enter(@This BodyDamage damage,@Argument(0) InventoryItem item){
   return SubstanceRuntime.blocksConsumption((IsoGameCharacter)ReflectionAccess.tryGet(damage,"parentChar",null),item);
  }
 }
 @Patch(className="zombie.characters.IsoGameCharacter",methodName="getCharacterTraits",strictMatch=true)
 public static final class TraitOwner {
  @OnExit public static void exit(@This IsoGameCharacter p,@Return CharacterTraits traits){SubstanceRuntime.rememberTraits(p,traits);}
 }
 @Patch(className="zombie.characters.traits.CharacterTraits",methodName="set",strictMatch=true)
 public static final class TraitAdd {
  @OnEnter(skipOn=true) public static boolean enter(@This CharacterTraits traits,@Argument(0) CharacterTrait trait,@Argument(1) boolean value){return SubstanceRuntime.blocksAcquisition(traits,trait,value);}
 }
 @Patch(className="zombie.characters.traits.CharacterTraits",methodName="load",strictMatch=true)
 public static final class TraitLoad {
  @OnEnter public static void enter(@This CharacterTraits traits){SubstanceRuntime.beginRestore(traits);}
  @OnExit(onThrowable=Throwable.class) public static void exit(@This CharacterTraits traits){SubstanceRuntime.endRestore(traits);}
 }
 @Patch(className="zombie.characters.traits.CharacterTraits",methodName="read",strictMatch=true)
 public static final class TraitRead {
  @OnEnter public static void enter(@This CharacterTraits traits){SubstanceRuntime.beginRestore(traits);}
  @OnExit(onThrowable=Throwable.class) public static void exit(@This CharacterTraits traits){SubstanceRuntime.endRestore(traits);}
 }
 @Patch(className="zombie.entity.components.crafting.recipe.CraftRecipeManager",methodName="isValidRecipeForCharacter",strictMatch=true)
 public static final class RecipeValid {
  @OnEnter(skipOn=true) public static boolean enter(@Argument(0) CraftRecipe recipe,@Argument(1) IsoGameCharacter p){return recipe!=null && SubstanceRuntime.blocksRecipe(p,recipe.getScriptObjectFullType());}
 }
 @Patch(className="zombie.inventory.InventoryItem",methodName="copyConditionStatesFrom",strictMatch=true)
 public static final class FoodReplacement {
  @OnExit public static void exit(@This InventoryItem target,@Argument(0) InventoryItem source){SubstanceRuntime.inheritFood(target,source);}
 }
 @Patch(className="zombie.inventory.types.Food",methodName="copyExtraItems",strictMatch=true)
 public static final class FoodSplit {
  @OnExit public static void exit(@This Food target,@Argument(0) Food source){SubstanceRuntime.inheritFood(target,source);}
 }
 @Patch(className="zombie.scripting.objects.EvolvedRecipe",methodName="addItem",strictMatch=true)
 public static final class EvolvedFood {
  @OnExit public static void exit(@Argument(0) InventoryItem originalBase,@Argument(1) InventoryItem ingredient,@Return InventoryItem result){
   // Exit advice backs up entry arguments: the native method reassigns baseItem
   // when turning bread into a sandwich. Preserve the original base's provenance.
   SubstanceRuntime.inheritFood(result,originalBase);
   SubstanceRuntime.inheritFood(result,ingredient);
  }
 }
}
