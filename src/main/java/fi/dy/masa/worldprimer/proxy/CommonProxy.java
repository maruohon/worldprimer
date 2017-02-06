package fi.dy.masa.worldprimer.proxy;

import net.minecraftforge.common.MinecraftForge;
import fi.dy.masa.worldprimer.event.EventHandler;

public class CommonProxy implements IProxy
{
    @Override
    public void registerEventHandlers()
    {
        MinecraftForge.EVENT_BUS.register(new EventHandler());
    }
}
