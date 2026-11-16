package ae2desktopping.client;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class TextFormat {

    private TextFormat() {}

    /** Same look as AE2's chat message (HH:mm:ss), but hours don't wrap at 24. */
    public static String formatElapsed(long elapsedNanos) {
        final long totalSeconds = Math.max(0L, TimeUnit.NANOSECONDS.toSeconds(elapsedNanos));
        final long hours = totalSeconds / 3600L;
        final long minutes = (totalSeconds / 60L) % 60L;
        final long seconds = totalSeconds % 60L;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    /** 250000 -> "250,000". */
    public static String formatCount(long count) {
        return String.format(Locale.US, "%,d", count);
    }
}
