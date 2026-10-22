package pz.playablechildren;

import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.*;
import zombie.characters.*;
import zombie.characters.BodyDamage.BodyPart;
import zombie.vehicles.BaseVehicle;

/** Native accident branches retain all vanilla guards and other random decisions. */
public final class PatchElderFractures {
    private PatchElderFractures() {}

    @Patch(className="zombie.characters.IsoGameCharacter",methodName="handleLandingImpact",strictMatch=true)
    public static final class Landing {
        @OnEnter public static void enter(@This IsoGameCharacter character,@Argument(0) FallDamage fall) {
            ElderFractureScope.enter(character,"zombie.characters.IsoGameCharacter","handleLandingImpact");
        }
        @OnExit(onThrowable=Throwable.class) public static void exit(@This IsoGameCharacter character,@Argument(0) FallDamage fall) { ElderFractureScope.exit(); }
    }

    @Patch(className="zombie.characters.IsoPlayer",methodName="applyDamageFromVehicleHit",strictMatch=true)
    public static final class VehicleHit {
        @OnEnter public static void enter(@This IsoPlayer player,@Argument(0) BaseVehicle vehicle,@Argument(1) float speed,@Argument(2) float damage) {
            ElderFractureScope.enter(player,"zombie.characters.IsoPlayer","applyDamageFromVehicleHit");
        }
        @OnExit(onThrowable=Throwable.class) public static void exit(@This IsoPlayer player,@Argument(0) BaseVehicle vehicle,@Argument(1) float speed,@Argument(2) float damage) { ElderFractureScope.exit(); }
    }

    @Patch(className="zombie.vehicles.BaseVehicle",methodName="addRandomDamageFromCrash",strictMatch=true)
    public static final class Crash {
        @OnEnter public static void enter(@This BaseVehicle vehicle,@Argument(0) IsoGameCharacter character,@Argument(1) float damage) {
            ElderFractureScope.enter(character,"zombie.vehicles.BaseVehicle","addRandomDamageFromCrash");
        }
        @OnExit(onThrowable=Throwable.class) public static void exit(@This BaseVehicle vehicle,@Argument(0) IsoGameCharacter character,@Argument(1) float damage) { ElderFractureScope.exit(); }
    }

    @Patch(className="zombie.core.random.Rand",methodName="Next",strictMatch=true)
    public static final class FractureRoll {
        @OnEnter public static void enter(@Argument(value=0,readOnly=false) int bound) { bound=ElderFractureScope.rollBound(bound); }
    }

    @Patch(className="zombie.characters.BodyDamage.BodyPart",methodName="AddDamage",strictMatch=true)
    public static final class CrashPart {
        @OnEnter public static void enter(@This BodyPart part,@Argument(0) float damage) { ElderFractureScope.rememberCrashPart(part); }
    }
}
