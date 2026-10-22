package pz.playablechildren;

/** Single source of truth for the two persistent life-stage bits. */
public final class StageBits {
    public static final int MASK = 0x60000000;
    public static final int TEEN = 0x20000000;
    public static final int CHILD = 0x40000000;
    public static final int ELDER = 0x60000000;

    private StageBits() {}

    public static int bitsFor(LifeStage stage) {
        return switch (stage == null ? LifeStage.ADULT : stage) {
            case TEEN -> TEEN;
            case CHILD -> CHILD;
            case ELDER -> ELDER;
            default -> 0;
        };
    }

    public static int withStage(int id, LifeStage stage) {
        return (id & ~MASK) | bitsFor(stage);
    }

    public static LifeStage stageOf(int id) {
        return switch (id & MASK) {
            case TEEN -> LifeStage.TEEN;
            case CHILD -> LifeStage.CHILD;
            case ELDER -> LifeStage.ELDER;
            default -> LifeStage.ADULT;
        };
    }

    public static int strip(int id) { return id & ~MASK; }
    public static int copy(int cleanResult, int markedSource) { return withStage(cleanResult, stageOf(markedSource)); }
    public static boolean isChild(int id) { return (id & MASK) == CHILD; }
    public static boolean isTeen(int id) { return (id & MASK) == TEEN; }
}
