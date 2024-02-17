package makamys.mclib.ext.assetdirector.mc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cpw.mods.fml.relauncher.ReflectionHelper;
import makamys.mclib.ext.assetdirector.AssetDirector;
import makamys.mclib.ext.assetdirector.AssetFetcher;
import makamys.mclib.ext.assetdirector.ResourcePackUtil;
import makamys.mclib.ext.assetdirector.AssetFetcher.AssetIndex;
import makamys.mclib.ext.assetdirector.AssetFetcher.VersionIndex;
import makamys.mclib.ext.assetdirector.mc.MCUtil.Version;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

import static makamys.mclib.ext.assetdirector.AssetDirector.SOUNDS_JSON_REQUESTED;

public class MultiVersionDefaultResourcePack implements IResourcePack {
    
    private static final boolean DUMP_SOUNDS_JSON = Boolean.parseBoolean(System.getProperty("assetDirector.dumpSoundsJson", "false"));
    
    private static final Version v1_7_10 = new Version("1.7.10");
    private static final Version v1_13 = new Version("1.13");
    
    private AssetDirector assetDirector;
    private AssetFetcher fetcher;
    
    private NameParserScratch scratch = new NameParserScratch();
    
    public MultiVersionDefaultResourcePack(AssetDirector assetDirector) {
        this.assetDirector = assetDirector;
        this.fetcher = assetDirector.getFetcher();
    }

    @Override
    public Set getResourceDomains() {
        return fetcher.versionIndexes.keySet().stream().map(v -> "minecraft_" + v).collect(Collectors.toSet());
    }
    
    public InputStream getInputStream(ResourceLocation resLoc) throws IOException {
        parseName(resLoc);
        if(!scratch.isValid) throw new IOException("Invalid resource location: " + resLoc);
        
        if(scratch.mcResPack != null) {
            // Get overridden by a user-added resource of the same name in the minecraft namespace
            return scratch.mcResPack.getInputStream(scratch.mcResLoc);
        }
        
        InputStream is = null;
        if(scratch.isInJar) {
            is = scratch.vi.getJarFileStream("assets/minecraft/" + scratch.name);
        } else if(fetcher.hashExists(scratch.hash)) {
            is = fetcher.getAssetInputStream(scratch.hash);
        } else {
            throw new IOException("Resource does not exist: " + resLoc);
        }
        if(scratch.namespace.equals("minecraft") && scratch.name.equals("sounds.json")) {
            JsonObject obj = assetDirector.getMassagedSoundJson(scratch.version);
            if(obj != null) {
                stripUnusedSounds(obj, fetcher.assetIndexes.get(scratch.version));
                if(DUMP_SOUNDS_JSON) {
                    dumpSoundsJson(obj, scratch.version);
                }
                return IOUtils.toInputStream(new Gson().toJson(obj));
            }
        }
        return is;
    }

    public boolean resourceExists(ResourceLocation resLoc) {
        parseName(resLoc);
        
        return scratch.isValid && (scratch.isInJar ? true : fetcher.hashExists(scratch.hash) || scratch.mcResPack != null);
    }
    
    private void parseName(ResourceLocation resLoc){
        if(resLoc.equals(scratch.resLoc)) return;
        
        scratch.isValid = resLoc.getResourceDomain().startsWith("minecraft_");
        if(!scratch.isValid) {
            return;
        }
        
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
        
        scratch.mcResPack = null;
        if(!resLoc.getResourcePath().equals("sounds.json")) {
            ResourceLocation mcResLocOld = new ResourceLocation("minecraft", convertPath(resLoc.getResourcePath(), v1_7_10));
            ResourceLocation mcResLocNew = new ResourceLocation("minecraft", convertPath(resLoc.getResourcePath(), v1_13));
            List<IResourcePack> mcResPacks = ResourcePackUtil.getMinecraftResourcePackList();
            for(int i = mcResPacks.size() - 1; i >= 0; i--) {
                IResourcePack resPack = mcResPacks.get(i);
                if(!ResourcePackUtil.isBuiltIn(resPack)) {
                    if(resPack.resourceExists(mcResLocOld)) {
                        scratch.mcResPack = resPack;
                        scratch.mcResLoc = mcResLocOld;
                        break;
                    } else if(!mcResLocOld.equals(mcResLocNew) && resPack.resourceExists(mcResLocNew)) {
                        scratch.mcResPack = resPack;
                        scratch.mcResLoc = mcResLocNew;
                        break;
                    }
                }
            }
        }
    }
    
    private String convertPath(String path, Version version) {
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
    
    public static void inject(AssetDirector assetDirector) {
        IResourcePack multiDefaultPack = new MultiVersionDefaultResourcePack(assetDirector);
        List defaultResourcePacks = ReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "defaultResourcePacks", "field_110449_ao");
        defaultResourcePacks.add(multiDefaultPack);
        IResourceManager resMan = Minecraft.getMinecraft().getResourceManager();
        if(resMan instanceof SimpleReloadableResourceManager) {
            ((SimpleReloadableResourceManager)resMan).reloadResourcePack(multiDefaultPack);
        }
    }
    
    private void stripUnusedSounds(JsonObject soundsJSON, AssetIndex index) {
        if(soundsJSON.has(SOUNDS_JSON_REQUESTED)) {
            Set<String> requested = 
                    StreamSupport.stream(soundsJSON.getAsJsonArray(SOUNDS_JSON_REQUESTED).spliterator(), false)
                    .map(e -> e.getAsString()).collect(Collectors.toSet());
            soundsJSON.remove(SOUNDS_JSON_REQUESTED);
            soundsJSON.entrySet().removeIf(sound -> !requested.contains(sound.getKey()));
        }
    }
    
    private static void dumpSoundsJson(JsonObject json, String version) {
        File outFile = new File(MCUtil.getInstanceDir(), "asset_director/out/sounds-" + version + ".json");
        outFile.getParentFile().mkdirs();
        try(FileWriter fw = new FileWriter(outFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(json, fw);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private static class NameParserScratch {
        boolean isValid;
        ResourceLocation resLoc;
        String namespace;
        String version;
        String name;
        VersionIndex vi;
        boolean isInJar;
        String hash;
        ResourceLocation mcResLoc;
        IResourcePack mcResPack;
    }
}
