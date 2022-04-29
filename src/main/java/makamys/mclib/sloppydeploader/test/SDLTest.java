package makamys.mclib.sloppydeploader.test;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import makamys.mclib.core.MCLib;
import makamys.mclib.sloppydeploader.SloppyDepLoaderAPI;
import makamys.mclib.sloppydeploader.SloppyDependency;

@Mod(modid = SDLTest.MODID, version = "0.0")
public class SDLTest {
    
    static {
        SloppyDepLoaderAPI.addDependenciesForMod(Loader.instance().activeModContainer().getModId(),
                new SloppyDependency("https://github.com/makamys/UpdateCheckLib/releases/download/" + SDLTest.UCL_VERSION,
                        "UpdateCheckLib-" + Loader.MC_VERSION + "-" + SDLTest.UCL_VERSION + ".jar",
                        "makamys.updatechecklib.UpdateCheckLib"),
                new SloppyDependency("garbage url",
                        "JarThatWillNeverExist-1.7.10-0.0.jar",
                        "class.that.will.never.Exist")
                );
        
        MCLib.init();
    }
    
    public static final String UCL_VERSION = "1.0";
    
    public static final String MODID = "SloppyDepLoaderTest"; 
    
}
