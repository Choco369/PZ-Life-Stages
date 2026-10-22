package pz.playablechildren;
import java.util.*;
public final class SubstancePolicy {
 private SubstancePolicy(){}
 private static final Set<String> MINOR_TRAITS=Set.of("nnc:benzoaddict","nnc:cokehead","nnc:methhead","nnc:mdmaaddict","nnc:opioidaddict","nnc:paranoiduser","nnc:steroidaddict");
 public static boolean blocked(LifeStage stage,SubstanceCatalog.Kind kind){
  return stage==LifeStage.CHILD && kind!=null && kind!=SubstanceCatalog.Kind.NONE
   || stage==LifeStage.TEEN && kind==SubstanceCatalog.Kind.NARCOTIC;
 }
 public static boolean blockedTrait(LifeStage stage,String id){
  if(stage!=LifeStage.CHILD&&stage!=LifeStage.TEEN || id==null)return false;
  String token=id.toLowerCase(Locale.ROOT);
  return MINOR_TRAITS.contains(token) || stage==LifeStage.CHILD && token.equals("nnc:pothead");
 }
}
