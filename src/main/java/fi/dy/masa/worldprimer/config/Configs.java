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
    public static boolean enableDimensionLoadingCommands;
    public static boolean enableDimensionLoadTracking;
    public static boolean enableWorldCreationCommands;
    public static boolean enableWorldLoadingCommands;
    public static String[] dimensionLoadingCommands;
    public static String[] worldCreationCommands;
    public static String[] worldLoadingCommands;

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

        prop = conf.get(CATEGORY_GENERIC, "enableDimensionLoadingCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables the dimension loading commands");
        enableDimensionLoadingCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enableDimensionLoadTracking", false).setRequiresMcRestart(false);
        prop.setComment("Enables tracking dimension load counts, by storing the counts in a file at worlddir/worldprimer/dim_loads.nbt");
        enableDimensionLoadTracking = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enableWorldCreationCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables the world creation commands");
        enableWorldCreationCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enableWorldLoadingCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables the world loading commands");
        enableWorldLoadingCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "dimensionLoadingCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run when a dimension gets loaded.\n" +
                        "You can target these to only be run when a specific dimension loads\n" +
                        "by starting the command with 'worldprimer-dim-command <dim id> <command>'.\n" +
                        "So for example: 'worldprimer-dim-command 1 say The End has loaded!'.\n" +
                        "You can also run a command only when a dimension loads for specific number of times:\n" +
                        "'worldprimer-dim-command-nth <dim id> <load count> <command>'.\n" +
                        "This would run the command only when the dimension loads for the 'load count'-th time.\n" +
                        "The count is incremented before the commands are parsed, so in other words the first load is 1, not 0.\n" +
                        "You can also run the command ever count-th time the dimension loads, by prefixing the count with a '%', so for example\n" +
                        "worldprimer-dim-command-nth 1 %5 say The End has loaded some multiple of 5 times!");
        dimensionLoadingCommands = prop.getStringList();

        prop = conf.get(CATEGORY_GENERIC, "worldCreationCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run on initial world creation");
        worldCreationCommands = prop.getStringList();

        prop = conf.get(CATEGORY_GENERIC, "worldLoadingCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run every time the world gets loaded (run when the overworld loads)");
        worldLoadingCommands = prop.getStringList();

        if (conf.hasChanged())
        {
            conf.save();
        }
    }
}
