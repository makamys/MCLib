package makamys.mclib.sloppydeploader.test;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import makamys.mclib.sloppydeploader.SloppyDepLoader;
import makamys.mclib.sloppydeploader.SloppyDependency;

@Mod(modid = SDLTest.MODID, version = "0.0")
public class SDLTest {
    
    public static final String MODID = "SloppyDepLoaderTest"; 
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        String uclVersion = "1.0";
        String mcVersion = Loader.MC_VERSION;
        SloppyDepLoader.addDependency(new SloppyDependency("https://github.com/makamys/UpdateCheckLib/releases/download/v" + uclVersion, "UpdateCheckLib-" + mcVersion + "-" + uclVersion + ".jar", "makamys.updatechecklib.UpdateCheckLib"));
        SloppyDepLoader.addDependency(new SloppyDependency("garbage url", "jar-that-will-never-exist-1.7.10-0.0.jar", "class.that.will.never.Exist"));
    }
    
}
