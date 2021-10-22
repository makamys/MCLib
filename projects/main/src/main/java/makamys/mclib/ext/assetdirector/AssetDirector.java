package makamys.mclib.ext.assetdirector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import makamys.mclib.ext.assetdirector.mc.MultiVersionDefaultResourcePack;

public class AssetDirector {
    
    static final String NS = "AssetDirector";
    static final File PATH = new File(OsPaths.getDefaultInstallationDir().toFile(), "asset_director");
    static final Logger LOGGER = LogManager.getLogger("AssetDirector");
    
    static AssetDirector instance;
    
    private static AssetFetcher fetcher = new AssetFetcher(PATH);
    
    static {
        instance = new AssetDirector();
    }
    
    private void parseJsonStream(InputStream jsonStream) throws IOException {
        JsonObject json = new Gson().fromJson(new InputStreamReader(jsonStream), JsonObject.class);
        JsonObject assets = json.get("assets").getAsJsonObject();
        for(Entry<String, JsonElement> entry : assets.entrySet()) {
            String version = entry.getKey();
            VersionEntryJSON entryObj = new Gson().fromJson(entry.getValue(), VersionEntryJSON.class);
            if(entryObj.objects != null) {
                fetcher.fetchResources(version, entryObj.objects);
            }
            if(entryObj.jar) {
                fetcher.loadJar(version);
            }
        }
    }
    
    private static class VersionEntryJSON {
        List<String> objects;
        boolean jar;
    }

    public void preInit() {
        AssetDirectorAPI.jsonStreams.forEach((modid, jsonStream) -> {
            try {
                LOGGER.trace("Fetching asets of " + modid);
                parseJsonStream(jsonStream);
            } catch(Exception e) {
                LOGGER.error("Failed to parse asset_director.json inside " + modid);
                e.printStackTrace();
            }
        });
        
        try {
            fetcher.fetchForAllVersions(Arrays.asList("minecraft/sounds.json"));
        } catch(IOException e) {
            e.printStackTrace();
        }
        
        fetcher.finish();
        MultiVersionDefaultResourcePack.inject(fetcher);
    }
    
}
