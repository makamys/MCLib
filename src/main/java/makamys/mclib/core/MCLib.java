package makamys.mclib.core;

import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.versioning.ComparableVersion;
import cpw.mods.fml.relauncher.ReflectionHelper;
import makamys.mclib.core.sharedstate.SharedLibHelper;

public class MCLib {
    
    public static final String VERSION = "@VERSION@";
    public static final String RESOURCES_VERSION = "v0_3_7";
    
    public static MCLib instance;
    
    public static Logger LOGGER = LogManager.getLogger("mclib()");
    public static final Logger GLOGGER = LogManager.getLogger("mclib");
    
    public static EventBus FML_MASTER;
    
    public MCLib(boolean subscribe) {
        String modid = Loader.instance().activeModContainer().getModId();
        LOGGER = LogManager.getLogger("mclib(" + modid + ")");
        
        LOGGER.debug("Initializing MCLib " + VERSION + " in " + modid);
        
        SharedLibHelper.register(this);
        
        if(subscribe) {
            try {
                LoadController lc = ReflectionHelper.getPrivateValue(Loader.class, Loader.instance(), "modController");
                FML_MASTER = ReflectionHelper.getPrivateValue(LoadController.class, lc, "masterChannel");
                FML_MASTER.register(this);
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
        }
    }
    
    public static void init(boolean subscribe) {
        instance = new MCLib(subscribe);
    }
    
    @Subscribe
    public void onPreInit(FMLPreInitializationEvent event) {
        if(SharedLibHelper.isNewestLib(this)) {
            LOGGER.trace("Running preinit");
            InternalModules.sloppyDepLoader.preInit();
        }
        TaskQueue.consume(LoaderState.PREINITIALIZATION, instance);
    }
    
}
