package makamys.mclib.updatecheck.test;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import makamys.mclib.updatecheck.UpdateCheckAPI;

@Mod(modid = UCLTest.MODID, version = "0.0")
public class UCLTest {
	
	public static final String MODID = "UpdateCheckLibTest"; 
	
	@EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    	if(Loader.isModLoaded("UpdateCheckLib")) {
    		initUpdateCheck();
    	}
    }
    
    @cpw.mods.fml.common.Optional.Method(modid = "UpdateCheckLib")
    private void initUpdateCheck() {
    	// outdated mod via network (json returns 1.7.10-35.4.1 as the version and https://github.com/makamys/MAtmos/releases as the homepage as of now)
    	UpdateCheckAPI.submitModTask(MODID, "https://raw.githubusercontent.com/makamys/MAtmos/master/updatejson/update-matmos.json");
    	
    	// outdated mod mock
    	UpdateCheckAPI.submitTask("outdated mod", "0.8", UpdateCheckAPI.MODS_CATEGORY_ID, "mock://1.0");
    	
    	// up to date mod mock
    	UpdateCheckAPI.submitTask("up to date mod", "1.0", UpdateCheckAPI.MODS_CATEGORY_ID, "mock://1.0");
    	
    	// very new mod mock
    	UpdateCheckAPI.submitTask("very new mod", "1.2", UpdateCheckAPI.MODS_CATEGORY_ID, "mock://1.0");
    	
    	// mod with weird version
    	UpdateCheckAPI.submitTask("weird mod", "@VERSION@", UpdateCheckAPI.MODS_CATEGORY_ID, "mock://1.0");
    	
    	// bad json url, resource pack test
    	UpdateCheckAPI.submitTask("bad res pack", "0.1", UpdateCheckAPI.RESOURCE_PACKS_CATEGORY_ID, "bad json url");
    	
    	// custom category with no interesting elements
    	UpdateCheckAPI.registerCategory("thingy", "1.1.1", "Thingy", false);
    	UpdateCheckAPI.submitTask("up to date thingy", "1.1", "thingy", "mock://1.1");
    	
    	// backwards compatible category
    	UpdateCheckAPI.registerCategory("matmosSoundpacks", "9999", "MAtmos soundpack", true);
    	UpdateCheckAPI.submitTask("some soundpack", "0.1", "matmosSoundpacks", "https://raw.githubusercontent.com/makamys/MAtmos/master/updatejson/update-mat_zen.json");
    }
    
}
