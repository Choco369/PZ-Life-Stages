package pz.playablechildren;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
/** Package identities and source metadata only; no third-party resources. */
public final class ClothingCatalog {
 private ClothingCatalog(){}
 public static String normalize(String id){return id==null?null:id.trim().replaceFirst("^\\\\+","");}
 private static final Set<String> RESTRICTED=Set.of("ahzclothingb42","ahzclothingb42FIX","CamisetasSeleccion","RandomClothing","senchou","seifuku","HideClothingSlot","GanydeBielovzki's Frockin Backstage!","GanydeBielovzki's Frockin Splendor!","GanydeBielovzki's Frockin Splendor! Vol.2","GanydeBielovzki's Frockin Splendor! Vol.3","GanydeBielovzki's Frockin Splendor! Vol.4","GanydeBielovzki's Frockin Splendor! Vol.5","GanydeBielovzki's Frockin Splendor! Vol.6","GanydeBielovzki's Frockin Wiseguys","GanydeBielovzki's Frockin Shirts n Ties");
 public static boolean restrictedMod(String id){return id!=null&&RESTRICTED.contains(normalize(id));}
 private record Entry(String mod,String type,String source,String clothing){}
 private static final List<Entry> ENTRIES=read();
 private static List<Entry> read(){
  InputStream input=ClothingCatalog.class.getResourceAsStream("/pz/playablechildren/clothing-restrictions.tsv");
  if(input==null)return List.of();
  List<Entry> entries=new ArrayList<>();
  try(var reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))){
   String line;while((line=reader.readLine())!=null){if(line.startsWith("#"))continue;String[] s=line.split("\t",-1);if(s.length==4)entries.add(new Entry(s[0],s[1],path(s[2]),s[3]));}
  }catch(IOException e){System.err.println("[PZ Life Stages] Clothing source metadata unavailable: "+e.getMessage());}
  return List.copyOf(entries);
 }
 private static String path(String s){return s==null?"":s.replace('\\','/').toLowerCase(Locale.ROOT);}
 /** Fallback requires a proven source suffix and active owner, never just Base.item. */
 public static String sourceMod(String fullType,String sourcePath,String clothingName,Set<String> activeMods){
  if(sourcePath==null||sourcePath.isBlank()||activeMods==null)return null;
  String source=path(sourcePath),result=null;
  for(Entry e:ENTRIES){
   if(!Objects.equals(fullType,e.type)||!source.endsWith(e.source))continue;
   boolean active=false;for(String m:activeMods)if(Objects.equals(normalize(m),e.mod)){active=true;break;}
   if(!active||(!e.clothing.isEmpty()&&!Objects.equals(clothingName,e.clothing)))continue;
   if(result!=null&&!result.equals(e.mod))return null;
   result=e.mod;
  }
  return result;
 }
}
