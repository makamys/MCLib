package makamys.mclib.ext.assetdirector.test;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import makamys.mclib.core.MCLib;
import makamys.mclib.ext.assetdirector.AssetDirectorAPI;
import net.minecraft.init.Blocks;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = "ADTest", version = "0.0")
public class ADTest {
    
    @EventHandler
    public void onConstruction(FMLConstructionEvent event) {
        MCLib.init();
        AssetDirectorAPI.register();
    }
    
    @EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        Blocks.gold_block.setBlockTextureName("minecraft_1.17:gold_block");
    }
    
}