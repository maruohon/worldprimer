package fi.dy.masa.worldprimer.config;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import fi.dy.masa.worldprimer.reference.Reference;

public class WorldPrimerConfigGui extends GuiConfig
{
    public WorldPrimerConfigGui(GuiScreen parent)
    {
        super(parent, getConfigElements(), Reference.MOD_ID, false, false, getTitle(parent));
    }

    @SuppressWarnings("rawtypes")
    private static List<IConfigElement> getConfigElements()
    {
        List<IConfigElement> configElements = new ArrayList<IConfigElement>();

        Configuration config = Configs.config;
        configElements.add(new ConfigElement(config.getCategory(Configs.CATEGORY_GENERIC)));

        return configElements;
    }

    private static String getTitle(GuiScreen parent)
    {
        return GuiConfig.getAbridgedConfigPath(Configs.configurationFile.toString());
    }
}
