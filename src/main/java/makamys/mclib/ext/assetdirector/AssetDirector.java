package makamys.mclib.ext.assetdirector;

import java.io.File;
import java.io.FileReader;
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
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private static final int DEFAULT_DOWNLOAD_THREADS = Math.max(4, Math.min(16, Runtime.getRuntime().availableProcessors() * 2));
    private static final int DOWNLOAD_THREADS = getDownloadThreads();
    static final File AD_DIR = getAssetDirectorDir();
    
    public static final String SOUNDS_JSON_REQUESTED = ":tmp:requested";
    
    static AssetDirector instance;
    
    private AssetFetcher fetcher = new AssetFetcher(MCUtil.getMCAssetsDir(), AD_DIR);
    private Map<String, JsonObject> soundJsons = new HashMap<>();
    
    static {
        instance = new AssetDirector();
    }
    
    private void parseJson(String json, String modid) throws Exception {
        ADConfig config = new Gson().fromJson(json, ADConfig.class);
        
        Set<String> objectFetchQueue = new HashSet<>();
        Map<String, String> objectName = new HashMap<>(); // For informational purposes
        Set<String> jarLoadQueue = new HashSet<>();
        Set<String> jarFetchQueue;
        
        for(Entry<String, VersionAssets> entry : config.assets.entrySet()) {
            String version = entry.getKey();
            fetcher.loadVersionDeps(version);
            
            VersionAssets entryObj = entry.getValue();
            Set<String> objects = entryObj.objects != null ? entryObj.objects : new HashSet<>();
            
            if(entryObj.soundEvents != null) {
                JsonObject soundJson = getOrFetchSoundJson(version);
                objects.addAll(getObjectsAndSetCategories(entryObj.soundEvents, soundJson, modid));
            }
            
            if(entryObj.jar) {
            	jarLoadQueue.add(version);
            }
            
            for(String name : objects) {
                String hash = fetcher.getAssetHash(version, name, true);
                if(hash != null) {
                    objectFetchQueue.add(hash);
                    objectName.put(hash, name);
                }
            }
        }
        
        objectFetchQueue = objectFetchQueue.stream().filter(fetcher::needsFetchAssetByHash).collect(Collectors.toSet());
        jarFetchQueue = jarLoadQueue.stream().filter(fetcher::needsFetchJar).collect(Collectors.toSet());
        downloadResources(jarFetchQueue, objectFetchQueue, objectName);
        
        for(String version : jarLoadQueue) {
            fetcher.loadJar(version);
        }
    }

    private void downloadResources(Set<String> jarFetchQueue, Set<String> objectFetchQueue, Map<String, String> objectName) throws Exception {
        int downloadCount = jarFetchQueue.size() + objectFetchQueue.size();
        if(downloadCount == 0) return;

        int threadCount = Math.min(DOWNLOAD_THREADS, downloadCount);
        LOGGER.info("Downloading " + downloadCount + " resources using " + threadCount + " threads...");
        ProgressBar downloadBar = ProgressBar.push("Downloading", downloadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CompletionService<DownloadResult> downloads = new ExecutorCompletionService<>(executor);

        for(String version : jarFetchQueue) {
            String name = "minecraft.jar, version " + version;
            downloads.submit(() -> {
                try {
                    fetcher.fetchJar(version);
                    return new DownloadResult(name, null);
                } catch(Exception e) {
                    return new DownloadResult(name, e);
                }
            });
        }

        for(String assetHash : objectFetchQueue) {
            String name = objectName.get(assetHash).replaceFirst("minecraft/", "").replaceFirst("sounds/", "");
            downloads.submit(() -> {
                try {
                    fetcher.fetchAssetByHash(assetHash);
                    return new DownloadResult(name, null);
                } catch(Exception e) {
                    return new DownloadResult(name, e);
                }
            });
        }

        Exception firstError = null;
        Exception firstConnectionError = null;
        try {
            for(int i = 0; i < downloadCount; i++) {
                DownloadResult result = downloads.take().get();
                downloadBar.step(result.name);
                if(result.error != null) {
                    LOGGER.error("Failed to download " + result.name + ": " + result.error);
                    if(firstError == null) {
                        firstError = result.error;
                    }
                    if(firstConnectionError == null && isConnectionFailure(result.error)) {
                        firstConnectionError = result.error;
                    }
                }
            }
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            executor.shutdownNow();
            downloadBar.pop();
        }

        if(firstConnectionError != null) {
            throw firstConnectionError;
        }
        if(firstError != null) {
            throw firstError;
        }
    }

    private static class DownloadResult {
        final String name;
        final Exception error;

        DownloadResult(String name, Exception error) {
            this.name = name;
            this.error = error;
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

    public void preInit() {
        long t0 = System.nanoTime();
        
        fetcher.init();
        
        ProgressBar bar = MCUtil.ProgressBar.push("AssetDirector - Loading assets", AssetDirectorAPI.jsons.size());
        boolean connectionOK = true;
        
        for(Entry<String, String> entry : AssetDirectorAPI.jsons.entrySet()) {
            String modid = entry.getKey();
            String json = entry.getValue();
            
            bar.step(modid);
            if(connectionOK) {
                try {
                    LOGGER.trace("Fetching assets of " + modid);
                    parseJson(json, modid);
                } catch(Exception e) {
                    LOGGER.error("Failed to fetch assets of " + modid);
                    if(e instanceof InterruptedException || Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                        LOGGER.warn("Asset downloads were interrupted; aborting further downloads.");
                        break;
                    }
                    if(isConnectionFailure(e)) {
                        LOGGER.error("Aborting further asset downloads since we seem to be offline.");
                        connectionOK = false;
                    }
                    e.printStackTrace();
                }
            }
        }
        bar.pop();
        
        MultiVersionDefaultResourcePack.inject(this);
        
        long t1 = System.nanoTime();
        LOGGER.debug("AssetDirector pre-init took " + (t1 - t0) / 1_000_000_000.0 + "s.");
    }
    
    private static int getDownloadThreads() {
        String configuredValue = System.getProperty("assetDirector.downloadThreads");
        if(configuredValue == null) return DEFAULT_DOWNLOAD_THREADS;

        try {
            return Math.max(1, Math.min(64, Integer.parseInt(configuredValue)));
        } catch(NumberFormatException e) {
            LOGGER.warn("Invalid assetDirector.downloadThreads value '" + configuredValue + "'; using " + DEFAULT_DOWNLOAD_THREADS + ".");
            return DEFAULT_DOWNLOAD_THREADS;
        }
    }

    private static boolean isConnectionFailure(Throwable error) {
        while(error != null) {
            if(error instanceof UnknownHostException || error instanceof SocketTimeoutException) {
                return true;
            }
            error = error.getCause();
        }
        return false;
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
