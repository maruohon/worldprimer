package fi.dy.masa.worldprimer.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.worldprimer.util.DataTracker;

public class SubCommandSpreadPlayer extends SubCommand
{
    private static final Random RAND = new Random();

    public SubCommandSpreadPlayer(CommandWorldPrimer baseCommand)
    {
        super(baseCommand);
    }

    @Override
    public String getName()
    {
        return "spread-player";
    }

    @Override
    public void printHelpGeneric(ICommandSender sender)
    {
        this.sendMessage(sender, this.getUsageStringCommon());
    }

    @Override
    protected List<String> getTabCompletionsSub(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        List<String> list = new ArrayList<>();

        list.addAll(CommandBase.getListOfStringsMatchingLastWord(args,
                "--find-surface=",
                "--grid=",
                "--max-radius=",
                "--min-separation=",
                "--new",
                "--y=",
                "--y-offset=",
                "--y-max=",
                "--y-min="));

        HashSet<String> names = new HashSet<>(Arrays.asList(server.getOnlinePlayerNames()));

        for (int i = 0; i < args.length - 1; ++i)
        {
            names.remove(args[i]);
        }

        list.addAll(names);

        return list;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length == 0)
        {
            throwUsage(this.getUsageStringCommon());
        }

        boolean findSurface = true;
        boolean updatePosition = false;
        int maxRadius = 10000;
        int minSeparation = 500;
        int y = -1;
        int yOffset = 0;
        int yMin = -1;
        int yMax = -1;
        int gridSize = -1;
        List<String> onlinePlayers = Arrays.asList(server.getOnlinePlayerNames());
        List<String> playerNames = new ArrayList<>();

        for (String arg : args)
        {
            // TODO these are not really nice & clean...

            if (arg.startsWith("--"))
            {
                if (arg.startsWith("--find-surface="))
                {
                    findSurface = getBooleanValue(arg.substring(15));
                }
                else if (arg.startsWith("--grid="))
                {
                    gridSize = CommandBase.parseInt(arg.substring(7));
                }
                else if (arg.startsWith("--max-radius="))
                {
                    maxRadius = CommandBase.parseInt(arg.substring(13));
                }
                else if (arg.startsWith("--min-separation="))
                {
                    minSeparation = CommandBase.parseInt(arg.substring(17));
                }
                else if (arg.startsWith("--new"))
                {
                    updatePosition = true;
                }
                else if (arg.startsWith("--y="))
                {
                    y = CommandBase.parseInt(arg.substring(4));
                }
                else if (arg.startsWith("--y-offset="))
                {
                    yOffset = CommandBase.parseInt(arg.substring(11));
                }
                else if (arg.startsWith("--y-max="))
                {
                    yMax = CommandBase.parseInt(arg.substring(8));
                }
                else if (arg.startsWith("--y-min="))
                {
                    yMin = CommandBase.parseInt(arg.substring(8));
                }
                else
                {
                    throw new WrongUsageException("Unknown argument: ", arg);
                }
            }
            else if (onlinePlayers.contains(arg))
            {
                playerNames.add(arg);
            }
            else
            {
                throw new WrongUsageException("Player not found: ", arg);
            }
        }

        if (playerNames.isEmpty())
        {
            throw new WrongUsageException("No players given");
        }

        List<EntityPlayer> players = new ArrayList<>();

        for (String playerName : playerNames)
        {
            EntityPlayer player = server.getPlayerList().getPlayerByUsername(playerName);

            if (player != null)
            {
                players.add(player);
            }
            else
            {
                this.sendMessage(sender, "/worldprimer spread-player: Invalid player name '" + playerName + "'");
            }
        }

