package makamys.mclib.updatecheck.test;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import makamys.mclib.core.MCLib;
import makamys.mclib.core.MCLibModules;
import makamys.mclib.updatecheck.MockHelper;
import makamys.mclib.updatecheck.UpdateCheckAPI;

@Mod(modid = UCLTest.MODID, version = "0.0")
public class UCLTest {
    
    public static final String MODID = "UpdateCheckLibTest"; 
    
    @EventHandler
    public void onConstruction(FMLConstructionEvent event) {
        MCLib.init();
    }
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        initUpdateCheck();
    }
    
    private void initUpdateCheck() {
        UpdateCheckAPI uc = MCLibModules.updateCheckAPI;
        
        String mock_1_0 = MockHelper.uploadMockText("mock://1.0.json", new UpdateJsonBuilder().version("1.0").build());
        String mock_1_1 = MockHelper.uploadMockText("mock://1.1.json", new UpdateJsonBuilder().version("1.1").build());
        String mock_1_0_homepages = MockHelper.uploadMockText("mock://1.0-homepages.json", new UpdateJsonBuilder().version("1.0").homepage("GitHub", "https://github.com/").homepage("CurseForge", "https://curseforge.com/").homepage("Modrinth", "https://modrinth.com/").build());
        String mock_1_0_troll = MockHelper.uploadMockText("mock://1.0-troll.json", new UpdateJsonBuilder().version("1.0").homepage("TrollHub", "\"></a><script>alert(\"trolled (using homepage url)\");</script><a href=\"").build());
        
        // outdated mod via network (json returns 1.7.10-35.4.1 as the version and https://github.com/makamys/MAtmos/releases as the homepage as of now)
        uc.submitModTask(MODID, "https://raw.githubusercontent.com/makamys/MAtmos/master/updatejson/update-matmos.json");
        
        // outdated mod mock
        uc.submitTask("outdated mod", "0.8", UpdateCheckAPI.MODS_CATEGORY_ID, mock_1_0);
        
        // up to date mod mock
        uc.submitTask("up to date mod", "1.0", UpdateCheckAPI.MODS_CATEGORY_ID, mock_1_0);
        
        // very new mod mock
        uc.submitTask("very new mod", "1.2", UpdateCheckAPI.MODS_CATEGORY_ID, mock_1_0);
        
        // mod with multiple homepages
        uc.submitTask("homepagey mod", "0.8", UpdateCheckAPI.MODS_CATEGORY_ID, mock_1_0_homepages);
        
        // mod with weird version
        uc.submitTask("weird mod", "@VERSION@", UpdateCheckAPI.MODS_CATEGORY_ID, mock_1_0);
        
        // mod with trolley name and homepage url
        uc.submitTask("</td><script>alert(\"trolled (using mod name)\");</script><td>", "0.8", UpdateCheckAPI.MODS_CATEGORY_ID, mock_1_0_troll);
        
        // bad json url, resource pack test
        uc.submitTask("bad res pack", "0.1", UpdateCheckAPI.RESOURCE_PACKS_CATEGORY_ID, "bad json url");
        
        // custom category with no interesting elements
        uc.registerCategory("thingy", "1.1.1", "Thingy", false);
        uc.submitTask("up to date thingy", "1.1", "thingy", mock_1_1);
        
        // backwards compatible category
        uc.registerCategory("matmosSoundpacks", "9999", "MAtmos soundpack", true);
        uc.submitTask("some soundpack", "0.1", "matmosSoundpacks", "https://raw.githubusercontent.com/makamys/MAtmos/master/updatejson/update-mat_zen.json");
    }
    
    private static class UpdateJsonBuilder {
        
        private String version;
        private List<Pair<String, String>> homepages = new ArrayList<>();
        
        public UpdateJsonBuilder() {
            
        }
        
        public UpdateJsonBuilder version(String version) {
            this.version = version;
            return this;
        }
        
        public UpdateJsonBuilder homepage(String display, String url) {
            homepages.add(Pair.of(display, url));
            return this;
        }
        
        public String build() {
            JsonObject rootObj = new JsonObject();
            
            if(!homepages.isEmpty()) {
                JsonObject homepagesObj = new JsonObject();
                for(Pair<String, String> hp : homepages) {
                    homepagesObj.addProperty(hp.getLeft(), hp.getRight());
                }
                rootObj.add("homepages", homepagesObj);
            }
            
            if(version != null) {
                JsonObject promosObj = new JsonObject();
                promosObj.addProperty(Loader.MC_VERSION + "-latest", version);
                rootObj.add("promos", promosObj);
            }
            
            return new Gson().toJson(rootObj);
        }
        
    }
    
}
