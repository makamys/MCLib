package makamys.mclib.sloppydeploader.test;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import makamys.mclib.sloppydeploader.SloppyDepLoader;
import makamys.mclib.sloppydeploader.SloppyDependency;

@Mod(modid = SDLTest.MODID, version = "0.0")
public class SDLTest {
    
    static {
        SloppyDepLoader.addDependenciesForMod(Loader.instance().activeModContainer().getModId(),
                new SloppyDependency("https://github.com/makamys/UpdateCheckLib/releases/download/v" + SDLTest.UCL_VERSION,
                        "UpdateCheckLib-" + Loader.MC_VERSION + "-" + SDLTest.UCL_VERSION + ".jar",
                        "makamys.mclib.updatecheck.UpdateCheckLib"),
                new SloppyDependency("garbage url",
                        "jar-that-will-never-exist-1.7.10-0.0.jar",
                        "class.that.will.never.Exist")
                );
    }
    
    public static final String UCL_VERSION = "1.0";
    
    public static final String MODID = "SloppyDepLoaderTest"; 
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Loader.instance().activeModContainer();
        SloppyDepLoader.preInit();
    }
    
}
