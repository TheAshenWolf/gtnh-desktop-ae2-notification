package ae2desktopping.mixins.late;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ae2desktopping.AE2DesktopPing;
import ae2desktopping.client.DiscordFlags;
import ae2desktopping.client.PingConfig;
import ae2desktopping.client.ToggleIconButton;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.widgets.GuiAeButton;
import appeng.container.implementations.ContainerCraftConfirm;

/**
 * Two toggles left of AE2's "save as image" button on the crafting plan screen:
 * <ul>
 * <li>Ping on Discord: stays as set until the game closes.</li>
 * <li>Ping @here: off every time a plan opens and after each start; turning it on implies a Discord ping.</li>
 * </ul>
 * "Start and follow" with either on flags the craft for a Discord ping.
 */
@Mixin(GuiCraftConfirm.class)
public abstract class MixinGuiCraftConfirm extends GuiContainer {

    @Unique
    private static final int AE2DESKTOPPING$BUTTON_SPACING = 18;
    @Unique
    private static final int AE2DESKTOPPING$PING_ME_X = -54;
    @Unique
    private static final int AE2DESKTOPPING$BUTTON_Y_FROM_BOTTOM = 18;

    @Shadow(remap = false)
    @Final
    private ContainerCraftConfirm ccc;

    @Shadow(remap = false)
    private GuiAeButton startWithFollow;

    @Unique
    private ToggleIconButton ae2desktopping$pingMeButton;

    @Unique
    private ToggleIconButton ae2desktopping$pingHereButton;

    /** Per screen: a new plan screen always starts with @here off. */
    @Unique
    private boolean ae2desktopping$pingHere;

    private MixinGuiCraftConfirm(Container container) {
        super(container);
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    private void ae2desktopping$addToggles(CallbackInfo callback) {
        final int y = guiTop + ySize - AE2DESKTOPPING$BUTTON_Y_FROM_BOTTOM;
        final int pingMeX = guiLeft + AE2DESKTOPPING$PING_ME_X;

        ae2desktopping$pingMeButton = new ToggleIconButton(0, pingMeX, y, 0, 16);
        ae2desktopping$pingHereButton = new ToggleIconButton(0, pingMeX - AE2DESKTOPPING$BUTTON_SPACING, y, 32, 48);
        buttonList.add(ae2desktopping$pingMeButton);
        buttonList.add(ae2desktopping$pingHereButton);
        ae2desktopping$refreshToggles();
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void ae2desktopping$onButton(GuiButton button, CallbackInfo callback) {
        if (button == ae2desktopping$pingMeButton) {
            DiscordFlags.setPingMeToggle(!DiscordFlags.isPingMeToggleOn());
            ae2desktopping$refreshToggles();
            callback.cancel();
            return;
        }

        if (button == ae2desktopping$pingHereButton) {
            ae2desktopping$pingHere = !ae2desktopping$pingHere;
            ae2desktopping$refreshToggles();
            callback.cancel();
            return;
        }

        if (button != startWithFollow || !PingConfig.isDiscordReady()) {
            return;
        }

        final boolean pingHere = ae2desktopping$pingHere;
        ae2desktopping$pingHere = false;

        if (!pingHere && !DiscordFlags.isPingMeToggleOn()) {
            return;
        }

        try {
            final IAEStack<?> output = ccc.getItemToCraft();
            DiscordFlags.flag(output, pingHere);
        } catch (RuntimeException exception) {
            AE2DesktopPing.LOG.error("Could not read which item this craft makes; no Discord ping for it.", exception);
        }
    }

    @Unique
    private void ae2desktopping$refreshToggles() {
        if (ae2desktopping$pingMeButton == null || ae2desktopping$pingHereButton == null) {
            return;
        }

        final boolean ready = PingConfig.isDiscordReady();
        final boolean pingMe = DiscordFlags.isPingMeToggleOn();

        ae2desktopping$pingMeButton.enabled = ready;
        ae2desktopping$pingHereButton.enabled = ready;
        ae2desktopping$pingMeButton.setOn(ready && pingMe);
        ae2desktopping$pingHereButton.setOn(ready && ae2desktopping$pingHere);

        if (!ready) {
            final String notReady = I18n.format("ae2desktopping.button.not_configured");
            ae2desktopping$pingMeButton
                .setTooltip(I18n.format("ae2desktopping.button.ping_me.title") + "\n" + notReady);
            ae2desktopping$pingHereButton
                .setTooltip(I18n.format("ae2desktopping.button.ping_here.title") + "\n" + notReady);
            return;
        }

        ae2desktopping$pingMeButton.setTooltip(
            I18n.format("ae2desktopping.button.ping_me.title") + "\n"
                + I18n.format(pingMe ? "ae2desktopping.button.on" : "ae2desktopping.button.off")
                + "\n"
                + I18n.format("ae2desktopping.button.ping_me.help"));
        ae2desktopping$pingHereButton.setTooltip(
            I18n.format("ae2desktopping.button.ping_here.title") + "\n"
                + I18n.format(ae2desktopping$pingHere ? "ae2desktopping.button.on" : "ae2desktopping.button.off")
                + "\n"
                + I18n.format("ae2desktopping.button.ping_here.help"));
    }
}
