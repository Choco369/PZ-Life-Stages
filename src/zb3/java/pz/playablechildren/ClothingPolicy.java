package pz.playablechildren;
public final class ClothingPolicy {
 private ClothingPolicy(){}
 public static boolean blocks(LifeStage stage,String ownerMod){
  return (stage==LifeStage.CHILD||stage==LifeStage.TEEN)&&ClothingCatalog.restrictedMod(ownerMod);
 }
 public static boolean blocksAppearance(LifeStage stage,boolean female,String fullType){
  if(stage!=LifeStage.CHILD&&stage!=LifeStage.TEEN)return false;
  if(stage==LifeStage.CHILD&&"Base.Skirt_Mini".equals(fullType))return true;
  if(fullType!=null&&fullType.startsWith("RasBodyMod."))return true;
  if(stage==LifeStage.CHILD&&("Base.M_Hair_Body".equals(fullType)||"Base.F_Hair_Body".equals(fullType)))return true;
  if("VOE.Skirt_Frilly".equals(fullType))return true;
  if(female&&"Base.F_Muscle".equals(fullType))return true;
  return stage==LifeStage.CHILD&&!female&&"Base.M_Beard_Stubble".equals(fullType);
 }
}
