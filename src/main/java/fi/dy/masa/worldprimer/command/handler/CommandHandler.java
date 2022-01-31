package fi.dy.masa.worldprimer.command.handler;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.parser.CommandParser;
import fi.dy.masa.worldprimer.command.parser.ExpressionParser;
import fi.dy.masa.worldprimer.command.parser.SubstitutionParser;
import fi.dy.masa.worldprimer.command.substitution.CommandContext;
import fi.dy.masa.worldprimer.config.Configs;

public class CommandHandler
{
    public static final CommandHandler INSTANCE = new CommandHandler();

    private final SubstitutionParser substitutionParser;
    private final ExpressionParser expressionParser;

    protected CommandList dimensionLoadingCommands;
    protected CommandList dimensionUnloadingCommands;
    protected CommandList earlyWorldCreationCommands;
    protected CommandList earlyWorldLoadingCommands;
    protected CommandList postWorldCreationCommands;
    protected CommandList postWorldLoadingCommands;
    protected CommandList timedCommands;

    protected CommandList playerChangedDimensionEnterCommands;
    protected CommandList playerChangedDimensionLeaveCommands;
    protected CommandList playerDeathCommands;
    protected CommandList playerJoinCommands;
    protected CommandList playerQuitCommands;
    protected CommandList playerRespawnCommands;

    protected CommandHandler()
    {
        this.substitutionParser = SubstitutionParser.INSTANCE;
        this.expressionParser = new ExpressionParser(this.substitutionParser);
    }

    public void executeCommandsWithSimpleContext(CommandType type, @Nullable World world)
    {
        this.executeCommands(type, new CommandContext(world, null, 0));
    }

    public void executeCommands(CommandType type, CommandContext ctx)
    {
        type.getCommandList(this).execute(ctx, this.expressionParser);
    }

    public void executeCommand(ParsedCommand command, CommandContext ctx)
    {
        command.execute(ctx, this.expressionParser);
    }

    public void rebuildCommands()
    {
        for (CommandType type : CommandType.VALUES)
        {
            CommandList list = this.buildCommandList(type.getName(), type.getConfig());
            type.setCommandList(this, list);
        }
    }

    protected CommandList buildCommandList(String name, String[] originalCommands)
    {
        return new CommandList(name, this.buildCommands(name, originalCommands));
    }

    protected ImmutableList<ParsedCommand> buildCommands(String name, String[] originalCommands)
    {
        ImmutableList.Builder<ParsedCommand> builder = ImmutableList.builder();

        for (String str : originalCommands)
        {
            ParsedCommand cmd = this.buildCommand(name, str);

            if (cmd != null)
            {
                builder.add(cmd);
            }
        }

        return builder.build();
    }

    @Nullable
    public ParsedCommand buildCommand(String name, String rawCommandString)
    {
        rawCommandString = rawCommandString.trim();

        if (rawCommandString.isEmpty() || rawCommandString.charAt(0) == '#')
        {
            return null;
        }

        try
        {
            ParsedCommand cmd = CommandParser.INSTANCE.parseCommand(rawCommandString, this.substitutionParser, this.expressionParser);

            if (cmd != null)
            {
                System.out.printf("CommandHandler#buildCommands(): Parsed command: '%s' as %s\n", cmd.getOriginalString(), cmd.getClass().getName());
                return cmd;
            }
            else
            {
                WorldPrimer.LOGGER.warn("CommandHandler#buildCommands(): Invalid '{}' command '{}'", name, rawCommandString);
            }
        }
        catch (Exception e)
        {
            WorldPrimer.LOGGER.warn("CommandHandler#buildCommands(): Failed to parse '{}' command '{}'", name, rawCommandString);
            WorldPrimer.LOGGER.warn("CommandHandler#buildCommands():   => {}", e.getMessage(), e);
        }

        return null;
    }

