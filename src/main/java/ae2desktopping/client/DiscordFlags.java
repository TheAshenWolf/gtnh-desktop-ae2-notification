package ae2desktopping.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import appeng.api.storage.data.IAEStack;

/**
 * Crafts started with "Start and follow" while a Discord toggle was on. Memory only: closing the game forgets them.
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

    public static void flag(IAEStack<?> output, boolean pingHere) {
        if (output == null) {
            return;
        }

        if (FLAGGED.size() >= MAX_FLAGGED) {
            FLAGGED.remove(0);
        }

        FLAGGED.add(new FlaggedCraft(currentServer(), output.copy(), output.getStackSize(), pingHere));
    }

    /**
     * Finds and removes the flag for a finished craft. An exact count match wins; otherwise the oldest flag for the
     * same item (merged crafts finish with a different count).
     */
    public static Match consume(IAEStack<?> output, long count) {
        final String server = currentServer();
        int sameTypeIndex = -1;
        int exactIndex = -1;

        for (int i = 0, size = FLAGGED.size(); i < size; i++) {
            final FlaggedCraft flagged = FLAGGED.get(i);

            if (!flagged.server.equals(server) || !flagged.output.isSameType(output)) {
                continue;
            }

            if (sameTypeIndex < 0) {
                sameTypeIndex = i;
            }

            if (flagged.count == count) {
                exactIndex = i;
                break;
            }
        }

        final int index = exactIndex >= 0 ? exactIndex : sameTypeIndex;

        if (index < 0) {
            return Match.NONE;
        }

        final FlaggedCraft removed = FLAGGED.remove(index);
        return removed.pingHere ? Match.PING_HERE : Match.PING_ME;
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
