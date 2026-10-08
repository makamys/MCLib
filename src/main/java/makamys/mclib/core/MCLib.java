package makamys.mclib.core;

import java.util.Map;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import makamys.mclib.core.sharedstate.SharedLibHelper;
import makamys.mclib.core.sharedstate.SharedReference;

public class MCLib {
    
    public static final String VERSION = "@VERSION@";
    public static final String RESOURCES_VERSION = "v0_3_7";
    
    public static MCLib instance;
    
    public static Logger LOGGER = LogManager.getLogger("mclib()");
    public static final Logger GLOGGER = LogManager.getLogger("mclib");
    
    public static EventBus FML_MASTER;

    private static final MutableBoolean modEventHandlerRegistered = SharedReference
            .get("mclib", "modEventHandlerRegistered", MutableBoolean.class);

    public MCLib(boolean subscribe) {
        String modid = Loader.instance().activeModContainer().getModId();
        LOGGER = LogManager.getLogger("mclib(" + modid + ")");
        
        LOGGER.debug("Initializing MCLib " + VERSION + " in " + modid);
        
        SharedLibHelper.register(this);
        
        if(subscribe) {
            try {
                LoadController lc = ReflectionHelper.getPrivateValue(Loader.class, Loader.instance(), "modController");
                FML_MASTER = ReflectionHelper.getPrivateValue(LoadController.class, lc, "masterChannel");

                if(modEventHandlerRegistered.isFalse()) {
                    Map<String, EventBus> eventChannels = ReflectionHelper
                            .getPrivateValue(LoadController.class, lc, "eventChannels");
                    ModEventHandler modEventHandler = new ModEventHandler();
                    for(EventBus modEventBus : eventChannels.values()) {
                        modEventBus.register(modEventHandler);
                    }
                    modEventHandlerRegistered.setTrue();
                }

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
        if(modEventHandlerRegistered.isFalse()) {
            // We can only reach this branch when no MCLib instance registered the automatic
            // per-mod pre-init handler. In that case this method must have been forwarded
            // manually by the current mod, so we run its per-mod tasks here.
            TaskQueue.runModTasks(LoaderState.PREINITIALIZATION);
        }
        if(SharedLibHelper.isNewestLib(this)) {
            LOGGER.trace("Running preinit");
            InternalModules.sloppyDepLoader.preInit();
        }
        TaskQueue.consume(LoaderState.PREINITIALIZATION, instance);
    }

    private static class ModEventHandler {

        @Subscribe
        public void onPreInit(FMLPreInitializationEvent event) {
            TaskQueue.runModTasks(LoaderState.PREINITIALIZATION);
        }
    }
    
}
