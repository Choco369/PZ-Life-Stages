package pz.playablechildren;

/** PZ Life Stages geometry policy. Combat rules remain owned by vanilla.
 * Packed native records contain three position floats and four rotation floats.
 */
public final class TargetGeometry {
    private TargetGeometry() {}

    public static void adjustPackedPositions(LifeStage stage, float[] records, int count) {
        if (stage != LifeStage.CHILD || records == null || count <= 0) return;
        int complete = Math.min(count, records.length / 7);
        float scale = LifeStageResolver.scaleFor(stage);
        for (int bone = 0; bone < complete; bone++) {
            int position = bone * 7;
            for (int axis = 0; axis < 3; axis++) records[position + axis] *= scale;
        }
    }

    public static boolean omitOutlineAttachment(LifeStage stage, boolean rigid, String shader) {
        return stage == LifeStage.CHILD && rigid && "aim_outline_solid".equals(shader);
    }
}
