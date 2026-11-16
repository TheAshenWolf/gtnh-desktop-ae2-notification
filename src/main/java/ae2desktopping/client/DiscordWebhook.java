package ae2desktopping.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import ae2desktopping.AE2DesktopPing;
import ae2desktopping.Tags;

/**
 * Posts to a Discord webhook on one background thread.
 * <ul>
 * <li>Only https URLs on Discord's own hosts with the webhook path are accepted; redirects are not followed.</li>
 * <li>allowed_mentions lists exactly who may be pinged, so text inside item names can't ping anyone.</li>
 * <li>The URL is never logged.</li>
 * </ul>
 */
public final class DiscordWebhook {

    private static final String[] ALLOWED_HOSTS = { "discord.com", "discordapp.com", "canary.discord.com",
        "ptb.discord.com" };
    private static final Pattern WEBHOOK_PATH = Pattern
        .compile("^/api(/v\\d{1,2})?/webhooks/\\d{17,20}/[A-Za-z0-9_-]{20,200}$");
    private static final Pattern USER_ID = Pattern.compile("^\\d{17,20}$");

    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int READ_TIMEOUT_MILLIS = 10000;
    private static final long MIN_MILLIS_BETWEEN_POSTS = 2000L;
    private static final long MAX_RETRY_AFTER_MILLIS = 30000L;
    private static final int MAX_QUEUED_POSTS = 16;
    private static final int MAX_ITEM_NAME_LENGTH = 100;

