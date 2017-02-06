package fi.dy.masa.worldprimer.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fi.dy.masa.worldprimer.reference.Reference;

public class Configs
{
    static File configurationFile;
    static Configuration config;

    public static final String CATEGORY_GENERIC = "Generic";

    public static boolean enableLoggingInfo;
    public static String[] worldCreationCommands;

    @SubscribeEvent
    public void onConfigChangedEvent(OnConfigChangedEvent event)
    {
        if (Reference.MOD_ID.equals(event.getModID()))
        {
            reloadConfigs();
        }
    }

    public static void loadConfigsFromFile(File configFile)
    {
        configurationFile = configFile;
        config = new Configuration(configurationFile, null, false);
        config.load();

        reloadConfigs();
    }

    public static void reloadConfigs()
    {
        loadConfigs(config);
    }

    private static void loadConfigs(Configuration conf)
    {
        Property prop;

        prop = conf.get(CATEGORY_GENERIC, "enableDebugLogging", false).setRequiresMcRestart(false);
        prop.setComment("Enables more verbose logging");
        enableLoggingInfo = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "worldCreationCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run on initial world creation");
        worldCreationCommands = prop.getStringList();

        if (conf.hasChanged())
        {
            conf.save();
        }
    }
}
