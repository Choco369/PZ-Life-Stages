package pz.playablechildren;

import java.util.List;
import java.util.Locale;
import zombie.ZomboidFileSystem;
import zombie.core.skinnedmodel.population.ClothingItem;

public final class AppearanceTextureRestrictions {
 private static final String MOD="DetailedPlayerTextureFemaleB42";
 private AppearanceTextureRestrictions(){}
 public static boolean blocks(Object owner,ClothingItem asset){
  LifeStage stage=ClothingRuntime.stage(owner);
  if(asset==null||(stage!=LifeStage.CHILD&&stage!=LifeStage.TEEN))return false;
  ZomboidFileSystem fs=ZomboidFileSystem.instance;
  if(!fs.getModIDs().contains(MOD))return false;
  String root=normalize(fs.getModDir(MOD));if(root.isEmpty())return false;
  if(!root.endsWith("/"))root+="/";
  return textures(fs,root,asset.baseTextures)||textures(fs,root,asset.textureChoices);
 }
 private static boolean textures(ZomboidFileSystem fs,String root,List<String> names){
  if(names==null)return false;
  for(String name:names){
   String path=normalize(name);if(path.isEmpty())continue;
   if(!path.startsWith("media/")&&!path.contains(":"))path="media/textures/"+path;
   if(!path.endsWith(".png"))path+=".png";
   if(normalize(fs.getString(path)).startsWith(root))return true;
  }
  return false;
 }
 private static String normalize(String value){return value==null?"":value.replace('\\','/').toLowerCase(Locale.ROOT);}
}
