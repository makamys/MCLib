package makamys.mclib.updatechecklibhelper;

import cpw.mods.fml.common.Loader;
import makamys.mclib.sloppydeploader.SloppyDepLoader;
import makamys.mclib.sloppydeploader.SloppyDependency;
import makamys.updatechecklib.UpdateCheckAPI;

/** Helper class for easily downloading UpdateCheckLib if it's not present, and registering an update JSON if it is. <br><br>
 * Usage: create a static field holding an instance of this class in your main mod class, and call preInit() in your pre-init handler. */

public class UpdateCheckLibHelper {
    
    String uclVersion;
    String updateJson;
    
    public UpdateCheckLibHelper(String uclVersion, String updateJson) {
        this.uclVersion = uclVersion;
        this.updateJson = updateJson;
        
        if(!uclVersion.startsWith("@")) {
            SloppyDepLoader.addDependenciesForMod(Loader.instance().activeModContainer().getModId(),
                    new SloppyDependency("https://github.com/makamys/UpdateCheckLib/releases/download/" + uclVersion,
                            "UpdateCheckLib-" + Loader.MC_VERSION + "-" + uclVersion + ".jar",
                            "makamys.updatechecklib.UpdateCheckLib")
                    );
        }
    }
    
    public void preInit() {
        SloppyDepLoader.preInit();
        if(Loader.isModLoaded("UpdateCheckLib")) {
            initUpdateCheckLib();
        }
    }
    
    @cpw.mods.fml.common.Optional.Method(modid = "UpdateCheckLib")
    private void initUpdateCheckLib() {
        UpdateCheckAPI.submitModTask(Loader.instance().activeModContainer().getModId(), updateJson);
    }
    
}
