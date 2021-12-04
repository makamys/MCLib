package makamys.mclib.ext.assetdirector.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.io.Charsets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import makamys.mclib.core.MCLib;
import makamys.mclib.ext.assetdirector.AssetDirectorAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = "ADTest", version = "0.0")
public class ADTest {
    
    @EventHandler
    public void onConstruction(FMLConstructionEvent event) {
        MCLib.init();
        AssetDirectorAPI.register();
    }
    
    @EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        Blocks.gold_block.setBlockTextureName("minecraft_1.17:gold_block");
        
        try {
            JsonObject lang = new Gson().fromJson(new BufferedReader(new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("minecraft_1.17:lang/ms_my.json")).getInputStream(), Charsets.UTF_8)), JsonObject.class);
            System.out.println("Trident in Malay is " + lang.get("item.minecraft.trident"));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
}