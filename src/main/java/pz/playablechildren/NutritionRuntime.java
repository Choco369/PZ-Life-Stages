package pz.playablechildren;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.*;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.Nutrition;
import zombie.GameTime;
import zombie.network.GameClient;
import zombie.scripting.objects.CharacterTrait;
import zombie.Lua.LuaEventManager;

/** Narrow native-field access for silent load/conversion, never compensatory damage. */
public final class NutritionRuntime {
 private static final Field PARENT=required("parent",IsoPlayer.class),WEIGHT=required("weight",double.class);
 private static final Set<Nutrition> LOADED=Collections.newSetFromMap(new WeakHashMap<>());
 private static final Set<Nutrition> INITIALIZED=Collections.newSetFromMap(new WeakHashMap<>());
 private static final Field COUNTER=required("updatedWeight",int.class);
 private static final ThreadLocal<Deque<Nutrition>> CALORIES=new ThreadLocal<>();
 private static final StackWalker CALLERS=StackWalker.getInstance();
 private NutritionRuntime(){}
 private static Field required(String name,Class<?> type){Field f=ReflectionAccess.field(Nutrition.class,name);if(f==null||f.getType()!=type)throw new IllegalStateException("Nutrition field mismatch: "+name);return f;}
 public static IsoPlayer owner(Nutrition n){try{return (IsoPlayer)PARENT.get(n);}catch(IllegalAccessException ex){throw new IllegalStateException(ex);}}
 public static double rawWeight(Nutrition n){try{return WEIGHT.getDouble(n);}catch(IllegalAccessException ex){throw new IllegalStateException(ex);}}
 public static void writeWeightWithoutDamage(Nutrition n,double w){try{WEIGHT.setDouble(n,w);}catch(IllegalAccessException ex){throw new IllegalStateException(ex);}}
 public static boolean loadForStage(Nutrition n,ByteBuffer b){
  IsoPlayer p=owner(n);LifeStage s=NutritionState.resolve(p);
  var data=NutritionState.dataIfPresent(p);Object marker=data==null?null:data.rawget(NutritionState.KEY);
  if(s==null || s==LifeStage.ADULT && marker==null)return false;
  boolean controlled=s!=LifeStage.ADULT;
  n.setCalories(loadedReserve(b,800,controlled));n.setProteins(loadedReserve(b,0,controlled));n.setLipids(loadedReserve(b,0,controlled));n.setCarbohydrates(loadedReserve(b,0,controlled));writeWeightWithoutDamage(n,b.getFloat());
  synchronized(LOADED){LOADED.add(n);}NutritionState.prepare(p,s);return true;
 }
 private static float loadedReserve(ByteBuffer b,float fallback,boolean controlled){
  float value=b.getFloat();
  // Validate the saved float before vanilla clamps infinities to finite endpoints.
  return controlled && !Float.isFinite(value)?fallback:value;
 }
 public static void markLoaded(Nutrition n){synchronized(LOADED){LOADED.add(n);}}
 public static boolean wasLoaded(Nutrition n){synchronized(LOADED){return LOADED.contains(n);}}
 public static boolean applyInitialTraits(Nutrition n){
  IsoPlayer p=owner(n);LifeStage s=NutritionState.resolve(p);
  if(s==null || s==LifeStage.ADULT)return false;
  synchronized(INITIALIZED){if(wasLoaded(n) || INITIALIZED.contains(n))return true;}
  if(!NutritionState.prepare(p,s))return true;
  double w=NutritionPolicy.reference(s);
  CharacterTrait[] traits={CharacterTrait.EMACIATED,CharacterTrait.VERY_UNDERWEIGHT,CharacterTrait.UNDERWEIGHT,CharacterTrait.OVERWEIGHT,CharacterTrait.OBESE};
  double[] weights={50,60,70,95,105};
  for(int i=0;i<traits.length;i++)if(p.hasTrait(traits[i]))w=weights[i]*NutritionPolicy.ratio(s);
  writeWeightWithoutDamage(n,w);synchronized(INITIALIZED){INITIALIZED.add(n);}return true;
 }
 public static boolean applyWeightTraits(Nutrition n){
  IsoPlayer p=owner(n);LifeStage s=NutritionState.resolve(p);if(s!=LifeStage.CHILD && s!=LifeStage.TEEN)return false;
  CharacterTrait[] traits={CharacterTrait.EMACIATED,CharacterTrait.VERY_UNDERWEIGHT,CharacterTrait.UNDERWEIGHT,CharacterTrait.OVERWEIGHT,CharacterTrait.OBESE};
  for(CharacterTrait t:traits)p.getCharacterTraits().remove(t);
  CharacterTrait t=switch(NutritionPolicy.category(rawWeight(n),s)){
   case EMACIATED->CharacterTrait.EMACIATED;case VERY_UNDERWEIGHT->CharacterTrait.VERY_UNDERWEIGHT;case UNDERWEIGHT->CharacterTrait.UNDERWEIGHT;case OVERWEIGHT->CharacterTrait.OVERWEIGHT;case OBESE->CharacterTrait.OBESE;case NORMAL->null;
  };if(t!=null)p.getCharacterTraits().add(t);return true;
 }
 public static boolean updateWeightForMinor(Nutrition n){
  IsoPlayer p=owner(n);LifeStage s=NutritionState.resolve(p);if(s!=LifeStage.CHILD && s!=LifeStage.TEEN)return false;
  n.setIncWeight(false);n.setIncWeightLot(false);n.setDecWeight(false);
  double real=rawWeight(n),equiv=NutritionPolicy.equivalent(real,s),r=NutritionPolicy.ratio(s);
  float gain=1000;
  if(equiv<90 && p.hasTrait(CharacterTrait.WEIGHT_GAIN))gain=700;
  if(equiv>70 && p.hasTrait(CharacterTrait.WEIGHT_LOSS))gain=1800;
  gain+=(float)((equiv-80)*40);
  float loss=(float)((equiv-70)*30);if(loss>0)loss=0;
  double next=real;float seconds=GameTime.getInstance().getGameWorldSecondsSinceLastUpdate();
  if(n.getCalories()>gain){
   n.setIncWeight(true);float delta=n.getCalories()/4000;if(delta>1)delta=1;float rate=1.3E-5f;
   if(n.getCarbohydrates()>700 || n.getLipids()>700){rate*=3;n.setIncWeightLot(true);}
   else if(n.getCarbohydrates()>400 || n.getLipids()>400){rate*=2;n.setIncWeightLot(true);}
   next=real+(rate*delta*seconds)*r;
  }else if(n.getCalories()<loss){n.setDecWeight(true);float delta=Math.abs(n.getCalories())/2500;if(delta>1)delta=1;next=real-(8.5E-6f*delta*seconds)*r;}
  if(!GameClient.client){
   n.setWeight(next);
   try{int count=COUNTER.getInt(n)+1;if(count>=2000){applyWeightTraits(n);count=0;}COUNTER.setInt(n,count);}catch(IllegalAccessException ex){throw new IllegalStateException(ex);}
  }
  return true;
 }
 public static boolean setWeightForStage(Nutrition n,double requested){
  IsoPlayer p=owner(n);LifeStage s=NutritionState.resolve(p);if(s==null || s==LifeStage.ADULT)return false;
  if(!Double.isFinite(requested)){writeWeightWithoutDamage(n,NutritionPolicy.reference(s));return true;}
  double min=NutritionPolicy.minimum(s);
  if(requested<min){
   requested=min;
   float damage=p.getBodyDamage().getHealthReductionFromSevereBadMoodles()*GameTime.instance.getMultiplier()*NutritionPolicy.damageFactor(s);
   p.getBodyDamage().ReduceGeneralHealth(damage);
   LuaEventManager.triggerEvent("OnPlayerGetDamage",p,"LOWWEIGHT",damage);
  }
  writeWeightWithoutDamage(n,requested);return true;
 }
 public static void enterCalories(Nutrition n){Deque<Nutrition> stack=CALORIES.get();if(stack==null){stack=new ArrayDeque<>();CALORIES.set(stack);}stack.push(n);}
 public static void exitCalories(Nutrition n){Deque<Nutrition> stack=CALORIES.get();if(stack==null)return;if(stack.peek()==n)stack.pop();else throw new IllegalStateException("Nutrition calorie scope mismatch");if(stack.isEmpty())CALORIES.remove();}
 public static float scaledCaloriesArgument(Nutrition n,float requested){
  Deque<Nutrition> stack=CALORIES.get();if(stack==null || stack.peek()!=n || !(requested<n.getCalories()))return requested;
  float factor=NutritionPolicy.expenditureFactor(NutritionState.resolve(owner(n)));if(factor==1)return requested;
  boolean nativeCaller=CALLERS.walk(frames->frames.filter(f->!f.getClassName().equals(NutritionRuntime.class.getName())).skip(1).findFirst().map(f->f.getClassName().equals(Nutrition.class.getName()) && f.getMethodName().equals("updateCalories")).orElse(false));
  return nativeCaller?n.getCalories()-(n.getCalories()-requested)*factor:requested;
 }
}
