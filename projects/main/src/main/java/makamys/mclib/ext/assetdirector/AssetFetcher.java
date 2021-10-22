package makamys.mclib.ext.assetdirector;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import makamys.mclib.core.MCLib;

public class AssetFetcher {
    
    private final static String MANIFEST_ENDPOINT = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private final static String RESOURCES_ENDPOINT = "http://resources.download.minecraft.net";
    private final File INFO_JSON;
    
    private static JsonObject manifest;
    private InfoJSON info;
    private Map<String, AssetIndex> assetIndexes = new HashMap<>();
    
    private File rootDir;
    
    public AssetFetcher(File rootDir) {
        this.rootDir = rootDir;
        INFO_JSON = new File(rootDir, "info.json");
        if(INFO_JSON.exists()) {
            try {
                info = loadJson(INFO_JSON, InfoJSON.class);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        if(info == null) {
            info = new InfoJSON();
        }
    }

    public void fetchResources(String version, List<String> resources) throws IOException {
        AssetIndex assetIndex = getAssetIndex(version);
        for(String asset : resources) {
            String hash = assetIndex.nameToHash.get(asset);
            if(hash != null) {
                if(!info.objectIndex.contains(hash)) {
                    downloadAsset(hash);
                }
            } else {
                MCLib.LOGGER.error("Couldn't find asset " + asset + " inside " + version + " asset index");
            }
        }
    }
    
    private void downloadAsset(String hash) throws IOException {
        String relPath = "/" + hash.substring(0, 2) + "/" + hash;
        FileUtils.copyURLToFile(new URL(RESOURCES_ENDPOINT + relPath), new File(rootDir, "assets/objects/" + relPath));
        info.objectIndex.add(hash);
    }

    private AssetIndex getAssetIndex(String version) throws IOException {
        AssetIndex assetIndex = assetIndexes.get(version);
        if(assetIndex == null) {
            File index = new File(rootDir, "assets/indexes/" + version + ".json");
            if(!index.exists()) {
                downloadAssetIndex(version, index);
            }
            assetIndexes.put(version, assetIndex = new AssetIndex(loadJson(index, JsonObject.class)));
        }
        return assetIndex;
    }
    
    private void downloadAssetIndex(String version, File dest) throws IOException {
        if(manifest == null) {
            manifest = downloadJson(MANIFEST_ENDPOINT, JsonObject.class);
        }
        for(JsonElement verElem : manifest.get("versions").getAsJsonArray()) {
            ManifestVersionJSON ver = new Gson().fromJson(verElem, ManifestVersionJSON.class);
            if(ver.id.equals(version)) {
                JsonObject indexJson = downloadJson(ver.url, JsonObject.class);
                String url = indexJson.get("assetIndex").getAsJsonObject().get("url").getAsString();
                FileUtils.copyURLToFile(new URL(url), dest);
                return;
            }
        }
        MCLib.LOGGER.error("Game version " + version + " could not be found in manifest json.");
    }
    
    private <T> T downloadJson(String url, Class<T> classOfT) throws IOException {
        return loadJson(new URL(url).openStream(), classOfT);
    }
    
    private <T> T loadJson(File file, Class<T> classOfT) throws IOException {
        return loadJson(new FileInputStream(file), classOfT);
    }
    
    private <T> T loadJson(InputStream stream, Class<T> classOfT) throws IOException {
        return new Gson().fromJson(new InputStreamReader(new BufferedInputStream(stream)), classOfT);
    }
    
    private static class ManifestVersionJSON {
        String id;
        String url;
    }
    
    private static class InfoJSON {
        Set<String> objectIndex = new HashSet<>();
    }
    
    private static class AssetIndex {
        JsonObject json;
        Map<String, String> nameToHash = new HashMap<>();
        
        public AssetIndex(JsonObject json) {
            this.json = json;
            json.get("objects").getAsJsonObject().entrySet().forEach(e -> {
                String name = e.getKey();
                String hash = e.getValue().getAsJsonObject().get("hash").getAsString();
                nameToHash.put(name, hash);
            });
        }
    }

    public void finish() {
        try(FileWriter writer = new FileWriter(INFO_JSON)){
            new Gson().toJson(info, writer);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
}
