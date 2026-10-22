package pz.playablechildren;

import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.Argument;
import me.zed_0xff.zombie_buddy.Patch.OnExit;
import me.zed_0xff.zombie_buddy.Patch.Return;
import me.zed_0xff.zombie_buddy.Patch.This;
import zombie.characters.IsoZombie;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.objects.IsoDeadBody;

public final class PatchChildInventory {
   private PatchChildInventory() {
   }

   @Patch(className = "zombie.inventory.ItemContainer", methodName = "isRemoveItemAllowed")
   public static final class IsRemoveItemAllowed {
      @OnExit
      public static void exit(@This ItemContainer var0, @Argument(0) InventoryItem var1, @Return(readOnly = false) boolean var2) {
         if (var0.getParent() instanceof IsoDeadBody var4 && ChildIdentity.isZombieChild(var4)) {
            var2 = var2 && var1 != null && ChildZombiePolicy.canRemoveCorpseItem(true, var1.getCategory(), var1.getType());
         }
      }
   }

   @Patch(warmUp = true, className = "zombie.characters.IsoZombie", methodName = "shouldZombieHaveKey")
   public static final class ShouldZombieHaveKey {
      @OnExit
      public static void exit(@Return(readOnly = false) boolean var0, @This IsoZombie var1) {
         var0 = ChildZombiePolicy.canHaveBuildingKey(ChildIdentity.isZombieChild(var1), var0);
      }
   }
}
