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
import ae2desktopping.client.HereStamps;
import ae2desktopping.client.PingConfig;
import ae2desktopping.client.ToggleIconButton;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.widgets.GuiAeButton;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiSimpleImgButton;
import appeng.container.implementations.ContainerCraftConfirm;

/**
 * Two toggles on the crafting plan screen, one row above AE2's "save as image" and "terminal style" buttons:
 * <ul>
 * <li>Ping me on Discord (above save as image): the bell, saved in the config. Read when a craft finishes, so it covers
 * every followed craft.</li>
 * <li>Ping @here (above terminal style): needs the first one on; off every time a plan opens.</li>
 * </ul>
 * Every "Start and follow" records the @here decision for its item and amount, see {@link HereStamps#recordFollow}.
 */
@Mixin(GuiCraftConfirm.class)
public abstract class MixinGuiCraftConfirm extends GuiContainer {

    @Shadow(remap = false)
    @Final
    private ContainerCraftConfirm ccc;

    @Shadow(remap = false)
    private GuiAeButton startWithFollow;

    @Shadow(remap = false)
    private GuiSimpleImgButton takeScreenshot;

    @Shadow(remap = false)
    private GuiImgButton switchTallMode;

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
        ae2desktopping$pingMeButton = new ToggleIconButton(0, 0, 0, 0, 16);
        ae2desktopping$pingHereButton = new ToggleIconButton(0, 0, 0, 32, 48);
        buttonList.add(ae2desktopping$pingMeButton);
        buttonList.add(ae2desktopping$pingHereButton);
        ae2desktopping$placeToggles();
        ae2desktopping$refreshToggles();
    }

    /** AE2 moves "save as image" every frame (list view: guiLeft - 38, tree view: guiLeft - 36), so follow it. */
    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void ae2desktopping$followAe2Buttons(int mouseX, int mouseY, float partialTicks, CallbackInfo callback) {
        ae2desktopping$placeToggles();
    }

    /**
     * One row above AE2's pair: the bell over "save as image", @here over "terminal style". The vertical gap is the
     * same as the horizontal gap between AE2's two buttons.
     */
    @Unique
    private void ae2desktopping$placeToggles() {
        if (ae2desktopping$pingMeButton == null || takeScreenshot == null || switchTallMode == null) {
            return;
        }

        final int gap = switchTallMode.xPosition - (takeScreenshot.xPosition + takeScreenshot.width);
        final int y = takeScreenshot.yPosition - ae2desktopping$pingMeButton.height - gap;

        ae2desktopping$pingMeButton.xPosition = takeScreenshot.xPosition;
        ae2desktopping$pingMeButton.yPosition = y;
        ae2desktopping$pingHereButton.xPosition = switchTallMode.xPosition;
        ae2desktopping$pingHereButton.yPosition = y;
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void ae2desktopping$onButton(GuiButton button, CallbackInfo callback) {
        if (button == ae2desktopping$pingMeButton) {
            final boolean pingMe = !PingConfig.pingMe;
            PingConfig.setPingMe(pingMe);

            if (!pingMe) {
                ae2desktopping$pingHere = false;
            }

            ae2desktopping$refreshToggles();
            callback.cancel();
            return;
        }

        if (button == ae2desktopping$pingHereButton) {
            ae2desktopping$pingHere = !ae2desktopping$pingHere && PingConfig.pingMe;
            ae2desktopping$refreshToggles();
            callback.cancel();
            return;
        }

        if (button != startWithFollow) {
            return;
        }

        final boolean pingHere = PingConfig.isDiscordReady() && PingConfig.pingMe && ae2desktopping$pingHere;
        ae2desktopping$pingHere = false;

        try {
            final IAEStack<?> output = ccc.getItemToCraft();
            HereStamps.recordFollow(output, pingHere);
        } catch (RuntimeException exception) {
            AE2DesktopPing.LOG.error("Could not read which item this craft makes; no @here ping for it.", exception);
        }
    }

    @Unique
    private void ae2desktopping$refreshToggles() {
        if (ae2desktopping$pingMeButton == null || ae2desktopping$pingHereButton == null) {
            return;
        }

        final boolean ready = PingConfig.isDiscordReady();
        final boolean pingMe = ready && PingConfig.pingMe;

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
