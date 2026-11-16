package ae2desktopping.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import appeng.api.storage.data.IAEStack;

/**
 * Crafts started with "Start and follow" while the Discord toggle was on. Memory only: closing the game forgets them.
 * Only a craft flagged here can ping, so a server can't trigger pings for crafts you never flagged.
 * Client thread only.
 */
public final class DiscordFlags {

    private static final int MAX_FLAGGED = 64;

    private static final class FlaggedCraft {

        final String server;
        final IAEStack<?> output;
        final long count;
        final boolean pingHere;

        FlaggedCraft(String server, IAEStack<?> output, long count, boolean pingHere) {
            this.server = server;
            this.output = output;
            this.count = count;
            this.pingHere = pingHere;
        }
    }

    /** Result of matching a finished craft against the flags. */
    public enum Match {
        NONE,
        PING_ME,
        PING_HERE
    }

    private static final List<FlaggedCraft> FLAGGED = new ArrayList<FlaggedCraft>();

    /** The "Ping on Discord" toggle. Stays as set until the game closes; starts off. */
    private static boolean pingMeToggle;

    private DiscordFlags() {}

    public static boolean isPingMeToggleOn() {
        return pingMeToggle;
    }

    public static void setPingMeToggle(boolean on) {
        pingMeToggle = on;
    }

    /**
     * Called on every "Start and follow". A finished craft is only known by its item and amount, so a flag left by a
     * craft that never finished (cancelled, failed to start) would match a later identical craft. The newest decision
     * for an item and amount therefore replaces any older one, whether it pings or not.
     */
    public static void recordFollow(IAEStack<?> output, boolean pingMe, boolean pingHere) {
        if (output == null) {
            return;
        }

        final String server = currentServer();
        final long count = output.getStackSize();
        int index = indexOf(server, output, count);

        while (index >= 0) {
            FLAGGED.remove(index);
            index = indexOf(server, output, count);
        }

        if (!pingMe) {
            return;
        }

        if (FLAGGED.size() >= MAX_FLAGGED) {
            FLAGGED.remove(0);
        }

        FLAGGED.add(new FlaggedCraft(server, output.copy(), count, pingHere));
    }

    /**
     * Finds and removes the flag for a finished craft: same server, item and amount. A craft merged into another
     * finishes with the combined amount, so it matches nothing and doesn't ping.
     */
    public static Match consume(IAEStack<?> output, long count) {
        final int index = indexOf(currentServer(), output, count);

        if (index < 0) {
            return Match.NONE;
        }

        final FlaggedCraft removed = FLAGGED.remove(index);
        return removed.pingHere ? Match.PING_HERE : Match.PING_ME;
    }

    private static int indexOf(String server, IAEStack<?> output, long count) {
        for (int i = 0, size = FLAGGED.size(); i < size; i++) {
            final FlaggedCraft flagged = FLAGGED.get(i);

            if (flagged.count == count && flagged.server.equals(server) && flagged.output.isSameType(output)) {
                return i;
            }
        }

        return -1;
    }

    public static int flaggedCount() {
        return FLAGGED.size();
    }

    private static String currentServer() {
        final Minecraft minecraft = Minecraft.getMinecraft();
        final ServerData serverData = minecraft.func_147104_D();

        if (serverData != null && serverData.serverIP != null) {
            return "server:" + serverData.serverIP;
        }

        return minecraft.isSingleplayer() ? "singleplayer" : "unknown";
    }
}
