package makamys.mclib.core;

import makamys.mclib.core.sharedstate.SharedModuleMethodRedirector;
import net.sf.cglib.proxy.Enhancer;

public class MCLibModules {
	
	public static TestModule testModule;
	public static OtherTestModule otherTestModule;
	
	public static void init() {
		otherTestModule = new OtherTestModule();
		try {
			Enhancer e = new Enhancer();
			e.setSuperclass(TestModule.class);
			e.setCallback(new SharedModuleMethodRedirector());
			Object obj = e.create();
			testModule = (TestModule)obj;
			testModule.hi();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
}
