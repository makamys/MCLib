package makamys.sloppydeploader.test;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = SDLTest.MODID, version = "0.0")
public class SDLTest {
    
    public static final String MODID = "SloppyDepLoaderTest"; 
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        System.out.println("hello");
    }
    
}
