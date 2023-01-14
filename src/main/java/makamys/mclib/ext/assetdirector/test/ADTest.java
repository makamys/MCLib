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
import makamys.mclib.ext.assetdirector.ADConfig;
import makamys.mclib.ext.assetdirector.AssetDirectorAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;

@Mod(modid = "ADTest", version = "0.0")
public class ADTest {
    
    @EventHandler
    public void onConstruction(FMLConstructionEvent event) {
        MCLib.init();
        
        ADConfig config = new ADConfig();
        config.addObject("1.17", "minecraft/lang/ms_my.json");
        config.addSoundEvent("1.17", "music_disc.pigstep", "record");
        config.addSoundEvent("1.17", "music_disc.pigstep", "music"); // Test of duplicate sound event
        config.addObject("1.17", "minecraft/sounds/item/bucket/fill_axolotl1.ogg"); // Test downloading single sound file (check the example sounds.json for how to use)
        config.addJar("1.17");
        config.addObject("1.19.3", "minecraft/lang/nah.json"); // Test of 1.19.3 asset (asset id is '2')
        
        AssetDirectorAPI.register(config);
    }
    
    @EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        if(event.getSide() == Side.CLIENT) {
            Blocks.gold_block.setBlockTextureName("minecraft_1.17:gold_block");
            
            try {
                JsonObject lang_1_17 = new Gson().fromJson(new BufferedReader(new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("minecraft_1.17:lang/ms_my.json")).getInputStream(), Charsets.UTF_8)), JsonObject.class);
                // Item added in 1.13
                System.out.println("Trident in Malay is " + lang_1_17.get("item.minecraft.trident"));
                
                JsonObject lang_1_19_3 = new Gson().fromJson(new BufferedReader(new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("minecraft_1.19.3:lang/nah.json")).getInputStream(), Charsets.UTF_8)), JsonObject.class);
                // Language added in 1.19.3
                System.out.println("Trident in Nahuatl is " + lang_1_19_3.get("item.minecraft.trident"));
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
    
}