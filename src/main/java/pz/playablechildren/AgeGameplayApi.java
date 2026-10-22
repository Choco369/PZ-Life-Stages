package pz.playablechildren;

import java.lang.reflect.Field;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import zombie.characters.IsoGameCharacter;
import zombie.characters.CharacterStat;
import zombie.characters.Stats;
import zombie.characters.skills.PerkFactory;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;

/** The live lifecycle context used by hooks; no permanent vanilla stat changes. */
public final class AgeGameplayApi {
    private record Context(LifeStage stage, float progress) {}
    private static final Map<IsoGameCharacter, Context> CONTEXTS =
        Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Stats, WeakReference<IsoGameCharacter>> STAT_OWNERS =
        Collections.synchronizedMap(new WeakHashMap<>());
    private static final Field XP_CHARACTER = findXpCharacter();

    private AgeGameplayApi() {}

    private static Field findXpCharacter() {
        try {
            Field field = IsoGameCharacter.XP.class.getDeclaredField("chr");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    public static boolean setContext(Object value, String stageName, double progress) {
        if (!(value instanceof IsoGameCharacter character) || stageName == null) return false;
        try {
            LifeStage stage = LifeStage.valueOf(stageName.trim().toUpperCase(java.util.Locale.ROOT));
            float ratio = Double.isFinite(progress) ? (float) progress : 0.5f;
            CONTEXTS.put(character, new Context(stage, ratio));
            STAT_OWNERS.put(character.getStats(), new WeakReference<>(character));
            PlayerLifeStages.remember(character, stage);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public static LifeStage stageOf(IsoGameCharacter character) {
        Context context = character == null ? null : CONTEXTS.get(character);
        return context == null ? PlayerLifeStages.stageOf(character) : context.stage();
    }

    public static float progressOf(IsoGameCharacter character) {
        Context context = character == null ? null : CONTEXTS.get(character);
        return context == null ? 0.5f : context.progress();
    }

    public static float factor(LifeStage stage, float progress, AgeProfile.Effect effect) {
        return effectsEnabled(stage) ? AgeProfile.value(stage, progress, effect) : 1.0f;
    }

    public static boolean effectsEnabled(LifeStage stage) {
        if (stage == null || stage == LifeStage.ADULT || LuaManager.env == null) return true;
        Object vars = LuaManager.env.rawget("SandboxVars");
        if (!(vars instanceof KahluaTable sandbox)
            || !(sandbox.rawget("PlayableChildren") instanceof KahluaTable options)) return true;
        String option = switch (stage) {
            case CHILD -> "EnableChildAgeEffects";
            case TEEN -> "EnableTeenAgeEffects";
            case ELDER -> "EnableElderAgeEffects";
            default -> null;
        };
        return option == null || !Boolean.FALSE.equals(options.rawget(option));
    }

    public static float factor(IsoGameCharacter character, AgeProfile.Effect effect) {
        return factor(stageOf(character), progressOf(character), effect);
    }

    public static int scaleCarry(int vanilla, LifeStage stage, float progress) {
        if (vanilla <= 0) return vanilla;
        float multiplier = factor(stage, progress, AgeProfile.Effect.CARRY);
        return multiplier == 1.0f ? vanilla : Math.max(1, Math.round(vanilla * multiplier));
    }

    public static float scaleXp(float vanilla, LifeStage stage, float progress, boolean fitness) {
        if (!Float.isFinite(vanilla) || vanilla <= 0.0f) return vanilla;
        float factor = factor(stage, progress, AgeProfile.Effect.GENERAL_XP);
        if (fitness) factor *= factor(stage, progress, AgeProfile.Effect.STRENGTH_FITNESS_XP);
        return vanilla * factor;
    }

    public static float scaleStat(float delta, LifeStage stage, float progress,
                                  CharacterStat stat) {
        if (!Float.isFinite(delta) || delta == 0.0f || stat == null) return delta;
        AgeProfile.Effect effect = null;
        if (delta > 0.0f) {
            if (stat == CharacterStat.PANIC) effect = AgeProfile.Effect.PANIC_GAIN;
            else if (stat == CharacterStat.BOREDOM) effect = AgeProfile.Effect.BOREDOM_GAIN;
            else if (stat == CharacterStat.FATIGUE) effect = AgeProfile.Effect.FATIGUE_GAIN;
            else if (stat == CharacterStat.HUNGER) effect = AgeProfile.Effect.HUNGER_GAIN;
            else if (stat == CharacterStat.INTOXICATION) effect = AgeProfile.Effect.ALCOHOL_INTENSITY;
        } else {
            if (stat == CharacterStat.ENDURANCE) effect = AgeProfile.Effect.ENDURANCE_USE;
            else if (stat == CharacterStat.PANIC) effect = AgeProfile.Effect.PANIC_RECOVERY;
        }
        return effect == null ? delta : delta * factor(stage, progress, effect);
    }

    public static float scaleStat(float delta, Stats stats, CharacterStat stat) {
        WeakReference<IsoGameCharacter> reference =
            stats == null ? null : STAT_OWNERS.get(stats);
        IsoGameCharacter character = reference == null ? null : reference.get();
        return character == null ? delta : scaleStat(
            delta, stageOf(character), progressOf(character), stat);
    }

    public static float scaleMelee(float vanilla, LifeStage stage, float progress,
                                   boolean melee) {
        if (!melee || !Float.isFinite(vanilla) || vanilla <= 0.0f) return vanilla;
        return vanilla * factor(stage, progress, AgeProfile.Effect.MELEE_DAMAGE);
    }

    public static float scaleHealing(float vanilla, LifeStage stage, float progress) {
        if (!Float.isFinite(vanilla) || vanilla <= 0.0f) return vanilla;
        return vanilla * factor(stage, progress, AgeProfile.Effect.WOUND_HEALING);
    }

    /** Conditional retention of a vanilla fracture event, not a rewrite of other injuries. */
    public static boolean keepFracture(LifeStage stage, float progress, float sample) {
        if (stage != LifeStage.CHILD) return true;
        return sample < factor(stage, progress, AgeProfile.Effect.FRACTURE_CHANCE);
    }

    public static float scaleIllnessAccumulation(float before, float vanillaAfter,
                                                  LifeStage stage, float progress) {
        if (!Float.isFinite(before) || !Float.isFinite(vanillaAfter)
            || vanillaAfter <= before) return vanillaAfter;
        return before + (vanillaAfter - before)
            * factor(stage, progress, AgeProfile.Effect.ILLNESS_CHANCE);
    }

    public static float expertiseFactor(LifeStage stage, PerkFactory.Perk perk, int level) {
        if (stage != LifeStage.ELDER || !effectsEnabled(stage) || perk == null || level < 7) return 1.0f;
        if (perk == PerkFactory.Perks.Cooking || perk == PerkFactory.Perks.Doctor
            || perk == PerkFactory.Perks.Farming
            || perk == PerkFactory.Perks.PlantScavenging
            || perk == PerkFactory.Perks.Woodwork
            || perk == PerkFactory.Perks.Mechanics
            || perk == PerkFactory.Perks.Maintenance
            || perk == PerkFactory.Perks.Tailoring) return 1.10f;
        return 1.0f;
    }

    public static IsoGameCharacter xpCharacter(IsoGameCharacter.XP xp) {
        if (xp == null || XP_CHARACTER == null) return null;
        try {
            return (IsoGameCharacter) XP_CHARACTER.get(xp);
        } catch (IllegalAccessException | ClassCastException ignored) {
            return null;
        }
    }
}
