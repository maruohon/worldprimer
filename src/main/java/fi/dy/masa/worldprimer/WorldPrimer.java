package fi.dy.masa.worldprimer;

import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.AnvilSaveConverter;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import fi.dy.masa.worldprimer.command.CommandWorldPrimer;
import fi.dy.masa.worldprimer.command.WorldPrimerCommandSender;
import fi.dy.masa.worldprimer.config.Configs;
import fi.dy.masa.worldprimer.proxy.IProxy;
import fi.dy.masa.worldprimer.reference.Reference;
import fi.dy.masa.worldprimer.util.CommandSubstitutions;
import fi.dy.masa.worldprimer.util.DimensionLoadTracker;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.MOD_VERSION, certificateFingerprint = Reference.FINGERPRINT,
    guiFactory = "fi.dy.masa.worldprimer.config.WorldPrimerGuiFactory",
    updateJSON = "https://raw.githubusercontent.com/maruohon/worldprimer/master/update.json",
    acceptableRemoteVersions = "*", acceptedMinecraftVersions = "1.12")
public class WorldPrimer
{
    @Mod.Instance(Reference.MOD_ID)
    public static WorldPrimer instance;

    @SidedProxy(clientSide = Reference.PROXY_CLIENT, serverSide = Reference.PROXY_SERVER)
    public static IProxy proxy;

    public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);
    public static String configDirPath;
    public static CommandWorldPrimer commandWorldPrimer;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        Configs.loadConfigsFromFile(event.getSuggestedConfigurationFile());
        configDirPath = new File(event.getModConfigurationDirectory(), Reference.MOD_ID).getAbsolutePath();
        commandWorldPrimer = new CommandWorldPrimer();
        proxy.registerEventHandlers();
    }

    @Mod.EventHandler
    public void onServerAboutToStart(FMLServerAboutToStartEvent event)
    {
        logInfo("FMLServerAboutToStartEvent");
        Configs.loadConfigsFromFile();

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

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(commandWorldPrimer);
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

    @Mod.EventHandler
    public void onServerStopped(FMLServerStoppingEvent event)
    {
        CommandSubstitutions.clearTopYCache();
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

    @Mod.EventHandler
    public void onFingerPrintViolation(FMLFingerprintViolationEvent event)
    {
        // Not running in a dev environment
        if (event.isDirectory() == false)
        {
            logger.warn("*********************************************************************************************");
            logger.warn("*****                                    WARNING                                        *****");
            logger.warn("*****                                                                                   *****");
            logger.warn("*****   The signature of the mod file '{}' does not match the expected fingerprint!     *****", event.getSource().getName());
            logger.warn("*****   This might mean that the mod file has been tampered with!                       *****");
            logger.warn("*****   If you did not download the mod {} directly from Curse/CurseForge,       *****", Reference.MOD_NAME);
            logger.warn("*****   or using one of the well known launchers, and you did not                       *****");
            logger.warn("*****   modify the mod file at all yourself, then it's possible,                        *****");
            logger.warn("*****   that it may contain malware or other unwanted things!                           *****");
            logger.warn("*********************************************************************************************");
        }
    }
}
