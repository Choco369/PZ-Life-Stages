package pz.playablechildren;
import java.util.HashSet;
import zombie.scripting.objects.Item;
/** Uses the effective engine definition, so overrides/reloads cannot leave stale owners. */
public final class ClothingOrigins {
 private ClothingOrigins(){}
 public static String owner(Item item){
  if(item==null)return null;
  String mod=item.getModID();
  if(mod!=null&&!mod.isBlank())return ClothingCatalog.normalize(mod);
  return ClothingCatalog.sourceMod(item.getFullName(),item.getFileAbsPath(),item.getClothingItem(),new HashSet<>(zombie.ZomboidFileSystem.instance.getModIDs()));
 }
}
