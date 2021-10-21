package makamys.mclib.ext.assetdirector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import makamys.mclib.core.MCLib;

public class AssetDirector {
    
    static final String NS = "AssetDirector";
    static final File PATH = new File(OsPaths.getDefaultInstallationDir().toFile(), "asset_director"); 
    
    static AssetDirector instance;
    
    private static AssetFetcher fetcher = new AssetFetcher(PATH);
    
    static {
        instance = new AssetDirector();
    }
    
    private void parseJsonStream(InputStream jsonStream) throws IOException {
        JsonObject json = new Gson().fromJson(new InputStreamReader(jsonStream), JsonObject.class);
        JsonObject assets = json.get("assets").getAsJsonObject();
        for(Entry<String, JsonElement> entry : assets.entrySet()) {
            fetcher.fetchResources(entry.getKey(), StreamSupport.stream(entry.getValue().getAsJsonArray().spliterator(), false).map(e -> e.getAsString()).collect(Collectors.toList()));
        }
    }

    public void preInit() {
        AssetDirectorAPI.jsonStreams.forEach((modid, jsonStream) -> {
            try {
                parseJsonStream(jsonStream);
            } catch(Exception e) {
                MCLib.LOGGER.error("Failed to parse asset_director.json inside " + modid);
                e.printStackTrace();
            }
        });
    }
    
}
