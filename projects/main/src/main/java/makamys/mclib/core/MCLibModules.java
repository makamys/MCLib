package makamys.mclib.core;

import makamys.mclib.core.sharedstate.SharedLibHelper;
import makamys.mclib.core.sharedstate.SharedField;

public class MCLibModules {
	
	@SharedField
	public static TestModule testModule;
	
	public static void init() {
		SharedLibHelper.shareifyClass(MCLibModules.class);
	}
	
}
