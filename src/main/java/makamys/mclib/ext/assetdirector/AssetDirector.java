package makamys.mclib.ext.assetdirector;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import makamys.mclib.ext.assetdirector.ADConfig.VersionAssets;
import makamys.mclib.ext.assetdirector.ADConfig.VersionAssets.SoundEvent;
import makamys.mclib.ext.assetdirector.mc.MCUtil;
import makamys.mclib.ext.assetdirector.mc.MCUtil.ProgressBar;
import makamys.mclib.ext.assetdirector.mc.MCUtil.Version;
import makamys.mclib.ext.assetdirector.mc.MultiVersionDefaultResourcePack;

/** Responsible for the high level logic of fetching assets. */
public class AssetDirector {
    
    static final Logger LOGGER = LogManager.getLogger("AssetDirector");
    static final String NS = "AssetDirector";
    static final File AD_DIR = getAssetDirectorDir();
    
    public static final String SOUNDS_JSON_REQUESTED = ":tmp:requested";
    
    static AssetDirector instance;
    
    private AssetFetcher fetcher = new AssetFetcher(MCUtil.getMCAssetsDir(), AD_DIR);
    private Map<String, JsonObject> soundJsons = new HashMap<>();
    private boolean initialized;
    private boolean connectionOK = true;
    private boolean resourcePackInjected;

    static {
        instance = new AssetDirector();
    }

    private static class AssetLoadPlan {

        Set<String> objectFetchQueue = new HashSet<>();
        Map<String, String> objectNames = new HashMap<>();

        Set<String> jarFetchQueue = new HashSet<>();
        Set<String> jarLoadQueue = new HashSet<>();
    }

    private AssetLoadPlan resolveAssets(String json, String modid) throws Exception {
        ADConfig config = new Gson().fromJson(json, ADConfig.class);
        AssetLoadPlan plan = new AssetLoadPlan();

        ProgressBar bar = ProgressBar.push("Version", config.assets.size());
        try {
            for (Entry<String, VersionAssets> entry : config.assets.entrySet()) {
                String version = entry.getKey();
                bar.step("Minecraft " + version);
                fetcher.loadVersionDeps(version);

                VersionAssets entryObj = entry.getValue();
                Set<String> objects = entryObj.objects != null ? entryObj.objects : new HashSet<>();

                if (entryObj.soundEvents != null) {
                    JsonObject soundJson = getOrFetchSoundJson(version);
                    objects.addAll(getObjectsAndSetCategories(entryObj.soundEvents, soundJson, modid));
                }

                if (entryObj.jar) {
                    plan.jarLoadQueue.add(version);
                }

                for (String name : objects) {
                    String hash = fetcher.getAssetHash(version, name, true);
                    if (hash != null) {
                        plan.objectFetchQueue.add(hash);
                        plan.objectNames.put(hash, name);
                    }
                }
            }
        } finally {
            bar.pop();
        }

        plan.objectFetchQueue = plan.objectFetchQueue.stream()
                .filter(fetcher::needsFetchAssetByHash)
                .collect(Collectors.toSet());

        plan.jarFetchQueue = plan.jarLoadQueue.stream()
                .filter(fetcher::needsFetchJar)
                .collect(Collectors.toSet());

        return plan;
    }

    private void downloadAssets(AssetLoadPlan plan) throws Exception {
        int count = plan.jarFetchQueue.size() + plan.objectFetchQueue.size();
        if(count == 0) return;

        ProgressBar bar = ProgressBar.push("Asset", count);
        try {
            for(String version : plan.jarFetchQueue) {
                bar.step("Minecraft " + version + " jar");
                fetcher.fetchJar(version);
            }

            for(String hash : plan.objectFetchQueue) {
                String name = plan.objectNames.get(hash);
                if(name.startsWith("minecraft/")) name = name.substring("minecraft/".length());
                if(name.startsWith("sounds/")) name = name.substring("sounds/".length());

                bar.step(name);
                fetcher.fetchAssetByHash(hash);
            }
        } finally {
            bar.pop();
        }
    }

