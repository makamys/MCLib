package makamys.mclib.ext.assetdirector;

import static makamys.mclib.ext.assetdirector.AssetDirector.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import makamys.mclib.core.MCLib;
import makamys.mclib.core.TaskQueue;
import makamys.mclib.core.sharedstate.SharedReference;

public class AssetDirectorAPI {
    
    static Map<String, InputStream> jsonStreams = SharedReference.get(NS, "jsonStreams", HashMap.class);
    
    /** Enqueues asset downloading for a mod. Can be called anytime before pre-init. */
    public static void register() {
        InputStream jsonStream = AssetDirectorAPI.class.getClassLoader().getResourceAsStream("asset_director.json");
        if(jsonStream == null) {
            MCLib.LOGGER.error("Missing asset_director.json");
        } else {
            jsonStreams.put(Loader.instance().activeModContainer().getModId(), jsonStream);
        }
    }
    
    static {
        TaskQueue.enqueueTask(LoaderState.PREINITIALIZATION, "AssetDirectorPreinit", () -> {
            AssetDirector.instance.preInit();
        });
    }
    
}
