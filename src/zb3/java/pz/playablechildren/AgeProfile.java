package pz.playablechildren;

/** Immutable age multipliers; vanilla values are multiplied only at the use site. */
public final class AgeProfile {
    public enum Effect {
        CARRY, ENDURANCE_USE, MELEE_DAMAGE, GENERAL_XP, READING_SPEED,
        WOUND_HEALING, FRACTURE_CHANCE, ILLNESS_CHANCE, PANIC_GAIN,
        ALCOHOL_INTENSITY, NOISE, PANIC_RECOVERY, BOREDOM_GAIN,
        FATIGUE_GAIN, STRENGTH_FITNESS_XP, HUNGER_GAIN, HEARING, VISION,
        DRIVING_SPEED
    }

    private static final float[][][] VALUES =
        new float[LifeStage.values().length][Effect.values().length][2];

    static {
        for (float[][] stage : VALUES) {
            for (float[] pair : stage) {
                pair[0] = 1.0f;
                pair[1] = 1.0f;
            }
        }
        set(LifeStage.CHILD, Effect.CARRY, .70f, .85f);
        set(LifeStage.CHILD, Effect.ENDURANCE_USE, 1.25f, 1.10f);
        set(LifeStage.CHILD, Effect.MELEE_DAMAGE, .75f, .90f);
        set(LifeStage.CHILD, Effect.GENERAL_XP, 1.25f, 1.15f);
        set(LifeStage.CHILD, Effect.READING_SPEED, .70f, .85f);
        set(LifeStage.CHILD, Effect.WOUND_HEALING, 1.15f, 1.10f);
        set(LifeStage.CHILD, Effect.FRACTURE_CHANCE, .65f, .80f);
        set(LifeStage.CHILD, Effect.ILLNESS_CHANCE, 1.40f, 1.20f);
        set(LifeStage.CHILD, Effect.PANIC_GAIN, 1.40f, 1.20f);
        set(LifeStage.CHILD, Effect.ALCOHOL_INTENSITY, 2.00f, 1.60f);
        set(LifeStage.CHILD, Effect.NOISE, .85f, .92f);
        set(LifeStage.CHILD, Effect.PANIC_RECOVERY, .80f, .90f);
        set(LifeStage.CHILD, Effect.BOREDOM_GAIN, 1.25f, 1.15f);
        set(LifeStage.CHILD, Effect.FATIGUE_GAIN, 1.20f, 1.10f);

        set(LifeStage.TEEN, Effect.CARRY, .85f, 1.00f);
        set(LifeStage.TEEN, Effect.ENDURANCE_USE, 1.12f, 1.00f);
        set(LifeStage.TEEN, Effect.MELEE_DAMAGE, .90f, 1.00f);
        set(LifeStage.TEEN, Effect.GENERAL_XP, 1.15f, 1.05f);
        set(LifeStage.TEEN, Effect.READING_SPEED, .95f, 1.00f);
        set(LifeStage.TEEN, Effect.WOUND_HEALING, 1.10f, 1.00f);
        set(LifeStage.TEEN, Effect.ILLNESS_CHANCE, 1.10f, 1.00f);
        set(LifeStage.TEEN, Effect.PANIC_GAIN, 1.15f, 1.00f);
        set(LifeStage.TEEN, Effect.ALCOHOL_INTENSITY, 1.40f, 1.10f);
        set(LifeStage.TEEN, Effect.STRENGTH_FITNESS_XP, 1.30f, 1.10f);
        set(LifeStage.TEEN, Effect.HUNGER_GAIN, 1.20f, 1.05f);
        set(LifeStage.TEEN, Effect.FATIGUE_GAIN, 1.15f, 1.00f);
        set(LifeStage.TEEN, Effect.BOREDOM_GAIN, 1.20f, 1.10f);
        set(LifeStage.TEEN, Effect.DRIVING_SPEED, .85f, 1.00f);

        set(LifeStage.ELDER, Effect.CARRY, .90f, .75f);
        set(LifeStage.ELDER, Effect.ENDURANCE_USE, 1.10f, 1.25f);
        set(LifeStage.ELDER, Effect.MELEE_DAMAGE, .95f, .85f);
        set(LifeStage.ELDER, Effect.GENERAL_XP, .90f, .75f);
        set(LifeStage.ELDER, Effect.READING_SPEED, .90f, .80f);
        set(LifeStage.ELDER, Effect.WOUND_HEALING, .85f, .65f);
        set(LifeStage.ELDER, Effect.FRACTURE_CHANCE, 1.25f, 1.60f);
        set(LifeStage.ELDER, Effect.ILLNESS_CHANCE, 1.20f, 1.50f);
        set(LifeStage.ELDER, Effect.PANIC_GAIN, .90f, .90f);
        set(LifeStage.ELDER, Effect.ALCOHOL_INTENSITY, 1.30f, 1.70f);
        set(LifeStage.ELDER, Effect.HEARING, .90f, .75f);
        set(LifeStage.ELDER, Effect.VISION, .95f, .85f);
        set(LifeStage.ELDER, Effect.FATIGUE_GAIN, 1.10f, 1.25f);
        set(LifeStage.ELDER, Effect.HUNGER_GAIN, .90f, .90f);
    }

    private AgeProfile() {}

    private static void set(LifeStage stage, Effect effect, float start, float end) {
        VALUES[stage.ordinal()][effect.ordinal()][0] = start;
        VALUES[stage.ordinal()][effect.ordinal()][1] = end;
    }

    public static float value(LifeStage stage, float progress, Effect effect) {
        if (stage == null || effect == null) return 1.0f;
        if (Float.isNaN(progress)) progress = 0.5f;
        if (progress < 0.0f) progress = 0.0f;
        if (progress > 1.0f) progress = 1.0f;
        float[] pair = VALUES[stage.ordinal()][effect.ordinal()];
        return pair[0] + (pair[1] - pair[0]) * progress;
    }
}
