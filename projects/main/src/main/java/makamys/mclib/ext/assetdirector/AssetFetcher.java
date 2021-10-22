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
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import static makamys.mclib.ext.assetdirector.AssetDirector.LOGGER;

public class AssetFetcher {
    
    private final static String MANIFEST_ENDPOINT = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private final static String RESOURCES_ENDPOINT = "http://resources.download.minecraft.net";
    
    private final static StringTemplate ASSET_INDEX_PATH = new StringTemplate("assets/indexes/{}.json");
    private final static StringTemplate CLIENT_JAR_PATH = new StringTemplate("versions/{}/{}.jar");
    private final static StringTemplate VERSION_INDEX_PATH = new StringTemplate("versions/{}/{}.json");
    
    private final File INFO_JSON;
    
    private static JsonObject manifest;
    private InfoJSON info;
    public Map<String, VersionIndex> versionIndexes = new HashMap<>();
    public Map<String, AssetIndex> assetIndexes = new HashMap<>();
    
    public File rootDir;
    
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
        loadVersionDeps(version);
        VersionIndex vi = versionIndexes.get(version);
        AssetIndex assetIndex = assetIndexes.get(vi.assetsId);
        for(String asset : resources) {
            String hash = assetIndex.nameToHash.get(asset);
            if(hash != null) {
                if(!info.objectIndex.contains(hash)) {
                    downloadAsset(hash);
                }
            } else {
                LOGGER.error("Couldn't find asset " + asset + " inside " + version + " asset index");
            }
        }
    }
    
    public void fetchForAllVersions(List<String> resources) throws IOException {
        for(String v : assetIndexes.keySet()) {
            fetchResources(v, resources);
        }
    }
    
    private void downloadAsset(String hash) throws IOException {
        String relPath = "/" + hash.substring(0, 2) + "/" + hash;
        copyURLToFile(new URL(RESOURCES_ENDPOINT + relPath), new File(rootDir, "assets/objects/" + relPath));
        info.objectIndex.add(hash);
    }
    
    /** Loads manifest, version index, asset index and client jar for the given version as needed. */
    private void loadVersionDeps(String version) throws IOException {
        if(versionIndexes.containsKey(version)) return;
        
        if(manifest == null) {
            manifest = downloadJson(MANIFEST_ENDPOINT, JsonObject.class);
        }
        
        // TODO redownload stuff if timestamp in manifest changes?
        File indexJson = new File(rootDir, VERSION_INDEX_PATH.get(version));
        if(!indexJson.exists()) {
            downloadVersionIndex(version, indexJson);
        }
        VersionIndex vi = new VersionIndex(loadJson(indexJson, JsonObject.class));
        versionIndexes.put(version, vi);
        
        if(!assetIndexes.containsKey(vi.assetsId)) {
            File assetIndex = new File(rootDir, ASSET_INDEX_PATH.get(vi.assetsId));
            if(!assetIndex.exists()) {
                String url = vi.json.get("assetIndex").getAsJsonObject().get("url").getAsString();
                copyURLToFile(new URL(url), assetIndex);
            }
            assetIndexes.put(vi.assetsId, new AssetIndex(loadJson(assetIndex, JsonObject.class)));
        }
    }
    
    public void loadJar(String version) throws IOException {
        versionIndexes.get(version).loadJar(version);
    }
    
    private void downloadVersionIndex(String version, File dest) throws IOException {
        for(JsonElement verElem : manifest.get("versions").getAsJsonArray()) {
            ManifestVersionJSON ver = new Gson().fromJson(verElem, ManifestVersionJSON.class);
            if(ver.id.equals(version)) {
                copyURLToFile(new URL(ver.url), dest);
                return;
            }
        }
        LOGGER.error("Game version " + version + " could not be found in manifest json.");
    }
    
    private void copyURLToFile(URL source, File destination) throws IOException {
        LOGGER.trace("Downloading " + source + " to " + destination);
        FileUtils.copyURLToFile(source, destination);
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
    
    public Set<String> getObjectIndex(){
        return info.objectIndex;
    }
    
    public InputStream getAssetInputStream(String hash) throws IOException {
        return new BufferedInputStream(new FileInputStream(new File(rootDir, "assets/objects/" + hash.substring(0, 2) + "/" + hash)));
    }
    
    private static class ManifestVersionJSON {
        String id;
        String url;
    }
    
    private static class InfoJSON {
        Set<String> objectIndex = new HashSet<>();
    }
    
    public static class AssetIndex {
        public JsonObject json;
        public Map<String, String> nameToHash = new HashMap<>();
        
        public AssetIndex(JsonObject json) {
            this.json = json;
            json.get("objects").getAsJsonObject().entrySet().forEach(e -> {
                String name = e.getKey();
                String hash = e.getValue().getAsJsonObject().get("hash").getAsString();
                nameToHash.put(name, hash);
            });
        }
    }
    
    public class VersionIndex {
        public JsonObject json;
        public String assetsId;
        private JarFile jar;
        public Set<String> jarContents;
        
        public VersionIndex(JsonObject json) {
            this.json = json;
            assetsId = json.get("assets").getAsString();
        }
        
        public void loadJar(String version) throws IOException {
            if(jar != null) return;
            
            File clientJar = new File(rootDir, CLIENT_JAR_PATH.get(version));
            if(!clientJar.exists()) {
                String url = json.get("downloads").getAsJsonObject().get("client").getAsJsonObject().get("url").getAsString();
                copyURLToFile(new URL(url), clientJar);   
            }
            
            this.jar = new JarFile(clientJar);
            jarContents = jar.stream().map(e -> e.getName()).collect(Collectors.toSet());
        }
        
        public boolean jarContainsFile(String path) {
            return jarContents != null && jarContents.contains(path);
        }
        
        public InputStream getJarFileStream(String path) throws IOException {
            return jar.getInputStream(jar.getEntry(path));
        }
    }

    public void finish() {
        try(FileWriter writer = new FileWriter(INFO_JSON)){
            new Gson().toJson(info, writer);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    private static final class StringTemplate {
        private String template;
        
        public StringTemplate(String template){
            this.template = template;
        }
        
        public String get(String replacement) {
            return template.replaceAll("\\{\\}", replacement);
        }
    }
    
}
