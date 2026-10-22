package pz.playablechildren;
import java.util.*;
import zombie.characters.BodyDamage.Nutrition;

/** Condition units only at two exact native read sites, never on a whole frame. */
public final class NutritionConditionScope {
 public enum Kind { THERMAL, SPRINT }
 private record Frame(Nutrition owner,Kind kind){}
 private static final ThreadLocal<Deque<Frame>> SCOPES=new ThreadLocal<>();
 private static final StackWalker CALLERS=StackWalker.getInstance();
 private NutritionConditionScope(){}
 public static void enter(Nutrition n,Kind kind){if(n==null)return;Deque<Frame> stack=SCOPES.get();if(stack==null){stack=new ArrayDeque<>();SCOPES.set(stack);}stack.push(new Frame(n,kind));}
 public static void exit(Nutrition n,Kind kind){if(n==null)return;Deque<Frame> stack=SCOPES.get();if(stack==null)return;Frame top=stack.peek();if(top.owner()!=n || top.kind()!=kind)throw new IllegalStateException("Nutrition condition scope mismatch");stack.pop();if(stack.isEmpty())SCOPES.remove();}
 public static double weightForRead(Nutrition n,double vanilla){
  Deque<Frame> stack=SCOPES.get();if(stack==null || stack.peek().owner()!=n)return vanilla;
  LifeStage stage=NutritionState.resolve(NutritionRuntime.owner(n));if(stage!=LifeStage.CHILD && stage!=LifeStage.TEEN)return vanilla;
  Kind kind=stack.peek().kind();
  boolean allowed=CALLERS.walk(frames->frames.filter(f->!f.getClassName().equals(NutritionConditionScope.class.getName())).skip(1).findFirst().map(f->kind==Kind.THERMAL?f.getClassName().equals("zombie.characters.BodyDamage.Thermoregulator") && f.getMethodName().equals("updateNodesHeatDelta"):f.getClassName().equals("zombie.characters.IsoPlayer") && f.getMethodName().equals("updateInternal2")).orElse(false));
  return allowed?NutritionPolicy.equivalent(vanilla,stage):vanilla;
 }
}
