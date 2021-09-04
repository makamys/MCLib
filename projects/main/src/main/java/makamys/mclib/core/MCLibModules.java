package makamys.mclib.core;

import makamys.mclib.core.sharedstate.SharedLibHelper;
import makamys.mclib.sloppydeploader.SloppyDepLoader;
import makamys.mclib.updatecheck.UpdateCheckAPI;

public class MCLibModules {
	
	static { SharedLibHelper.shareifyClass(MCLibModules.class); }
	
	public static UpdateCheckAPI updateCheckAPI;
}
