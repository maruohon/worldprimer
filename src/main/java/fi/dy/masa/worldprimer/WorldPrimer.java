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
import fi.dy.masa.worldprimer.command.handler.CommandHandler;
import fi.dy.masa.worldprimer.config.Configs;
import fi.dy.masa.worldprimer.proxy.IProxy;
import fi.dy.masa.worldprimer.reference.Reference;
import fi.dy.masa.worldprimer.util.DataTracker;

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

    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);
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

        CommandHandler.INSTANCE.rebuildCommands();

        if (Configs.enableDataTracking)
        {
            // We need to read the data before any dimension loads
            DataTracker.INSTANCE.readFromDisk(worldDir);

            int count = DataTracker.INSTANCE.getServerStartCount();
            logInfo("FMLServerAboutToStartEvent - server starting, previous start count: {}", count);

            // The server start count is incremented in the FMLServerStartedEvent,
            // so on world creation it will be 0 here
            if (Configs.enableEarlyWorldCreationCommands && count == 0)
            {
                WorldPrimer.logInfo("FMLServerAboutToStartEvent - running earlyWorldCreationCommands");
                CommandHandler.INSTANCE.executeCommandsWithSimpleContext(CommandHandler.CommandType.EARLY_WORLD_CREATION, null);
            }
        }

        if (Configs.enableEarlyWorldLoadingCommands)
        {
            WorldPrimer.logInfo("FMLServerAboutToStartEvent - running earlyWorldLoadingCommands");
            CommandHandler.INSTANCE.executeCommandsWithSimpleContext(CommandHandler.CommandType.EARLY_WORLD_LOAD, null);
        }
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        logInfo("FMLServerStartingEvent");
        event.registerServerCommand(commandWorldPrimer);
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event)
    {
        logInfo("FMLServerStartedEvent");

        if (Configs.enableDataTracking &&
            Configs.enablePostWorldCreationCommands &&
            DataTracker.INSTANCE.getServerStartCount() == 0)
        {
            WorldPrimer.logInfo("FMLServerStartedEvent - running postWorldCreationCommands");
            World world = FMLCommonHandler.instance().getMinecraftServerInstance().worlds[0];
            CommandHandler.INSTANCE.executeCommandsWithSimpleContext(CommandHandler.CommandType.POST_WORLD_CREATION, world);
        }

        // Increment the server start count
        DataTracker.INSTANCE.incrementServerStartCount();

        if (Configs.enablePostWorldLoadingCommands)
        {
            WorldPrimer.logInfo("FMLServerStartedEvent - running postWorldLoadingCommands");
            World world = FMLCommonHandler.instance().getMinecraftServerInstance().worlds[0];
            CommandHandler.INSTANCE.executeCommandsWithSimpleContext(CommandHandler.CommandType.POST_WORLD_LOAD, world);
        }
    }

    @Mod.EventHandler
    public void onServerStopped(FMLServerStoppingEvent event)
    {
        logInfo("FMLServerStoppingEvent");
    }

    public static void logInfo(String message, Object... params)
    {
        if (Configs.enableLoggingInfo)
        {
            LOGGER.info(message, params);
        }
        else
        {
            LOGGER.debug(message, params);
        }
    }

    @Mod.EventHandler
    public void onFingerPrintViolation(FMLFingerprintViolationEvent event)
    {
        // Not running in a dev environment
        if (event.isDirectory() == false)
        {
            LOGGER.warn("*********************************************************************************************");
            LOGGER.warn("*****                                    WARNING                                        *****");
            LOGGER.warn("*****                                                                                   *****");
            LOGGER.warn("*****   The signature of the mod file '{}' does not match the expected fingerprint!     *****", event.getSource().getName());
            LOGGER.warn("*****   This might mean that the mod file has been tampered with!                       *****");
            LOGGER.warn("*****   If you did not download the mod {} directly from Curse/CurseForge,       *****", Reference.MOD_NAME);
            LOGGER.warn("*****   or using one of the well known launchers, and you did not                       *****");
            LOGGER.warn("*****   modify the mod file at all yourself, then it's possible,                        *****");
            LOGGER.warn("*****   that it may contain malware or other unwanted things!                           *****");
            LOGGER.warn("*********************************************************************************************");
        }
    }
}