        for (EntityPlayer player : players)
        {
            int dimension = player.getEntityWorld().provider.getDimension();
            BlockPos pos = DataTracker.INSTANCE.getLastPlayerSpreadPosition(player);
            List<BlockPos> existingPositions = new ArrayList<>(DataTracker.INSTANCE.getPlayerSpreadPositions(dimension));

            player.getEntityWorld().playerEntities.forEach((p) -> existingPositions.add(new BlockPos(p)));

            if (updatePosition || pos == null)
            {
                if (gridSize > 0)
                {
                    pos = this.findFreeGridPositionXZ(gridSize, minSeparation, maxRadius, existingPositions);
                }
                else
                {
                    pos = this.findFreeRandomPositionXZ(minSeparation, maxRadius, existingPositions);
                }

                if (pos != null)
                {
                    pos = this.updatePositionY(player.getEntityWorld(), pos, findSurface, y, yMin, yMax, yOffset);
                }
            }

            if (pos != null)
            {
                String str = String.format("Spreading player '%s' to [%d, %d, %d]", player.getName(), pos.getX(), pos.getY(), pos.getZ());
                this.sendMessage(sender, str);

                player.setLocationAndAngles(pos.getX(), pos.getY(), pos.getZ(), player.prevRotationYaw, player.prevRotationPitch);
                player.setPositionAndUpdate(pos.getX(), pos.getY(), pos.getZ());

                DataTracker.INSTANCE.addPlayerSpreadPosition(player, pos);
            }
        }
    }

    @Nullable
    private BlockPos findFreeRandomPositionXZ(int minSeparation, int maxRadius, List<BlockPos> existingPositions)
    {
        for (int i = 0; i < 10000; ++i)
        {
            int x = RAND.nextInt(maxRadius) - RAND.nextInt(maxRadius);
            int z = RAND.nextInt(maxRadius) - RAND.nextInt(maxRadius);

            if (this.isClearOfPositions(x, z, minSeparation, existingPositions))
            {
                return new BlockPos(x, 64, z);
            }
        }

        return null;
    }

    @Nullable
    private BlockPos findFreeGridPositionXZ(int gridSize, int minSeparation, int maxRadius, List<BlockPos> existingPositions)
    {
        int maxR = maxRadius / gridSize;

        for (int r = 1; r <= maxR; ++r)
        {
            // west edge
            for (int x = -r * gridSize, z = -r * gridSize; z <= r * gridSize; z += gridSize)
            {
                if (this.isClearOfPositions(x, z, minSeparation, existingPositions))
                {
                    return new BlockPos(x, 64, z);
                }
            }

            // south edge
            for (int x = -r * gridSize, z = r * gridSize; x <= r * gridSize; x += gridSize)
            {
                if (this.isClearOfPositions(x, z, minSeparation, existingPositions))
                {
                    return new BlockPos(x, 64, z);
                }
            }

            // east edge
            for (int x = r * gridSize, z = r * gridSize; z >= -r * gridSize; z -= gridSize)
            {
                if (this.isClearOfPositions(x, z, minSeparation, existingPositions))
                {
                    return new BlockPos(x, 64, z);
                }
            }

            // north edge
            for (int x = r * gridSize, z = -r * gridSize; x >= -r * gridSize; x -= gridSize)
            {
                if (this.isClearOfPositions(x, z, minSeparation, existingPositions))
                {
                    return new BlockPos(x, 64, z);
                }
            }
        }

        return null;
    }

    private boolean isClearOfPositions(int x, int z, int minSeparation, List<BlockPos> existingPositions)
    {
        for (BlockPos pos : existingPositions)
        {
            double xDiff = x - pos.getX();
            double zDiff = z - pos.getZ();

            if ((xDiff * xDiff + zDiff * zDiff) < minSeparation * minSeparation)
            {
                return false;
            }
        }

        return true;
    }

    private BlockPos updatePositionY(World world, BlockPos pos, boolean findSurface, int y, int yMin, int yMax, int yOffset)
    {
        if (y != -1)
        {
            return new BlockPos(pos.getX(), y, pos.getZ());
        }
        else if (findSurface)
        {
            y = world.getTopSolidOrLiquidBlock(pos).getY() + yOffset;

            if (yMin != -1)
            {
                y = Math.max(y, yMin);
            }

            if (yMax != -1)
            {
                y = Math.min(y, yMax);
            }

            return new BlockPos(pos.getX(), y, pos.getZ());
        }

        return pos;
    }
}
