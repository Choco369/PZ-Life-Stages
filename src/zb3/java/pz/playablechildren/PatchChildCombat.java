package pz.playablechildren;

import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.Argument;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.Return;
import me.zed_0xff.zombie_buddy.annotations.Patch.This;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.inventory.types.HandWeapon;

public final class PatchChildCombat {
    private PatchChildCombat() {}

    @Patch(className = "zombie.characters.IsoPlayer",
           methodName = "calculateCritChance", strictMatch = true)
    public static final class ShoveChance {
        @OnExit
        public static void exit(@This IsoPlayer player,
                                @Argument(0) IsoGameCharacter target,
                                @Return(readOnly = false) int result) {
            result = ChildCombatPolicy.shoveChance(result, player, target);
        }
    }

    @Patch(className = "zombie.characters.IsoGameCharacter",
           methodName = "processHitDamage", strictMatch = true)
    public static final class ShoveForce {
        @OnExit
        public static void exit(@This IsoGameCharacter target,
                                @Argument(0) HandWeapon weapon,
                                @Argument(1) IsoGameCharacter attacker) {
            ChildCombatPolicy.scaleShoveForce(target, attacker, weapon);
        }
    }

    @Patch(className = "zombie.characters.IsoGameCharacter",
           methodName = "getSurroundingAttackingZombies", strictMatch = true)
    public static final class DragDown {
        @OnExit
        public static void exit(@This IsoGameCharacter character,
                                @Argument(0) boolean includeCrawlers,
                                @Return(readOnly = false) int result) {
            result = ChildCombatPolicy.dragDownCount(character, result);
        }
    }
}
