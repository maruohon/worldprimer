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
    public static boolean enableEarlyWorldCreationCommands;
    public static boolean enableEarlyWorldLoadingCommands;
    public static boolean enablePostWorldCreationCommands;
    public static boolean enablePostWorldLoadingCommands;
    public static String[] dimensionLoadingCommands;
    public static String[] earlyWorldCreationCommands;
    public static String[] earlyWorldLoadingCommands;
    public static String[] postWorldCreationCommands;
    public static String[] postWorldLoadingCommands;

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
        config = new Configuration(configurationFile, null, true);
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
        prop.setComment("Enables verbose logging for debug purposes");
        enableLoggingInfo = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enableDimensionLoadingCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables the dimension loading commands");
        enableDimensionLoadingCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enableDimensionLoadTracking", true).setRequiresMcRestart(false);
        prop.setComment("Enables tracking dimension load counts, by storing the counts in a file in worlddir/worldprimer/dim_loads.nbt");
        enableDimensionLoadTracking = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enableEarlyWorldCreationCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables early world creation commands, which are executed before any dimensions\n" +
                        "have been loaded and thus before any chunks have been generated or loaded.");
        enableEarlyWorldCreationCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enableEarlyWorldLoadingCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables early world loading commands, which are executed once at each server start,\n"+
                        "before the overworld spawn chunks have been loaded.");
        enableEarlyWorldLoadingCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enablePostWorldCreationCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables late world creation commands, which are executed after the overworld spawn chunks have been generated");
        enablePostWorldCreationCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enablePostWorldLoadingCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables late world loading commands, which are executed once at each server start,\n"+
                        "after the overworld spawn chunks have been loaded.");
        enablePostWorldLoadingCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "dimensionLoadingCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run when a dimension gets loaded.\n" +
                        "You can target these to only be run when a specific dimension loads\n" +
                        "by specifying the command as 'worldprimer-dim-command <dim id> <command>'.\n" +
                        "So for example: 'worldprimer-dim-command 1 say The End has loaded!'.\n" +
                        "You can also run a command only when a dimension loads for specific number of times:\n" +
                        "'worldprimer-dim-command-nth <dim id> <load count> <command>'.\n" +
                        "This would run the command only when the dimension loads for the 'load count'-th time.\n" +
                        "The count is incremented before the commands are parsed, so in other words the first load is 1, not 0.\n" +
                        "You can also run the command every count-th time the dimension loads, by prefixing the count with a '%', so for example\n" +
                        "worldprimer-dim-command-nth 1 %5 say The End has loaded some multiple of 5 times!");
        dimensionLoadingCommands = prop.getStringList();

        prop = conf.get(CATEGORY_GENERIC, "earlyWorldCreationCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run on initial world creation, before the spawn chunks have been generated or loaded.\n" +
                        "If dimension load tracking is enabled, then this happens even before any dimensions have been loaded/initialized yet.");
        earlyWorldCreationCommands = prop.getStringList();

        prop = conf.get(CATEGORY_GENERIC, "earlyWorldLoadingCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run every time the world gets loaded.\n" +
                        "These are run when the server is starting, before any worlds have been loaded.");
        earlyWorldLoadingCommands = prop.getStringList();

        prop = conf.get(CATEGORY_GENERIC, "postWorldCreationCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run on initial world creation, after the spawn chunks have been generated and loaded.");
        postWorldCreationCommands = prop.getStringList();

        prop = conf.get(CATEGORY_GENERIC, "postWorldLoadingCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run every time the world gets loaded.\n" +
                        "These are run when the server has started and the overworld spawn chunks have been loaded.");
        postWorldLoadingCommands = prop.getStringList();

        if (conf.hasChanged())
        {
            conf.save();
        }
    }
}
