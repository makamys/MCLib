package makamys.mclib.ext.assetdirector;

import static makamys.mclib.ext.assetdirector.AssetDirector.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.Side;
import makamys.mclib.core.MCLib;
import makamys.mclib.core.TaskQueue;
import makamys.mclib.core.sharedstate.SharedReference;

public class AssetDirectorAPI {
    
    static Map<String, InputStream> jsonStreams = SharedReference.get(NS, "jsonStreams", HashMap.class);
    
    private static boolean active = FMLLaunchHandler.side() == Side.CLIENT;
    
    /** Enqueues asset downloading for a mod, using <code>asset_director.json</code> inside the mod jar for configuration. Can be called anytime before pre-init.
     */
    public static void register() {
        if(!active) return;
        
        InputStream jsonStream = AssetDirectorAPI.class.getClassLoader().getResourceAsStream("asset_director.json");
        if(jsonStream == null) {
            MCLib.LOGGER.error("Missing asset_director.json");
        } else {
            jsonStreams.put(Loader.instance().activeModContainer().getModId(), jsonStream);
        }
    }
    
    /** Enqueues asset downloading for a mod, using the argument JSON object for configuration. Can be called anytime before pre-init.
     * <p>Use this if your configuration is constructed dynamically (at runtime). {@link ADJsonHelper} can be used to create the JSON object.
     * @param assetDirectorJSON
     */
    public static void register(JsonObject assetDirectorJSON) {
        if(!active) return;
        
        jsonStreams.put(Loader.instance().activeModContainer().getModId(), IOUtils.toInputStream(new Gson().toJson(assetDirectorJSON)));
    }
    
    static {
        if(active) {
            TaskQueue.enqueueTask(LoaderState.PREINITIALIZATION, "AssetDirectorPreinit", () -> {
                AssetDirector.instance.preInit();
            });
        }
    }
    
}
