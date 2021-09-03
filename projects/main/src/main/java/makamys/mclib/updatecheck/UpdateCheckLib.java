package makamys.mclib.updatecheck;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.eventbus.Subscribe;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import makamys.mclib.updatecheck.gui.GuiButtonUpdates;
import makamys.mclib.core.MCLib;
import makamys.mclib.updatecheck.ConfigUCL;
import makamys.mclib.updatecheck.ResultHTMLRenderer;
import makamys.mclib.updatecheck.UpdateCheckAPI;
import makamys.mclib.updatecheck.UpdateCheckLib;
import makamys.mclib.updatecheck.UpdateCheckTask;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.common.MinecraftForge;

public class UpdateCheckLib
{
    public static final String MODID = "UpdateCheckLib";
    public static final String VERSION = "@VERSION@";
    
    public static final Logger LOGGER = LogManager.getLogger("updatechecklib");

    private static UpdateCheckLib instance;
    
    private static BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
    static ThreadPoolExecutor executor = new ThreadPoolExecutor(0, 4, 60, TimeUnit.SECONDS, workQueue);
    static List<CompletableFuture<UpdateCheckTask.Result>> futures = new ArrayList<>();
    private static int updateCount = -1;
    private static final File updatesFile = new File(Launch.minecraftHome, "updates.html");
    
    static UpdateCategory MODS = new UpdateCategory(UpdateCheckAPI.MODS_CATEGORY_ID, Loader.MC_VERSION, "Mod", false);
    static UpdateCategory RESOURCE_PACKS = new UpdateCategory(UpdateCheckAPI.RESOURCE_PACKS_CATEGORY_ID, Loader.MC_VERSION, "Resource pack", false);
    static Map<String, UpdateCategory> categories = new HashMap<>();
    
    private static final int UPDATES_BUTTON_ID = 1615486202;
    
    @SideOnly(Side.CLIENT)
    GuiButtonUpdates updatesButton;
    
    static {
    	instance = new UpdateCheckLib();
    	MCLib.registerOnFMLBus(instance);
    	MinecraftForge.EVENT_BUS.register(instance);
    	
    	categories.put(UpdateCheckAPI.MODS_CATEGORY_ID, MODS);
    	categories.put(UpdateCheckAPI.RESOURCE_PACKS_CATEGORY_ID, RESOURCE_PACKS);
    }
    
    static boolean isEnabled() {
    	ConfigUCL.loadIfNotAlready();
    	return ConfigUCL.enabled;
    }
    
    @Subscribe
    public void preInit(FMLPreInitializationEvent event) {
    	
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onGui(InitGuiEvent.Post event) {
    	if(event.gui instanceof GuiMainMenu) {
    		ConfigUCL.reload();
    		if(ConfigUCL.showUpdatesButton) {
    			String url = null;
    			try {
					url = updatesFile.toURI().toURL().toString();
				} catch (MalformedURLException e) {
					url = "";
					e.printStackTrace();
				}
    			int buttonX = ConfigUCL.updatesButtonX + (ConfigUCL.updatesButtonAbsolutePos ? 0 : event.gui.width / 2);
    			int buttonY = ConfigUCL.updatesButtonY + (ConfigUCL.updatesButtonAbsolutePos ? 0 : event.gui.height / 4);
	    		updatesButton = new GuiButtonUpdates(UPDATES_BUTTON_ID, buttonX, buttonY, 20, 20, updateCount, url);
	    		event.buttonList.add(updatesButton);
    		}
    	} else {
    		updatesButton = null;
    	}
    }
    
    @Subscribe
    public void postInit(FMLPostInitializationEvent event) {
    	if(!isEnabled()) return;
    	CompletableFuture.allOf(futures.toArray(new CompletableFuture[] {})).thenRun(new Runnable() {	
			@Override
			public void run() {
				List<UpdateCheckTask.Result> results = futures.stream().map(f -> {
					try {
						return f.get();
					} catch(Exception e) {
						LOGGER.error("Failed to get retrieve update check result: " + e.getMessage());
						return null;
					}
				})
				.collect(Collectors.toList());
				
				updateCount = 0;
				for(UpdateCheckTask.Result result : results) {
					if(result.foundUpdate()) {
						updateCount++;
					}
					result.task.category.results.add(result);
				}
				
				onFinished();
				if(event.getSide() == Side.CLIENT) {
					onFinishedClient();
				}
			}
		});
    }
    
    private void onFinished() {
    	LOGGER.info("Found " + updateCount + " updates.");
    	new ResultHTMLRenderer().render(updatesFile);
    }
    
    @SideOnly(Side.CLIENT)
    private void onFinishedClient() {
    	if(updatesButton != null) {
    		updatesButton.setUpdateCount(updateCount);
    	}
    }
    
    static class UpdateCategory implements Comparable<UpdateCategory> {
    	public String id;
    	public String displayName;
    	public String version;
    	public boolean backwardsCompatible; 
    	public List<UpdateCheckTask.Result> results = new ArrayList<>();
    	
    	public UpdateCategory(String id, String version, String displayName, boolean backwardsCompatible) {
    		this.id = id;
    		this.version = version;
    		this.displayName = displayName;
    		this.backwardsCompatible = backwardsCompatible;
    	}

		@Override
		public int compareTo(UpdateCategory o) {
			return this.id.equals(UpdateCheckAPI.MODS_CATEGORY_ID) ? -1 : displayName.compareTo(o.displayName);
		}
    }
}
