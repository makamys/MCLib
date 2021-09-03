package makamys.mclib.updatecheck;

import java.io.File;

import makamys.mclib.updatecheck.UpdateCheckLib;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.Configuration;

public class ConfigUCL {
	
	private static boolean loaded;
	
	public static boolean enabled;
	public static boolean hideErrored;
	public static String promoChannel;
	
	public static boolean showUpdatesButton;
	
	public static int updatesButtonX;
	public static int updatesButtonY;
	public static boolean updatesButtonAbsolutePos;
	
	public static void loadIfNotAlready() {
		if(!loaded) {
			reload();
			loaded = true;
		}
	}
	
	public static void reload() {
		Configuration config = new Configuration(new File(Launch.minecraftHome, "config/" + UpdateCheckLib.MODID + ".cfg"));
        
        config.load();
        
        enabled = config.getBoolean("enable", "_general", true, "Set this to false to disable this mod");
        hideErrored = config.getBoolean("hideErrored", "_general", false, "If true, results that errored won't be shown");
        promoChannel = config.getString("promoChannel", "_general", "latest", "The update channel which should be checked. Forge defines 'latest' for the bleeding-edge versions and 'recommended' for the stable versions of mods.");
        
        showUpdatesButton = config.getBoolean("showUpdatesButton", "interface", true, "Show updates button in main menu when there are updates available");
        updatesButtonX = config.getInt("updatesButtonX", "interface", 104, Integer.MIN_VALUE, Integer.MAX_VALUE, "");
        updatesButtonY = config.getInt("updatesButtonY", "interface", 96, Integer.MIN_VALUE, Integer.MAX_VALUE, "");
        updatesButtonAbsolutePos = config.getBoolean("updatesButtonAbsolutePos", "interface", false, "true: the X and Y are absolute coordinates\nfalse: the X and Y is relative to WIDTH/2 and HEIGHT/4 respectively (which is how the buttons are placed in the vanilla menu GUI)");
        
        if (config.hasChanged()) {
            config.save();
        }
	}
	
}
