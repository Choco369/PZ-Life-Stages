package pz.playablechildren;

/** Extra endurance recovery applies only while an elder actually sits. */
public final class ElderSittingPolicy {
    private static final String ROCKING_PREFIX = "furniture_seating_indoor_03_";

    private ElderSittingPolicy() {}

    public static float multiplier(LifeStage stage, boolean seated, String spriteName) {
        if (stage != LifeStage.ELDER || !AgeGameplayApi.effectsEnabled(stage) || !seated) return 1.0f;
        if (spriteName != null && spriteName.startsWith(ROCKING_PREFIX)) {
            try {
                int suffix = Integer.parseInt(spriteName.substring(ROCKING_PREFIX.length()));
                if (suffix >= 144 && suffix <= 151) return 1.25f;
            } catch (NumberFormatException ignored) {
                // Not one of the vanilla rocking-chair sprite IDs.
            }
        }
        return 1.15f;
    }

    public static float extraRecovery(float before, float after, float multiplier) {
        if (after <= before || multiplier <= 1.0f) return 0.0f;
        return (after - before) * (multiplier - 1.0f);
    }
}
