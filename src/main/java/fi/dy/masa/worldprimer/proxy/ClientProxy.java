package fi.dy.masa.worldprimer.proxy;

import net.minecraftforge.common.MinecraftForge;
import fi.dy.masa.worldprimer.config.Configs;

public class ClientProxy extends CommonProxy
{
    @Override
    public void registerEventHandlers()
    {
        super.registerEventHandlers();

        MinecraftForge.EVENT_BUS.register(new Configs());
    }
}
