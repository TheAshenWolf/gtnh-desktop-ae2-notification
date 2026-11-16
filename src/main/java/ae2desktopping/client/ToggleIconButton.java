package ae2desktopping.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import ae2desktopping.AE2DesktopPing;
import appeng.client.gui.widgets.ITooltip;

/** 16x16 AE2-style toggle: AE2's button background with one of our icons on top. AE2 draws the tooltip. */
public final class ToggleIconButton extends GuiButton implements ITooltip {

    private static final ResourceLocation AE2_STATES = new ResourceLocation(
        "appliedenergistics2",
        "textures/guis/states.png");
    private static final ResourceLocation ICONS = new ResourceLocation(
        AE2DesktopPing.MODID,
        "textures/gui/toggles.png");
    private static final int SIZE = 16;
    private static final int ICON_SHEET_WIDTH = 64;
    private static final int ICON_SHEET_HEIGHT = 16;

    private final int iconOffU;
    private final int iconOnU;
    private boolean on;
    private String tooltip = "";

    public ToggleIconButton(int id, int x, int y, int iconOffU, int iconOnU) {
        super(id, x, y, SIZE, SIZE, "");
        this.iconOffU = iconOffU;
        this.iconOnU = iconOnU;
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }

        final float shade = enabled ? 1.0f : 0.5f;
        GL11.glColor4f(shade, shade, shade, 1.0f);
        minecraft.getTextureManager()
            .bindTexture(AE2_STATES);
        drawTexturedModalRect(xPosition, yPosition, 240, 240, SIZE, SIZE);

        GL11.glEnable(GL11.GL_BLEND);
        minecraft.getTextureManager()
            .bindTexture(ICONS);
        func_146110_a(
            xPosition,
            yPosition,
            on ? iconOnU : iconOffU,
            0,
            SIZE,
            SIZE,
            ICON_SHEET_WIDTH,
            ICON_SHEET_HEIGHT);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        field_146123_n = mouseX >= xPosition && mouseY >= yPosition
            && mouseX < xPosition + width
            && mouseY < yPosition + height;
    }

    @Override
    public String getMessage() {
        return tooltip;
    }

    @Override
    public int xPos() {
        return xPosition;
    }

    @Override
    public int yPos() {
        return yPosition;
    }

    @Override
    public int getWidth() {
        return SIZE;
    }

    @Override
    public int getHeight() {
        return SIZE;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }
}