    private void loadJars(AssetLoadPlan plan) throws IOException {
        if(plan.jarLoadQueue.isEmpty()) {
            return;
        }

        ProgressBar bar = ProgressBar.push("Jar", plan.jarLoadQueue.size());
        try {
            for (String version : plan.jarLoadQueue) {
                bar.step("Minecraft " + version);
                fetcher.loadJar(version);
            }
        } finally {
            bar.pop();
        }
    }
    
    private List<String> getObjectsAndSetCategories(Collection<SoundEvent> soundEvents, JsonObject soundJson, String modid) {
        List<String> objects = new ArrayList<>();
        JsonArray requested = null;
        if(!soundJson.has(SOUNDS_JSON_REQUESTED)) {
            soundJson.add(SOUNDS_JSON_REQUESTED, new JsonArray());
        }
        requested = soundJson.get(SOUNDS_JSON_REQUESTED).getAsJsonArray();
        
        for(SoundEvent eventObj : soundEvents) {
            String name = eventObj.name;
            String category = eventObj.category;
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
    
    private JsonObject getOrFetchSoundJson(String version) throws Exception {
        String assetsId = fetcher.versionToAssetsId(version);
        JsonObject soundJson = soundJsons.get(assetsId);
        if(soundJson == null) {
            if(fetcher.needsFetchAsset(version, "minecraft/sounds.json", true)) {
                fetcher.fetchAsset(version, "minecraft/sounds.json");
            }
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

    public void preInit(String modid) {
        String json = AssetDirectorAPI.jsons.remove(modid);
        if(json == null) return;

        long t0 = System.nanoTime();
        
        if(!initialized) {
            fetcher.init();
            initialized = true;
        }

        if (connectionOK) {
            ProgressBar bar = ProgressBar.push("AssetDirector", 3);
            try {
                LOGGER.trace("Fetching assets of {}", modid);

                bar.step("Resolving assets");
                AssetLoadPlan plan = resolveAssets(json, modid);

                bar.step("Downloading assets");
                downloadAssets(plan);

                bar.step("Loading jars");
                loadJars(plan);
            } catch (Exception e) {
                LOGGER.error("Failed to fetch assets of {}", modid, e);
                if (e instanceof UnknownHostException || e instanceof SocketTimeoutException) {
                    LOGGER.error("Aborting further asset downloads since we seem to be offline.");
                    connectionOK = false;
                }
            }
            bar.pop();
        }

        if(AssetDirectorAPI.jsons.isEmpty() && !resourcePackInjected) {
            MultiVersionDefaultResourcePack.inject(this);
            resourcePackInjected = true;
        }
        
        long t1 = System.nanoTime();
        LOGGER.debug("AssetDirector pre-init for {} took {}s.", modid, (t1 - t0) / 1_000_000_000.0);
    }
    
    private static File getAssetDirectorDir() {
        String sharedDataDir = System.getProperty("minecraft.sharedDataDir");
        if(sharedDataDir == null) {
            sharedDataDir = System.getenv("MINECRAFT_SHARED_DATA_DIR");
        }
        if(sharedDataDir != null) {
            return new File(sharedDataDir, "asset_director");
        }
        
        File assetsDir = MCUtil.getMCAssetsDir();
        // The old launcher deletes extra files from the assets directory, so we can't live there. Use `<launcher work dir>/asset_director` instead. 
        return !isOldLauncher(assetsDir) ? new File(assetsDir, "asset_director") : new File(assetsDir, "../asset_director");
    }
    
    private static boolean isOldLauncher(File assetsDir) {
        File launcherJson = new File(assetsDir, "../launcher_profiles.json");
        if(launcherJson.exists()) {
            try(FileReader fr = new FileReader(launcherJson)){
                JsonObject object = new Gson().fromJson(fr, JsonObject.class);
                JsonObject launcherVersionObj = object.getAsJsonObject("launcherVersion");
                if(launcherVersionObj != null) {
                    JsonPrimitive name = launcherVersionObj.getAsJsonPrimitive("name");
                    if(name.isString()) {
                        String launcherVersion = name.getAsString();
                        
                        LOGGER.debug("Detected official launcher (version " + launcherVersion + "). Will use alternative directory location.");
                        
                        return new Version(launcherVersion).compareTo(new Version("1.6.93")) <= 0;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        LOGGER.debug("Couldn't read launcher_profiles.json. Assuming official launcher is not used.");
        return false;
    }
    
}
