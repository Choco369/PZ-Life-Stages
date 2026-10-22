package pz.playablechildren;

import java.util.*;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.Nutrition;

/** A marker describes the units already applied, not a second copy of nutrition. */
public final class NutritionState {
 public static final String KEY="PlayableChildrenNutrition";
 private static final Set<IsoPlayer> WARNED=Collections.newSetFromMap(new WeakHashMap<>());
 private static final Map<IsoPlayer,LifeStage> PENDING_PUBLICATION=Collections.synchronizedMap(new WeakHashMap<>());
 private NutritionState() {}
 public static LifeStage parse(Object raw){try{return raw==null?null:LifeStage.valueOf(raw.toString());}catch(IllegalArgumentException ex){return null;}}
 public static KahluaTable dataIfPresent(IsoPlayer p){return p==null?null:(KahluaTable)ReflectionAccess.tryGet(p,"table",null);}
 public static LifeStage resolve(IsoPlayer p){
  if(p==null)return null;
  LifeStage dynamic=PlayerLifeStages.find(p);
  KahluaTable data=dataIfPresent(p);Object raw=data==null?null:data.rawget("PlayableChildrenLifecycle");
  LifeStage stored=null;Object pending=null;
  if(raw instanceof KahluaTable t && Boolean.TRUE.equals(t.rawget("initialized"))){stored=parse(t.rawget("stage"));pending=t.rawget("pendingVisualFromStage");}
  LifeStage explicit=PENDING_PUBLICATION.get(p);
  if(explicit!=null){
   if(explicit==stored && dynamic!=explicit)return explicit;
   PENDING_PUBLICATION.remove(p);
  }
  // An explicitly committed transition can precede successful Java publication.
  if(stored!=null && dynamic!=null && dynamic==parse(pending) && stored!=dynamic)return stored;
  if(dynamic!=null)return dynamic;
  if(stored!=null)return stored;
  return LifeStageResolver.stageOf(p.getDescriptor());
 }
 public static boolean prepare(IsoPlayer p){return prepareInternal(p,resolve(p));}
 public static boolean prepare(IsoPlayer p,LifeStage stage){
  boolean success=prepareInternal(p,stage);
  if(success && p!=null){
   KahluaTable data=dataIfPresent(p);Object raw=data==null?null:data.rawget("PlayableChildrenLifecycle");
   if(raw instanceof KahluaTable t && Boolean.TRUE.equals(t.rawget("initialized")) && stage==parse(t.rawget("stage")) && PlayerLifeStages.find(p)!=stage)PENDING_PUBLICATION.put(p,stage);
  }
  return success;
 }
 private static synchronized boolean prepareInternal(IsoPlayer p,LifeStage stage){
  if(p==null || stage==null || p.getNutrition()==null)return false;
  Nutrition n=p.getNutrition();KahluaTable data=dataIfPresent(p);Object previous=data==null?null:data.rawget(KEY);
  if(previous==null && stage==LifeStage.ADULT)return true;
  if(data==null)data=p.getModData();
  double old=NutritionRuntime.rawWeight(n);double next=old;
  LifeStage before=null;boolean valid=false;
  if(previous instanceof KahluaTable t){
   before=parse(t.rawget("stage"));Object v=t.rawget("version"),r=t.rawget("reference");
   valid=v instanceof Number && ((Number)v).doubleValue()==1 && before!=null && r instanceof Number && ((Number)r).doubleValue()==NutritionPolicy.reference(before);
  }
  if(valid && before==stage && Double.isFinite(old) && old>=NutritionPolicy.minimum(stage)){
   if(stage!=LifeStage.ADULT)repairReserves(n);return true;
  }
  if(!Double.isFinite(old)){if(stage==LifeStage.ADULT)return true;next=NutritionPolicy.reference(stage);}
  else if(valid)next=NutritionPolicy.convert(old,before,stage);
  else if(previous==null)next=old*NutritionPolicy.ratio(stage);
  else if(stage!=LifeStage.ADULT){synchronized(WARNED){if(WARNED.add(p))System.err.println("[PlayableChildren] Invalid nutrition marker; preserving finite kg, repairing units marker.");}}
  else return true;
  next=Math.max(NutritionPolicy.minimum(stage),next);
  KahluaTable mark=new KahluaTableImpl(new HashMap<>());mark.rawset("version",1.0);mark.rawset("stage",stage.name());mark.rawset("reference",NutritionPolicy.reference(stage));
  try{NutritionRuntime.writeWeightWithoutDamage(n,next);data.rawset(KEY,mark);}
  catch(RuntimeException ex){NutritionRuntime.writeWeightWithoutDamage(n,old);data.rawset(KEY,previous);System.err.println("[PlayableChildren] Nutrition preparation failed: "+ex);return false;}
  if(stage!=LifeStage.ADULT)repairReserves(n);
  return true;
 }
 private static void repairReserves(Nutrition n){
  if(!Float.isFinite(n.getCalories()))n.setCalories(800);
  if(!Float.isFinite(n.getProteins()))n.setProteins(0);
  if(!Float.isFinite(n.getLipids()))n.setLipids(0);
  if(!Float.isFinite(n.getCarbohydrates()))n.setCarbohydrates(0);
 }
}
