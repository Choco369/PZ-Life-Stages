package pz.playablechildren;
import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.*;
import zombie.characters.BodyDamage.Nutrition;
import java.nio.ByteBuffer;
public final class PatchNutrition {
 private PatchNutrition(){}
 @Patch(className="zombie.characters.BodyDamage.Nutrition",methodName="update",strictMatch=false)
 public static final class Prepare {
  @OnEnter public static void enter(@This Nutrition n){NutritionState.prepare(NutritionRuntime.owner(n));}
 }
 @Patch(className="zombie.characters.BodyDamage.Nutrition",methodName="load",strictMatch=true)
 public static final class Load {
  @OnEnter(skipOn=true) public static boolean enter(@This Nutrition n,@Argument(0) ByteBuffer b){return NutritionRuntime.loadForStage(n,b);}
  @OnExit public static void exit(@This Nutrition n){NutritionRuntime.markLoaded(n);}
 }
 @Patch(className="zombie.characters.BodyDamage.Nutrition",methodName="applyWeightFromTraits",strictMatch=false)
 public static final class InitialTraits {
  @OnEnter(skipOn=true) public static boolean enter(@This Nutrition n){return NutritionRuntime.applyInitialTraits(n);}
 }
 @Patch(className="zombie.characters.BodyDamage.Nutrition",methodName="applyTraitFromWeight",strictMatch=false)
 public static final class WeightTraits {
  @OnEnter(skipOn=true) public static boolean enter(@This Nutrition n){return NutritionRuntime.applyWeightTraits(n);}
 }
 @Patch(className="zombie.characters.BodyDamage.Nutrition",methodName="updateWeight",strictMatch=false)
 public static final class WeightUpdate {
  @OnEnter(skipOn=true) public static boolean enter(@This Nutrition n){return NutritionRuntime.updateWeightForMinor(n);}
 }
 @Patch(className="zombie.characters.BodyDamage.Nutrition",methodName="setWeight",strictMatch=true)
 public static final class WeightSetter {
  @OnEnter(skipOn=true) public static boolean enter(@This Nutrition n,@Argument(0) double w){return NutritionRuntime.setWeightForStage(n,w);}
 }
 @Patch(className="zombie.characters.BodyDamage.Nutrition",methodName="updateCalories",strictMatch=false)
 public static final class CaloriesUpdate {
  @OnEnter public static void enter(@This Nutrition n){NutritionRuntime.enterCalories(n);}
  @OnExit(onThrowable=Throwable.class) public static void exit(@This Nutrition n){NutritionRuntime.exitCalories(n);}
 }
 @Patch(className="zombie.characters.BodyDamage.Nutrition",methodName="setCalories",strictMatch=true)
 public static final class CaloriesSetter {
  @OnEnter public static void enter(@This Nutrition n,@Argument(value=0,readOnly=false) float value){value=NutritionRuntime.scaledCaloriesArgument(n,value);}
 }
}
