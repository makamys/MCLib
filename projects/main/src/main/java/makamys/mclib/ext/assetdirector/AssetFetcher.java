package makamys.mclib.ext.assetdirector;

import static makamys.mclib.ext.assetdirector.AssetDirector.LOGGER;

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
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cpw.mods.fml.common.versioning.ComparableVersion;

/** Responsible for the implementation details of fetching assets, most notably interfacing with Mojang's API. */
public class AssetFetcher {
    
    final static String MANIFEST_ENDPOINT = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    final static String RESOURCES_ENDPOINT = "http://resources.download.minecraft.net";
    
    final static StringTemplate ASSET_INDEX_PATH = new StringTemplate("assets/indexes/{}.json");
    final static StringTemplate CLIENT_JAR_PATH = new StringTemplate("versions/{}/{}.jar");
    final static StringTemplate VERSION_INDEX_PATH = new StringTemplate("versions/{}/{}.json");
    
    private static final int    DOWNLOAD_TIMEOUT = 10_000, // ms
                                DOWNLOAD_ATTEMPTS = 3;
    
    private final File INFO_JSON;
    
    private static JsonObject manifest;
    InfoJSON info;
    public Map<String, VersionIndex> versionIndexes = new HashMap<>();
    public Map<String, AssetIndex> assetIndexes = new HashMap<>();
    
    public File rootDir, adDir;
    
    public AssetFetcher(File rootDir, File adDir) {
        this.rootDir = rootDir;
        this.adDir = adDir;
        if(!adDir.exists()) {
        	adDir.mkdirs();
        }
        INFO_JSON = new File(adDir, "info.json");
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

    public void fetchAsset(String version, String asset) throws IOException {
        loadVersionDeps(version);
        VersionIndex vi = versionIndexes.get(version);
        AssetIndex assetIndex = assetIndexes.get(vi.assetsId);
        String hash = assetIndex.nameToHash.get(asset);
        if(hash != null) {
            downloadAsset(hash);
        }
    }
    
    public boolean needsFetchResource(String version, String asset) {
        return needsFetchAsset(version, asset, false);
    }
    
    public boolean needsFetchAsset(String version, String asset, boolean printErrors) {
        VersionIndex vi = versionIndexes.get(version);
        AssetIndex assetIndex = assetIndexes.get(vi.assetsId);
        String hash = assetIndex.nameToHash.get(asset);
        if(hash != null) {
            return !info.fileIsPresent(hash);
        } else if(printErrors) {
            LOGGER.error("Couldn't find asset " + asset + " inside " + version + " asset index");
        }
        return false;
    }
    
    public void fetchForAllVersions(String asset) throws IOException {
        for(String v : assetIndexes.keySet()) {
            fetchAsset(v, asset);
        }
    }
    
    private void downloadAsset(String hash) throws IOException {
        String relPath = "/" + hash.substring(0, 2) + "/" + hash;
        File outFile = new File(rootDir, "assets/objects/" + relPath);
        
        for(int i = 0; i < DOWNLOAD_ATTEMPTS; i++) {
            copyURLToFile(new URL(RESOURCES_ENDPOINT + relPath), outFile);
            if(hash.equals(getSha1(outFile))) {
                // OK
                info.objectIndex.add(hash);
                break;
            } else {
                LOGGER.warn("Got invalid hash when downloading " + hash + ". Attempt " + (i + 1) + "/" + DOWNLOAD_ATTEMPTS);
            }
        }
        
        info.dirty = true;
    }
    
    /** Loads manifest, version index, asset index and client jar for the given version as needed. */
    public void loadVersionDeps(String version) throws IOException {
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
    
    public void fetchJar(String version) throws IOException {
        versionIndexes.get(version).fetchJar(version);
    }
    
    public void loadJar(String version) throws IOException {
        versionIndexes.get(version).loadJar(version);
    }
    
    public boolean needsFetchJar(String version) {
        return !new File(rootDir, AssetFetcher.CLIENT_JAR_PATH.get(version)).exists();
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
        FileUtils.copyURLToFile(source, destination, DOWNLOAD_TIMEOUT, DOWNLOAD_TIMEOUT);
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
    
    public File getAssetFile(String hash) {
        return new File(rootDir, "assets/objects/" + hash.substring(0, 2) + "/" + hash);
    }
    
    public InputStream getAssetInputStream(String hash) throws IOException {
        return new BufferedInputStream(new FileInputStream(getAssetFile(hash)));
    }
    
    public InputStream getAssetInputStream(String version, String path) throws IOException {
        return getAssetInputStream(assetIndexes.get(versionToAssetsId(version)).nameToHash.get(path));
    }
    
    public boolean hashExists(String hash) {
        if(!getObjectIndex().contains(hash)) {
            return false;
        } else {
            if(!getAssetFile(hash).exists()) {
                // correct the index
                getObjectIndex().remove(hash);
                info.dirty = true;
                return false;
            } else {
                return true;
            }
        }
    }
    
    static String getSha1(File file) {
        try {
            return Files.hash(file, Hashing.sha1()).toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public String versionToAssetsId(String version) {
        try {
            loadVersionDeps(version);
        } catch(IOException e) {
            e.printStackTrace();
        }
        
        return versionIndexes.get(version).assetsId;
    }
    
    private static class ManifestVersionJSON {
        String id;
        String url;
    }
    
    class InfoJSON {
        // Objects known to have been present and valid at one point. They are assumed to still be, for performance.
        // False positives are removed when attempted to be accessed.
        Set<String> objectIndex = new HashSet<>();
        
        private transient Set<String> checkedObjects = new HashSet<>();
        
        private transient boolean dirty;

        public boolean fileIsPresent(String hash) {
            if(!objectIndex.contains(hash) && !checkedObjects.contains(hash)) {
                // verify missing entries the first time, so we don't accidentally redownload them
                File assetFile = getAssetFile(hash);
                if(assetFile.exists() && hash.equals(getSha1(assetFile))) {
                    objectIndex.add(hash);
                    dirty = true;
                }
                checkedObjects.add(hash);
            }
            
            return objectIndex.contains(hash);
        }
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
        JarFile jar;
        public Set<String> jarContents;
        public ComparableVersion version;
        
        public VersionIndex(JsonObject json) {
            this.json = json;
            assetsId = json.get("assets").getAsString();
            this.version = new ComparableVersion(json.get("id").getAsString());
        }
        
        public void fetchJar(String version) throws IOException {
            if(jar != null) return;
            
            File clientJar = new File(rootDir, CLIENT_JAR_PATH.get(version));
            
            String url = json.get("downloads").getAsJsonObject().get("client").getAsJsonObject().get("url").getAsString();
            copyURLToFile(new URL(url), clientJar);   
        }
        
        public void loadJar(String version) throws IOException {
            if(jar != null) return;
            
            File clientJar = new File(rootDir, CLIENT_JAR_PATH.get(version));
            
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
        if(info.dirty) {
            try(FileWriter writer = new FileWriter(INFO_JSON)){
                new Gson().toJson(info, writer);
            } catch(IOException e) {
                e.printStackTrace();
            }
            info.dirty = false;
        }
    }
    
    static final class StringTemplate {
        private String template;
        
        public StringTemplate(String template){
            this.template = template;
        }
        
        public String get(String replacement) {
            return template.replaceAll("\\{\\}", replacement);
        }
    }
    
}
