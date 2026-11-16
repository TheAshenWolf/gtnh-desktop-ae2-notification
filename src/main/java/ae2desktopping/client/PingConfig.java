package ae2desktopping.client;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import ae2desktopping.AE2DesktopPing;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * config/ae2desktopping.cfg, also editable in game (Mods > AE2 Desktop Ping > Config, or /ae2ping config).
 * The webhook URL is a secret: anyone holding it can post to that channel.
 */
public final class PingConfig {

    public static final String CATEGORY_DESKTOP = "desktop";
    public static final String CATEGORY_DISCORD = "discord";
    private static final String LANG_PREFIX = "ae2desktopping.config.";

    public static boolean flashTaskbar = true;
    public static boolean windowsNotification = true;
    public static boolean onlyWhenUnfocused = true;

    /** Validated webhook URL, or null when unset or rejected. */
    public static String webhookUrl;
    /** Validated Discord user ID (snowflake), or null when unset or rejected. */
    public static String userId;

    private static Configuration configuration;

    /** Only instantiated to receive {@link ConfigChangedEvent} on the FML bus. */
    public PingConfig() {}

    public static void load(File configDirectory) {
        configuration = new Configuration(new File(configDirectory, AE2DesktopPing.MODID + ".cfg"));
        sync();
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    /** Re-reads the file (for hand edits), then applies it. */
    public static void reloadFromDisk() {
        configuration.load();
        sync();
    }

    /** Fired by Forge's config screen after Done; the screen has already written into the Configuration. */
    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (AE2DesktopPing.MODID.equals(event.modID)) {
            sync();
        }
    }

    private static void sync() {
        configuration.setCategoryLanguageKey(CATEGORY_DESKTOP, LANG_PREFIX + CATEGORY_DESKTOP);
        configuration.setCategoryLanguageKey(CATEGORY_DISCORD, LANG_PREFIX + CATEGORY_DISCORD);

        flashTaskbar = bool(
            CATEGORY_DESKTOP,
            "flashTaskbar",
            true,
            "Flash the taskbar button when a followed craft finishes.");
        windowsNotification = bool(
            CATEGORY_DESKTOP,
            "windowsNotification",
            true,
            "Windows notification when a followed craft finishes.");
        onlyWhenUnfocused = bool(
            CATEGORY_DESKTOP,
            "onlyWhenUnfocused",
            true,
            "Skip desktop alerts while the game has focus.");

        final String rawWebhook = string(
            CATEGORY_DISCORD,
            "webhookUrl",
            "https://discord.com/api/webhooks/<id>/<token>. Keep it private.");
        final String rawUserId = string(
            CATEGORY_DISCORD,
            "userId",
            "Your Discord user ID (Developer Mode > right-click yourself > Copy User ID).");

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

    private static boolean bool(String category, String key, boolean defaultValue, String comment) {
        final Property property = configuration.get(category, key, defaultValue, comment);
        property.setLanguageKey(LANG_PREFIX + key);
        return property.getBoolean(defaultValue);
    }

    private static String string(String category, String key, String comment) {
        final Property property = configuration.get(category, key, "", comment);
        property.setLanguageKey(LANG_PREFIX + key);
        return property.getString();
    }

    public static boolean isDiscordReady() {
        return webhookUrl != null && userId != null;
    }
}