    private static final ThreadPoolExecutor WORKER = new ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<Runnable>(MAX_QUEUED_POSTS),
        runnable -> {
            final Thread thread = new Thread(runnable, "AE2DesktopPing-Discord");
            thread.setDaemon(true);
            return thread;
        },
        (runnable, executor) -> AE2DesktopPing.LOG.error("Too many Discord pings queued; dropped one."));

    private static long lastPostMillis;

    private DiscordWebhook() {}

    /** Returns the trimmed URL when it is a Discord webhook URL, otherwise null. */
    public static String validateWebhookUrl(String raw) {
        if (raw == null) {
            return null;
        }

        final String trimmed = raw.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        final URI uri;

        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException exception) {
            return null;
        }

        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getRawUserInfo() != null
            || uri.getRawQuery() != null
            || uri.getRawFragment() != null
            || (uri.getPort() != -1 && uri.getPort() != 443)
            || uri.getHost() == null
            || uri.getRawPath() == null) {
            return null;
        }

        final String host = uri.getHost()
            .toLowerCase(Locale.ROOT);
        boolean hostAllowed = false;

        for (int i = 0, count = ALLOWED_HOSTS.length; i < count; i++) {
            if (ALLOWED_HOSTS[i].equals(host)) {
                hostAllowed = true;
                break;
            }
        }

        if (!hostAllowed || !WEBHOOK_PATH.matcher(uri.getRawPath())
            .matches()) {
            return null;
        }

        return trimmed;
    }

    public static boolean isValidUserId(String raw) {
        return raw != null && USER_ID.matcher(raw.trim())
            .matches();
    }

    /** Queues a ping for a finished craft. Call on the client thread; the post happens on the worker. */
    public static void sendCraftFinished(String itemName, long count, long elapsedNanos, boolean pingHere) {
        if (!PingConfig.isDiscordReady()) {
            return;
        }

        final String mentions = pingHere ? "@here <@" + PingConfig.userId + ">" : "<@" + PingConfig.userId + ">";
        final String content = mentions + " AE2 craft finished: **"
            + TextFormat.formatCount(count)
            + " × "
            + sanitize(itemName)
            + "** in "
            + TextFormat.formatElapsed(elapsedNanos)
            + ".";

        queue(PingConfig.webhookUrl, buildPayload(content, PingConfig.userId, pingHere));
    }

    /** Queues a test message from /ae2ping test. Pings only you, never @here. */
    public static boolean sendTest() {
        if (!PingConfig.isDiscordReady()) {
            return false;
        }

        final String content = "<@" + PingConfig.userId + "> AE2 Desktop Ping test: the webhook works.";
        queue(PingConfig.webhookUrl, buildPayload(content, PingConfig.userId, false));
        return true;
    }

    private static void queue(String url, String payload) {
        // The URL and payload are captured now, so a config reload can't change a post halfway.
        WORKER.execute(() -> post(url, payload));
    }

    private static String buildPayload(String content, String userId, boolean pingHere) {
        final JsonObject allowedMentions = new JsonObject();
        final JsonArray parse = new JsonArray();

        if (pingHere) {
            // "everyone" is the parse type that covers @here; @everyone itself never appears in the content.
            parse.add(new JsonPrimitive("everyone"));
        }

        allowedMentions.add("parse", parse);

        final JsonArray users = new JsonArray();
        users.add(new JsonPrimitive(userId));
        allowedMentions.add("users", users);

        final JsonObject root = new JsonObject();
        root.addProperty("content", content);
        root.add("allowed_mentions", allowedMentions);
        return root.toString();
    }

    /** Item names come from the server (and anvils): strip formatting, break mentions, escape markdown. */
    static String sanitize(String raw) {
        if (raw == null) {
            return "?";
        }

        final StringBuilder builder = new StringBuilder();
        boolean skipNext = false;

        for (int i = 0, length = raw.length(); i < length; i++) {
            final char character = raw.charAt(i);

            if (skipNext) {
                skipNext = false;
                continue;
            }

            if (character == '§') {
                // Minecraft formatting code: drop it and its argument.
                skipNext = true;
                continue;
            }

            if (Character.isISOControl(character)) {
                continue;
            }

            if ("\\*_~`|[]()<:".indexOf(character) >= 0) {
                builder.append('\\');
            }

            builder.append(character);

            if (character == '@') {
                // Zero-width space: "@everyone" in a renamed item stays plain text.
                builder.append('​');
            }

            if (builder.length() >= MAX_ITEM_NAME_LENGTH) {
                builder.append('…');
                break;
            }
        }

        return builder.length() == 0 ? "?" : builder.toString();
    }

    private static void post(String url, String payload) {
        try {
            final long waitMillis = lastPostMillis + MIN_MILLIS_BETWEEN_POSTS - System.currentTimeMillis();

            if (waitMillis > 0) {
                Thread.sleep(waitMillis);
            }

            int status = postOnce(url, payload);

            if (status == 429) {
                Thread.sleep(MAX_RETRY_AFTER_MILLIS);
                status = postOnce(url, payload);
            }

            lastPostMillis = System.currentTimeMillis();

            if (status < 200 || status >= 300) {
                AE2DesktopPing.LOG.error("Discord webhook answered HTTP " + status + "; the ping was not delivered.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread()
                .interrupt();
        } catch (IOException exception) {
            // The exception text can include the URL, so only its type is logged.
            AE2DesktopPing.LOG.error(
                "Could not reach Discord (" + exception.getClass()
                    .getSimpleName() + "); the ping was not delivered.");
        } catch (RuntimeException exception) {
            AE2DesktopPing.LOG.error(
                "Discord ping failed (" + exception.getClass()
                    .getSimpleName() + ").");
        }
    }

    private static int postOnce(String url, String payload) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        if (!(connection instanceof HttpsURLConnection)) {
            connection.disconnect();
            throw new IOException("not https");
        }

        try {
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            // Discord rejects Java's default user agent.
            connection.setRequestProperty("User-Agent", "AE2DesktopPing/" + Tags.VERSION);

            final byte[] body = payload.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }

            final int status = connection.getResponseCode();
            drain(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
            return status;
        } finally {
            connection.disconnect();
        }
    }

    private static void drain(InputStream stream) throws IOException {
        if (stream == null) {
            return;
        }

        try (InputStream input = stream) {
            final byte[] buffer = new byte[1024];

            while (input.read(buffer) != -1) {
                // Discard the body; it is never logged.
            }
        }
    }
}
