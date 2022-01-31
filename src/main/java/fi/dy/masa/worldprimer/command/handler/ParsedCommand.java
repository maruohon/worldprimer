package fi.dy.masa.worldprimer.command.handler;

import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.WorldPrimer;
import fi.dy.masa.worldprimer.command.parser.ExpressionParser;
import fi.dy.masa.worldprimer.command.substitution.CommandContext;
import fi.dy.masa.worldprimer.command.substitution.StringSubstitution;
import fi.dy.masa.worldprimer.command.util.WorldPrimerCommandSender;

public class ParsedCommand
{
    protected final ImmutableList<StringSubstitution> commandParts;
    protected final String originalString;
    @Nullable protected final Function<CommandContext, World> executionWorldFunction;
    @Nullable protected final Function<CommandContext, ICommandSender> senderFunction;
    @Nullable protected final Function<CommandContext, Entity> senderEntityFunction;
    @Nullable protected final Function<CommandContext, String> senderNameFunction;

    public ParsedCommand(ImmutableList<StringSubstitution> commandParts, String originalString, Builder builder)
    {
        this.commandParts = commandParts;
        this.originalString = originalString;
        this.executionWorldFunction = builder.executionWorldFunction;
        this.senderFunction = builder.senderFunction;
        this.senderEntityFunction = builder.senderEntityFunction;
        this.senderNameFunction = builder.senderNameFunction;
    }

    public String getOriginalString()
    {
        return this.originalString;
    }

    public String getCommand(CommandContext ctx)
    {
        if (this.commandParts.size() == 1)
        {
            return this.commandParts.get(0).evaluate(ctx);
        }

        StringBuilder sb = new StringBuilder();

        for (StringSubstitution provider : this.commandParts)
        {
            sb.append(provider.evaluate(ctx));
        }

        return sb.toString();
    }

    protected String getExecutionDebugMessage(String commandStr, String originalStr)
    {
        if (Objects.equals(commandStr, originalStr))
        {
            return String.format("Executing command '%s'", commandStr);
        }
        else
        {
            return String.format("Executing substituted command '%s' (original: '%s')", commandStr, originalStr);
        }
    }

    protected void logExecutionDebugMessage(String commandStr, String originalStr)
    {
        WorldPrimer.logInfo(this.getExecutionDebugMessage(commandStr, originalStr));
    }

    @Nullable
    public World getExecutionWorld(CommandContext ctx)
    {
        return this.executionWorldFunction != null ? this.executionWorldFunction.apply(ctx) : null;
    }

    @Nullable
    public ICommandSender getCommandSender(CommandContext ctx)
    {
        return this.senderFunction != null ? this.senderFunction.apply(ctx) : null;
    }

    @Nullable
    public Entity getCommandSenderEntity(CommandContext ctx)
    {
        return this.senderEntityFunction != null ? this.senderEntityFunction.apply(ctx) : null;
    }

    @Nullable
    public String getCommandSenderName(CommandContext ctx)
    {
        return this.senderNameFunction != null ? this.senderNameFunction.apply(ctx) : null;
    }

    public void execute(CommandContext ctx, ExpressionParser parser)
    {
        String commandStr = this.getCommand(ctx);
        this.logExecutionDebugMessage(commandStr, this.getOriginalString());
        WorldPrimerCommandSender.INSTANCE.executeCommand(this, commandStr, ctx);
    }

    @Override
    public String toString()
    {
        return String.format("ParsedCommand{commandParts='%s',originalString='%s',executionWorldFunction=%s," +
                             "senderFunction=%s,senderEntityFunction=%s,senderNameFunction=%s}",
                             this.commandParts, this.originalString,
                             this.executionWorldFunction, this.senderFunction,
                             this.senderEntityFunction, this.senderNameFunction);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        @Nullable protected Function<CommandContext, World> executionWorldFunction;
        @Nullable protected Function<CommandContext, ICommandSender> senderFunction;
        @Nullable protected Function<CommandContext, Entity> senderEntityFunction;
        @Nullable protected Function<CommandContext, String> senderNameFunction;

        public Builder()
        {
        }

        public Builder withExecutionWorldFunction(@Nullable Function<CommandContext, World> executionWorldFunction)
        {
            this.executionWorldFunction = executionWorldFunction;
            return this;
        }

        public Builder withSenderFunction(@Nullable Function<CommandContext, ICommandSender> senderFunction)
        {
            this.senderFunction = senderFunction;
            return this;
        }

        public Builder withSenderEntityFunction(@Nullable Function<CommandContext, Entity> senderEntityFunction)
        {
            this.senderEntityFunction = senderEntityFunction;
            return this;
        }

        public Builder withSenderNameFunction(@Nullable Function<CommandContext, String> senderNameFunction)
        {
            this.senderNameFunction = senderNameFunction;
            return this;
        }

        public ParsedCommand build(ImmutableList<StringSubstitution> commandParts, String originalString)
        {
            return new ParsedCommand(commandParts, originalString, this);
        }
    }
}
