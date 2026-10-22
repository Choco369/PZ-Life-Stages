package pz.playablechildren;

import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.Argument;
import me.zed_0xff.zombie_buddy.Patch.OnEnter;
import me.zed_0xff.zombie_buddy.Patch.OnExit;
import me.zed_0xff.zombie_buddy.Patch.Return;
import me.zed_0xff.zombie_buddy.Patch.This;
import zombie.PersistentOutfits;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.population.OutfitManager;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;

public final class PatchPersistentOutfit {
    private PatchPersistentOutfit() {}

    /** Native multiplayer zone outfits use ZZD.<zone>.<outfit>. */
    public static LifeStage naturalStage(String name) {
        if (name == null) return LifeStage.ADULT;
        if (name.startsWith("ZZD.")) name = name.substring(name.lastIndexOf('.') + 1);
        if (ChildPolicy.isChildOutfitName(name)) return LifeStage.CHILD;
        if (TeenPolicy.isTeenOutfitName(name)) return LifeStage.TEEN;
        if (name.equals("ElderZombie01") || name.equals("ElderZombie_HospitalPatient")) return LifeStage.ELDER;
        return LifeStage.ADULT;
    }

    /** Color only: never clear the visual, replace rot textures or blood/holes. */
    public static void grayNaturalElder(IsoGameCharacter character) {
        if (!(character instanceof IsoZombie zombie) || zombie.isReanimatedPlayer()
                || StageBits.stageOf(zombie.getPersistentOutfitID()) != LifeStage.ELDER) return;
        var visual = zombie.getHumanVisual();
        if (visual == null) return;
        var gray = new ImmutableColor(0.65f, 0.64f, 0.61f, 1f);
        visual.setHairColor(gray);
        visual.setBeardColor(gray);
    }

    public static boolean naturalEnabled(LifeStage stage) {
        if (stage == LifeStage.ADULT || LuaManager.env == null) return true;
        Object vars = LuaManager.env.rawget("SandboxVars");
        if (!(vars instanceof KahluaTable sandbox)
                || !(sandbox.rawget("PlayableChildren") instanceof KahluaTable options)) return true;
        String enabled = stage == LifeStage.CHILD ? "EnableChildZombies"
                : stage == LifeStage.TEEN ? "EnableTeenZombies" : "EnableElderZombies";
        if (Boolean.FALSE.equals(options.rawget(enabled))) return false;
        Object frequency = options.rawget(stage == LifeStage.ELDER ? "ElderPopulation" : "Population");
        return !(frequency instanceof Number number) || number.doubleValue() != 5.0;
    }

    /** Native missing-outfit fallback samples the whole XML registry, not zone
     * weights. Filter only a disabled-age result; ordinary draws stay untouched. */
    public static boolean eligibleOutfit(Outfit outfit, boolean nonSilly) {
        return naturalEnabled(naturalStage(outfit.name)) && (!nonSilly
                || (!outfit.name.contains("Santa") && !outfit.name.contains("Spiffo") && !outfit.name.contains("Naked")));
    }

    public static Outfit filterRandomOutfit(Outfit result, OutfitManager manager, boolean female, boolean nonSilly) {
        if (result == null || naturalEnabled(naturalStage(result.name))) return result;
        var list = female ? manager.femaleOutfits : manager.maleOutfits;
        int eligible = 0;
        for (Outfit outfit : list) if (eligibleOutfit(outfit, nonSilly)) eligible++;
        if (eligible == 0) return null; // Vanilla always has adult outfits.
        int choice = zombie.core.random.Rand.Next(eligible);
        for (Outfit outfit : list) {
            if (eligibleOutfit(outfit, nonSilly) && choice-- == 0) return outfit;
        }
        return null;
    }

    @Patch(className="zombie.PersistentOutfits", methodName="pickOutfit")
    public static final class PickOutfit {
        @OnExit public static void exit(@Return(readOnly=false) int result, @Argument(0) String outfitName) {
            if (result == 0) return;
            LifeStage natural = PatchPersistentOutfit.naturalStage(outfitName);
            if (natural != LifeStage.ADULT) {
                result = PatchChildReanimation.addStageBit(result, natural);
                return;
            }
            LifeStage stage = PatchChildReanimation.activeReanimationStage();
            if ("ReanimatedPlayer".equals(outfitName) && (stage == LifeStage.CHILD || stage == LifeStage.TEEN))
                result = PatchChildReanimation.addStageBit(result, stage);
        }
    }

    @Patch(className="zombie.PersistentOutfits", methodName="getOutfit")
    public static final class GetOutfit {
        @OnExit public static void exit(@Return(readOnly=false) int result,
                                        @Argument(0) int outfitID,
                                        @This PersistentOutfits outfits) {
            if (StageBits.stageOf(outfitID) != LifeStage.ADULT) {
                int clean = PatchChildReanimation.stripStageBits(outfitID);
                int resolved = outfits.getOutfit(clean);
                result = resolved == 0 ? 0 : PatchChildReanimation.copyStageBits(resolved, outfitID);
            }
        }
    }

    @Patch(className="zombie.PersistentOutfits", methodName="dressInOutfit")
    public static final class DressInOutfit {
        @OnEnter public static void enter(@Argument(0) IsoGameCharacter character,
                                          @Argument(value=1, readOnly=false) int outfitID) {
            outfitID = PatchChildReanimation.stripStageBits(outfitID);
        }
        @OnExit public static void exit(@Argument(0) IsoGameCharacter character) {
            PatchPersistentOutfit.grayNaturalElder(character);
        }
    }

    // Clothing assets can finish loading after the persistent outfit call.
    @Patch(className="zombie.characters.IsoZombie", methodName="dressInNamedOutfit")
    public static final class DressZombieNamed {
        @OnExit public static void exit(@This IsoZombie zombie) {
            PatchPersistentOutfit.grayNaturalElder(zombie);
        }
    }

    /** Final outfit registration follows OnInitGlobalModData. Another mod can
     * materialize the native cache earlier, before our Lua weights are added. */
    @Patch(className="zombie.characters.ZombiesZoneDefinition", methodName="registerCustomOutfits")
    public static final class RegisterZoneOutfits {
        @OnEnter public static void enter() {
            zombie.characters.ZombiesZoneDefinition.dirty = true;
        }
    }

    @Patch(className="zombie.core.skinnedmodel.population.OutfitManager", methodName="GetRandomOutfit")
    public static final class RandomFallback {
        @OnExit public static void exit(@This OutfitManager manager, @Argument(0) boolean female,
                @Return(readOnly=false) Outfit result) {
            result = PatchPersistentOutfit.filterRandomOutfit(result, manager, female, false);
        }
    }

    @Patch(className="zombie.core.skinnedmodel.population.OutfitManager", methodName="GetRandomNonSillyOutfit")
    public static final class NonSillyFallback {
        @OnExit public static void exit(@This OutfitManager manager, @Argument(0) boolean female,
                @Return(readOnly=false) Outfit result) {
            result = PatchPersistentOutfit.filterRandomOutfit(result, manager, female, true);
        }
    }
}
