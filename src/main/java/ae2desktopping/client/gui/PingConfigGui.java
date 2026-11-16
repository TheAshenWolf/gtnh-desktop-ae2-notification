package ae2desktopping.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import ae2desktopping.AE2DesktopPing;
import ae2desktopping.client.PingConfig;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;

/** Forge's config screen over config/ae2desktopping.cfg. Done saves and fires OnConfigChangedEvent. */
public final class PingConfigGui extends GuiConfig {

    public PingConfigGui(GuiScreen parent) {
        super(parent, createElements(), AE2DesktopPing.MODID, false, false, "AE2 Desktop Ping");
    }

    @SuppressWarnings("rawtypes")
    private static List<IConfigElement> createElements() {
        final Configuration configuration = PingConfig.getConfiguration();
        final List<IConfigElement> elements = new ArrayList<IConfigElement>();
        elements.add(new ConfigElement(configuration.getCategory(PingConfig.CATEGORY_DISCORD)));
        elements.add(new ConfigElement(configuration.getCategory(PingConfig.CATEGORY_DESKTOP)));
        return elements;
    }
}
