package pz.playablechildren;

/** Tracks one uninterrupted play of the supplied 61.5-second recording. */
public final class DMVPlaybackSession {
    public enum Status { WAITING, PLAYING, COMPLETE, INTERRUPTED }

    private static final long MINIMUM_FULL_PLAY_MILLIS = 61_000L;
    private static final long ACCELERATED_DURATION_MILLIS = 61_500L;
    private static final long MAX_OBSERVED_GAP_MILLIS = 1_000L;
    private long lastObservedMillis;
    private long audibleMillis;
    private double simulatedMillis;
    private boolean heardSound;
    private boolean accelerated;
    private Status status = Status.WAITING;

    public DMVPlaybackSession(long startedAtMillis) {
        this.lastObservedMillis = startedAtMillis;
    }

    public Status observe(long nowMillis, boolean soundPlaying, float gameSpeed) {
        if (status == Status.COMPLETE || status == Status.INTERRUPTED) return status;
        if (soundPlaying) {
            if (!heardSound) {
                heardSound = true;
                if (gameSpeed > 1.01f) accelerated = true;
                lastObservedMillis = nowMillis;
                status = Status.PLAYING;
                return status;
            }
            advance(nowMillis, gameSpeed);
            if (accelerated && simulatedMillis >= ACCELERATED_DURATION_MILLIS) {
                status = Status.COMPLETE;
                return status;
            }
            status = Status.PLAYING;
        } else if (heardSound) {
            advance(nowMillis, gameSpeed);
            status = audibleMillis >= MINIMUM_FULL_PLAY_MILLIS
                || accelerated && simulatedMillis >= ACCELERATED_DURATION_MILLIS
                ? Status.COMPLETE : Status.INTERRUPTED;
        }
        return status;
    }

    private void advance(long nowMillis, float gameSpeed) {
        long elapsed = Math.min(MAX_OBSERVED_GAP_MILLIS,
            Math.max(0L, nowMillis - lastObservedMillis));
        lastObservedMillis = nowMillis;
        float speed = Float.isFinite(gameSpeed)
            ? Math.max(0.0f, Math.min(40.0f, gameSpeed)) : 0.0f;
        if (speed > 1.01f) accelerated = true;
        audibleMillis += elapsed;
        simulatedMillis += elapsed * speed;
    }

    public boolean playsForCourse(boolean soundPlaying, float gameSpeed) {
        return soundPlaying || gameSpeed > 1.01f || accelerated;
    }

    public boolean shouldHoldFinalLine(boolean atFinalLine, boolean soundPlaying) {
        return atFinalLine && soundPlaying && status == Status.PLAYING;
    }
}
