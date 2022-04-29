package makamys.mclib.sloppydeploader;

import java.io.File;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.Configuration;

public class ConfigSDL {
    
    private static boolean loaded;
    
    public static boolean enabled;
    public static boolean showRestartNotification;
    
    public static void loadIfNotAlready() {
        if(!loaded) {
            reload();
            loaded = true;
        }
    }
    
    public static void reload() {
        Configuration config = new Configuration(new File(Launch.minecraftHome, "config/sloppydeploader.cfg"));
        
        config.load();
        
        enabled = config.getBoolean("enable", "_general", true, "Set this to false to disable this module");
        
        showRestartNotification = config.getBoolean("showRestartNotification", "interface", true, "Show notification when new dependencies have been downloaded and a restart is needed.");
        
        if (config.hasChanged()) {
            config.save();
        }
    }
    
}
