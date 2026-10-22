package pz.playablechildren;

import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.Argument;
import me.zed_0xff.zombie_buddy.Patch.OnEnter;
import me.zed_0xff.zombie_buddy.Patch.OnExit;
import me.zed_0xff.zombie_buddy.Patch.Return;
import me.zed_0xff.zombie_buddy.Patch.This;
import zombie.characters.IsoGameCharacter;
import zombie.characters.CharacterStat;
import zombie.characters.Stats;
import zombie.characters.skills.PerkFactory;
import zombie.vehicles.BaseVehicle;
import zombie.inventory.types.HandWeapon;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.core.random.Rand;

/** Multipliers are applied to each vanilla operation, never stored as traits. */
public final class PatchAgeEffects {
    private static final ThreadLocal<Float> COLD_BEFORE = new ThreadLocal<>();

    private PatchAgeEffects() {}

    public static void beforeCold(BodyDamage damage) {
        COLD_BEFORE.set(damage.getCatchACold());
    }

    public static void afterCold(BodyDamage damage) {
        Float before = COLD_BEFORE.get();
        COLD_BEFORE.remove();
        if (before == null) return;
        IsoGameCharacter character = damage.getParentChar();
        float vanillaAfter = damage.getCatchACold();
        float scaled = AgeGameplayApi.scaleIllnessAccumulation(
            before, vanillaAfter, AgeGameplayApi.stageOf(character),
            AgeGameplayApi.progressOf(character));
        if (scaled != vanillaAfter) damage.setCatchACold(scaled);
    }

    @Patch(className = "zombie.characters.IsoGameCharacter",
           methodName = "getMaxWeight", strictMatch = true)
    public static final class MaxCarry {
        @OnExit
        public static void exit(@This IsoGameCharacter character,
                                @Return(readOnly = false) int result) {
            result = AgeGameplayApi.scaleCarry(
                result, AgeGameplayApi.stageOf(character),
                AgeGameplayApi.progressOf(character));
        }
    }

    @Patch(className = "zombie.characters.IsoGameCharacter$XP",
           methodName = "AddXP", strictMatch = true)
    public static final class XpGain {
        @OnEnter
        public static void enter(@This IsoGameCharacter.XP xp,
                                 @Argument(0) PerkFactory.Perk perk,
                                 @Argument(value = 1, readOnly = false) float amount,
                                 @Argument(2) boolean applyMultiplier,
                                 @Argument(3) boolean isLocal,
                                 @Argument(4) boolean isForced,
                                 @Argument(5) boolean remote) {
            IsoGameCharacter character = AgeGameplayApi.xpCharacter(xp);
            if (character == null) return;
            boolean physical = perk == PerkFactory.Perks.Fitness
                || perk == PerkFactory.Perks.Strength;
            LifeStage stage = AgeGameplayApi.stageOf(character);
            float expertise = Float.isFinite(amount) && amount > 0.0f && perk != null
                ? AgeGameplayApi.expertiseFactor(stage, perk, character.getPerkLevel(perk))
                : 1.0f;
            // Mastered elder skills are 10% above adult, not above reduced elder XP.
            amount = expertise > 1.0f ? amount * expertise
                : AgeGameplayApi.scaleXp(amount, stage,
                    AgeGameplayApi.progressOf(character), physical);
        }
    }

    // Stats.remove delegates to add with a negative value. Patching add only
    // scales both signs once and leaves direct set/reset values untouched.
    @Patch(className = "zombie.characters.Stats",
           methodName = "add", strictMatch = true)
    public static final class StatDelta {
        @OnEnter
        public static void enter(@This Stats stats,
                                 @Argument(0) CharacterStat stat,
                                 @Argument(value = 1, readOnly = false) float delta) {
            delta = AgeGameplayApi.scaleStat(delta, stats, stat);
        }
    }

    @Patch(className = "zombie.vehicles.BaseVehicle",
           methodName = "getMaxSpeed", strictMatch = true)
    public static final class TeenDrivingSpeed {
        @OnExit
        public static void exit(@This BaseVehicle vehicle,
                                @Return(readOnly = false) float result) {
            IsoGameCharacter driver = vehicle.getDriver();
            if (AgeGameplayApi.stageOf(driver) == LifeStage.TEEN) {
                result *= AgeGameplayApi.factor(driver, AgeProfile.Effect.DRIVING_SPEED);
            }
        }
    }

    @Patch(className = "zombie.characters.IsoGameCharacter",
           methodName = "processHitDamage", strictMatch = true)
    public static final class MeleeDamage {
        @OnExit
        public static void exit(@Argument(0) HandWeapon weapon,
                                @Argument(1) IsoGameCharacter attacker,
                                @Return(readOnly = false) float result) {
            boolean melee = weapon != null && !weapon.isRanged() && !weapon.isExplosive();
            result = AgeGameplayApi.scaleMelee(result,
                AgeGameplayApi.stageOf(attacker),
                AgeGameplayApi.progressOf(attacker), melee);
        }
    }

    @Patch(className = "zombie.characters.BodyDamage.BodyPart",
           methodName = "AddHealth", strictMatch = true)
    public static final class WoundHealing {
        @OnEnter
        public static void enter(@This BodyPart part,
                                 @Argument(value = 0, readOnly = false) float amount) {
            IsoGameCharacter character = part.getParentChar();
            amount = AgeGameplayApi.scaleHealing(amount,
                AgeGameplayApi.stageOf(character),
                AgeGameplayApi.progressOf(character));
        }
    }

    @Patch(className = "zombie.characters.BodyDamage.BodyPart",
           methodName = "generateFractureNew", strictMatch = true)
    public static final class ChildFractureRisk {
        @OnEnter(skipOn = true)
        public static boolean enter(@This BodyPart part) {
            IsoGameCharacter character = part.getParentChar();
            if (AgeGameplayApi.stageOf(character) != LifeStage.CHILD
                || !AgeGameplayApi.effectsEnabled(LifeStage.CHILD)) return false;
            float sample = Rand.Next(10000) / 10000.0f;
            return !AgeGameplayApi.keepFracture(
                LifeStage.CHILD, AgeGameplayApi.progressOf(character), sample);
        }
    }

    @Patch(className = "zombie.characters.BodyDamage.BodyDamage",
           methodName = "UpdateCold", strictMatch = true)
    public static final class ColdAccumulation {
        @OnEnter
        public static void enter(@This BodyDamage damage) {
            PatchAgeEffects.beforeCold(damage);
        }

        @OnExit
        public static void exit(@This BodyDamage damage) {
            PatchAgeEffects.afterCold(damage);
        }
    }

    @Patch(className = "zombie.characters.IsoGameCharacter",
           methodName = "getHearDistanceModifier", strictMatch = true)
    public static final class ElderHearing {
        @OnExit
        public static void exit(@This IsoGameCharacter character,
                                @Return(readOnly = false) float result) {
            if (AgeGameplayApi.stageOf(character) == LifeStage.ELDER) {
                result *= AgeGameplayApi.factor(character, AgeProfile.Effect.HEARING);
            }
        }
    }

    @Patch(className = "zombie.characters.IsoGameCharacter",
           methodName = "getWornItemsVisionMultiplier", strictMatch = true)
    public static final class ElderVision {
        @OnExit
        public static void exit(@This IsoGameCharacter character,
                                @Return(readOnly = false) float result) {
            if (AgeGameplayApi.stageOf(character) == LifeStage.ELDER) {
                result *= AgeGameplayApi.factor(character, AgeProfile.Effect.VISION);
            }
        }
    }
}
