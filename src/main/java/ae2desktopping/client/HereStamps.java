package ae2desktopping.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import appeng.api.storage.data.IAEStack;

/**
 * Crafts started with "Start and follow" while the @here toggle was on. Memory only: closing the game forgets them, so
 * a craft that finishes after a restart never pings the team. Client thread only.
 * <p>
 * Pinging yourself doesn't need a stamp: that follows the bell (see {@link PingConfig#pingMe}) when the craft finishes.
 */
public final class HereStamps {

    private static final int MAX_STAMPS = 64;

    private static final class Stamp {

        final String server;
        final IAEStack<?> output;
        final long count;

        Stamp(String server, IAEStack<?> output, long count) {
            this.server = server;
            this.output = output;
            this.count = count;
        }
    }

    private static final List<Stamp> STAMPS = new ArrayList<Stamp>();

    private HereStamps() {}

    /**
     * Called on every "Start and follow". A finished craft is only known by its item and amount, so a stamp left by a
     * craft that never finished (cancelled, failed to start) would match a later identical craft. The newest decision
     * for an item and amount therefore replaces any older one, with or without @here.
     */
    public static void recordFollow(IAEStack<?> output, boolean pingHere) {
        if (output == null) {
            return;
        }

        final String server = currentServer();
        final long count = output.getStackSize();
        int index = indexOf(server, output, count);

        while (index >= 0) {
            STAMPS.remove(index);
            index = indexOf(server, output, count);
        }

        if (!pingHere) {
            return;
        }

        if (STAMPS.size() >= MAX_STAMPS) {
            STAMPS.remove(0);
        }

        STAMPS.add(new Stamp(server, output.copy(), count));
    }

    /**
     * Removes the stamp for a finished craft (same server, item and amount) and says whether there was one. A craft
     * merged into another finishes with the combined amount, so it matches nothing.
     */
    public static boolean consume(IAEStack<?> output, long count) {
        final int index = indexOf(currentServer(), output, count);

        if (index < 0) {
            return false;
        }

        STAMPS.remove(index);
        return true;
    }

    public static int count() {
        return STAMPS.size();
    }

    private static int indexOf(String server, IAEStack<?> output, long count) {
        for (int i = 0, size = STAMPS.size(); i < size; i++) {
            final Stamp stamp = STAMPS.get(i);

            if (stamp.count == count && stamp.server.equals(server) && stamp.output.isSameType(output)) {
                return i;
            }
        }

        return -1;
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
