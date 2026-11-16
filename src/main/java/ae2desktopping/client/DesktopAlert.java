package ae2desktopping.client;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import ae2desktopping.AE2DesktopPing;

/**
 * Taskbar flash and Windows notification.
 * <ul>
 * <li>Flash: SDL_FlashWindow on the game window. Needs lwjgl3ify (SDL3); reached by reflection, so without it the
 * flash is simply skipped. SDL video calls belong on the client thread.</li>
 * <li>Notification: an AWT tray icon balloon, which Windows 10/11 shows as a toast. Plain strings go to the OS API;
 * no shell or script is involved. Windows only, on its own thread.</li>
 * </ul>
 */
public final class DesktopAlert {

    private static final String TRAY_TOOLTIP = "GregTech: New Horizons";
    private static final ResourceLocation PACK_ICON = new ResourceLocation(
        "dreamcraft",
        "textures/icon/GTNH_256x256.png");
    private static final int MAX_QUEUED_NOTIFICATIONS = 8;

    private static final ThreadPoolExecutor NOTIFIER = new ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<Runnable>(MAX_QUEUED_NOTIFICATIONS),
        runnable -> {
            final Thread thread = new Thread(runnable, "AE2DesktopPing-Notify");
            thread.setDaemon(true);
            return thread;
        },
        new ThreadPoolExecutor.DiscardPolicy());

    private static boolean flashResolved;
    private static Method getWindowMethod;
    private static Method flashWindowMethod;
    private static int flashUntilFocused;

    private static BufferedImage trayImage;
    private static boolean trayFailed;
    private static TrayIcon trayIcon;

    private DesktopAlert() {}

    /** Client thread. */
    public static void alert(String title, String message, boolean ignoreFocus) {
        if (!ignoreFocus && PingConfig.onlyWhenUnfocused && isWindowFocused()) {
            return;
        }

        if (PingConfig.flashTaskbar) {
            flashTaskbar();
        }

        if (PingConfig.windowsNotification && isWindows()) {
            final BufferedImage image = getTrayImage();
            NOTIFIER.execute(() -> showNotification(title, message, image));
        }
    }

    /** Client thread: the resource manager isn't thread-safe. Loaded once. */
    private static BufferedImage getTrayImage() {
        if (trayImage == null) {
            trayImage = loadPackIcon();

            if (trayImage == null) {
                trayImage = createFallbackIcon();
            }
        }

        return trayImage;
    }

    /** The GTNH logo shipped by the pack's core mod (dreamcraft); null when that mod isn't there. */
    private static BufferedImage loadPackIcon() {
        try (InputStream input = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(PACK_ICON)
            .getInputStream()) {
            return ImageIO.read(input);
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    private static boolean isWindowFocused() {
        try {
            return org.lwjgl.opengl.Display.isActive();
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT)
            .startsWith("windows");
    }

    private static void flashTaskbar() {
        if (!flashResolved) {
            resolveFlash();
        }

        if (flashWindowMethod == null) {
            return;
        }

        try {
            final long window = (Long) getWindowMethod.invoke(null);

            if (window != 0L) {
                flashWindowMethod.invoke(null, window, flashUntilFocused);
            }
        } catch (Throwable throwable) {
            AE2DesktopPing.LOG.error("Taskbar flash failed; turning it off for this session.", throwable);
            flashWindowMethod = null;
        }
    }

    private static void resolveFlash() {
        flashResolved = true;

        try {
            final Class<?> display = Class.forName("org.lwjglx.opengl.Display");
            final Class<?> sdlVideo = Class.forName("org.lwjgl.sdl.SDLVideo");
            final Field untilFocused = sdlVideo.getField("SDL_FLASH_UNTIL_FOCUSED");

            getWindowMethod = display.getMethod("getWindow");
            flashUntilFocused = untilFocused.getInt(null);
            flashWindowMethod = sdlVideo.getMethod("SDL_FlashWindow", long.class, int.class);
        } catch (Throwable throwable) {
            // No lwjgl3ify / SDL: nothing to flash with.
            getWindowMethod = null;
            flashWindowMethod = null;
        }
    }

    /**
     * Notifier thread. Windows uses the tray icon as the toast's header icon. The header text is the process name
     * (javaw.exe calls itself "OpenJDK Platform binary"); MessageType.NONE drops the generic "i" badge.
     */
    private static void showNotification(String title, String message, BufferedImage image) {
        if (trayFailed) {
            return;
        }

        try {
            if (trayIcon == null) {
                if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
                    trayFailed = true;
                    return;
                }

                final TrayIcon icon = new TrayIcon(image, TRAY_TOOLTIP);
                icon.setImageAutoSize(true);
                SystemTray.getSystemTray()
                    .add(icon);
                trayIcon = icon;
            }

            trayIcon.displayMessage(title, message, TrayIcon.MessageType.NONE);
        } catch (AWTException | RuntimeException | LinkageError exception) {
            trayFailed = true;
            AE2DesktopPing.LOG
                .error("Windows notifications are unavailable; turning them off for this session.", exception);
        }
    }

    private static BufferedImage createFallbackIcon() {
        final int size = 16;
        final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(0x5B, 0x3E, 0x9E));
        graphics.fillRoundRect(0, 0, size, size, 6, 6);
        graphics.setColor(new Color(0xD8, 0xC8, 0xFF));
        graphics.fillRect(4, 4, 8, 8);
        graphics.dispose();
        return image;
    }
}
