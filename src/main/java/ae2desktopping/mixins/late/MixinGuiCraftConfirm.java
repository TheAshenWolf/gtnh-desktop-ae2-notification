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
 * Two toggles on the crafting plan screen, one row above AE2's "save as image" and "terminal style" buttons:
 * <ul>
 * <li>Ping me on Discord (above save as image): stays as set until the game closes.</li>
 * <li>Ping @here (above terminal style): needs the first one on; off every time a plan opens.</li>
 * </ul>
 * Every "Start and follow" records the decision for its item and amount, see {@link DiscordFlags#recordFollow}.
 */
@Mixin(GuiCraftConfirm.class)
public abstract class MixinGuiCraftConfirm extends GuiContainer {

    /** AE2 puts terminal style at guiLeft - 18 and save as image at guiLeft - 36, 18 above the bottom edge. */
    @Unique
    private static final int AE2DESKTOPPING$PING_ME_X = -36;
    @Unique
    private static final int AE2DESKTOPPING$PING_HERE_X = -18;
    @Unique
    private static final int AE2DESKTOPPING$Y_FROM_BOTTOM = 36;

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
        final int y = guiTop + ySize - AE2DESKTOPPING$Y_FROM_BOTTOM;

        ae2desktopping$pingMeButton = new ToggleIconButton(0, guiLeft + AE2DESKTOPPING$PING_ME_X, y, 0, 16);
        ae2desktopping$pingHereButton = new ToggleIconButton(0, guiLeft + AE2DESKTOPPING$PING_HERE_X, y, 32, 48);
        buttonList.add(ae2desktopping$pingMeButton);
        buttonList.add(ae2desktopping$pingHereButton);
        ae2desktopping$refreshToggles();
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void ae2desktopping$onButton(GuiButton button, CallbackInfo callback) {
        if (button == ae2desktopping$pingMeButton) {
            final boolean pingMe = !DiscordFlags.isPingMeToggleOn();
            DiscordFlags.setPingMeToggle(pingMe);

            if (!pingMe) {
                ae2desktopping$pingHere = false;
            }

            ae2desktopping$refreshToggles();
            callback.cancel();
            return;
        }

        if (button == ae2desktopping$pingHereButton) {
            ae2desktopping$pingHere = !ae2desktopping$pingHere && DiscordFlags.isPingMeToggleOn();
            ae2desktopping$refreshToggles();
            callback.cancel();
            return;
        }

        if (button != startWithFollow) {
            return;
        }

        final boolean ready = PingConfig.isDiscordReady();
        final boolean pingMe = ready && DiscordFlags.isPingMeToggleOn();
        final boolean pingHere = pingMe && ae2desktopping$pingHere;
        ae2desktopping$pingHere = false;

        try {
            final IAEStack<?> output = ccc.getItemToCraft();
            DiscordFlags.recordFollow(output, pingMe, pingHere);
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
        final boolean pingMe = ready && DiscordFlags.isPingMeToggleOn();

        if (!pingMe) {
            ae2desktopping$pingHere = false;
        }

        ae2desktopping$pingMeButton.enabled = ready;
        ae2desktopping$pingHereButton.enabled = pingMe;
        ae2desktopping$pingMeButton.setOn(pingMe);
        ae2desktopping$pingHereButton.setOn(ae2desktopping$pingHere);

        final String pingMeTitle = I18n.format("ae2desktopping.button.ping_me.title");
        final String pingHereTitle = I18n.format("ae2desktopping.button.ping_here.title");

        if (!ready) {
            final String notReady = I18n.format("ae2desktopping.button.not_configured");
            ae2desktopping$pingMeButton.setTooltip(pingMeTitle + "\n" + notReady);
            ae2desktopping$pingHereButton.setTooltip(pingHereTitle + "\n" + notReady);
            return;
        }

        ae2desktopping$pingMeButton.setTooltip(
            pingMeTitle + "\n"
                + I18n.format(pingMe ? "ae2desktopping.button.on" : "ae2desktopping.button.off")
                + "\n"
                + I18n.format("ae2desktopping.button.ping_me.help"));

        if (!pingMe) {
            ae2desktopping$pingHereButton
                .setTooltip(pingHereTitle + "\n" + I18n.format("ae2desktopping.button.ping_here.needs_ping_me"));
            return;
        }

        ae2desktopping$pingHereButton.setTooltip(
            pingHereTitle + "\n"
                + I18n.format(ae2desktopping$pingHere ? "ae2desktopping.button.on" : "ae2desktopping.button.off")
                + "\n"
                + I18n.format("ae2desktopping.button.ping_here.help"));
    }
}
