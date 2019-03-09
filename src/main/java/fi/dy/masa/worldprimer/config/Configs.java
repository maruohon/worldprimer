package fi.dy.masa.worldprimer.config;

import java.io.File;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fi.dy.masa.worldprimer.reference.Reference;
import fi.dy.masa.worldprimer.util.TimedCommands;

public class Configs
{
    static File configurationFile;
    static Configuration config;

    public static final String CATEGORY_COMMANDS = "Commands";
    public static final String CATEGORY_GENERIC = "Generic";
    public static final String CATEGORY_TOGGLES = "Toggles";

    public static boolean enableLoggingInfo;
    public static boolean enableDataTracking;
    public static boolean enableChiselsAndBitsCrossWorldFormat;
    public static boolean enableDimensionLoadingCommands;
    public static boolean enableEarlyWorldCreationCommands;
    public static boolean enableEarlyWorldLoadingCommands;
    public static boolean enablePostWorldCreationCommands;
    public static boolean enablePostWorldLoadingCommands;
    public static boolean enableTimedCommands;

    public static boolean enablePlayerDeathCommands;
    public static boolean enablePlayerChangedDimensionEnterCommands;
    public static boolean enablePlayerChangedDimensionLeaveCommands;
    public static boolean enablePlayerJoinCommands;
    public static boolean enablePlayerQuitCommands;
    public static boolean enablePlayerRespawnCommands;

    public static String[] dimensionLoadingCommands;
    public static String[] earlyWorldCreationCommands;
    public static String[] earlyWorldLoadingCommands;
    public static String[] postWorldCreationCommands;
    public static String[] postWorldLoadingCommands;
    public static String[] timedCommands;

    public static String[] playerDeathCommands;
    public static String[] playerChangedDimensionEnterCommands;
    public static String[] playerChangedDimensionLeaveCommands;
    public static String[] playerJoinCommands;
    public static String[] playerQuitCommands;
    public static String[] playerRespawnCommands;

    @SubscribeEvent
    public void onConfigChangedEvent(OnConfigChangedEvent event)
    {
        if (Reference.MOD_ID.equals(event.getModID()))
        {
            loadConfigs(config);
        }
    }

    public static void loadConfigsFromFile(File configFile)
    {
        configurationFile = configFile;
        loadConfigsFromFile();
    }

    public static void loadConfigsFromFile()
    {
        config = new Configuration(configurationFile, null, true);
        config.load();

        loadConfigs(config);
    }

