package makamys.mclib.core;

import makamys.mclib.core.sharedstate.SharedLibHelper;
import makamys.mclib.core.sharedstate.SharedField;

public class MCLibModules {
	
	static { SharedLibHelper.shareifyClass(MCLibModules.class); }
	
	@SharedField
	public static TestModule testModule;
	
}
