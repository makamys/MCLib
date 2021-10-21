package makamys.mclib.ext.assetdirector;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import makamys.mclib.core.MCLib;

public class AssetFetcher {
    
    private final static String MANIFEST_ENDPOINT = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    
    private static JsonObject manifest;
    
    private File assetsDir;
    
    public AssetFetcher(File assetsDir) {
        this.assetsDir = assetsDir;
    }

    public void fetchResources(String version, List<String> resources) throws IOException {
        File index = new File(assetsDir, "indexes/" + version + ".json");
        if(!index.exists()) {
            downloadIndex(version, index);
        }
    }
    
    private void downloadIndex(String version, File dest) throws IOException {
        if(manifest == null) {
            manifest = downloadManifest();
        }
        for(JsonElement verElem : manifest.get("versions").getAsJsonArray()) {
            ManifestVersionJSON ver = new Gson().fromJson(verElem, ManifestVersionJSON.class);
            if(ver.id.equals(version)) {
                FileUtils.copyURLToFile(new URL(ver.url), dest);
                return;
            }
        }
        MCLib.LOGGER.error("Game version " + version + " could not be found in manifest json.");
    }
    
    private JsonObject downloadManifest() throws IOException {
        return new Gson().fromJson(new InputStreamReader(new BufferedInputStream(new URL(MANIFEST_ENDPOINT).openStream())), JsonObject.class);
    }
    
    private static class ManifestVersionJSON {
        String id;
        String url;
    }
    
}
