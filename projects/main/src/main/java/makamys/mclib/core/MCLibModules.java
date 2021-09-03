package makamys.mclib.core;

import makamys.mclib.core.sharedstate.SharedLibHelper;

public class MCLibModules {
	
	public static TestModule testModule;
	
	public static void init() {
		SharedLibHelper.shareifyClass(MCLibModules.class);
	}
	
}
