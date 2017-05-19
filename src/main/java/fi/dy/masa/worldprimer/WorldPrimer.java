package fi.dy.masa.worldprimer;

import java.io.File;
import org.apache.logging.log4j.Logger;
import net.minecraft.world.chunk.storage.AnvilSaveConverter;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import fi.dy.masa.worldprimer.command.WorldPrimerCommandSender;
import fi.dy.masa.worldprimer.config.Configs;
import fi.dy.masa.worldprimer.proxy.IProxy;
import fi.dy.masa.worldprimer.reference.Reference;
import fi.dy.masa.worldprimer.util.DimensionLoadTracker;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.MOD_VERSION,
    guiFactory = "fi.dy.masa.worldprimer.config.WorldPrimerGuiFactory",
    updateJSON = "https://raw.githubusercontent.com/maruohon/worldprimer/master/update.json",
    acceptableRemoteVersions = "*", acceptedMinecraftVersions = "[1.10,1.11.2]")
public class WorldPrimer
{
    @Mod.Instance(Reference.MOD_ID)
    public static WorldPrimer instance;

    @SidedProxy(clientSide = Reference.PROXY_CLIENT, serverSide = Reference.PROXY_SERVER)
    public static IProxy proxy;

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        Configs.loadConfigsFromFile(event.getSuggestedConfigurationFile());
        proxy.registerEventHandlers();
    }

    @Mod.EventHandler
    public void onServerAboutToStart(FMLServerAboutToStartEvent event)
    {
        logInfo("FMLServerAboutToStartEvent");
        Configs.reloadConfigs();

        File worldDir = new File(((AnvilSaveConverter) event.getServer().getActiveAnvilConverter()).savesDirectory, event.getServer().getFolderName());
        // We need to read the data before any dimension loads
        DimensionLoadTracker.instance().readFromDisk(worldDir);
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event)
    {
        logInfo("FMLServerStartedEvent");
        DimensionLoadTracker.instance().serverStarted();

        if (Configs.enableWorldLoadingCommands)
        {
            WorldPrimerCommandSender.instance().runCommands(Configs.worldLoadingCommands);
        }
    }

    public static void logInfo(String message, Object... params)
    {
        if (Configs.enableLoggingInfo)
        {
            logger.info(message, params);
        }
        else
        {
            logger.debug(message, params);
        }
    }
}
