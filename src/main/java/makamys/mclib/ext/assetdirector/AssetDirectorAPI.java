package makamys.mclib.ext.assetdirector;

import static makamys.mclib.ext.assetdirector.AssetDirector.*;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.Side;
import makamys.mclib.core.TaskQueue;
import makamys.mclib.core.sharedstate.SharedReference;

public class AssetDirectorAPI {
    
    static Map<String, String> jsons = SharedReference.get(NS, "jsons", HashMap.class);
    
    private static boolean active = FMLLaunchHandler.side() == Side.CLIENT;
    
    /** Enqueues asset downloading for a mod, using the argument ADConfig object for configuration. Can be called anytime before pre-init.
     */
    public static void register(ADConfig config) {
        if(!active) return;
        
        // We convert configs to JSON strings to remove class identity
        jsons.put(Loader.instance().activeModContainer().getModId(), new Gson().toJson(config));
    }
    
    static {
        if(active) {
            TaskQueue.enqueueTask(LoaderState.PREINITIALIZATION, "AssetDirectorPreinit", () -> {
                AssetDirector.instance.preInit();
            });
        }
    }
    
}
