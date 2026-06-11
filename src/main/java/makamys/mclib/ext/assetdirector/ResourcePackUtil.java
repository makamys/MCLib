package makamys.mclib.ext.assetdirector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;

public class ResourcePackUtil {
    
    private static Map<String, IResourceManager> domainResourceManagers;
    private static IResourceManager mcResMan;
    private static List<IResourcePack> mcResPacks;
    
    public static List<IResourcePack> getMinecraftResourcePackList() {
        if(domainResourceManagers == null) {
            IResourceManager resMan = Minecraft.getMinecraft().getResourceManager();
            if(resMan instanceof SimpleReloadableResourceManager) {
                domainResourceManagers = (Map<String, IResourceManager>)ReflectionHelper.getPrivateValue(SimpleReloadableResourceManager.class, (SimpleReloadableResourceManager)resMan, "domainResourceManagers", "field_110548_a");
            } else {
                AssetDirector.LOGGER.warn("Failed to retrieve domainResourceManagers");
                domainResourceManagers = new HashMap<>();
            }
        }
        IResourceManager newMcResMan = domainResourceManagers.get("minecraft");
        
        List<IResourcePack> newMcResPacks;
        if(newMcResMan == mcResMan) {
            newMcResPacks = mcResPacks;
        } else {
            if(newMcResMan instanceof FallbackResourceManager) {
                newMcResPacks = ReflectionHelper.getPrivateValue(FallbackResourceManager.class, (FallbackResourceManager)newMcResMan, "resourcePacks", "field_110540_a");
            } else {
                AssetDirector.LOGGER.warn("Failed to retrieve resourcePacks");
                newMcResPacks = new ArrayList<>();
            }
        }
        mcResMan = newMcResMan;
        mcResPacks = newMcResPacks;
        return mcResPacks;
    }

    /**
     * Returns true if the resource pack is one that should be ignored by AssetDirector when searching for overrides of modern assets.
     */
    public static boolean isBuiltIn(IResourcePack resPack) {
        return resPack instanceof DefaultResourcePack || resPack.getPackName().equals("FMLFileResourcePack:Forge Mod Loader") || resPack.getPackName().equals("FMLFileResourcePack:Minecraft Forge");
    }
    
}
