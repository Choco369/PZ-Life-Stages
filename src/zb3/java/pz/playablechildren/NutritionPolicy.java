package pz.playablechildren;

/** Game-relative balance; native adult rules remain the reference. */
public final class NutritionPolicy {
 private NutritionPolicy() {}
 public enum WeightCategory { EMACIATED, VERY_UNDERWEIGHT, UNDERWEIGHT, NORMAL, OVERWEIGHT, OBESE }
 public static double reference(LifeStage s){return s==LifeStage.CHILD?35:s==LifeStage.TEEN?65:80;}
 public static double ratio(LifeStage s){return reference(s)/80.;}
 private static void finite(double w){if(!Double.isFinite(w))throw new IllegalArgumentException("Non-finite nutrition weight");}
 public static double equivalent(double w,LifeStage s){finite(w);return w/ratio(s);}
 public static double convert(double w,LifeStage from,LifeStage to){finite(w);return w*reference(to)/reference(from);}
 public static double minimum(LifeStage s){return 35*ratio(s);}
 public static float expenditureFactor(LifeStage s){return !AgeGameplayApi.effectsEnabled(s)?1:s==LifeStage.CHILD?1.05f:s==LifeStage.TEEN?1.03f:s==LifeStage.ELDER?.97f:1;}
 public static float damageFactor(LifeStage s){return !AgeGameplayApi.effectsEnabled(s)?1:s==LifeStage.CHILD?1.05f:s==LifeStage.ELDER?1.10f:1;}
 public static WeightCategory category(double w,LifeStage s){
  finite(w);double r=ratio(s);
  if(w<=50*r)return WeightCategory.EMACIATED;
  if(w<=65*r)return WeightCategory.VERY_UNDERWEIGHT;
  if(w<=75*r)return WeightCategory.UNDERWEIGHT;
  if(w<85*r)return WeightCategory.NORMAL;
  if(w<100*r)return WeightCategory.OVERWEIGHT;
  return WeightCategory.OBESE;
 }
}
