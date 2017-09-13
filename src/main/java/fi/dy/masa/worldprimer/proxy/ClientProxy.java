package fi.dy.masa.worldprimer.proxy;

import cpw.mods.fml.common.FMLCommonHandler;
import fi.dy.masa.worldprimer.config.Configs;

public class ClientProxy extends CommonProxy
{
    @Override
    public void registerEventHandlers()
    {
        super.registerEventHandlers();

        FMLCommonHandler.instance().bus().register(new Configs());
    }
}
