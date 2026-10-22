package pz.playablechildren;

import java.util.ArrayDeque;
import java.util.Deque;
import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.OnEnter;
import me.zed_0xff.zombie_buddy.Patch.OnExit;
import me.zed_0xff.zombie_buddy.Patch.This;
import me.zed_0xff.zombie_buddy.Patch.Argument;
import zombie.PersistentOutfits;
import zombie.characters.IsoZombie;
import zombie.characters.WornItems.BodyLocations;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ItemBodyLocation;

/** Additive wardrobe preference; population weights and XML outfits unchanged.
 * The optional bag is represented by a native persistent outfitter, not modData:
 * ordinary zombie virtualization saves the outfit ID but discards modData. */
public final class PatchPrimarySchoolBags {
    private static final String NORMAL = "ChildZombie_PrimaryBag01";
    private static final String SHELL = "ChildZombie_PrimaryBagShellSuit";
    private static final ThreadLocal<Deque<IsoZombie>> SPAWNING = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<zombie.characters.IsoGameCharacter>> DRESSING = ThreadLocal.withInitial(ArrayDeque::new);
    private PatchPrimarySchoolBags() {}

    public static void beginSpawn(IsoZombie zombie) { SPAWNING.get().push(zombie); }
    public static void endSpawn() {
        var scope = SPAWNING.get();
        if (!scope.isEmpty()) scope.pop();
        if (scope.isEmpty()) SPAWNING.remove();
    }

    /** Append after the full native registry; never insert XML outfits into its
     * sorted list, which would renumber existing saved zombie outfit IDs. */
    public static void register(PersistentOutfits outfits) {
        outfits.registerOutfitter(NORMAL, true, (id, name, character) -> dressBase(outfits, id, "ChildZombie01", character));
        outfits.registerOutfitter(SHELL, true, (id, name, character) -> dressBase(outfits, id, "ChildZombie_ShellSuit", character));
    }

    private static void dressBase(PersistentOutfits outfits, int id, String name, zombie.characters.IsoGameCharacter character) {
        int index = outfits.getOutfitNames().indexOf(name);
        if (index < 0) return;
        // Preserve sex, seed and fallen-hat state. Delegate the entire original
        // dresser, including its hair, blood, dirt and attached-item rules.
        var scope = DRESSING.get();
        scope.push(character);
        try {
            outfits.dressInOutfit(character, (index << 16) | (id & 0x8000ffff));
        } finally {
            scope.pop();
            if (scope.isEmpty()) DRESSING.remove();
        }
        // Bag reference randomization consumes OutfitRNG. Wait until native
        // attachment/blood/holes work completes so its RNG continuation stays
        // identical to the base outfit. This never re-dresses the zombie.
        if (character instanceof IsoZombie zombie) afterDress(zombie, name);
    }

    private static String persistentName(int id) {
        int index = (StageBits.strip(id) & 0x7fffffff) >>> 16;
        var names = PersistentOutfits.instance.getOutfitNames();
        return index < names.size() ? names.get(index) : null;
    }

    public static int selectAtSpawn(IsoZombie zombie, int id) {
        var scope = SPAWNING.get();
        if (scope.isEmpty() || scope.peek() != zombie || zombie.isUsingWornItems()
                || StageBits.stageOf(id) != LifeStage.CHILD
                || !PatchPersistentOutfit.naturalEnabled(LifeStage.CHILD)
                || (id & 1) != 0 || zombie.getCurrentSquare() == null) return id;
        var room = zombie.getCurrentSquare().getRoom();
        String roomName = room == null ? null : room.getName();
        if (!"elementaryclassroom".equals(roomName) && !"elementaryhall".equals(roomName)) return id;
        String base = persistentName(id);
        String target = "ChildZombie01".equals(base) ? NORMAL : "ChildZombie_ShellSuit".equals(base) ? SHELL : null;
        if (target == null) return id;
        int index = PersistentOutfits.instance.getOutfitNames().indexOf(target);
        if (index < 0) return id;
        return StageBits.withStage((index << 16) | (id & 0x8000ffff), LifeStage.CHILD);
    }

    public static void afterDress(IsoZombie zombie, String outfitName) {
        if (zombie == null || zombie.isUsingWornItems()
                || StageBits.stageOf(zombie.getPersistentOutfitID()) != LifeStage.CHILD) return;
        if (DRESSING.get().contains(zombie)) return;
        String saved = persistentName(zombie.getPersistentOutfitID());
        if (!NORMAL.equals(saved) && !SHELL.equals(saved)) return;
        var visual = zombie.getHumanVisual();
        if (visual == null || visual.getOutfit() == null) return;
        var items = zombie.getItemVisuals();
        // clear() retains the previous Outfit pointer while deferred native
        // asset loading empties itemVisuals. Never produce a bag-only zombie.
        if (items.isEmpty()) return;
        var locations = BodyLocations.getGroup("Human");
        if (locations.getLocation(ItemBodyLocation.BACK) == null) return;
        for (var item : items) {
            var script = item.getScriptItem();
            if (script == null) return; // unknown mod clothing: do not risk removal
            var location = script.getBodyLocation();
            if (location == null) location = script.canBeEquipped;
            if (location == ItemBodyLocation.BACK || locations.isExclusive(ItemBodyLocation.BACK, location)) return;
        }
        var bag = ScriptManager.instance.getItem("Base.Bag_Schoolbag_Kids");
        if (bag == null || bag.getClothingItemAsset() == null || !bag.getClothingItemAsset().isReady()) return;
        // Native insertion preserves other visuals. dressInClothingItem clears them.
        if (visual.addClothingItem(items, bag) != null) zombie.onWornItemsChanged();
    }

    @Patch(className="zombie.PersistentOutfits", methodName="init")
    public static final class Register {
        @OnExit public static void exit(@This PersistentOutfits outfits) { PatchPrimarySchoolBags.register(outfits); }
    }
    @Patch(className="zombie.characters.ZombiesZoneDefinition", methodName="dressInRandomOutfit")
    public static final class NaturalSpawn {
        @OnEnter public static void enter(@Argument(0) IsoZombie zombie) { PatchPrimarySchoolBags.beginSpawn(zombie); }
        @OnExit(onThrowable=Throwable.class) public static void exit() { PatchPrimarySchoolBags.endSpawn(); }
    }
    @Patch(className="zombie.characters.IsoZombie", methodName="dressInPersistentOutfitID")
    public static final class Choose {
        @OnEnter public static void enter(@This IsoZombie zombie, @Argument(value=0, readOnly=false) int id) {
            id = PatchPrimarySchoolBags.selectAtSpawn(zombie, id);
        }
    }
    @Patch(className="zombie.characters.IsoZombie", methodName="dressInNamedOutfit")
    public static final class DressZombieNamed {
        @OnExit public static void exit(@This IsoZombie zombie, @Argument(0) String outfitName) {
            PatchPrimarySchoolBags.afterDress(zombie, outfitName);
        }
    }
    @Patch(className="zombie.characters.IsoZombie", methodName="clothingItemChanged")
    public static final class AssetReady {
        @OnExit public static void exit(@This IsoZombie zombie) { PatchPrimarySchoolBags.afterDress(zombie, null); }
    }
}
