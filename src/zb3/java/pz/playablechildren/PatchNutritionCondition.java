package pz.playablechildren;
import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.*;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.*;
public final class PatchNutritionCondition {
 private PatchNutritionCondition(){}
 public static Nutrition thermalOwner(Thermoregulator t){IsoPlayer p=(IsoPlayer)ReflectionAccess.tryGet(t,"player",null);return p==null?null:p.getNutrition();}
 @Patch(className="zombie.characters.BodyDamage.Nutrition",methodName="getWeight",strictMatch=false)
 public static final class Read {
  @OnExit public static void exit(@This Nutrition n,@Return(readOnly=false) double w){w=NutritionConditionScope.weightForRead(n,w);}
 }
 @Patch(className="zombie.characters.BodyDamage.Thermoregulator",methodName="updateNodesHeatDelta",strictMatch=false)
 public static final class Thermal {
  @OnEnter public static void enter(@This Thermoregulator t){NutritionConditionScope.enter(PatchNutritionCondition.thermalOwner(t),NutritionConditionScope.Kind.THERMAL);}
  @OnExit(onThrowable=Throwable.class) public static void exit(@This Thermoregulator t){NutritionConditionScope.exit(PatchNutritionCondition.thermalOwner(t),NutritionConditionScope.Kind.THERMAL);}
 }
 @Patch(className="zombie.characters.IsoPlayer",methodName="updateInternal2",strictMatch=false)
 public static final class Sprint {
  @OnEnter public static void enter(@This IsoPlayer p){NutritionConditionScope.enter(p.getNutrition(),NutritionConditionScope.Kind.SPRINT);}
  @OnExit(onThrowable=Throwable.class) public static void exit(@This IsoPlayer p){NutritionConditionScope.exit(p.getNutrition(),NutritionConditionScope.Kind.SPRINT);}
 }
}
