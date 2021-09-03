package makamys.mclib.core.sharedstate;

import java.lang.reflect.Method;

import makamys.mclib.core.MCLibModules;
import makamys.mclib.core.OtherTestModule;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class SharedModuleMethodRedirector implements MethodInterceptor {

	@Override
	public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
		Class<?> otherClass = OtherTestModule.class;
		Object other = MCLibModules.otherTestModule;
		Method otherMethod = otherClass.getMethod(method.getName(), method.getParameterTypes());
		return otherMethod.invoke(other, args);
	}

}
