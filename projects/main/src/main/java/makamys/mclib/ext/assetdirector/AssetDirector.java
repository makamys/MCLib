package makamys.mclib.ext.assetdirector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import cpw.mods.fml.common.ProgressManager;
import cpw.mods.fml.common.ProgressManager.ProgressBar;
import makamys.mclib.ext.assetdirector.mc.MultiVersionDefaultResourcePack;

public class AssetDirector {
    
    static final String NS = "AssetDirector";
    static final File ROOT_DIR = OsPaths.getDefaultInstallationDir().toFile();
    static final File AD_DIR = new File(ROOT_DIR, "asset_director");
    static final Logger LOGGER = LogManager.getLogger("AssetDirector");
    
    public static final String SOUNDS_JSON_REQUESTED = ":tmp:requested";
    
    static AssetDirector instance;
    
    private AssetFetcher fetcher = new AssetFetcher(ROOT_DIR, AD_DIR);
    private Map<String, JsonObject> soundJsons = new HashMap<>();
    
    static {
        instance = new AssetDirector();
    }
    
    @SuppressWarnings("deprecation")
    private void parseJsonStream(InputStream jsonStream, String modid) throws IOException {
        JsonObject json = new Gson().fromJson(new InputStreamReader(jsonStream), JsonObject.class);
        JsonObject assets = json.get("assets").getAsJsonObject();
        
        Map<String, List<String>> objectFetchQueue = new HashMap<>();
        List<String> jarLoadQueue = new ArrayList<>();
        List<String> jarFetchQueue;
        
        ProgressBar downloadBar = null;
        
        for(Entry<String, JsonElement> entry : assets.entrySet()) {
            String version = entry.getKey();
            fetcher.loadVersionDeps(version);
            
            VersionEntryJSON entryObj = new Gson().fromJson(entry.getValue(), VersionEntryJSON.class);
            List<String> objects = entryObj.objects != null ? entryObj.objects : new ArrayList<>();
            
            if(entryObj.soundEvents != null) {
                JsonObject soundJson = getOrFetchSoundJson(version);
                objects.addAll(getObjectsAndSetCategories(entryObj.soundEvents, soundJson, modid));
            }
            
            if(entryObj.jar) {
            	jarLoadQueue.add(version);
            }
            
            objectFetchQueue.put(version, objects.stream().filter(o -> fetcher.needsFetchAsset(version, o, true)).collect(Collectors.toList()));
        }
        
        jarFetchQueue = jarLoadQueue.stream().filter(v -> fetcher.needsFetchJar(v)).collect(Collectors.toList());
        int downloadCount = jarFetchQueue.size() + objectFetchQueue.values().stream().mapToInt(q -> q.size()).sum();
        if(downloadCount > 0) {
        	downloadBar = ProgressManager.push("Downloading", downloadCount);
        }
        
        for(String version : jarFetchQueue) {
            if(downloadBar != null) {
                downloadBar.step("minecraft.jar, version " + version);
            }
            fetcher.fetchJar(version);
        }
        
        for(String version : jarLoadQueue) {
            fetcher.loadJar(version);
        }
    	
        for(Entry<String, List<String>> versionAndAssets : objectFetchQueue.entrySet()) {
            for(String asset : versionAndAssets.getValue()) {
                String[] assetPathSplit = asset.split("/");
                downloadBar.step(assetPathSplit[assetPathSplit.length - 1]);
                
                fetcher.fetchAsset(versionAndAssets.getKey(), asset);
            }
        }

    	if(downloadBar != null) {
            ProgressManager.pop(downloadBar);
    	}
    }
    
    private List<String> getObjectsAndSetCategories(List<JsonObject> soundEvents, JsonObject soundJson, String modid) {
        List<String> objects = new ArrayList<>();
        JsonArray requested = null;
        if(!soundJson.has(SOUNDS_JSON_REQUESTED)) {
            soundJson.add(SOUNDS_JSON_REQUESTED, new JsonArray());
        }
        requested = soundJson.get(SOUNDS_JSON_REQUESTED).getAsJsonArray();
        
        for(JsonObject eventObj : soundEvents) {
            String name, category = null;
            if(eventObj.isJsonPrimitive() && eventObj.getAsJsonPrimitive().isString()) {
                name = eventObj.getAsJsonPrimitive().getAsString();
            } else {
                name = eventObj.get("name").getAsString();
                category = eventObj.get("category").getAsString();
            }
            
            JsonObject event = soundJson.getAsJsonObject(name);
            if(event == null) {
                LOGGER.warn("Mod " + modid + " requested non-existent sound event " + name);
            } else {
                for(JsonElement soundElem : event.getAsJsonArray("sounds")) {
                    String soundPath = null;
                    if(soundElem.isJsonPrimitive() && soundElem.getAsJsonPrimitive().isString()) {
                        soundPath = soundElem.getAsString();
                    } else {
                        JsonObject soundObj = soundElem.getAsJsonObject();
                        soundPath = soundObj.get("name").getAsString();
                    }
                    objects.add("minecraft/sounds/" + soundPath + ".ogg");
                }
                if(event.has("category")) {
                    String originalCategory = event.get("category").getAsString();
                    if(!originalCategory.equals(category)) {
                        LOGGER.warn("Ignoring mod " + modid + "'s category request (" + category + ") for sound event " + name + " that already has one (" + originalCategory + ").");
                    }
                } else {
                    event.addProperty("category", category);
                }
            }
            requested.add(new JsonPrimitive(name));
        }
        return objects;
    }
    
    private JsonObject getOrFetchSoundJson(String version) throws IOException {
        String assetsId = fetcher.versionToAssetsId(version);
        JsonObject soundJson = soundJsons.get(assetsId);
        if(soundJson == null) {
            fetcher.fetchAsset(version, "minecraft/sounds.json");
            soundJson = new Gson().fromJson(new InputStreamReader(fetcher.getAssetInputStream(version, "minecraft/sounds.json")), JsonObject.class);
            soundJsons.put(assetsId, soundJson);
        }
        return soundJson;
    }
    
    public JsonObject getMassagedSoundJson(String version) {
        return soundJsons.get(fetcher.versionToAssetsId(version));    
    }
    
    public AssetFetcher getFetcher() {
        return fetcher;
    }
    
    private static class VersionEntryJSON {
        List<String> objects;
        List<JsonObject> soundEvents;
        boolean jar;
    }

    @SuppressWarnings("deprecation")
    public void preInit() {
        ProgressBar bar = ProgressManager.push("AssetDirector - Loading assets", AssetDirectorAPI.jsonStreams.size());
        boolean connectionOK = true;
        
        for(Entry<String, InputStream> entry : AssetDirectorAPI.jsonStreams.entrySet()) {
            String modid = entry.getKey();
            InputStream jsonStream = entry.getValue();
            
            bar.step(modid);
            if(connectionOK) {
                try {
                    LOGGER.trace("Fetching assets of " + modid);
                    parseJsonStream(jsonStream, modid);
                } catch(Exception e) {
                    LOGGER.error("Failed to fetch assets of " + modid);
                    if(e instanceof UnknownHostException || e instanceof SocketTimeoutException) {
                        LOGGER.error("Aborting further asset downloads since we seem to be offline.");
                        connectionOK = false;
                    }
                    e.printStackTrace();
                }
            }
        }
        ProgressManager.pop(bar);
        
        fetcher.finish();
        MultiVersionDefaultResourcePack.inject(this);
        
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                fetcher.finish();
            }}, "AssetDirector shutdown thread"));
    }
    
}
