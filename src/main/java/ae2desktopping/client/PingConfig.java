package ae2desktopping.client;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import ae2desktopping.AE2DesktopPing;

/** config/ae2desktopping.cfg. The webhook URL is a secret: anyone holding it can post to that channel. */
public final class PingConfig {

    private static final String CATEGORY_DESKTOP = "desktop";
    private static final String CATEGORY_DISCORD = "discord";

    public static boolean flashTaskbar = true;
    public static boolean windowsNotification = true;
    public static boolean onlyWhenUnfocused = true;

    /** Validated webhook URL, or null when unset or rejected. */
    public static String webhookUrl;
    /** Validated Discord user ID (snowflake), or null when unset or rejected. */
    public static String userId;

    private static File configFile;

    private PingConfig() {}

    public static void load(File configDirectory) {
        configFile = new File(configDirectory, AE2DesktopPing.MODID + ".cfg");
        reload();
    }

    public static void reload() {
        final Configuration configuration = new Configuration(configFile);

        flashTaskbar = configuration.getBoolean(
            "flashTaskbar",
            CATEGORY_DESKTOP,
            true,
            "Flash the game's taskbar button when a followed craft finishes.");
        windowsNotification = configuration.getBoolean(
            "windowsNotification",
            CATEGORY_DESKTOP,
            true,
            "Show a Windows notification when a followed craft finishes.");
        onlyWhenUnfocused = configuration.getBoolean(
            "onlyWhenUnfocused",
            CATEGORY_DESKTOP,
            true,
            "Skip the desktop alerts while the game window has focus.");

        final String rawWebhook = configuration.getString(
            "webhookUrl",
            CATEGORY_DISCORD,
            "",
            "Discord webhook URL (https://discord.com/api/webhooks/<id>/<token>). Keep it private.");
        final String rawUserId = configuration.getString(
            "userId",
            CATEGORY_DISCORD,
            "",
            "Your Discord user ID (Developer Mode > right-click yourself > Copy User ID). Only this user can be pinged.");

        webhookUrl = DiscordWebhook.validateWebhookUrl(rawWebhook);
        userId = DiscordWebhook.isValidUserId(rawUserId) ? rawUserId.trim() : null;

        if (!rawWebhook.trim()
            .isEmpty() && webhookUrl == null) {
            // Never log the value itself: it is a secret.
            AE2DesktopPing.LOG.error("discord.webhookUrl is not a Discord webhook URL; Discord pings are off.");
        }

        if (!rawUserId.trim()
            .isEmpty() && userId == null) {
            AE2DesktopPing.LOG.error("discord.userId must be 17-20 digits; Discord pings are off.");
        }

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static boolean isDiscordReady() {
        return webhookUrl != null && userId != null;
    }
}
