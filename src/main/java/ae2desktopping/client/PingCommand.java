package ae2desktopping.client;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentTranslation;

/** Client-side /ae2ping: status, reload (re-reads the config), test. */
public final class PingCommand extends CommandBase {

    private static final String[] SUBCOMMANDS = { "status", "reload", "test" };

    @Override
    public String getCommandName() {
        return "ae2ping";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ae2ping <status|reload|test>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] arguments) {
        final String subcommand = arguments.length == 0 ? "status" : arguments[0];

        if ("reload".equalsIgnoreCase(subcommand)) {
            PingConfig.reload();
            sender.addChatMessage(new ChatComponentTranslation("ae2desktopping.chat.reloaded"));
            sendStatus(sender);
            return;
        }

        if ("test".equalsIgnoreCase(subcommand)) {
            ClientTicker.scheduleTestAlert();
            sender.addChatMessage(new ChatComponentTranslation("ae2desktopping.chat.test_desktop"));

            if (DiscordWebhook.sendTest()) {
                sender.addChatMessage(new ChatComponentTranslation("ae2desktopping.chat.test_discord"));
            } else {
                sender.addChatMessage(new ChatComponentTranslation("ae2desktopping.chat.discord_not_ready"));
            }

            return;
        }

        sendStatus(sender);
    }

    private static void sendStatus(ICommandSender sender) {
        sender.addChatMessage(
            new ChatComponentTranslation(
                PingConfig.isDiscordReady() ? "ae2desktopping.chat.status_ready"
                    : "ae2desktopping.chat.discord_not_ready"));
        sender.addChatMessage(
            new ChatComponentTranslation(
                "ae2desktopping.chat.status_flags",
                DiscordFlags.flaggedCount(),
                DiscordFlags.isPingMeToggleOn() ? "ON" : "OFF"));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] arguments) {
        if (arguments.length == 1) {
            return getListOfStringsMatchingLastWord(arguments, SUBCOMMANDS);
        }

        return Collections.emptyList();
    }
}
