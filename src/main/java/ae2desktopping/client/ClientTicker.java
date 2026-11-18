package ae2desktopping.client;

import java.util.concurrent.TimeUnit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import ae2desktopping.AE2DesktopPing;
import ae2desktopping.client.gui.PingConfigGui;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

/**
 * Turns finished crafts into alerts on the client thread.
 * <p>
 * Crafts that finished while you were offline are replayed by the server right after you log in. Those arrive within
 * {@link #LOGIN_GRACE_SECONDS} of joining; they still clear their Discord flag but alert nothing.
 */
public final class ClientTicker {

    static final long LOGIN_GRACE_SECONDS = 20L;
    private static final long LOGIN_GRACE_NANOS = TimeUnit.SECONDS.toNanos(LOGIN_GRACE_SECONDS);
    private static final int TEST_DELAY_TICKS = 100;

    /** Written on the network thread. */
    private static volatile long connectedAtNanos = System.nanoTime();
    private static long worldJoinedAtNanos = System.nanoTime();
    private static boolean hadWorld;
    private static int testCountdownTicks = -1;
    private static boolean openConfig;

    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        connectedAtNanos = System.nanoTime();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        final boolean hasWorld = Minecraft.getMinecraft().theWorld != null;

        if (hasWorld && !hadWorld) {
            worldJoinedAtNanos = System.nanoTime();
        }

        hadWorld = hasWorld;

        CraftCompletions.Completion completion = CraftCompletions.poll();

        while (completion != null) {
            handle(completion);
            completion = CraftCompletions.poll();
        }

        if (openConfig) {
            openConfig = false;
            Minecraft.getMinecraft()
                .displayGuiScreen(new PingConfigGui(null));
        }

        if (testCountdownTicks > 0) {
            testCountdownTicks--;

            if (testCountdownTicks == 0) {
                testCountdownTicks = -1;
                DesktopAlert
                    .alert(I18n.format("ae2desktopping.alert.title"), I18n.format("ae2desktopping.alert.test"), true);
            }
        }
    }

    /** Opens the config screen once the chat screen that ran /ae2ping config has closed. */
    static void openConfigNextTick() {
        openConfig = true;
    }

    /** Fires the desktop alert a few seconds from now, so there is time to switch away from the game. */
    static void scheduleTestAlert() {
        testCountdownTicks = TEST_DELAY_TICKS;
    }

    private static void handle(CraftCompletions.Completion completion) {
        try {
            // Consumed even for a login replay, so the stamp doesn't linger for a later identical craft.
            final boolean pingHere = HereStamps.consume(completion.stack, completion.count);

            if (isLoginReplay(completion)) {
                return;
            }

            final String itemName = EnumChatFormatting.getTextWithoutFormattingCodes(completion.stack.getDisplayName());
            final String elapsed = TextFormat.formatElapsed(completion.elapsedNanos);

            DesktopAlert.alert(
                I18n.format("ae2desktopping.alert.title"),
                I18n.format(
                    "ae2desktopping.alert.finished",
                    TextFormat.formatCount(completion.count),
                    itemName,
                    elapsed),
                false);

            // The bell is read now, at finish time, so it covers every followed craft: the plan screen's
            // Start and follow, the CPU screen's Follow button and Ctrl auto-start alike.
            if (pingHere || PingConfig.pingMe) {
                DiscordWebhook.sendCraftFinished(itemName, completion.count, completion.elapsedNanos, pingHere);
            }
        } catch (RuntimeException exception) {
            AE2DesktopPing.LOG.error("Could not handle a finished AE2 craft", exception);
        }
    }

    private static boolean isLoginReplay(CraftCompletions.Completion completion) {
        if (!hadWorld) {
            return true;
        }

        final long joinedAtNanos = Math.max(connectedAtNanos, worldJoinedAtNanos);
        return completion.arrivedAtNanos - joinedAtNanos < LOGIN_GRACE_NANOS;
    }
}
