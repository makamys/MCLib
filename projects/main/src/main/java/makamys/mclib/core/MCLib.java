package makamys.mclib.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.eventbus.EventBus;

import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.ReflectionHelper;
import makamys.mclib.core.sharedstate.SharedField;
import makamys.mclib.core.sharedstate.SharedLibHelper;
import makamys.mclib.updatecheck.UpdateCheckAPI;

public class MCLib {
	
	static { SharedLibHelper.shareifyClass(MCLib.class); }
	
	public static final String VERSION = "0.1.3";
	
	public static MCLib instance;
	
	public static Logger LOGGER;
	public static final Logger GLOGGER = LogManager.getLogger("mclib");
	
	private static EventBus fmlMasterChannel;
	
	@SharedField
	public static UpdateCheckAPI updateCheckAPI;
	
	public MCLib(boolean subscribe) {
		String modid = Loader.instance().activeModContainer().getModId();
		LOGGER = LogManager.getLogger("mclib(" + modid + ")");
		
		LOGGER.debug("Initializing MCLib " + VERSION + " in " + modid);
		
		SharedLibHelper.register(this);
		
		if(subscribe) {
			try {
				LoadController lc = ReflectionHelper.getPrivateValue(Loader.class, Loader.instance(), "modController");
				fmlMasterChannel = ReflectionHelper.getPrivateValue(LoadController.class, lc, "masterChannel");
				registerOnFMLBus(this);
			} catch(Exception e) {
				LOGGER.error("Failed to subscribe to LoadController's bus. The state change event handlers will have to be called manually from your mod.");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Call this in your FMLConstructionEvent handler to initialize the library framework.
	 */
	public static void init() {
		if(instance == null) {
			init(true);
		} else {
			LOGGER.warn("Tried to call init() when library was already initialized");
		}
	}
	
	public static void init(boolean subscribe) {
		instance = new MCLib(subscribe);
	}
	
	public static void registerOnFMLBus(Object object) {
		fmlMasterChannel.register(object);
	}
	
}
