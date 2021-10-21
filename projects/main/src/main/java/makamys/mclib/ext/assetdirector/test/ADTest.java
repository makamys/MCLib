package makamys.mclib.ext.assetdirector.test;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import makamys.mclib.core.MCLib;
import makamys.mclib.ext.assetdirector.AssetDirectorAPI;
import cpw.mods.fml.common.event.FMLConstructionEvent;

@Mod(modid = "ADTest", version = "0.0")
public class ADTest {
    
    @EventHandler
    public void onConstruction(FMLConstructionEvent event) {
        MCLib.init();
        AssetDirectorAPI.register();
    }
    
}