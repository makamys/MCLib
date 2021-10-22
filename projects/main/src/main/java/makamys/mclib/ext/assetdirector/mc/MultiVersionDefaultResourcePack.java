package makamys.mclib.ext.assetdirector.mc;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cpw.mods.fml.relauncher.ReflectionHelper;
import makamys.mclib.ext.assetdirector.AssetFetcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

public class MultiVersionDefaultResourcePack implements IResourcePack {
    
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
        return fetcher.getAssetInputStream(scratch.hash);
    }

    public boolean resourceExists(ResourceLocation resLoc) {
        parseName(resLoc);
        return fetcher.getObjectIndex().contains(scratch.hash);
    }
    
    private void parseName(ResourceLocation resLoc){
        if(resLoc.equals(scratch.resLoc)) return;
        
        String fullDomain = resLoc.getResourceDomain();
        int firstUnderscore = fullDomain.indexOf('_');
        scratch.resLoc = resLoc;
        scratch.namespace = fullDomain.substring(0, firstUnderscore);
        scratch.version = fullDomain.substring(firstUnderscore + 1);
        scratch.name = resLoc.getResourcePath();
        scratch.hash = fetcher.assetIndexes.get(scratch.version).nameToHash.get(scratch.namespace + "/" + scratch.name);
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
    
    private static class NameParserScratch {
        ResourceLocation resLoc;
        String namespace;
        String version;
        String name;
        String hash;
    }
}
