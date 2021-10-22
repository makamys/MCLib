package makamys.mclib.ext.assetdirector.mc;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cpw.mods.fml.common.versioning.ComparableVersion;
import cpw.mods.fml.relauncher.ReflectionHelper;
import makamys.mclib.ext.assetdirector.AssetFetcher;
import makamys.mclib.ext.assetdirector.AssetFetcher.AssetIndex;
import makamys.mclib.ext.assetdirector.AssetFetcher.VersionIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

public class MultiVersionDefaultResourcePack implements IResourcePack {
    
    private static final ComparableVersion v1_13 = new ComparableVersion("1.13");
    
    private AssetFetcher fetcher;
    
    private NameParserScratch scratch = new NameParserScratch();
    
    public MultiVersionDefaultResourcePack(AssetFetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public Set getResourceDomains() {
        return fetcher.assetIndexes.keySet().stream().map(v -> "minecraft_" + v).collect(Collectors.toSet());
    }
    
    public InputStream getInputStream(ResourceLocation resLoc) throws IOException {
        parseName(resLoc);
        InputStream is = null;
        if(!scratch.isInJar) {
            is = fetcher.getAssetInputStream(scratch.hash);
        } else {
            is = scratch.vi.getJarFileStream("assets/minecraft/" + scratch.name);
        }
        if(scratch.namespace.equals("minecraft") && scratch.name.equals("sounds.json")) {
            JsonObject obj = new Gson().fromJson(new InputStreamReader(is), JsonObject.class);
            stripUnusedSounds(obj, fetcher.assetIndexes.get(scratch.version));
            return IOUtils.toInputStream(new Gson().toJson(obj));
        }
        return is;
    }

    public boolean resourceExists(ResourceLocation resLoc) {
        parseName(resLoc);
        return scratch.isInJar ? true : fetcher.getObjectIndex().contains(scratch.hash);
    }
    
    private void parseName(ResourceLocation resLoc){
        if(resLoc.equals(scratch.resLoc)) return;
        
        String fullDomain = resLoc.getResourceDomain();
        int firstUnderscore = fullDomain.indexOf('_');
        scratch.resLoc = resLoc;
        scratch.namespace = fullDomain.substring(0, firstUnderscore);
        scratch.version = fullDomain.substring(firstUnderscore + 1);
        scratch.vi = fetcher.versionIndexes.get(scratch.version);
        scratch.name = convertPath(resLoc.getResourcePath(), scratch.vi.version);
        if(scratch.vi.jarContainsFile("assets/minecraft/" + scratch.name)) {
            scratch.isInJar = true;
        } else {
            scratch.isInJar = false;
            scratch.hash = fetcher.assetIndexes.get(scratch.vi.assetsId).nameToHash
                    .get(scratch.namespace + "/" + scratch.name);
        }
    }
    
    private String convertPath(String path, ComparableVersion version) {
        if(version.compareTo(v1_13) >= 0) {
            path = path.replaceFirst("^textures/blocks/", "textures/block/");
            path = path.replaceFirst("^textures/items/", "textures/item/");
        }
        return path;
    }

    @Override
    public IMetadataSection getPackMetadata(IMetadataSerializer p_135058_1_, String p_135058_2_) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPackName() {
        return "AssetDirector";
    }
    
    public static void inject(AssetFetcher fetcher) {
        List defaultResourcePacks = ReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "defaultResourcePacks");
        defaultResourcePacks.add(new MultiVersionDefaultResourcePack(fetcher));
    }
    
    private void stripUnusedSounds(JsonObject soundsJSON, AssetIndex index) {
        soundsJSON.entrySet().removeIf(sound -> {
            JsonArray sounds = sound.getValue().getAsJsonObject().get("sounds").getAsJsonArray();
            for(Iterator<JsonElement> it = sounds.iterator(); it.hasNext();) {
                JsonElement soundElem = it.next();
                String name = (soundElem.isJsonPrimitive() && soundElem.getAsJsonPrimitive().isString()) 
                        ? soundElem.getAsString() : soundElem.getAsJsonObject().get("name").getAsString();
                if(!fetcher.getObjectIndex().contains(index.nameToHash.get("minecraft/sounds/" + name + ".ogg"))){
                    it.remove();
                }
            }
            return sounds.size() == 0;
        });
    }
    
    private static class NameParserScratch {
        ResourceLocation resLoc;
        String namespace;
        String version;
        String name;
        VersionIndex vi;
        boolean isInJar;
        String hash;
    }
}
