package ae2desktopping.client;

import java.util.concurrent.TimeUnit;

public final class TimeFormat {

    private TimeFormat() {}

    /** Same look as AE2's chat message (HH:mm:ss), but hours don't wrap at 24. */
    public static String formatElapsed(long elapsedNanos) {
        final long totalSeconds = Math.max(0L, TimeUnit.NANOSECONDS.toSeconds(elapsedNanos));
        final long hours = totalSeconds / 3600L;
        final long minutes = (totalSeconds / 60L) % 60L;
        final long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
