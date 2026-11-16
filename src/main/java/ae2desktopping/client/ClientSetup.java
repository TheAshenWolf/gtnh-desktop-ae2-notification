package ae2desktopping.client;

import java.io.File;

import net.minecraftforge.client.ClientCommandHandler;

import cpw.mods.fml.common.FMLCommonHandler;

public final class ClientSetup {

    private ClientSetup() {}

    public static void preInit(File configDirectory) {
        PingConfig.load(configDirectory);
        FMLCommonHandler.instance()
            .bus()
            .register(new ClientTicker());
        FMLCommonHandler.instance()
            .bus()
            .register(new PingConfig());
        ClientCommandHandler.instance.registerCommand(new PingCommand());
    }
}
