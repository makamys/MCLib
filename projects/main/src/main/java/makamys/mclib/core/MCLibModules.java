package makamys.mclib.core;

import makamys.mclib.core.sharedstate.SharedField;
import makamys.mclib.core.sharedstate.SharedLibHelper;
import makamys.mclib.updatecheck.UpdateCheckAPI;

public class MCLibModules {
	
	static { SharedLibHelper.shareifyClass(MCLibModules.class); }
	
	@SharedField
	public static UpdateCheckAPI updateCheckAPI;
}