    public enum CommandType
    {
        DIM_LOADING             ("Dimension Loading Commands",              () -> Configs.dimensionLoadingCommands,            (ch) -> ch.dimensionLoadingCommands,            (ch, cl) -> ch.dimensionLoadingCommands = cl),
        DIM_UNLOADING           ("Dimension Unloading Commands",            () -> Configs.dimensionUnloadingCommands,          (ch) -> ch.dimensionUnloadingCommands,          (ch, cl) -> ch.dimensionUnloadingCommands = cl),
        EARLY_WORLD_CREATION    ("Early World Creation Commands",           () -> Configs.earlyWorldCreationCommands,          (ch) -> ch.earlyWorldCreationCommands,          (ch, cl) -> ch.earlyWorldCreationCommands = cl),
        POST_WORLD_CREATION     ("Early World Loading Commands",            () -> Configs.earlyWorldLoadingCommands,           (ch) -> ch.postWorldCreationCommands,           (ch, cl) -> ch.postWorldCreationCommands = cl),
        EARLY_WORLD_LOAD        ("Post World Creation Commands",            () -> Configs.postWorldCreationCommands,           (ch) -> ch.earlyWorldLoadingCommands,           (ch, cl) -> ch.earlyWorldLoadingCommands = cl),
        POST_WORLD_LOAD         ("Post World Loading Commands",             () -> Configs.postWorldLoadingCommands,            (ch) -> ch.postWorldLoadingCommands,            (ch, cl) -> ch.postWorldLoadingCommands = cl),
        TIMED                   ("Timed Commands",                          () -> Configs.timedCommands,                       (ch) -> ch.timedCommands,                       (ch, cl) -> ch.timedCommands = cl),
        PLAYER_DIM_ENTER        ("Player Changed Dimension Enter Commands", () -> Configs.playerChangedDimensionEnterCommands, (ch) -> ch.playerChangedDimensionEnterCommands, (ch, cl) -> ch.playerChangedDimensionEnterCommands = cl),
        PLAYER_DIM_LEAVE        ("Player Changed Dimension Leave Commands", () -> Configs.playerChangedDimensionLeaveCommands, (ch) -> ch.playerChangedDimensionLeaveCommands, (ch, cl) -> ch.playerChangedDimensionLeaveCommands = cl),
        PLAYER_DEATH            ("Player Death Commands",                   () -> Configs.playerDeathCommands,                 (ch) -> ch.playerDeathCommands,                 (ch, cl) -> ch.playerDeathCommands = cl),
        PLAYER_JOIN             ("Player Join Commands",                    () -> Configs.playerJoinCommands,                  (ch) -> ch.playerJoinCommands,                  (ch, cl) -> ch.playerJoinCommands = cl),
        PLAYER_QUIT             ("Player Quit Commands",                    () -> Configs.playerQuitCommands,                  (ch) -> ch.playerQuitCommands,                  (ch, cl) -> ch.playerQuitCommands = cl),
        PLAYER_RESPAWN          ("Player Respawn Commands",                 () -> Configs.playerRespawnCommands,               (ch) -> ch.playerRespawnCommands,               (ch, cl) -> ch.playerRespawnCommands = cl);

        public static final ImmutableList<CommandType> VALUES = ImmutableList.copyOf(values());

        private final String name;
        private final Supplier<String[]> configSource;
        private final Function<CommandHandler, CommandList> listFetcher;
        private final BiConsumer<CommandHandler, CommandList> listSetter;

        CommandType(String name,
                    Supplier<String[]> configSource,
                    Function<CommandHandler, CommandList> listFetcher,
                    BiConsumer<CommandHandler, CommandList> listSetter)
        {
            this.name = name;
            this.configSource = configSource;
            this.listFetcher = listFetcher;
            this.listSetter = listSetter;
        }

        public String getName()
        {
            return this.name;
        }

        private String[] getConfig()
        {
            return this.configSource.get();
        }

        public CommandList getCommandList(CommandHandler handler)
        {
            return this.listFetcher.apply(handler);
        }

        private void setCommandList(CommandHandler handler, CommandList list)
        {
            this.listSetter.accept(handler, list);
        }
    }
}