    private static void loadConfigs(Configuration conf)
    {
        ConfigCategory category = conf.getCategory(CATEGORY_COMMANDS);
        category.setComment("Command documentation:\n" +
                            "There are a few substitutions available to use in the commands:\n" +
                            "Basic number substitutions from world data:\n{DIMENSION}\n{SPAWN_X}\n{SPAWN_Y}\n{SPAWN_Z}.\n\n" +
                            "Also {SPAWN_POINT_X} etc. that return the spawn point instead of the possible spawn coordinate (like in the End).\n" +
                            "Any occurences of those strings will be replaced by the current dimension ID,\n" +
                            "or the coordinates of the spawn point respectively.\n\n" +

                            "Random numbers, integer and double type: {RAND:min,max},\n" +
                            "for example {RAND:5,15} or {RAND:1.2,3.9} (the max value is exclusive)\n\n" +

                            "Real time/clock values:\n" +
                            "\t{TIME_Y} => year   (4 digits: 2017)\n" +
                            "\t{TIME_M} => month  (2 digits: 03)\n" +
                            "\t{TIME_D} => day    (2 digits: 04)\n" +
                            "\t{TIME_H} => hour   (2 digits: 09)\n" +
                            "\t{TIME_I} => minute (2 digits: 05)\n" +
                            "\t{TIME_S} => second (2 digits: 07)\n" +
                            "\t{TIME_TICK} => current world total time in ticks\n" +
                            "\t{TIME_TICK_DAY} => current world/day time in ticks\n\n" +

                            "The y-coordinate of the top-most block in the world in the given coordinates\n" +
                            "(actually the air block above it): {TOP_Y:x,z} for example: {TOP_Y:-37,538}\n\n" +

                            "The x y z coordinates of the top-most block in the world in a random location around a given x,z location\n" +
                            "(again, actually the air block above it): {TOP_Y_RAND:x,z;x-range,z-range}\n" +
                            "for example: {TOP_Y_RAND:-37,538;32,32} would be the top block at a random location within\n" +
                            "32 blocks of x = -37, z = 538. That substitution will be replaced with a string like '-49 72 544' (without the quotes)\n\n" +

                            "For the player-specific commands, the following substitutions are available:\n" +
                            "{PLAYER_X}, {PLAYER_Y}, {PLAYER_Z} and {PLAYER_NAME}\n\n" +
                            "Also {PLAYER_BED_X}, {PLAYER_BED_Y} and {PLAYER_BED_Z} for the last set (bed) spawn point.\n" +
                            "There are also {PLAYER_BED_SPAWN_X}, {PLAYER_BED_SPAWN_Y} and {PLAYER_BED_SPAWN_Z} that also check that the bed exists,\n" +
                            "or otherwise they will revert to the fallback world spawn point.\n" +
                            "Note that these will thus load that one chunk to check for the bed.\n\n" +

                            "The player-specific commands can be targeted to specific counts (like the 6th respawn for example)\n" +
                            "or a multiple of a count (similarly to the dim-loading-command prefix)\n" +
                            "by prefixing the command like so: 'worldprimer-tracked-nth <count> <actual command>'\n" +
                            "For example in the playerRespawnCommands:\n" +
                            "worldprimer-tracked-nth 3 say The player {PLAYER_NAME} has respawned for the third time at {PLAYER_X}, {PLAYER_Y}, {PLAYER_Z}\n" +
                            "worldprimer-tracked-nth %5 say The player {PLAYER_NAME} has respawned for some multiple of 5 times\n\n" +

                            "Note that the above only applies to the other player-specific commands, but not the playerChangedDimension commands.\n" +
                            "For the playerChangedDimension commands, the same format works instead as for the dimension loading commands:\n" +
                            "worldprimer-dim-command-nth 3 1 say The player {PLAYER_NAME} has entered The End (dimension {DIMENSION}) for the third time\n\n" +

                            "The substitutions also support very basic arithmetic operations [+-*/].\nSo you can do for example:\n" +
                            "fill {SPAWN_X}-2 {SPAWN_Y}+3 {SPAWN_Z}-2 {SPAWN_X}+2 {SPAWN_Y}+7 {SPAWN_Z}+2 minecraft:emerald_block\n\n" +

                            "Note however that there is no actual order of operations/priorities/grouping.\n" +
                            "The commands are parsed/substituted from the left, and if there is an arithmetic\n" +
                            "operation immediately following the closing curly brace of a substitution, then the value\n" +
                            "of the following string interpreted as a number is added to the substituted value.\n" +
                            "That string following the arithmetic operation is first substituted recursively though,\n" +
                            "which means that the order of operations is actually starting from the right.\n\n" +

                            "Both the substitutions and the arithmetic operations following them can be escaped by a preceding backslash '\\',\n" +
                            "if it should actually appear in the command as-is and not be substituted.\n" +
                            "For example: 'say Foo bar \\{SPANW_X}' would become 'say Foo bar {SPANW_X}' in the final command.\n" +
                            "Or 'say Foo bar {SPAWN_X}\\+16' would become 'say Foo bar 34+16', assuming the spawn x-coordinate is 34.\n\n" +

                            "Note, that the earlyWorldCreationCommands and the earlyWorldLoadingCommands\n" +
                            "DO NOT have a world available yet, so the substitutions will NOT happen for those commands.\n" +
                            "Thus, those commands also can't do anything that would require a world.\n" +
                            "An example of this is setting the game rules - those are kept in the WorldInfo object,\n" +
                            "which is stored in the World, so the overworld specifically needs to be loaded for changing any game rules.\n\n" +

                            "Additionally, the postWorldCreationCommands and the postWorldLoadingCommands will use\n" +
                            "the Overworld (or whichever world is dimension 0) for the substitutions.\n\n" +

                            "So it's mostly the dimension loading commands that benefit from the {DIMENSION} substitution.\n\n" +

                            "Note also, that by default in vanilla/Forge, ALL dimensions use the WorldInfo from the overworld,\n" +
                            "which means that they will have the exact same spawn coordinates and game rules etc. as the overworld.\n" +
                            "Some mods may change this so that dimensions can have separate spawn points, game rules etc.\n" +
                            "One such mod is Just Enough Dimensions.");

        Property prop;

        prop = conf.get(CATEGORY_GENERIC, "enableChiselsAndBitsCrossWorldFormat", true).setRequiresMcRestart(false);
        prop.setComment("Enables saving any Chisels & Bits blocks in the cross-world compatible format\nin the 'create-structure' command when using the Schematic format");
        enableChiselsAndBitsCrossWorldFormat = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enableDebugLogging", false).setRequiresMcRestart(false);
        prop.setComment("Enables verbose logging for debug purposes");
        enableLoggingInfo = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enableDataTracking", true).setRequiresMcRestart(false);
        prop.setComment("Enables tracking of dimension load counts, player join counts etc. by storing the counts in a file in worlddir/worldprimer/data_tracker.nbt");
        enableDataTracking = prop.getBoolean();


        /*** COMMAND TOGGLES ***/

        prop = conf.get(CATEGORY_TOGGLES, "enableDimensionLoadingCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables the dimension loading commands");
        enableDimensionLoadingCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_TOGGLES, "enableEarlyWorldCreationCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables early world creation commands, which are executed before any dimensions\n" +
                        "have been loaded and thus before any chunks have been generated or loaded.");
        enableEarlyWorldCreationCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_TOGGLES, "enableEarlyWorldLoadingCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables early world loading commands, which are executed once at each server start,\n"+
                        "before the overworld spawn chunks have been loaded.");
        enableEarlyWorldLoadingCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_TOGGLES, "enablePostWorldCreationCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables late world creation commands, which are executed after the overworld spawn chunks have been generated");
        enablePostWorldCreationCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_TOGGLES, "enablePostWorldLoadingCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables late world loading commands, which are executed once at each server start,\n"+
                        "after the overworld spawn chunks have been loaded.");
        enablePostWorldLoadingCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_TOGGLES, "enableTimedCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables running commands tied to the world time");
        enableTimedCommands = prop.getBoolean();

        /**** player commands ****/

        prop = conf.get(CATEGORY_TOGGLES, "enablePlayerJoinCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables player join commands");
        enablePlayerJoinCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_TOGGLES, "enablePlayerQuitCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables running the playerQuitCommands");
        enablePlayerQuitCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_TOGGLES, "enablePlayerDeathCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables running the playerDeathCommands");
        enablePlayerDeathCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_TOGGLES, "enablePlayerRespawnCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables running the playerRespawnCommands");
        enablePlayerRespawnCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_TOGGLES, "enablePlayerChangedDimensionEnterCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables running the playerChangedDimensionEnterCommands");
        enablePlayerChangedDimensionEnterCommands = prop.getBoolean();

        prop = conf.get(CATEGORY_TOGGLES, "enablePlayerChangedDimensionLeaveCommands", false).setRequiresMcRestart(false);
        prop.setComment("Enables running the playerChangedDimensionLeaveCommands");
        enablePlayerChangedDimensionLeaveCommands = prop.getBoolean();


        /*** COMMANDS ***/

        prop = conf.get(CATEGORY_COMMANDS, "dimensionLoadingCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run when a dimension gets loaded.\n" +
                        "You can target these to only be run when a specific dimension loads\n" +
                        "by specifying the command as 'worldprimer-dim-command <dimId> <command>'.\n" +
                        "So for example: 'worldprimer-dim-command 1 say The End has loaded!'.\n" +
                        "You can also run a command only when a dimension loads for specific number of times:\n" +
                        "'worldprimer-dim-command-nth <load count> <dim id> <command>'.\n" +
                        "This would run the command only when the dimension loads for the 'load count'-th time.\n" +
                        "The count is incremented before the commands are parsed, so in other words the first load is 1, not 0.\n" +
                        "You can also run the command every count-th time the dimension loads, by prefixing the count with a '%', so for example\n" +
                        "worldprimer-dim-command-nth %5 1 say The End has loaded some multiple of 5 times!");
        dimensionLoadingCommands = prop.getStringList();

        prop = conf.get(CATEGORY_COMMANDS, "earlyWorldCreationCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run on initial world creation, before the spawn chunks have been generated or loaded.\n" +
                        "If dimension load tracking is enabled, then this happens even before any dimensions have been loaded/initialized yet.");
        earlyWorldCreationCommands = prop.getStringList();

        prop = conf.get(CATEGORY_COMMANDS, "earlyWorldLoadingCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run every time the world gets loaded.\n" +
                        "These are run when the server is starting, before any worlds have been loaded.");
        earlyWorldLoadingCommands = prop.getStringList();

        prop = conf.get(CATEGORY_COMMANDS, "postWorldCreationCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run on initial world creation, after the spawn chunks have been generated and loaded.");
        postWorldCreationCommands = prop.getStringList();

        prop = conf.get(CATEGORY_COMMANDS, "postWorldLoadingCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run every time the world gets loaded.\n" +
                        "These are run when the server has started and the overworld spawn chunks have been loaded.");
        postWorldLoadingCommands = prop.getStringList();

        prop = conf.get(CATEGORY_COMMANDS, "timedCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run based on the world time.\n" +
                        "Must be in the format: 'worldprimer-timed-command <time> <dimension> <command>',\n" +
                        "where <time> is the total world time in ticks when the command should run.\n" +
                        "The time can be prefixed with a '%' to make it run periodically, with that interval (basically a modulo).\n" +
                        "With the periodic time, you can also use offsets, like so:\n" +
                        "worldprimer-timed-command %1200-80 0 say Something happens in 4 seconds!\n" +
                        "worldprimer-timed-command %1200 0 say Something happens now!\n" +
                        "worldprimer-timed-command %1200+80 0 Say something happened 4 seconds ago!");
        timedCommands = prop.getStringList();
        TimedCommands.setTimedCommands(timedCommands);
        TimedCommands.updateAllTimedCommands(false);

        /**** player commands ****/

        prop = conf.get(CATEGORY_COMMANDS, "playerJoinCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run when a player joins (connects to) the server");
        playerJoinCommands = prop.getStringList();

        prop = conf.get(CATEGORY_COMMANDS, "playerQuitCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run when a player disconnects from the server");
        playerQuitCommands = prop.getStringList();

        prop = conf.get(CATEGORY_COMMANDS, "playerDeathCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run when a player dies");
        playerDeathCommands = prop.getStringList();

        prop = conf.get(CATEGORY_COMMANDS, "playerRespawnCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run when a player respawns after dying");
        playerRespawnCommands = prop.getStringList();

        prop = conf.get(CATEGORY_COMMANDS, "playerChangedDimensionEnterCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run when a player enters a dimension while changing dimensions.\n" +
                        "Note that these will NOT run when a player joins the game or respawns after dying.\n" +
                        "You can use the 'worldprimer-dim-command <dimId>' (or the -nth variant) prefix to target entering a specific dimension.");
        playerChangedDimensionEnterCommands = prop.getStringList();

        prop = conf.get(CATEGORY_COMMANDS, "playerChangedDimensionLeaveCommands", new String[0]).setRequiresMcRestart(false);
        prop.setComment("Commands to run when a player leaves a dimension while changing dimensions.\n" +
                        "Note that these will NOT run when a player leaves the game/server.\n" +
                        "You can use the 'worldprimer-dim-command <dimId>' (or the -nth variant) prefix to target leaving a specific dimension.\n" +
                        "NOTE: These commands will run AFTER the player is already in the new dimension!!");
        playerChangedDimensionLeaveCommands = prop.getStringList();

        if (conf.hasChanged())
        {
            conf.save();
        }
    }
}
