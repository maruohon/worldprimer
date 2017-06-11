package fi.dy.masa.worldprimer;

import java.io.File;
import org.apache.logging.log4j.Logger;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.AnvilSaveConverter;
import net.minecraftforge.fml.common.FMLCommonHandler;
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
    acceptableRemoteVersions = "*", acceptedMinecraftVersions = "1.12")
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

        File worldDir = new File(((AnvilSaveConverter) event.getServer().getActiveAnvilConverter()).savesDirectory,
                event.getServer().getFolderName());

        if (Configs.enableDimensionLoadTracking)
        {
            // We need to read the data before any dimension loads
            DimensionLoadTracker.instance().readFromDisk(worldDir);

            int count = DimensionLoadTracker.instance().getServerStartCount();
            logInfo("FMLServerAboutToStartEvent - server starting, previous start count: {}", count);

            // The server start count is incremented in the FMLServerStartedEvent,
            // so on world creation it will be 0 here
            if (Configs.enableEarlyWorldCreationCommands && count == 0)
            {
                WorldPrimer.logInfo("FMLServerAboutToStartEvent - running earlyWorldCreationCommands");
                WorldPrimerCommandSender.instance().runCommands(null, Configs.earlyWorldCreationCommands);
            }
        }

        if (Configs.enableEarlyWorldLoadingCommands)
        {
            WorldPrimer.logInfo("FMLServerAboutToStartEvent - running earlyWorldLoadingCommands");
            WorldPrimerCommandSender.instance().runCommands(null, Configs.earlyWorldLoadingCommands);
        }
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event)
    {
        logInfo("FMLServerStartedEvent");
        World world = FMLCommonHandler.instance().getMinecraftServerInstance().worlds[0];

        if (Configs.enableDimensionLoadTracking &&
            Configs.enablePostWorldCreationCommands &&
            DimensionLoadTracker.instance().getServerStartCount() == 0)
        {
            WorldPrimer.logInfo("FMLServerStartedEvent - running postWorldCreationCommands");
            WorldPrimerCommandSender.instance().runCommands(world, Configs.postWorldCreationCommands);
        }

        // Increment the server start count
        DimensionLoadTracker.instance().serverStarted();

        if (Configs.enablePostWorldLoadingCommands)
        {
            WorldPrimer.logInfo("FMLServerStartedEvent - running postWorldLoadingCommands");
            WorldPrimerCommandSender.instance().runCommands(world, Configs.postWorldLoadingCommands);
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
