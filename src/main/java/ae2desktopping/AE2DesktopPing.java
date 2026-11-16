package ae2desktopping;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;

/**
 * Client-only AE2 addon. acceptableRemoteVersions lets the client join servers that don't have it.
 */
@Mod(
    modid = AE2DesktopPing.MODID,
    name = "AE2 Desktop Ping",
    version = Tags.VERSION,
    acceptableRemoteVersions = "*",
    dependencies = "required-after:appliedenergistics2")
public class AE2DesktopPing {

    public static final String MODID = "ae2desktopping";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (event.getSide() != Side.CLIENT) {
            return;
        }

        ae2desktopping.client.ClientSetup.preInit(event.getModConfigurationDirectory());
    }
}
